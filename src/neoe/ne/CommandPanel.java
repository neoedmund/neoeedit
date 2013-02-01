package neoe.ne;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandPanel {

	public static class CommandPanelPaint {
		static Font fontTopLine1 = new Font("Courier", Font.PLAIN, 11);
		static Font fontTopLine2 = new Font("Courier", Font.ITALIC, 11);
		private static void drawTip2(Graphics2D g, int mx, int my, String tip,
				Dimension dim) {
			Color bg = Color.YELLOW;
			Color fg = Color.BLACK;
			String[] ss = tip.split("\\n", 2);
			int w1 = g.getFontMetrics(fontTopLine1).stringWidth(ss[0]);
			int h1 = g.getFontMetrics(fontTopLine1).getHeight();
			int w = w1, h = h1;
			if (ss.length > 1) {
				int w2 = g.getFontMetrics(fontTopLine2).stringWidth(ss[1]);
				int h2 = g.getFontMetrics(fontTopLine2).getHeight();
				w = Math.max(w, w2);
				h += 4 + h2;
			}
			Rectangle r = new Rectangle(mx, my, w + 8, h + 8);
			if (r.x + r.width > dim.width) {
				r.x = dim.width - r.width;
			}
			if (r.y + r.height > dim.height) {
				r.y = dim.height - r.height;
			}
			g.setColor(bg);
			g.fillRect(r.x, r.y, r.width, r.height);
			g.setColor(Color.BLACK);
			g.drawRect(r.x, r.y, r.width, r.height);
			g.setColor(fg);
			g.setFont(fontTopLine1);
			g.drawString(ss[0], r.x + 4, r.y + 4
					- g.getFontMetrics().getDescent()
					+ g.getFontMetrics().getHeight());
			if (ss.length > 1) {
				g.setFont(fontTopLine2);
				g.drawString(ss[1], r.x + 4,
						r.y + 4 + 4
								+ g.getFontMetrics(fontTopLine1).getHeight()
								- g.getFontMetrics().getDescent()
								+ g.getFontMetrics().getHeight());
			}
		}
		private Color bkColor = new Color(0xcccccc);
		String clickedName;
		private List<Object> comps = new ArrayList<Object>();
		long controlDownMs;
		private Dimension dim;
		private Font font1, font2;

		private Graphics2D g;

		boolean hasCheckThread;

		boolean inited = false;
		private Object lastInObj;

		long lastInObjMs;

		private int maxHeight;

		private int mx, my;

		boolean showCommandPanel;

		private long tipMs = 490;

		int x = 0, y = 0;

		private XButton addButtonFlow(List row, Font font) {
			String desc = U.getStr(row, 3);
			// String group = getStr(row, 2);
			String key = U.getStr(row, 1);
			String name = U.getStr(row, 0);

			XButton btn = new XButton();
			btn.setText(name, font, 2, 6, x, y, g);
			btn.tip = key;
			if (desc.length() > 0)
				btn.tip = key + "\n" + desc;
			addFlow(btn);
			return btn;
		}
		private void addFlow(XButton btn) {
			Rectangle r = btn.rect;
			maxHeight = Math.max(maxHeight, r.height);
			x += r.width;
			if (x > dim.width) {
				y += maxHeight;
				r.x = 0;
				r.y = y;
				x = r.width;
				maxHeight = r.height;
			}
			comps.add(btn);
		}
		private void addGroup(String name, Font font) {
			XButton btn = new XButton();
			btn.setText(name, font, 4, 6, x, y, g);
			btn.disabled = true;
			btn.color = Color.BLUE;
			addFlow(btn);
		}
		private void addGroupDetail(List list) {
			Collections.sort(list, new Comparator() {
				@Override
				public int compare(Object o1, Object o2) {
					List r1 = (List) o1;
					List r2 = (List) o2;
					return r1.get(0).toString().compareTo(r2.get(0).toString());
				}
			});
			for (Object o : list) {
				List row = (List) o;
				addButtonFlow(row, font1);
			}

		}
		private void drawComps(Graphics2D g) {
			String tip = null;
			for (Object o : comps) {
				if (o instanceof XButton) {
					XButton btn = (XButton) o;
					if (btn == lastInObj) {
						btn.selected = true;
						long now = System.currentTimeMillis();
						if (now - lastInObjMs > tipMs) {
							btn.showTip = true;
						}
					}
					btn.xpaint(g);

					if (btn.showTip) {
						tip = btn.tip;
					}

					btn.selected = false;
					btn.showTip = false;
				}
			}
			if (tip != null) {
				drawTip2(g, mx, my, tip, dim);
			}

		}
		private void fireClicked(Object inObj) {
			if (inObj != null
					&& (inObj instanceof XButton && !((XButton) inObj).disabled)) {
				clickedName = ((XButton) inObj).text;
			}
		}
		private Object getInObj(int mx, int my) {
			for (Object o : comps) {
				if (o instanceof XButton) {
					XButton btn = (XButton) o;
					if (btn.disabled)
						continue;
					Rectangle r = new Rectangle(btn.rect);
					r.x = r.x + btn.spacing;
					r.y = r.y + btn.spacing;
					r.width = r.width - 2 * btn.spacing;
					r.height = r.height - 2 * btn.spacing;
					if (r.contains(mx, my))
						return o;
				}
			}
			return null;
		}

		private void init() {
			inited = true;
			comps.clear();
			font1 = new Font("Courier", Font.PLAIN, 18);
			font2 = new Font("comic sans ms", Font.BOLD, 20);
			Map<String, Object> groupMap = new HashMap<String, Object>();
			for (Object o2 : U.originKeys) {
				List row = (List) o2;
				String group = U.getStr(row, 2);
				// String name = U.getStr(row, 0);
				List multi = (List) groupMap.get(group);
				if (multi == null) {
					multi = new ArrayList();
					groupMap.put(group, multi);
				}
				multi.add(row);
			}
			//
			x = 0;
			y = 0;
			List<String> groupName = new ArrayList<String>(groupMap.keySet());
			Collections.sort(groupName);
			for (String gname : groupName) {
				addGroup(gname, font2);
				addGroupDetail((List) groupMap.get(gname));
			}

		}

		public void mouseClicked(MouseEvent evt) {
			clickedName = null;
			int mx = evt.getX();
			int my = evt.getY();
			Object inObj = getInObj(mx, my);
			if (inObj != null
					&& (inObj instanceof XButton && !((XButton) inObj).disabled)) {
				fireClicked(inObj);
			}
		}

		public void mouseMoved(MouseEvent evt) {
			mx = evt.getX();
			my = evt.getY();
			Object inObj = getInObj(mx, my);
			if (inObj != null
					&& (inObj instanceof XButton && !((XButton) inObj).disabled)) {
				if (inObj == lastInObj) {
					// need show tip
				} else {
					lastInObj = inObj;
					lastInObjMs = System.currentTimeMillis();
				}
			} else {
				lastInObj = inObj;
				lastInObjMs = System.currentTimeMillis();
			}

		}

		public void xpaint(Graphics2D g, Dimension size) {
			// fill background
			this.dim = size;
			this.g = g;
			g.setColor(bkColor);
			g.fillRect(0, 0, size.width, size.height);
			//
			if (!inited) {
				init();
			}
			drawComps(g);
		}
	}

	public static class XButton {

		private Color color = Color.BLACK;
		public boolean disabled;
		public Font font;
		public int padding;
		public Rectangle rect;
		public boolean selected;
		private Color selectedBk = new Color(0x9999dd);
		public boolean showTip;
		public int spacing;
		public String text;
		public String tip;

		public void setText(String name, Font font2, int spacing2,
				int padding2, int x, int y, Graphics2D g) {
			spacing = spacing2;
			padding = padding2;
			text = name;
			font = font2;
			Rectangle r = new Rectangle();
			FontMetrics fm = g.getFontMetrics(font2);
			r.setBounds(x, y, fm.stringWidth(name) + (spacing + padding) * 2,
					fm.getHeight() + (spacing + padding) * 2);
			rect = r;
		}

		public void xpaint(Graphics2D g) {

			g.setColor(Color.WHITE);
			g.drawRoundRect(rect.x + spacing, rect.y + spacing, rect.width - 2
					* +spacing, rect.height - 2 * +spacing, padding, padding);
			if (selected) {
				g.setColor(selectedBk);
				g.fillRoundRect(1 + rect.x + spacing, 1 + rect.y + spacing,
						rect.width - 2 * +spacing, rect.height - 2 * +spacing,
						padding, padding);
			} else {
				g.setColor(Color.BLACK);
				g.drawRoundRect(1 + rect.x + spacing, 1 + rect.y + spacing,
						rect.width - 2 * +spacing, rect.height - 2 * +spacing,
						padding, padding);
			}
			g.setColor(color);
			g.setFont(font);
			g.drawString(text, rect.x + padding + spacing, rect.y + padding
					+ spacing + g.getFontMetrics().getHeight()
					- g.getFontMetrics().getDescent());

		}

	}

}
