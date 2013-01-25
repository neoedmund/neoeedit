package neoe.util.onetime;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import neoe.ne.U;
import neoe.util.FileUtil;

public class MakeKeywords {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		new MakeKeywords().addTextFile("E:/neoe/kw_shader2");

	}

	private void addTextFile(String fn) throws Exception {
		String text = FileUtil.readString(new FileInputStream(fn), "utf8");
		String[] ss  = text.split(" |\n");
		Set<String> kws1 =new HashSet<String>();
		addKw(kws1,ss);
		addKw(kws1,U.KWS);
		List<String> kws= new ArrayList<String>(kws1);
		Collections.sort(kws);
		output(kws);
	}

	private void output(List<String> sl) {
		StringBuffer sb= new StringBuffer();
		for (String s:sl){
			sb.append(" ").append(s);
		}
		System.out.println("size:"+sl.size());
		System.out.println(sb.toString());		
	}

	private void addKw(Collection<String> sl, String[] ss) {
		for(String s:ss){
			String x=s.trim();
			if (x.isEmpty())continue;
			sl.add(x);
		}
	}

}
