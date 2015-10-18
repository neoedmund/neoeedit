package neoe.ne;

public class ViewMailPluginRegist {

	public static void main(String[] args) throws Exception {
		ClassLoader cl = ViewMailPluginRegist.class.getClassLoader();
		cl.loadClass("neoe.mail.ViewMail").getField("neMainClass")
				.set(null, Main.class);
	}

}
