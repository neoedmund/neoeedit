/*
 *  
 */
package neoe . ne ;

import java . awt . Rectangle ;
import java . util . ArrayList ;
import java . util . List ;

/**
 *
 * @author neoe
 */
public class ReadonlyLines {
	PageData data ;

	ReadonlyLines ( PageData data ) {
		this . data = data ;
	}

	CharSequence getInLine ( int y , int x1 , int x2 ) {
		CharSequence cs = getline ( y ) ;
		if ( x1 < 0 )
		x1 = 0 ;
		if ( x2 > cs . length ( ) )
		x2 = cs . length ( ) ;
		return getline ( y ) . subSequence ( x1 , x2 ) ;
	}

	CharSequence getline ( int i ) {
		if ( i < 0 || i >= data . lines . size ( ) )
		return "" ;
		return ( CharSequence ) data . lines . get ( i ) ;
	}

	int getLinesize ( ) {
		return data . lines . size ( ) ;
	}

	List < CharSequence > getTextInRect ( Rectangle r , boolean rectSelectMode ) {
		int x1 = r . x ;
		int y1 = r . y ;
		int x2 = r . width ;
		int y2 = r . height ;
		List < CharSequence > sb = new ArrayList < CharSequence > ( ) ;
		if ( rectSelectMode )
		for ( int i = y1 ; i <= y2 ; i ++ )
		sb . add ( getInLine ( i , x1 , x2 ) ) ;
		else if ( y1 == y2 && x1 < x2 )
		sb . add ( getInLine ( y1 , x1 , x2 ) ) ;
		else if ( y1 < y2 ) {
			sb . add ( getInLine ( y1 , x1 , Integer . MAX_VALUE ) ) ;
			// for (int i = y1 + 1; i < y2; i++) {
			// sb.add(getline(i));
			// }
			sb . addAll ( data . lines . subList ( y1 + 1 , y2 ) ) ;
			sb . add ( getInLine ( y2 , 0 , x2 ) ) ;
		}
		return sb ;
	}
}
