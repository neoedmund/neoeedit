package neoe . ne ;

import java . awt . BorderLayout ;
import java . awt . Color ;
import java . awt . Component ;
import java . awt . FlowLayout ;
import java . awt . event . KeyAdapter ;
import java . awt . event . KeyEvent ;
import java . awt . event . KeyListener ;
import java . awt . event . MouseAdapter ;
import java . awt . event . MouseEvent ;
import java . io . IOException ;

import javax . swing . Icon ;
import javax . swing . JButton ;
import javax . swing . JDialog ;
import javax . swing . JInternalFrame ;
import javax . swing . JLabel ;
import javax . swing . JPanel ;
import javax . swing . JTabbedPane ;
import javax . swing . JTextField ;
import javax . swing . SwingUtilities ;
import javax . swing . event . ChangeEvent ;
import javax . swing . event . ChangeListener ;

import neoe . ne . U . SimpleLayout ;

public class Tabs {
	private static int tabCounter ;

	public static JTabbedPane createNewTab ( EditorPanel uiComp ) {
		JTabbedPane tabbedPane = new JTabbedPane ( ) ;
		tabbedPane . setFocusable ( false ) ;
		enableTabMove ( tabbedPane ) ;
		setupTabController ( tabbedPane , uiComp ) ;
		uiComp . setLayout ( new BorderLayout ( ) ) ;
		tabbedPane . add ( getTabName ( ) , uiComp ) ;
		uiComp . tabs = tabbedPane ;
		if ( uiComp . frame != null ) {
			uiComp . frame . getContentPane ( ) . removeAll ( ) ;
			uiComp . frame . getContentPane ( ) . add ( tabbedPane ) ;
			uiComp . frame . revalidate ( ) ;
			uiComp . frame . repaint ( ) ;
		} else if ( uiComp . iframe != null ) {
			uiComp . iframe . getContentPane ( ) . removeAll ( ) ;
			uiComp . iframe . getContentPane ( ) . add ( tabbedPane ) ;
			uiComp . iframe . revalidate ( ) ;
			uiComp . iframe . repaint ( ) ;
		}
		tabbedPane . addChangeListener ( new ChangeListener ( ) {
				@ Override
				public void stateChanged ( ChangeEvent e ) {
					EditorPanel comp = ( EditorPanel ) tabbedPane . getSelectedComponent ( ) ;
					if ( comp == null )
					return ;
					if ( comp . fcb != null ) {
						comp . fcb . focus ( comp ) ;
					}
				}
			} ) ;
		// System . out . println ( "[d]uicomp size=" + uiComp . getSize ( ) ) ;
		return tabbedPane ;
	}

	private static String getTabName ( ) {
		return "Tab" + ( ++ tabCounter ) ;
	}

	static void setupTabController ( JTabbedPane tabbedPane , EditorPanel uiComp ) {
		tabbedPane . addMouseListener ( new MouseAdapter ( ) {
				@ Override
				public void mouseClicked ( MouseEvent e ) {
					if ( e . getClickCount ( ) == 2 && SwingUtilities . isLeftMouseButton ( e ) ) {
						int index = tabbedPane . indexAtLocation ( e . getX ( ) , e . getY ( ) ) ;
						if ( index != -1 ) {
							showControlDialog ( tabbedPane , index ) ;
						}
					}
				}
			} ) ;
	}

	private static void showControlDialog ( JTabbedPane tabbedPane , int index ) {
		EditorPanel uiComp = ( EditorPanel ) tabbedPane . getComponentAt ( index ) ;
		if ( uiComp == null )
		return ;
		String currentTitle = tabbedPane . getTitleAt ( index ) ;
		JDialog dialog ;

		dialog = new JDialog ( uiComp . realJFrame ( ) , "Tab Control" , true ) ;

		JTextField textField = new JTextField ( currentTitle , 12 ) ;
		JButton btnRename = new JButton ( "Rename Tab" ) ;
		textField . addKeyListener ( new KeyAdapter ( ) {
				@ Override
				public void keyPressed ( KeyEvent e ) {
					if ( e . getKeyCode ( ) == KeyEvent . VK_ENTER ) {
						btnRename . doClick ( ) ;
					}
				}
			} ) ;
		JButton btnDetach = null ;
		JButton btnClose = new JButton ( "Close Tab" ) ;
		btnClose . setForeground ( Color . RED ) ;
		boolean canDetach = uiComp . tabs != null && uiComp . tabs . getTabCount ( ) > 1 ;
		if ( canDetach ) {
			btnDetach = new JButton ( "Detach" ) ;
			btnDetach . addActionListener ( e -> {
					dialog . dispose ( ) ;
					try {
						detachTab ( uiComp , index ) ;
					} catch ( Exception ex ) {
						ex . printStackTrace ( ) ;
					}
				} ) ;
		}
		btnRename . addActionListener ( e -> {
				String newTitle = textField . getText ( ) . trim ( ) ;
				if ( ! newTitle . isEmpty ( ) ) {
					tabbedPane . setTitleAt ( index , newTitle ) ;
					dialog . dispose ( ) ;
				}
			} ) ;

		btnClose . addActionListener ( e -> {
				dialog . dispose ( ) ;
				closeTab ( uiComp , index ) ;
			} ) ;
		JPanel panel = new JPanel ( ) ;
		SimpleLayout sl = new SimpleLayout ( panel ) ;
		sl . add ( new JLabel ( "新名称:" ) ) ;
		sl . add ( textField ) ;
		sl . newline ( ) ;
		sl . add ( btnRename ) ;
		if ( btnDetach != null )
		sl . add ( btnDetach ) ;
		sl . add ( btnClose ) ;
		sl . newline ( ) ;
		dialog . getContentPane ( ) . add ( panel ) ;
		dialog . pack ( ) ;
		dialog . setLocationRelativeTo ( uiComp . realJFrame ( ) ) ;
		dialog . setVisible ( true ) ;
	}

	private static void enableTabMove ( JTabbedPane tabbedPane ) {
		// 添加鼠标拖拽监听
		MouseAdapter dragListener = new MouseAdapter ( ) {
			private int draggedIndex = -1 ;

			@ Override
			public void mousePressed ( MouseEvent e ) {
				// 按下鼠标时，记录当前被拖拽的 Tab 索引
				draggedIndex = tabbedPane . indexAtLocation ( e . getX ( ) , e . getY ( ) ) ;
			}

			@ Override
			public void mouseDragged ( MouseEvent e ) {
				if ( draggedIndex == -1 )
				return ;

				// 获取鼠标当前移动到的位置对应的 Tab 索引
				int targetIndex = tabbedPane . indexAtLocation ( e . getX ( ) , e . getY ( ) ) ;

				// 如果目标位置有效，且不等于当前位置，则进行交换
				if ( targetIndex != -1 && targetIndex != draggedIndex ) {
					// 1. 记住当前拖拽 Tab 的标题和内容组件
					String title = tabbedPane . getTitleAt ( draggedIndex ) ;
					EditorPanel component = ( EditorPanel ) tabbedPane . getComponentAt ( draggedIndex ) ;
					Icon icon = tabbedPane . getIconAt ( draggedIndex ) ;
					String tip = tabbedPane . getToolTipTextAt ( draggedIndex ) ;
					boolean isEnabled = tabbedPane . isEnabledAt ( draggedIndex ) ;
					tabbedPane . removeTabAt ( draggedIndex ) ;
					tabbedPane . insertTab ( title , icon , component , tip , targetIndex ) ;
					tabbedPane . setEnabledAt ( targetIndex , isEnabled ) ;
					tabbedPane . setSelectedIndex ( targetIndex ) ;
					draggedIndex = targetIndex ;
				}
			}

			@ Override
			public void mouseReleased ( MouseEvent e ) {
				// 鼠标松开，重置索引
				draggedIndex = -1 ;
			}
		} ;

		// 同时注册监听器到鼠标按下和拖拽事件
		tabbedPane . addMouseListener ( dragListener ) ;
		tabbedPane . addMouseMotionListener ( dragListener ) ;
	}

	/** create a EditorPanel in new tab , start from pp */
	public static void newTab ( PlainPage pp ) throws Exception {
		JTabbedPane tabs = pp . uiComp . tabs ;
		if ( tabs == null ) {
			tabs = Tabs . createNewTab ( pp . uiComp ) ;
		}
		int index = tabs . indexOfComponent ( pp . uiComp ) ;
		EditorPanel ep = new EditorPanel ( pp . uiComp . config ) ;
		ep . openWindowInTab ( pp . uiComp , getTabName ( ) , index + 1 ) ;
		ep . page . workPath = pp . workPath ;
	}

	public static void closeTab ( EditorPanel uiComp , int index ) {
		uiComp . tabs . remove ( index ) ;
		if ( uiComp . tabs . getTabCount ( ) == 0 ) {
			uiComp . tabs = null ;
			if ( uiComp . frame != null ) {
				uiComp . frame . dispose ( ) ;
			} else if ( uiComp . iframe != null ) {
				uiComp . iframe . dispose ( ) ;
			}
		}
	}

	public static void detachTab ( EditorPanel ep , int index ) throws Exception {
		JTabbedPane tabs = ep . tabs ;
		if ( tabs == null || tabs . getTabCount ( ) <= 1 )
		return ;

		tabs . remove ( index ) ;
		EditorPanel uiComp = ( EditorPanel ) tabs . getComponent ( 0 ) ;
		ep . tabs = null ;
		if ( uiComp . desktopPane == null ) {
			ep . frame = null ;
			ep . openWindow ( ) ;
		} else {
			// U.e_png, parentUI, frame, frame, null
			JInternalFrame iframe = new JInternalFrame ( "ne" , true , true , true , true ) ;
			EditorPanel . addFocusForIFrame ( iframe , uiComp . fcb ) ;
			ep . openWindowInIFrame ( null , iframe , uiComp . iframesFrame , uiComp . desktopPane , uiComp . fcb ) ;
			uiComp . desktopPane . add ( iframe ) ;
			iframe . setVisible ( true ) ;

			JInternalFrame p1 = uiComp . iframe ;
			iframe . setLocation ( p1 . getLocation ( ) . x + 25 , p1 . getLocation ( ) . y + 25 ) ;
			iframe . setLayer ( p1 . getLayer ( ) ) ;
			iframe . setSize ( p1 . getSize ( ) ) ;
			iframe . setSelected ( true ) ;
		}
	}
}
