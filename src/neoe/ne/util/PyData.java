// ver1.2 can parse string with double quote like 'neo''s home'
// ver151223, parse LISP-alike(separated by only space, no comma)
package neoe.ne.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * python like data support: list:Yes Map:yes multi-line String: yes, just put
 * \n in '' string escape: yes, use "\", comment:no v1.2 add comment like /* *|
 */
public class PyData {

	static final String VER = "11h22";

	static char EOF = (char) -1;

	private boolean sepByComma;

	private boolean lastIsRN;

	/** for dump */
	public PyData() {
		lastIsRN = false;
	}

	public void dump(Object data, Writer out, String indent, boolean listIndex) throws IOException {

		if (data == null)
			return;
		if (data instanceof List) {
			if (lastIsRN)
				out.write(indent);

			out.write("[\n");

			lastIsRN = true;
			List l = (List) data;
			int i = 0;
			boolean first = true;
			for (Object o : l) {
				if (sepByComma) {
					if (first) {
						first = false;
					} else {
						out.write(" , ");
					}
				}
				if (listIndex) {
					out.write(String.format(" /*%s*/ ", i++));
				}
				dump(o, out, indent + "  ", listIndex);
			}
			if (!lastIsRN)
				out.write("\n");
			out.write(indent);
			out.write("]\n");
			lastIsRN = true;
		} else if (data instanceof Map) {
			if (lastIsRN)
				out.write(indent);
			out.write("{\n");
			lastIsRN = true;
			Map m = (Map) data;
			String in2 = indent + "  ";
			boolean first = true;
			for (Object o : m.keySet()) {
				out.write(in2);
				if (sepByComma) {
					if (first) {
						first = false;
					} else {
						out.write(" , ");
					}
				}
				if (sepByComma) {
					out.write("\"");
				} else {
				}
				out.write(o.toString());
				if (sepByComma) {
					out.write("\"");
				} else {
				}
				out.write(":");
				lastIsRN = false;
				dump(m.get(o), out, in2, listIndex);
				out.write("\n");
			}
			if (!lastIsRN)
				out.write("\n");
			out.write(indent);
			out.write("}\n");
			lastIsRN = true;
		} else {
			if (lastIsRN)
				out.write(indent);
			if (sepByComma) {
				out.write("\"");
			} else {
				out.write("`");
			}
			out.write(data.toString());
			if (sepByComma) {
				out.write("\"");
			} else {
				out.write("`");
			}
			out.write("\t");
			lastIsRN = false;
		}
	}

	public PyData(boolean sepByComma, boolean useEscape) {
		this.sepByComma = sepByComma;
		this.useEscape = useEscape;
	}

	public static void main(String[] args) throws Exception {
		// BufferedReader in = new BufferedReader(new StringReader(
		// //"{/*comment*/
		// CATEGORIES:{1:1},'D\\'GM\nATTRIBS':{1:1,2:4},GROUPS:{2:\"
		// \"},TYPES:{2:2,3:'ad\nas10'}}"));
		// "{ /* ddd */ CATEGORIES:[1,2,3,4]}"));

		System.out.println("reading file:" + new File(args[0]).getAbsolutePath());
		Object o = PyData.parseAll(FileUtil.readString(new FileInputStream(args[0]), "utf8"), false);

		Writer out = new OutputStreamWriter(new FileOutputStream(args[0] + ".format"), "utf8");
		new PyData(true, true).dump(o, out, "\t", false);
		out.close();
		System.out.println("saved " + (args[0] + ".format"));
	}

	public static Object parseAll(String s, boolean sepByComma, boolean useEscape) throws Exception {
		Object o = new PyData(sepByComma, useEscape).parseAll(new StringReader(s));
		return o;
	}

	public static Object parseAll(String s, boolean sepByComma) throws Exception {
		return parseAll(s, sepByComma, false);
	}

	StringBuffer buf = new StringBuffer();

	int lno = 1, pos;

	private boolean useEscape = false;

	String at() {
		return " at line:" + lno + " pos:" + pos;
	}

	void confirm(char i, char c) throws Exception {
		if (c == ',' && !sepByComma) {
			if (i != c)
				pushBack(i);
		} else {
			if (i != c) {
				throw new Exception("Expected to read `" + c + "` but `" + i + "`(" + ((int) i) + ") found" + at()
						+ ", sep=" + sepByComma);
			}
		}
	}

	void confirm(Reader in, char c) throws Exception {
		char i = readA(in);
		confirm(i, c);
	}

	Object parse(Reader in) throws Exception {
		char i = readA(in);
		// add comment
		if (i == '/') {
			char i2 = xread(in);
			if (i2 == '*') {
				skipUtil(in, "*/");
				i = readA(in);
			} else {
				pushBack(i2);
			}
		}

		if (i == EOF) {
			return null;
		}

		if (i == '{') {
			Map m = new LinkedHashMap();
			readMap(in, m, '}');
			return m;
		}
		if (i == '[') {
			List l = new ArrayList();
			readList(in, l, ']');
			return l;
		}
		if (i == '(') {
			List l = new ArrayList();
			readList(in, l, ')');
			return l;
		}
		if (i == '"') {
			String s = readString(in, '"');
			return s;
		}
		if (i == '\'') {
			String s = readString(in, '\'');
			return s;
		}
		if (i == '`') {
			String s = readString(in, '`');
			return s;
		}
		return readToken(in, i);
	}

	public Object parseAll(Reader in) throws Exception {
		Object o = parse(in);
		char i = readA(in);
		if (i == EOF) {
			in.close();
			return o;
		}
		in.close();
		System.err.println("drop char after " + i);
		return o;
	}

	void pushBack(char c) {
		buf.append(c);
	}

	char read(Reader in) throws Exception {
		char c = (char) in.read();
		if (c == '\n') {
			lno++;
			pos = 0;
		} else {
			pos++;
		}
		return c;
	}

	char readA(Reader in) throws Exception {
		char i = xread(in);
		while (true) {
			while (i == '\n' || i == '\r' || i == ' ' || i == '\t') {
				i = xread(in);
			}
			// add comment
			if (i == '/') {
				char i2 = xread(in);
				if (i2 == '*') {
					skipUtil(in, "*/");
					i = xread(in);
				} else {
					pushBack(i2);
					return i;
				}
			} else {
				return i;
			}
		}
	}

	Object readToken(Reader in, char first) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append(first);
		while (true) {
			char i = xread(in);
			if (i == EOF || i == ' ' || i == '\n' || i == '\r' || i == '\t' || i == ',' || i == '}' || i == ')'
					|| i == ']' || i == ':') {
				pushBack(i);
				break;
			}
			sb.append(i);
		}
		try {
			return new BigDecimal(sb.toString());
		} catch (NumberFormatException ex) {
			return sb.toString();
		}
	}

	void readList(Reader in, List l, char end) throws Exception {
		while (true) {
			char i = readA(in);
			if (i == EOF) {
				throw new Exception("Expected to read " + end + " but EOF found" + at());
			}
			if (i == end) {
				return;
			}
			pushBack(i);
			Object e = parse(in);
			l.add(e);
			i = readA(in);
			if (i == end) {
				return;
			}
			confirm(i, ',');
		}
	}

	void readMap(Reader in, Map m, char end) throws Exception {
		while (true) {
			char i = readA(in);
			if (i == EOF) {
				throw new Exception("Expected to read " + end + " but EOF found" + at());
			}
			if (i == end) {
				return;
			}
			pushBack(i);
			Object key = parse(in);
			confirm(in, ':');
			Object value = parse(in);
			m.put(key, value);
			i = readA(in);
			if (i == end) {
				return;
			}
			confirm(i, ',');
		}
	}

	String readString(Reader in, char end) throws Exception {
		StringBuffer sb = new StringBuffer();
		char i = xread(in);
		while (true) {
			if (i == end) {
				char i2 = xread(in);
				if (i2 == end && (i2 == '"' || i2 == '\'')) {
					sb.append(i2);
					i = xread(in);
					continue;
				} else {
					pushBack(i2);
					break;
				}
			}

			if (i == '\\' && useEscape) {
				i = xread(in);
				if (i == 'n')
					i = '\n';
				else if (i == 'r')
					i = '\r';
				else if (i == 't')
					i = '\t';

			}
			if (i == EOF) {
				throw new Exception("Expected to read " + end + " but EOF found" + at());
			}
			sb.append(i);
			i = xread(in);
		}
		return sb.toString();

	}

	char xread(Reader in) throws Exception {
		int len = buf.length();
		if (len > 0) {
			char i = buf.charAt(len - 1);
			buf.setLength(len - 1);
			return i;
		}
		return read(in);
	}

	public static class LoopStringBuffer {

		private int[] cs;
		private int p;
		private int size;

		LoopStringBuffer(int size) {
			this.size = size;
			p = 0;
			cs = new int[size];
		}

		void add(int c) {
			cs[p++] = (char) c;
			if (p >= size) {
				p = 0;
			}
		}

		public String get() {
			int q = p;
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < size; i++) {
				sb.append((char) cs[q++]);
				if (q >= size) {
					q = 0;
				}
			}
			return sb.toString();
		}
	}

	private void skipUtil(Reader in, String end) throws Exception {
		LoopStringBuffer lsb = new LoopStringBuffer(end.length());
		while (true) {
			char b;
			if ((b = xread(in)) == EOF) {
				// not found end string
				return;
			}
			// total++;
			// ba.write(b);
			lsb.add(b);
			if (lsb.get().equals(end)) {
				break;
			}
		}
	}

	public static Object parseAll(String s) throws Exception {
		return parseAll(s, false, false);
	}

	public static Object parseFile(String fn) throws Exception {
		return parseAll(FileUtil.readString(new FileInputStream(fn), null), false);
	}

}
