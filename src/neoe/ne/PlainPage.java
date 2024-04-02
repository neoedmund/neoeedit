package neoe . ne ;

import static neoe . ne . U . showPageListPage ;

import java . awt . Color ;
import java . awt . Dimension ;
import java . awt . Font ;
import java . awt . Graphics ;
import java . awt . Graphics2D ;
import java . awt . Rectangle ;
import java . awt . RenderingHints ;
import java . awt . TexturePaint ;
import java . awt . event . InputEvent ;
import java . awt . event . KeyEvent ;
import java . awt . event . MouseEvent ;
import java . awt . event . MouseWheelEvent ;
import java . awt . image . BufferedImage ;
import java . io . File ;
import java . io . IOException ;
import java . util . ArrayList ;
import java . util . LinkedHashMap ;
import java . util . List ;
import java . util . Map ;

import javax . swing . JFrame ;
import javax . swing . JInternalFrame ;
import javax . swing . JOptionPane ;

import neoe . ne . CommandPanel . CommandPanelPaint ;
import neoe . ne . Ime . Out ;
import neoe . ne . Plugin . PluginAction ;
import neoe . ne . Plugin . PluginAction2 ;
import neoe . ne . util . FindJDK ;

public class PlainPage {
	private static boolean isButtonDown ( int i , MouseEvent evt ) {
		int b = InputEvent . getMaskForButton ( i ) ;
		int ex = evt . getModifiersEx ( ) ;
		return ( ex & b ) != 0 ;
	}

	// boolean changedOutside = false ;
	public Console console ;
	Cursor cursor = new Cursor ( ) ;
	public int cx ;
	public int cy ;
	public Map < String , String > env ;
	public String [ ] envs ;
	FontList fontList ;
	boolean ignoreCase = true ;
	public int keepx = -1 ;
	Dimension lastSize = new Dimension ( ) ;
	int mcount ;
	String msg ;
	long msgtime ;

	//
	boolean mshift ;
	/* mouse x,y */
	int mx , my ;
	public String workPath = "." ;
	public PageData pageData ;

	private String preeditText ;
	public EasyEdit ptEdit = new EasyEdit ( ) ;
	public FindAndReplace ptFind = new FindAndReplace ( this ) ;
	public Selection ptSelection = new Selection ( ) ;
	public boolean readonly = false ;

	boolean rectSelectMode = false ;
	boolean savingFromSelectionCancel ;

	int selectstartx , selectstarty , selectstopx , selectstopy ;

	int showLineCnt , showLineCnt2 ;
	int sx , sy ;

	int toolbarHeight = 25 ;

	public Paint ui = new Paint ( ) ;
	public EditorPanel uiComp ;
	boolean follow ;

	private PlainPage ( ) {
	}

	/**
	 * there is only a few caller
	 */
	public PlainPage ( EditorPanel editor , PageData data , PlainPage parent ) throws Exception {
		this . uiComp = editor ;
		this . pageData = data ;
		if ( parent != null ) {
			ui . applyColorMode ( parent . ui . colorMode ) ;
			ui . scalev = parent . ui . scalev ;
			ui . shrinkWord = parent . ui . shrinkWord ;
			fontList = parent . fontList ;
			if ( fontList == Conf . defaultConsoleFonts )
			fontList = Conf . defaultFontList ;
			workPath = parent . workPath ;
			showLineCnt = parent . showLineCnt ;
			follow = parent . follow ;
			if ( parent . env != null )
			env = new LinkedHashMap < > ( parent . env ) ;
		} else
		fontList = Conf . defaultFontList ;
		if ( data . fileLoaded )
		workPath = new File ( data . title ) . getParent ( ) ;
		editor . pageSet . add ( this ) ;
		editor . setPage ( this , true ) ;
		data . ref ++ ;
	}

	public void close ( ) throws Exception {
		String lastPageAndPos = uiComp . pageHis . back ( U . getLocString ( this ) ) ;

		uiComp . page = null ;
		uiComp . pageSet . remove ( this ) ;

		pageData . ref -- ;
		if ( pageData . ref <= 0 )
		pageData . close ( ) ;
		pageData = null ;

		if ( uiComp . pageSet . size ( ) <= 0 ) {
			// nothing to show
			if ( uiComp . frame != null )
			if ( uiComp . frame instanceof JFrame )
			( ( JFrame ) uiComp . frame ) . dispose ( ) ;
			else if ( uiComp . frame instanceof JFrame )
			( ( JInternalFrame ) uiComp . frame ) . dispose ( ) ;
			else
			System . out . println ( "cannot close frame, bug" ) ;
		} else
		gotoFileLine ( lastPageAndPos , uiComp , false ) ;
		if ( uiComp . page == null && ! uiComp . pageSet . isEmpty ( ) ) { // if anything failed
			PlainPage lp = uiComp . pageSet . get ( 0 ) ;
			uiComp . setPage ( lp , false ) ;
		}
	}

	public void go ( String line , boolean newWindow ) throws Exception {
		if ( line == null )
		return ;
		line = line . trim ( ) ;
		if ( line . isEmpty ( ) )
		return ;
		if ( line . startsWith ( "set-font:" ) ) {
			String fn = line . substring ( "set-font:" . length ( ) ) . trim ( ) ;
			Font font = U . getFont ( fn , fontList . getlineHeight ( ) ) ;
			U . setFont ( uiComp , font ) ;
		} else {
			if ( line . startsWith ( "~/" ) )
			line = U . getUserHome ( ) + line . substring ( 1 ) ;
			uiComp . newWindow = newWindow ;
			if ( pageData . searchResultOf == null || ! gotoFileLineSearchResult ( uiComp , line , pageData . searchResultOf ) )
			if ( ! gotoFileLine ( line , uiComp , true ) )
			if ( ! U . listDirOrOpenFile ( PlainPage . this , cy ) )
			if ( ! Plugin . goHandle ( line , PlainPage . this ) )
			U . launch ( line ) ;
			uiComp . newWindow = false ;
		}
	}

	private static boolean gotoFileLine ( String s , EditorPanel ep , boolean rec ) throws Exception {
		if ( s == null )
		return false ;
		int p1 , p2 ;
		String fn = s ;
		if ( ( p1 = s . indexOf ( '|' ) ) >= 0 ) {
			fn = s . substring ( 0 , p1 ) . trim ( ) ;
			if ( ( p2 = s . indexOf ( ':' , p1 ) ) >= 0 ) { // search result
				int line = -1 ;
				try {
					String v = s . substring ( p1 + 1 , p2 ) ;
					line = Integer . parseInt ( v ) ;
				} catch ( NumberFormatException e ) {
				}
				if ( line >= 0 ) {
					ep . findAndShowPage ( fn , line , rec ) ;
					return true ;
				}
			}
		} else if ( ( p1 = s . indexOf ( ':' , 2 ) ) > 0 ) { // try filename:lineno pattern
			int line = 0 ;
			try {
				fn = s . substring ( 0 , p1 ) . trim ( ) ;
				p2 = s . indexOf ( ':' , p1 + 1 ) ;
				String v ;
				if ( p2 > 0 ) // fn:line:nnn in some format(like javac output)
				v = s . substring ( p1 + 1 , p2 ) . trim ( ) ;
				else
				v = s . substring ( p1 + 1 ) . trim ( ) ;
				line = Integer . parseInt ( v ) ;
			} catch ( NumberFormatException e ) {
			}
			return ep . findAndShowPage ( fn , line , rec ) ;
		}

		return false ;
	}

	/*
	 * goto search result
	 */
	private static boolean gotoFileLineSearchResult ( EditorPanel ep , String sb , String fn ) throws Exception {
		int p1 ;
		if ( ( p1 = sb . indexOf ( ":" ) ) >= 0 )
		try {
			int line = Integer . parseInt ( sb . substring ( 0 , p1 ) ) ;
			return ep . findAndShowPage ( fn , line , true ) ;
		} catch ( Exception e ) {
		}
		return false ;
	}

	public void doMoveViewDown ( ) {
		sy = Math . min ( sy + 1 , pageData . roLines . getLinesize ( ) - 1 ) ;
	}

	public void doMoveViewUp ( ) {
		sy = Math . max ( 0 , sy - 1 ) ;
	}

	public void doMoveViewPageDown ( ) {
		sy = Math . min ( sy + showLineCnt2 , pageData . roLines . getLinesize ( ) - 1 ) ;
	}

	public void doMoveViewPageUp ( ) {
		sy = Math . max ( 0 , sy - showLineCnt2 ) ;
	}

	/* let cursor get see */
	public void focusCursor ( ) {
		sy = U . between ( U . between ( sy , cy - showLineCnt + 3 , cy - 3 ) , 0 , pageData . roLines . getLinesize ( ) - 1 ) ;
	}

	/** change cursor to middle of page */
	public void adjustCursor ( ) {
		if ( showLineCnt == 0 ) { // not yet painted,
			showLineCnt = ( int ) Math
			. ceil ( ( uiComp . getSize ( ) . height - toolbarHeight ) / ( ( ui . lineHeight + ui . lineGap ) * ui . scalev ) ) ;
			showLineCnt2 = ( int ) Math
			. floor ( ( uiComp . getSize ( ) . height - toolbarHeight ) / ( ( ui . lineHeight + ui . lineGap ) * ui . scalev ) ) ;
			if ( showLineCnt2 <= 0 )
			showLineCnt2 = 1 ;
		}
		int sc = Math . max ( 5 , showLineCnt ) ;
		sy = Math . max ( 0 , cy - sc / 2 + 1 ) ;
		int totalLine = pageData . roLines . getLinesize ( ) ;
		int emptyLines = sc - ( totalLine - sy ) ;
		if ( emptyLines > 0 )
		sy = Math . max ( 0 , sy - emptyLines + 1 ) ;
	}

	private boolean isButtonBack ( MouseEvent evt ) {
		if ( !FindJDK . isLinux ) {
			if ( isButtonDown ( 4 , evt ) )
			return true ;
		} else // Linux
		if ( isButtonDown ( 6 , evt ) )
		return true ;
		return false ;
	}

	private boolean isButtonForward ( MouseEvent evt ) {
		if ( !FindJDK . isLinux ) {
			if ( isButtonDown ( 5 , evt ) )
			return true ;
		} else // Linux
		if ( isButtonDown ( 7 , evt ) )
		return true ;
		return false ;
	}

	public void keyPressed ( KeyEvent evt ) {
		Ime . ImeInterface ime = Ime . getCurrentIme ( ) ;
		if ( ime != null && evt . getKeyCode ( ) != KeyEvent . VK_TAB ) {
			Out param = new Out ( ) ;
			if ( ! ( evt . isControlDown ( ) || evt . isAltDown ( ) ) )
			ime . keyPressed ( evt . getKeyCode ( ) , param ) ;

			if ( param . yield != null )
			ptEdit . insertString ( param . yield ) ;
			preeditText = param . preedit ;
			if ( param . consumed ) {
				repaint ( ) ;
				return ;
			}
		}

		if ( evt . getKeyCode ( ) == KeyEvent . VK_ESCAPE )
		if ( ui . cp . showCommandPanel ) {
			ui . cp . showCommandPanel = false ;
			evt . consume ( ) ;
			repaint ( ) ;
			return ;
		}

		int kc = evt . getKeyCode ( ) ;
		if ( kc != KeyEvent . VK_SHIFT && kc != KeyEvent . VK_CONTROL && kc != KeyEvent . VK_ALT ) {
		} else {
			return ;
		}

		pageData . history . beginAtom ( ) ;
		try {
			mshift = evt . isShiftDown ( ) ;
			int ocx = cx ;
			int ocy = cy ;

			// int kc = evt.getKeyCode();
			// boolean onlyShift = evt . isShiftDown ( ) && ! evt . isControlDown ( ) && !
			// evt . isAltDown ( ) ;
			// if ( ! onlyShift
			//// && ( evt . isActionKey ( ) || evt . isControlDown ( ) || evt . isAltDown (
			// ) )
			//// && ( kc != KeyEvent . VK_SHIFT && kc != KeyEvent . VK_CONTROL && kc !=
			// KeyEvent . VK_ALT )
			// ) {
			String name = U . getKeyName ( evt ) ;
			PluginAction2 ac2 = Plugin . findAction ( name ) ;
			if ( ac2 != null ) {
				try {
					ac2 . run ( this , name ) ;
				} catch ( Throwable e ) {
					e . printStackTrace ( ) ;
					if ( e . getCause ( ) != null )
					e = e . getCause ( ) ;
					ui . message ( "plugin:" + e . getMessage ( ) ) ;
				}
				evt . consume ( ) ;
				repaint ( ) ;
				return ;
			}

			PluginAction ac = U . pluginKeys . get ( name ) ;
			if ( ac != null ) {
				try {
					ac . run ( this ) ;
				} catch ( Throwable e ) {
					e . printStackTrace ( ) ;
					if ( e . getCause ( ) != null )
					e = e . getCause ( ) ;
					ui . message ( "plugin:" + e . getMessage ( ) ) ;
				}
				evt . consume ( ) ;
				repaint ( ) ;
				return ;
			}

			Commands cmd = U . mappingToCommand ( evt ) ;
			if ( cmd != null ) {
				processCommand ( cmd ) ;
				evt . consume ( ) ;
			} else {
				if ( evt . isControlDown ( ) || evt . isAltDown ( ) || evt . isActionKey ( ) )
				unknownCommand ( evt ) ;
			}

			boolean cmoved = ! ( ocx == cx && ocy == cy ) ;
			if ( cmoved )
			if ( evt . isShiftDown ( ) ) {
				selectstopx = cx ;
				selectstopy = cy ;
			} else if ( savingFromSelectionCancel )
			savingFromSelectionCancel = false ;
			else
			ptSelection . cancelSelect ( ) ;
			repaint ( ) ;
		} catch ( Throwable e ) {
			ui . message ( "err:" + e ) ;
			e . printStackTrace ( ) ;
		} finally {
			if ( pageData != null ) // in case closed
			pageData . history . endAtom ( ) ;
		}
	}

	public void keyReleased ( KeyEvent env ) {
	}

	public void keyTyped ( KeyEvent env ) {
		if ( readonly )
		return ;
		if ( env . isControlDown ( ) || env . isAltDown ( ) ) {
			// ignore
		} else {
			pageData . history . beginAtom ( ) ;
			try {
				char kc = env . getKeyChar ( ) ;
				if ( kc == KeyEvent . VK_TAB && env . isShiftDown ( ) ) {
					Rectangle r = ptSelection . getSelectRect ( ) ;
					if ( r . y < r . height )
					ptEdit . moveRectLeft ( r . y , r . height ) ;
					else
					ptEdit . moveLineLeft ( cy ) ;
				} else if ( kc == KeyEvent . VK_TAB && ! env . isShiftDown ( ) && selectstarty != selectstopy
					&& ! rectSelectMode ) {
					Rectangle r = ptSelection . getSelectRect ( ) ;
					ptEdit . moveRectRight ( r . y , r . height ) ;
				} else {
					Ime . ImeInterface ime = Ime . getCurrentIme ( ) ;
					if ( ime != null && kc != KeyEvent . VK_TAB ) {
						Out param = new Out ( ) ;
						if ( ! ( env . isControlDown ( ) || env . isAltDown ( ) ) )
						ime . keyTyped ( env . getKeyChar ( ) , param ) ;

						if ( param . yield != null )
						ptEdit . insertString ( param . yield ) ;
						preeditText = param . preedit ;

						if ( ! param . consumed )
						ptEdit . insert ( kc ) ;
					} else {
						ptEdit . insert ( kc ) ;
						if ( kc == '=' ) {
							String ss = pageData . roLines . getline ( cy ) . toString ( ) ;
							if ( cx <= ss . length ( ) && cx >= 3 )
							try {
								ss = ss . substring ( 0 , cx ) ;
								if ( ss . endsWith ( "=" ) )
								ss = ss . substring ( 0 , ss . length ( ) - 1 ) ;
								ss = U . getMathExprTail ( ss ) ;
								if ( ! ss . isEmpty ( ) )
								ptEdit . insertString ( " " + U . evalMath ( ss ) ) ;
							} catch ( Exception ex ) {
								/* ignore */
							}
						}
					}
				}
			} finally {
				pageData . history . endAtom ( ) ;
			}
		}
	}

	public void mouseClicked ( MouseEvent evt ) {
		if ( ui . cp . showCommandPanel ) {
			ui . cp . mouseClicked ( evt ) ;
			if ( ui . cp . clickedName != null ) {
				try {
					processCommand ( Commands . valueOf ( ui . cp . clickedName ) ) ;
					repaint ( ) ;
					mx = 0 ;
					my = 0 ;
					ui . cp . showCommandPanel = false ;
				} catch ( Throwable e ) {
					ui . message ( "err:" + e ) ;
					e . printStackTrace ( ) ;
				}
				ui . cp . clickedName = null ;
			}
			return ;
		} {
			if ( isButtonDown ( 4 , evt ) || isButtonDown ( 5 , evt ) || isButtonDown ( 6 , evt ) || isButtonDown ( 7 , evt ) )
			return ;
		}
		int my1 = evt . getY ( ) ;
		if ( my1 > 0 && my1 < toolbarHeight ) {
			if ( pageData . fileLoaded ) {
				U . setClipBoard ( pageData . title ) ;
				ui . message ( "filename copied" ) ;
				my1 = 0 ;
				// repaint();
			} else if ( workPath != null ) {
				U . setClipBoard ( workPath ) ;
				ui . message ( "work path copied" ) ;
				my1 = 0 ;
			} else {
				try {
					if ( U . saveFile ( this ) )
					ui . message ( "saved" ) ;
				} catch ( Throwable e ) {
					ui . message ( "err:" + e ) ;
					e . printStackTrace ( ) ;
				}
			}
		} else {
			int mx1 = evt . getX ( ) ;
			if ( mx1 > 0 && mx1 < ui . gutterWidth ) {
				cursor . gotoLine ( ) ;
				repaint ( ) ;
			}
		}
	}

	public void mouseDragged ( MouseEvent evt ) { {
			if ( isButtonDown ( 4 , evt ) || isButtonDown ( 5 , evt ) || isButtonDown ( 6 , evt ) || isButtonDown ( 7 , evt ) )
			return ;
		}
		mx = evt . getX ( ) ;
		my = evt . getY ( ) ;
		mshift = true ;
		repaint ( ) ;
	}

	public void mouseMoved ( MouseEvent evt ) {
		if ( ui . cp . showCommandPanel )
		ui . cp . mouseMoved ( evt ) ;
	}

	public void mousePressed ( MouseEvent evt ) throws Exception {
		// System.out.println(evt.getButton());
		if ( isButtonBack ( evt ) )
		gotoFileLine ( uiComp . pageHis . back ( U . getLocString ( this ) ) , uiComp , false ) ;
		else if ( isButtonForward ( evt ) )
		gotoFileLine ( uiComp . pageHis . forward ( U . getLocString ( this ) ) , uiComp , false ) ;
		else {
			mx = evt . getX ( ) ;
			my = evt . getY ( ) ;
			mshift = evt . isShiftDown ( ) ;
			mcount = evt . getClickCount ( ) ;
			repaint ( ) ;
		}
	}

	public void mouseWheelMoved ( MouseWheelEvent env ) {
		int amount = env . getWheelRotation ( ) * env . getScrollAmount ( ) ;
		if ( env . isControlDown ( ) ) { // scale
			U . scale ( amount , ui ) ;
			this . repaint ( ) ;
		} else if ( env . isAltDown ( ) ) // horizon scroll
		cursor . scrollHorizon ( amount ) ;
		else // scroll
		cursor . scroll ( amount ) ;
	}

	/*
	 * Add support to on-the-spot pre-editing of input method like CJK IME, not
	 * perfect(the current java implementation seems not support pre-edit window
	 * following function), but keep up with what did as swing JTextComponent.
	 *
	 */
	public void preedit ( String text , int committedCharacterCount ) {
		// System.out.println("preedit:" + text + "," +
		// committedCharacterCount);
		if ( committedCharacterCount > 0 ) {
			String commit = text . substring ( 0 , committedCharacterCount ) ;
			text = text . substring ( committedCharacterCount ) ;
			ptEdit . insertString ( commit ) ;
		}
		this . preeditText = text ;
		repaint ( ) ;
	}

	void processCommand ( Commands cmd ) throws Exception {
		switch ( cmd ) {
			case showHelp :
			U . showHelp ( ui , uiComp ) ;
			break ;
			case saveAs :
			U . saveAs ( this ) ;
			break ;
			case toggleTimeExec :
			U . addTime = ! U . addTime ;
			U . setEnv ( this , "ne_addTime" , "" + U . addTime ) ;
			ui . message ( "timestamp in console:" + U . addTime ) ;
			break ;
			case toggleFollowExec :
			if ( console != null ) {
				console . follow = ! console . follow ;
				follow = console . follow ;
				ui . message ( "follow the console:" + console . follow ) ;
			}
			break ;
			case toggleShrinkWord :
			U . shrinkWord = ! U . shrinkWord ;
			ui . message ( "shrinkWord:" + U . shrinkWord ) ;
			break ;
			case changePathSep :
			U . changePathSep ( pageData , cy ) ;
			break ;
			case findNext :
			if ( ptFind . back )
			ptFind . findPrev ( ptFind . word ) ;
			else
			ptFind . findNext ( ptFind . word ) ;
			break ;
			case findPrev :
			if ( ! ptFind . back )
			ptFind . findPrev ( ptFind . word ) ;
			else
			ptFind . findNext ( ptFind . word ) ;
			break ;
			case commandPanel :
			ui . cp . showCommandPanel = true ;
			break ;
			case reloadWithEncoding :
			U . reloadWithEncodingByUser ( this ) ;
			break ;
			case moveLeft :
			if ( readonly )
			doMoveViewPageUp ( ) ;
			else {
				cursor . moveLeft ( ) ;
				focusCursor ( ) ;
			}
			break ;
			case moveRight :
			if ( readonly )
			doMoveViewPageDown ( ) ;
			else {
				cursor . moveRight ( ) ;
				focusCursor ( ) ;
			}
			break ;
			case moveUp :
			if ( readonly )
			doMoveViewUp ( ) ;
			else {
				cursor . moveUp ( ) ;
				focusCursor ( ) ;
			}
			break ;
			case moveDown :
			if ( readonly )
			doMoveViewDown ( ) ;
			else {
				cursor . moveDown ( ) ;
				focusCursor ( ) ;
			}
			break ;
			case moveHome :
			cursor . moveHome ( ) ;
			focusCursor ( ) ;
			break ;
			case moveEnd :
			cursor . moveEnd ( ) ;
			focusCursor ( ) ;
			break ;
			case movePageUp :
			if ( readonly )
			doMoveViewPageUp ( ) ;
			else {
				cursor . movePageUp ( ) ;
				focusCursor ( ) ;
			}
			break ;
			case movePageDown :
			if ( readonly )
			doMoveViewPageDown ( ) ;
			else {
				cursor . movePageDown ( ) ;
				focusCursor ( ) ;
			}
			break ;
			case indentLeft :
			ptEdit . moveLineLeft ( cy ) ;
			focusCursor ( ) ;
			break ;
			case indentRight :
			ptEdit . moveLineRight ( cy ) ;
			focusCursor ( ) ;
			break ;
			case rectangleMode :
			rectSelectMode = ! rectSelectMode ;
			break ;
			case makeNoise :
			ui . noise = ! ui . noise ;
			if ( ui . noise )
			U . startNoiseThread ( ui , uiComp ) ;
			break ;
			case toggleFps :
			ui . fpsOn = ! ui . fpsOn ;
			break ;
			case switchLineSeperator :
			if ( pageData . lineSep . equals ( "\n" ) )
			pageData . lineSep = "\r\n" ;
			else
			pageData . lineSep = "\n" ;
			break ;
			case wrapLines :
			ptEdit . wrapLines ( cx ) ;
			focusCursor ( ) ;
			break ;
			case Javascript :
			U . runScript ( this ) ;
			break ;
			case moveLeftBig :
			cx = Math . max ( 0 , cx - uiComp . getWidth ( ) / 10 ) ;
			focusCursor ( ) ;
			break ;
			case moveRightBig :
			cx = cx + uiComp . getWidth ( ) / 10 ;
			focusCursor ( ) ;
			break ;
			case switchColorMode :
			ui . setNextColorMode ( ) ;
			ui . applyColorMode ( ui . colorMode ) ;
			break ;

			case moveBetweenPair :
			cursor . moveToPair ( ) ;
			break ;

			case execute :
			if ( cy < pageData . roLines . getLinesize ( ) )
			U . exec ( this , pageData . roLines . getline ( cy ) . toString ( ) ) ;
			break ;
			case hex :
			String s = U . exportString ( ptSelection . getSelected ( ) , pageData . lineSep ) ;
			if ( s != null && s . length ( ) > 0 )
			U . showHexOfString ( s , PlainPage . this ) ;
			break ;
			case listFonts :
			U . listFonts ( this ) ;
			break ;
			case listDoc :
			U . listDoc ( uiComp ) ;
			break ;
			case copySelected :
			ptSelection . copySelected ( ) ;
			break ;
			case paste :
			if ( keepx == -1 ) // use-case 2: paste same thing along lines
			keepx = cx ;
			ptEdit . insertString ( U . getClipBoard ( ) , true ) ;
			break ;
			case cut :
			ptSelection . cutSelected ( ) ;
			break ;
			case selectLine :
			ptSelection . selectLine ( ) ;
			break ;
			case selectAll :
			ptSelection . selectAll ( ) ;
			break ;
			case deleteLine :
			if ( ptSelection . isSelected ( ) )
			ptEdit . deleteRect ( ptSelection . getSelectRect ( ) ) ;
			else
			ptEdit . deleteLine ( cy ) ;
			focusCursor ( ) ;
			break ;
			case openFile :
			U . listDirToNewPage ( this ) ;
			break ;
			case newPage :
			PlainPage pp = new PlainPage ( uiComp , PageData . newUntitled ( ) , this ) ;
			pp . ptSelection . selectAll ( ) ;
			break ;
			case newWindow :
			U . newWindow ( this ) ;
			break ;
			case save :
			if ( U . saveFile ( this ) )
			ui . message ( "saved" ) ;
			break ;
			case gotoLine :
			cursor . gotoLine ( ) ;
			break ;
			case gotoX :
			cursor . gotoX ( ) ;
			break ;
			case undo :
			pageData . history . undo ( this ) ;
			break ;
			case find :
			ptFind . showFindDialog ( ) ;
			break ;
			case redo :
			pageData . history . redo ( this ) ;
			break ;
			case closePage :
			U . closePage ( this ) ;
			break ;
			case setEncoding :
			U . setEncodingByUser ( this , "Set Encoding:" ) ;
			break ;
			case moveToHead :
			cy = 0 ;
			cx = 0 ;
			focusCursor ( ) ;
			break ;
			case moveToTail :
			cy = pageData . roLines . getLinesize ( ) - 1 ;
			cx = 0 ;
			focusCursor ( ) ;
			break ;
			case removeTralingSpace :
			U . removeTrailingSpace ( pageData ) ;
			break ;
			case moveLeftWord :
			cursor . moveLeftWord ( ) ;
			focusCursor ( ) ;
			break ;
			case deleteWord :
			ptEdit . deleteSpace ( ) ;
			focusCursor ( ) ;
			break ;
			case deleteWordBack :
			ptEdit . deleteSpaceBack ( ) ;
			focusCursor ( ) ;
			break ;
			case moveRightWord :
			cursor . moveRightWord ( ) ;
			focusCursor ( ) ;
			break ;
			case moveViewUp :
			doMoveViewUp ( ) ;
			break ;
			case moveViewDown :
			doMoveViewDown ( ) ;
			break ;
			case moveUpLangLevel :
			cursor . doMoveUpLangLevel ( ) ;
			break ;
			case resetScale :
			ui . scalev = 1 ;
			break ;
			case go :
			if ( cy < pageData . roLines . getLinesize ( ) ) {
				String line = pageData . roLines . getline ( cy ) . toString ( ) ;
				go ( line , false ) ;
			}
			break ;
			case goInNewWindow : // not work yet
			if ( cy < pageData . roLines . getLinesize ( ) ) {
				String line = pageData . roLines . getline ( cy ) . toString ( ) ;
				go ( line , true ) ;
			}
			break ;
			case launch :
			if ( cy < pageData . roLines . getLinesize ( ) ) {
				String line = pageData . roLines . getline ( cy ) . toString ( ) ;
				U . launch ( line ) ;
			}
			break ;
			case readonlyMode :
			readonly = ! readonly ;
			break ;
			case fileHistory :
			U . openFileHistory ( uiComp ) ;
			break ;
			case dirHistory :
			U . openCmdHistory ( uiComp ) ;
			break ;
			case openFileSelector :
			if ( cy < pageData . roLines . getLinesize ( ) ) {
				String line = pageData . roLines . getline ( cy ) . toString ( ) ;
				U . openFileSelector ( line , this ) ;
			}
			break ;
			case print :
			new Print ( PlainPage . this ) . printPages ( ) ;
			break ;
			case pageList :
			showPageListPage ( uiComp ) ;
			break ;
			case quickSwitchPage :
			gotoFileLine ( uiComp . pageHis . back ( U . getLocString ( this ) ) , uiComp , true ) ;
			break ;
			case resetIME :
			Ime . resetIme ( ) ;
			break ;
			case toggleIME :
			Ime . nextIme ( ) ;
			Ime . ImeInterface ime = Ime . getCurrentIme ( ) ;
			if ( ime != null )
			ime . setEnabled ( true ) ;
			break ;
			// case ShellCommand :
			// Shell . run ( PlainPage . this , cy ) ;
			// break ;
			case pageForward :
			gotoFileLine ( uiComp . pageHis . forward ( U . getLocString ( this ) ) , uiComp , false ) ;
			break ;
			case pageBack :
			gotoFileLine ( uiComp . pageHis . back ( U . getLocString ( this ) ) , uiComp , false ) ;
			break ;
			default :
			ui . message ( "unprocessed Command:" + cmd ) ;
		}
	}

	public void repaint ( ) {
		new Thread ( ( ) -> uiComp . repaint ( ) ) . start ( ) ;
		// SwingUtilities . invokeLater ( ) ;
	}

	private void unknownCommand ( KeyEvent env ) {
		StringBuilder sb = new StringBuilder ( ) ;
		if ( env . isControlDown ( ) )
		sb . append ( "Ctrl" ) ;
		if ( env . isAltDown ( ) ) {
			if ( sb . length ( ) > 0 )
			sb . append ( "-" ) ;
			sb . append ( "Alt" ) ;
		}
		if ( env . isShiftDown ( ) ) {
			if ( sb . length ( ) > 0 )
			sb . append ( "-" ) ;
			sb . append ( "Shift" ) ;
		}
		if ( sb . length ( ) > 0 )
		sb . append ( "-" ) ;
		sb . append ( KeyEvent . getKeyText ( env . getKeyCode ( ) ) ) ;
		ui . message ( "Unknow Command:" + sb ) ;
	}

	public void xpaint ( Graphics g , Dimension size ) {
		if ( ! lastSize . equals ( size ) ) { // resized
			lastSize = size ;
			ui . cp . inited = false ;
		}
		ui . xpaint ( g , size ) ;
	}

	class Cursor {
		void gotoLine ( ) {
			String s = JOptionPane . showInputDialog ( uiComp , "Goto Line" ) ;
			int line = -1 ;
			try {
				line = Integer . parseInt ( s ) ;
			} catch ( Exception e ) {
				line = -1 ;
			}
			if ( line > pageData . roLines . getLinesize ( ) )
			line = -1 ;
			if ( line > 0 ) {
				line -= 1 ;
				sy = Math . max ( 0 , line - showLineCnt / 2 + 1 ) ;
				cy = line ;
				cx = 0 ;
				adjustCursor ( ) ;
			}
		}

		void gotoX ( ) {
			String s = JOptionPane . showInputDialog ( uiComp , "Goto X" ) ;
			int x = -1 ;
			try {
				x = Integer . parseInt ( s ) ;
			} catch ( Exception e ) {
				x = -1 ;
			}
			if ( x > 0 ) {
				setSafePos ( x , cy ) ;
				adjustCursor ( ) ;
			}
		}

		void moveDown ( ) {
			cy += 1 ;
			if ( cy >= pageData . roLines . getLinesize ( ) )
			if ( rectSelectMode ) {
				pageData . editRec . insertEmptyLine ( cy ) ;
				if ( cx > 0 )
				pageData . editRec . insertInLine ( cy , 0 , U . spaces ( cx ) ) ;
			} else
			cy = pageData . roLines . getLinesize ( ) - 1 ;
			keepX ( ) ;
		}

		void moveEnd ( ) {
			keepx = -1 ;
			CharSequence line = pageData . roLines . getline ( cy ) ;
			int p1 = line . length ( ) ;
			while ( p1 > 0 && U . isSpaceChar ( line . charAt ( p1 - 1 ) ) )
			p1 -- ;
			if ( cx < p1 || cx >= line . length ( ) )
			cx = p1 ;
			else
			cx = Integer . MAX_VALUE ;
		}

		void moveHome ( ) {
			keepx = -1 ;
			CharSequence line = pageData . roLines . getline ( cy ) ;
			int p1 = 0 ;
			int len = line . length ( ) ;
			while ( p1 < len - 1 && U . isSpaceChar ( line . charAt ( p1 ) ) )
			p1 ++ ;
			if ( cx > p1 || cx == 0 )
			cx = p1 ;
			else
			cx = 0 ;
		}

		void moveLeft ( ) {
			keepx = -1 ;
			cx -= 1 ;
			if ( cx < 0 )
			if ( cy > 0 && ! ptSelection . isRectSelecting ( ) ) {
				cy -= 1 ;
				cx = pageData . roLines . getline ( cy ) . length ( ) ;
			} else
			cx = 0 ;
		}

		void moveLeftWord ( ) {
			keepx = -1 ;
			CharSequence line = pageData . roLines . getline ( cy ) ;
			cx = Math . max ( 0 , cx - 1 ) ;
			char ch1 = U . charAtWhenMove ( line , cx ) ;
			while ( true )
			if ( cx <= 0 )
			if ( cy <= 0 )
			break ;
			else {
				cy -- ;
				line = pageData . roLines . getline ( cy ) ;
				cx = Math . max ( 0 , line . length ( ) - 1 ) ;
			}
			else {
				cx -- ;
				if ( U . isSkipChar ( line . charAt ( cx ) , ch1 ) )
				continue ;
				else {
					cx ++ ;
					break ;
				}
			}
		}

		void movePageDown ( ) {
			cy += showLineCnt ;
			if ( cy >= pageData . roLines . getLinesize ( ) )
			if ( rectSelectMode ) {
				String SP = U . spaces ( cx ) ;
				int cnt = cy - pageData . roLines . getLinesize ( ) + 1 ;
				int p = pageData . roLines . getLinesize ( ) ;
				for ( int i = 0 ; i < cnt ; i ++ ) {
					pageData . editRec . insertEmptyLine ( p ) ;
					if ( cx > 0 )
					pageData . editRec . insertInLine ( p , 0 , SP ) ;
				}
			} else
			cy = pageData . roLines . getLinesize ( ) - 1 ;
			keepX ( ) ;
		}

		void keepX ( ) {
			if ( rectSelectMode )
			return ;
			if ( keepx == -1 )
			keepx = cx ; // System.out.println("keepx=" + keepx);
			else
			cx = Math . min ( keepx , pageData . roLines . getline ( cy ) . length ( ) ) ;
		}

		void movePageUp ( ) {
			cy -= showLineCnt ;
			if ( cy < 0 )
			cy = 0 ;
			keepX ( ) ;
		}

		void moveRight ( ) {
			keepx = -1 ;
			cx += 1 ;
			if ( ptSelection . isRectSelecting ( ) ) {
				if ( cx > pageData . roLines . getline ( cy ) . length ( ) )
				ptEdit . setLength ( cy , cx ) ;
			} else if ( cx > pageData . roLines . getline ( cy ) . length ( ) && cy < pageData . roLines . getLinesize ( ) - 1 ) {
				cy += 1 ;
				cx = 0 ;
			}
		}

		void moveRightWord ( ) {
			keepx = -1 ;
			CharSequence line = pageData . roLines . getline ( cy ) ;
			char ch1 = U . charAtWhenMove ( line , cx ) ;
			cx = Math . min ( line . length ( ) , cx + 1 ) ;
			while ( U . isSkipChar ( U . charAtWhenMove ( line , cx ) , ch1 ) ) {
				cx = Math . min ( line . length ( ) , cx + 1 ) ;
				if ( cx >= line . length ( ) )
				if ( cy >= pageData . roLines . getLinesize ( ) - 1 )
				break ;
				else {
					cy ++ ;
					line = pageData . roLines . getline ( cy ) ;
					cx = 0 ;
				}
			}
		}

		void moveToPair ( ) {
			keepx = -1 ;
			// move cursor between (){}[]<> pair
			if ( cx - 1 < pageData . roLines . getline ( cy ) . length ( ) && cx - 1 >= 0 ) {
				char c = pageData . roLines . getline ( cy ) . charAt ( cx - 1 ) ;
				String pair = "(){}[]<>" ;
				int p1 = pair . indexOf ( c ) ;
				if ( p1 >= 0 )
				if ( p1 % 2 == 0 )
				PlainPage . this . ui . pairMarker . moveToPairMark ( cx - 1 , cy , pair . charAt ( p1 + 1 ) , c , 1 ) ;
				else
				PlainPage . this . ui . pairMarker . moveToPairMark ( cx - 1 , cy , pair . charAt ( p1 - 1 ) , c , -1 ) ;
			}
		}

		void moveUp ( ) {
			cy -= 1 ;
			if ( cy < 0 )
			cy = 0 ;
			keepX ( ) ;
		}

		void scroll ( int amount ) {
			sy += amount ;
			if ( sy >= pageData . roLines . getLinesize ( ) )
			sy = pageData . roLines . getLinesize ( ) - 1 ;
			if ( sy < 0 )
			sy = 0 ;
			repaint ( ) ;
		}

		void scrollHorizon ( int amount ) {
			sx += amount ;
			if ( sx < 0 )
			sx = 0 ;
			repaint ( ) ;
		}

		void setSafePos ( int x , int y ) {
			cy = U . between ( y , 0 , pageData . roLines . getLinesize ( ) - 1 ) ;
			cx = U . between ( x , 0 , pageData . roLines . getline ( cy ) . length ( ) ) ;
		}

		public void doMoveUpLangLevel ( ) {
			PlainPage . this . ui . pairMarker . moveToPairMark ( cx - 1 , cy , '{' , '}' , -1 ) ;
		}
	}

	public class EasyEdit {
		public synchronized void append ( String s ) {
			cy = pageData . roLines . getLinesize ( ) - 1 ;
			cx = pageData . roLines . getline ( cy ) . length ( ) ;
			insertString ( s ) ;
		}

		public void deleteLine ( int cy ) {
			deleteLineRange ( cy , cy + 1 ) ;
		}

		public void deleteLineRange ( int start , int end ) {
			pageData . editRec . deleteLines ( start , end ) ;
		}

		public void deleteRect ( Rectangle r ) {
			int x1 = r . x ;
			int y1 = r . y ;
			int x2 = r . width ;
			int y2 = r . height ;
			if ( x1 == x2 && y1 == y2 )
			return ;
			if ( rectSelectMode ) {
				for ( int i = y1 ; i <= y2 ; i ++ )
				pageData . editRec . deleteInLine ( i , x1 , x2 ) ;
				selectstartx = x1 ;
				selectstopx = x1 ;
			} else if ( y1 == y2 && x1 < x2 )
			pageData . editRec . deleteInLine ( y1 , x1 , x2 ) ;
			else if ( y1 < y2 ) {
				pageData . editRec . deleteInLine ( y1 , x1 , Integer . MAX_VALUE ) ;
				pageData . editRec . deleteInLine ( y2 , 0 , x2 ) ;
				deleteLineRange ( y1 + 1 , y2 ) ;
				pageData . editRec . mergeLine ( y1 ) ;
			}
			cx = x1 ;
			cy = y1 ;
			if ( y2 - y1 > 400 )
			U . gc ( ) ;
			adjustCursor ( ) ;
		}

		public void deleteSpace ( ) {
			// CharSequence line = pageData.roLines.getline(cy);
			int x0 = cx , y0 = cy ;
			cursor . moveRightWord ( ) ;
			int x2 = cx , y2 = cy ;
			deleteRect ( new Rectangle ( x0 , y0 , x2 , y2 ) ) ;
		}

		public void deleteSpaceBack ( ) {
			// CharSequence line = pageData.roLines.getline(cy);
			int x0 = cx , y0 = cy ;
			cursor . moveLeftWord ( ) ;
			int x2 = cx , y2 = cy ;
			deleteRect ( new Rectangle ( x2 , y2 , x0 , y0 ) ) ;
		}

		String getIndent ( String s ) {
			int p = 0 ;
			while ( p < s . length ( ) && ( s . charAt ( p ) == ' ' || s . charAt ( p ) == '\t' ) )
			p += 1 ;
			return s . substring ( 0 , p ) ;
		}

		public void insert ( char ch ) {
			// Fix cy here! ?
			if ( cy < 0 )
			cy = 0 ;

			if ( ch == KeyEvent . VK_ENTER ) {
				if ( ptSelection . isSelected ( ) )
				deleteRect ( ptSelection . getSelectRect ( ) ) ;
				CharSequence sb = pageData . roLines . getline ( cy ) ;
				String indent = getIndent ( sb . toString ( ) ) ;
				CharSequence s = sb . subSequence ( cx , sb . length ( ) ) ;
				pageData . editRec . insertEmptyLine ( cy + 1 ) ;
				pageData . editRec . insertInLine ( cy + 1 , 0 , indent + U . trimLeft ( s ) ) ;
				pageData . editRec . deleteInLine ( cy , cx , Integer . MAX_VALUE ) ;
				cy += 1 ;
				cx = indent . length ( ) ;
			} else if ( ch == KeyEvent . VK_BACK_SPACE ) {
				if ( ptSelection . isSelected ( ) )
				deleteRect ( ptSelection . getSelectRect ( ) ) ;
				else if ( rectSelectMode ) {
					if ( cx > 0 ) {
						Rectangle r = ptSelection . getSelectRect ( ) ;
						for ( int i = r . y ; i <= r . height ; i ++ )
						pageData . editRec . deleteInLine ( i , cx - 1 , cx ) ;
						cx -- ;
						selectstartx = cx ;
						selectstopx = cx ;
					}
				} else if ( cx > 0 ) {
					pageData . editRec . deleteInLine ( cy , cx - 1 , cx ) ;
					cx -= 1 ;
				} else if ( cy > 0 ) {
					cx = pageData . roLines . getline ( cy - 1 ) . length ( ) ;
					pageData . editRec . mergeLine ( cy - 1 ) ;
					cy -= 1 ;
				}
			} else if ( ch == KeyEvent . VK_DELETE ) {
				if ( ptSelection . isSelected ( ) )
				deleteRect ( ptSelection . getSelectRect ( ) ) ;
				else if ( rectSelectMode ) {
					Rectangle r = ptSelection . getSelectRect ( ) ;
					for ( int i = r . y ; i <= r . height ; i ++ )
					pageData . editRec . deleteInLine ( i , cx , cx + 1 ) ;
					selectstartx = cx ;
					selectstopx = cx ;
				} else if ( cx < pageData . roLines . getline ( cy ) . length ( ) )
				pageData . editRec . deleteInLine ( cy , cx , cx + 1 ) ;
				else if ( cy < pageData . roLines . getLinesize ( ) - 1 )
				pageData . editRec . mergeLine ( cy ) ;
			} else if ( ch == KeyEvent . VK_ESCAPE )
			ptSelection . cancelSelect ( ) ;
			else {
				if ( ptSelection . isSelected ( ) )
				deleteRect ( ptSelection . getSelectRect ( ) ) ;
				if ( rectSelectMode ) {
					Rectangle r = ptSelection . getSelectRect ( ) ;
					for ( int i = r . y ; i <= r . height ; i ++ )
					pageData . editRec . insertInLine ( i , cx , "" + ch ) ;
					cx += 1 ;
					selectstartx = cx ;
					selectstopx = cx ;
				} else {
					pageData . editRec . insertInLine ( cy , cx , "" + ch ) ;
					cx += 1 ;
				}
			}
			focusCursor ( ) ;
			if ( ! rectSelectMode )
			ptSelection . cancelSelect ( ) ;
			repaint ( ) ;
		}

		public void insertString ( List < CharSequence > ss , boolean userInput ) {
			// Fix cy here! ?
			if ( cy < 0 )
			cy = 0 ;

			if ( ptSelection . isSelected ( ) )
			ptEdit . deleteRect ( ptSelection . getSelectRect ( ) ) ;
			int len = ss . size ( ) ;
			if ( rectSelectMode ) {
				Rectangle rect = ptSelection . getSelectRect ( ) ;
				int pi = 0 ;
				for ( int iy = rect . y ; iy <= rect . height ; iy ++ ) {
					CharSequence s1 = ss . get ( pi ) ;
					pageData . editRec . insertInLine ( iy , cx , s1 ) ;
					pi ++ ;
					if ( pi >= len )
					pi = 0 ;
				}
				if ( len == 1 ) {
					selectstartx += ss . get ( 0 ) . length ( ) ;
					selectstopx += ss . get ( 0 ) . length ( ) ;
					cx += ss . get ( 0 ) . length ( ) ;
					savingFromSelectionCancel = true ;
				}
			} else {
				if ( len == 1 ) {
					pageData . editRec . insertInLine ( cy , cx , ss . get ( 0 ) ) ;
					cx += ss . get ( 0 ) . length ( ) ;
				} else {
					CharSequence rem = pageData . roLines . getInLine ( cy , cx , Integer . MAX_VALUE ) ;
					pageData . editRec . deleteInLine ( cy , cx , Integer . MAX_VALUE ) ;
					pageData . editRec . insertInLine ( cy , cx , ss . get ( 0 ) ) ;
					for ( int i = 1 ; i < len ; i ++ ) {
						pageData . editRec . insertEmptyLine ( cy + i ) ;
						pageData . editRec . insertInLine ( cy + i , 0 , ss . get ( i ) ) ;
					}
					cy += len - 1 ;
					cx = ss . get ( len - 1 ) . length ( ) ;
					pageData . editRec . insertInLine ( cy , cx , rem ) ;
				}
				ptSelection . cancelSelect ( ) ;
			}
			if ( len >= 5 && pageData . comment == null ) {
				pageData . isCommentChecked = true ;
				U . startDaemonThread ( new Thread ( ) {
						@ Override
						public void run ( ) {
							U . guessComment ( PlainPage . this ) ;
						}
					} ) ;
			}
			focusCursor ( ) ;
		}

		public void insertString ( String s ) {
			insertString ( s , false ) ;
		}

		public void insertString ( String s , boolean userInput ) {
			insertString ( U . removeTailR ( U . split ( s , U . N ) ) , userInput ) ;
		}

		public void moveLineLeft ( int cy ) {
			String s = pageData . roLines . getline ( cy ) . toString ( ) ;
			if ( s . length ( ) > 0 && ( s . charAt ( 0 ) == '\t' || s . charAt ( 0 ) == ' ' ) )
			pageData . editRec . deleteInLine ( cy , 0 , 1 ) ;
			cx -= 1 ;
			if ( cx < 0 )
			cx = 0 ;
		}

		public void moveLineRight ( int cy ) {
			pageData . editRec . insertInLine ( cy , 0 , "\t" ) ;
			cx += 1 ;
		}

		public void moveRectLeft ( int from , int to ) {
			for ( int i = from ; i <= to ; i ++ )
			moveLineLeft ( i ) ;
		}

		public void moveRectRight ( int from , int to ) {
			for ( int i = from ; i <= to ; i ++ )
			moveLineRight ( i ) ;
		}

		public void setLength ( int cy , int cx ) {
			int oldLen = pageData . roLines . getline ( cy ) . length ( ) ;
			if ( cx - oldLen > 0 )
			pageData . editRec . insertInLine ( cy , oldLen , U . spaces ( cx - oldLen ) ) ;
		}

		public void wrapLines ( int cx ) throws Exception {
			int lineLen = 0 ;
			{
				int len = 0 ;
				CharSequence sb = pageData . roLines . getInLine ( cy , 0 , cx ) ;
				for ( int i = 0 ; i < sb . length ( ) ; i ++ )
				len += ( sb . charAt ( i ) > 255 ) ? 2 : 1 ;
				lineLen = Math . max ( 10 , len ) ;
			}
			ui . message ( "wrapLine at " + lineLen ) ;
			if ( ptSelection . isSelected ( ) )
			ptSelection . cancelSelect ( ) ;
			List < CharSequence > newtext = new ArrayList < CharSequence > ( ) ;
			for ( int y = 0 ; y < pageData . roLines . getLinesize ( ) ; y ++ )
			if ( pageData . lines . get ( y ) . length ( ) * 2 > lineLen ) {
				int len = 0 ;
				CharSequence sb = pageData . roLines . getline ( y ) ;
				int start = 0 ;
				for ( int i = 0 ; i < sb . length ( ) ; i ++ ) {
					len += ( sb . charAt ( i ) > 255 ) ? 2 : 1 ;
					if ( len >= lineLen ) {
						newtext . add ( sb . subSequence ( start , i + 1 ) . toString ( ) ) ;
						start = i + 1 ;
						len = 0 ;
					}
				}
				if ( start < sb . length ( ) )
				newtext . add ( sb . subSequence ( start , sb . length ( ) ) . toString ( ) ) ;
			} else
			newtext . add ( pageData . lines . get ( y ) . toString ( ) ) ;
			String title = "wrapped " + pageData . title + " #" + U . randomID ( ) ;
			PlainPage p2 = new PlainPage ( uiComp , PageData . fromTitle ( title ) , PlainPage . this ) ;
			p2 . pageData . resetLines ( newtext ) ;
		}
	}

	public class Paint {
		/* not used yet */
		@ Deprecated
		public boolean shrinkWord ;

		class PairMark {
			void markBox ( Graphics2D g2 , int x , int y ) {
				if ( y >= sy && y <= sy + showLineCnt && x >= sx ) {
					int mw = getMaxW ( ) ;
					CharSequence sb = pageData . roLines . getline ( y ) ;
					int w1 = drawLineOrTest ( g2 , fontList , y , -1 , false , null , false , mw , x - sx ) ;
					String c = sb . subSequence ( x , x + 1 ) . toString ( ) ;
					int w2 = g2 . getFontMetrics ( ) . stringWidth ( c ) ;
					int h = lineHeight + lineGap ;
					g2 . setColor ( Color . WHITE ) ;
					g2 . drawRect ( w1 - 1 , ( y - sy ) * h - 1 , w2 , h ) ;
					g2 . setColor ( colorNormal ) ;
					g2 . drawRect ( w1 , ( y - sy ) * h , w2 , h ) ;
					U . drawString ( g2 , fontList , c , w1 , lineHeight + ( y - sy ) * h , mw ) ;
				}
			}

			void markGutLine ( Graphics2D g2 , int y1 , int y2 ) {
				if ( y1 > y2 ) {
					int t = y1 ;
					y1 = y2 ;
					y2 = t ;
				}
				int o1 = y1 , o2 = y2 ;
				y1 = Math . min ( Math . max ( y1 , sy ) , sy + showLineCnt ) ;
				y2 = Math . min ( Math . max ( y2 , sy ) , sy + showLineCnt ) ;
				int scy1 = 5 + ( y1 - sy ) * ( lineHeight + lineGap ) ;
				int scy2 = -8 + ( y2 + 1 - sy ) * ( lineHeight + lineGap ) ;
				g2 . setColor ( colorGutMark1 ) ;
				g2 . drawLine ( -6 , scy1 - 1 , -6 , scy2 - 1 ) ;
				if ( o1 == y1 ) {
					g2 . setColor ( colorGutMark1 ) ;
					g2 . drawLine ( -6 , scy1 - 1 , -1 , scy1 - 1 ) ;
				}
				if ( o2 == y2 ) {
					g2 . setColor ( colorGutMark1 ) ;
					g2 . drawLine ( -6 , scy2 - 1 , -1 , scy2 - 1 ) ;
				}
				g2 . setColor ( colorGutMark2 ) ;
				g2 . drawLine ( -5 , scy1 , -5 , scy2 ) ;
				if ( o1 == y1 ) {
					g2 . setColor ( colorGutMark2 ) ;
					g2 . drawLine ( -5 , scy1 , 0 , scy1 ) ;
				}
				if ( o2 == y2 ) {
					g2 . setColor ( colorGutMark2 ) ;
					g2 . drawLine ( -5 , scy2 , 0 , scy2 ) ;
				}
			}

			void findchar ( PlainPage page , char ch , int inc , int [ ] c1 , char chx ) {
				int cx1 = c1 [ 0 ] ;
				int cy1 = c1 [ 1 ] ;
				CharSequence csb = page . pageData . roLines . getline ( cy1 ) ;
				int lv = 1 ;
				while ( true )
				if ( inc == -1 ) {
					cx1 -- ;
					if ( cx1 < 0 ) {
						cy1 -- ;
						if ( cy1 < 0 ) {
							c1 [ 0 ] = -1 ;
							c1 [ 1 ] = -1 ;
							return ;
						} else {
							csb = page . pageData . roLines . getline ( cy1 ) ;
							cx1 = csb . length ( ) - 1 ;
							if ( cx1 < 0 )
							continue ;
						}
					}
					char ch2 = csb . charAt ( cx1 ) ;
					if ( ch2 == chx )
					lv ++ ;
					else if ( ch2 == ch ) {
						lv -- ;
						if ( lv == 0 ) {
							c1 [ 0 ] = cx1 ;
							c1 [ 1 ] = cy1 ;
							return ;
						}
					}
				} else {
					cx1 ++ ;
					if ( cx1 >= csb . length ( ) ) {
						cy1 ++ ;
						if ( cy1 >= page . pageData . roLines . getLinesize ( ) ) {
							c1 [ 0 ] = -1 ;
							c1 [ 1 ] = -1 ;
							return ;
						} else {
							csb = page . pageData . roLines . getline ( cy1 ) ;
							cx1 = 0 ;
							if ( cx1 >= csb . length ( ) )
							continue ;
						}
					}
					char ch2 = csb . charAt ( cx1 ) ;
					if ( ch2 == chx )
					lv ++ ;
					else if ( ch2 == ch ) {
						lv -- ;
						if ( lv == 0 ) {
							c1 [ 0 ] = cx1 ;
							c1 [ 1 ] = cy1 ;
							return ;
						}
					}
				}
			}

			void moveToPairMark ( int cx2 , int cy2 , char ch , char ch2 , int inc ) {
				int [ ] c1 = new int [ ] { cx2 , cy2 } ;
				findchar ( PlainPage . this , ch , inc , c1 , ch2 ) ;
				if ( c1 [ 0 ] >= 0 ) { // found
					cx = c1 [ 0 ] + 1 ;
					int delta = Math . abs ( cy - c1 [ 1 ] ) ;
					if ( delta >= 10 )
					ui . message ( String . format ( "moved across %,d lines" , delta ) ) ;
					cy = c1 [ 1 ] ;
					focusCursor ( ) ;
				}
			}

			void pairMark ( Graphics2D g2 , int cx2 , int cy2 , char ch , char ch2 , int inc ) {
				int [ ] c1 = new int [ ] { cx2 , cy2 } ;
				findchar ( PlainPage . this , ch , inc , c1 , ch2 ) ;
				if ( c1 [ 0 ] >= 0 ) { // found
					markBox ( g2 , cx2 , cy2 ) ;
					markBox ( g2 , c1 [ 0 ] , c1 [ 1 ] ) ;
					if ( cy2 != c1 [ 1 ] )
					markGutLine ( g2 , cy2 , c1 [ 1 ] ) ;
				}
			}
		}

		BufferedImage aboutImg ;
		boolean aboutOn ;
		int aboutY ;

		boolean closed = false ;
		Color colorBg , colorComment , colorComment2 , colorCurrentLineBg , colorDigit , colorGutLine , colorGutNumber ,
		colorKeyword , colorGutMark1 , colorGutMark2 , colorReturnMark ;
		int colorMode ;
		/**
		 * 0:white mode 1: black mode 2: blue mode * 1 bg, 2 normal, 3 keyword, 4 digit,
		 * 5 comment, 6 gutNumber, 7 gutLine, 8 currentLineBg, 9 comment2
		 */
		int [ ] [ ] ColorModes = null ;
		Color colorNormal = Color . BLACK ;

		PairMark pairMarker = new PairMark ( ) ;
		CommandPanelPaint cp = new CommandPanelPaint ( PlainPage . this ) ;
		Dimension dim ;

		int getMaxW ( ) {
			return dim . width - gutterWidth ;
		}

		int getMaxW2 ( ) {
			return ( int ) ( ( dim . width - gutterWidth ) / scalev ) ;
		}

		final int gutterWidth = 40 ;
		int lineGap = 5 ;
		int lineHeight ;
		long MSG_VANISH_TIME = 3000 ;
		List < Object [ ] > msgs = new ArrayList < Object [ ] > ( ) ;
		boolean noise = false ;

		int noisesleep = 500 ;

		float scalev = 1 ;

		private int nextXToolBar ;
		private boolean fpsOn = false ;
		private boolean inComment ;
		private String commentClose ;
		private String commentStart ;

		Paint ( ) {
			try {
				U . TAB_WIDTH = Conf . readTabWidth ( ) ;
				int cm = Conf . getDefaultColorMode ( ) ;
				applyColorMode ( cm ) ;
			} catch ( IOException e ) {
				e . printStackTrace ( ) ;
			}
		}

		public void applyColorMode ( int i ) throws IOException {
			if ( ColorModes == null )
			ColorModes = Conf . loadColorModes ( ) ;

			if ( i >= ColorModes . length )
			i = 0 ;
			colorMode = i ;
			int [ ] cm = ColorModes [ i ] ;
			colorBg = new Color ( cm [ 0 ] ) ;
			colorNormal = new Color ( cm [ 1 ] ) ;
			colorKeyword = new Color ( cm [ 2 ] ) ;
			colorDigit = new Color ( cm [ 3 ] ) ;
			colorComment = new Color ( cm [ 4 ] ) ;
			colorGutNumber = new Color ( cm [ 5 ] ) ;
			colorGutLine = new Color ( cm [ 6 ] ) ;
			colorCurrentLineBg = new Color ( cm [ 7 ] ) ;
			colorComment2 = new Color ( cm [ 8 ] ) ;
			colorGutMark1 = new Color ( cm [ 9 ] ) ;
			colorGutMark2 = new Color ( cm [ 10 ] ) ;
			colorReturnMark = new Color ( cm [ 11 ] ) ;
		}

		void drawGutter ( Graphics2D g2 ) {
			if ( scalev < 1 ) {
				Graphics2D g3 = ( Graphics2D ) g2 . create ( ) ;
				g3 . scale ( scalev , scalev ) ;
				g3 . setColor ( colorGutNumber ) ;
				for ( int i = 0 ; i < showLineCnt ; i ++ ) {
					if ( sy + i + 1 > pageData . roLines . getLinesize ( ) )
					break ;
					U . drawStringShrink ( g3 , fontList , "" + ( sy + i + 1 ) , 0 , ( lineHeight + ( lineHeight + lineGap ) * i ) ,
						gutterWidth / scalev ) ;
				}
				g3 . dispose ( ) ;
			} else { // zoom out not scale gutter font
				g2 . setColor ( colorGutNumber ) ;
				for ( int i = 0 ; i < showLineCnt ; i ++ ) {
					if ( sy + i + 1 > pageData . roLines . getLinesize ( ) )
					break ;
					U . drawStringShrink ( g2 , fontList , "" + ( sy + i + 1 ) , 0 ,
						( int ) ( scalev * ( lineHeight + ( lineHeight + lineGap ) * i ) ) , gutterWidth ) ;
				}
			}
		}

		void drawNextToolbarText ( Graphics2D g2 , String s ) {
			g2 . setColor ( colorGutMark2 ) ;
			nextXToolBar += 10 + U . drawString ( g2 , fontList , s , 10 + nextXToolBar , lineHeight , getMaxW ( ) ) ;
		}

		void drawReturn ( Graphics2D g2 , int w , int py ) {
			g2 . setColor ( colorReturnMark ) ;
			g2 . drawLine ( w , py - lineHeight + fontList . getlineHeight ( ) , w + 3 ,
				py - lineHeight + fontList . getlineHeight ( ) ) ;
		}

		void drawSelect ( Graphics2D g2 , int y1 , int x1 , int x2 ) {
			int scry = y1 - sy ;
			if ( scry < showLineCnt ) {
				CharSequence s = pageData . roLines . getline ( y1 ) ;
				if ( sx > s . length ( ) )
				return ;
				x1 = Math . min ( x1 , s . length ( ) ) ;
				boolean full = x2 > s . length ( ) ;
				x2 = Math . min ( x2 , s . length ( ) ) ;
				int mw2 = getMaxW2 ( ) ;
				if ( x1 == x2 ) {
					int w1 = x1 <= sx ? 0 : drawLineOrTest ( g2 , fontList , y1 , -1 , y1 == cy , null , false , mw2 , x1 - sx ) ;
					g2 . fillRect ( w1 , scry * ( lineHeight + lineGap ) , 3 , lineHeight + lineGap ) ;
				} else {
					int w1 = x1 <= sx ? 0 : drawLineOrTest ( g2 , fontList , y1 , -1 , y1 == cy , null , false , mw2 , x1 - sx ) ;
					int w2 = full ? getMaxW2 ( )
					: ( x2 <= sx ? 0
						: drawLineOrTest ( g2 , fontList , y1 , -1 , y1 == cy , null , false , mw2 , x2 - sx ) ) ;
					g2 . fillRect ( w1 , scry * ( lineHeight + lineGap ) , ( w2 - w1 ) , lineHeight + lineGap ) ;
				}
			}
		}

		void drawSelectLine ( Graphics2D g2 , int y1 , int y2 ) {
			int scry = U . between ( y1 - sy , 0 , showLineCnt ) ;
			int scry2 = U . between ( y2 - sy , 0 , showLineCnt ) ;
			if ( y1 < y2 )
			g2 . fillRect ( 0 , scry * ( lineHeight + lineGap ) , getMaxW2 ( ) , ( lineHeight + lineGap ) * ( scry2 - scry ) ) ;
		}

		private void drawSelfDispMessages ( Graphics2D g ) {
			long now = System . currentTimeMillis ( ) ;
			for ( int i = 0 ; i < msgs . size ( ) ; i ++ ) {
				Object [ ] row = msgs . get ( i ) ;
				long disapear = ( Long ) row [ 1 ] ;
				if ( disapear < now ) {
					msgs . remove ( i ) ;
					i -- ;
				}
			}
			if ( ! msgs . isEmpty ( ) ) {
				// System.out.println("msgs:"+msgs.size());
				int w = U . maxWidth ( msgs , g , fontList , getMaxW ( ) ) + 100 ;
				int h = 30 * msgs . size ( ) + 60 ;
				g . setXORMode ( Color . BLACK ) ;
				g . setPaintMode ( ) ;
				g . setColor ( Color . decode ( "0xFFCCFF" ) ) ;
				g . fillRoundRect ( ( dim . width - w ) / 2 , ( dim . height - h ) / 2 , w , h , 3 , 3 ) ;
				g . setColor ( Color . BLACK ) ;
				int maxw = getMaxW ( ) ;
				for ( int i = 0 ; i < msgs . size ( ) ; i ++ ) {
					Object [ ] row = msgs . get ( i ) ;
					int w1 = ( Integer ) row [ 2 ] ;
					U . drawString ( g , fontList , row [ 0 ] . toString ( ) , ( dim . width - w1 ) / 2 ,
						( 10 + dim . height / 2 + 30 * ( i - msgs . size ( ) / 2 ) ) , maxw ) ;
				}
			}
		}

		int drawLineOrTest ( Graphics2D g , FontList fonts , int cy , int y , boolean isCurrentLine , int [ ] outDrawCharCnt ,
			boolean isRealDraw , int maxw , int maxChar ) {
			if ( maxChar == 0 )
			return 0 ;
			CharSequence sb = pageData . roLines . getline ( cy ) ;
			CharSequence s = U . subs ( sb , sx , sx + maxw / 4 ) ;
			/* guess 4 pixel per char is min */
			if ( maxChar > s . length ( ) )
			maxChar = -1 ;
			// width per char
			int [ ] ch = ( maxChar > 0 ) ? new int [ ] { maxChar } : null ;
			int x = 0 ;
			int w ;
			if ( inComment ) {
				int p1 = FindAndReplace . indexOfSeq ( s , commentClose , 0 , false ) ;
				if ( p1 >= 0 ) {
					inComment = false ;
					CharSequence s1 = s . subSequence ( 0 , p1 + commentClose . length ( ) ) ;
					CharSequence s2 = s . subSequence ( p1 + commentClose . length ( ) , s . length ( ) ) ;
					int w1 = drawText ( g , fonts , s1 , x , y , true , isCurrentLine , outDrawCharCnt , isRealDraw , maxw , ch ) ;
					if ( watchChar ( ch ) )
					return w1 ;
					w = w1 + drawText ( g , fonts , s2 , x + w1 , y , false , isCurrentLine , outDrawCharCnt , isRealDraw , maxw ,
						ch ) ;
				} else
				w = drawText ( g , fonts , s , x , y , true , isCurrentLine , outDrawCharCnt , isRealDraw , maxw , ch ) ;
			} else {
				int commentPos = getCommentPos ( s ) ;
				if ( commentPos >= 0 ) {
					CharSequence s1 = s . subSequence ( 0 , commentPos ) ;
					CharSequence s2 = s . subSequence ( commentPos , s . length ( ) ) ;
					if ( inComment ) {
						int p1 = FindAndReplace . indexOfSeq ( s2 , commentClose , commentStart . length ( ) , false ) ;
						if ( p1 >= 0 ) {
							CharSequence s2a = s2 . subSequence ( 0 , p1 + commentClose . length ( ) ) ;
							CharSequence s2b = s2 . subSequence ( p1 + commentClose . length ( ) , s2 . length ( ) ) ;
							int w1 = drawText ( g , fonts , s1 , x , y , false , isCurrentLine , outDrawCharCnt , isRealDraw ,
								maxw , ch ) ;
							if ( watchChar ( ch ) )
							return w1 ;
							int w2 = w1 + drawText ( g , fonts , s2a , x + w1 , y , true , isCurrentLine , outDrawCharCnt ,
								isRealDraw , maxw , ch ) ;
							if ( watchChar ( ch ) )
							return w2 ;
							w = w2 + drawText ( g , fonts , s2b , x + w2 , y , false , isCurrentLine , outDrawCharCnt ,
								isRealDraw , maxw , ch ) ;
							inComment = false ;
						} else {
							int w1 = drawText ( g , fonts , s1 , x , y , false , isCurrentLine , outDrawCharCnt , isRealDraw ,
								maxw , ch ) ;
							if ( watchChar ( ch ) )
							return w1 ;
							w = w1 + drawText ( g , fonts , s2 , x + w1 , y , true , isCurrentLine , outDrawCharCnt , isRealDraw ,
								maxw , ch ) ;
						}
					} else {
						int w1 = drawText ( g , fonts , s1 , x , y , false , isCurrentLine , outDrawCharCnt , isRealDraw , maxw ,
							ch ) ;
						if ( watchChar ( ch ) )
						return w1 ;
						w = w1 + drawText ( g , fonts , s2 , x + w1 , y , true , isCurrentLine , outDrawCharCnt , isRealDraw ,
							maxw , ch ) ;
					}
				} else
				w = drawText ( g , fonts , s , x , y , false , isCurrentLine , outDrawCharCnt , isRealDraw , maxw , ch ) ;
			}
			return w ;
		}

		private boolean watchChar ( int [ ] ch ) {
			if ( ch == null )
			return false ;
			return ( ch [ 0 ] <= 0 ) ;
		}

		int drawText ( Graphics2D g2 , FontList fonts , CharSequence s , int x , int y , boolean isComment ,
			boolean isCurrentLine , int [ ] outDrawCharCnt , boolean isRealDraw , int maxw , int [ ] ch ) {
			int w = 0 ;
			if ( x + w >= maxw )
			return w ;
			List < CharSequence > s1x = U . splitToken ( s ) ;

			for ( CharSequence s1c : s1x ) {
				String s1 = s1c . toString ( ) ;

				if ( s1 . equals ( "\t" ) ) {
					if ( isRealDraw )
					g2 . drawImage ( U . tabImg , x + w , y - lineHeight , null ) ;
					w += U . TAB_WIDTH ;
					if ( outDrawCharCnt != null && x + w < maxw )
					outDrawCharCnt [ 0 ] += 1 ;
					if ( ch != null )
					ch [ 0 ] -= 1 ;
				} else if ( isComment ) {
					int w1 = U . drawTwoColor ( g2 , fonts , s1 , x + w , y , colorComment , colorComment2 , 1 , maxw , isRealDraw ) ;
					if ( outDrawCharCnt != null )
					if ( x + w + w1 <= maxw )
					outDrawCharCnt [ 0 ] += s1 . length ( ) ;
					else
					outDrawCharCnt [ 0 ] += U . exactRemainChar ( g2 , fonts , s1 , maxw - x - w ) ;
					if ( ch != null )
					if ( ch [ 0 ] >= s1 . length ( ) )
					ch [ 0 ] -= s1 . length ( ) ;
					else {
						w1 = U . exactRemainCharWidth ( g2 , fonts , s1 , ch [ 0 ] ) ;
						ch [ 0 ] = 0 ;
					}

					w += w1 ;
				} else {
					if ( isRealDraw )
					U . getHighLightID ( s1 , g2 , colorKeyword , colorDigit , colorNormal ) ;
					int w1 = U . drawString ( g2 , fontList , s1 , x + w , y , maxw , isRealDraw ) ;
					if ( outDrawCharCnt != null )
					if ( x + w + w1 <= maxw )
					outDrawCharCnt [ 0 ] += s1 . length ( ) ;
					else
					outDrawCharCnt [ 0 ] += U . exactRemainChar ( g2 , fonts , s1 , maxw - x - w ) ;
					if ( ch != null )
					if ( ch [ 0 ] >= s1 . length ( ) )
					ch [ 0 ] -= s1 . length ( ) ;
					else {
						w1 = U . exactRemainCharWidth ( g2 , fonts , s1 , ch [ 0 ] ) ;
						ch [ 0 ] = 0 ;
					}
					w += w1 ;
				}
				if ( ch != null && ch [ 0 ] <= 0 )
				break ;

				if ( x + w >= maxw )
				break ;
			}

			return w ;
		}

		void drawTextLines ( Graphics2D g2 , FontList fonts , int maxw ) {
			int y = sy ;
			int py = lineHeight ;
			for ( int i = 0 ; i < showLineCnt ; i ++ ) {
				if ( y >= pageData . roLines . getLinesize ( ) )
				break ;
				CharSequence sb = pageData . roLines . getline ( y ) ;
				if ( sx < sb . length ( ) ) {
					g2 . setColor ( colorNormal ) ;
					int w = drawLineOrTest ( g2 , fonts , y , py , y == cy , null , true , maxw , -1 ) ;
					// U.strWidth(g2,s,TAB_WIDTH);
					drawReturn ( g2 , w , py ) ;
				} else
				drawReturn ( g2 , 0 , py ) ;
				y += 1 ;
				py += lineHeight + lineGap ;
			}
		}

		void drawToolbar ( Graphics2D g2 ) {
			Ime . ImeInterface ime = Ime . getCurrentIme ( ) ;
			ReadonlyLines lines = pageData . roLines ;
			int lineCnt = pageData . roLines . getLinesize ( ) ;
			int curPer = 0 ;
			if ( lineCnt > 20 )
			curPer = 100 * cy / lineCnt ;
			String s1 = String . format ( "%s %s %s%s L:%s%d X:%d undo:%d%s %s%s%s%s <F1>:Help%s" , //
				( pageData . changedOutside ? " [ChangedOutside!]" : "" ) , //
				( pageData . encoding == null ? "" : pageData . encoding ) , //
				( pageData . lineSep . equals ( "\n" ) ? "U" : "W" ) , //
				( rectSelectMode ? " R " : "" ) , //
				( curPer == 0 ? "" : "" + curPer + "%" ) , lineCnt , ( cx + 1 ) , //
				pageData . history . size ( ) , //
				( ime == null ? "" : " " + ime . getImeName ( ) ) , //
				( pageData . title ) , //
				( readonly ? " ro" : "" ) , pageData . gzip ? " Z" : "" , //
				( pageData . fileLoaded ? "" : " WD:" + workPath ) , //
				( console == null ? "" : " " + lines . getline ( lineCnt - 1 ) ) // console live
			) ;
			g2 . setColor ( colorGutMark1 ) ;
			U . drawString ( g2 , fontList , s1 , 2 , lineHeight + 2 , dim . width ) ;
			g2 . setColor ( colorGutMark2 ) ;
			nextXToolBar = 2 + U . drawString ( g2 , fontList , s1 , 1 , lineHeight + 1 , dim . width ) ;
			if ( msg != null )
			if ( System . currentTimeMillis ( ) - msgtime > MSG_VANISH_TIME )
			msg = null ;
			else {
				int w = U . stringWidth ( g2 , fontList , msg , dim . width ) ;
				g2 . setColor ( new Color ( 0xee6666 ) ) ;
				g2 . fillRect ( dim . width - w , 0 , dim . width , lineHeight + lineGap ) ;
				g2 . setColor ( Color . YELLOW ) ;
				U . drawString ( g2 , fontList , msg , dim . width - w , lineHeight , dim . width ) ;
			}
		}

		private int getCommentPos ( CharSequence s ) {
			if ( pageData . comment == null ) {
				inComment = false ;
				return -1 ;
			}
			for ( String c : pageData . comment ) {
				int p = FindAndReplace . indexOfSeq ( s , c , 0 , false ) ;
				if ( p >= 0 ) {
					if ( "/*" . equals ( c ) ) {
						inComment = true ;
						commentClose = "*/" ;
						commentStart = c ;
					} else if ( "<!--" . equals ( c ) ) {
						inComment = true ;
						commentClose = "-->" ;
						commentStart = c ;
					}
					return p ;
				}
			}
			inComment = false ;
			return -1 ;
		}

		public void message ( final String s ) {
			msg = s ;
			msgtime = System . currentTimeMillis ( ) ;
			repaint ( ) ;
			U . repaintAfter ( MSG_VANISH_TIME , uiComp ) ;
			System . out . println ( s ) ;
		}

		void setNextColorMode ( ) {
			if ( ++ colorMode >= ColorModes . length )
			colorMode = 0 ;
		}

		void xpaint ( Graphics g , Dimension size ) {
			inComment = false ;
			long fpsT1 = System . currentTimeMillis ( ) ;
			Graphics2D g2 = ( Graphics2D ) g ;

			g2 . setRenderingHint ( RenderingHints . KEY_TEXT_ANTIALIASING , uiComp . config . VALUE_TEXT_ANTIALIAS ) ;
			if ( fontList == null )
			fontList = Conf . defaultFontList ;
			lineHeight = fontList . getlineHeight ( ) ;
			this . dim = size ;
			Graphics2D g3 = null ;
			if ( fpsOn )
			g3 = ( Graphics2D ) g2 . create ( ) ;
			boolean needRepaint = false ;

			try {
				if ( ui . cp . showCommandPanel ) {
					cp . xpaint ( ( Graphics2D ) g , size ) ;
					return ;
				}

				if ( ! pageData . isCommentChecked ) { // find comment pattern
					pageData . isCommentChecked = true ;
					U . startDaemonThread ( new Thread ( ) {
							@ Override
							public void run ( ) {
								U . guessComment ( PlainPage . this ) ;
							}
						} ) ;
				}

				// g2.setFont(font);
				showLineCnt = ( int ) Math . ceil ( ( size . height - toolbarHeight ) / ( ( lineHeight + lineGap ) * scalev ) ) ;
				showLineCnt2 = ( int ) Math . floor ( ( size . height - toolbarHeight ) / ( ( lineHeight + lineGap ) * scalev ) ) ;
				if ( showLineCnt2 <= 0 )
				showLineCnt2 = 1 ;
				final int maxw = dim . width - gutterWidth ;
				final int maxw2 = ( int ) ( maxw / scalev ) ;
				{ // change cy if needed
					if ( cy >= pageData . roLines . getLinesize ( ) ) {
						cy = Math . max ( 0 , pageData . roLines . getLinesize ( ) - 1 ) ;
					}
				}

				g2 . setColor ( colorBg ) ;
				g2 . fillRect ( 0 , 0 , size . width , size . height ) ;
				if ( noise )
				U . paintNoise ( g2 , dim ) ;

				// draw toolbar
				drawToolbar ( g2 ) ;

				// draw gutter
				g2 . translate ( 0 , toolbarHeight ) ;
				g2 . setColor ( colorGutLine ) ;
				g2 . drawRect ( gutterWidth , -1 , dim . width - gutterWidth , dim . height - toolbarHeight ) ;

				drawGutter ( g2 ) ;
				g2 . scale ( scalev , scalev ) ;
				// draw text
				int gws = ( int ) ( gutterWidth / scalev ) ;
				g2 . translate ( gws , 0 ) ;
				g2 . setClip ( - gws , 0 , maxw2 + gws , ( int ) ( dim . height / scalev ) ) ;

				Graphics2D g0 = ( Graphics2D ) g2 . create ( ) ;
				g0 . setClip ( 0 , 0 , 0 , 0 ) ; // quick hack to not do real draw
				// change sx if needed
				if ( ptSelection . isRectSelecting ( ) )
				ptEdit . setLength ( cy , cx ) ;
				else
				cx = Math . min ( pageData . roLines . getline ( cy ) . length ( ) , cx ) ;

				adjustSx ( g0 , maxw2 ) ;

				boolean mousePos = false ;
				if ( my > 0 && my < toolbarHeight ) {
				} else if ( my > 0 && mx >= gutterWidth && my >= toolbarHeight ) {
					mx -= gutterWidth ;
					my -= toolbarHeight ;
					mx = ( int ) ( mx / scalev ) ;
					my = ( int ) ( my / scalev ) ;
					cy = sy + my / ( lineHeight + lineGap ) ;
					if ( cy >= pageData . roLines . getLinesize ( ) ) {
						// add a empty line for sake
						if ( ! readonly && pageData . roLines . getline ( pageData . roLines . getLinesize ( ) - 1 ) . length ( ) > 0 ) {
							pageData . editRec . insertEmptyLine ( cy ) ;
						}
						cy = pageData . roLines . getLinesize ( ) - 1 ;
					}
					mousePos = true ;
				} { // highlight current line
					int l1 = cy - sy ;
					if ( l1 >= 0 && l1 < showLineCnt ) {
						g2 . setColor ( colorCurrentLineBg ) ;
						g2 . fillRect ( 0 , l1 * ( lineHeight + lineGap ) , maxw2 , lineHeight + lineGap - 1 ) ;
					}
				}

				if ( mousePos ) { // set cx according to mouse x
					CharSequence sb = pageData . roLines . getline ( cy ) ;
					int [ ] wc = new int [ 1 ] ;
					drawLineOrTest ( g0 , fontList , cy , -1 , true , wc , false , mx , -1 ) ; // test
					cx = sx + wc [ 0 ] ;
					my = 0 ;
					needRepaint = ptSelection . mouseSelection ( sb ) ;
				}

				drawSelectionBackground ( g2 ) ;

				g2 . setColor ( colorNormal ) ;
				drawTextLines ( g2 , fontList , ( int ) ( maxw / scalev ) ) ;

				// (){}[]<> pair marking
				if ( cx - 1 < pageData . roLines . getline ( cy ) . length ( ) && cx - 1 >= 0 ) {
					char c = pageData . roLines . getline ( cy ) . charAt ( cx - 1 ) ;
					String pair = "(){}[]<>" ;
					int p1 = pair . indexOf ( c ) ;
					if ( p1 >= 0 )
					if ( p1 % 2 == 0 )
					pairMarker . pairMark ( g2 , cx - 1 , cy , pair . charAt ( p1 + 1 ) , c , 1 ) ;
					else
					pairMarker . pairMark ( g2 , cx - 1 , cy , pair . charAt ( p1 - 1 ) , c , -1 ) ;
				}

				// draw cursor
				if ( cy >= sy && cy <= sy + showLineCnt ) {
					int w = drawLineOrTest ( g0 , fontList , cy , -1 , true , null , false , maxw2 , cx - sx ) ; // test
					int y0 = ( cy - sy ) * ( lineHeight + lineGap ) ;
					g2 . setXORMode ( new Color ( 0x30f0f0 ) ) ;
					g2 . fillRect ( w , y0 , 2 , lineHeight + 3 ) ;

					Ime . ImeInterface ime = Ime . getCurrentIme ( ) ;

					// draw preedit
					if ( preeditText != null && preeditText . length ( ) > 0 && ime != null && ! ime . longTextMode ( ) ) {
						g2 . setPaintMode ( ) ;
						g2 . setColor ( new Color ( 0xaaaa00 ) ) ;
						int w0 = U . stringWidth ( g2 , fontList , preeditText , maxw2 ) ;
						g2 . fillRect ( w , y0 , w0 + 4 , lineHeight + lineGap ) ;
						g2 . setColor ( new Color ( 0x0000aa ) ) ;
						U . drawString ( g2 , fontList , preeditText , w + 2 , y0 + lineHeight , maxw2 ) ;
					}

					if ( ime != null )
					ime . paint ( g2 , fontList , w , y0 + lineHeight + lineGap , g2 . getClipBounds ( ) ) ;
				}

				if ( aboutOn ) { // about info
					g2 . setPaintMode ( ) ;
					g2 . drawImage ( aboutImg , 0 , aboutY , null ) ;
				}

				drawSelfDispMessages ( g2 ) ;
				g0 . dispose ( ) ;
			} catch ( Throwable th ) {
				th . printStackTrace ( ) ;
				ui . message ( "Bug when xpaint():" + th ) ;
			} finally {
				if ( fpsOn ) {
					long t2 = System . currentTimeMillis ( ) ;
					int v = ( int ) ( t2 - fpsT1 ) ;
					if ( v == 0 )
					drawNextToolbarText ( g3 , "↻∞" ) ;
					else {
						float fps = 1000f / v ;
						if ( fps >= 1 )
						drawNextToolbarText ( g3 , "↻" + ( int ) fps ) ;
						else
						drawNextToolbarText ( g3 , String . format ( "↻%.3f" , fps ) ) ;
					}
					if ( g3 != null )
					g3 . dispose ( ) ;
				}
			}
			if ( needRepaint )
			repaint ( ) ;
		}

		private void adjustSx ( Graphics2D g0 , int maxw2 ) { // TODO
			if ( sx + 6 > cx && sx > 0 )
			sx = Math . max ( 0 , cx - 6 ) ; // scroll left
			else {
				int [ ] wc = new int [ 1 ] ;
				drawLineOrTest ( g0 , fontList , cy , -1 , true , wc , false , maxw2 , -1 ) ; // test
				int q = wc [ 0 ] ;
				CharSequence sb = pageData . roLines . getline ( cy ) ;
				if ( sx + q < sb . length ( ) && ( cx + 6 > sx + q ) ) { // scroll right
					sx = U . between ( cx - q + 6 , 0 , sb . length ( ) - 1 ) ;
				}
			}
		}

		private void drawSelectionBackground ( Graphics2D g ) {
			Graphics2D g2 = ( Graphics2D ) g . create ( ) ;

			TexturePaint tp = getFillImagePaint ( ) ;
			g2 . setPaint ( tp ) ;

			// g2.setColor(colorNormal);
			if ( rectSelectMode ) {
				Rectangle r = ptSelection . getSelectRect ( ) ;
				int x1 = r . x ;
				int y1 = r . y ;
				int x2 = r . width ;
				int y2 = r . height ;
				int start = Math . max ( sy , y1 ) ;
				int end = Math . min ( sy + showLineCnt + 1 , y2 ) ;
				for ( int i = start ; i <= end ; i ++ )
				// g2.setColor(Color.BLUE);
				// g2.setXORMode(new Color(0xf0f030));
				drawSelect ( g2 , i , x1 , x2 ) ;
			} else { // select mode
				Rectangle r = ptSelection . getSelectRect ( ) ;
				int x1 = r . x ;
				int y1 = r . y ;
				int x2 = r . width ;
				int y2 = r . height ;
				if ( y1 == y2 && x1 < x2 )
				// g2.setColor(Color.BLUE);
				// g2.setXORMode(new Color(0xf0f030));
				drawSelect ( g2 , y1 , x1 , x2 ) ;
				else if ( y1 < y2 ) {
					// g2.setColor(Color.BLUE);
					// g2.setXORMode(new Color(0xf0f030));
					drawSelect ( g2 , y1 , x1 , Integer . MAX_VALUE ) ;
					int start = Math . max ( sy , y1 + 1 ) ;
					int end = Math . min ( sy + showLineCnt + 1 , y2 ) ;
					drawSelectLine ( g2 , start , end ) ;
					drawSelect ( g2 , y2 , 0 , x2 ) ;
				}
			}
			g2 . dispose ( ) ;
		}

		private TexturePaint getFillImagePaint ( ) {
			int w = 13 ;
			BufferedImage img = new BufferedImage ( w , w , BufferedImage . TYPE_INT_ARGB ) ;
			Graphics2D g = img . createGraphics ( ) ;
			g . setColor ( colorBg ) ;
			g . fillRect ( 0 , 0 , w , w ) ;
			g . setColor ( dissRed ( colorBg ) ) ;
			int k = w - 1 ;
			int k1 = 0 ;
			int k2 = k - k1 ;
			g . drawLine ( k1 , k1 , k2 , k2 ) ;
			g . drawLine ( k1 , k2 , k2 , k1 ) ;
			g . dispose ( ) ;
			return new TexturePaint ( img , new Rectangle ( 0 , 0 , w , w ) ) ;
		}

		private Color dissRed ( Color c ) {
			int r = c . getRed ( ) ;
			if ( r < 100 )
			r = 220 ;
			else
			r = 30 ;
			return new Color ( r , c . getGreen ( ) , c . getBlue ( ) ) ;
		}
	}

	public class Selection {
		public void cancelSelect ( ) {
			selectstartx = cx ;
			selectstarty = cy ;
			selectstopx = cx ;
			selectstopy = cy ;
		}

		public void copySelected ( ) {
			String s = U . exportString ( getSelected ( ) , pageData . lineSep ) ;
			s = U . removeAsciiZero ( s ) ;
			U . setClipBoard ( s ) ;
			ui . message ( "copied " + s . length ( ) ) ;
		}

		public void cutSelected ( ) {
			copySelected ( ) ;
			ptEdit . deleteRect ( getSelectRect ( ) ) ;
			cancelSelect ( ) ;
		}

		public List < CharSequence > getSelected ( ) {
			return pageData . roLines . getTextInRect ( getSelectRect ( ) , rectSelectMode ) ;
		}

		public Rectangle getSelectRect ( ) {
			int x1 , x2 , y1 , y2 ;
			if ( rectSelectMode ) {
				y1 = selectstopy ;
				y2 = selectstarty ;
				x1 = selectstopx ;
				x2 = selectstartx ;
				if ( y1 > y2 ) {
					int t = y1 ;
					y1 = y2 ;
					y2 = t ;
				}
				if ( x1 > x2 ) {
					int t = x1 ;
					x1 = x2 ;
					x2 = t ;
				}
			} else if ( selectstopy < selectstarty ) {
				y1 = selectstopy ;
				y2 = selectstarty ;
				x1 = selectstopx ;
				x2 = selectstartx ;
			} else {
				y2 = selectstopy ;
				y1 = selectstarty ;
				x2 = selectstopx ;
				x1 = selectstartx ;
				if ( x1 > x2 && y1 == y2 ) {
					x1 = selectstopx ;
					x2 = selectstartx ;
				}
			}
			return new Rectangle ( x1 , y1 , x2 , y2 ) ;
		}

		public boolean isRectSelecting ( ) {
			return mshift && rectSelectMode ;
		}

		public boolean isSelected ( ) {
			Rectangle r = getSelectRect ( ) ;
			int x1 = r . x ;
			int y1 = r . y ;
			int x2 = r . width ;
			int y2 = r . height ;
			if ( rectSelectMode )
			return x1 < x2 ;
			else {
				if ( y1 == y2 && x1 < x2 )
				return true ;
				else if ( y1 < y2 )
				return true ;
				return false ;
			}
		}

		/**
		 *
		 * @param sb
		 * @return mouseSelectedOnLimit, need move screen and repaint
		 */
		public boolean mouseSelection ( CharSequence sb ) {
			if ( mcount == 2 ) {
				int x1 = cx ;
				int x2 = cx ;
				if ( sb . length ( ) > x1 && Character . isJavaIdentifierPart ( sb . charAt ( x1 ) ) )
				while ( x1 > 0 && Character . isJavaIdentifierPart ( sb . charAt ( x1 - 1 ) ) )
				x1 -= 1 ;
				if ( sb . length ( ) > x2 && Character . isJavaIdentifierPart ( sb . charAt ( x2 ) ) )
				while ( x2 < sb . length ( ) - 1 && Character . isJavaIdentifierPart ( sb . charAt ( x2 + 1 ) ) )
				x2 += 1 ;
				selectstartx = x1 ;
				selectstarty = cy ;
				selectstopx = x2 + 1 ;
				selectstopy = cy ;
			} else if ( mcount == 3 ) {
				selectstartx = 0 ;
				selectstarty = cy ;
				selectstopx = sb . length ( ) ;
				selectstopy = cy ;
			} else if ( mshift ) {
				selectstopx = cx ;
				selectstopy = cy ;
				if ( cy == sy && cy > 0 ) {
					sy -- ;
					return true ;
				} else if ( cy >= sy + showLineCnt - 1
					&& sy + 1 + showLineCnt / 2 < pageData . roLines . getLinesize ( ) - 1 ) {
					sy ++ ;
					return true ;
				}
			} else
			cancelSelect ( ) ;
			return false ;
		}

		public void selectAll ( ) {
			selectstartx = 0 ;
			selectstarty = 0 ;
			selectstopy = pageData . roLines . getLinesize ( ) - 1 ;
			if ( selectstopy < 0 )
			selectstopy = 0 ;
			selectstopx = pageData . roLines . getline ( selectstopy ) . length ( ) ;
		}

		public void selectLength ( int x , int y , int length ) {
			cx = x ;
			cy = y ;
			selectstartx = cx ;
			selectstarty = cy ;
			selectstopx = cx + length ;
			selectstopy = cy ;
			adjustCursor ( ) ;
			savingFromSelectionCancel = true ;
		}

		public void selectLine ( ) {
			selectstartx = 0 ;
			selectstarty = cy ;
			selectstopy = cy ;
			if ( selectstopy < 0 )
			selectstopy = 0 ;
			selectstopx = pageData . roLines . getline ( selectstopy ) . length ( ) ;
			copySelected ( ) ;
		}
	}
}
