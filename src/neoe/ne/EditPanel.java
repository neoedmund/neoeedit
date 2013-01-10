package neoe.ne;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class EditPanel extends JPanel implements MouseMotionListener,
		MouseListener, MouseWheelListener, KeyListener {

	private static final long serialVersionUID = -1667283144475200365L;

	private boolean debugFPS = false;

	JFrame frame;

	PlainPage lastPage;

	private PlainPage page;
	List<PlainPage> pageSet = new ArrayList<PlainPage>();

	public Font _font;

	public EditPanel() throws Exception {
		setFocusable(true);
		addMouseMotionListener(this);
		addMouseListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setOpaque(false);
		setCursor(new Cursor(Cursor.TEXT_CURSOR));
		setFocusTraversalKeysEnabled(false);
		PlainPage pp = new PlainPage(this, PageData.newEmpty("UNTITLED #"
				+ U.randomID()));
		pp.ptSelection.selectAll();
	}

	void changeTitle() {
		if (frame == null)
			return;
		String fn = page.pageData.getFn();
		if (fn != null) {
			frame.setTitle(new File(fn).getName() + " "
					+ new File(fn).getParent() + " - (" + pageSet.size()
					+ ") - " + PlainPage.WINDOW_NAME + suNotice());
		} else {
			frame.setTitle(page.pageData.getTitle() + " - (" + pageSet.size()
					+ ") - " + PlainPage.WINDOW_NAME + suNotice());
		}
	}

	private String suNotice() {
		String user = System.getProperty("user.name");
		if ("root".equals(user)) {
			return " [su]";
		} else {
			return "";
		}
	}

	PlainPage getPage() {
		return page;
	}

	@Override
	public void keyPressed(KeyEvent env) {
		try {
			page.keyPressed(env);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
		}
	}

	@Override
	public void keyReleased(KeyEvent env) {
		try {
			page.keyReleased(env);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
		}
	}

	@Override
	public void keyTyped(KeyEvent env) {
		try {
			page.keyTyped(env);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
		}
	}

	@Override
	public void mouseClicked(MouseEvent evt) {
		page.mouseClicked(evt);

	}

	@Override
	public void mouseDragged(MouseEvent env) {
		page.mouseDragged(env);
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {

	}

	@Override
	public void mouseExited(MouseEvent arg0) {

	}

	@Override
	public void mouseMoved(MouseEvent arg0) {

	}

	@Override
	public void mousePressed(MouseEvent evt) {
		page.mousePressed(evt);
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {

	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent env) {
		try {
			page.mouseWheelMoved(env);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
		}
	}

	public void openWindow() throws IOException {
		if (frame != null)
			return;
		frame = new JFrame(PlainPage.WINDOW_NAME);
		frame.setIconImage(ImageIO.read(EditPanel.class
				.getResourceAsStream("/Alien.png")));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		U.setFrameSize(frame, 800, 600);
		frame.getContentPane().add(this);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				for (PlainPage pp : pageSet) {
					if (pp.pageData.getFn() != null) {
						try {
							U.saveFileHistory(pp.pageData.getFn(), pp.cy + 1);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		});

		frame.setTransferHandler(new U.TH(this));
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			private long lastWarning;

			public void windowActivated(WindowEvent e) {
				for (PlainPage pp : pageSet) {
					if (U.changedOutside(pp)) {
						long t = new File(pp.pageData.getFn()).lastModified();
						if (t > lastWarning) {
							lastWarning = t;
							if (!pp.changedOutside) {
								pp.changedOutside = true;
								if (pp.pageData.history.size() == 0) {
									U.readFile(pp.pageData, pp.pageData.getFn());// reload
									U.showSelfDispMessage(pp,
											"File changed outside.(reloaded)",
											4000);
									pp.changedOutside = false;
								} else {
									U.showSelfDispMessage(pp,
											"File changed outside.", 4000);
								}
								// break;
							}
						}

					}
				}
				// EditPanel.this.requestFocus();
			}
		});
		// frame.addWindowFocusListener(new WindowAdapter() {
		// public void windowGainedFocus(WindowEvent e) {
		// EditPanel.this.requestFocusInWindow();
		// }
		// });

		changeTitle();
		repaint();
	}

	public void paint(Graphics g) {
		long t1 = 0;
		if (debugFPS) {
			t1 = System.currentTimeMillis();
		}
		try {
			if (page != null) {
				page.xpaint(g, this.getSize());
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		if (debugFPS) {
			System.out.println("p " + (System.currentTimeMillis() - t1));
		}
	}

	public void setPage(PlainPage pp) {
		page = pp;
		changeTitle();
	}

}
