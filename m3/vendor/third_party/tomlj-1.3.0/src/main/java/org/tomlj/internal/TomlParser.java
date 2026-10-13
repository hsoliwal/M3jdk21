// Generated from org/tomlj/internal/TomlParser.g4 by ANTLR 4.13.2

package org.tomlj.internal;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class TomlParser extends AbstractTomlParser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		TripleQuotationMark=1, TripleApostrophe=2, StringChars=3, Comma=4, Dot=5, 
		Equals=6, QuotationMark=7, Apostrophe=8, TableKeyStart=9, TableKeyEnd=10, 
		ArrayTableKeyStart=11, ArrayTableKeyEnd=12, UnquotedKey=13, WS=14, Comment=15, 
		NewLine=16, Error=17, DecimalInteger=18, HexInteger=19, OctalInteger=20, 
		BinaryInteger=21, FloatingPoint=22, FloatingPointInf=23, FloatingPointNaN=24, 
		TrueBoolean=25, FalseBoolean=26, ArrayStart=27, ArrayEnd=28, InlineTableStart=29, 
		EscapeSequence=30, MLBasicStringLineEndBackslash=31, Dash=32, Plus=33, 
		Colon=34, Z=35, TimeDelimiter=36, DateDigits=37, InlineTableEnd=38, ValueComma=39;
	public static final int
		RULE_toml = 0, RULE_expression = 1, RULE_tomlKey = 2, RULE_keyval = 3, 
		RULE_key = 4, RULE_simpleKey = 5, RULE_unquotedKey = 6, RULE_quotedKey = 7, 
		RULE_val = 8, RULE_string = 9, RULE_basicString = 10, RULE_basicChar = 11, 
		RULE_basicUnescaped = 12, RULE_escaped = 13, RULE_mlBasicString = 14, 
		RULE_mlBasicChar = 15, RULE_mlBasicUnescaped = 16, RULE_literalString = 17, 
		RULE_literalBody = 18, RULE_mlLiteralString = 19, RULE_mlLiteralBody = 20, 
		RULE_integer = 21, RULE_decInt = 22, RULE_hexInt = 23, RULE_octInt = 24, 
		RULE_binInt = 25, RULE_floatValue = 26, RULE_regularFloat = 27, RULE_regularFloatInf = 28, 
		RULE_regularFloatNaN = 29, RULE_booleanValue = 30, RULE_trueBool = 31, 
		RULE_falseBool = 32, RULE_dateTime = 33, RULE_offsetDateTime = 34, RULE_localDateTime = 35, 
		RULE_localDate = 36, RULE_localTime = 37, RULE_date = 38, RULE_time = 39, 
		RULE_timeOffset = 40, RULE_hourOffset = 41, RULE_minuteOffset = 42, RULE_secondFraction = 43, 
		RULE_year = 44, RULE_month = 45, RULE_day = 46, RULE_hour = 47, RULE_minute = 48, 
		RULE_second = 49, RULE_array = 50, RULE_arrayValues = 51, RULE_arrayValue = 52, 
		RULE_table = 53, RULE_standardTable = 54, RULE_inlineTable = 55, RULE_inlineTableValues = 56, 
		RULE_inlineTableValue = 57, RULE_arrayTable = 58;
	private static String[] makeRuleNames() {
		return new String[] {
			"toml", "expression", "tomlKey", "keyval", "key", "simpleKey", "unquotedKey", 
			"quotedKey", "val", "string", "basicString", "basicChar", "basicUnescaped", 
			"escaped", "mlBasicString", "mlBasicChar", "mlBasicUnescaped", "literalString", 
			"literalBody", "mlLiteralString", "mlLiteralBody", "integer", "decInt", 
			"hexInt", "octInt", "binInt", "floatValue", "regularFloat", "regularFloatInf", 
			"regularFloatNaN", "booleanValue", "trueBool", "falseBool", "dateTime", 
			"offsetDateTime", "localDateTime", "localDate", "localTime", "date", 
			"time", "timeOffset", "hourOffset", "minuteOffset", "secondFraction", 
			"year", "month", "day", "hour", "minute", "second", "array", "arrayValues", 
			"arrayValue", "table", "standardTable", "inlineTable", "inlineTableValues", 
			"inlineTableValue", "arrayTable"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, "']'", "'[['", 
			"']]'", null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, "'-'", "'+'", "':'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "TripleQuotationMark", "TripleApostrophe", "StringChars", "Comma", 
			"Dot", "Equals", "QuotationMark", "Apostrophe", "TableKeyStart", "TableKeyEnd", 
			"ArrayTableKeyStart", "ArrayTableKeyEnd", "UnquotedKey", "WS", "Comment", 
			"NewLine", "Error", "DecimalInteger", "HexInteger", "OctalInteger", "BinaryInteger", 
			"FloatingPoint", "FloatingPointInf", "FloatingPointNaN", "TrueBoolean", 
			"FalseBoolean", "ArrayStart", "ArrayEnd", "InlineTableStart", "EscapeSequence", 
			"MLBasicStringLineEndBackslash", "Dash", "Plus", "Colon", "Z", "TimeDelimiter", 
			"DateDigits", "InlineTableEnd", "ValueComma"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "TomlParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public TomlParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TomlContext extends AbstractTomlParserRuleContext {
		public TerminalNode EOF() { return getToken(TomlParser.EOF, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> NewLine() { return getTokens(TomlParser.NewLine); }
		public TerminalNode NewLine(int i) {
			return getToken(TomlParser.NewLine, i);
		}
		public TomlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_toml; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterToml(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitToml(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitToml(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TomlContext toml() throws RecognitionException {
		TomlContext _localctx = new TomlContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_toml);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(119);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 11136L) != 0)) {
				{
				setState(118);
				expression();
				}
			}

			setState(127);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NewLine) {
				{
				{
				setState(121);
				match(NewLine);
				setState(123);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 11136L) != 0)) {
					{
					setState(122);
					expression();
					}
				}

				}
				}
				setState(129);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(130);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends AbstractTomlParserRuleContext {
		public KeyvalContext keyval() {
			return getRuleContext(KeyvalContext.class,0);
		}
		public TableContext table() {
			return getRuleContext(TableContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_expression);
		try {
			setState(134);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case QuotationMark:
			case Apostrophe:
			case UnquotedKey:
				enterOuterAlt(_localctx, 1);
				{
				setState(132);
				keyval();
				}
				break;
			case TableKeyStart:
			case ArrayTableKeyStart:
				enterOuterAlt(_localctx, 2);
				{
				setState(133);
				table();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TomlKeyContext extends AbstractTomlParserRuleContext {
		public KeyContext key() {
			return getRuleContext(KeyContext.class,0);
		}
		public TerminalNode EOF() { return getToken(TomlParser.EOF, 0); }
		public TomlKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tomlKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterTomlKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitTomlKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitTomlKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TomlKeyContext tomlKey() throws RecognitionException {
		TomlKeyContext _localctx = new TomlKeyContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_tomlKey);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(136);
			key();
			setState(137);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class KeyvalContext extends AbstractTomlParserRuleContext {
		public int nesting;
		public KeyContext key() {
			return getRuleContext(KeyContext.class,0);
		}
		public TerminalNode Equals() { return getToken(TomlParser.Equals, 0); }
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
		public KeyvalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keyval; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterKeyval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitKeyval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitKeyval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeyvalContext keyval() throws RecognitionException {
		KeyvalContext _localctx = new KeyvalContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_keyval);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(139);
			key();
			setState(140);
			match(Equals);
			setState(141);
			val();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class KeyContext extends AbstractTomlParserRuleContext {
		public List<SimpleKeyContext> simpleKey() {
			return getRuleContexts(SimpleKeyContext.class);
		}
		public SimpleKeyContext simpleKey(int i) {
			return getRuleContext(SimpleKeyContext.class,i);
		}
		public List<TerminalNode> Dot() { return getTokens(TomlParser.Dot); }
		public TerminalNode Dot(int i) {
			return getToken(TomlParser.Dot, i);
		}
		public KeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_key; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeyContext key() throws RecognitionException {
		KeyContext _localctx = new KeyContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_key);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(143);
			simpleKey();
			setState(148);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==Dot) {
				{
				{
				setState(144);
				match(Dot);
				setState(145);
				simpleKey();
				}
				}
				setState(150);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SimpleKeyContext extends AbstractTomlParserRuleContext {
		public QuotedKeyContext quotedKey() {
			return getRuleContext(QuotedKeyContext.class,0);
		}
		public UnquotedKeyContext unquotedKey() {
			return getRuleContext(UnquotedKeyContext.class,0);
		}
		public SimpleKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterSimpleKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitSimpleKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitSimpleKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SimpleKeyContext simpleKey() throws RecognitionException {
		SimpleKeyContext _localctx = new SimpleKeyContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_simpleKey);
		try {
			setState(153);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case QuotationMark:
			case Apostrophe:
				enterOuterAlt(_localctx, 1);
				{
				setState(151);
				quotedKey();
				}
				break;
			case UnquotedKey:
				enterOuterAlt(_localctx, 2);
				{
				setState(152);
				unquotedKey();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnquotedKeyContext extends AbstractTomlParserRuleContext {
		public TerminalNode UnquotedKey() { return getToken(TomlParser.UnquotedKey, 0); }
		public UnquotedKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unquotedKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterUnquotedKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitUnquotedKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitUnquotedKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnquotedKeyContext unquotedKey() throws RecognitionException {
		UnquotedKeyContext _localctx = new UnquotedKeyContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_unquotedKey);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(155);
			match(UnquotedKey);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class QuotedKeyContext extends AbstractTomlParserRuleContext {
		public BasicStringContext basicString() {
			return getRuleContext(BasicStringContext.class,0);
		}
		public LiteralStringContext literalString() {
			return getRuleContext(LiteralStringContext.class,0);
		}
		public QuotedKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quotedKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterQuotedKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitQuotedKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitQuotedKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QuotedKeyContext quotedKey() throws RecognitionException {
		QuotedKeyContext _localctx = new QuotedKeyContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_quotedKey);
		try {
			setState(159);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case QuotationMark:
				enterOuterAlt(_localctx, 1);
				{
				setState(157);
				basicString();
				}
				break;
			case Apostrophe:
				enterOuterAlt(_localctx, 2);
				{
				setState(158);
				literalString();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ValContext extends AbstractTomlParserRuleContext {
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public IntegerContext integer() {
			return getRuleContext(IntegerContext.class,0);
		}
		public FloatValueContext floatValue() {
			return getRuleContext(FloatValueContext.class,0);
		}
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public DateTimeContext dateTime() {
			return getRuleContext(DateTimeContext.class,0);
		}
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public InlineTableContext inlineTable() {
			return getRuleContext(InlineTableContext.class,0);
		}
		public ValContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_val; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterVal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitVal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitVal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValContext val() throws RecognitionException {
		ValContext _localctx = new ValContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_val);
		try {
			enterOuterAlt(_localctx, 1);
			{
			checkNestingDepth();
			setState(169);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TripleQuotationMark:
			case TripleApostrophe:
			case QuotationMark:
			case Apostrophe:
				{
				setState(162);
				string();
				}
				break;
			case DecimalInteger:
			case HexInteger:
			case OctalInteger:
			case BinaryInteger:
				{
				setState(163);
				integer();
				}
				break;
			case FloatingPoint:
			case FloatingPointInf:
			case FloatingPointNaN:
				{
				setState(164);
				floatValue();
				}
				break;
			case TrueBoolean:
			case FalseBoolean:
				{
				setState(165);
				booleanValue();
				}
				break;
			case DateDigits:
				{
				setState(166);
				dateTime();
				}
				break;
			case ArrayStart:
				{
				setState(167);
				array();
				}
				break;
			case InlineTableStart:
				{
				setState(168);
				inlineTable();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StringContext extends AbstractTomlParserRuleContext {
		public MlBasicStringContext mlBasicString() {
			return getRuleContext(MlBasicStringContext.class,0);
		}
		public BasicStringContext basicString() {
			return getRuleContext(BasicStringContext.class,0);
		}
		public MlLiteralStringContext mlLiteralString() {
			return getRuleContext(MlLiteralStringContext.class,0);
		}
		public LiteralStringContext literalString() {
			return getRuleContext(LiteralStringContext.class,0);
		}
		public StringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_string; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringContext string() throws RecognitionException {
		StringContext _localctx = new StringContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_string);
		try {
			setState(175);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TripleQuotationMark:
				enterOuterAlt(_localctx, 1);
				{
				setState(171);
				mlBasicString();
				}
				break;
			case QuotationMark:
				enterOuterAlt(_localctx, 2);
				{
				setState(172);
				basicString();
				}
				break;
			case TripleApostrophe:
				enterOuterAlt(_localctx, 3);
				{
				setState(173);
				mlLiteralString();
				}
				break;
			case Apostrophe:
				enterOuterAlt(_localctx, 4);
				{
				setState(174);
				literalString();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BasicStringContext extends AbstractTomlParserRuleContext {
		public List<TerminalNode> QuotationMark() { return getTokens(TomlParser.QuotationMark); }
		public TerminalNode QuotationMark(int i) {
			return getToken(TomlParser.QuotationMark, i);
		}
		public List<BasicCharContext> basicChar() {
			return getRuleContexts(BasicCharContext.class);
		}
		public BasicCharContext basicChar(int i) {
			return getRuleContext(BasicCharContext.class,i);
		}
		public BasicStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_basicString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterBasicString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitBasicString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitBasicString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BasicStringContext basicString() throws RecognitionException {
		BasicStringContext _localctx = new BasicStringContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_basicString);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(177);
			match(QuotationMark);
			setState(181);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==StringChars || _la==EscapeSequence) {
				{
				{
				setState(178);
				basicChar();
				}
				}
				setState(183);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(184);
			match(QuotationMark);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BasicCharContext extends AbstractTomlParserRuleContext {
		public BasicUnescapedContext basicUnescaped() {
			return getRuleContext(BasicUnescapedContext.class,0);
		}
		public EscapedContext escaped() {
			return getRuleContext(EscapedContext.class,0);
		}
		public BasicCharContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_basicChar; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterBasicChar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitBasicChar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitBasicChar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BasicCharContext basicChar() throws RecognitionException {
		BasicCharContext _localctx = new BasicCharContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_basicChar);
		try {
			setState(188);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case StringChars:
				enterOuterAlt(_localctx, 1);
				{
				setState(186);
				basicUnescaped();
				}
				break;
			case EscapeSequence:
				enterOuterAlt(_localctx, 2);
				{
				setState(187);
				escaped();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BasicUnescapedContext extends AbstractTomlParserRuleContext {
		public TerminalNode StringChars() { return getToken(TomlParser.StringChars, 0); }
		public BasicUnescapedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_basicUnescaped; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterBasicUnescaped(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitBasicUnescaped(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitBasicUnescaped(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BasicUnescapedContext basicUnescaped() throws RecognitionException {
		BasicUnescapedContext _localctx = new BasicUnescapedContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_basicUnescaped);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(190);
			match(StringChars);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EscapedContext extends AbstractTomlParserRuleContext {
		public TerminalNode EscapeSequence() { return getToken(TomlParser.EscapeSequence, 0); }
		public EscapedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_escaped; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterEscaped(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitEscaped(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitEscaped(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EscapedContext escaped() throws RecognitionException {
		EscapedContext _localctx = new EscapedContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_escaped);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(192);
			match(EscapeSequence);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MlBasicStringContext extends AbstractTomlParserRuleContext {
		public List<TerminalNode> TripleQuotationMark() { return getTokens(TomlParser.TripleQuotationMark); }
		public TerminalNode TripleQuotationMark(int i) {
			return getToken(TomlParser.TripleQuotationMark, i);
		}
		public List<MlBasicCharContext> mlBasicChar() {
			return getRuleContexts(MlBasicCharContext.class);
		}
		public MlBasicCharContext mlBasicChar(int i) {
			return getRuleContext(MlBasicCharContext.class,i);
		}
		public MlBasicStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mlBasicString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterMlBasicString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitMlBasicString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitMlBasicString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MlBasicStringContext mlBasicString() throws RecognitionException {
		MlBasicStringContext _localctx = new MlBasicStringContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_mlBasicString);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(194);
			match(TripleQuotationMark);
			setState(198);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==StringChars || _la==EscapeSequence) {
				{
				{
				setState(195);
				mlBasicChar();
				}
				}
				setState(200);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(201);
			match(TripleQuotationMark);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MlBasicCharContext extends AbstractTomlParserRuleContext {
		public MlBasicUnescapedContext mlBasicUnescaped() {
			return getRuleContext(MlBasicUnescapedContext.class,0);
		}
		public EscapedContext escaped() {
			return getRuleContext(EscapedContext.class,0);
		}
		public MlBasicCharContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mlBasicChar; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterMlBasicChar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitMlBasicChar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitMlBasicChar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MlBasicCharContext mlBasicChar() throws RecognitionException {
		MlBasicCharContext _localctx = new MlBasicCharContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_mlBasicChar);
		try {
			setState(205);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case StringChars:
				enterOuterAlt(_localctx, 1);
				{
				setState(203);
				mlBasicUnescaped();
				}
				break;
			case EscapeSequence:
				enterOuterAlt(_localctx, 2);
				{
				setState(204);
				escaped();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MlBasicUnescapedContext extends AbstractTomlParserRuleContext {
		public TerminalNode StringChars() { return getToken(TomlParser.StringChars, 0); }
		public MlBasicUnescapedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mlBasicUnescaped; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterMlBasicUnescaped(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitMlBasicUnescaped(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitMlBasicUnescaped(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MlBasicUnescapedContext mlBasicUnescaped() throws RecognitionException {
		MlBasicUnescapedContext _localctx = new MlBasicUnescapedContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_mlBasicUnescaped);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(207);
			match(StringChars);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralStringContext extends AbstractTomlParserRuleContext {
		public List<TerminalNode> Apostrophe() { return getTokens(TomlParser.Apostrophe); }
		public TerminalNode Apostrophe(int i) {
			return getToken(TomlParser.Apostrophe, i);
		}
		public LiteralBodyContext literalBody() {
			return getRuleContext(LiteralBodyContext.class,0);
		}
		public LiteralStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literalString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterLiteralString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitLiteralString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitLiteralString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralStringContext literalString() throws RecognitionException {
		LiteralStringContext _localctx = new LiteralStringContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_literalString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(209);
			match(Apostrophe);
			setState(210);
			literalBody();
			setState(211);
			match(Apostrophe);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralBodyContext extends AbstractTomlParserRuleContext {
		public List<TerminalNode> StringChars() { return getTokens(TomlParser.StringChars); }
		public TerminalNode StringChars(int i) {
			return getToken(TomlParser.StringChars, i);
		}
		public LiteralBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literalBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterLiteralBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitLiteralBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitLiteralBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralBodyContext literalBody() throws RecognitionException {
		LiteralBodyContext _localctx = new LiteralBodyContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_literalBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(216);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==StringChars) {
				{
				{
				setState(213);
				match(StringChars);
				}
				}
				setState(218);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MlLiteralStringContext extends AbstractTomlParserRuleContext {
		public List<TerminalNode> TripleApostrophe() { return getTokens(TomlParser.TripleApostrophe); }
		public TerminalNode TripleApostrophe(int i) {
			return getToken(TomlParser.TripleApostrophe, i);
		}
		public MlLiteralBodyContext mlLiteralBody() {
			return getRuleContext(MlLiteralBodyContext.class,0);
		}
		public MlLiteralStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mlLiteralString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterMlLiteralString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitMlLiteralString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitMlLiteralString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MlLiteralStringContext mlLiteralString() throws RecognitionException {
		MlLiteralStringContext _localctx = new MlLiteralStringContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_mlLiteralString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(219);
			match(TripleApostrophe);
			setState(220);
			mlLiteralBody();
			setState(221);
			match(TripleApostrophe);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MlLiteralBodyContext extends AbstractTomlParserRuleContext {
		public List<TerminalNode> StringChars() { return getTokens(TomlParser.StringChars); }
		public TerminalNode StringChars(int i) {
			return getToken(TomlParser.StringChars, i);
		}
		public MlLiteralBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mlLiteralBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterMlLiteralBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitMlLiteralBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitMlLiteralBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MlLiteralBodyContext mlLiteralBody() throws RecognitionException {
		MlLiteralBodyContext _localctx = new MlLiteralBodyContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_mlLiteralBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(226);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==StringChars) {
				{
				{
				setState(223);
				match(StringChars);
				}
				}
				setState(228);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IntegerContext extends AbstractTomlParserRuleContext {
		public DecIntContext decInt() {
			return getRuleContext(DecIntContext.class,0);
		}
		public HexIntContext hexInt() {
			return getRuleContext(HexIntContext.class,0);
		}
		public OctIntContext octInt() {
			return getRuleContext(OctIntContext.class,0);
		}
		public BinIntContext binInt() {
			return getRuleContext(BinIntContext.class,0);
		}
		public IntegerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_integer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterInteger(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitInteger(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitInteger(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntegerContext integer() throws RecognitionException {
		IntegerContext _localctx = new IntegerContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_integer);
		try {
			setState(233);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DecimalInteger:
				enterOuterAlt(_localctx, 1);
				{
				setState(229);
				decInt();
				}
				break;
			case HexInteger:
				enterOuterAlt(_localctx, 2);
				{
				setState(230);
				hexInt();
				}
				break;
			case OctalInteger:
				enterOuterAlt(_localctx, 3);
				{
				setState(231);
				octInt();
				}
				break;
			case BinaryInteger:
				enterOuterAlt(_localctx, 4);
				{
				setState(232);
				binInt();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DecIntContext extends AbstractTomlParserRuleContext {
		public TerminalNode DecimalInteger() { return getToken(TomlParser.DecimalInteger, 0); }
		public DecIntContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_decInt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterDecInt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitDecInt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitDecInt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DecIntContext decInt() throws RecognitionException {
		DecIntContext _localctx = new DecIntContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_decInt);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(235);
			match(DecimalInteger);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class HexIntContext extends AbstractTomlParserRuleContext {
		public TerminalNode HexInteger() { return getToken(TomlParser.HexInteger, 0); }
		public HexIntContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hexInt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterHexInt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitHexInt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitHexInt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HexIntContext hexInt() throws RecognitionException {
		HexIntContext _localctx = new HexIntContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_hexInt);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(237);
			match(HexInteger);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OctIntContext extends AbstractTomlParserRuleContext {
		public TerminalNode OctalInteger() { return getToken(TomlParser.OctalInteger, 0); }
		public OctIntContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_octInt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterOctInt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitOctInt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitOctInt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OctIntContext octInt() throws RecognitionException {
		OctIntContext _localctx = new OctIntContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_octInt);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(239);
			match(OctalInteger);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BinIntContext extends AbstractTomlParserRuleContext {
		public TerminalNode BinaryInteger() { return getToken(TomlParser.BinaryInteger, 0); }
		public BinIntContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_binInt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterBinInt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitBinInt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitBinInt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BinIntContext binInt() throws RecognitionException {
		BinIntContext _localctx = new BinIntContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_binInt);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(241);
			match(BinaryInteger);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FloatValueContext extends AbstractTomlParserRuleContext {
		public RegularFloatContext regularFloat() {
			return getRuleContext(RegularFloatContext.class,0);
		}
		public RegularFloatInfContext regularFloatInf() {
			return getRuleContext(RegularFloatInfContext.class,0);
		}
		public RegularFloatNaNContext regularFloatNaN() {
			return getRuleContext(RegularFloatNaNContext.class,0);
		}
		public FloatValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_floatValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterFloatValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitFloatValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitFloatValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FloatValueContext floatValue() throws RecognitionException {
		FloatValueContext _localctx = new FloatValueContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_floatValue);
		try {
			setState(246);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FloatingPoint:
				enterOuterAlt(_localctx, 1);
				{
				setState(243);
				regularFloat();
				}
				break;
			case FloatingPointInf:
				enterOuterAlt(_localctx, 2);
				{
				setState(244);
				regularFloatInf();
				}
				break;
			case FloatingPointNaN:
				enterOuterAlt(_localctx, 3);
				{
				setState(245);
				regularFloatNaN();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RegularFloatContext extends AbstractTomlParserRuleContext {
		public TerminalNode FloatingPoint() { return getToken(TomlParser.FloatingPoint, 0); }
		public RegularFloatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_regularFloat; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterRegularFloat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitRegularFloat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitRegularFloat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RegularFloatContext regularFloat() throws RecognitionException {
		RegularFloatContext _localctx = new RegularFloatContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_regularFloat);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(248);
			match(FloatingPoint);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RegularFloatInfContext extends AbstractTomlParserRuleContext {
		public TerminalNode FloatingPointInf() { return getToken(TomlParser.FloatingPointInf, 0); }
		public RegularFloatInfContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_regularFloatInf; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterRegularFloatInf(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitRegularFloatInf(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitRegularFloatInf(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RegularFloatInfContext regularFloatInf() throws RecognitionException {
		RegularFloatInfContext _localctx = new RegularFloatInfContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_regularFloatInf);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(250);
			match(FloatingPointInf);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RegularFloatNaNContext extends AbstractTomlParserRuleContext {
		public TerminalNode FloatingPointNaN() { return getToken(TomlParser.FloatingPointNaN, 0); }
		public RegularFloatNaNContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_regularFloatNaN; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterRegularFloatNaN(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitRegularFloatNaN(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitRegularFloatNaN(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RegularFloatNaNContext regularFloatNaN() throws RecognitionException {
		RegularFloatNaNContext _localctx = new RegularFloatNaNContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_regularFloatNaN);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(252);
			match(FloatingPointNaN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BooleanValueContext extends AbstractTomlParserRuleContext {
		public TrueBoolContext trueBool() {
			return getRuleContext(TrueBoolContext.class,0);
		}
		public FalseBoolContext falseBool() {
			return getRuleContext(FalseBoolContext.class,0);
		}
		public BooleanValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterBooleanValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitBooleanValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitBooleanValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanValueContext booleanValue() throws RecognitionException {
		BooleanValueContext _localctx = new BooleanValueContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_booleanValue);
		try {
			setState(256);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TrueBoolean:
				enterOuterAlt(_localctx, 1);
				{
				setState(254);
				trueBool();
				}
				break;
			case FalseBoolean:
				enterOuterAlt(_localctx, 2);
				{
				setState(255);
				falseBool();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TrueBoolContext extends AbstractTomlParserRuleContext {
		public TerminalNode TrueBoolean() { return getToken(TomlParser.TrueBoolean, 0); }
		public TrueBoolContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_trueBool; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterTrueBool(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitTrueBool(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitTrueBool(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TrueBoolContext trueBool() throws RecognitionException {
		TrueBoolContext _localctx = new TrueBoolContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_trueBool);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(258);
			match(TrueBoolean);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FalseBoolContext extends AbstractTomlParserRuleContext {
		public TerminalNode FalseBoolean() { return getToken(TomlParser.FalseBoolean, 0); }
		public FalseBoolContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_falseBool; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterFalseBool(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitFalseBool(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitFalseBool(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FalseBoolContext falseBool() throws RecognitionException {
		FalseBoolContext _localctx = new FalseBoolContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_falseBool);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(260);
			match(FalseBoolean);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DateTimeContext extends AbstractTomlParserRuleContext {
		public OffsetDateTimeContext offsetDateTime() {
			return getRuleContext(OffsetDateTimeContext.class,0);
		}
		public LocalDateTimeContext localDateTime() {
			return getRuleContext(LocalDateTimeContext.class,0);
		}
		public LocalDateContext localDate() {
			return getRuleContext(LocalDateContext.class,0);
		}
		public LocalTimeContext localTime() {
			return getRuleContext(LocalTimeContext.class,0);
		}
		public DateTimeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dateTime; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterDateTime(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitDateTime(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitDateTime(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DateTimeContext dateTime() throws RecognitionException {
		DateTimeContext _localctx = new DateTimeContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_dateTime);
		try {
			setState(266);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(262);
				offsetDateTime();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(263);
				localDateTime();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(264);
				localDate();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(265);
				localTime();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OffsetDateTimeContext extends AbstractTomlParserRuleContext {
		public DateContext date() {
			return getRuleContext(DateContext.class,0);
		}
		public TerminalNode TimeDelimiter() { return getToken(TomlParser.TimeDelimiter, 0); }
		public TimeContext time() {
			return getRuleContext(TimeContext.class,0);
		}
		public TimeOffsetContext timeOffset() {
			return getRuleContext(TimeOffsetContext.class,0);
		}
		public OffsetDateTimeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_offsetDateTime; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterOffsetDateTime(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitOffsetDateTime(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitOffsetDateTime(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OffsetDateTimeContext offsetDateTime() throws RecognitionException {
		OffsetDateTimeContext _localctx = new OffsetDateTimeContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_offsetDateTime);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(268);
			date();
			setState(269);
			match(TimeDelimiter);
			setState(270);
			time();
			setState(271);
			timeOffset();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LocalDateTimeContext extends AbstractTomlParserRuleContext {
		public DateContext date() {
			return getRuleContext(DateContext.class,0);
		}
		public TerminalNode TimeDelimiter() { return getToken(TomlParser.TimeDelimiter, 0); }
		public TimeContext time() {
			return getRuleContext(TimeContext.class,0);
		}
		public LocalDateTimeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_localDateTime; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterLocalDateTime(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitLocalDateTime(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitLocalDateTime(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocalDateTimeContext localDateTime() throws RecognitionException {
		LocalDateTimeContext _localctx = new LocalDateTimeContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_localDateTime);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(273);
			date();
			setState(274);
			match(TimeDelimiter);
			setState(275);
			time();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LocalDateContext extends AbstractTomlParserRuleContext {
		public DateContext date() {
			return getRuleContext(DateContext.class,0);
		}
		public LocalDateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_localDate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterLocalDate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitLocalDate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitLocalDate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocalDateContext localDate() throws RecognitionException {
		LocalDateContext _localctx = new LocalDateContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_localDate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(277);
			date();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LocalTimeContext extends AbstractTomlParserRuleContext {
		public TimeContext time() {
			return getRuleContext(TimeContext.class,0);
		}
		public LocalTimeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_localTime; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterLocalTime(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitLocalTime(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitLocalTime(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocalTimeContext localTime() throws RecognitionException {
		LocalTimeContext _localctx = new LocalTimeContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_localTime);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(279);
			time();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DateContext extends AbstractTomlParserRuleContext {
		public YearContext year() {
			return getRuleContext(YearContext.class,0);
		}
		public List<TerminalNode> Dash() { return getTokens(TomlParser.Dash); }
		public TerminalNode Dash(int i) {
			return getToken(TomlParser.Dash, i);
		}
		public MonthContext month() {
			return getRuleContext(MonthContext.class,0);
		}
		public DayContext day() {
			return getRuleContext(DayContext.class,0);
		}
		public DateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_date; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterDate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitDate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitDate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DateContext date() throws RecognitionException {
		DateContext _localctx = new DateContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_date);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(281);
			year();
			setState(282);
			match(Dash);
			setState(283);
			month();
			setState(284);
			match(Dash);
			setState(285);
			day();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TimeContext extends AbstractTomlParserRuleContext {
		public HourContext hour() {
			return getRuleContext(HourContext.class,0);
		}
		public List<TerminalNode> Colon() { return getTokens(TomlParser.Colon); }
		public TerminalNode Colon(int i) {
			return getToken(TomlParser.Colon, i);
		}
		public MinuteContext minute() {
			return getRuleContext(MinuteContext.class,0);
		}
		public SecondContext second() {
			return getRuleContext(SecondContext.class,0);
		}
		public TerminalNode Dot() { return getToken(TomlParser.Dot, 0); }
		public SecondFractionContext secondFraction() {
			return getRuleContext(SecondFractionContext.class,0);
		}
		public TimeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_time; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterTime(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitTime(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitTime(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TimeContext time() throws RecognitionException {
		TimeContext _localctx = new TimeContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_time);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(287);
			hour();
			setState(288);
			match(Colon);
			setState(289);
			minute();
			setState(296);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Colon) {
				{
				setState(290);
				match(Colon);
				setState(291);
				second();
				setState(294);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Dot) {
					{
					setState(292);
					match(Dot);
					setState(293);
					secondFraction();
					}
				}

				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TimeOffsetContext extends AbstractTomlParserRuleContext {
		public TerminalNode Z() { return getToken(TomlParser.Z, 0); }
		public HourOffsetContext hourOffset() {
			return getRuleContext(HourOffsetContext.class,0);
		}
		public TerminalNode Colon() { return getToken(TomlParser.Colon, 0); }
		public MinuteOffsetContext minuteOffset() {
			return getRuleContext(MinuteOffsetContext.class,0);
		}
		public TimeOffsetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_timeOffset; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterTimeOffset(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitTimeOffset(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitTimeOffset(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TimeOffsetContext timeOffset() throws RecognitionException {
		TimeOffsetContext _localctx = new TimeOffsetContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_timeOffset);
		try {
			setState(303);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Z:
				enterOuterAlt(_localctx, 1);
				{
				setState(298);
				match(Z);
				}
				break;
			case Dash:
			case Plus:
				enterOuterAlt(_localctx, 2);
				{
				setState(299);
				hourOffset();
				setState(300);
				match(Colon);
				setState(301);
				minuteOffset();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class HourOffsetContext extends AbstractTomlParserRuleContext {
		public HourContext hour() {
			return getRuleContext(HourContext.class,0);
		}
		public TerminalNode Dash() { return getToken(TomlParser.Dash, 0); }
		public TerminalNode Plus() { return getToken(TomlParser.Plus, 0); }
		public HourOffsetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hourOffset; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterHourOffset(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitHourOffset(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitHourOffset(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HourOffsetContext hourOffset() throws RecognitionException {
		HourOffsetContext _localctx = new HourOffsetContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_hourOffset);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(305);
			_la = _input.LA(1);
			if ( !(_la==Dash || _la==Plus) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(306);
			hour();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MinuteOffsetContext extends AbstractTomlParserRuleContext {
		public TerminalNode DateDigits() { return getToken(TomlParser.DateDigits, 0); }
		public MinuteOffsetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_minuteOffset; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterMinuteOffset(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitMinuteOffset(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitMinuteOffset(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MinuteOffsetContext minuteOffset() throws RecognitionException {
		MinuteOffsetContext _localctx = new MinuteOffsetContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_minuteOffset);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(308);
			match(DateDigits);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SecondFractionContext extends AbstractTomlParserRuleContext {
		public TerminalNode DateDigits() { return getToken(TomlParser.DateDigits, 0); }
		public SecondFractionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_secondFraction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterSecondFraction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitSecondFraction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitSecondFraction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SecondFractionContext secondFraction() throws RecognitionException {
		SecondFractionContext _localctx = new SecondFractionContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_secondFraction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(310);
			match(DateDigits);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class YearContext extends AbstractTomlParserRuleContext {
		public TerminalNode DateDigits() { return getToken(TomlParser.DateDigits, 0); }
		public YearContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_year; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterYear(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitYear(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitYear(this);
			else return visitor.visitChildren(this);
		}
	}

	public final YearContext year() throws RecognitionException {
		YearContext _localctx = new YearContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_year);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(312);
			match(DateDigits);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MonthContext extends AbstractTomlParserRuleContext {
		public TerminalNode DateDigits() { return getToken(TomlParser.DateDigits, 0); }
		public MonthContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_month; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterMonth(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitMonth(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitMonth(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MonthContext month() throws RecognitionException {
		MonthContext _localctx = new MonthContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_month);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(314);
			match(DateDigits);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DayContext extends AbstractTomlParserRuleContext {
		public TerminalNode DateDigits() { return getToken(TomlParser.DateDigits, 0); }
		public DayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_day; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterDay(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitDay(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitDay(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DayContext day() throws RecognitionException {
		DayContext _localctx = new DayContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_day);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(316);
			match(DateDigits);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class HourContext extends AbstractTomlParserRuleContext {
		public TerminalNode DateDigits() { return getToken(TomlParser.DateDigits, 0); }
		public HourContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hour; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterHour(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitHour(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitHour(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HourContext hour() throws RecognitionException {
		HourContext _localctx = new HourContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_hour);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(318);
			match(DateDigits);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MinuteContext extends AbstractTomlParserRuleContext {
		public TerminalNode DateDigits() { return getToken(TomlParser.DateDigits, 0); }
		public MinuteContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_minute; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterMinute(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitMinute(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitMinute(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MinuteContext minute() throws RecognitionException {
		MinuteContext _localctx = new MinuteContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_minute);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(320);
			match(DateDigits);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SecondContext extends AbstractTomlParserRuleContext {
		public TerminalNode DateDigits() { return getToken(TomlParser.DateDigits, 0); }
		public SecondContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_second; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterSecond(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitSecond(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitSecond(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SecondContext second() throws RecognitionException {
		SecondContext _localctx = new SecondContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_second);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(322);
			match(DateDigits);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArrayContext extends AbstractTomlParserRuleContext {
		public TerminalNode ArrayStart() { return getToken(TomlParser.ArrayStart, 0); }
		public TerminalNode ArrayEnd() { return getToken(TomlParser.ArrayEnd, 0); }
		public ArrayValuesContext arrayValues() {
			return getRuleContext(ArrayValuesContext.class,0);
		}
		public List<TerminalNode> NewLine() { return getTokens(TomlParser.NewLine); }
		public TerminalNode NewLine(int i) {
			return getToken(TomlParser.NewLine, i);
		}
		public TerminalNode Comma() { return getToken(TomlParser.Comma, 0); }
		public ArrayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterArray(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitArray(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitArray(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayContext array() throws RecognitionException {
		ArrayContext _localctx = new ArrayContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_array);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(324);
			match(ArrayStart);
			setState(335);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				{
				setState(325);
				arrayValues();
				setState(329);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(326);
						match(NewLine);
						}
						} 
					}
					setState(331);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				}
				setState(333);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(332);
					match(Comma);
					}
				}

				}
				break;
			}
			setState(340);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NewLine) {
				{
				{
				setState(337);
				match(NewLine);
				}
				}
				setState(342);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(343);
			match(ArrayEnd);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArrayValuesContext extends AbstractTomlParserRuleContext {
		public List<ArrayValueContext> arrayValue() {
			return getRuleContexts(ArrayValueContext.class);
		}
		public ArrayValueContext arrayValue(int i) {
			return getRuleContext(ArrayValueContext.class,i);
		}
		public List<TerminalNode> Comma() { return getTokens(TomlParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(TomlParser.Comma, i);
		}
		public List<TerminalNode> NewLine() { return getTokens(TomlParser.NewLine); }
		public TerminalNode NewLine(int i) {
			return getToken(TomlParser.NewLine, i);
		}
		public ArrayValuesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayValues; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterArrayValues(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitArrayValues(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitArrayValues(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayValuesContext arrayValues() throws RecognitionException {
		ArrayValuesContext _localctx = new ArrayValuesContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_arrayValues);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(345);
			arrayValue();
			setState(356);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(349);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==NewLine) {
						{
						{
						setState(346);
						match(NewLine);
						}
						}
						setState(351);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(352);
					match(Comma);
					setState(353);
					arrayValue();
					}
					} 
				}
				setState(358);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArrayValueContext extends AbstractTomlParserRuleContext {
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
		public List<TerminalNode> NewLine() { return getTokens(TomlParser.NewLine); }
		public TerminalNode NewLine(int i) {
			return getToken(TomlParser.NewLine, i);
		}
		public ArrayValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterArrayValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitArrayValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitArrayValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayValueContext arrayValue() throws RecognitionException {
		ArrayValueContext _localctx = new ArrayValueContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_arrayValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(362);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NewLine) {
				{
				{
				setState(359);
				match(NewLine);
				}
				}
				setState(364);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(365);
			val();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TableContext extends AbstractTomlParserRuleContext {
		public StandardTableContext standardTable() {
			return getRuleContext(StandardTableContext.class,0);
		}
		public ArrayTableContext arrayTable() {
			return getRuleContext(ArrayTableContext.class,0);
		}
		public TableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_table; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TableContext table() throws RecognitionException {
		TableContext _localctx = new TableContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_table);
		try {
			setState(369);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TableKeyStart:
				enterOuterAlt(_localctx, 1);
				{
				setState(367);
				standardTable();
				}
				break;
			case ArrayTableKeyStart:
				enterOuterAlt(_localctx, 2);
				{
				setState(368);
				arrayTable();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StandardTableContext extends AbstractTomlParserRuleContext {
		public TerminalNode TableKeyStart() { return getToken(TomlParser.TableKeyStart, 0); }
		public TerminalNode TableKeyEnd() { return getToken(TomlParser.TableKeyEnd, 0); }
		public KeyContext key() {
			return getRuleContext(KeyContext.class,0);
		}
		public StandardTableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_standardTable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterStandardTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitStandardTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitStandardTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StandardTableContext standardTable() throws RecognitionException {
		StandardTableContext _localctx = new StandardTableContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_standardTable);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(371);
			match(TableKeyStart);
			setState(373);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8576L) != 0)) {
				{
				setState(372);
				key();
				}
			}

			setState(375);
			match(TableKeyEnd);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InlineTableContext extends AbstractTomlParserRuleContext {
		public TerminalNode InlineTableStart() { return getToken(TomlParser.InlineTableStart, 0); }
		public TerminalNode InlineTableEnd() { return getToken(TomlParser.InlineTableEnd, 0); }
		public InlineTableValuesContext inlineTableValues() {
			return getRuleContext(InlineTableValuesContext.class,0);
		}
		public List<TerminalNode> NewLine() { return getTokens(TomlParser.NewLine); }
		public TerminalNode NewLine(int i) {
			return getToken(TomlParser.NewLine, i);
		}
		public TerminalNode Comma() { return getToken(TomlParser.Comma, 0); }
		public InlineTableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineTable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterInlineTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitInlineTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitInlineTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineTableContext inlineTable() throws RecognitionException {
		InlineTableContext _localctx = new InlineTableContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_inlineTable);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(377);
			match(InlineTableStart);
			setState(388);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
			case 1:
				{
				setState(378);
				inlineTableValues();
				setState(382);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(379);
						match(NewLine);
						}
						} 
					}
					setState(384);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
				}
				setState(386);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Comma) {
					{
					setState(385);
					match(Comma);
					}
				}

				}
				break;
			}
			setState(393);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NewLine) {
				{
				{
				setState(390);
				match(NewLine);
				}
				}
				setState(395);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(396);
			match(InlineTableEnd);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InlineTableValuesContext extends AbstractTomlParserRuleContext {
		public List<InlineTableValueContext> inlineTableValue() {
			return getRuleContexts(InlineTableValueContext.class);
		}
		public InlineTableValueContext inlineTableValue(int i) {
			return getRuleContext(InlineTableValueContext.class,i);
		}
		public List<TerminalNode> Comma() { return getTokens(TomlParser.Comma); }
		public TerminalNode Comma(int i) {
			return getToken(TomlParser.Comma, i);
		}
		public List<TerminalNode> NewLine() { return getTokens(TomlParser.NewLine); }
		public TerminalNode NewLine(int i) {
			return getToken(TomlParser.NewLine, i);
		}
		public InlineTableValuesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineTableValues; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterInlineTableValues(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitInlineTableValues(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitInlineTableValues(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineTableValuesContext inlineTableValues() throws RecognitionException {
		InlineTableValuesContext _localctx = new InlineTableValuesContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_inlineTableValues);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(398);
			inlineTableValue();
			setState(409);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(402);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==NewLine) {
						{
						{
						setState(399);
						match(NewLine);
						}
						}
						setState(404);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(405);
					match(Comma);
					setState(406);
					inlineTableValue();
					}
					} 
				}
				setState(411);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InlineTableValueContext extends AbstractTomlParserRuleContext {
		public KeyvalContext keyval() {
			return getRuleContext(KeyvalContext.class,0);
		}
		public List<TerminalNode> NewLine() { return getTokens(TomlParser.NewLine); }
		public TerminalNode NewLine(int i) {
			return getToken(TomlParser.NewLine, i);
		}
		public InlineTableValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineTableValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterInlineTableValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitInlineTableValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitInlineTableValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineTableValueContext inlineTableValue() throws RecognitionException {
		InlineTableValueContext _localctx = new InlineTableValueContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_inlineTableValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(415);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NewLine) {
				{
				{
				setState(412);
				match(NewLine);
				}
				}
				setState(417);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(418);
			keyval();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArrayTableContext extends AbstractTomlParserRuleContext {
		public TerminalNode ArrayTableKeyStart() { return getToken(TomlParser.ArrayTableKeyStart, 0); }
		public TerminalNode ArrayTableKeyEnd() { return getToken(TomlParser.ArrayTableKeyEnd, 0); }
		public KeyContext key() {
			return getRuleContext(KeyContext.class,0);
		}
		public ArrayTableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayTable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).enterArrayTable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TomlParserListener ) ((TomlParserListener)listener).exitArrayTable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TomlParserVisitor ) return ((TomlParserVisitor<? extends T>)visitor).visitArrayTable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayTableContext arrayTable() throws RecognitionException {
		ArrayTableContext _localctx = new ArrayTableContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_arrayTable);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(420);
			match(ArrayTableKeyStart);
			setState(422);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8576L) != 0)) {
				{
				setState(421);
				key();
				}
			}

			setState(424);
			match(ArrayTableKeyEnd);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001\'\u01ab\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0002"+
		"#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0002"+
		"(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007,\u0002"+
		"-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u00071\u0002"+
		"2\u00072\u00023\u00073\u00024\u00074\u00025\u00075\u00026\u00076\u0002"+
		"7\u00077\u00028\u00078\u00029\u00079\u0002:\u0007:\u0001\u0000\u0003\u0000"+
		"x\b\u0000\u0001\u0000\u0001\u0000\u0003\u0000|\b\u0000\u0005\u0000~\b"+
		"\u0000\n\u0000\f\u0000\u0081\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001"+
		"\u0001\u0001\u0003\u0001\u0087\b\u0001\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0005\u0004\u0093\b\u0004\n\u0004\f\u0004\u0096\t\u0004\u0001"+
		"\u0005\u0001\u0005\u0003\u0005\u009a\b\u0005\u0001\u0006\u0001\u0006\u0001"+
		"\u0007\u0001\u0007\u0003\u0007\u00a0\b\u0007\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0003\b\u00aa\b\b\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0003\t\u00b0\b\t\u0001\n\u0001\n\u0005\n\u00b4\b\n\n\n\f\n"+
		"\u00b7\t\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0003\u000b\u00bd\b"+
		"\u000b\u0001\f\u0001\f\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0005\u000e"+
		"\u00c5\b\u000e\n\u000e\f\u000e\u00c8\t\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000f\u0001\u000f\u0003\u000f\u00ce\b\u000f\u0001\u0010\u0001\u0010\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0005\u0012\u00d7"+
		"\b\u0012\n\u0012\f\u0012\u00da\t\u0012\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0014\u0005\u0014\u00e1\b\u0014\n\u0014\f\u0014\u00e4"+
		"\t\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u00ea"+
		"\b\u0015\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0018\u0001"+
		"\u0018\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a\u0003"+
		"\u001a\u00f7\b\u001a\u0001\u001b\u0001\u001b\u0001\u001c\u0001\u001c\u0001"+
		"\u001d\u0001\u001d\u0001\u001e\u0001\u001e\u0003\u001e\u0101\b\u001e\u0001"+
		"\u001f\u0001\u001f\u0001 \u0001 \u0001!\u0001!\u0001!\u0001!\u0003!\u010b"+
		"\b!\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001#\u0001#\u0001#\u0001"+
		"#\u0001$\u0001$\u0001%\u0001%\u0001&\u0001&\u0001&\u0001&\u0001&\u0001"+
		"&\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0003\'\u0127"+
		"\b\'\u0003\'\u0129\b\'\u0001(\u0001(\u0001(\u0001(\u0001(\u0003(\u0130"+
		"\b(\u0001)\u0001)\u0001)\u0001*\u0001*\u0001+\u0001+\u0001,\u0001,\u0001"+
		"-\u0001-\u0001.\u0001.\u0001/\u0001/\u00010\u00010\u00011\u00011\u0001"+
		"2\u00012\u00012\u00052\u0148\b2\n2\f2\u014b\t2\u00012\u00032\u014e\b2"+
		"\u00032\u0150\b2\u00012\u00052\u0153\b2\n2\f2\u0156\t2\u00012\u00012\u0001"+
		"3\u00013\u00053\u015c\b3\n3\f3\u015f\t3\u00013\u00013\u00053\u0163\b3"+
		"\n3\f3\u0166\t3\u00014\u00054\u0169\b4\n4\f4\u016c\t4\u00014\u00014\u0001"+
		"5\u00015\u00035\u0172\b5\u00016\u00016\u00036\u0176\b6\u00016\u00016\u0001"+
		"7\u00017\u00017\u00057\u017d\b7\n7\f7\u0180\t7\u00017\u00037\u0183\b7"+
		"\u00037\u0185\b7\u00017\u00057\u0188\b7\n7\f7\u018b\t7\u00017\u00017\u0001"+
		"8\u00018\u00058\u0191\b8\n8\f8\u0194\t8\u00018\u00018\u00058\u0198\b8"+
		"\n8\f8\u019b\t8\u00019\u00059\u019e\b9\n9\f9\u01a1\t9\u00019\u00019\u0001"+
		":\u0001:\u0003:\u01a7\b:\u0001:\u0001:\u0001:\u0000\u0000;\u0000\u0002"+
		"\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e"+
		" \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprt\u0000\u0001\u0001\u0000"+
		" !\u01a2\u0000w\u0001\u0000\u0000\u0000\u0002\u0086\u0001\u0000\u0000"+
		"\u0000\u0004\u0088\u0001\u0000\u0000\u0000\u0006\u008b\u0001\u0000\u0000"+
		"\u0000\b\u008f\u0001\u0000\u0000\u0000\n\u0099\u0001\u0000\u0000\u0000"+
		"\f\u009b\u0001\u0000\u0000\u0000\u000e\u009f\u0001\u0000\u0000\u0000\u0010"+
		"\u00a1\u0001\u0000\u0000\u0000\u0012\u00af\u0001\u0000\u0000\u0000\u0014"+
		"\u00b1\u0001\u0000\u0000\u0000\u0016\u00bc\u0001\u0000\u0000\u0000\u0018"+
		"\u00be\u0001\u0000\u0000\u0000\u001a\u00c0\u0001\u0000\u0000\u0000\u001c"+
		"\u00c2\u0001\u0000\u0000\u0000\u001e\u00cd\u0001\u0000\u0000\u0000 \u00cf"+
		"\u0001\u0000\u0000\u0000\"\u00d1\u0001\u0000\u0000\u0000$\u00d8\u0001"+
		"\u0000\u0000\u0000&\u00db\u0001\u0000\u0000\u0000(\u00e2\u0001\u0000\u0000"+
		"\u0000*\u00e9\u0001\u0000\u0000\u0000,\u00eb\u0001\u0000\u0000\u0000."+
		"\u00ed\u0001\u0000\u0000\u00000\u00ef\u0001\u0000\u0000\u00002\u00f1\u0001"+
		"\u0000\u0000\u00004\u00f6\u0001\u0000\u0000\u00006\u00f8\u0001\u0000\u0000"+
		"\u00008\u00fa\u0001\u0000\u0000\u0000:\u00fc\u0001\u0000\u0000\u0000<"+
		"\u0100\u0001\u0000\u0000\u0000>\u0102\u0001\u0000\u0000\u0000@\u0104\u0001"+
		"\u0000\u0000\u0000B\u010a\u0001\u0000\u0000\u0000D\u010c\u0001\u0000\u0000"+
		"\u0000F\u0111\u0001\u0000\u0000\u0000H\u0115\u0001\u0000\u0000\u0000J"+
		"\u0117\u0001\u0000\u0000\u0000L\u0119\u0001\u0000\u0000\u0000N\u011f\u0001"+
		"\u0000\u0000\u0000P\u012f\u0001\u0000\u0000\u0000R\u0131\u0001\u0000\u0000"+
		"\u0000T\u0134\u0001\u0000\u0000\u0000V\u0136\u0001\u0000\u0000\u0000X"+
		"\u0138\u0001\u0000\u0000\u0000Z\u013a\u0001\u0000\u0000\u0000\\\u013c"+
		"\u0001\u0000\u0000\u0000^\u013e\u0001\u0000\u0000\u0000`\u0140\u0001\u0000"+
		"\u0000\u0000b\u0142\u0001\u0000\u0000\u0000d\u0144\u0001\u0000\u0000\u0000"+
		"f\u0159\u0001\u0000\u0000\u0000h\u016a\u0001\u0000\u0000\u0000j\u0171"+
		"\u0001\u0000\u0000\u0000l\u0173\u0001\u0000\u0000\u0000n\u0179\u0001\u0000"+
		"\u0000\u0000p\u018e\u0001\u0000\u0000\u0000r\u019f\u0001\u0000\u0000\u0000"+
		"t\u01a4\u0001\u0000\u0000\u0000vx\u0003\u0002\u0001\u0000wv\u0001\u0000"+
		"\u0000\u0000wx\u0001\u0000\u0000\u0000x\u007f\u0001\u0000\u0000\u0000"+
		"y{\u0005\u0010\u0000\u0000z|\u0003\u0002\u0001\u0000{z\u0001\u0000\u0000"+
		"\u0000{|\u0001\u0000\u0000\u0000|~\u0001\u0000\u0000\u0000}y\u0001\u0000"+
		"\u0000\u0000~\u0081\u0001\u0000\u0000\u0000\u007f}\u0001\u0000\u0000\u0000"+
		"\u007f\u0080\u0001\u0000\u0000\u0000\u0080\u0082\u0001\u0000\u0000\u0000"+
		"\u0081\u007f\u0001\u0000\u0000\u0000\u0082\u0083\u0005\u0000\u0000\u0001"+
		"\u0083\u0001\u0001\u0000\u0000\u0000\u0084\u0087\u0003\u0006\u0003\u0000"+
		"\u0085\u0087\u0003j5\u0000\u0086\u0084\u0001\u0000\u0000\u0000\u0086\u0085"+
		"\u0001\u0000\u0000\u0000\u0087\u0003\u0001\u0000\u0000\u0000\u0088\u0089"+
		"\u0003\b\u0004\u0000\u0089\u008a\u0005\u0000\u0000\u0001\u008a\u0005\u0001"+
		"\u0000\u0000\u0000\u008b\u008c\u0003\b\u0004\u0000\u008c\u008d\u0005\u0006"+
		"\u0000\u0000\u008d\u008e\u0003\u0010\b\u0000\u008e\u0007\u0001\u0000\u0000"+
		"\u0000\u008f\u0094\u0003\n\u0005\u0000\u0090\u0091\u0005\u0005\u0000\u0000"+
		"\u0091\u0093\u0003\n\u0005\u0000\u0092\u0090\u0001\u0000\u0000\u0000\u0093"+
		"\u0096\u0001\u0000\u0000\u0000\u0094\u0092\u0001\u0000\u0000\u0000\u0094"+
		"\u0095\u0001\u0000\u0000\u0000\u0095\t\u0001\u0000\u0000\u0000\u0096\u0094"+
		"\u0001\u0000\u0000\u0000\u0097\u009a\u0003\u000e\u0007\u0000\u0098\u009a"+
		"\u0003\f\u0006\u0000\u0099\u0097\u0001\u0000\u0000\u0000\u0099\u0098\u0001"+
		"\u0000\u0000\u0000\u009a\u000b\u0001\u0000\u0000\u0000\u009b\u009c\u0005"+
		"\r\u0000\u0000\u009c\r\u0001\u0000\u0000\u0000\u009d\u00a0\u0003\u0014"+
		"\n\u0000\u009e\u00a0\u0003\"\u0011\u0000\u009f\u009d\u0001\u0000\u0000"+
		"\u0000\u009f\u009e\u0001\u0000\u0000\u0000\u00a0\u000f\u0001\u0000\u0000"+
		"\u0000\u00a1\u00a9\u0006\b\uffff\uffff\u0000\u00a2\u00aa\u0003\u0012\t"+
		"\u0000\u00a3\u00aa\u0003*\u0015\u0000\u00a4\u00aa\u00034\u001a\u0000\u00a5"+
		"\u00aa\u0003<\u001e\u0000\u00a6\u00aa\u0003B!\u0000\u00a7\u00aa\u0003"+
		"d2\u0000\u00a8\u00aa\u0003n7\u0000\u00a9\u00a2\u0001\u0000\u0000\u0000"+
		"\u00a9\u00a3\u0001\u0000\u0000\u0000\u00a9\u00a4\u0001\u0000\u0000\u0000"+
		"\u00a9\u00a5\u0001\u0000\u0000\u0000\u00a9\u00a6\u0001\u0000\u0000\u0000"+
		"\u00a9\u00a7\u0001\u0000\u0000\u0000\u00a9\u00a8\u0001\u0000\u0000\u0000"+
		"\u00aa\u0011\u0001\u0000\u0000\u0000\u00ab\u00b0\u0003\u001c\u000e\u0000"+
		"\u00ac\u00b0\u0003\u0014\n\u0000\u00ad\u00b0\u0003&\u0013\u0000\u00ae"+
		"\u00b0\u0003\"\u0011\u0000\u00af\u00ab\u0001\u0000\u0000\u0000\u00af\u00ac"+
		"\u0001\u0000\u0000\u0000\u00af\u00ad\u0001\u0000\u0000\u0000\u00af\u00ae"+
		"\u0001\u0000\u0000\u0000\u00b0\u0013\u0001\u0000\u0000\u0000\u00b1\u00b5"+
		"\u0005\u0007\u0000\u0000\u00b2\u00b4\u0003\u0016\u000b\u0000\u00b3\u00b2"+
		"\u0001\u0000\u0000\u0000\u00b4\u00b7\u0001\u0000\u0000\u0000\u00b5\u00b3"+
		"\u0001\u0000\u0000\u0000\u00b5\u00b6\u0001\u0000\u0000\u0000\u00b6\u00b8"+
		"\u0001\u0000\u0000\u0000\u00b7\u00b5\u0001\u0000\u0000\u0000\u00b8\u00b9"+
		"\u0005\u0007\u0000\u0000\u00b9\u0015\u0001\u0000\u0000\u0000\u00ba\u00bd"+
		"\u0003\u0018\f\u0000\u00bb\u00bd\u0003\u001a\r\u0000\u00bc\u00ba\u0001"+
		"\u0000\u0000\u0000\u00bc\u00bb\u0001\u0000\u0000\u0000\u00bd\u0017\u0001"+
		"\u0000\u0000\u0000\u00be\u00bf\u0005\u0003\u0000\u0000\u00bf\u0019\u0001"+
		"\u0000\u0000\u0000\u00c0\u00c1\u0005\u001e\u0000\u0000\u00c1\u001b\u0001"+
		"\u0000\u0000\u0000\u00c2\u00c6\u0005\u0001\u0000\u0000\u00c3\u00c5\u0003"+
		"\u001e\u000f\u0000\u00c4\u00c3\u0001\u0000\u0000\u0000\u00c5\u00c8\u0001"+
		"\u0000\u0000\u0000\u00c6\u00c4\u0001\u0000\u0000\u0000\u00c6\u00c7\u0001"+
		"\u0000\u0000\u0000\u00c7\u00c9\u0001\u0000\u0000\u0000\u00c8\u00c6\u0001"+
		"\u0000\u0000\u0000\u00c9\u00ca\u0005\u0001\u0000\u0000\u00ca\u001d\u0001"+
		"\u0000\u0000\u0000\u00cb\u00ce\u0003 \u0010\u0000\u00cc\u00ce\u0003\u001a"+
		"\r\u0000\u00cd\u00cb\u0001\u0000\u0000\u0000\u00cd\u00cc\u0001\u0000\u0000"+
		"\u0000\u00ce\u001f\u0001\u0000\u0000\u0000\u00cf\u00d0\u0005\u0003\u0000"+
		"\u0000\u00d0!\u0001\u0000\u0000\u0000\u00d1\u00d2\u0005\b\u0000\u0000"+
		"\u00d2\u00d3\u0003$\u0012\u0000\u00d3\u00d4\u0005\b\u0000\u0000\u00d4"+
		"#\u0001\u0000\u0000\u0000\u00d5\u00d7\u0005\u0003\u0000\u0000\u00d6\u00d5"+
		"\u0001\u0000\u0000\u0000\u00d7\u00da\u0001\u0000\u0000\u0000\u00d8\u00d6"+
		"\u0001\u0000\u0000\u0000\u00d8\u00d9\u0001\u0000\u0000\u0000\u00d9%\u0001"+
		"\u0000\u0000\u0000\u00da\u00d8\u0001\u0000\u0000\u0000\u00db\u00dc\u0005"+
		"\u0002\u0000\u0000\u00dc\u00dd\u0003(\u0014\u0000\u00dd\u00de\u0005\u0002"+
		"\u0000\u0000\u00de\'\u0001\u0000\u0000\u0000\u00df\u00e1\u0005\u0003\u0000"+
		"\u0000\u00e0\u00df\u0001\u0000\u0000\u0000\u00e1\u00e4\u0001\u0000\u0000"+
		"\u0000\u00e2\u00e0\u0001\u0000\u0000\u0000\u00e2\u00e3\u0001\u0000\u0000"+
		"\u0000\u00e3)\u0001\u0000\u0000\u0000\u00e4\u00e2\u0001\u0000\u0000\u0000"+
		"\u00e5\u00ea\u0003,\u0016\u0000\u00e6\u00ea\u0003.\u0017\u0000\u00e7\u00ea"+
		"\u00030\u0018\u0000\u00e8\u00ea\u00032\u0019\u0000\u00e9\u00e5\u0001\u0000"+
		"\u0000\u0000\u00e9\u00e6\u0001\u0000\u0000\u0000\u00e9\u00e7\u0001\u0000"+
		"\u0000\u0000\u00e9\u00e8\u0001\u0000\u0000\u0000\u00ea+\u0001\u0000\u0000"+
		"\u0000\u00eb\u00ec\u0005\u0012\u0000\u0000\u00ec-\u0001\u0000\u0000\u0000"+
		"\u00ed\u00ee\u0005\u0013\u0000\u0000\u00ee/\u0001\u0000\u0000\u0000\u00ef"+
		"\u00f0\u0005\u0014\u0000\u0000\u00f01\u0001\u0000\u0000\u0000\u00f1\u00f2"+
		"\u0005\u0015\u0000\u0000\u00f23\u0001\u0000\u0000\u0000\u00f3\u00f7\u0003"+
		"6\u001b\u0000\u00f4\u00f7\u00038\u001c\u0000\u00f5\u00f7\u0003:\u001d"+
		"\u0000\u00f6\u00f3\u0001\u0000\u0000\u0000\u00f6\u00f4\u0001\u0000\u0000"+
		"\u0000\u00f6\u00f5\u0001\u0000\u0000\u0000\u00f75\u0001\u0000\u0000\u0000"+
		"\u00f8\u00f9\u0005\u0016\u0000\u0000\u00f97\u0001\u0000\u0000\u0000\u00fa"+
		"\u00fb\u0005\u0017\u0000\u0000\u00fb9\u0001\u0000\u0000\u0000\u00fc\u00fd"+
		"\u0005\u0018\u0000\u0000\u00fd;\u0001\u0000\u0000\u0000\u00fe\u0101\u0003"+
		">\u001f\u0000\u00ff\u0101\u0003@ \u0000\u0100\u00fe\u0001\u0000\u0000"+
		"\u0000\u0100\u00ff\u0001\u0000\u0000\u0000\u0101=\u0001\u0000\u0000\u0000"+
		"\u0102\u0103\u0005\u0019\u0000\u0000\u0103?\u0001\u0000\u0000\u0000\u0104"+
		"\u0105\u0005\u001a\u0000\u0000\u0105A\u0001\u0000\u0000\u0000\u0106\u010b"+
		"\u0003D\"\u0000\u0107\u010b\u0003F#\u0000\u0108\u010b\u0003H$\u0000\u0109"+
		"\u010b\u0003J%\u0000\u010a\u0106\u0001\u0000\u0000\u0000\u010a\u0107\u0001"+
		"\u0000\u0000\u0000\u010a\u0108\u0001\u0000\u0000\u0000\u010a\u0109\u0001"+
		"\u0000\u0000\u0000\u010bC\u0001\u0000\u0000\u0000\u010c\u010d\u0003L&"+
		"\u0000\u010d\u010e\u0005$\u0000\u0000\u010e\u010f\u0003N\'\u0000\u010f"+
		"\u0110\u0003P(\u0000\u0110E\u0001\u0000\u0000\u0000\u0111\u0112\u0003"+
		"L&\u0000\u0112\u0113\u0005$\u0000\u0000\u0113\u0114\u0003N\'\u0000\u0114"+
		"G\u0001\u0000\u0000\u0000\u0115\u0116\u0003L&\u0000\u0116I\u0001\u0000"+
		"\u0000\u0000\u0117\u0118\u0003N\'\u0000\u0118K\u0001\u0000\u0000\u0000"+
		"\u0119\u011a\u0003X,\u0000\u011a\u011b\u0005 \u0000\u0000\u011b\u011c"+
		"\u0003Z-\u0000\u011c\u011d\u0005 \u0000\u0000\u011d\u011e\u0003\\.\u0000"+
		"\u011eM\u0001\u0000\u0000\u0000\u011f\u0120\u0003^/\u0000\u0120\u0121"+
		"\u0005\"\u0000\u0000\u0121\u0128\u0003`0\u0000\u0122\u0123\u0005\"\u0000"+
		"\u0000\u0123\u0126\u0003b1\u0000\u0124\u0125\u0005\u0005\u0000\u0000\u0125"+
		"\u0127\u0003V+\u0000\u0126\u0124\u0001\u0000\u0000\u0000\u0126\u0127\u0001"+
		"\u0000\u0000\u0000\u0127\u0129\u0001\u0000\u0000\u0000\u0128\u0122\u0001"+
		"\u0000\u0000\u0000\u0128\u0129\u0001\u0000\u0000\u0000\u0129O\u0001\u0000"+
		"\u0000\u0000\u012a\u0130\u0005#\u0000\u0000\u012b\u012c\u0003R)\u0000"+
		"\u012c\u012d\u0005\"\u0000\u0000\u012d\u012e\u0003T*\u0000\u012e\u0130"+
		"\u0001\u0000\u0000\u0000\u012f\u012a\u0001\u0000\u0000\u0000\u012f\u012b"+
		"\u0001\u0000\u0000\u0000\u0130Q\u0001\u0000\u0000\u0000\u0131\u0132\u0007"+
		"\u0000\u0000\u0000\u0132\u0133\u0003^/\u0000\u0133S\u0001\u0000\u0000"+
		"\u0000\u0134\u0135\u0005%\u0000\u0000\u0135U\u0001\u0000\u0000\u0000\u0136"+
		"\u0137\u0005%\u0000\u0000\u0137W\u0001\u0000\u0000\u0000\u0138\u0139\u0005"+
		"%\u0000\u0000\u0139Y\u0001\u0000\u0000\u0000\u013a\u013b\u0005%\u0000"+
		"\u0000\u013b[\u0001\u0000\u0000\u0000\u013c\u013d\u0005%\u0000\u0000\u013d"+
		"]\u0001\u0000\u0000\u0000\u013e\u013f\u0005%\u0000\u0000\u013f_\u0001"+
		"\u0000\u0000\u0000\u0140\u0141\u0005%\u0000\u0000\u0141a\u0001\u0000\u0000"+
		"\u0000\u0142\u0143\u0005%\u0000\u0000\u0143c\u0001\u0000\u0000\u0000\u0144"+
		"\u014f\u0005\u001b\u0000\u0000\u0145\u0149\u0003f3\u0000\u0146\u0148\u0005"+
		"\u0010\u0000\u0000\u0147\u0146\u0001\u0000\u0000\u0000\u0148\u014b\u0001"+
		"\u0000\u0000\u0000\u0149\u0147\u0001\u0000\u0000\u0000\u0149\u014a\u0001"+
		"\u0000\u0000\u0000\u014a\u014d\u0001\u0000\u0000\u0000\u014b\u0149\u0001"+
		"\u0000\u0000\u0000\u014c\u014e\u0005\u0004\u0000\u0000\u014d\u014c\u0001"+
		"\u0000\u0000\u0000\u014d\u014e\u0001\u0000\u0000\u0000\u014e\u0150\u0001"+
		"\u0000\u0000\u0000\u014f\u0145\u0001\u0000\u0000\u0000\u014f\u0150\u0001"+
		"\u0000\u0000\u0000\u0150\u0154\u0001\u0000\u0000\u0000\u0151\u0153\u0005"+
		"\u0010\u0000\u0000\u0152\u0151\u0001\u0000\u0000\u0000\u0153\u0156\u0001"+
		"\u0000\u0000\u0000\u0154\u0152\u0001\u0000\u0000\u0000\u0154\u0155\u0001"+
		"\u0000\u0000\u0000\u0155\u0157\u0001\u0000\u0000\u0000\u0156\u0154\u0001"+
		"\u0000\u0000\u0000\u0157\u0158\u0005\u001c\u0000\u0000\u0158e\u0001\u0000"+
		"\u0000\u0000\u0159\u0164\u0003h4\u0000\u015a\u015c\u0005\u0010\u0000\u0000"+
		"\u015b\u015a\u0001\u0000\u0000\u0000\u015c\u015f\u0001\u0000\u0000\u0000"+
		"\u015d\u015b\u0001\u0000\u0000\u0000\u015d\u015e\u0001\u0000\u0000\u0000"+
		"\u015e\u0160\u0001\u0000\u0000\u0000\u015f\u015d\u0001\u0000\u0000\u0000"+
		"\u0160\u0161\u0005\u0004\u0000\u0000\u0161\u0163\u0003h4\u0000\u0162\u015d"+
		"\u0001\u0000\u0000\u0000\u0163\u0166\u0001\u0000\u0000\u0000\u0164\u0162"+
		"\u0001\u0000\u0000\u0000\u0164\u0165\u0001\u0000\u0000\u0000\u0165g\u0001"+
		"\u0000\u0000\u0000\u0166\u0164\u0001\u0000\u0000\u0000\u0167\u0169\u0005"+
		"\u0010\u0000\u0000\u0168\u0167\u0001\u0000\u0000\u0000\u0169\u016c\u0001"+
		"\u0000\u0000\u0000\u016a\u0168\u0001\u0000\u0000\u0000\u016a\u016b\u0001"+
		"\u0000\u0000\u0000\u016b\u016d\u0001\u0000\u0000\u0000\u016c\u016a\u0001"+
		"\u0000\u0000\u0000\u016d\u016e\u0003\u0010\b\u0000\u016ei\u0001\u0000"+
		"\u0000\u0000\u016f\u0172\u0003l6\u0000\u0170\u0172\u0003t:\u0000\u0171"+
		"\u016f\u0001\u0000\u0000\u0000\u0171\u0170\u0001\u0000\u0000\u0000\u0172"+
		"k\u0001\u0000\u0000\u0000\u0173\u0175\u0005\t\u0000\u0000\u0174\u0176"+
		"\u0003\b\u0004\u0000\u0175\u0174\u0001\u0000\u0000\u0000\u0175\u0176\u0001"+
		"\u0000\u0000\u0000\u0176\u0177\u0001\u0000\u0000\u0000\u0177\u0178\u0005"+
		"\n\u0000\u0000\u0178m\u0001\u0000\u0000\u0000\u0179\u0184\u0005\u001d"+
		"\u0000\u0000\u017a\u017e\u0003p8\u0000\u017b\u017d\u0005\u0010\u0000\u0000"+
		"\u017c\u017b\u0001\u0000\u0000\u0000\u017d\u0180\u0001\u0000\u0000\u0000"+
		"\u017e\u017c\u0001\u0000\u0000\u0000\u017e\u017f\u0001\u0000\u0000\u0000"+
		"\u017f\u0182\u0001\u0000\u0000\u0000\u0180\u017e\u0001\u0000\u0000\u0000"+
		"\u0181\u0183\u0005\u0004\u0000\u0000\u0182\u0181\u0001\u0000\u0000\u0000"+
		"\u0182\u0183\u0001\u0000\u0000\u0000\u0183\u0185\u0001\u0000\u0000\u0000"+
		"\u0184\u017a\u0001\u0000\u0000\u0000\u0184\u0185\u0001\u0000\u0000\u0000"+
		"\u0185\u0189\u0001\u0000\u0000\u0000\u0186\u0188\u0005\u0010\u0000\u0000"+
		"\u0187\u0186\u0001\u0000\u0000\u0000\u0188\u018b\u0001\u0000\u0000\u0000"+
		"\u0189\u0187\u0001\u0000\u0000\u0000\u0189\u018a\u0001\u0000\u0000\u0000"+
		"\u018a\u018c\u0001\u0000\u0000\u0000\u018b\u0189\u0001\u0000\u0000\u0000"+
		"\u018c\u018d\u0005&\u0000\u0000\u018do\u0001\u0000\u0000\u0000\u018e\u0199"+
		"\u0003r9\u0000\u018f\u0191\u0005\u0010\u0000\u0000\u0190\u018f\u0001\u0000"+
		"\u0000\u0000\u0191\u0194\u0001\u0000\u0000\u0000\u0192\u0190\u0001\u0000"+
		"\u0000\u0000\u0192\u0193\u0001\u0000\u0000\u0000\u0193\u0195\u0001\u0000"+
		"\u0000\u0000\u0194\u0192\u0001\u0000\u0000\u0000\u0195\u0196\u0005\u0004"+
		"\u0000\u0000\u0196\u0198\u0003r9\u0000\u0197\u0192\u0001\u0000\u0000\u0000"+
		"\u0198\u019b\u0001\u0000\u0000\u0000\u0199\u0197\u0001\u0000\u0000\u0000"+
		"\u0199\u019a\u0001\u0000\u0000\u0000\u019aq\u0001\u0000\u0000\u0000\u019b"+
		"\u0199\u0001\u0000\u0000\u0000\u019c\u019e\u0005\u0010\u0000\u0000\u019d"+
		"\u019c\u0001\u0000\u0000\u0000\u019e\u01a1\u0001\u0000\u0000\u0000\u019f"+
		"\u019d\u0001\u0000\u0000\u0000\u019f\u01a0\u0001\u0000\u0000\u0000\u01a0"+
		"\u01a2\u0001\u0000\u0000\u0000\u01a1\u019f\u0001\u0000\u0000\u0000\u01a2"+
		"\u01a3\u0003\u0006\u0003\u0000\u01a3s\u0001\u0000\u0000\u0000\u01a4\u01a6"+
		"\u0005\u000b\u0000\u0000\u01a5\u01a7\u0003\b\u0004\u0000\u01a6\u01a5\u0001"+
		"\u0000\u0000\u0000\u01a6\u01a7\u0001\u0000\u0000\u0000\u01a7\u01a8\u0001"+
		"\u0000\u0000\u0000\u01a8\u01a9\u0005\f\u0000\u0000\u01a9u\u0001\u0000"+
		"\u0000\u0000\'w{\u007f\u0086\u0094\u0099\u009f\u00a9\u00af\u00b5\u00bc"+
		"\u00c6\u00cd\u00d8\u00e2\u00e9\u00f6\u0100\u010a\u0126\u0128\u012f\u0149"+
		"\u014d\u014f\u0154\u015d\u0164\u016a\u0171\u0175\u017e\u0182\u0184\u0189"+
		"\u0192\u0199\u019f\u01a6";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}