package lockvis.view;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lockvis.model.MutexAction;
import lockvis.model.Vertex;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.SatelliteVisualizationViewer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

/**
 * This is a wrapper for the SatelliteVisualizationViewer that correctly resizes and re-layouts when either
 * it's parent view or it's own container resizes.
 * 
 * TODO: We should modify this so that we don't create a new SatelliteVisualizationViewer every time we change back to this model.
 */
public class GraphOverviewDisplay extends JPanel implements ChangeListener{

	private static final long serialVersionUID = 1L;
	private SatelliteVisualizationViewer<Vertex, MutexAction> overviewVV;

	public GraphOverviewDisplay(Dimension size) {
		super(new BorderLayout());
	}

	public void showModel(VisualizationViewer<Vertex, MutexAction> parentView) {
		this.removeAll();

		Dimension preferredSize = getVVFromPanelSize();
		overviewVV = new SatelliteVisualizationViewer<Vertex, MutexAction>(parentView, preferredSize);
		this.add(overviewVV, BorderLayout.CENTER);
		this.invalidate();
		parentView.addChangeListener(this);
	}

	private Dimension getVVFromPanelSize() {
		return new Dimension((int) getSize().getWidth() - 4, (int) getSize().getHeight() - 4);
	}

	@Override
	/**
	 * When the layout changes we want to scale the overview and re-centre it. 
	 * e.g. when the container this is in changes size.
	 */
	public void doLayout() {
		super.doLayout();		//We want to layout the view first so it sizes internal components correctly.
		if (overviewVV != null) {
			rescale();
		}		
	}

	private void rescale() {
		MutableTransformer viewTransformer = overviewVV.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW);
		float scaleFactor = GraphUtils.scaleToFit(overviewVV.getGraphLayout().getSize(), this.getSize());
		viewTransformer.setScale(scaleFactor, scaleFactor, overviewVV.getCenter());
		GraphUtils.centerGraph(overviewVV);
	}

	@Override
	//We want to re-scale this when the parent view changes state. e.g. when it resizes.
	public void stateChanged(ChangeEvent e) {
		rescale();
	}

}
