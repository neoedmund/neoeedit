
import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

/** Delete comment lines in config files */
public class DelCommetSemi1 implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		final char COMMENT_CHAR = ';'; //if not #
		List<CharSequence> ret = new ArrayList<CharSequence>();
		int emptyLine= 0;
		for (CharSequence line : lines) {
			if (line.length()>0 && line.charAt(0) == COMMENT_CHAR) continue;
			if (line.length()==0){
				emptyLine++;
			}else{
				emptyLine=0;
			}
			if (emptyLine<=3)
				ret.add(line);
		}
		return ret;
	}
}
