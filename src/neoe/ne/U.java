package neoe . ne ;

import java . awt . BorderLayout ;
import java . awt . Color ;
import java . awt . Container ;
import java . awt . Desktop ;
import java . awt . Dimension ;
import java . awt . Font ;
import java . awt . FontMetrics ;
import java . awt . Graphics ;
import java . awt . Graphics2D ;
import java . awt . GraphicsEnvironment ;
import java . awt . Image ;
import java . awt . Point ;
import java . awt . Rectangle ;
import java . awt . RenderingHints ;
import java . awt . Toolkit ;
import java . awt . Window ;
import java . awt . datatransfer . Clipboard ;
import java . awt . datatransfer . DataFlavor ;
import java . awt . datatransfer . StringSelection ;
import java . awt . datatransfer . Transferable ;
import java . awt . event . ActionEvent ;
import java . awt . event . ActionListener ;
import java . awt . event . KeyEvent ;
import java . awt . image . BufferedImage ;
import java . awt . print . Book ;
import java . awt . print . PageFormat ;
import java . awt . print . Printable ;
import java . awt . print . PrinterException ;
import java . awt . print . PrinterJob ;
import java . io . BufferedOutputStream ;
import java . io . BufferedReader ;
import java . io . BufferedWriter ;
import java . io . File ;
import java . io . FileInputStream ;
import java . io . FileOutputStream ;
import java . io . IOException ;
import java . io . InputStream ;
import java . io . InputStreamReader ;
import java . io . OutputStream ;
import java . io . OutputStreamWriter ;
import java . io . PrintWriter ;
import java . io . Reader ;
import java . io . StringWriter ;
import java . io . UnsupportedEncodingException ;
import java . lang . reflect . Field ;
import java . net . URI ;
import java . nio . file . Files ;
import java . text . SimpleDateFormat ;
import java . util . ArrayList ;
import java . util . Arrays ;
import java . util . Collections ;
import java . util . Comparator ;
import java . util . Date ;
import java . util . Enumeration ;
import java . util . HashMap ;
import java . util . HashSet ;
import java . util . LinkedHashMap ;
import java . util . LinkedList ;
import java . util . List ;
import java . util . Map ;
import java . util . Map . Entry ;
import java . util . Random ;
import java . util . Set ;
import java . util . stream . Collectors ;
import java . util . zip . GZIPInputStream ;
import java . util . zip . GZIPOutputStream ;
import java . util . zip . ZipException ;
import javax . imageio . ImageIO ;
import javax . swing . BoxLayout ;
import javax . swing . JButton ;
import javax . swing . JComponent ;
import javax . swing . JFileChooser ;
import javax . swing . JFrame ;
import javax . swing . JInternalFrame ;
import javax . swing . JOptionPane ;
import javax . swing . JPanel ;
import javax . swing . TransferHandler ;
import javax . swing . UIDefaults ;
import javax . swing . UIManager ;
import neoe . ne . PlainPage . Paint ;
import neoe . ne . Plugin . PluginAction ;
import neoe . ne . util . FileIterator ;
import neoe . ne . util . FileUtil ;
import neoe . ne . util . PyData ;

/**
 * Trivial static methods.
 */
public class U {
	final static CharSequence EMPTY = "empty" ;
	final static String NE_ADDTIME = "ne_addtime" ;

	static Font getFont ( String font , float size ) throws Exception {
		Font f ;
		if ( new File ( font ) . isFile ( ) ) {
			f = Font . createFont ( Font . TRUETYPE_FONT , new File ( font ) ) ;
			f = f . deriveFont ( size ) ;
		} else
		f = new Font ( font , Font . PLAIN , ( int ) size ) ;
		return f ;
	}

	static void checkChangedOutside ( PlainPage pp ) {
		if ( U . changedOutside ( pp . pageData ) )
		if ( ! pp . pageData . changedOutside ) {
			pp . pageData . changedOutside = true ;
			if ( pp . pageData . history . size ( ) == 0 ) {
				pp . pageData . reloadFile ( ) ;
				U . showSelfDispMessage ( pp , "File changed outside.(reloaded)" , 4000 ) ;
				pp . pageData . changedOutside = false ;
			} else
			U . showSelfDispMessage ( pp , "File changed outside." , 4000 ) ;
		}
	}

	static EditorPanel newWindow ( PlainPage pp ) throws Exception {
		EditorPanel uiComp = pp . uiComp ;
		EditorPanel ep = new EditorPanel ( uiComp . config ) ;
		if ( uiComp . desktopPane == null )
		ep . openWindow ( ) ;
		else {
			// U.e_png, parentUI, frame, frame, null
			JInternalFrame neframe
			= new JInternalFrame ( "ne" , true , true , true , true ) ;
			ep . openWindow ( U . e_png , neframe , uiComp . realJFrame ,
				uiComp . desktopPane ) ;
			uiComp . desktopPane . add ( neframe ) ;
			neframe . setVisible ( true ) ;
			int fc = uiComp . desktopPane . getAllFrames ( ) . length ;
			JInternalFrame p1 = ( JInternalFrame ) uiComp . frame ;
			neframe . setLocation ( p1 . getLocation ( ) . x + 5 * fc ,
				p1 . getLocation ( ) . y + 5 * fc ) ;
			neframe . setLayer ( p1 . getLayer ( ) ) ;
			neframe . setSize ( p1 . getSize ( ) ) ;
			neframe . setSelected ( true ) ;
		}
		// set default working path
		ep . page . workPath = pp . workPath ;
		return ep ;
	}

	public static void save ( List < String > ss , String encoding , String fn ) throws IOException {
		OutputStream out = new BufferedOutputStream ( new FileOutputStream ( fn ) , 8192 * 16 ) ;
		for ( String s : ss ) {
			out . write ( s . getBytes ( encoding ) ) ;
			out . write ( '\n' ) ;
		}
		out . close ( ) ;
	}

	public static void setEnv ( PlainPage pp , String k , String v ) {
		if ( pp . env == null )
		pp . env = new LinkedHashMap < > ( ) ;
		Map m = pp . env ;
		if ( v . isEmpty ( ) )
		m . remove ( k ) ;
		else
		m . put ( k , v ) ;
		pp . envs = null ; // clean cache
	}

	public static class LocationHistory < E > {
		LinkedList < E > his = new LinkedList < E > ( ) ;
		int pos = 0 ;

		public E back ( E updateCurrent ) {
			if ( pos > 0 ) {
				his . set ( pos , updateCurrent ) ;
				pos -- ;
				return his . get ( pos ) ;
			} else
			return null ;
		}

		public E forward ( E updateCurrent ) {
			// pos);
			if ( pos < his . size ( ) - 1 ) {
				his . set ( pos , updateCurrent ) ;
				pos ++ ;
				return his . get ( pos ) ;
			} else
			return null ;
		}

		public void add ( E loc , E updateCurrent ) {
			if ( his . size ( ) > pos + 1 )
			removeLastN ( his . size ( ) - pos - 1 ) ;
			// String last = "<Empty>";
			if ( ! his . isEmpty ( ) )
			// last = his.getLast().toString();
			his . set ( his . size ( ) - 1 , updateCurrent ) ; // System.out.println("[d]" + last + "=>" + updateCurrent);

			his . add ( loc ) ;
			pos = his . size ( ) - 1 ;
			// System.out.printf("his.add size=%s, pos=%s\n", his.size(), pos);
		}

		private void removeLastN ( int cnt ) {
			for ( int i = 0 ; i < cnt ; i ++ )
			his . removeLast ( ) ;
		}
	}

	static void drawStringShrink ( Graphics2D g2 , FontList fontList , String s ,
		int x , int y , float maxWidth ) {
		int max = Math . round ( maxWidth ) ;
		int width = stringWidth ( g2 , fontList , s , max ) ;
		if ( width <= max )
		drawString ( g2 , fontList , s , x , y , max ) ;
		else {
			Graphics2D g3 = ( Graphics2D ) g2 . create ( ) ;
			g3 . scale ( ( maxWidth - 3 ) / ( float ) width , 1 ) ;
			drawString ( g3 , fontList , s , x , y , max ) ;
			g3 . dispose ( ) ;
		}
	}

	static String suNotice ( ) {
		String user = System . getProperty ( "user.name" ) ;
		if ( "root" . equals ( user ) || "administrator" . equalsIgnoreCase ( user ) )
		return " [su]" ;
		else
		return "" ;
	}

	/*
	 * only for plugin(like neoeime) compatible
	 */
	public static int drawString ( Graphics2D g2 , FontList fonts , String s , int x , int y ) {
		return drawString ( g2 , fonts , s , x , y , 8000 ) ;
	}

	/*
	 * only for plugin(like neoeime) compatible
	 */
	public static int stringWidth ( Graphics2D g2 , FontList fonts , String s ) {
		return stringWidth ( g2 , fonts , s , 8000 ) ;
	}

	/**
	 * no TAB needed to care here
	 */
	public static int drawString ( Graphics2D g2 , FontList fonts , String s , int x , int y , int maxWidth ) {
		if ( s == null || s . length ( ) <= 0 )
		return 0 ;

		// draw separated by fonts
		int w = 0 ;
		Font cf = fonts . font [ 0 ] ;
		StringBuilder sb = new StringBuilder ( ) ;
		int w1 = 0 ;
		int i = 0 ;
		Font [ ] fo = new Font [ 1 ] ;
		while ( i < s . length ( ) ) {
			char c = s . charAt ( i ) ;
			int w0 = charWidth ( g2 , fonts , c , fo ) ;
			if ( cf . equals ( fo [ 0 ] ) ) {
				sb . append ( c ) ;
				w1 += w0 ;
			} else {
				w1 = submitStr ( g2 , cf , sb . toString ( ) , x , y ) ;
				x += w1 ;
				w += w1 ;
				w1 = w0 ;
				sb . setLength ( 0 ) ;
				sb . append ( c ) ;
				cf = fo [ 0 ] ;
			}
			i ++ ;
			if ( w > maxWidth )
			break ;
		}
		if ( sb . length ( ) > 0 ) {
			w1 = submitStr ( g2 , cf , sb . toString ( ) , x , y ) ;
			w += w1 ;
		}

		return w ;
	}

	public static int exactRemainChar ( Graphics2D g2 , FontList fonts , String s ,
		int maxWidth ) {
		if ( s == null || s . length ( ) <= 0 )
		return 0 ;
		int [ ] wc = new int [ 1 ] ;
		// draw separated by fonts
		int w = 0 ;
		Font cf = fonts . font [ 0 ] ;
		StringBuilder sb = new StringBuilder ( ) ;
		int w1 = 0 ;
		int i = 0 ;
		int x = 0 ;
		Font [ ] fo = new Font [ 1 ] ;
		while ( i < s . length ( ) ) {
			char c = s . charAt ( i ) ;
			int w0 = charWidth ( g2 , fonts , c , fo ) ;
			if ( cf . equals ( fo [ 0 ] ) ) {
				sb . append ( c ) ;
				w1 += w0 ;
			} else {
				w1 = submitStrNoDraw ( g2 , cf , sb . toString ( ) , maxWidth - x , wc ) ;
				x += w1 ;
				w += w1 ;
				w1 = w0 ;
				sb . setLength ( 0 ) ;
				sb . append ( c ) ;
				cf = fo [ 0 ] ;
			}
			i ++ ;
			if ( w > maxWidth )
			break ;
		}
		if ( sb . length ( ) > 0 ) {
			w1 = submitStrNoDraw ( g2 , cf , sb . toString ( ) , maxWidth - x , wc ) ;
			w += w1 ;
		}

		return wc [ 0 ] ;
	}

	private static int submitStrNoDraw ( Graphics2D g2 , Font cf , String s ,
		int width , int [ ] wc ) {
		if ( s . isEmpty ( ) )
		return 0 ;
		g2 . setFont ( cf ) ;
		// g2.drawString(s, x, y);
		FontMetrics fm = g2 . getFontMetrics ( ) ;
		int w = fm . stringWidth ( s ) ;
		if ( w <= width )
		wc [ 0 ] += s . length ( ) ;
		else
		wc [ 0 ] += tryStrWidth ( fm , s , width , s . length ( ) / 2 , 0 , s . length ( ) , 0 ) ;
		return w ;
	}

	static int tryStrWidth ( FontMetrics fm , String s , int width , int n , int a ,
		int b , int safe ) {
		if ( n <= a )
		return a ;
		if ( n >= b )
		return b ;
		if ( safe > 32 ) {
			System . out . println ( "bug in tryStrWidth()!" ) ;
			return n ;
		}
		int w = fm . stringWidth ( s . substring ( 0 , n ) ) ;
		if ( w < width )
		return tryStrWidth ( fm , s , width , ( n + b ) / 2 , n , b , safe + 1 ) ;
		else if ( w > width )
		return tryStrWidth ( fm , s , width , ( n + a ) / 2 , a , n , safe + 1 ) ;
		else
		return n ;
	}

	private static int submitStr ( Graphics2D g2 , Font cf , String s , int x , int y ) {
		if ( s . isEmpty ( ) )
		return 0 ;

		g2 . setFont ( cf ) ;
		g2 . drawString ( s , x , y ) ;
		return g2 . getFontMetrics ( ) . stringWidth ( s ) ;
	}

	/*
   * use first font, if cannot display character in that font , use second,
   * and so on
	 */
	public static int stringWidth ( Graphics2D g2 , FontList fonts , String s , int maxw ) {
		int w = 0 ;
		List < CharSequence > s1x = U . splitToken ( s ) ;
		for ( CharSequence s1c : s1x ) {
			String s1 = s1c . toString ( ) ;
			if ( s1 . equals ( "\t" ) )
			w += U . TAB_WIDTH ;
			else
			w += stringWidthSection ( g2 , fonts , s1 , maxw ) ;
			if ( w > maxw )
			break ;
		}
		return w ;
	}

	public static int stringWidthSection ( Graphics2D g2 , FontList fonts , String s , int maxw ) {
		if ( s == null || s . length ( ) <= 0 )
		return 0 ;

		// draw separated by fonts
		int w = 0 ;
		Font cf = fonts . font [ 0 ] ;
		StringBuilder sb = new StringBuilder ( ) ;
		int w1 = 0 ;
		int i = 0 ;
		Font [ ] fo = new Font [ 1 ] ;
		while ( i < s . length ( ) ) {
			char c = s . charAt ( i ) ;
			int w0 = charWidth ( g2 , fonts , c , fo ) ;
			if ( cf . equals ( fo [ 0 ] ) ) {
				sb . append ( c ) ;
				w1 += w0 ;
			} else {
				w1 = g2 . getFontMetrics ( cf ) . stringWidth ( sb . toString ( ) ) ;
				w += w1 ;
				w1 = w0 ;
				sb . setLength ( 0 ) ;
				sb . append ( c ) ;
				cf = fo [ 0 ] ;
			}
			i ++ ;
		}
		if ( sb . length ( ) > 0 ) {
			w1 = g2 . getFontMetrics ( cf ) . stringWidth ( sb . toString ( ) ) ;
			w += w1 ;
		}

		return w ;
	}

	static int TAB_WIDTH = 20 ;

	public static int charWidth ( Graphics2D g2 , FontList fonts , char c ) {
		// for compact with IME interface
		return charWidth ( g2 , fonts , c , null ) ;
	}

	public static int charWidth ( Graphics2D g2 , FontList fonts , char c ,
		Font [ ] fo ) {
		Object [ ] row
		= c < 256 ? fonts . charWidthCaches256 [ c ] : fonts . charWidthCaches . get ( c ) ;
		if ( row == null ) {
			row = genCharWidthCaches ( g2 , c , fonts ) ;
			if ( c < 256 )
			fonts . charWidthCaches256 [ c ] = row ;
			else
			fonts . charWidthCaches . put ( c , row ) ;
		}
		if ( fo != null )
		fo [ 0 ] = ( Font ) row [ 0 ] ;
		return ( Integer ) row [ 1 ] ;
	}

	/*
	 * match the first font can show the char
	 */
	private static Object [ ] genCharWidthCaches ( Graphics2D g2 , char c , FontList fontList ) {
		Font [ ] fonts = fontList . font ;
		Font f = fonts [ 0 ] ;
		for ( Font font : fonts )
		if ( font . canDisplay ( c ) ) {
			f = font ;
			break ;
		}
		return new Object [ ] { f , g2 . getFontMetrics ( f ) . charWidth ( c ) } ;
	}

	public static class SimpleLayout {
		JPanel curr ;
		JPanel p ;

		public SimpleLayout ( JPanel p ) {
			this . p = p ;
			p . setLayout ( new BoxLayout ( p , BoxLayout . PAGE_AXIS ) ) ;
			newCurrent ( ) ;
		}

		public void add ( JComponent co ) {
			curr . add ( co ) ;
		}

		void newCurrent ( ) {
			curr = new JPanel ( ) ;
			curr . setLayout ( new BoxLayout ( curr , BoxLayout . LINE_AXIS ) ) ;
		}

		public void newline ( ) {
			p . add ( curr ) ;
			newCurrent ( ) ;
		}
	}

	static class TH extends TransferHandler {
		private final EditorPanel ep ;

		TH ( EditorPanel ep ) {
			this . ep = ep ;
		}

		@ Override
		public boolean canImport ( TransferHandler . TransferSupport support ) {
			if ( ! support . isDataFlavorSupported ( DataFlavor . javaFileListFlavor ) )
			return false ;
			return true ;
		}

		@ Override
		@ SuppressWarnings ( "unchecked" )
		public boolean importData ( TransferHandler . TransferSupport support ) {
			if ( ! canImport ( support ) )
			return false ;
			Transferable t = support . getTransferable ( ) ;
			try {
				List < File > l
				= ( List < File > ) t . getTransferData ( DataFlavor . javaFileListFlavor ) ;
				for ( File f : l )
				if ( f . isFile ( ) )
				try {
					ep . findAndShowPage ( f . getAbsolutePath ( ) , -1 , true ) ;
				} catch ( Exception e ) {
					e . printStackTrace ( ) ;
				}
			} catch ( Exception e ) {
				e . printStackTrace ( ) ;
				return false ;
			}
			ep . repaint ( ) ;
			return true ;
		}
	}

	public static class UnicodeFormatter {
		static public String byteToHex ( byte b ) {
			// Returns hex String representation of byte b
			char hexDigit [ ] = { '0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' ,
				'8' , '9' , 'a' , 'b' , 'c' , 'd' , 'e' , 'f' } ;
			char [ ] array = { hexDigit [ ( b >> 4 ) & 0x0f ] , hexDigit [ b & 0x0f ] } ;
			return new String ( array ) ;
		}

		static public String charToHex ( char c ) {
			// Returns hex String representation of char c
			byte hi = ( byte ) ( c >>> 8 ) ;
			byte lo = ( byte ) ( c & 0xff ) ;
			return byteToHex ( hi ) + byteToHex ( lo ) ;
		}
	}

	static final Object [ ] [ ] BOMS = new Object [ ] [ ] {
		new Object [ ] { new int [ ] { 0xEF , 0xBB , 0xBF } , "UTF-8" } ,
		new Object [ ] { new int [ ] { 0xFE , 0xFF } , "UTF-16BE" } ,
		new Object [ ] { new int [ ] { 0xFF , 0xFE } , "UTF-16LE" } ,
		new Object [ ] { new int [ ] { 0 , 0 , 0xFE , 0xFF } , "UTF-32BE" } ,
		new Object [ ] { new int [ ] { 0xFF , 0xFE , 0 , 0 } , "UTF-32LE" } , } ;

	static Map < String , Commands > keys ;
	static Map < String , PluginAction > pluginKeys ;

	public final static String [ ] KWS
	= "ArithmeticError AssertionError AttributeError BufferType BuiltinFunctionType BuiltinMethodType ClassType CodeType ComplexType DeprecationWarning DictProxyType DictType DictionaryType EOFError EllipsisType EmitStreamVertex EmitVertex EndPrimitive EndStreamPrimitive EnvironmentError Err Exception False FileType FloatType FloatingPointError FrameType FunctionType GeneratorType IOError ImportError IndentationError IndexError InstanceType IntType KeyError KeyboardInterrupt LambdaType ListType LongType LookupError MemoryError MethodType ModuleType NameError None NoneType NotImplemented NotImplementedError OSError ObjectType OverflowError OverflowWarning ReferenceError RuntimeError RuntimeWarning Self SliceType StandardError StopIteration StringType StringTypes SyntaxError SyntaxWarning SystemError SystemExit TabError TracebackType True TupleType TypeError TypeType UnboundLocalError UnboundMethodType UnicodeError UnicodeType UserWarning ValueError Warning WindowsError XRangeType ZeroDivisionError __abs__ __add__ __all__ __author__ __bases__ __builtins__ __call__ __class__ __cmp__ __coerce__ __contains__ __debug__ __del__ __delattr__ __delitem__ __delslice__ __dict__ __div__ __divmod__ __doc__ __docformat__ __eq__ __file__ __float__ __floordiv__ __future__ __ge__ __getattr__ __getattribute__ __getitem__ __getslice__ __gt__ __hash__ __hex__ __iadd__ __import__ __imul__ __init__ __int__ __invert__ __iter__ __le__ __len__ __long__ __lshift__ __lt__ __members__ __metaclass__ __mod__ __mro__ __mul__ __name__ __ne__ __neg__ __new__ __nonzero__ __oct__ __or__ __path__ __pos__ __pow__ __radd__ __rdiv__ __rdivmod__ __reduce__ __repr__ __rfloordiv__ __rlshift__ __rmod__ __rmul__ __ror__ __rpow__ __rrshift__ __rsub__ __rtruediv__ __rxor__ __self__ __setattr__ __setitem__ __setslice__ __slots__ __str__ __sub__ __truediv__ __version__ __xor__ abs abstract acos acosh active all and any apply array as asc ascb ascw asin asinh asm assert async atan atanh atn atomicAdd atomicAnd atomicCompSwap atomicCounter atomicCounterDecrement atomicCounterIncrement atomicExchange atomicMax atomicMin atomicOr atomicXor atomic_uint attribute auto await barrier become bitCount bitfieldExtract bitfieldInsert bitfieldReverse bool boolean box break buffer bvec2 bvec3 bvec4 byref byte byval call callable case cast catch cbool cbyte ccur cdate cdbl ceil centroid char chr chrb chrw cint clamp class classmethod clng cmp coerce coherent common compile complex const continue cos cosh crate createobject cross csng cstr dFdx dFdy date dateadd datediff datepart dateserial datevalue day def default degrees del delattr determinant dict dim dir discard distance divmod dmat2 dmat2x2 dmat2x3 dmat2x4 dmat3 dmat3x2 dmat3x3 dmat3x4 dmat4 dmat4x2 dmat4x3 dmat4x4 do dot double dvec2 dvec3 dvec4 dyn each elif else elseif empty end enum enumerate equal erase error eval except exec execfile execute exit exp exp2 explicit extends extern external faceforward false file filter final finally findLSB findMSB fix fixed flat float floatBitsToInt floatBitsToUint floor fma fn for formatcurrency formatdatetime formatnumber formatpercent fract frexp from frozenset function fvec2 fvec3 fvec4 fwidth get getattr getobject getref gl_ClipDistance gl_FragCoord gl_FragDepth gl_FrontFacing gl_GlobalInvocationID gl_InstanceID gl_InvocationID gl_Layer gl_LocalInvocationID gl_LocalInvocationIndex gl_NumSamples gl_NumWorkGroups gl_PatchVerticesIn gl_PointCoord gl_PointSize gl_Position gl_PrimitiveID gl_PrimitiveIDIn gl_SampleID gl_SampleMask gl_SampleMaskIn gl_SamplePosition gl_TessCoord gl_TessLevelInner gl_TessLevelOuter gl_VertexID gl_ViewportIndex gl_WorkGroupID gl_WorkGroupSize global globals goto greaterThan greaterThanEqual groupMemoryBarrier half hasattr hash hex highp hour hvec2 hvec3 hvec4 id if iimage1D iimage1DArray iimage2D iimage2DArray iimage2DMS iimage2DMSArray iimage2DRect iimage3D iimageBuffer iimageCube iimageCubeArray image1D image1DArray image2D image2DArray image2DMS image2DMSArray image2DRect image3D imageAtomicAdd imageAtomicAnd imageAtomicCompSwap imageAtomicExchange imageAtomicMax imageAtomicMin imageAtomicOr imageAtomicXor imageBuffer imageCube imageCubeArray imageLoad imageSize imageStore imp impl implements import imulExtended in inline inout input inputbox instanceof instr instrb instrrev int intBitsToFloat interface intern interpolateAtCentroid interpolateAtOffset interpolateAtSample invariant inverse inversesqrt is isampler1D isampler1DArray isampler2D isampler2DArray isampler2DMS isampler2DMSArray isampler2DRect isampler3D isamplerBuffer isamplerCube isamplerCubeArray isarray isdate isempty isinf isinstance isnan isnull isnumeric isobject issubclass iter ivec2 ivec3 ivec4 join lambda layout lbound lcase ldexp left leftb len lenb length lessThan lessThanEqual let list loadpicture local locals log log2 long loop lowp ltrim macro map mat2 mat2x2 mat2x3 mat2x4 mat3 mat3x2 mat3x3 mat3x4 mat4 mat4x2 mat4x3 mat4x4 match matrixCompMult max mediump memoryBarrier memoryBarrierAtomicCounter memoryBarrierBuffer memoryBarrierImage memoryBarrierShared mid midb min minute mix mod modf month monthname move msgbox mut namespace native new next nil noinline noise noperspective normalize not notEqual nothing now null object oct on open option or ord out outerProduct output override packDouble2x32 packHalf2x16 packSnorm2x16 packSnorm4x8 packUnorm2x16 packUnorm4x8 package packed partition pass patch pow precision preserve print priv private property protected pub public radians raise randomize range raw_input readonly redim reduce ref reflect refract register reload rem repeat replace repr resource restrict resume return reversed rgb right rightb rnd round roundEven row_major rtrim sample sampler1D sampler1DArray sampler1DArrayShadow sampler1DShadow sampler2D sampler2DArray sampler2DArrayShadow sampler2DMS sampler2DMSArray sampler2DRect sampler2DRectShadow sampler2DShadow sampler3D sampler3DRect samplerBuffer samplerCube samplerCubeArray samplerCubeArrayShadow samplerCubeShadow scriptengine scriptenginebuildversion scriptenginemajorversion scriptengineminorversion second select self set setattr sgn shared short sign signed sin sinh sizeof slice smooth smoothstep sorted space split sqr sqrt static staticmethod step str strcomp strictfp string strreverse struct sub subroutine sum super superp switch synchronized tan tanh template texelFetch texelFetchOffset texture textureGather textureGatherOffset textureGatherOffsets textureGrad textureGradOffset textureLod textureLodOffset textureOffset textureProj textureProjGrad textureProjGradOffset textureProjLod textureProjLodOffset textureProjOffset textureQueryLevels textureQueryLod textureSize then this throw throws time timeserial timevalue to trait transient transpose trim true trunc try tuple type typedef typename typeof uaddCarry ubound ucase uimage1D uimage1DArray uimage2D uimage2DArray uimage2DMS uimage2DMSArray uimage2DRect uimage3D uimageBuffer uimageCube uimageCubeArray uint uintBitsToFloat umulExtended unichr unicode uniform union unpackDouble2x32 unpackHalf2x16 unpackSnorm2x16 unpackSnorm4x8 unpackUnorm2x16 unpackUnorm4x8 unsafe unsigned unsized until usampler1D usampler1DArray usampler2D usampler2DArray usampler2DMS usampler2DMSArray usampler2DRect usampler3D usamplerBuffer usamplerCube usamplerCubeArray use using usubBorrow uvec2 uvec3 uvec4 vars vartype varying vbAbort vbAbortRetryIgnore vbApplicationModal vbCancel vbCritical vbDefaultButton1 vbDefaultButton2 vbDefaultButton3 vbDefaultButton4 vbExclamation vbFalse vbGeneralDate vbIgnore vbInformation vbLongDate vbLongTime vbNo vbOK vbOKCancel vbOKOnly vbObjectError vbQuestion vbRetry vbRetryCancel vbShortDate vbShortTime vbSystemModal vbTrue vbUseDefault vbYes vbYesNo vbYesNoCancel vbarray vbblack vbblue vbboolean vbbyte vbcr vbcrlf vbcurrency vbcyan vbdataobject vbdate vbdecimal vbdouble vbempty vberror vbformfeed vbgreen vbinteger vblf vblong vbmagenta vbnewline vbnull vbnullchar vbnullstring vbobject vbred vbsingle vbstring vbtab vbvariant vbverticaltab vbwhite vbyellow vec2 vec3 vec4 virtual void volatile weekday weekdayname wend where while with writeonly xor xrange year yield zip"
	. split ( " " ) ;

	public static List originKeys ;

	static Random random = new Random ( ) ;

	public static Image tabImg , tabImgPrint ;

	static final String UTF8 = "UTF8" ;

	public static final char N = '\n' ;

	static void addKey ( Map < String , Commands > keys , String key , String cmd )
	throws Exception {
		String name = getKeyNameFromTextName ( key ) ;

		Commands c1 ;
		try {
			c1 = Commands . valueOf ( cmd ) ;
		} catch ( Exception ex ) {
			System . out . println ( "undefined command:" + cmd ) ;
			return ;
		}
		try {
			// System.out.println(""+name+":"+c1);
			if ( keys . containsKey ( name ) )
			System . err . println ( "duplicated key:" + name ) ;
			else
			keys . put ( name , c1 ) ;
		} catch ( Exception ex ) {
			System . err . println ( "Error: unknow key:" + key ) ;
		}
	}

	public static String getKeyNameFromTextName ( String key ) throws Exception {
		String k = key . toUpperCase ( ) ;
		String name = "" ;
		int p1 ;
		p1 = k . indexOf ( "SHIFT-" ) ;
		if ( p1 >= 0 ) {
			k = k . substring ( 0 , p1 ) + k . substring ( p1 + 6 ) ;
			name = name + "S" ;
		}
		p1 = k . indexOf ( "CTRL-" ) ;
		if ( p1 >= 0 ) {
			k = k . substring ( 0 , p1 ) + k . substring ( p1 + 5 ) ;
			name = name + "C" ;
		}
		p1 = k . indexOf ( "ALT-" ) ;
		if ( p1 >= 0 ) {
			k = k . substring ( 0 , p1 ) + k . substring ( p1 + 4 ) ;
			name = name + "A" ;
		}

		Field f = KeyEvent . class . getField ( "VK_" + k ) ;
		int kc = f . getInt ( null ) ;
		String kt = KeyEvent . getKeyText ( kc ) ;
		name = name + kt ;

		return name ;
	}

	public static void startDaemonThread ( Thread thread ) {
		thread . setDaemon ( true ) ;
		thread . start ( ) ;
	}
	static boolean addTime = true ;
	static void attach ( final PlainPage page , final InputStream std , String name ) {
		U . startDaemonThread ( new Thread ( ) {
				SimpleDateFormat sdf1 = new SimpleDateFormat ( "yyyyMMdd HH:mm:ss:SSS\t" ) ;
				@ Override
				public void run ( ) {
					try {
						String enc = page . pageData . encoding ;
						if ( enc == null ) enc = UTF8 ;
						InputStream in = std ;
						BufferedReader reader = new BufferedReader ( new InputStreamReader ( in , enc ) ) ;
						long t1 = System . currentTimeMillis ( ) ;
						String line = "<begin " + name + ">" ;
						if ( addTime ) line = sdf1 . format ( new Date ( ) ) + line ;
						page . pageData . editRec . appendLine ( line ) ;
						while ( true ) {
							line = reader . readLine ( ) ;
							if ( line == null ) break ;
							if ( addTime ) line = sdf1 . format ( new Date ( ) ) + line ;

							page . pageData . editRec . appendLine ( line ) ;
							long t2 = System . currentTimeMillis ( ) ;
							if ( t2 - t1 > 100 ) {
								t1 = t2 ;
								page . uiComp . repaint ( ) ;
							}
						}
						line = "<end " + name + ">" ;
						if ( addTime ) line = sdf1 . format ( new Date ( ) ) + line ;
						page . pageData . editRec . appendLine ( line ) ;
						page . uiComp . repaint ( ) ;
					} catch ( Throwable e ) {
						page . ptEdit . append ( "error:" + e + "\n" ) ;
					}
				}
			} ) ;
	}

	static boolean changedOutside ( PageData pd ) {
		if ( pd . fileLoaded ) {
			long t = new File ( pd . title ) . lastModified ( ) ;
			if ( t > pd . fileLastModified + 100 )
			return true ;
		}
		return false ;
	}

	static void closePage ( PlainPage page ) throws Exception {
		EditorPanel editor = page . uiComp ;
		int opt = JOptionPane . YES_OPTION ;
		if ( page . console != null ) {
			//quiet
		} else if ( page . pageData . history . size ( ) != 0 ) {
			opt = JOptionPane . showConfirmDialog (
				editor , "Are you sure to SAVE and close?" , "Changes made" ,
				JOptionPane . YES_NO_CANCEL_OPTION , JOptionPane . QUESTION_MESSAGE ) ;

			if ( opt == JOptionPane . CANCEL_OPTION || opt == -1 )
			return ;
			if ( opt == JOptionPane . YES_OPTION )
			if ( ! saveFile ( page ) )
			return ;
		}
		if ( page . pageData . fileLoaded )
		saveFileHistory ( page . pageData . title , page . cy ) ;
		page . close ( ) ;
	}

	static int drawTwoColor ( Graphics2D g2 , FontList fonts , String s , int x , int y ,
		Color c1 , Color c2 , int d , int maxw ) {
		g2 . setColor ( c2 ) ;
		int w = U . drawString ( g2 , fonts , s , x + d , y + d , maxw ) ;
		g2 . setColor ( c1 ) ;
		U . drawString ( g2 , fonts , s , x , y , maxw ) ;
		return w ;
	}

	public static void exec ( PlainPage pp , String cmd ) throws Exception {
		cmd = cmd . trim ( ) ;
		if ( cmd . length ( ) <= 0 )
		return ;
		if ( isCmdCd ( cmd , pp ) )
		return ;
		if ( isCmdExport ( cmd , pp ) )
		return ;
		File dir ;
		if ( cmd . startsWith ( "[" ) ) {
			int p1 = cmd . indexOf ( "]" ) ;
			String path = cmd . substring ( 1 , p1 ) . trim ( ) ;
			dir = new File ( path ) ;
			cmd = cmd . substring ( p1 + 1 ) . trim ( ) ;
		} else if ( pp . workPath != null )
		dir = new File ( pp . workPath ) ;
		else
		dir = new File ( "." ) ;
		addCmdHistory ( cmd , dir . getAbsolutePath ( ) ) ;
		Process proc = Runtime . getRuntime ( ) . exec ( splitCommand ( cmd ) , getEnv ( pp ) , dir ) ;
		OutputStream out = proc . getOutputStream ( ) ;
		InputStream stdout = proc . getInputStream ( ) ;
		InputStream stderr = proc . getErrorStream ( ) ;

		PlainPage pp2 = new PlainPage ( pp . uiComp , PageData . fromTitle ( String . format ( "[cmd][%s] %s #%s" , dir . getAbsolutePath ( ) ,
					cmd , U . randomID ( ) ) ) , pp ) ;
		pp2 . workPath = dir . getAbsolutePath ( ) ;
		pp2 . ptSelection . selectAll ( ) ;

		new Console ( cmd , out , stdout , stderr , proc , pp . uiComp , dir ) . start ( ) ;
	}

	private static void addCmdHistory ( String cmd , String path )
	throws IOException {
		String s = String . format ( "[%s] %s" , path , cmd ) ;
		File ch = getCmdHistoryName ( ) ;
		String old = FileUtil . readString ( new FileInputStream ( ch ) , null ) ;
		List < String > his = Arrays . asList ( old . split ( "\n" ) ) ;
		BufferedWriter out = new BufferedWriter (
			new OutputStreamWriter ( new FileOutputStream ( ch ) , UTF8 ) ) ;
		if ( ! his . contains ( s ) ) {
			out . write ( s ) ;
			out . write ( "\n" ) ;
			out . write ( old ) ;
		} else {
			out . write ( s ) ;
			out . write ( "\n" ) ;
			List < String > his2 = new ArrayList ( his ) ;
			his2 . remove ( s ) ;
			for ( String line : his2 ) {
				out . write ( line ) ;
				out . write ( "\n" ) ;
			}
		}
		out . close ( ) ;
	}

	private static String [ ] getEnv ( PlainPage pp ) {
		if ( pp . envs != null )
		return pp . envs ;
		if ( pp . env == null )
		return null ;
		int size = pp . env . size ( ) ;
		String [ ] ss = new String [ size ] ;
		int i = 0 ;
		for ( Entry < String , String > en : pp . env . entrySet ( ) )
		ss [ i ++ ] = en . getKey ( ) + "=" + en . getValue ( ) ;
		pp . envs = ss ;
		return ss ;
	}

	private static String [ ] splitCommand ( String cmd ) throws Exception {
		List list = ( List ) PyData . parseAll ( "[" + cmd + "]" , false , true ) ;
		String [ ] ss = new String [ list . size ( ) ] ;
		int len = list . size ( ) ;
		for ( int i = 0 ; i < len ; i ++ )
		ss [ i ] = "" + list . get ( i ) ;
		return ss ;
	}

	private static boolean isCmdCd ( String cmd , PlainPage pp ) {
		if ( "cd" . equals ( cmd ) )
		cmd = "cd " ;
		if ( ! cmd . startsWith ( "cd " ) )
		return false ;
		String path = cmd . substring ( 3 ) . trim ( ) ;
		if ( path . isEmpty ( ) )
		path = System . getProperty ( "user.home" ) ;
		else {
			path = dequote ( path ) ;
			File f = new File ( path ) ;
			if ( ! f . exists ( ) ) {
				String path2 = new Xcd ( ) . run ( path ) ;
				if ( path2 == null ) {
					pp . ui . message ( "path not exist:" + path ) ;
					return true ;
				}
				path = path2 ;
			} else if ( f . isFile ( ) )
			path = f . getParent ( ) ;
		}
		pp . workPath = path ;
		pp . ui . message ( "CWD=" + path ) ;
		return true ;
	}

	private static String dequote ( String s ) {
		if ( s . startsWith ( "\"" ) && s . endsWith ( "\"" ) )
		s = s . substring ( 1 , s . length ( ) - 1 ) ;
		return s ;
	}

	private static boolean isCmdExport ( String cmd , PlainPage pp ) {
		if ( ! cmd . startsWith ( "export " ) )
		return false ;
		String kv = cmd . substring ( 7 ) . trim ( ) ;
		int p1 = kv . indexOf ( '=' ) ;
		if ( p1 <= 0 )
		return false ;
		String k = kv . substring ( 0 , p1 ) . trim ( ) ;
		String v = kv . substring ( p1 + 1 ) . trim ( ) ;
		v = dequote ( v ) ;
		setEnv ( pp , k , v ) ;
		pp . ui . message ( String . format ( "ENV[%s]=%s" , k , v ) ) ;
		return true ;
	}

	/*
	 * No good.
	 */
	static void gc ( ) {
		new Thread ( ( ) -> {
				sleep ( 50 ) ;
				System . gc ( ) ;
			} ) . start ( ) ;
	}

	private static void sleep ( int ms ) {
		try {
			Thread . sleep ( ms ) ;
		} catch ( InterruptedException e ) {
		}
	}

	static String getClipBoard ( ) {
		try {
			Clipboard clip = Toolkit . getDefaultToolkit ( ) . getSystemClipboard ( ) ;
			Object o ;
			if ( clip . isDataFlavorAvailable ( DataFlavor . stringFlavor ) ) {
				o = clip . getData ( DataFlavor . stringFlavor ) ;
				if ( o != null )
				return o . toString ( ) ;
			}
			if ( clip . isDataFlavorAvailable ( DataFlavor . javaFileListFlavor ) ) {
				o = clip . getData ( DataFlavor . javaFileListFlavor ) ;
				if ( o != null ) {
					List < File > l = ( List < File > ) o ;
					StringBuffer sb = new StringBuffer ( ) ;
					for ( File f : l )
					sb . append ( f . getAbsolutePath ( ) ) . append ( '\n' ) ;
					return sb . toString ( ) ;
				}
			}
		} catch ( Exception e ) {
			e . printStackTrace ( ) ;
		}
		return "" ;
	}

	public static File getFileHistoryName ( ) throws IOException {
		File f = new File ( getMyDir ( ) , "fh.txt" ) ;
		if ( ! f . exists ( ) )
		new FileOutputStream ( f ) . close ( ) ;
		return f ;
	}

	static File getDirHistoryName ( ) throws IOException {
		File f = new File ( getMyDir ( ) , "dh.txt" ) ;
		if ( ! f . exists ( ) )
		new FileOutputStream ( f ) . close ( ) ;
		return f ;
	}

	static File getCmdHistoryName ( ) throws IOException {
		File f = new File ( getMyDir ( ) , "ch.txt" ) ;
		if ( ! f . exists ( ) )
		new FileOutputStream ( f ) . close ( ) ;
		return f ;
	}

	static int getHighLightID ( String s , Graphics2D g2 , Color colorKeyword ,
		Color colorDigital , Color color ) {
		if ( Arrays . binarySearch ( KWS , s ) >= 0
			|| Arrays . binarySearch ( KWS , s . toLowerCase ( ) ) >= 0 )
		g2 . setColor ( colorKeyword ) ;
		else if ( isAllDigital ( s ) )
		g2 . setColor ( colorDigital ) ;
		else
		g2 . setColor ( color ) ;
		return 0 ;
	}

	public static Reader getInstalledReader ( String fn ) throws IOException {
		File installed = new File ( getMyDir ( ) , fn ) ;
		if ( ! installed . exists ( ) )
		try {
			FileUtil . copy ( U . class . getResourceAsStream ( fn ) ,
				new FileOutputStream ( installed ) ) ;
		} catch ( IOException e ) {
			e . printStackTrace ( ) ;
			return getResourceReader ( fn ) ;
		}
		return new InputStreamReader ( new FileInputStream ( installed ) , UTF8 ) ;
	}

	public static File getMyDir ( ) {
		String home = System . getProperty ( "user.home" ) ;
		File dir = new File ( home , ".neoeedit" ) ;
		dir . mkdirs ( ) ;
		return dir ;
	}

	public static List < CharSequence > getPageListStrings ( EditorPanel ep ) {
		List < CharSequence > ss = new ArrayList < > ( ) ;
		Collections . sort ( ep . pageSet , ( a , b ) -> a . pageData . title . compareTo ( b . pageData . title ) ) ;
		for ( PlainPage pp : ep . pageSet )
		ss . add ( pp . pageData . title + "|" + ( pp . cy + 1 ) + ":"
			+ " Edited:" + pp . pageData . history . size ( )
			+ ( pp . pageData . encoding == null ? "" : " " + pp . pageData . encoding )
			+ ( changedOutside ( pp . pageData ) ? " [Changed Outside!!]" : "" ) ) ;
		return ss ;
	}
	public static List < CharSequence > getDocListStrings ( ) {
		List < CharSequence > ss = new ArrayList < > ( PageData . dataPool . keySet ( ) ) ;
		Collections . sort ( ss , ( a , b ) -> a . toString ( ) . compareTo ( b . toString ( ) ) ) ;
		return ss ;
	}

	static String getLocString ( PlainPage pp ) {
		if ( pp == null )
		return null ;
		return pp . pageData . title + "|" + ( pp . cy + 1 ) + ":" ;
	}

	public static Reader getResourceReader ( String fn ) throws IOException {
		return new InputStreamReader ( U . class . getResourceAsStream ( fn ) , UTF8 ) ;
	}

	public static String getStr ( List row , int i ) {
		if ( i < 0 || i >= row . size ( ) )
		return "" ;
		return "" + row . get ( i ) ;
	}

	private static String guessByBOM ( byte [ ] src ) {
		for ( Object [ ] row : BOMS ) {
			int [ ] seq = ( int [ ] ) row [ 0 ] ;
			// compare 2 array
			if ( seq . length > src . length )
			continue ;
			boolean same = true ;
			for ( int i = 0 ; i < seq . length ; i ++ )
			if ( ( byte ) seq [ i ] != src [ i ] ) {
				same = false ;
				break ;
			}
			if ( same )
			return ( String ) row [ 1 ] ;
		}
		return null ;
	}

	static void guessComment ( PlainPage page ) {
		List < String > comment = new ArrayList < String > ( ) ;
		String [ ] commentchars = {
			"/*" , "<!--" , "#" , "%" , "'" , "//" , "!" , ";" , "--" , } ;
		int [ ] cnts = new int [ commentchars . length ] ;
		int maxLines = Math . min ( 1000 , page . pageData . roLines . getLinesize ( ) ) ;
		for ( int i = 0 ; i < maxLines ; i ++ ) {
			CharSequence sb = page . pageData . roLines . getline ( i ) ;
			CharSequence tl = U . trimLeft ( sb ) ;
			String s = tl . subSequence ( 0 , Math . min ( 40 , tl . length ( ) ) ) . toString ( ) ;
			for ( int j = 0 ; j < cnts . length ; j ++ ) {
				String k = commentchars [ j ] ;
				if ( s . startsWith ( k ) || s . indexOf ( k ) >= 0 )
				cnts [ j ] += k . length ( ) ;
			}
		}
		int kind = 0 ;
		int max = 0 ;
		for ( int j = 0 ; j < cnts . length ; j ++ )
		if ( cnts [ j ] > 0 ) {
			kind ++ ;
			max = Math . max ( max , cnts [ j ] ) ;
		}
		if ( kind == 1 ) {
			for ( int j = 0 ; j < cnts . length ; j ++ )
			if ( cnts [ j ] > 0 ) {
				comment . add ( commentchars [ j ] ) ;
				break ;
			}
		} else {
			int lv2 = Math . max ( 5 , max / 10 ) ;
			for ( int j = 0 ; j < cnts . length ; j ++ )
			if ( cnts [ j ] > lv2 )
			comment . add ( commentchars [ j ] ) ;
		}
		if ( comment . isEmpty ( ) )
		comment = null ; // page.ui.message("no comment found");
		else {
			// page.ui.message("comment found:" + comment);
		}
		page . pageData . comment
		= comment == null ? null : comment . toArray ( new String [ comment . size ( ) ] ) ;
		page . repaint ( ) ;
	}

	static String guessEncoding ( String fn , PageData data ) throws Exception {
		// S/ystem.out.println("guessing encoding");
		String [ ] encodings = { UTF8 , "gbk" , "sjis" , "unicode" , "euc-jp" , "big5" } ;
		int maxLen ;
		if ( data != null && data . gzip )
		maxLen = data . bs . length ;
		else
		maxLen = ( int ) new File ( fn ) . length ( ) ;
		final int defsize = 1024 * 1024 * 2 ;
		int len = Math . min ( defsize , maxLen ) ;
		if ( len < 0 ) // a large file over 2GB
		len = defsize ;
		byte [ ] buf ;
		if ( data != null && data . gzip )
		buf = data . bs ;
		else {
			buf = new byte [ len ] ;
			FileInputStream in = new FileInputStream ( fn ) ;
			len = in . read ( buf ) ;
			in . close ( ) ;
		}
		String encoding = guessByBOM ( buf ) ;
		if ( encoding != null )
		return encoding ;
		for ( String enc : encodings ) {
			String s = new String ( buf , 0 , len , enc ) ;
			if ( s . toLowerCase ( ) . indexOf ( enc . toLowerCase ( ) ) >= 0 )
			return enc ; // mentioned
			if ( s . length ( ) > 3 ) // multi bytes string, so tail may be mistaken
			s = s . substring ( 0 , s . length ( ) - 3 ) ;
			else
			return UTF8 ; // utf8 for empty file
			byte [ ] bs2 = s . getBytes ( enc ) ;
			// bs2 maybe short than buf
			if ( bsCompare ( buf , bs2 , bs2 . length ) )
			return enc ;
		}
		return null ;
	}

	private static boolean bsCompare ( byte [ ] b1 , byte [ ] b2 , int len ) {
		for ( int i = 0 ; i < len ; i ++ )
		if ( b1 [ i ] != b2 [ i ] )
		return false ;
		return true ;
	}

	static String guessEncodingForEditor ( String fn , PageData data ) {
		try {
			String s = guessEncoding ( fn , data ) ;
			if ( s == null ) // unknow
			return UTF8 ;
			return s ;
		} catch ( Exception e ) {
			return UTF8 ;
		}
	}

	static String guessLineSepForEditor ( String fn , PageData data ) {
		try {
			// S/ystem.out.println("guessing encoding");
			int maxLen ;
			if ( data . gzip )
			maxLen = data . bs . length ;
			else
			maxLen = ( int ) new File ( fn ) . length ( ) ;
			int len = Math . min ( 4096 , maxLen ) ;
			byte [ ] buf ;
			if ( data . gzip )
			buf = data . bs ;
			else {
				buf = new byte [ len ] ;
				FileInputStream in = new FileInputStream ( fn ) ;
				len = in . read ( buf ) ;
				in . close ( ) ;
			}
			return new String ( buf , 0 , len , "iso8859-1" ) . indexOf ( "\r\n" ) >= 0 ? "\r\n"
			: "\n" ;
		} catch ( Exception e ) {
			return "\n" ;
		}
	}

	static boolean isAllDigital ( String s ) {
		for ( char c : s . toCharArray ( ) )
		if ( ! Character . isDigit ( c ) )
		return false ;
		return true ;
	}

	public static boolean isImageFile ( File f ) {
		String fn = f . getName ( ) . toLowerCase ( ) ;
		return ( fn . endsWith ( ".gif" ) || fn . endsWith ( ".jpg" ) || fn . endsWith ( ".png" )
			|| fn . endsWith ( ".bmp" ) || fn . endsWith ( ".jpeg" ) ) ;
	}

	static boolean isSkipChar ( char ch , char ch1 ) {
		if ( U . isSpaceChar ( ch1 ) )
		return U . isSpaceChar ( ch ) ;
		else
		return Character . isJavaIdentifierPart ( ch ) ;
	}

	static String km ( long v ) {
		float m = 1024 * 1024f ;
		if ( v > m )
		return String . format ( "%.1fMB" , v / m ) ;
		else if ( v > 1024 )
		return String . format ( "%.1fKB" , v / 1024f ) ;
		return "" + v ;
	}

	public static boolean launch ( String s ) throws Exception {
		s = s . trim ( ) ;
		String slo = s . toLowerCase ( ) ;
		Desktop dt = Desktop . getDesktop ( ) ;
		if ( slo . startsWith ( "mailto:" ) )
		dt . mail ( new URI ( s ) ) ;
		else if ( slo . startsWith ( "http://" ) || slo . startsWith ( "https://" ) )
		dt . browse ( new URI ( s ) ) ;
		else if ( new File ( s ) . exists ( ) )
		dt . open ( new File ( s ) ) ;
		else
		return false ;
		return true ;
	}

	static boolean listDirOrOpenFile ( PlainPage page , int atLine ) throws Exception {
		String line = page . pageData . roLines . getline ( atLine ) . toString ( ) ;
		File f = findFile ( page . workPath , line ) ;
		if ( f == null ) return false ;
		if ( f . isFile ( ) && f . exists ( ) ) return page . uiComp . findAndShowPage ( f . getAbsolutePath ( ) , -1 , true ) ;
		if ( f . isDirectory ( ) ) {
			File [ ] fs = f . listFiles ( ) ;
			page . cx = line . length ( ) ;
			page . ptEdit . insertString ( "\n{-----" ) ;
			Arrays . sort ( fs ) ; // for god's sake
			for ( File f1 : fs )
			if ( f1 . isDirectory ( ) )
			page . ptEdit . insertString ( "\n" + f1 . getAbsolutePath ( ) + " | <DIR>" ) ;
			else
			page . ptEdit . insertString ( "\n" + f1 . getAbsolutePath ( ) ) ;
			page . ptEdit . insertString ( "\n-----}" ) ;
			return true ;
		} else
		return false ;
	}

	private static File findFile ( String dir , String line ) {
		if ( dir == null )
		dir = "." ;
		String fn = line . trim ( ) ;
		{
			int p1 = fn . indexOf ( '|' ) ;
			if ( p1 >= 0 )
			fn = fn . substring ( 0 , p1 ) . trim ( ) ;
		} {
			int p1 = fn . indexOf ( '\t' ) ;
			if ( p1 >= 0 )
			fn = fn . substring ( 0 , p1 ) . trim ( ) ;
		} {
			File f = new File ( fn ) ;
			if ( f . exists ( ) )
			return f ;
			f = new File ( dir , fn ) ;
			if ( f . exists ( ) )
			return f ;
		} {
			int p1 = fn . indexOf ( ' ' ) ; // more try
			if ( p1 >= 0 ) {
				String fn2 = fn . substring ( 0 , p1 ) . trim ( ) ;
				File f = new File ( fn2 ) ;
				if ( f . exists ( ) )
				return f ;
				f = new File ( dir , fn2 ) ;
				if ( f . exists ( ) )
				return f ;
			}
		} {
			int p1 = fn . lastIndexOf ( ':' ) ; // more try
			if ( p1 >= 0 ) {
				String fn2 = fn . substring ( 0 , p1 ) . trim ( ) ;
				File f = new File ( fn2 ) ;
				if ( f . exists ( ) )
				return f ;
				f = new File ( dir , fn2 ) ;
				if ( f . exists ( ) )
				return f ;
			}
		}
		return null ;
	}

	public static void listFonts ( PlainPage pp ) throws Exception {
		PlainPage p2 = new PlainPage ( pp . uiComp , PageData . fromTitle ( "[Fonts]" ) , pp ) ;
		List < CharSequence > sbs = new ArrayList < > ( ) ;
		String fonts [ ] = GraphicsEnvironment . getLocalGraphicsEnvironment ( )
		. getAvailableFontFamilyNames ( ) ;
		for ( String font : fonts )
		sbs . add ( "set-font:" + font ) ;
		p2 . pageData . resetLines ( sbs ) ;
	}

	static void loadTabImage ( ) throws Exception {
		BufferedImage img = ImageIO . read ( U . class . getResourceAsStream ( "/icontab.png" ) ) ;
		tabImg = img . getScaledInstance ( TAB_WIDTH , 8 , Image . SCALE_SMOOTH ) ;
		tabImgPrint = img . getScaledInstance ( TAB_WIDTH , 8 , Image . SCALE_SMOOTH ) ;
	}

	public static Commands mappingToCommand ( KeyEvent env ) {
		int kc = env . getKeyCode ( ) ;
		if ( kc == KeyEvent . VK_SHIFT || kc == KeyEvent . VK_CONTROL
			|| kc == KeyEvent . VK_ALT ) // fast pass
		return null ;
		String name = getKeyName ( env ) ;
		//		System.out.println("key name=" + name);
		Commands cmd = keys . get ( name ) ;
		return cmd ;
	}

	static String getKeyName ( KeyEvent evt ) {
		int kc = evt . getKeyCode ( ) ;
		String kt = KeyEvent . getKeyText ( kc ) ;
		String name = kt ;
		boolean other = false ;
		if ( evt . isAltDown ( ) ) {
			name = "A" + name ;
			other = true ;
		}
		if ( evt . isControlDown ( ) ) {
			name = "C" + name ;
			other = true ;
		}
		if ( other && kt . length ( ) == 1 && evt . isShiftDown ( ) ) {
			//			name = "S" + name;
		}
		return name ;
	}

	public static int maxWidth ( List < Object [ ] > msgs , Graphics2D g , FontList fonts , int maxw ) {
		int max = 0 ;
		for ( int i = 0 ; i < msgs . size ( ) ; i ++ ) {
			Object [ ] row = msgs . get ( i ) ;
			int w1 = ( Integer ) row [ 2 ] ;
			if ( w1 == -1 ) {
				w1 = U . stringWidth ( g , fonts , row [ 0 ] . toString ( ) , maxw ) ;
				row [ 2 ] = w1 ;
			}
			if ( w1 > max )
			max = w1 ;
		}
		return max ;
	}

	static void listDirToNewPage ( PlainPage page ) throws Exception {
		String dir = page . workPath ;
		if ( dir == null )
		dir = new File ( "." ) . getAbsolutePath ( ) ;
		String title = "[Dir]" + dir ;

		if ( page . uiComp . findAndShowPage ( title , -1 , true ) )
		return ;

		PageData pd = PageData . fromTitle ( title ) ;
		pd . setText ( dir ) ;
		PlainPage pp = new PlainPage ( page . uiComp , pd , page ) ;
		listDirOrOpenFile ( pp , 0 ) ;
	}

	static void openFileHistory ( EditorPanel ep ) throws Exception {
		File fhn = getFileHistoryName ( ) ;
		if ( ep . findAndShowPage ( fhn . getAbsolutePath ( ) , -1 , true ) ) {
			PlainPage pp = ep . page ;
			pp . cursor . setSafePos ( 0 , pp . pageData . lines . size ( ) ) ;
			pp . focusCursor ( ) ;
		}
	}

	static void openDirHistory ( EditorPanel ep ) throws Exception {
		File f = getDirHistoryName ( ) ;
		ep . findAndShowPage ( f . getAbsolutePath ( ) , -1 , true ) ;
	}

	static void paintNoise ( Graphics2D g2 , Dimension dim ) {
		int cnt = 1000 ;
		int w = dim . width ;
		int h = dim . height ;
		int cs = 0xffffff ;
		for ( int i = 0 ; i < cnt ; i ++ ) {
			int x = random . nextInt ( w ) ;
			int y = random . nextInt ( h ) ;
			g2 . setColor ( new Color ( random . nextInt ( cs ) ) ) ;
			g2 . drawLine ( x , y , x + 1 , y ) ;
		}
	}

	public static int parseInt ( Object o ) {
		int v ;
		if ( o == null )
		throw new RuntimeException ( "expect int but get null" ) ;
		String s = o . toString ( ) ;
		if ( s . startsWith ( "0x" ) )
		v = Integer . parseInt ( s . substring ( 2 ) , 16 ) ;
		else
		v = Integer . parseInt ( s ) ;
		return v ;
	}

	static int idIndex ;
	static final int IDRANGE = 36 * 36 * 36 ;
	public static String randomID ( ) {
		return Integer . toString ( ( int ) ( random . nextInt ( IDRANGE ) ) , 36 ) + "_" + ( idIndex ++ ) ;
	}

	public static boolean tryGzip ( String fn , PageData data ) {
		try {
			GZIPInputStream gin = new GZIPInputStream ( new FileInputStream ( fn ) ) ;
			try {
				data . bs = gin . readAllBytes ( ) ;
				gin . close ( ) ;
				return true ;
			} catch ( ZipException e ) {
				System . err . println ( "seems not gzip:" + e ) ;
				return false ;
			}
		} catch ( Exception e ) {
			System . out . println ( e ) ;
			return false ;
		}
	}

	static List < CharSequence > readFileForEditor ( String fn , String encoding ,
		PageData data ) {
		try {
			// System.out.println("read file:" + fn + " encoding=" + encoding);
			List < String > ls ;
			if ( data . gzip ) {
				ls = FileUtil . readStringBig ( data . bs , encoding ) ;
				data . bs = null ; // can release
			} else
			ls = FileUtil . readStringBig ( new File ( fn ) , encoding ) ;
			return U . removeTailR ( ls ) ;
		} catch ( Throwable e ) {
			e . printStackTrace ( ) ;
			List < CharSequence > lines = new ArrayList < CharSequence > ( ) ;
			lines . add ( e . toString ( ) ) ;
			return lines ;
		}
	}

	static void reloadWithEncodingByUser ( PlainPage pp ) {
		if ( ! pp . pageData . fileLoaded ) {
			pp . ui . message ( "file not saved." ) ;
			return ;
		}
		//need save changes?
		if ( setEncodingByUser ( pp , "Reload with Encoding:" ) )
		pp . pageData . reloadFile ( ) ;
	}

	static String removeAsciiZero ( String s ) {
		int cnt = 0 ;
		char zero = ( char ) 0 ;

		int p = s . indexOf ( zero ) ;
		if ( p < 0 )
		return s ;
		String zeros = "" + zero ;
		StringBuilder sb = new StringBuilder ( s ) ;
		while ( p >= 0 ) {
			sb . deleteCharAt ( p ) ;
			cnt ++ ;
			p = sb . indexOf ( zeros , p ) ;
		}
		System . out . println ( "removed " + cnt + " NULL char" ) ;
		return sb . toString ( ) ;
	}

	static CharSequence removeTailR ( CharSequence s ) {
		if ( s . length ( ) == 0 )
		return s ;
		if ( s . charAt ( s . length ( ) - 1 ) == '\r' )
		s = s . subSequence ( 0 , s . length ( ) - 1 ) ;
		return s ;
	}

	static void removeTrailingSpace ( PageData data ) {
		for ( int i = 0 ; i < data . roLines . getLinesize ( ) ; i ++ ) {
			CharSequence sb = data . roLines . getline ( i ) ;
			int p = sb . length ( ) - 1 ;
			while ( p >= 0 && "\r\n\t " . indexOf ( sb . charAt ( p ) ) >= 0 )
			p -- ;
			if ( p < sb . length ( ) - 1 )
			data . editRec . deleteInLine ( i , p + 1 , sb . length ( ) ) ;
		}
	}

	static void repaintAfter ( final long t , final JComponent edit ) {
		U . startDaemonThread ( new Thread ( ( ) -> {
					try {
						Thread . sleep ( t ) ;
						edit . repaint ( ) ;
					} catch ( InterruptedException e ) {
						e . printStackTrace ( ) ;
					}
				} ) ) ;
	}

	static void runScript ( final PlainPage ppTarget ) throws Exception {
		final JFrame f = new JFrame ( "Script for " + ppTarget . pageData . title ) ;
		JPanel panel = new JPanel ( new BorderLayout ( ) ) ;
		final EditorPanel ep1 = new EditorPanel ( ) ;
		File scriptDir = new File ( U . getMyDir ( ) , "scripts" ) ;
		scriptDir . mkdirs ( ) ;
		ep1 . page . workPath = scriptDir . getAbsolutePath ( ) ;
		ep1 . frame = f ;
		U . listDirToNewPage ( ep1 . page ) ;
		// EditorPanel.openedWindows++;
		panel . add ( ep1 , BorderLayout . CENTER ) ;
		JButton jb1 ;
		panel . add ( jb1 = new JButton ( "Run!" ) , BorderLayout . SOUTH ) ;
		jb1 . addActionListener ( new ActionListener ( ) {
				@ Override
				public void actionPerformed ( ActionEvent e ) {
					try {
						runScript ( ppTarget , exportString ( ep1 . page . pageData . lines , "\n" ) ) ;
					} catch ( Exception e1 ) {
						System . out . println ( e1 ) ;
						StringWriter errors = new StringWriter ( ) ;
						e1 . printStackTrace ( new PrintWriter ( errors ) ) ;
						ep1 . page . ptEdit . append ( "\n/*\n" + errors . toString ( ) + "\n*/\n" ) ;
					}
				}
			} ) ;
		f . setDefaultCloseOperation ( JFrame . DISPOSE_ON_CLOSE ) ;
		f . getContentPane ( ) . add ( panel ) ;
		f . setSize ( 600 , 400 ) ;
		f . setLocationRelativeTo ( ppTarget . uiComp ) ;
		f . setVisible ( true ) ;
	}

	static void runScript ( final PlainPage ppTarget , String script )
	throws Exception {
		ReadonlyLines lines = ppTarget . pageData . roLines ;
		List < CharSequence > export = new ArrayList < > ( ) ;
		{
			int size = lines . getLinesize ( ) ;
			for ( int i = 0 ; i < size ; i ++ )
			export . add ( lines . getline ( i ) ) ; // hmmm...
		}
		new ScriptUtil ( ) . runSingleScript ( ppTarget , script , export ) ;
	}

	static void saveAs ( PlainPage page ) throws Exception {
		EditorPanel editor = page . uiComp ;
		JFileChooser chooser = new JFileChooser ( page . workPath ) ;
		int returnVal = chooser . showSaveDialog ( editor ) ;
		if ( returnVal == JFileChooser . APPROVE_OPTION ) {
			String fn = chooser . getSelectedFile ( ) . getAbsolutePath ( ) ;
			if ( new File ( fn ) . exists ( )
				&& JOptionPane . YES_OPTION
				!= JOptionPane . showConfirmDialog (
					editor , "file exists, are you sure to overwrite?" ,
					"save as..." , JOptionPane . YES_NO_OPTION ) ) {
				page . ui . message ( "not renamed" ) ;
				return ;
			}
			page . pageData . renameTo ( fn ) ;
			U . saveFileHistory ( fn , page . cy + 1 ) ;
			editor . changeTitle ( ) ;
			page . ui . message ( "file renamed" ) ;
			savePageToFile ( page ) ;
		}
	}

	static boolean saveFile ( PlainPage page ) throws Exception {
		if ( page . pageData . fileLoaded ) {
			String fn0 = page . pageData . title ;
			if ( ! page . pageData . changedOutside && new File ( fn0 ) . lastModified ( ) > page . pageData . fileLastModified )
			page . pageData . changedOutside = true ;
			if ( page . pageData . changedOutside
				&& JOptionPane . YES_OPTION
				!= JOptionPane . showConfirmDialog (
					page . uiComp ,
					"File Changed Outside!! Do you really want to overwrite it?" ,
					"File Changed Outside!!" , JOptionPane . YES_NO_OPTION ) ) {
				page . ui . message ( "saved canceled" ) ;
				return false ;
			}
			return savePageToFile ( page ) ;
		} else {
			JFileChooser chooser = new JFileChooser ( page . workPath ) ;
			int returnVal = chooser . showSaveDialog ( page . uiComp ) ;
			if ( returnVal != JFileChooser . APPROVE_OPTION )
			return false ;

			String fn = chooser . getSelectedFile ( ) . getAbsolutePath ( ) ;
			if ( new File ( fn ) . exists ( ) )
			if ( JOptionPane . YES_OPTION != JOptionPane . showConfirmDialog (
					page . uiComp , "Are you sure to overwrite?" , "File exists" ,
					JOptionPane . YES_NO_OPTION ) ) {
				page . ui . message ( "saved canceled" ) ;
				return false ;
			}
			PageData pd = page . pageData ;
			pd . renameTo ( fn ) ;
			return savePageToFile ( page ) ;
		}
	}

	static void saveFileHistory ( String fn , int line ) throws IOException {
		File fhn = getFileHistoryName ( ) ;
		OutputStream out = new FileOutputStream ( fhn , true ) ;
		out . write ( String . format ( "\n%s|%s:" , fn , line ) . getBytes ( UTF8 ) ) ;
		out . close ( ) ;
		saveDirHistory ( fn ) ;
	}

	static void saveFileHistorys ( String text ) throws IOException {
		File fhn = getFileHistoryName ( ) ;
		OutputStream out = new FileOutputStream ( fhn , true ) ;
		out . write ( text . getBytes ( UTF8 ) ) ;
		out . close ( ) ;
	}

	private static void saveDirHistory ( String fn ) throws IOException {
		File dir = new File ( fn ) . getParentFile ( ) ;
		if ( dir == null )
		return ;
		String s = dir . getAbsolutePath ( ) ;

		String old = FileUtil . readString ( new FileInputStream ( getDirHistoryName ( ) ) , null ) ;
		List < String > his = Arrays . asList ( old . split ( "\n" ) ) ;
		BufferedWriter out = new BufferedWriter ( new OutputStreamWriter (
				new FileOutputStream ( getDirHistoryName ( ) ) , UTF8 ) ) ;
		if ( ! his . contains ( s ) ) {
			out . write ( s ) ;
			out . write ( "\n" ) ;
			out . write ( old ) ;
		} else {
			out . write ( s ) ;
			out . write ( "\n" ) ;
			List < String > his2 = new ArrayList ( his ) ;
			his2 . remove ( s ) ;
			for ( String line : his2 ) {
				out . write ( line ) ;
				out . write ( "\n" ) ;
			}
		}
		out . close ( ) ;
	}

	static boolean savePageToFile ( PlainPage page ) throws Exception {
		try {
			String fn = page . pageData . title ;
			System . out . println ( "save " + fn ) ;
			File f = new File ( fn ) ;
			if ( page . pageData . encoding == null )
			page . pageData . encoding = UTF8 ;
			OutputStream out ;
			if ( fn . endsWith ( ".gz" ) )
			page . pageData . gzip = true ;
			String encoding = page . pageData . encoding ;
			if ( ! page . pageData . gzip )
			out = new BufferedOutputStream ( new FileOutputStream ( fn ) , 8192 * 16 ) ;
			else
			out = new BufferedOutputStream ( new GZIPOutputStream ( new FileOutputStream ( fn ) ) , 8192 * 16 ) ;
			byte [ ] sep = page . pageData . lineSep . getBytes ( encoding ) ;
			for ( int i = 0 ; i < page . pageData . lines . size ( ) ; i ++ ) {
				out . write ( page . pageData . lines . get ( i ) . toString ( ) . getBytes ( encoding ) ) ;
				out . write ( sep ) ;
			}
			out . close ( ) ;
			page . pageData . fileLastModified = f . lastModified ( ) ;
			page . pageData . changedOutside = false ;
			page . pageData . fileLoaded = true ;
			page . pageData . isCommentChecked = false ;
			page . workPath = f . getParent ( ) ;
			return true ;
		} catch ( Throwable ex ) {
			U . showSelfDispMessage ( page , "error when save file:" + ex , 1000 * 8 ) ;
			ex . printStackTrace ( ) ;
			return false ;
		}
	}

	static void scale ( int amount , Paint ui ) {
		if ( amount > 0 )
		ui . scalev *= 1.1f ;
		else if ( amount < 0 )
		ui . scalev *= 0.9f ;
	}

	static void setClipBoard ( String s ) {
		Toolkit . getDefaultToolkit ( ) . getSystemClipboard ( ) . setContents (
			new StringSelection ( s ) , null ) ;
	}

	static boolean setEncodingByUser ( PlainPage pp , String msg ) {
		String s = JOptionPane . showInputDialog ( pp . uiComp , msg , pp . pageData . encoding ) ;
		if ( s == null )
		return false ;
		try {
			"a" . getBytes ( s ) ;
		} catch ( Exception e ) {
			pp . ui . message ( "bad encoding:" + s ) ;
			return false ;
		}
		pp . pageData . encoding = s ;
		return true ;
	}

	public static void setFont ( EditorPanel ep , Font f ) throws Exception {
		ArrayList fonts = new ArrayList ( Arrays . asList ( Conf . defaultFontList . font ) ) ;
		fonts . add ( 0 , f ) ;
		FontList font2
		= new FontList ( ( Font [ ] ) fonts . toArray ( new Font [ fonts . size ( ) ] ) ) ;
		for ( PlainPage pp : ep . pageSet )
		pp . fontList = font2 ;
	}

	static void setFrameSize ( JFrame f ) {
		Dimension dim = Toolkit . getDefaultToolkit ( ) . getScreenSize ( ) ;
		Dimension p = Conf . readFrameSize ( ) ;
		f . setSize ( Math . min ( dim . width , p . width ) , Math . min ( p . height , dim . height ) ) ;
	}

	static void showHelp ( final Paint ui , final EditorPanel uiComp ) {
		if ( ui . aboutImg != null )
		return ;
		U . startDaemonThread ( new Thread ( ) {
				@ Override
				public void run ( ) {
					try {
						int w = uiComp . getWidth ( ) ;
						int h = 60 ;
						ui . aboutImg = new BufferedImage ( w , h , BufferedImage . TYPE_INT_ARGB ) ;
						Graphics2D gi = ui . aboutImg . createGraphics ( ) ;
						gi . setColor ( Color . BLUE ) ;
						gi . fillRect ( 0 , 0 , w , h ) ;
						gi . setColor ( Color . CYAN ) ;
						gi . setFont ( new Font ( "Arial" , Font . BOLD , 40 ) ) ;
						gi . drawString ( "NeoeEdit" , 6 , h - 20 ) ;
						gi . setFont ( new Font ( "Arial" , Font . PLAIN , 16 ) ) ;
						gi . setColor ( Color . ORANGE ) ;
						gi . drawString ( "ver:" + Version . REV , 220 , h - 22 ) ;
						gi . setColor ( Color . YELLOW ) ;
						gi . drawString ( "press F1 key to see all commands" , 6 , h - 6 ) ;
						gi . dispose ( ) ;
						ui . aboutY = - h ;
						ui . aboutOn = true ;
						for ( int i = - h ; i <= 0 ; i ++ ) {
							ui . aboutY = i ;
							uiComp . repaint ( ) ;
							Thread . sleep ( 500 / h ) ;
						}
						Thread . sleep ( 2000 ) ;
						for ( int i = 0 ; i >= - h ; i -- ) {
							ui . aboutY = i ;
							uiComp . repaint ( ) ;
							Thread . sleep ( 500 / h ) ;
						}
					} catch ( Exception e ) {
						e . printStackTrace ( ) ;
					} finally {
						ui . aboutOn = false ;
						ui . aboutImg = null ;
					}
				}
			} ) ;
	}

	public static PlainPage getPP ( EditorPanel editor , PageData data , PlainPage parent )
	throws Exception {
		PlainPage pp = U . findPageByData ( editor . pageSet , data ) ;
		if ( pp != null ) {
			editor . setPage ( pp , true ) ;
			return pp ;
		}
		return new PlainPage ( editor , data , parent ) ;
	}

	public static void showHexOfString ( String s , PlainPage pp ) throws Exception {
		PlainPage p2 = new PlainPage ( pp . uiComp , PageData . fromTitle ( String . format ( "Hex for String #%s" , randomID ( ) ) ) , pp ) ;
		List < CharSequence > sbs = new ArrayList < CharSequence > ( ) ;
		sbs . add ( new StringBuilder ( String . format ( "Hex for '%s'" , s ) ) ) ;
		for ( char c : s . toCharArray ( ) )
		sbs . add ( c + ":" + UnicodeFormatter . charToHex ( c ) ) ;
		p2 . pageData . resetLines ( sbs ) ;
	}

	public static void showPageListPage ( EditorPanel ep ) throws Exception {
		if ( ep . findAndShowPage ( titleOfPages ( ep ) , 0 , true ) ) {
			ep . page . pageData . resetLines ( getPageListStrings ( ep ) ) ; // refresh
			ep . repaint ( ) ;
			return ;
		}
		// boolean isFirstTime = !PageData.dataPool.containsKey(TITLE_OF_PAGES);
		PageData pd = PageData . fromTitle ( titleOfPages ( ep ) ) ;
		new PlainPage ( ep , pd , ep . page ) ;
		pd . resetLines ( getPageListStrings ( ep ) ) ;
		ep . repaint ( ) ;
	}

	public static void listDoc ( EditorPanel ep ) throws Exception {
		String theTitle = "[DOC]" ;
		if ( ep . findAndShowPage ( theTitle , 0 , true ) ) {
			ep . page . pageData . resetLines ( getDocListStrings ( ) ) ; // refresh
			ep . repaint ( ) ;
			return ;
		}
		// boolean isFirstTime = !PageData.dataPool.containsKey(TITLE_OF_PAGES);
		PageData pd = PageData . fromTitle ( theTitle ) ;
		new PlainPage ( ep , pd , ep . page ) ;
		pd . resetLines ( getDocListStrings ( ) ) ;
		ep . repaint ( ) ;
	}

	public static void showSelfDispMessage ( PlainPage pp , String msg , int disapearMS ) {
		long now = System . currentTimeMillis ( ) ;
		pp . ui . msgs . add ( new Object [ ] { msg , now + disapearMS , -1 /* draw width */ } ) ;
		repaintAfter ( 4000 , pp . uiComp ) ;
	}

	static String SPACES = "                                                                                         " ;

	static String spaces ( int cx ) {
		if ( cx <= 0 )
		return "" ;
		if ( cx <= SPACES . length ( ) )
		return SPACES . substring ( 0 , cx ) ;
		StringBuilder sb = new StringBuilder ( cx ) ;
		sb . setLength ( cx ) ;
		for ( int i = 0 ; i < cx ; i ++ )
		sb . setCharAt ( i , ' ' ) ;
		return sb . toString ( ) ;
	}

	static List < CharSequence > splitToken ( CharSequence s ) {
		StringBuilder sb = new StringBuilder ( ) ;
		List < CharSequence > sl = new ArrayList < > ( ) ;
		for ( int i = 0 ; i < s . length ( ) ; i ++ ) {
			char c = s . charAt ( i ) ;
			if ( ! Character . isJavaIdentifierPart ( c ) ) {
				if ( sb . length ( ) > 0 ) {
					sl . add ( sb . toString ( ) ) ;
					sb . setLength ( 0 ) ;
				}
				sl . add ( "" + c ) ;
			} else
			sb . append ( c ) ;
		}
		if ( sb . length ( ) > 0 ) {
			sl . add ( sb . toString ( ) ) ;
			sb . setLength ( 0 ) ;
		}
		return sl ;
	}

	static List < String > split ( String all , char sep ) {
		List < String > s1 = new ArrayList < > ( ) ;
		int p1 = 0 ;
		while ( true ) {
			int p2 = all . indexOf ( sep , p1 ) ;
			if ( p2 < 0 ) {
				String s2 = ( String ) all . subSequence ( p1 , all . length ( ) ) ;
				s1 . add ( s2 ) ;
				break ;
			} else {
				String s2 = ( String ) all . subSequence ( p1 , p2 ) ;
				s1 . add ( s2 ) ;
				p1 = p2 + 1 ;
			}
		}
		return s1 ;
	}

	static void startNoiseThread ( final Paint ui , final EditorPanel uiComp ) {
		U . startDaemonThread ( new Thread ( ) {
				@ Override
				public void run ( ) {
					try { // noise thread
						while ( true )
						if ( ui . noise && ! ui . closed ) {
							uiComp . repaint ( ) ;
							// System.out.println("paint noise");
							Thread . sleep ( ui . noisesleep ) ;
						} else
						break ;
						System . out . println ( "noise stopped" ) ;
					} catch ( InterruptedException e ) {
						e . printStackTrace ( ) ;
					}
				}
			} ) ;
	}

	static CharSequence subs ( CharSequence sb , int a , int b ) {
		if ( a >= b )
		return "" ;
		if ( a >= sb . length ( ) )
		return "" ;
		if ( a < 0 || b < 0 )
		return "" ;
		if ( b > sb . length ( ) )
		b = sb . length ( ) ;
		return sb . subSequence ( a , b ) ;
	}

	static String titleOfPages ( EditorPanel ep ) {
		return "[PAGES]#" + Integer . toString ( ep . hashCode ( ) , 36 ) ;
	}

	static CharSequence trimLeft ( CharSequence s ) {
		int i = 0 ;
		while ( i < s . length ( ) && ( s . charAt ( i ) == ' ' || s . charAt ( i ) == '\t' ) )
		i ++ ;
		return i > 0 ? s . subSequence ( i , s . length ( ) ) : s ;
	}

	public static String exportString ( List < CharSequence > ss , String lineSep ) {
		StringBuilder sb = new StringBuilder ( ) ;
		boolean notfirst = false ;
		for ( CharSequence cs : ss ) {
			if ( notfirst )
			sb . append ( lineSep ) ;
			else
			notfirst = true ;
			sb . append ( cs ) ;
		}
		return sb . toString ( ) ;
	}

	public static List < CharSequence > removeTailR ( List < String > split ) {
		List < CharSequence > r = new ArrayList < > ( ) ;
		for ( String s : split ) {
			s = U . removeTailR ( s ) . toString ( ) ;
			// for console
			if ( s . startsWith ( "\r" ) )
			s = s . substring ( 1 ) ;
			if ( s . contains ( "\r" ) ) { // lines that replacing the last line
				String [ ] ss = s . split ( "\\r" ) ;
				for ( String s1 : ss )
				r . add ( s1 ) ;
			} else
			r . add ( s ) ;
		}
		return r ;
	}

	public static int between ( int i , int min , int max ) {
		return Math . min ( max , Math . max ( min , i ) ) ;
	}

	public static final String e_png = "e.jpg" ;
	public static final String e2_png = "e2.jpg" ;
	public static final String e3_png = "e3.jpg" ;
	static Map < String , Image > appIcons = new HashMap ( ) ;

	public static Image getAppIcon ( String name ) throws IOException {
		Image appIcon = appIcons . get ( name ) ;
		if ( appIcon != null )
		return appIcon ;
		appIcon = ImageIO . read ( EditorPanel . class . getResourceAsStream ( "/" + name ) ) ;
		appIcons . put ( name , appIcon ) ;
		return appIcon ;
	}

	public static boolean isSpaceChar ( char ch ) {
		return Character . isSpaceChar ( ch ) || ch == '\t' ;
	}

	public static void changePathSep ( PageData pageData , int cy ) {
		if ( cy >= pageData . lines . size ( ) )
		return ;
		String line = pageData . lines . get ( cy ) . toString ( ) ;
		int p1 = line . indexOf ( '/' ) ;
		String line2 = null ;
		if ( p1 >= 0 )
		line2 = line . replace ( '/' , '\\' ) ;
		else {
			int p2 = line . indexOf ( '\\' ) ;
			if ( p2 >= 0 )
			line2 = line . replace ( '\\' , '/' ) ;
		}
		if ( line2 != null ) {
			pageData . editRec . deleteInLine ( cy , 0 , line . length ( ) ) ;
			pageData . editRec . insertInLine ( cy , 0 , line2 ) ;
		}
	}

	public static String evalMath ( final String str ) {
		System . out . println ( "eval:" + str ) ;
		return new MathExprParser ( str ) . parse ( ) . stripTrailingZeros ( ) . toPlainString ( ) ;
	}

	public static String getMathExprTail ( String ss ) {
		int p1 = ss . length ( ) ;
		while ( p1 > 0 && isMathExprChar ( ss . charAt ( p1 - 1 ) ) )
		p1 -- ;
		ss = ss . substring ( p1 ) ;
		for ( int i = 0 ; i < ss . length ( ) ; i ++ ) {
			char ch = ss . charAt ( i ) ;
			if ( "+-/*^x" . indexOf ( ch ) >= 0 )
			return ss ;
		}
		return "" ;
	}

	static boolean isMathExprChar ( char c ) {
		return isMathExprNumberChar ( c ) || ( "+-/*^() " . indexOf ( c ) >= 0 ) ;
	}

	static boolean isMathExprNumberChar ( int c ) {
		return ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'f' )
		|| ( c >= 'A' && c <= 'F' ) || c == '.' || c == ',' || c == 'E' || c == 'x' ;
	}

	public static char charAtWhenMove ( CharSequence line , int index ) {
		if ( line . length ( ) == 0 )
		return ' ' ;
		if ( index >= line . length ( ) )
		return ' ' ;
		return line . charAt ( index ) ;
	}

	public static boolean getBool ( Object o ) {
		if ( o == null )
		return false ;
		if ( o instanceof Boolean )
		return ( ( Boolean ) o ) . booleanValue ( ) ;
		String s = o . toString ( ) . toLowerCase ( ) ;
		if ( "y" . equals ( s ) || "1" . equals ( s ) || "true" . equals ( s ) )
		return true ;
		if ( "n" . equals ( s ) || "0" . equals ( s ) || "false" . equals ( s ) )
		return false ;
		return false ;
	}

	public static int getInt ( Object o ) {
		if ( o == null )
		return 0 ;
		if ( o instanceof Number )
		return ( ( Number ) o ) . intValue ( ) ;
		return ( int ) Float . parseFloat ( o . toString ( ) ) ;
	}

	public static float getFloat ( Object o ) {
		if ( o == null )
		return 0 ;
		if ( o instanceof Number )
		return ( ( Number ) o ) . floatValue ( ) ;
		return Float . parseFloat ( o . toString ( ) ) ;
	}

	/*
   * show str will short then show char by char in font, so ret is shorted
   * and approximately and a pre-cut. the purpose is just avoid show a string
   * like 10000 chars in later draw
	 */
	public static int maxShowIndexApproximate ( CharSequence sb , int sx , int W ,
		Graphics2D g2 , FontList fonts ) {
		int w = 0 ;
		for ( int i = sx ; i < sb . length ( ) - 1 ; i ++ ) {
			char c = sb . charAt ( i ) ;
			if ( c == '\t' )
			w += TAB_WIDTH ;
			else
			w += charWidth ( g2 , fonts , c ) ;
			if ( w > W )
			return i + 1 ;
		}
		return sb . length ( ) ;
	}

	public static void openFileSelector ( String line , PlainPage pp ) {
		File dir = findFile ( pp . workPath , line ) ;
		if ( dir == null ) {
			pp . ui . message ( "cannot find filename in current line" ) ;
			return ;
		}
		JFileChooser c = new JFileChooser ( dir ) ;
		c . setFileSelectionMode ( JFileChooser . FILES_AND_DIRECTORIES ) ;
		c . setDialogTitle ( "browse and copy file name" ) ;
		c . setPreferredSize ( new Dimension ( 800 , 600 ) ) ;
		int r = c . showOpenDialog ( null ) ;
		if ( r == JFileChooser . APPROVE_OPTION ) {
			String s = c . getSelectedFile ( ) . getAbsolutePath ( ) ;
			U . setClipBoard ( s ) ;
			pp . ui . message ( "filename copied" ) ;
		}
	}

	public static PlainPage findPageByData ( List < PlainPage > pageSet ,
		PageData data ) {
		for ( PlainPage pp : pageSet )
		if ( pp . pageData . equals ( data ) )
		return pp ;
		return null ;
	}

	public static int optimizeFileHistory ( ) throws IOException {
		File fhn = U . getFileHistoryName ( ) ;
		List < String > fs = Files . readAllLines ( fhn . toPath ( ) ) ;
		Set < String > e = new HashSet < > ( ) ;
		List < String > fs2 = new ArrayList < > ( ) ;
		int cy = 0 ;
		for ( int i = fs . size ( ) - 1 ; i >= 0 ; i -- ) {
			String s = fs . get ( i ) ;
			if ( s . endsWith ( "|0:" ) ) s = s . substring ( 0 , s . length ( ) -3 ) ;
			int p1 = s . lastIndexOf ( '|' ) ;
			String fn = s . trim ( ) ;
			if ( fn . isEmpty ( ) )
			continue ;
			if ( p1 > 0 )
			fn = s . substring ( 0 , p1 ) ;
			if ( e . contains ( fn ) )
			continue ;
			e . add ( fn ) ;
			fs2 . add ( s ) ;
		}
		Collections . reverse ( fs2 ) ;
		U . save ( fs2 , UTF8 , fhn . getAbsolutePath ( ) ) ;
		System . out . println ( "file history optimized" ) ;
		return cy ;
	}
}
