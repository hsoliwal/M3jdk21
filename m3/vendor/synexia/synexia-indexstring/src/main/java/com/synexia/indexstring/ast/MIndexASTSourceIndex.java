// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable source-position index over one AST document.
 *
 * <p>Occurrences with valid javac source ranges are sorted by start offset. A primitive range-max
 * tree stores the maximum end offset for each block so point queries can skip blocks that cannot
 * contain the requested source position.</p>
 */
public final class MIndexASTSourceIndex {
  private final MIndexASTDocument document;
  private final long[] starts;
  private final long[] ends;
  private final int[] occurrences;
  private final int treeBase;
  private final long[] maxEndTree;

  private MIndexASTSourceIndex(
      MIndexASTDocument document,
      long[] starts,
      long[] ends,
      int[] occurrences,
      int treeBase,
      long[] maxEndTree) {
    this.document = document;
    this.starts = starts;
    this.ends = ends;
    this.occurrences = occurrences;
    this.treeBase = treeBase;
    this.maxEndTree = maxEndTree;
  }

  public static MIndexASTSourceIndex build(MIndexASTDocument document) {
    Objects.requireNonNull(document, "document");
    int count = 0;
    for (int occurrence = 0; occurrence < document.occurrenceCount(); occurrence++) {
      long start = document.startPosition(occurrence);
      long end = document.endPosition(occurrence);
      if (start >= 0 && end >= start) count++;
    }

    long[] starts = new long[count];
    long[] ends = new long[count];
    int[] occurrences = new int[count];
    int cursor = 0;
    for (int occurrence = 0; occurrence < document.occurrenceCount(); occurrence++) {
      long start = document.startPosition(occurrence);
      long end = document.endPosition(occurrence);
      if (start >= 0 && end >= start) {
        starts[cursor] = start;
        ends[cursor] = end;
        occurrences[cursor] = occurrence;
        cursor++;
      }
    }
    sortRanges(document, starts, ends, occurrences);

    int base = 1;
    while (base < Math.max(1, count)) {
      if (base >= (1 << 29)) throw new IllegalStateException("source interval tree too large");
      base <<= 1;
    }
    long[] tree = new long[base << 1];
    Arrays.fill(tree, Long.MIN_VALUE);
    for (int index = 0; index < count; index++) tree[base + index] = ends[index];
    for (int node = base - 1; node > 0; node--) {
      tree[node] = Math.max(tree[node << 1], tree[(node << 1) | 1]);
    }

    return new MIndexASTSourceIndex(document, starts, ends, occurrences, base, tree);
  }

  public MIndexASTDocument document() {
    return document;
  }

  public int indexedOccurrenceCount() {
    return occurrences.length;
  }

  /**
   * Returns the deepest occurrence whose half-open source range contains {@code sourceOffset}.
   *
   * <p>Returns -1 when no indexed source range contains the offset.</p>
   */
  public int deepestContaining(long sourceOffset) {
    if (sourceOffset < 0 || starts.length == 0) return -1;
    int limit = upperBound(starts, sourceOffset) - 1;
    int bestOccurrence = -1;
    int bestDepth = -1;
    while (limit >= 0) {
      int index =
          rightmostWithEndAtLeast(
              1, 0, treeBase - 1, limit, sourceOffset);
      if (index < 0) break;
      int occurrence = occurrences[index];
      if (starts[index] <= sourceOffset && sourceOffset < ends[index]) {
        int depth = document.depth(occurrence);
        if (depth > bestDepth) {
          bestDepth = depth;
          bestOccurrence = occurrence;
        }
      }
      limit = index - 1;
    }
    return bestOccurrence;
  }

  /** Returns root -> deepest-containing occurrence, or an empty array when no range contains it. */
  public int[] containingPath(long sourceOffset) {
    int deepest = deepestContaining(sourceOffset);
    if (deepest < 0) return new int[0];
    int length = document.depth(deepest) + 1;
    int[] path = new int[length];
    int write = length - 1;
    for (int current = deepest; current >= 0; current = document.parentOccurrence(current)) {
      path[write--] = current;
    }
    return write < 0 ? path : Arrays.copyOfRange(path, write + 1, path.length);
  }

  public int firstStartingAtOrAfter(long sourceOffset) {
    int index = lowerBound(starts, sourceOffset);
    return index >= starts.length ? -1 : occurrences[index];
  }

  public long primitivePayloadBytes() {
    return Long.BYTES * (long) (starts.length + ends.length + maxEndTree.length)
        + Integer.BYTES * (long) occurrences.length;
  }

  /** Detached primitive payload used by the rebuildable secondary-tier sidecar. */
  static Payload snapshot(MIndexASTSourceIndex index) {
    Objects.requireNonNull(index, "index");
    return new Payload(
        index.starts.clone(),
        index.ends.clone(),
        index.occurrences.clone(),
        index.treeBase,
        index.maxEndTree.clone());
  }

  /** Rehydrates a projection after its identity and shape have been validated by the codec. */
  static MIndexASTSourceIndex restore(MIndexASTDocument document, Payload payload) {
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(payload, "payload");
    return new MIndexASTSourceIndex(
        document,
        payload.starts().clone(),
        payload.ends().clone(),
        payload.occurrences().clone(),
        payload.treeBase(),
        payload.maxEndTree().clone());
  }

  record Payload(
      long[] starts,
      long[] ends,
      int[] occurrences,
      int treeBase,
      long[] maxEndTree) {}

  private int rightmostWithEndAtLeast(
      int node, int left, int right, int limit, long minimumEnd) {
    if (left > limit || maxEndTree[node] < minimumEnd) return -1;
    if (left == right) return left < starts.length ? left : -1;
    int middle = (left + right) >>> 1;
    int rightResult =
        rightmostWithEndAtLeast(
            (node << 1) | 1, middle + 1, right, limit, minimumEnd);
    if (rightResult >= 0) return rightResult;
    return rightmostWithEndAtLeast(node << 1, left, middle, limit, minimumEnd);
  }

  private static int lowerBound(long[] values, long target) {
    int low = 0;
    int high = values.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (values[middle] < target) low = middle + 1;
      else high = middle;
    }
    return low;
  }

  private static int upperBound(long[] values, long target) {
    int low = 0;
    int high = values.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (values[middle] <= target) low = middle + 1;
      else high = middle;
    }
    return low;
  }

  private static void sortRanges(
      MIndexASTDocument document,
      long[] starts,
      long[] ends,
      int[] occurrences) {
    int length = starts.length;
    if (length < 2) return;

    long[] tempStarts = new long[length];
    long[] tempEnds = new long[length];
    int[] tempOccurrences = new int[length];

    long[] sourceStarts = starts;
    long[] sourceEnds = ends;
    int[] sourceOccurrences = occurrences;
    long[] targetStarts = tempStarts;
    long[] targetEnds = tempEnds;
    int[] targetOccurrences = tempOccurrences;

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
                  && compareRange(
                          document,
                          sourceStarts[left],
                          sourceOccurrences[left],
                          sourceStarts[right],
                          sourceOccurrences[right])
                      <= 0) {
            targetStarts[out] = sourceStarts[left];
            targetEnds[out] = sourceEnds[left];
            targetOccurrences[out++] = sourceOccurrences[left++];
          } else {
            targetStarts[out] = sourceStarts[right];
            targetEnds[out] = sourceEnds[right];
            targetOccurrences[out++] = sourceOccurrences[right++];
          }
        }
      }

      long[] startsSwap = sourceStarts;
      sourceStarts = targetStarts;
      targetStarts = startsSwap;
      long[] endsSwap = sourceEnds;
      sourceEnds = targetEnds;
      targetEnds = endsSwap;
      int[] occurrencesSwap = sourceOccurrences;
      sourceOccurrences = targetOccurrences;
      targetOccurrences = occurrencesSwap;
      width = block;
    }

    if (sourceStarts != starts) {
      System.arraycopy(sourceStarts, 0, starts, 0, length);
      System.arraycopy(sourceEnds, 0, ends, 0, length);
      System.arraycopy(sourceOccurrences, 0, occurrences, 0, length);
    }
  }

  private static int compareRange(
      MIndexASTDocument document,
      long leftStart,
      int leftOccurrence,
      long rightStart,
      int rightOccurrence) {
    int start = Long.compare(leftStart, rightStart);
    if (start != 0) return start;
    int depth = Integer.compare(document.depth(leftOccurrence), document.depth(rightOccurrence));
    return depth != 0 ? depth : Integer.compare(leftOccurrence, rightOccurrence);
  }
}
