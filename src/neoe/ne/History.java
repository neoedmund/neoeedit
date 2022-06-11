/*
 *  
 */
package neoe . ne ;

import java . util . ArrayList ;
import java . util . LinkedList ;
import java . util . List ;

/**
 *
 * @author neoe
 */
public class History {
	public final static int MAXSIZE = 200 ;
	List < HistoryCell > atom ;
	LinkedList < List < HistoryCell >> data ;
	private boolean inAtom ;
	int p ;
	//		PageData pageData;
	public History ( PageData pageData ) {
		data = new LinkedList < > ( ) ;
		p = 0 ;
		atom = new ArrayList < > ( ) ;
		//			this.pageData = pageData;
	}

	void add ( List < HistoryCell > o ) {
		if ( p < data . size ( ) && p >= 0 )
		for ( int i = 0 ; i < data . size ( ) - p ; i ++ )
		data . removeLast ( ) ;
		List < HistoryCell > last = data . peekLast ( ) ;
		// stem.out.println("last=" + last);
		if ( ! append ( last , o ) ) {
			// System.out.println("add:" + o);
			data . add ( o ) ;
			if ( data . size ( ) > MAXSIZE )
			data . removeFirst ( ) ;
			else
			p += 1 ;
		} else {
			// System.out.println("append:" + o);
		}
	}

	public void addOne ( HistoryCell historyInfo ) {
		atom . add ( historyInfo ) ;
	}

	/**
	 * try to append this change to the last ones
	 */
	boolean append ( List < HistoryCell > lasts , List < HistoryCell > os ) {
		if ( lasts == null )
		return false ;
		boolean ret = false ;
		if ( os . size ( ) == 1 ) {
			HistoryCell o = os . get ( 0 ) ;
			HistoryCell last = lasts . get ( lasts . size ( ) - 1 ) ;
			if ( o . canAppend ( last ) ) {
				lasts . add ( o ) ;
				ret = true ;
			}
		}
		return ret ;
	}

	public void beginAtom ( ) {
		if ( inAtom ) {
			System . err . println ( "bug:double beginAtom" ) ;
			new Exception ( "debug" ) . printStackTrace ( ) ;
		}
		inAtom = true ;
		if ( ! atom . isEmpty ( ) )
		endAtom ( ) ;
	}

	public void clear ( ) {
		atom . clear ( ) ;
		data . clear ( ) ;
		p = 0 ;
	}

	public void endAtom ( ) {
		if ( ! atom . isEmpty ( ) ) {
			// System.out.println("end atom");
			add ( atom ) ;
			atom = new ArrayList < > ( ) ;
		}
		inAtom = false ;
	}

	public List < HistoryCell > get ( ) {
		if ( p <= 0 )
		return null ;
		p -= 1 ;
		// System.out.println("undo:" + data.get(p));
		return data . get ( p ) ;
	}

	public List < HistoryCell > getRedo ( ) {
		if ( p < data . size ( ) ) {
			p += 1 ;
			return data . get ( p - 1 ) ;
		} else
		return null ;
	}

	void redo ( PlainPage page ) throws Exception {
		List < HistoryCell > os = getRedo ( ) ;
		if ( os == null )
		return ;
		for ( HistoryCell o : os )
		o . redo ( page ) ;
	}

	public int size ( ) {
		return p ;
	}

	void undo ( PlainPage page ) throws Exception {
		List < HistoryCell > os = get ( ) ;
		if ( os == null )
		return ;
		for ( int i = os . size ( ) - 1 ; i >= 0 ; i -- ) {
			HistoryCell o = os . get ( i ) ;
			o . undo ( page ) ;
		}
	}
}
