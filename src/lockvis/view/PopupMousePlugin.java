package lockvis.view;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import lockvis.model.Mutex;
import lockvis.model.MutexAction;
import lockvis.model.ThreadInfo;
import lockvis.model.ThreadInfoSet;
import lockvis.model.Vertex;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;

public class PopupMousePlugin extends AbstractPopupGraphMousePlugin {

    private static Logger LOGGER = Logger.getLogger(PopupMousePlugin.class.getName());
    protected JPopupMenu popup = new JPopupMenu();

    private List<ThreadInfoSetSelectionListener> threadSetSelectionListeners = new ArrayList<ThreadInfoSetSelectionListener>();    
    
    public void addThreadInfoSetSelectionListener(ThreadInfoSetSelectionListener l)	{
    	threadSetSelectionListeners.add(l);
    }
    
    private void notifyThreadInfoSetSelectionListeners(ThreadInfoSet tis)	{
    	for (ThreadInfoSetSelectionListener l : threadSetSelectionListeners) {
			l.selected(tis);
		}
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handlePopup(MouseEvent e) {
        final VisualizationViewer<Vertex, MutexAction> vv = (VisualizationViewer<Vertex, MutexAction>) e.getSource();
        final Layout<Vertex, MutexAction> layout = vv.getGraphLayout();
        final Point2D ivp = e.getPoint();
        popup.removeAll();
        GraphElementAccessor<Vertex, MutexAction> pickSupport = vv.getPickSupport();
        if (pickSupport != null) {

            final Vertex vertex = pickSupport.getVertex(layout, ivp.getX(), ivp.getY());
            final MutexAction edge = pickSupport.getEdge(layout, ivp.getX(), ivp.getY());

            if (vertex != null) {
                if (vertex instanceof ThreadInfo) {
                    popup.add(new AbstractAction("Show sub-graph in tab") {
                        private static final long serialVersionUID = 1L;

                        public void actionPerformed(ActionEvent e) {
                            ThreadInfo st = (ThreadInfo) vertex;
                            ThreadInfoSet containingEntanglement = st.getContainingEntanglement();
                            notifyThreadInfoSetSelectionListeners(containingEntanglement);
                        }
                    });
                    popup.add(new AbstractAction("Show Stack") {
                        private static final long serialVersionUID = 1L;

                        public void actionPerformed(ActionEvent e) {
                            StackTraceViewer stv = new StackTraceViewer((ThreadInfo) vertex, null);
                            stv.setVisible(true);
                        }
                    });
                }
                if (vertex instanceof Mutex) {
                    popup.add(new AbstractAction("Show sub-graph in tab") {
                        private static final long serialVersionUID = 1L;

                        public void actionPerformed(ActionEvent e) {
                            Mutex mu = (Mutex) vertex;
                            ThreadInfoSet containingEntanglement = mu.getContainingEntanglement();
                            notifyThreadInfoSetSelectionListeners(containingEntanglement);
                        }
                    });
                }
            } else if (edge != null) {
                popup.add(new AbstractAction("Show line in stack") {
                    private static final long serialVersionUID = 1L;

                    public void actionPerformed(ActionEvent e) {
                        LOGGER.info("Line in stack is " + edge.toString());
                        StackTraceViewer stv = new StackTraceViewer(edge.getActor(), edge.getPosition());
                        stv.setVisible(true);
                    }
                });
            }

            if (popup.getComponentCount() > 0) {
                popup.show(vv, e.getX(), e.getY());
            }
        }
    }
}
