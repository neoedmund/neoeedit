/*this script is used by neoeedit alt-j*/
import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

/** Delete comment lines in config files */
public class sstfilter implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List<CharSequence> ret = new ArrayList<CharSequence>();
		List<CharSequence> ret2 = new ArrayList<CharSequence>();
		for (CharSequence line : lines) {
			String s = line.toString();
			s = s.replace('\\', '/');
			if (s.endsWith("/build")
			||s.endsWith("/bin")
			||s.endsWith("/dist")
			||s.endsWith("/classes")
			||s.endsWith(".log")
			){
			}else{
				if (s.startsWith("D ")||s.startsWith("A ")||s.startsWith("M ")){
					ret2.add(line);
				}else{
					ret.add(line);
				}
			}
		}
		ret.addAll(ret2);
		return ret;
	}
}

