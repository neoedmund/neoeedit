
import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

public class FilterHref implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List<CharSequence> ret = new ArrayList<CharSequence>();
		int len = "a href=\"".length();
		for (CharSequence line:lines){
			String s = line.toString();
			int p1 = s.indexOf("a href=\"");
			if (p1>0){
				int p2 = s.indexOf("\"", p1+len+1);
				String url = s.substring(p1+len, p2);
				ret.add("aria2c \""+url+"\"");
			}
		}
		return ret;
	}
}
