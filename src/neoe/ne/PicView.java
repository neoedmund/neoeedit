package neoe.ne;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class PicView {

	public class Panel extends JPanel implements MouseMotionListener,
			MouseListener, MouseWheelListener, KeyListener {

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

		public Panel(JFrame f, File fn) throws IOException {
			this.frame = f;
			long t1 = System.currentTimeMillis();
			this.f = fn;
			img = ImageIO.read(fn);
			System.out.println("read in " + (System.currentTimeMillis() - t1));
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
					if (kc == KeyEvent.VK_F1) {
						small = !small;
						repaint();
					} else if (kc == KeyEvent.VK_LEFT) {
						viewFile(-1);
					} else if (kc == KeyEvent.VK_RIGHT) {
						viewFile(1);
					} else if (kc == KeyEvent.VK_UP) {
						rotate(1);
					} else if (kc == KeyEvent.VK_DOWN) {
						rotate(-1);
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

		// private void openFile() throws Exception {
		// JFileChooser chooser = new JFileChooser();
		// if (f != null) {
		// chooser.setSelectedFile(f);
		// }
		// int returnVal = chooser.showOpenDialog(frame);
		// if (returnVal == JFileChooser.APPROVE_OPTION) {
		// System.out.println("You chose to open this file: "
		// + chooser.getSelectedFile().getAbsolutePath());
		// File f = chooser.getSelectedFile();
		// U.openFile(f);
		// }
		// }

		@Override
		public void keyReleased(KeyEvent e) {

		}

		@Override
		public void keyTyped(KeyEvent e) {

		}

		private List<File> listImgs() {
			List<File> files = new ArrayList<File>();
			File[] fs = f.getParentFile().listFiles();
			for (File f1 : fs) {
				if (U.isImageFile(f1)) {
					files.add(f1);
				}
			}
			Collections.sort(files);
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
				vx = (int) (vx1 - dx);
				vy = (int) (vy1 - dy);
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
			g.clearRect(0, 0, w, h);
			// System.out.println(w+"x"+h);
			int sw = w / 4;
			int sh = sw * ph / pw;

			g.drawImage(img, 0, 0, w, h, (int) (vx * rate), (int) (vy * rate),
					(int) ((w + vx) * rate), (int) ((h + vy) * rate), null);
			if (small) {
				g.drawImage(img, w - sw, h - sh, w, h, 0, 0, pw, ph, null);
				g.setColor(Color.WHITE);
				g.drawRect(w - sw, h - sh, sw, sh);
				g.setColor(Color.RED);
				g.drawRect((int) (w - sw + vx * rate * sw / pw),//
						(int) (h - sh + vy * rate * sh / ph),//
						(int) (sw * w * rate / pw),//
						(int) (sh * h * rate / ph));
			}
		}

		public void rotate(int direction) {
			int angle = direction * 90;
			AffineTransform at = new AffineTransform();
			at.rotate(angle * Math.PI / 180.0, img.getWidth() / 2.0,
					img.getHeight() / 2.0);
			BufferedImageOp op = new AffineTransformOp(at,
					AffineTransformOp.TYPE_BILINEAR);
			img = op.filter(img, null);
			repaint();
		}

		private void setSize(BufferedImage img) {
			setPreferredSize(new Dimension(pw = img.getWidth(),
					ph = img.getHeight()));
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
				frame.setTitle("PicView " + files.get(fi).getName());
				setSize(img = ImageIO.read(files.get(fi)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			repaint();
		}

	}

	public static void main(String[] args) throws IOException {
		new PicView().show(new File(args[0]));

	}

	private EditPanel ep;

	public PicView() {
	}

	public PicView(EditPanel ep) {
		this.ep = ep;
	}

	public void show(File fn) throws IOException {
		JFrame f = new JFrame("PicView " + fn.getName());
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Panel p;
		f.add(p = new Panel(f, fn));
		U.setFrameSize(f, p.pw, p.ph);
		f.setTransferHandler(new U.TH(ep));
		f.setVisible(true);
		U.saveFileHistory(fn.getAbsolutePath(), 0);
	}
}
