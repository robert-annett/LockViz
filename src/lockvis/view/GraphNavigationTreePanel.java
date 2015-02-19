package lockvis.view;

import java.awt.BorderLayout;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import lockvis.model.ThreadInfoSet;
import lockvis.model.VMThreadDump;


public class GraphNavigationTreePanel extends JPanel {
    
    private static final long serialVersionUID = 1L;

    private static final String ROOT_NAME = "Dumps";
    
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private JTree tree;
    private JScrollPane treeView;
    

    public GraphNavigationTreePanel(TreeSelectionListener treeListener) {
        super(new BorderLayout());

        // tree
        this.setBorder(new TitledBorder("Navigation"));
        rootNode = new DefaultMutableTreeNode(ROOT_NAME);
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(treeListener);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);

        ImageIcon openIcon = new ImageIcon(this.getClass().getResource("/icons/folder-open-16x16.png"));
        ImageIcon closeIcon = new ImageIcon(this.getClass().getResource("/icons/folder-16x16.png"));
        ImageIcon leafIcon = new ImageIcon(this.getClass().getResource("/icons/text-x-generic-16x16.png"));
        tree.setCellRenderer(new GraphTreeRenderer(closeIcon, openIcon, leafIcon));
        ToolTipManager.sharedInstance().registerComponent(tree);

        treeView = new JScrollPane(tree);

        this.add(treeView, BorderLayout.CENTER);
    }

    public void setSelectionPath(TreePath treePath) {
        if (treePath != null && !treePath.equals(tree.getSelectionPath())) {
            tree.setSelectionPath(treePath);
        }
    }

    public void updateTree(VMThreadDump vmThreadDump) {
        DefaultMutableTreeNode vmThreadDumpNode = new DefaultMutableTreeNode(vmThreadDump);
        treeModel.insertNodeInto(vmThreadDumpNode, rootNode, rootNode.getChildCount());

        // extract all the subgraphs
        List<ThreadInfoSet> extractSimpleGraphs = vmThreadDump.getSimpleGraphs();
        Collections.sort(extractSimpleGraphs, new Comparator<ThreadInfoSet>() {
            @Override
            public int compare(ThreadInfoSet o1, ThreadInfoSet o2) {
            	//If it's in deadlock then move to the top otherwise do it alphabetically
                if (o1.isInDeadlock() != o2.isInDeadlock())	{
                	return o1.isInDeadlock() ? -1 : 1;
                }
                		
            	return o1.getDumpName().compareTo(o2.getDumpName());
            }

        });
        for (ThreadInfoSet list : extractSimpleGraphs) {
            DefaultMutableTreeNode vmThreadDumpSubNode = new DefaultMutableTreeNode(list);
            treeModel.insertNodeInto(vmThreadDumpSubNode, vmThreadDumpNode, vmThreadDumpNode.getChildCount());
            tree.scrollPathToVisible(new TreePath(vmThreadDumpSubNode.getPath()));
        }

        tree.setSelectionPath(new TreePath(vmThreadDumpNode.getPath()));
        tree.scrollPathToVisible(new TreePath(rootNode));
    }

    public DefaultMutableTreeNode getSelectedNode() {
        return (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
    }
}
