package lockvis.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * A collection of thread informations that are grouped together for a reason. Maybe a VM dump or an entanglement of threads.
 */
public class ThreadInfoSet implements ObjectProperties {
    private String name = "";
    private String deadLockInfo = "";
    protected List<ThreadInfo> threadInfos = new ArrayList<ThreadInfo>(2);
    
    public void addThreadInfo(ThreadInfo threadInfo) {
        threadInfos.add(threadInfo);
    }
    
    public boolean contains(ThreadInfo st)  {
        return threadInfos.contains(st);
    }
    
    public List<ThreadInfo> getThreadInfos() {
        return threadInfos;
    }

    public int size() {
        return threadInfos.size();
    }
    
    public String getDumpName() {
        return name;
    }

    public void setDumpName(String dumpName) {
        this.name = dumpName;
    }
    
    public String getDeadLockInfo() {
        return deadLockInfo;
    }

    public void setDeadLockInfo(String deadLockInfo) {
        this.deadLockInfo = deadLockInfo;
    }
    
    public boolean isInDeadlock()   {
        return !getDeadLockInfo().trim().equals("");
    }
    
    public String toString()   {
        return getDumpName();
    }

    @Override
    public Map<String, String> getProperties() {
        
        Map<String, String> prop = new HashMap<String, String>();
        prop.put("Dump Name", getDumpName());
        prop.put("Deadlock Info", getDeadLockInfo());
        prop.put("Threads", String.valueOf(getThreadInfos().size()));
        
        return prop;
    }
}
