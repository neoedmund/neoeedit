package neoe.ne;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Gimp {

	private static final int RANGE = 3;

	public static BufferedImage glowing(BufferedImage img, Color color2) {
		int w = img.getWidth();
		int h = img.getHeight();
		float[][] f = new float[w][h];
		final int[] pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
		getColorF(f, img, w, h, pixels);
		f = glowing(f, w, h, RANGE);
		f = normalize(f, w, h);

		BufferedImage img2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img2.createGraphics();
		g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, w, h);
		g.dispose();
		int color = (0xff000000) | color2.getRGB(); // (0xff000000)|pixels[x+y*w];//
		// img.getRGB(x, y);
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int c = getColor(f[x][y], color);
				img2.setRGB(x, y, c);
			}
		}
		return img2;
	}

	private static float[][] normalize(float[][] f, int w, int h) {
		float min = 1;
		float max = 0;
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				float v = f[x][y];
				if (v > max)
					max = v;
				if (v < min)
					min = v;
			}
		}
		float d = max - min;
		if (d == 0)
			return f;
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				float v = f[x][y];
				v = (v - min) / d;
				v = (float) Math.sqrt(1 - sqr(1 - v));
				f[x][y] = v;
			}
		}
		return f;

	}

	private static float[][] glowing(float[][] f, int w, int h, int r) {
		float[][] weight = getWeight2D(r);
		float[][] f2 = new float[w][h];
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				f2[x][y] = useWeight(f, x, y, weight, r, w, h);
			}
		}
		return f2;
	}

	private static float useWeight(float[][] f, int x1, int y1, float[][] weight, int r, int w, int h) {
		float ret = 0;
		for (int x = -r; x <= r; x++) {
			for (int y = -r; y <= r; y++) {
				int x2 = x + x1;
				int y2 = y + y1;
				if (x2 < 0 || x2 >= w || y2 < 0 || y2 >= h)
					continue;
				ret += f[x2][y2] * weight[r + x][r + y];
			}
		}
		// return sqr(ret);
		return ret;
	}

	private static float sqr(float f) {
		return f * f;
	}

	static float[][] _weight;

	private synchronized static float[][] getWeight2D(int r) {
		if (_weight != null)
			return _weight;
		int len = r * 2 + 1;
		float[][] f = new float[len][len];
		for (int x = -r; x <= r; x++) {
			for (int y = -r; y <= r; y++) {
				int d = x * x + y * y;
				if (d == 0) {
					f[r + x][r + y] = 1;
				} else {
					float k;
					k = 1.0f / d / d;
					f[r + x][r + y] = k;
				}

			}
		}
		_weight = f;
		return f;
	}

	private static void getColorF(float[][] f, BufferedImage image, int width, int height, int[] pixels) {

		// int[][] result = new int[height][width];
		final int pixelLength = 1;
		// int zerocnt=0;
		for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
			// int argb = 0;
			// argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
			// argb += ((int) pixels[pixel + 1] & 0xff); // blue
			// argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
			// argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
			int alpha = (pixels[pixel] >> 24) & 0xff;
			f[col][row] = alpha / 256.0f;
			// result[row][col] = argb;
			col++;
			if (col == width) {
				col = 0;
				row++;
			}
		}
		// System.out.println("pixels=" + pixels.length+",zero="+zerocnt);

	}

	private static int getColor(float f, int c) {
		return b(0xff000000, 3, f) | b(c, 2, f) | b(c, 1, f) | b(c, 0, f);
	}

	private static int b(int v, int shift, float f) {
		int x = Math.round((0xff & (v >> (8 * shift))) * f);
		return x << (8 * shift);
	}
}
