/*
 *  
 */
package neoe . ne ;

import java . util . List ;

/**
 *
 * @author neoe
 */
public class HistoryCell {
	BasicAction action ;
	Object s1 ;
	int x1 , x2 , y1 , y2 ;

	public HistoryCell ( BasicAction action , int x1 , int x2 , int y1 , int y2 ,
		Object s1 ) {
		super ( ) ;
		this . s1 = s1 ;
		this . x1 = x1 ;
		this . x2 = x2 ;
		this . y1 = y1 ;
		this . y2 = y2 ;
		this . action = action ;
	}

	public boolean canAppend ( HistoryCell last ) {
		return ( ( last . action == BasicAction . Delete
				&& this . action == BasicAction . Delete
				&& //
				( ( last . x1 == this . x1 || last . x1 == this . x2 )
					&& last . y1 == this . y1 ) ) //
			|| ( last . action == BasicAction . Insert
				&& this . action == BasicAction . Insert
				&& //
				( ( last . x1 == this . x1 || last . x2 == this . x1 )
					&& last . y1 == this . y1 ) ) ) ;
	}

	public void redo ( PlainPage page ) {
		BasicEdit editNoRec = page . pageData . editNoRec ;
		ReadonlyLines roLines = page . pageData . roLines ;
		switch ( action ) {
			case Delete :
			s1 = roLines . getInLine ( y1 , x1 , x2 ) . toString ( ) ;
			editNoRec . deleteInLine ( y1 , x1 , x2 ) ;
			page . cursor . setSafePos ( x1 , y1 ) ;
			break ;
			case DeleteLines :
			editNoRec . deleteLines ( x1 , x2 ) ;
			page . cursor . setSafePos ( 0 , x1 ) ;
			break ;
			case Insert : {
				String s1 = ( String ) this . s1 ;
				editNoRec . insertInLine ( y1 , x1 , s1 ) ;
				page . cursor . setSafePos ( x1 + s1 . length ( ) , y1 ) ;
				s1 = null ;
				break ;
			}
			case InsertEmptyLine :
			editNoRec . insertEmptyLine ( y1 ) ;
			page . cursor . setSafePos ( 0 , y1 + 1 ) ;
			break ;
			case MergeLine :
			editNoRec . mergeLine ( y1 ) ;
			page . cursor . setSafePos ( x1 , y1 ) ;
			break ;
			default :
			throw new RuntimeException ( "unkown action " + action ) ;
		}
	}

	@ Override
	public String toString ( ) {
		return "HistoryInfo [action=" + action + ", x1=" + x1 + ", x2=" + x2
		+ ", y1=" + y1 + ", y2=" + y2 + ", s1=" + s1 + "]\n" ;
	}

	public void undo ( PlainPage page ) {
		BasicEdit editNoRec = page . pageData . editNoRec ;
		ReadonlyLines roLines = page . pageData . roLines ;
		switch ( action ) {
			case Delete : {
				String s1 = ( String ) this . s1 ;
				editNoRec . insertInLine ( y1 , x1 , s1 ) ;
				page . cursor . setSafePos ( x1 + s1 . length ( ) , y1 ) ;
				s1 = null ;
				break ;
			}
			case DeleteLines :
			editNoRec . insertLines ( x1 , ( List < CharSequence > ) s1 ) ;
			page . cursor . setSafePos ( 0 , x2 ) ;
			break ;
			case Insert :
			s1 = roLines . getInLine ( y1 , x1 , x2 ) . toString ( ) ;
			editNoRec . deleteInLine ( y1 , x1 , x2 ) ;
			page . cursor . setSafePos ( 0 , y1 ) ;
			break ;
			case InsertEmptyLine :
			editNoRec . deleteLines ( y1 , y1 + 1 ) ;
			page . cursor . setSafePos ( 0 , y1 ) ;
			break ;
			case MergeLine :
			String s2 = roLines . getInLine ( y1 , x1 , Integer . MAX_VALUE ) . toString ( ) ;
			editNoRec . deleteInLine ( y1 , x1 , Integer . MAX_VALUE ) ;
			editNoRec . insertEmptyLine ( y1 + 1 ) ;
			editNoRec . insertInLine ( y1 + 1 , 0 , s2 ) ;
			page . cursor . setSafePos ( 0 , y1 + 1 ) ;
			break ;
			default :
			throw new RuntimeException ( "unkown action " + action ) ;
		}
	}
}
