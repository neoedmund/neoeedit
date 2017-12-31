package neoe.ne;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

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
					} else {
						isDigit = true;
					}
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
		private int vx;

		private int vx1;

		private int vy;
		private int vy1;
		Rectangle maxWindow;
		private boolean autoplay;

		public PicViewPanel(JFrame f, File fn) throws IOException {
			this.frame = f;
			long t1 = System.currentTimeMillis();
			this.f = fn;
			GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
			maxWindow = env.getMaximumWindowBounds();
			img = ImageIO.read(fn);
			System.out.println("read in " + (System.currentTimeMillis() - t1));
			files = listImgs();
			setTitleWithSize(fn, fi, files.size());
			setSize(img);
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
			addKeyListener(this);
			setFocusable(true);
		}

		@Override
		public void keyPressed(KeyEvent e) {
			int kc = e.getKeyCode();
			try {
				if (e.isControlDown()) {
					// if (kc == KeyEvent.VK_H) {
					// U.openFileHistory();
					// } else if (kc == KeyEvent.VK_O) {
					// openFile();
					// } else
					if (kc == KeyEvent.VK_W) {
						frame.dispose();
					}
				} else {
					if (kc == KeyEvent.VK_F1 || kc == KeyEvent.VK_TAB) {
						small = !small;
						repaint();
					} else if (kc == KeyEvent.VK_LEFT || kc == KeyEvent.VK_BACK_SPACE) {
						viewFile(-1);
					} else if (kc == KeyEvent.VK_RIGHT || kc == KeyEvent.VK_SPACE) {
						viewFile(1);
					} else if (kc == KeyEvent.VK_UP) {
						rotate(1);
					} else if (kc == KeyEvent.VK_DOWN) {
						rotate(-1);
					} else if (kc == KeyEvent.VK_P) {
						autoplay = true;
					} else if (kc == KeyEvent.VK_OPEN_BRACKET) {
						ss.decDelay();
					} else if (kc == KeyEvent.VK_CLOSE_BRACKET) {
						ss.incDelay();
					} else if (kc == KeyEvent.VK_0) {
						rate = 1;
						vx = 0;
						vy = 0;
						repaint();
					}
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
			for (File f1 : fs) {
				if (U.isImageFile(f1)) {
					files.add(f1);
				}
			}
			Collections.sort(files, new DigitFilenameCompare());
			fi = files.indexOf(f);
			if (fi < 0) {
				fi = 0;
				System.out.println("what?not found file name in list");
			} else {
				System.out.println("list image files count " + files.size());
			}
			return files;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			int x = e.getX(), y = e.getY();
			if (e.getClickCount() == 2) {
				vx = (int) ((vx + x) * rate - x);
				vy = (int) ((vy + y) * rate - y);
				rate = 1;
				repaint();
			} else {

			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			int x = e.getX(), y = e.getY();
			int w = getWidth();
			int h = getHeight();
			int sw = w / 4;
			int sh = sw * ph / pw;
			if (x > w - sw && y > h - sh && small) {
				vx = (int) ((x - w + sw) * pw / rate / sw - w / 2);
				vy = (int) ((y - h + sh) * ph / rate / sh - h / 2);
				repaint();
			} else {
				int dx = e.getX() - mx;
				int dy = e.getY() - my;
				vx = vx1 - dx;
				vy = vy1 - dy;
				repaint();
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {

		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

		@Override
		public void mouseMoved(MouseEvent e) {

		}

		@Override
		public void mousePressed(MouseEvent e) {
			int x = e.getX(), y = e.getY();
			int w = getWidth();
			int h = getHeight();
			int sw = w / 4;
			int sh = sw * ph / pw;
			{
				mx = e.getX();
				my = e.getY();
				vx1 = vx;
				vy1 = vy;
			}
			if (x > w - sw && y > h - sh && small) {
				vx = (int) ((x - w + sw) * pw / rate / sw - w / 2);
				vy = (int) ((y - h + sh) * ph / rate / sh - h / 2);
				repaint();
			} else {

			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int amount = e.getWheelRotation() * e.getScrollAmount();
			int x = e.getX(), y = e.getY();
			if (amount < 0) {
				rate = rate / 1.1;
				vx = (int) ((vx + x) * 1.1 - x);
				vy = (int) ((vy + y) * 1.1 - y);
			} else {
				rate = rate * 1.1;
				vx = (int) ((vx + x) / 1.1 - x);
				vy = (int) ((vy + y) / 1.1 - y);
			}
			// rate=rate1;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {

			int w = getWidth();
			int h = getHeight();
			{
				g.setPaintMode();
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, w, h);
			}
			// System.out.println(w+"x"+h);
			int sw = w / 4;
			int sh = sw * ph / pw;

			g.drawImage(img, 0, 0, w, h, (int) (vx * rate), (int) (vy * rate), (int) ((w + vx) * rate),
					(int) ((h + vy) * rate), null);
			if (small) {
				g.drawImage(img, w - sw, h - sh, w, h, 0, 0, pw, ph, null);
				g.setColor(Color.WHITE);
				g.drawRect(w - sw, h - sh, sw, sh);
				g.setColor(Color.RED);
				g.drawRect((int) (w - sw + vx * rate * sw / pw), //
						(int) (h - sh + vy * rate * sh / ph), //
						(int) (sw * w * rate / pw), //
						(int) (sh * h * rate / ph));
			}
		}

		public void rotate(int direction) {
			int angle = direction * 90;
			int w = img.getWidth();
			int h = img.getHeight();
			int neww = h, newh = w;
			BufferedImage dest = new BufferedImage(neww, newh, img.getType());
			Graphics2D g = dest.createGraphics();
			g.translate((neww - w) / 2, (newh - h) / 2);
			g.rotate(Math.toRadians(angle), w / 2, h / 2);
			g.drawRenderedImage(img, null);
			g.dispose();
			img = dest;
			setSize(img);
			repaint();
		}

		private void setSize(BufferedImage img) {
			Dimension dim = new Dimension(pw = img.getWidth() + 20, ph = img.getHeight() + 50);
			dim.width = Math.min(maxWindow.width, Math.max(200, dim.width));
			dim.height = Math.min(maxWindow.height, Math.max(200, dim.height));
			frame.setSize(dim);
		}

		public void viewFile(int i) {
			if (files == null) {
				files = listImgs();
			}
			if (files.size() <= 0)
				return;

			fi += i;
			if (fi < 0)
				fi = files.size() - 1;
			else if (fi >= files.size())
				fi = 0;
			try {
				File f = files.get(fi);
				img = ImageIO.read(f);
				setTitleWithSize(f, fi, files.size());
			} catch (IOException e) {
				e.printStackTrace();
			}
			repaint();
		}

		private void setTitleWithSize(File f, int index, int total) {
			String ss1 = "";
			if (ss != null) {
				ss1 = ss.delay > 0 ? " slide:" + ss.delay + " sec" : "";
			}
			frame.setTitle(String.format("PicView %s [%dx%d] %d/%d %,d%s", f.getName(), img.getWidth(), img.getHeight(),
					index + 1, total, f.length(), ss1));
			setSize(img);
		}

	}

	public static void main(String[] args) throws IOException {
		new PicView().show(new File(args[0]));

	}

	private EditorPanel ep;

	public PicView() {
	}

	public PicView(EditorPanel ep) {
		this.ep = ep;
	}

	public void show(File fn) throws IOException {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		f.setIconImage(U.getAppIcon(U.e3_png));
		PicViewPanel p = new PicViewPanel(f, fn);
		f.getContentPane().setLayout(new BorderLayout());
		f.getContentPane().add(p);
		f.setTransferHandler(new U.TH(ep));
		f.setVisible(true);
		U.saveFileHistory(fn.getAbsolutePath(), 0);
		installSlideshow(f, p);
	}

	private class Slideshow {
		boolean exited, iconed;
		int delay = 0;
		private PicViewPanel p;

		public Slideshow(PicViewPanel p) {
			this.p = p;
		}

		public void decDelay() {
			if (delay > 0) {
				delay--;
			} else {
				delay = 0;
			}

		}

		public void incDelay() {
			if (delay <= 0)
				delay = 5;
			else {
				delay++;
			}

		}

		public void next() {
			try {
				Thread.sleep(delay <= 0 ? 1000 : 1000 * delay);
				if (delay > 0 && !iconed && !exited) {
					p.viewFile(1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	Slideshow ss;

	private void installSlideshow(JFrame frame, PicViewPanel p) {
		ss = new Slideshow(p);
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(WindowEvent e) {
				ss.exited = true;
			}

			@Override
			public void windowIconified(WindowEvent e) {
				ss.iconed = true;
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				ss.iconed = false;
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
}
