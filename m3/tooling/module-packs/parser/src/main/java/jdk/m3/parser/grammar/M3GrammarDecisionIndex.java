// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarDecisionIndex.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;
import java.util.Objects;

/**
 * Frozen predictive dispatch index derived from one immutable grammar.
 *
 * <p>The index preserves Earley semantics but precomputes the viable production subset for each
 * (non-terminal, lookahead-terminal) pair and EOF. Token text is dispatched through token-class
 * postings plus exact/folded literal-hash postings, with exact terminal matching as the proof
 * boundary.</p>
 */
public final class M3GrammarDecisionIndex {
  public static final int NO_CANDIDATE = -1;
  public static final int MULTIPLE_CANDIDATES = -2;
  private final M3Grammar grammar;
  private final long grammarFingerprint;

  private final int[] tokenClassOffsets;
  private final int[] terminalsByTokenClass;
  private final int[] exactLiteralHashes;
  private final int[] exactLiteralSymbols;
  private final int[] foldedLiteralHashes;
  private final int[] foldedLiteralSymbols;

  private final long[] decisionKeys;
  private final int[] decisionOffsets;
  private final int[] decisionProductions;

  private final int[] eofOffsets;
  private final int[] eofProductions;

  private final int deterministicDecisionCount;
  private final int ambiguousDecisionCount;
  private final int maxCandidatesPerDecision;
  private final long baselineProductionScans;
  private final long retainedProductionScans;

  private M3GrammarDecisionIndex(
      M3Grammar grammar,
      int[] tokenClassOffsets,
      int[] terminalsByTokenClass,
      int[] exactLiteralHashes,
      int[] exactLiteralSymbols,
      int[] foldedLiteralHashes,
      int[] foldedLiteralSymbols,
      long[] decisionKeys,
      int[] decisionOffsets,
      int[] decisionProductions,
      int[] eofOffsets,
      int[] eofProductions,
      int deterministicDecisionCount,
      int ambiguousDecisionCount,
      int maxCandidatesPerDecision,
      long baselineProductionScans,
      long retainedProductionScans) {
    this.grammar = grammar;
    this.grammarFingerprint = grammar.fingerprint();
    this.tokenClassOffsets = tokenClassOffsets;
    this.terminalsByTokenClass = terminalsByTokenClass;
    this.exactLiteralHashes = exactLiteralHashes;
    this.exactLiteralSymbols = exactLiteralSymbols;
    this.foldedLiteralHashes = foldedLiteralHashes;
    this.foldedLiteralSymbols = foldedLiteralSymbols;
    this.decisionKeys = decisionKeys;
    this.decisionOffsets = decisionOffsets;
    this.decisionProductions = decisionProductions;
    this.eofOffsets = eofOffsets;
    this.eofProductions = eofProductions;
    this.deterministicDecisionCount = deterministicDecisionCount;
    this.ambiguousDecisionCount = ambiguousDecisionCount;
    this.maxCandidatesPerDecision = maxCandidatesPerDecision;
    this.baselineProductionScans = baselineProductionScans;
    this.retainedProductionScans = retainedProductionScans;
  }

  public static M3GrammarDecisionIndex build(M3Grammar grammar) {
    M3Grammar checked = Objects.requireNonNull(grammar, "grammar");
    int symbolCount = checked.symbolCount();
    int productionCount = checked.productionCount();

    int[] classCounts = new int[M3TokenClass.values().length];
    int exactCount = 0;
    int foldedCount = 0;
    for (int symbol = 0; symbol < symbolCount; symbol++) {
      switch (checked.symbolKind(symbol)) {
        case TOKEN_CLASS -> classCounts[checked.tokenClass(symbol).orElseThrow().ordinal()]++;
        case LITERAL -> {
          if (caseInsensitive(checked, symbol)) foldedCount++;
          else exactCount++;
        }
        case NON_TERMINAL -> {
          // no terminal dispatch lane
        }
      }
    }

    int[] tokenClassOffsets = offsets(classCounts);
    int[] terminalsByTokenClass = new int[tokenClassOffsets[classCounts.length]];
    int[] classCursor = Arrays.copyOf(tokenClassOffsets, classCounts.length);
    int[] exactHashes = new int[exactCount];
    int[] exactSymbols = new int[exactCount];
    int[] foldedHashes = new int[foldedCount];
    int[] foldedSymbols = new int[foldedCount];
    int exactWrite = 0;
    int foldedWrite = 0;

    for (int symbol = 0; symbol < symbolCount; symbol++) {
      switch (checked.symbolKind(symbol)) {
        case TOKEN_CLASS -> {
          int tokenClass = checked.tokenClass(symbol).orElseThrow().ordinal();
          terminalsByTokenClass[classCursor[tokenClass]++] = symbol;
        }
        case LITERAL -> {
          CharSequence literal = checked.symbolName(symbol);
          if (caseInsensitive(checked, symbol)) {
            foldedHashes[foldedWrite] = M3TokenStream.foldedHashCode(literal);
            foldedSymbols[foldedWrite++] = symbol;
          } else {
            exactHashes[exactWrite] = literal.toString().hashCode();
            exactSymbols[exactWrite++] = symbol;
          }
        }
        case NON_TERMINAL -> {
          // no terminal dispatch lane
        }
      }
    }
    sortPairs(exactHashes, exactSymbols);
    sortPairs(foldedHashes, foldedSymbols);

    int[] marks = new int[symbolCount];
    int[] predictionBuffer = new int[symbolCount];
    int totalPredictions = 0;
    for (int production = 0; production < productionCount; production++) {
      int count =
          collectPredictionSymbols(
              checked, production, marks, production + 1, predictionBuffer);
      totalPredictions = Math.addExact(totalPredictions, count);
    }

    long[] rawDecisionKeys = new long[totalPredictions];
    int[] rawDecisionProductions = new int[totalPredictions];
    int rawWrite = 0;
    Arrays.fill(marks, 0);
    for (int production = 0; production < productionCount; production++) {
      int count =
          collectPredictionSymbols(
              checked, production, marks, production + 1, predictionBuffer);
      int lhs = checked.productionLhs(production);
      for (int index = 0; index < count; index++) {
        int terminal = predictionBuffer[index];
        rawDecisionKeys[rawWrite] = decisionKey(lhs, terminal);
        rawDecisionProductions[rawWrite++] = production;
      }
    }
    sortPairs(rawDecisionKeys, rawDecisionProductions);

    int uniqueDecisions = 0;
    for (int index = 0; index < rawDecisionKeys.length; ) {
      uniqueDecisions++;
      long key = rawDecisionKeys[index];
      do index++;
      while (index < rawDecisionKeys.length && rawDecisionKeys[index] == key);
    }

    long[] decisionKeys = new long[uniqueDecisions];
    int[] decisionOffsets = new int[uniqueDecisions + 1];
    int[] decisionProductions = rawDecisionProductions;
    int decision = 0;
    int deterministic = 0;
    int ambiguous = 0;
    int maximum = 0;
    long baselineScans = 0L;
    long retainedScans = 0L;
    for (int index = 0; index < rawDecisionKeys.length; ) {
      int start = index;
      long key = rawDecisionKeys[index];
      do index++;
      while (index < rawDecisionKeys.length && rawDecisionKeys[index] == key);
      decisionKeys[decision] = key;
      decisionOffsets[decision] = start;
      int count = index - start;
      int lhs = (int) (key >>> 32);
      baselineScans =
          Math.addExact(baselineScans, checked.productionCountFor(lhs));
      retainedScans = Math.addExact(retainedScans, count);
      if (count == 1) deterministic++;
      else if (count > 1) ambiguous++;
      maximum = Math.max(maximum, count);
      decision++;
    }
    decisionOffsets[uniqueDecisions] = rawDecisionKeys.length;

    int[] eofCounts = new int[symbolCount];
    for (int production = 0; production < productionCount; production++) {
      if (canPredictAtEof(checked, production)) {
        eofCounts[checked.productionLhs(production)]++;
      }
    }
    int[] eofOffsets = offsets(eofCounts);
    int[] eofProductions = new int[eofOffsets[symbolCount]];
    int[] eofCursor = Arrays.copyOf(eofOffsets, symbolCount);
    for (int production = 0; production < productionCount; production++) {
      if (canPredictAtEof(checked, production)) {
        int lhs = checked.productionLhs(production);
        eofProductions[eofCursor[lhs]++] = production;
      }
    }

    return new M3GrammarDecisionIndex(
        checked,
        tokenClassOffsets,
        terminalsByTokenClass,
        exactHashes,
        exactSymbols,
        foldedHashes,
        foldedSymbols,
        decisionKeys,
        decisionOffsets,
        decisionProductions,
        eofOffsets,
        eofProductions,
        deterministic,
        ambiguous,
        maximum,
        baselineScans,
        retainedScans);
  }

  public M3Grammar grammar() {
    return grammar;
  }

  public long grammarFingerprint() {
    return grammarFingerprint;
  }

  public int decisionCount() {
    return decisionKeys.length;
  }

  public int decisionPostingCount() {
    return decisionProductions.length;
  }

  public int deterministicDecisionCount() {
    return deterministicDecisionCount;
  }

  public int ambiguousDecisionCount() {
    return ambiguousDecisionCount;
  }

  public int maxCandidatesPerDecision() {
    return maxCandidatesPerDecision;
  }

  /** Static lookahead cost if every decision scanned all productions owned by its LHS. */
  public long baselineProductionScans() {
    return baselineProductionScans;
  }

  /** Static lookahead cost retained by the sparse viable-production postings. */
  public long retainedProductionScans() {
    return retainedProductionScans;
  }

  public long prunedProductionScans() {
    return baselineProductionScans - retainedProductionScans;
  }

  public double retainedScanRatio() {
    return baselineProductionScans == 0L
        ? 1.0d
        : (double) retainedProductionScans / (double) baselineProductionScans;
  }

  public int eofPostingCount() {
    return eofProductions.length;
  }

  public int candidateProductionCount(int nonTerminal, int terminalSymbol) {
    int row = decisionRow(nonTerminal, terminalSymbol);
    return row < 0 ? 0 : decisionOffsets[row + 1] - decisionOffsets[row];
  }

  public int candidateProduction(int nonTerminal, int terminalSymbol, int ordinal) {
    int row = decisionRow(nonTerminal, terminalSymbol);
    if (row < 0) throw new IndexOutOfBoundsException("decision has no candidates");
    int from = decisionOffsets[row];
    int length = decisionOffsets[row + 1] - from;
    return decisionProductions[from + Objects.checkIndex(ordinal, length)];
  }

  public int[] candidateProductions(int nonTerminal, int terminalSymbol) {
    int row = decisionRow(nonTerminal, terminalSymbol);
    if (row < 0) return new int[0];
    return Arrays.copyOfRange(
        decisionProductions, decisionOffsets[row], decisionOffsets[row + 1]);
  }

  public int eofCandidateCount(int nonTerminal) {
    int checked = checkedNonTerminal(nonTerminal);
    return eofOffsets[checked + 1] - eofOffsets[checked];
  }

  public int eofCandidateProduction(int nonTerminal, int ordinal) {
    int checked = checkedNonTerminal(nonTerminal);
    int from = eofOffsets[checked];
    int length = eofOffsets[checked + 1] - from;
    return eofProductions[from + Objects.checkIndex(ordinal, length)];
  }

  public int[] eofCandidateProductions(int nonTerminal) {
    int checked = checkedNonTerminal(nonTerminal);
    return Arrays.copyOfRange(eofProductions, eofOffsets[checked], eofOffsets[checked + 1]);
  }

  /**
   * Returns the single viable production for this token, or {@link #NO_CANDIDATE} / {@link
   * #MULTIPLE_CANDIDATES}.
   *
   * <p>This is used by the parse-tree path to preserve the historical production-order behavior
   * whenever lookahead is ambiguous: only a proven single-production decision bypasses the normal
   * Earley prediction loop.</p>
   */
  public int uniqueCandidateProduction(
      int nonTerminal, M3GrammarTokenDispatch dispatch, int tokenIndex) {
    M3GrammarTokenDispatch checkedDispatch = checkDispatch(dispatch);
    Objects.checkIndex(tokenIndex, checkedDispatch.tokenCount());
    int found = NO_CANDIDATE;
    for (int terminalOrdinal = 0;
        terminalOrdinal < checkedDispatch.terminalCount(tokenIndex);
        terminalOrdinal++) {
      int terminal = checkedDispatch.terminalSymbol(tokenIndex, terminalOrdinal);
      int row = decisionRow(nonTerminal, terminal);
      if (row < 0) continue;
      for (int index = decisionOffsets[row]; index < decisionOffsets[row + 1]; index++) {
        int production = decisionProductions[index];
        if (found == NO_CANDIDATE) found = production;
        else if (found != production) return MULTIPLE_CANDIDATES;
      }
    }
    return found;
  }

  public int uniqueEofCandidateProduction(int nonTerminal) {
    int checked = checkedNonTerminal(nonTerminal);
    int from = eofOffsets[checked];
    int count = eofOffsets[checked + 1] - from;
    return count == 0
        ? NO_CANDIDATE
        : count == 1 ? eofProductions[from] : MULTIPLE_CANDIDATES;
  }

  /** Precomputes exact grammar terminal matches for each lexical token once. */
  public M3GrammarTokenDispatch dispatch(M3TokenStream tokens) {
    return dispatch(tokens, new M3GrammarBudget(M3GrammarLimits.defaults()));
  }

  M3GrammarTokenDispatch dispatch(M3TokenStream tokens, M3GrammarBudget budget) {
    M3TokenStream checked = Objects.requireNonNull(tokens, "tokens");
    budget.source(checked.source().length());
    budget.tokens(checked.size());
    int[] offsets = new int[checked.size() + 1];
    IntArena arena = new IntArena(Math.max(16, checked.size()), budget);
    int[] seen = new int[grammar.symbolCount()];

    for (int token = 0; token < checked.size(); token++) {
      budget.work(1L + 2L * checked.length(token));
      int generation = token + 1;
      int start = arena.size();
      M3TokenClass tokenClass = checked.tokenClass(token);
      addTokenClassCandidates(tokenClass.ordinal(), checked, token, seen, generation, arena, budget);
      if (tokenClass != M3TokenClass.ANY) {
        addTokenClassCandidates(
            M3TokenClass.ANY.ordinal(), checked, token, seen, generation, arena, budget);
      }

      addLiteralCandidates(
          exactLiteralHashes,
          exactLiteralSymbols,
          checked.hashCodeAt(token),
          checked,
          token,
          seen,
          generation,
          arena,
          budget);
      addLiteralCandidates(
          foldedLiteralHashes,
          foldedLiteralSymbols,
          checked.foldedHashCodeAt(token),
          checked,
          token,
          seen,
          generation,
          arena,
          budget);

      arena.sortRange(start, arena.size());
      offsets[token + 1] = arena.size();
    }

    return new M3GrammarTokenDispatch(
        grammar, checked, offsets, arena.toArray());
  }

  public long primitivePayloadBytes() {
    return Integer.BYTES
            * ((long) tokenClassOffsets.length
                    + terminalsByTokenClass.length
                    + exactLiteralHashes.length
                    + exactLiteralSymbols.length
                    + foldedLiteralHashes.length
                    + foldedLiteralSymbols.length
                    + decisionOffsets.length
                    + decisionProductions.length
                    + eofOffsets.length
                    + eofProductions.length)
        + Long.BYTES * (long) decisionKeys.length;
  }

  private void addTokenClassCandidates(
      int tokenClass,
      M3TokenStream tokens,
      int tokenIndex,
      int[] seen,
      int generation,
      IntArena arena,
      M3GrammarBudget budget) {
    int from = tokenClassOffsets[tokenClass];
    int to = tokenClassOffsets[tokenClass + 1];
    for (int index = from; index < to; index++) {
      int symbol = terminalsByTokenClass[index];
      if (seen[symbol] != generation && budget.matches(grammar, symbol, tokens, tokenIndex)) {
        seen[symbol] = generation;
        arena.add(symbol);
      }
    }
  }

  private void addLiteralCandidates(
      int[] hashes,
      int[] symbols,
      int hash,
      M3TokenStream tokens,
      int tokenIndex,
      int[] seen,
      int generation,
      IntArena arena,
      M3GrammarBudget budget) {
    int from = lowerBound(hashes, hash);
    int to = upperBound(hashes, hash);
    for (int index = from; index < to; index++) {
      int symbol = symbols[index];
      if (seen[symbol] != generation && budget.matches(grammar, symbol, tokens, tokenIndex)) {
        seen[symbol] = generation;
        arena.add(symbol);
      }
    }
  }

  private M3GrammarTokenDispatch checkDispatch(M3GrammarTokenDispatch dispatch) {
    M3GrammarTokenDispatch checked = Objects.requireNonNull(dispatch, "dispatch");
    if (checked.grammar() != grammar) {
      throw new IllegalArgumentException("token dispatch belongs to a different grammar");
    }
    return checked;
  }

  private int decisionRow(int nonTerminal, int terminalSymbol) {
    int checkedNonTerminal = checkedNonTerminal(nonTerminal);
    Objects.checkIndex(terminalSymbol, grammar.symbolCount());
    if (grammar.isNonTerminal(terminalSymbol)) return -1;
    long key = decisionKey(checkedNonTerminal, terminalSymbol);
    int low = 0;
    int high = decisionKeys.length - 1;
    while (low <= high) {
      int middle = (low + high) >>> 1;
      long candidate = decisionKeys[middle];
      if (candidate < key) low = middle + 1;
      else if (candidate > key) high = middle - 1;
      else return middle;
    }
    return -1;
  }

  private int checkedNonTerminal(int symbol) {
    Objects.checkIndex(symbol, grammar.symbolCount());
    if (!grammar.isNonTerminal(symbol)) {
      throw new IllegalArgumentException("symbol is not non-terminal: " + symbol);
    }
    return symbol;
  }

  private static boolean caseInsensitive(M3Grammar grammar, int literalSymbol) {
    return switch (grammar.literalCase(literalSymbol)) {
      case PROFILE -> grammar.lexerProfile().caseInsensitiveLiterals();
      case SENSITIVE -> false;
      case INSENSITIVE -> true;
    };
  }

  private static int collectPredictionSymbols(
      M3Grammar grammar,
      int production,
      int[] marks,
      int generation,
      int[] output) {
    int count = 0;
    boolean allNullable = true;
    int length = grammar.productionRhsLength(production);
    for (int ordinal = 0; ordinal < length; ordinal++) {
      int symbol = grammar.productionRhsSymbol(production, ordinal);
      for (int terminal : grammar.firstSymbols(symbol)) {
        if (marks[terminal] != generation) {
          marks[terminal] = generation;
          output[count++] = terminal;
        }
      }
      if (!grammar.nullable(symbol)) {
        allNullable = false;
        break;
      }
    }

    if (length == 0) allNullable = true;
    if (allNullable) {
      int lhs = grammar.productionLhs(production);
      for (int terminal : grammar.followSymbols(lhs)) {
        if (marks[terminal] != generation) {
          marks[terminal] = generation;
          output[count++] = terminal;
        }
      }
    }
    Arrays.sort(output, 0, count);
    return count;
  }

  private static boolean canPredictAtEof(M3Grammar grammar, int production) {
    int length = grammar.productionRhsLength(production);
    for (int ordinal = 0; ordinal < length; ordinal++) {
      if (!grammar.nullable(grammar.productionRhsSymbol(production, ordinal))) return false;
    }
    return grammar.canEndAfter(grammar.productionLhs(production));
  }

  private static long decisionKey(int nonTerminal, int terminalSymbol) {
    return ((long) nonTerminal << 32) | Integer.toUnsignedLong(terminalSymbol);
  }

  private static int[] offsets(int[] counts) {
    int[] offsets = new int[counts.length + 1];
    for (int index = 0; index < counts.length; index++) {
      offsets[index + 1] = Math.addExact(offsets[index], counts[index]);
    }
    return offsets;
  }

  private static int lowerBound(int[] values, int target) {
    int low = 0;
    int high = values.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (values[middle] < target) low = middle + 1;
      else high = middle;
    }
    return low;
  }

  private static int upperBound(int[] values, int target) {
    int low = 0;
    int high = values.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (values[middle] <= target) low = middle + 1;
      else high = middle;
    }
    return low;
  }

  private static void sortPairs(int[] keys, int[] values) {
    if (keys.length < 2) return;
    int[] tempKeys = new int[keys.length];
    int[] tempValues = new int[values.length];
    int width = 1;
    int[] sourceKeys = keys;
    int[] sourceValues = values;
    int[] targetKeys = tempKeys;
    int[] targetValues = tempValues;

    while (width < keys.length) {
      int block = width > keys.length / 2 ? keys.length : width << 1;
      for (int start = 0; start < keys.length; start += block) {
        int middle = Math.min(keys.length, start + width);
        int end = Math.min(keys.length, start + block);
        int left = start;
        int right = middle;
        int out = start;
        while (left < middle || right < end) {
          if (right >= end
              || left < middle
                  && compare(
                          sourceKeys[left],
                          sourceValues[left],
                          sourceKeys[right],
                          sourceValues[right])
                      <= 0) {
            targetKeys[out] = sourceKeys[left];
            targetValues[out++] = sourceValues[left++];
          } else {
            targetKeys[out] = sourceKeys[right];
            targetValues[out++] = sourceValues[right++];
          }
        }
      }
      int[] keySwap = sourceKeys;
      sourceKeys = targetKeys;
      targetKeys = keySwap;
      int[] valueSwap = sourceValues;
      sourceValues = targetValues;
      targetValues = valueSwap;
      width = block;
    }

    if (sourceKeys != keys) {
      System.arraycopy(sourceKeys, 0, keys, 0, keys.length);
      System.arraycopy(sourceValues, 0, values, 0, values.length);
    }
  }

  private static void sortPairs(long[] keys, int[] values) {
    if (keys.length < 2) return;
    long[] tempKeys = new long[keys.length];
    int[] tempValues = new int[values.length];
    int width = 1;
    long[] sourceKeys = keys;
    int[] sourceValues = values;
    long[] targetKeys = tempKeys;
    int[] targetValues = tempValues;

    while (width < keys.length) {
      int block = width > keys.length / 2 ? keys.length : width << 1;
      for (int start = 0; start < keys.length; start += block) {
        int middle = Math.min(keys.length, start + width);
        int end = Math.min(keys.length, start + block);
        int left = start;
        int right = middle;
        int out = start;
        while (left < middle || right < end) {
          if (right >= end
              || left < middle
                  && compare(
                          sourceKeys[left],
                          sourceValues[left],
                          sourceKeys[right],
                          sourceValues[right])
                      <= 0) {
            targetKeys[out] = sourceKeys[left];
            targetValues[out++] = sourceValues[left++];
          } else {
            targetKeys[out] = sourceKeys[right];
            targetValues[out++] = sourceValues[right++];
          }
        }
      }
      long[] keySwap = sourceKeys;
      sourceKeys = targetKeys;
      targetKeys = keySwap;
      int[] valueSwap = sourceValues;
      sourceValues = targetValues;
      targetValues = valueSwap;
      width = block;
    }

    if (sourceKeys != keys) {
      System.arraycopy(sourceKeys, 0, keys, 0, keys.length);
      System.arraycopy(sourceValues, 0, values, 0, values.length);
    }
  }

  private static int compare(int leftKey, int leftValue, int rightKey, int rightValue) {
    int key = Integer.compare(leftKey, rightKey);
    return key != 0 ? key : Integer.compare(leftValue, rightValue);
  }

  private static int compare(long leftKey, int leftValue, long rightKey, int rightValue) {
    int key = Long.compare(leftKey, rightKey);
    return key != 0 ? key : Integer.compare(leftValue, rightValue);
  }

  private static final class IntArena {
    private int[] values;
    private int size;
    private final M3GrammarBudget budget;

    IntArena(int initialCapacity, M3GrammarBudget budget) {
      this.budget = budget;
      values = new int[Math.max(1, initialCapacity)];
    }

    int size() {
      return size;
    }

    void add(int value) {
      budget.work(1);
      if (size == values.length) {
        values = Arrays.copyOf(values, M3GrammarBudget.capacity(values.length, size + 1, Integer.MAX_VALUE - 8));
      }
      values[size++] = value;
    }

    void sortRange(int from, int to) {
      Arrays.sort(values, from, to);
    }

    int[] toArray() {
      return Arrays.copyOf(values, size);
    }
  }
}
