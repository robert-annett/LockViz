package lockvis.view;

import java.awt.Dimension;
import java.awt.geom.Point2D;

import lockvis.model.MutexAction;
import lockvis.model.Vertex;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

public class GraphUtils {

	public static void centerGraph(VisualizationViewer<Vertex, MutexAction> vv2) {
	    Dimension sizeOfGraph = vv2.getGraphLayout().getSize();
	    Point2D centerOfLayout = getCenter(sizeOfGraph);
	    Dimension sizeOfView = vv2.getSize();
	    Point2D centerOfView = getCenter(sizeOfView);
	    
	    MutableTransformer modelTransformer = vv2.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
	    
	    modelTransformer.setTranslate(0, 0);  //reset to the top left
	    modelTransformer.translate(centerOfView.getX()-centerOfLayout.getX(), centerOfView.getY()-centerOfLayout.getY());  //set to the center.
	    vv2.repaint();
	}

    private static Point2D getCenter(Dimension sizeOfGraph) {
        return new Point2D.Double(sizeOfGraph.getWidth()/2, sizeOfGraph.getHeight()/2);
    }

    /**
     * Scales to fit into a required area. Assumes the original size is larger than
     * the wanted size.
     */
	public static float scaleToFit(Dimension originalSize, Dimension wantedSize)	{
		float widthfactor = (float) (wantedSize.getWidth()/originalSize.getWidth());
		float heightfactor = (float) (wantedSize.getHeight()/originalSize.getHeight());
		
		return Math.min(widthfactor, heightfactor);
	}

}
