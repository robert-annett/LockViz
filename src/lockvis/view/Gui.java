package lockvis.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ThreadMXBean;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import lockvis.model.MutexAction;
import lockvis.model.ThreadInfoSet;
import lockvis.model.VMJmxThreadDumpFactory;
import lockvis.model.VMThreadDump;
import lockvis.model.VMThreadDumpFactory;
import lockvis.model.Vertex;
import lockvis.util.JMXConnectorUtil;
import lockvis.view.LockvisPreferences.PreferenceKey;
import edu.uci.ics.jung.graph.Graph;

public class Gui {

	private static final String version = "1.02 Release. Now with JMX!";

	private static Logger LOGGER = Logger.getLogger(Gui.class.getName());

	private static final String FILE_CHOOSER_START = "resources/examples";

	private JFrame mainFrame;
	private JPanel mainPanel;
	private TabbedGraphPanel tabbedGraphPnl;
	private JSplitPane mainSplitPane;
	private JSplitPane leftPnl;
	private GraphNavigationTreePanel treePnl;
	private StatsPanel nodeStatsPnl;
	private ControllerPanel viewControlsPnl;
	private GraphOverviewDisplay graphOverviewDisplay;

	private Map<ThreadGraphDisplay, TreePath> displayToNode = new HashMap<ThreadGraphDisplay, TreePath>();

	public static void main(String[] args) {
		Gui g = new Gui();
		g.init();
		g.show();
	}

	public void init() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
		}
		createMainFrame();
		createDefaultExceptionHandler();
		createMenus();
		createMainPaneContents();
	}

	private void createControlToolBar(JPanel parent) {
		viewControlsPnl = new ControllerPanel();
		parent.add(viewControlsPnl, BorderLayout.NORTH);
	}

	private void createDefaultExceptionHandler() {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainFrame, "Sorry but an error has occurred. Please report this: " + e.getMessage());
			}
		});
	}

	private void createMainPaneContents() {
		// create the tree on the left
		JComponent leftPnl = createLeftPanel();

		// This listener is for syncing up the tabs and the tree
		ChangeListener changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent changeEvent) {
				JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
				Component selectedComponent = sourceTabbedPane.getSelectedComponent();
				TreePath treePath = displayToNode.get(selectedComponent);
				treePnl.setSelectionPath(treePath);
			}
		};

		// create the tabbed pane for opened subgraphs
		tabbedGraphPnl = new TabbedGraphPanel();
		tabbedGraphPnl.addChangeListener(changeListener);

		JPanel graphWithControls = new JPanel(new BorderLayout());
		graphWithControls.add(tabbedGraphPnl, BorderLayout.CENTER);
		createControlToolBar(graphWithControls);

		// Split pane in the center
		mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPnl, graphWithControls);
		mainSplitPane.setOneTouchExpandable(true);

		mainSplitPane.setDividerLocation(LockvisPreferences.getInt(PreferenceKey.VERTICAL_DIVIDER_LOCATION));

		mainPanel.add(mainSplitPane, BorderLayout.CENTER);
	}

	private JComponent createLeftPanel() {
		treePnl = new GraphNavigationTreePanel(new VMNodeTreeSelectionListener());

		nodeStatsPnl = new StatsPanel();
		nodeStatsPnl.setPreferredSize(new Dimension(100, 130));
		nodeStatsPnl.setBorder(new TitledBorder("Selected Item Properties"));
		JScrollPane stateScr = new JScrollPane(nodeStatsPnl);

		JPanel controlAndStatsPnl = new JPanel(new BorderLayout());
		graphOverviewDisplay = new GraphOverviewDisplay(new Dimension(320, 200));
		final JPanel overviewTitle = new JPanel(new BorderLayout());
		overviewTitle.setBorder(new TitledBorder("Satellite View"));
		overviewTitle.add(graphOverviewDisplay, BorderLayout.CENTER);
		controlAndStatsPnl.add(overviewTitle, BorderLayout.CENTER);
		controlAndStatsPnl.add(stateScr, BorderLayout.SOUTH);

		leftPnl = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treePnl, controlAndStatsPnl);
		leftPnl.setDividerLocation(LockvisPreferences.getInt(PreferenceKey.CTLPNL_DIVIDER_LOCATION));

		return leftPnl;
	}

	// create a menu in the main frame
	private void createMenus() {
		JMenuBar menuBar = new JMenuBar();

		JMenu file = new JMenu("File");
		file.add(openFileAction);
		file.add(remoteConnectionAction);
		file.addSeparator();
		file.add(exitAction);
		file.setMnemonic('f');
		menuBar.add(file);

		JMenu help = new JMenu("Help");
		help.add(aboutAction);
		help.add(documentationAction);
		help.setMnemonic('h');
		menuBar.add(help);

		mainFrame.setJMenuBar(menuBar);
	}

	private void createMainFrame() {
		mainFrame = new JFrame("Thread Dump Visualiser");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setIconImage(Icons.lockVizIcon);
		mainPanel = new JPanel(new BorderLayout());
		mainFrame.getContentPane().add(mainPanel);
	}

	public void show() {
		mainFrame.pack();
		mainFrame.setSize(LockvisPreferences.getInt(PreferenceKey.MAIN_WIDTH), LockvisPreferences.getInt(PreferenceKey.MAIN_HEIGHT));
		mainFrame.setVisible(true);
	}

	// --------- GUI Action methods ---------------
	private Action openFileAction = new AbstractAction("Open Dump File", Icons.documentOpenIcon) {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			JFileChooser chooser = new JFileChooser(FILE_CHOOSER_START);
			FileFilter filter = new FileNameExtensionFilter("Thread dump files", "tdump", "dmp", "txt");
			chooser.addChoosableFileFilter(filter);

			int returnVal = chooser.showOpenDialog(mainFrame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File selectedFile = chooser.getSelectedFile();
				new FileImportWorker(selectedFile, new PleaseWait("Please wait while importing from: " + selectedFile, mainFrame)).execute();
			}
		}
	};
	
	
	private Action remoteConnectionAction = new AbstractAction("Remote Connection", Icons.remoteConnectionIcon) {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed(ActionEvent e) {
			//TODO: We need to have a proper GUI with connection details/security etc. Bespoke GUI for this and allow saves of the connections.
			String connectionUrl = (String)JOptionPane.showInputDialog(mainFrame, "Please enter remote JMX connection string\r\n" + "" +
					"Usage:  service:jmx:<protocol>:<sap>\r\ne.g. service:jmx:rmi://localhost/jndi/rmi://myserver:5000/jmxrmi", "Connect to remote process via JMX", 
					JOptionPane.QUESTION_MESSAGE, null, null, "service:jmx:rmi://localhost/jndi/rmi://SERVERNAME:PORT/jmxrmi");
			
			try	{
				if (connectionUrl==null || "".equals(connectionUrl))	{
					return;
				}
				
				PleaseWait pw = new PleaseWait("Please wait while importing from: " + connectionUrl, mainFrame);
				JMXImportWorker iw = new JMXImportWorker(connectionUrl, pw);
				iw.execute();
						
			}
			catch(Exception ex)	{
				ex.printStackTrace();
				JOptionPane.showMessageDialog(mainFrame, "Error: " + ex.getMessage());
			}
		}
	};
	
	private abstract class ImportWorker<T> extends SwingWorker<VMThreadDump, Integer>	{
		
		private VMThreadDump result;
		private Exception except;
		private PleaseWait pw;
		protected T connection;
		
		public ImportWorker(T connection, PleaseWait pw)	{
			this.pw = pw;
			this.connection = connection;
		}
		
		protected abstract VMThreadDump actualImport() throws Exception;
		
		@Override
		protected VMThreadDump doInBackground() throws Exception {			
			try {
				result = actualImport();
				except = null;				
				return result;
			}
			catch (Exception ex) {
				ex.printStackTrace();
				except = ex;
			}
			return result;
		}
		
		@Override
		protected void done()	{
			if (result!=null)	{
				showDump(result);
				JOptionPane.showMessageDialog(mainFrame, "Successfully imported " + connection, "Success Importing", JOptionPane.INFORMATION_MESSAGE);
			}
			if (except!=null)	{
				JOptionPane.showMessageDialog(mainFrame, "Could not import " + connection + " due to " + except.getMessage(), "Error importing", JOptionPane.ERROR_MESSAGE);
			}
			if (pw!=null)	{
				pw.setVisible(false);
				pw.dispose();
			}
		}
	}
	
	private class JMXImportWorker extends ImportWorker<String> 	{

		public JMXImportWorker(String connection, PleaseWait pw)	{
			super(connection, pw);
		}

		@Override
		protected VMThreadDump actualImport() throws  Exception {
			JMXServiceURL url = new JMXServiceURL(connection);
			JMXConnector jmxc = JMXConnectorUtil.connectWithTimeout(url, 2000, TimeUnit.MILLISECONDS);				
			MBeanServerConnection mBeanServerConnection = jmxc.getMBeanServerConnection();				
			ThreadMXBean bean = java.lang.management.ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, java.lang.management.ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
			return VMJmxThreadDumpFactory.constructVMThreadDump(bean, connection);
		}
	}
	
	private class FileImportWorker extends ImportWorker<File> 	{

		public FileImportWorker(File connection, PleaseWait pw)	{
			super(connection, pw);
		}

		@Override
		protected VMThreadDump actualImport() throws  Exception {
			VMThreadDumpFactory dumpFactory = new VMThreadDumpFactory();
			return dumpFactory.constructVMThreadDump(connection);
		}
	}


	private Action exitAction = new AbstractAction("Exit") {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			// save preferences
			LockvisPreferences.putInt(PreferenceKey.MAIN_WIDTH, mainFrame.getWidth());
			LockvisPreferences.putInt(PreferenceKey.MAIN_HEIGHT, mainFrame.getHeight());
			LockvisPreferences.putInt(PreferenceKey.VERTICAL_DIVIDER_LOCATION, mainSplitPane.getDividerLocation());
			LockvisPreferences.putInt(PreferenceKey.CTLPNL_DIVIDER_LOCATION, leftPnl.getDividerLocation());

			System.exit(0);
		}
	};

	private Action aboutAction = new AbstractAction("About...", Icons.aboutIcon) {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(mainFrame,
					"LockVis Version " + version + "\r\nWritten by Robert Annett 2012", "About",
					JOptionPane.INFORMATION_MESSAGE);
		}
	};

	private Action documentationAction = new AbstractAction("Online Documentation", Icons.aboutIcon) {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				JOptionPane.showMessageDialog(mainFrame,
						"Sorry but browser cannot be launched from this version of java", "Help",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			Desktop desktop = Desktop.getDesktop();
			try {
				desktop.browse(new URI("http://sourceforge.net/p/lockviz/wiki/LockVizFullDocumentation/"));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	};


	private void showDump(VMThreadDump vmThreadDump) {
		treePnl.updateTree(vmThreadDump);
	}

	// See if the given dump already has a tab in which case choose it.
	// Otherwise create one.
	protected ThreadGraphDisplay displayDump(final ThreadInfoSet dump) {
		// We might already be showing this one.
		ThreadGraphDisplay display = tabbedGraphPnl.getDisplay(dump);
		if (display == null) {
			// else create a new tab for the display
			ThreadGraphFactory tgf = new ThreadGraphFactory();
			Graph<Vertex, MutexAction> createGraphFromDump = tgf.createGraphFromDump(dump.getThreadInfos());
			display = new ThreadGraphDisplay(createGraphFromDump, threadInfoSetChangeListener, nodeStatsPnl);

			String tabName = dump.getDumpName().length() < 12 ? dump.getDumpName() : dump.getDumpName().substring(0, 11);
			if (dump instanceof VMThreadDump) {
				VMThreadDump d = (VMThreadDump) dump;
				tabName = d.getOverrideName() != null ? d.getOverrideName() : d.getDumpName();
			}

			tabbedGraphPnl.addTab(tabName, display, dump);
		}

		tabbedGraphPnl.selectDisplay(display);
		viewControlsPnl.setSelectedDisplay(display);
		graphOverviewDisplay.showModel(display.getVisualizationViewer());
		return display;
	}

	private ThreadInfoSetSelectionListener threadInfoSetChangeListener = new ThreadInfoSetSelectionListener() {
		@Override
		public void selected(ThreadInfoSet e) {
			displayDump(e);
		}
	};

	// This listeners main task is to sync up the tree and the display panes
	private class VMNodeTreeSelectionListener implements TreeSelectionListener {

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			// If the node is unselected or the tree collapsed the new selection can be null
			if (e.getNewLeadSelectionPath() == null || e.getNewLeadSelectionPath().getLastPathComponent() == null) {
				return;
			}

			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getNewLeadSelectionPath().getLastPathComponent();
			Object nodeInfo = node.getUserObject();
			if (nodeInfo instanceof ThreadInfoSet) {
				ThreadInfoSet selectedDump = (ThreadInfoSet) nodeInfo;
				ThreadGraphDisplay displayDump = displayDump(selectedDump);
				TreePath selectionPath = e.getNewLeadSelectionPath();
				displayToNode.put(displayDump, selectionPath);
				nodeStatsPnl.selected(selectedDump);
			} else {
				nodeStatsPnl.clearStats();
			}
		}
	}


}
