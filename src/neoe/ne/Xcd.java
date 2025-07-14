package neoe . ne ;

import java . awt . event . KeyAdapter ;
import java . awt . event . KeyEvent ;
import java . awt . event . MouseAdapter ;
import java . awt . event . MouseEvent ;
import java . io . BufferedReader ;
import java . io . File ;
import java . io . FileInputStream ;
import java . io . IOException ;
import java . io . InputStream ;
import java . io . InputStreamReader ;
import java . util . ArrayList ;
import java . util . Collections ;
import java . util . HashSet ;
import java . util . List ;
import java . util . Set ;
import javax . swing . JComponent ;
import javax . swing . JDialog ;
import javax . swing . JFrame ;
import javax . swing . JList ;
import javax . swing . JScrollPane ;
import javax . swing . ListSelectionModel ;

public class Xcd {
	public static String getUserHomeDir ( ) {
		return U . getUserHome ( ) ; // . getProperty ( "user.home" ) ;
	}

	public static String readString ( InputStream ins , String enc )
	throws IOException {
		if ( enc == null )
		enc = U . UTF8 ;
		BufferedReader in = new BufferedReader ( new InputStreamReader ( ins , enc ) ) ;
		char [ ] buf = new char [ 1000 ] ;
		int len ;
		StringBuffer sb = new StringBuffer ( ) ;
		while ( ( len = in . read ( buf ) ) > 0 )
		sb . append ( buf , 0 , len ) ;
		in . close ( ) ;
		return sb . toString ( ) ;
	}

	private String [ ] selection = new String [ 1 ] ;

	public String run ( String key ) {
		Set < String > uniq = new HashSet < String > ( ) ;
		try {
			readFromBashHistory ( uniq , key ) ;
		} catch ( Exception e ) {
		}
		try {
			readFromNeoeeditHistory ( uniq , key ) ;
		} catch ( Exception e ) {
		}
		if ( uniq . isEmpty ( ) ) {
			System . err . println ( "no cd entry, quit" ) ;
			return null ;
		}
		List < String > res = new ArrayList < String > ( uniq ) ;
		Collections . sort ( res ) ;
		System . setProperty ( "awt.useSystemAAFontSettings" , "on" ) ;
		String cd1 = selectFromList ( res ) ;
		if ( cd1 == null ) {
			System . err . println ( "not selected, quit" ) ;
			return null ;
		}
		return doCd ( cd1 ) ;
	}

	private void readFromNeoeeditHistory ( Set < String > uniq , String key )
	throws IOException {
		// ~/.neoeedit/dh.txt
		File hisfile = new File ( getUserHomeDir ( ) , ".neoeedit/dh.txt" ) ;
		if ( ! hisfile . exists ( ) ) {
			System . out . println ( "~/.neoeedit/dh.txt not exists" ) ;
			return ;
		}
		String [ ] ss = readString ( new FileInputStream ( hisfile ) , null ) . split ( "\n" ) ;
		for ( String s : ss ) {
			s = s . trim ( ) ;
			if ( ! ( s . startsWith ( "/" ) || s . startsWith ( "~/" ) ) )
			continue ;
			if ( key != null && ! s . toLowerCase ( ) . contains ( key ) )
			continue ;
			File f = new File ( s ) ;
			if ( f . isDirectory ( ) ) {
				s = f . getAbsolutePath ( ) ;
				uniq . add ( s ) ;
			}
		}
	}

	private void readFromBashHistory ( Set < String > uniq , String key )
	throws IOException {
		File hisfile = new File ( getUserHomeDir ( ) , ".bash_history" ) ;
		if ( ! hisfile . exists ( ) ) {
			System . out . println ( ".bash_history not exists" ) ;
			return ;
		}
		String [ ] ss = readString ( new FileInputStream ( hisfile ) , null ) . split ( "\n" ) ;
		for ( String s : ss ) {
			s = s . trim ( ) ;
			if ( ! s . startsWith ( "cd " ) )
			continue ;
			s = s . substring ( 3 ) . trim ( ) ;
			if ( ! ( s . startsWith ( "/" ) || s . startsWith ( "~/" ) ) )
			continue ;
			if ( key != null && ! s . toLowerCase ( ) . contains ( key ) )
			continue ;
			File f = new File ( s ) ;
			if ( f . isDirectory ( ) ) {
				s = f . getAbsolutePath ( ) ;
				uniq . add ( s ) ;
			}
		}
	}

	private String doCd ( String cd1 ) {
		System . out . println ( cd1 ) ;
		return cd1 ;
	}

	private String selectFromList ( List < String > res ) {
		if ( res . size ( ) == 1 )
		return res . get ( 0 ) ;
		final JList list = new JList ( res . toArray ( ) ) ;
		list . setSelectionMode ( ListSelectionModel . SINGLE_SELECTION ) ;
		final JDialog frame = wrapFrame ( new JScrollPane ( list ) , "select dir to cd" ) ;

		list . addMouseListener ( new MouseAdapter ( ) {
				@ Override
				public void mouseClicked ( MouseEvent e ) {
					if ( e . getClickCount ( ) > 1 ) {
						String s = ( String ) list . getSelectedValue ( ) ;
						selection [ 0 ] = s ;
						frame . dispose ( ) ;
					}
				}
			} ) ;
		list . addKeyListener ( new KeyAdapter ( ) {
				@ Override
				public void keyPressed ( KeyEvent e ) {
					int kc = e . getKeyCode ( ) ;
					if ( kc == KeyEvent . VK_ESCAPE )
					frame . dispose ( ) ;
					else if ( kc == KeyEvent . VK_ENTER ) {
						String s = ( String ) list . getSelectedValue ( ) ;
						selection [ 0 ] = s ;
						frame . dispose ( ) ;
					}
				}
			} ) ;

		frame . setVisible ( true ) ;
		frame . requestFocus ( ) ;
		return selection [ 0 ] ;
	}

	private JDialog wrapFrame ( JComponent comp , String title ) {
		JDialog frame = new JDialog ( ) ;
		frame . setTitle ( title ) ;
		frame . setModal ( true ) ;
		frame . add ( comp ) ;
		frame . setDefaultCloseOperation ( JFrame . DISPOSE_ON_CLOSE ) ;
		frame . setSize ( 400 , 600 ) ;
		frame . setLocationRelativeTo ( null ) ;
		return frame ;
	}
}
