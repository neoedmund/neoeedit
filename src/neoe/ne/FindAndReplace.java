/*
 *  
 */
package neoe . ne ;

import java . awt . Container ;
import java . awt . Point ;
import java . awt . Window ;
import java . io . BufferedReader ;
import java . io . File ;
import java . io . FileInputStream ;
import java . io . InputStreamReader ;
import java . util . ArrayList ;
import java . util . List ;
import javax . swing . JFrame ;
import javax . swing . JOptionPane ;
import neoe . ne . util . FileIterator ;
import neoe . ne . util . PyData ;

/**
 *
 * @author neoe
 */
class FindAndReplace {
	private static final String [ ] binExt = {
		".jar" , ".class" , ".o" , ".so" , ".out" , ".ko" , ".exe" , ".dll" ,
		".jpg" , ".gif" , ".png" , ".mp3" , ".mp4" , ".war" , ".zip" , ".gz" ,
		".rar" , ".7z" , ".ttc" , ".ttf" , ".pdf" , ".xlsx" , ".xls" , ".mpeg" ,
		".bz2" , ".bin" , ".xz" , ".bz2" , ".iso" } ;

	private static boolean _endsWithAny ( String [ ] binExt , String name ) {
		name = name . toLowerCase ( ) ;
		for ( String ext : binExt )
		if ( name . endsWith ( ext ) )
		return true ;
		return false ;
	}

	private static void checkSearchKW ( List tfs , PlainPage page ) {
		for ( Object o : tfs )
		if ( ! ( o instanceof String ) ) {
			String msg = "bad search word, it should be in special format" ;
			JOptionPane . showMessageDialog ( page . uiComp , msg ) ;
			throw new RuntimeException ( msg ) ;
		}
	}

	static void doFindInDir ( PlainPage page , String text , boolean ignoreCase ,
		boolean selected2 , boolean inDir , String dir ,
		String fnFilter , boolean word ) throws Exception {
		Iterable < File > it = new FileIterator ( dir ) ;
		List < String > all = new ArrayList < String > ( ) ;
		fnFilter = fnFilter . trim ( ) . toLowerCase ( ) ;
		List fs = ( List ) PyData . parseAll ( "[" + fnFilter + "]" , false , true ) ;
		// search, skip binary, filtered
		int [ ] cnts = new int [ 3 ] ;
		for ( File f : it ) {
			if ( f . isDirectory ( ) )
			continue ;
			if ( ! fs . isEmpty ( ) ) {
				String fn = f . getName ( ) . toLowerCase ( ) ;
				boolean match = false ;
				for ( Object ft : fs )
				if ( fn . contains ( ft . toString ( ) ) ) {
					match = true ;
					break ;
				}
				if ( ! match ) {
					cnts [ 2 ] ++ ;
					continue ;
				}
			}
			if ( text . isEmpty ( ) ) { // do filename search
				all . add ( f . getAbsolutePath ( ) ) ;
			} else {
				List < String > res = findInFile ( f , text , ignoreCase , cnts , word ) ;
				all . addAll ( res ) ;
			}
		}
		showResult ( page , all , "dir" , dir , text , fnFilter , cnts , word ) ;
		page . uiComp . repaint ( ) ;
	}

	static void doFindInPage ( PlainPage page , String text2find , boolean ignoreCase ,
		boolean word ) throws Exception {
		text2find = text2find . trim ( ) ;
		page . ptFind . text2find = text2find ;
		if ( text2find != null && text2find . length ( ) > 0 ) {
			List tfs = null ;
			if ( ignoreCase )
			text2find = text2find . toLowerCase ( ) ;
			tfs = ( List ) PyData . parseAll ( "[" + text2find + "]" , false , true ) ;
			checkSearchKW ( tfs , page ) ;
			Point p = find ( page , tfs , 0 , 0 , ignoreCase , word ) ;
			if ( p == null )
			page . ui . message ( "string not found" ) ;
			else {
				List < String > all = new ArrayList < String > ( ) ;
				while ( true ) {
					all . add ( String . format ( "%s:%s" , p . y + 1 ,
							page . pageData . roLines . getline ( p . y ) ) ) ;
					Point p2 = find ( page , tfs , 0 , p . y + 1 , ignoreCase , word ) ;
					if ( p2 == null || p2 . y <= p . y )
					break ;
					else
					p = p2 ;
				}
				String type = page . pageData . fileLoaded ? "file" : "page" ;
				showResult ( page , all , type , page . pageData . title , text2find , null ,
					null , word ) ;
				page . uiComp . repaint ( ) ;
			}
		}
	}

	static void doReplace ( PlainPage page , String text , boolean ignoreCase ,
		boolean selected2 , String text2 , boolean all ,
		boolean inDir , String dir ) {
		page . ptFind . text2find = text ;
		Point p0 = all ? new Point ( 0 , 0 ) : new Point ( page . cx , page . cy ) ;
		if ( text != null && text . length ( ) > 0 ) {
			Point p = replace ( page , text , p0 . x , p0 . y , text2 , all , ignoreCase ) ;
			if ( p == null )
			page . ui . message ( "string not found" ) ;
			else {
				if ( ! all ) {
					page . cx = p . x ;
					page . cy = p . y ;
				}
				page . focusCursor ( ) ;
				page . ptSelection . cancelSelect ( ) ;
			}
		}
		page . uiComp . repaint ( ) ;
	}

	static void doReplaceAll ( PlainPage page , String text , boolean ignoreCase ,
		boolean selected2 , String text2 , boolean inDir ,
		String dir , String fnFilter ) throws Exception {
		if ( inDir ) {
			int ret = JOptionPane . showConfirmDialog ( page . uiComp , "Do you really want to do replace in files in dir?" ) ;
			if ( ret != JOptionPane . OK_OPTION ) return ;
			doReplaceInDir ( page , text , ignoreCase , text2 , inDir , dir , fnFilter ) ;
		}
		else
		doReplace ( page , text , ignoreCase , selected2 , text2 , true , inDir , dir ) ;
	}

	static void doReplaceInDir ( PlainPage page , String text , boolean ignoreCase2 ,
		String text2 , boolean inDir , String dir ,
		String fnFilter ) throws Exception {
		Iterable < File > it = new FileIterator ( dir ) ;
		List < String > all = new ArrayList < > ( ) ;
		fnFilter = fnFilter . trim ( ) . toLowerCase ( ) ;
		int [ ] cnts = new int [ 3 ] ;
		for ( File f : it ) {
			if ( f . isDirectory ( ) )
			continue ;
			if ( fnFilter . length ( ) > 0 ) {
				String fn = f . getName ( ) . toLowerCase ( ) ;
				if ( fn . indexOf ( fnFilter ) < 0 ) {
					cnts [ 2 ] ++ ;
					continue ;
				}
			}
			List < String > res = findInFilePlain ( f , text , page . ignoreCase , cnts ) ;
			if ( ! res . isEmpty ( ) ) {
				page . uiComp . findAndShowPage ( f . getAbsolutePath ( ) , -1 , true ) ;
				doReplaceAll ( page . uiComp . page , text , ignoreCase2 , false , text2 , false , null ,
					fnFilter ) ;
				all . addAll ( res ) ;
			}
		}
		showResult ( page , all , "dir" , dir , text , fnFilter , cnts , false ) ;
		page . uiComp . repaint ( ) ;
	}

	static Point find ( PlainPage page , String s , int x , int y , boolean ignoreCase ,
		boolean word ) {
		if ( y >= page . pageData . roLines . getLinesize ( ) )
		return null ;
		if ( ignoreCase )
		s = s . toLowerCase ( ) ;
		x = Math . min ( x , page . pageData . roLines . getline ( y ) . length ( ) ) ;
		// first half row
		int p1
		= indexOfSeq ( page . pageData . roLines . getline ( y ) , ignoreCase , s , x , word ) ;
		if ( p1 >= 0 )
		return new Point ( p1 , y ) ;
		// middle rows
		int fy = y ;
		for ( int i = 0 ; i < page . pageData . roLines . getLinesize ( ) - 1 ; i ++ ) {
			fy += 1 ;
			if ( fy >= page . pageData . roLines . getLinesize ( ) )
			fy = 0 ;
			p1 = indexOfSeq ( page . pageData . roLines . getline ( fy ) , ignoreCase , s , 0 , word ) ;
			if ( p1 >= 0 )
			return new Point ( p1 , fy ) ;
		}
		// last half row
		CharSequence sb = page . pageData . roLines . getline ( y ) ;
		p1 = indexOfSeq ( sb . subSequence ( 0 , x ) , ignoreCase , s , 0 , word ) ;
		if ( p1 >= 0 )
		return new Point ( p1 , fy ) ;
		return null ;
	}

	static Point find ( PlainPage page , List < String > ss , int x , int y ,
		boolean ignoreCase , boolean word ) {
		if ( ss == null || ss . size ( ) <= 0 )
		return null ;
		if ( y >= page . pageData . roLines . getLinesize ( ) )
		return null ;
		x = 0 ; // Math.min(x, page.pageData.roLines.getline(y).length());
		// first half row
		int p1 = indexOf ( page . pageData . roLines . getline ( y ) , ignoreCase , ss , x , word ) ;
		if ( p1 >= 0 ) return new Point ( p1 , y ) ;
		// middle rows
		int fy = y ;
		for ( int i = 0 ; i < page . pageData . roLines . getLinesize ( ) - 1 ; i ++ ) {
			fy += 1 ;
			if ( fy >= page . pageData . roLines . getLinesize ( ) )
			fy = 0 ;
			p1
			= indexOf ( page . pageData . roLines . getline ( fy ) , ignoreCase , ss , 0 , word ) ;
			if ( p1 >= 0 )
			return new Point ( p1 , fy ) ;
		}
		// last half row
		CharSequence sb = page . pageData . roLines . getline ( y ) ;
		p1 = indexOf ( sb . subSequence ( 0 , x ) , ignoreCase , ss , 0 , word ) ;
		if ( p1 >= 0 )
		return new Point ( p1 , fy ) ;
		return null ;
	}

	static List < String > findInFile ( File f , String text , boolean ignoreCase2 ,
		int [ ] cnts , boolean word ) {
		// System.out.println("find in "+f.getName());
		int MAX_SHOW_CHARS_IN_LINE = 30 ;
		List < String > a = new ArrayList < String > ( ) ;
		try {
			if ( guessIsBinFile ( f ) ) {
				if ( cnts != null )
				cnts [ 1 ] ++ ;
				return a ;
			}
			String enc = U . guessEncoding ( f . getAbsolutePath ( ) ,
				null /* not to search in gzip file */ ) ;
			if ( enc != null ) { // skip binary
				String fn = f . getAbsolutePath ( ) ;
				if ( ignoreCase2 )
				text = text . toLowerCase ( ) ;
				List ts = ( List ) PyData . parseAll ( "[" + text + "]" , false , true ) ;
				BufferedReader in = new BufferedReader (
					new InputStreamReader ( new FileInputStream ( f ) , enc ) ) ;
				String line ;
				int lineno = 0 ;
				while ( ( line = in . readLine ( ) ) != null ) {
					lineno ++ ;
					String oline = line ;
					int p1 = indexOf ( line , ignoreCase2 , ts , 0 , word ) ;
					if ( p1 >= 0 ) {
						if ( line . length ( ) > MAX_SHOW_CHARS_IN_LINE )
						line = line . substring ( 0 , MAX_SHOW_CHARS_IN_LINE ) + "..." ;
						a . add ( String . format ( "%s|%s:%s" , fn , lineno , oline ) ) ;
					}
				}
				in . close ( ) ;
				if ( cnts != null )
				cnts [ 0 ] ++ ;
			}
		} catch ( Exception e ) {
			e . printStackTrace ( ) ;
		}
		return a ;
	}

	static List < String > findInFilePlain ( File f , String text , boolean ignoreCase2 ,
		int [ ] cnts ) {
		// System.out.println("find in "+f.getName());
		int MAX_SHOW_CHARS_IN_LINE = 30 ;
		List < String > a = new ArrayList < String > ( ) ;
		try {
			if ( guessIsBinFile ( f ) ) {
				if ( cnts != null )
				cnts [ 1 ] ++ ;
				return a ;
			}
			String enc = U . guessEncoding ( f . getAbsolutePath ( ) ,
				null /* not to search in gzip file */ ) ;
			if ( enc != null ) { // skip binary
				String fn = f . getAbsolutePath ( ) ;
				if ( ignoreCase2 )
				text = text . toLowerCase ( ) ;
				BufferedReader in = new BufferedReader (
					new InputStreamReader ( new FileInputStream ( f ) , enc ) ) ;
				String line ;
				int lineno = 0 ;
				while ( ( line = in . readLine ( ) ) != null ) {
					lineno ++ ;
					String oline = line ;
					int p1 = indexOfSeq ( line , ignoreCase2 , text , 0 , false ) ;
					if ( p1 >= 0 ) {
						if ( line . length ( ) > MAX_SHOW_CHARS_IN_LINE )
						line = line . substring ( 0 , MAX_SHOW_CHARS_IN_LINE ) + "..." ;
						a . add ( String . format ( "%s|%s:%s" , fn , lineno , oline ) ) ;
					}
				}
				in . close ( ) ;
				if ( cnts != null )
				cnts [ 0 ] ++ ;
			}
		} catch ( Exception e ) {
			e . printStackTrace ( ) ;
		}
		return a ;
	}

	static Point find_prev ( PlainPage page , String s , int x , int y ,
		boolean ignoreCase , boolean word ) {
		if ( y >= page . pageData . roLines . getLinesize ( ) )
		return null ;
		if ( ignoreCase )
		s = s . toLowerCase ( ) ;
		if ( x < 0 ) {
			y -- ;
			if ( y < 0 )
			y = page . pageData . roLines . getLinesize ( ) - 1 ;
			if ( y < 0 )
			return null ;
			if ( y >= 0 )
			x = page . pageData . roLines . getline ( y ) . length ( ) ;
		}

		x = Math . min ( x , page . pageData . roLines . getline ( y ) . length ( ) ) ;
		// first half row
		int p1 = indexOfLast ( page . pageData . roLines . getline ( y ) , ignoreCase , s , x , word ) ;
		if ( p1 >= 0 )
		return new Point ( p1 , y ) ;
		// middle rows
		int fy = y ;
		for ( int i = 0 ; i < page . pageData . roLines . getLinesize ( ) - 1 ; i ++ ) {
			fy -= 1 ;
			if ( fy < 0 )
			fy = page . pageData . roLines . getLinesize ( ) - 1 ;
			CharSequence line = page . pageData . roLines . getline ( fy ) ;
			p1 = indexOfLast ( line , ignoreCase , s , line . length ( ) , word ) ;
			if ( p1 >= 0 )
			return new Point ( p1 , fy ) ;
		} {
			// last half row
			CharSequence sb = page . pageData . roLines . getline ( y ) ;
			if ( x >= sb . length ( ) )
			return null ;
			CharSequence tail = sb . subSequence ( x , sb . length ( ) ) ;
			p1 = indexOfLast ( tail , ignoreCase , s , tail . length ( ) , word ) ;
			if ( p1 >= 0 )
			return new Point ( p1 + x , fy ) ;
		}
		return null ;
	}

	private static boolean guessIsBinFile ( File f ) {
		// first, encoding guessed is null
		String name = f . getName ( ) ;
		if ( _endsWithAny ( binExt , name ) )
		return true ;
		long size = f . length ( ) ;
		if ( size >= 30 * 1000 * 1000 )
		return true ;
		return false ;
	}

	private static int indexOf ( CharSequence t , boolean ignoreCase ,
		List < String > ss , int x , boolean word ) {
		if ( ignoreCase ) t = t . toString ( ) . toLowerCase ( ) ;
		int p = 0 ;
		for ( int i = 0 ; i < ss . size ( ) ; i ++ ) {
			p = FindAndReplace . indexOfSeq ( t , ss . get ( i ) , x , word ) ;
			if ( p < 0 ) return p ;
		}
		return p ;
	}

	public static int indexOfLast ( CharSequence input , String kw , int start , boolean word ) {
		String target = input . toString ( ) ;
		int fromIndex = start ;
		while ( true ) { //if one fail , search another		
			int p1 = target . lastIndexOf ( kw , fromIndex ) ;
			if ( p1 < 0 ) return p1 ;
			if ( isWordMatch ( target , kw , p1 , word ) >= 0 ) return p1 ;
			fromIndex = p1 -1 ;
			if ( fromIndex < 0 ) return -1 ;
		}
	}

	private static int indexOfLast ( CharSequence t , boolean ignoreCase , String s , int x , boolean word ) {
		if ( ! ignoreCase ) return indexOfLast ( t , s , x , word ) ;
		else return indexOfLast ( t . toString ( ) . toLowerCase ( ) , s , x , word ) ;
	}

	private static int indexOfSeq ( CharSequence t , boolean ignoreCase , String s , int x , boolean word ) {
		if ( ! ignoreCase ) return FindAndReplace . indexOfSeq ( t , s , x , word ) ;
		else {
			String t2 = t . toString ( ) . toLowerCase ( ) ;
			return FindAndReplace . indexOfSeq ( t2 , s , x , word ) ;
		}
	}

	public static int indexOfSeq ( CharSequence input , char kw ) {
		if ( input instanceof String ) {
			String text = ( String ) input ;
			return text . indexOf ( kw ) ;
		}
		if ( input instanceof StringBuilder ) {
			StringBuilder text = ( StringBuilder ) input ;
			return text . indexOf ( "" + kw ) ;
		}
		System . out . println ( "indexOf char for rare type=" + input . getClass ( ) ) ;
		return input . toString ( ) . indexOf ( kw ) ;
	}

	public static int indexOfSeq ( CharSequence input , String kw , int start , boolean word ) {
		String target = input . toString ( ) ;
		int fromIndex = start ;
		while ( true ) {
			int p1 = target . indexOf ( kw , fromIndex ) ;
			if ( p1 < 0 ) return p1 ;
			if ( isWordMatch ( target , kw , p1 , word ) >= 0 ) return p1 ;
			fromIndex = p1 + kw . length ( ) ;
			if ( fromIndex >= target . length ( ) ) return -1 ;
		}
	}

	private static boolean isIdChar ( char c ) {
		return c == '_' || c == '$' || Character . isAlphabetic ( c )
		|| Character . isDigit ( c ) ;
	}
	private static int isWordMatch ( CharSequence t , String s , int p , boolean word ) {
		if ( ! word || p <= 0 ) return p ;
		if ( isIdChar ( t . charAt ( p - 1 ) ) ) return -1 ;
		int q = p + s . length ( ) ;
		if ( q < t . length ( ) && isIdChar ( t . charAt ( q ) ) ) return -1 ;
		return p ;
	}

	static Point replace ( PlainPage page , String s , int x , int y , String s2 ,
		boolean all , boolean ignoreCase ) {
		int cnt = 0 ;
		BasicEdit editRec = page . pageData . editRec ;
		if ( ignoreCase )
		s = s . toLowerCase ( ) ;
		// first half row
		int p1 = x ;
		while ( true ) {
			p1 = indexOfSeq ( page . pageData . roLines . getline ( y ) , ignoreCase , s , p1 , false ) ;
			if ( p1 >= 0 ) {
				cnt ++ ;
				editRec . deleteInLine ( y , p1 , p1 + s . length ( ) ) ;
				editRec . insertInLine ( y , p1 , s2 ) ;
				if ( ! all )
				return new Point ( p1 + s2 . length ( ) , y ) ;
				p1 = p1 + s2 . length ( ) ;
			} else
			break ;
		}
		// middle rows
		int fy = y ;
		for ( int i = 0 ; i < page . pageData . roLines . getLinesize ( ) - 1 ; i ++ ) {
			fy += 1 ;
			if ( fy >= page . pageData . roLines . getLinesize ( ) )
			fy = 0 ;
			p1 = 0 ;
			while ( true ) {
				p1 = indexOfSeq ( page . pageData . roLines . getline ( fy ) , ignoreCase , s , p1 ,
					false ) ;
				if ( p1 >= 0 ) {
					cnt ++ ;
					editRec . deleteInLine ( fy , p1 , p1 + s . length ( ) ) ;
					editRec . insertInLine ( fy , p1 , s2 ) ;
					if ( ! all )
					return new Point ( p1 + s2 . length ( ) , fy ) ;
					p1 = p1 + s2 . length ( ) ;
				} else
				break ;
			}
		}
		// last half row
		fy += 1 ;
		if ( fy >= page . pageData . roLines . getLinesize ( ) )
		fy = 0 ;
		p1 = 0 ;
		CharSequence sb = page . pageData . roLines . getline ( fy ) ;
		while ( true ) {
			p1 = indexOfSeq ( sb . subSequence ( 0 , x ) , ignoreCase , s , p1 , false ) ;
			if ( p1 >= 0 ) {
				cnt ++ ;
				editRec . deleteInLine ( fy , p1 , p1 + s . length ( ) ) ;
				editRec . insertInLine ( fy , p1 , s2 ) ;
				if ( ! all )
				return new Point ( p1 + s2 . length ( ) , fy ) ;
				p1 = p1 + s2 . length ( ) ;
			} else
			break ;
		}
		if ( cnt > 0 ) {
			page . ui . message ( "replaced " + cnt + " places" ) ;
			return new Point ( x , y ) ;
		} else
		return null ;
	}

	static void showResult ( PlainPage pp , List < String > all , String type ,
		String name , String text , String fnFilter , int [ ] cnts ,
		boolean word ) throws Exception {
		String withFilter = "" ;
		if ( fnFilter != null && fnFilter . length ( ) > 0 )
		withFilter = String . format ( " with filter '" + fnFilter + "'" ) ;
		String cntInfo = "" ;
		if ( cnts != null ) {
			cntInfo = String . format ( ",searched %d files" , cnts [ 0 ] ) ;
			if ( cnts [ 1 ] != 0 )
			cntInfo += String . format ( ", skipped binary:%d" , cnts [ 1 ] ) ;
			if ( cnts [ 2 ] != 0 )
			cntInfo += String . format ( ", filtered:%d" , cnts [ 2 ] ) ;
		}
		if ( word )
		cntInfo += " in word mode" ;
		PlainPage p2 = new PlainPage ( pp . uiComp , PageData . fromTitle ( String . format (
					"[find](%s)'%s' in %s '%s'%s %s #%s" , all . size ( ) , text , type ,
					name , withFilter , cntInfo , U . randomID ( ) ) ) , pp ) ;
		List < CharSequence > sbs = new ArrayList < > ( ) ;
		sbs . add ( new StringBuilder (
				String . format ( "find %s results in '%s'%s for '%s' %s" , all . size ( ) , name ,
					withFilter , text , cntInfo ) ) ) ;
		for ( Object o : all )
		sbs . add ( o . toString ( ) ) ;
		p2 . pageData . resetLines ( sbs ) ;
		p2 . pageData . searchResultOf = name ;
		//		gc();
	}

	boolean back ;
	FindReplaceWindow findWindow ;
	final private PlainPage pp ;
	String text2find ;
	boolean word ;

	public FindAndReplace ( PlainPage plainPage ) {
		this . pp = plainPage ;
	}

	void doFind ( String text , boolean ignoreCase , boolean selected2 ,
		boolean inDir , String dir , String fnFilter , boolean backward ,
		boolean word ) throws Exception {
		text2find = text ;
		this . word = word ;
		if ( ! inDir ) {
			pp . ignoreCase = ignoreCase ;
			back = backward ;
			if ( backward )
			findPrev ( word ) ;
			else
			findNext ( word ) ;
			pp . uiComp . repaint ( ) ;
		} else
		doFindInDir ( pp , text , ignoreCase , selected2 , inDir , dir , fnFilter , word ) ;
	}

	void findNext ( boolean word ) {
		if ( text2find != null && text2find . length ( ) > 0 ) {
			Point p = find ( pp , text2find , pp . cx + 1 , pp . cy , pp . ignoreCase , word ) ;
			if ( p == null )
			pp . ui . message ( "string not found" ) ;
			else
			pp . ptSelection . selectLength ( p . x , p . y , text2find . length ( ) ) ;
		}
	}

	void findPrev ( boolean word ) {
		if ( text2find != null && text2find . length ( ) > 0 ) {
			Point p
			= find_prev ( pp , text2find , pp . cx - 1 , pp . cy , pp . ignoreCase , word ) ;
			if ( p == null )
			pp . ui . message ( "string not found" ) ;
			else
			pp . ptSelection . selectLength ( p . x , p . y , text2find . length ( ) ) ;
		}
	}

	private Window findWindow ( Container c ) {
		int safe = 100 ;
		while ( true ) {
			if ( c == null )
			return null ;
			if ( c instanceof Window )
			return ( Window ) c ;
			c = c . getParent ( ) ;
			if ( -- safe <= 0 )
			return null ;
		}
	}

	void showFindDialog ( ) {
		List < CharSequence > ss = pp . ptSelection . getSelected ( ) ;

		String t = ss . isEmpty ( ) ? "" : ss . get ( 0 ) . toString ( ) ;

		if ( t . length ( ) == 0 && text2find != null )
		t = text2find ;
		if ( findWindow == null ) {
			Window f0 = null ;
			if ( pp . uiComp . frame instanceof JFrame )
			f0 = ( JFrame ) pp . uiComp . frame ;
			else
			f0 = findWindow ( pp . uiComp . frame . getContentPane ( ) ) ;
			findWindow = new FindReplaceWindow ( f0 , pp ) ;
		}
		if ( t . length ( ) > 0 )
		findWindow . jta1 . setText ( t ) ;
		findWindow . show ( ) ;
		findWindow . jta1 . grabFocus ( ) ;
	}
}
