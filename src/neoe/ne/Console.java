package neoe.ne;

import java.io.InputStream;
import java.io.OutputStream;

public class Console {
	String cmd;
	OutputStream out;
	InputStream stdout;
	InputStream stderr;
	Process proc;
	private PlainPage pp;

	public Console(String cmd, OutputStream out, InputStream stdout, InputStream stderr, Process proc) {
		this.cmd = cmd;
		this.out = out;
		this.stdout = stdout;
		this.stderr = stderr;
		this.proc = proc;
	}

	public void start() throws Exception {
		EditorPanel ep = new EditorPanel(EditorPanelConfig.DEFAULT);
		ep.openWindow();
		// ep.changeTitle();
		PlainPage pp = ep.getPage();
		this.pp = pp;
		pp.console = this;
		U.attach(pp, stdout);
		U.attach(pp, stderr);
	}

	public void submit(String s) {
		try {
			out.write(s.getBytes());
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			pp.ptEdit.consoleAppend(e.toString());
		}
	}

	public void submit(int i) {
		try {
			out.write(i);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			pp.ptEdit.consoleAppend(e.toString());
		}

	}

}
