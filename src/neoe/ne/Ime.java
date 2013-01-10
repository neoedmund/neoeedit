package neoe.ne;

import java.awt.event.KeyEvent;

public abstract class Ime {
	public static Ime instance;
	public static boolean enabled;
	static class Param{
		boolean consumed;
		String yield;
	}
	abstract void keyPressed(KeyEvent env, Param param);
	abstract void setEnabled(boolean b);
}
