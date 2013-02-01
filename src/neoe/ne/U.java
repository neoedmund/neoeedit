package neoe.ne;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;

import neoe.ne.PlainPage.Paint;
import neoe.util.FileIterator;
import neoe.util.FileUtil;

/**
 * util
 */
public class U {

	static enum BasicAction {
		Delete, DeleteEmtpyLine, Insert, InsertEmptyLine, MergeLine
	}

	static class BasicEdit {
		PageData data;
		boolean record;

		BasicEdit(boolean record, PageData data) {
			this.record = record;
			this.data = data;
		}

		void deleteEmptyLine(int y) {
			StringBuffer sb = lines().get(y);
			if (sb.length() > 0) {
				throw new RuntimeException("not a empty line " + y + ":" + sb);
			}
			if (lines().size() > 1) {
				lines().remove(y);
				if (record) {
					history().addOne(
							new HistoryCell(BasicAction.DeleteEmtpyLine, -1,
									-1, y, -1, null));
				}
			}
		}

		void deleteInLine(int y, int x1, int x2) {
			StringBuffer sb = lines().get(y);
			if (x1 >= sb.length())
				return;
			x2 = Math.min(x2, sb.length());
			String d = sb.substring(x1, x2);
			if (d.length() > 0) {
				sb.delete(x1, x2);
				if (record) {
					history().addOne(
							new HistoryCell(BasicAction.Delete, x1, x2, y, -1,
									d));
				}
			}
		}

		History history() {
			return data.history;
		}

		void insertEmptyLine(int y) {
			lines().add(y, new StringBuffer());
			if (record) {
				history().addOne(
						new HistoryCell(BasicAction.InsertEmptyLine, -1, -1, y,
								-1, null));
			}
		}

		void insertInLine(int y, int x, String s) {
			if (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
				throw new RuntimeException("cannot contains line-seperator:["
						+ s + "]" + s.indexOf('\n'));
			}
			if (y == data.roLines.getLinesize()) {
				data.editRec.insertEmptyLine(y);
			}
			StringBuffer sb = lines().get(y);
			if (x > sb.length()) {
				sb.setLength(x);
			}
			sb.insert(x, s);
			if (record) {
				history().addOne(
						new HistoryCell(BasicAction.Insert, x, x + s.length(),
								y, -1, null));
			}
		}

		List<StringBuffer> lines() {
			return data.lines;
		}

		void mergeLine(int y) {
			StringBuffer sb1 = lines().get(y);
			StringBuffer sb2 = lines().get(y + 1);
			int x1 = sb1.length();
			sb1.append(sb2);
			lines().remove(y + 1);
			if (record) {
				history().addOne(
						new HistoryCell(BasicAction.MergeLine, x1, -1, y, -1,
								null));
			}
		}
	}

	static class FindAndReplace {
		FindReplaceWindow findWindow;
		final private PlainPage pp;
		String text2find;

		public FindAndReplace(PlainPage plainPage) {
			this.pp = plainPage;
		}

		void doFind(String text, boolean ignoreCase, boolean selected2,
				boolean inDir, String dir) throws Exception {
			if (!inDir) {
				text2find = text;
				pp.ignoreCase = ignoreCase;
				findNext();
				pp.uiComp.repaint();
			} else {
				doFindInDir(pp, text, ignoreCase, selected2, inDir, dir);
			}
		}

		void findNext() {
			if (text2find != null && text2find.length() > 0) {
				Point p = find(pp, text2find, pp.cx + 1, pp.cy, pp.ignoreCase);
				if (p == null) {
					pp.ui.message("string not found");
				} else {
					pp.ptSelection.selectLength(p.x, p.y, text2find.length());

				}
			}
		}

		void showFindDialog() {
			String t = pp.ptSelection.getSelected();
			int p1 = t.indexOf('\n');
			if (p1 >= 0) {
				t = t.substring(0, p1);
			}
			if (t.length() == 0 && text2find != null) {
				t = text2find;
			}
			if (findWindow == null)
				findWindow = new FindReplaceWindow(pp.uiComp.frame, pp);
			if (t.length() > 0) {
				findWindow.jta1.setText(t);
			}
			findWindow.show();
			findWindow.jta1.grabFocus();
		}
	}

	static class History {
		public final static int MAXSIZE = 200;
		List<HistoryCell> atom;
		LinkedList<List<HistoryCell>> data;
		private boolean inAtom;
		int p;
		PageData pageData;

		public History(PageData pageData) {
			data = new LinkedList<List<HistoryCell>>();
			p = 0;
			atom = new ArrayList<HistoryCell>();
			this.pageData = pageData;
		}

		void add(List<HistoryCell> o) {
			if (p < data.size() && p >= 0) {
				for (int i = 0; i < data.size() - p; i++) {
					data.removeLast();
				}
			}
			List<HistoryCell> last = data.peekLast();
			// stem.out.println("last=" + last);
			if (!append(last, o)) {
				// System.out.println("add:" + o);
				data.add(o);
				if (data.size() > MAXSIZE) {
					data.removeFirst();
				} else {
					p += 1;
				}
			} else {
				// System.out.println("append:" + o);
			}
		}

		public void addOne(HistoryCell historyInfo) {
			atom.add(historyInfo);
		}

		/**
		 * try to append this change to the last ones
		 */
		boolean append(List<HistoryCell> lasts, List<HistoryCell> os) {
			if (lasts == null) {
				return false;
			}
			boolean ret = false;
			if (os.size() == 1) {
				HistoryCell o = os.get(0);
				HistoryCell last = lasts.get(lasts.size() - 1);
				if (o.canAppend(last)) {
					lasts.add(o);
					ret = true;
				}
			}
			return ret;
		}

		public void beginAtom() {
			if (inAtom) {
				System.err.println("bug:double beginAtom");
				new Exception("debug").printStackTrace();
			}
			inAtom = true;
			if (!atom.isEmpty()) {
				endAtom();
			}
		}

		public void clear() {
			atom.clear();
			data.clear();
			p = 0;
		}

		public void endAtom() {
			if (!atom.isEmpty()) {
				// System.out.println("end atom");
				add(atom);
				atom = new ArrayList<HistoryCell>();
			}
			inAtom = false;
		}

		public List<HistoryCell> get() {
			if (p <= 0) {
				return null;
			}
			p -= 1;
			// System.out.println("undo:" + data.get(p));
			return data.get(p);
		}

		public List<HistoryCell> getRedo() {
			if (p < data.size()) {
				p += 1;
				return data.get(p - 1);
			} else {
				return null;
			}
		}

		void redo(PlainPage page) throws Exception {
			List<HistoryCell> os = getRedo();
			if (os == null) {
				return;
			}
			for (HistoryCell o : os) {
				o.redo(page);
			}
		}

		public int size() {
			return p;
		}

		void undo(PlainPage page) throws Exception {
			List<HistoryCell> os = get();
			if (os == null) {
				return;
			}
			for (int i = os.size() - 1; i >= 0; i--) {
				HistoryCell o = os.get(i);
				o.undo(page);
			}
		}
	}

	static class HistoryCell {
		U.BasicAction action;
		String s1;
		int x1, x2, y1, y2;

		public HistoryCell(U.BasicAction action, int x1, int x2, int y1,
				int y2, String s1) {
			super();
			this.s1 = s1;
			this.x1 = x1;
			this.x2 = x2;
			this.y1 = y1;
			this.y2 = y2;
			this.action = action;
		}

		public boolean canAppend(HistoryCell last) {
			return ((last.action == U.BasicAction.Delete
					&& this.action == U.BasicAction.Delete && //
			((last.x1 == this.x1 || last.x1 == this.x2) && last.y1 == this.y1))//
			|| (last.action == U.BasicAction.Insert
					&& this.action == U.BasicAction.Insert && //
			((last.x1 == this.x1 || last.x2 == this.x1) && last.y1 == this.y1)));
		}

		public void redo(PlainPage page) {
			BasicEdit editNoRec = page.pageData.editNoRec;
			ReadonlyLines roLines = page.pageData.roLines;
			switch (action) {
			case Delete:
				s1 = roLines.getInLine(y1, x1, x2);
				editNoRec.deleteInLine(y1, x1, x2);
				page.cursor.setSafePos(x1, y1);
				break;
			case DeleteEmtpyLine:
				editNoRec.deleteEmptyLine(y1);
				page.cursor.setSafePos(0, y1);
				break;
			case Insert:
				editNoRec.insertInLine(y1, x1, s1);
				page.cursor.setSafePos(x1 + s1.length(), y1);
				s1 = null;
				break;
			case InsertEmptyLine:
				editNoRec.insertEmptyLine(y1);
				page.cursor.setSafePos(0, y1 + 1);
				break;
			case MergeLine:
				editNoRec.mergeLine(y1);
				page.cursor.setSafePos(x1, y1);
				break;
			default:
				throw new RuntimeException("unkown action " + action);
			}
		}

		@Override
		public String toString() {
			return "HistoryInfo [action=" + action + ", x1=" + x1 + ", x2="
					+ x2 + ", y1=" + y1 + ", y2=" + y2 + ", s1=" + s1 + "]\n";
		}

		public void undo(PlainPage page) {
			BasicEdit editNoRec = page.pageData.editNoRec;
			ReadonlyLines roLines = page.pageData.roLines;
			switch (action) {
			case Delete:
				editNoRec.insertInLine(y1, x1, s1);
				page.cursor.setSafePos(x1 + s1.length(), y1);
				s1 = null;
				break;
			case DeleteEmtpyLine:
				editNoRec.insertEmptyLine(y1);
				page.cursor.setSafePos(0, y1 + 1);
				break;
			case Insert:
				s1 = roLines.getInLine(y1, x1, x2);
				editNoRec.deleteInLine(y1, x1, x2);
				page.cursor.setSafePos(0, y1);
				break;
			case InsertEmptyLine:
				editNoRec.deleteEmptyLine(y1);
				page.cursor.setSafePos(0, y1);
				break;
			case MergeLine:
				String s2 = roLines.getInLine(y1, x1, Integer.MAX_VALUE);
				editNoRec.deleteInLine(y1, x1, Integer.MAX_VALUE);
				editNoRec.insertEmptyLine(y1 + 1);
				editNoRec.insertInLine(y1 + 1, 0, s2);
				page.cursor.setSafePos(0, y1 + 1);
				break;
			default:
				throw new RuntimeException("unkown action " + action);
			}
		}
	}

	static class Print implements Printable {
		Color colorLineNumber = new Color(0x30C200),
				colorGutterLine = new Color(0x30C200),
				colorNormal = Color.BLACK, colorDigit = new Color(0xA8002A),
				colorKeyword = new Color(0x0099CC),
				colorHeaderFooter = new Color(0x8A00B8),
				colorComment = new Color(200, 80, 50);
		Dimension dim;
		String fn;
		Font font = new Font("Monospaced", Font.PLAIN, 9);

		int lineGap = 3, lineHeight = 8, headerHeight = 20, footerHeight = 20,
				gutterWidth = 24, TAB_WIDTH_PRINT = 20;

		int linePerPage;
		ReadonlyLines roLines;
		String title;
		int totalPage;
		Paint ui;
		EditPanel uiComp;

		Print(PlainPage pp) {
			this.ui = pp.ui;
			this.uiComp = pp.uiComp;
			this.roLines = pp.pageData.roLines;
			this.fn = pp.pageData.getFn();
			this.title = pp.pageData.getTitle();
		}

		void drawReturn(Graphics2D g2, int w, int py) {
			g2.setColor(Color.red);
			g2.drawLine(w, py - lineHeight + font.getSize(), w + 3, py
					- lineHeight + font.getSize());
		}

		int drawStringLine(Graphics2D g2, String s, int x, int y) {
			int w = 0;
			int commentPos = getCommentPos(s);
			if (commentPos >= 0) {
				String s1 = s.substring(0, commentPos);
				String s2 = s.substring(commentPos);
				int w1 = drawText(g2, s1, x, y, false);
				w = w1 + drawText(g2, s2, x + w1, y, true);
			} else {
				w = drawText(g2, s, x, y, false);
			}
			return w;
		}

		int drawText(Graphics2D g2, String s, int x, int y, boolean isComment) {
			int w = 0;
			if (isComment) {
				String[] ws = s.split("\t");
				int i = 0;
				for (String s1 : ws) {
					if (i++ != 0) {
						g2.drawImage(U.tabImgPrint, x + w, y - lineHeight, null);
						w += TAB_WIDTH_PRINT;
					}
					g2.setColor(colorComment);
					g2.drawString(s1, x + w, y);
					w += g2.getFontMetrics().stringWidth(s1);
					if (w > dim.width - gutterWidth) {
						break;
					}
				}
			} else {
				List<String> s1x = U.split(s);
				for (String s1 : s1x) {
					if (s1.equals("\t")) {
						g2.drawImage(U.tabImgPrint, x + w, y - lineHeight, null);
						w += TAB_WIDTH_PRINT;
					} else {
						// int highlightid =
						U.getHighLightID(s1, g2, colorKeyword, colorDigit,
								colorNormal);
						g2.drawString(s1, x + w, y);
						w += g2.getFontMetrics().stringWidth(s1);
					}
					if (w > dim.width - gutterWidth) {
						break;
					}
				}
			}
			return w;
		}

		void drawTextLine(Graphics2D g2, String s, int x0, int y0,
				int charCntInLine) {
			int w = drawStringLine(g2, s, x0, y0);
			drawReturn(g2, w + gutterWidth + 2, y0);
		}

		private int getCommentPos(String s) {
			String[] comment = ui.comment;
			if (comment == null)
				return -1;
			for (String c : comment) {
				int p = s.indexOf(c);
				if (p >= 0)
					return p;
			}
			return -1;
		}

		int getTotalPage(PageFormat pf) {
			linePerPage = ((int) pf.getImageableHeight() - footerHeight - headerHeight)
					/ (lineGap + lineHeight);
			System.out.println("linePerPage=" + linePerPage);
			if (linePerPage <= 0)
				return 0;
			int lines = roLines.getLinesize();
			int page = (lines % linePerPage == 0) ? lines / linePerPage : lines
					/ linePerPage + 1;
			return page;
		}

		@Override
		public int print(Graphics graphics, PageFormat pf, int pageIndex)
				throws PrinterException {
			if (pageIndex > totalPage)
				return Printable.NO_SUCH_PAGE;
			// print
			ui.message("printing " + (pageIndex + 1) + "/" + totalPage);
			uiComp.repaint();
			Graphics2D g2 = (Graphics2D) graphics;
			g2.translate(pf.getImageableX(), pf.getImageableY());
			if (ui.noise) {
				U.paintNoise(g2, new Dimension((int) pf.getImageableWidth(),
						(int) pf.getImageableHeight()));
			}
			g2.setFont(font);
			g2.setColor(colorHeaderFooter);
			g2.drawString(fn == null ? title : new File(fn).getName(), 0,
					lineGap + lineHeight);
			{
				String s = (pageIndex + 1) + "/" + totalPage;
				g2.drawString(
						s,
						(int) pf.getImageableWidth()
								- U.strWidth(g2, s, TAB_WIDTH_PRINT) - 2,
						lineGap + lineHeight);
				s = new Date().toString() + " - NeoeEdit";
				g2.drawString(
						s,
						(int) pf.getImageableWidth()
								- U.strWidth(g2, s, TAB_WIDTH_PRINT) - 2,
						(int) pf.getImageableHeight() - 2);
				g2.setColor(colorGutterLine);
				g2.drawLine(gutterWidth - 4, headerHeight, gutterWidth - 4,
						(int) pf.getImageableHeight() - footerHeight);
			}
			int p = linePerPage * pageIndex;
			int charCntInLine = (int) pf.getImageableWidth() / 5 + 5;// inaccurate
			for (int i = 0; i < linePerPage; i++) {
				if (p >= roLines.getLinesize())
					break;
				int y = headerHeight + (lineGap + lineHeight) * (i + 1);
				g2.setColor(colorLineNumber);
				g2.drawString("" + (p + 1), 0, y);
				g2.setColor(colorNormal);
				String s = roLines.getline(p++).toString();
				if (s.length() > charCntInLine)
					s = s.substring(0, charCntInLine);
				drawTextLine(g2, s, gutterWidth, y, charCntInLine);

			}

			return Printable.PAGE_EXISTS;
		}

		void printPages() {

			new Thread() {
				@Override
				public void run() {
					try {
						PrinterJob job = PrinterJob.getPrinterJob();
						PageFormat pf = job.pageDialog(job.defaultPage());
						totalPage = getTotalPage(pf);
						if (totalPage <= 0)
							return;
						dim = new Dimension((int) pf.getImageableWidth(),
								(int) pf.getImageableHeight());
						Book bk = new Book();
						bk.append(Print.this, pf, totalPage);
						job.setPageable(bk);
						if (job.printDialog()) {
							ui.message("printing...");
							uiComp.repaint();
							job.print();
							ui.message("print ok");
							uiComp.repaint();
						}
					} catch (Exception e) {
						ui.message("err:" + e);
						uiComp.repaint();
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	static class ReadonlyLines {
		PageData data;

		ReadonlyLines(PageData data) {
			this.data = data;
		}

		String getInLine(int y, int x1, int x2) {
			RoSb sb = getline(y);
			if (x2 > sb.length()) {
				x2 = sb.length();
			}
			if (x1 > sb.length()) {
				x1 = sb.length();
			}
			return sb.substring(x1, x2);
		}

		RoSb getline(int i) {
			if (i < 0 || i >= data.lines.size()) {
				System.out.println("bug:?RoSb.getline(" + i + "),size="
						+ data.lines.size());
				return new RoSb(new StringBuffer());
			}
			return new RoSb(data.lines.get(i));
		}

		int getLinesize() {
			return data.lines.size();
		}

		String getTextInRect(Rectangle r, boolean rectSelectMode) {
			int x1 = r.x;
			int y1 = r.y;
			int x2 = r.width;
			int y2 = r.height;
			StringBuffer sb = new StringBuffer();
			if (rectSelectMode) {
				for (int i = y1; i <= y2; i++) {
					if (i != y1) {
						sb.append(data.lineSep);
					}
					sb.append(getInLine(i, x1, x2));
				}
			} else {
				if (y1 == y2 && x1 < x2) {
					sb.append(getInLine(y1, x1, x2));
				} else if (y1 < y2) {
					sb.append(getInLine(y1, x1, Integer.MAX_VALUE));
					for (int i = y1 + 1; i < y2; i++) {
						sb.append(data.lineSep);
						sb.append(getline(i));
					}
					sb.append(data.lineSep);
					sb.append(getInLine(y2, 0, x2));
				}
			}
			return sb.toString();
		}
	}

	/**
	 * read-only stringbuffer.
	 */
	static class RoSb {

		final private StringBuffer sb;

		public RoSb(StringBuffer sb) {
			this.sb = sb;
		}

		public char charAt(int i) {
			return sb.charAt(i);
		}

		public int length() {
			return sb.length();
		}

		public String substring(int i) {
			return sb.substring(i);
		}

		public String substring(int a, int b) {
			return sb.substring(a, b);
		}

		@Override
		public String toString() {
			return sb.toString();
		}

		public String toString(boolean ignoreCase) {
			String s = sb.toString();
			if (ignoreCase) {
				return s.toLowerCase();
			} else {
				return s;
			}
		}

	}

	public static class SimpleLayout {
		JPanel curr;
		JPanel p;

		public SimpleLayout(JPanel p) {
			this.p = p;
			p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
			newCurrent();
		}

		public void add(JComponent co) {
			curr.add(co);
		}

		void newCurrent() {
			curr = new JPanel();
			curr.setLayout(new BoxLayout(curr, BoxLayout.LINE_AXIS));
		}

		public void newline() {
			p.add(curr);
			newCurrent();
		}
	}

	static class TH extends TransferHandler {
		private static final long serialVersionUID = 5046626748299023865L;

		private EditPanel ep;

		TH(EditPanel ep) {
			this.ep = ep;
		}

		@Override
		public boolean canImport(TransferHandler.TransferSupport support) {
			if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				return false;
			}
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean importData(TransferHandler.TransferSupport support) {
			if (!canImport(support)) {
				return false;
			}
			Transferable t = support.getTransferable();
			try {
				List<File> l = (List<File>) t
						.getTransferData(DataFlavor.javaFileListFlavor);
				for (File f : l) {
					if (f.isFile())
						try {
							U.openFile(f, ep);
						} catch (Exception e) {
							e.printStackTrace();
						}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			ep.frame.repaint();
			return true;
		}
	}

	public static class UnicodeFormatter {
		static public String byteToHex(byte b) {
			// Returns hex String representation of byte b
			char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
					'9', 'a', 'b', 'c', 'd', 'e', 'f' };
			char[] array = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };
			return new String(array);
		}

		static public String charToHex(char c) {
			// Returns hex String representation of char c
			byte hi = (byte) (c >>> 8);
			byte lo = (byte) (c & 0xff);
			return byteToHex(hi) + byteToHex(lo);
		}
	}

	private final static String _TITLE_OF_PAGES = "__PAGES__";

	static final Object[][] BOMS = new Object[][] {
			new Object[] { new int[] { 0xEF, 0xBB, 0xBF }, "UTF-8" },
			new Object[] { new int[] { 0xFE, 0xFF }, "UTF-16BE" },
			new Object[] { new int[] { 0xFF, 0xFE }, "UTF-16LE" },
			new Object[] { new int[] { 0, 0, 0xFE, 0xFF }, "UTF-32BE" },
			new Object[] { new int[] { 0xFF, 0xFE, 0, 0 }, "UTF-32LE" }, };;

	static Map<String, Commands> keys;
	public final static String[] KWS = "ArithmeticError AssertionError AttributeError BufferType BuiltinFunctionType BuiltinMethodType ClassType CodeType ComplexType DeprecationWarning DictProxyType DictType DictionaryType EOFError EllipsisType EmitStreamVertex EmitVertex EndPrimitive EndStreamPrimitive EnvironmentError Err Exception False FileType FloatType FloatingPointError FrameType FunctionType GeneratorType IOError ImportError IndentationError IndexError InstanceType IntType KeyError KeyboardInterrupt LambdaType ListType LongType LookupError MemoryError MethodType ModuleType NameError None NoneType NotImplemented NotImplementedError OSError ObjectType OverflowError OverflowWarning ReferenceError RuntimeError RuntimeWarning SliceType StandardError StopIteration StringType StringTypes SyntaxError SyntaxWarning SystemError SystemExit TabError TracebackType True TupleType TypeError TypeType UnboundLocalError UnboundMethodType UnicodeError UnicodeType UserWarning ValueError Warning WindowsError XRangeType ZeroDivisionError __abs__ __add__ __all__ __author__ __bases__ __builtins__ __call__ __class__ __cmp__ __coerce__ __contains__ __debug__ __del__ __delattr__ __delitem__ __delslice__ __dict__ __div__ __divmod__ __doc__ __docformat__ __eq__ __file__ __float__ __floordiv__ __future__ __ge__ __getattr__ __getattribute__ __getitem__ __getslice__ __gt__ __hash__ __hex__ __iadd__ __import__ __imul__ __init__ __int__ __invert__ __iter__ __le__ __len__ __long__ __lshift__ __lt__ __members__ __metaclass__ __mod__ __mro__ __mul__ __name__ __ne__ __neg__ __new__ __nonzero__ __oct__ __or__ __path__ __pos__ __pow__ __radd__ __rdiv__ __rdivmod__ __reduce__ __repr__ __rfloordiv__ __rlshift__ __rmod__ __rmul__ __ror__ __rpow__ __rrshift__ __rsub__ __rtruediv__ __rxor__ __self__ __setattr__ __setitem__ __setslice__ __slots__ __str__ __sub__ __truediv__ __version__ __xor__ abs abstract acos acosh active all and any apply array as asc ascb ascw asin asinh asm assert atan atanh atn atomicAdd atomicAnd atomicCompSwap atomicCounter atomicCounterDecrement atomicCounterIncrement atomicExchange atomicMax atomicMin atomicOr atomicXor atomic_uint attribute auto barrier bitCount bitfieldExtract bitfieldInsert bitfieldReverse bool boolean break buffer bvec2 bvec3 bvec4 byref byte byval call callable case cast catch cbool cbyte ccur cdate cdbl ceil centroid char chr chrb chrw cint clamp class classmethod clng cmp coerce coherent common compile complex const continue cos cosh createobject cross csng cstr dFdx dFdy date dateadd datediff datepart dateserial datevalue day def default degrees del delattr determinant dict dim dir discard distance divmod dmat2 dmat2x2 dmat2x3 dmat2x4 dmat3 dmat3x2 dmat3x3 dmat3x4 dmat4 dmat4x2 dmat4x3 dmat4x4 do dot double dvec2 dvec3 dvec4 each elif else elseif empty end enum enumerate equal erase error eval except exec execfile execute exit exp exp2 explicit extends extern external faceforward false file filter final finally findLSB findMSB fix fixed flat float floatBitsToInt floatBitsToUint floor fma for formatcurrency formatdatetime formatnumber formatpercent fract frexp from frozenset function fvec2 fvec3 fvec4 fwidth get getattr getobject getref gl_ClipDistance gl_FragCoord gl_FragDepth gl_FrontFacing gl_GlobalInvocationID gl_InstanceID gl_InvocationID gl_Layer gl_LocalInvocationID gl_LocalInvocationIndex gl_NumSamples gl_NumWorkGroups gl_PatchVerticesIn gl_PointCoord gl_PointSize gl_Position gl_PrimitiveID gl_PrimitiveIDIn gl_SampleID gl_SampleMask gl_SampleMaskIn gl_SamplePosition gl_TessCoord gl_TessLevelInner gl_TessLevelOuter gl_VertexID gl_ViewportIndex gl_WorkGroupID gl_WorkGroupSize global globals goto greaterThan greaterThanEqual groupMemoryBarrier half hasattr hash hex highp hour hvec2 hvec3 hvec4 id if iimage1D iimage1DArray iimage2D iimage2DArray iimage2DMS iimage2DMSArray iimage2DRect iimage3D iimageBuffer iimageCube iimageCubeArray image1D image1DArray image2D image2DArray image2DMS image2DMSArray image2DRect image3D imageAtomicAdd imageAtomicAnd imageAtomicCompSwap imageAtomicExchange imageAtomicMax imageAtomicMin imageAtomicOr imageAtomicXor imageBuffer imageCube imageCubeArray imageLoad imageSize imageStore imp implements import imulExtended in inline inout input inputbox instanceof instr instrb instrrev int intBitsToFloat interface intern interpolateAtCentroid interpolateAtOffset interpolateAtSample invariant inverse inversesqrt is isampler1D isampler1DArray isampler2D isampler2DArray isampler2DMS isampler2DMSArray isampler2DRect isampler3D isamplerBuffer isamplerCube isamplerCubeArray isarray isdate isempty isinf isinstance isnan isnull isnumeric isobject issubclass iter ivec2 ivec3 ivec4 join lambda layout lbound lcase ldexp left leftb len lenb length lessThan lessThanEqual let list loadpicture locals log log2 long loop lowp ltrim map mat2 mat2x2 mat2x3 mat2x4 mat3 mat3x2 mat3x3 mat3x4 mat4 mat4x2 mat4x3 mat4x4 matrixCompMult max mediump memoryBarrier memoryBarrierAtomicCounter memoryBarrierBuffer memoryBarrierImage memoryBarrierShared mid midb min minute mix mod modf month monthname msgbox namespace native new next noinline noise noperspective normalize not notEqual nothing now null object oct on open option or ord out outerProduct output packDouble2x32 packHalf2x16 packSnorm2x16 packSnorm4x8 packUnorm2x16 packUnorm4x8 package packed partition pass patch pow precision preserve print private property protected public radians raise randomize range raw_input readonly redim reduce reflect refract register reload rem replace repr resource restrict resume return reversed rgb right rightb rnd round roundEven row_major rtrim sample sampler1D sampler1DArray sampler1DArrayShadow sampler1DShadow sampler2D sampler2DArray sampler2DArrayShadow sampler2DMS sampler2DMSArray sampler2DRect sampler2DRectShadow sampler2DShadow sampler3D sampler3DRect samplerBuffer samplerCube samplerCubeArray samplerCubeArrayShadow samplerCubeShadow scriptengine scriptenginebuildversion scriptenginemajorversion scriptengineminorversion second select self set setattr sgn shared short sign signed sin sinh sizeof slice smooth smoothstep sorted space split sqr sqrt static staticmethod step str strcomp strictfp string strreverse struct sub subroutine sum super superp switch synchronized tan tanh template texelFetch texelFetchOffset texture textureGather textureGatherOffset textureGatherOffsets textureGrad textureGradOffset textureLod textureLodOffset textureOffset textureProj textureProjGrad textureProjGradOffset textureProjLod textureProjLodOffset textureProjOffset textureQueryLevels textureQueryLod textureSize then this throw throws time timeserial timevalue to transient transpose trim true trunc try tuple type typedef typename uaddCarry ubound ucase uimage1D uimage1DArray uimage2D uimage2DArray uimage2DMS uimage2DMSArray uimage2DRect uimage3D uimageBuffer uimageCube uimageCubeArray uint uintBitsToFloat umulExtended unichr unicode uniform union unpackDouble2x32 unpackHalf2x16 unpackSnorm2x16 unpackSnorm4x8 unpackUnorm2x16 unpackUnorm4x8 unsigned until usampler1D usampler1DArray usampler2D usampler2DArray usampler2DMS usampler2DMSArray usampler2DRect usampler3D usamplerBuffer usamplerCube usamplerCubeArray using usubBorrow uvec2 uvec3 uvec4 vars vartype varying vbAbort vbAbortRetryIgnore vbApplicationModal vbCancel vbCritical vbDefaultButton1 vbDefaultButton2 vbDefaultButton3 vbDefaultButton4 vbExclamation vbFalse vbGeneralDate vbIgnore vbInformation vbLongDate vbLongTime vbNo vbOK vbOKCancel vbOKOnly vbObjectError vbQuestion vbRetry vbRetryCancel vbShortDate vbShortTime vbSystemModal vbTrue vbUseDefault vbYes vbYesNo vbYesNoCancel vbarray vbblack vbblue vbboolean vbbyte vbcr vbcrlf vbcurrency vbcyan vbdataobject vbdate vbdecimal vbdouble vbempty vberror vbformfeed vbgreen vbinteger vblf vblong vbmagenta vbnewline vbnull vbnullchar vbnullstring vbobject vbred vbsingle vbstring vbtab vbvariant vbverticaltab vbwhite vbyellow vec2 vec3 vec4 void volatile weekday weekdayname wend while with writeonly xor xrange year yield zip"
			.split(" ");

	public static List originKeys;

	static Random random = new Random();

	public static Image tabImg, tabImgPrint;

	static final String UTF8 = "utf8";

	static {
		try {
			System.out.println("welcome to " + PlainPage.WINDOW_NAME);
			loadTabImage();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void attach(final PlainPage page, final InputStream std) {
		new Thread() {
			@Override
			public void run() {
				try {
					String enc = System.getProperty("sun.jnu.encoding");
					if (enc == null)
						enc = "utf8";
					BufferedReader in = new BufferedReader(
							new InputStreamReader(std, enc));
					String line;
					page.ptEdit.append("encoding:" + enc + "\n");
					while ((line = in.readLine()) != null) {
						page.ptEdit.append(line + "\n");
						page.uiComp.repaint();
					}
					page.ptEdit.append("<EOF>\n");
				} catch (Throwable e) {
					page.ptEdit.append("error:" + e + "\n");
				}
			}
		}.start();
	}

	static boolean changedOutside(PlainPage pp) {
		PageData page = pp.pageData;
		String his = "";
		try {
			his = getFileHistoryName().getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (page.getFn() != null && (!page.getFn().equals(his))
				&& page.fileLastModified != 0) {
			long t = new File(page.getFn()).lastModified();
			if (t > page.fileLastModified + 100) {
				return true;
			}
		}
		return false;
	}

	static void closePage(PlainPage page) throws Exception {
		EditPanel editor = page.uiComp;
		int opt = JOptionPane.NO_OPTION;
		if (page.pageData.history.size() != 0) {
			opt = JOptionPane.showConfirmDialog(editor,
					"Are you sure to SAVE and close?", "Changes made",
					JOptionPane.YES_NO_CANCEL_OPTION);
			System.out.println(opt);
			if (opt == JOptionPane.CANCEL_OPTION || opt == -1)
				return;
		}
		if (opt == JOptionPane.YES_OPTION) {
			saveFile(page);
		}
		if (page.pageData.getFn() != null) {
			saveFileHistory(page.pageData.getFn(), page.cy);
		}
		page.close();
	}

	/**
	 * quick find how much char can be shown in width
	 * 
	 * @param width
	 * @param g2
	 * @return
	 */
	static int computeShowIndex(String s, int width, Graphics2D g2, int TABWIDTH) {
		if (s.length() == 0) {
			return 0;
		}
		if (U.strWidth(g2, s, TABWIDTH) <= width) {
			return s.length();
		}
		int i = s.length() / 2;
		while (true) {
			if (i == 0) {
				return 0;
			}
			int w = U.strWidth(g2, s.substring(0, i), TABWIDTH);
			if (w <= width) {
				return i
						+ computeShowIndex(s.substring(i), width - w, g2,
								TABWIDTH);
			} else {
				i = i / 2;
			}
		}
	}

	static void doFindInDir(PlainPage page, String text, boolean ignoreCase,
			boolean selected2, boolean inDir, String dir) throws Exception {
		Iterable<File> it = new FileIterator(dir);
		List<String> all = new ArrayList<String>();
		for (File f : it) {
			if (f.isDirectory()) {
				continue;
			}
			List<String> res = U.findInFile(f, text, ignoreCase);
			all.addAll(res);
		}
		showResult(page, all, "dir", dir, text);
		page.uiComp.repaint();
	}

	static void doFindInPage(PlainPage page, String text2find,
			boolean ignoreCase) throws Exception {
		if (text2find != null && text2find.length() > 0) {
			Point p = U.find(page, text2find, 0, 0, ignoreCase);
			if (p == null) {
				page.ui.message("string not found");
			} else {
				List<String> all = new ArrayList<String>();
				while (true) {
					all.add(String.format("%s:%s", p.y + 1,
							page.pageData.roLines.getline(p.y)));
					Point p2 = U.find(page, text2find, 0, p.y + 1, ignoreCase);
					if (p2 == null || p2.y <= p.y) {
						break;
					} else {
						p = p2;
					}
				}
				showResult(page, all, "file", page.pageData.getTitle(),
						text2find);
				page.uiComp.repaint();
			}
		}
	}

	static void doReplace(PlainPage page, String text, boolean ignoreCase,
			boolean selected2, String text2, boolean all, boolean inDir,
			String dir) {
		page.ptFind.text2find = text;
		Point p0 = all ? new Point(0, 0) : new Point(page.cx, page.cy);
		if (text != null && text.length() > 0) {
			Point p = replace(page, text, p0.x, p0.y, text2, all, ignoreCase);
			if (p == null) {
				page.ui.message("string not found");
			} else {
				if (!all) {
					page.cx = p.x;
					page.cy = p.y;
				}
				page.focusCursor();
				page.ptSelection.cancelSelect();
			}
		}
		page.uiComp.repaint();
	}

	static void doReplaceAll(PlainPage page, String text, boolean ignoreCase,
			boolean selected2, String text2, boolean inDir, String dir)
			throws Exception {
		if (inDir) {
			U.doReplaceInDir(page, text, ignoreCase, text2, inDir, dir);
		} else {
			U.doReplace(page, text, ignoreCase, selected2, text2, true, inDir,
					dir);
		}
	}

	static void doReplaceInDir(PlainPage page, String text,
			boolean ignoreCase2, String text2, boolean inDir, String dir)
			throws Exception {
		Iterable<File> it = new FileIterator(dir);
		List<String> all = new ArrayList<String>();
		for (File f : it) {
			if (f.isDirectory()) {
				continue;
			}
			try {
				List<String> res = U.findInFile(f, text, page.ignoreCase);
				if (!res.isEmpty()) {
					PlainPage pi = new PlainPage(page.uiComp,
							PageData.newFromFile(f.getCanonicalPath()));
					if (pi != null) {
						doReplaceAll(pi, text, ignoreCase2, false, text2,
								false, null);
					}
				}
				all.addAll(res);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		showResult(page, all, "dir", dir, text);
		page.uiComp.repaint();
	}

	static int drawTwoColor(Graphics2D g2, String s, int x, int y, Color c1,
			Color c2, int d) {
		g2.setColor(c2);
		g2.drawString(s, x + d, y + d);
		g2.setColor(c1);
		g2.drawString(s, x, y);
		return g2.getFontMetrics().stringWidth(s);

	}

	public static void exec(PlainPage pp, String cmd) throws Exception {
		if (cmd.trim().length() <= 0)
			return;
		Process proc = Runtime.getRuntime().exec(cmd);
		InputStream stdout = proc.getInputStream();
		InputStream stderr = proc.getErrorStream();
		attach(getPage(pp.uiComp, "[stderr]"), stderr);
		attach(getPage(pp.uiComp, "[stdout]"), stdout);
	}

	static Point find(PlainPage page, String s, int x, int y, boolean ignoreCase) {
		if (y >= page.pageData.roLines.getLinesize())
			return null;
		if (ignoreCase) {
			s = s.toLowerCase();
		}
		x = Math.min(x, page.pageData.roLines.getline(y).toString(ignoreCase)
				.length());
		// first half row
		int p1 = page.pageData.roLines.getline(y).toString(ignoreCase)
				.indexOf(s, x);
		if (p1 >= 0) {
			return new Point(p1, y);
		}
		// middle rows
		int fy = y;
		for (int i = 0; i < page.pageData.roLines.getLinesize() - 1; i++) {
			fy += 1;
			if (fy >= page.pageData.roLines.getLinesize()) {
				fy = 0;
			}
			p1 = page.pageData.roLines.getline(fy).toString(ignoreCase)
					.indexOf(s);
			if (p1 >= 0) {
				return new Point(p1, fy);
			}
		}
		// last half row
		p1 = page.pageData.roLines.getline(y).toString(ignoreCase).substring(x)
				.indexOf(s);
		if (p1 >= 0) {
			return new Point(p1, fy);
		}
		return null;
	}

	static boolean findAndShowPageListPage(EditPanel ep, String title) {
		return findAndShowPageListPage(ep, title, true);
	}

	static boolean findAndShowPageListPage(EditPanel ep, String title,
			boolean show) {
		PlainPage pp = findPage(ep, title);
		if (pp == null)
			return false;
		else {
			if (show)
				ep.setPage(pp);
			return true;
		}
	}

	static boolean findAndShowPageListPage(EditPanel ep, String title,
			int lineNo) {
		boolean b = findAndShowPageListPage(ep, title);
		if (b) {
			ep.getPage().cursor.setSafePos(0, lineNo - 1);
			ep.getPage().focusCursor();
			ep.repaint();
		}
		return b;
		// boolean isPLP = title.equals(titleOfPages(ep));
		// for (PlainPage pp : ep.pageSet) {
		// if (pp.pageData.getTitle().equals(title)
		// && (pp.cy + 1 == lineNo || isPLP)) {
		// ep.setPage(pp);
		// return true;
		// }
		// }
		// return false;
	}

	static void findchar(PlainPage page, char ch, int inc, int[] c1, char chx) {
		int cx1 = c1[0];
		int cy1 = c1[1];
		RoSb csb = page.pageData.roLines.getline(cy1);
		int lv = 1;
		while (true) {
			if (inc == -1) {
				cx1--;
				if (cx1 < 0) {
					cy1--;
					if (cy1 < 0) {
						c1[0] = -1;
						c1[1] = -1;
						return;
					} else {
						csb = page.pageData.roLines.getline(cy1);
						cx1 = csb.length() - 1;
						if (cx1 < 0) {
							continue;
						}
					}
				}
				char ch2 = csb.charAt(cx1);
				if (ch2 == chx) {
					lv++;
				} else if (ch2 == ch) {
					lv--;
					if (lv == 0) {
						c1[0] = cx1;
						c1[1] = cy1;
						return;
					}
				}
			} else {
				cx1++;
				if (cx1 >= csb.length()) {
					cy1++;
					if (cy1 >= page.pageData.roLines.getLinesize()) {
						c1[0] = -1;
						c1[1] = -1;
						return;
					} else {
						csb = page.pageData.roLines.getline(cy1);
						cx1 = 0;
						if (cx1 >= csb.length()) {
							continue;
						}
					}
				}
				char ch2 = csb.charAt(cx1);
				if (ch2 == chx) {
					lv++;
				} else if (ch2 == ch) {
					lv--;
					if (lv == 0) {
						c1[0] = cx1;
						c1[1] = cy1;
						return;
					}
				}
			}
		}
	}

	static List<String> findInFile(File f, String text, boolean ignoreCase2) {
		// System.out.println("find in "+f.getName());
		int MAX_SHOW_CHARS_IN_LINE = 30;
		List<String> a = new ArrayList<String>();
		try {
			String enc = guessEncoding(f.getAbsolutePath());
			if (enc == null)
				enc = UTF8;// avoid wrong skip
			if (enc != null) {// skip binary
				String fn = f.getAbsolutePath();
				if (ignoreCase2) {
					text = text.toLowerCase();
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(
						new FileInputStream(f), enc));
				String line;
				int lineno = 0;
				while ((line = in.readLine()) != null) {
					lineno++;
					String oline = line;
					if (ignoreCase2) {
						line = line.toLowerCase();
					}

					if (line.indexOf(text) >= 0) {
						if (line.length() > MAX_SHOW_CHARS_IN_LINE) {
							line = line.substring(0, MAX_SHOW_CHARS_IN_LINE)
									+ "...";
						}
						a.add(String.format("%s|%s:%s", fn, lineno, oline));
					}
				}
				in.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return a;
	}

	static PlainPage findPage(EditPanel ep, String title) {
		for (PlainPage pp : ep.pageSet) {
			if (pp.pageData.getTitle().equals(title)) {
				return pp;
			}
		}
		return null;
	}

	static void gc() {
		System.out.print(km(Runtime.getRuntime().freeMemory()) + "/"
				+ km(Runtime.getRuntime().totalMemory()) + " -> ");
		Runtime.getRuntime().gc();
		System.out.println(km(Runtime.getRuntime().freeMemory()) + "/"
				+ km(Runtime.getRuntime().totalMemory()));
	}

	static String getClipBoard() {
		String s;
		try {
			s = Toolkit.getDefaultToolkit().getSystemClipboard()
					.getData(DataFlavor.stringFlavor).toString();
		} catch (Exception e) {
			s = "";
		}
		return s;
	}

	static File getFileHistoryName() throws IOException {
		File f = new File(getMyDir(), "fh.txt");
		if (!f.exists()) {
			new FileOutputStream(f).close();
		}
		return f;
	}

	static int getHighLightID(String s, Graphics2D g2, Color colorKeyword,
			Color colorDigital, Color color) {
		if (Arrays.binarySearch(KWS, s) >= 0
				|| Arrays.binarySearch(KWS, s.toLowerCase()) >= 0) {
			g2.setColor(colorKeyword);
		} else if (isAllDigital(s)) {
			g2.setColor(colorDigital);
		} else {
			g2.setColor(color);
		}
		return 0;
	}

	static String getIndent(String s) {
		int p = 0;
		while (p < s.length() && (s.charAt(p) == ' ' || s.charAt(p) == '\t')) {
			p += 1;
		}
		return s.substring(0, p);
	}

	public static Reader getInstalledReader(String fn) throws IOException {
		File installed = new File(getMyDir(), fn);
		if (!installed.exists()) {
			try {
				FileUtil.copy(ClassLoader.getSystemResourceAsStream(fn),
						new FileOutputStream(installed));
			} catch (IOException e) {
				e.printStackTrace();
				return getJarReader(fn);
			}
		}
		return new InputStreamReader(new FileInputStream(installed), "utf8");
	}

	static Reader getJarReader(String fn) throws UnsupportedEncodingException {
		return new InputStreamReader(ClassLoader.getSystemResourceAsStream(fn),
				"utf8");
	}

	public static File getMyDir() {
		String home = System.getProperty("user.home");
		File dir = new File(home, ".neoeedit");
		dir.mkdirs();
		return dir;
	}

	/** see findPage() */
	static PlainPage getPage(EditPanel ep, String title) throws Exception {
		PlainPage pp = findPage(ep, title);
		if (pp != null)
			return pp;
		PageData pd = PageData.dataPool.get(title);
		if (pd == null)
			pd = PageData.newEmpty(title, "");
		final PlainPage page = new PlainPage(ep, pd);
		return page;
	}

	public static List<StringBuffer> getPageListStrings(EditPanel ep)
			throws IOException {
		List<StringBuffer> ss = new ArrayList<StringBuffer>();
		sort(ep.pageSet);
		for (PlainPage pp : ep.pageSet) {
			StringBuffer sb = new StringBuffer();
			sb.append(pp.pageData.getTitle()
					+ "|"
					+ (pp.cy + 1)
					+ ":"
					+ " Edited:"
					+ pp.pageData.history.size()
					+ " "
					+ (pp.pageData.encoding == null ? "" : pp.pageData.encoding
							+ " ")
					+ (changedOutside(pp) ? "[Changed Outside!!]" : ""));
			ss.add(sb);
		}
		return ss;
	}

	public static String getStr(List row, int i) {
		if (i < 0 || i >= row.size())
			return "";
		return "" + row.get(i);
	}

	static String getText(PlainPage page) {
		StringBuffer sb = new StringBuffer();
		int len = page.pageData.roLines.getLinesize();
		for (int i = 0; i < len; i++) {
			if (i > 0)
				sb.append(page.pageData.lineSep);
			sb.append(page.pageData.roLines.getline(i).toString());
		}
		return sb.toString();
	}

	static boolean gotoFileLine(String sb, EditPanel ep,
			boolean isInPageListPage) throws Exception {
		int p1, p2;
		if ((p1 = sb.indexOf("|")) >= 0) {
			String fn = sb.substring(0, p1);
			if ((p2 = sb.indexOf(":", p1)) >= 0) {
				int line = -1;
				try {
					line = Integer.parseInt(sb.substring(p1 + 1, p2));
				} catch (Exception e) {
				}
				if (line >= 0) {
					if (!U.findAndShowPageListPage(ep, fn, line)) {
						openFile(fn, line, ep);
					}
					return true;
				}
			}
		}
		return false;
	}

	public static boolean gotoFileLine2(EditPanel ep, String sb, String title)
			throws Exception {
		int p2;
		if ((p2 = sb.indexOf(":")) >= 0) {
			int line = -1;
			try {
				line = Integer.parseInt(sb.substring(0, p2));
			} catch (Exception e) {
			}
			if (line >= 0) {
				if (!U.findAndShowPageListPage(ep, title, line)) {
					openFile(title, line, ep);
				}
				return true;
			}
		}
		return false;
	}

	private static String guessByBOM(byte[] src) {
		for (Object[] row : BOMS) {
			int[] seq = (int[]) row[0];
			// compare 2 array
			if (seq.length > src.length)
				continue;
			boolean same = true;
			for (int i = 0; i < seq.length; i++) {
				if ((byte) seq[i] != src[i]) {
					same = false;
					break;
				}
			}
			if (same)
				return (String) row[1];
		}
		return null;
	}

	static void guessComment(PlainPage page) {
		List<String> comment = new ArrayList<String>();
		String[] commentchars = { "#", "%", "'", "//", "!", ";", "--", "/*",
				"<!--" };
		int[] cnts = new int[commentchars.length];
		for (int i = 0; i < page.pageData.roLines.getLinesize(); i++) {
			RoSb sb = page.pageData.roLines.getline(i);
			for (int j = 0; j < cnts.length; j++) {
				if (sb.substring(0, Math.min(sb.length(), 80)).toString()
						.trim().startsWith(commentchars[j])) {
					cnts[j]++;
				}
			}
		}
		int kind = 0;
		int max = 0;
		for (int j = 0; j < cnts.length; j++) {
			if (cnts[j] > 0) {
				kind++;
				max = Math.max(max, cnts[j]);
			}
		}
		if (kind == 1) {
			for (int j = 0; j < cnts.length; j++) {
				if (cnts[j] > 0) {
					comment.add(commentchars[j]);
					break;
				}
			}
		} else {
			int lv2 = Math.max(5, max / 10);
			for (int j = 0; j < cnts.length; j++) {
				if (cnts[j] > lv2) {
					comment.add(commentchars[j]);
				}
			}
		}
		if (comment.isEmpty()) {
			comment = null;
			page.ui.message("no comment found");
		} else {
			page.ui.message("comment found:" + comment);
		}
		page.ui.comment = comment == null ? null : comment
				.toArray(new String[comment.size()]);
		page.uiComp.repaint();
	}

	static String guessEncoding(String fn) throws Exception {
		// S/ystem.out.println("guessing encoding");
		String[] encodings = { UTF8, "utf-8", "sjis", "gbk", "unicode",
				"euc-jp", "gb2312" };

		FileInputStream in = new FileInputStream(fn);
		final int defsize = 4096 * 2;
		int len = Math.min(defsize, (int) new File(fn).length());
		try {
			byte[] buf = new byte[len];
			len = in.read(buf);
			in.close();
			if (len != defsize) {
				byte[] b2 = new byte[len];
				System.arraycopy(buf, 0, b2, 0, len);
				buf = b2;
			}
			String encoding = guessByBOM(buf);
			if (encoding != null)
				return encoding;
			for (String enc : encodings) {
				String s = new String(buf, enc);
				if (s.toLowerCase().indexOf(enc.toLowerCase()) >= 0)
					return enc;
			}
			for (String enc : encodings) {
				String s = new String(buf, enc);
				if (new String(s.getBytes(enc), enc).equals(s)
						&& s.indexOf("�") < 0) {
					return enc;
				}
			}
		} finally {
			in.close();
		}

		return null;
	}

	static String guessEncodingForEditor(String fn) {
		try {
			String s = guessEncoding(fn);
			if (s == null) {// unknow
				s = UTF8;
			}
			return s;
		} catch (Exception e) {
			return UTF8;
		}
	}

	static String guessLineSepForEditor(String fn) {
		try {
			// S/ystem.out.println("guessing encoding");
			FileInputStream in = new FileInputStream(fn);
			final int defsize = 4096;
			int len = Math.min(defsize, (int) new File(fn).length());
			try {
				// S/ystem.out.println("len:" + len);
				byte[] buf = new byte[len];
				len = in.read(buf);
				// S/ystem.out.println("len2:" + len);
				if (len != defsize) {
					byte[] b2 = new byte[len];
					System.arraycopy(buf, 0, b2, 0, len);
					buf = b2;
				}
				return new String(buf, "iso8859-1").indexOf("\r\n") >= 0 ? "\r\n"
						: "\n";
			} finally {
				in.close();
			}
		} catch (Exception e) {
			return "\n";
		}

	}

	static boolean isAllDigital(String s) {
		for (char c : s.toCharArray()) {
			if (!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}

	static boolean isImageFile(File f) {
		String fn = f.getName().toLowerCase();
		return (fn.endsWith(".gif") || fn.endsWith(".jpg")
				|| fn.endsWith(".png") || fn.endsWith(".bmp"));
	}

	static boolean isSkipChar(char ch, char ch1) {
		if (Character.isSpaceChar(ch1) || ch1 == '\t') {
			return Character.isSpaceChar(ch) || ch == '\t';
		} else {
			return Character.isJavaIdentifierPart(ch);
		}
	}

	static String km(long v) {
		float m = 1024 * 1024f;
		if (v > m) {
			return String.format("%.1fMB", v / m);
		} else if (v > 1024) {
			return String.format("%.1fKB", v / 1024f);
		}
		return "" + v;
	}

	public static void launch(String s) throws Exception {
		s = s.trim();
		String slo = s.toLowerCase();
		Desktop dt = Desktop.getDesktop();
		if (slo.startsWith("mailto:")) {
			dt.mail(new URI(s));
		} else if (slo.startsWith("http://") || slo.startsWith("https://")) {
			dt.browse(new URI(s));
		} else {
			dt.open(new File(s));
		}
	}

	static void listDir(PlainPage page, int atLine) throws Exception {
		String line = page.pageData.roLines.getline(atLine).toString();
		String fn = line.trim();
		int p1 = fn.indexOf('|');
		if (p1 >= 0)
			fn = fn.substring(0, p1).trim();
		File f = new File(fn);
		if (f.isFile() && f.exists()) {
			openFile(fn, 0, page.uiComp);
		} else if (f.isDirectory()) {
			File[] fs = f.listFiles();
			page.cx = line.length();
			page.ptEdit.insertString("\n{-----");
			for (File f1 : fs) {
				if (f1.isDirectory()) {
					page.ptEdit.insertString("\n" + f1.getAbsolutePath()
							+ " | <DIR>");
				} else {
					page.ptEdit.insertString("\n" + f1.getAbsolutePath());
				}
			}
			page.ptEdit.insertString("\n-----}");
		}
	}

	public static void listFonts(PlainPage pp) throws Exception {
		PlainPage p2 = new PlainPage(pp.uiComp, PageData.newEmpty(String
				.format("<Fonts>")));
		p2.pageData.workPath = pp.pageData.workPath;
		p2.ui.applyColorMode(pp.ui.colorMode);
		List<StringBuffer> sbs = new ArrayList<StringBuffer>();
		String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames();
		for (String font : fonts) {
			sbs.add(new StringBuffer("set-font:" + font));
		}
		p2.pageData.setLines(sbs);

	}

	static void loadTabImage() throws Exception {
		BufferedImage img = ImageIO.read(U.class
				.getResourceAsStream("/icontab.png"));
		tabImg = img.getScaledInstance(40, 8, Image.SCALE_SMOOTH);
		tabImgPrint = img.getScaledInstance(20, 8, Image.SCALE_SMOOTH);
	}

	public static Commands mappingToCommand(KeyEvent env) {
		int kc = env.getKeyCode();
		String name = "" + (char) kc;
		if (env.isAltDown()) {
			name = "A" + name;
		}
		if (env.isControlDown()) {
			name = "C" + name;
		}
		// if (env.isShiftDown()) {
		// name = "S" + name;
		// }
		Commands cmd = keys.get(name);
		return cmd;
	}

	public static int maxWidth(List<Object[]> msgs, Graphics2D g, Font font) {
		int max = 0;
		for (int i = 0; i < msgs.size(); i++) {
			Object[] row = msgs.get(i);
			int w1 = (Integer) row[2];
			if (w1 == -1) {
				w1 = g.getFontMetrics(font).stringWidth(row[0].toString());
				row[2] = w1;
			}
			if (w1 > max)
				max = w1;
		}
		return max;
	}

	static PlainPage openFile(File f, EditPanel ep) throws Exception {
		if (isImageFile(f)) {
			new PicView(ep).show(f);
			return null;
		} else {
			if (ep == null)
				return null;// ignore
			if (findAndShowPageListPage(ep, f.getCanonicalPath()))
				return ep.getPage();
			return new PlainPage(ep, PageData.newFromFile(f.getCanonicalPath()));
		}
	}

	static void openFile(PlainPage page) throws Exception {
		// JFileChooser chooser = new JFileChooser();
		//
		// if (page.pageData.getFn() != null) {
		// chooser.setSelectedFile(new File(page.pageData.getFn()));
		// } else if (page.pageData.workPath != null) {
		// chooser.setSelectedFile(new File(page.pageData.workPath));
		// // check later:cannot set correctly
		// }
		// int returnVal = chooser.showOpenDialog(page.uiComp);
		// if (returnVal == JFileChooser.APPROVE_OPTION) {
		// System.out.println("You chose to open this file: "
		// + chooser.getSelectedFile().getAbsolutePath());
		// File f = chooser.getSelectedFile();
		// openFile(f, page.uiComp);
		// }
		String dir = page.pageData.workPath;
		if (dir == null)
			dir = new File(".").getCanonicalPath();
		String title = "[Dir]" + dir;
		PageData pd = PageData.dataPool.get(title);
		if (pd == null) {
			pd = PageData.newEmpty(title);
			pd.setText(dir);
			PlainPage pp = new PlainPage(page.uiComp, pd);
			U.listDir(pp, 0);
		} else {
			EditPanel ep = page.uiComp;
			if (!U.findAndShowPageListPage(ep, title)) {
				new PlainPage(page.uiComp, pd);
			}
		}

	}

	static void openFile(String title, int line, EditPanel ep) throws Exception {
		File f = new File(title);
		if (isImageFile(f)) {
			new PicView().show(f);
			return;
		}
		if (findAndShowPageListPage(ep, title))
			return;
		PageData pd = PageData.dataPool.get(title);
		// including titles not saved
		if (pd == null)
			pd = PageData.newFromFile(f.getCanonicalPath());
		final PlainPage page = new PlainPage(ep, pd);
		if (page != null && page.pageData.lines.size() > 0) {
			line -= 1;
			page.cx = 0;
			page.cy = Math.max(0,
					Math.min(line, page.pageData.lines.size() - 1));
			page.sy = Math.max(0, page.cy - 3);
			page.uiComp.repaint();
		}
	}

	static void openFileHistory(EditPanel ep) throws Exception {
		File fhn = getFileHistoryName();
		PlainPage page = new PlainPage(ep, PageData.newFromFile(fhn
				.getCanonicalPath()));
		page.cy = Math.max(0, page.pageData.lines.size() - 1);
		page.sy = Math.max(0, page.cy - 5);
		page.uiComp.repaint();
	}

	static void paintNoise(Graphics2D g2, Dimension dim) {
		int cnt = 1000;
		int w = dim.width;
		int h = dim.height;
		int cs = 0xffffff;
		for (int i = 0; i < cnt; i++) {
			int x = random.nextInt(w);
			int y = random.nextInt(h);
			g2.setColor(new Color(random.nextInt(cs)));
			g2.drawLine(x, y, x + 1, y);
		}
	}

	public static int randomID() {
		return (int) (System.currentTimeMillis() % 1000000);
	}

	static void readFile(PageData data, String fn) {
		data.isCommentChecked = false;
		if (data.encoding == null) {
			data.encoding = U.guessEncodingForEditor(fn);
		}
		data.lineSep = U.guessLineSepForEditor(fn);
		data.setLines(U.readFileForEditor(fn, data.encoding));
		File f = new File(fn);
		data.fileLastModified = f.lastModified();
		data.workPath = f.getParent();
	}

	static List<StringBuffer> readFileForEditor(String fn, String encoding) {
		try {
			List<StringBuffer> lines = new ArrayList<StringBuffer>();
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						new FileInputStream(fn), encoding));
				String line;
				while ((line = in.readLine()) != null) {
					lines.add(new StringBuffer(line));
				}
				in.close();
			} catch (OutOfMemoryError e) {
				lines = new ArrayList<StringBuffer>();
				lines.add(new StringBuffer(e.toString()));
				return lines;
			} catch (Throwable e1) {
				lines.add(new StringBuffer(e1.toString()));
			}
			if (lines.isEmpty()) {
				lines.add(new StringBuffer());
			}
			return lines;
		} catch (OutOfMemoryError e) {
			List<StringBuffer> lines = new ArrayList<StringBuffer>();
			lines.add(new StringBuffer(e.toString()));
			return lines;
		}
	}

	static void reloadWithEncodingByUser(String fn, PlainPage pp) {
		if (fn == null) {
			pp.ui.message("file not saved.");
			return;
		}
		setEncodingByUser(pp, "Reload with Encoding:");
		readFile(pp.pageData, fn);
	}

	static String removeAsciiZero(String s) {
		int cnt = 0;
		char zero = (char) 0;
		String zeros = "" + zero;
		int p = s.indexOf(zero);
		if (p < 0)
			return s;
		StringBuffer sb = new StringBuffer(s);
		while (p >= 0) {
			sb.deleteCharAt(p);
			cnt++;
			p = sb.indexOf(zeros, p);
		}
		System.out.println("removed " + cnt + " NULL char");
		return sb.toString();
	}

	static String removeTailR(String s) {
		while (s.endsWith("\r")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	static void removeTrailingSpace(PageData data) {
		for (int i = 0; i < data.roLines.getLinesize(); i++) {
			RoSb sb = data.roLines.getline(i);
			int p = sb.length() - 1;
			while (p >= 0 && "\r\n\t ".indexOf(sb.charAt(p)) >= 0) {
				p--;
			}
			if (p < sb.length() - 1) {
				data.editRec.deleteInLine(i, p + 1, sb.length());
			}
		}
	}

	static void repaintAfter(final long t, final JComponent edit) {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(t);
					edit.repaint();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	static Point replace(PlainPage page, String s, int x, int y, String s2,
			boolean all, boolean ignoreCase) {
		int cnt = 0;
		U.BasicEdit editRec = page.pageData.editRec;
		if (ignoreCase) {
			s = s.toLowerCase();
		}
		// first half row
		int p1 = x;
		while (true) {
			p1 = page.pageData.roLines.getline(y).toString(ignoreCase)
					.indexOf(s, p1);
			if (p1 >= 0) {
				cnt++;
				editRec.deleteInLine(y, p1, p1 + s.length());
				editRec.insertInLine(y, p1, s2);
				if (!all) {
					return new Point(p1 + s2.length(), y);
				}
				p1 = p1 + s2.length();
			} else {
				break;
			}
		}
		// middle rows
		int fy = y;
		for (int i = 0; i < page.pageData.roLines.getLinesize() - 1; i++) {
			fy += 1;
			if (fy >= page.pageData.roLines.getLinesize()) {
				fy = 0;
			}
			p1 = 0;
			while (true) {
				p1 = page.pageData.roLines.getline(fy).toString(ignoreCase)
						.indexOf(s, p1);
				if (p1 >= 0) {
					cnt++;
					editRec.deleteInLine(fy, p1, p1 + s.length());
					editRec.insertInLine(fy, p1, s2);
					if (!all) {
						return new Point(p1 + s2.length(), fy);
					}
					p1 = p1 + s2.length();
				} else {
					break;
				}
			}
		}
		// last half row
		fy += 1;
		if (fy >= page.pageData.roLines.getLinesize()) {
			fy = 0;
		}
		p1 = 0;
		while (true) {
			p1 = page.pageData.roLines.getline(fy).toString(ignoreCase)
					.substring(0, x).indexOf(s, p1);
			if (p1 >= 0) {
				cnt++;
				editRec.deleteInLine(fy, p1, p1 + s.length());
				editRec.insertInLine(fy, p1, s2);
				if (!all) {
					return new Point(p1 + s2.length(), fy);
				}
				p1 = p1 + s2.length();
			} else {
				break;
			}
		}
		if (cnt > 0) {
			page.ui.message("replaced " + cnt + " places");
			return new Point(x, y);
		} else {
			return null;
		}
	}

	static void runScript(final PlainPage ppTarget) throws Exception {
		final JFrame sf = new JFrame("Javascript");
		sf.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		JPanel p = new JPanel();
		sf.getContentPane().add(p);
		SimpleLayout s = new SimpleLayout(p);
		String sample = "var i=0; \nfunction run(s,cur,max){\nreturn s;\n}";
		final EditPanel ed = new EditPanel();// single window for js, because
												// has 2 more buttons
		final PlainPage ppJs = new PlainPage(ed, PageData.newEmpty("js for "
				+ ppTarget.pageData.getTitle() + " #" + randomID()));
		ppJs.pageData.setText(sample);
		ed.frame = sf;
		ppJs.pageData.workPath = ppTarget.pageData.workPath;
		s.add(ppJs.uiComp);
		s.newline();
		JButton jb1 = new JButton("run");
		JButton jb2 = new JButton("close");
		s.add(jb1);
		s.add(jb2);
		s.newline();
		jb1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					ed.grabFocus();
					List<StringBuffer> newLines = JS.run(
							ppTarget.pageData.lines, getText(ppJs));
					PlainPage ppResult = new PlainPage(ppTarget.uiComp,
							PageData.newEmpty("js result for "
									+ ppTarget.pageData.getTitle() + " #"
									+ randomID()));
					ppResult.pageData.workPath = ppTarget.pageData.workPath;
					ppResult.pageData.setLines(newLines);
					ppResult.ui.applyColorMode(ppTarget.ui.colorMode);
				} catch (Exception e1) {
					System.out.println(e1);
					String s1 = "" + e1;
					String expect = "javax.script.ScriptException: sun.org.mozilla.javascript.internal.EvaluatorException:";
					if (s1.startsWith(expect))
						s1 = s1.substring(expect.length());
					ppJs.ptEdit.append("\n//Error:" + s1 + "\n");
					ed.repaint();
				}

			}
		});
		jb2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (ppJs.pageData.history.size() != 0
						&& JOptionPane.YES_OPTION != JOptionPane
								.showConfirmDialog(ed,
										"Are you sure to close?",
										"Changes made",
										JOptionPane.YES_NO_OPTION)) {
					ed.grabFocus();
					return;
				}
				sf.dispose();
			}
		});
		setFrameSize(sf, 800, 600);
		sf.setVisible(true);
	}

	static void saveAs(PlainPage page) throws Exception {
		EditPanel editor = page.uiComp;
		JFileChooser chooser = new JFileChooser(page.pageData.getFn());
		int returnVal = chooser.showSaveDialog(editor);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String fn = chooser.getSelectedFile().getAbsolutePath();
			if (new File(fn).exists()
					&& JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
							editor, "file exists, are you sure to overwrite?",
							"save as...", JOptionPane.YES_NO_OPTION)) {
				page.ui.message("not renamed");
				return;
			}
			page.pageData.setFn(fn);
			U.saveFileHistory(fn, page.cy+1);
			editor.changeTitle();
			page.ui.message("file renamed");
			savePageToFile(page);
		}
	}

	static boolean saveFile(PlainPage page) throws Exception {
		if (page.changedOutside
				&& JOptionPane.YES_OPTION != JOptionPane
						.showConfirmDialog(
								page.uiComp,
								"File Changed Outside!! Do you really want to overwrite it?",
								"File Changed Outside!!",
								JOptionPane.YES_NO_OPTION)) {
			page.ui.message("saved canceled");
			return false;
		}

		if (page.pageData.getFn() == null) {
			JFileChooser chooser = new JFileChooser(page.pageData.workPath);
			int returnVal = chooser.showSaveDialog(page.uiComp);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				page.isCommentChecked = false;
				String fn = chooser.getSelectedFile().getAbsolutePath();
				if (new File(fn).exists()
						&& JOptionPane.YES_OPTION != JOptionPane
								.showConfirmDialog(page.uiComp,
										"Are you sure to overwrite?",
										"File exists",
										JOptionPane.YES_NO_OPTION)) {
					page.ui.message("saved canceled");
					return false;
				}
				page.pageData.setFn(fn);
				page.uiComp.changeTitle();
				U.saveFileHistory(fn, page.cy+1);
			} else {
				return false;
			}
		}
		return savePageToFile(page);

	}

	static void saveFileHistory(String fn, int line) throws IOException {
		File fhn = getFileHistoryName();
		if (fhn.getAbsoluteFile().equals(new File(fn).getAbsoluteFile()))
			return;
		OutputStream out = new FileOutputStream(fhn, true);
		out.write(String.format("\n%s|%s:", fn, line).getBytes("utf8"));
		out.close();
	}

	static boolean savePageToFile(PlainPage page) throws Exception {
		System.out.println("save " + page.pageData.getFn());
		if (page.pageData.encoding == null) {
			page.pageData.encoding = UTF8;
		}
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(page.pageData.getFn()),
				page.pageData.encoding));
		for (int i = 0; i < page.pageData.roLines.getLinesize(); i++) {
			out.write(page.pageData.roLines.getline(i).toString());
			out.write(page.pageData.lineSep);
		}
		out.close();
		page.pageData.fileLastModified = new File(page.pageData.getFn())
				.lastModified();
		page.changedOutside = false;
		return true;
	}

	static void scale(int amount, Paint ui) {
		if (amount > 0) {
			ui.scalev *= 1.1f;
		} else if (amount < 0) {
			ui.scalev *= 0.9f;
		}
	}

	static void setClipBoard(String s) {
		Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new StringSelection(s), null);
	}

	static void setEncodingByUser(PlainPage plainPage, String msg) {
		String s = JOptionPane.showInputDialog(plainPage.uiComp, msg,
				plainPage.pageData.encoding);
		if (s == null) {
			return;
		}
		try {
			"a".getBytes(s);
		} catch (Exception e) {
			plainPage.ui.message("bad encoding:" + s);
			return;
		}
		plainPage.pageData.encoding = s;
	}

	public static void setFont(PlainPage pp, String font) {
		Font f = new Font(font, Font.PLAIN, 12);
		for (PlainPage p : pp.uiComp.pageSet) {
			p.ui.font = f;
		}
		showSelfDispMessage(pp, "set font:" + font, 3000);
	}

	static void setFrameSize(JFrame f, int w, int h) {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		f.setSize(Math.min(800, Math.min(dim.width, w)),
				Math.min(600, Math.min(dim.height, h)));
	}

	static void showHelp(final Paint ui, final EditPanel uiComp) {
		if (ui.aboutImg != null)
			return;
		new Thread() {
			@Override
			public void run() {
				try {
					int w = uiComp.getWidth();
					int h = 60;
					ui.aboutImg = new BufferedImage(w, h,
							BufferedImage.TYPE_INT_ARGB);
					Graphics2D gi = ui.aboutImg.createGraphics();
					gi.setColor(Color.BLUE);
					gi.fillRect(0, 0, w, h);
					gi.setColor(Color.CYAN);
					gi.setFont(new Font("Arial", Font.BOLD, 40));
					gi.drawString("NeoeEdit", 6, h - 20);
					gi.setColor(Color.YELLOW);
					gi.setFont(new Font("Arial", Font.PLAIN, 16));
					String url = "http://code.google.com/p/neoeedit/";
					gi.drawString("visit " + url
							+ " for more info.(url copied)", 6, h - 6);
					setClipBoard(url);
					gi.dispose();
					ui.aboutY = -h;
					ui.aboutOn = true;
					for (int i = -h; i <= 0; i++) {
						ui.aboutY = i;
						uiComp.repaint();
						Thread.sleep(500 / h);
					}
					Thread.sleep(2000);
					for (int i = 0; i >= -h; i--) {
						ui.aboutY = i;
						uiComp.repaint();
						Thread.sleep(500 / h);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					ui.aboutOn = false;
					ui.aboutImg = null;
				}
			}
		}.start();

	}

	public static void showHexOfString(String s, PlainPage pp) throws Exception {
		PlainPage p2 = new PlainPage(pp.uiComp, PageData.newEmpty(String
				.format("Hex for String #%s", randomID())));
		p2.pageData.workPath = pp.pageData.workPath;
		p2.ui.applyColorMode(pp.ui.colorMode);
		List<StringBuffer> sbs = new ArrayList<StringBuffer>();
		sbs.add(new StringBuffer(String.format("Hex for '%s'", s)));
		for (char c : s.toCharArray()) {
			sbs.add(new StringBuffer(c + ":" + UnicodeFormatter.charToHex(c)));
		}
		p2.pageData.setLines(sbs);
	}

	public static void showPageListPage(EditPanel ep) throws Exception {
		if (findAndShowPageListPage(ep, titleOfPages(ep))) {
			ep.getPage().pageData.setLines(getPageListStrings(ep));// refresh
			ep.repaint();
			return;
		}
		// boolean isFirstTime = !PageData.dataPool.containsKey(TITLE_OF_PAGES);
		PageData pd = PageData.newEmpty(titleOfPages(ep));
		new PlainPage(ep, pd);
		pd.setLines(getPageListStrings(ep));
		ep.repaint();
	}

	static void showResult(PlainPage pp, List<String> all, String type,
			String name, String text) throws Exception {
		PlainPage p2 = new PlainPage(pp.uiComp, PageData.newEmpty(String
				.format("(%s)'%s' in %s %s #%s", all.size(), text, type, name,
						randomID())));
		p2.pageData.workPath = pp.pageData.workPath;
		p2.ui.applyColorMode(pp.ui.colorMode);
		List<StringBuffer> sbs = new ArrayList<StringBuffer>();
		sbs.add(new StringBuffer(String.format(
				"find %s results in %s for '%s'", all.size(), name, text)));
		for (Object o : all) {
			sbs.add(new StringBuffer(o.toString()));
		}
		p2.pageData.setLines(sbs);
		if (type.equals("file")) {
			p2.searchResultOf = name;
		}
		// gc();
	}

	public static void showSelfDispMessage(PlainPage pp, String msg,
			int disapearMS) {
		long now = System.currentTimeMillis();
		pp.ui.msgs
				.add(new Object[] { msg, now + disapearMS, -1 /* draw width */});
		// System.out.println("add msgs:"+pp.ui.msgs.size());
		repaintAfter(4000, pp.uiComp);
	}

	static void sort(List<PlainPage> pageSet) {
		Collections.sort(pageSet, new Comparator<PlainPage>() {
			@Override
			public int compare(PlainPage o1, PlainPage o2) {
				return o1.pageData.getTitle().compareTo(o2.pageData.getTitle());
			}
		});
	}

	static String spaces(int cx) {
		if (cx <= 0)
			return "";
		StringBuffer sb = new StringBuffer(cx);
		sb.setLength(cx);
		for (int i = 0; i < cx; i++)
			sb.setCharAt(i, ' ');
		return sb.toString();
	}

	static List<String> split(String s) {
		StringBuffer sb = new StringBuffer();
		List<String> sl = new ArrayList<String>();
		for (char c : s.toCharArray()) {
			if (!Character.isJavaIdentifierPart(c)) {
				if (sb.length() > 0) {
					sl.add(sb.toString());
					sb.setLength(0);
				}
				sl.add("" + c);
			} else {
				sb.append(c);
			}
		}
		if (sb.length() > 0) {
			sl.add(sb.toString());
			sb.setLength(0);
		}
		return sl;
	}

	static String[] splitLine(String s) {
		String sep = "\n";
		List<String> s1 = new ArrayList<String>();
		int p1 = 0;
		while (true) {
			int p2 = s.indexOf(sep, p1);
			if (p2 < 0) {
				String s2 = U.removeTailR(s.substring(p1));
				if (s2.indexOf('\r') >= 0) {
					String[] ss2 = s2.split("\\r");
					for (String ss : ss2)
						s1.add(ss);
				} else {
					s1.add(s2);
				}
				break;
			} else {
				String s2 = U.removeTailR(s.substring(p1, p2));
				if (s2.indexOf('\r') >= 0) {
					String[] ss2 = s2.split("\\r");
					for (String ss : ss2)
						s1.add(ss);
				} else {
					s1.add(s2);
				}
				p1 = p2 + 1;
			}
		}
		return s1.toArray(new String[s1.size()]);
	}

	static void startNoiseThread(final Paint ui, final EditPanel uiComp) {
		Thread t = new Thread() {
			@Override
			public void run() {
				try {// noise thread
					while (true) {
						if (ui.noise && !ui.closed) {
							uiComp.repaint();
							// System.out.println("paint noise");
							Thread.sleep(ui.noisesleep);
						} else {
							break;
						}
					}
					System.out.println("noise stopped");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	static int strWidth(Graphics2D g2, String s, int TABWIDTH) {
		if (s.indexOf("\t") < 0) {
			return g2.getFontMetrics().stringWidth(s);
		} else {
			int w = 0;
			int p1 = 0;
			while (true) {
				int p2 = s.indexOf("\t", p1);
				if (p2 < 0) {
					w += g2.getFontMetrics().stringWidth(s.substring(p1));
					break;
				} else {
					w += g2.getFontMetrics().stringWidth(s.substring(p1, p2));
					w += TABWIDTH;
					p1 = p2 + 1;
				}
			}
			return w;
		}
	}

	static String subs(RoSb sb, int a, int b) {
		return subs(sb.toString(), a, b);
	}

	static String subs(String sb, int a, int b) {
		if (a >= b) {
			return "";
		}
		if (a >= sb.length()) {
			return "";
		}
		if (a < 0 || b < 0) {
			return "";
		}
		if (b > sb.length()) {
			b = sb.length();
		}
		return sb.substring(a, b);
	}

	public static void switchPageInOrder(PlainPage pp) {
		List<PlainPage> pps = pp.uiComp.pageSet;
		if (pps.size() <= 1)
			return;
		int i = (1 + pps.indexOf(pp)) % pps.size();
		pp.uiComp.setPage(pps.get(i));
		pp.uiComp.repaint();
	}

	public static void switchToPageListPage(PlainPage pp) throws Exception {
		EditPanel uiComp = pp.uiComp;
		if (pp.pageData.getTitle().equals(U.titleOfPages(uiComp))
				&& uiComp.lastPage != null) {
			if (uiComp.pageSet.contains(uiComp.lastPage)) {
				uiComp.setPage(uiComp.lastPage);
			} else {
				uiComp.lastPage = null;
			}
		} else {
			uiComp.lastPage = uiComp.getPage();
			showPageListPage(uiComp);
		}
	}

	static String titleOfPages(EditPanel ep) {
		return _TITLE_OF_PAGES + "@" + ep.hashCode();
	}

	static String trimLeft(String s) {
		int i = 0;
		while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t'))
			i++;
		return i > 0 ? s.substring(i) : s;
	}
}
