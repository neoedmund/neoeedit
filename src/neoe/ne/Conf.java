/*
 *  
 */
package neoe . ne ;

import java . awt . Color ;
import java . awt . Dimension ;
import java . awt . Font ;
import java . awt . GraphicsEnvironment ;
import java . awt . RenderingHints ;
import java . io . BufferedReader ;
import java . io . File ;
import java . io . IOException ;
import java . lang . reflect . Field ;
import java . util . ArrayList ;
import java . util . Arrays ;
import java . util . Enumeration ;
import java . util . HashMap ;
import java . util . HashSet ;
import java . util . List ;
import java . util . Map ;
import java . util . Set ;
import javax . swing . UIDefaults ;
import javax . swing . UIManager ;
import static neoe . ne . U . addKey ;
import neoe . ne . util . PyData ;

/**
 *
 * @author neoe
 */
public class Conf {
	static Map conf ;

	public static Map getConfig ( ) {
		if ( conf != null )
		return conf ;
		try {
			System . out . println ( "load " + Version . CONFIG_FN ) ;
			BufferedReader in = new BufferedReader ( U . getInstalledReader ( Version . CONFIG_FN ) ) ;
			conf = ( Map ) new PyData ( ) . parseAll ( in ) ;
			return conf ;
		} catch ( Exception ex ) {
			throw new RuntimeException ( "fatal: cannot read conf " + Version . CONFIG_FN , ex ) ;
		}
	}

	public static Color getDefaultBgColor ( ) {
		Map config = getConfig ( ) ;
		Map colorConf = ( Map ) config . get ( "color" ) ;
		String value = "" + colorConf . get ( "defaultBackgroundColor" ) ;
		Color c = Color . WHITE ;
		if ( value . startsWith ( "0x" ) )
		c = Color . decode ( value ) ;
		else
		c = Color . getColor ( value , c ) ;
		return c ;
	}

	public static int getDefaultColorMode ( ) {
		Map config = getConfig ( ) ;
		String v = ( String ) ( ( Map ) config . get ( "color" ) ) . get ( "defaultMode" ) ;
		if ( v != null ) try {
			return Integer . parseInt ( v ) ;
		} catch ( NumberFormatException e ) {
		}
		return 0 ;
	}

	public static void loadOtherConfig ( EditorPanelConfig conf ) {
		Map config = getConfig ( ) ;
		String v = "" + config . get ( "KEY_TEXT_ANTIALIASING" ) ;
		if ( v . length ( ) == 0 || "null" . equals ( v ) )
		return ;
		try {
			Field f = RenderingHints . class . getDeclaredField ( v ) ;
			Object o = f . get ( null ) ;
			if ( o != null )
			conf . VALUE_TEXT_ANTIALIAS = o ;
		} catch ( Exception e ) {
			System . out . println ( "cannot find in RenderingHints:" + v ) ;
		}
	}
	static List < String > localFonts ;
	static FontList defaultConsoleFonts ;
	static FontList defaultFontList
	= Conf . getFont ( new Font [ ] { new Font ( "Monospaced" , Font . PLAIN , 12 ) ,
			new Font ( "Simsun" , Font . PLAIN , 12 ) } ) ;

	public static FontList getFont ( Font [ ] defaultIfFail ) {
		try {
			Map config = getConfig ( ) ;
			Map m = ( Map ) config . get ( "font" ) ;
			Font defaultConsoleFont = null ;
			{
				List console = ( List ) m . get ( "console" ) ;
				if ( console != null )
				defaultConsoleFont = getFontFromDesc ( console ) ;
			}
			Object v = m . get ( "list" ) ;
			if ( v == null || "null" . equals ( v ) )
			return new FontList ( defaultIfFail ) ;
			else {
				if ( localFonts == null )
				localFonts
				= Arrays . asList ( GraphicsEnvironment . getLocalGraphicsEnvironment ( )
					. getAvailableFontFamilyNames ( ) ) ;
				List < Font > fonts = new ArrayList < Font > ( ) ;
				for ( Object o : ( List ) v ) {
					List l = ( List ) o ;
					Font font = getFontFromDesc ( l ) ;
					if ( font != null )
					fonts . add ( font ) ;
				}
				for ( Font f : defaultIfFail )
				fonts . add ( f ) ;

				FontList ret = new FontList ( fonts . toArray ( new Font [ fonts . size ( ) ] ) ) ;
				if ( defaultConsoleFont == null )
				defaultConsoleFonts = ret ;
				else {
					fonts . add ( 0 , defaultConsoleFont ) ;
					defaultConsoleFonts
					= new FontList ( fonts . toArray ( new Font [ fonts . size ( ) ] ) ) ;
				}
				return ret ;
			}
		} catch ( Exception e ) {
			e . printStackTrace ( ) ;
			return new FontList ( defaultIfFail ) ;
		}
	}

	@ SuppressWarnings ( "rawtypes" )
	static void initKeys ( ) throws Exception {
		Map o = getConfig ( ) ;
		List o1 = ( List ) ( ( Map ) o ) . get ( "keys" ) ;
		U . originKeys = o1 ;
		U . keys = new HashMap < > ( ) ;
		U . pluginKeys = new HashMap < > ( ) ;
		Set < String > keys = new HashSet < > ( ) ;
		for ( Object o2 : o1 ) {
			List row = ( List ) o2 ;
			String cmd = row . get ( 0 ) . toString ( ) ;
			Object kk = row . get ( 1 ) ;
			if ( kk instanceof List )
			for ( Object k : ( List ) kk ) {
				String key = k . toString ( ) . toUpperCase ( ) ;
				addOneKey ( key , cmd , keys ) ;
			}
			else {
				String key = row . get ( 1 ) . toString ( ) . toUpperCase ( ) ;
				addOneKey ( key , cmd , keys ) ;
			}
		}
		addKey ( U . keys , "alt-Enter" , "ShellCommand" ) ;
	}

	private static void addOneKey ( String key , String cmd , Set < String > keys ) {
		if ( keys . contains ( key ) ) {
			System . err . println ( "Error: duplicated key:" + key ) ;
			return ;
		}
		keys . add ( key ) ;
		try {
			addKey ( U . keys , key , cmd ) ;
		} catch ( Exception e ) {
			e . printStackTrace ( ) ;
		}
	}

	public static int [ ] [ ] loadColorModes ( ) throws IOException {
		Map config = getConfig ( ) ;
		List l = ( List ) ( ( Map ) config . get ( "color" ) ) . get ( "modes" ) ;
		int colorCnt = 12 ;
		int [ ] [ ] modes = new int [ l . size ( ) ] [ colorCnt ] ;
		for ( int i = 0 ; i < l . size ( ) ; i ++ ) {
			List row = ( List ) l . get ( i ) ;
			for ( int j = 1 ; j <= colorCnt ; j ++ ) {
				int v ;
				Object o = row . get ( j ) ;
				v = U . parseInt ( o . toString ( ) ) ;
				modes [ i ] [ j - 1 ] = v ;
			}
		}
		return modes ;
	}

	public static Dimension readFrameSize ( ) {
		Map config = getConfig ( ) ;
		List l = ( List ) config . get ( "frameSize" ) ;
		if ( l != null )
		return new Dimension ( U . parseInt ( l . get ( 0 ) ) , U . parseInt ( l . get ( 1 ) ) ) ;
		return new Dimension ( 800 , 600 ) ;
	}

	public static int readTabWidth ( ) {
		try {
			Map config = getConfig ( ) ;
			return Integer . parseInt ( config . get ( "tabWidthInPixel" ) . toString ( ) ) ;
		} catch ( NumberFormatException e ) {
		}
		return 40 ;
	}

	public static void setDefaultBKColor ( ) throws IOException {
		UIDefaults uiDefaults = UIManager . getDefaults ( ) ;
		for ( Enumeration e = uiDefaults . keys ( ) ; e . hasMoreElements ( ) ; ) {
			Object obj = e . nextElement ( ) ;
			if ( obj instanceof String )
			if ( ( ( String ) obj ) . contains ( "background" )
				&& uiDefaults . get ( obj ) instanceof Color ) {
				// System.out.println(obj);
				uiDefaults . put ( obj , getDefaultBgColor ( ) ) ;
				UIManager . put ( obj , getDefaultBgColor ( ) ) ;
			}
		}
	}

	public static void setDefaultLookAndFeel ( ) throws IOException {
		Map config = getConfig ( ) ;
		String v = "" + config . get ( "lookAndFeel" ) ;
		if ( v . length ( ) == 0 || "null" . equals ( v ) )
		return ;
		try {
			Class . forName ( v ) ;
			try {
				UIManager . setLookAndFeel ( v ) ;
			} catch ( Exception e ) {
				e . printStackTrace ( ) ;
			}
		} catch ( ClassNotFoundException e ) {
			System . out . println ( "not found lookAndFeel:" + e ) ;
		}
	}

	public static Object get ( String path , Object dv ) throws IOException {
		Object o = get ( getConfig ( ) , path ) ;
		if ( o == null )
		return dv ;
		return o ;
	}

	/**
	 * xxx.[2].yyy.[0]
	 */
	public static Object get ( Map config , String name ) {
		String [ ] ss = name . split ( "\\." ) ;
		Object node = config ;
		Object o = null ;
		for ( int i = 0 ; i < ss . length ; i ++ ) {
			if ( node == null )
			return null ;
			String s = ss [ i ] ;
			if ( s . startsWith ( "[" ) && s . endsWith ( "]" ) ) {
				int p = Integer . parseInt ( s . substring ( 1 , s . length ( ) - 1 ) ) ;
				if ( node instanceof Map )
				o = ( ( Map ) node ) . values ( ) . toArray ( ) [ p ] ;
				else
				o = ( ( List ) node ) . get ( p ) ;
			} else
			o = ( ( Map ) node ) . get ( s ) ;
			node = o ;
		}
		// Log.log("config["+name+"]="+o);
		return o ;
	}

	private static Font getFontFromDesc ( List l ) throws Exception {
		String fontfn = ( String ) l . get ( 0 ) ;
		File f = new File ( fontfn ) ;
		Font font = null ;
		int fontsize = Integer . parseInt ( l . get ( 1 ) . toString ( ) ) ;
		if ( f . exists ( ) && f . isFile ( ) ) {
			font = Font . createFont ( Font . TRUETYPE_FONT , f ) ;
			if ( font == null ) {
				System . out . println ( "cannot load truetype font:" + fontfn ) ;
				return null ;
			}
			System . out . println ( "load font file:" + fontfn
				+ ",name=" + font . getFontName ( ) ) ;
		} else {
			if ( localFonts == null )
			localFonts
			= Arrays . asList ( GraphicsEnvironment . getLocalGraphicsEnvironment ( )
				. getAvailableFontFamilyNames ( ) ) ;
			if ( localFonts . contains ( fontfn ) )
			font = new Font ( fontfn , Font . PLAIN , 12 ) ;
			else
			System . out . println ( "font file not exists:" + fontfn ) ;
		}
		if ( font != null )
		if ( l . size ( ) > 2 && l . get ( 2 ) . equals ( "BOLD" ) )
		font = font . deriveFont ( Font . BOLD , fontsize ) ;
		else if ( l . size ( ) > 2 && l . get ( 2 ) . equals ( "ITALIC" ) )
		font = font . deriveFont ( Font . ITALIC , fontsize ) ;
		else
		font = font . deriveFont ( Font . PLAIN , fontsize ) ;
		return font ;
	}
}
