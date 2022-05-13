package neoe.ne;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class CopyFullName {

  public static void main(String[] args) throws Exception {
    String fn = args[0].replace('\\', '/');
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
        new StringSelection(fn), null);
    Thread.sleep(10000); // 10 sec window for user to paste, for
                         // https://wiki.ubuntu.com/ClipboardPersistence
  }
}
