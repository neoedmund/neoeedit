package neoe.ne;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Graphics;
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

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
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

	public RootPaneContainer frame;

	PlainPage lastPage;

	LocationHistory<String> pageHis = new LocationHistory<String>();

	public PlainPage page;

	List<PlainPage> pageSet = new ArrayList<PlainPage>();

	EditorPanelConfig config;

	JDesktopPane desktopPane;

	/** for handle the window events */
	JFrame realJFrame;

	public EditorPanel() throws Exception {
		this(EditorPanelConfig.DEFAULT);
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
		PlainPage pp =  PlainPage.getPP(this, PageData.newEmpty("UNTITLED #" + U.randomID()));
		pp.ptSelection.selectAll();
	}

	void changeTitle() {
		if (frame == null)
			return;
		String fn = page.pageData.getFn();
		String title = null;
		if (page.console != null) {
			title = ("Console - " + page.console.cmd);
		} else {
			if (fn != null) {
				title = (new File(fn).getName() + " " + new File(fn).getParent() + " - (" + pageSet.size() + ") - "
						+ EditorPanel.WINDOW_NAME + U.suNotice());
			} else {
				title = (page.pageData.getTitle() + " - (" + pageSet.size() + ") - " + EditorPanel.WINDOW_NAME
						+ U.suNotice());
			}
		}
		if (title != null) {
			if (frame instanceof JFrame) {
				((JFrame) frame).setTitle(title);
			} else if (frame instanceof JInternalFrame) {
				((JInternalFrame) frame).setTitle(title);
			}
		}
	}

	public String getCurrentText() {
		PageData pd = getPage().pageData;
		return U.exportString(pd.lines, pd.lineSep);
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
		openedWindows++;
		if (frame != null)
			return;
		JFrame frame = new JFrame(EditorPanel.WINDOW_NAME);
		openWindow(U.e_png, parentUI, frame, frame, null);
		installWindowListener(frame);
	}
	
	
	public void openWindow(String iconname, EditorPanel parentUI, RootPaneContainer outFrame, JFrame realJFrame,
			JDesktopPane desktopPane) throws IOException {
		frame = outFrame;
		this.desktopPane = desktopPane;
		if (iconname == null)
			iconname = U.e_png;
		if (frame instanceof JFrame) {
			initJFrame(iconname, parentUI, (JFrame) frame);
		} else if (frame instanceof JInternalFrame) {
			JInternalFrame ji = (JInternalFrame) frame;
			ji.add(this);
		}
		this.realJFrame = realJFrame;
		changeTitle();
		repaint();
	}
	
	public void installWindowListener(JFrame realJFrame) {
		realJFrame.addWindowListener(new WindowAdapter() {
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
				int size = pageSet.size();
				int i=0;
				for (PlainPage pp : pageSet) {
					if (pp.pageData.getFn() != null) {
						try {
							System.out.printf("save file his[%d/%d]%s\n",++i,size,pp.pageData.getFn());
							U.saveFileHistory(pp.pageData.getFn(), pp.cy + 1);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
				// System.out.println("exit");
			}
		});
	}

	private void initJFrame(String iconname, EditorPanel parentUI, JFrame frame) throws IOException {
		if (iconname != null) {
			frame.setIconImage(U.getAppIcon(iconname));
		}
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		U.setFrameSize(frame);
		frame.setTransferHandler(new U.TH(this));
		frame.setLocationRelativeTo(parentUI);
		frame.add(this);
		frame.setVisible(true);

		// frame.addWindowFocusListener(new WindowAdapter() {
		// public void windowGainedFocus(WindowEvent e) {
		// EditPanel.this.requestFocusInWindow();
		// }
		// });

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

}
