package lockvis.view;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import lockvis.model.ThreadInfoSet;
import lockvis.model.VMThreadDump;

public class GraphTreeRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;
	private ImageIcon leafIcon;

	public GraphTreeRenderer(ImageIcon closeIcon, ImageIcon openIcon, ImageIcon leafIcon) {
		this.leafIcon = leafIcon;

		setClosedIcon(closeIcon);
		setOpenIcon(openIcon);
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (leaf && node.getUserObject() instanceof VMThreadDump) {
			setIcon(leafIcon);
			VMThreadDump nodeInfo = (VMThreadDump) (node.getUserObject());

			setToolTipText(nodeInfo.getDumpName());
		}

		this.setBackground(Color.WHITE);

		if (leaf && node.getUserObject() instanceof ThreadInfoSet) {
			ThreadInfoSet stc = (ThreadInfoSet) node.getUserObject();
			if (stc.isInDeadlock()) {
				this.setForeground(Color.RED);
			}
		}

		return this;
	}

}
