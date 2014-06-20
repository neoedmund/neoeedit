package neoe.ne;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class DumpToString {

	private static final int MAX_DUMP_ARR = 99;

	private static boolean definedToString(Class oClass) {
		try {
			Method m = oClass.getDeclaredMethod("toString", new Class[] {});
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static String dump(Object o, int maxLevel) throws Exception {
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		Writer out = new BufferedWriter(new OutputStreamWriter(ba, "utf8"));
		out.write("dump[");
		dump(o, out, 0, maxLevel, new HashSet());
		out.write("\n]");
		out.close();
		return ba.toString();
	}

	public static void dump(Object o, Writer out, int indent, int maxLevel, Set objSet) throws Exception {
		if (o == null)
			return;

		String name = o.getClass().getName();
		if (isPrimitive(name, o)) {
			out.write(o.toString());
		} else {
			if (objSet.contains(o)) {
				out.write(o.getClass().getName() + "@" + Integer.toHexString(o.hashCode()) + "...");
				return;
			}
			objSet.add(o);
			if (indent >= maxLevel && maxLevel > 0) {
				return;
			}
			if (o.getClass().isArray()) {
				out.write("arr[");
				for (int i = 0; i < Array.getLength(o); i++) {
					if (i > MAX_DUMP_ARR) {
						out.write("\n");
						indent(indent + 1, out);
						out.write(",...(more as " + Array.getLength(o) + ")");
						break;
					}
					if (i > 0) {
						out.write("\n");
						indent(indent + 1, out);
						out.write(",");
					}
					dump(Array.get(o, i), out, indent + 1, maxLevel, objSet);

				}
				out.write("]");
			} else if (o instanceof Iterable) {
				Iterator i = ((Iterable) o).iterator();
				out.write(o.getClass() + "[");
				int index = 0;
				while (i.hasNext()) {
					if (index > 0) {
						out.write("\n");
						indent(indent, out);
						out.write(",");
					}
					dump(i.next(), out, indent + 1, maxLevel, objSet);
					index++;
				}
				out.write("\n");
				indent(indent, out);
				out.write("]");
			} else if (o instanceof Map) {
				Map m = (Map) o;
				out.write(o.getClass().getName() + "{");
				int i = 0;
				for (Object o2 : m.keySet()) {
					if (i > 0) {
						out.write("\n");
						indent(indent + 1, out);
						out.write(",");
					}
					dump(o2, out, indent + 1, maxLevel, objSet);
					out.write(":");
					dump(m.get(o2), out, indent + 1, maxLevel, objSet);
					i++;
				}
				out.write("}");
			} else {
				out.write(name);
				out.write("{\n");
				indent(indent + 1, out);
				Class oClass = o.getClass();
				int i2 = 0;
				while (oClass != null) {
					if (isPrimitive(oClass.getName(), o) || definedToString(oClass)) {
						if (i2 > 0) {
							out.write("\n");
							indent(indent + 1, out);
							out.write(",");
						} else
							i2++;
						out.write("toString()=" + o.toString());
						break;
					}
					Field[] fields = oClass.getDeclaredFields();

					for (int i = 0; i < fields.length; i++) {
						fields[i].setAccessible(true);
						{
							int mod = fields[i].getModifiers();
							if (Modifier.isStatic(mod) && Modifier.isFinal(mod))
								continue;
						}
						Object value = fields[i].get(o);
						if (value != null) {
							if (i2 > 0) {
								out.write("\n");
								indent(indent + 1, out);
								out.write(",");
							} else
								i2++;
							out.write(fields[i].getName());
							out.write("=");
							dump(value, out, indent + 1, maxLevel, objSet);
						}
					}
					oClass = oClass.getSuperclass();
				}
				out.write("\n");
				indent(indent, out);
				out.write("}");
			}
		}

	}

	private static void indent(int i, Writer out) throws IOException {
		for (; i > 0; i--)
			out.write("\t");
	}

	private static boolean isPrimitive(String name, Object o) {
		if (o.getClass().isArray() || o instanceof Iterable || o instanceof Map)
			return false;
		return
		// name.startsWith("java.") || name.startsWith("javax.")||
		// name.startsWith("sun.") ||
		name.startsWith("java.lang.") || name.indexOf(".") < 0;
	}
}

public class Shell {

	public static void run(PlainPage pp, int cy) {
		if (cy < pp.pageData.lines.size() && cy >= 0) {
			String s = pp.pageData.lines.get(cy).toString();
			String r;
			try {
				r = new Shell(pp).run(s);
			} catch (Exception e) {
				e.printStackTrace();
				r = e.toString();
			}
			if (cy == pp.pageData.lines.size() - 1) {
				pp.pageData.editRec.insertEmptyLine(cy + 1);
			}
			pp.cursor.moveDown();
			pp.cursor.moveHome();
			pp.ptEdit.insertString(r);
		}
	}

	private PlainPage pp;

	private StringBuffer ret = new StringBuffer("#");

	public Shell(PlainPage pp) {
		this.pp = pp;
	}

	private Object getObj(Object o, String name) {
		try {
			Field field = o.getClass().getDeclaredField(name);
			Object value = field.get(o);
			return value;
		} catch (Exception e) {
			Method[] methods = o.getClass().getDeclaredMethods();
			for (Method m : methods) {
				if (m.getName().equals(name)) {

					return m;
				}
			}
		}
		return null;
	}

	private void list(Object o) throws Exception {
		if (o instanceof Method) {
			ret.append(" method:" + o);
			return;
		}
		ret.append(" " + DumpToString.dump(o, 0));
	}

	private void list(String name) throws Exception {
		if (name.equals("this")) {
			list(pp);
		} else {
			Object o = getObj(pp, name);
			list(o);
		}

	}

	private String run(String s) throws Exception {
		s = s.trim();
		list(s);
		return ret.toString() + "\n";
	}

}
