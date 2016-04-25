import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

public class JsStringConcat implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {

		List<CharSequence> ret = new ArrayList<CharSequence>();
		ret.add("[");
		for (CharSequence line:lines){
			String s = line.toString().trim();
			StringBuilder sb= new StringBuilder(", \"");
			for(int i=0;i<s.length();i++){
				char c = s.charAt(i);
				if (c=='"') {
					sb.append("\\\"");
				}else{
					sb.append(c);
				}
			}
			sb.append("\"");
			ret.add(sb.toString());
		}
		return ret;
	}
}
