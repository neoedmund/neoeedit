package neoe . ne ;

import java . io . BufferedReader ;
import java . io . File ;
import java . io . IOException ;
import java . io . InputStream ;
import java . io . InputStreamReader ;
import java . io . PrintWriter ;
import java . io . StringWriter ;
import java . net . URL ;
import java . net . URLClassLoader ;
import java . nio . file . Files ;
import java . util . ArrayList ;
import java . util . List ;
import neoe . ne . util . FileUtil ;
import neoe . ne . util . FindJDK ;
import neoe . ne . util . Finder ;

public class ScriptUtil {
	public void runSingleScript ( PlainPage pp , String script ,
		List < CharSequence > input ) throws Exception {
		// 3.1.
		String neoeeditCP = findMyCP ( ) ;
		System . out . println ( "mycp=" + neoeeditCP ) ;
		String javaPath = new FindJDK ( ) . find ( 0 , true ) ;
		String javac = javaPath + ( FindJDK . isWindows ? "/bin/javac.exe" : "/bin/javac" ) ;

		// 1.
		String className = findClassName ( script ) ;
		if ( className . isEmpty ( ) )
		error ( "Sorry, I cannot find the public class name in the script" ) ;
		// 1.1
		String packageName = findPackageName ( script ) ;
		if ( className . isEmpty ( ) ) {
			// error("Sorry, I cannot find the package name in the script");
		}
		// 2. save script to file
		File dir = Files . createTempDirectory ( "neoeedit" ) . toFile ( ) ;
		File src = new File ( dir , className + ".java" ) ;
		FileUtil . save ( script . getBytes ( "utf8" ) , src . getAbsolutePath ( ) ) ;
		// 3. compile
		if ( ! javaPath . isEmpty ( ) )
		log ( "found latest JDK:" + javaPath ) ;
		else
		error ( "didnot found JDK" ) ;
		Exec exec = new Exec ( ) ;
		exec . setCmd ( javac ) ;
		File destdir = new File ( dir , "bin" ) ;
		destdir . mkdirs ( ) ;
		exec . addArg ( "-d" , destdir . getAbsolutePath ( ) ) ;
		exec . addArg ( "-encoding" , "utf8" ) ;
		exec . addArg ( "-cp" , new File ( neoeeditCP ) . getAbsolutePath ( ) ) ;
		exec . addArg ( src . getAbsolutePath ( ) ) ;
		int retcode = exec . execute ( ) ;
		if ( retcode != 0 )
		throw new RuntimeException ( "Java Compile failed: " + exec . sw ) ;
		else
		log ( "compiled to " + destdir . getAbsolutePath ( ) ) ;
		// 4.
		URL bin = destdir . toURI ( ) . toURL ( ) ;
		log ( "bin=" + bin ) ;
		ClassLoader ncl
		= new URLClassLoader ( new URL [ ] { bin } , U . class . getClassLoader ( ) ) ;
		Class cls = ncl . loadClass (
			packageName . isEmpty ( ) ? className : ( packageName + "." + className ) ) ;
		Script sc = ( Script ) cls . newInstance ( ) ;
		List < CharSequence > ret = sc . run ( input ) ;
		// delete when hot
		deleteDir ( destdir , ".class" ) ;
		src . delete ( ) ;
		dir . delete ( ) ;
		if ( ! dir . exists ( ) )
		log ( "temp dir deleted successfully:" + dir . getAbsolutePath ( ) ) ;
		String hint = "result of [" + className + "]" ;
		PlainPage p2 = new PlainPage ( pp . uiComp , PageData . fromTitle ( String . format ( "%s#%s" , hint , U . randomID ( ) ) ) , pp ) ;
		p2 . pageData . resetLines ( ret ) ;
		p2 . uiComp . repaint ( ) ;
		p2 . uiComp . grabFocus ( ) ;
	}

	private void deleteDir ( File dir , String ends ) {
		File [ ] sub = dir . listFiles ( ) ;
		for ( File f : sub )
		if ( f . isFile ( ) && f . getName ( ) . endsWith ( ends ) )
		f . delete ( ) ;
		else if ( f . isDirectory ( ) ) {
			deleteDir ( f , ends ) ;
			f . delete ( ) ;
		}
		dir . delete ( ) ;
	}

	private static String findPackageName ( String script ) {
		Finder f = new Finder ( script ) ;
		f . find ( "package " ) ;
		String s = f . readUntil ( ";" ) . trim ( ) ;
		return s ;
	}

	private static void error ( String s ) {
		throw new RuntimeException ( s ) ;
	}

	private String findMyCP ( ) {
		URL location = U . class . getResource ( '/' + U . class . getName ( ) . replace ( '.' , '/' ) + ".class" ) ;
		if ( location == null )
		error ( "Sorry I cannot find where the neoeedit.jar is located." ) ;
		String path = location . getPath ( ) ;
		System . out . println ( "path=" + path ) ;
		if ( path . startsWith ( "file:" ) )
		path = path . substring ( "file:" . length ( ) ) ;
		int p1 = path . indexOf ( '!' ) ;
		if ( p1 < 0 ) {
			int p2 = path . lastIndexOf ( "/bin/" ) ;
			if ( p2 < 0 )
			error ( "cannot understand the path:" + path ) ;
			else
			p1 = p2 + "/bin/" . length ( ) ;
		}
		path = path . substring ( 0 , p1 ) ;
		return path ;
	}

	private static void log ( String s ) {
		System . out . println ( s ) ;
	}

	private static String findClassName ( String script ) {
		Finder f = new Finder ( script ) ;
		f . find ( "public class " ) ;
		String s = f . readUntil ( " " ) . trim ( ) ;
		return s ;
	}

	class Exec {
		public StringWriter sw = new StringWriter ( ) ;
		List < String > sb ;

		public void setCmd ( String executable ) {
			sb = new ArrayList < > ( ) ;
			sb . add ( executable ) ;
		}

		public void addArg ( String s ) {
			sb . add ( s ) ;
		}

		public void addArg ( String s1 , String s2 ) {
			sb . add ( s1 ) ;
			sb . add ( s2 ) ;
		}

		public int execute ( ) throws Exception {
			System . out . println ( sb . toString ( ) ) ;
			Process p = new ProcessBuilder ( ) . command ( sb ) . start ( ) ;
			StreamGobbler errorGobbler
			= new StreamGobbler ( p . getErrorStream ( ) , "stderr" ) ;
			StreamGobbler outputGobbler
			= new StreamGobbler ( p . getInputStream ( ) , "stdout" ) ;
			outputGobbler . start ( ) ;
			errorGobbler . start ( ) ;
			return p . waitFor ( ) ;
		}

		private class StreamGobbler extends Thread {
			InputStream is ;
			String type ;
			private PrintWriter out ;

			private StreamGobbler ( InputStream is , String type ) {
				setDaemon ( true ) ;
				this . is = is ;
				this . type = type ;
				out = new PrintWriter ( sw ) ;
			}

			@ Override
			public void run ( ) {
				try {
					InputStreamReader isr = new InputStreamReader ( is ) ;
					BufferedReader br = new BufferedReader ( isr ) ;
					String line = null ;
					while ( ( line = br . readLine ( ) ) != null )
					out . println ( type + "> " + line ) ;
				} catch ( IOException ioe ) {
					ioe . printStackTrace ( ) ;
				}
			}
		}
	}
}
