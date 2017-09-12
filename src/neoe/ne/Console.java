package neoe.ne;

import java.io.InputStream;
import java.io.OutputStream;

public class Console {
	public String cmd;
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
		ep.changeTitle();
		{
			PageData pageData = pp.pageData;
			int size = pageData.roLines.getline(pp.cy).length();
			pageData.editRec.deleteInLine(pp.cy, 0, size);
		}
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

	public static String filterSimpleTTY(String s) {
		while (true) {
			{
				String k1 = "[0m";
				int p1 = s.indexOf(k1);
				if (p1 >= 0) {
					s = s.substring(0, p1) + s.substring(p1 + k1.length());
					continue;
				}
			}
			{
				String k1 = "[01;";
				int p1 = s.indexOf(k1);
				if (p1 >= 0) {
					int p2 = s.indexOf("m", p1 + k1.length());
					if (p2 >= 0) {
						s = s.substring(0, p1) + s.substring(p2 + 1);
						continue;
					}
				}
			}
			break;
		}
		return s;
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
