import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

/** Prototype of Concat lines to a single line */
public class JsChangeConcat implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List<CharSequence> ret = new ArrayList<CharSequence>();
		String key = "trStr +=";
		for (CharSequence line:lines){
			String s = line.toString();
			int p1 = s.indexOf(key);
			if (p1>=0) {
				int p2 = s.lastIndexOf(";");
				if (p2<0){
					p2 = s.length()-1;
				}
				s = s.substring(0, p1)
					+"trStr.push("
					+s.substring(p1+key.length(), p2)
					+");"
					+s.substring(p2+1);
			}
			ret.add(s);
		}
		
		return ret;
	}
}
