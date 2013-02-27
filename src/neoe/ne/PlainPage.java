package neoe.ne;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import neoe.ne.Ime.Param;
import neoe.ne.CommandPanel.CommandPanelPaint;
import neoe.ne.U.RoSb;

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
			String line = pageData.roLines.getline(cy).toString();
			String lx = line.trim();
			int p1 = line.lastIndexOf(lx) + lx.length();
			if (cx < p1 || cx >= line.length()) {
				cx = p1;
			} else {
				cx = Integer.MAX_VALUE;
			}
		}

		void moveHome() {
			String line = pageData.roLines.getline(cy).toString();
			String lx = line.trim();
			int p1 = line.indexOf(lx);
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
			RoSb line = pageData.roLines.getline(cy);
			cx = Math.max(0, cx - 1);
			char ch1 = line.charAt(cx);
			while (cx > 0 && U.isSkipChar(line.charAt(cx), ch1)) {
				cx--;
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
			} else if (cx > pageData.roLines.getline(cy).length()
					&& cy < pageData.roLines.getLinesize() - 1) {
				cy += 1;
				cx = 0;
			}
		}

		void moveRightWord() {
			RoSb line = pageData.roLines.getline(cy);
			cx = Math.min(line.length(), cx + 1);
			if (cx < line.length()) {
				char ch1 = line.charAt(cx);
				while (cx < line.length() && U.isSkipChar(line.charAt(cx), ch1)) {
					cx++;
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
						PlainPage.this.ui.commentor.moveToPairMark(cx - 1, cy,
								pair.charAt(p1 + 1), c, 1);
					} else {
						PlainPage.this.ui.commentor.moveToPairMark(cx - 1, cy,
								pair.charAt(p1 - 1), c, -1);
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
			if (sx < 0)
				sx = 0;
			uiComp.repaint();
		}

		void setSafePos(int x, int y) {
			cy = Math.max(0, Math.min(pageData.roLines.getLinesize() - 1, y));
			cx = Math
					.max(0, Math.min(pageData.roLines.getline(cy).length(), x));
		}
	}

	class EasyEdit {
		void append(String s) {
			cy = pageData.roLines.getLinesize() - 1;
			cx = pageData.roLines.getline(cy).length();
			insertString(s);
		}

		void deleteLine(int cy) {
			cx = 0;
			int len = pageData.roLines.getline(cy).length();
			if (len > 0) {
				pageData.editRec.deleteInLine(cy, 0, len);
			}
			pageData.editRec.deleteEmptyLine(cy);
		}

		void deleteRect(Rectangle r) {
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
					for (int i = y1 + 1; i < y2; i++) {
						deleteLine(y1 + 1);
					}
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

		void insert(char ch) {
			if (ch == KeyEvent.VK_ENTER) {
				if (ptSelection.isSelected()) {
					deleteRect(ptSelection.getSelectRect());
				}
				RoSb sb = pageData.roLines.getline(cy);
				String indent = U.getIndent(sb.toString());
				String s = sb.substring(cx, sb.length());
				pageData.editRec.insertEmptyLine(cy + 1);
				pageData.editRec
						.insertInLine(cy + 1, 0, indent + U.trimLeft(s));
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

		void insertString(String s) {
			String[] ss = U.splitLine(s);
			insertString(ss);
		}

		void insertString(String[] ss) {
			if (rectSelectMode) {
				Rectangle rect = ptSelection.getSelectRect();
				int pi = 0;
				for (int iy = rect.y; iy <= rect.height; iy++) {
					String s1 = ss[pi];
					pageData.editRec.insertInLine(iy, cx, s1);
					pi++;
					if (pi >= ss.length)
						pi = 0;
				}
				if (ss.length == 1) {
					selectstartx += ss[0].length();
					selectstopx += ss[0].length();
					cx += ss[0].length();
					saveSelectionCancel = true;
				}
			} else {
				if (ss.length == 1) {
					pageData.editRec.insertInLine(cy, cx, ss[0]);
					cx += ss[0].length();
				} else {
					String rem = pageData.roLines.getInLine(cy, cx,
							Integer.MAX_VALUE);
					pageData.editRec.deleteInLine(cy, cx, Integer.MAX_VALUE);
					pageData.editRec.insertInLine(cy, cx, ss[0]);
					for (int i = 1; i < ss.length; i++) {
						pageData.editRec.insertEmptyLine(cy + i);
						pageData.editRec.insertInLine(cy + i, 0, ss[i]);
					}
					cy += ss.length - 1;
					cx = ss[ss.length - 1].length();
					pageData.editRec.insertInLine(cy, cx, rem);
				}
			}
			if (ss.length >= 5 && ui.comment == null) {
				new Thread() {
					@Override
					public void run() {
						U.guessComment(PlainPage.this);
					}
				}.start();
			}
			focusCursor();
		}

		void moveLineLeft(int cy) {
			String s = pageData.roLines.getline(cy).toString();
			if (s.length() > 0 && (s.charAt(0) == '\t' || s.charAt(0) == ' ')) {
				pageData.editRec.deleteInLine(cy, 0, 1);
			}
			cx -= 1;
			if (cx < 0) {
				cx = 0;
			}
		}

		void moveLineRight(int cy) {
			pageData.editRec.insertInLine(cy, 0, "\t");
			cx += 1;
		}

		void moveRectLeft(int from, int to) {
			for (int i = from; i <= to; i++) {
				moveLineLeft(i);
			}
		}

		void moveRectRight(int from, int to) {
			for (int i = from; i <= to; i++) {
				moveLineRight(i);
			}
		}

		void setLength(int cy, int cx) {
			int oldLen = pageData.roLines.getline(cy).length();
			if (cx - oldLen > 0)
				pageData.editRec
						.insertInLine(cy, oldLen, U.spaces(cx - oldLen));
		}

		void wrapLines(int cx) throws Exception {
			int lineLen = 0;
			{
				int len = 0;
				String sb = pageData.roLines.getInLine(cy, 0, cx);
				for (int i = 0; i < sb.length(); i++) {
					len += (sb.charAt(i) > 255) ? 2 : 1;
				}
				lineLen = Math.max(10, len);
			}
			ui.message("wrapLine at " + lineLen);
			if (ptSelection.isSelected()) {
				ptSelection.cancelSelect();
			}
			List<StringBuffer> newtext = new ArrayList<StringBuffer>();
			for (int y = 0; y < pageData.lines.size(); y++) {
				if (pageData.lines.get(y).length() * 2 > lineLen) {
					int len = 0;
					RoSb sb = pageData.roLines.getline(y);
					int start = 0;
					for (int i = 0; i < sb.length(); i++) {
						len += (sb.charAt(i) > 255) ? 2 : 1;
						if (len >= lineLen) {
							newtext.add(new StringBuffer(sb.substring(start,
									i + 1)));
							start = i + 1;
							len = 0;
						}
					}
					if (start < sb.length()) {
						newtext.add(new StringBuffer(sb.substring(start)));
					}
				} else {
					newtext.add(new StringBuffer(pageData.lines.get(y)));
				}
			}
			String title = "wrapped " + pageData.getTitle() + " #"
					+ U.randomID();
			PlainPage p2 = new PlainPage(uiComp, PageData.newEmpty(title));
			p2.pageData.setLines(newtext);
		}
	}

	class Paint {
		class Comment {
			void markBox(Graphics2D g2, int x, int y) {
				if (y >= sy && y <= sy + showLineCnt && x >= sx) {
					RoSb sb = pageData.roLines.getline(y);
					int w1 = x > 0 ? U.strWidth(g2, sb.substring(sx, x),
							TABWIDTH) : 0;
					String c = sb.substring(x, x + 1);
					int w2 = U.strWidth(g2, c, TABWIDTH);
					g2.setColor(Color.WHITE);
					g2.drawRect(w1 - 1, (y - sy) * (lineHeight + lineGap) - 4,
							w2, 16);
					g2.setColor(colorNormal);
					g2.drawRect(w1, (y - sy) * (lineHeight + lineGap) - 3, w2,
							16);
					g2.drawString(c, w1, lineHeight + (y - sy)
							* (lineHeight + lineGap));
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
				g2.setColor(Color.WHITE);
				g2.drawLine(-6, scy1 - 1, -6, scy2 - 1);
				if (o1 == y1) {
					g2.setColor(Color.WHITE);
					g2.drawLine(-6, scy1 - 1, -1, scy1 - 1);
				}
				if (o2 == y2) {
					g2.setColor(Color.WHITE);
					g2.drawLine(-6, scy2 - 1, -1, scy2 - 1);
				}
				g2.setColor(Color.BLUE);
				g2.drawLine(-5, scy1, -5, scy2);
				if (o1 == y1) {
					g2.setColor(Color.BLUE);
					g2.drawLine(-5, scy1, 0, scy1);
				}
				if (o2 == y2) {
					g2.setColor(Color.BLUE);
					g2.drawLine(-5, scy2, 0, scy2);
				}
			}

			void moveToPairMark(int cx2, int cy2, char ch, char ch2, int inc) {
				int[] c1 = new int[] { cx2, cy2 };
				U.findchar(PlainPage.this, ch, inc, c1, ch2);
				if (c1[0] >= 0) {// found
					cx = c1[0] + 1;
					cy = c1[1];
					focusCursor();
				}
			}

			void pairMark(Graphics2D g2, int cx2, int cy2, char ch, char ch2,
					int inc) {
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

		static final int TABWIDTH = 40;
		BufferedImage aboutImg;
		boolean aboutOn;

		int aboutY;
		boolean closed = false;
		Color colorBg, colorComment, colorComment2, colorCurrentLineBg,
				colorDigit, colorGutLine, colorGutNumber, colorKeyword;
		int colorMode;
		/**
		 * 0:white mode 1: black mode 2: blue mode * 1 bg, 2 normal, 3 keyword,
		 * 4 digit, 5 comment, 6 gutNumber, 7 gutLine, 8 currentLineBg, 9
		 * comment2
		 */
		final int[][] ColorModes = new int[][] {
				{ 0xdddddd, BLACK.getRGB(), BLUE.getRGB(), RED.getRGB(),
						0xC85032, 0x115511, 0xffffff, 0xF0F0F0, 0xffffff },
				{ 0x0, LIGHT_GRAY.getRGB(), YELLOW.darker().getRGB(),
						GREEN.getRGB(), BLUE.brighter().getRGB(), 0xC85032,
						LIGHT_GRAY.getRGB(), 0x222222, 0x404040 },
				{ BLUE.darker().getRGB(), LIGHT_GRAY.getRGB(), YELLOW.getRGB(),
						GREEN.getRGB(), RED.getRGB(), 0x008800,
						LIGHT_GRAY.getRGB(), 0x2222ff, 0x0 } };
		Color colorNormal = Color.BLACK;
		String[] comment = null;
		Comment commentor = new Comment();
		CommandPanelPaint cp = new CommandPanelPaint();
		Dimension dim;
		Font font = new Font("Monospaced", Font.PLAIN, 12);
		Font FONT_BIG = new Font("Monospaced", Font.PLAIN, 24);
		int gutterWidth = 40;
		int lineGap = 5;
		int lineHeight = 10;
		long MSG_VANISH_TIME = 3000;
		List<Object[]> msgs = new ArrayList<Object[]>();

		boolean noise = false;

		int noisesleep = 500;

		float scalev = 1;

		Paint() {
			applyColorMode(0);
		}

		void applyColorMode(int i) {
			if (i >= ColorModes.length)
				i = 0;
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
		}

		void drawGutter(Graphics2D g2) {
			g2.setColor(colorGutNumber);
			for (int i = 0; i < showLineCnt; i++) {
				if (sy + i + 1 > pageData.roLines.getLinesize()) {
					break;
				}
				g2.drawString("" + (sy + i + 1), 0, lineHeight
						+ (lineHeight + lineGap) * i);
			}
		}

		void drawReturn(Graphics2D g2, int w, int py) {
			g2.setColor(Color.red);
			g2.drawLine(w, py - lineHeight + font.getSize(), w + 3, py
					- lineHeight + font.getSize());
		}

		void drawSelect(Graphics2D g2, int y1, int x1, int x2) {
			int scry = y1 - sy;
			if (scry < showLineCnt) {
				String s = pageData.roLines.getline(y1).toString();
				if (sx > s.length()) {
					return;
				}
				s = U.subs(s, sx, s.length());
				x1 -= sx;
				x2 -= sx;
				if (x1 < 0) {
					x1 = 0;
				}
				if (x2 < 0) {
					x2 = 0;
				}
				if (x2 > s.length()) {
					x2 = s.length();
				}
				if (x1 > s.length()) {
					x1 = s.length();
				}
				if (x1 == x2) {
					int w1 = U.strWidth(g2, s.substring(0, x1), TABWIDTH);
					g2.fillRect(w1, scry * (lineHeight + lineGap), 3,
							lineHeight + lineGap);
				} else {
					int w1 = U.strWidth(g2, s.substring(0, x1), TABWIDTH);
					int w2 = U.strWidth(g2, s.substring(0, x2), TABWIDTH);
					g2.fillRect(w1, scry * (lineHeight + lineGap), (w2 - w1),
							lineHeight + lineGap);
				}
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
				g.setFont(FONT_BIG);
				int w = U.maxWidth(msgs, g, FONT_BIG) + 100;
				int h = 30 * msgs.size() + 60;
				g.setXORMode(Color.BLACK);
				g.setPaintMode();
				g.setColor(Color.decode("0xFFCCFF"));
				g.fillRoundRect((dim.width - w) / 2, (dim.height - h) / 2, w,
						h, 3, 3);
				g.setColor(Color.BLACK);
				for (int i = 0; i < msgs.size(); i++) {
					Object[] row = msgs.get(i);
					int w1 = (Integer) row[2];
					g.drawString(row[0].toString(), (dim.width - w1) / 2,
							(10 + dim.height / 2 + 30 * (i - msgs.size() / 2)));
				}
			}

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
						g2.drawImage(U.tabImg, x + w, y - lineHeight, null);
						w += TABWIDTH;
					}
					w += U.drawTwoColor(g2, s1, x + w, y, colorComment,
							colorComment2, 1);
					if (w > dim.width - gutterWidth) {
						break;
					}
				}
			} else {
				List<String> s1x = U.split(s);
				for (String s1 : s1x) {
					if (s1.equals("\t")) {
						g2.drawImage(U.tabImg, x + w, y - lineHeight, null);
						w += TABWIDTH;
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

		void drawTextLines(Graphics2D g2, int charCntInLine) {
			int y = sy;
			int py = lineHeight;
			for (int i = 0; i < showLineCnt; i++) {
				if (y >= pageData.roLines.getLinesize()) {
					break;
				}
				RoSb sb = pageData.roLines.getline(y);
				if (sx < sb.length()) {
					int chari2 = Math.min(charCntInLine + sx, sb.length());
					String s = U.subs(sb, sx, chari2);
					g2.setColor(colorNormal);
					int w = drawStringLine(g2, s, 0, py); // U.strWidth(g2, s,
					// TABWIDTH);
					drawReturn(g2, w, py);
				} else {
					drawReturn(g2, 0, py);
				}
				y += 1;
				py += lineHeight + lineGap;
			}
		}

		void drawToolbar(Graphics2D g2) {
			String s1 = "<F1>:Help, "
					+ (pageData.encoding == null ? "-" : pageData.encoding)
					+ (pageData.lineSep.equals("\n") ? ", U" : ", W")
					+ ", Line:"
					+ pageData.roLines.getLinesize()
					+ ", X:"
					+ (cx + 1)
					+ ", undo:"
					+ pageData.history.size()
					+ ", "
					+ (rectSelectMode ? "R, " : "")
					+ (pageData.getFn() == null ? "-" : pageData.getFn()
							+ (changedOutside ? " [ChangedOutside!]" : ""));
			g2.setColor(Color.WHITE);
			g2.drawString(s1, 2, lineHeight + 2);
			g2.setColor(Color.BLACK);
			g2.drawString(s1, 1, lineHeight + 1);
			if (msg != null) {
				if (System.currentTimeMillis() - msgtime > MSG_VANISH_TIME) {
					msg = null;
				} else {
					int w = g2.getFontMetrics().stringWidth(msg);
					g2.setColor(new Color(0xee6666));
					g2.fillRect(dim.width - w, 0, dim.width, lineHeight
							+ lineGap);
					g2.setColor(Color.YELLOW);
					g2.drawString(msg, dim.width - w, lineHeight);
				}
			}
		}

		private int getCommentPos(String s) {
			if (comment == null)
				return -1;
			for (String c : comment) {
				int p = s.indexOf(c);
				if (p >= 0)
					return p;
			}
			return -1;
		}

		void message(final String s) {
			msg = s;
			msgtime = System.currentTimeMillis();
			uiComp.repaint();
			U.repaintAfter(MSG_VANISH_TIME, uiComp);
			System.out.println(s);
		}

		void setNextColorMode() {
			if (++colorMode >= ColorModes.length)
				colorMode = 0;
		}

		void xpaint(Graphics g, Dimension size) {

			try {
				this.dim = size;

				if (ui.cp.showCommandPanel) {
					cp.xpaint((Graphics2D) g, size);
					return;
				}

				if (!isCommentChecked) {// find comment pattern
					isCommentChecked = true;
					new Thread() {
						@Override
						public void run() {
							U.guessComment(PlainPage.this);
						}
					}.start();
				}
				Graphics2D g2 = (Graphics2D) g;
				g2.setFont(font);
				showLineCnt = (int) ((size.height - toolbarHeight)
						/ (lineHeight + lineGap) / scalev);
				int charCntInLine = (int) ((size.width - gutterWidth)
						/ (lineHeight) * 2 / scalev);

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
					if (U.strWidth(g2,
							U.subs(pageData.roLines.getline(cy), sx, cx),
							TABWIDTH) > size.width - lineHeight * 3) {
						sx = Math.max(0, cx - charCntInLine / 2);
						int xx = charCntInLine / 4;
						while (xx > 0
								&& U.strWidth(g2, U.subs(
										pageData.roLines.getline(cy), sx, cx),
										TABWIDTH) > size.width - lineHeight * 3) {
							sx = Math.max(0, cx - xx - 1);
							xx /= 2; // quick guess
						}
					}
				}
				if (my > 0)
					// uiComp.grabFocus(); // bug: get focus when dont need
					// apply mouse click position
					if (my > 0 && my < toolbarHeight) {
					} else if (my > 0 && mx >= gutterWidth
							&& my >= toolbarHeight) {
						mx -= gutterWidth;
						my -= toolbarHeight;
						mx = (int) (mx / scalev);
						my = (int) (my / scalev);
						cy = sy + my / (lineHeight + lineGap);
						if (cy >= pageData.roLines.getLinesize()) {
							cy = pageData.roLines.getLinesize() - 1;
						}
						RoSb sb = pageData.roLines.getline(cy);
						sx = Math.min(sx, sb.length());
						cx = sx
								+ U.computeShowIndex(sb.substring(sx), mx, g2,
										TABWIDTH);
						my = 0;
						ptSelection.mouseSelection(sb);
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
				g2.drawRect(gutterWidth, -1, dim.width - gutterWidth,
						dim.height - toolbarHeight);

				g2.scale(scalev, scalev);
				drawGutter(g2);
				// draw text
				g2.setClip(0, 0, dim.width, dim.height - toolbarHeight);
				g2.translate(gutterWidth / scalev, 0);

				{ // highlight current line
					int l1 = cy - sy;
					if (l1 >= 0 && l1 < showLineCnt) {
						g2.setColor(colorCurrentLineBg);
						g2.fillRect(0, l1 * (lineHeight + lineGap), size.width,
								lineHeight + lineGap - 1);
					}
				}
				g2.setColor(colorNormal);
				// g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				// RenderingHints.VALUE_ANTIALIAS_ON);
				drawTextLines(g2, charCntInLine);
				if (rectSelectMode) {
					Rectangle r = ptSelection.getSelectRect();
					int x1 = r.x;
					int y1 = r.y;
					int x2 = r.width;
					int y2 = r.height;
					for (int i = y1; i <= y2; i++) {
						g2.setColor(Color.BLUE);
						g2.setXORMode(new Color(0xf0f030));
						drawSelect(g2, i, x1, x2);
					}
				} else {// select mode
					Rectangle r = ptSelection.getSelectRect();
					int x1 = r.x;
					int y1 = r.y;
					int x2 = r.width;
					int y2 = r.height;
					if (y1 == y2 && x1 < x2) {
						g2.setColor(Color.BLUE);
						g2.setXORMode(new Color(0xf0f030));
						drawSelect(g2, y1, x1, x2);
					} else if (y1 < y2) {
						g2.setColor(Color.BLUE);
						g2.setXORMode(new Color(0xf0f030));
						drawSelect(g2, y1, x1, Integer.MAX_VALUE);
						for (int i = y1 + 1; i < y2; i++) {
							drawSelect(g2, i, 0, Integer.MAX_VALUE);
						}
						drawSelect(g2, y2, 0, x2);
					}
				}
				if (true) {// (){}[]<> pair marking
					if (cx - 1 < pageData.roLines.getline(cy).length()
							&& cx - 1 >= 0) {
						char c = pageData.roLines.getline(cy).charAt(cx - 1);
						String pair = "(){}[]<>";
						int p1 = pair.indexOf(c);
						if (p1 >= 0) {
							if (p1 % 2 == 0) {
								commentor.pairMark(g2, cx - 1, cy,
										pair.charAt(p1 + 1), c, 1);
							} else {
								commentor.pairMark(g2, cx - 1, cy,
										pair.charAt(p1 - 1), c, -1);
							}
						}
					}
				}
				// draw cursor
				if (cy >= sy && cy <= sy + showLineCnt) {
					g2.setXORMode(new Color(0x30f0f0));
					String s = U.subs(pageData.roLines.getline(cy), sx, cx);
					int w = U.strWidth(g2, s, TABWIDTH);
					g2.fillRect(w, (cy - sy) * (lineHeight + lineGap), 2,
							lineHeight);
				}

				if (aboutOn) {// about info
					g.setPaintMode();
					g.drawImage(aboutImg, 0, aboutY, null);
				}

				drawSelfDispMessages(g2);

			} catch (Throwable th) {
				th.printStackTrace();
				ui.message("Bug:" + th);
			}
		}

	}

	class Selection {
		void cancelSelect() {
			selectstartx = cx;
			selectstarty = cy;
			selectstopx = cx;
			selectstopy = cy;
		}

		void copySelected() {
			String s = getSelected();
			s = U.removeAsciiZero(s);
			U.setClipBoard(s);
			ui.message("copied " + s.length());
		}

		void cutSelected() {
			copySelected();
			ptEdit.deleteRect(getSelectRect());
			cancelSelect();
		}

		String getSelected() {
			return pageData.roLines.getTextInRect(getSelectRect(),
					rectSelectMode);
		}

		Rectangle getSelectRect() {
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

		boolean isRectSelecting() {
			return mshift && rectSelectMode;
		}

		boolean isSelected() {
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

		void mouseSelection(RoSb sb) {
			if (mcount == 2) {
				int x1 = cx;
				int x2 = cx;
				if (sb.length() > x1
						&& Character.isJavaIdentifierPart(sb.charAt(x1)))
					while (x1 > 0
							&& Character
									.isJavaIdentifierPart(sb.charAt(x1 - 1))) {
						x1 -= 1;
					}
				if (sb.length() > x2
						&& Character.isJavaIdentifierPart(sb.charAt(x2)))
					while (x2 < sb.length() - 1
							&& Character
									.isJavaIdentifierPart(sb.charAt(x2 + 1))) {
						x2 += 1;
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
				} else {
					cancelSelect();
				}
			}
		}

		void selectAll() {
			selectstartx = 0;
			selectstarty = 0;
			selectstopy = pageData.roLines.getLinesize() - 1;
			selectstopx = pageData.roLines.getline(selectstopy).length();
		}

		void selectLength(int x, int y, int length) {
			cx = x;
			cy = y;
			selectstartx = cx;
			selectstarty = cy;
			selectstopx = cx + length;
			selectstopy = cy;
			focusCursor();
		}
	}

	static final String WINDOW_NAME = "neoeedit " + Version.REV;
	boolean changedOutside = false;

	Cursor cursor = new Cursor();
	int cx;
	int cy;
	boolean ignoreCase = true;
	boolean isCommentChecked = false;
	int mcount;
	String msg;
	long msgtime;
	boolean mshift;

	//

	int mx, my;
	PageData pageData;

	EasyEdit ptEdit = new EasyEdit();

	U.FindAndReplace ptFind = new U.FindAndReplace(this);
	Selection ptSelection = new Selection();
	boolean rectSelectMode = false;
	boolean saveSelectionCancel;

	String searchResultOf;
	int selectstartx, selectstarty, selectstopx, selectstopy;
	int showLineCnt;
	int sy, sx;

	int toolbarHeight = 25;
	Paint ui = new Paint();

	EditPanel uiComp;

	private PlainPage() {
	}

	public PlainPage(EditPanel editor, PageData data) throws Exception {
		this();
		this.uiComp = editor;
		this.pageData = data;
		int index = editor.pageSet.indexOf(editor.getPage());
		if (index >= editor.pageSet.size() || index < 0)
			index = 0;
		editor.pageSet.add(index, this);
		editor.setPage(this);
		editor.changeTitle();
		data.ref++;
	}

	private void checkControlLater(final int lag) {
		new Thread() {
			@Override
			public void run() {
				ui.cp.hasCheckThread = true;
				try {
					Thread.sleep(lag);
					// System.out.println("checkControlLater");
					long now = System.currentTimeMillis();
					if (ui.cp.controlDownMs > 0
							&& now - ui.cp.controlDownMs >= lag) {
						ui.cp.showCommandPanel = true;
						uiComp.repaint();
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					ui.cp.hasCheckThread = false;
				}

			}
		}.start();

	}

	public void close() {
		int index = uiComp.pageSet.indexOf(this);
		uiComp.pageSet.remove(this);

		pageData.ref--;
		if (pageData.ref == 0)
			pageData.close();

		if (index >= uiComp.pageSet.size()) {
			index = uiComp.pageSet.size() - 1;
		}
		if (index >= 0) {
			uiComp.setPage(uiComp.pageSet.get(index));
		} else {
			// nothing to show
			uiComp.frame.dispose();
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

	public void keyPressed(KeyEvent env) {
		if (Ime.enabled && Ime.instance != null) {
			Param param = new Param();
			Ime.instance.keyPressed(env, param);
			if (param.yield != null) {
				pageData.history.beginAtom();
				ptEdit.insertString(param.yield);
				pageData.history.endAtom();
			}
			if (param.consumed) {
				return;
			}
		}

		if (env.getKeyCode() == KeyEvent.VK_CONTROL) {
			if (ui.cp.controlDownMs <= 0 && !ui.cp.hasCheckThread) {
				ui.cp.controlDownMs = System.currentTimeMillis();
				// System.out.println("control pressed");
				checkControlLater(1100);// 1 sec
			}
		} else {
			if (ui.cp.controlDownMs > 0) {
				ui.cp.controlDownMs = 0;
				ui.cp.showCommandPanel = false;
			}
		}

		pageData.history.beginAtom();
		try {
			mshift = env.isShiftDown();
			int ocx = cx;
			int ocy = cy;

			Commands cmd = U.mappingToCommand(env);
			if (cmd == null) {
				int kc = env.getKeyCode();
				if (!Character.isIdentifierIgnorable(kc)
						&& (env.isAltDown() || env.isControlDown())) {
					unknownCommand(env);
				}
			} else {
				processCommand(cmd);
			}

			boolean cmoved = !(ocx == cx && ocy == cy);
			if (cmoved) {
				if (env.isShiftDown()) {
					selectstopx = cx;
					selectstopy = cy;
				} else {
					if (saveSelectionCancel) {
						saveSelectionCancel = false;
					} else {
						ptSelection.cancelSelect();
					}
				}
			}
			uiComp.repaint();
		} catch (Exception e) {
			ui.message("err:" + e);
			e.printStackTrace();
		}
		pageData.history.endAtom();
	}

	public void keyReleased(KeyEvent env) {
		if (env.getKeyCode() == KeyEvent.VK_CONTROL) {
			// System.out.println("control released");
			if (ui.cp.controlDownMs > 0) {
				ui.cp.controlDownMs = 0;
				ui.cp.showCommandPanel = false;
				uiComp.repaint();
			}
		}
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
			} else if (kc == KeyEvent.VK_TAB && !env.isShiftDown()
					&& selectstarty != selectstopy && !rectSelectMode) {
				Rectangle r = ptSelection.getSelectRect();
				ptEdit.moveRectRight(r.y, r.height);
			} else {
				if (Ime.enabled && Ime.instance != null) {
					Param param = new Param();
					Ime.instance.keyPressed(env, param);
					if (param.yield != null)
						ptEdit.insertString(param.yield);
					if (!param.consumed) {
						ptEdit.insert(kc);
					}
				} else {
					ptEdit.insert(kc);
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
				} catch (Exception e) {
					e.printStackTrace();
				}
				ui.cp.clickedName = null;
			}
			return;
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
				} catch (Exception e) {
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

	public void mouseDragged(MouseEvent env) {
		mx = env.getX();
		my = env.getY();
		mshift = true;
		uiComp.repaint();
	}

	public void mouseMoved(MouseEvent evt) {
		if (ui.cp.showCommandPanel) {
			ui.cp.mouseMoved(evt);
		}

	}

	public void mousePressed(MouseEvent env) {
		mx = env.getX();
		my = env.getY();
		mshift = env.isShiftDown();
		mcount = env.getClickCount();
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

	void processCommand(Commands cmd) throws Exception {
		switch (cmd) {
		case showHelp:
			U.showHelp(ui, uiComp);
			break;
		case saveAs:
			U.saveAs(this);
			break;
		case findNext:
			ptFind.findNext();
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
			cursor.moveUp();
			focusCursor();
			break;
		case moveDown:
			cursor.moveDown();
			focusCursor();
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
			cursor.movePageUp();
			focusCursor();
			break;
		case movePageDown:
			cursor.movePageDown();
			focusCursor();
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
		case switchLineSeperator:
			if (pageData.lineSep.equals("\n"))
				pageData.lineSep = "\r\n";
			else
				pageData.lineSep = "\n";
			break;
		case wrapLines:
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
		case launch:
			if (cy < pageData.lines.size())
				U.launch(pageData.roLines.getline(cy).toString());
			break;
		case execute:
			if (cy < pageData.lines.size())
				U.exec(this, pageData.roLines.getline(cy).toString());
			break;
		case hex:
			String s = ptSelection.getSelected();
			if (s != null && s.length() > 0) {
				U.showHexOfString(s, PlainPage.this);
			}
			break;
		case listFonts:
			U.listFonts(this);
			break;
		case copySelected:
			ptSelection.copySelected();
			break;
		case paste:
			if (ptSelection.isSelected()) {
				ptEdit.deleteRect(ptSelection.getSelectRect());
			}
			ptEdit.insertString(U.getClipBoard());
			break;
		case cut:
			ptSelection.cutSelected();
			break;
		case selectAll:
			System.out.println("selectall");
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
			PlainPage pp = new PlainPage(uiComp, PageData.newEmpty("UNTITLED #"
					+ U.randomID()));
			pp.pageData.workPath = this.pageData.workPath;
			pp.ptSelection.selectAll();
			break;
		case newWindow:
			EditPanel ep = new EditPanel();
			ep.openWindow();
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
		case moveRightWord:
			cursor.moveRightWord();
			focusCursor();
			break;
		case moveViewUp:
			sy = Math.max(0, sy - 1);
			break;
		case moveViewDown:
			sy = Math.min(sy + 1, pageData.roLines.getLinesize() - 1);
			break;
		case resetScale:
			ui.scalev = 1;
			break;
		case go:
			if (cy < pageData.lines.size()) {
				String line = pageData.roLines.getline(cy).toString();
				if (line.startsWith("set-font:")) {
					U.setFont(this, line.substring("set-font:".length()).trim());
				} else {
					if (searchResultOf == null
							|| !U.gotoFileLine2(uiComp, line, searchResultOf)) {
						if (!U.gotoFileLine(line, uiComp, pageData.getTitle()
								.equals(U.titleOfPages(uiComp)))) {
							U.listDir(PlainPage.this, cy);
						}
					}
				}
			}
			break;
		case fileHistory:
			U.openFileHistory(uiComp);
			break;
		case print:
			new U.Print(PlainPage.this).printPages();
			break;
		case pageList:
			U.switchToPageListPage(this);
			break;
		case quickSwitchPage:
			U.switchPageInOrder(this);
			break;
		case toggleIME:
			if (Ime.instance == null)
				ui.message("IME plugin not present.");
			else {
				Ime.enabled = !Ime.enabled;
				Ime.instance.setEnabled(Ime.enabled);
			}
			break;
		case ShellCommand:
			Shell.run(PlainPage.this, cy);
			break;
		default:

		}
	}

	private void unknownCommand(KeyEvent env) {
		StringBuilder sb = new StringBuilder();
		if (env.isControlDown())
			sb.append("Ctrl");
		if (env.isAltDown()) {
			if (sb.length() > 0)
				sb.append("-");
			sb.append("Alt");
		}
		if (sb.length() > 0)
			sb.append("-");
		sb.append((char) env.getKeyCode());
		ui.message("Unknow Command:" + sb);
	}
	Dimension lastSize=new Dimension();
	public void xpaint(Graphics g, Dimension size) {
		if (!lastSize.equals(size)){		//resized
			lastSize=size;	
			ui.cp.inited=false;
		}
		ui.xpaint(g, size);
	}

}