package neoe . ne . util ;

public class Finder {
	int i ;
	private String page ;

	public Finder ( String page ) {
		this . page = page ;
		i = 0 ;
	}

	public void find ( String s ) {
		int p1 = page . indexOf ( s , i ) ;
		if ( p1 < 0 )
		i = page . length ( ) ;
		else
		i = p1 + s . length ( ) ;
	}

	public String readUntil ( String s ) {
		int p1 = page . indexOf ( s , i ) ;
		if ( p1 < 0 ) {
			i = page . length ( ) ;
			return "" ;
		} else {
			String r = page . substring ( i , p1 ) ;
			i = p1 + s . length ( ) ;
			return r ;
		}
	}

	public boolean finished ( ) {
		return i >= page . length ( ) ;
	}

	public void reset ( ) {
		i = 0 ;
	}
}
