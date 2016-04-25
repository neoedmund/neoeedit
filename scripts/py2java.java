import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

public class py2java implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List<CharSequence> ret = new ArrayList<CharSequence>();
		int lastIndent = 0;
		for (CharSequence cs:lines){
			String s = cs.toString();
			boolean needColon=true;
			boolean isEmpty = false;
			if (s.trim().isEmpty()) {
				needColon = false;
				isEmpty =true;
			}
			// if
			for (String key: new String[]{" if ", " while ", " for "}) {
				int p1 = s.indexOf(key);
				if (p1>=0){
					int p2 = s.indexOf(":", p1+key.length());
					if (p2>0){
						s = s.substring(0,p1)+key+" ("+s.substring(p1+key.length(),p2)+") {" ;
						needColon =false;
					}
				}
			}

			// def
			if (s.trim().startsWith("def ")) {
				int p1 = s.indexOf("def ");
				s = s.substring(0, p1) + "Object " + s.substring(p1+4);
			}
			// :
			if(s.trim().endsWith(":")){
				int p1 = s.lastIndexOf(":");
				s = s.substring(0, p1) + " { ";
				needColon =false;
			}
			int indent = 0;
			if (!isEmpty) {
				indent = getIndent(s);
				if (indent < lastIndent) {
					ret.add(s.substring(0,indent) + " } ") ;
					needColon=false;
				}
			}
			// #
			{
				int p1 = s.indexOf("#");
				if (p1>=0){
					s= s.substring(0,p1)+"// "+s.substring(p1+1);
					needColon=false;
				}
			}
			{	
				String s2 = s.trim();
				if (!s2.isEmpty()){
					char c = s2.charAt(s2.length()-1);
					if (c==','||c=='['||c=='('||c=='{')	needColon=false;
				}
			}			
			s = s.replace('\'', '"');
			{
				int p1 = s.length()-1;
				while(true){
					if (p1<0) break;
					char c = s.charAt(p1);
					if (c==' '||c=='\t') p1--;
					else break;
				}
				if (p1!= s.length()-1) s = s.substring(0, p1+1);
			}
			if (!isEmpty) lastIndent = indent;
			if (needColon ) s =s + " ;" ;
			ret.add(s);
		}
		return ret;
	}

	int getIndent(String s) {
		int i=0;
		while(true){
			if (i>=s.length()) break;
			char c = s.charAt(i);
			if (c==' '||c=='\t') i++;
			else break;
		}
		return i;
	}
}
