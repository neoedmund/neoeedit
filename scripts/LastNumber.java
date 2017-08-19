
import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

/*-
WMIC path win32_process get Caption,Processid,Commandline > proc1
ne proc1
*/
public class LastNumber implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		String pre = "taskkill /PID ";
		List<CharSequence> ret = new ArrayList<CharSequence>();
		for (CharSequence line:lines){
			String s = line.toString().trim();
			int p1 = s.length()-1;
			while(p1>=0 && Character.isDigit(s.charAt(p1))){
				p1--;
			}
			String s1 = s.substring(p1+1);
			if (!s1.isEmpty()){
				ret.add(pre+s1);
			}
		}
		return ret;
	}
}
