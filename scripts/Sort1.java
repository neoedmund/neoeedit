import java.util.*;

import neoe.ne.Script;

public class Sort1 implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List ret = new ArrayList();
		for(CharSequence cs:lines)ret.add(cs.toString());
		Collections.sort(ret);
		return ret;
	}
}
