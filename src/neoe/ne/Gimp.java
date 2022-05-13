package neoe.ne;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;

public class Gimp {

  private static final int Alpha = 0xff000000;
  private static int range = 3;
  private static float center_weight = 100f;
  public static boolean glowDisabled = false;
  public static boolean glowAll = false;
  private static float glow_threshold;

  public static void loadFromConfig() throws IOException {
    center_weight = U.getFloat(U.Config.get("glow.center", 100f));
    range = U.getInt(U.Config.get("glow.range", 3));
    glowDisabled = U.getBool(U.Config.get("glow.disabled", false));
    System.out.println("glowDisabled=" + glowDisabled + "," +
                       U.Config.get(U.Config.getConfig(), "glow.disabled"));
    glow_threshold = U.getFloat(U.Config.get("glow.threshold", 0));
  }

  public static void drawString(Graphics2D g2, int x, int y, int lineHeight,
                                String s, Color c2, Font f, int w) {
    // System.out.println(Long.toHexString(0xffffffffL & c2.getRGB()));
    int ad = g2.getFontMetrics().getMaxDescent();
    BufferedImage img =
        new BufferedImage(w, lineHeight + ad, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g3 = img.createGraphics();
    g3.setColor(c2);
    g3.setFont(f);
    g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        EditorPanelConfig.VALUE_TEXT_ANTIALIAS);
    g3.drawString(s, 0, lineHeight);
    g3.dispose();
    img = glowing(img, c2);
    // g2 = (Graphics2D) g2.create();
    // g2.setComposite(AlphaComposite.SrcOver);
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
    g2.drawImage(img, x, y - lineHeight, null);
    // g2.dispose();
  }

  public static BufferedImage glowing(BufferedImage img, Color color2) {

    int w = img.getWidth();
    int h = img.getHeight();
    float[][] f = new float[w][h];
    final int[] pixels =
        ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
    getColorF(f, img, w, h, pixels);
    f = glowing(f, w, h, range);
    f = normalize(f, w, h);

    BufferedImage img2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img2.createGraphics();
    g.setComposite(AlphaComposite.Clear);
    g.fillRect(0, 0, w, h);
    g.dispose();
    color2 = color2.brighter();
    int color = 0xffffff & color2.getRGB(); // (Alpha)|pixels[x+y*w];//
    // img.getRGB(x, y);
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        float f1 = f[x][y];
        if (f1 < glow_threshold)
          continue;
        // int c = getColor(f1, color);
        int t = b(Alpha, 3, f1);
        int c = t | color; // b(c, 2, f) | b(c, 1, f) | b(c, 0, f);
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
        v = (float)Math.sqrt(1 - sqr(1 - v));
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

  private static float useWeight(float[][] f, int x1, int y1, float[][] weight,
                                 int r, int w, int h) {
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

  private static float sqr(float f) { return f * f; }

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
          f[r + x][r + y] = center_weight;
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

  private static void getColorF(float[][] f, BufferedImage image, int width,
                                int height, int[] pixels) {

    final int pixelLength = 1;
    for (int pixel = 0, row = 0, col = 0; pixel < pixels.length;
         pixel += pixelLength) {
      int alpha = (pixels[pixel] >> 24) & 0xff;
      f[col][row] = alpha / 256.0f;
      col++;
      if (col == width) {
        col = 0;
        row++;
      }
    }
  }

  private static int b(int v, int shift, float f) {
    int x = Math.round((0xff & (v >> (8 * shift))) * f);
    return x << (8 * shift);
  }
}
