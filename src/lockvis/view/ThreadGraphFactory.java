package lockvis.view;

import java.util.List;

import lockvis.model.Mutex;
import lockvis.model.MutexAction;
import lockvis.model.ThreadInfo;
import lockvis.model.ThreadLocation;
import lockvis.model.Vertex;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class ThreadGraphFactory {

    public Graph<Vertex, MutexAction> createGraphFromDump(List<ThreadInfo> stackTraces) {
        Graph<Vertex, MutexAction> graph = new DirectedSparseMultigraph<Vertex, MutexAction>();

        for (ThreadInfo dump : stackTraces) {
            graph.addVertex(dump);

            // now add all the monitors it holds and link an edge to it.
            List<ThreadLocation> stack = dump.getStack();
            for (ThreadLocation threadLocation : stack) {
                List<MutexAction> mutexes = threadLocation.getMutexActions();
                
                for (MutexAction mutexAction : mutexes) {                    
                    if (mutexAction != null) {
                        Mutex monitor = mutexAction.getMutex();
                        if (!graph.containsVertex(monitor)) {
                            graph.addVertex(monitor);
                        }
                        if (!graph.containsEdge(mutexAction)) {
                            graph.addEdge(mutexAction, dump, monitor, EdgeType.DIRECTED);
                        }
                    }                    
                }
            }
        }
        return graph;
    }
}
