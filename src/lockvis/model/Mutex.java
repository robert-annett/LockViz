package lockvis.model;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mutex implements Vertex {

    public enum LockType    {ObjectMonitor, OwnableSynchronizer};
    
    private boolean inDeadLock = false;
    private String id;                  //The ID given to the mutex by the JVM
    private String lockObject;          //The actual object providing the lock/monitor
    private LockType lockType;
    
    private List<MutexAction> referencedBy = new ArrayList<MutexAction>(2);

    protected Mutex(String id, String lockObject, LockType locktype) {
        this.lockObject = lockObject;
        this.id = id;
        this.lockType = locktype;
    }


    @Override
    public String toString() {
        return id;
    }

    public String toFullString() {
        return id + " " + lockObject;
    }

    @Override
    public String getToolTip() {
        return toFullString();
    }

    public void setInDeadLock(boolean inDeadLock) {
        this.inDeadLock = inDeadLock;
    }

    public boolean isInDeadLock() {
        return inDeadLock;
    }

    @Override
    public String getName() {
        return id;
    }
    
    public void addReference(MutexAction ma)	{
    	referencedBy.add(ma);
    }

    public List<MutexAction> getReferencedBy() {
        return referencedBy;
    }   

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> prop = new HashMap<String, String>();
        prop.put("Id", id);
        prop.put("Deadlock", String.valueOf(inDeadLock));
        prop.put("Lock Object", lockObject);
        prop.put("References", String.valueOf(referencedBy.size()));
        prop.put("Mutex Type", lockType.toString());
        
        return prop;
    }

    @Override
    public ThreadInfoSet getContainingEntanglement() {
        ThreadInfo st = this.getReferencedBy().get(0).getActor();
        return st.getContainingEntanglement();
    }

    public void setLockType(LockType lockType) {
        this.lockType = lockType;
    }

    public LockType getLockType() {
        return lockType;
    }

    public Color getColor() {
        if (lockType.equals(LockType.ObjectMonitor))    {
            return Color.BLUE;
        }
        else    {
            return Color.CYAN;
        }
    }

    @Override
    public Shape getShape() {
        return new Ellipse2D.Float(-10.0f, -10.0f, 20.0f, 20.0f);
    }
}