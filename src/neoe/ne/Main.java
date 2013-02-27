package neoe.ne;

import java.io.File;

public class Main {
	public static void init() throws Exception {
		U.initKeys();
	}

	

	public static void main(String[] args) throws Exception {
		init();
		EditPanel editor = new EditPanel();
		if (args.length > 0) {
			File f = new File(args[0]);
			if (U.isImageFile(f)) {
				new PicView(editor).show(f);
			} else {
				PlainPage emptyPage = editor.getPage();
				new PlainPage(editor,
						PageData.newFromFile(f.getCanonicalPath()));
				emptyPage.close();
				editor.openWindow();
			}
		} else {
			editor.getPage().ptSelection.selectAll();
			// U.showSelfDispMessage(editor.getPage(),"hello ...",4000);
			editor.openWindow();
		}
	}
}
