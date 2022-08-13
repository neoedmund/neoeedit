package neoe . ne ;

import java . math . BigDecimal ;
import java . math . BigInteger ;

/**
 * http://stackoverflow.com/questions/3422673/evaluating-a-math-expression-
 * given-in-string-form<br/>
 * and modified to support HEX(0xBEEF) and Scientific form(1E10) and BigDecimal
 */
public class MathExprParser {
	int pos = -1 , c ;
	private String str ;

	public MathExprParser ( String str ) {
		this . str = str ;
	}

	void eatChar ( ) {
		c = ( ++ pos < str . length ( ) ) ? str . charAt ( pos ) : -1 ;
	}

	void eatSpace ( ) {
		while ( Character . isWhitespace ( c ) )
		eatChar ( ) ;
	}

	public BigDecimal parse ( ) {
		eatChar ( ) ;
		BigDecimal v = parseExpression ( ) ;
		if ( c != -1 )
		throw new RuntimeException ( "Unexpected: " + ( char ) c ) ;
		return v ;
	}

	// Grammar:
	// expression = term | expression `+` term | expression `-` term
	// term = factor | term `*` factor | term `/` factor | term brackets
	// factor = brackets | number | factor `^` factor
	// brackets = `(` expression `)`
	BigDecimal parseExpression ( ) {
		BigDecimal v = parseTerm ( ) ;
		while ( true ) {
			eatSpace ( ) ;
			if ( c == '+' ) { // addition
				eatChar ( ) ;
				v = v . add ( parseTerm ( ) ) ;
			} else if ( c == '-' ) { // subtraction
				eatChar ( ) ;
				v = v . subtract ( parseTerm ( ) ) ;
			} else
			return v ;
		}
	}

	BigDecimal parseTerm ( ) {
		BigDecimal v = parseFactor ( ) ;
		while ( true ) {
			eatSpace ( ) ;
			if ( c == '/' ) { // division
				eatChar ( ) ;
				v = v . divide ( parseFactor ( ) , 8 , BigDecimal . ROUND_HALF_UP ) ;
			} else if ( c == '%' ) {
				eatChar ( ) ;
				v = new BigDecimal ( v . longValue ( ) % parseFactor ( ) . longValue ( ) ) ;
			} else if ( c == '*' || c == '(' ) { // multiplication
				if ( c == '*' )
				eatChar ( ) ;
				v = v . multiply ( parseFactor ( ) ) ;
			} else
			return v ;
		}
	}

	BigDecimal parseFactor ( ) {
		BigDecimal v ;
		boolean negate = false ;
		eatSpace ( ) ;
		if ( c == '+' || c == '-' ) { // unary plus & minus
			negate = c == '-' ;
			eatChar ( ) ;
			eatSpace ( ) ;
		}
		if ( c == '(' ) { // brackets
			eatChar ( ) ;
			v = parseExpression ( ) ;
			if ( c == ')' )
			eatChar ( ) ;
		} else { // numbers
			StringBuilder sb = new StringBuilder ( ) ;
			while ( U . isMathExprNumberChar ( c ) ) {
				sb . append ( ( char ) c ) ;
				eatChar ( ) ;
			}
			if ( sb . length ( ) == 0 )
			throw new RuntimeException ( "Unexpected: " + ( char ) c ) ;
			if ( sb . length ( ) >= 2 && sb . substring ( 0 , 2 ) . equals ( "0x" ) )
			v = new BigDecimal ( new BigInteger ( sb . substring ( 2 ) , 16 ) ) ;
			else if ( sb . length ( ) >= 2 && sb . substring ( 0 , 2 ) . equals ( "0b" ) )
			v = new BigDecimal ( new BigInteger ( sb . substring ( 2 ) , 2 ) ) ;
			else
			v = new BigDecimal ( removeComma ( sb ) . toString ( ) ) ;
		}
		eatSpace ( ) ;
		if ( c == '^' ) { // exponentiation
			eatChar ( ) ;
			v = BigDecimal . valueOf ( Math . pow ( v . doubleValue ( ) , parseFactor ( ) . doubleValue ( ) ) ) ;
		}
		if ( negate ) // unary minus is applied after exponentiation; e.g. -3^2=-9
		v = v . negate ( ) ;

		return v ;
	}

	private StringBuilder removeComma ( StringBuilder sb ) {
		while ( true ) {
			int p1 = sb . indexOf ( "," ) ;
			if ( p1 < 0 )
			return sb ;
			sb . deleteCharAt ( p1 ) ;
		}
	}
}
