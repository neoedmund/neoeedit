package neoe . util . onetime ;

import java . io . File ;
import java . nio . file . Files ;
import java . util . ArrayList ;
import java . util . Collections ;
import java . util . HashSet ;
import java . util . List ;
import java . util . Set ;
import neoe . ne . U ;
import neoe . ne . util . FileUtil ;

/**
 * remove filename not exists
 */
public class RefreshFileHistory {
	public static void main ( String [ ] args ) throws Exception {
		File fhn = U . getFileHistoryName ( ) ;
		List < String > fs = Files . readAllLines ( fhn . toPath ( ) ) ;
		Set < String > e = new HashSet < > ( ) ;
		List < String > fs2 = new ArrayList < > ( ) ;
		for ( int i = fs . size ( ) - 1 ; i >= 0 ; i -- ) {
			String s = fs . get ( i ) ;
			boolean fail = false ;
			int p1 = s . lastIndexOf ( '|' ) ;
			if ( p1 > 0 ) {
				String fn = s . substring ( 0 , p1 ) ;
				if ( ! new File ( fn ) . isFile ( ) )
				fail = true ;
				else
				if ( e . contains ( fn ) )
				fail = true ;
				else
				e . add ( fn ) ;
			} else
			fail = true ;
			if ( fail )
			System . out . println ( "fail: " + s ) ;
			else
			fs2 . add ( s ) ;
		}
		Collections . reverse ( fs2 ) ;
		FileUtil . save ( String . join ( "\n" , fs2 ) . getBytes ( "utf8" ) ,
			fhn . getAbsolutePath ( ) ) ;
	}
}
