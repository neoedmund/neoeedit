package neoe.ne;

/**
 * ReadOnly, efficency for substring
 */
public class Str implements CharSequence {

	char[] data;
	int p1, p2, len;

	public Str(char[] data, int p1, int p2) {
		this.data = data;
		this.p1 = p1;
		this.p2 = p2;
		int v = p2 - p1;
		if (v < 0)
			v = 0;
		this.len = v;
	}

	@Override
	public int length() {
		return len;
	}

	@Override
	public char charAt(int index) {
		if (index >= length())
			throw new IndexOutOfBoundsException("" + index);
		return data[p1 + index];
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		if (start < 0 || start > len)
			throw new IndexOutOfBoundsException("start=" + start + ",len="
					+ len);
		if (end < 0 || end > len)
			throw new IndexOutOfBoundsException("end=" + end + ",len=" + len);
		Str n = new Str(data, p1 + start, p1 + end);
		return n;
	}

	public int indexOf(char needle) {
		for (int i = 0; i < len; i++) {
			if (data[p1 + i] == needle)
				return i;
		}
		return -1;
	}

	public int indexOf(String needle) {
		return toString().indexOf(needle);
	}

	public StringBuilder toStringBuilder() {
		StringBuilder sb = new StringBuilder();
		sb.append(data, p1, length());
		return sb;
	}

	String s = null;

	@Override
	public String toString() {
		if (s == null)
			s = new String(data, p1, length());
		return s;
	}

	public int indexOf(String needle, int start) {
		return toString().indexOf(needle, start);
	}

	public int indexOf(char needle, int start) {
		for (int i = start; i < len; i++) {
			if (data[p1 + i] == needle)
				return i;
		}
		return -1;
	}

}
