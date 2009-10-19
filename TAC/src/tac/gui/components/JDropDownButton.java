package tac.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicArrowButton;

public class JDropDownButton extends JButton {

	private BasicArrowButton arrowButton;
	private JPopupMenu buttonPopupMenu;

	public JDropDownButton(String text) {
		super(text);
		buttonPopupMenu = new JPopupMenu();
		arrowButton = new BasicArrowButton(SwingConstants.SOUTH, null, null, Color.BLACK, null);
		arrowButton.setBorder(BorderFactory.createEmptyBorder());
		arrowButton.setFocusable(false);
		setLayout(new BorderLayout());
		add(arrowButton, BorderLayout.EAST);
		arrowButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Rectangle r = getBounds();
				buttonPopupMenu.show(JDropDownButton.this, r.x, r.y + r.height);
			}
		});
	}

	public void addDropDownItem(String text, ActionListener l) {
		JMenuItem item = new JMenuItem(text);
		buttonPopupMenu.add(item);
	}
}