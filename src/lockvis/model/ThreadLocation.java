package lockvis.model;

import java.util.ArrayList;
import java.util.List;


public class ThreadLocation {
    private final String location;
    private List<MutexAction> actions = new ArrayList<MutexAction>();
    private final ThreadInfo owner;

    ThreadLocation(String location, ThreadInfo owner) {
        this.location = location;
        this.owner = owner;
    }

    public String getLocation() {
        return location;
    }

    public List<MutexAction> getMutexActions() {
        return actions;
    }

    public void addMutexAction(MutexAction ma) {
        actions.add(ma);
    }

    @Override
    public String toString() {
        return location;
    }

    public String toFullString() {
        String muS = "";
        if (actions.size()>0)   {
            for(MutexAction ma : actions)    {
                muS += "\r\n    ";
                muS += ma.getState() + " on " + ma.getMutex().getName() + " ";
            }
        }
        
    	return location + muS;
    }

    public ThreadInfo getOwner() {
        return owner;
    }
    
    public int getStackLocationIndex()  {
        return owner.getStackLocationIndex(this);
    }
}