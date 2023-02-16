/* neoe */
package neoe . ne ;

import java . io . File ;
import java . lang . reflect . Method ;
import java . net . URL ;
import java . net . URLClassLoader ;
import java . text . SimpleDateFormat ;
import java . util . ArrayList ;
import java . util . Date ;
import java . util . HashMap ;
import java . util . List ;
import java . util . Map ;

/**
 * one plug-in in one jar, or in one directory
 */
public class Plugin {
	public static boolean loaded = false ; // ClassLoader cl;
	public static void load ( ) throws Exception {
		if ( loaded ) {
			System . out . println ( "plugin seems already loaded, pass" ) ;
			return ;
		}
		loaded = true ;
		String libDir = U . getMyDir ( ) + "/plugins" ;
		new File ( libDir ) . mkdirs ( ) ;
		File libDir2 = new File ( libDir ) . getAbsoluteFile ( ) ;
		File [ ] it = libDir2 . listFiles ( ) ;

		int cnt = 0 ;
		for ( File f : it )
		if ( f . isDirectory ( ) ) {
			if ( addDirPlugin ( f ) )
			cnt ++ ;
		} else if ( f . getName ( ) . toLowerCase ( ) . endsWith ( ".jar" ) )
		if ( addJarPlugin ( f ) )
		cnt ++ ;
		System . out . println ( "added plugin count:" + cnt ) ;
	}

	private static boolean addDirPlugin ( File dir ) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy/MM/dd HH:mm:ss" ) ;
		System . out . println ( "add plugin in dir:" + dir . getAbsolutePath ( ) ) ;
		List < URL > jars = new ArrayList < URL > ( ) ;
		for ( File f : dir . listFiles ( ) )
		if ( f . isFile ( ) && f . getName ( ) . endsWith ( ".jar" ) ) {
			jars . add ( f . toURI ( ) . toURL ( ) ) ;
			System . out . println ( "\tadd: " + f . getName ( ) + " \t " + sdf . format ( new Date ( f . lastModified ( ) ) ) ) ;
		}
		URLClassLoader cl = new URLClassLoader ( jars . toArray ( new URL [ jars . size ( ) ] ) , Plugin . class . getClassLoader ( ) ) ;
		return initPlugin ( cl ) ;
	}

	private static boolean addJarPlugin ( File f ) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy/MM/dd HH:mm:ss" ) ;
		System . out . println ( "add plugin " + f . getAbsolutePath ( ) + " \t " + sdf . format ( new Date ( f . lastModified ( ) ) ) ) ;
		List < URL > jars = new ArrayList < URL > ( ) ;
		jars . add ( f . toURI ( ) . toURL ( ) ) ;
		URLClassLoader cl = new URLClassLoader ( jars . toArray ( new URL [ jars . size ( ) ] ) , Plugin . class . getClassLoader ( ) ) ;
		return initPlugin ( cl ) ;
	}

	private static boolean initPlugin ( ClassLoader cl ) throws Exception {
		try {
			Class clz = cl . loadClass ( "neoe.ne.plugin.PluginInit" ) ;
			Method run = clz . getMethod ( "run" , new Class [ ] { } ) ;
			run . invoke ( null ) ;
			return true ;
		} catch ( Exception e ) {
			e . printStackTrace ( ) ;
		}
		return false ;
	}

	public interface PluginAction {
		/**
		 * plugin is responsible for synchronization
		 */
		void run ( PlainPage pp ) throws Exception ;
	}

	/** another kind. not changing PluginAction for back-compatible */
	public interface PluginAction2 {
		void run ( PlainPage pp , String key ) throws Exception ;
	}

	public interface GoHandle {
		boolean run ( String line , PlainPage pp ) ;
	}

	public interface PluginCall {
		Object run ( Object pp ) throws Exception ;
	}

	public interface KeyHandle {
		PluginAction2 findAction ( String key ) throws Exception ;
	}

	static Map < String , PluginCall > calls = new HashMap < > ( ) ;
	static List < KeyHandle > keyHandles = new ArrayList < > ( ) ;

	public static Object call ( String key , Object param ) throws Exception {
		PluginCall pc = calls . get ( key ) ;
		if ( pc == null )
		return null ;
		return pc . run ( param ) ;
	}

	public static void registerCall ( String key , PluginCall pc ) {
		calls . put ( key , pc ) ;
	}

	public static void registerKeyHandle ( KeyHandle h ) {
		if ( h != null )
		keyHandles . add ( h ) ;
	}

	/**
	 *
	 * @param key0 like SHIFT-CTRL-ALT-X
	 * @param clz  action class
	 * @return null if OK, otherwise reason
	 */
	public static String registerCmd ( String key0 , PluginAction pa ) {
		String key ;
		try {
			key = U . getKeyNameFromTextName ( key0 ) ;
		} catch ( Exception e ) {
			return String . format ( "Key '%s' is not correct:%s" , key0 , e ) ;
		}
		if ( U . keys . containsKey ( key ) )
		return String . format ( "Key '%s[%s]' is already in use by build-in function '%s'." , key0 , key ,
			U . keys . get ( key ) . name ( ) ) ;
		if ( U . pluginKeys . containsKey ( key ) )
		return String . format ( "Key '%s[%s]' is already in use by other plugin '%s'." , key0 , key ,
			U . pluginKeys . get ( key ) . getClass ( ) . getName ( ) ) ;
		U . pluginKeys . put ( key , pa ) ;
		return null ;
	}

	public static List < GoHandle > goHandles = new ArrayList < > ( ) ;

	public static boolean goHandle ( String line , PlainPage pp ) {
		for ( GoHandle h : goHandles ) {
			if ( h . run ( line , pp ) )
			return true ;
		}
		return false ;
	}

	public static PluginAction2 findAction ( String key ) throws Exception {
		for ( KeyHandle h : keyHandles ) {
			PluginAction2 a = h . findAction ( key ) ;
			if ( a != null )
			return a ;
		}
		return null ;
	}
}
