package neoe . ne . obsolete ;

import java . util . Vector ;
import neoe . ne . EditorPanel ;
import neoe . ne . PlainPage ;
import neoe . ne . U ;

public class CursorHistory {
	CursorHistoryData data =
	new CursorHistoryData ( new Vector < CursorHistoryItem > ( ) ) ;
	PlainPage page ;
	EditorPanel ep ;

	void back ( String curTitle , int curX , int curY ) throws Exception {
		if ( data . p >= 0 && data . items . size ( ) > 0 ) {
			if ( data . p >= data . items . size ( ) )
			data . p = data . items . size ( ) - 1 ; // bug auto fix
			CursorHistoryItem item = data . items . get ( data . p -- ) ;
			if ( item . pageName . equals ( curTitle ) && item . y == curY && item . x == curX ) {
				back ( curTitle , curX , curY ) ;
				return ;
			}
			if ( move ( item ) )
			page . ui . message ( "moved back" ) ;
			else
			page . ui . message ( "position not exists, try again" ) ;
		} else {
			page . ui . message ( "no more history" ) ;
		}
	}

	void forward ( ) throws Exception {
		if ( ! data . items . isEmpty ( ) && data . p < data . items . size ( ) - 1 ) {
			data . p ++ ;
			CursorHistoryItem item = data . items . get ( data . p ) ;
			if ( move ( item ) )
			page . ui . message ( "moved forward" ) ;
			else
			page . ui . message ( "position not exists, try again" ) ;
		} else {
			page . ui . message ( "no more history" ) ;
		}
	}

	private CursorHistoryItem getCurrentItem ( PlainPage page ) {
		return new CursorHistoryItem ( page . pageData . getTitle ( ) , page . cx , page . cy ) ;
	}

	private CursorHistoryItem getLastItem ( ) {
		if ( data . items . size ( ) > 0 && data . p > 0 && data . p <= data . items . size ( ) )
		return data . items . get ( data . p - 1 ) ;
		return null ;
	}

	private boolean isSameLine ( CursorHistoryItem last , CursorHistoryItem item ) {
		return ( last != null && last . pageName . equals ( item . pageName ) &&
			last . y == item . y ) ;
	}

	private boolean move ( CursorHistoryItem item ) throws Exception {
		return U . gotoFileLinePos ( ep , item . pageName , item . y + 1 , item . x , false ) ;
	}

	void record ( CursorHistoryItem item ) {
		CursorHistoryItem last = getLastItem ( ) ;
		if ( isSameLine ( last , item ) )
		return ; // same line, skip
		if ( data . p < 0 )
		data . p = 0 ; // bug auto fix
		if ( data . p < data . items . size ( ) )
		data . items . setSize ( data . p ) ;
		data . items . add ( item ) ;
		data . p ++ ;
	}

	void record ( String pageName , int x , int y ) {
		record ( new CursorHistoryItem ( pageName , x , y ) ) ;
	}

	public void recordCurrent ( PlainPage page ) { record ( getCurrentItem ( page ) ) ;
	}

	public void recordInput ( PlainPage page ) {
		CursorHistoryItem last = getLastItem ( ) ;
		CursorHistoryItem item = getCurrentItem ( page ) ;
		if ( isSameLine ( last , item ) ) {
			return ;
		}
		record ( item ) ;
	}

	static class CursorHistoryData {
		public Vector < CursorHistoryItem > items ;
		public int p ;

		public CursorHistoryData ( Vector < CursorHistoryItem > items ) {
			this . items = items ;
		}
	}

	static class CursorHistoryItem {
		String pageName ;
		int x , y ;

		public CursorHistoryItem ( String pageName , int x , int y ) {
			this . pageName = pageName ;
			this . x = x ;
			this . y = y ;
		}
	}
}