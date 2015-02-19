package lockvis.model;

import java.util.Date;
import java.util.List;

/**
 * Represents a full thread dump of a java VM. This contains a multi-graph of all the threads and mutex entanglements.
 * Here we store the set of threads and their groupings into individual entanglements/simple graphs.
 *
 */
public class VMThreadDump extends ThreadInfoSet {
	private Date timestamp = null;	
	private String overrideName = null;	
	private String jniGLobalReferences = null;
	
	private List<ThreadInfoSet> simpleGraphs;
	
	
	public Date getTimestamp() {
		return timestamp;
	}

	public ThreadInfo getDump(String threadId) {
		for (ThreadInfo td : threadInfos) {
			if (td.getThreadId().equals(threadId)) {
				return td;
			}
		}
		return null;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getJniGLobalReferences() {
		return jniGLobalReferences;
	}

	public void setJniGLobalReferences(String jniGLobalReferences) {
		this.jniGLobalReferences = jniGLobalReferences;
	}

	public String toString()   {
	    return overrideName!=null ? overrideName : getDumpName();
	}
	
	public String toFullString() {
		StringBuilder sb = new StringBuilder("VM thread dump for: ").append(getDumpName()).append("\r\n");
		sb.append("At: ").append(timestamp).append("\r\n");
		for (ThreadInfo dump : threadInfos) {
			sb.append(dump.toFullString()).append("\r\n");
		}

		sb.append(jniGLobalReferences).append("\r\n").append(getDeadLockInfo()).append("\r\n");

		return sb.toString();
	}

    public void setOverrideName(String overrideName) {
        this.overrideName = overrideName;
    }

    public String getOverrideName() {
        return overrideName;
    }

    void setSimpleGraphs(List<ThreadInfoSet> simpleGraphs) {
        this.simpleGraphs = simpleGraphs;
    }

    public List<ThreadInfoSet> getSimpleGraphs() {
        return simpleGraphs;
    }
    



}