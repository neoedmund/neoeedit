package neoe.ne;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class CopyFullName {

	public static void main(String[] args) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(args[0]), null);
	}

}
