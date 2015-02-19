package lockvis.model;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ThreadInfo implements Vertex {
    private boolean inDeadLock = false;
    private String threadName;
    private String threadId;
    private java.lang.Thread.State state;
    private List<ThreadLocation> stack = new ArrayList<ThreadLocation>(5);
    private ThreadInfoSet containingEntanglement;
    private List<MutexAction> actions = new ArrayList<MutexAction>();

    public ThreadInfo(String threadName) {
        this.threadName = threadName;
        String id = threadName;

        // create a minimal ID. Remove anything outside the quotes.
        int firstQuote = threadName.indexOf("\"");
        if (firstQuote!=-1)	{
        	int lastQuote = threadName.lastIndexOf("\"");
        	if (lastQuote!=-1)	{
        		id = threadName.substring(firstQuote + 1, lastQuote);
        	}
        }
		this.threadId = id;
    }

    public List<MutexAction> getMutexActions() {
        return actions;
    }

    public void addMutexAction(MutexAction ma) {
        actions.add(ma);
    }
    
    public void addLocation(ThreadLocation tl) {
        stack.add(tl);
    }

    public String getThreadName() {
        return threadName;
    }

    public java.lang.Thread.State getState() {
        return state;
    }

    public void setState(java.lang.Thread.State state) {
        this.state = state;
    }

    public List<ThreadLocation> getStack() {
        return stack;
    }

    @Override
    public String toString() {
        return threadId;
    }

    public String toFullString() {
        StringBuilder sb = new StringBuilder();
        getStackHeader(sb).append("\r\n");
        for (ThreadLocation loc : stack) {
            sb.append(loc.toFullString()).append("\r\n");
        }

        return sb.toString();
    }

    public StringBuilder getStackHeader(StringBuilder sb) {
        return sb.append(threadName).append(" ").append(state == null ? "" : state);
    }

    @Override
    public Shape getShape() {
        return new Rectangle2D.Float(-10.0f, -10.0f, 20.0f, 20.0f);
    }

    @Override
    public Color getColor() {
        if (getState() == null) {
            return Color.BLUE;
        }
        if (getState().equals(State.RUNNABLE)) {
            return Color.GREEN;
        }
        if (getState().equals(State.WAITING) || getState().equals(State.TIMED_WAITING)) {
            return Color.ORANGE;
        }
        if (getState().equals(State.BLOCKED)) {
            return Color.RED;
        }
        if (getState().equals(State.TERMINATED)) {
            return Color.ORANGE;
        }
        return Color.BLUE;
    }

    @Override
    public String getToolTip() {
        return getThreadName() + " " + getState();
    }

    public void setInDeadLock(boolean inDeadLock) {
        this.inDeadLock = inDeadLock;
    }

    public boolean isInDeadLock() {
        return inDeadLock;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getThreadId() {
        return threadId;
    }

    @Override
    public String getName() {
        return threadId;
    }

    public void setContainingEntanglement(ThreadInfoSet containingEntanglement) {
        this.containingEntanglement = containingEntanglement;
    }

    public ThreadInfoSet getContainingEntanglement() {
        return containingEntanglement;
    }
    
    public int getStackLocationIndex(ThreadLocation tl)  {
        return stack.indexOf(tl);
    }
    
    public ThreadLocation getByIndex(int index)	{
    	return stack.get(index);
    }

    @Override
    public Map<String, String> getProperties() {
        HashMap<String, String> prop = new HashMap<String, String>();
        prop.put("Name", getName());
        prop.put("Deadlock", String.valueOf(inDeadLock));
        prop.put("ThreadId", threadId);
        prop.put("State", state==null ? "Unknown" : state.name());
        prop.put("StackSize", String.valueOf(stack.size()));
        
        return prop;
    }
    
    
    
}