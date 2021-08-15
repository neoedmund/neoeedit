package neoe.ne;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import neoe.ne.U.SimpleLayout;

public class FindReplaceWindow implements ActionListener, KeyListener {

	private JDialog dialog;
	JButton jb1;
	JButton jb2;
	JButton jb3;
	private JButton jb4;
	// private JFrame f;
	private JCheckBox jcb1;
	private JCheckBox jcb2;
	private JCheckBox jcb3;
	JRadioButton jrb1;
	private JRadioButton jrb2;
	JTextField jta1;
	JTextField jta2;
	private JTextField jtadir, jtFnFilter;
	private PlainPage page;
	private JCheckBox jcbDirection;
	private JLabel lbfnfilter;
	private JLabel lbdir;

	public FindReplaceWindow(Window f, PlainPage page) {
		this.page = page;
		// this.f = f;
		dialog = new JDialog(f, "Find/Replace");
		JPanel p = new JPanel();
		dialog.getContentPane().add(p);
		SimpleLayout s = new SimpleLayout(p);
		s.add(new JLabel("Find:"));
		s.add(jta1 = new JTextField());
		s.newline();
		s.add(new JLabel("Replace:"));
		s.add(jta2 = new JTextField());
		s.newline();

		s.add(jrb1 = new JRadioButton("IgnoreCase"));
		jrb1.setSelected(true);
		s.add(jrb2 = new JRadioButton("RegularExpression"));
		s.newline();
		jrb1.setVisible(false);
		jrb2.setVisible(false);

		s.add(jcb1 = new JCheckBox("in files", false));
		s.add(lbdir = new JLabel("Dir:"));
		s.add(jtadir = new JTextField());
		s.newline();
		s.add(lbfnfilter = new JLabel("fn filter:"));
		s.add(jtFnFilter = new JTextField());
		s.newline();
		s.add(jcbDirection = new JCheckBox("backward", false));
		s.add(jcb2 = new JCheckBox("include subdir", true));
		s.add(jcb3 = new JCheckBox("skip binary", true));
		s.newline();

		s.add(jb1 = new JButton("Find"));
		s.add(jb4 = new JButton("FindAll"));
		s.add(jb2 = new JButton("Replace"));
		s.add(jb3 = new JButton("Replace All"));
		s.newline();
		final JComponent[] pack1 = new JComponent[] { jtadir, jtFnFilter, lbdir, lbfnfilter };

		jcb1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				jb4.setVisible(!jcb1.isSelected());
				jb2.setVisible(!jcb1.isSelected());
				if (jcb1.isSelected()) {
					jb1.setText("Find in Files");
					jb3.setText("Replace in Files");
					setVisible(pack1, true);
				} else {
					jb1.setText("Find");
					jb3.setText("Replace All");
					setVisible(pack1, false);
				}
			}
		});
		jcb1.setSelected(false);
		setVisible(pack1, false);
		jb1.setActionCommand("find");
		jb2.setActionCommand("replace");
		jb3.setActionCommand("replaceall");
		jb4.setActionCommand("findall");
		jb1.addActionListener(this);
		jb2.addActionListener(this);
		jb3.addActionListener(this);
		jb4.addActionListener(this);
		jcb2.setEnabled(false);
		jcb3.setEnabled(false);
		dialog.pack();
		dialog.setLocationRelativeTo(f);
		if (page != null && page.pageData.getFn() != null) {
			jtadir.setText(new File(page.pageData.getFn()).getParent());
		}
		jta1.addKeyListener(this);
		jta2.addKeyListener(this);
		KeyListener closeOnEsc = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent env) {
				if (env.getKeyCode() == KeyEvent.VK_ESCAPE) {
					dialog.dispose();
				}
			}
		};
		dialog.addKeyListener(closeOnEsc);
		jta1.addKeyListener(closeOnEsc);
		jta2.addKeyListener(closeOnEsc);
		jtadir.addKeyListener(closeOnEsc);
		jtFnFilter.addKeyListener(closeOnEsc);
		jrb1.addKeyListener(closeOnEsc);
		jrb2.addKeyListener(closeOnEsc);
		jcb1.addKeyListener(closeOnEsc);
		jcb2.addKeyListener(closeOnEsc);
		jcb3.addKeyListener(closeOnEsc);
	}

	protected void setVisible(JComponent[] pack, boolean b) {
		for (JComponent p : pack) {
			p.setVisible(b);
		}
		dialog.pack();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		page.pageData.history.beginAtom();
		try {
			String command = ae.getActionCommand();
			if (command.equals("find")) {
				page.ptFind.doFind(jta1.getText(), jrb1.isSelected(), jrb2.isSelected(), jcb1.isSelected(),
						jtadir.getText(), jtFnFilter.getText(), jcbDirection.isSelected());

			} else if (command.equals("findall")) {
				U.doFindInPage(page, jta1.getText(), jrb1.isSelected());
			} else if (command.equals("replace")) {
				U.doReplace(page, jta1.getText(), jrb1.isSelected(), jrb2.isSelected(), jta2.getText(), false, false,
						null);
			} else if (command.equals("replaceall")) {
				U.doReplaceAll(page, jta1.getText(), jrb1.isSelected(), jrb2.isSelected(), jta2.getText(),
						jcb1.isSelected(), jtadir.getText(), jtFnFilter.getText());
			} else {
				return;
			}
			dialog.setVisible(false);
		} catch (Throwable e) {
			e.printStackTrace();
			page.ui.message(e.toString());
		}
		page.pageData.history.endAtom();
	}

	// public static void main(String[] args) {
	// JFrame f = new JFrame();
	// f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	// f.setVisible(true);
	// new FindReplaceWindow(f, null).show();
	// }

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getSource() == jta1) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				ActionEvent ae = new ActionEvent(this, 1, "find");
				actionPerformed(ae);
			}
		} else {
			if (e.getSource() == jta2) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					ActionEvent ae = new ActionEvent(this, 1, "replaceall");
					actionPerformed(ae);
				}
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	public void show() {
		dialog.setVisible(true);
	}
}
