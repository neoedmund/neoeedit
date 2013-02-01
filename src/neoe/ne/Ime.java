package neoe.ne;

import java.awt.event.KeyEvent;

public abstract class Ime {
	static class Param {
		boolean consumed;
		String yield;
	}
	public static boolean enabled;

	public static Ime instance;

	abstract void keyPressed(KeyEvent env, Param param);

	abstract void setEnabled(boolean b);
}
