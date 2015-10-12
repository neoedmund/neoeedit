package neoe.ne;

import java.io.File;

public class Main {

	public static void main(String[] args) throws Exception {
		doinit();
		EditPanel editor = new EditPanel();
		if (args.length > 0) {
			File f = new File(args[0]);
			if (U.isImageFile(f)) {
				new PicView(editor).show(f);
			} else {
				new PlainPage(editor, PageData.newFromFile(f.getCanonicalPath()));
				editor.openWindow();
			}
		} else {
			editor.openWindow();
		}

		SwingJniJvmPatch();
	}

	public static EditPanel open(String title, String text) throws Exception {
		doinit();
		EditPanel editor = new EditPanel();
		PlainPage pp = new PlainPage(editor, PageData.newEmpty(title, ""));
		pp.ptEdit.append(text);
		editor.openWindow();		
		return editor;
	}

	private static void doinit() throws Exception {
		Plugin.load();
		U.Config.setDefaultLookAndFeel();
		U.Config.setDefaultBKColor();
		U.Config.initKeys();
		Ime.loadImes();
	}

	/**
	 * something like said in https://forums.oracle.com/thread/1542114
	 */
	private static void SwingJniJvmPatch() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (EditPanel.openedWindows <= 0) {
				System.out.println("SwingJniJvmPatch exit");
				break;
			}
		}
	}
}
