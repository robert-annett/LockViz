package lockvis.util;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JMXConnectorUtil {

	static final ThreadFactory daemonThreadFactory = new DaemonThreadFactory();

	public static JMXConnector connectWithTimeout(final JMXServiceURL url, long timeout, TimeUnit unit) throws Exception {
		final BlockingQueue<Object> mailbox = new ArrayBlockingQueue<Object>(1);
		ExecutorService executor = Executors.newSingleThreadExecutor(daemonThreadFactory);
		executor.submit(new Runnable() {
			public void run() {
				try {
					JMXConnector connector = JMXConnectorFactory.connect(url);
					if (!mailbox.offer(connector)) {
						connector.close();
					}
				} catch (Exception t) {
					mailbox.offer(t);
				}
			}
		});
		Object result;
		try {
			result = mailbox.poll(timeout, unit);
			if (result == null) {
				if (!mailbox.offer("")) {
					result = mailbox.take();
				}
			}
		} catch (InterruptedException e) {
			throw new IOException("Interrupted while trying to wait for connection", e);
		} finally {
			executor.shutdown();
		}
		if (result == null) {
			throw new SocketTimeoutException("Connect timed out: " + url);
		}
		if (result instanceof JMXConnector) {
			return (JMXConnector) result;
		}

		throw (Exception) result;
	}

	private static final class DaemonThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			Thread t = Executors.defaultThreadFactory().newThread(r);
			t.setDaemon(true);
			return t;
		}
	}
}
