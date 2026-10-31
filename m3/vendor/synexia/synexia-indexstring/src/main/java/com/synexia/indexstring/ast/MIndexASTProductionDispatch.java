// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.grammar.MIndexGrammarSymbolKind;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable predictive dispatch index from (non-terminal, lookahead terminal) to grammar productions.
 *
 * <p>The index is derived exclusively from the frozen production prediction sets. It preserves every
 * candidate for ambiguous grammars and keeps EOF candidates in a separate CSR. Hot lookup uses one
 * fixed open-addressed directory over deterministic sorted cells.</p>
 */
public final class MIndexASTProductionDispatch {
  private static final int MIN_HASH_CAPACITY = 2;

  private final int symbolCount;
  private final int productionCount;
  private final long[] keys;
  private final int[] offsets;
  private final int[] candidates;
  private final long[] slotKeys;
  private final int[] slotEntries;
  private final int[] entryOffsetsByNonTerminal;
  private final int[] eofOffsets;
  private final int[] eofCandidates;
  private final int uniqueEntryCount;
  private final int ambiguousEntryCount;
  private final long fingerprint;

  private MIndexASTProductionDispatch(
      int symbolCount,
      int productionCount,
      long[] keys,
      int[] offsets,
      int[] candidates,
      long[] slotKeys,
      int[] slotEntries,
      int[] entryOffsetsByNonTerminal,
      int[] eofOffsets,
      int[] eofCandidates,
      int uniqueEntryCount,
      int ambiguousEntryCount,
      long fingerprint) {
    this.symbolCount = symbolCount;
    this.productionCount = productionCount;
    this.keys = keys;
    this.offsets = offsets;
    this.candidates = candidates;
    this.slotKeys = slotKeys;
    this.slotEntries = slotEntries;
    this.entryOffsetsByNonTerminal = entryOffsetsByNonTerminal;
    this.eofOffsets = eofOffsets;
    this.eofCandidates = eofCandidates;
    this.uniqueEntryCount = uniqueEntryCount;
    this.ambiguousEntryCount = ambiguousEntryCount;
    this.fingerprint = fingerprint;
  }

  static MIndexASTProductionDispatch build(MIndexASTSpecPrecompute spec) {
    Objects.requireNonNull(spec, "spec");
    return build(
        new Source() {
          @Override public int symbolCount() { return spec.symbolCount(); }
          @Override public int productionCount() { return spec.productionCount(); }
          @Override public MIndexGrammarSymbolKind symbolKind(int symbol) { return spec.symbolKind(symbol); }
          @Override public int productionLhs(int production) { return spec.productionLhs(production); }
          @Override public int[] predictionSymbols(int production) { return spec.predictionSymbols(production); }
          @Override public boolean productionCanEndAtEof(int production) { return spec.productionCanEndAtEof(production); }
        });
  }

  static MIndexASTProductionDispatch build(MIndexASTSpecImageView image) {
    Objects.requireNonNull(image, "image");
    return build(
        new Source() {
          @Override public int symbolCount() { return image.symbolCount(); }
          @Override public int productionCount() { return image.productionCount(); }
          @Override public MIndexGrammarSymbolKind symbolKind(int symbol) { return image.symbolKind(symbol); }
          @Override public int productionLhs(int production) { return image.productionLhs(production); }
          @Override public int[] predictionSymbols(int production) { return image.predictionSymbols(production); }
          @Override public boolean productionCanEndAtEof(int production) { return image.productionCanEndAtEof(production); }
        });
  }

  private static MIndexASTProductionDispatch build(Source source) {
    int symbols = source.symbolCount();
    int productions = source.productionCount();

    int pairCount = 0;
    int[] eofCounts = new int[symbols];
    for (int production = 0; production < productions; production++) {
      int lhs = source.productionLhs(production);
      requireNonTerminal(source, lhs);
      int[] predicted = source.predictionSymbols(production);
      pairCount = Math.addExact(pairCount, predicted.length);
      for (int terminal : predicted) requireTerminal(source, terminal);
      if (source.productionCanEndAtEof(production)) eofCounts[lhs]++;
    }

    long[] pairKeys = new long[pairCount];
    int[] pairProductions = new int[pairCount];
    int write = 0;
    for (int production = 0; production < productions; production++) {
      int lhs = source.productionLhs(production);
      int[] predicted = source.predictionSymbols(production);
      for (int terminal : predicted) {
        pairKeys[write] = pack(lhs, terminal);
        pairProductions[write] = production;
        write++;
      }
    }
    sortPairs(pairKeys, pairProductions);

    int entryCount = 0;
    long previousKey = Long.MIN_VALUE;
    int previousProduction = -1;
    for (int index = 0; index < pairCount; index++) {
      long key = pairKeys[index];
      int production = pairProductions[index];
      if (index != 0 && key == previousKey && production == previousProduction) {
        throw new IllegalStateException("duplicate production prediction pair");
      }
      if (index == 0 || key != previousKey) entryCount++;
      previousKey = key;
      previousProduction = production;
    }

    long[] keys = new long[entryCount];
    int[] offsets = new int[entryCount + 1];
    int[] candidates = new int[pairCount];

    int entry = -1;
    int candidateWrite = 0;
    previousKey = Long.MIN_VALUE;
    for (int index = 0; index < pairCount; index++) {
      long key = pairKeys[index];
      if (index == 0 || key != previousKey) {
        entry++;
        keys[entry] = key;
        offsets[entry] = candidateWrite;
        previousKey = key;
      }
      candidates[candidateWrite++] = pairProductions[index];
    }
    offsets[entryCount] = candidateWrite;

    int[] entryCountsByNonTerminal = new int[symbols];
    for (int index = 0; index < entryCount; index++) {
      entryCountsByNonTerminal[(int) (keys[index] >>> 32)]++;
    }
    int[] entryOffsetsByNonTerminal = prefixOffsets(entryCountsByNonTerminal);

    int unique = 0;
    int ambiguous = 0;
    for (int index = 0; index < entryCount; index++) {
      int count = offsets[index + 1] - offsets[index];
      if (count == 1) unique++;
      else if (count > 1) ambiguous++;
    }

    int hashCapacity = hashCapacityForEntries(entryCount);
    long[] slotKeys = new long[hashCapacity];
    int[] slotEntries = new int[hashCapacity];
    int mask = hashCapacity - 1;
    for (int index = 0; index < entryCount; index++) {
      long encoded = encodeKey(keys[index]);
      int slot = mixToInt(encoded) & mask;
      while (slotKeys[slot] != 0L) slot = (slot + 1) & mask;
      slotKeys[slot] = encoded;
      slotEntries[slot] = index + 1;
    }

    int[] eofOffsets = prefixOffsets(eofCounts);
    int eofTotal = eofOffsets[symbols];
    int[] eofCandidates = new int[eofTotal];
    int[] eofCursor = Arrays.copyOf(eofOffsets, symbols);
    for (int production = 0; production < productions; production++) {
      if (source.productionCanEndAtEof(production)) {
        int lhs = source.productionLhs(production);
        eofCandidates[eofCursor[lhs]++] = production;
      }
    }

    long fingerprint = avalanche(0x4d494e4453504154L ^ symbols);
    fingerprint = mix(fingerprint, productions);
    for (int index = 0; index < entryCount; index++) {
      fingerprint = mix(fingerprint, keys[index]);
      int from = offsets[index];
      int to = offsets[index + 1];
      fingerprint = mix(fingerprint, to - from);
      for (int candidate = from; candidate < to; candidate++) {
        fingerprint = mix(fingerprint, candidates[candidate]);
      }
    }
    for (int symbol = 0; symbol < symbols; symbol++) {
      int from = eofOffsets[symbol];
      int to = eofOffsets[symbol + 1];
      if (from == to) continue;
      fingerprint = mix(fingerprint, 0x454f460000000000L ^ Integer.toUnsignedLong(symbol));
      for (int index = from; index < to; index++) {
        fingerprint = mix(fingerprint, eofCandidates[index]);
      }
    }
    fingerprint = avalanche(fingerprint);

    return new MIndexASTProductionDispatch(
        symbols,
        productions,
        keys,
        offsets,
        candidates,
        slotKeys,
        slotEntries,
        entryOffsetsByNonTerminal,
        eofOffsets,
        eofCandidates,
        unique,
        ambiguous,
        fingerprint);
  }

  public long fingerprint() {
    return fingerprint;
  }

  public int entryCount() {
    return keys.length;
  }

  public int candidateCount() {
    return candidates.length;
  }

  public int uniqueEntryCount() {
    return uniqueEntryCount;
  }

  public int ambiguousEntryCount() {
    return ambiguousEntryCount;
  }

  public int eofCandidateCount() {
    return eofCandidates.length;
  }

  public int dispatchEntryCount(int nonTerminalSymbol) {
    int from = entryStart(nonTerminalSymbol);
    return entryEnd(nonTerminalSymbol) - from;
  }

  /** Sorted lookahead terminals having at least one production candidate for this non-terminal. */
  public int[] predictedTerminals(int nonTerminalSymbol) {
    int from = entryStart(nonTerminalSymbol);
    int to = entryEnd(nonTerminalSymbol);
    int[] result = new int[to - from];
    for (int entry = from; entry < to; entry++) {
      result[entry - from] = entryTerminal(entry);
    }
    return result;
  }

  public boolean mayPredict(int nonTerminalSymbol, int terminalSymbol) {
    return findEntry(nonTerminalSymbol, terminalSymbol) >= 0;
  }

  public int candidateCount(int nonTerminalSymbol, int terminalSymbol) {
    int entry = findEntry(nonTerminalSymbol, terminalSymbol);
    return entry < 0 ? 0 : offsets[entry + 1] - offsets[entry];
  }

  public int[] candidateProductions(int nonTerminalSymbol, int terminalSymbol) {
    int entry = findEntry(nonTerminalSymbol, terminalSymbol);
    if (entry < 0) return new int[0];
    return Arrays.copyOfRange(candidates, offsets[entry], offsets[entry + 1]);
  }

  /** Returns the only candidate production or -1 when absent/ambiguous. */
  public int uniqueProductionOrMinusOne(int nonTerminalSymbol, int terminalSymbol) {
    int entry = findEntry(nonTerminalSymbol, terminalSymbol);
    if (entry < 0 || offsets[entry + 1] - offsets[entry] != 1) return -1;
    return candidates[offsets[entry]];
  }

  public boolean ambiguous(int nonTerminalSymbol, int terminalSymbol) {
    int entry = findEntry(nonTerminalSymbol, terminalSymbol);
    return entry >= 0 && offsets[entry + 1] - offsets[entry] > 1;
  }

  public int[] eofCandidateProductions(int nonTerminalSymbol) {
    int checked = checkSymbol(nonTerminalSymbol);
    return Arrays.copyOfRange(
        eofCandidates, eofOffsets[checked], eofOffsets[checked + 1]);
  }

  public int uniqueEofProductionOrMinusOne(int nonTerminalSymbol) {
    int checked = checkSymbol(nonTerminalSymbol);
    int from = eofOffsets[checked];
    int to = eofOffsets[checked + 1];
    return to - from == 1 ? eofCandidates[from] : -1;
  }

  public boolean eofAmbiguous(int nonTerminalSymbol) {
    int checked = checkSymbol(nonTerminalSymbol);
    return eofOffsets[checked + 1] - eofOffsets[checked] > 1;
  }

  public void batchDispatch(
      int[] nonTerminals,
      int[] terminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    MIndexASTDispatchNative.batchHeap(
        this, nonTerminals, terminals, candidateCounts, uniqueProductions, flags);
  }

  public void batchEofDispatch(
      int[] nonTerminals,
      int[] candidateCounts,
      int[] uniqueProductions,
      byte[] flags) {
    MIndexASTDispatchNative.batchHeapEof(
        this, nonTerminals, candidateCounts, uniqueProductions, flags);
  }

  public long primitivePayloadBytes() {
    return Long.BYTES * (long) (keys.length + slotKeys.length)
        + Integer.BYTES
            * (long)
                (offsets.length
                    + candidates.length
                    + slotEntries.length
                    + entryOffsetsByNonTerminal.length
                    + eofOffsets.length
                    + eofCandidates.length);
  }

  long keyAt(int entry) {
    return keys[Objects.checkIndex(entry, keys.length)];
  }

  int entryNonTerminal(int entry) {
    return (int) (keyAt(entry) >>> 32);
  }

  int entryTerminal(int entry) {
    return (int) keyAt(entry);
  }

  int entryCandidateStart(int entry) {
    return offsets[Objects.checkIndex(entry, keys.length)];
  }

  int entryCandidateEnd(int entry) {
    int checked = Objects.checkIndex(entry, keys.length);
    return offsets[checked + 1];
  }

  int candidateAtAbsolute(int index) {
    return candidates[Objects.checkIndex(index, candidates.length)];
  }

  int hashSlotCount() {
    return slotKeys.length;
  }

  long hashSlotKey(int slot) {
    return slotKeys[Objects.checkIndex(slot, slotKeys.length)];
  }

  int hashSlotEntry(int slot) {
    return slotEntries[Objects.checkIndex(slot, slotEntries.length)];
  }

  int eofOffset(int symbol) {
    return eofOffsets[Objects.checkIndex(symbol, symbolCount + 1)];
  }

  int eofCandidateAtAbsolute(int index) {
    return eofCandidates[Objects.checkIndex(index, eofCandidates.length)];
  }

  int symbolCountForNative() {
    return symbolCount;
  }

  long[] slotKeysForNative() {
    return slotKeys;
  }

  int[] slotEntriesForNative() {
    return slotEntries;
  }

  int[] offsetsForNative() {
    return offsets;
  }

  int[] candidatesForNative() {
    return candidates;
  }

  int[] eofOffsetsForNative() {
    return eofOffsets;
  }

  int[] eofCandidatesForNative() {
    return eofCandidates;
  }

  int entryOffset(int symbol) {
    return entryOffsetsByNonTerminal[Objects.checkIndex(symbol, symbolCount + 1)];
  }

  private int entryStart(int nonTerminalSymbol) {
    int nonTerminal = checkSymbol(nonTerminalSymbol);
    return entryOffsetsByNonTerminal[nonTerminal];
  }

  private int entryEnd(int nonTerminalSymbol) {
    int nonTerminal = checkSymbol(nonTerminalSymbol);
    return entryOffsetsByNonTerminal[nonTerminal + 1];
  }

  private int findEntry(int nonTerminalSymbol, int terminalSymbol) {
    int nonTerminal = checkSymbol(nonTerminalSymbol);
    int terminal = checkSymbol(terminalSymbol);
    long encoded = encodeKey(pack(nonTerminal, terminal));
    int mask = slotKeys.length - 1;
    int slot = mixToInt(encoded) & mask;
    for (int probe = 0; probe < slotKeys.length; probe++) {
      long stored = slotKeys[slot];
      if (stored == 0L) return -1;
      if (stored == encoded) return slotEntries[slot] - 1;
      slot = (slot + 1) & mask;
    }
    return -1;
  }

  private int checkSymbol(int symbol) {
    return Objects.checkIndex(symbol, symbolCount);
  }

  private static void requireNonTerminal(Source source, int symbol) {
    if (symbol < 0
        || symbol >= source.symbolCount()
        || source.symbolKind(symbol) != MIndexGrammarSymbolKind.NON_TERMINAL) {
      throw new IllegalArgumentException("production LHS is not a non-terminal: " + symbol);
    }
  }

  private static void requireTerminal(Source source, int symbol) {
    if (symbol < 0
        || symbol >= source.symbolCount()
        || source.symbolKind(symbol) == MIndexGrammarSymbolKind.NON_TERMINAL) {
      throw new IllegalArgumentException("prediction symbol is not a terminal: " + symbol);
    }
  }

  private static long pack(int nonTerminal, int terminal) {
    return (Integer.toUnsignedLong(nonTerminal) << 32) | Integer.toUnsignedLong(terminal);
  }

  private static long encodeKey(long key) {
    return key + 1L;
  }

  static int hashCapacityForEntries(int entries) {
    int needed =
        Math.max(
            MIN_HASH_CAPACITY,
            Math.addExact(Math.addExact(entries, entries >>> 1), 1));
    int capacity = 1;
    while (capacity < needed) {
      if (capacity >= (1 << 30)) {
        throw new IllegalStateException("production dispatch hash table exceeds int capacity");
      }
      capacity <<= 1;
    }
    return capacity;
  }

  private static int[] prefixOffsets(int[] counts) {
    int[] offsets = new int[counts.length + 1];
    for (int index = 0; index < counts.length; index++) {
      offsets[index + 1] = Math.addExact(offsets[index], counts[index]);
    }
    return offsets;
  }

  private static int lowerBound(long[] values, long target) {
    int low = 0;
    int high = values.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (Long.compareUnsigned(values[middle], target) < 0) low = middle + 1;
      else high = middle;
    }
    return low;
  }

  private static void sortPairs(long[] keys, int[] values) {
    int length = keys.length;
    if (length < 2) return;

    long[] tempKeys = new long[length];
    int[] tempValues = new int[length];
    long[] sourceKeys = keys;
    int[] sourceValues = values;
    long[] targetKeys = tempKeys;
    int[] targetValues = tempValues;

    int width = 1;
    while (width < length) {
      int block = width > length / 2 ? length : width << 1;
      for (int start = 0; start < length; start += block) {
        int middle = Math.min(length, start + width);
        int end = Math.min(length, start + block);
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
      System.arraycopy(sourceKeys, 0, keys, 0, length);
      System.arraycopy(sourceValues, 0, values, 0, length);
    }
  }

  private static int compare(long leftKey, int leftProduction, long rightKey, int rightProduction) {
    int key = Long.compareUnsigned(leftKey, rightKey);
    return key != 0 ? key : Integer.compare(leftProduction, rightProduction);
  }

  private static int mixToInt(long value) {
    long z = value;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    z ^= z >>> 31;
    return (int) (z ^ (z >>> 32));
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

  private interface Source {
    int symbolCount();
    int productionCount();
    MIndexGrammarSymbolKind symbolKind(int symbol);
    int productionLhs(int production);
    int[] predictionSymbols(int production);
    boolean productionCanEndAtEof(int production);
  }
}
