import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

public class Line2Tab implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List<CharSequence> ret = new ArrayList<CharSequence>();
		StringBuilder sb= new StringBuilder();
		for (CharSequence line:lines){
			sb.append(line).append("\t");
		}
		ret.add(sb.toString());
		return ret;
	}
}
