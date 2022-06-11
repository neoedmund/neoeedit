package neoe . ne ;

import java . io . File ;

public class Main {
	static boolean init = false ;

	public static void doinit ( ) throws Exception {
		if ( init )
		return ;
		else
		init = true ;
		Conf . setDefaultLookAndFeel ( ) ;
		Conf . setDefaultBKColor ( ) ;
		Conf . initKeys ( ) ;
		U . loadTabImage ( ) ;
		U . addTime = "true" . equals ( System . getenv ( U . NE_ADDTIME ) ) ;
		//		Gimp.loadFromConfig();
		Plugin . load ( ) ;
	}

	public static void main ( String [ ] args ) throws Exception {
		EditorPanel editor = null ;
		int pic = 0 ;
		for ( String fn : args ) {
			if ( fn . startsWith ( "-" ) )
			continue ; //no opt yet
			File f = new File ( fn ) ;
			if ( f . isFile ( ) )
			if ( U . isImageFile ( f ) ) {
				new PicView ( ) . show ( f ) ;
				pic ++ ;
			} else {
				if ( editor == null ) editor = new EditorPanel ( ) ;
				new PlainPage ( editor , PageData . fromFile ( f . getAbsolutePath ( ) ) , null ) ;
			}
			else {
				if ( editor == null ) editor = new EditorPanel ( ) ;
				PlainPage pp = new PlainPage ( editor , PageData . newUntitled ( ) , null ) ;
				pp . pageData . setText ( fn ) ;
			}
		}
		if ( editor == null && pic == 0 )
		editor = new EditorPanel ( ) ;

		if ( editor != null ) {
			editor . openWindow ( ) ;
			U . optimizeFileHistory ( ) ;
			SwingJniJvmPatch ( ) ;
		}
	}

	/**
	 * something like https://forums.oracle.com/thread/1542114
	 */
	private static void SwingJniJvmPatch ( ) {
		while ( true ) {
			try {
				Thread . sleep ( 100 ) ;
			} catch ( InterruptedException e ) {
				e . printStackTrace ( ) ;
			}
			if ( EditorPanel . openedWindows <= 0 ) {
				System . out . println ( "SwingJniJvmPatch exit" ) ;
				break ;
			}
		}
	}
}
