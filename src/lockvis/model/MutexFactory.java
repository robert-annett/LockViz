package lockvis.model;

import java.util.HashMap;
import java.util.Map;

import lockvis.model.Mutex.LockType;

public class MutexFactory {
    
    protected Map<String, Mutex> allMutexes = new HashMap<String, Mutex>();
    
    public Mutex find(String id) {
        Mutex monitor = allMutexes.get(id);
        return monitor;
    }
    
    /**
     * We need to know whether to create an ObjectMonitor or an OwnableSynchronizer. We could examine the preceding stack, object type 
     * or the message/action. I've decided on the action as this is available on the current processing line and is correct even if
     * the user is using an OwnableSynchronizer object as an ObjectMonitor - which is horrible.
     */
    public Mutex getMutex(String id, String lockObject, String action) {
        Mutex mutex = find(id);
        
        if (mutex == null) {

            if (isActionObjectMonitor(action) )    {
                mutex = new Mutex(id, lockObject, LockType.ObjectMonitor);
            }
            else    {
                mutex = new Mutex(id, lockObject, LockType.OwnableSynchronizer);
            }
        
            allMutexes.put(id, mutex);
        }

        if (isActionObjectMonitor(action) && (mutex.getLockType().equals(LockType.OwnableSynchronizer)))    {
            throw new IllegalStateException("Looks like someone is using a OwnableSynchronizer as an object monitor. This is too nasty to model.");
        }

        return mutex;
    }

    private boolean isActionObjectMonitor(String action) {
        return action.equals("waiting to lock") || action.equals("locked") || action.equals("waiting on");
    }


}
