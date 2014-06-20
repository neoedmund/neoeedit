package neoe.ne;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static neoe.ne.U.Config.getConfig;

public abstract class Ime {

    public static class Out {

        public boolean consumed;
        public String yield;
        public String preedit;
    }

    private static boolean enabled;
    private static ImeInterface[] instances;
    private static int index;

    public static void loadImes() throws IOException {
        if (instances != null) {
            return;
        }
        Map config = getConfig();
        List list = (List) config.get("ime");
        if (list == null || list.size() == 0) {
            return;
        }
        List<ImeInterface> imes = new ArrayList();
        for (Object o : list) {
            String cls = (String) o;
            try {
                Class clz = Plugin.cl.loadClass(cls);
                if (!ImeInterface.class.isAssignableFrom(clz)) {
                    System.out.println("IME class '" + cls + "' not implements 'ImeInterface'.");
                    continue;
                }
                ImeInterface ime = null;
                try {
                    imes.add(ime = (ImeInterface) clz.newInstance());
                    System.out.println("add IME:" + ime.getImeName());
                } catch (Exception ex) {
                    System.out.println("IME class '" + cls + "' cannot be inited:" + ex);
                }
            } catch (ClassNotFoundException ex) {
                System.out.println("IME class not found:" + cls);
            }
        }
        instances = imes.toArray(new ImeInterface[imes.size()]);
    }

    public static void nextIme() {
        if (instances == null || instances.length == 0) {
            enabled = false;
            return;
        }
        if (enabled) {
            index++;
            if (index >= instances.length) {
                index = 0;
                enabled = false;
            }
        } else {
            enabled = true;
            index = 0;
        }
    }

    public static ImeInterface getCurrentIme() {
        if (instances == null || !enabled) {
            return null;
        }
        return instances[index];
    }

    public interface ImeInterface {

        void keyPressed(KeyEvent env, Out param);

        void keyTyped(KeyEvent env, Out param);

        void setEnabled(boolean b);

        String getImeName();

        void paint(Graphics2D g2, Font[] fonts, int cursorX, int cursorY, Rectangle clipBounds);

    }

}
