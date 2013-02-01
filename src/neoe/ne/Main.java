package neoe.ne;

import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import neoe.util.PyData;

public class Main {
	public static void init() throws Exception {
		initKeys();
	}

	@SuppressWarnings("rawtypes")
	private static void initKeys() throws Exception {
		BufferedReader in = new BufferedReader(U.getInstalledReader("data.py"));
		Object o = new PyData().parseAll(in);
		List o1 = (List) ((Map) o).get("keys");
		U.originKeys = o1;
		U.keys = new HashMap<String, Commands>();
		Set<String> keys = new HashSet<String>();
		for (Object o2 : o1) {
			List row = (List) o2;
			String cmd = row.get(0).toString();
			String key = row.get(1).toString().toUpperCase();
			if (keys.contains(key)) {
				System.err.println("Error: duplicated key:" + key);
			}
			keys.add(key);
			String k = key;
			String name = "";
			int p1;
			// p1= k.indexOf("SHIFT-");
			// if (p1 >= 0) {
			// k = k.substring(0, p1) + k.substring(p1 + 6);
			// name = name + "S";
			// }
			p1 = k.indexOf("CTRL-");
			if (p1 >= 0) {
				k = k.substring(0, p1) + k.substring(p1 + 5);
				name = name + "C";
			}
			p1 = k.indexOf("ALT-");
			if (p1 >= 0) {
				k = k.substring(0, p1) + k.substring(p1 + 4);
				name = name + "A";
			}
			Commands c1;
			try {
				c1 = Commands.valueOf(cmd);
			} catch (Exception ex) {
				System.out.println("undefined command:" + cmd);
				continue;
			}
			try {
				Field f = KeyEvent.class.getField("VK_" + k);
				int kc = f.getInt(null);
				name = name + (char) kc;
				U.keys.put(name, c1);
			} catch (NoSuchFieldException ex) {
				System.err.println("Error: unknow key:" + key);
			}
		}
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
