package lockvis.view;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PleaseWait extends JFrame {

	private static final long serialVersionUID = 1L;
	private JLabel message = new JLabel("Please wait.........");
	
	public PleaseWait(String messageTxt, Component parent)	{
		super("Please Wait");
		JPanel mainPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
		this.add(mainPnl);
		mainPnl.add(message);
		setMessage(messageTxt);
		this.pack();
		Point location = parent.getLocation();
		this.setLocation((int)(location.getX() + parent.getWidth()/2 - this.getWidth()/2), (int)(location.getY() + parent.getHeight()/2 - this.getHeight()/2));
		setVisible(true);
	}
	
	public void setMessage(String messageTxt)	{
		message.setText(messageTxt);
	}
}
