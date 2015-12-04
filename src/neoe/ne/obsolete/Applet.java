package neoe.ne.obsolete;

import javax.swing.JApplet;

import neoe.ne.EditorPanel;
import neoe.ne.EditorPanelConfig;

public class Applet extends JApplet {
	private static final long serialVersionUID = -2224712202119400491L;

	@Override
	public void init() {
		super.init();
		try {
			add(new EditorPanel(EditorPanelConfig.DEFAULT));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
