/*
 *  
 */
package neoe . ne ;

import java . util . ArrayList ;
import java . util . List ;

/**
 *
 * @author neoe
 */
enum BasicAction {
	Delete ,
	DeleteLines ,
	Insert ,
	MergeLine ,
	InsertEmptyLine
}

public class BasicEdit {
	PageData pd ;
	boolean record ;

	BasicEdit ( boolean record , PageData data ) {
		this . record = record ;
		this . pd = data ;
	}

	public void insertEmptyLine ( int y ) {
		if ( y > lines ( ) . size ( ) )
		y = lines ( ) . size ( ) ;
		if ( y < 0 ) y = 0 ;
		lines ( ) . add ( y , new StringBuilder ( ) ) ;
		if ( record )
		history ( ) . addOne (
			new HistoryCell ( BasicAction . InsertEmptyLine , -1 , -1 , y , -1 , null ) ) ;
	}

	public void deleteLines ( int start , int end ) {
		int len = end - start ;
		if ( len <= 0 )
		return ;
		if ( lines ( ) . isEmpty ( ) )
		return ;
		List < CharSequence > deleted = lines ( ) . subList ( start , end ) ;
		if ( record )
		history ( ) . addOne ( new HistoryCell ( BasicAction . DeleteLines , start , end , 0 ,
				0 , new ArrayList ( deleted ) ) ) ;
		deleted . clear ( ) ;
	}

	StringBuilder getLineSb ( int y ) {
		if ( lines ( ) . size ( ) == 0 )
		insertEmptyLine ( y ) ;
		if ( y < 0 || y >= lines ( ) . size ( ) )
		return null ;
		CharSequence o = lines ( ) . get ( y ) ;

		if ( o instanceof StringBuilder )
		return ( StringBuilder ) o ;

		if ( o instanceof String ) {
			String str = ( String ) o ;
			StringBuilder sb = new StringBuilder ( str ) ;
			lines ( ) . set ( y , sb ) ;
			return sb ;
		}

		StringBuilder sb = new StringBuilder ( ( CharSequence ) o ) ;
		lines ( ) . set ( y , sb ) ;
		return sb ;
	}

	void deleteInLine ( int y , int x1 , int x2 ) {
		StringBuilder sb = getLineSb ( y ) ;
		if ( sb == null )
		return ;
		if ( x1 >= sb . length ( ) )
		return ;
		x2 = Math . min ( x2 , sb . length ( ) ) ;
		String d = sb . substring ( x1 , x2 ) ;
		if ( d . length ( ) > 0 ) {
			sb . delete ( x1 , x2 ) ;
			if ( record )
			history ( ) . addOne (
				new HistoryCell ( BasicAction . Delete , x1 , x2 , y , -1 , d ) ) ;
		}
	}

	History history ( ) {
		return pd . history ;
	}

	void insertInLine ( int y , int x , CharSequence s ) {
		if ( FindAndReplace . indexOfSeq ( s , '\n' ) >= 0 || FindAndReplace . indexOfSeq ( s , '\r' ) >= 0 )
		throw new RuntimeException ( "cannot contains line-seperator:[" + s
			+ "]" + FindAndReplace . indexOfSeq ( s , '\n' ) ) ;
		if ( y == pd . roLines . getLinesize ( ) )
		pd . editRec . insertEmptyLine ( y ) ;
		StringBuilder sb = getLineSb ( y ) ;
		if ( x > sb . length ( ) )
		sb . setLength ( x ) ;
		sb . insert ( x , s ) ;
		if ( record )
		history ( ) . addOne ( new HistoryCell ( BasicAction . Insert , x , x + s . length ( ) ,
				y , -1 , null ) ) ;
	}

	List < CharSequence > lines ( ) {
		return pd . lines ;
	}

	void mergeLine ( int y ) {
		StringBuilder sb1 = getLineSb ( y ) ;
		CharSequence sb2 = lines ( ) . get ( y + 1 ) ;
		int x1 = sb1 . length ( ) ;
		sb1 . append ( sb2 ) ;
		lines ( ) . remove ( y + 1 ) ;
		if ( record )
		history ( ) . addOne (
			new HistoryCell ( BasicAction . MergeLine , x1 , -1 , y , -1 , null ) ) ;
	}

	void insertLines ( int x1 , List < CharSequence > s1 ) {
		lines ( ) . addAll ( x1 , s1 ) ;
	}

	void appendLines ( List < CharSequence > s1 ) {
		lines ( ) . addAll ( s1 ) ;
	}

	void appendLine ( CharSequence s1 ) {
		lines ( ) . add ( s1 ) ;
	}
}
