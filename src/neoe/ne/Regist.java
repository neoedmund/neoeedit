package neoe . ne ;

public class Regist {
	public static void main ( String [ ] args ) throws Exception {
		regist ( args [ 0 ] , args [ 1 ] ) ;
	}

	private static void regist ( String cls , String staticField ) throws Exception {
		ClassLoader cl = Regist . class . getClassLoader ( ) ;
		cl . loadClass ( cls ) . getField ( staticField ) . set ( null , Main . class ) ;
	}
}
