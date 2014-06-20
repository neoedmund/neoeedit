package neoe.ne;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Font;
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
import java.text.CharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class EditPanel extends JPanel implements MouseMotionListener,
		MouseListener, MouseWheelListener, KeyListener {

	class CursorHistory {
		Vector<CursorHistoryItem> items = new Vector<CursorHistoryItem>();
		int p;

		void back(String curTitle, int curX, int curY) throws Exception {
			if (p >= 0 && items.size() > 0) {
				if (p >= items.size())
					p = items.size() - 1;// bug auto fix
				CursorHistoryItem item = items.get(p--);
				if (item.pageName.equals(curTitle) && item.y == curY
						&& item.x == curX) {
					back(curTitle, curX, curY);
					return;
				}
				if (move(item))
					page.ui.message("moved back");
				else
					page.ui.message("position not exists, try again");
			} else {
				page.ui.message("no more history");
			}
		}

		void forward() throws Exception {
			if (!items.isEmpty() && p < items.size() - 1) {
				p++;
				CursorHistoryItem item = items.get(p);
				if (move(item))
					page.ui.message("moved forward");
				else
					page.ui.message("position not exists, try again");
			} else {
				page.ui.message("no more history");
			}
		}

		private CursorHistoryItem getCurrentItem(PlainPage page) {
			return new CursorHistoryItem(page.pageData.getTitle(), page.cx,
					page.cy);
		}

		private CursorHistoryItem getLastItem() {
			if (items.size() > 0 && p > 0 && p <= items.size())
				return items.get(p - 1);
			return null;
		}

		private boolean isSameLine(CursorHistoryItem last,
				CursorHistoryItem item) {
			return (last != null && last.pageName.equals(item.pageName) && last.y == item.y);
		}

		private boolean move(CursorHistoryItem item) throws Exception {
			return U.gotoFileLinePos(EditPanel.this, item.pageName, item.y + 1,
					item.x, false);
		}

		void record(CursorHistoryItem item) {
			CursorHistoryItem last = getLastItem();
			if (isSameLine(last, item))
				return;// same line, skip
			if (p < 0)
				p = 0;// bug auto fix
			if (p < items.size())
				items.setSize(p);
			items.add(item);
			p++;
		}

		void record(String pageName, int x, int y) {
			record(new CursorHistoryItem(pageName, x, y));
		}

		public void recordCurrent(PlainPage page) {
			record(getCurrentItem(page));
		}

		public void recordInput(PlainPage page) {
			CursorHistoryItem last = getLastItem();
			CursorHistoryItem item = getCurrentItem(page);
			if (isSameLine(last, item)) {
				return;
			}
			record(item);
		}

	}

	static class CursorHistoryItem {
		String pageName;
		int x, y;

		public CursorHistoryItem(String pageName, int x, int y) {
			this.pageName = pageName;
			this.x = x;
			this.y = y;
		}
	}

	/** It's only need to be not-null and not actually called? */
	public class MyInputMethodRequestsHandler implements InputMethodRequests {
		Rectangle rect = new Rectangle(200, 200, 0, 10);

		@Override
		public AttributedCharacterIterator cancelLatestCommittedText(
				Attribute[] attributes) {
			System.out.println("cancelLatestCommittedText="
					+ Arrays.deepToString(attributes));
			return null;
		}

		@Override
		public AttributedCharacterIterator getCommittedText(int beginIndex,
				int endIndex, Attribute[] attributes) {
			System.out.printf("getCommittedText %d, %d, %s\n", beginIndex,
					endIndex, Arrays.deepToString(attributes));
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
		public AttributedCharacterIterator getSelectedText(
				Attribute[] attributes) {
			System.out.println("getSelectedText="
					+ Arrays.deepToString(attributes));
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

	public Font _font;
	private boolean debugFPS = false;

	JFrame frame;

	private InputMethodRequests inputMethodRequestsHandler;

	PlainPage lastPage;

	private PlainPage page;

	List<PlainPage> pageSet = new ArrayList<PlainPage>();

	CursorHistory ptCh = new CursorHistory();

	public EditPanel() throws Exception {
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
				System.out
						.println("if you see this, tell neoeedit's author what system you are in pls. caretPositionChanged="
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
				int committedCharacterCount = event
						.getCommittedCharacterCount();
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

	public InputMethodRequests getInputMethodRequests() {
		// System.out.println("getInputMethodRequests()");
		if (inputMethodRequestsHandler == null) {
			inputMethodRequestsHandler = new MyInputMethodRequestsHandler();
		}
		return inputMethodRequestsHandler;
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
		try {
			page.mouseClicked(evt);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
		}

	}

	@Override
	public void mouseDragged(MouseEvent env) {
		try {
			page.mouseDragged(env);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
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
		}
	}

	@Override
	public void mousePressed(MouseEvent evt) {
		try {
			page.mousePressed(evt);
		} catch (Throwable e) {
			page.ui.message("err:" + e);
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
		}
	}

	public void openWindow() throws IOException {
		openedWindows++;
		if (frame != null)
			return;
		frame = new JFrame(PlainPage.WINDOW_NAME);
		frame.setIconImage(ImageIO.read(EditPanel.class
				.getResourceAsStream("/Alien.png")));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Point p = U.Config.readFrameSize();
		U.setFrameSize(frame, p.x, p.y);
		frame.getContentPane().add(this);
		frame.setTransferHandler(new U.TH(this));
		frame.setLocationRelativeTo(null);
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

	public void setPage(PlainPage pp, boolean recCh) {
		lastPage=page;
		page = pp;
		changeTitle();
		if (recCh) {
			ptCh.record(pp.pageData.getTitle(), pp.cx, pp.cy);
		}
	}

	private String suNotice() {
		String user = System.getProperty("user.name");
		if ("root".equals(user) || "administrator".equalsIgnoreCase(user)) {
			return " [su]";
		} else {
			return "";
		}
	}

}
