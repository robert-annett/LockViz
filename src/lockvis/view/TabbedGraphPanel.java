package lockvis.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import lockvis.model.ThreadInfoSet;

public class TabbedGraphPanel extends JTabbedPane {

    private static final long serialVersionUID = 1L;
    
    private Map<ThreadInfoSet, ThreadGraphDisplay> modelToViewMap = new HashMap<ThreadInfoSet, ThreadGraphDisplay>();

    public TabbedGraphPanel()   {
        super();
        this.setBorder(new TitledBorder("Selected Subgraph"));        
    }
    
    public void addTab(String tabName, ThreadGraphDisplay display, final ThreadInfoSet dump) {
        final JPanel tab = new JPanel(new BorderLayout());
        tab.setOpaque(false);
        JLabel tabLabel = new JLabel(tabName);
        tabLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        tab.add(tabLabel, BorderLayout.WEST);
        tab.add(new CloseTabButton(tab, dump), BorderLayout.EAST);
        this.addTab(tabName, display);
        this.setTabComponentAt(this.getTabCount() - 1, tab);
        
        modelToViewMap.put(dump, display);
    }
    
    public ThreadGraphDisplay getDisplay(ThreadInfoSet dump)  {
        return modelToViewMap.get(dump);
    }

    public void selectDisplay(ThreadGraphDisplay display) {
        if (this.getSelectedComponent() != display) {
            this.setSelectedComponent(display);
        }
    }
    
    public class CloseTabButton extends JButton {
        private static final long serialVersionUID = 1L;

        public CloseTabButton(final JPanel tab, final ThreadInfoSet dump) {
            setPreferredSize(new Dimension(17, 17));
            setToolTipText("close tab");
            setUI(new BasicButtonUI());
            setContentAreaFilled(false);
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            setRolloverEnabled(true);
            addMouseListener(new MouseAdapter() {
                public void mouseExited(MouseEvent e) {
                    CloseTabButton.this.setBorderPainted(false);
                }

                public void mouseEntered(MouseEvent e) {
                    CloseTabButton.this.setBorderPainted(true);
                }
            });
            //remove it from the GUI AND the underlying model.
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int i = TabbedGraphPanel.this.indexOfTabComponent(tab);
                    if (i != -1) {
                        TabbedGraphPanel.this.remove(i);
                        modelToViewMap.remove(dump);
                    }
                }
            });
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);
            if (getModel().isRollover()) {
                g2.setColor(Color.MAGENTA);
            }
            int delta = 6;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }
    }
    
    public ThreadGraphDisplay getSelectedDisplay()  {
        return (ThreadGraphDisplay) this.getSelectedComponent();
    }
}
