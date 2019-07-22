package neoe.ne;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import neoe.ne.CommandPanel.CommandPanelPaint;
import neoe.ne.Ime.Out;
import neoe.ne.Plugin.PluginAction;

public class PlainPage {

	class Cursor {

		void gotoLine() {
			String s = JOptionPane.showInputDialog(uiComp, "Goto Line");
			int line = -1;
			try {
				line = Integer.parseInt(s);
			} catch (Exception e) {
				line = -1;
			}
			if (line > pageData.roLines.getLinesize()) {
				line = -1;
			}
			if (line > 0) {
				line -= 1;
				sy = Math.max(0, line - showLineCnt / 2 + 1);
				cy = line;
				cx = 0;
				focusCursor();
			}
		}

		void moveDown() {
			cy += 1;
			if (cy >= pageData.roLines.getLinesize()) {
				if (rectSelectMode) {
					pageData.editRec.insertEmptyLine(cy);
					if (cx > 0) {
						pageData.editRec.insertInLine(cy, 0, U.spaces(cx));
					}
				} else {
					cy = pageData.roLines.getLinesize() - 1;
				}
			}
		}

		void moveEnd() {
			CharSequence line = pageData.roLines.getline(cy);
			int p1 = line.length();
			while (p1 > 0 && U.isSpaceChar(line.charAt(p1 - 1)))
				p1--;
			if (cx < p1 || cx >= line.length()) {
				cx = p1;
			} else {
				cx = Integer.MAX_VALUE;
			}
		}

		void moveHome() {
			CharSequence line = pageData.roLines.getline(cy);
			int p1 = 0;
			int len = line.length();
			while (p1 < len - 1 && U.isSpaceChar(line.charAt(p1)))
				p1++;
			if (cx > p1 || cx == 0) {
				cx = p1;
			} else {
				cx = 0;
			}
		}

		void moveLeft() {
			cx -= 1;
			if (cx < 0) {
				if (cy > 0 && !ptSelection.isRectSelecting()) {
					cy -= 1;
					cx = pageData.roLines.getline(cy).length();
				} else {
					cx = 0;
				}
			}
		}

		void moveLeftWord() {
			CharSequence line = pageData.roLines.getline(cy);
			cx = Math.max(0, cx - 1);
			char ch1 = U.charAtWhenMove(line, cx);
			while (true) {
				if (cx <= 0) {
					if (cy <= 0) {
						break;
					} else {
						cy--;
						line = pageData.roLines.getline(cy);
						cx = Math.max(0, line.length() - 1);
					}
				} else {
					cx--;
					if (U.isSkipChar(line.charAt(cx), ch1))
						continue;
					else {
						cx++;
						break;
					}
				}

			}
		}

		void movePageDown() {
			cy += showLineCnt;
			if (cy >= pageData.roLines.getLinesize()) {
				if (rectSelectMode) {
					String SP = U.spaces(cx);
					int cnt = cy - pageData.roLines.getLinesize() + 1;
					int p = pageData.roLines.getLinesize();
					for (int i = 0; i < cnt; i++) {
						pageData.editRec.insertEmptyLine(p);
						if (cx > 0) {
							pageData.editRec.insertInLine(p, 0, SP);
						}
					}
				} else {
					cy = pageData.roLines.getLinesize() - 1;
				}
			}
		}

		void movePageUp() {
			cy -= showLineCnt;
			if (cy < 0) {
				cy = 0;
			}
		}

		void moveRight() {
			cx += 1;
			if (ptSelection.isRectSelecting()) {
				if (cx > pageData.roLines.getline(cy).length()) {
					ptEdit.setLength(cy, cx);
				}
			} else if (cx > pageData.roLines.getline(cy).length() && cy < pageData.roLines.getLinesize() - 1) {
				cy += 1;
				cx = 0;
			}
		}

		void moveRightWord() {
			CharSequence line = pageData.roLines.getline(cy);
			char ch1 = U.charAtWhenMove(line, cx);
			cx = Math.min(line.length(), cx + 1);
			while (U.isSkipChar(U.charAtWhenMove(line, cx), ch1)) {
				cx = Math.min(line.length(), cx + 1);
				if (cx >= line.length()) {
					if (cy >= pageData.roLines.getLinesize() - 1) {
						break;
					} else {
						cy++;
						line = pageData.roLines.getline(cy);
						cx = 0;
					}
				}
			}
		}

		void moveToPair() {
			// move cursor between (){}[]<> pair
			if (cx - 1 < pageData.roLines.getline(cy).length() && cx - 1 >= 0) {
				char c = pageData.roLines.getline(cy).charAt(cx - 1);
				String pair = "(){}[]<>";
				int p1 = pair.indexOf(c);
				if (p1 >= 0) {
					if (p1 % 2 == 0) {
						PlainPage.this.ui.commentor.moveToPairMark(cx - 1, cy, pair.charAt(p1 + 1), c, 1);
					} else {
						PlainPage.this.ui.commentor.moveToPairMark(cx - 1, cy, pair.charAt(p1 - 1), c, -1);
					}
				}
			}
		}

		void moveUp() {
			cy -= 1;
			if (cy < 0) {
				cy = 0;
			}
		}

		void scroll(int amount) {
			sy += amount;
			if (sy >= pageData.roLines.getLinesize()) {
				sy = pageData.roLines.getLinesize() - 1;
			}
			if (sy < 0) {
				sy = 0;
			}
			uiComp.repaint();
		}

		void scrollHorizon(int amount) {
			sx += amount;
			if (sx < 0) {
				sx = 0;
			}
			uiComp.repaint();
		}

		void setSafePos(int x, int y, boolean record) {
			cy = Math.max(0, Math.min(pageData.roLines.getLinesize() - 1, y));
			cx = Math.max(0, Math.min(pageData.roLines.getline(cy).length(), x));
		}

		public void doMoveUpLangLevel() {
			PlainPage.this.ui.commentor.moveToPairMark(cx - 1, cy, '{', '}', -1);
		}
	}

	public class EasyEdit {

		public void append(String s) {
			cy = pageData.roLines.getLinesize() - 1;
			cx = pageData.roLines.getline(cy).length();
			insertString(s);
		}

		public void consoleAdjustToLastLine() {
			int size = pageData.lines.size();
			if (cy != size - 1) {
				cy = size - 1;
				cx = pageData.lines.get(size - 1).length();
				focusCursor();
				uiComp.repaint();
			}
		}

		public void consoleAppend(String s) {
			synchronized (console) {
				int size = pageData.lines.size();
				CharSequence lastLine = pageData.lines.get(size - 1);
				pageData.lines.remove(size - 1);
				cy = pageData.roLines.getLinesize() - 1;
				cx = pageData.roLines.getline(cy).length();
				insertString(s);
				pageData.lines.add(lastLine);
			}
		}

		public void consoleInsertChar(char ch) {
			synchronized (console) {
				consoleAdjustToLastLine();
				if (ch == KeyEvent.VK_ENTER) {
					consoleSubmitLastLine();

				} else if (ch == KeyEvent.VK_BACK_SPACE) {
					if (cx > 0) {
						pageData.editRec.deleteInLine(cy, cx - 1, cx);
						cx -= 1;
					}
				} else if (ch == KeyEvent.VK_DELETE) {
					if (cx < pageData.roLines.getline(cy).length()) {
						pageData.editRec.deleteInLine(cy, cx, cx + 1);
					}
				} else if (ch == KeyEvent.VK_ESCAPE) {
					int size = pageData.roLines.getline(cy).length();
					pageData.editRec.deleteInLine(cy, 0, size);
				} else {
					pageData.editRec.insertInLine(cy, cx, "" + ch);
					cx += 1;
				}
			}
			focusCursor();
			uiComp.repaint();

		}

		public void consoleSubmitLastLine() {
			cy = pageData.roLines.getLinesize() - 1;
			String sb = pageData.roLines.getline(cy).toString();
			if (sb.trim().length() == 0) {
				consoleAppend("\n");
			}
			pageData.editRec.deleteLines(cy, cy + 1);
			cx = 0;
			sb += "\n";
			console.submit(sb);
		}

		public void consoleUserInput(List<CharSequence> ss) {
			synchronized (console) {
				consoleAdjustToLastLine();
				int len = ss.size();
				if (len == 1) {
					pageData.editRec.insertInLine(cy, cx, ss.get(0));
					cx += ss.get(0).length();
				} else {
					pageData.editRec.deleteInLine(cy, cx, Integer.MAX_VALUE);
					pageData.editRec.insertInLine(cy, cx, ss.get(0));
					for (int i = 1; i < len; i++) {
						consoleSubmitLastLine();
						cy++;
						pageData.editRec.insertEmptyLine(cy);
						pageData.editRec.insertInLine(cy, 0, ss.get(i));
					}
					cx = ss.get(len - 1).length();
				}
			}
			focusCursor();
		}

		public void deleteLine(int cy) {
			deleteLineRange(cy, cy + 1);
			// cx = 0;
			// int len = pageData.roLines.getline(cy).length();
			// if (len > 0) {
			// pageData.editRec.deleteInLine(cy, 0, len);
			// }
			// pageData.editRec.deleteEmptyLine(cy);
		}

		public void deleteLineRange(int start, int end) {
			pageData.editRec.deleteLines(start, end);
		}

		public void deleteRect(Rectangle r) {
			int x1 = r.x;
			int y1 = r.y;
			int x2 = r.width;
			int y2 = r.height;
			if (rectSelectMode) {
				for (int i = y1; i <= y2; i++) {
					pageData.editRec.deleteInLine(i, x1, x2);
				}
				selectstartx = x1;
				selectstopx = x1;
			} else {
				if (y1 == y2 && x1 < x2) {
					pageData.editRec.deleteInLine(y1, x1, x2);
				} else if (y1 < y2) {
					pageData.editRec.deleteInLine(y1, x1, Integer.MAX_VALUE);
					pageData.editRec.deleteInLine(y2, 0, x2);
					deleteLineRange(y1 + 1, y2);
					pageData.editRec.mergeLine(y1);
				}
			}
			cx = x1;
			cy = y1;
			if (y2 - y1 > 400) {
				U.gc();
			}
			focusCursor();
		}

		public void deleteSpace() {
			// CharSequence line = pageData.roLines.getline(cy);
			int x0 = cx, y0 = cy;
			cursor.moveRightWord();
			int x2 = cx, y2 = cy;
			deleteRect(new Rectangle(x0, y0, x2, y2));
		}

		public void insert(char ch) {
			if (console != null) {
				consoleInsertChar(ch);
				return;
			}
			// Fix cy here! ?
			if (cy < 0)
				cy = 0;

			if (ch == KeyEvent.VK_ENTER) {
				if (ptSelection.isSelected()) {
					deleteRect(ptSelection.getSelectRect());
				}
				CharSequence sb = pageData.roLines.getline(cy);
				String indent = U.getIndent(sb.toString());
				CharSequence s = sb.subSequence(cx, sb.length());
				pageData.editRec.insertEmptyLine(cy + 1);
				pageData.editRec.insertInLine(cy + 1, 0, indent + U.trimLeft(s));
				pageData.editRec.deleteInLine(cy, cx, Integer.MAX_VALUE);
				cy += 1;
				cx = indent.length();
			} else if (ch == KeyEvent.VK_BACK_SPACE) {
				if (ptSelection.isSelected()) {
					deleteRect(ptSelection.getSelectRect());
				} else {
					if (rectSelectMode) {
						if (cx > 0) {
							Rectangle r = ptSelection.getSelectRect();
							for (int i = r.y; i <= r.height; i++) {
								pageData.editRec.deleteInLine(i, cx - 1, cx);
							}
							cx--;
							selectstartx = cx;
							selectstopx = cx;
						}
					} else {
						if (cx > 0) {
							pageData.editRec.deleteInLine(cy, cx - 1, cx);
							cx -= 1;
						} else {
							if (cy > 0) {
								cx = pageData.roLines.getline(cy - 1).length();
								pageData.editRec.mergeLine(cy - 1);
								cy -= 1;
							}
						}
					}
				}
			} else if (ch == KeyEvent.VK_DELETE) {
				if (ptSelection.isSelected()) {
					deleteRect(ptSelection.getSelectRect());
				} else {
					if (rectSelectMode) {
						Rectangle r = ptSelection.getSelectRect();
						for (int i = r.y; i <= r.height; i++) {
							pageData.editRec.deleteInLine(i, cx, cx + 1);
						}
						selectstartx = cx;
						selectstopx = cx;
					} else {
						if (cx < pageData.roLines.getline(cy).length()) {
							pageData.editRec.deleteInLine(cy, cx, cx + 1);
						} else {
							if (cy < pageData.roLines.getLinesize() - 1) {
								pageData.editRec.mergeLine(cy);
							}
						}
					}
				}
			} else if (ch == KeyEvent.VK_ESCAPE) {
				ptSelection.cancelSelect();
			} else {
				if (ptSelection.isSelected()) {
					deleteRect(ptSelection.getSelectRect());
				}
				if (rectSelectMode) {
					Rectangle r = ptSelection.getSelectRect();
					for (int i = r.y; i <= r.height; i++) {
						pageData.editRec.insertInLine(i, cx, "" + ch);
					}
					cx += 1;
					selectstartx = cx;
					selectstopx = cx;
				} else {
					pageData.editRec.insertInLine(cy, cx, "" + ch);
					cx += 1;
				}
			}
			focusCursor();
			if (!rectSelectMode) {
				ptSelection.cancelSelect();
			}
			uiComp.repaint();
		}

		public void insertString(List<CharSequence> ss, boolean userInput) {
			if (userInput && console != null) {
				consoleUserInput(ss);
				return;
			}
			// Fix cy here! ?
			if (cy < 0)
				cy = 0;

			if (ptSelection.isSelected()) {
				ptEdit.deleteRect(ptSelection.getSelectRect());
			}
			int len = ss.size();
			if (rectSelectMode) {
				Rectangle rect = ptSelection.getSelectRect();
				int pi = 0;
				for (int iy = rect.y; iy <= rect.height; iy++) {
					CharSequence s1 = ss.get(pi);
					pageData.editRec.insertInLine(iy, cx, s1);
					pi++;
					if (pi >= len) {
						pi = 0;
					}
				}
				if (len == 1) {
					selectstartx += ss.get(0).length();
					selectstopx += ss.get(0).length();
					cx += ss.get(0).length();
					savingFromSelectionCancel = true;
				}
			} else {
				if (len == 1) {
					pageData.editRec.insertInLine(cy, cx, ss.get(0));
					cx += ss.get(0).length();
				} else {
					CharSequence rem = pageData.roLines.getInLine(cy, cx, Integer.MAX_VALUE);
					pageData.editRec.deleteInLine(cy, cx, Integer.MAX_VALUE);
					pageData.editRec.insertInLine(cy, cx, ss.get(0));
					for (int i = 1; i < len; i++) {
						pageData.editRec.insertEmptyLine(cy + i);
						pageData.editRec.insertInLine(cy + i, 0, ss.get(i));
					}
					cy += len - 1;
					cx = ss.get(len - 1).length();
					pageData.editRec.insertInLine(cy, cx, rem);
				}
				ptSelection.cancelSelect();
			}
			if (len >= 5 && ui.comment == null) {
				isCommentChecked = true;
				U.startThread(new Thread() {
					@Override
					public void run() {
						U.guessComment(PlainPage.this);
					}
				});
			}
			focusCursor();
		}

		public void insertString(String s) {
			insertString(s, false);
		}

		public void insertString(String s, boolean userInput) {
			insertString(U.removeTailR(U.split(s, U.N)), userInput);
		}

		public void moveLineLeft(int cy) {
			String s = pageData.roLines.getline(cy).toString();
			if (s.length() > 0 && (s.charAt(0) == '\t' || s.charAt(0) == ' ')) {
				pageData.editRec.deleteInLine(cy, 0, 1);
			}
			cx -= 1;
			if (cx < 0) {
				cx = 0;
			}
		}

		public void moveLineRight(int cy) {
			pageData.editRec.insertInLine(cy, 0, "\t");
			cx += 1;
		}

		public void moveRectLeft(int from, int to) {
			for (int i = from; i <= to; i++) {
				moveLineLeft(i);
			}
		}

		public void moveRectRight(int from, int to) {
			for (int i = from; i <= to; i++) {
				moveLineRight(i);
			}
		}

		public void setLength(int cy, int cx) {
			int oldLen = pageData.roLines.getline(cy).length();
			if (cx - oldLen > 0) {
				pageData.editRec.insertInLine(cy, oldLen, U.spaces(cx - oldLen));
			}
		}

		public void wrapLines(int cx) throws Exception {
			int lineLen = 0;
			{
				int len = 0;
				CharSequence sb = pageData.roLines.getInLine(cy, 0, cx);
				for (int i = 0; i < sb.length(); i++) {
					len += (sb.charAt(i) > 255) ? 2 : 1;
				}
				lineLen = Math.max(10, len);
			}
			ui.message("wrapLine at " + lineLen);
			if (ptSelection.isSelected()) {
				ptSelection.cancelSelect();
			}
			List<CharSequence> newtext = new ArrayList<CharSequence>();
			for (int y = 0; y < pageData.lines.size(); y++) {
				if (pageData.lines.get(y).length() * 2 > lineLen) {
					int len = 0;
					CharSequence sb = pageData.roLines.getline(y);
					int start = 0;
					for (int i = 0; i < sb.length(); i++) {
						len += (sb.charAt(i) > 255) ? 2 : 1;
						if (len >= lineLen) {
							newtext.add(sb.subSequence(start, i + 1).toString());
							start = i + 1;
							len = 0;
						}
					}
					if (start < sb.length()) {
						newtext.add(sb.subSequence(start, sb.length()).toString());
					}
				} else {
					newtext.add(pageData.lines.get(y).toString());
				}
			}
			String title = "wrapped " + pageData.getTitle() + " #" + U.randomID();
			PlainPage p2 = new PlainPage(uiComp, PageData.newEmpty(title));
			p2.pageData.workPath = pageData.workPath;
			p2.pageData.setLines(newtext);
		}
	}

	public class Paint {

		class Comment {

			void markBox(Graphics2D g2, int x, int y) {
				if (y >= sy && y <= sy + showLineCnt && x >= sx) {
					CharSequence sb = pageData.roLines.getline(y);
					int w1 = x > 0 ? U.strWidth(g2, U.fontList, sb.subSequence(sx, x).toString(), TABWIDTH) : 0;
					String c = sb.subSequence(x, x + 1).toString();
					int w2 = U.strWidth(g2, U.fontList, c, TABWIDTH);
					g2.setColor(Color.WHITE);
					g2.drawRect(w1 - 1, (y - sy) * (lineHeight + lineGap) - 1, w2, lineHeight);
					g2.setColor(colorNormal);
					g2.drawRect(w1, (y - sy) * (lineHeight + lineGap), w2, lineHeight);
					U.drawString(g2, U.fontList, c, w1, lineHeight + (y - sy) * (lineHeight + lineGap));
				}
			}

			void markGutLine(Graphics2D g2, int y1, int y2) {
				if (y1 > y2) {
					int t = y1;
					y1 = y2;
					y2 = t;
				}
				int o1 = y1, o2 = y2;
				y1 = Math.min(Math.max(y1, sy), sy + showLineCnt);
				y2 = Math.min(Math.max(y2, sy), sy + showLineCnt);
				int scy1 = 5 + (y1 - sy) * (lineHeight + lineGap);
				int scy2 = -8 + (y2 + 1 - sy) * (lineHeight + lineGap);
				g2.setColor(colorGutMark1);
				g2.drawLine(-6, scy1 - 1, -6, scy2 - 1);
				if (o1 == y1) {
					g2.setColor(colorGutMark1);
					g2.drawLine(-6, scy1 - 1, -1, scy1 - 1);
				}
				if (o2 == y2) {
					g2.setColor(colorGutMark1);
					g2.drawLine(-6, scy2 - 1, -1, scy2 - 1);
				}
				g2.setColor(colorGutMark2);
				g2.drawLine(-5, scy1, -5, scy2);
				if (o1 == y1) {
					g2.setColor(colorGutMark2);
					g2.drawLine(-5, scy1, 0, scy1);
				}
				if (o2 == y2) {
					g2.setColor(colorGutMark2);
					g2.drawLine(-5, scy2, 0, scy2);
				}
			}

			void moveToPairMark(int cx2, int cy2, char ch, char ch2, int inc) {
				int[] c1 = new int[] { cx2, cy2 };
				U.findchar(PlainPage.this, ch, inc, c1, ch2);
				if (c1[0] >= 0) {// found
					cx = c1[0] + 1;
					int delta = Math.abs(cy - c1[1]);
					if (delta >= 10) {
						ui.message(String.format("moved across %,d lines", delta));
					}
					cy = c1[1];
					focusCursor();
				}
			}

			void pairMark(Graphics2D g2, int cx2, int cy2, char ch, char ch2, int inc) {
				int[] c1 = new int[] { cx2, cy2 };
				U.findchar(PlainPage.this, ch, inc, c1, ch2);
				if (c1[0] >= 0) {// found
					markBox(g2, cx2, cy2);
					markBox(g2, c1[0], c1[1]);
					if (cy2 != c1[1]) {
						markGutLine(g2, cy2, c1[1]);
					}
				}
			}
		}

		BufferedImage aboutImg;
		boolean aboutOn;
		int aboutY;

		boolean closed = false;
		Color colorBg, colorComment, colorComment2, colorCurrentLineBg, colorDigit, colorGutLine, colorGutNumber,
				colorKeyword, colorGutMark1, colorGutMark2, colorReturnMark;
		int colorMode;
		/**
		 * 0:white mode 1: black mode 2: blue mode * 1 bg, 2 normal, 3 keyword, 4 digit,
		 * 5 comment, 6 gutNumber, 7 gutLine, 8 currentLineBg, 9 comment2
		 */
		int[][] ColorModes = null;
		Color colorNormal = Color.BLACK;
		String[] comment = null;
		Comment commentor = new Comment();
		CommandPanelPaint cp = new CommandPanelPaint(PlainPage.this);
		Dimension dim;

		int gutterWidth = 40;
		int lineGap = 5;
		int lineHeight = U.fontList[0].getSize();
		long MSG_VANISH_TIME = 3000;
		List<Object[]> msgs = new ArrayList<Object[]>();
		boolean noise = false;

		int noisesleep = 500;

		float scalev = 1;

		int TABWIDTH = 40;
		private int nextXToolBar;
		private boolean fpsOn = false;
		private int charCntInLine;
		private int textAreaWidth;
		private boolean inComment;
		private String commentClose;
		private String commentStart;

		Paint() {
			try {
				TABWIDTH = U.Config.readTabWidth();
				int cm = U.Config.getDefaultColorMode();
				applyColorMode(cm);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void applyColorMode(int i) throws IOException {
			if (ColorModes == null || U.Config.configChanged()) {
				ColorModes = U.Config.loadColorModes();
			}

			if (i >= ColorModes.length) {
				i = 0;
			}
			colorMode = i;
			int[] cm = ColorModes[i];
			colorBg = new Color(cm[0]);
			colorNormal = new Color(cm[1]);
			colorKeyword = new Color(cm[2]);
			colorDigit = new Color(cm[3]);
			colorComment = new Color(cm[4]);
			colorGutNumber = new Color(cm[5]);
			colorGutLine = new Color(cm[6]);
			colorCurrentLineBg = new Color(cm[7]);
			colorComment2 = new Color(cm[8]);
			colorGutMark1 = new Color(cm[9]);
			colorGutMark2 = new Color(cm[10]);
			colorReturnMark = new Color(cm[11]);
		}

		void drawGutter(Graphics2D g2) {
			g2.setColor(colorGutNumber);
			for (int i = 0; i < showLineCnt; i++) {
				if (sy + i + 1 > pageData.roLines.getLinesize()) {
					break;
				}
				U.drawStringShrink(g2, U.fontList, "" + (sy + i + 1), 0, lineHeight + (lineHeight + lineGap) * i,
						gutterWidth / scalev);
			}
		}

		void drawNextToolbarText(Graphics2D g2, String s) {
			g2.setColor(colorGutMark2);
			nextXToolBar += 10 + U.drawString(g2, U.fontList, s, 10 + nextXToolBar, lineHeight);
		}

		void drawReturn(Graphics2D g2, int w, int py) {
			g2.setColor(colorReturnMark);
			g2.drawLine(w, py - lineHeight + U.fontList[0].getSize(), w + 3, py - lineHeight + U.fontList[0].getSize());
		}

		void drawSelect(Graphics2D g2, int y1, int x1, int x2) {
			int scry = y1 - sy;
			if (scry < showLineCnt) {
				CharSequence s = pageData.roLines.getline(y1);
				if (sx > s.length()) {
					return;
				}
				s = U.subs(s, sx, sx + Math.min(charCntInLine, s.length() - sx));
				x1 -= sx;
				x2 -= sx;
				if (x1 < 0) {
					x1 = 0;
				}
				int x2a = x2;
				if (x2a < 0) {
					x2a = 0;
				}
				if (x2a > s.length()) {
					x2a = s.length();
				}
				if (x1 > s.length()) {
					x1 = s.length();
				}
				if (x1 == x2) {
					int w1 = U.strWidth(g2, U.fontList, s.subSequence(0, x1).toString(), TABWIDTH);
					g2.fillRect(w1, scry * (lineHeight + lineGap), 3, lineHeight + lineGap);
				} else {
					int w1 = U.strWidth(g2, U.fontList, s.subSequence(0, x1).toString(), TABWIDTH);
					int w2 = x2 > x2a ? textAreaWidth
							: U.strWidth(g2, U.fontList, s.subSequence(0, x2a).toString(), TABWIDTH);
					g2.fillRect(w1, scry * (lineHeight + lineGap), (w2 - w1), lineHeight + lineGap);
				}
			}
		}

		void drawSelectLine(Graphics2D g2, int y1, int y2) {
			int scry = U.between(y1 - sy, 0, showLineCnt);
			int scry2 = U.between(y2 - sy, 0, showLineCnt);
			if (y1 < y2) {
				g2.fillRect(0, scry * (lineHeight + lineGap), textAreaWidth, (lineHeight + lineGap) * (scry2 - scry));

			}
		}

		private void drawSelfDispMessages(Graphics2D g) {
			long now = System.currentTimeMillis();
			for (int i = 0; i < msgs.size(); i++) {
				Object[] row = msgs.get(i);
				long disapear = (Long) row[1];
				if (disapear < now) {
					msgs.remove(i);
					i--;
				}
			}
			if (!msgs.isEmpty()) {
				// System.out.println("msgs:"+msgs.size());

				int w = U.maxWidth(msgs, g, U.fontList) + 100;
				int h = 30 * msgs.size() + 60;
				g.setXORMode(Color.BLACK);
				g.setPaintMode();
				g.setColor(Color.decode("0xFFCCFF"));
				g.fillRoundRect((dim.width - w) / 2, (dim.height - h) / 2, w, h, 3, 3);
				g.setColor(Color.BLACK);
				for (int i = 0; i < msgs.size(); i++) {
					Object[] row = msgs.get(i);
					int w1 = (Integer) row[2];
					U.drawString(g, U.fontList, row[0].toString(), (dim.width - w1) / 2,
							(10 + dim.height / 2 + 30 * (i - msgs.size() / 2)));
				}
			}

		}

		int drawStringLine(Graphics2D g2, Font[] fonts, CharSequence s, int x, int y, boolean isCurrentLine) {
			int w = 0;
			if (inComment) {
				int p1 = U.indexOf(s, commentClose, 0);
				if (p1 >= 0) {
					inComment = false;
					CharSequence s1 = s.subSequence(0, p1 + commentClose.length());
					CharSequence s2 = s.subSequence(p1 + commentClose.length(), s.length());
					int w1 = drawText(g2, fonts, s1, x, y, true, isCurrentLine);
					w = w1 + drawText(g2, fonts, s2, x + w1, y, false, isCurrentLine);
				} else {
					w = drawText(g2, fonts, s, x, y, true, isCurrentLine);
				}
			} else {
				int commentPos = getCommentPos(s);
				if (commentPos >= 0) {
					CharSequence s1 = s.subSequence(0, commentPos);
					CharSequence s2 = s.subSequence(commentPos, s.length());
					if (inComment) {
						int p1 = U.indexOf(s2, commentClose, commentStart.length());
						if (p1 >= 0) {
							CharSequence s2a = s2.subSequence(0, p1 + commentClose.length());
							CharSequence s2b = s2.subSequence(p1 + commentClose.length(), s2.length());
							int w1 = drawText(g2, fonts, s1, x, y, false, isCurrentLine);
							int w2 = w1 + drawText(g2, fonts, s2a, x + w1, y, true, isCurrentLine);
							w = w2 + drawText(g2, fonts, s2b, x + w2, y, false, isCurrentLine);
							inComment = false;
						} else {
							int w1 = drawText(g2, fonts, s1, x, y, false, isCurrentLine);
							w = w1 + drawText(g2, fonts, s2, x + w1, y, true, isCurrentLine);
						}
					} else {
						int w1 = drawText(g2, fonts, s1, x, y, false, isCurrentLine);
						w = w1 + drawText(g2, fonts, s2, x + w1, y, true, isCurrentLine);
					}
				} else {
					w = drawText(g2, fonts, s, x, y, false, isCurrentLine);
				}
			}
			return w;
		}

		int drawText(Graphics2D g2, Font[] fonts, CharSequence s, int x, int y, boolean isComment,
				boolean isCurrentLine) {
			int w = 0;
			if (isComment) {
				List<String> ws = U.split(s.toString(), '\t');
				int i = 0;
				for (CharSequence s1 : ws) {
					if (i++ != 0) {
						g2.drawImage(U.tabImg, x + w, y - lineHeight, null);
						w += TABWIDTH;
					}
					w += U.drawTwoColor(g2, fonts, s1.toString(), x + w, y, colorComment, colorComment2, 1,
							isCurrentLine, lineHeight);
					if (w > dim.width - gutterWidth) {
						break;
					}
				}
			} else {
				List<CharSequence> s1x = U.splitToken(s);
				for (CharSequence s1c : s1x) {
					String s1 = s1c.toString();
					if (s1.equals("\t")) {
						g2.drawImage(U.tabImg, x + w, y - lineHeight, null);
						w += TABWIDTH;
					} else {
						// int highlightid =
						U.getHighLightID(s1, g2, colorKeyword, colorDigit, colorNormal);
						U.drawString(g2, U.fontList, s1, x + w, y, isCurrentLine, lineHeight);
						w += U.stringWidth(g2, U.fontList, s1);
					}
					if (w > dim.width - gutterWidth) {
						break;
					}
				}
			}
			return w;
		}

		void drawTextLines(Graphics2D g2, Font[] fonts, int charCntInLine) {
			int y = sy;
			int py = lineHeight;
			for (int i = 0; i < showLineCnt; i++) {
				if (y >= pageData.roLines.getLinesize()) {
					break;
				}
				CharSequence sb = pageData.roLines.getline(y);
				if (sx < sb.length()) {
					int chari2 = Math.min(charCntInLine + sx, sb.length());
					CharSequence s = U.subs(sb, sx, chari2);
					g2.setColor(colorNormal);
					int w = drawStringLine(g2, fonts, s, 0, py, y == cy && !Gimp.glowDisabled || Gimp.glowAll);
					// U.strWidth(g2,s,TABWIDTH);
					drawReturn(g2, w, py);
				} else {
					drawReturn(g2, 0, py);
				}
				y += 1;
				py += lineHeight + lineGap;
			}
		}

		void drawToolbar(Graphics2D g2) {
			Ime.ImeInterface ime = Ime.getCurrentIme();
			String s1 = "<F1>:Help, " + (pageData.encoding == null ? "-" : pageData.encoding)
					+ (pageData.lineSep.equals("\n") ? ", U" : ", W") + ", Line:" + pageData.roLines.getLinesize()
					+ ", X:" + (cx + 1) + ", undo:" + pageData.history.size() + ", " + (rectSelectMode ? "R, " : "")
					+ (ime == null ? "" : ime.getImeName() + ", ")
					+ (pageData.getFn() == null ? "-" : pageData.getFn() + (changedOutside ? " [ChangedOutside!]" : ""))
					+ (readonly ? ", ro" : "");
			g2.setColor(colorGutMark1);
			U.drawString(g2, U.fontList, s1, 2, lineHeight + 2);
			g2.setColor(colorGutMark2);
			nextXToolBar = 2 + U.drawString(g2, U.fontList, s1, 1, lineHeight + 1);
			if (msg != null) {
				if (System.currentTimeMillis() - msgtime > MSG_VANISH_TIME) {
					msg = null;
				} else {
					int w = U.stringWidth(g2, U.fontList, msg);
					g2.setColor(new Color(0xee6666));
					g2.fillRect(dim.width - w, 0, dim.width, lineHeight + lineGap);
					g2.setColor(Color.YELLOW);
					U.drawString(g2, U.fontList, msg, dim.width - w, lineHeight);
				}
			}
		}

		private int getCommentPos(CharSequence s) {
			if (comment == null) {
				inComment = false;
				return -1;
			}
			for (String c : comment) {
				int p = U.indexOf(s, c, 0);
				if (p >= 0) {
					if ("/*".equals(c)) {
						inComment = true;
						commentClose = "*/";
						commentStart = c;
					} else if ("<!--".equals(c)) {
						inComment = true;
						commentClose = "-->";
						commentStart = c;
					}
					return p;
				}
			}
			inComment = false;
			return -1;
		}

		public void message(final String s) {
			msg = s;
			msgtime = System.currentTimeMillis();
			uiComp.repaint();
			U.repaintAfter(MSG_VANISH_TIME, uiComp);
			System.out.println(s);
		}

		void setNextColorMode() {
			if (++colorMode >= ColorModes.length) {
				colorMode = 0;
			}
		}

		void xpaint(Graphics g, Dimension size) {
			inComment = false;
			long fpsT1 = System.currentTimeMillis();
			Graphics2D g2 = (Graphics2D) g;

			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, uiComp.config.VALUE_TEXT_ANTIALIAS);
			this.dim = size;
			Graphics2D g3 = null;
			if (fpsOn) {
				g3 = (Graphics2D) g2.create();
			}
			boolean needRepaint = false;

			try {

				if (ui.cp.showCommandPanel) {
					cp.xpaint((Graphics2D) g, size);
					return;
				}

				if (!isCommentChecked) {// find comment pattern
					isCommentChecked = true;
					U.startThread(new Thread() {
						@Override
						public void run() {
							U.guessComment(PlainPage.this);
						}
					});
				}

				// g2.setFont(font);
				showLineCnt = Math.round((size.height - toolbarHeight) / ((lineHeight + lineGap) * scalev));
				charCntInLine = (int) ((size.width - gutterWidth) / (lineHeight) * 2 / scalev);
				textAreaWidth = size.width - gutterWidth;

				{ // change cy if needed
					if (cy >= pageData.roLines.getLinesize()) {
						cy = Math.max(0, pageData.roLines.getLinesize() - 1);
					}
				}
				// change sx if needed
				if (ptSelection.isRectSelecting()) {
					ptEdit.setLength(cy, cx);
				} else {
					cx = Math.min(pageData.roLines.getline(cy).length(), cx);
				}
				if (cx < sx) {
					sx = Math.max(0, cx - charCntInLine / 2);
				} else {
					sx = Math.max(0, Math.max(sx, cx - charCntInLine + 10));
					if (U.strWidth(g2, U.fontList, U.subs(pageData.roLines.getline(cy), sx, cx).toString(),
							TABWIDTH) > size.width - lineHeight * 3) {
						sx = Math.max(0, cx - charCntInLine / 2);
						int xx = charCntInLine / 4;

						while (xx > 0
								&& U.strWidth(g2, U.fontList, U.subs(pageData.roLines.getline(cy), sx, cx).toString(),
										TABWIDTH) > size.width - lineHeight * 3) {
							sx = Math.max(0, cx - xx - 1);
							xx /= 2; // quick guess
						}
					}
				}
				if (my > 0) // uiComp.grabFocus(); // bug: get focus when dont
							// need
				// apply mouse click position
				{
					if (my > 0 && my < toolbarHeight) {
					} else if (my > 0 && mx >= gutterWidth && my >= toolbarHeight) {
						mx -= gutterWidth;
						my -= toolbarHeight;
						mx = (int) (mx / scalev);
						my = (int) (my / scalev);
						cy = sy + my / (lineHeight + lineGap);
						if (cy >= pageData.roLines.getLinesize()) {
							cy = pageData.roLines.getLinesize() - 1;
						}
						CharSequence sb = pageData.roLines.getline(cy);
						sx = Math.min(sx, sb.length());
						cx = sx + U.computeShowIndex(sb.subSequence(sx, sx + Math.min(sb.length() - sx, charCntInLine)),
								mx, g2, U.fontList, TABWIDTH);
						my = 0;
						needRepaint = ptSelection.mouseSelection(sb);

					}
				}
				g2.setColor(colorBg);
				g2.fillRect(0, 0, size.width, size.height);
				if (noise) {
					U.paintNoise(g2, dim);
				}

				// draw toolbar
				drawToolbar(g2);

				// draw gutter
				g2.translate(0, toolbarHeight);
				g2.setColor(colorGutLine);
				g2.drawRect(gutterWidth, -1, dim.width - gutterWidth, dim.height - toolbarHeight);

				g2.scale(scalev, scalev);
				drawGutter(g2);
				// draw text
				g2.setClip(0, 0, (int) (dim.width / scalev), (int) ((dim.height - toolbarHeight) / scalev));
				g2.translate(gutterWidth / scalev, 0);

				{ // highlight current line
					int l1 = cy - sy;
					if (l1 >= 0 && l1 < showLineCnt) {
						g2.setColor(colorCurrentLineBg);
						g2.fillRect(0, l1 * (lineHeight + lineGap), size.width, lineHeight + lineGap - 1);
					}
				}

				// g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				// RenderingHints.VALUE_ANTIALIAS_ON);
				{ // draw selection background
					drawSelectionBackground(g2);

				}
				g2.setColor(colorNormal);
				drawTextLines(g2, U.fontList, charCntInLine);

				if (true) {// (){}[]<> pair marking
					if (cx - 1 < pageData.roLines.getline(cy).length() && cx - 1 >= 0) {
						char c = pageData.roLines.getline(cy).charAt(cx - 1);
						String pair = "(){}[]<>";
						int p1 = pair.indexOf(c);
						if (p1 >= 0) {
							if (p1 % 2 == 0) {
								commentor.pairMark(g2, cx - 1, cy, pair.charAt(p1 + 1), c, 1);
							} else {
								commentor.pairMark(g2, cx - 1, cy, pair.charAt(p1 - 1), c, -1);
							}
						}
					}
				}
				// draw cursor
				if (cy >= sy && cy <= sy + showLineCnt) {
					g2.setXORMode(new Color(0x30f0f0));
					CharSequence s = U.subs(pageData.roLines.getline(cy), sx, cx);
					int w = U.strWidth(g2, U.fontList, s.toString(), TABWIDTH);
					int y0 = (cy - sy) * (lineHeight + lineGap);
					g2.fillRect(w, y0, 2, lineHeight + 3);
					// draw preedit
					if (preeditText != null && preeditText.length() > 0) {
						g2.setPaintMode();
						g2.setColor(new Color(0xaaaa00));
						int w0 = U.strWidth(g2, U.fontList, preeditText, TABWIDTH);
						g2.fillRect(w, y0, w0 + 4, lineHeight + lineGap);
						g2.setColor(new Color(0x0000aa));
						U.drawString(g2, U.fontList, preeditText, w + 2, y0 + lineHeight);
					}

					// ime
					Ime.ImeInterface ime = Ime.getCurrentIme();
					if (ime != null) {
						ime.paint(g2, U.fontList, w, y0 + lineHeight + lineGap, g2.getClipBounds());
					}

				}

				if (aboutOn) {// about info
					g.setPaintMode();
					g.drawImage(aboutImg, 0, aboutY, null);
				}

				drawSelfDispMessages(g2);

			} catch (Throwable th) {
				th.printStackTrace();
				ui.message("Bug when xpaint():" + th);
			} finally {
				if (fpsOn) {
					long t2 = System.currentTimeMillis();
					int v = (int) (t2 - fpsT1);
					if (v == 0)
						drawNextToolbarText(g3, "↻∞");
					else {
						float fps = 1000f / v;
						if (fps >= 1) {
							drawNextToolbarText(g3, "↻" + (int) fps);
						} else {
							drawNextToolbarText(g3, String.format("↻%.3f", fps));
						}
					}
					g3.dispose();
				}
			}
			if (needRepaint) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						uiComp.repaint();
					}
				});
				return;
			}
		}

		private void drawSelectionBackground(Graphics2D g) {
			Graphics2D g2 = (Graphics2D) g.create();

			TexturePaint tp = getFillImagePaint();
			g2.setPaint(tp);

			// g2.setColor(colorNormal);
			if (rectSelectMode) {
				Rectangle r = ptSelection.getSelectRect();
				int x1 = r.x;
				int y1 = r.y;
				int x2 = r.width;
				int y2 = r.height;
				int start = Math.max(sy, y1);
				int end = Math.min(sy + showLineCnt + 1, y2);
				for (int i = start; i <= end; i++) {
					// g2.setColor(Color.BLUE);
					// g2.setXORMode(new Color(0xf0f030));
					drawSelect(g2, i, x1, x2);
				}
			} else {// select mode
				Rectangle r = ptSelection.getSelectRect();
				int x1 = r.x;
				int y1 = r.y;
				int x2 = r.width;
				int y2 = r.height;
				if (y1 == y2 && x1 < x2) {
					// g2.setColor(Color.BLUE);
					// g2.setXORMode(new Color(0xf0f030));
					drawSelect(g2, y1, x1, x2);
				} else if (y1 < y2) {
					// g2.setColor(Color.BLUE);
					// g2.setXORMode(new Color(0xf0f030));
					drawSelect(g2, y1, x1, Integer.MAX_VALUE);
					int start = Math.max(sy, y1 + 1);
					int end = Math.min(sy + showLineCnt + 1, y2);
					drawSelectLine(g2, start, end);
					drawSelect(g2, y2, 0, x2);
				}
			}
			g2.dispose();
		}

		private TexturePaint getFillImagePaint() {
			int w = 13;
			BufferedImage img = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			g.setColor(colorBg);
			g.fillRect(0, 0, w, w);
			g.setColor(dissRed(colorBg));
			int k = w - 1;
			int k1 = 0;
			int k2 = k - k1;
			g.drawLine(k1, k1, k2, k2);
			g.drawLine(k1, k2, k2, k1);
			g.dispose();
			return new TexturePaint(img, new Rectangle(0, 0, w, w));
		}

		private Color dissRed(Color c) {
			int r = c.getRed();
			if (r < 100)
				r = 220;
			else
				r = 30;
			return new Color(r, c.getGreen(), c.getBlue());
		}

		public void copy(Paint ui) throws IOException {
			this.applyColorMode(ui.colorMode);
			this.scalev = ui.scalev;
		}
	}

	public class Selection {

		public void cancelSelect() {
			selectstartx = cx;
			selectstarty = cy;
			selectstopx = cx;
			selectstopy = cy;
		}

		public void copySelected() {
			String s = U.exportString(getSelected(), pageData.lineSep);
			s = U.removeAsciiZero(s);
			U.setClipBoard(s);
			ui.message("copied " + s.length());
		}

		public void cutSelected() {
			copySelected();
			ptEdit.deleteRect(getSelectRect());
			cancelSelect();
		}

		public List<CharSequence> getSelected() {
			return pageData.roLines.getTextInRect(getSelectRect(), rectSelectMode);
		}

		public Rectangle getSelectRect() {
			int x1, x2, y1, y2;
			if (rectSelectMode) {
				y1 = selectstopy;
				y2 = selectstarty;
				x1 = selectstopx;
				x2 = selectstartx;
				if (y1 > y2) {
					int t = y1;
					y1 = y2;
					y2 = t;
				}
				if (x1 > x2) {
					int t = x1;
					x1 = x2;
					x2 = t;
				}
			} else {
				if (selectstopy < selectstarty) {
					y1 = selectstopy;
					y2 = selectstarty;
					x1 = selectstopx;
					x2 = selectstartx;
				} else {
					y2 = selectstopy;
					y1 = selectstarty;
					x2 = selectstopx;
					x1 = selectstartx;
					if (x1 > x2 && y1 == y2) {
						x1 = selectstopx;
						x2 = selectstartx;
					}
				}
			}
			return new Rectangle(x1, y1, x2, y2);
		}

		public boolean isRectSelecting() {
			return mshift && rectSelectMode;
		}

		public boolean isSelected() {
			Rectangle r = getSelectRect();
			int x1 = r.x;
			int y1 = r.y;
			int x2 = r.width;
			int y2 = r.height;
			if (rectSelectMode) {
				return x1 < x2;
			} else {
				if (y1 == y2 && x1 < x2) {
					return true;
				} else if (y1 < y2) {
					return true;
				}
				return false;
			}
		}

		/**
		 * 
		 * @param sb
		 * @return mouseSelectedOnLimit, need move screen and repaint
		 */
		public boolean mouseSelection(CharSequence sb) {
			if (mcount == 2) {
				int x1 = cx;
				int x2 = cx;
				if (sb.length() > x1 && Character.isJavaIdentifierPart(sb.charAt(x1))) {
					while (x1 > 0 && Character.isJavaIdentifierPart(sb.charAt(x1 - 1))) {
						x1 -= 1;
					}
				}
				if (sb.length() > x2 && Character.isJavaIdentifierPart(sb.charAt(x2))) {
					while (x2 < sb.length() - 1 && Character.isJavaIdentifierPart(sb.charAt(x2 + 1))) {
						x2 += 1;
					}
				}
				selectstartx = x1;
				selectstarty = cy;
				selectstopx = x2 + 1;
				selectstopy = cy;
			} else if (mcount == 3) {
				selectstartx = 0;
				selectstarty = cy;
				selectstopx = sb.length();
				selectstopy = cy;
			} else {
				if (mshift) {
					selectstopx = cx;
					selectstopy = cy;
					if (cy == sy && cy > 0) {
						sy--;
						return true;
					} else if (cy >= sy + showLineCnt - 1
							&& sy + 1 + showLineCnt / 2 < pageData.roLines.getLinesize() - 1) {
						sy++;
						return true;
					}
				} else {
					cancelSelect();
				}
			}
			return false;
		}

		public void selectAll() {
			selectstartx = 0;
			selectstarty = 0;
			selectstopy = pageData.roLines.getLinesize() - 1;
			if (selectstopy < 0)
				selectstopy = 0;
			selectstopx = pageData.roLines.getline(selectstopy).length();
		}

		public void selectLength(int x, int y, int length) {
			cx = x;
			cy = y;
			selectstartx = cx;
			selectstarty = cy;
			selectstopx = cx + length;
			selectstopy = cy;
			focusCursor();
			savingFromSelectionCancel = true;
		}
	}

	boolean changedOutside = false;

	Cursor cursor = new Cursor();
	public int cx;
	public int cy;
	boolean ignoreCase = true;
	boolean isCommentChecked = false;
	Dimension lastSize = new Dimension();
	int mcount;
	String msg;
	long msgtime;

	//
	boolean mshift;
	int mx, my;

	public PageData pageData;

	private String preeditText;
	public EasyEdit ptEdit = new EasyEdit();
	public U.FindAndReplace ptFind = new U.FindAndReplace(this);
	public Selection ptSelection = new Selection();

	boolean rectSelectMode = false;
	boolean savingFromSelectionCancel;
	String searchResultOf;
	int selectstartx, selectstarty, selectstopx, selectstopy;

	int showLineCnt;
	int sy, sx;

	boolean readonly = false;

	int toolbarHeight = 25;

	public Paint ui = new Paint();
	EditorPanel uiComp;

	public Console console;

	private PlainPage() {
	}

	public PlainPage(EditorPanel editor, PageData data) throws Exception {
		this();
		PlainPage cp = editor.getPage();
		if (cp != null) {
			this.ui.copy(cp.ui);
		}
		this.uiComp = editor;
		this.pageData = data;
		int index = editor.pageSet.indexOf(editor.getPage());
		if (index >= editor.pageSet.size() || index < 0) {
			index = 0;
		}
		editor.pageSet.add(index, this);
		editor.setPage(this, true);
		editor.changeTitle();
		// uiComp.ptCh.record(data.getTitle(), cx, cy);
		data.ref++;
	}

	public void close() {
		int index = uiComp.pageSet.indexOf(this);
		uiComp.pageSet.remove(this);

		pageData.ref--;
		if (pageData.ref == 0) {
			pageData.close();
		}

		if (index >= uiComp.pageSet.size()) {
			index = uiComp.pageSet.size() - 1;
		}
		if (index >= 0) {
			uiComp.setPage(uiComp.pageSet.get(index), true);
		} else {
			// nothing to show
			if (uiComp.frame != null)
				uiComp.frame.dispose();
		}
	}

	private void doGo(String line, boolean record) throws Exception {

		if (line.startsWith("set-font:")) {
			U.setFont(this, line.substring("set-font:".length()).trim());
		} else {
			if (searchResultOf == null || !U.gotoFileLine2(uiComp, line, searchResultOf, record)) {
				if (!U.gotoFileLine(line, uiComp, record /*
															 * , pageData.getTitle().equals(U.titleOfPages( uiComp))
															 */)) {
					if (!U.listDir(PlainPage.this, cy)) {
						U.launch(line);
					}
				}
			}
		}

	}

	public void focusCursor() {
		if (cy < sy) {
			sy = Math.max(0, cy - showLineCnt / 2 + 1);
		}
		if (showLineCnt > 0) {
			if (sy + showLineCnt - 1 < cy) {
				sy = Math.max(0, cy - showLineCnt / 2 + 1);
			}
		}
	}

	private static boolean isButtonDown(int i, MouseEvent evt) {
		int b = InputEvent.getMaskForButton(i);
		int ex = evt.getModifiersEx();
		return (ex & b) != 0;
	}

	public void keyPressed(KeyEvent evt) {
		Ime.ImeInterface ime = Ime.getCurrentIme();
		if (ime != null) {
			Out param = new Out();
			ime.keyPressed(evt, param);

			if (param.yield != null) {
				ptEdit.insertString(param.yield);
			}
			preeditText = param.preedit;
			if (param.consumed) {
				uiComp.repaint();
				return;
			}
		}

		if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
			if (ui.cp.showCommandPanel)
				ui.cp.showCommandPanel = false;
		}

		pageData.history.beginAtom();
		try {
			mshift = evt.isShiftDown();
			int ocx = cx;
			int ocy = cy;
			Commands cmd = U.mappingToCommand(evt);
			if (cmd == null) {
				int kc = evt.getKeyCode();
				boolean onlyShift = evt.isShiftDown() && !evt.isControlDown() && !evt.isAltDown();
				if (!onlyShift && (evt.isActionKey() || evt.isControlDown() || evt.isAltDown())
						&& (kc != KeyEvent.VK_SHIFT && kc != KeyEvent.VK_CONTROL && kc != KeyEvent.VK_ALT)) {
					String name = U.getKeyName(evt);
					PluginAction ac = U.pluginKeys.get(name);
					if (ac != null) {
						ac.run(this);
					} else {
						unknownCommand(evt);
					}
				}
			} else {
				processCommand(cmd);
			}

			boolean cmoved = !(ocx == cx && ocy == cy);
			if (cmoved) {
				if (evt.isShiftDown()) {
					selectstopx = cx;
					selectstopy = cy;
				} else {
					if (savingFromSelectionCancel) {
						savingFromSelectionCancel = false;
					} else {
						ptSelection.cancelSelect();
					}
				}
			}
			uiComp.repaint();
		} catch (Throwable e) {
			ui.message("err:" + e);
			e.printStackTrace();
		}
		pageData.history.endAtom();
	}

	public void keyReleased(KeyEvent env) {

	}

	public void keyTyped(KeyEvent env) {
		if (env.isControlDown() || env.isAltDown()) {
			// ignore
		} else {
			pageData.history.beginAtom();
			char kc = env.getKeyChar();
			if (kc == KeyEvent.VK_TAB && env.isShiftDown()) {
				Rectangle r = ptSelection.getSelectRect();
				if (r.y < r.height) {
					ptEdit.moveRectLeft(r.y, r.height);
				} else {
					ptEdit.moveLineLeft(cy);
				}
			} else if (kc == KeyEvent.VK_TAB && !env.isShiftDown() && selectstarty != selectstopy && !rectSelectMode) {
				Rectangle r = ptSelection.getSelectRect();
				ptEdit.moveRectRight(r.y, r.height);
			} else {
				Ime.ImeInterface ime = Ime.getCurrentIme();
				if (ime != null) {
					Out param = new Out();
					ime.keyTyped(env, param);

					if (param.yield != null) {
						ptEdit.insertString(param.yield);
					}
					preeditText = param.preedit;

					if (!param.consumed) {
						ptEdit.insert(kc);
					}
				} else {
					ptEdit.insert(kc);
					if (kc == '=') {
						String ss = pageData.roLines.getline(cy).toString();
						if (cx <= ss.length() && cx >= 3) {
							try {
								ss = ss.substring(0, cx);
								if (ss.endsWith("="))
									ss = ss.substring(0, ss.length() - 1);
								ss = U.getMathExprTail(ss);
								if (!ss.isEmpty()) {
									ptEdit.insertString(" " + U.evalMath(ss));
								}
							} catch (Exception ex) {
								/* ignore */
							}
						}
					}
				}

			}
			pageData.history.endAtom();
		}
	}

	public void mouseClicked(MouseEvent evt) {
		if (ui.cp.showCommandPanel) {
			ui.cp.mouseClicked(evt);
			if (ui.cp.clickedName != null) {
				try {
					processCommand(Commands.valueOf(ui.cp.clickedName));
					uiComp.repaint();
					mx = 0;
					my = 0;
					ui.cp.showCommandPanel = false;
				} catch (Throwable e) {
					ui.message("err:" + e);
					e.printStackTrace();
				}
				ui.cp.clickedName = null;
			}
			return;
		}
		{
			if (isButtonDown(4, evt) || isButtonDown(5, evt)) {
				return;
			}
		}
		int my = evt.getY();
		if (my > 0 && my < toolbarHeight) {
			if (pageData.getFn() != null) {
				U.setClipBoard(pageData.getFn());
				ui.message("filename copied");
				my = 0;
				// uiComp.repaint();
			} else {
				try {
					if (U.saveFile(this)) {
						ui.message("saved");
					}
				} catch (Throwable e) {
					ui.message("err:" + e);
					e.printStackTrace();
				}
			}
		} else {
			int mx = evt.getX();
			if (mx > 0 && mx < ui.gutterWidth) {
				cursor.gotoLine();
				uiComp.repaint();
			}
		}
	}

	public void mouseDragged(MouseEvent evt) {
		{
			if (isButtonDown(4, evt) || isButtonDown(5, evt) || isButtonDown(6, evt) || isButtonDown(7, evt)) {
				return;
			}
		}
		mx = evt.getX();
		my = evt.getY();
		mshift = true;
		uiComp.repaint();
	}

	public void mouseMoved(MouseEvent evt) {
		if (ui.cp.showCommandPanel) {
			ui.cp.mouseMoved(evt);
		}

	}

	public void mousePressed(MouseEvent evt) {
		{
			if (processButton4(evt) || processButton5(evt)) {
				return;
			}
		}
		mx = evt.getX();
		my = evt.getY();
		mshift = evt.isShiftDown();
		mcount = evt.getClickCount();
		uiComp.repaint();
		// System.out.println("m press");
	}

	public void mouseWheelMoved(MouseWheelEvent env) {
		int amount = env.getWheelRotation() * env.getScrollAmount();
		if (env.isControlDown()) {// scale
			U.scale(amount, ui);
			this.uiComp.repaint();
		} else if (env.isAltDown()) {// horizon scroll
			cursor.scrollHorizon(amount);
		} else {// scroll
			cursor.scroll(amount);
		}

	}

	/**
	 * Add support to on-the-spot pre-editing of input method like CJK IME, not
	 * perfect(the current java implementation seems not support pre-edit window
	 * following function), but keep up with what did as swing JTextComponent.
	 *
	 */
	public void preedit(String text, int committedCharacterCount) {
		// System.out.println("preedit:" + text + "," +
		// committedCharacterCount);
		if (committedCharacterCount > 0) {
			String commit = text.substring(0, committedCharacterCount);
			text = text.substring(committedCharacterCount);
			ptEdit.insertString(commit);
		}
		this.preeditText = text;
		uiComp.repaint();
	}

	private boolean processButton4(MouseEvent evt) {
		if (!isButtonDown(4, evt) && !isButtonDown(6, evt)) {
			return false;
		}
		pageBack();
		return true;
	}

	private void pageBack() {
		String s = uiComp.pageHis.back(U.getLocString(this));
		if (s != null) {
			try {
				doGo(s, false);
			} catch (Throwable e) {
				ui.message("err:" + e);
				e.printStackTrace();
			}
		}
	}

	private boolean processButton5(MouseEvent evt) {
		if (!isButtonDown(5, evt) && !isButtonDown(7, evt)) {
			return false;
		}
		pageForward();
		return true;
	}

	private void pageForward() {
		String s = uiComp.pageHis.forward(U.getLocString(this));
		if (s != null) {
			try {
				doGo(s, false);
			} catch (Throwable e) {
				ui.message("err:" + e);
				e.printStackTrace();
			}
		}
	}

	void processCommand(Commands cmd) throws Exception {
		switch (cmd) {
		case showHelp:
			U.showHelp(ui, uiComp);
			break;
		case saveAs:
			U.saveAs(this);
			break;
		case changePathSep:
			U.changePathSep(pageData, cy);
			break;
		case findNext:
			if (ptFind.back)
				ptFind.findPrev();
			else
				ptFind.findNext();
			break;
		case findPrev:
			if (!ptFind.back)
				ptFind.findPrev();
			else
				ptFind.findNext();
			break;
		case commandPanel:
			ui.cp.showCommandPanel = true;
			break;
		case reloadWithEncoding:
			if (pageData.getTitle().equals(U.titleOfPages(uiComp))) {
				pageData.setLines(U.getPageListStrings(uiComp));
			}
			U.reloadWithEncodingByUser(pageData.getFn(), this);
			PlainPage.this.changedOutside = false;
			break;
		case moveLeft:
			cursor.moveLeft();
			focusCursor();
			break;
		case moveRight:
			cursor.moveRight();
			focusCursor();
			break;
		case moveUp:
			if (readonly) {
				doMoveViewUp();
			} else {
				cursor.moveUp();
				focusCursor();
			}
			break;
		case moveDown:
			if (readonly) {
				doMoveViewDown();
			} else {
				cursor.moveDown();
				focusCursor();
			}
			break;
		case moveHome:
			cursor.moveHome();
			focusCursor();
			break;
		case moveEnd:
			cursor.moveEnd();
			focusCursor();
			break;
		case movePageUp:
			if (readonly) {
				sy = Math.max(0, sy - showLineCnt);
			} else {
				cursor.movePageUp();
				focusCursor();
			}
			break;
		case movePageDown:
			if (readonly) {
				sy = Math.min(sy + showLineCnt, pageData.roLines.getLinesize() - 1);
			} else {
				cursor.movePageDown();
				focusCursor();
			}
			break;
		case indentLeft:
			ptEdit.moveLineLeft(cy);
			focusCursor();
			break;
		case indentRight:
			ptEdit.moveLineRight(cy);
			focusCursor();
			break;
		case rectangleMode:
			rectSelectMode = !rectSelectMode;
			break;
		case makeNoise:
			ui.noise = !ui.noise;
			if (ui.noise) {
				U.startNoiseThread(ui, uiComp);
			}
			break;
		case toggleFps:
			ui.fpsOn = !ui.fpsOn;
			break;
		case switchLineSeperator:
			if (pageData.lineSep.equals("\n")) {
				pageData.lineSep = "\r\n";
			} else {
				pageData.lineSep = "\n";
			}
			break;
		case wrapLines:
			System.out.println("wrapLines!");
			ptEdit.wrapLines(cx);
			focusCursor();
			break;
		case Javascript:
			U.runScript(this);
			break;
		case moveLeftBig:
			cx = Math.max(0, cx - uiComp.getWidth() / 10);
			focusCursor();
			break;
		case moveRightBig:
			cx = cx + uiComp.getWidth() / 10;
			focusCursor();
			break;
		case switchColorMode:
			ui.setNextColorMode();
			ui.applyColorMode(ui.colorMode);
			break;

		case moveBetweenPair:
			cursor.moveToPair();
			break;

		case execute:
			if (cy < pageData.lines.size()) {
				U.exec(this, pageData.roLines.getline(cy).toString());
			}
			break;
		case hex:
			String s = U.exportString(ptSelection.getSelected(), pageData.lineSep);
			if (s != null && s.length() > 0) {
				U.showHexOfString(s, PlainPage.this);
			}
			break;
		case listFonts:
			U.listFonts(this);
			break;
		case copySelected:
			if (console != null) {
				if (!ptSelection.isSelected()) {
					console.submit(3);
				}
			}
			ptSelection.copySelected();
			break;
		case paste:
			ptEdit.insertString(U.getClipBoard(), true);
			break;
		case cut:
			ptSelection.cutSelected();
			break;
		case selectAll:
			ptSelection.selectAll();
			break;
		case deleteLine:
			if (ptSelection.isSelected()) {
				ptEdit.deleteRect(ptSelection.getSelectRect());
			} else {
				ptEdit.deleteLine(cy);
			}
			focusCursor();
			break;
		case openFile:
			U.openFile(this);
			break;
		case newPage:
			PlainPage pp = new PlainPage(uiComp, PageData.newEmpty("UNTITLED #" + U.randomID()));
			pp.pageData.workPath = this.pageData.workPath;
			pp.ptSelection.selectAll();
			break;
		case newWindow:
			EditorPanel ep = new EditorPanel(EditorPanelConfig.DEFAULT);
			ep.openWindow();
			// set default working path
			ep.getPage().pageData.workPath = pageData.workPath;
			break;
		case save:
			if (U.saveFile(this)) {
				ui.message("saved");
			}
			break;
		case gotoLine:
			cursor.gotoLine();
			break;
		case undo:
			pageData.history.undo(this);
			break;
		case find:
			ptFind.showFindDialog();
			break;
		case redo:
			pageData.history.redo(this);
			break;
		case closePage:
			U.closePage(this);
			break;
		case setEncoding:
			U.setEncodingByUser(this, "Set Encoding:");
			break;
		case moveToHead:
			cy = 0;
			cx = 0;
			focusCursor();
			break;
		case moveToTail:
			cy = pageData.roLines.getLinesize() - 1;
			cx = 0;
			focusCursor();
			break;
		case removeTralingSpace:
			U.removeTrailingSpace(pageData);
			break;
		case moveLeftWord:
			cursor.moveLeftWord();
			focusCursor();
			break;
		case deleteWord:
			ptEdit.deleteSpace();
			focusCursor();
			break;
		case moveRightWord:
			cursor.moveRightWord();
			focusCursor();
			break;
		case moveViewUp:
			doMoveViewUp();
			break;
		case moveViewDown:
			doMoveViewDown();
			break;
		case moveUpLangLevel:
			cursor.doMoveUpLangLevel();
		case resetScale:
			ui.scalev = 1;
			break;
		case go:
			if (cy < pageData.lines.size()) {
				String line = pageData.roLines.getline(cy).toString();
				doGo(line, true);
			}
			break;
		case launch:
			if (cy < pageData.lines.size()) {
				String line = pageData.roLines.getline(cy).toString();
				U.launch(line);
			}
			break;
		case readonlyMode:
			readonly = !readonly;
			break;
		case fileHistory:
			U.openFileHistory(uiComp);
			break;
		case dirHistory:
			U.openDirHistory(uiComp);
			break;
		case print:
			new U.Print(PlainPage.this).printPages();
			break;
		case pageList:
			U.switchToPageListPage(this);
			break;
		case quickSwitchPage:
			// U.switchPageInOrder(this);
			// U.switchToLastPage(this);
			doGo(uiComp.pageHis.back(U.getLocString(this)), false);
			break;
		case toggleIME:
			Ime.nextIme();
			Ime.ImeInterface ime = Ime.getCurrentIme();
			if (ime != null) {
				ime.setEnabled(true);
			}
			break;
		case ShellCommand:
			Shell.run(PlainPage.this, cy);
			break;
		case pageForward:
			pageForward();
			break;
		case pageBack:
			pageBack();
			break;
		case mathEval:
			String ss = pageData.roLines.getline(cy).toString();
			if (cx <= ss.length() && cx >= 3) {
				try {
					ss = ss.substring(0, cx);
					if (ss.endsWith("="))
						ss = ss.substring(0, ss.length() - 1);
					ss = U.getMathExprTail(ss);
					if (!ss.isEmpty()) {
						ptEdit.insertString(" = " + U.evalMath(ss));
					}
				} catch (Exception ex) {
					/* ignore */
				}
			}
			break;
		default:
			ui.message("unprocessed Command:" + cmd);
		}
	}

	private void doMoveViewUp() {
		sy = Math.max(0, sy - 1);
	}

	private void doMoveViewDown() {
		sy = Math.min(sy + 1, pageData.roLines.getLinesize() - 1);
	}

	private void unknownCommand(KeyEvent env) {
		StringBuilder sb = new StringBuilder();
		if (env.isControlDown()) {
			sb.append("Ctrl");
		}
		if (env.isAltDown()) {
			if (sb.length() > 0) {
				sb.append("-");
			}
			sb.append("Alt");
		}
		if (env.isShiftDown()) {
			if (sb.length() > 0) {
				sb.append("-");
			}
			sb.append("Shift");
		}
		if (sb.length() > 0) {
			sb.append("-");
		}
		sb.append(KeyEvent.getKeyText(env.getKeyCode()));
		ui.message("Unknow Command:" + sb);
	}

	public void xpaint(Graphics g, Dimension size) {
		if (!lastSize.equals(size)) { // resized
			lastSize = size;
			ui.cp.inited = false;
		}
		ui.xpaint(g, size);
	}

}
