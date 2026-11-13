// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexEarleyWaitingIndex.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;

/**
 * Sparse primitive adjacency index from expected non-terminal symbol to chart-state indices.
 *
 * <p>Rows are created only for symbols actually awaited in one Earley chart. State postings retain
 * insertion order so completer traversal remains deterministic.</p>
 */
final class M3EarleyWaitingIndex {
  private static final int UNREGISTERED = -1;
  private static final int END = -2;

  private int[] nextByState = filled(16, UNREGISTERED);

  private int[] rowSymbols = new int[8];
  private int[] rowHeads = filled(8, -1);
  private int[] rowTails = filled(8, -1);
  private int[] rowCounts = new int[8];
  private int rowCount;

  private int[] slots = new int[16];
  private final M3GrammarBudget budget;

  M3EarleyWaitingIndex(M3GrammarBudget budget) {
    this.budget = budget;
  }

  void register(int stateIndex, int nonTerminalSymbol) {
    if (stateIndex < 0 || nonTerminalSymbol < 0) {
      throw new IllegalArgumentException("negative Earley waiting coordinate");
    }
    ensureStateCapacity(stateIndex + 1);
    if (nextByState[stateIndex] != UNREGISTERED) {
      throw new IllegalStateException("Earley state already registered in waiting index");
    }

    int row = row(nonTerminalSymbol, true);
    int tail = rowTails[row];
    if (tail < 0) {
      rowHeads[row] = stateIndex;
    } else {
      nextByState[tail] = stateIndex;
    }
    nextByState[stateIndex] = END;
    rowTails[row] = stateIndex;
    rowCounts[row]++;
  }

  int first(int nonTerminalSymbol) {
    int row = row(nonTerminalSymbol, false);
    return row < 0 ? -1 : rowHeads[row];
  }

  int next(int stateIndex) {
    if (stateIndex < 0 || stateIndex >= nextByState.length) return -1;
    int next = nextByState[stateIndex];
    return next >= 0 ? next : -1;
  }

  int count(int nonTerminalSymbol) {
    int row = row(nonTerminalSymbol, false);
    return row < 0 ? 0 : rowCounts[row];
  }

  int distinctSymbolCount() {
    return rowCount;
  }

  long primitivePayloadBytes() {
    return Integer.BYTES
        * ((long) nextByState.length
                + rowSymbols.length
                + rowHeads.length
                + rowTails.length
                + rowCounts.length
                + slots.length);
  }

  private int row(int symbol, boolean create) {
    if (create && (rowCount + 1L) * 10 >= slots.length * 6L) {
      rehash(slots.length << 1);
    }

    int mask = slots.length - 1;
    int slot = mix(symbol) & mask;
    while (true) {
      budget.work(1);
      int encoded = slots[slot];
      if (encoded == 0) {
        if (!create) return -1;
        int row = appendRow(symbol);
        slots[slot] = row + 1;
        return row;
      }
      int row = encoded - 1;
      if (rowSymbols[row] == symbol) return row;
      slot = (slot + 1) & mask;
    }
  }

  private int appendRow(int symbol) {
    if (rowCount == rowSymbols.length) {
      int old = rowSymbols.length;
      int next = M3GrammarBudget.capacity(old, rowCount + 1, budget.limits().maxStates());
      rowSymbols = Arrays.copyOf(rowSymbols, next);
      rowHeads = growFilled(rowHeads, next, -1);
      rowTails = growFilled(rowTails, next, -1);
      rowCounts = Arrays.copyOf(rowCounts, next);
    }
    rowSymbols[rowCount] = symbol;
    return rowCount++;
  }

  private void ensureStateCapacity(int required) {
    if (required <= nextByState.length) return;
    int old = nextByState.length;
    int next = M3GrammarBudget.capacity(old, required, budget.limits().maxStates());
    nextByState = growFilled(nextByState, next, UNREGISTERED);
  }

  private void rehash(int capacity) {
    int[] replacement = new int[capacity];
    int mask = capacity - 1;
    for (int row = 0; row < rowCount; row++) {
      budget.work(1);
      int slot = mix(rowSymbols[row]) & mask;
      while (replacement[slot] != 0) { budget.work(1); slot = (slot + 1) & mask; }
      replacement[slot] = row + 1;
    }
    slots = replacement;
  }

  private static int mix(int value) {
    int x = value;
    x ^= x >>> 16;
    x *= 0x7feb352d;
    x ^= x >>> 15;
    x *= 0x846ca68b;
    return x ^ (x >>> 16);
  }

  private static int[] filled(int size, int value) {
    int[] result = new int[size];
    Arrays.fill(result, value);
    return result;
  }

  private static int[] growFilled(int[] source, int size, int value) {
    int old = source.length;
    int[] result = Arrays.copyOf(source, size);
    Arrays.fill(result, old, size, value);
    return result;
  }
}
