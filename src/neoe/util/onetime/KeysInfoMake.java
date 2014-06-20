package neoe.util.onetime;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import neoe.util.FileUtil;
import neoe.util.PyData;

public class KeysInfoMake {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		new KeysInfoMake().run();

	}

	private void run() throws Exception {
		BufferedReader in = new BufferedReader(new StringReader(FileUtil.readString(new FileInputStream("src/data.py"), "utf8")));
		Object o = new PyData().parseAll(in);
		// System.out.println("V=" + o);
		List o1 = (List) ((Map) o).get("keys");
		for (Object o2 : o1) {
			List row = (List) o2;
			System.out.println("break;case " + row.get(0) + ":");
		}

	}

}
