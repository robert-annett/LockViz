package lockvis.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Thread.State;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class VMThreadDumpFactory {

	private static Logger LOGGER = Logger.getLogger(VMThreadDumpFactory.class.getName());
	private static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public VMThreadDump constructVMThreadDump(String file) throws IOException, ParseException {
		File threadDump = new File(file);
		VMThreadDump td = constructVMThreadDump(threadDump);
		return td;
	}

	public VMThreadDump constructVMThreadDump(File threadDump) throws IOException, ParseException {

		try (BufferedReader br = new BufferedReader(new FileReader(threadDump))) {
			VMThreadDump vmThreadDump = constructVMThreadDump8(br);
			vmThreadDump.setOverrideName(threadDump.getName());
			return vmThreadDump;
		}
	}

	
	//TODO: Replace this badly structured method with a statefull parser. Break back into nested block progressing counter.
	public VMThreadDump constructVMThreadDump8(BufferedReader br) throws ParseException {
		VMThreadDump vmThreadDump = new VMThreadDump();
		
		// read this in and collect into a List<String> so I can process it multiple times
		List<String> lines = br.lines().collect(Collectors.toList());

		vmThreadDump.setTimestamp(getTimeStamp(lines));
		vmThreadDump.setDumpName(getDumpName(lines));
		vmThreadDump.setJniGLobalReferences(getJNIGlobalReferences(lines));
		vmThreadDump.setDeadLockInfo(getDeadlockInfo(lines));

		List<List<String>> threadBlocks = splitOutThreadInfoBlocks(lines);

		MutexFactory mutexFactory = new MutexFactory();
		ThreadDumpMapper threadDumpMapper = new ThreadDumpMapper(mutexFactory);
		threadBlocks.stream().map(threadDumpMapper).forEach(p -> vmThreadDump.addThreadInfo(p));

		

				
		//find the deadlock info
		//TODO Write code to extract out the deadlock blocks and then map them to updates.
		boolean deadlockBockFound = false;
		for (int lineCnt=0; lineCnt<lines.size();lineCnt++)	{
			String line = lines.get(lineCnt).trim();
			if (line.startsWith("Found ") ) {
				deadlockBockFound = true;				
			}
			else if (line.startsWith("Java stack information"))	{
				break;
			}
			else if (!deadlockBockFound)	{
				continue;
			}
			else	{
				//Mark the deadlocked threads
				if (line.startsWith("\"")) {
					String threadId = line.replace("\"", "").replace(":", "");
					ThreadInfo dump = vmThreadDump.getDump(threadId);
					dump.setInDeadLock(true);
				}

				// find and mark the mutexes listed
				else if (line.startsWith("waiting to lock monitor")) {
					String mutexId = line.split(" ")[6].replace(",", "");
					Mutex find = mutexFactory.find(mutexId);
					find.setInDeadLock(true);
				}
				else if (line.startsWith("waiting for ownable synchronizer")) {
					String mutexId = line.split(" ")[4].replace(",", "");
					Mutex find = mutexFactory.find(mutexId);
					find.setInDeadLock(true);
					//now need to use the "which is held by xx" to tag the mutex onto that thread.
					String linea = lines.get(++lineCnt).trim();
					String holder = linea.substring(linea.indexOf("\"")+1, linea.indexOf("\"", linea.indexOf("\"")+1));
					ThreadInfo thread = vmThreadDump.getDump(holder);
					
					ThreadLocation lastThreadLocation = thread.getByIndex(thread.getStack().size()-1);
					MutexAction ma = new MutexAction(thread, lastThreadLocation , find, "Owned");
					thread.addMutexAction(ma);
					lastThreadLocation.addMutexAction(ma);
				}
			}
			
		}
		
		//Calc the entanglements
		SimpleGraphExtractor sge = new SimpleGraphExtractor();
		List<ThreadInfoSet> entanglements = sge.extractSimpleGraphs(vmThreadDump.getThreadInfos());
		vmThreadDump.setSimpleGraphs(entanglements);

		
		return vmThreadDump;
	}


	private class ThreadDumpMapper implements Function<List<String>, ThreadInfo> {

		private MutexFactory mutexFactory;

		public ThreadDumpMapper(MutexFactory mutexFactory) {
			this.mutexFactory = mutexFactory;
		}

		@Override
		public ThreadInfo apply(List<String> lines) {
			ThreadInfo stackTrace = new ThreadInfo(lines.get(0));

			Optional<String> stateS = lines.stream().filter(p -> p.startsWith("java.lang.Thread.State")).findFirst();
			if (!stateS.isPresent()) {
				return stackTrace;
			}

			stackTrace.setState(State.valueOf(stateS.get().split(" ")[1].trim()));

			ThreadLocation lastThreadLocation = null;

			int i = 2;
			while (i<lines.size() && !lines.get(i).startsWith("Locked ownable synchronizers")) { //this marker line may not be present
				String line = lines.get(i++);
				if (line.startsWith("- ")) {
					// it's a lock or wait. Add it to the previous location created.
					String[] split = line.split(" ");
					String monitorOwnershipState = split[1].trim();
					String lockId = getLockId(line);
					String objectType = getObjectType(split);
					String action = getMutexAction(line);
					Mutex mutex = mutexFactory.getMutex(lockId, objectType, action); // this is an object monitor
					MutexAction ma = new MutexAction(stackTrace, lastThreadLocation, mutex, monitorOwnershipState);
					stackTrace.addMutexAction(ma);
					lastThreadLocation.addMutexAction(ma);
				} else if (line.startsWith("at ")) {
					// it's a thread location
					lastThreadLocation = new ThreadLocation(line, stackTrace);
					stackTrace.addLocation(lastThreadLocation);
				}
			}

			for (i++; i < lines.size(); i++) {
				String line = lines.get(i);
				if (line.equals("- None")) {
					break;
				}

				String lockId = getLockId(line);
				String objectType = getObjectType(line.split(" "));
				String action = getMutexAction(line);
				Mutex mutex = mutexFactory.getMutex(lockId, objectType, action); // this is an ownable synchronizer
				MutexAction ma = new MutexAction(stackTrace, lastThreadLocation, mutex, "Owned");
				stackTrace.addMutexAction(ma);
				lastThreadLocation.addMutexAction(ma);
			}

			return stackTrace;
		}

	}

	/*
	 * Extract all the text blocks that represent threads.
	 */
	private List<List<String>> splitOutThreadInfoBlocks(List<String> lines) {

		List<List<String>> threadBlocks = new ArrayList<List<String>>();
		
		// This initial block (non-thread info) is not added so will be thrown away
		List<String> currentBlock = new ArrayList<String>(); 

		for (String line : lines) {
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}

			// JNI global references means the thread section is over
			if (line.startsWith("JNI global references")) {
				break;
			}

			// Opening with a quote means it's a new threadblock
			if (line.startsWith("\"")) {
				currentBlock = new ArrayList<String>();
				threadBlocks.add(currentBlock);
			}
			currentBlock.add(line.trim());
		}

		return threadBlocks;
	}

	public VMThreadDump constructVMThreadDump(BufferedReader br) throws IOException, ParseException {

		MutexFactory mutexFactory = new MutexFactory();
		VMThreadDump vmThreadDump = new VMThreadDump();

		vmThreadDump.setTimestamp(getTimeStamp(br));
		vmThreadDump.setDumpName(getDumpName(br));

		// repeat for the dumps
		String fileLine = getNextLine(br).trim();
		threadloop: while (!fileLine.startsWith("JNI global references")) {
			ThreadInfo stackTrace = new ThreadInfo(fileLine);
			vmThreadDump.addThreadInfo(stackTrace);
			String state = getNextLine(br).trim();
			if (!state.startsWith("java.lang.Thread.State")) {
				// must be a thread with no state has no stack so move onto next one
				fileLine = state;
				continue threadloop;
			}
			String stateName = state.split(" ")[1].trim();
			State threadState = java.lang.Thread.State.valueOf(stateName);
			stackTrace.setState(threadState);

			// repeat for the thread locations within the dumps
			ThreadLocation lastThreadLocation = null;
			while (true) {
				String line = getNextLine(br);
				if (line.startsWith("- ")) {
					// it's a lock or wait. Add it to the previous location created.
					String[] split = line.split(" ");
					String monitorOwnershipState = split[1].trim();
					String lockId = getLockId(line);
					String objectType = getObjectType(split);
					String action = getMutexAction(line);
					Mutex mutex = mutexFactory.getMutex(lockId, objectType, action); // this is an object monitor
					MutexAction ma = new MutexAction(stackTrace, lastThreadLocation, mutex, monitorOwnershipState);
					stackTrace.addMutexAction(ma);
					lastThreadLocation.addMutexAction(ma);
				} else if (line.startsWith("at ")) {
					// it's a thread location
					lastThreadLocation = new ThreadLocation(line, stackTrace);
					stackTrace.addLocation(lastThreadLocation);
				} else if (line.startsWith("Locked ownable synchronizers")) {
					// It's a Locked ownable synchronizers. Last line in the stack dump.
					while (true) {
						fileLine = getNextLine(br).trim();
						if (fileLine.equals("- None")) {
							fileLine = getNextLine(br);
							break;
						}
						if (fileLine.startsWith("\"")) {
							break;
						}

						String lockId = getLockId(fileLine);
						String objectType = getObjectType(fileLine.split(" "));
						String action = getMutexAction(fileLine);
						Mutex mutex = mutexFactory.getMutex(lockId, objectType, action); // this is an ownable synchronizer
						MutexAction ma = new MutexAction(stackTrace, lastThreadLocation, mutex, "Owned");
						stackTrace.addMutexAction(ma);
						lastThreadLocation.addMutexAction(ma);
					}

					continue threadloop;
				} else {
					LOGGER.warning("Should never reach here the file is not constructed as expected.");
				}
			}

		}

		// JNI global references
		vmThreadDump.setJniGLobalReferences(fileLine);

		String lookingForDeadLock = getNextLine(br);
		while (lookingForDeadLock != null && !lookingForDeadLock.startsWith("Found ")) {
			lookingForDeadLock = getNextLine(br);
		}
		vmThreadDump.setDeadLockInfo(lookingForDeadLock);

		if (lookingForDeadLock != null) {
			// now we want to process the locks listed so we can mark them as know to be part of the deadlock
			getNextLine(br);
			String deadLockMarkingLines = getNextLine(br);
			while (!deadLockMarkingLines.startsWith("Java stack information")) {
				// find and mark the threads listed.
				if (deadLockMarkingLines.startsWith("\"")) {
					String threadId = deadLockMarkingLines.replace("\"", "").replace(":", "");
					ThreadInfo dump = vmThreadDump.getDump(threadId);
					dump.setInDeadLock(true);
				}

				// find and mark the mutexes listed
				if (deadLockMarkingLines.startsWith("waiting to lock monitor")) {
					String mutexId = deadLockMarkingLines.split(" ")[6].replace(",", "");
					Mutex find = mutexFactory.find(mutexId);
					find.setInDeadLock(true);
				}

				deadLockMarkingLines = getNextLine(br);
			}
		}

		SimpleGraphExtractor sge = new SimpleGraphExtractor();
		List<ThreadInfoSet> entanglements = sge.extractSimpleGraphs(vmThreadDump.getThreadInfos());
		vmThreadDump.setSimpleGraphs(entanglements);
		return vmThreadDump;
	}

	private String getObjectType(String[] split) {
		return split[split.length - 1].replace(")", "").trim();
	}

	private String getLockId(String line) {
		return line.substring(line.indexOf("<") + 1, line.indexOf(">")).trim();
	}

	private String getMutexAction(String line) {
		return line.substring(line.indexOf("-") + 1, line.indexOf("<")).trim();
	}

	private String getNextLine(BufferedReader br) throws IOException {
		String eachLine = br.readLine();
		while (eachLine != null) {
			if (hasContent(eachLine)) {
				return eachLine.trim();
			}
			eachLine = br.readLine();
		}
		return null;
	}

	private String getDumpName(List<String> dump) throws ParseException {
		Optional<String> dumpTitle = dump.stream().filter(p -> p.startsWith("Full thread dump")).findFirst();
		return dumpTitle.orElseThrow(() -> new ParseException("Could not find title ", 0));
	}

	private String getDeadlockInfo(List<String> dump) throws ParseException {
		return dump.stream().filter(p -> p.startsWith("Found ")).findFirst().orElse("");
	}

	private String getJNIGlobalReferences(List<String> dump) throws ParseException {
		Optional<String> dumpTitle = dump.stream().filter(p -> p.startsWith("JNI global references")).findFirst();
		return dumpTitle.orElseThrow(() -> new ParseException("Could not find references ", 0));
	}

	private Date getTimeStamp(List<String> dump) throws ParseException {
		Optional<String> dumpTimestamp = dump.stream()
				.filter(p -> p.matches("\\d\\d\\d\\d-\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d")).findFirst();
		return timestampFormat.parse(dumpTimestamp
				.orElseThrow(() -> new ParseException("Could not find timestamp ", 0)));
	}

	private String getDumpName(BufferedReader br) throws IOException, ParseException {
		String line = getNextLine(br);

		if (line.startsWith("Full thread dump")) {
			return line;
		} else {
			throw new ParseException("Could not find title ", 0);
		}
	}

	private Date getTimeStamp(BufferedReader br) throws IOException, ParseException {
		String line = getNextLine(br);
		try {
			return timestampFormat.parse(line);
		} catch (ParseException e) {
			throw new ParseException("Could not transform first line into timestamp " + e.getMessage(), 0);
		}
	}

	private boolean hasContent(String eachLine) {
		return eachLine != null && !(eachLine.trim().equals(""));
	}

}
