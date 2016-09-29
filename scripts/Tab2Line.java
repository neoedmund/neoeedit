import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

public class Tab2Line implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List<CharSequence> ret = new ArrayList<CharSequence>();
		for (CharSequence line:lines){
			String s = line.toString();
			String[] ss = s.split("\t");
			for (String o:ss)
				ret.add(o);
		}
		
		return ret;
	}
}
