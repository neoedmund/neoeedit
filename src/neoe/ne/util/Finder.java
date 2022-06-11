package neoe . ne . util ;

public class Finder {
	public int i ;
	public String text ;

	public Finder ( String page ) {
		this . text = page ;
		i = 0 ;
	}

	public void find ( String s ) {
		int p1 = text . indexOf ( s , i ) ;
		if ( p1 < 0 )
		i = text . length ( ) ;
		else
		i = p1 + s . length ( ) ;
	}

	public void findReverse ( String s ) {
		int p1 = text . substring ( 0 , i ) . lastIndexOf ( s ) ;
		if ( p1 < 0 )
		i = text . length ( ) ;
		else
		i = p1 + s . length ( ) ;
	}

	public String readUntil ( String s ) {
		int p1 = text . indexOf ( s , i ) ;
		if ( p1 < 0 ) {
			return readRemain ( ) ;
		} else {
			String r = text . substring ( i , p1 ) ;
			i = p1 + s . length ( ) ;
			return r ;
		}
	}

	public boolean finished ( ) { return i >= text . length ( ) ;
	}

	public void reset ( ) { i = 0 ;
	}

	public void setPos ( int pos ) { i = pos ;
	}

	public int getPos ( ) { return i ;
	}

	public String readRemain ( ) {
		String r = text . substring ( i ) ;
		i = text . length ( ) ;
		return r ;
	}
}