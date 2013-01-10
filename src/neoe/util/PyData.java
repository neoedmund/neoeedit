// ver1.1 can parse string with double quote like 'neo''s home'
package neoe.util;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * python like data support: list:Yes Map:yes multi-line String: yes, just put
 * \n in '' string escape: yes, use "\"
 */
public class PyData {
	static char EOF = (char) -1;

	Object parse(Reader in) throws Exception {
		char i = readA(in);
		if (i == EOF) {
			return null;
		}
		if (i == '{') {
			Map m = new HashMap();
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
		return readDecimal(in, i);
	}

	String readString(Reader in, char end) throws Exception {
		StringBuffer sb = new StringBuffer();
		char i = readA(in);
		while (true) {
			if (i == end) {
				char i2 = readA(in);
				if (i2 == end && (i2 == '"' || i2 == '\'')) {
					sb.append(i2);
					i = read(in);
					continue;
				} else {
					pushBack(i2);
					break;
				}
			}
			if (i == '\\') {
				i = read(in);
			}
			if (i == EOF) {
				throw new Exception("Expected to read " + end
						+ " but EOF found" + at());
			}
			sb.append(i);
			i = read(in);
		}
		return sb.toString();

	}

	Object readDecimal(Reader in, char first) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append(first);
		while (true) {
			char i = (char) read(in);
			if (i == EOF || i == ' ' || i == '\n' || i == '\r' || i == '\t'
					|| i == ',' || i == '}' || i == ')' || i == ']' || i == ':') {
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

	void readMap(Reader in, Map m, char end) throws Exception {
		while (true) {
			char i = readA(in);
			if (i == EOF) {
				throw new Exception("Expected to read " + end
						+ " but EOF found" + at());
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

	void readList(Reader in, List l, char end) throws Exception {
		while (true) {
			char i = readA(in);
			if (i == EOF) {
				throw new Exception("Expected to read " + end
						+ " but EOF found" + at());
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

	void confirm(char i, char c) throws Exception {
		if (i != c) {
			throw new Exception("Expected to read " + c + " but " + i
					+ " found" + at());
		}
	}

	void confirm(Reader in, char c) throws Exception {
		char i = readA(in);
		confirm(i, c);
	}

	void pushBack(char c) {
		buf.append(c);
	}

	StringBuffer buf = new StringBuffer();

	char readA(Reader in) throws Exception {
		char i = xread(in);
		while (i == '\n' || i == '\r' || i == ' ' || i == '\t') {
			i = (char) read(in);
		}
		return i;
	}

	char xread(Reader in) throws Exception {
		int len = buf.length();
		if (len > 0) {
			char i = buf.charAt(len - 1);
			buf.setLength(len - 1);
			return i;
		}
		return (char) read(in);
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

	int lno = 1, pos;

	String at() {
		return " at line:" + lno + " pos:" + pos;
	}

	public static void main(String[] args) throws Exception {
		BufferedReader in = new BufferedReader(
				new StringReader(
						"{CATEGORIES:{1:1},'D\\'GM\nATTRIBS':{1:1,2:4},GROUPS:{2:2},TYPES:{2:2,3:'ad\n" +
						"as10'}}"));
		Object o = new PyData().parseAll(in);
		System.out.println("V=" + o);
	}

	public static Object parseAll(String s) throws Exception {
		Object o = cache.get(s);
		if (o == null) {
			o = new PyData().parseAll(new StringReader(s));
			cache.put(s, o);
		}
		return o;
	}

	static Map cache = new HashMap();

}
