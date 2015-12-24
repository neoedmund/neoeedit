package neoeedit;

import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

public class Concat1 implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List<CharSequence> ret = new ArrayList<CharSequence>();
		StringBuilder sb = new StringBuilder();
		for (CharSequence line:lines){
			sb.append(String.format("\"%s\" + ", line));
		}
		ret.add(sb);
		return ret;
	}
}
