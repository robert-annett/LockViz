package lockvis.model;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import lockvis.model.Mutex.LockType;

import org.junit.Test;

public class TestVMThreadDumpFactory {

	
	
	@Test
	public void testMIssionControlDump() {
		VMThreadDumpFactory dumpFactory = new VMThreadDumpFactory();		
		try {
			VMThreadDump constructVMThreadDump = dumpFactory.constructVMThreadDump8(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(missionControlDump.getBytes()))));
			assertNotNull(constructVMThreadDump);
			assertEquals("Timestamp from dump", 1423842632000L, constructVMThreadDump.getTimestamp().getTime());
		} catch (ParseException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testTimestampExtraction() {
		VMThreadDumpFactory dumpFactory = new VMThreadDumpFactory();		
		try {
			VMThreadDump constructVMThreadDump = dumpFactory.constructVMThreadDump8(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(dumpFile.getBytes()))));
			assertNotNull(constructVMThreadDump);
			assertEquals("Timestamp from dump", 1326722826000L, constructVMThreadDump.getTimestamp().getTime());
		} catch (ParseException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testNameExtraction() {
		VMThreadDumpFactory dumpFactory = new VMThreadDumpFactory();		
		try {
			VMThreadDump constructVMThreadDump = dumpFactory.constructVMThreadDump8(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(dumpFile.getBytes()))));
			assertNotNull(constructVMThreadDump);
			assertEquals("Name from dump", "Full thread dump Java HotSpot(TM) Client VM (16.0-b13 mixed mode, sharing):", constructVMThreadDump.getDumpName());
		} catch (ParseException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testConstructVMThreadDumpWithDeadlock() {
		VMThreadDumpFactory dumpFactory = new VMThreadDumpFactory();
		
		InputStream is = new ByteArrayInputStream(dumpFile.getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		try {
			VMThreadDump constructVMThreadDump = dumpFactory.constructVMThreadDump8(br);
			assertNotNull(constructVMThreadDump);
			assertEquals("jniGlobal", "JNI global references: 824", constructVMThreadDump.getJniGLobalReferences());
			assertEquals("Found DeadLocks", "Found one Java-level deadlock:", constructVMThreadDump.getDeadLockInfo());
			assertEquals("Number of Threads in dump", 17, constructVMThreadDump.getThreadInfos().size());
			assertEquals("Number of Simple Graphs in dump", 16, constructVMThreadDump.getSimpleGraphs().size());
			assertEquals("Timestamp from dump", 1326722826000L, constructVMThreadDump.getTimestamp().getTime());
			
			List<ThreadInfoSet> graph = constructVMThreadDump.getSimpleGraphs().stream().filter(p-> p.getDumpName().equals("Popper-Descartes")).collect(Collectors.toList());
			assertEquals("Should be a matching Graph found", 1, graph.size());
			assertEquals("Should be deadlocked", true, graph.get(0).isInDeadlock());
			
			graph = constructVMThreadDump.getSimpleGraphs().stream().filter(p-> p.getDumpName().equals("RMI TCP Connection(3)-10.1.10.26")).collect(Collectors.toList());
			assertEquals("Should be a matching Graph found", 1, graph.size());
			assertEquals("Should not be deadlocked", false, graph.get(0).isInDeadlock());
			
			List<ThreadInfo> threadInfo = constructVMThreadDump.getThreadInfos().stream().filter(p-> p.getName().equals("Popper")).collect(Collectors.toList());
			assertEquals("Should be a matching thread found", 1, threadInfo.size());
			assertEquals("In correct entanglement", "Popper-Descartes", threadInfo.get(0).getContainingEntanglement().getDumpName());
			assertEquals("Deadlocked thread", true, threadInfo.get(0).isInDeadLock());
			assertEquals("State", java.lang.Thread.State.BLOCKED, threadInfo.get(0).getState());
			assertEquals("Size of stack", 3, threadInfo.get(0).getStack().size());

			List<ThreadInfo> threadInfoD = constructVMThreadDump.getThreadInfos().stream().filter(p-> p.getName().equals("Descartes")).collect(Collectors.toList());
			assertEquals("Should be a matching thread found", 1, threadInfoD.size());
			assertEquals("In correct entanglement", "Popper-Descartes", threadInfoD.get(0).getContainingEntanglement().getDumpName());
			assertEquals("Deadlocked thread", true, threadInfoD.get(0).isInDeadLock());
			assertEquals("State", java.lang.Thread.State.BLOCKED, threadInfoD.get(0).getState());
			assertEquals("Size of stack", 3, threadInfoD.get(0).getStack().size());
			
			assertTrue("The mutexes in the deadlock should be shared", threadInfo.get(0).getMutexActions().get(0).getMutex() == threadInfoD.get(0).getMutexActions().get(1).getMutex());
			assertTrue("The mutexes in the deadlock should be shared", threadInfo.get(0).getMutexActions().get(1).getMutex() == threadInfoD.get(0).getMutexActions().get(0).getMutex());
			assertEquals("Mutex State", "waiting" , threadInfo.get(0).getMutexActions().get(0).getState());
			assertEquals("Mutex State", "locked" , threadInfo.get(0).getMutexActions().get(1).getState());
			assertEquals("Mutex Action should point back to Thread", threadInfo.get(0) , threadInfo.get(0).getMutexActions().get(0).getActor());
			assertEquals("Mutex Action should point back to Thread", threadInfo.get(0) , threadInfo.get(0).getMutexActions().get(1).getActor());
			assertEquals("Mutex State", "waiting" , threadInfoD.get(0).getMutexActions().get(0).getState());
			assertEquals("Mutex State", "locked" , threadInfoD.get(0).getMutexActions().get(1).getState());
			assertEquals("Lock Type of mutexes", LockType.ObjectMonitor, threadInfo.get(0).getMutexActions().get(0).getMutex().getLockType());
			assertEquals("Lock Type of mutexes", LockType.ObjectMonitor, threadInfo.get(0).getMutexActions().get(1).getMutex().getLockType());
			assertEquals("Location of Mutex", "at lockvis.DeadlockGenerator$Philosopher.eat(DeadlockGenerator.java:27)", threadInfo.get(0).getMutexActions().get(0).getPosition().getLocation());

			threadInfo = constructVMThreadDump.getThreadInfos().stream().filter(p-> p.getName().equals("RMI TCP Connection(3)-10.1.10.26")).collect(Collectors.toList());
			assertEquals("Should be a matching thread found", 1, threadInfo.size());
			assertEquals("In correct entanglement", "RMI TCP Connection(3)-10.1.10.26", threadInfo.get(0).getContainingEntanglement().getDumpName());
			assertEquals("Deadlocked thread", false, threadInfo.get(0).isInDeadLock());
			assertEquals("State", java.lang.Thread.State.RUNNABLE, threadInfo.get(0).getState());
			assertEquals("Size of stack", 11, threadInfo.get(0).getStack().size());

			threadInfo = constructVMThreadDump.getThreadInfos().stream().filter(p-> p.getName().equals("JMX server connection timeout 15")).collect(Collectors.toList());
			assertEquals("Should be a matching thread found", 1, threadInfo.size());
			assertEquals("In correct entanglement", "JMX server connection timeout 15", threadInfo.get(0).getContainingEntanglement().getDumpName());
			assertEquals("Deadlocked thread", false, threadInfo.get(0).isInDeadLock());
			assertEquals("State", java.lang.Thread.State.TIMED_WAITING, threadInfo.get(0).getState());
			assertEquals("Number mutex actions", 2, threadInfo.get(0).getMutexActions().size());
			assertEquals("Size of stack", 3, threadInfo.get(0).getStack().size());
			
			
		} catch ( ParseException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	

	private static String dumpFile = "2012-01-16 14:07:06\r\n" + 
			"Full thread dump Java HotSpot(TM) Client VM (16.0-b13 mixed mode, sharing):\r\n" + 
			"\r\n" + 
			"\"Popper\" prio=6 tid=0x02b1d400 nid=0x1350 waiting for monitor entry [0x02eef000]\r\n" + 
			"   java.lang.Thread.State: BLOCKED (on object monitor)\r\n" + 
			"	at lockvis.DeadlockGenerator$Philosopher.eat(DeadlockGenerator.java:27)\r\n" + 
			"	- waiting to lock <0x22e60d70> (a lockvis.DeadlockGenerator$Chopstick)\r\n" + 
			"	- locked <0x22e60d78> (a lockvis.DeadlockGenerator$Chopstick)\r\n" + 
			"	at lockvis.DeadlockGenerator$2.run(DeadlockGenerator.java:51)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:619)\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"	\r\n" + 
			"\"RMI TCP Connection(3)-10.1.10.26\" daemon prio=6 tid=0x0318a000 nid=0x1538 runnable [0x034af000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"	at java.net.SocketInputStream.socketRead0(Native Method)\r\n" + 
			"	at java.net.SocketInputStream.read(SocketInputStream.java:129)\r\n" + 
			"	at java.io.BufferedInputStream.fill(BufferedInputStream.java:218)\r\n" + 
			"	at java.io.BufferedInputStream.read(BufferedInputStream.java:237)\r\n" + 
			"	- locked <0x229b2798> (a java.io.BufferedInputStream)\r\n" + 
			"	at java.io.FilterInputStream.read(FilterInputStream.java:66)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:517)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:790)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:649)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor$Worker.runTask(ThreadPoolExecutor.java:886)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:908)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:619)\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- <0x2299ede0> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)\r\n" + 
			"\r\n" + 
			"\"RMI TCP Connection(2)-10.1.10.26\" daemon prio=6 tid=0x0318ac00 nid=0x12f8 in Object.wait() [0x0345e000]\r\n" + 
			"   java.lang.Thread.State: TIMED_WAITING (on object monitor)\r\n" + 
			"	at java.lang.Object.wait(Native Method)\r\n" + 
			"	- waiting on <0x22ea34d8> (a com.sun.jmx.remote.internal.ArrayNotificationBuffer)\r\n" + 
			"	at com.sun.jmx.remote.internal.ArrayNotificationBuffer.fetchNotifications(ArrayNotificationBuffer.java:417)\r\n" + 
			"	- locked <0x22ea34d8> (a com.sun.jmx.remote.internal.ArrayNotificationBuffer)\r\n" + 
			"	at com.sun.jmx.remote.internal.ArrayNotificationBuffer$ShareBuffer.fetchNotifications(ArrayNotificationBuffer.java:209)\r\n" + 
			"	at com.sun.jmx.remote.internal.ServerNotifForwarder.fetchNotifs(ServerNotifForwarder.java:258)\r\n" + 
			"	at javax.management.remote.rmi.RMIConnectionImpl$2.run(RMIConnectionImpl.java:1227)\r\n" + 
			"	at javax.management.remote.rmi.RMIConnectionImpl$2.run(RMIConnectionImpl.java:1225)\r\n" + 
			"	at javax.management.remote.rmi.RMIConnectionImpl.fetchNotifications(RMIConnectionImpl.java:1231)\r\n" + 
			"	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\r\n" + 
			"	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)\r\n" + 
			"	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\r\n" + 
			"	at java.lang.reflect.Method.invoke(Method.java:597)\r\n" + 
			"	at sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:305)\r\n" + 
			"	at sun.rmi.transport.Transport$1.run(Transport.java:159)\r\n" + 
			"	at java.security.AccessController.doPrivileged(Native Method)\r\n" + 
			"	at sun.rmi.transport.Transport.serviceCall(Transport.java:155)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:535)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:790)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:649)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor$Worker.runTask(ThreadPoolExecutor.java:886)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:908)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:619)\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- <0x2299e6e0> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)\r\n" + 
			"\r\n" + 
			"\"JMX server connection timeout 15\" daemon prio=6 tid=0x03175400 nid=0xe90 in Object.wait() [0x0340f000]\r\n" + 
			"   java.lang.Thread.State: TIMED_WAITING (on object monitor)\r\n" + 
			"	at java.lang.Object.wait(Native Method)\r\n" + 
			"	- waiting on <0x22e60108> (a [I)\r\n" + 
			"	at com.sun.jmx.remote.internal.ServerCommunicatorAdmin$Timeout.run(ServerCommunicatorAdmin.java:150)\r\n" + 
			"	- locked <0x22e60108> (a [I)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:619)\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"\r\n" + 
			"\"RMI Scheduler(0)\" daemon prio=6 tid=0x03173400 nid=0x118c waiting on condition [0x033bf000]\r\n" + 
			"   java.lang.Thread.State: TIMED_WAITING (parking)\r\n" + 
			"	at sun.misc.Unsafe.park(Native Method)\r\n" + 
			"	- parking to wait for  <0x22e60198> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)\r\n" + 
			"	at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:198)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:1963)\r\n" + 
			"	at java.util.concurrent.DelayQueue.take(DelayQueue.java:164)\r\n" + 
			"	at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:583)\r\n" + 
			"	at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:576)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:947)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:907)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:619)\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"\r\n" + 
			"\"RMI TCP Connection(1)-10.1.10.26\" daemon prio=6 tid=0x03172400 nid=0x103c runnable [0x0336f000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"	at java.net.SocketInputStream.socketRead0(Native Method)\r\n" + 
			"	at java.net.SocketInputStream.read(SocketInputStream.java:129)\r\n" + 
			"	at java.io.BufferedInputStream.fill(BufferedInputStream.java:218)\r\n" + 
			"	at java.io.BufferedInputStream.read(BufferedInputStream.java:237)\r\n" + 
			"	- locked <0x22e60968> (a java.io.BufferedInputStream)\r\n" + 
			"	at java.io.FilterInputStream.read(FilterInputStream.java:66)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:517)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:790)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:649)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor$Worker.runTask(ThreadPoolExecutor.java:886)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:908)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:619)\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- <0x22e6e538> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)\r\n" + 
			"\r\n" + 
			"\"RMI TCP Accept-0\" daemon prio=6 tid=0x030e3c00 nid=0x270 runnable [0x0331f000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"	at java.net.PlainSocketImpl.socketAccept(Native Method)\r\n" + 
			"	at java.net.PlainSocketImpl.accept(PlainSocketImpl.java:390)\r\n" + 
			"	- locked <0x22e60b60> (a java.net.SocksSocketImpl)\r\n" + 
			"	at java.net.ServerSocket.implAccept(ServerSocket.java:453)\r\n" + 
			"	at java.net.ServerSocket.accept(ServerSocket.java:421)\r\n" + 
			"	at sun.management.jmxremote.LocalRMIServerSocketFactory$1.accept(LocalRMIServerSocketFactory.java:34)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport$AcceptLoop.executeAcceptLoop(TCPTransport.java:369)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport$AcceptLoop.run(TCPTransport.java:341)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:619)\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"\r\n" + 
			"\"DestroyJavaVM\" prio=6 tid=0x003a7800 nid=0x1588 waiting on condition [0x00000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"\"Descartes\" prio=6 tid=0x02b1c800 nid=0xc4 waiting for monitor entry [0x02e9f000]\r\n" + 
			"   java.lang.Thread.State: BLOCKED (on object monitor)\r\n" + 
			"	at lockvis.DeadlockGenerator$Philosopher.eat(DeadlockGenerator.java:32)\r\n" + 
			"	- waiting to lock <0x22e60d78> (a lockvis.DeadlockGenerator$Chopstick)\r\n" + 
			"	- locked <0x22e60d70> (a lockvis.DeadlockGenerator$Chopstick)\r\n" + 
			"	at lockvis.DeadlockGenerator$1.run(DeadlockGenerator.java:46)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:619)\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"\r\n" + 
			"\"Low Memory Detector\" daemon prio=6 tid=0x02afec00 nid=0x1730 runnable [0x00000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"\r\n" + 
			"\"CompilerThread0\" daemon prio=10 tid=0x02af8400 nid=0x164c waiting on condition [0x00000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"\r\n" + 
			"\"Attach Listener\" daemon prio=10 tid=0x02af6c00 nid=0x1574 runnable [0x00000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"\r\n" + 
			"\"Signal Dispatcher\" daemon prio=10 tid=0x02af5800 nid=0x858 runnable [0x00000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"\r\n" + 
			"\"Finalizer\" daemon prio=8 tid=0x02af1000 nid=0xbc4 in Object.wait() [0x02cbf000]\r\n" + 
			"   java.lang.Thread.State: WAITING (on object monitor)\r\n" + 
			"	at java.lang.Object.wait(Native Method)\r\n" + 
			"	- waiting on <0x22e61030> (a java.lang.ref.ReferenceQueue$Lock)\r\n" + 
			"	at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:118)\r\n" + 
			"	- locked <0x22e61030> (a java.lang.ref.ReferenceQueue$Lock)\r\n" + 
			"	at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:134)\r\n" + 
			"	at java.lang.ref.Finalizer$FinalizerThread.run(Finalizer.java:159)\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"\r\n" + 
			"\"Reference Handler\" daemon prio=10 tid=0x02aec400 nid=0x144c in Object.wait() [0x02c6f000]\r\n" + 
			"   java.lang.Thread.State: WAITING (on object monitor)\r\n" + 
			"	at java.lang.Object.wait(Native Method)\r\n" + 
			"	- waiting on <0x22e60480> (a java.lang.ref.Reference$Lock)\r\n" + 
			"	at java.lang.Object.wait(Object.java:485)\r\n" + 
			"	at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:116)\r\n" + 
			"	- locked <0x22e60480> (a java.lang.ref.Reference$Lock)\r\n" + 
			"\r\n" + 
			"   Locked ownable synchronizers:\r\n" + 
			"	- None\r\n" + 
			"\r\n" + 
			"\"VM Thread\" prio=10 tid=0x02aeac00 nid=0xebc runnable \r\n" + 
			"\r\n" + 
			"\"VM Periodic Task Thread\" prio=10 tid=0x02b00c00 nid=0x10f8 waiting on condition \r\n" + 
			"\r\n" + 
			"JNI global references: 824\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"Found one Java-level deadlock:\r\n" + 
			"=============================\r\n" + 
			"\"Popper\":\r\n" + 
			"  waiting to lock monitor 0x02aef2cc (object 0x22e60d70, a lockvis.DeadlockGenerator$Chopstick),\r\n" + 
			"  which is held by \"Descartes\"\r\n" + 
			"\"Descartes\":\r\n" + 
			"  waiting to lock monitor 0x02aeff64 (object 0x22e60d78, a lockvis.DeadlockGenerator$Chopstick),\r\n" + 
			"  which is held by \"Popper\"\r\n" + 
			"\r\n" + 
			"Java stack information for the threads listed above:\r\n" + 
			"===================================================\r\n" + 
			"\"Popper\":\r\n" + 
			"	at lockvis.DeadlockGenerator$Philosopher.eat(DeadlockGenerator.java:27)\r\n" + 
			"	- waiting to lock <0x22e60d70> (a lockvis.DeadlockGenerator$Chopstick)\r\n" + 
			"	- locked <0x22e60d78> (a lockvis.DeadlockGenerator$Chopstick)\r\n" + 
			"	at lockvis.DeadlockGenerator$2.run(DeadlockGenerator.java:51)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:619)\r\n" + 
			"\"Descartes\":\r\n" + 
			"	at lockvis.DeadlockGenerator$Philosopher.eat(DeadlockGenerator.java:32)\r\n" + 
			"	- waiting to lock <0x22e60d78> (a lockvis.DeadlockGenerator$Chopstick)\r\n" + 
			"	- locked <0x22e60d70> (a lockvis.DeadlockGenerator$Chopstick)\r\n" + 
			"	at lockvis.DeadlockGenerator$1.run(DeadlockGenerator.java:46)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:619)\r\n" + 
			"\r\n" + 
			"Found 1 deadlock."
			+ "";
	
	private static String missionControlDump = "2015-02-13 15:50:32\r\n" + 
			"Full thread dump Java HotSpot(TM) 64-Bit Server VM (25.25-b02 mixed mode):\r\n" + 
			"\r\n" + 
			"\"RMI TCP Connection(idle)\" #22 daemon prio=5 os_prio=0 tid=0x000000001d8a7800 nid=0x48c0 waiting on condition [0x000000001f1af000]\r\n" + 
			"   java.lang.Thread.State: TIMED_WAITING (parking)\r\n" + 
			"	at sun.misc.Unsafe.park(Native Method)\r\n" + 
			"	- parking to wait for  <0x0000000783620818> (a java.util.concurrent.SynchronousQueue$TransferStack)\r\n" + 
			"	at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)\r\n" + 
			"	at java.util.concurrent.SynchronousQueue$TransferStack.awaitFulfill(SynchronousQueue.java:460)\r\n" + 
			"	at java.util.concurrent.SynchronousQueue$TransferStack.transfer(SynchronousQueue.java:362)\r\n" + 
			"	at java.util.concurrent.SynchronousQueue.poll(SynchronousQueue.java:941)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1066)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1127)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\r\n" + 
			"\"JMX server connection timeout 20\" #20 daemon prio=5 os_prio=0 tid=0x000000001d81e800 nid=0x5a5c in Object.wait() [0x000000001e43f000]\r\n" + 
			"   java.lang.Thread.State: TIMED_WAITING (on object monitor)\r\n" + 
			"	at java.lang.Object.wait(Native Method)\r\n" + 
			"	- waiting on <0x00000007836006b8> (a [I)\r\n" + 
			"	at com.sun.jmx.remote.internal.ServerCommunicatorAdmin$Timeout.run(ServerCommunicatorAdmin.java:168)\r\n" + 
			"	- locked <0x00000007836006b8> (a [I)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\r\n" + 
			"\"RMI Scheduler(0)\" #19 daemon prio=5 os_prio=0 tid=0x000000001d819000 nid=0x2a4c waiting on condition [0x000000001cccf000]\r\n" + 
			"   java.lang.Thread.State: TIMED_WAITING (parking)\r\n" + 
			"	at sun.misc.Unsafe.park(Native Method)\r\n" + 
			"	- parking to wait for  <0x0000000783608790> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)\r\n" + 
			"	at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:215)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2078)\r\n" + 
			"	at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:1093)\r\n" + 
			"	at java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ScheduledThreadPoolExecutor.java:809)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1067)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1127)\r\n" + 
			"	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\r\n" + 
			"\"RMI TCP Accept-0\" #17 daemon prio=5 os_prio=0 tid=0x000000001b2fd800 nid=0x12ec runnable [0x000000001e20f000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"	at java.net.DualStackPlainSocketImpl.accept0(Native Method)\r\n" + 
			"	at java.net.DualStackPlainSocketImpl.socketAccept(DualStackPlainSocketImpl.java:131)\r\n" + 
			"	at java.net.AbstractPlainSocketImpl.accept(AbstractPlainSocketImpl.java:404)\r\n" + 
			"	at java.net.PlainSocketImpl.accept(PlainSocketImpl.java:199)\r\n" + 
			"	- locked <0x00000007836282a8> (a java.net.SocksSocketImpl)\r\n" + 
			"	at java.net.ServerSocket.implAccept(ServerSocket.java:545)\r\n" + 
			"	at java.net.ServerSocket.accept(ServerSocket.java:513)\r\n" + 
			"	at sun.management.jmxremote.LocalRMIServerSocketFactory$1.accept(LocalRMIServerSocketFactory.java:52)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport$AcceptLoop.executeAcceptLoop(TCPTransport.java:389)\r\n" + 
			"	at sun.rmi.transport.tcp.TCPTransport$AcceptLoop.run(TCPTransport.java:361)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\r\n" + 
			"\"DestroyJavaVM\" #15 prio=5 os_prio=0 tid=0x000000000205f000 nid=0x50f8 waiting on condition [0x0000000000000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"\"Ge Hong\" #14 prio=5 os_prio=0 tid=0x000000001b078000 nid=0x3d8c waiting on condition [0x000000001d1cf000]\r\n" + 
			"   java.lang.Thread.State: WAITING (parking)\r\n" + 
			"	at sun.misc.Unsafe.park(Native Method)\r\n" + 
			"	- parking to wait for  <0x00000007836302f0> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)\r\n" + 
			"	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:52)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:43)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.run(DiningPhilosophers.java:70)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\r\n" + 
			"\"Zhang Daoling\" #13 prio=5 os_prio=0 tid=0x000000001b077000 nid=0x2c9c waiting on condition [0x000000001d2ef000]\r\n" + 
			"   java.lang.Thread.State: WAITING (parking)\r\n" + 
			"	at sun.misc.Unsafe.park(Native Method)\r\n" + 
			"	- parking to wait for  <0x0000000783630508> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)\r\n" + 
			"	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:52)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:43)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.run(DiningPhilosophers.java:70)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\r\n" + 
			"\"Popper\" #12 prio=5 os_prio=0 tid=0x000000001b076800 nid=0x3f50 waiting on condition [0x000000001d07f000]\r\n" + 
			"   java.lang.Thread.State: WAITING (parking)\r\n" + 
			"	at sun.misc.Unsafe.park(Native Method)\r\n" + 
			"	- parking to wait for  <0x0000000783630710> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)\r\n" + 
			"	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:52)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:43)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.run(DiningPhilosophers.java:70)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\r\n" + 
			"\"Descartes\" #11 prio=5 os_prio=0 tid=0x000000001b074800 nid=0x5384 waiting on condition [0x000000001cf4e000]\r\n" + 
			"   java.lang.Thread.State: WAITING (parking)\r\n" + 
			"	at sun.misc.Unsafe.park(Native Method)\r\n" + 
			"	- parking to wait for  <0x0000000783630918> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)\r\n" + 
			"	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:52)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:43)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.run(DiningPhilosophers.java:70)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\r\n" + 
			"\"Service Thread\" #10 daemon prio=9 os_prio=0 tid=0x000000001b054000 nid=0x3ad4 runnable [0x0000000000000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"\"C1 CompilerThread3\" #9 daemon prio=9 os_prio=2 tid=0x000000001aff5800 nid=0x1c28 waiting on condition [0x0000000000000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"\"C2 CompilerThread2\" #8 daemon prio=9 os_prio=2 tid=0x000000001afd4800 nid=0x5784 waiting on condition [0x0000000000000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"\"C2 CompilerThread1\" #7 daemon prio=9 os_prio=2 tid=0x000000001afc1000 nid=0x5e28 waiting on condition [0x0000000000000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"\"C2 CompilerThread0\" #6 daemon prio=9 os_prio=2 tid=0x000000001afb7800 nid=0x4db4 waiting on condition [0x0000000000000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"\"Attach Listener\" #5 daemon prio=5 os_prio=2 tid=0x000000001afb5000 nid=0x5350 runnable [0x0000000000000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"\"Signal Dispatcher\" #4 daemon prio=9 os_prio=2 tid=0x000000001afb4000 nid=0x2248 runnable [0x0000000000000000]\r\n" + 
			"   java.lang.Thread.State: RUNNABLE\r\n" + 
			"\r\n" + 
			"\"Finalizer\" #3 daemon prio=8 os_prio=1 tid=0x0000000019efc800 nid=0x4d10 in Object.wait() [0x000000001c37f000]\r\n" + 
			"   java.lang.Thread.State: WAITING (on object monitor)\r\n" + 
			"	at java.lang.Object.wait(Native Method)\r\n" + 
			"	- waiting on <0x0000000783628b58> (a java.lang.ref.ReferenceQueue$Lock)\r\n" + 
			"	at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:142)\r\n" + 
			"	- locked <0x0000000783628b58> (a java.lang.ref.ReferenceQueue$Lock)\r\n" + 
			"	at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:158)\r\n" + 
			"	at java.lang.ref.Finalizer$FinalizerThread.run(Finalizer.java:209)\r\n" + 
			"\r\n" + 
			"\"Reference Handler\" #2 daemon prio=10 os_prio=2 tid=0x0000000019ef4000 nid=0x13fc in Object.wait() [0x000000001c25e000]\r\n" + 
			"   java.lang.Thread.State: WAITING (on object monitor)\r\n" + 
			"	at java.lang.Object.wait(Native Method)\r\n" + 
			"	- waiting on <0x0000000783628d10> (a java.lang.ref.Reference$Lock)\r\n" + 
			"	at java.lang.Object.wait(Object.java:502)\r\n" + 
			"	at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:157)\r\n" + 
			"	- locked <0x0000000783628d10> (a java.lang.ref.Reference$Lock)\r\n" + 
			"\r\n" + 
			"\"VM Thread\" os_prio=2 tid=0x000000001af61800 nid=0x5f34 runnable \r\n" + 
			"\r\n" + 
			"\"GC task thread#0 (ParallelGC)\" os_prio=0 tid=0x00000000020dd800 nid=0x1ae4 runnable \r\n" + 
			"\r\n" + 
			"\"GC task thread#1 (ParallelGC)\" os_prio=0 tid=0x00000000020df000 nid=0x569c runnable \r\n" + 
			"\r\n" + 
			"\"GC task thread#2 (ParallelGC)\" os_prio=0 tid=0x00000000020e0800 nid=0x1348 runnable \r\n" + 
			"\r\n" + 
			"\"GC task thread#3 (ParallelGC)\" os_prio=0 tid=0x00000000020e2000 nid=0x5a94 runnable \r\n" + 
			"\r\n" + 
			"\"GC task thread#4 (ParallelGC)\" os_prio=0 tid=0x00000000020e5800 nid=0x5aac runnable \r\n" + 
			"\r\n" + 
			"\"GC task thread#5 (ParallelGC)\" os_prio=0 tid=0x00000000020e6800 nid=0x840 runnable \r\n" + 
			"\r\n" + 
			"\"GC task thread#6 (ParallelGC)\" os_prio=0 tid=0x00000000020e7800 nid=0x50b8 runnable \r\n" + 
			"\r\n" + 
			"\"GC task thread#7 (ParallelGC)\" os_prio=0 tid=0x00000000020e9000 nid=0x14c8 runnable \r\n" + 
			"\r\n" + 
			"\"VM Periodic Task Thread\" os_prio=2 tid=0x000000001b069800 nid=0x274c waiting on condition \r\n" + 
			"\r\n" + 
			"JNI global references: 56\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"Found one Java-level deadlock:\r\n" + 
			"=============================\r\n" + 
			"\"Ge Hong\":\r\n" + 
			"  waiting for ownable synchronizer 0x00000007836302f0, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),\r\n" + 
			"  which is held by \"Zhang Daoling\"\r\n" + 
			"\"Zhang Daoling\":\r\n" + 
			"  waiting for ownable synchronizer 0x0000000783630508, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),\r\n" + 
			"  which is held by \"Popper\"\r\n" + 
			"\"Popper\":\r\n" + 
			"  waiting for ownable synchronizer 0x0000000783630710, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),\r\n" + 
			"  which is held by \"Descartes\"\r\n" + 
			"\"Descartes\":\r\n" + 
			"  waiting for ownable synchronizer 0x0000000783630918, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),\r\n" + 
			"  which is held by \"Ge Hong\"\r\n" + 
			"\r\n" + 
			"Java stack information for the threads listed above:\r\n" + 
			"===================================================\r\n" + 
			"\"Ge Hong\":\r\n" + 
			"	at sun.misc.Unsafe.park(Native Method)\r\n" + 
			"	- parking to wait for  <0x00000007836302f0> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)\r\n" + 
			"	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:52)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:43)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.run(DiningPhilosophers.java:70)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\"Zhang Daoling\":\r\n" + 
			"	at sun.misc.Unsafe.park(Native Method)\r\n" + 
			"	- parking to wait for  <0x0000000783630508> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)\r\n" + 
			"	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:52)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:43)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.run(DiningPhilosophers.java:70)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\"Popper\":\r\n" + 
			"	at sun.misc.Unsafe.park(Native Method)\r\n" + 
			"	- parking to wait for  <0x0000000783630710> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)\r\n" + 
			"	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:52)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:43)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.run(DiningPhilosophers.java:70)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\"Descartes\":\r\n" + 
			"	at sun.misc.Unsafe.park(Native Method)\r\n" + 
			"	- parking to wait for  <0x0000000783630918> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)\r\n" + 
			"	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\r\n" + 
			"	at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\r\n" + 
			"	at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:52)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.eat(DiningPhilosophers.java:43)\r\n" + 
			"	at lockvis.tools.DiningPhilosophers$Philosopher.run(DiningPhilosophers.java:70)\r\n" + 
			"	at java.lang.Thread.run(Thread.java:745)\r\n" + 
			"\r\n" + 
			"Found 1 deadlock.\r\n" + 
			"\r\n" + 
			"";
	
}
