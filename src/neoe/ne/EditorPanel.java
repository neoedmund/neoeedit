package neoe . ne ;

import java . awt . AWTEvent ;
import java . awt . Cursor ;
import java . awt . Dimension ;
import java . awt . Graphics ;
import java . awt . Point ;
import java . awt . Rectangle ;
import java . awt . event . InputMethodEvent ;
import java . awt . event . InputMethodListener ;
import java . awt . event . KeyEvent ;
import java . awt . event . KeyListener ;
import java . awt . event . MouseEvent ;
import java . awt . event . MouseListener ;
import java . awt . event . MouseMotionListener ;
import java . awt . event . MouseWheelEvent ;
import java . awt . event . MouseWheelListener ;
import java . awt . event . WindowAdapter ;
import java . awt . event . WindowEvent ;
import java . awt . font . TextHitInfo ;
import java . awt . im . InputMethodRequests ;
import java . io . File ;
import java . io . IOException ;
import java . text . AttributedCharacterIterator ;
import java . text . AttributedCharacterIterator . Attribute ;
import java . text . CharacterIterator ;
import java . util . ArrayList ;
import java . util . Arrays ;
import java . util . List ;

import javax . swing . JDesktopPane ;
import javax . swing . JFrame ;
import javax . swing . JInternalFrame ;
import javax . swing . JPanel ;
import javax . swing . RootPaneContainer ;
import javax . swing . SwingUtilities ;
import javax . swing . WindowConstants ;

import neoe . ne . U . LocationHistory ;

public class EditorPanel extends JPanel implements MouseMotionListener , MouseListener , MouseWheelListener , KeyListener {
	private Dimension dim ;

	private Point loc ;

	private int myCursor ;
	/*
	 * a hack to pass param, not work yet
	 */
	boolean newWindow ;

	boolean findAndShowPage ( String title , int line , boolean rec ) throws Exception {
		if ( title == null )
		return false ;

		{ // show image file
			File f = new File ( title ) ;
			if ( f . isFile ( ) && U . isImageFile ( f ) ) {
				new PicView ( ) . show0 ( f , this ) ;
				return true ;
			}
		}
		PlainPage pp = findPage ( title ) ;
		if ( pp != null ) {
			if ( newWindow /* && pp . pageData . fileLoaded */ ) {
				openInNewWindow ( title , line ) ;
				return true ;
			}
			setPage ( pp , rec ) ;
			if ( line > 0 )
			pp . cursor . setSafePos ( 0 , line - 1 ) ;
			pp . adjustCursor ( ) ;
			U . checkChangedOutside ( pp ) ;
			SwingUtilities . invokeLater ( ( ) -> repaint ( ) ) ;
			return true ;
		}

		// can i open the file?
		if ( title . startsWith ( "[" ) ) {
			PageData pd = PageData . dataPool . get ( title ) ;
			// PageData . fromTitle ( title ) ;
			if ( pd != null ) {
				if ( newWindow /* && pp . pageData . fileLoaded */ ) {
					openInNewWindow ( title , line ) ;
					return true ;
				} else {
					PlainPage pp2 = new PlainPage ( this , pd , this . page ) ;
					if ( line > 0 )
					pp2 . cursor . setSafePos ( 0 , line - 1 ) ;
					pp2 . adjustCursor ( ) ;
					U . checkChangedOutside ( pp2 ) ;
					SwingUtilities . invokeLater ( ( ) -> repaint ( ) ) ;
					return true ;
				}
			}
			return false ; // not likely
		}
		File f = new File ( title ) ;
		if ( ! f . isFile ( ) )
		f = new File ( page . workPath , title ) ;
		if ( ! f . isFile ( ) )
		return false ;
		else
		title = f . getCanonicalPath ( ) ;

		if ( newWindow ) {
			openInNewWindow ( title , line ) ;
			return true ;
		}
		PageData pd = PageData . fromFile ( title ) ;
		pp = U . findPageByData ( pageSet , pd ) ;
		if ( pp == null ) {
			pp = new PlainPage ( this , pd , page ) ;
		}
		if ( line > 0 )
		pp . cursor . setSafePos ( 0 , line - 1 ) ;
		pp . adjustCursor ( ) ;
		return true ;
	}

	private PlainPage findPage ( String title ) {
		for ( int i = 0 ; i < pageSet . size ( ) ; i ++ ) {
			PlainPage p = pageSet . get ( i ) ;
			if ( p . pageData . title . equals ( title ) )
			return p ;
		}
		return null ;
	}

	private void openInNewWindow ( String title , int line ) throws Exception {
		EditorPanel ep = new EditorPanel ( this . config ) ;
		ep . openWindow ( ) ;
		PlainPage pp = new PlainPage ( ep , PageData . fromFile ( title ) , page ) ;
		if ( line > 0 )
		pp . cursor . setSafePos ( 0 , line - 1 ) ;
		pp . adjustCursor ( ) ;
	}

	private boolean inRangeOfWindowMove ( MouseEvent evt ) {
		if ( page == null || realJFrame == null )
		return false ;
		int x = evt . getX ( ) ;
		int y = evt . getY ( ) ;
		return x <= page . toolbarHeight && y <= page . toolbarHeight ;
	}

	private boolean inRangeOfWindowResize ( MouseEvent evt ) {
		if ( page == null || realJFrame == null )
		return false ;
		int x = evt . getX ( ) ;
		int y = evt . getY ( ) ;
		Dimension dim0 = getSize ( ) ;
		return x >= dim0 . width - page . toolbarHeight && y >= dim0 . height - page . toolbarHeight ;
	}

	int mx , my ;

	private void startWindowMove ( MouseEvent evt ) {
		mx = evt . getXOnScreen ( ) ;
		my = evt . getYOnScreen ( ) ;
		loc = getLocationOnScreen ( ) ;
		if ( frame != null ) {
			if ( frame instanceof JFrame ) {
				loc = ( ( JFrame ) frame ) . getLocationOnScreen ( ) ;
			} else if ( frame instanceof JInternalFrame ) {
				loc = ( ( JInternalFrame ) frame ) . getLocationOnScreen ( ) ;
			}
		} else {
			loc = new Point ( ) ; // dummy
		}
		inWindowMove = true ;
	}

	private void startWindowResize ( MouseEvent evt ) {
		mx = evt . getXOnScreen ( ) ;
		my = evt . getYOnScreen ( ) ;
		if ( frame != null ) {
			if ( frame instanceof JFrame ) {
				dim = ( ( JFrame ) frame ) . getSize ( ) ;
			} else if ( frame instanceof JInternalFrame ) {
				dim = ( ( JInternalFrame ) frame ) . getSize ( ) ;
			}
		} else {
			dim = new Dimension ( ) ; // dummy
		}
		inWindowResize = true ;
	}

	private void windowMove ( MouseEvent evt ) {
		int x = evt . getXOnScreen ( ) ;
		int y = evt . getYOnScreen ( ) ;
		if ( loc == null ) {
			startWindowMove ( evt ) ;
			return ;
		}
		Point loc2 = new Point ( loc ) ;
		loc2 . x += x - mx ;
		loc2 . y += y - my ;
		if ( frame != null ) {
			if ( frame instanceof JFrame ) {
				( ( JFrame ) frame ) . setLocation ( loc2 ) ;
			} else if ( frame instanceof JInternalFrame ) {
				( ( JInternalFrame ) frame ) . setLocation ( loc2 ) ;
			}
		}
	}

	private void windowResize ( MouseEvent evt ) {
		int x = evt . getXOnScreen ( ) ;
		int y = evt . getYOnScreen ( ) ;
		if ( dim == null ) {
			startWindowResize ( evt ) ;
			return ;
		}
		Dimension dim2 = new Dimension ( dim ) ;
		dim2 . width += x - mx ;
		dim2 . height += y - my ;
		if ( frame != null ) {
			if ( frame instanceof JFrame ) {
				( ( JFrame ) frame ) . setSize ( dim2 ) ;
			} else if ( frame instanceof JInternalFrame ) {
				( ( JInternalFrame ) frame ) . setSize ( dim2 ) ;
			}
		}
	}

	/**
	 * It's only need to be not-null and not actually called?
	 */
	class MyInputMethodRequestsHandler implements InputMethodRequests {
		Rectangle rect = new Rectangle ( 200 , 200 , 0 , 10 ) ;

		@ Override
		public AttributedCharacterIterator cancelLatestCommittedText ( Attribute [ ] attributes ) {
			System . out . println ( "cancelLatestCommittedText=" + Arrays . deepToString ( attributes ) ) ;
			return null ;
		}

		@ Override
		public AttributedCharacterIterator getCommittedText ( int beginIndex , int endIndex , Attribute [ ] attributes ) {
			System . out . printf ( "getCommittedText %d, %d, %s\n" , beginIndex , endIndex , Arrays . deepToString ( attributes ) ) ;
			return null ;
		}

		@ Override
		public int getCommittedTextLength ( ) {
			System . out . println ( "getCommittedTextLength" ) ;
			return 0 ;
		}

		@ Override
		public int getInsertPositionOffset ( ) {
			System . out . println ( "getInsertPositionOffset" ) ;
			return 0 ;
		}

		@ Override
		public TextHitInfo getLocationOffset ( int x , int y ) {
			System . out . println ( "getLocationOffset" ) ;
			return null ;
		}

		@ Override
		public AttributedCharacterIterator getSelectedText ( Attribute [ ] attributes ) {
			System . out . println ( "getSelectedText=" + Arrays . deepToString ( attributes ) ) ;
			return null ;
		}

		@ Override
		public Rectangle getTextLocation ( TextHitInfo offset ) {
			System . out . println ( "getTextLocation" ) ;
			// return rect;
			return null ;
		}
	}

	public static int openedWindows ;

	static final String WINDOW_NAME = "neoeedit " + Version . REV ;

	// CursorHistory ptCh = new CursorHistory();
	public RootPaneContainer frame ;

	PlainPage lastPage ;

	LocationHistory < String > pageHis = new LocationHistory < String > ( ) ;

	public PlainPage page ;

	List < PlainPage > pageSet = new ArrayList < PlainPage > ( ) ;

	EditorPanelConfig config ;

	JDesktopPane desktopPane ;

	JFrame realJFrame ;
	private static final EditorPanelConfig DEFAULT = new EditorPanelConfig ( ) ;

	public EditorPanel ( ) throws Exception {
		this ( DEFAULT ) ;
	}

	public EditorPanel ( EditorPanelConfig config ) throws Exception {
		this . config = config ;
		Main . doinit ( ) ;
		Conf . loadOtherConfig ( config ) ;
		enableEvents ( AWTEvent . KEY_EVENT_MASK | AWTEvent . INPUT_METHOD_EVENT_MASK ) ;
		setBackground ( Conf . getDefaultBgColor ( ) ) ;
		setFocusable ( true ) ;
		addMouseMotionListener ( this ) ;
		addMouseListener ( this ) ;
		addMouseWheelListener ( this ) ;
		addKeyListener ( this ) ;
		addInputMethodListener ( new InputMethodListener ( ) {
				@ Override
				public void caretPositionChanged ( InputMethodEvent event ) {
					System . out . println (
						"if you see this, tell neoeedit's author what system you are using. caretPositionChanged="
						+ event . paramString ( ) ) ;
				}

				@ Override
				public void inputMethodTextChanged ( InputMethodEvent event ) {
					if ( page == null )
					return ;
					AttributedCharacterIterator text = event . getText ( ) ;
					if ( text == null ) {
						page . preedit ( "" , 0 ) ;
						return ;
					}
					StringBuilder textBuffer = new StringBuilder ( ) ;
					int committedCharacterCount = event . getCommittedCharacterCount ( ) ;
					char c = text . first ( ) ;
					while ( c != CharacterIterator . DONE ) {
						textBuffer . append ( c ) ;
						c = text . next ( ) ;
					}
					String textString = textBuffer . toString ( ) ;
					page . preedit ( textString , committedCharacterCount ) ;
				}
			} ) ;
		setOpaque ( false ) ;
		setMyCursor ( Cursor . TEXT_CURSOR ) ;
		setFocusTraversalKeysEnabled ( false ) ;
		PlainPage pp = new PlainPage ( this , PageData . newUntitled ( ) , null ) ;
		pp . ptSelection . selectAll ( ) ;
	}

	void setMyCursor ( int type ) {
		setCursor ( new Cursor ( myCursor = type ) ) ;
	}

	void changeTitle ( ) {
		if ( frame == null )
		return ;
		String tag = " (" + pageSet . size ( ) + ") - " + EditorPanel . WINDOW_NAME + U . suNotice ( ) ;
		String pre = U . TitleName ;
		String title ;
		String fn = page . pageData . title ;
		if ( page . console != null )
		title = pre + "(exec)" + page . console . cmd + tag ;
		else if ( page . pageData . fileLoaded )
		title = pre + new File ( fn ) . getName ( ) + " " + new File ( fn ) . getParent ( ) + tag ;
		else
		title = pre + fn + tag ;
		if ( title != null )
		if ( frame instanceof JFrame )
		( ( JFrame ) frame ) . setTitle ( title ) ;
		else if ( frame instanceof JInternalFrame )
		( ( JInternalFrame ) frame ) . setTitle ( title ) ;
	}

	@ Override
	public void keyPressed ( KeyEvent env ) {
		try {
			page . keyPressed ( env ) ;
		} catch ( Throwable e ) {
			page . ui . message ( "err:" + e ) ;
			e . printStackTrace ( ) ;
		}
	}

	@ Override
	public void keyReleased ( KeyEvent env ) {
		try {
			page . keyReleased ( env ) ;
		} catch ( Throwable e ) {
			page . ui . message ( "err:" + e ) ;
			e . printStackTrace ( ) ;
		}
	}

	@ Override
	public void keyTyped ( KeyEvent env ) {
		try {
			if ( U . hardwareFailWorkaroundFilterOut ( env ) )
			return ;
			page . keyTyped ( env ) ;
		} catch ( Throwable e ) {
			page . ui . message ( "err:" + e ) ;
			e . printStackTrace ( ) ;
		}
	}

	@ Override
	public void mouseClicked ( MouseEvent evt ) {
		try {
			page . mouseClicked ( evt ) ;
			grabFocus ( ) ;
		} catch ( Throwable e ) {
			page . ui . message ( "err:" + e ) ;
			e . printStackTrace ( ) ;
		}
	}

	@ Override
	public void mouseDragged ( MouseEvent env ) {
		try {
			if ( inWindowMove ) {
				windowMove ( env ) ;
				return ;
			}
			if ( inWindowResize ) {
				windowResize ( env ) ;
				return ;
			}
			page . mouseDragged ( env ) ;
		} catch ( Throwable e ) {
			page . ui . message ( "err:" + e ) ;
			e . printStackTrace ( ) ;
		}
	}

	@ Override
	public void mouseEntered ( MouseEvent arg0 ) {
	}

	@ Override
	public void mouseExited ( MouseEvent arg0 ) {
	}

	@ Override
	public void mouseMoved ( MouseEvent evt ) {
		if ( inRangeOfWindowMove ( evt ) ) {
			setMyCursor ( Cursor . MOVE_CURSOR ) ;
			return ;
		}
		if ( inRangeOfWindowResize ( evt ) ) {
			setMyCursor ( Cursor . SE_RESIZE_CURSOR ) ;
			return ;
		}
		if ( myCursor != Cursor . TEXT_CURSOR ) {
			setMyCursor ( Cursor . TEXT_CURSOR ) ;
		}
		try {
			page . mouseMoved ( evt ) ;
		} catch ( Throwable e ) {
			page . ui . message ( "err:" + e ) ;
			e . printStackTrace ( ) ;
		}
	}

	@ Override
	public void mousePressed ( MouseEvent evt ) {
		try {
			if ( inRangeOfWindowMove ( evt ) ) {
				startWindowMove ( evt ) ;
				return ;
			}
			if ( inRangeOfWindowResize ( evt ) ) {
				startWindowResize ( evt ) ;
				return ;
			}
			page . mousePressed ( evt ) ;
		} catch ( Throwable e ) {
			page . ui . message ( "err:" + e ) ;
			e . printStackTrace ( ) ;
		}
	}

	boolean inWindowMove ;
	boolean inWindowResize ;

	public UserFunc userfunc ;

	@ Override
	public void mouseReleased ( MouseEvent arg0 ) {
		inWindowMove = false ;
		inWindowResize = false ;
	}

	@ Override
	public void mouseWheelMoved ( MouseWheelEvent env ) {
		try {
			page . mouseWheelMoved ( env ) ;
		} catch ( Throwable e ) {
			page . ui . message ( "err:" + e ) ;
			e . printStackTrace ( ) ;
		}
	}

	static List < EditorPanel > insts = new ArrayList < > ( ) ;

	public void openWindow ( ) throws IOException {
		if ( frame != null ) // ?
		return ;
		openedWindows ++ ;
		insts . add ( this ) ;
		JFrame f = new JFrame ( EditorPanel . WINDOW_NAME ) ;
		openWindow ( U . e_png , f , f , null ) ;
		installWindowListener ( f ) ;
		// not called, why? f . addFocusListener ( new FocusAdapter ( ) {
		// @ Override
		// public void focusGained ( FocusEvent e ) {
		// System . out . println ( "JFrame.focusGained" ) ;
		// EditorPanel . this . requestFocusInWindow ( ) ; }
		// } ) ;
		grabFocus ( ) ;
	}

	public void openWindow ( String iconname , RootPaneContainer outFrame , JFrame realJFrame , JDesktopPane desktopPane )
	throws IOException {
		frame = outFrame ;
		this . desktopPane = desktopPane ;
		if ( iconname == null )
		iconname = U . e_png ;
		if ( frame instanceof JFrame )
		initJFrame ( iconname , ( JFrame ) frame ) ;
		else if ( frame instanceof JInternalFrame ) {
			JInternalFrame ji = ( JInternalFrame ) frame ;
			ji . add ( this ) ;
		}
		this . realJFrame = realJFrame ;
		changeTitle ( ) ;
		repaint ( ) ;
	}

	public void installWindowListener ( JFrame realJFrame ) {
		realJFrame . addWindowListener ( new WindowAdapter ( ) {
				@ Override
				public void windowActivated ( WindowEvent e ) {
					if ( page != null )
					U . checkChangedOutside ( page ) ;
				}

				@ Override
				public void windowClosed ( WindowEvent e ) {
					System . out . println ( "closed" ) ;
					openedWindows -- ;
					insts . remove ( EditorPanel . this ) ;
					try {
						U . appendEpFileHistory ( EditorPanel . this ) ;
					} catch ( IOException e1 ) {
						e1 . printStackTrace ( ) ;
					}
				}

				@ Override
				public void windowClosing ( WindowEvent e ) {
					// note: history record when file open, don't do when closing
					// StringBuilder sb = new StringBuilder ( ) ;
					// for ( PlainPage pp : pageSet )
					// if ( pp . pageData . fileLoaded )
					// sb . append ( String . format ( "\n%s|%s:" , pp . pageData . title , pp . cy
					// + 1 ) ) ;
					// try {
					// U . saveFileHistorys ( sb . toString ( ) ) ;
					// } catch ( IOException e1 ) {
					// e1 . printStackTrace ( ) ;
					// }
				}
			} ) ;
	}

	private void initJFrame ( String iconname , JFrame frame ) throws IOException {
		if ( iconname != null )
		frame . setIconImage ( U . getAppIcon ( iconname ) ) ;
		frame . setDefaultCloseOperation ( WindowConstants . DISPOSE_ON_CLOSE ) ;
		U . setFrameSize ( frame ) ;
		frame . setTransferHandler ( new U . TH ( this ) ) ;
		frame . add ( this ) ;
		frame . setVisible ( true ) ;
	}

	@ Override
	public void paint ( Graphics g ) {
		try {
			// super . paint ( g ) ;
			if ( page != null )
			page . xpaint ( g , this . getSize ( ) ) ;
		} catch ( Throwable e ) {
			e . printStackTrace ( ) ;
		}
	}

	public EditorPanel setPage ( PlainPage pp , boolean rec ) {
		lastPage = page ;
		page = pp ;
		if ( rec )
		pageHis . add ( U . getLocString ( pp ) , U . getLocString ( lastPage ) ) ;
		changeTitle ( ) ;
		return pp . uiComp ;
	}
}
