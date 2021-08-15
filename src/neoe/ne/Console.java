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
	private EditorPanel parentUI;

	public Console(String cmd, OutputStream out, InputStream stdout, InputStream stderr, Process proc,
			EditorPanel uiComp) {
		this.cmd = cmd;
		this.out = out;
		this.stdout = stdout;
		this.stderr = stderr;
		this.proc = proc;
		this.parentUI = uiComp;

	}

	public void start() throws Exception {
		EditorPanel ep = new EditorPanel(EditorPanelConfig.DEFAULT);
		ep.openWindow( parentUI);
		// ep.changeTitle();
		PlainPage pp = ep.getPage();

		this.pp = pp;
		pp.console = this;
		ep.changeTitle();
		{
			PageData pageData = pp.pageData;
			pageData.encoding = System.getProperty("sun.jnu.encoding");
			if (pageData.encoding == null) {
				pageData.encoding = "UTF-8";
			}
			int size = pageData.roLines.getline(pp.cy).length();
			pageData.editRec.deleteInLine(pp.cy, 0, size);
		}
		U.attach(pp, stdout);
		U.attach(pp, stderr);
	}

	public void submit(String s) {
		try {
			out.write(s.getBytes(pp.pageData.encoding));
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			pp.ptEdit.consoleAppend(e.toString());
		}
	}

	public static String filterSimpleTTY(String s) {
		while (true) {
			{
				String k1 = "[";
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
