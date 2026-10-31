// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.grammar.MIndexExpressionOperator;
import com.synexia.indexstring.grammar.MIndexGrammar;
import com.synexia.indexstring.grammar.MIndexGrammarDecisionIndex;
import com.synexia.indexstring.grammar.MIndexGrammarParseTree;
import com.synexia.indexstring.grammar.MIndexGrammarSymbolKind;
import com.synexia.indexstring.grammar.MIndexLang;
import com.synexia.indexstring.grammar.MIndexLiteralCase;
import com.synexia.indexstring.grammar.MIndexTokenClass;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Frozen AST-generation/specification image for one {@link MIndexLang}.
 *
 * <p>This is the language-neutral layer below Java's Tree.Kind-specific metadata. Every formal
 * language gets the same integer-addressed syntax geometry from its compiled grammar before any
 * source is parsed: symbol facts, production facts, FIRST/FOLLOW prediction sets, reverse
 * production-use postings, operator mappings and stable syntax hashes.</p>
 */
public final class MIndexASTSpecPrecompute {
  private static final byte FLAG_NULLABLE = 1;
  private static final byte FLAG_PRODUCTIVE = 1 << 1;
  private static final byte FLAG_REACHABLE = 1 << 2;
  private static final byte FLAG_LEFT_RECURSIVE = 1 << 3;
  private static final byte FLAG_CAN_END = 1 << 4;
  private static final byte FLAG_OPERATOR_TERMINAL = 1 << 5;

  private final MIndexLang lang;
  private final MIndexGrammar grammar;
  private final MIndexLanguageSpec sourceSpec;
  private final int symbolCount;
  private final int productionCount;
  private final byte[] symbolKinds;
  private final byte[] tokenClasses;
  private final byte[] literalCases;
  private final byte[] symbolFlags;
  private final int[] minimumTokens;
  private final long[] symbolHashes;

  private final int[] firstOffsets;
  private final int[] firstSymbols;
  private final int[] followOffsets;
  private final int[] followSymbols;

  private final int[] productionLhs;
  private final int[] productionRhsStarts;
  private final int[] productionRhsSymbols;
  private final int[] productionMinimumTokens;
  private final byte[] productionCanEndAtEof;
  private final long[] productionHashes;
  private final int[] predictionOffsets;
  private final int[] predictionSymbols;

  private final int[] useOffsets;
  private final int[] productionsUsingSymbol;
  private final int[] operatorIndexBySymbol;
  private final int[] sourceRuleOperatorIndexes;
  private final MIndexASTProductionPlans productionPlans;
  private final MIndexASTProductionDispatch productionDispatch;
  private final long fingerprint;

  MIndexASTSpecPrecompute(MIndexLang lang) {
    this.lang = Objects.requireNonNull(lang, "lang");
    this.grammar = lang.grammar();
    this.sourceSpec = lang.sourceSpec().orElse(null);
    this.symbolCount = grammar.symbolCount();
    this.productionCount = grammar.productionCount();

    symbolKinds = new byte[symbolCount];
    tokenClasses = new byte[symbolCount];
    literalCases = new byte[symbolCount];
    symbolFlags = new byte[symbolCount];
    minimumTokens = new int[symbolCount];
    symbolHashes = new long[symbolCount];
    Arrays.fill(tokenClasses, (byte) -1);

    int[] firstCounts = new int[symbolCount];
    int[] followCounts = new int[symbolCount];

    for (int symbol = 0; symbol < symbolCount; symbol++) {
      MIndexGrammarSymbolKind kind = grammar.symbolKind(symbol);
      symbolKinds[symbol] = (byte) kind.ordinal();
      Optional<MIndexTokenClass> tokenClass = grammar.tokenClass(symbol);
      if (tokenClass.isPresent()) tokenClasses[symbol] = (byte) tokenClass.orElseThrow().ordinal();
      MIndexLiteralCase literalCase = grammar.literalCase(symbol);
      literalCases[symbol] = (byte) literalCase.ordinal();

      byte flags = 0;
      if (grammar.nullable(symbol)) flags |= FLAG_NULLABLE;
      if (grammar.productive(symbol)) flags |= FLAG_PRODUCTIVE;
      if (grammar.reachable(symbol)) flags |= FLAG_REACHABLE;
      if (kind == MIndexGrammarSymbolKind.NON_TERMINAL) {
        if (grammar.leftRecursive(symbol)) flags |= FLAG_LEFT_RECURSIVE;
        if (grammar.canEndAfter(symbol)) flags |= FLAG_CAN_END;
      }
      symbolFlags[symbol] = flags;
      minimumTokens[symbol] = grammar.minimumTokens(symbol);
      symbolHashes[symbol] = stableSymbolHash(grammar, symbol);

      firstCounts[symbol] = grammar.firstSymbols(symbol).length;
      followCounts[symbol] =
          kind == MIndexGrammarSymbolKind.NON_TERMINAL
              ? grammar.followSymbols(symbol).length
              : 0;
    }

    firstOffsets = offsets(firstCounts);
    firstSymbols = new int[firstOffsets[symbolCount]];
    followOffsets = offsets(followCounts);
    followSymbols = new int[followOffsets[symbolCount]];

    for (int symbol = 0; symbol < symbolCount; symbol++) {
      int[] first = grammar.firstSymbols(symbol);
      System.arraycopy(first, 0, firstSymbols, firstOffsets[symbol], first.length);
      if (grammar.symbolKind(symbol) == MIndexGrammarSymbolKind.NON_TERMINAL) {
        int[] follow = grammar.followSymbols(symbol);
        System.arraycopy(follow, 0, followSymbols, followOffsets[symbol], follow.length);
      }
    }

    productionLhs = new int[productionCount];
    productionRhsStarts = new int[productionCount + 1];
    int rhsTotal = 0;
    for (int production = 0; production < productionCount; production++) {
      productionLhs[production] = grammar.productionLhs(production);
      productionRhsStarts[production] = rhsTotal;
      rhsTotal = Math.addExact(rhsTotal, grammar.productionRhsLength(production));
    }
    productionRhsStarts[productionCount] = rhsTotal;
    productionRhsSymbols = new int[rhsTotal];
    productionMinimumTokens = new int[productionCount];
    productionCanEndAtEof = new byte[productionCount];
    productionHashes = new long[productionCount];

    int[] useCounts = new int[symbolCount];
    int[] predictCounts = new int[productionCount];
    boolean[] seen = new boolean[symbolCount];
    boolean[] usedInProduction = new boolean[symbolCount];

    int rhsCursor = 0;
    for (int production = 0; production < productionCount; production++) {
      int rhsLength = grammar.productionRhsLength(production);
      int minimum = 0;
      boolean productive = true;
      Arrays.fill(usedInProduction, false);
      for (int ordinal = 0; ordinal < rhsLength; ordinal++) {
        int symbol = grammar.productionRhsSymbol(production, ordinal);
        productionRhsSymbols[rhsCursor++] = symbol;
        if (!usedInProduction[symbol]) {
          usedInProduction[symbol] = true;
          useCounts[symbol]++;
        }

        int symbolMinimum = grammar.minimumTokens(symbol);
        if (symbolMinimum < 0) productive = false;
        else if (productive) minimum = Math.addExact(minimum, symbolMinimum);
      }
      productionMinimumTokens[production] = productive ? minimum : -1;
      productionHashes[production] = stableProductionHash(grammar, production);

      Arrays.fill(seen, false);
      boolean allNullable = true;
      for (int ordinal = 0; ordinal < rhsLength; ordinal++) {
        int symbol = grammar.productionRhsSymbol(production, ordinal);
        for (int candidate : grammar.firstSymbols(symbol)) {
          if (!seen[candidate]) {
            seen[candidate] = true;
            predictCounts[production]++;
          }
        }
        if (!grammar.nullable(symbol)) {
          allNullable = false;
          break;
        }
      }
      if (rhsLength == 0) allNullable = true;
      if (allNullable) {
        int lhs = grammar.productionLhs(production);
        for (int candidate : grammar.followSymbols(lhs)) {
          if (!seen[candidate]) {
            seen[candidate] = true;
            predictCounts[production]++;
          }
        }
        if (grammar.canEndAfter(lhs)) productionCanEndAtEof[production] = 1;
      }
    }

    useOffsets = offsets(useCounts);
    productionsUsingSymbol = new int[useOffsets[symbolCount]];
    int[] useCursor = Arrays.copyOf(useOffsets, symbolCount);
    for (int production = 0; production < productionCount; production++) {
      Arrays.fill(usedInProduction, false);
      int from = productionRhsStarts[production];
      int to = productionRhsStarts[production + 1];
      for (int index = from; index < to; index++) {
        int symbol = productionRhsSymbols[index];
        if (!usedInProduction[symbol]) {
          usedInProduction[symbol] = true;
          productionsUsingSymbol[useCursor[symbol]++] = production;
        }
      }
    }

    predictionOffsets = offsets(predictCounts);
    predictionSymbols = new int[predictionOffsets[productionCount]];
    for (int production = 0; production < productionCount; production++) {
      Arrays.fill(seen, false);
      int write = predictionOffsets[production];
      int rhsLength = grammar.productionRhsLength(production);
      boolean allNullable = true;
      for (int ordinal = 0; ordinal < rhsLength; ordinal++) {
        int symbol = grammar.productionRhsSymbol(production, ordinal);
        for (int candidate : grammar.firstSymbols(symbol)) {
          if (!seen[candidate]) {
            seen[candidate] = true;
            predictionSymbols[write++] = candidate;
          }
        }
        if (!grammar.nullable(symbol)) {
          allNullable = false;
          break;
        }
      }
      if (rhsLength == 0) allNullable = true;
      if (allNullable) {
        int lhs = grammar.productionLhs(production);
        for (int candidate : grammar.followSymbols(lhs)) {
          if (!seen[candidate]) {
            seen[candidate] = true;
            predictionSymbols[write++] = candidate;
          }
        }
      }
      Arrays.sort(
          predictionSymbols,
          predictionOffsets[production],
          predictionOffsets[production + 1]);
    }

    operatorIndexBySymbol = new int[symbolCount];
    Arrays.fill(operatorIndexBySymbol, -1);
    for (int operator = 0; operator < lang.expressionSpec().operatorCount(); operator++) {
      MIndexExpressionOperator value = lang.expressionSpec().operatorAt(operator);
      for (int symbol = 0; symbol < symbolCount; symbol++) {
        if (grammar.symbolKind(symbol) == MIndexGrammarSymbolKind.LITERAL
            && grammar.symbolName(symbol).equals(value.token())) {
          if (operatorIndexBySymbol[symbol] < 0) operatorIndexBySymbol[symbol] = operator;
          symbolFlags[symbol] |= FLAG_OPERATOR_TERMINAL;
        }
      }
    }

    int sourceRuleCount = sourceSpec == null ? 0 : sourceSpec.ruleCount();
    sourceRuleOperatorIndexes = new int[sourceRuleCount];
    Arrays.fill(sourceRuleOperatorIndexes, -1);
    if (sourceSpec != null) {
      for (int rule = 0; rule < sourceRuleCount; rule++) {
        MIndexOperatorRule operator = sourceSpec.rule(rule).operator();
        if (!operator.isOperator()) continue;
        sourceRuleOperatorIndexes[rule] = findExpressionOperator(operator.token());
      }
    }

    productionPlans = MIndexASTProductionPlans.build(this);
    productionDispatch = MIndexASTProductionDispatch.build(this);
    fingerprint = computeFingerprint();
  }

  public MIndexLang lang() {
    return lang;
  }

  public String identity() {
    return lang.identity();
  }

  public String language() {
    return lang.language();
  }

  public String version() {
    return lang.version();
  }

  public MIndexGrammar grammar() {
    return grammar;
  }

  /** Canonical process-level predictive dispatch derived from the same immutable grammar. */
  public MIndexGrammarDecisionIndex decisionIndex() {
    return grammar.decisionIndex();
  }

  public long fingerprint() {
    return fingerprint;
  }

  public int symbolCount() {
    return symbolCount;
  }

  public int productionCount() {
    return productionCount;
  }

  public MIndexGrammarSymbolKind symbolKind(int symbol) {
    return MIndexGrammarSymbolKind.values()[
        Byte.toUnsignedInt(symbolKinds[checkSymbol(symbol)])];
  }

  public Optional<MIndexTokenClass> tokenClass(int symbol) {
    int encoded = tokenClasses[checkSymbol(symbol)];
    return encoded < 0
        ? Optional.empty()
        : Optional.of(MIndexTokenClass.values()[encoded]);
  }

  public MIndexLiteralCase literalCase(int symbol) {
    return MIndexLiteralCase.values()[
        Byte.toUnsignedInt(literalCases[checkSymbol(symbol)])];
  }

  public boolean nullable(int symbol) {
    return flag(symbol, FLAG_NULLABLE);
  }

  public boolean productive(int symbol) {
    return flag(symbol, FLAG_PRODUCTIVE);
  }

  public boolean reachable(int symbol) {
    return flag(symbol, FLAG_REACHABLE);
  }

  public boolean leftRecursive(int symbol) {
    return flag(symbol, FLAG_LEFT_RECURSIVE);
  }

  public boolean canEndAfter(int symbol) {
    return flag(symbol, FLAG_CAN_END);
  }

  public int minimumTokens(int symbol) {
    return minimumTokens[checkSymbol(symbol)];
  }

  public long symbolHash64(int symbol) {
    return symbolHashes[checkSymbol(symbol)];
  }

  public int[] firstSymbols(int symbol) {
    int checked = checkSymbol(symbol);
    return Arrays.copyOfRange(
        firstSymbols, firstOffsets[checked], firstOffsets[checked + 1]);
  }

  public int[] followSymbols(int symbol) {
    int checked = checkSymbol(symbol);
    return Arrays.copyOfRange(
        followSymbols, followOffsets[checked], followOffsets[checked + 1]);
  }

  public int productionLhs(int production) {
    return productionLhs[checkProduction(production)];
  }

  public int productionRhsLength(int production) {
    int checked = checkProduction(production);
    return productionRhsStarts[checked + 1] - productionRhsStarts[checked];
  }

  public int productionRhsSymbol(int production, int ordinal) {
    int checked = checkProduction(production);
    int length = productionRhsLength(checked);
    return productionRhsSymbols[
        productionRhsStarts[checked] + Objects.checkIndex(ordinal, length)];
  }

  public int productionMinimumTokens(int production) {
    return productionMinimumTokens[checkProduction(production)];
  }

  public boolean productionCanEndAtEof(int production) {
    return productionCanEndAtEof[checkProduction(production)] != 0;
  }

  public long productionHash64(int production) {
    return productionHashes[checkProduction(production)];
  }

  public int[] predictionSymbols(int production) {
    int checked = checkProduction(production);
    return Arrays.copyOfRange(
        predictionSymbols,
        predictionOffsets[checked],
        predictionOffsets[checked + 1]);
  }

  public boolean predictsTerminal(int production, int terminalSymbol) {
    int checkedProduction = checkProduction(production);
    int checkedSymbol = checkSymbol(terminalSymbol);
    if (grammar.symbolKind(checkedSymbol) == MIndexGrammarSymbolKind.NON_TERMINAL) return false;
    return Arrays.binarySearch(
            predictionSymbols,
            predictionOffsets[checkedProduction],
            predictionOffsets[checkedProduction + 1],
            checkedSymbol)
        >= 0;
  }

  public int[] productionsUsingSymbol(int symbol) {
    int checked = checkSymbol(symbol);
    return Arrays.copyOfRange(
        productionsUsingSymbol, useOffsets[checked], useOffsets[checked + 1]);
  }

  public boolean operatorTerminal(int symbol) {
    return flag(symbol, FLAG_OPERATOR_TERMINAL);
  }

  public int operatorIndexForSymbol(int symbol) {
    return operatorIndexBySymbol[checkSymbol(symbol)];
  }

  public Optional<MIndexExpressionOperator> operatorForSymbol(int symbol) {
    int operator = operatorIndexForSymbol(symbol);
    return operator < 0
        ? Optional.empty()
        : Optional.of(lang.expressionSpec().operatorAt(operator));
  }

  public boolean hasSourceRules() {
    return sourceSpec != null;
  }

  public int sourceRuleCount() {
    return sourceRuleOperatorIndexes.length;
  }

  public MIndexLanguageRule sourceRule(int ruleIndex) {
    if (sourceSpec == null) throw new IllegalStateException("language has no source-tree rule image");
    return sourceSpec.rule(ruleIndex);
  }

  public int sourceRuleOperatorIndex(int ruleIndex) {
    return sourceRuleOperatorIndexes[Objects.checkIndex(ruleIndex, sourceRuleOperatorIndexes.length)];
  }

  public Optional<MIndexExpressionOperator> sourceRuleExpressionOperator(int ruleIndex) {
    int operator = sourceRuleOperatorIndex(ruleIndex);
    return operator < 0
        ? Optional.empty()
        : Optional.of(lang.expressionSpec().operatorAt(operator));
  }

  /**
   * Frozen production-to-AST construction plan for this language/DSL.
   *
   * <p>The plan is process-level data: child roles, nullable prefix/suffix facts, token offsets,
   * operator positions, reverse prediction tables and transitive terminal/operator reachability are
   * computed once from the grammar and reused for every parsed source instance.</p>
   */
  public MIndexASTProductionPlans productionPlans() {
    return productionPlans;
  }

  public MIndexASTProductionDispatch productionDispatch() {
    return productionDispatch;
  }

  public MIndexASTSpecTreePrecompute precompute(MIndexGrammarParseTree tree) {
    Objects.requireNonNull(tree, "tree");
    if (tree.grammar().fingerprint() != grammar.fingerprint()) {
      throw new IllegalArgumentException("parse tree belongs to a different grammar image");
    }
    return MIndexASTSpecTreePrecompute.build(this, tree);
  }

  public long primitivePayloadBytes() {
    return symbolKinds.length
        + tokenClasses.length
        + literalCases.length
        + symbolFlags.length
        + productionCanEndAtEof.length
        + Integer.BYTES
            * (long)
                (minimumTokens.length
                    + firstOffsets.length
                    + firstSymbols.length
                    + followOffsets.length
                    + followSymbols.length
                    + productionLhs.length
                    + productionRhsStarts.length
                    + productionRhsSymbols.length
                    + productionMinimumTokens.length
                    + predictionOffsets.length
                    + predictionSymbols.length
                    + useOffsets.length
                    + productionsUsingSymbol.length
                    + operatorIndexBySymbol.length
                    + sourceRuleOperatorIndexes.length)
        + Long.BYTES * (long) (symbolHashes.length + productionHashes.length)
        + productionPlans.primitivePayloadBytes()
        + productionDispatch.primitivePayloadBytes();
  }

  private boolean flag(int symbol, byte mask) {
    return (symbolFlags[checkSymbol(symbol)] & mask) != 0;
  }

  private int findExpressionOperator(String token) {
    for (int operator = 0; operator < lang.expressionSpec().operatorCount(); operator++) {
      if (lang.expressionSpec().operatorAt(operator).token().equals(token)) return operator;
    }
    return -1;
  }

  private int checkSymbol(int symbol) {
    return Objects.checkIndex(symbol, symbolCount);
  }

  private int checkProduction(int production) {
    return Objects.checkIndex(production, productionCount);
  }

  private long computeFingerprint() {
    long hash = avalanche(lang.fingerprint() ^ 0x4d494e4153545350L);
    for (long symbolHash : symbolHashes) hash = mix(hash, symbolHash);
    for (long productionHash : productionHashes) hash = mix(hash, productionHash);
    for (int operator : sourceRuleOperatorIndexes) hash = mix(hash, operator);
    hash = mix(hash, productionPlans.fingerprint());
    hash = mix(hash, productionDispatch.fingerprint());
    return avalanche(hash);
  }

  private static long stableSymbolHash(MIndexGrammar grammar, int symbol) {
    long hash = avalanche(grammar.fingerprint() ^ Integer.toUnsignedLong(symbol));
    hash = mix(hash, grammar.symbolKind(symbol).ordinal());
    hash = mix(hash, grammar.literalCase(symbol).ordinal());
    hash = mix(hash, grammar.minimumTokens(symbol));
    Optional<MIndexTokenClass> tokenClass = grammar.tokenClass(symbol);
    if (tokenClass.isPresent()) hash = mix(hash, tokenClass.orElseThrow().ordinal());
    CharSequence name = grammar.symbolName(symbol);
    for (int index = 0; index < name.length(); index++) hash = mix(hash, name.charAt(index));
    return avalanche(hash);
  }

  private static long stableProductionHash(MIndexGrammar grammar, int production) {
    long hash =
        avalanche(
            grammar.fingerprint()
                ^ 0x50524f445543544eL
                ^ Integer.toUnsignedLong(production));
    hash = mix(hash, grammar.productionLhs(production));
    int length = grammar.productionRhsLength(production);
    hash = mix(hash, length);
    for (int ordinal = 0; ordinal < length; ordinal++) {
      hash = mix(hash, grammar.productionRhsSymbol(production, ordinal));
    }
    return avalanche(hash);
  }

  private static int[] offsets(int[] counts) {
    int[] offsets = new int[counts.length + 1];
    for (int index = 0; index < counts.length; index++) {
      offsets[index + 1] = Math.addExact(offsets[index], counts[index]);
    }
    return offsets;
  }

  private static long mix(long left, long right) {
    return avalanche(left ^ Long.rotateLeft(right + 0x9e3779b97f4a7c15L, 23));
  }

  private static long avalanche(long value) {
    long z = value;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }
}
