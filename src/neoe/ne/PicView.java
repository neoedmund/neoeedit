package neoe.ne;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import neoe.ne.util.FileIterator;
import neoe.ne.util.FileUtil;

public class PicView {
	public static class DigitFilenameCompare implements Comparator {
		private Map<File, String> cache;

		DigitFilenameCompare() {
			cache = new HashMap();
		}

		@Override
		public int compare(Object o1, Object o2) {
			File f1 = (File) o1;
			File f2 = (File) o2;
			return dfn(f1).compareTo(dfn(f2));
		}

		private String dfn(File f) {
			String s = cache.get(f);
			if (s == null) {
				s = dfn_get(f);
				cache.put(f, s);
			}
			return s;
		}

		private String dfn_get(File f) {
			String fn = f.getName();
			StringBuilder sb = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			boolean isDigit = false;
			for (int i = 0; i < fn.length(); i++) {
				char c = fn.charAt(i);
				if (Character.isDigit(c)) {
					sb2.append(c);
					if (isDigit) {
					} else
						isDigit = true;
				} else {
					if (isDigit) {
						submit(sb2, sb);
						isDigit = false;
					} else {
					}
					sb.append(c);
				}
				if (sb2.length() >= 4) {
					submit(sb2, sb);
					isDigit = false;
				}
			}
			submit(sb2, sb);
			return sb.toString();
		}

		private void submit(StringBuilder sb2, StringBuilder sb) {
			if (sb2.length() <= 0)
				return;
			int v = Integer.parseInt(sb2.toString());
			sb2.setLength(0);
			sb.append((char) v);
		}
	}

	public class PicViewPanel extends JPanel
			implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener {
		private static final long serialVersionUID = -74255011004476996L;
		private File f;
		int fi;
		List<File> files;
		private JFrame frame;
		private BufferedImage img;
		private int mx;
		private int my;
		private int ph;
		private int pw;
		double rate = 1.0;
		private boolean small = true;
		private double vx, vx1, vy, vy1;

		Rectangle maxWindow;

		boolean superMode = false;
		private Iterator<File> sfi;
		List<File> superModeHistory = new LinkedList<>();
		int superModeHistoryPointer = 0;

		boolean needRepaint;

		int rx, ry;
		boolean drawMousePos = true;
		private int direction;

		public PicViewPanel(JFrame f, File fn) throws Exception {
			this.frame = f;
			long t1 = System.currentTimeMillis();
			this.f = fn;
			GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
			maxWindow = env.getMaximumWindowBounds();
			img = loadImage(fn);
			small = (img.getWidth() > maxWindow.width || img.getHeight() > maxWindow.height);
			System.out.println("read in " + (System.currentTimeMillis() - t1));
			files = listImgs();
			setTitleWithSize(fn, fi, files.size());
			setSize(img);
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
			addKeyListener(this);
			setFocusable(true);
			Thread repaintThread = new Thread(() -> {
				while (true) {
					if (needRepaint) {
						needRepaint = false;
						repaint();
						needRepaint = false;
					}
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			repaintThread.setDaemon(true);
			repaintThread.start();
		}

		@Override
		public void keyPressed(KeyEvent e) {
			int kc = e.getKeyCode();
			try {
				if (e.isControlDown()) {
					if (kc == KeyEvent.VK_W)
						frame.dispose();
					else if (kc == KeyEvent.VK_S)
						saveCut();
				} else if (kc == KeyEvent.VK_F1 || kc == KeyEvent.VK_TAB) {
					small = !small;
					repaint1();
				} else if (kc == KeyEvent.VK_LEFT || kc == KeyEvent.VK_BACK_SPACE)
					viewFile(-1);
				else if (kc == KeyEvent.VK_RIGHT || kc == KeyEvent.VK_SPACE)
					viewFile(1);
				else if (kc == KeyEvent.VK_UP)
					rotate(1);
				else if (kc == KeyEvent.VK_DOWN)
					rotate(-1);
				else if (kc == KeyEvent.VK_P)
					ss.stop();
				else if (kc == KeyEvent.VK_S)
					toggleSuperMode();
				else if (kc == KeyEvent.VK_OPEN_BRACKET)
					ss.decDelay();
				else if (kc == KeyEvent.VK_CLOSE_BRACKET)
					ss.incDelay();
				else if (kc == KeyEvent.VK_0) {
					rate = 1;
					vx = 0;
					vy = 0;
					repaint1();
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

		private List<File> listImgs() {
			List<File> files = new ArrayList<File>();
			// bug of getParentFile?
			File[] fs = f.getAbsoluteFile().getParentFile().listFiles();
			for (File f1 : fs)
				if (U.isImageFile(f1))
					files.add(f1);
			Collections.sort(files, new DigitFilenameCompare());
			fi = files.indexOf(f);
			if (fi < 0) {
				fi = 0;
				System.out.println("what?not found file name in list");
			} else
				System.out.println("list image files count " + files.size());
			return files;
		}

		private void setRate(int x, int y, double r2) {
			vx = r2 / rate * (vx - x) + x;
			vy = r2 / rate * (vy - y) + y;
			rate = r2;
		}

		public void startCut(int x, int y) {
			cutx2 = cutx = x;
			cuty2 = cuty = y;
			repaint1();
		}

		public void saveCut() throws IOException {
			if (cutx == cutx2 || cuty == cuty2)
				return;
			savingCut = true;
			Graphics2D g = null;
			try {
				BufferedImage im = new BufferedImage(picviewpanel.getWidth(), picviewpanel.getHeight(),
						BufferedImage.TYPE_INT_RGB);
				g = im.createGraphics();
				paintComponent(g);
				sortCutPoints();
				int w = cutx2 - cutx;
				int h = cuty2 - cuty;
				BufferedImage imcut = im.getSubimage(cutx, cuty, w, h);
				ByteArrayOutputStream png = new ByteArrayOutputStream();
				ByteArrayOutputStream jpg = new ByteArrayOutputStream();
				ImageIO.write(imcut, "JPG", jpg);
				ImageIO.write(imcut, "PNG", png);
				png.close();
				jpg.close();
				if (jpg.size() == 0 && png.size() == 0) {
					JOptionPane.showMessageDialog(null, "no JPG or PNG support in ImageIO");
					return;
				}
				boolean usepng = (png.size() < jpg.size());
				if (jpg.size() == 0)
					usepng = true;
				else
					usepng = (png.size() < jpg.size());
				System.out.printf("size png(%,d) vs jpg(%,d)\n", png.size(), jpg.size());
				String f0 = f.getName();
				int p1 = f0.lastIndexOf('.');
				if (p1 > 0)
					f0 = f0.substring(0, p1);
				String fn = String.format("%s_%dx%d%s", f0, w, h, usepng ? ".png" : ".jpg");
				saveCut(fn, usepng, usepng ? png : jpg);
			} finally {
				savingCut = false;
				if (g != null)
					g.dispose();
			}
		}

		private void saveCut(String fn0, boolean usepng, ByteArrayOutputStream ba) throws IOException {
			JFileChooser chooser = new JFileChooser(f.getAbsoluteFile());
			chooser.setSelectedFile(new File(f.getParentFile(), fn0));
			int returnVal = chooser.showSaveDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String fn = chooser.getSelectedFile().getAbsolutePath();
				FileOutputStream out = new FileOutputStream(chooser.getSelectedFile());
				ba.writeTo(out);
				out.close();
				U.saveFileHistory(fn, 0);
				JOptionPane.showMessageDialog(this, String.format("saved %s(%,d bytes)", fn0, ba.size()));
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			int x = e.getX(), y = e.getY();
			if (e.isControlDown()) {
				if (!e.isShiftDown())
					startCut(x, y);
			} else if (e.getClickCount() == 2) {
				setRate(x, y, 1);
				repaint1();
			} else {
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			int x = e.getX(), y = e.getY();
			if (e.isControlDown()) {
				if (cutx == -1)
					startCut(x, y);
				else {
					cutx2 = x;
					cuty2 = y;
				}
				repaint1();
			} else if (inSmall(x, y))
				setPosSmall(x, y);
			else {
				int dx = e.getX() - mx;
				int dy = e.getY() - my;
				vx = (int) (vx1 + dx);
				vy = (int) (vy1 + dy);
				repaint1();
			}
		}

		private boolean inSmall(int x, int y) {
			int[] swh = getSwh();
			return x > swh[2] - swh[0] && y > swh[3] - swh[1] && small;
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			rx = e.getX();
			ry = e.getY();
			repaint1();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			int x = e.getX(), y = e.getY();
			mx = e.getX();
			my = e.getY();
			if (inSmall(x, y))
				setPosSmall(x, y);
			else {
				vx1 = vx;
				vy1 = vy;
			}
		}

		private void setPosSmall(int x, int y) {
			int[] s = getSwh();
			int w = s[2]; // getWidth();
			int h = s[3]; // getHeight();
			int sw = s[0]; // w / 4;
			int sh = s[1]; // sw * ph / pw;
			// x = (w - sw - vx / rate / w * sw)
			double x1 = pw * (x - (w - sw)) / (double) sw;
			double y1 = ph * (y - (h - sh)) / (double) sh;
			vx = -(x1 - w / rate / 2) * rate;
			vy = -(y1 - h / rate / 2) * rate;
			repaint1();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int amount = e.getWheelRotation() * e.getScrollAmount();
			int x = e.getX(), y = e.getY();
			if (amount > 0)
				setRate(x, y, rate / 1.1);
			else
				setRate(x, y, rate * 1.1);
			repaint1();
		}

		private File nextSpFile() {
			while (sfi.hasNext()) {
				File f = sfi.next();
				if (U.isImageFile(f)) {
					superModeHistory.add(f);
					superModeHistoryPointer = superModeHistory.size();
					if (superModeHistoryPointer > 500) {
						superModeHistory.remove(0);
						superModeHistoryPointer--;
					}
					return f;
				}
			}
			return null;
		}

		@Override
		protected void paintComponent(Graphics g) {
			int[] swh = getSwh();
			int w = swh[2]; // getWidth();
			int h = swh[3]; // getHeight();
			int sw = swh[0];
			int sh = swh[1];
			{
				g.setPaintMode();
				fillbg((Graphics2D) g, w, h);

			}

			g.drawImage(img, (int) vx, (int) vy, (int) (pw * rate), (int) (ph * rate), null);

			if (small) {
				g.drawImage(img, w - sw, h - sh, w, h, 0, 0, pw, ph, null);
				g.setColor(Color.WHITE);
				g.drawRect(w - sw, h - sh, sw, sh);
				g.setColor(Color.RED);
				int a = (int) (w - sw - sw * (vx / rate / pw));
				int b = (int) (h - sh - sh * (vy / rate / ph));
				int a2 = (int) (sw * w / rate / pw);
				int b2 = (int) (sh * h / rate / ph);
				g.drawRect(a, b, a2, b2);
			}
			if (drawMousePos) {
				double x, y;
				if (inSmall(rx, ry)) {
					x = (rx - (w - sw)) * pw / sw;
					y = (ry - (h - sh)) * ph / sh;
				} else {
					x = (int) ((-vx + rx) / rate);
					y = (int) ((-vy + ry) / rate);
				}
				String spos = String.format("%d,%d", (int) x, (int) y);
				int textW = g.getFontMetrics().stringWidth(spos);
				int textH = g.getFont().getSize();
				int x2 = rx;
				int y2 = ry;
				if (x2 + textW > w)
					x2 = w - textW;
				if (y2 < textH)
					y2 = textH;
				g.setColor(Color.BLACK);
				g.drawString(spos, x2 + 1, y2 + 1);
				g.setColor(Color.WHITE);
				g.drawString(spos, x2, y2);
			}
			drawCut((Graphics2D) g);
			g.dispose();
		}

		

		private void fillbg(Graphics2D g0, int w, int h) {
			Graphics2D g = (Graphics2D) g0.create();
			g.setPaint(getFillImagePaint());
			g.fillRect(0, 0, w, h);
			g.dispose();
		}

		private int[] getSwh() {
			int w = getWidth();
			int h = getHeight();
			int sw = w / 4;
			int sh = h / 4;
			if (ph / pw > h / w)
				sw = sh * pw / ph;
			else
				sh = sw * ph / pw;
			return new int[] { sw, sh, w, h };
		}

		private void drawCut(Graphics2D g) {
			if (cutx == cutx2 || cuty == cuty2)
				return;
			if (savingCut)
				return;
			sortCutPoints();

			g.setColor(Color.red);
			float[] dashingPattern1 = { 2f, 2f };
			Stroke stroke1 = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dashingPattern1,
					2.0f);

			g.setStroke(stroke1);
			g.drawRect(cutx, cuty, cutx2 - cutx, cuty2 - cuty);
			g.drawString(String.format("%dx%d", cutx2 - cutx, cuty2 - cuty), cutx, cuty);
			g.drawString(String.format("%dx%d", cutx2 - cutx, cuty2 - cuty), cutx2, cuty2);
		}

		public void repaint1() {
			needRepaint = true;
		}

		public void rotate(int direction) {
			this.direction = direction;
			int angle = direction * 90;
			int w = img.getWidth();
			int h = img.getHeight();
			// System.out.printf("wh=[%dx%d]\n", w, h);
			int neww = h, newh = w;
			BufferedImage dest = new BufferedImage(neww, newh, img.getType());
			Graphics2D g = dest.createGraphics();
			g.translate((neww - w) / 2, (newh - h) / 2);
			g.rotate(Math.toRadians(angle), w / 2, h / 2);
			g.drawRenderedImage(img, null);
			g.dispose();
			img = dest;
			setSize(img);
			// vx = 0;vy = 0;
			repaint1();
		}

		private void setSize(BufferedImage img) {
			Dimension dim = new Dimension(pw = (int) (img.getWidth() + 20), ph = (int) (img.getHeight() + 20));
			dim.width = Math.min(maxWindow.width, Math.max(200, dim.width));
			dim.height = Math.min(maxWindow.height, Math.max(200, dim.height));
			Dimension d0 = frame.getSize();
			dim.width = Math.max(dim.width, d0.width);
			dim.height = Math.max(dim.height, d0.height);
			frame.setSize(dim);
		}

		private void setTitleWithSize(File f, int index, int total) {
			String ss1 = "";
			if (ss != null)
				ss1 = ss.delay > 0 ? " slide:" + ss.delay + " sec" : "";
			frame.setTitle(String.format("PicView %s [%dx%d] %s %,d BS%s %s - neoeedit %s", f.getName(), img.getWidth(),
					img.getHeight(), superMode ? "SP" : String.format("(%d/%d)", index + 1, total), f.length(), ss1,
					f.getAbsoluteFile().getParent(), Version.REV));
			setSize(img);
		}

		/**
		 * i only supported to be 1 or -1
		 */
		private void superModeViewFile(int i) {
			if (i > 0)
				if (superModeHistoryPointer < superModeHistory.size())
					viewFile(superModeHistory.get(superModeHistoryPointer++));
				else {
					File f = nextSpFile();
					if (f != null)
						viewFile(f); // else skip
				}
			else if (i < 0)
				if (superModeHistoryPointer > 0 && superModeHistoryPointer <= superModeHistory.size())
					viewFile(superModeHistory.get(--superModeHistoryPointer)); // else skip
		}

		public void toggleSuperMode() {
			superMode = !superMode;
			if (superMode && sfi == null) {
				File dir = f.getParentFile();
				if (dir != null)
					sfi = new FileIterator(dir.getAbsolutePath()).iterator();
			}
		}

		private void viewFile(File f) {
			try {
				img = loadImage(f);
				this.f = f;
				if (direction != 0 && img.getWidth() < img.getHeight()) {
					rotate(direction);
				}
				setTitleWithSize(f, fi, files.size());
				resetCut();
				repaint1();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void viewFile(int i) {
			if (superMode && sfi != null) {
				superModeViewFile(i);
				return;
			}
			if (files == null)
				files = listImgs();
			if (files.size() <= 0)
				return;

			fi += i;
			if (fi < 0)
				fi = files.size() - 1;
			else if (fi >= files.size())
				fi = 0;

			File f = files.get(fi);
			viewFile(f);
		}
	}

	private class Slideshow {
		boolean exited, iconed;
		int delay = 0;
		private PicViewPanel p;

		public Slideshow(PicViewPanel p) {
			this.p = p;
		}

		public void decDelay() {
			if (delay > 0)
				delay--;
			else
				delay = 0;
		}

		public void incDelay() {
			if (delay <= 0)
				delay = 5;
			else
				delay++;
		}

		public void next() {
			try {
				Thread.sleep(delay <= 0 ? 1000 : 1000 * delay);
				if (delay > 0 && !iconed && !exited)
					p.viewFile(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void stop() {
			delay = 0;
		}
	}

	public static void main(String[] args) throws Exception {
		new PicView().show(new File(args[0]));
	}

	public BufferedImage loadImage(File f) throws Exception {
		if (f.getName().endsWith(".webp")) {
			return (BufferedImage) Plugin.call("webp", f);
		}
		return ImageIO.read(f);
	}

	private void sortCutPoints() {
		int t;
		if (cutx > cutx2) {
			t = cutx;
			cutx = cutx2;
			cutx2 = t;
		}
		if (cuty > cuty2) {
			t = cuty;
			cuty = cuty2;
			cuty2 = t;
		}
	}

	public void resetCut() {
		cutx = cuty = cutx2 = cuty2 = -1;
	}

	private int cutx, cuty, cutx2, cuty2;
	private boolean savingCut;

	Slideshow ss;

	public PicView() {
	}

	private void installSlideshow(JFrame frame, PicViewPanel p) {
		ss = new Slideshow(p);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				ss.exited = true;
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				ss.iconed = false;
			}

			@Override
			public void windowIconified(WindowEvent e) {
				ss.iconed = true;
			}
		});
		new Thread(() -> {
			while (true) {
				if (ss.exited)
					break;
				ss.next();
			}
		}).start();
	}

	PicViewPanel picviewpanel;

	public void show(File fn) throws Exception {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		f.setIconImage(U.getAppIcon(U.e3_png));
		picviewpanel = new PicViewPanel(f, fn);
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(picviewpanel);
		f.setVisible(true);
		U.saveFileHistory(fn.getAbsolutePath(), 0);
		installSlideshow(f, picviewpanel);
		resetCut();
	}
	static TexturePaint tp1;

	private static TexturePaint getFillImagePaint() {
		if (tp1 != null)
			return tp1;
		int w = 10;
		BufferedImage img = new BufferedImage(w + w, w + w, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, w, w);
		g.fillRect(w, w, w, w);
		g.setColor(Color.lightGray);
		g.fillRect(w, 0, w, w);
		g.fillRect(0, w, w, w);
		g.dispose();
		return tp1 = new TexturePaint(img, new Rectangle(0, 0, w + w, w + w));
	}
}
