package neoe . ne . util ;

import java . io . File ;
import java . nio . file . Files ;
import java . util . ArrayList ;
import java . util . Arrays ;
import java . util . Comparator ;
import java . util . Iterator ;
import java . util . List ;

public class FileIterator implements Iterable < File > {
	public interface Filter {
		boolean run ( File f ) ;
	}

	// private String root;
	List < Object [ ] > buf ;
	private boolean sortByName ;
	private int maxlevel = 0 ;
	private boolean followLink = true ;
	private Filter filter ;

	public FileIterator ( String dir , Filter filter , boolean sortByName , boolean followLink , int maxlevel ) {
		this . sortByName = sortByName ;
		this . maxlevel = maxlevel ;
		this . followLink = followLink ;
		this . filter = filter ;
		buf = new ArrayList < > ( ) ;
		File f = new File ( dir ) ;
		if ( f . isFile ( ) ) {
			boolean add = false ;
			if ( filter != null ) {
				if ( filter . run ( f ) ) {
					add = true ;
				}
			} else {
				add = true ;
			}
			if ( add ) {
				buf . add ( new Object [ ] { f , 0 } ) ;
			}
		} else {
			expend ( f , 0 ) ;
		}
	}

	public FileIterator ( String dir ) {
		this ( dir , null , false , false , 0 ) ;
	}

	public FileIterator ( String dir , boolean sortByName , boolean followLink ) {
		this ( dir , null , sortByName , followLink , 0 ) ;
	}

	public FileIterator ( String dir , boolean sortByName , int maxlevel ) {
		this ( dir , sortByName , maxlevel , null ) ;
	}

	public FileIterator ( String dir , boolean sortByName , int maxlevel , Filter filter ) {
		this ( dir , filter , sortByName , false , maxlevel ) ;
	}

	public FileIterator ( String dir , boolean sortByName , boolean followLink , int maxlevel , Filter filter ) {
		this ( dir , filter , sortByName , followLink , maxlevel ) ;
	}

	@ Override
	public Iterator < File > iterator ( ) {
		return new Iterator < File > ( ) {
			@ Override
			public boolean hasNext ( ) {
				return buf . size ( ) > 0 ;
			}

			@ Override
			public File next ( ) {
				Object [ ] row = buf . remove ( 0 ) ;
				File f = ( File ) row [ 0 ] ;
				int lv = ( int ) row [ 1 ] ;
				expend ( f , lv ) ;
				return f ;
			}

			@ Override
			public void remove ( ) {
			}
		} ;
	}

	private void expend ( File f , int lv ) {
		if ( f . isDirectory ( ) && ( maxlevel <= 0 || lv < maxlevel ) ) {
			if ( ( ! followLink ) && Files . isSymbolicLink ( f . toPath ( ) ) ) {
				// skip
			} else {
				if ( filter != null && filter . run ( f ) == false )
				return ;
				File [ ] sub = f . listFiles ( ) ;
				if ( sub != null ) {
					if ( sortByName ) { // fixme: after filter?
						sortFiles ( sub ) ;
					}
					Arrays . stream ( sub ) . forEach ( x -> {
							if ( filter != null && filter . run ( x ) == false )
							return ;
							buf . add ( new Object [ ] { x , lv + 1 } ) ;
						} ) ;
				}
			}
		}
	}

	public static void sortFiles ( File [ ] sub ) {
		Arrays . sort ( sub , new Comparator < File > ( ) {
				@ Override
				public int compare ( File o1 , File o2 ) {
					return o1 . getName ( ) . compareTo ( o2 . getName ( ) ) ;
				}
			} ) ;
	}

	static boolean ignoreSearch ( File f ) {
		String name = f . getName ( ) ;
		return f . isDirectory ( ) && ! name . equals ( ".svn" ) && ! name . equals ( ".cvs" ) && ! name . equals ( ".bzr" )
		&& ! name . equals ( ".git" ) ;
	}
}
