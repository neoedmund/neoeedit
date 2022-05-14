package neoe . ne ;

import java . io . File ;

public class Main {
	public static void main ( String [ ] args ) throws Exception {
		if ( args . length > 0 ) {
			File f = new File ( args [ 0 ] ) ;
			if ( U . isImageFile ( f ) ) {
				new PicView ( ) . show ( f ) ;
			} else {
				EditorPanel editor = new EditorPanel ( EditorPanelConfig . DEFAULT ) ;
				PlainPage . getPP ( editor , PageData . newFromFile ( f . getAbsolutePath ( ) ) ) ;
				editor . openWindow ( ) ;
				SwingJniJvmPatch ( ) ;
			}
		} else {
			EditorPanel editor = new EditorPanel ( EditorPanelConfig . DEFAULT ) ;
			editor . openWindow ( ) ;
			SwingJniJvmPatch ( ) ;
		}
	}

	/**
	 * something like https://forums.oracle.com/thread/1542114
	 */
	private static void SwingJniJvmPatch ( ) {
		while ( true ) {
			try {
				Thread . sleep ( 1000 ) ;
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
