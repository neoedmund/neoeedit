package neoe.ne;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class CopyFullName {

	public static void main(String[] args) throws Exception {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(args[0]), null);
		Thread.sleep(10000); // 10 sec window for user to paste, for
								// https://wiki.ubuntu.com/ClipboardPersistence
	}

}
