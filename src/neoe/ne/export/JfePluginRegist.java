package neoe.ne.export;

import java.io.File;

import neoe.ne.Main;

public class JfePluginRegist {

	public static void main(String[] args) throws Exception {
		ClassLoader cl = JfePluginRegist.class.getClassLoader();
		cl.loadClass("neoe.jfe.PluginRegist").getMethod("regist",
				//ClassLoader cl, String ext, int priority, String className
				new Class[] { ClassLoader.class, String.class, int.class, String.class }).invoke(null, 
						new Object[] { cl,"", 0,  "neoe.ne.JfePluginRegist"});

	}

	public static void open(File f) throws Exception{
		Main.main(new String[]{f.getAbsolutePath()});
	}
}
