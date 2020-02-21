package neoe.ne;

import java.awt.BorderLayout;
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
import java.awt.RenderingHints;
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
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import neoe.ne.PlainPage.Paint;
import neoe.ne.Plugin.PluginAction;
import neoe.ne.util.FileIterator;
import neoe.ne.util.FileUtil;
import neoe.ne.util.PyData;

/**
 * Trivial static methods.
 */
public class U {

	public static class LocationHistory<E> {

		LinkedList<E> his = new LinkedList<E>();
		int pos = 0;

		public E back(E updateCurrent) {
			// System.out.printf("his.back size=%s, pos=%s\n", his.size(), pos);
			if (pos > 0) {
				his.set(pos, updateCurrent);
				pos--;
				return his.get(pos);
			} else {
				return null;
			}
		}

		public E forward(E updateCurrent) {
			// System.out.printf("his.forward size=%s, pos=%s\n", his.size(),
			// pos);
			if (pos < his.size() - 1) {
				his.set(pos, updateCurrent);
				pos++;
				return his.get(pos);
			} else {
				return null;
			}
		}

		public void add(E loc, E updateCurrent) {

			if (his.size() > pos + 1) {
				removeLastN(his.size() - pos - 1);
			}
			// String last = "<Empty>";
			if (!his.isEmpty()) {
				// last = his.getLast().toString();
				his.set(his.size() - 1, updateCurrent);
				// System.out.println("[d]" + last + "=>" + updateCurrent);
			}

			his.add(loc);
			pos = his.size() - 1;
			// System.out.printf("his.add size=%s, pos=%s\n", his.size(), pos);
		}

		private void removeLastN(int cnt) {
			for (int i = 0; i < cnt; i++)
				his.removeLast();
		}

	}

	static void drawStringShrink(Graphics2D g2, Font[] fontList, String s, int x, int y, float maxWidth) {
		int width = stringWidth(g2, fontList, s);
		int max = Math.round(maxWidth);
		if (width <= max) {
			drawString(g2, fontList, s, x, y);
		} else {
			Graphics2D g3 = (Graphics2D) g2.create();
			g3.scale((maxWidth - 3) / (float) width, 1);
			drawString(g3, fontList, s, x, y);
			g3.dispose();
		}
	}

	static String suNotice() {
		String user = System.getProperty("user.name");
		if ("root".equals(user) || "administrator".equalsIgnoreCase(user)) {
			return " [su]";
		} else {
			return "";
		}
	}

	public static int drawString(Graphics2D g2, Font[] fonts, String s, int x, int y) {
		return drawString(g2, fonts, s, x, y, false, 0);
	}

	/** no TAB needed to care here */
	public static int drawString(Graphics2D g2, Font[] fonts, String s, int x, int y, boolean isCurrentLine,
			int lineHeight) {
		if (s == null || s.length() <= 0) {
			return 0;
		}

		// draw separated by fonts
		int w = 0;
		Font cf = fonts[0];
		StringBuilder sb = new StringBuilder();
		int w1 = 0;
		int i = 0;
		Font[] fo = new Font[1];
		while (i < s.length()) {
			char c = s.charAt(i);
			int w0 = charWidth(g2, fonts, c, fo);
			if (cf.equals(fo[0])) {
				sb.append(c);
				w1 += w0;
			} else {
				submitStr(g2, cf, sb.toString(), x, y, isCurrentLine, lineHeight, w1);
				x += w1;
				w += w1;
				w1 = w0;
				sb.setLength(0);
				sb.append(c);
				cf = fo[0];
			}
			i++;
		}
		if (sb.length() > 0) {
			submitStr(g2, cf, sb.toString(), x, y, isCurrentLine, lineHeight, w1);
			w += w1;
		}

		return w;
	}

	private static void submitStr(Graphics2D g2, Font cf, String s, int x, int y, boolean isCurrentLine, int lineHeight,
			int w) {
		if (s.isEmpty())
			return;
		if (isCurrentLine && w > 0) {
			Color c2 = g2.getColor();
			Gimp.drawString(g2, x, y, lineHeight, s, c2, cf, w);
		} else {
			g2.setFont(cf);
			g2.drawString(s, x, y);
		}
	}

	static Map<Character, Object[]/* Font, Integer */> charWidthCaches = new HashMap();
	static Object[][] charWidthCaches256 = new Object[256][];

	/**
	 * use first font, if cannot display character in that font , use second, and so
	 * on
	 */
	public static int stringWidth(Graphics2D g2, Font[] fonts, String s0) {
		if (s0 == null || s0.length() <= 0) {
			return 0;
		}
		int w = 0;
		int len = s0.length();
		for (int i = 0; i < len; i++) {
			char c = s0.charAt(i);
			if (c == '\t')
				w += TAB_WIDTH;
			else
				w += charWidth(g2, fonts, c, null);
		}
		return w;
	}

	static int TAB_WIDTH = 20;

	public static int charWidth(Graphics2D g2, Font[] fonts, char c) {
		// for compact with IME interface
		return charWidth(g2, fonts, c, null);
	}

	public static int charWidth(Graphics2D g2, Font[] fonts, char c, Font[] fo) {
		Object[] row = c < 256 ? charWidthCaches256[c] : charWidthCaches.get(c);
		if (row == null) {
			row = genCharWidthCaches(g2, c, fonts);
			if (c < 256) {
				charWidthCaches256[c] = row;
			} else {
				charWidthCaches.put(c, row);
			}
		}
		if (fo != null)
			fo[0] = (Font) row[0];
		return (Integer) row[1];
	}

	/** match the first font can show the char */
	private static Object[] genCharWidthCaches(Graphics2D g2, char c, Font[] fonts) {
		Font f = fonts[0];
		for (Font font : fonts) {
			if (font.canDisplay(c)) {
				f = font;
				break;
			}
		}
		return new Object[] { f, g2.getFontMetrics(f).charWidth(c) };
	}

	static enum BasicAction {
		Delete, DeleteLines, Insert, MergeLine, InsertEmptyLine
	}

	static class BasicEdit {

		PageData data;
		boolean record;

		BasicEdit(boolean record, PageData data) {
			this.record = record;
			this.data = data;
		}

		// void deleteEmptyLine(int y) {
		// CharSequence sb = lines().get(y);
		// if (sb.length() > 0) {
		// throw new RuntimeException("not a empty line " + y + ":" + sb);
		// }
		// if (lines().size() > 1) {
		// lines().remove(y);
		// if (record) {
		// history().addOne(new HistoryCell(BasicAction.DeleteEmtpyLine, -1, -1,
		// y, -1, null));
		// }
		// }
		// }
		void insertEmptyLine(int y) {
			if (y > lines().size()) {
				y = lines().size();
			}
			lines().add(y, new StringBuilder());
			if (record) {
				history().addOne(new HistoryCell(BasicAction.InsertEmptyLine, -1, -1, y, -1, null));
			}
		}

		public void deleteLines(int start, int end) {
			int len = end - start;
			if (len <= 0)
				return;
			if (lines().isEmpty())
				return;
			List<CharSequence> deleted = lines().subList(start, end);
			if (record) {
				history().addOne(new HistoryCell(BasicAction.DeleteLines, start, end, 0, 0, new ArrayList(deleted)));
			}
			deleted.clear();
		}

		StringBuilder getLineSb(int y) {
			if (lines().size() == 0) {
				insertEmptyLine(y);
			}
			if (y < 0 || y >= lines().size())
				return null;
			CharSequence o = lines().get(y);

			if (o instanceof StringBuilder) {
				return (StringBuilder) o;
			}

			if (o instanceof String) {
				String str = (String) o;
				StringBuilder sb = new StringBuilder(str);
				lines().set(y, sb);
				return sb;
			}

			StringBuilder sb = new StringBuilder((CharSequence) o);
			lines().set(y, sb);
			return sb;

		}

		void deleteInLine(int y, int x1, int x2) {
			StringBuilder sb = getLineSb(y);
			if (sb == null)
				return;
			if (x1 >= sb.length()) {
				return;
			}
			x2 = Math.min(x2, sb.length());
			String d = sb.substring(x1, x2);
			if (d.length() > 0) {
				sb.delete(x1, x2);
				if (record) {
					history().addOne(new HistoryCell(BasicAction.Delete, x1, x2, y, -1, d));
				}
			}
		}

		History history() {
			return data.history;
		}

		void insertInLine(int y, int x, CharSequence s) {
			if (U.indexOf(s, '\n') >= 0 || U.indexOf(s, '\r') >= 0) {
				throw new RuntimeException("cannot contains line-seperator:[" + s + "]" + U.indexOf(s, '\n'));
			}
			if (y == data.roLines.getLinesize()) {
				data.editRec.insertEmptyLine(y);
			}
			StringBuilder sb = getLineSb(y);
			if (x > sb.length()) {
				sb.setLength(x);
			}
			sb.insert(x, s);
			if (record) {
				history().addOne(new HistoryCell(BasicAction.Insert, x, x + s.length(), y, -1, null));
			}
		}

		List<CharSequence> lines() {
			return data.lines;
		}

		void mergeLine(int y) {
			StringBuilder sb1 = getLineSb(y);
			CharSequence sb2 = lines().get(y + 1);
			int x1 = sb1.length();
			sb1.append(sb2);
			lines().remove(y + 1);
			if (record) {
				history().addOne(new HistoryCell(BasicAction.MergeLine, x1, -1, y, -1, null));
			}
		}

		void insertLines(int x1, List<CharSequence> s1) {
			if (record) {
				new RuntimeException("Bug? unpected").printStackTrace();
			}
			lines().addAll(x1, s1);
		}

	}

	public static class Config {

		public static boolean configChanged() {
			if (configFileLoadTime > 0) {
				File installed = new File(getMyDir(), Version.CONFIG_FN);
				long time = installed.lastModified();
				if (time <= configFileLoadTime) {
					return false;
				}
			}
			return true;
		}

		public static Map getConfig() throws IOException {
			if (!configChanged()) {
				return conf;
			}
			System.out.println("load " + Version.CONFIG_FN);
			BufferedReader in = new BufferedReader(U.getInstalledReader(Version.CONFIG_FN));
			Map o = null;
			try {
				o = (Map) new PyData().parseAll(in);
				conf = o;
				File installed = new File(getMyDir(), Version.CONFIG_FN);
				configFileLoadTime = installed.lastModified() + 500;
			} catch (Exception e) {
				System.err.println("cannot parse config file:" + e + ", pls fix it. use orginal config.");
				in = new BufferedReader(U.getResourceReader(Version.CONFIG_FN));
				try {
					o = (Map) new PyData().parseAll(in);
					configFileLoadTime = 0;
				} catch (Exception e1) {// still fail?bug
					e1.printStackTrace();
				}
			}
			return o;
		}

		public static Color getDefaultBgColor() throws IOException {
			Map conf = getConfig();
			Map colorConf = (Map) conf.get("color");
			String value = "" + colorConf.get("defaultBackgroundColor");
			Color c = Color.WHITE;
			if (value.startsWith("0x")) {
				c = Color.decode(value);
			} else {
				c = Color.getColor(value, c);
			}
			return c;
		}

		public static int getDefaultColorMode() {

			try {
				Map config = getConfig();
				BigDecimal v = (BigDecimal) ((Map) config.get("color")).get("defaultMode");
				if (v == null) {
					return 0;
				} else {
					return v.intValue();
				}
			} catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
		}

		public static void loadOtherConfig(EditorPanelConfig conf) throws IOException {
			Map config = getConfig();
			String v = "" + config.get("KEY_TEXT_ANTIALIASING");
			if (v.length() == 0 || "null".equals(v)) {
				return;
			}
			try {
				Field f = RenderingHints.class.getDeclaredField(v);
				Object o = f.get(null);
				if (o != null)
					conf.VALUE_TEXT_ANTIALIAS = o;
			} catch (Exception e) {
				System.out.println("cannot find in RenderingHints:" + v);
			}
		}

		public static Font[] getFont(Font[] defaultIfFail) {
			try {
				Map config = getConfig();
				Map m = (Map) config.get("font");
				Object v = m.get("list");
				if (v == null || "null".equals(v)) {
					return defaultIfFail;
				} else {
//					GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
					List localFonts = null;
					List<Font> fonts = new ArrayList<Font>();
					for (Object o : (List) v) {
						List l = (List) o;
						String fontfn = (String) l.get(0);
						File f = new File(fontfn);
						Font font = null;
						int fontsize = ((BigDecimal) l.get(1)).intValue();
						if (f.exists() && f.isFile()) {
							font = Font.createFont(Font.TRUETYPE_FONT, f);
							if (font == null) {
								System.out.println("cannot load truetype font:" + fontfn);
								continue;
							}
							System.out.println("load font file:" + fontfn);

						} else {
							if (localFonts == null) {
								localFonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
										.getAvailableFontFamilyNames());
							}
							if (localFonts.contains(fontfn)) {
								font = new Font(fontfn, Font.PLAIN, 12);
							} else {
								System.out.println("font file not exists:" + fontfn);
							}
						}
						if (font != null) {
							if (l.size() > 2 && l.get(2).equals("BOLD")) {
								font = font.deriveFont(Font.BOLD, fontsize);
							} else if (l.size() > 2 && l.get(2).equals("ITALIC")) {
								font = font.deriveFont(Font.ITALIC, fontsize);
							} else {
								font = font.deriveFont(Font.PLAIN, fontsize);
							}
							fonts.add(font);
						}
					}
					for (Font f : defaultIfFail) {
						fonts.add(f);
					}
////					if (fonts.isEmpty()) {
////						System.out.println("use default fonts.");
////						return defaultIfFail;
////					}
//					System.out.println("loaded custom fonts:" + fonts);
					return fonts.toArray(new Font[fonts.size()]);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return defaultIfFail;
			}
		}

		@SuppressWarnings("rawtypes")
		static void initKeys() throws Exception {

			Map o = getConfig();
			List o1 = (List) ((Map) o).get("keys");
			U.originKeys = o1;
			U.keys = new HashMap<String, Commands>();
			U.pluginKeys = new HashMap<String, PluginAction>();
			Set<String> keys = new HashSet<String>();
			for (Object o2 : o1) {
				List row = (List) o2;
				String cmd = row.get(0).toString();
				Object kk = row.get(1);
				if (kk instanceof List) {
					for (Object k : (List) kk) {
						String key = k.toString().toUpperCase();
						addOneKey(key, cmd, keys);
					}

				} else {
					String key = row.get(1).toString().toUpperCase();
					addOneKey(key, cmd, keys);
				}
			}
			addKey(U.keys, "alt-Enter", "ShellCommand");
		}

		private static void addOneKey(String key, String cmd, Set<String> keys) {
			if (keys.contains(key)) {
				System.err.println("Error: duplicated key:" + key);
				return;
			}
			keys.add(key);
			try {
				addKey(U.keys, key, cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public static int[][] loadColorModes() throws IOException {
			Map config = getConfig();
			List l = (List) ((Map) config.get("color")).get("modes");
			int colorCnt = 12;
			int[][] modes = new int[l.size()][colorCnt];
			for (int i = 0; i < l.size(); i++) {
				List row = (List) l.get(i);
				for (int j = 1; j <= colorCnt; j++) {
					int v;
					Object o = row.get(j);
					if (o instanceof BigDecimal) {
						v = ((BigDecimal) o).intValue();
					} else {
						v = U.parseInt(o.toString());
					}
					modes[i][j - 1] = v;
				}
			}
			return modes;
		}

		public static Point readFrameSize() {
			try {
				Map config = getConfig();
				List l = (List) config.get("frameSize");
				if (l != null) {
					return new Point(U.parseInt(l.get(0)), U.parseInt(l.get(1)));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return new Point(800, 600);
		}

		public static int readTabWidth() {
			try {
				Map config = getConfig();
				BigDecimal d = (BigDecimal) config.get("tabWidthInPixel");
				if (d != null) {
					return d.intValue();
				}
			} catch (IOException e) {
				e.printStackTrace();

			}
			return 40;
		}

		public static void setDefaultBKColor() throws IOException {
			UIDefaults uiDefaults = UIManager.getDefaults();
			for (Enumeration e = uiDefaults.keys(); e.hasMoreElements();) {
				Object obj = e.nextElement();
				if (obj instanceof String) {
					if (((String) obj).contains("background") && uiDefaults.get(obj) instanceof Color) {
						// System.out.println(obj);
						uiDefaults.put(obj, getDefaultBgColor());
						UIManager.put(obj, getDefaultBgColor());
					}
				}
			}
		}

		public static void setDefaultLookAndFeel() throws IOException {
			Map config = getConfig();
			String v = "" + config.get("lookAndFeel");
			if (v.length() == 0 || "null".equals(v)) {
				return;
			}
			try {
				Class.forName(v);
				try {
					UIManager.setLookAndFeel(v);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (ClassNotFoundException e) {
				System.out.println("not found lookAndFeel:" + e);
			}
		}

		public static Object get(String path, Object dv) throws IOException {
			Object o = get(getConfig(), path);
			if (o == null)
				return dv;
			return o;
		}

		/**
		 * xxx.[2].yyy.[0]
		 */
		public static Object get(Map config, String name) {
			String[] ss = name.split("\\.");
			Object node = config;
			Object o = null;
			for (int i = 0; i < ss.length; i++) {
				if (node == null)
					return null;
				String s = ss[i];
				if (s.startsWith("[") && s.endsWith("]")) {
					int p = Integer.parseInt(s.substring(1, s.length() - 1));
					if (node instanceof Map) {
						o = ((Map) node).values().toArray()[p];
					} else {
						o = ((List) node).get(p);
					}
				} else {
					o = ((Map) node).get(s);
				}
				node = o;
			}
			// Log.log("config["+name+"]="+o);
			return o;
		}

	}

	static class FindAndReplace {

		FindReplaceWindow findWindow;
		final private PlainPage pp;
		String text2find;
		boolean back;

		public FindAndReplace(PlainPage plainPage) {
			this.pp = plainPage;
		}

		void doFind(String text, boolean ignoreCase, boolean selected2, boolean inDir, String dir, String fnFilter,
				boolean backward) throws Exception {
			if (!inDir) {
				text2find = text;
				pp.ignoreCase = ignoreCase;
				back = backward;
				if (backward)
					findPrev();
				else
					findNext();
				pp.uiComp.repaint();
			} else {
				doFindInDir(pp, text, ignoreCase, selected2, inDir, dir, fnFilter);
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
			List<CharSequence> ss = pp.ptSelection.getSelected();

			String t = ss.isEmpty() ? "" : ss.get(0).toString();

			if (t.length() == 0 && text2find != null) {
				t = text2find;
			}
			if (findWindow == null) {
				findWindow = new FindReplaceWindow(pp.uiComp.frame, pp);
			}
			if (t.length() > 0) {
				findWindow.jta1.setText(t);
			}
			findWindow.show();
			findWindow.jta1.grabFocus();
		}

		void findPrev() {
			if (text2find != null && text2find.length() > 0) {
				Point p = find_prev(pp, text2find, pp.cx - 1, pp.cy, pp.ignoreCase);
				if (p == null) {
					pp.ui.message("string not found");
				} else {
					pp.ptSelection.selectLength(p.x, p.y, text2find.length());
				}
			}

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
		Object s1;
		int x1, x2, y1, y2;

		public HistoryCell(U.BasicAction action, int x1, int x2, int y1, int y2, Object s1) {
			super();
			this.s1 = s1;
			this.x1 = x1;
			this.x2 = x2;
			this.y1 = y1;
			this.y2 = y2;
			this.action = action;
		}

		public boolean canAppend(HistoryCell last) {
			return ((last.action == U.BasicAction.Delete && this.action == U.BasicAction.Delete && //
					((last.x1 == this.x1 || last.x1 == this.x2) && last.y1 == this.y1))//
					|| (last.action == U.BasicAction.Insert && this.action == U.BasicAction.Insert && //
							((last.x1 == this.x1 || last.x2 == this.x1) && last.y1 == this.y1)));
		}

		public void redo(PlainPage page) {
			BasicEdit editNoRec = page.pageData.editNoRec;
			ReadonlyLines roLines = page.pageData.roLines;
			boolean recCh = false;
			switch (action) {
			case Delete:
				s1 = roLines.getInLine(y1, x1, x2).toString();
				editNoRec.deleteInLine(y1, x1, x2);
				page.cursor.setSafePos(x1, y1, recCh);
				break;
			case DeleteLines:
				editNoRec.deleteLines(x1, x2);
				page.cursor.setSafePos(0, x1, recCh);
				break;
			case Insert: {
				String s1 = (String) this.s1;
				editNoRec.insertInLine(y1, x1, s1);
				page.cursor.setSafePos(x1 + s1.length(), y1, recCh);
				s1 = null;
				break;
			}
			case InsertEmptyLine:
				editNoRec.insertEmptyLine(y1);
				page.cursor.setSafePos(0, y1 + 1, recCh);
				break;
			case MergeLine:
				editNoRec.mergeLine(y1);
				page.cursor.setSafePos(x1, y1, recCh);
				break;
			default:
				throw new RuntimeException("unkown action " + action);
			}
		}

		@Override
		public String toString() {
			return "HistoryInfo [action=" + action + ", x1=" + x1 + ", x2=" + x2 + ", y1=" + y1 + ", y2=" + y2 + ", s1="
					+ s1 + "]\n";
		}

		public void undo(PlainPage page) {
			BasicEdit editNoRec = page.pageData.editNoRec;
			ReadonlyLines roLines = page.pageData.roLines;
			boolean recCh = false;
			switch (action) {
			case Delete: {
				String s1 = (String) this.s1;
				editNoRec.insertInLine(y1, x1, s1);
				page.cursor.setSafePos(x1 + s1.length(), y1, recCh);
				s1 = null;
				break;
			}
			case DeleteLines:
				editNoRec.insertLines(x1, (List<CharSequence>) s1);
				page.cursor.setSafePos(0, x2, recCh);
				break;
			case Insert:
				s1 = roLines.getInLine(y1, x1, x2).toString();
				editNoRec.deleteInLine(y1, x1, x2);
				page.cursor.setSafePos(0, y1, recCh);
				break;
			case InsertEmptyLine:
				editNoRec.deleteLines(y1, y1 + 1);
				page.cursor.setSafePos(0, y1, recCh);
				break;
			case MergeLine:
				String s2 = roLines.getInLine(y1, x1, Integer.MAX_VALUE).toString();
				editNoRec.deleteInLine(y1, x1, Integer.MAX_VALUE);
				editNoRec.insertEmptyLine(y1 + 1);
				editNoRec.insertInLine(y1 + 1, 0, s2);
				page.cursor.setSafePos(0, y1 + 1, recCh);
				break;
			default:
				throw new RuntimeException("unkown action " + action);
			}
		}
	}

	static class Print implements Printable {

		Color colorLineNumber = new Color(0x30C200), colorGutterLine = new Color(0x30C200), colorNormal = Color.BLACK,
				colorDigit = new Color(0xA8002A), colorKeyword = new Color(0x0099CC),
				colorHeaderFooter = new Color(0x8A00B8), colorComment = new Color(200, 80, 50);
		Dimension dim;
		String fn;
		Font[] fonts;

		int lineGap = 3, lineHeight = 8, headerHeight = 20, footerHeight = 20, gutterWidth = 24;// TAB_WIDTH = 20;

		int linePerPage;
		ReadonlyLines roLines;
		String title;
		int totalPage;
		Paint ui;
		EditorPanel uiComp;

		Print(PlainPage pp) {
			this.ui = pp.ui;
			this.uiComp = pp.uiComp;
			this.roLines = pp.pageData.roLines;
			this.fn = pp.pageData.getFn();
			this.title = pp.pageData.getTitle();
			this.fonts = U.fontList;
			lineHeight = fonts[0].getSize();
		}

		void drawReturn(Graphics2D g2, int w, int py) {
			g2.setColor(Color.red);
			g2.drawLine(w, py - lineHeight + fonts[0].getSize(), w + 3, py - lineHeight + fonts[0].getSize());
		}

		int drawStringLine(Graphics2D g2, Font[] fonts, String s, int x, int y) {
			int w = 0;
			int commentPos = getCommentPos(s);
			if (commentPos >= 0) {
				String s1 = s.substring(0, commentPos);
				String s2 = s.substring(commentPos);
				int w1 = drawText(g2, fonts, s1, x, y, false);
				w = w1 + drawText(g2, fonts, s2, x + w1, y, true);
			} else {
				w = drawText(g2, fonts, s, x, y, false);
			}
			return w;
		}

		int drawText(Graphics2D g2, Font[] fonts, String s, int x, int y, boolean isComment) {
			int w = 0;
			if (isComment) {
				String[] ws = s.split("\t");
				int i = 0;
				for (String s1 : ws) {
					if (i++ != 0) {
						g2.drawImage(U.tabImgPrint, x + w, y - lineHeight, null);
						w += TAB_WIDTH;
					}
					g2.setColor(colorComment);
					w += U.drawString(g2, fonts, s1, x + w, y);
					if (w > dim.width - gutterWidth) {
						break;
					}
				}
			} else {
				List<CharSequence> s1x = U.splitToken(s);
				for (CharSequence s1c : s1x) {
					String s1 = s1c.toString();
					if (s1.equals("\t")) {
						g2.drawImage(U.tabImgPrint, x + w, y - lineHeight, null);
						w += TAB_WIDTH;
					} else {
						// int highlightid =
						U.getHighLightID(s1, g2, colorKeyword, colorDigit, colorNormal);
						w += U.drawString(g2, fonts, s1, x + w, y);
					}
					if (w > dim.width - gutterWidth) {
						break;
					}
				}
			}
			return w;
		}

		void drawTextLine(Graphics2D g2, Font[] fonts, String s, int x0, int y0, int charCntInLine) {
			int w = drawStringLine(g2, fonts, s, x0, y0);
			drawReturn(g2, w + gutterWidth + 2, y0);
		}

		private int getCommentPos(String s) {
			String[] comment = ui.comment;
			if (comment == null) {
				return -1;
			}
			for (String c : comment) {
				int p = s.indexOf(c);
				if (p >= 0) {
					return p;
				}
			}
			return -1;
		}

		int getTotalPage(PageFormat pf) {
			linePerPage = ((int) pf.getImageableHeight() - footerHeight - headerHeight) / (lineGap + lineHeight);
			System.out.println("linePerPage=" + linePerPage);
			if (linePerPage <= 0) {
				return 0;
			}
			int lines = roLines.getLinesize();
			int page = (lines % linePerPage == 0) ? lines / linePerPage : lines / linePerPage + 1;
			return page;
		}

		@Override
		public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
			if (pageIndex > totalPage) {
				return Printable.NO_SUCH_PAGE;
			}
			// print
			ui.message("printing " + (pageIndex + 1) + "/" + totalPage);
			uiComp.repaint();
			Graphics2D g2 = (Graphics2D) graphics;
			g2.translate(pf.getImageableX(), pf.getImageableY());
			if (ui.noise) {
				U.paintNoise(g2, new Dimension((int) pf.getImageableWidth(), (int) pf.getImageableHeight()));
			}

			g2.setColor(colorHeaderFooter);
			U.drawString(g2, fonts, fn == null ? title : new File(fn).getName(), 0, lineGap + lineHeight);
			{
				String s = (pageIndex + 1) + "/" + totalPage;
				U.drawString(g2, fonts, s, (int) pf.getImageableWidth() - U.stringWidth(g2, fonts, s) - 2,
						lineGap + lineHeight);
				s = new Date().toString() + " - NeoeEdit";
				U.drawString(g2, fonts, s, (int) pf.getImageableWidth() - U.stringWidth(g2, fonts, s) - 2,
						(int) pf.getImageableHeight() - 2);
				g2.setColor(colorGutterLine);
				g2.drawLine(gutterWidth - 4, headerHeight, gutterWidth - 4,
						(int) pf.getImageableHeight() - footerHeight);
			}
			int p = linePerPage * pageIndex;
			int charCntInLine = (int) pf.getImageableWidth() / 5 + 5;// inaccurate
			for (int i = 0; i < linePerPage; i++) {
				if (p >= roLines.getLinesize()) {
					break;
				}
				int y = headerHeight + (lineGap + lineHeight) * (i + 1);
				g2.setColor(colorLineNumber);
				U.drawString(g2, fonts, "" + (p + 1), 0, y);
				g2.setColor(colorNormal);
				String s = roLines.getline(p++).toString();
				if (s.length() > charCntInLine) {
					s = s.substring(0, charCntInLine);
				}
				drawTextLine(g2, fonts, s, gutterWidth, y, charCntInLine);

			}

			return Printable.PAGE_EXISTS;
		}

		void printPages() {

			U.startThread(new Thread() {
				@Override
				public void run() {
					try {
						PrinterJob job = PrinterJob.getPrinterJob();
						PageFormat pf = job.pageDialog(job.defaultPage());
						totalPage = getTotalPage(pf);
						if (totalPage <= 0) {
							return;
						}
						dim = new Dimension((int) pf.getImageableWidth(), (int) pf.getImageableHeight());
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
			});
		}
	}

	static class ReadonlyLines {

		PageData data;

		ReadonlyLines(PageData data) {
			this.data = data;
		}

		CharSequence getInLine(int y, int x1, int x2) {
			CharSequence cs = getline(y);
			if (x1 < 0)
				x1 = 0;
			if (x2 > cs.length())
				x2 = cs.length();
			return getline(y).subSequence(x1, x2);
		}

		CharSequence getline(int i) {
			if (i < 0 || i >= data.lines.size())
				return "";
			return (CharSequence) data.lines.get(i);
		}

		int getLinesize() {
			return data.lines.size();
		}

		List<CharSequence> getTextInRect(Rectangle r, boolean rectSelectMode) {
			int x1 = r.x;
			int y1 = r.y;
			int x2 = r.width;
			int y2 = r.height;
			List<CharSequence> sb = new ArrayList<CharSequence>();
			if (rectSelectMode) {
				for (int i = y1; i <= y2; i++) {
					sb.add(getInLine(i, x1, x2));
				}
			} else {
				if (y1 == y2 && x1 < x2) {
					sb.add(getInLine(y1, x1, x2));
				} else if (y1 < y2) {
					sb.add(getInLine(y1, x1, Integer.MAX_VALUE));
					// for (int i = y1 + 1; i < y2; i++) {
					// sb.add(getline(i));
					// }
					sb.addAll(data.lines.subList(y1 + 1, y2));
					sb.add(getInLine(y2, 0, x2));
				}
			}
			return sb;
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

		private EditorPanel ep;

		TH(EditorPanel ep) {
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
				List<File> l = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				for (File f : l) {
					if (f.isFile()) {
						try {
							U.openFile(f, ep);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			ep.repaint();
			return true;
		}
	}

	public static class UnicodeFormatter {

		static public String byteToHex(byte b) {
			// Returns hex String representation of byte b
			char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
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

	static final Object[][] BOMS = new Object[][] { new Object[] { new int[] { 0xEF, 0xBB, 0xBF }, "UTF-8" },
			new Object[] { new int[] { 0xFE, 0xFF }, "UTF-16BE" },
			new Object[] { new int[] { 0xFF, 0xFE }, "UTF-16LE" },
			new Object[] { new int[] { 0, 0, 0xFE, 0xFF }, "UTF-32BE" },
			new Object[] { new int[] { 0xFF, 0xFE, 0, 0 }, "UTF-32LE" }, };;

	static private Map conf;
	private static long configFileLoadTime;

	static Map<String, Commands> keys;
	static Map<String, PluginAction> pluginKeys;

	public final static String[] KWS = "ArithmeticError AssertionError AttributeError BufferType BuiltinFunctionType BuiltinMethodType ClassType CodeType ComplexType DeprecationWarning DictProxyType DictType DictionaryType EOFError EllipsisType EmitStreamVertex EmitVertex EndPrimitive EndStreamPrimitive EnvironmentError Err Exception False FileType FloatType FloatingPointError FrameType FunctionType GeneratorType IOError ImportError IndentationError IndexError InstanceType IntType KeyError KeyboardInterrupt LambdaType ListType LongType LookupError MemoryError MethodType ModuleType NameError None NoneType NotImplemented NotImplementedError OSError ObjectType OverflowError OverflowWarning ReferenceError RuntimeError RuntimeWarning Self SliceType StandardError StopIteration StringType StringTypes SyntaxError SyntaxWarning SystemError SystemExit TabError TracebackType True TupleType TypeError TypeType UnboundLocalError UnboundMethodType UnicodeError UnicodeType UserWarning ValueError Warning WindowsError XRangeType ZeroDivisionError __abs__ __add__ __all__ __author__ __bases__ __builtins__ __call__ __class__ __cmp__ __coerce__ __contains__ __debug__ __del__ __delattr__ __delitem__ __delslice__ __dict__ __div__ __divmod__ __doc__ __docformat__ __eq__ __file__ __float__ __floordiv__ __future__ __ge__ __getattr__ __getattribute__ __getitem__ __getslice__ __gt__ __hash__ __hex__ __iadd__ __import__ __imul__ __init__ __int__ __invert__ __iter__ __le__ __len__ __long__ __lshift__ __lt__ __members__ __metaclass__ __mod__ __mro__ __mul__ __name__ __ne__ __neg__ __new__ __nonzero__ __oct__ __or__ __path__ __pos__ __pow__ __radd__ __rdiv__ __rdivmod__ __reduce__ __repr__ __rfloordiv__ __rlshift__ __rmod__ __rmul__ __ror__ __rpow__ __rrshift__ __rsub__ __rtruediv__ __rxor__ __self__ __setattr__ __setitem__ __setslice__ __slots__ __str__ __sub__ __truediv__ __version__ __xor__ abs abstract acos acosh active all and any apply array as asc ascb ascw asin asinh asm assert async atan atanh atn atomicAdd atomicAnd atomicCompSwap atomicCounter atomicCounterDecrement atomicCounterIncrement atomicExchange atomicMax atomicMin atomicOr atomicXor atomic_uint attribute auto await barrier become bitCount bitfieldExtract bitfieldInsert bitfieldReverse bool boolean box break buffer bvec2 bvec3 bvec4 byref byte byval call callable case cast catch cbool cbyte ccur cdate cdbl ceil centroid char chr chrb chrw cint clamp class classmethod clng cmp coerce coherent common compile complex const continue cos cosh crate createobject cross csng cstr dFdx dFdy date dateadd datediff datepart dateserial datevalue day def default degrees del delattr determinant dict dim dir discard distance divmod dmat2 dmat2x2 dmat2x3 dmat2x4 dmat3 dmat3x2 dmat3x3 dmat3x4 dmat4 dmat4x2 dmat4x3 dmat4x4 do dot double dvec2 dvec3 dvec4 dyn each elif else elseif empty end enum enumerate equal erase error eval except exec execfile execute exit exp exp2 explicit extends extern external faceforward false file filter final finally findLSB findMSB fix fixed flat float floatBitsToInt floatBitsToUint floor fma fn for formatcurrency formatdatetime formatnumber formatpercent fract frexp from frozenset function fvec2 fvec3 fvec4 fwidth get getattr getobject getref gl_ClipDistance gl_FragCoord gl_FragDepth gl_FrontFacing gl_GlobalInvocationID gl_InstanceID gl_InvocationID gl_Layer gl_LocalInvocationID gl_LocalInvocationIndex gl_NumSamples gl_NumWorkGroups gl_PatchVerticesIn gl_PointCoord gl_PointSize gl_Position gl_PrimitiveID gl_PrimitiveIDIn gl_SampleID gl_SampleMask gl_SampleMaskIn gl_SamplePosition gl_TessCoord gl_TessLevelInner gl_TessLevelOuter gl_VertexID gl_ViewportIndex gl_WorkGroupID gl_WorkGroupSize global globals goto greaterThan greaterThanEqual groupMemoryBarrier half hasattr hash hex highp hour hvec2 hvec3 hvec4 id if iimage1D iimage1DArray iimage2D iimage2DArray iimage2DMS iimage2DMSArray iimage2DRect iimage3D iimageBuffer iimageCube iimageCubeArray image1D image1DArray image2D image2DArray image2DMS image2DMSArray image2DRect image3D imageAtomicAdd imageAtomicAnd imageAtomicCompSwap imageAtomicExchange imageAtomicMax imageAtomicMin imageAtomicOr imageAtomicXor imageBuffer imageCube imageCubeArray imageLoad imageSize imageStore imp impl implements import imulExtended in inline inout input inputbox instanceof instr instrb instrrev int intBitsToFloat interface intern interpolateAtCentroid interpolateAtOffset interpolateAtSample invariant inverse inversesqrt is isampler1D isampler1DArray isampler2D isampler2DArray isampler2DMS isampler2DMSArray isampler2DRect isampler3D isamplerBuffer isamplerCube isamplerCubeArray isarray isdate isempty isinf isinstance isnan isnull isnumeric isobject issubclass iter ivec2 ivec3 ivec4 join lambda layout lbound lcase ldexp left leftb len lenb length lessThan lessThanEqual let list loadpicture local locals log log2 long loop lowp ltrim macro map mat2 mat2x2 mat2x3 mat2x4 mat3 mat3x2 mat3x3 mat3x4 mat4 mat4x2 mat4x3 mat4x4 match matrixCompMult max mediump memoryBarrier memoryBarrierAtomicCounter memoryBarrierBuffer memoryBarrierImage memoryBarrierShared mid midb min minute mix mod modf month monthname move msgbox mut namespace native new next nil noinline noise noperspective normalize not notEqual nothing now null object oct on open option or ord out outerProduct output override packDouble2x32 packHalf2x16 packSnorm2x16 packSnorm4x8 packUnorm2x16 packUnorm4x8 package packed partition pass patch pow precision preserve print priv private property protected pub public radians raise randomize range raw_input readonly redim reduce ref reflect refract register reload rem repeat replace repr resource restrict resume return reversed rgb right rightb rnd round roundEven row_major rtrim sample sampler1D sampler1DArray sampler1DArrayShadow sampler1DShadow sampler2D sampler2DArray sampler2DArrayShadow sampler2DMS sampler2DMSArray sampler2DRect sampler2DRectShadow sampler2DShadow sampler3D sampler3DRect samplerBuffer samplerCube samplerCubeArray samplerCubeArrayShadow samplerCubeShadow scriptengine scriptenginebuildversion scriptenginemajorversion scriptengineminorversion second select self set setattr sgn shared short sign signed sin sinh sizeof slice smooth smoothstep sorted space split sqr sqrt static staticmethod step str strcomp strictfp string strreverse struct sub subroutine sum super superp switch synchronized tan tanh template texelFetch texelFetchOffset texture textureGather textureGatherOffset textureGatherOffsets textureGrad textureGradOffset textureLod textureLodOffset textureOffset textureProj textureProjGrad textureProjGradOffset textureProjLod textureProjLodOffset textureProjOffset textureQueryLevels textureQueryLod textureSize then this throw throws time timeserial timevalue to trait transient transpose trim true trunc try tuple type typedef typename typeof uaddCarry ubound ucase uimage1D uimage1DArray uimage2D uimage2DArray uimage2DMS uimage2DMSArray uimage2DRect uimage3D uimageBuffer uimageCube uimageCubeArray uint uintBitsToFloat umulExtended unichr unicode uniform union unpackDouble2x32 unpackHalf2x16 unpackSnorm2x16 unpackSnorm4x8 unpackUnorm2x16 unpackUnorm4x8 unsafe unsigned unsized until usampler1D usampler1DArray usampler2D usampler2DArray usampler2DMS usampler2DMSArray usampler2DRect usampler3D usamplerBuffer usamplerCube usamplerCubeArray use using usubBorrow uvec2 uvec3 uvec4 vars vartype varying vbAbort vbAbortRetryIgnore vbApplicationModal vbCancel vbCritical vbDefaultButton1 vbDefaultButton2 vbDefaultButton3 vbDefaultButton4 vbExclamation vbFalse vbGeneralDate vbIgnore vbInformation vbLongDate vbLongTime vbNo vbOK vbOKCancel vbOKOnly vbObjectError vbQuestion vbRetry vbRetryCancel vbShortDate vbShortTime vbSystemModal vbTrue vbUseDefault vbYes vbYesNo vbYesNoCancel vbarray vbblack vbblue vbboolean vbbyte vbcr vbcrlf vbcurrency vbcyan vbdataobject vbdate vbdecimal vbdouble vbempty vberror vbformfeed vbgreen vbinteger vblf vblong vbmagenta vbnewline vbnull vbnullchar vbnullstring vbobject vbred vbsingle vbstring vbtab vbvariant vbverticaltab vbwhite vbyellow vec2 vec3 vec4 virtual void volatile weekday weekdayname wend where while with writeonly xor xrange year yield zip"
			.split(" ");

	public static List originKeys;

	static Random random = new Random();

	public static Image tabImg, tabImgPrint;

	static final String UTF8 = "UTF-8";

	public static final char N = '\n';

	static {
		try {
			System.out.println("welcome to " + EditorPanel.WINDOW_NAME);
			loadTabImage();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void addKey(Map<String, Commands> keys, String key, String cmd) throws Exception {
		String name = getKeyNameFromTextName(key);

		Commands c1;
		try {
			c1 = Commands.valueOf(cmd);
		} catch (Exception ex) {
			System.out.println("undefined command:" + cmd);
			return;
		}
		try {
			// System.out.println(""+name+":"+c1);
			if (keys.containsKey(name)) {
				System.err.println("duplicated key:" + name);
			} else {
				keys.put(name, c1);
			}
		} catch (Exception ex) {
			System.err.println("Error: unknow key:" + key);
		}

	}

	public static String getKeyNameFromTextName(String key) throws Exception {
		String k = key.toUpperCase();
		String name = "";
		int p1;
		p1 = k.indexOf("SHIFT-");
		if (p1 >= 0) {
			k = k.substring(0, p1) + k.substring(p1 + 6);
			name = name + "S";
		}
		p1 = k.indexOf("CTRL-");
		if (p1 >= 0) {
			k = k.substring(0, p1) + k.substring(p1 + 5);
			name = name + "C";
		}
		p1 = k.indexOf("ALT-");
		if (p1 >= 0) {
			k = k.substring(0, p1) + k.substring(p1 + 4);
			name = name + "A";
		}

		Field f = KeyEvent.class.getField("VK_" + k);
		int kc = f.getInt(null);
		String kt = KeyEvent.getKeyText(kc);
		name = name + kt;

		return name;
	}

	public static void startThread(Thread thread) {
		thread.setDaemon(true);
		thread.start();
	}

	public static int indexOf(CharSequence input, char needle) {
		if (input instanceof String) {
			String text = (String) input;
			return text.indexOf(needle);
		}
		// if (input instanceof String) {
		// String text = (String) input;
		// return text.indexOf(needle);
		// }
		if (input instanceof StringBuilder) {
			StringBuilder text = (StringBuilder) input;
			return text.indexOf("" + needle);
		}
		System.out.println("input=" + input);
		return input.toString().indexOf(needle);
	}

	public static int indexOf(CharSequence input, String needle, int start) {
		if (input instanceof StringBuilder) {
			StringBuilder text = (StringBuilder) input;
			return text.indexOf(needle, start);
		}
		return input.toString().indexOf(needle, start);
	}

	public static int indexOfLast(CharSequence input, String needle, int start) {
		if (input instanceof StringBuilder) {
			StringBuilder text = (StringBuilder) input;
			return text.lastIndexOf(needle, start);
		}
		return input.toString().lastIndexOf(needle, start);
	}

	static void attach(final PlainPage page, final InputStream std) {
		U.startThread(new Thread() {
			@Override
			public void run() {
				try {
					InputStream in = std;
					int len;
					byte[] buf = new byte[10240];
					// page.ptEdit.consoleAppend("encoding:" + enc + "\n");
					while ((len = in.read(buf)) > 0) {
						String enc = page.pageData.encoding;
						if (enc == null) {
							enc = "utf8";
						}
						String line = new String(buf, 0, len, enc);
						line = Console.filterSimpleTTY(line);
						String[] ss = line.split("\\n");
						for (String s : ss) {
							s = U.removeTailR(s).toString();
							if (s.startsWith("\r")) {
								s = s.substring(1);
								// int y = page.pageData.roLines.getLinesize() - 2;
								// if (y >= 0) {
								// page.ptEdit.deleteLine(y);
								// }
							}
							page.ptEdit.consoleAppend(s + "\n");
						}
						page.uiComp.repaint();
					}
					page.ptEdit.consoleAppend("<EOF>\n");
				} catch (Throwable e) {
					page.ptEdit.consoleAppend("error:" + e + "\n");
				}
			}
		});
	}

	static boolean changedOutside(PlainPage pp) {
		PageData page = pp.pageData;
		String his = "";
		try {
			his = getFileHistoryName().getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (page.getFn() != null && (!page.getFn().equals(his)) && page.fileLastModified != 0) {
			long t = new File(page.getFn()).lastModified();
			if (t > page.fileLastModified + 100) {
				return true;
			}
		}
		return false;
	}

	static void closePage(PlainPage page) throws Exception {
		EditorPanel editor = page.uiComp;
		int opt = JOptionPane.NO_OPTION;
		if (page.pageData.history.size() != 0) {
			opt = JOptionPane.showConfirmDialog(editor, "Are you sure to SAVE and close?", "Changes made",
					JOptionPane.YES_NO_CANCEL_OPTION);
			System.out.println(opt);
			if (opt == JOptionPane.CANCEL_OPTION || opt == -1) {
				return;
			}
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
	static int computeShowIndex(CharSequence s0, int width, Graphics2D g2, Font[] fonts) {
		if (s0.length() == 0) {
			return 0;
		}
		String s = s0.toString();

		if (U.stringWidth(g2, fonts, s) <= width) {
			return s.length();
		}
		int i = s.length() / 2;
		while (true) {
			if (i == 0) {
				return 0;
			}
			int w = U.stringWidth(g2, fonts, s.substring(0, i));
			if (w <= width) {
				return i + computeShowIndex(s.substring(i), width - w, g2, fonts);
			} else {
				i = i / 2;
			}
		}
	}

	static void dialogMsg(String s) {
		JOptionPane.showMessageDialog(null, s);
	}

	static void doFindInDir(PlainPage page, String text, boolean ignoreCase, boolean selected2, boolean inDir,
			String dir, String fnFilter) throws Exception {
		Iterable<File> it = new FileIterator(dir);
		List<String> all = new ArrayList<String>();
		fnFilter = fnFilter.trim().toLowerCase();
		// search, skip binary, filtered
		int[] cnts = new int[3];
		for (File f : it) {
			if (f.isDirectory()) {
				continue;
			}
			if (fnFilter.length() > 0) {
				String fn = f.getName().toLowerCase();
				if (fn.indexOf(fnFilter) < 0) {
					cnts[2]++;
					continue;
				}
			}
			List<String> res = U.findInFile(f, text, ignoreCase, cnts);
			all.addAll(res);
		}
		showResult(page, all, "dir", dir, text, fnFilter, cnts);
		page.uiComp.repaint();
	}

	static void doFindInPage(PlainPage page, String text2find, boolean ignoreCase) throws Exception {
		if (text2find != null && text2find.length() > 0) {
			Point p = U.find(page, text2find, 0, 0, ignoreCase);
			if (p == null) {
				page.ui.message("string not found");
			} else {
				List<String> all = new ArrayList<String>();
				while (true) {
					all.add(String.format("%s:%s", p.y + 1, page.pageData.roLines.getline(p.y)));
					Point p2 = U.find(page, text2find, 0, p.y + 1, ignoreCase);
					if (p2 == null || p2.y <= p.y) {
						break;
					} else {
						p = p2;
					}
				}
				showResult(page, all, "file", page.pageData.getTitle(), text2find, null, null);
				page.uiComp.repaint();
			}
		}
	}

	static void doReplace(PlainPage page, String text, boolean ignoreCase, boolean selected2, String text2, boolean all,
			boolean inDir, String dir) {
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

	static void doReplaceAll(PlainPage page, String text, boolean ignoreCase, boolean selected2, String text2,
			boolean inDir, String dir, String fnFilter) throws Exception {
		if (inDir) {
			U.doReplaceInDir(page, text, ignoreCase, text2, inDir, dir, fnFilter);
		} else {
			U.doReplace(page, text, ignoreCase, selected2, text2, true, inDir, dir);
		}
	}

	static void doReplaceInDir(PlainPage page, String text, boolean ignoreCase2, String text2, boolean inDir,
			String dir, String fnFilter) throws Exception {
		Iterable<File> it = new FileIterator(dir);
		List<String> all = new ArrayList<String>();
		fnFilter = fnFilter.trim().toLowerCase();
		int[] cnts = new int[3];
		for (File f : it) {
			if (f.isDirectory()) {
				continue;
			}
			if (fnFilter.length() > 0) {
				String fn = f.getName().toLowerCase();
				if (fn.indexOf(fnFilter) < 0) {
					cnts[2]++;
					continue;
				}
			}
			try {
				List<String> res = U.findInFile(f, text, page.ignoreCase, cnts);
				if (!res.isEmpty()) {
					PlainPage pi = new PlainPage(page.uiComp, PageData.newFromFile(f.getCanonicalPath()));
					if (pi != null) {
						doReplaceAll(pi, text, ignoreCase2, false, text2, false, null, fnFilter);
					}
				}
				all.addAll(res);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		showResult(page, all, "dir", dir, text, fnFilter, cnts);
		page.uiComp.repaint();
	}

	static int drawTwoColor(Graphics2D g2, Font[] fonts, String s, int x, int y, Color c1, Color c2, int d,
			boolean isCurrentLine, int lineHeight) {
		g2.setColor(c2);
		int w = U.drawString(g2, fonts, s, x + d, y + d);
		g2.setColor(c1);
		U.drawString(g2, fonts, s, x, y, isCurrentLine, lineHeight);
		return w;

	}

	public static void exec(PlainPage pp, String cmd) throws Exception {
		if (cmd.trim().length() <= 0) {
			return;
		}
		Process proc = Runtime.getRuntime().exec(cmd);
		OutputStream out = proc.getOutputStream();
		InputStream stdout = proc.getInputStream();
		InputStream stderr = proc.getErrorStream();

		new Console(cmd, out, stdout, stderr, proc).start();
		// attach(getPage(pp.uiComp, "[stderr]"), stderr);
		// attach(getPage(pp.uiComp, "[stdout]"), stdout);
	}

	static Point find(PlainPage page, String s, int x, int y, boolean ignoreCase) {
		if (y >= page.pageData.roLines.getLinesize()) {
			return null;
		}
		if (ignoreCase) {
			s = s.toLowerCase();
		}
		x = Math.min(x, page.pageData.roLines.getline(y).length());
		// first half row
		int p1 = U.indexOf(page.pageData.roLines.getline(y), ignoreCase, s, x);
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
			p1 = U.indexOf(page.pageData.roLines.getline(fy), ignoreCase, s, 0);
			if (p1 >= 0) {
				return new Point(p1, fy);
			}
		}
		// last half row
		CharSequence sb = page.pageData.roLines.getline(y);
		p1 = U.indexOf(sb.subSequence(0, x), ignoreCase, s, 0);
		if (p1 >= 0) {
			return new Point(p1, fy);
		}
		return null;
	}

	static Point find_prev(PlainPage page, String s, int x, int y, boolean ignoreCase) {
		if (y >= page.pageData.roLines.getLinesize()) {
			return null;
		}
		if (ignoreCase) {
			s = s.toLowerCase();
		}
		if (x < 0) {
			y--;
			if (y < 0) {
				y = page.pageData.roLines.getLinesize() - 1;
			}
			if (y < 0)
				return null;
			if (y >= 0)
				x = page.pageData.roLines.getline(y).length();
		}

		x = Math.min(x, page.pageData.roLines.getline(y).length());
		// first half row
		int p1 = U.indexOfLast(page.pageData.roLines.getline(y), ignoreCase, s, x);
		if (p1 >= 0) {
			return new Point(p1, y);
		}
		// middle rows
		int fy = y;
		for (int i = 0; i < page.pageData.roLines.getLinesize() - 1; i++) {
			fy -= 1;
			if (fy < 0) {
				fy = page.pageData.roLines.getLinesize() - 1;
			}
			CharSequence line = page.pageData.roLines.getline(fy);
			p1 = U.indexOfLast(line, ignoreCase, s, line.length());
			if (p1 >= 0) {
				return new Point(p1, fy);
			}
		}
		{
			// last half row
			CharSequence sb = page.pageData.roLines.getline(y);
			if (x >= sb.length())
				return null;
			CharSequence tail = sb.subSequence(x, sb.length());
			p1 = U.indexOfLast(tail, ignoreCase, s, tail.length());
			if (p1 >= 0) {
				return new Point(p1 + x, fy);
			}
		}
		return null;
	}

	private static int indexOf(CharSequence t, boolean ignoreCase, String s, int x) {
		if (!ignoreCase) {
			return U.indexOf(t, s, x);
		} else {
			return t.toString().toLowerCase().indexOf(s, x);
		}
	}

	private static int indexOfLast(CharSequence t, boolean ignoreCase, String s, int x) {
		if (!ignoreCase) {
			return U.indexOfLast(t, s, x);
		} else {
			return t.toString().toLowerCase().lastIndexOf(s, x);
		}
	}

	static boolean findAndShowPageListPage(EditorPanel ep, String title, int lineNo, boolean rec) {
		PlainPage pp = findPage(ep, title);
		if (pp == null) {
			return false;
		} else {
			ep.setPage(pp, rec);
			return true;
		}
	}

	static boolean findAndShowPageListPage(EditorPanel ep, String title, int lineNo, int x, boolean recCh) {
		boolean b = findAndShowPageListPage(ep, title, lineNo, recCh);
		if (b) {
			ep.getPage().cursor.setSafePos(x, lineNo - 1, recCh);
			ep.getPage().focusCursor();
			ep.repaint();
		}
		return b;
	}

	static void findchar(PlainPage page, char ch, int inc, int[] c1, char chx) {
		int cx1 = c1[0];
		int cy1 = c1[1];
		CharSequence csb = page.pageData.roLines.getline(cy1);
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

	static List<String> findInFile(File f, String text, boolean ignoreCase2, int[] cnts) {
		// System.out.println("find in "+f.getName());
		int MAX_SHOW_CHARS_IN_LINE = 30;
		List<String> a = new ArrayList<String>();
		try {
			if (f.getName().endsWith(".class")) {// some .class will be guess to UTF8
				if (cnts != null)
					cnts[1]++;
				return a;
			}
			String enc = guessEncoding(f.getAbsolutePath());
			if (enc == null) {
				if (guessIsBinFile(f)) {
					if (cnts != null)
						cnts[1]++;
					return a;
				}
				enc = UTF8;// avoid wrong skip
			}
			if (enc != null) {// skip binary
				String fn = f.getAbsolutePath();
				if (ignoreCase2) {
					text = text.toLowerCase();
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), enc));
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
							line = line.substring(0, MAX_SHOW_CHARS_IN_LINE) + "...";
						}
						a.add(String.format("%s|%s:%s", fn, lineno, oline));
					}
				}
				in.close();
				if (cnts != null)
					cnts[0]++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return a;
	}

	private static final String[] binExt = { ".jar", ".class", ".o", ".so", ".exe", ".dll", ".jpg", ".gif", ".png",
			".mp3", ".mp4", ".war", ".zip", ".gz", ".rar", ".7z", ".ttc", ".ttf", ".pdf", ".xlsx", ".xls", ".mpeg",
			".bz2", ".bin" };

	private static boolean guessIsBinFile(File f) {
		// first, encoding guessed is null
		String name = f.getName();
		if (_endsWithAny(binExt, name)) {
			return true;
		}
		long size = f.length();
		if (size >= 3 * 1000 * 1000)
			return true;
		return false;
	}

	private static boolean _endsWithAny(String[] binExt, String name) {
		name = name.toLowerCase();
		for (String ext : binExt) {
			if (name.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	static PlainPage findPage(EditorPanel ep, String title) {
		for (PlainPage pp : ep.pageSet) {
			if (pp.pageData.getTitle().equals(title)) {
				return pp;
			}
		}
		return null;
	}

	/**
	 * No good.
	 */
	static void gc() {
		// System.out.print(km(Runtime.getRuntime().freeMemory()) + "/" +
		// km(Runtime.getRuntime().totalMemory()) + " -> ");
		// Runtime.getRuntime().gc();
		// System.out.println(km(Runtime.getRuntime().freeMemory()) + "/" +
		// km(Runtime.getRuntime().totalMemory()));
	}

	static String getClipBoard() {
		String s;
		try {
			s = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor).toString();
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

	static File getDirHistoryName() throws IOException {
		File f = new File(getMyDir(), "dh.txt");
		if (!f.exists()) {
			new FileOutputStream(f).close();
		}
		return f;
	}

	static int getHighLightID(String s, Graphics2D g2, Color colorKeyword, Color colorDigital, Color color) {
		if (Arrays.binarySearch(KWS, s) >= 0 || Arrays.binarySearch(KWS, s.toLowerCase()) >= 0) {
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
				FileUtil.copy(U.class.getResourceAsStream(fn), new FileOutputStream(installed));
			} catch (IOException e) {
				e.printStackTrace();
				return getResourceReader(fn);
			}
		}
		return new InputStreamReader(new FileInputStream(installed), "utf8");
	}

	public static File getMyDir() {
		String home = System.getProperty("user.home");
		File dir = new File(home, ".neoeedit");
		dir.mkdirs();
		return dir;
	}

	/**
	 * see findPage()
	 */
	static PlainPage getPage(EditorPanel ep, String title) throws Exception {
		PlainPage pp = findPage(ep, title);
		if (pp != null) {
			return pp;
		}
		PageData pd = PageData.dataPool.get(title);
		if (pd == null) {
			pd = PageData.newEmpty(title, "");
		}
		final PlainPage page = new PlainPage(ep, pd);
		return page;
	}

	public static List<CharSequence> getPageListStrings(EditorPanel ep) throws IOException {
		List<CharSequence> ss = new ArrayList<CharSequence>();
		sort(ep.pageSet);
		for (PlainPage pp : ep.pageSet) {
			ss.add(pp.pageData.getTitle() + "|" + (pp.cy + 1) + ":" + " Edited:" + pp.pageData.history.size() + " "
					+ (pp.pageData.encoding == null ? "" : pp.pageData.encoding + " ")
					+ (changedOutside(pp) ? "[Changed Outside!!]" : ""));
		}
		return ss;
	}

	static String getLocString(PlainPage pp) {
		if (pp == null)
			return null;
		return pp.pageData.getTitle() + "|" + (pp.cy + 1) + ":";
	}

	public static Reader getResourceReader(String fn) throws IOException {
		return new InputStreamReader(U.class.getResourceAsStream(fn), "utf8");
	}

	public static String getStr(List row, int i) {
		if (i < 0 || i >= row.size()) {
			return "";
		}
		return "" + row.get(i);
	}

	static boolean gotoFileLine(String sb, EditorPanel ep, boolean record) throws Exception {
		int p1, p2;
		String fn = sb.toString();
		if ((p1 = sb.indexOf("|")) >= 0) {
			fn = sb.substring(0, p1);
			if ((p2 = sb.indexOf(":", p1)) >= 0) {// search result
				int line = -1;
				try {
					line = Integer.parseInt(sb.substring(p1 + 1, p2));
				} catch (Exception e) {
				}
				if (line >= 0) {
					gotoFileLinePos(ep, fn, line, -1, record);
					return true;
				}
			}
		} else if ((p1 = sb.lastIndexOf(":")) > 0) { // try filename:lineno pattern
			int line = -1;
			try {
				line = Integer.parseInt(sb.substring(p1 + 1).trim());
				fn = fn.substring(0, p1).trim();
			} catch (Exception e) {
			}
			if (line >= 0) {
				return gotoFileLinePos(ep, fn, line, -1, record);
			}
		}

		{
			String dir = ep.getPage().pageData.workPath;
			if (dir == null) {
				dir = ".";
			}
			File f = new File(dir, sb.toString());
			if (f.exists() && f.isFile()) {
				gotoFileLinePos(ep, f.getAbsolutePath(), 0, -1, record);
				return true;
			}
		}
		return false;
	}

	/** goto search result */
	public static boolean gotoFileLine2(EditorPanel ep, String sb, String title, boolean record) throws Exception {
		int p2;
		if ((p2 = sb.indexOf(":")) >= 0) {
			int line = -1;
			try {
				line = Integer.parseInt(sb.substring(0, p2));
			} catch (Exception e) {
			}
			if (line >= 0) {
				if (!U.findAndShowPageListPage(ep, title, line, 0, record)) {
					return openFile(title, line, ep, record);
				}
			}
		}
		return false;
	}

	public static boolean gotoFileLinePos(EditorPanel ep, String fn, int line, int x, boolean recCh) throws Exception {
		if (!U.findAndShowPageListPage(ep, fn, line, x, recCh)) {
			return openFile(fn, line, ep, recCh);
		} else {
			return true;
		}

	}

	private static String guessByBOM(byte[] src) {
		for (Object[] row : BOMS) {
			int[] seq = (int[]) row[0];
			// compare 2 array
			if (seq.length > src.length) {
				continue;
			}
			boolean same = true;
			for (int i = 0; i < seq.length; i++) {
				if ((byte) seq[i] != src[i]) {
					same = false;
					break;
				}
			}
			if (same) {
				return (String) row[1];
			}
		}
		return null;
	}

	static void guessComment(PlainPage page) {
		List<String> comment = new ArrayList<String>();
		String[] commentchars = { "/*", "<!--", "#", "%", "'", "//", "!", ";", "--", };
		int[] cnts = new int[commentchars.length];
		int maxLines = Math.min(10000, page.pageData.roLines.getLinesize());
		for (int i = 0; i < maxLines; i++) {
			CharSequence sb = page.pageData.roLines.getline(i);
			for (int j = 0; j < cnts.length; j++) {
				CharSequence tl = U.trimLeft(sb);
				String s = tl.subSequence(0, Math.min(40, tl.length())).toString();
				String k = commentchars[j];
				if (s.startsWith(k) || s.indexOf(k) >= 0) {
					cnts[j] += k.length();
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
		page.ui.comment = comment == null ? null : comment.toArray(new String[comment.size()]);
		page.uiComp.repaint();
	}

	static String guessEncoding(String fn) throws Exception {
		// S/ystem.out.println("guessing encoding");
		String[] encodings = { UTF8, "gbk", "sjis", "unicode", "euc-jp" };

		FileInputStream in = new FileInputStream(fn);
		final int defsize = 1024 * 1024 * 2;
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
			if (encoding != null) {
				return encoding;
			}
			for (String enc : encodings) {
				String s = new String(buf, enc);
				if (s.toLowerCase().indexOf(enc.toLowerCase()) >= 0) {
					return enc;
				}
			}
			for (String enc : encodings) {
				String s = new String(buf, enc);
				if (s.length() > 3) {
					// multi bytes string, so tail may be mistaken
					s = s.substring(0, s.length() - 3);
				} else {
					return UTF8;// utf8 for empty file
				}
				byte[] bs2 = s.getBytes(enc);
				// bs2 maybe short than buf
				if (bsCompare(buf, bs2, bs2.length)) {
					return enc;
				}

			}
		} finally {
			in.close();
		}

		return null;
	}

	private static boolean bsCompare(byte[] b1, byte[] b2, int len) {
		for (int i = 0; i < len; i++) {
			if (b1[i] != b2[i])
				return false;
		}
		return true;
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
				return new String(buf, "iso8859-1").indexOf("\r\n") >= 0 ? "\r\n" : "\n";
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
		return (fn.endsWith(".gif") || fn.endsWith(".jpg") || fn.endsWith(".png") || fn.endsWith(".bmp")
				|| fn.endsWith(".jpeg"));
	}

	static boolean isSkipChar(char ch, char ch1) {
		if (U.isSpaceChar(ch1)) {
			return U.isSpaceChar(ch);
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

	public static boolean launch(String s) throws Exception {
		s = s.trim();
		String slo = s.toLowerCase();
		Desktop dt = Desktop.getDesktop();
		if (slo.startsWith("mailto:")) {
			dt.mail(new URI(s));
		} else if (slo.startsWith("http://") || slo.startsWith("https://")) {
			dt.browse(new URI(s));
		} else if (new File(s).exists()) {
			dt.open(new File(s));
		} else {
			return false;
		}
		return true;
	}

	static boolean listDir(PlainPage page, int atLine) throws Exception {
		String line = page.pageData.roLines.getline(atLine).toString();
		String fn = line.trim();
		int p1 = fn.indexOf('|');
		if (p1 >= 0) {
			fn = fn.substring(0, p1).trim();
		}
		File f = new File(fn);
		if (f.isFile() && f.exists()) {
			openFile(fn, 0, page.uiComp, true);
		} else if (f.isDirectory()) {
			File[] fs = f.listFiles();
			page.cx = line.length();
			page.ptEdit.insertString("\n{-----");
			for (File f1 : fs) {
				if (f1.isDirectory()) {
					page.ptEdit.insertString("\n" + f1.getAbsolutePath() + " | <DIR>");
				} else {
					page.ptEdit.insertString("\n" + f1.getAbsolutePath());
				}
			}
			page.ptEdit.insertString("\n-----}");
		} else {
			return false;
		}
		return true;
	}

	public static void listFonts(PlainPage pp) throws Exception {
		PlainPage p2 = new PlainPage(pp.uiComp, PageData.newEmpty(String.format("<Fonts>")));
		p2.pageData.workPath = pp.pageData.workPath;
		p2.ui.applyColorMode(pp.ui.colorMode);
		List<CharSequence> sbs = new ArrayList<CharSequence>();
		String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (String font : fonts) {
			sbs.add("set-font:" + font);
		}
		p2.pageData.setLines(sbs);

	}

	static void loadTabImage() throws Exception {
		BufferedImage img = ImageIO.read(U.class.getResourceAsStream("/icontab.png"));
		tabImg = img.getScaledInstance(TAB_WIDTH, 8, Image.SCALE_SMOOTH);
		tabImgPrint = img.getScaledInstance(TAB_WIDTH, 8, Image.SCALE_SMOOTH);
	}

	public static Commands mappingToCommand(KeyEvent env) {
		int kc = env.getKeyCode();
		if (kc == KeyEvent.VK_SHIFT || kc == KeyEvent.VK_CONTROL || kc == KeyEvent.VK_ALT)
			// fast pass
			return null;
		String name = getKeyName(env);
//		System.out.println("key name=" + name);
		Commands cmd = keys.get(name);
		return cmd;
	}

	static String getKeyName(KeyEvent evt) {
		int kc = evt.getKeyCode();
		String kt = KeyEvent.getKeyText(kc);
		String name = kt;
		boolean other = false;
		if (evt.isAltDown()) {
			name = "A" + name;
			other = true;
		}
		if (evt.isControlDown()) {
			name = "C" + name;
			other = true;
		}
		if (other && kt.length() == 1 && evt.isShiftDown()) {
			name = "S" + name;
		}
		return name;
	}

	public static int maxWidth(List<Object[]> msgs, Graphics2D g, Font[] fonts) {
		int max = 0;
		for (int i = 0; i < msgs.size(); i++) {
			Object[] row = msgs.get(i);
			int w1 = (Integer) row[2];
			if (w1 == -1) {
				w1 = U.stringWidth(g, fonts, row[0].toString());
				row[2] = w1;
			}
			if (w1 > max) {
				max = w1;
			}
		}
		return max;
	}

	static PlainPage openFile(File f, EditorPanel ep) throws Exception {
		if (isImageFile(f)) {
			new PicView(ep).show(f);
			return null;
		} else {
			if (ep == null) {
				return null;// ignore
			}
			if (findAndShowPageListPage(ep, f.getCanonicalPath(), 0, true)) {
				return ep.getPage();
			}
			return new PlainPage(ep, PageData.newFromFile(f.getCanonicalPath()));
		}
	}

	static void openFile(PlainPage page) throws Exception {

		String dir = page.pageData.workPath;
		if (dir == null) {
			dir = new File(".").getCanonicalPath();
		}
		String title = "[Dir]" + dir;
		PageData pd = PageData.dataPool.get(title);
		if (pd == null) {
			pd = PageData.newEmpty(title);
			pd.setText(dir);
			PlainPage pp = new PlainPage(page.uiComp, pd);
			U.listDir(pp, 0);
			pp.pageData.workPath = page.pageData.workPath;
		} else {
			EditorPanel ep = page.uiComp;
			if (!U.findAndShowPageListPage(ep, title, 0, true)) {
				new PlainPage(page.uiComp, pd);
			}
		}

	}

	static boolean openFile(String title, int line, EditorPanel ep, boolean recCh) throws Exception {
		File f = new File(title);
		if (isImageFile(f)) {
			new PicView().show(f);
			return true;
		}
		if (findAndShowPageListPage(ep, title, line, recCh)) {
			return true;
		}

		PageData pd = PageData.dataPool.get(title);
		// including titles not saved
		if (pd == null) {
			pd = PageData.newFromFile(f.getCanonicalPath());
		}
		final PlainPage page = new PlainPage(ep, pd);
		if (page != null && page.pageData.lines.size() > 0) {
			line -= 1;
			page.cx = 0;
			page.cy = Math.max(0, Math.min(line, page.pageData.lines.size() - 1));
			page.selectstartx = 0;
			page.selectstarty = page.cy;
			page.selectstopx = 0;
			page.selectstopy = page.cy;
			page.sy = Math.max(0, page.cy - 3);
			page.uiComp.repaint();
		}

		return true;
	}

	static void openFileHistory(EditorPanel ep) throws Exception {
		File fhn = getFileHistoryName();
		PlainPage page = new PlainPage(ep, PageData.newFromFile(fhn.getCanonicalPath()));
		page.cy = Math.max(0, page.pageData.lines.size() - 1);
		page.sy = Math.max(0, page.cy - 5);
		page.uiComp.repaint();

	}

	static void openDirHistory(EditorPanel ep) throws Exception {
		File f = getDirHistoryName();
		PlainPage page = new PlainPage(ep, PageData.newFromFile(f.getCanonicalPath()));
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

	public static int parseInt(Object o) {
		int v;
		if (o == null) {
			throw new RuntimeException("expect int but get null");
		}
		if (o instanceof BigDecimal) {
			return ((BigDecimal) o).intValue();
		}
		String s = o.toString();
		if (s.startsWith("0x")) {
			v = Integer.parseInt(s.substring(2), 16);
		} else {
			v = Integer.parseInt(s);
		}
		return v;
	}

	static int idIndex;

	public static String randomID() {
		return Integer.toString((int) (System.currentTimeMillis() % 0xfffffff), 36) + "_"
				+ Integer.toString(idIndex++, 36);
	}

	static void readFile(PageData data, String fn) {
		data.isCommentChecked = false;
		if (data.encoding == null) {
			data.encoding = U.guessEncodingForEditor(fn);
		}
		data.lineSep = U.guessLineSepForEditor(fn);
		data.lines = null;
		data.history.clear();
		data.setLines(U.readFileForEditor(fn, data.encoding));
		File f = new File(fn);
		data.fileLastModified = f.lastModified();
		data.workPath = f.getParent();
	}

	static List<CharSequence> readFileForEditor(String fn, String encoding) {
		try {
			System.out.println("read file:" + fn + " encoding=" + encoding);
			return U.removeTailR(FileUtil.readStringBig(new File(fn), encoding));
		} catch (Throwable e) {
			e.printStackTrace();
			List<CharSequence> lines = new ArrayList<CharSequence>();
			lines.add(e.toString());
			return lines;
		}
	}

	static void reloadWithEncodingByUser(String fn, PlainPage pp) {
		if (fn == null) {
			pp.ui.message("file not saved.");
			return;
		}
		if (setEncodingByUser(pp, "Reload with Encoding:"))
			readFile(pp.pageData, fn);
	}

	static String removeAsciiZero(String s) {
		int cnt = 0;
		char zero = (char) 0;

		int p = s.indexOf(zero);
		if (p < 0) {
			return s;
		}
		String zeros = "" + zero;
		StringBuilder sb = new StringBuilder(s);
		while (p >= 0) {
			sb.deleteCharAt(p);
			cnt++;
			p = sb.indexOf(zeros, p);
		}
		System.out.println("removed " + cnt + " NULL char");
		return sb.toString();
	}

	static CharSequence removeTailR(CharSequence s) {
		if (s.length() == 0)
			return s;
		if (s.charAt(s.length() - 1) == '\r') {
			s = s.subSequence(0, s.length() - 1);
		}
		return s;
	}

	static void removeTrailingSpace(PageData data) {
		for (int i = 0; i < data.roLines.getLinesize(); i++) {
			CharSequence sb = data.roLines.getline(i);
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
		U.startThread(new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(t);
					edit.repaint();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

	static Point replace(PlainPage page, String s, int x, int y, String s2, boolean all, boolean ignoreCase) {
		int cnt = 0;
		U.BasicEdit editRec = page.pageData.editRec;
		if (ignoreCase) {
			s = s.toLowerCase();
		}
		// first half row
		int p1 = x;
		while (true) {
			p1 = U.indexOf(page.pageData.roLines.getline(y), ignoreCase, s, p1);
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
				p1 = U.indexOf(page.pageData.roLines.getline(fy), ignoreCase, s, p1);
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
		CharSequence sb = page.pageData.roLines.getline(fy);
		while (true) {
			p1 = U.indexOf(sb.subSequence(0, x), ignoreCase, s, p1);
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
		final JFrame f = new JFrame("Script for " + ppTarget.pageData.getTitle());
		JPanel panel = new JPanel(new BorderLayout());
		final EditorPanel ep1 = new EditorPanel();
		File scriptDir = new File(U.getMyDir(), "scripts");
		scriptDir.mkdirs();
		ep1.page.pageData.workPath = scriptDir.getAbsolutePath();
		ep1.frame = f;
		U.openFile(ep1.page);
		// EditorPanel.openedWindows++;
		panel.add(ep1, BorderLayout.CENTER);
		JButton jb1;
		panel.add(jb1 = new JButton("Run!"), BorderLayout.SOUTH);
		jb1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					runScript(ppTarget, exportString(ep1.page.pageData.lines, "\n"));
				} catch (Exception e1) {
					System.out.println(e1);
					StringWriter errors = new StringWriter();
					e1.printStackTrace(new PrintWriter(errors));
					ep1.page.ptEdit.append("/*\n" + errors.toString() + "\n*/\n");
				}

			}
		});
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.getContentPane().add(panel);
		f.setSize(600, 400);
		f.setLocationRelativeTo(ppTarget.uiComp);
		f.setVisible(true);
	}

	static void runScript(final PlainPage ppTarget, String script) throws Exception {
		ReadonlyLines lines = ppTarget.pageData.roLines;
		List<CharSequence> export = new ArrayList<CharSequence>();
		{
			int size = lines.getLinesize();
			for (int i = 0; i < size; i++) {
				export.add(lines.getline(i));
			}
		}
		ScriptUtil su = new ScriptUtil();
		List<CharSequence> ret = su.runSingleScript(script, export);
		EditorPanel ep = new EditorPanel(ppTarget.uiComp.config);
		ep.openWindow();
		ep.getPage().pageData.workPath = ppTarget.pageData.workPath;
		ep.getPage().pageData.setLines(ret);
	}

	static void saveAs(PlainPage page) throws Exception {
		EditorPanel editor = page.uiComp;
		JFileChooser chooser = new JFileChooser(page.pageData.workPath);
		int returnVal = chooser.showSaveDialog(editor);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String fn = chooser.getSelectedFile().getAbsolutePath();
			if (new File(fn).exists() && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(editor,
					"file exists, are you sure to overwrite?", "save as...", JOptionPane.YES_NO_OPTION)) {
				page.ui.message("not renamed");
				return;
			}
			page.pageData.setFn(fn);
			U.saveFileHistory(fn, page.cy + 1);
			editor.changeTitle();
			page.ui.message("file renamed");
			savePageToFile(page);
		}
	}

	static boolean saveFile(PlainPage page) throws Exception {
		if (page.changedOutside && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(page.uiComp,
				"File Changed Outside!! Do you really want to overwrite it?", "File Changed Outside!!",
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
				if (new File(fn).exists() && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(page.uiComp,
						"Are you sure to overwrite?", "File exists", JOptionPane.YES_NO_OPTION)) {
					page.ui.message("saved canceled");
					return false;
				}
				page.pageData.setFn(fn);
				page.uiComp.changeTitle();
				U.saveFileHistory(fn, page.cy + 1);
			} else {
				return false;
			}
		}
		return savePageToFile(page);

	}

	static void saveFileHistory(String fn, int line) throws IOException {
		File fhn = getFileHistoryName();
		if (new File(fn).getAbsolutePath().startsWith(getMyDir().getAbsolutePath())) {
			// dont save neoeedit internal file in history
			return;
		}
		OutputStream out = new FileOutputStream(fhn, true);
		out.write(String.format("\n%s|%s:", fn, line).getBytes("utf8"));
		out.close();
		saveDirHistory(fn);
	}

	private static void saveDirHistory(String fn) throws IOException {
		File dir = new File(fn).getParentFile();
		if (dir == null)
			return;
		String s = dir.getAbsolutePath();

		String old = FileUtil.readString(new FileInputStream(getDirHistoryName()), null);
		List<String> his = Arrays.asList(old.split("\n"));
		BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(getDirHistoryName()), UTF8));
		if (!his.contains(s)) {
			out.write(s);
			out.write("\n");
			out.write(old);
		} else {
			out.write(s);
			out.write("\n");
			List<String> his2 = new ArrayList(his);
			his2.remove(s);
			for (String line : his2) {
				out.write(line);
				out.write("\n");
			}
		}
		out.close();
	}

	static boolean savePageToFile(PlainPage page) throws Exception {
		try {
			System.out.println("save " + page.pageData.getFn());
			if (page.pageData.encoding == null) {
				page.pageData.encoding = UTF8;
			}
			BufferedWriter out = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(page.pageData.getFn()), page.pageData.encoding));
			for (int i = 0; i < page.pageData.lines.size(); i++) {
				out.write(page.pageData.lines.get(i).toString());
				out.write(page.pageData.lineSep);
			}
			out.close();
			page.pageData.fileLastModified = new File(page.pageData.getFn()).lastModified();
			page.changedOutside = false;
			return true;
		} catch (Throwable ex) {
			U.showSelfDispMessage(page, "error when save file:" + ex, 1000 * 8);
			ex.printStackTrace();
			return false;
		}
	}

	static void scale(int amount, Paint ui) {
		if (amount > 0) {
			ui.scalev *= 1.1f;
		} else if (amount < 0) {
			ui.scalev *= 0.9f;
		}
	}

	static void setClipBoard(String s) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), null);
	}

	static boolean setEncodingByUser(PlainPage plainPage, String msg) {
		String s = JOptionPane.showInputDialog(plainPage.uiComp, msg, plainPage.pageData.encoding);
		if (s == null) {
			return false;
		}
		try {
			"a".getBytes(s);
		} catch (Exception e) {
			plainPage.ui.message("bad encoding:" + s);
			return false;
		}
		plainPage.pageData.encoding = s;
		return true;
	}

	public static void setFont(PlainPage pp, String font) {
		Font f = new Font(font, Font.PLAIN, 12);
		ArrayList fonts = new ArrayList(Arrays.asList(U.fontList));
		fonts.add(0, f);
		U.fontList = (Font[]) fonts.toArray(new Font[fonts.size()]);
		// clear cache
		charWidthCaches.clear();
		charWidthCaches256 = new Object[256][];
		// showSelfDispMessage(pp, "Deprecated! Please set font by editing
		// data.py in <home>/.neoeedit", 3000);
	}

	static void setFrameSize(JFrame f, int w, int h) {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		Point p = U.Config.readFrameSize();
		f.setSize(Math.min(p.x, Math.min(dim.width, w)), Math.min(p.y, Math.min(dim.height, h)));
	}

	static void showHelp(final Paint ui, final EditorPanel uiComp) {
		if (ui.aboutImg != null) {
			return;
		}
		U.startThread(new Thread() {
			@Override
			public void run() {
				try {
					int w = uiComp.getWidth();
					int h = 60;
					ui.aboutImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
					Graphics2D gi = ui.aboutImg.createGraphics();
					gi.setColor(Color.BLUE);
					gi.fillRect(0, 0, w, h);
					gi.setColor(Color.CYAN);
					gi.setFont(new Font("Arial", Font.BOLD, 40));
					gi.drawString("NeoeEdit", 6, h - 20);
					gi.setColor(Color.YELLOW);
					gi.setFont(new Font("Arial", Font.PLAIN, 16));
					gi.drawString("press F1 key to see all commands", 6, h - 6);
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
		});

	}

	public static void showHexOfString(String s, PlainPage pp) throws Exception {
		PlainPage p2 = new PlainPage(pp.uiComp, PageData.newEmpty(String.format("Hex for String #%s", randomID())));
		p2.pageData.workPath = pp.pageData.workPath;
		p2.ui.applyColorMode(pp.ui.colorMode);
		List<CharSequence> sbs = new ArrayList<CharSequence>();
		sbs.add(new StringBuilder(String.format("Hex for '%s'", s)));
		for (char c : s.toCharArray()) {
			sbs.add(c + ":" + UnicodeFormatter.charToHex(c));
		}
		p2.pageData.setLines(sbs);
	}

	public static void showPageListPage(EditorPanel ep) throws Exception {
		if (findAndShowPageListPage(ep, titleOfPages(ep), 0, true)) {
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

	static void showResult(PlainPage pp, List<String> all, String type, String name, String text, String fnFilter,
			int[] cnts) throws Exception {
		String withFilter = "";
		if (fnFilter != null && fnFilter.length() > 0) {
			withFilter = String.format(" with filter '" + fnFilter + "'");
		}
		String cntInfo = "";
		if (cnts != null) {
			cntInfo = String.format(",searched %d files", cnts[0]);
			if (cnts[1] != 0) {
				cntInfo += String.format(", skipped binary:%d", cnts[1]);
			}
			if (cnts[2] != 0) {
				cntInfo += String.format(", filtered:%d", cnts[2]);
			}
		}
		PlainPage p2 = new PlainPage(pp.uiComp, PageData.newEmpty(String.format("(%s)'%s' in %s '%s'%s %s #%s",
				all.size(), text, type, name, withFilter, cntInfo, randomID())));
		p2.pageData.workPath = pp.pageData.workPath;
		p2.ui.applyColorMode(pp.ui.colorMode);
		List<CharSequence> sbs = new ArrayList<CharSequence>();
		sbs.add(new StringBuilder(
				String.format("find %s results in '%s'%s for '%s' %s", all.size(), name, withFilter, text, cntInfo)));
		for (Object o : all) {
			sbs.add(o.toString());
		}
		p2.pageData.setLines(sbs);
		if (type.equals("file")) {
			p2.searchResultOf = name;
		}
		// gc();
	}

	public static void showSelfDispMessage(PlainPage pp, String msg, int disapearMS) {
		long now = System.currentTimeMillis();
		pp.ui.msgs.add(new Object[] { msg, now + disapearMS, -1 /* draw width */ });
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
		if (cx <= 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder(cx);
		sb.setLength(cx);
		for (int i = 0; i < cx; i++) {
			sb.setCharAt(i, ' ');
		}
		return sb.toString();
	}

	static List<CharSequence> splitToken(CharSequence s) {
		StringBuilder sb = new StringBuilder();
		List<CharSequence> sl = new ArrayList<CharSequence>();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
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

	static List<String> split(String all, char sep) {
		List<String> s1 = new ArrayList<String>();
		int p1 = 0;
		while (true) {
			int p2 = all.indexOf(sep, p1);
			if (p2 < 0) {
				String s2 = (String) all.subSequence(p1, all.length());
				// if (s2.indexOf('\r') >= 0) {
				// String[] ss2 = s2.split("\\r");
				// for (String ss : ss2) {
				// s1.add(ss);
				// }
				// } else {
				s1.add(s2);
				// }
				break;
			} else {
				String s2 = (String) all.subSequence(p1, p2);
				// if (s2.indexOf('\r') >= 0) {
				// String[] ss2 = s2.split("\\r");
				// for (String ss : ss2) {
				// s1.add(ss);
				// }
				// } else {
				s1.add(s2);
				// }
				p1 = p2 + 1;
			}
		}
		return s1;
	}

	static void startNoiseThread(final Paint ui, final EditorPanel uiComp) {
		U.startThread(new Thread() {
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
		});
	}

	static CharSequence subs(CharSequence sb, int a, int b) {
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
		return sb.subSequence(a, b);
	}

	// public static void switchPageInOrder(PlainPage pp) {
	// List<PlainPage> pps = pp.uiComp.pageSet;
	// if (pps.size() <= 1) {
	// return;
	// }
	// int i = (1 + pps.indexOf(pp)) % pps.size();
	// pp.uiComp.setPage(pps.get(i));
	// pp.uiComp.repaint();
	// }

	public static void switchToPageListPage(PlainPage pp) throws Exception {
		EditorPanel uiComp = pp.uiComp;
		// if (pp.pageData.getTitle().equals(U.titleOfPages(uiComp)) &&
		// uiComp.lastPage != null) {
		// if (uiComp.pageSet.contains(uiComp.lastPage)) {
		// uiComp.setPage(uiComp.lastPage, true);
		// } else {
		// uiComp.lastPage = null;
		// }
		// } else {
		// uiComp.lastPage = uiComp.getPage();
		showPageListPage(uiComp);
		// }
	}

	static String titleOfPages(EditorPanel ep) {
		return _TITLE_OF_PAGES + "@" + ep.hashCode();
	}

	static CharSequence trimLeft(CharSequence s) {
		int i = 0;
		while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
			i++;
		}
		return i > 0 ? s.subSequence(i, s.length()) : s;
	}

	// public static void switchToLastPage(PlainPage pp) {
	// EditorPanel uiComp = pp.uiComp;
	// PlainPage lastPage = uiComp.lastPage;
	// if (lastPage != null) {
	// uiComp.setPage(lastPage);
	// }
	// }

	public static String exportString(List<CharSequence> ss, String lineSep) {
		StringBuilder sb = new StringBuilder();
		boolean notfirst = false;
		for (CharSequence cs : ss) {
			if (notfirst)
				sb.append(lineSep);
			else
				notfirst = true;
			sb.append(cs);
		}
		return sb.toString();
	}

	static Font[] fontList = Config
			.getFont(new Font[] { new Font("Monospaced", Font.PLAIN, 12), new Font("Simsun", Font.PLAIN, 12) });

	public static List<CharSequence> removeTailR(List<String> split) {
		List<CharSequence> r = new ArrayList<CharSequence>();
		for (String s : split) {
			s = U.removeTailR(s).toString();
			// for console
			if (s.startsWith("\r")) {
				// if (r.size() > 0) {
				// r.remove(r.size() - 1);
				// }
				s = s.substring(1);
			}
			if (s.contains("\r")) { // lines that replacing the last line
				String[] ss = s.split("\\r");
				for (String s1 : ss)
					r.add(s1);
			} else {
				r.add(s);
			}
		}
		return r;
	}

	public static int between(int i, int min, int max) {
		return Math.min(max, Math.max(min, i));
	}

	public static final String e_png = "e.jpg";
	public static final String e2_png = "e2.jpg";
	public static final String e3_png = "e3.jpg";
	static Map<String, Image> appIcons = new HashMap();

	public static Image getAppIcon(String name) throws IOException {
		Image appIcon = appIcons.get(name);
		if (appIcon != null)
			return appIcon;
		appIcon = ImageIO.read(EditorPanel.class.getResourceAsStream("/" + name));
		appIcons.put(name, appIcon);
		return appIcon;
	}

	public static boolean isSpaceChar(char ch) {
		return Character.isSpaceChar(ch) || ch == '\t';
	}

	public static void changePathSep(PageData pageData, int cy) {
		if (cy >= pageData.lines.size()) {
			return;
		}
		String line = pageData.lines.get(cy).toString();
		int p1 = line.indexOf('/');
		String line2 = null;
		if (p1 >= 0) {
			line2 = line.replace('/', '\\');
		} else {
			int p2 = line.indexOf('\\');
			if (p2 >= 0) {
				line2 = line.replace('\\', '/');
			}
		}
		if (line2 != null) {
			pageData.editRec.deleteInLine(cy, 0, line.length());
			pageData.editRec.insertInLine(cy, 0, line2);
		}
	}

	public static String evalMath(final String str) {
		System.out.println("eval:" + str);
		return new MathExprParser(str).parse().stripTrailingZeros().toPlainString();
	}

	public static String getMathExprTail(String ss) {
		// System.out.println("getMathExprTail=" + ss);
		int p1 = ss.length();
		while (p1 > 0 && isMathExprChar(ss.charAt(p1 - 1))) {
			p1--;
		}
		ss = ss.substring(p1);
		// System.out.println("getMathExprTail2=" + ss);
		for (int i = 0; i < ss.length(); i++) {
			char ch = ss.charAt(i);
			if ("+-/*^x".indexOf(ch) >= 0)
				return ss;
		}
		// System.out.println("getMathExprTail ret empty");
		return "";
	}

	static boolean isMathExprChar(char c) {
		return isMathExprNumberChar(c) || ("+-/*^() ".indexOf(c) >= 0);
	}

	static boolean isMathExprNumberChar(int c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || c == '.' || c == 'E'
				|| c == 'x';
	}

	public static char charAtWhenMove(CharSequence line, int index) {
		if (line.length() == 0)
			return ' ';
		if (index >= line.length())
			return ' ';
		return line.charAt(index);
	}

	public static boolean getBool(Object o) {
		if (o == null)
			return false;
		if (o instanceof Boolean)
			return ((Boolean) o).booleanValue();
		String s = o.toString().toLowerCase();
		if ("y".equals(s) || "1".equals(s) || "true".equals(s))
			return true;
		if ("n".equals(s) || "0".equals(s) || "false".equals(s))
			return false;
		return false;
	}

	public static int getInt(Object o) {
		if (o == null)
			return 0;
		if (o instanceof Number)
			return ((Number) o).intValue();
		return (int) Float.parseFloat(o.toString());
	}

	public static float getFloat(Object o) {
		if (o == null)
			return 0;
		if (o instanceof Number)
			return ((Number) o).floatValue();
		return Float.parseFloat(o.toString());
	}

	public static int maxShowLength(CharSequence sb, int sx, int W, Graphics2D g2, Font[] fonts) {
		int w = 0;
		for (int i = sx; i < sb.length() - 1; i++) {
			char c = sb.charAt(i);
			if (c == '\t')
				w += TAB_WIDTH;
			else
				w += charWidth(g2, fonts, c);
			if (w > W) {
				return i + 1;
			}
		}
		return sb.length();
	}

}
