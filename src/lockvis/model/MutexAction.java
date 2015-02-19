package lockvis.model;

import java.util.HashMap;
import java.util.Map;

public class MutexAction implements ObjectProperties{

    private final ThreadLocation position;
    private final ThreadInfo actor;
    private final Mutex mutex;
    private final String state;
    
    public MutexAction(ThreadInfo actor, ThreadLocation position, Mutex mutex, String state) {
        this.actor = actor;
        this.position = position;
        this.mutex = mutex;
        this.state = state;
        
        //also tell the mutex we have a handle to it
        mutex.addReference(this);
    }

    public ThreadInfo getActor() {
        return actor;
    }

    public Mutex getMutex() {
        return mutex;
    }

    public String getState() {
        return state;
    }
    
    public String toString()    {
        return state + " on " + mutex.getName() + " by " + actor.getName() + (position!=null ? position.getLocation() : "");
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> prop = new HashMap<String, String>();
        prop.put("Type", "Action on mutex");
        prop.put("State", getState());
        prop.put("Actor", actor.getName());
        prop.put("Destination", mutex.getName());        
        return prop;
    }

    public ThreadLocation getPosition() {
        return position;
    }
    
}
