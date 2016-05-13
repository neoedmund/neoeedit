
import java.util.*;

import neoe.ne.Script;

public class SmallHistory implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List<CharSequence> ret = new ArrayList<CharSequence>();
		StringBuilder sb = new StringBuilder();
		for (CharSequence line:lines){
			String s = line.toString();
			String[] ss = s.split("\\|");
			if (ss.length==2){
				ret.add(ss[0]);
			}
		}
		Set set =new HashSet();
		List<CharSequence> ret2 = new ArrayList<CharSequence>();
		for (int i=ret.size()-1; i>=0; i--) {
			String s = ret.get(i).toString();
			if (set.contains(s)) continue;
			set.add(s);
			ret2.add(0,s);
		}
		return ret2;
	}
}
