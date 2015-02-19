package lockvis.view;

import java.awt.BorderLayout;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import lockvis.model.ObjectProperties;

/**
 * Simple table in a panel to show the statistics from a domain object
 * @author rannett
 */
public class StatsPanel extends JPanel implements ObjectPropertiesSelectionListener{
    private static final long serialVersionUID = 1L;
    private JTable statsTbl;
    private DefaultTableModel statsTableModel;
    
    public StatsPanel() {
        super(new BorderLayout());
        statsTbl = new JTable();
        clearStats();
        this.add(statsTbl.getTableHeader(), BorderLayout.PAGE_START);
        this.add(statsTbl, BorderLayout.CENTER);
    }
    
    public void clearStats() {
        statsTableModel = new DefaultTableModel(new String[] { "Name", "Value" }, 0);
        statsTbl.setModel(statsTableModel);
    }

    @Override
    public void selected(ObjectProperties v) {
        clearStats();
        if (v!=null)	{
        	Map<String, String> displayProperties = v.getProperties();
        	Set<String> keySet = new TreeSet<String>(displayProperties.keySet());
        	for (String name : keySet) {
        		statsTableModel.addRow(new String[] { name, displayProperties.get(name) });
        	}
        }
    }

}
