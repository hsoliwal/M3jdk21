// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarAnalysis.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;

/**
 * One-time fixed-point analysis over a compiled context-free grammar.
 *
 * <p>Only compact analysis lanes survive freeze: nullable, productive, reachable, left-recursive,
 * minimum token length, FIRST/FOLLOW and EOF-follow state.</p>
 */
final class M3GrammarAnalysis {
  private static final int INFINITY = Integer.MAX_VALUE >>> 3;

  private M3GrammarAnalysis() {}

  static Result compute(byte[] kinds, int[] lhs, int[] rhsStart, int[] rhs, int startSymbol) {
    int symbols = kinds.length;
    int[] terminalOrdinal = new int[symbols];
    Arrays.fill(terminalOrdinal, -1);
    int terminalCount = 0;
    for (int symbol = 0; symbol < symbols; symbol++) {
      if (!nonTerminal(kinds[symbol])) terminalOrdinal[symbol] = terminalCount++;
    }
    int[] terminalSymbol = new int[terminalCount];
    for (int symbol = 0; symbol < symbols; symbol++) {
      if (terminalOrdinal[symbol] >= 0) terminalSymbol[terminalOrdinal[symbol]] = symbol;
    }
    int words = Math.max(1, (terminalCount + 63) >>> 6);

    byte[] nullable = nullable(kinds, lhs, rhsStart, rhs);
    ProductiveResult productiveResult = productiveAndMinimum(kinds, lhs, rhsStart, rhs);
    byte[] reachable = reachable(kinds, lhs, rhsStart, rhs, startSymbol);
    byte[] leftRecursive = leftRecursive(kinds, lhs, rhsStart, rhs, nullable);

    long[] first = new long[Math.multiplyExact(symbols, words)];
    for (int symbol = 0; symbol < symbols; symbol++) {
      int ordinal = terminalOrdinal[symbol];
      if (ordinal >= 0) set(first, symbol, words, ordinal);
    }

    boolean changed;
    do {
      changed = false;
      for (int production = 0; production < lhs.length; production++) {
        int left = lhs[production];
        for (int index = rhsStart[production]; index < rhsStart[production + 1]; index++) {
          int symbol = rhs[index];
          changed |= or(first, left, symbol, words);
          if (!nonTerminal(kinds[symbol]) || nullable[symbol] == 0) break;
        }
      }
    } while (changed);

    long[] follow = new long[Math.multiplyExact(symbols, words)];
    byte[] eof = new byte[symbols];
    eof[startSymbol] = 1;
    do {
      changed = false;
      for (int production = 0; production < lhs.length; production++) {
        int left = lhs[production];
        int from = rhsStart[production];
        int to = rhsStart[production + 1];
        for (int index = from; index < to; index++) {
          int current = rhs[index];
          if (!nonTerminal(kinds[current])) continue;

          boolean suffixNullable = true;
          for (int nextIndex = index + 1; nextIndex < to; nextIndex++) {
            int next = rhs[nextIndex];
            changed |= or(follow, current, first, next, words);
            if (!nonTerminal(kinds[next]) || nullable[next] == 0) {
              suffixNullable = false;
              break;
            }
          }

          if (suffixNullable) {
            changed |= or(follow, current, left, words);
            if (eof[left] != 0 && eof[current] == 0) {
              eof[current] = 1;
              changed = true;
            }
          }
        }
      }
    } while (changed);

    return new Result(
        nullable,
        productiveResult.productive(),
        reachable,
        leftRecursive,
        productiveResult.minimumTokens(),
        first,
        follow,
        eof,
        terminalOrdinal,
        terminalSymbol,
        words);
  }

  private static byte[] nullable(byte[] kinds, int[] lhs, int[] rhsStart, int[] rhs) {
    byte[] nullable = new byte[kinds.length];
    boolean changed;
    do {
      changed = false;
      for (int production = 0; production < lhs.length; production++) {
        int left = lhs[production];
        if (nullable[left] != 0) continue;

        boolean allNullable = true;
        for (int index = rhsStart[production]; index < rhsStart[production + 1]; index++) {
          int symbol = rhs[index];
          if (!nonTerminal(kinds[symbol]) || nullable[symbol] == 0) {
            allNullable = false;
            break;
          }
        }
        if (allNullable) {
          nullable[left] = 1;
          changed = true;
        }
      }
    } while (changed);
    return nullable;
  }

  private static ProductiveResult productiveAndMinimum(
      byte[] kinds, int[] lhs, int[] rhsStart, int[] rhs) {
    byte[] productive = new byte[kinds.length];
    int[] minimum = new int[kinds.length];
    Arrays.fill(minimum, INFINITY);
    for (int symbol = 0; symbol < kinds.length; symbol++) {
      if (!nonTerminal(kinds[symbol])) {
        productive[symbol] = 1;
        minimum[symbol] = 1;
      }
    }

    boolean changed;
    do {
      changed = false;
      for (int production = 0; production < lhs.length; production++) {
        int sum = 0;
        boolean allProductive = true;
        for (int index = rhsStart[production]; index < rhsStart[production + 1]; index++) {
          int symbol = rhs[index];
          if (productive[symbol] == 0 || minimum[symbol] == INFINITY) {
            allProductive = false;
            break;
          }
          sum = Math.min(INFINITY, sum + minimum[symbol]);
        }

        if (allProductive) {
          int left = lhs[production];
          if (productive[left] == 0) {
            productive[left] = 1;
            changed = true;
          }
          if (sum < minimum[left]) {
            minimum[left] = sum;
            changed = true;
          }
        }
      }
    } while (changed);

    for (int symbol = 0; symbol < minimum.length; symbol++) {
      if (minimum[symbol] == INFINITY) minimum[symbol] = -1;
    }
    return new ProductiveResult(productive, minimum);
  }

  private static byte[] reachable(
      byte[] kinds, int[] lhs, int[] rhsStart, int[] rhs, int startSymbol) {
    byte[] reachable = new byte[kinds.length];
    reachable[startSymbol] = 1;

    boolean changed;
    do {
      changed = false;
      for (int production = 0; production < lhs.length; production++) {
        if (reachable[lhs[production]] == 0) continue;
        for (int index = rhsStart[production]; index < rhsStart[production + 1]; index++) {
          int symbol = rhs[index];
          if (reachable[symbol] == 0) {
            reachable[symbol] = 1;
            changed = true;
          }
        }
      }
    } while (changed);
    return reachable;
  }

  private static byte[] leftRecursive(
      byte[] kinds, int[] lhs, int[] rhsStart, int[] rhs, byte[] nullable) {
    byte[] result = new byte[kinds.length];
    int[] queue = new int[kinds.length];
    byte[] visited = new byte[kinds.length];

    for (int start = 0; start < kinds.length; start++) {
      if (!nonTerminal(kinds[start])) continue;
      Arrays.fill(visited, (byte) 0);
      int head = 0;
      int tail = 0;
      queue[tail++] = start;
      visited[start] = 1;

      boolean recursive = false;
      while (head < tail && !recursive) {
        int current = queue[head++];
        for (int production = 0; production < lhs.length; production++) {
          if (lhs[production] != current) continue;
          for (int index = rhsStart[production]; index < rhsStart[production + 1]; index++) {
            int symbol = rhs[index];
            if (!nonTerminal(kinds[symbol])) break;
            if (symbol == start) {
              recursive = true;
              break;
            }
            if (visited[symbol] == 0) {
              visited[symbol] = 1;
              queue[tail++] = symbol;
            }
            if (nullable[symbol] == 0) break;
          }
          if (recursive) break;
        }
      }
      if (recursive) result[start] = 1;
    }
    return result;
  }

  private static boolean nonTerminal(byte kind) {
    return Byte.toUnsignedInt(kind) == M3GrammarSymbolKind.NON_TERMINAL.ordinal();
  }

  private static void set(long[] bits, int row, int words, int bit) {
    bits[row * words + (bit >>> 6)] |= 1L << bit;
  }

  private static boolean or(long[] target, int targetRow, int sourceRow, int words) {
    return or(target, targetRow, target, sourceRow, words);
  }

  private static boolean or(
      long[] target, int targetRow, long[] source, int sourceRow, int words) {
    boolean changed = false;
    int targetOffset = targetRow * words;
    int sourceOffset = sourceRow * words;
    for (int word = 0; word < words; word++) {
      long before = target[targetOffset + word];
      long after = before | source[sourceOffset + word];
      if (before != after) {
        target[targetOffset + word] = after;
        changed = true;
      }
    }
    return changed;
  }

  private record ProductiveResult(byte[] productive, int[] minimumTokens) {}

  record Result(
      byte[] nullable,
      byte[] productive,
      byte[] reachable,
      byte[] leftRecursive,
      int[] minimumTokens,
      long[] first,
      long[] follow,
      byte[] followEof,
      int[] terminalOrdinalBySymbol,
      int[] terminalSymbolByOrdinal,
      int terminalWords) {}
}
