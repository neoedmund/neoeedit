
import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

/** Format, adjust indent on C-like source. */
public class formatC1 implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List<CharSequence> ret = new ArrayList<CharSequence>();
		int indent = 0;
		for (CharSequence line : lines) {

			if (line.length() > 2 && line.charAt(0) == '#'
					&& line.charAt(1) != ' ') {
				// predefine
				ret.add(line);
				continue;
			}
			StringBuilder sb = new StringBuilder();
			String s = line.toString().trim();
			int indent2 = 0;
			if (s.length() == 0) {
				ret.add("");
				continue;
			} else {
				char ch = s.charAt(0);
				if (ch == ')' || ch == ']' || ch == '}')
					indent2 = -1;
			}
			for (int i = 0; i < indent + indent2; i++)
				sb.append('\t');
			sb.append(s);

			boolean quote = false;
			int p1 = s.indexOf("//");
			if (p1 > 0)
				s = s.substring(0, p1);
			for (int i = 0; i < s.length(); i++) {
				char ch = s.charAt(i);
				if (ch == '#') {
					break;
				}
				if (ch == '"') {
					quote = !quote;
				}
				if (!quote) {
					if (ch == '(' || ch == '[' || ch == '{')
						indent++;
					if (ch == ')' || ch == ']' || ch == '}')
						if (indent > 0)
							indent--;
				}
			}
			ret.add(sb);
		}

		return ret;
	}
}
