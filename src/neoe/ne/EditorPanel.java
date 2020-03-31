package neoe.ne;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.io.File;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import neoe.ne.U.LocationHistory;

public class EditorPanel extends JPanel implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener {

	/** It's only need to be not-null and not actually called? */
	class MyInputMethodRequestsHandler implements InputMethodRequests {
		Rectangle rect = new Rectangle(200, 200, 0, 10);

		@Override
		public AttributedCharacterIterator cancelLatestCommittedText(Attribute[] attributes) {
			System.out.println("cancelLatestCommittedText=" + Arrays.deepToString(attributes));
			return null;
		}

		@Override
		public AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex, Attribute[] attributes) {
			System.out.printf("getCommittedText %d, %d, %s\n", beginIndex, endIndex, Arrays.deepToString(attributes));
			return null;
		}

		@Override
		public int getCommittedTextLength() {
			System.out.println("getCommittedTextLength");
			return 0;
		}

		@Override
		public int getInsertPositionOffset() {
			System.out.println("getInsertPositionOffset");
			return 0;
		}

		@Override
		public TextHitInfo getLocationOffset(int x, int y) {
			System.out.println("getLocationOffset");
			return null;
		}

		@Override
		public AttributedCharacterIterator getSelectedText(Attribute[] attributes) {
			System.out.println("getSelectedText=" + Arrays.deepToString(attributes));
			return null;
		}

		@Override
		public Rectangle getTextLocation(TextHitInfo offset) {
			System.out.println("getTextLocation");
			// return rect;
			return null;
		}

	}

	static int openedWindows;

	private static final long serialVersionUID = -1667283144475200365L;

	JFrame frame;

	PlainPage lastPage;

	LocationHistory<String> pageHis = new LocationHistory<String>();

	public PlainPage page;

	List<PlainPage> pageSet = new ArrayList<PlainPage>();

	EditorPanelConfig config;

	static final String WINDOW_NAME = "neoeedit " + Version.REV;

	// CursorHistory ptCh = new CursorHistory();
	static boolean init = false;

	private static void doinit() throws Exception {
		if (init)
			return;
		else
			init = true;

		U.Config.setDefaultLookAndFeel();
		U.Config.setDefaultBKColor();
		U.Config.initKeys();
		Gimp.loadFromConfig();
		Plugin.load();

	}

	public EditorPanel(EditorPanelConfig config) throws Exception {
		doinit();
		this.config = config;
		U.Config.loadOtherConfig(config);
		enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.INPUT_METHOD_EVENT_MASK);
		setBackground(U.Config.getDefaultBgColor());
		setFocusable(true);
		addMouseMotionListener(this);
		addMouseListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		addInputMethodListener(new InputMethodListener() {

			@Override
			public void caretPositionChanged(InputMethodEvent event) {
				System.out.println(
						"if you see this, tell neoeedit's author what system you are in pls. caretPositionChanged="
								+ event.paramString());

			}

			@Override
			public void inputMethodTextChanged(InputMethodEvent event) {
				// System.out.println("getInputContext0=" + getInputContext());
				// System.out.println("inputMethodTextChanged="
				// + event.paramString());
				if (page == null)
					return;
				AttributedCharacterIterator text = event.getText();
				if (text == null) {
					page.preedit("", 0);
					return;
				}
				StringBuilder textBuffer = new StringBuilder();
				int committedCharacterCount = event.getCommittedCharacterCount();
				char c = text.first();
				while (c != CharacterIterator.DONE) {
					textBuffer.append(c);
					c = text.next();
				}
				String textString = textBuffer.toString();
				page.preedit(textString, committedCharacterCount);
			}
		});
		setOpaque(false);
		setCursor(new Cursor(Cursor.TEXT_CURSOR));
		setFocusTraversalKeysEnabled(false);
		PlainPage pp = new PlainPage(this, PageData.newEmpty("UNTITLED #" + U.randomID()));
		pp.ptSelection.selectAll();
	}

	public EditorPanel() throws Exception {
		this(EditorPanelConfig.DEFAULT);
	}

	void changeTitle() {
		if (frame == null)
			return;
		String fn = page.pageData.getFn();
		if (page.console != null) {
			frame.setTitle("Console - " + page.console.cmd);
			return;
		}
		if (fn != null) {
			frame.setTitle(new File(fn).getName() + " " + new File(fn).getParent() + " - (" + pageSet.size() + ") - "
					+ EditorPanel.WINDOW_NAME + U.suNotice());
		} else {
			frame.setTitle(page.pageData.getTitle() + " - (" + pageSet.size() + ") - " + EditorPanel.WINDOW_NAME
					+ U.suNotice());
		}
	}

	public PlainPage getPage() {
		return page;
	}

	@Override
	public void keyPressed(KeyEvent env) {
		try {
			page.keyPressed(env);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
			e.printStackTrace();
		}
	}

	@Override
	public void keyReleased(KeyEvent env) {
		try {
			page.keyReleased(env);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
			e.printStackTrace();
		}
	}

	@Override
	public void keyTyped(KeyEvent env) {
		try {
			page.keyTyped(env);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
			e.printStackTrace();
		}
	}

	@Override
	public void mouseClicked(MouseEvent evt) {
		try {
			page.mouseClicked(evt);
			grabFocus();
		} catch (Throwable e) {
			page.ui.message("err:" + e);
			e.printStackTrace();
		}

	}

	@Override
	public void mouseDragged(MouseEvent env) {
		try {
			page.mouseDragged(env);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
			e.printStackTrace();
		}
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {

	}

	@Override
	public void mouseExited(MouseEvent arg0) {

	}

	@Override
	public void mouseMoved(MouseEvent evt) {
		try {
			page.mouseMoved(evt);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
			e.printStackTrace();
		}
	}

	@Override
	public void mousePressed(MouseEvent evt) {
		try {
			page.mousePressed(evt);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
			e.printStackTrace();
		}
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
			e.printStackTrace();
		}
	}

	public void openWindow(EditorPanel parentUI) throws IOException {
		openWindow(U.e_png, parentUI);
	}

	public void openWindow(String iconname, EditorPanel parentUI) throws IOException {
		openedWindows++;
		if (frame != null)
			return;

		frame = new JFrame(EditorPanel.WINDOW_NAME);
		frame.setIconImage(U.getAppIcon(iconname));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Point p = U.Config.readFrameSize();
		U.setFrameSize(frame, p.x, p.y);
		frame.getContentPane().add(this);
		frame.setTransferHandler(new U.TH(this));
		frame.setLocationRelativeTo(parentUI);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			private long lastWarning;

			@Override
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
									U.showSelfDispMessage(pp, "File changed outside.(reloaded)", 4000);
									pp.changedOutside = false;
								} else {
									U.showSelfDispMessage(pp, "File changed outside.", 4000);
								}
								// break;
							}
						}

					}
				}
				// EditPanel.this.requestFocus();
			}

			public void windowClosed(WindowEvent e) {
				System.out.println("closed");
				openedWindows--;
			}

			@Override
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
				System.out.println("exit");
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

	@Override
	public void paint(Graphics g) {
		try {
			if (page != null) {
				page.xpaint(g, this.getSize());
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

	public void setPage(PlainPage pp, boolean rec) {
		lastPage = page;
		page = pp;
		if (rec) {
			pageHis.add(U.getLocString(pp), U.getLocString(lastPage));
		}
		changeTitle();
	}

	public String getCurrentText() {
		PageData pd = getPage().pageData;
		return U.exportString(pd.lines, pd.lineSep);
	}

}
