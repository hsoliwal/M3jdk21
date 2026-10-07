// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import java.util.Arrays;
import java.util.Objects;

/**
 * Optional O(1) lowest-common-ancestor index using an Euler tour plus a range-min sparse table.
 *
 * <p>Construction costs O(n log n) primitive storage/work. Use {@link MIndexASTPrecompute} directly
 * when O(log n) binary-lifting LCA is sufficient and lower retained memory is preferred.</p>
 */
public final class MIndexASTLcaSparseTable {
  private final MIndexASTDocument document;
  private final int[] firstEuler;
  private final int[] eulerOccurrences;
  private final int[] eulerDepths;
  private final int[] log2;
  private final int levels;
  private final int[] sparseMinIndex;

  /**
   * Package-private primitive image used by the authenticated cold projection codec.
   *
   * <p>The image is deliberately not a public serialization contract. A restored image must be
   * checked against the already-loaded document by {@link #restore(MIndexASTDocument, Snapshot)}
   * before it can become a live LCA accelerator.</p>
   */
  static final class Snapshot {
    private final int[] firstEuler;
    private final int[] eulerOccurrences;
    private final int[] eulerDepths;
    private final int[] log2;
    private final int levels;
    private final int[] sparseMinIndex;

    Snapshot(
        int[] firstEuler,
        int[] eulerOccurrences,
        int[] eulerDepths,
        int[] log2,
        int levels,
        int[] sparseMinIndex) {
      this.firstEuler = firstEuler;
      this.eulerOccurrences = eulerOccurrences;
      this.eulerDepths = eulerDepths;
      this.log2 = log2;
      this.levels = levels;
      this.sparseMinIndex = sparseMinIndex;
    }

    int[] firstEuler() {
      return firstEuler;
    }

    int[] eulerOccurrences() {
      return eulerOccurrences;
    }

    int[] eulerDepths() {
      return eulerDepths;
    }

    int[] log2() {
      return log2;
    }

    int levels() {
      return levels;
    }

    int[] sparseMinIndex() {
      return sparseMinIndex;
    }
  }

  private MIndexASTLcaSparseTable(
      MIndexASTDocument document,
      int[] firstEuler,
      int[] eulerOccurrences,
      int[] eulerDepths,
      int[] log2,
      int levels,
      int[] sparseMinIndex) {
    this.document = document;
    this.firstEuler = firstEuler;
    this.eulerOccurrences = eulerOccurrences;
    this.eulerDepths = eulerDepths;
    this.log2 = log2;
    this.levels = levels;
    this.sparseMinIndex = sparseMinIndex;
  }

  public static MIndexASTLcaSparseTable build(MIndexASTDocument document) {
    Objects.requireNonNull(document, "document");
    int n = document.occurrenceCount();
    if (n == 0) throw new IllegalArgumentException("AST document has no occurrences");

    int eulerLength = Math.subtractExact(Math.multiplyExact(2, n), 1);
    int[] firstEuler = new int[n];
    Arrays.fill(firstEuler, -1);
    int[] eulerOccurrences = new int[eulerLength];
    int[] eulerDepths = new int[eulerLength];

    int[] nodeStack = new int[n];
    int[] nextChildStack = new int[n];
    int top = 0;
    int root = document.rootOccurrence();
    nodeStack[0] = root;
    nextChildStack[0] = document.firstChildOccurrence(root);

    int cursor = 0;
    cursor = visit(document, root, cursor, firstEuler, eulerOccurrences, eulerDepths);

    while (top >= 0) {
      int child = nextChildStack[top];
      if (child >= 0) {
        nextChildStack[top] = document.nextSiblingOccurrence(child);
        top++;
        nodeStack[top] = child;
        nextChildStack[top] = document.firstChildOccurrence(child);
        cursor =
            visit(document, child, cursor, firstEuler, eulerOccurrences, eulerDepths);
      } else {
        top--;
        if (top >= 0) {
          int parent = nodeStack[top];
          cursor =
              visit(document, parent, cursor, firstEuler, eulerOccurrences, eulerDepths);
        }
      }
    }

    if (cursor != eulerLength) {
      throw new IllegalStateException(
          "Euler tour length mismatch: expected=" + eulerLength + ", actual=" + cursor);
    }

    int[] log2 = new int[eulerLength + 1];
    for (int length = 2; length <= eulerLength; length++) {
      log2[length] = log2[length >>> 1] + 1;
    }
    int levels = log2[eulerLength] + 1;
    int[] sparse = new int[Math.multiplyExact(levels, eulerLength)];
    for (int index = 0; index < eulerLength; index++) sparse[index] = index;

    for (int level = 1; level < levels; level++) {
      int interval = 1 << level;
      int half = interval >>> 1;
      int previousBase = (level - 1) * eulerLength;
      int base = level * eulerLength;
      int maximumStart = eulerLength - interval;
      for (int start = 0; start <= maximumStart; start++) {
        int left = sparse[previousBase + start];
        int right = sparse[previousBase + start + half];
        sparse[base + start] =
            eulerDepths[left] <= eulerDepths[right] ? left : right;
      }
    }

    return new MIndexASTLcaSparseTable(
        document, firstEuler, eulerOccurrences, eulerDepths, log2, levels, sparse);
  }

  /** Snapshot primitive arrays for an authenticated, document-local cold image. */
  Snapshot snapshot() {
    return new Snapshot(
        firstEuler.clone(),
        eulerOccurrences.clone(),
        eulerDepths.clone(),
        log2.clone(),
        levels,
        sparseMinIndex.clone());
  }

  /**
   * Restore a cold image only after proving that every coordinate and RMQ cell belongs to the
   * supplied document. The supplied arrays are copied before publication.
   */
  static MIndexASTLcaSparseTable restore(
      MIndexASTDocument document,
      Snapshot payload) {
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(payload, "payload");
    validateSnapshot(document, payload);
    return new MIndexASTLcaSparseTable(
        document,
        payload.firstEuler().clone(),
        payload.eulerOccurrences().clone(),
        payload.eulerDepths().clone(),
        payload.log2().clone(),
        payload.levels(),
        payload.sparseMinIndex().clone());
  }

  public MIndexASTDocument document() {
    return document;
  }

  public int lowestCommonAncestor(int leftOccurrence, int rightOccurrence) {
    int left = firstEuler[Objects.checkIndex(leftOccurrence, firstEuler.length)];
    int right = firstEuler[Objects.checkIndex(rightOccurrence, firstEuler.length)];
    if (left > right) {
      int swap = left;
      left = right;
      right = swap;
    }
    int length = right - left + 1;
    int level = log2[length];
    int interval = 1 << level;
    int base = level * eulerOccurrences.length;
    int leftIndex = sparseMinIndex[base + left];
    int rightIndex = sparseMinIndex[base + right - interval + 1];
    int best =
        eulerDepths[leftIndex] <= eulerDepths[rightIndex] ? leftIndex : rightIndex;
    return eulerOccurrences[best];
  }

  public int eulerLength() {
    return eulerOccurrences.length;
  }

  public int levels() {
    return levels;
  }

  public long primitivePayloadBytes() {
    return Integer.BYTES
        * (long)
            (firstEuler.length
                + eulerOccurrences.length
                + eulerDepths.length
                + log2.length
                + sparseMinIndex.length);
  }

  private static void validateSnapshot(
      MIndexASTDocument document,
      Snapshot payload) {
    int occurrenceCount = document.occurrenceCount();
    if (occurrenceCount < 1) {
      throw new IllegalArgumentException("LCA document has no occurrences");
    }
    int expectedEulerLength;
    try {
      expectedEulerLength = Math.subtractExact(Math.multiplyExact(2, occurrenceCount), 1);
    } catch (ArithmeticException overflow) {
      throw new IllegalArgumentException("LCA Euler length overflows", overflow);
    }
    int expectedLevels =
        Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(expectedEulerLength));
    int[] firstEuler = payload.firstEuler();
    int[] eulerOccurrences = payload.eulerOccurrences();
    int[] eulerDepths = payload.eulerDepths();
    int[] log2 = payload.log2();
    int[] sparse = payload.sparseMinIndex();
    int expectedSparseLength;
    try {
      expectedSparseLength = Math.multiplyExact(expectedLevels, expectedEulerLength);
    } catch (ArithmeticException overflow) {
      throw new IllegalArgumentException("LCA sparse table length overflows", overflow);
    }
    if (firstEuler.length != occurrenceCount
        || eulerOccurrences.length != expectedEulerLength
        || eulerDepths.length != expectedEulerLength
        || log2.length != expectedEulerLength + 1
        || payload.levels() != expectedLevels
        || sparse.length != expectedSparseLength) {
      throw new IllegalArgumentException("LCA snapshot shape mismatch");
    }

    int[] expectedFirst = new int[occurrenceCount];
    Arrays.fill(expectedFirst, -1);
    int[] counts = new int[occurrenceCount];
    for (int cursor = 0; cursor < expectedEulerLength; cursor++) {
      int occurrence = eulerOccurrences[cursor];
      if (occurrence < 0 || occurrence >= occurrenceCount) {
        throw new IllegalArgumentException("LCA Euler occurrence is out of range");
      }
      if (cursor == 0) {
        if (occurrence != document.rootOccurrence()) {
          throw new IllegalArgumentException("LCA Euler root mismatch");
        }
      } else {
        int previous = eulerOccurrences[cursor - 1];
        if (document.parentOccurrence(occurrence) != previous
            && document.parentOccurrence(previous) != occurrence) {
          throw new IllegalArgumentException("LCA Euler transition is not adjacent");
        }
      }
      if (eulerDepths[cursor] != document.depth(occurrence)) {
        throw new IllegalArgumentException("LCA Euler depth mismatch");
      }
      if (expectedFirst[occurrence] < 0) expectedFirst[occurrence] = cursor;
      counts[occurrence]++;
    }
    for (int occurrence = 0; occurrence < occurrenceCount; occurrence++) {
      int expectedCount = 1;
      for (int child = document.firstChildOccurrence(occurrence);
          child >= 0;
          child = document.nextSiblingOccurrence(child)) {
        expectedCount++;
      }
      if (counts[occurrence] != expectedCount
          || firstEuler[occurrence] != expectedFirst[occurrence]) {
        throw new IllegalArgumentException("LCA Euler occurrence multiplicity mismatch");
      }
    }

    if (log2[0] != 0 || log2[1] != 0) {
      throw new IllegalArgumentException("LCA logarithm origin mismatch");
    }
    for (int length = 2; length <= expectedEulerLength; length++) {
      if (log2[length] != log2[length >>> 1] + 1) {
        throw new IllegalArgumentException("LCA logarithm table mismatch");
      }
    }

    for (int level = 0; level < expectedLevels; level++) {
      int interval = 1 << level;
      int half = interval >>> 1;
      int previousBase = (level - 1) * expectedEulerLength;
      int base = level * expectedEulerLength;
      int maximumStart = expectedEulerLength - interval;
      for (int start = 0; start < expectedEulerLength; start++) {
        int value = sparse[base + start];
        if (level == 0) {
          if (value != start) {
            throw new IllegalArgumentException("LCA sparse level-zero mismatch");
          }
        } else if (start <= maximumStart) {
          int left = sparse[previousBase + start];
          int right = sparse[previousBase + start + half];
          int expected =
              eulerDepths[left] <= eulerDepths[right] ? left : right;
          if (value != expected) {
            throw new IllegalArgumentException("LCA sparse aggregate mismatch");
          }
        } else if (value != 0) {
          throw new IllegalArgumentException("LCA sparse padding mismatch");
        }
        if (value < 0 || value >= expectedEulerLength) {
          throw new IllegalArgumentException("LCA sparse index is out of range");
        }
      }
    }
  }

  private static int visit(
      MIndexASTDocument document,
      int occurrence,
      int cursor,
      int[] firstEuler,
      int[] eulerOccurrences,
      int[] eulerDepths) {
    eulerOccurrences[cursor] = occurrence;
    eulerDepths[cursor] = document.depth(occurrence);
    if (firstEuler[occurrence] < 0) firstEuler[occurrence] = cursor;
    return cursor + 1;
  }
}
