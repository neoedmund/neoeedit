package neoe . ne ;

import java . io . File ;
import java . io . IOException ;
import java . util . ArrayList ;
import java . util . HashMap ;
import java . util . List ;
import java . util . Map ;
import static neoe . ne . U . saveFileHistory ;

/**
 * Text data stores here.
 */
public class PageData {
	/**
	 * whole JVM, regardless how many window opened, data get shared
	 */
	static Map < String , PageData > dataPool = new HashMap < > ( ) ;

	public static PageData newUntitled ( ) {
		return fromTitle ( "[Untitled]#" + U . randomID ( ) ) ;
	}

	public static PageData fromTitle ( String title ) {
		PageData pd = dataPool . get ( title ) ;
		if ( pd != null )
		return pd ;
		pd = new PageData ( ) ;
		pd . title = title ;
		pd . lines = new ArrayList < > ( ) ;
		pd . lines . add ( U . EMPTY ) ;
		dataPool . put ( title , pd ) ;
		return pd ;
	}

	public static PageData fromFile ( String fn ) throws IOException {
		PageData pd = dataPool . get ( fn ) ;
		if ( pd != null )
		return pd ;
		pd = new PageData ( ) ;
		readFile ( pd , fn ) ;
		dataPool . put ( fn , pd ) ;
		saveFileHistory ( fn , 0 ) ;
		return pd ;
	}
	public boolean changedOutside ;

	BasicEdit editNoRec = new BasicEdit ( false , this ) ;
	BasicEdit editRec = new BasicEdit ( true , this ) ;

	String encoding ;

	long fileLastModified ;

	boolean fileLoaded ;
	History history ;
	boolean isCommentChecked ;
	String [ ] comment = null ;
	/* element: String or StringBuilder(after edit) */
	public List < CharSequence > lines ;
	public String lineSep = "\n" ;

	public int ref ;
	ReadonlyLines roLines = new ReadonlyLines ( this ) ;
	public String title ;

	public boolean gzip ;

	public byte [ ] bs ;

	private PageData ( ) {
		history = new History ( this ) ;
	}

	public void close ( ) {
		dataPool . remove ( title ) ;
		lines . clear ( ) ;
		lines = null ;
	}

	private static void readFile ( PageData data , String fn ) {
		File f = new File ( fn ) ;
		if ( fn . endsWith ( ".gz" ) )
		data . gzip = U . tryGzip ( fn , data ) ;
		data . isCommentChecked = false ;
		if ( data . encoding == null )
		data . encoding = U . guessEncodingForEditor ( fn , data ) ;
		data . lineSep = U . guessLineSepForEditor ( fn , data ) ;
		data . lines = null ;
		data . history . clear ( ) ;
		data . resetLines ( U . readFileForEditor ( fn , data . encoding , data ) ) ;

		data . fileLastModified = f . lastModified ( ) ;
		data . changedOutside = false ;
		data . title = fn ;
		data . fileLoaded = true ;
	}

	public void reloadFile ( ) {
		if ( fileLoaded )
		readFile ( this , title ) ;
	}

	void renameTo ( String fn ) {
		PageData . dataPool . remove ( title ) ;
		title = fn ;
		PageData . dataPool . put ( fn , this ) ;
	}

	public void resetLines ( List < CharSequence > newLines ) {
		lines = newLines ;
		history . clear ( ) ;
	}

	public void setText ( String s ) {
		List < CharSequence > ss = U . removeTailR ( U . split ( s , U . N ) ) ;
		if ( ss . isEmpty ( ) )
		ss . add ( U . EMPTY ) ;
		resetLines ( ss ) ;
	}
}
