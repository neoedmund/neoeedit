package neoe.ne;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * All IME plugin extend this class.
 */
public abstract class Ime {

	public static class Out {

		public boolean consumed;
		public String yield;
		public String preedit;
	}

	private static boolean enabled;
	public static List<ImeInterface> instances = new ArrayList<ImeInterface>();
	private static int index;

	public static void nextIme() {
		if (instances == null || instances.size() == 0) {
			enabled = false;
			return;
		}
		if (enabled) {
			index++;
			if (index >= instances.size()) {
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
		return instances.get(index);
	}

	public interface ImeInterface {

		void keyPressed(KeyEvent env, Out param);

		void keyTyped(KeyEvent env, Out param);

		void setEnabled(boolean b);

		String getImeName();
		
		boolean longTextMode();

		void paint(Graphics2D g2, FontList fonts, int cursorX, int cursorY, Rectangle clipBounds);

	}

}
