package neoe.ne.util;

import java.io.File;
import java.io.IOException;

/**
 * For the intelligence of launch a java, Find the JDK smartly.
 */
public class FindJDK {
	private static final boolean DEBUG = false;
	static String osname;
	static String osarch;
	public static boolean isWindows;
	static boolean isX86;

	// String[] pathWindow = new String[] { "Program Files/Java/" };
	// String[] pathWindow32 = new String[] { "Program Files (x86)/Java/" };
	// String[] pathLinux = new String[] { "/usr/lib/jvm/", };
	private boolean jdk;

	static {
		debug(osname = System.getProperty("os.name"));
		debug(osarch = System.getProperty("os.arch"));
		debug(System.getProperty("os.version"));
		isWindows = osname.indexOf("Windows") >= 0;
		isX86 = osarch.indexOf("x86") >= 0 && System.getenv("ProgramW6432") == null;
	}

	public FindJDK() {
	}

	public static void main(String[] args) throws IOException {

		// test

		System.out.println("JDK64=" + new FindJDK().find(64, true));
		System.out.println("JRE64=" + new FindJDK().find(64, false));
		System.out.println("JDK32=" + new FindJDK().find(32, true));
		System.out.println("JRE32=" + new FindJDK().find(32, false));
		System.out.println("JDK0=" + new FindJDK().find(0, true));
		System.out.println("JRE0=" + new FindJDK().find(0, false));
	}

	/**
	 * 
	 * @param bit
	 *            64,32,0==any
	 * @param jdk
	 *            true must be JDK, false JRE is okay
	 * @return
	 * @throws IOException
	 */
	public String find(int bit, boolean jdk) throws IOException {
		String path = "";
		this.jdk = jdk;
		System.out.println("isX86:" + isX86);
		if (isWindows) {
			if (isX86) {
				if (bit == 64) {
					System.out.println("This is Win32 but you need 64bit JDK");
					return "";
				}
				path = searchPath(new String[] { "Program Files/Java/" });
			} else {// 64
				if (bit == 32) {
					path = searchPath(new String[] { "Program Files (x86)/Java/" });
				} else if (bit == 0) {// any
					path = searchPath(new String[] { "Program Files/Java/", "Program Files (x86)/Java/" });
				} else {// bit==64
					path = searchPath(new String[] { "Program Files/Java/", });
				}
			}
		} else {// linux
			path = searchPath(new String[] { "/usr/lib/jvm/", "/usr/java/", "/usr/local/java/", "/opt/" });
		}
		if (path.isEmpty()) {
			path = System.getenv("JAVA_HOME");
		}
		if (path == null)
			path = "";
		return path;
	}

	private String searchPath(String[] paths) throws IOException {
		String driver = "";
		if (isWindows) {
			driver = System.getenv("SystemDrive") + "/";
		}
		for (String path : paths) {
			String s = searchAPath(driver + path);
			if (!s.isEmpty())
				return s;
		}
		return "";
	}

	private String searchAPath(String path) throws IOException {
		debug("search " + path);
		File p = new File(path);
		String latestVer = "";
		String ret = "";
		if (p.exists() && p.isDirectory()) {
			for (File f : p.listFiles()) {
				if (f.isDirectory()) {
					String fn = f.getName().toLowerCase();
					boolean isJavaDir = jdk ? (fn.indexOf("jdk") >= 0 || fn.indexOf("java") >= 0)
							: (fn.indexOf("jdk") >= 0 || fn.indexOf("jre") >= 0 || fn.indexOf("java") >= 0);
					if (!isJavaDir)
						continue;
					debug("check java dir:" + f.getCanonicalPath());
					boolean found = false;
					if (jdk) {
						if (isWindows) {
							found = new File(f, "bin/javac.exe").exists();
						} else {
							found = new File(f, "bin/javac").exists();
						}
					} else {
						if (isWindows) {
							found = new File(f, "bin/java.exe").exists();
						} else {
							found = new File(f, "bin/java").exists();
						}
					}
					if (found) {
						String ver = getVersion(f.getName());
						if (ver.compareTo(latestVer) > 0) {
							latestVer = ver;
							ret = f.getCanonicalPath();
						}

					}
				}
			}
		}
		return ret;
	}

	private static void debug(String s) {
		if (DEBUG)
			System.out.println("[D]" + s);

	}

	private String getVersion(String s) {
		int p1 = -1;
		int p2 = s.length();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isDigit(c) || c == '.' || c == '_') {
				if (p1 == -1) {
					p1 = i;
				} else {

				}
			} else {
				if (p1 >= 0) {
					p2 = i;
					break;
				} else {

				}
			}
		}
		if (p1 >= 0 && p2 > p1) {
			String s2 = s.substring(p1, p2).replace('_', '.');
			String[] ss = s2.split("\\.");
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 4; i++) {
				String s3 = i < ss.length ? ss[i] : "0";
				sb.append(String.format("%03d", Integer.parseInt(s3)));
				sb.append('.');
			}
			String ret = sb.toString();
			debug("find version:" + ret);
			return ret;
		}
		debug("cannot find version on:" + s);
		return "";
	}

}
