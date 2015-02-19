package lockvis.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import lockvis.model.MutexAction;
import lockvis.model.ThreadInfo;
import lockvis.model.ThreadLocation;
import lockvis.model.VMThreadDump;
import lockvis.model.VMThreadDumpFactory;

public class StackTraceViewer extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextPane textPane;

    public StackTraceViewer(ThreadInfo stackTrace, ThreadLocation highlightedLocation) {
        buildGui(stackTrace);
        refreshText(stackTrace, highlightedLocation);
        this.pack();
        this.setSize(640,480);
        this.setTitle("Stack Trace for " + stackTrace.getName());
    }

    // This recreates the text but styled for formatting.
    private void refreshText(ThreadInfo stackTrace, ThreadLocation highlightedLocation) {
        StyledDocument doc = textPane.getStyledDocument();;
        try {
            //header
            doc.insertString(doc.getLength(), (stackTrace.getStackHeader(new StringBuilder()).append("\r\n")).toString(), doc.getStyle("bold"));
            
            //the stack itself
            for (ThreadLocation loc : stackTrace.getStack()) {                
                doc.insertString(doc.getLength(), loc.getLocation() + "\r\n", loc==highlightedLocation ? doc.getStyle("highlight") : doc.getStyle("regular"));
                
                if (loc.getMutexActions().size()>0)   {
                    for(MutexAction ma : loc.getMutexActions())    {
                        String muS = "    " + ma.getState() + " on " + ma.getMutex().getName() + " ";
                        doc.insertString(doc.getLength(), muS + "\r\n", ma.getState().equals("waiting") ? doc.getStyle("red") : doc.getStyle("green"));
                    }
                }
            }

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void buildGui(ThreadInfo stackTrace) {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setIconImage(Icons.lockVizIcon);
        
        textPane = new JTextPane();
        StyledDocument doc = textPane.getStyledDocument();

        setStyles(doc);
        
        JPanel noWrapPanel = new JPanel( new BorderLayout() );
        noWrapPanel.add( textPane );
        
        JScrollPane areaScrollPane = new JScrollPane(noWrapPanel);
        areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        areaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        areaScrollPane.setPreferredSize(new Dimension(640, 480));
        areaScrollPane.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createCompoundBorder(
                                        BorderFactory.createTitledBorder("Stack Trace for " + stackTrace.getName()),
                                        BorderFactory.createEmptyBorder(5,5,5,5)),
                                        areaScrollPane.getBorder()));



        JPanel mainPanel = new JPanel(true);
        mainPanel.setLayout(new BorderLayout());
        this.getContentPane().add(mainPanel);
        
        mainPanel.add(areaScrollPane, BorderLayout.CENTER);
        
        JPanel buttonPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPnl.add(closeBtn);
        mainPanel.add(buttonPnl, BorderLayout.SOUTH);
    }

    private void setStyles(StyledDocument doc) {
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "SansSerif");

        Style s = doc.addStyle("bold", regular);
        StyleConstants.setBold(s, true);

        s = doc.addStyle("red", regular);
        StyleConstants.setForeground(s, Color.RED);
        
        s = doc.addStyle("green", regular);
        StyleConstants.setForeground(s, Color.GREEN.darker());

        s = doc.addStyle("highlight", regular);
        StyleConstants.setBackground(s, Color.YELLOW);
    }
    
    public static void main(String[] args) throws IOException, ParseException {
        VMThreadDumpFactory dumpFactory = new VMThreadDumpFactory();
        VMThreadDump vmThreadDump = dumpFactory.constructVMThreadDump(new File("resources/examples/dining.txt"));
        ThreadInfo loadedStackTrace = vmThreadDump.getThreadInfos().get(0);
        StackTraceViewer stv = new StackTraceViewer(loadedStackTrace, null);
        stv.setVisible(true);
        
        StackTraceViewer st2 = new StackTraceViewer(loadedStackTrace, loadedStackTrace.getStack().get(1));
        st2.setVisible(true);
    }
}
