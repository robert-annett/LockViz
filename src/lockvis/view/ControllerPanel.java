package lockvis.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ToolTipManager;
import javax.swing.border.EtchedBorder;

/**
 * This is a controller panel that can be linked to a graph pane and interact with the control there.
 * It has a set of components on the left and a set on the right.
 */
public class ControllerPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private ThreadGraphDisplay selectedDisplay;
    private JRadioButton pickingButton;
    private JRadioButton transformingButton;
     
    public ControllerPanel() {
        super(new BorderLayout());
        
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.add(left, BorderLayout.WEST);
        
        //Some control buttons
        JPanel controlButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlButtons.add(createToolBarButton("Reset Layout", new ToolbarAction(Icons.redrawIcon, resetAction)));
        controlButtons.add(createToolBarButton("Zoom out to birds-eye view", new ToolbarAction(Icons.zoomOutIcon, birdsEyeAction)));
        controlButtons.add(createToolBarButton("Zoom in from birds-eye view", new ToolbarAction(Icons.zoomInIcon, birdsEyeInAction)));
        controlButtons.add(createToolBarButton("Center on Graph", new ToolbarAction(Icons.zoomTargetIcon, centerGraphAction)));
        left.add(controlButtons, BorderLayout.NORTH);
        
        //transforming node selection
        pickingButton = new JRadioButton("Picking");
        transformingButton = new JRadioButton("Transforming");
        ButtonGroup group = new ButtonGroup();
        group.add(pickingButton);
        group.add(transformingButton);   
        
        ActionListener al = new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                String actionCommand = e.getActionCommand();                
                if (selectedDisplay!=null)  {
                    if ("Picking".equals(actionCommand))    {
                        selectedDisplay.selectPickingMode();
                    }
                    else    {
                        selectedDisplay.selectTransformingMode();
                    }
                }
            }
        };
        
        pickingButton.addActionListener(al);
        transformingButton.addActionListener(al);
        
        JPanel nodeSelectionTypePnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        nodeSelectionTypePnl.add(pickingButton);
        nodeSelectionTypePnl.add(transformingButton);
        nodeSelectionTypePnl.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        transformingButton.setMargin(new Insets(0, 0, 0, 0));
        pickingButton.setMargin(new Insets(0, 0, 0, 0));
        left.add(nodeSelectionTypePnl);
        
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        this.add(right, BorderLayout.EAST);
    }

	private JButton createToolBarButton(String toolTip, Action action) {
		JButton redrawBtn = new JButton(action);
        redrawBtn.setMargin(new Insets(0, 0, 0, 0));
        ToolTipManager.sharedInstance().registerComponent(redrawBtn);
		redrawBtn.setToolTipText(toolTip);
		return redrawBtn;
	}
    
    private ActionListener resetAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			selectedDisplay.resetLayout();
		}
	}; 
    		
    private ActionListener birdsEyeAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			selectedDisplay.birdsEyeZoomOut();
        }
    };
    
    private ActionListener birdsEyeInAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			selectedDisplay.birdsEyeZoomIn();
        }
    };

    private ActionListener centerGraphAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			selectedDisplay.centerGraph();
        }
    };
    
    public void setSelectedDisplay(ThreadGraphDisplay selectedDisplay) {
        //Lets set the radio button to the correct setting before we actually get a handle to it
        this.selectedDisplay = null;
        
        if (selectedDisplay.isPickingMode())    {
            pickingButton.setSelected(true);
        }
        else    {
            transformingButton.setSelected(true);
        }
        
        this.selectedDisplay = selectedDisplay;
    }

    private class ToolbarAction extends AbstractAction {
    	private static final long serialVersionUID = 1L;
    	private ActionListener al;
    	public ToolbarAction(Icon icon, ActionListener al)  {
    		super("", icon); 
    		this.al = al;
    	}

        public void actionPerformed(ActionEvent e) {
            if (selectedDisplay!=null)  {
                al.actionPerformed(e);
            }
        }
    };
  
    

}
