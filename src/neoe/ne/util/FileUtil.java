package neoe . ne . util ;

import java . io . BufferedOutputStream ;
import java . io . BufferedReader ;
import java . io . ByteArrayInputStream ;
import java . io . ByteArrayOutputStream ;
import java . io . EOFException ;
import java . io . File ;
import java . io . FileInputStream ;
import java . io . FileNotFoundException ;
import java . io . FileOutputStream ;
import java . io . IOException ;
import java . io . InputStream ;
import java . io . InputStreamReader ;
import java . io . OutputStream ;
import java . io . UnsupportedEncodingException ;
import java . nio . file . Files ;
import java . util . ArrayList ;
import java . util . Iterator ;
import java . util . List ;

public class FileUtil {
	private static final String UTF8 = "UTF8" ;

	public static void copy ( File from , File to ) throws IOException {
		FileInputStream in = new FileInputStream ( from ) ;
		FileOutputStream out = new FileOutputStream ( to ) ;
		copy ( in , out ) ;
		in . close ( ) ;
		out . close ( ) ;
	}

	public static void copy ( InputStream in , OutputStream outstream )
	throws IOException {
		BufferedOutputStream out = new BufferedOutputStream ( outstream ) ;
		byte [ ] buf = new byte [ 1024 * 16 ] ;
		int len ;
		while ( ( len = in . read ( buf ) ) > 0 )
		out . write ( buf , 0 , len ) ;
		in . close ( ) ;
		out . close ( ) ;
	}

	public static void copy2 ( InputStream in , OutputStream outstream )
	throws IOException {
		BufferedOutputStream out = new BufferedOutputStream ( outstream ) ;
		byte [ ] buf = new byte [ 1024 * 16 ] ;
		int len ;
		while ( ( len = in . read ( buf ) ) > 0 )
		out . write ( buf , 0 , len ) ;
		out . flush ( ) ;
	}

	public static BufferedReader getBufferedReader ( String fn , String enc )
	throws IOException {
		InputStream in = getFileInputStream ( fn ) ;
		return new BufferedReader ( new InputStreamReader ( in , enc ) ) ;
	}

	public static InputStream getFileInputStream ( String fn ) {
		System . out . println ( "getFileInputStream:in "
			+ FileUtil . class . getClassLoader ( ) + ":" + fn ) ;
		InputStream in = FileUtil . class . getClassLoader ( ) . getResourceAsStream ( fn ) ;
		if ( in == null )
		if ( fn . startsWith ( "/" ) )
		in = FileUtil . class . getClassLoader ( ) . getResourceAsStream (
			fn . substring ( 1 ) ) ;
		else
		in = FileUtil . class . getClassLoader ( ) . getResourceAsStream ( "/" + fn ) ;
		return in ;
	}

	public static BufferedReader getRawBufferedReader ( String fn , String enc )
	throws UnsupportedEncodingException , FileNotFoundException {
		InputStream in = new FileInputStream ( fn ) ;
		return new BufferedReader ( new InputStreamReader ( in , enc ) ) ;
	}

	public static void pass ( InputStream in , OutputStream out ) throws IOException {
		byte [ ] buf = new byte [ 1024 * 16 ] ;
		int len ;
		while ( ( len = in . read ( buf ) ) > 0 )
		out . write ( buf , 0 , len ) ;
		out . flush ( ) ;
	}

	public static void pass ( InputStream in , OutputStream out , long total )
	throws IOException {
		byte [ ] buf = new byte [ 1024 * 16 ] ;
		int len ;
		long sum = 0 ;
		while ( ( len = in . read ( buf ) ) > 0 ) {
			out . write ( buf , 0 , len ) ;
			sum += len ;
			if ( sum >= total )
			// System.out.println("read finish");
			break ;
		}
		out . flush ( ) ;
	}

	public static String readString ( InputStream ins , String enc )
	throws IOException {
		if ( enc == null )
		enc = UTF8 ;
		BufferedReader in = new BufferedReader ( new InputStreamReader ( ins , enc ) ) ;
		char [ ] buf = new char [ 1000 ] ;
		int len ;
		StringBuffer sb = new StringBuffer ( ) ;
		while ( ( len = in . read ( buf ) ) > 0 )
		sb . append ( buf , 0 , len ) ;
		in . close ( ) ;
		return sb . toString ( ) ;
	}

	public static List < String > readStringBig ( File f , String enc )
	throws IOException {
		if ( enc == null )
		enc = UTF8 ;
		BufferedReader in
		= new BufferedReader ( new InputStreamReader ( new FileInputStream ( f ) , enc ) ) ;
		String line ;
		List < String > ret = new ArrayList < String > ( ) ;
		while ( true ) {
			line = in . readLine ( ) ;
			if ( line == null )
			break ;
			ret . add ( line ) ;
		}
		in . close ( ) ;
		return ret ;
	}

	public static List < String > readStringBig ( byte [ ] bs , String enc )
	throws IOException {
		if ( enc == null )
		enc = UTF8 ;
		BufferedReader in = new BufferedReader (
			new InputStreamReader ( new ByteArrayInputStream ( bs ) , enc ) ) ;
		String line ;
		List < String > ret = new ArrayList < String > ( ) ;
		while ( true ) {
			line = in . readLine ( ) ;
			if ( line == null )
			break ;
			ret . add ( line ) ;
		}
		in . close ( ) ;
		return ret ;
	}

	public static byte [ ] read ( InputStream in ) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream ( ) ;
		copy ( in , baos ) ;
		return baos . toByteArray ( ) ;
	}

	public static void save ( byte [ ] bs , String fn ) throws IOException {
		File f = new File ( fn ) ;
		f . getAbsoluteFile ( ) . getParentFile ( ) . mkdirs ( ) ;
		File ftmp = new File ( fn + "." + System . currentTimeMillis ( ) ) ;
		BufferedOutputStream out
		= new BufferedOutputStream ( new FileOutputStream ( ftmp ) ) ;
		out . write ( bs ) ;
		out . close ( ) ;
		if ( f . exists ( ) )
		f . delete ( ) ;
		Files . move ( ftmp . toPath ( ) , f . toPath ( ) ) ;
	}

	public static Iterable < CharSequence > split ( final CharSequence src ,
		final char sep ) {
		return new Iterable < CharSequence > ( ) {
			@ Override
			public Iterator < CharSequence > iterator ( ) {
				return new Iterator < CharSequence > ( ) {
					int p1 ;

					@ Override
					public CharSequence next ( ) {
						int p2 = -1 ;
						for ( int i = p1 ; i < src . length ( ) ; i ++ )
						if ( src . charAt ( i ) == sep ) {
							p2 = i ;
							break ;
						}
						if ( p2 == -1 ) {
							CharSequence r = src . subSequence ( p1 , src . length ( ) ) ;
							p1 = src . length ( ) ;
							return r ;
						} else {
							CharSequence r = src . subSequence ( p1 , p2 ) ;
							p1 = p2 + 1 ;
							return r ;
						}
					}

					@ Override
					public boolean hasNext ( ) {
						if ( src == null )
						return false ;
						if ( p1 >= src . length ( ) )
						return false ;
						return true ;
					}
				} ;
			}
		} ;
	}

	public static byte [ ] readBs ( InputStream in , int len ) throws IOException {
		byte [ ] b = new byte [ len ] ;
		int off = 0 ;
		int n = 0 ;
		while ( n < len ) {
			int count = in . read ( b , off + n , len - n ) ;
			if ( count < 0 )
			throw new EOFException ( ) ;
			n += count ;
		}
		return b ;
	}
}
