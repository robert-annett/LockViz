package lockvis.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import lockvis.model.MutexAction;
import lockvis.model.ObjectProperties;
import lockvis.model.Vertex;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.util.Relaxer;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.DefaultVisualizationModel;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationModel;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ViewScalingControl;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

@SuppressWarnings("serial")
public class ThreadGraphDisplay extends JPanel {
	
    private VisualizationViewer<Vertex, MutexAction> vv;
    private MyDefaultModalGraphMouse<Vertex, MutexAction> graphMouse;
    private GraphZoomScrollPane gzsp;
    
    public ThreadGraphDisplay(final Graph<Vertex, MutexAction> graph, ThreadInfoSetSelectionListener threadInfoSetChangeListener, ObjectPropertiesSelectionListener objectPropertiesSelectionListener) {
    	super();
        Dimension preferredSize = calcGraphSize(graph);
        
        Layout<Vertex, MutexAction> layout = new ISOMLayout<Vertex, MutexAction>(graph);

        VisualizationModel<Vertex, MutexAction> visualizationModel = new DefaultVisualizationModel<Vertex, MutexAction>(layout, preferredSize);
        vv = new VisualizationViewer<Vertex, MutexAction>(visualizationModel, preferredSize);
        vv.setBackground(Color.white);
        
        setVertexLabeler();
        setVertexShapeAndColour();
        setVertexToolTip();
                
        setEdgeLineAndColour();
        setEdgeToolTip();
        setEdgeLabeler();

        this.setLayout(new BorderLayout());
        gzsp = new GraphZoomScrollPane(vv);
        this.add(gzsp, BorderLayout.CENTER);

        graphMouse = new MyDefaultModalGraphMouse<Vertex, MutexAction>();

        vv.setGraphMouse(graphMouse);
        vv.addKeyListener(graphMouse.getModeKeyListener());

        JMenuBar menubar = new JMenuBar();
        JMenu modeMenu = graphMouse.getModeMenu();
        graphMouse.setMode(Mode.PICKING);
		menubar.add(modeMenu);
        gzsp.setCorner(menubar);
        
        //my stuff
        PopupMousePlugin popUpMousePlugin = new PopupMousePlugin();
        popUpMousePlugin.addThreadInfoSetSelectionListener(threadInfoSetChangeListener);
		graphMouse.add(popUpMousePlugin);
		
        SelectionNodeMousePlugin selectionNodePlugin = new SelectionNodeMousePlugin();
        selectionNodePlugin.addObjectPropertiesSelectionListener(objectPropertiesSelectionListener);
		graphMouse.add(selectionNodePlugin);
        
    }
    
    //The first time this is called we want to reset the layout so that it fits correctly.
	public void doLayout() {
		super.doLayout();
		if (vv.getHeight()==0)	{
			gzsp.doLayout(); //I need to force this early so the bounds are set on its components.
			resetLayout();
		}
	}

	private Dimension calcGraphSize(final Graph<Vertex, MutexAction> graph) {
		int vertexCount = graph.getVertexCount();
        int calcXSize = vertexCount*20;//40;
        int calcYSize = vertexCount*10;//35;
        
        Dimension availableGraphSize = vv!=null ? vv.getSize() : new Dimension(0,0);
        Dimension preferredSize = new Dimension(calcXSize>availableGraphSize.getWidth() ? calcXSize : (int)availableGraphSize.getWidth(), 
                calcYSize>availableGraphSize.getHeight() ? calcYSize : (int)availableGraphSize.getHeight());
		return preferredSize;
	}

    public void selectPickingMode() {
        graphMouse.setMode(Mode.PICKING);
    }

    public void selectTransformingMode() {
        graphMouse.setMode(Mode.TRANSFORMING);
    }
    
    public boolean isPickingMode()  {
        return graphMouse.getMode()==Mode.PICKING;
    }

    private void setEdgeToolTip() {
        vv.setEdgeToolTipTransformer(new Transformer<MutexAction, String>() {
            public String transform(MutexAction edge) {
                return edge.toString();
            }
        });
    }


    private void setEdgeLineAndColour() {
        PickedState<MutexAction> pes = vv.getPickedEdgeState();
        vv.getRenderContext().setEdgeDrawPaintTransformer(new LockEdgePaintTransformer(pes, Color.black, Color.cyan));
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.QuadCurve<Vertex, MutexAction>());
    }


    private void setVertexToolTip() {
        vv.setVertexToolTipTransformer(new Transformer<Vertex, String>() {
            public String transform(Vertex ver) {
                return ver.getToolTip();
            }
        });
    }


    private void setVertexShapeAndColour() {
        Transformer<Vertex, Shape> vertexShapeTransformer = new Transformer<Vertex, Shape>() {
            @Override
            public Shape transform(Vertex arg0) {
                return arg0.getShape();
            }
        };
        
        vv.getRenderContext().setVertexShapeTransformer(vertexShapeTransformer);
        
        PickedState<Vertex> ps = vv.getPickedVertexState();
        vv.getRenderContext().setVertexFillPaintTransformer(new LockVertexPaintTransformer(ps, Color.red, Color.yellow));
    }


    private void setVertexLabeler() {
        Transformer<Vertex, String> vertexStringer = new Transformer<Vertex, String>() {
            @Override
            public String transform(Vertex v) {
                return v.getName().length()<30 ? v.getName() : v.getName().substring(0, 29);    //trim the ID to something more screen friendly
            }
        };
        
        vv.getRenderContext().setVertexLabelTransformer(vertexStringer);
        vv.getRenderer().getVertexLabelRenderer().setPosition(Position.S);
    }

    private void setEdgeLabeler() {
        Transformer<MutexAction, String> edgeStringer = new Transformer<MutexAction, String>() {
            @Override
            public String transform(MutexAction e) {
                return String.valueOf(e.getActor().getStackLocationIndex(e.getPosition())); //"hello";    //trim the ID to something more screen friendly
            }
        };
        
        vv.getRenderContext().setEdgeLabelTransformer(edgeStringer);
    }
        
    public void resetLayout()	{
		Layout<Vertex, MutexAction> layout = vv.getGraphLayout();		
		layout.setSize(calcGraphSize(layout.getGraph()));
		layout.initialize();
		Relaxer relaxer = vv.getModel().getRelaxer();
		if(relaxer != null) {
			relaxer.stop();
			relaxer.prerelax();
			relaxer.relax();
		}
    }

    public void birdsEyeZoomOut() {

		centerGraph();

		ViewScalingControl scaler = new ViewScalingControl();
		scaler.scale(vv, 1/3.0f, vv.getCenter());
		
        vv.repaint();
	}

	public void birdsEyeZoomIn() {
		centerGraph();

		ViewScalingControl scaler = new ViewScalingControl();
		scaler.scale(vv, 3.0f, vv.getCenter());
		centerGraph();
        vv.repaint();
	}

	public void centerGraph() {
		GraphUtils.centerGraph(vv);
	}
    
	public VisualizationViewer<Vertex, MutexAction> getVisualizationViewer()	{
		return vv;
	}

    public static class MyDefaultModalGraphMouse<V,E> extends DefaultModalGraphMouse<V,E>   {
        public Mode getMode()   {
            return mode;
        }
    }
    
	private class SelectionNodeMousePlugin extends AbstractGraphMousePlugin implements MouseListener {
		
		List<ObjectPropertiesSelectionListener> objectPropertiesSelectionListeners = new ArrayList<ObjectPropertiesSelectionListener>(1);
		
		public void addObjectPropertiesSelectionListener(ObjectPropertiesSelectionListener l)	{
			objectPropertiesSelectionListeners.add(l);			
		}
		
		public void notifyObjectPropertiesSelectionListeners(ObjectProperties o)	{
			for (ObjectPropertiesSelectionListener l : objectPropertiesSelectionListeners) {
				l.selected(o);
			}
		}
		
		public SelectionNodeMousePlugin() {
			this(MouseEvent.BUTTON1_MASK);
		}
		
		public SelectionNodeMousePlugin(int modifiers) {
			super(modifiers);
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			showStats(e);
		}
		
		@Override
		public void mouseEntered(MouseEvent e) {
		}
		
		@Override
		public void mouseExited(MouseEvent e) {
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			showStats(e);
		}
		
		private void showStats(MouseEvent e) {
			GraphElementAccessor<Vertex, MutexAction> pickSupport = vv.getPickSupport();
			PickedState<Vertex> pickedVertexState = vv.getPickedVertexState();
			PickedState<MutexAction> pickedEdgeState = vv.getPickedEdgeState();
			if(pickSupport != null && pickedVertexState != null) {
				Layout<Vertex, MutexAction> layout = vv.getGraphLayout();
				if(e.getModifiers() == modifiers) {
					Point2D ip = e.getPoint();
					Vertex vertex = pickSupport.getVertex(layout, ip.getX(), ip.getY());
					if (vertex!=null)	{
						notifyObjectPropertiesSelectionListeners(vertex);
					}
				}
			}
			if(pickSupport != null && pickedEdgeState != null) {
				Layout<Vertex, MutexAction> layout = vv.getGraphLayout();
				if(e.getModifiers() == modifiers) {
					Point2D ip = e.getPoint();
					MutexAction edge = pickSupport.getEdge(layout, ip.getX(), ip.getY());
					if (edge!=null)	{
						notifyObjectPropertiesSelectionListeners(edge);
					}
				}
			}
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {
			showStats(e);
		}
		
	}
}