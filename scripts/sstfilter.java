
import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

/** Delete comment lines in config files */
public class sstfilter implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List<CharSequence> ret = new ArrayList<CharSequence>();
		for (CharSequence line : lines) {
			String s = line.toString();
			if (s.endsWith("build")
			||s.endsWith("bin")
			||s.endsWith("dist")
			||s.endsWith(".log")
			){
			}else{
				ret.add(line);
			}
		}
		return ret;
	}
}

