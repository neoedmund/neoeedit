package neoe.ne;

import javax.swing.JApplet;

public class Applet extends JApplet {
	private static final long serialVersionUID = -2224712202119400491L;

	public void init() {
		super.init();
		try {
			add(new EditPanel());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
