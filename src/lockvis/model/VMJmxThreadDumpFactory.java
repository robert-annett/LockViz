package lockvis.model;

import java.lang.Thread.State;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VMJmxThreadDumpFactory {

	public static VMThreadDump constructVMThreadDump(ThreadMXBean bean, String sourceUrl) {
		
        MutexFactory mutexFactory = new MutexFactory();
        VMThreadDump vmThreadDump = new VMThreadDump();

        Date timestamp = new Date();
        vmThreadDump.setOverrideName(sourceUrl + " at " + timestamp);	        
		vmThreadDump.setTimestamp(timestamp);
        vmThreadDump.setDumpName(sourceUrl);
		
        ThreadInfo[] tinfos = bean.dumpAllThreads(true, true);
        long[] deadLocks = bean.findDeadlockedThreads();
        Set<Long> deadlockSet = new HashSet<Long>();
		if (deadLocks!=null)	{
			vmThreadDump.setDeadLockInfo("Found " + deadLocks.length + " deadlocks");
			for (long l : deadLocks) {
				deadlockSet.add(l);							
			}
		}
        
		for (ThreadInfo ti : tinfos) {
			
			lockvis.model.ThreadInfo threadInfoModel = new lockvis.model.ThreadInfo(ti.getThreadName());
			vmThreadDump.addThreadInfo(threadInfoModel);
			threadInfoModel.setState(ti.getThreadState());
			
			if (deadlockSet.contains(ti.getThreadId()))	{
				threadInfoModel.setInDeadLock(true);
			}
			
			//Add all the threadlocations first
			StackTraceElement[] stacktrace = ti.getStackTrace();
			for (StackTraceElement stackTraceElement : stacktrace) {
				ThreadLocation threadLocation = new ThreadLocation(stackTraceElement.toString(), threadInfoModel);
				threadInfoModel.addLocation(threadLocation);				
			}
			
			
			//Add all the monitor infos into the stack
			if (ti.getLockName() != null) {
				LockInfo li = ti.getLockInfo();
				//todo: We should directly examine state in the mutexFactory
				State threadState = ti.getThreadState();
				Mutex mutex;
				if (li instanceof MonitorInfo)	{ 
					mutex = mutexFactory.getMutex(Integer.toHexString(li.getIdentityHashCode()), li.getClassName(), "waiting to lock");
				}
				else	{
					mutex = mutexFactory.getMutex(Integer.toHexString(li.getIdentityHashCode()), li.getClassName(), "waiting");
				}
                ThreadLocation topElement = threadInfoModel.getByIndex(0);
				MutexAction ma = new MutexAction(threadInfoModel, topElement, mutex, threadState.toString());
                threadInfoModel.addMutexAction(ma);                        
                topElement.addMutexAction(ma);
			}
			MonitorInfo[] monitors = ti.getLockedMonitors();
			for (MonitorInfo monitorInfo : monitors) {
				Mutex mutex = mutexFactory.getMutex(Integer.toHexString(monitorInfo.getIdentityHashCode()), monitorInfo.getClassName(), "locked");
				ThreadLocation threadLocation = threadInfoModel.getByIndex(monitorInfo.getLockedStackDepth());
                MutexAction ma = new MutexAction(threadInfoModel, threadLocation, mutex, "locked");
                threadInfoModel.addMutexAction(ma);                        
                threadLocation.addMutexAction(ma);			
			}
			
			//All the locked synchronizers
			LockInfo[] locks = ti.getLockedSynchronizers();
			for (LockInfo lockInfo : locks) {
				Mutex mutex = mutexFactory.getMutex(Integer.toHexString(lockInfo.getIdentityHashCode()), lockInfo.getClassName(), "Owned");
				ThreadLocation threadLocation = threadInfoModel.getStack().get(threadInfoModel.getStack().size()-1);
                MutexAction ma = new MutexAction(threadInfoModel, threadLocation, mutex, "Owned");
                threadInfoModel.addMutexAction(ma);                        
                threadLocation.addMutexAction(ma);	   
			}
		    
		}
		
		vmThreadDump.setJniGLobalReferences("NA");

        SimpleGraphExtractor sge = new SimpleGraphExtractor();
        List<ThreadInfoSet> entanglements = sge.extractSimpleGraphs(vmThreadDump.getThreadInfos());
        vmThreadDump.setSimpleGraphs(entanglements);
		
		
		return vmThreadDump;
	}
}
