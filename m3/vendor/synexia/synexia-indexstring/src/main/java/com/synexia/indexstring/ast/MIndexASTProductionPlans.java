// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.grammar.MIndexGrammar;
import com.synexia.indexstring.grammar.MIndexGrammarSymbolKind;
import java.util.Arrays;
import java.util.Objects;

/**
 * Frozen production-to-AST construction plan derived from one language grammar.
 *
 * <p>The plan is built once from {@link MIndexASTSpecPrecompute}. It precomputes every RHS role,
 * nullable prefix/suffix, minimum-token coordinate, direct operator position, reverse LHS/prediction
 * posting and transitive terminal/operator derivation bitset needed by later AST builders.</p>
 */
public final class MIndexASTProductionPlans {
  private static final byte RHS_NULLABLE = 1;
  private static final byte RHS_PREFIX_NULLABLE = 1 << 1;
  private static final byte RHS_SUFFIX_NULLABLE = 1 << 2;
  private static final byte RHS_OPERATOR = 1 << 3;
  private static final int TERMINAL_MASK = Integer.MIN_VALUE;
  private static final int VALUE_MASK = Integer.MAX_VALUE;

  private final MIndexASTSpecPrecompute spec;
  private final MIndexGrammar grammar;
  private final int symbolCount;
  private final int productionCount;

  private final int[] rhsStarts;
  private final int[] rhsSymbols;
  private final byte[] rhsRoles;
  private final byte[] rhsFlags;
  private final int[] rhsOperatorIndexes;
  private final int[] minimumTokensBefore;
  private final int[] minimumTokensAfter;

  private final int[] terminalCounts;
  private final int[] nonTerminalCounts;
  private final int[] operatorCounts;
  private final long[] shapeHashes;
  private final long[] planHashes;

  private final int[] operatorOffsets;
  private final int[] operatorOrdinals;

  private final int[] lhsOffsets;
  private final int[] productionsByLhs;

  private final int[] predictionUseOffsets;
  private final int[] productionsPredictingSymbol;

  private final int[] terminalOrdinalBySymbol;
  private final int[] terminalSymbolByOrdinal;
  private final int terminalWords;
  private final long[] derivableTerminalBits;
  private final long[] productionTerminalBits;

  private final int operatorWords;
  private final long[] derivableOperatorBits;
  private final long[] productionOperatorBits;

  private final long fingerprint;

  private MIndexASTProductionPlans(
      MIndexASTSpecPrecompute spec,
      int[] rhsStarts,
      int[] rhsSymbols,
      byte[] rhsRoles,
      byte[] rhsFlags,
      int[] rhsOperatorIndexes,
      int[] minimumTokensBefore,
      int[] minimumTokensAfter,
      int[] terminalCounts,
      int[] nonTerminalCounts,
      int[] operatorCounts,
      long[] shapeHashes,
      long[] planHashes,
      int[] operatorOffsets,
      int[] operatorOrdinals,
      int[] lhsOffsets,
      int[] productionsByLhs,
      int[] predictionUseOffsets,
      int[] productionsPredictingSymbol,
      int[] terminalOrdinalBySymbol,
      int[] terminalSymbolByOrdinal,
      int terminalWords,
      long[] derivableTerminalBits,
      long[] productionTerminalBits,
      int operatorWords,
      long[] derivableOperatorBits,
      long[] productionOperatorBits,
      long fingerprint) {
    this.spec = spec;
    this.grammar = spec.grammar();
    this.symbolCount = spec.symbolCount();
    this.productionCount = spec.productionCount();
    this.rhsStarts = rhsStarts;
    this.rhsSymbols = rhsSymbols;
    this.rhsRoles = rhsRoles;
    this.rhsFlags = rhsFlags;
    this.rhsOperatorIndexes = rhsOperatorIndexes;
    this.minimumTokensBefore = minimumTokensBefore;
    this.minimumTokensAfter = minimumTokensAfter;
    this.terminalCounts = terminalCounts;
    this.nonTerminalCounts = nonTerminalCounts;
    this.operatorCounts = operatorCounts;
    this.shapeHashes = shapeHashes;
    this.planHashes = planHashes;
    this.operatorOffsets = operatorOffsets;
    this.operatorOrdinals = operatorOrdinals;
    this.lhsOffsets = lhsOffsets;
    this.productionsByLhs = productionsByLhs;
    this.predictionUseOffsets = predictionUseOffsets;
    this.productionsPredictingSymbol = productionsPredictingSymbol;
    this.terminalOrdinalBySymbol = terminalOrdinalBySymbol;
    this.terminalSymbolByOrdinal = terminalSymbolByOrdinal;
    this.terminalWords = terminalWords;
    this.derivableTerminalBits = derivableTerminalBits;
    this.productionTerminalBits = productionTerminalBits;
    this.operatorWords = operatorWords;
    this.derivableOperatorBits = derivableOperatorBits;
    this.productionOperatorBits = productionOperatorBits;
    this.fingerprint = fingerprint;
  }

  static MIndexASTProductionPlans build(MIndexASTSpecPrecompute spec) {
    Objects.requireNonNull(spec, "spec");
    MIndexGrammar grammar = spec.grammar();
    int symbolCount = spec.symbolCount();
    int productionCount = spec.productionCount();

    int[] rhsStarts = new int[productionCount + 1];
    int rhsTotal = 0;
    for (int production = 0; production < productionCount; production++) {
      rhsStarts[production] = rhsTotal;
      rhsTotal = Math.addExact(rhsTotal, spec.productionRhsLength(production));
    }
    rhsStarts[productionCount] = rhsTotal;

    int[] rhsSymbols = new int[rhsTotal];
    byte[] rhsRoles = new byte[rhsTotal];
    byte[] rhsFlags = new byte[rhsTotal];
    int[] rhsOperatorIndexes = new int[rhsTotal];
    Arrays.fill(rhsOperatorIndexes, -1);
    int[] minimumTokensBefore = new int[rhsTotal];
    int[] minimumTokensAfter = new int[rhsTotal];
    Arrays.fill(minimumTokensBefore, -1);
    Arrays.fill(minimumTokensAfter, -1);

    int[] terminalCounts = new int[productionCount];
    int[] nonTerminalCounts = new int[productionCount];
    int[] operatorCounts = new int[productionCount];
    long[] shapeHashes = new long[productionCount];
    long[] planHashes = new long[productionCount];

    int directOperatorTotal = 0;
    for (int production = 0; production < productionCount; production++) {
      int from = rhsStarts[production];
      int length = spec.productionRhsLength(production);
      boolean prefixNullable = true;
      boolean prefixProductive = true;
      int prefixMinimum = 0;

      long shape = avalanche(0x415354504c414e31L ^ Integer.toUnsignedLong(length));
      for (int ordinal = 0; ordinal < length; ordinal++) {
        int absolute = from + ordinal;
        int symbol = spec.productionRhsSymbol(production, ordinal);
        rhsSymbols[absolute] = symbol;
        MIndexASTChildRole role = role(spec, symbol);
        rhsRoles[absolute] = (byte) role.ordinal();

        byte flags = 0;
        if (spec.nullable(symbol)) flags |= RHS_NULLABLE;
        if (prefixNullable) flags |= RHS_PREFIX_NULLABLE;

        int operator = spec.operatorIndexForSymbol(symbol);
        if (operator >= 0) {
          flags |= RHS_OPERATOR;
          rhsOperatorIndexes[absolute] = operator;
          operatorCounts[production]++;
          directOperatorTotal++;
        }

        if (prefixProductive) minimumTokensBefore[absolute] = prefixMinimum;
        int minimum = spec.minimumTokens(symbol);
        if (minimum < 0) {
          prefixProductive = false;
        } else if (prefixProductive) {
          prefixMinimum = Math.addExact(prefixMinimum, minimum);
        }
        prefixNullable &= spec.nullable(symbol);
        rhsFlags[absolute] = flags;

        if (role == MIndexASTChildRole.NON_TERMINAL) nonTerminalCounts[production]++;
        else terminalCounts[production]++;

        shape = mix(shape, role.ordinal());
        shape = mix(shape, spec.tokenClass(symbol).map(Enum::ordinal).orElse(-1));
        shape = mix(shape, operator >= 0 ? 1L : 0L);
      }

      boolean suffixNullable = true;
      boolean suffixProductive = true;
      int suffixMinimum = 0;
      for (int ordinal = length - 1; ordinal >= 0; ordinal--) {
        int absolute = from + ordinal;
        if (suffixNullable) rhsFlags[absolute] |= RHS_SUFFIX_NULLABLE;
        if (suffixProductive) minimumTokensAfter[absolute] = suffixMinimum;

        int symbol = rhsSymbols[absolute];
        int minimum = spec.minimumTokens(symbol);
        if (minimum < 0) {
          suffixProductive = false;
        } else if (suffixProductive) {
          suffixMinimum = Math.addExact(suffixMinimum, minimum);
        }
        suffixNullable &= spec.nullable(symbol);
      }
      shapeHashes[production] = avalanche(shape);
      planHashes[production] =
          avalanche(
              mix(
                  spec.productionHash64(production),
                  shapeHashes[production] ^ Integer.toUnsignedLong(spec.productionMinimumTokens(production))));
    }

    int[] operatorOffsets = new int[productionCount + 1];
    for (int production = 0; production < productionCount; production++) {
      operatorOffsets[production + 1] =
          Math.addExact(operatorOffsets[production], operatorCounts[production]);
    }
    int[] operatorOrdinals = new int[directOperatorTotal];
    int operatorWrite = 0;
    for (int production = 0; production < productionCount; production++) {
      int from = rhsStarts[production];
      int to = rhsStarts[production + 1];
      for (int absolute = from; absolute < to; absolute++) {
        if ((rhsFlags[absolute] & RHS_OPERATOR) != 0) {
          operatorOrdinals[operatorWrite++] = absolute - from;
        }
      }
    }

    int[] lhsCounts = new int[symbolCount];
    for (int production = 0; production < productionCount; production++) {
      lhsCounts[spec.productionLhs(production)]++;
    }
    int[] lhsOffsets = offsets(lhsCounts);
    int[] productionsByLhs = new int[productionCount];
    int[] lhsCursor = Arrays.copyOf(lhsOffsets, lhsCounts.length);
    for (int production = 0; production < productionCount; production++) {
      int lhs = spec.productionLhs(production);
      productionsByLhs[lhsCursor[lhs]++] = production;
    }

    int[] predictionUseCounts = new int[symbolCount];
    for (int production = 0; production < productionCount; production++) {
      for (int symbol : spec.predictionSymbols(production)) predictionUseCounts[symbol]++;
    }
    int[] predictionUseOffsets = offsets(predictionUseCounts);
    int[] productionsPredictingSymbol =
        new int[predictionUseOffsets[predictionUseCounts.length]];
    int[] predictionCursor = Arrays.copyOf(predictionUseOffsets, predictionUseCounts.length);
    for (int production = 0; production < productionCount; production++) {
      for (int symbol : spec.predictionSymbols(production)) {
        productionsPredictingSymbol[predictionCursor[symbol]++] = production;
      }
    }

    int[] terminalOrdinalBySymbol = new int[symbolCount];
    Arrays.fill(terminalOrdinalBySymbol, -1);
    int terminalCount = 0;
    for (int symbol = 0; symbol < symbolCount; symbol++) {
      if (spec.symbolKind(symbol) != MIndexGrammarSymbolKind.NON_TERMINAL) {
        terminalOrdinalBySymbol[symbol] = terminalCount++;
      }
    }
    int[] terminalSymbolByOrdinal = new int[terminalCount];
    for (int symbol = 0; symbol < symbolCount; symbol++) {
      int ordinal = terminalOrdinalBySymbol[symbol];
      if (ordinal >= 0) terminalSymbolByOrdinal[ordinal] = symbol;
    }
    int terminalWords = Math.max(1, (terminalCount + 63) >>> 6);
    long[] derivableTerminalBits = new long[Math.multiplyExact(symbolCount, terminalWords)];
    for (int symbol = 0; symbol < symbolCount; symbol++) {
      int ordinal = terminalOrdinalBySymbol[symbol];
      if (ordinal >= 0) setBit(derivableTerminalBits, symbol, terminalWords, ordinal);
    }

    int operatorCount = spec.lang().expressionSpec().operatorCount();
    int operatorWords = Math.max(1, (operatorCount + 63) >>> 6);
    long[] derivableOperatorBits = new long[Math.multiplyExact(symbolCount, operatorWords)];
    for (int symbol = 0; symbol < symbolCount; symbol++) {
      int operator = spec.operatorIndexForSymbol(symbol);
      if (operator >= 0) setBit(derivableOperatorBits, symbol, operatorWords, operator);
    }

    propagateDerivationClosure(
        spec,
        derivableTerminalBits,
        terminalWords,
        derivableOperatorBits,
        operatorWords);

    long[] productionTerminalBits =
        new long[Math.multiplyExact(productionCount, terminalWords)];
    long[] productionOperatorBits =
        new long[Math.multiplyExact(productionCount, operatorWords)];
    for (int production = 0; production < productionCount; production++) {
      int from = rhsStarts[production];
      int to = rhsStarts[production + 1];
      for (int absolute = from; absolute < to; absolute++) {
        int symbol = rhsSymbols[absolute];
        orRows(
            productionTerminalBits,
            production,
            derivableTerminalBits,
            symbol,
            terminalWords);
        orRows(
            productionOperatorBits,
            production,
            derivableOperatorBits,
            symbol,
            operatorWords);
      }
    }

    long fingerprint = avalanche(spec.lang().fingerprint() ^ 0x415354504c414e53L);
    for (long planHash : planHashes) fingerprint = mix(fingerprint, planHash);
    for (int value : minimumTokensBefore) fingerprint = mix(fingerprint, value);
    for (int value : minimumTokensAfter) fingerprint = mix(fingerprint, value);
    fingerprint = avalanche(fingerprint);

    return new MIndexASTProductionPlans(
        spec,
        rhsStarts,
        rhsSymbols,
        rhsRoles,
        rhsFlags,
        rhsOperatorIndexes,
        minimumTokensBefore,
        minimumTokensAfter,
        terminalCounts,
        nonTerminalCounts,
        operatorCounts,
        shapeHashes,
        planHashes,
        operatorOffsets,
        operatorOrdinals,
        lhsOffsets,
        productionsByLhs,
        predictionUseOffsets,
        productionsPredictingSymbol,
        terminalOrdinalBySymbol,
        terminalSymbolByOrdinal,
        terminalWords,
        derivableTerminalBits,
        productionTerminalBits,
        operatorWords,
        derivableOperatorBits,
        productionOperatorBits,
        fingerprint);
  }

  public MIndexASTSpecPrecompute spec() {
    return spec;
  }

  public MIndexGrammar grammar() {
    return grammar;
  }

  public long fingerprint() {
    return fingerprint;
  }

  public int productionCount() {
    return productionCount;
  }

  public int productionRhsLength(int production) {
    int checked = checkProduction(production);
    return rhsStarts[checked + 1] - rhsStarts[checked];
  }

  public int rhsSymbol(int production, int ordinal) {
    int checked = checkProduction(production);
    return rhsSymbols[
        rhsStarts[checked] + Objects.checkIndex(ordinal, productionRhsLength(checked))];
  }

  public MIndexASTChildRole childRole(int production, int ordinal) {
    int absolute = absolute(production, ordinal);
    return MIndexASTChildRole.values()[Byte.toUnsignedInt(rhsRoles[absolute])];
  }

  public boolean childNullable(int production, int ordinal) {
    return flag(production, ordinal, RHS_NULLABLE);
  }

  /** True when every RHS symbol before this ordinal is nullable. */
  public boolean nullablePrefixBefore(int production, int ordinal) {
    return flag(production, ordinal, RHS_PREFIX_NULLABLE);
  }

  /** True when every RHS symbol after this ordinal is nullable. */
  public boolean nullableSuffixAfter(int production, int ordinal) {
    return flag(production, ordinal, RHS_SUFFIX_NULLABLE);
  }

  public int minimumTokensBefore(int production, int ordinal) {
    return minimumTokensBefore[absolute(production, ordinal)];
  }

  public int minimumTokensAfter(int production, int ordinal) {
    return minimumTokensAfter[absolute(production, ordinal)];
  }

  public boolean directOperator(int production, int ordinal) {
    return flag(production, ordinal, RHS_OPERATOR);
  }

  public int operatorIndex(int production, int ordinal) {
    return rhsOperatorIndexes[absolute(production, ordinal)];
  }

  public int terminalChildCount(int production) {
    return terminalCounts[checkProduction(production)];
  }

  public int nonTerminalChildCount(int production) {
    return nonTerminalCounts[checkProduction(production)];
  }

  public int directOperatorCount(int production) {
    return operatorCounts[checkProduction(production)];
  }

  public int[] directOperatorOrdinals(int production) {
    int checked = checkProduction(production);
    return Arrays.copyOfRange(
        operatorOrdinals, operatorOffsets[checked], operatorOffsets[checked + 1]);
  }

  /**
   * Role-only production shape hash.
   *
   * <p>It deliberately ignores grammar symbol names/IDs and the specific operator token. It is a
   * structural candidate key across productions/languages, not proof of semantic equivalence.</p>
   */
  public long shapeHash64(int production) {
    return shapeHashes[checkProduction(production)];
  }

  /** Exact stable AST-construction-plan hash for this grammar production. */
  public long planHash64(int production) {
    return planHashes[checkProduction(production)];
  }

  public int[] productionsForLhs(int nonTerminalSymbol) {
    int checked = checkSymbol(nonTerminalSymbol);
    if (spec.symbolKind(checked) != MIndexGrammarSymbolKind.NON_TERMINAL) return new int[0];
    return Arrays.copyOfRange(
        productionsByLhs, lhsOffsets[checked], lhsOffsets[checked + 1]);
  }

  public int[] productionsPredicting(int terminalSymbol) {
    int checked = checkSymbol(terminalSymbol);
    if (spec.symbolKind(checked) == MIndexGrammarSymbolKind.NON_TERMINAL) return new int[0];
    return Arrays.copyOfRange(
        productionsPredictingSymbol,
        predictionUseOffsets[checked],
        predictionUseOffsets[checked + 1]);
  }

  /** Exact transitive grammar fact: whether this symbol can derive the terminal anywhere below it. */
  public boolean derivesTerminal(int symbol, int terminalSymbol) {
    int checkedSymbol = checkSymbol(symbol);
    int checkedTerminal = checkSymbol(terminalSymbol);
    int ordinal = terminalOrdinalBySymbol[checkedTerminal];
    if (ordinal < 0) return false;
    return bit(derivableTerminalBits, checkedSymbol, terminalWords, ordinal);
  }

  public int[] derivableTerminals(int symbol) {
    int checked = checkSymbol(symbol);
    return symbolsFromBits(
        derivableTerminalBits,
        checked,
        terminalWords,
        terminalSymbolByOrdinal);
  }

  public boolean productionDerivesTerminal(int production, int terminalSymbol) {
    int checkedProduction = checkProduction(production);
    int checkedTerminal = checkSymbol(terminalSymbol);
    int ordinal = terminalOrdinalBySymbol[checkedTerminal];
    if (ordinal < 0) return false;
    return bit(
        productionTerminalBits, checkedProduction, terminalWords, ordinal);
  }

  public int[] productionDerivableTerminals(int production) {
    return symbolsFromBits(
        productionTerminalBits,
        checkProduction(production),
        terminalWords,
        terminalSymbolByOrdinal);
  }

  public boolean derivesOperator(int symbol, int operatorIndex) {
    int checked = checkSymbol(symbol);
    int operatorCount = spec.lang().expressionSpec().operatorCount();
    Objects.checkIndex(operatorIndex, operatorCount);
    return bit(derivableOperatorBits, checked, operatorWords, operatorIndex);
  }

  public int[] derivableOperators(int symbol) {
    int checked = checkSymbol(symbol);
    return ordinalsFromBits(
        derivableOperatorBits,
        checked,
        operatorWords,
        spec.lang().expressionSpec().operatorCount());
  }

  public boolean productionDerivesOperator(int production, int operatorIndex) {
    int checked = checkProduction(production);
    int operatorCount = spec.lang().expressionSpec().operatorCount();
    Objects.checkIndex(operatorIndex, operatorCount);
    return bit(productionOperatorBits, checked, operatorWords, operatorIndex);
  }

  public int[] productionDerivableOperators(int production) {
    return ordinalsFromBits(
        productionOperatorBits,
        checkProduction(production),
        operatorWords,
        spec.lang().expressionSpec().operatorCount());
  }

  /** Non-terminal syntax-kind codes are production IDs. */
  public int syntaxKindCodeForProduction(int production) {
    return checkProduction(production);
  }

  /** Terminal syntax-kind codes set the sign bit and retain the grammar symbol in the low 31 bits. */
  public int syntaxKindCodeForTerminal(int terminalSymbol) {
    int checked = checkSymbol(terminalSymbol);
    if (spec.symbolKind(checked) == MIndexGrammarSymbolKind.NON_TERMINAL) {
      throw new IllegalArgumentException("symbol is not terminal");
    }
    return TERMINAL_MASK | checked;
  }

  public boolean terminalSyntaxKindCode(int kindCode) {
    return kindCode < 0;
  }

  public int terminalSymbolFromSyntaxKindCode(int kindCode) {
    if (!terminalSyntaxKindCode(kindCode)) {
      throw new IllegalArgumentException("syntax kind is not terminal");
    }
    return kindCode & VALUE_MASK;
  }

  public int productionFromSyntaxKindCode(int kindCode) {
    if (terminalSyntaxKindCode(kindCode)) {
      throw new IllegalArgumentException("syntax kind is not production-backed");
    }
    return checkProduction(kindCode);
  }

  public long primitivePayloadBytes() {
    return rhsRoles.length
        + rhsFlags.length
        + Integer.BYTES
            * (long)
                (rhsStarts.length
                    + rhsSymbols.length
                    + rhsOperatorIndexes.length
                    + minimumTokensBefore.length
                    + minimumTokensAfter.length
                    + terminalCounts.length
                    + nonTerminalCounts.length
                    + operatorCounts.length
                    + operatorOffsets.length
                    + operatorOrdinals.length
                    + lhsOffsets.length
                    + productionsByLhs.length
                    + predictionUseOffsets.length
                    + productionsPredictingSymbol.length
                    + terminalOrdinalBySymbol.length
                    + terminalSymbolByOrdinal.length)
        + Long.BYTES
            * (long)
                (shapeHashes.length
                    + planHashes.length
                    + derivableTerminalBits.length
                    + productionTerminalBits.length
                    + derivableOperatorBits.length
                    + productionOperatorBits.length);
  }

  private boolean flag(int production, int ordinal, byte mask) {
    return (rhsFlags[absolute(production, ordinal)] & mask) != 0;
  }

  private int absolute(int production, int ordinal) {
    int checked = checkProduction(production);
    return rhsStarts[checked]
        + Objects.checkIndex(ordinal, productionRhsLength(checked));
  }

  private int checkProduction(int production) {
    return Objects.checkIndex(production, productionCount);
  }

  private int checkSymbol(int symbol) {
    return Objects.checkIndex(symbol, symbolCount);
  }

  private static MIndexASTChildRole role(MIndexASTSpecPrecompute spec, int symbol) {
    MIndexGrammarSymbolKind kind = spec.symbolKind(symbol);
    if (kind == MIndexGrammarSymbolKind.NON_TERMINAL) return MIndexASTChildRole.NON_TERMINAL;
    if (kind == MIndexGrammarSymbolKind.TOKEN_CLASS) return MIndexASTChildRole.TOKEN_CLASS;
    return spec.operatorIndexForSymbol(symbol) >= 0
        ? MIndexASTChildRole.OPERATOR_LITERAL
        : MIndexASTChildRole.LITERAL;
  }

  private static int[] offsets(int[] counts) {
    int[] offsets = new int[counts.length + 1];
    for (int index = 0; index < counts.length; index++) {
      offsets[index + 1] = Math.addExact(offsets[index], counts[index]);
    }
    return offsets;
  }

  private static void setBit(long[] matrix, int row, int words, int bit) {
    matrix[row * words + (bit >>> 6)] |= 1L << bit;
  }

  private static boolean bit(long[] matrix, int row, int words, int bit) {
    return (matrix[row * words + (bit >>> 6)] & (1L << bit)) != 0L;
  }

  private static void propagateDerivationClosure(
      MIndexASTSpecPrecompute spec,
      long[] terminalBits,
      int terminalWords,
      long[] operatorBits,
      int operatorWords) {
    int symbols = spec.symbolCount();
    int[] queue = new int[Math.max(1, symbols)];
    boolean[] queued = new boolean[symbols];
    int head = 0;
    int tail = 0;
    int queuedCount = 0;

    for (int symbol = 0; symbol < symbols; symbol++) {
      if (rowHasAnyBit(terminalBits, symbol, terminalWords)
          || rowHasAnyBit(operatorBits, symbol, operatorWords)) {
        queue[tail] = symbol;
        tail = (tail + 1) % queue.length;
        queued[symbol] = true;
        queuedCount++;
      }
    }

    while (queuedCount != 0) {
      int symbol = queue[head];
      head = (head + 1) % queue.length;
      queuedCount--;
      queued[symbol] = false;

      for (int production : spec.productionsUsingSymbol(symbol)) {
        int lhs = spec.productionLhs(production);
        boolean changed =
            orRow(terminalBits, lhs, symbol, terminalWords)
                | orRow(operatorBits, lhs, symbol, operatorWords);
        if (changed && !queued[lhs]) {
          queue[tail] = lhs;
          tail = (tail + 1) % queue.length;
          queued[lhs] = true;
          queuedCount++;
        }
      }
    }
  }

  private static boolean rowHasAnyBit(long[] matrix, int row, int words) {
    int base = row * words;
    for (int word = 0; word < words; word++) {
      if (matrix[base + word] != 0L) return true;
    }
    return false;
  }

  private static boolean orRow(long[] matrix, int targetRow, int sourceRow, int words) {
    boolean changed = false;
    int target = targetRow * words;
    int source = sourceRow * words;
    for (int word = 0; word < words; word++) {
      long before = matrix[target + word];
      long after = before | matrix[source + word];
      if (before != after) {
        matrix[target + word] = after;
        changed = true;
      }
    }
    return changed;
  }

  private static void orRows(
      long[] target,
      int targetRow,
      long[] source,
      int sourceRow,
      int words) {
    int targetBase = targetRow * words;
    int sourceBase = sourceRow * words;
    for (int word = 0; word < words; word++) {
      target[targetBase + word] |= source[sourceBase + word];
    }
  }

  private static int[] symbolsFromBits(
      long[] matrix, int row, int words, int[] symbolByOrdinal) {
    int count = 0;
    int base = row * words;
    for (int word = 0; word < words; word++) count += Long.bitCount(matrix[base + word]);
    int[] result = new int[count];
    int out = 0;
    for (int ordinal = 0; ordinal < symbolByOrdinal.length; ordinal++) {
      if ((matrix[base + (ordinal >>> 6)] & (1L << ordinal)) != 0L) {
        result[out++] = symbolByOrdinal[ordinal];
      }
    }
    return result;
  }

  private static int[] ordinalsFromBits(
      long[] matrix, int row, int words, int count) {
    int bitCount = 0;
    int base = row * words;
    for (int word = 0; word < words; word++) bitCount += Long.bitCount(matrix[base + word]);
    int[] result = new int[bitCount];
    int out = 0;
    for (int ordinal = 0; ordinal < count; ordinal++) {
      if ((matrix[base + (ordinal >>> 6)] & (1L << ordinal)) != 0L) {
        result[out++] = ordinal;
      }
    }
    return result;
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
