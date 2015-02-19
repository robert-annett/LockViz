package lockvis.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Given a set of thread infos this will pull out a set of simple graphs from the multigraph
 */
class SimpleGraphExtractor {

    List<ThreadInfoSet> extractSimpleGraphs(List<ThreadInfo> threads)    {
        List<ThreadInfoSet> entanglements = new ArrayList<ThreadInfoSet>();
        
        List<ThreadInfo> processed = new ArrayList<ThreadInfo>();
        
        
        //loop through all the threads and place them into buckets and into processed as we touch them. Once processed we can ignore.
        for (ThreadInfo toCheck : threads) {
            
            //don't reprocess files we have already touched. We are assuming
            //that every node is bi-directional so if its touched then it and all
            //nodes it is directly or indirectly attached to is correctly assigned.
            if (!processed.contains(toCheck))    {
                ThreadInfoSet entanglement = extractEntanglement(toCheck);
                
                //record all assigned
                entanglements.add(entanglement);
                processed.addAll(entanglement.getThreadInfos());
            }           
        }
                
        return entanglements;
    }

    private ThreadInfoSet extractEntanglement(ThreadInfo toCheck) {
        ThreadInfoSet entanglement = new ThreadInfoSet();
        
        //loop through all it's connections and add to the entanglement
        addAllConnections(toCheck, entanglement);
        return entanglement;
    }

    //Iteratively add all the sub connections
    private void addAllConnections(ThreadInfo toCheck, ThreadInfoSet entanglement) {
        entanglement.addThreadInfo(toCheck);
        toCheck.setContainingEntanglement(entanglement);
        entanglement.setDumpName(entanglement.getDumpName() + (entanglement.getDumpName().equals("") ? "" : "-") + toCheck.getName() );
        if (toCheck.isInDeadLock()) {
            entanglement.setDeadLockInfo("In Deadlock");
        }
        
        List<MutexAction> mutexActions = toCheck.getMutexActions();
        for (MutexAction mutexAction : mutexActions) {
                Mutex mutex = mutexAction.getMutex();                
                List<MutexAction> referencedBy = mutex.getReferencedBy();
                for (MutexAction otherMa : referencedBy) {
                    ThreadInfo owner = otherMa.getActor();
                    if (!entanglement.contains(owner))  {
                        addAllConnections(owner, entanglement);
                    }
                }
        }
        
    }
    
}
