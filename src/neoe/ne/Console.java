package neoe . ne ;

import java . io . File ;
import java . io . InputStream ;
import java . io . OutputStream ;
import java . util . Arrays ;

public class Console {
	public String cmd ;
	private boolean finished ;
	OutputStream out ;
	InputStream stdout ;
	InputStream stderr ;
	Process proc ;
	private PlainPage pp ;
	private EditorPanel parentUI ;
	private File dir ;

	public Console ( String cmd , OutputStream out , InputStream stdout ,
		InputStream stderr , Process proc , EditorPanel uiComp ,
		File dir ) {
		this . cmd = cmd ;
		this . out = out ;
		this . stdout = stdout ;
		this . stderr = stderr ;
		this . proc = proc ;
		this . parentUI = uiComp ;
		this . dir = dir ;
	}

	public void start ( ) throws Exception {
		final long t1 = System . currentTimeMillis ( ) ;
		finished = false ;
		EditorPanel ep = parentUI ;
		this . pp = parentUI . page ;
		if ( pp . fontList == null || pp . fontList == Conf . defaultFontList )
		pp . fontList = Conf . defaultConsoleFonts ;
		pp . console = this ;
		ep . changeTitle ( ) ;
		final String id = String . format ( "[%s] %s\n" , dir . getAbsolutePath ( ) , cmd ) ;
		{
			PageData pageData = pp . pageData ;
			pageData . encoding = System . getProperty ( "sun.jnu.encoding" ) ;
			if ( pageData . encoding == null )
			pageData . encoding = U . UTF8 ;
			if ( dir != null )
			pp . workPath = dir . getAbsolutePath ( ) ;
			pp . ptEdit . append ( id ) ;
		}
		U . attach ( pp , stdout , "stdout" ) ;
		U . attach ( pp , stderr , "stderr" ) ;
		new Thread ( ( ) -> {
				try {
					proc . waitFor ( ) ;
					pp . pageData . editRec . appendLines ( Arrays . asList (
							String . format ( "\nExit(%s) in about %,d ms for\n%s" , proc . exitValue ( ) ,
								System . currentTimeMillis ( ) - t1 , id ) . split ( "\n" ) ) ) ;
					pp . pageData . editRec . appendLine ( "" ) ;
					pp . cursor . setSafePos ( 0 , Integer . MAX_VALUE ) ; //go last line
					finished = true ;
					pp . adjustCursor ( ) ;
					pp . uiComp . repaint ( ) ;
				} catch ( InterruptedException e ) {
					pp . pageData . editRec . appendLine ( "Interrupted:" + e ) ;
				}
			} ) . start ( ) ;
	}

	public static String filterSimpleTTY ( String s ) {
		while ( true ) { {
				String k1 = "[" ;
				int p1 = s . indexOf ( k1 ) ;
				if ( p1 >= 0 ) {
					int p2 = s . indexOf ( "m" , p1 + k1 . length ( ) ) ;
					if ( p2 >= 0 ) {
						s = s . substring ( 0 , p1 ) + s . substring ( p2 + 1 ) ;
						continue ;
					}
				}
			}
			break ;
		}
		return s ;
	}
}
