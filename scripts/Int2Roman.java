
import java.util.ArrayList;
import java.util.List;

import neoe.ne.Script;

/**print out 1-100 in Roman */
public class Int2Roman implements Script {

	@Override
	public List<CharSequence> run(List<CharSequence> lines) {
		List<CharSequence> ret = new ArrayList<CharSequence>();
		for (int i=1;i<=100;i++) {
			ret.add(intToRoman(i));
		}
		return ret;
	}
String intToRoman(int num) {
	String c[][]={{"0","I","II","III","IV","V","VI","VII","VIII","IX"},{"0","X","XX","XXX","XL","L","LX"
	,"LXX","LXXX","XC"},{"0","C","CC","CCC","CD","D",
	"DC","DCC","DCCC","CM"},{"0","M","MM","MMM"}};
	int t=1;
	int tmp=num;
	String st="";
	if(tmp/1000!=0) st+=c[3][tmp/1000];
	if((tmp%1000)/100!=0) st+=c[2][(tmp%1000)/100];
	if((tmp%100)/10!=0) st+=c[1][(tmp%100)/10];
	if(tmp%10!=0) st+=c[0][tmp%10];
	return st;
}
}
