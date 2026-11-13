// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammar.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Frozen context-free grammar image.
 *
 * <p>Runtime structure is integer-addressed: symbols, productions, RHS spans, LHS production
 * postings and precomputed nullable/FIRST/FOLLOW lanes. No rule parsing occurs per input.</p>
 */
public final class M3Grammar {
  private final String identity;
  private final M3GrammarFamily family;
  private final M3GrammarStrictness strictness;
  private final M3LexerProfile lexerProfile;
  private final List<M3GrammarSource> sources;
  private final int startSymbol;
  private final String[] symbolNames;
  private final byte[] symbolKinds;
  private final byte[] tokenClasses;
  private final byte[] literalCases;
  private final int[] productionLhs;
  private final int[] rhsStart;
  private final int[] rhsSymbols;
  private final int[] lhsProductionStart;
  private final int[] productionByLhs;
  private final byte[] nullable;
  private final byte[] productive;
  private final byte[] reachable;
  private final byte[] leftRecursive;
  private final int[] minimumTokens;
  private final long[] firstBits;
  private final long[] followBits;
  private final byte[] followEof;
  private final int[] terminalOrdinalBySymbol;
  private final int[] terminalSymbolByOrdinal;
  private final int terminalWords;
  private final long fingerprint;
  private volatile M3GrammarDecisionIndex ownedDecisions;

  private M3Grammar(
      String identity, M3GrammarFamily family, M3GrammarStrictness strictness, M3LexerProfile lexerProfile,
      List<M3GrammarSource> sources,
      int startSymbol,
      String[] symbolNames,
      byte[] symbolKinds,
      byte[] tokenClasses,
      byte[] literalCases,
      int[] productionLhs, int[] rhsStart, int[] rhsSymbols, int[] lhsProductionStart, int[] productionByLhs,
      M3GrammarAnalysis.Result analysis, long fingerprint) {
    this.identity = identity; this.family = family; this.strictness = strictness; this.lexerProfile = lexerProfile;
    this.sources = sources; this.startSymbol = startSymbol; this.symbolNames = symbolNames; this.symbolKinds = symbolKinds;
    this.tokenClasses = tokenClasses; this.literalCases = literalCases; this.productionLhs = productionLhs; this.rhsStart = rhsStart; this.rhsSymbols = rhsSymbols;
    this.lhsProductionStart = lhsProductionStart; this.productionByLhs = productionByLhs;
    nullable = analysis.nullable();
    productive = analysis.productive();
    reachable = analysis.reachable();
    leftRecursive = analysis.leftRecursive();
    minimumTokens = analysis.minimumTokens();
    firstBits = analysis.first();
    followBits = analysis.follow();
    followEof = analysis.followEof();
    terminalOrdinalBySymbol = analysis.terminalOrdinalBySymbol(); terminalSymbolByOrdinal = analysis.terminalSymbolByOrdinal();
    terminalWords = analysis.terminalWords(); this.fingerprint = fingerprint;
  }

  static M3Grammar compile(
      String identity, M3GrammarFamily family, M3GrammarStrictness strictness, M3LexerProfile lexerProfile,
      List<M3GrammarSource> sources,
      int startSymbol,
      String[] symbolNames,
      byte[] symbolKinds,
      byte[] tokenClasses,
      byte[] literalCases,
      int[] productionLhs, int[] rhsStart, int[] rhsSymbols, int[] lhsProductionStart, int[] productionByLhs) {
    validate(
        symbolNames,
        symbolKinds,
        tokenClasses,
        literalCases,
        productionLhs,
        rhsStart,
        rhsSymbols,
        startSymbol);
    M3GrammarAnalysis.Result analysis =
        M3GrammarAnalysis.compute(
            symbolKinds, productionLhs, rhsStart, rhsSymbols, startSymbol);
    if (analysis.productive()[startSymbol] == 0) {
      throw new M3GrammarException("start symbol is not productive: " + symbolNames[startSymbol]);
    }
    long fingerprint =
        fingerprint(
            identity,
            family,
            strictness,
            lexerProfile,
            symbolNames,
            symbolKinds,
            tokenClasses,
            literalCases,
            productionLhs,
            rhsStart,
            rhsSymbols);
    return new M3Grammar(
        identity,
        family,
        strictness,
        lexerProfile,
        sources,
        startSymbol,
        symbolNames,
        symbolKinds,
        tokenClasses,
        literalCases,
        productionLhs,
        rhsStart,
        rhsSymbols,
        lhsProductionStart,
        productionByLhs,
        analysis,
        fingerprint);
  }

  public String identity() { return identity; }
  public M3GrammarFamily family() { return family; }
  public M3GrammarStrictness strictness() { return strictness; }
  public M3LexerProfile lexerProfile() { return lexerProfile; }
  public List<M3GrammarSource> sources() { return sources; }
  public long fingerprint() { return fingerprint; }
  public int startSymbol() { return startSymbol; }
  public int symbolCount() { return symbolNames.length; }
  public int productionCount() { return productionLhs.length; }
  public int terminalCount() { return terminalSymbolByOrdinal.length; }
  public String symbolName(int symbol) { return symbolNames[Objects.checkIndex(symbol, symbolNames.length)]; }
  public M3GrammarSymbolKind symbolKind(int symbol) { return M3GrammarSymbolKind.values()[Byte.toUnsignedInt(symbolKinds[Objects.checkIndex(symbol, symbolKinds.length)])]; }
  public Optional<M3TokenClass> tokenClass(int symbol) {
    int value = tokenClasses[Objects.checkIndex(symbol, tokenClasses.length)];
    return value < 0 ? Optional.empty() : Optional.of(M3TokenClass.values()[value]);
  }
  public M3LiteralCase literalCase(int symbol) {
    return M3LiteralCase.values()[
        Byte.toUnsignedInt(literalCases[Objects.checkIndex(symbol, literalCases.length)])];
  }
  public int productionLhs(int production) { return productionLhs[Objects.checkIndex(production, productionLhs.length)]; }
  public int productionRhsLength(int production) { int p = Objects.checkIndex(production, productionLhs.length); return rhsStart[p + 1] - rhsStart[p]; }
  public int productionRhsSymbol(int production, int ordinal) { int p = Objects.checkIndex(production, productionLhs.length); int length = rhsStart[p + 1] - rhsStart[p]; return rhsSymbols[rhsStart[p] + Objects.checkIndex(ordinal, length)]; }
  public int productionCountFor(int nonTerminal) { int s = checkNonTerminal(nonTerminal); return lhsProductionStart[s + 1] - lhsProductionStart[s]; }
  public int productionFor(int nonTerminal, int ordinal) { int s = checkNonTerminal(nonTerminal); int length = lhsProductionStart[s + 1] - lhsProductionStart[s]; return productionByLhs[lhsProductionStart[s] + Objects.checkIndex(ordinal, length)]; }
  public boolean nullable(int symbol) {
    return nullable[Objects.checkIndex(symbol, nullable.length)] != 0;
  }

  public boolean productive(int symbol) {
    return productive[Objects.checkIndex(symbol, productive.length)] != 0;
  }

  public boolean reachable(int symbol) {
    return reachable[Objects.checkIndex(symbol, reachable.length)] != 0;
  }

  public boolean leftRecursive(int nonTerminal) {
    return leftRecursive[checkNonTerminal(nonTerminal)] != 0;
  }

  /** Minimum number of lexer tokens derivable from this symbol, or -1 when unproductive. */
  public int minimumTokens(int symbol) {
    return minimumTokens[Objects.checkIndex(symbol, minimumTokens.length)];
  }

  public int[] unreachableSymbols() {
    int count = 0;
    for (byte value : reachable) if (value == 0) count++;
    int[] result = new int[count];
    int out = 0;
    for (int symbol = 0; symbol < reachable.length; symbol++) {
      if (reachable[symbol] == 0) result[out++] = symbol;
    }
    return result;
  }

  public boolean canEndAfter(int nonTerminal) {
    return followEof[checkNonTerminal(nonTerminal)] != 0;
  }
  public int[] firstSymbols(int symbol) { return symbolsFromBits(firstBits, Objects.checkIndex(symbol, symbolCount())); }
  public int[] followSymbols(int nonTerminal) { return symbolsFromBits(followBits, checkNonTerminal(nonTerminal)); }
  public int findNonTerminal(String name) {
    Objects.requireNonNull(name, "name");
    for (int s = 0; s < symbolNames.length; s++) if (symbolKind(s) == M3GrammarSymbolKind.NON_TERMINAL && symbolNames[s].equals(name)) return s;
    return -1;
  }
  public M3TokenStream lex(CharSequence source) { return new M3GrammarLexer(lexerProfile).lex(source); }

  /** Predictive dispatch index owned by this exact immutable grammar object. */
  public M3GrammarDecisionIndex decisionIndex() {
    M3GrammarDecisionIndex existing = ownedDecisions;
    if (existing != null) return existing;
    synchronized (this) {
      if (ownedDecisions == null) ownedDecisions = M3GrammarDecisionIndex.build(this);
      return ownedDecisions;
    }
  }
  public M3GrammarValidation validate(CharSequence source) {
    return new M3GrammarRecognizer(this).validate(source);
  }

  public M3GrammarParseResult parse(CharSequence source) {
    return new M3GrammarParser(this).parse(source);
  }

  public void requireValid(CharSequence source) {
    M3GrammarValidation validation = validate(source);
    if (!validation.accepted()) throw new M3GrammarException("input does not conform to " + identity + ": " + validation);
  }
  public long primitivePayloadBytes() {
    return (long) symbolKinds.length
        + tokenClasses.length
        + literalCases.length
        + nullable.length
        + productive.length
        + reachable.length
        + leftRecursive.length
        + followEof.length
        + Long.BYTES * ((long) firstBits.length + followBits.length)
        + Integer.BYTES
            * ((long) productionLhs.length
                    + rhsStart.length
                    + rhsSymbols.length
                    + lhsProductionStart.length
                    + productionByLhs.length
                    + minimumTokens.length
                    + terminalOrdinalBySymbol.length
                    + terminalSymbolByOrdinal.length);
  }

  boolean isNonTerminal(int symbol) { return Byte.toUnsignedInt(symbolKinds[symbol]) == M3GrammarSymbolKind.NON_TERMINAL.ordinal(); }
  boolean terminalMatches(int symbol, M3TokenStream tokens, int tokenIndex) {
    M3GrammarSymbolKind kind = symbolKind(symbol);
    if (kind == M3GrammarSymbolKind.LITERAL) {
      boolean ignoreCase =
          switch (literalCase(symbol)) {
            case PROFILE -> lexerProfile.caseInsensitiveLiterals();
            case SENSITIVE -> false;
            case INSENSITIVE -> true;
          };
      return tokens.matchesLiteral(tokenIndex, symbolNames[symbol], ignoreCase);
    }
    if (kind == M3GrammarSymbolKind.TOKEN_CLASS) { M3TokenClass expected = M3TokenClass.values()[tokenClasses[symbol]]; return expected == M3TokenClass.ANY || tokens.tokenClass(tokenIndex) == expected; }
    return false;
  }
  int rhsStart(int production) { return rhsStart[production]; }
  int rhsEnd(int production) { return rhsStart[production + 1]; }
  int rhsSymbolAbsolute(int absolute) { return rhsSymbols[absolute]; }

  private int checkNonTerminal(int symbol) { Objects.checkIndex(symbol, symbolCount()); if (!isNonTerminal(symbol)) throw new IllegalArgumentException("not a non-terminal: " + symbol); return symbol; }
  private int[] symbolsFromBits(long[] matrix, int row) {
    int count = 0, base = row * terminalWords;
    for (int w = 0; w < terminalWords; w++) count += Long.bitCount(matrix[base + w]);
    int[] result = new int[count]; int out = 0;
    for (int ordinal = 0; ordinal < terminalSymbolByOrdinal.length; ordinal++) if ((matrix[base + (ordinal >>> 6)] & (1L << ordinal)) != 0L) result[out++] = terminalSymbolByOrdinal[ordinal];
    return result;
  }
  private static void validate(
      String[] names,
      byte[] kinds,
      byte[] tokenClasses,
      byte[] literalCases,
      int[] lhs,
      int[] rhsStart,
      int[] rhs,
      int start) {
    if (names.length != kinds.length
        || names.length != tokenClasses.length
        || names.length != literalCases.length) throw new M3GrammarException("symbol lane length mismatch");
    Objects.checkIndex(start, names.length);
    if (Byte.toUnsignedInt(kinds[start]) != M3GrammarSymbolKind.NON_TERMINAL.ordinal()) throw new M3GrammarException("start symbol must be non-terminal");
    if (rhsStart.length != lhs.length + 1 || rhsStart[0] != 0 || rhsStart[rhsStart.length - 1] != rhs.length) throw new M3GrammarException("invalid production spans");
    for (int p = 0; p < lhs.length; p++) { Objects.checkIndex(lhs[p], names.length); if (Byte.toUnsignedInt(kinds[lhs[p]]) != M3GrammarSymbolKind.NON_TERMINAL.ordinal()) throw new M3GrammarException("production LHS is terminal"); if (rhsStart[p] > rhsStart[p + 1]) throw new M3GrammarException("descending RHS span"); }
    for (int symbol : rhs) Objects.checkIndex(symbol, names.length);
  }
  private static long fingerprint(
      String identity,
      M3GrammarFamily family,
      M3GrammarStrictness strictness,
      M3LexerProfile lexerProfile,
      String[] names,
      byte[] kinds,
      byte[] tokenClasses,
      byte[] literalCases,
      int[] lhs,
      int[] starts,
      int[] rhs) {
    long hash = mix(0x4d494e4752414d31L, identity.hashCode());
    hash = mix(hash, family.ordinal());
    hash = mix(hash, strictness.ordinal());
    hash = mix(hash, lexerProfile.fingerprint());
    for (int i = 0; i < names.length; i++) {
      hash = mix(hash, names[i].hashCode());
      hash = mix(hash, kinds[i]);
      hash = mix(hash, tokenClasses[i]);
      hash = mix(hash, literalCases[i]);
    }
    for (int value : lhs) hash = mix(hash, value); for (int value : starts) hash = mix(hash, value); for (int value : rhs) hash = mix(hash, value); return hash;
  }
  private static long mix(long left, long right) { long z = left ^ Long.rotateLeft(right + 0x9e3779b97f4a7c15L, 29); z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L; z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL; return z ^ (z >>> 31); }
}
