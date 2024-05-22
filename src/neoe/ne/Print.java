/*
 *  
 */
package neoe . ne ;

import java . awt . Color ;
import java . awt . Dimension ;
import java . awt . Graphics ;
import java . awt . Graphics2D ;
import java . awt . print . Book ;
import java . awt . print . PageFormat ;
import java . awt . print . Printable ;
import java . awt . print . PrinterException ;
import java . awt . print . PrinterJob ;
import java . io . File ;
import java . util . Date ;
import java . util . List ;
import static neoe . ne . U . TAB_WIDTH ;

/**
 * Seldom used, but hope it work.
 *
 * @author neoe
 */
public class Print implements Printable {
	Color colorLineNumber = new Color ( 0x30C200 ) , colorGutterLine = new Color ( 0x30C200 ) , colorNormal = Color . BLACK ,
	colorDigit = new Color ( 0xA8002A ) , colorKeyword = new Color ( 0x0099CC ) ,
	colorHeaderFooter = new Color ( 0x8A00B8 ) , colorComment = new Color ( 200 , 80 , 50 ) ;
	Dimension dim ;
	String fn ;
	FontList fonts ;

	int lineGap = 3 , lineHeight = 8 , headerHeight = 20 , footerHeight = 20 , gutterWidth = 24 ; // TAB_WIDTH = 20;
	int linePerPage ;
	private final PlainPage pp ;
	ReadonlyLines roLines ;
	String title ;
	int totalPage ;
	PlainPage . Paint ui ;
	EditorPanel uiComp ;

	public Print ( PlainPage pp ) {
		this . pp = pp ;
		this . ui = pp . ui ;
		this . uiComp = pp . uiComp ;
		this . roLines = pp . pageData . roLines ;
		this . fn = pp . pageData . title ;
		this . title = pp . pageData . title ;
		this . fonts = pp . fontList ;
		lineHeight = fonts . getlineHeight ( ) ;
	}

	void drawReturn ( Graphics2D g2 , int w , int py ) {
		g2 . setColor ( Color . red ) ;
		g2 . drawLine ( w , py - lineHeight + fonts . getlineHeight ( ) , w + 3 , py - lineHeight + fonts . getlineHeight ( ) ) ;
	}

	int drawStringLine ( Graphics2D g2 , FontList fonts , String s , int x , int y ) {
		int w = 0 ;
		int commentPos = getCommentPos ( s ) ;
		if ( commentPos >= 0 ) {
			String s1 = s . substring ( 0 , commentPos ) ;
			String s2 = s . substring ( commentPos ) ;
			int w1 = drawText ( g2 , fonts , s1 , x , y , false ) ;
			w = w1 + drawText ( g2 , fonts , s2 , x + w1 , y , true ) ;
		} else
		w = drawText ( g2 , fonts , s , x , y , false ) ;
		return w ;
	}

	int drawText ( Graphics2D g2 , FontList fonts , String s , int x , int y , boolean isComment ) {
		int w = 0 ;
		int maxw = dim . width - gutterWidth ;

		List < CharSequence > s1x = U . splitToken ( s ) ;
		for ( CharSequence s1c : s1x ) {
			String s1 = s1c . toString ( ) ;
			if ( s1 . equals ( "\t" ) ) {
				g2 . drawImage ( U . tabImgPrint , x + w , y - lineHeight , null ) ;
				w += TAB_WIDTH ;
			} else if ( isComment ) {
				g2 . setColor ( colorComment ) ;
				w += U . drawString ( g2 , fonts , s1 , x + w , y , maxw ) ;
				if ( w > dim . width - gutterWidth )
				break ;
			} else {
				U . getHighLightID ( s1 , g2 , colorKeyword , colorDigit , colorNormal ) ;
				w += U . drawString ( g2 , fonts , s1 , x + w , y , maxw ) ;
			}
			if ( w > maxw )
			break ;
		}

		return w ;
	}

	private int getCommentPos ( String s ) {
		String [ ] comment = pp . pageData . comment ;
		if ( comment == null )
		return -1 ;
		for ( String c : comment ) {
			int p = s . indexOf ( c ) ;
			if ( p >= 0 )
			return p ;
		}
		return -1 ;
	}

	int getTotalPage ( PageFormat pf ) {
		linePerPage = ( ( int ) pf . getImageableHeight ( ) - footerHeight - headerHeight ) / ( lineGap + lineHeight ) ;
		System . out . printf ( "page[%fx%f] linePerPage=%d" , pf . getImageableHeight ( ) , pf . getImageableWidth ( ) , linePerPage ) ;
		if ( linePerPage <= 0 ) {
			linePerPage = 1 ;
			return 1 ;
		}
		int lines = roLines . getLinesize ( ) ;
		int page = ( lines % linePerPage == 0 ) ? lines / linePerPage : lines / linePerPage + 1 ;
		return page ;
	}

	@ Override
	public int print ( Graphics graphics , PageFormat pf , int pageIndex ) throws PrinterException {
		if ( pageIndex > totalPage )
		return Printable . NO_SUCH_PAGE ;
		// print
		ui . message ( "printing " + ( pageIndex + 1 ) + "/" + totalPage ) ;
		uiComp . repaint ( ) ;
		Graphics2D g2 = ( Graphics2D ) graphics ;
		g2 . translate ( pf . getImageableX ( ) , pf . getImageableY ( ) ) ;
		if ( ui . noise )
		U . paintNoise ( g2 , new Dimension ( ( int ) pf . getImageableWidth ( ) , ( int ) pf . getImageableHeight ( ) ) ) ;
		int maxw = ( int ) pf . getImageableWidth ( ) ;
		g2 . setColor ( colorHeaderFooter ) ;
		U . drawString ( g2 , fonts , fn == null ? title : new File ( fn ) . getName ( ) , 0 , lineGap + lineHeight , maxw ) ;
		{
			String s = ( pageIndex + 1 ) + "/" + totalPage ;
			U . drawString ( g2 , fonts , s , ( int ) pf . getImageableWidth ( ) - U . stringWidth ( g2 , fonts , s , maxw ) - 2 ,
				lineGap + lineHeight , maxw ) ;
			s = new Date ( ) . toString ( ) + " - NeoeEdit" ;
			U . drawString ( g2 , fonts , s , ( int ) pf . getImageableWidth ( ) - U . stringWidth ( g2 , fonts , s , maxw ) - 2 ,
				( int ) pf . getImageableHeight ( ) - 2 , maxw ) ;
			g2 . setColor ( colorGutterLine ) ;
			g2 . drawLine ( gutterWidth - 4 , headerHeight , gutterWidth - 4 , ( int ) pf . getImageableHeight ( ) - footerHeight ) ;
		}
		int p = linePerPage * pageIndex ;
		int charCntInLine = ( int ) pf . getImageableWidth ( ) / 5 + 5 ; // inaccurate
		for ( int i = 0 ; i < linePerPage ; i ++ ) {
			if ( p >= roLines . getLinesize ( ) )
			break ;
			int y = headerHeight + ( lineGap + lineHeight ) * ( i + 1 ) ;
			g2 . setColor ( colorLineNumber ) ;
			U . drawString ( g2 , fonts , "" + ( p + 1 ) , 0 , y , maxw ) ;
			g2 . setColor ( colorNormal ) ;
			String s = roLines . getline ( p ++ ) . toString ( ) ;
			if ( s . length ( ) > charCntInLine )
			s = s . substring ( 0 , charCntInLine ) ;
			int w = drawStringLine ( g2 , fonts , s , gutterWidth , y ) ;
			drawReturn ( g2 , w + gutterWidth + 2 , y ) ;
		}

		return Printable . PAGE_EXISTS ;
	}

	void printPages ( ) {
		U . startDaemonThread ( new Thread ( ) {
				@ Override
				public void run ( ) {
					try {
						PrinterJob job = PrinterJob . getPrinterJob ( ) ;
						PageFormat pf = job . pageDialog ( job . defaultPage ( ) ) ;
						totalPage = getTotalPage ( pf ) ;
						if ( totalPage <= 0 )
						return ;
						dim = new Dimension ( ( int ) pf . getImageableWidth ( ) , ( int ) pf . getImageableHeight ( ) ) ;
						Book bk = new Book ( ) ;
						bk . append ( Print . this , pf , totalPage ) ;
						job . setPageable ( bk ) ;
						if ( job . printDialog ( ) ) {
							ui . message ( "printing..." ) ;
							uiComp . repaint ( ) ;
							job . print ( ) ;
							ui . message ( "print ok" ) ;
							uiComp . repaint ( ) ;
						}
					} catch ( Exception e ) {
						ui . message ( "err:" + e ) ;
						uiComp . repaint ( ) ;
						e . printStackTrace ( ) ;
					}
				}
			} ) ;
	}
}
