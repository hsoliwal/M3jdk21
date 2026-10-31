// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable precomputed navigation/search geometry for one AST document occurrence tree.
 *
 * <p>The canonical atom pool is a DAG and can have multiple parents. This index deliberately works
 * on {@link MIndexASTDocument} occurrences, where parentage is a real tree. It precomputes preorder
 * intervals, subtree sizes, child ordinals, binary-lifting ancestors, root-path hashes, kind
 * postings and canonical-atom postings once.</p>
 */
public final class MIndexASTPrecompute {
  private static final long PATH_BASE = 0x9e3779b185ebca87L;
  private static final long KIND_SEED = 0x4d494e4445584b50L;
  private static final long EXACT_SEED = 0x4d494e4445584558L;

  private final MIndexASTDocument document;
  private final MIndexASTPool pool;
  private final int occurrenceCount;
  private final int[] preorderToOccurrence;
  private final int[] occurrenceToPreorder;
  private final int[] subtreeSizes;
  private final int[] childCounts;
  private final int[] childOrdinals;
  private final int levels;
  private final int[] ancestors;
  private final long[] powers;
  private final long[] kindPathHashes;
  private final long[] exactPathHashes;
  private final byte[] preorderKinds;
  private final int[] kindOffsets;
  private final int[] occurrencesByKind;
  private final int atomLimit;
  private final int[] atomOffsets;
  private final int[] occurrencesByAtom;

  private MIndexASTPrecompute(
      MIndexASTDocument document,
      int[] preorderToOccurrence,
      int[] occurrenceToPreorder,
      int[] subtreeSizes,
      int[] childCounts,
      int[] childOrdinals,
      int levels,
      int[] ancestors,
      long[] powers,
      long[] kindPathHashes,
      long[] exactPathHashes,
      byte[] preorderKinds,
      int[] kindOffsets,
      int[] occurrencesByKind,
      int atomLimit,
      int[] atomOffsets,
      int[] occurrencesByAtom) {
    this.document = document;
    this.pool = document.root().pool();
    this.occurrenceCount = document.occurrenceCount();
    this.preorderToOccurrence = preorderToOccurrence;
    this.occurrenceToPreorder = occurrenceToPreorder;
    this.subtreeSizes = subtreeSizes;
    this.childCounts = childCounts;
    this.childOrdinals = childOrdinals;
    this.levels = levels;
    this.ancestors = ancestors;
    this.powers = powers;
    this.kindPathHashes = kindPathHashes;
    this.exactPathHashes = exactPathHashes;
    this.preorderKinds = preorderKinds;
    this.kindOffsets = kindOffsets;
    this.occurrencesByKind = occurrencesByKind;
    this.atomLimit = atomLimit;
    this.atomOffsets = atomOffsets;
    this.occurrencesByAtom = occurrencesByAtom;
  }

  /**
   * Package-private immutable array view used by the authenticated cold-image codec.
   *
   * <p>It is intentionally not a public serialization contract. A restored image must pass
   * {@link #restore(MIndexASTDocument, Snapshot)} before it can become a live accelerator.</p>
   */
  static final class Snapshot {
    private final int occurrenceCount;
    private final int[] preorderToOccurrence;
    private final int[] occurrenceToPreorder;
    private final int[] subtreeSizes;
    private final int[] childCounts;
    private final int[] childOrdinals;
    private final int levels;
    private final int[] ancestors;
    private final long[] powers;
    private final long[] kindPathHashes;
    private final long[] exactPathHashes;
    private final byte[] preorderKinds;
    private final int[] kindOffsets;
    private final int[] occurrencesByKind;
    private final int atomLimit;
    private final int[] atomOffsets;
    private final int[] occurrencesByAtom;

    Snapshot(
        int occurrenceCount,
        int[] preorderToOccurrence,
        int[] occurrenceToPreorder,
        int[] subtreeSizes,
        int[] childCounts,
        int[] childOrdinals,
        int levels,
        int[] ancestors,
        long[] powers,
        long[] kindPathHashes,
        long[] exactPathHashes,
        byte[] preorderKinds,
        int[] kindOffsets,
        int[] occurrencesByKind,
        int atomLimit,
        int[] atomOffsets,
        int[] occurrencesByAtom) {
      this.occurrenceCount = occurrenceCount;
      this.preorderToOccurrence = preorderToOccurrence;
      this.occurrenceToPreorder = occurrenceToPreorder;
      this.subtreeSizes = subtreeSizes;
      this.childCounts = childCounts;
      this.childOrdinals = childOrdinals;
      this.levels = levels;
      this.ancestors = ancestors;
      this.powers = powers;
      this.kindPathHashes = kindPathHashes;
      this.exactPathHashes = exactPathHashes;
      this.preorderKinds = preorderKinds;
      this.kindOffsets = kindOffsets;
      this.occurrencesByKind = occurrencesByKind;
      this.atomLimit = atomLimit;
      this.atomOffsets = atomOffsets;
      this.occurrencesByAtom = occurrencesByAtom;
    }

    int occurrenceCount() {
      return occurrenceCount;
    }

    int[] preorderToOccurrence() {
      return preorderToOccurrence;
    }

    int[] occurrenceToPreorder() {
      return occurrenceToPreorder;
    }

    int[] subtreeSizes() {
      return subtreeSizes;
    }

    int[] childCounts() {
      return childCounts;
    }

    int[] childOrdinals() {
      return childOrdinals;
    }

    int levels() {
      return levels;
    }

    int[] ancestors() {
      return ancestors;
    }

    long[] powers() {
      return powers;
    }

    long[] kindPathHashes() {
      return kindPathHashes;
    }

    long[] exactPathHashes() {
      return exactPathHashes;
    }

    byte[] preorderKinds() {
      return preorderKinds;
    }

    int[] kindOffsets() {
      return kindOffsets;
    }

    int[] occurrencesByKind() {
      return occurrencesByKind;
    }

    int atomLimit() {
      return atomLimit;
    }

    int[] atomOffsets() {
      return atomOffsets;
    }

    int[] occurrencesByAtom() {
      return occurrencesByAtom;
    }
  }

  Snapshot snapshot() {
    return new Snapshot(
        occurrenceCount,
        preorderToOccurrence.clone(),
        occurrenceToPreorder.clone(),
        subtreeSizes.clone(),
        childCounts.clone(),
        childOrdinals.clone(),
        levels,
        ancestors.clone(),
        powers.clone(),
        kindPathHashes.clone(),
        exactPathHashes.clone(),
        preorderKinds.clone(),
        kindOffsets.clone(),
        occurrencesByKind.clone(),
        atomLimit,
        atomOffsets.clone(),
        occurrencesByAtom.clone());
  }

  static MIndexASTPrecompute restore(
      MIndexASTDocument document,
      Snapshot payload) {
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(payload, "payload");
    validateSnapshot(document, payload);
    return new MIndexASTPrecompute(
        document,
        payload.preorderToOccurrence().clone(),
        payload.occurrenceToPreorder().clone(),
        payload.subtreeSizes().clone(),
        payload.childCounts().clone(),
        payload.childOrdinals().clone(),
        payload.levels(),
        payload.ancestors().clone(),
        payload.powers().clone(),
        payload.kindPathHashes().clone(),
        payload.exactPathHashes().clone(),
        payload.preorderKinds().clone(),
        payload.kindOffsets().clone(),
        payload.occurrencesByKind().clone(),
        payload.atomLimit(),
        payload.atomOffsets().clone(),
        payload.occurrencesByAtom().clone());
  }

  private static void validateSnapshot(
      MIndexASTDocument document,
      Snapshot payload) {
    int n = document.occurrenceCount();
    int expectedLevels = Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(n));
    if (payload.occurrenceCount() != n
        || payload.preorderToOccurrence().length != n
        || payload.occurrenceToPreorder().length != n
        || payload.subtreeSizes().length != n
        || payload.childCounts().length != n
        || payload.childOrdinals().length != n
        || payload.levels() != expectedLevels
        || payload.ancestors().length != Math.multiplyExact(expectedLevels, n)
        || payload.powers().length != n + 1
        || payload.kindPathHashes().length != n
        || payload.exactPathHashes().length != n
        || payload.preorderKinds().length != n
        || payload.kindOffsets().length != Tree.Kind.values().length + 1
        || payload.occurrencesByKind().length != n
        || payload.atomLimit() < 0
        || payload.atomLimit() > document.root().pool().size()
        || payload.atomOffsets().length != payload.atomLimit() + 2
        || payload.occurrencesByAtom().length != n) {
      throw new IllegalArgumentException("navigation snapshot shape mismatch");
    }

    boolean[] seen = new boolean[n];
    for (int preorder = 0; preorder < n; preorder++) {
      int occurrence = payload.preorderToOccurrence()[preorder];
      if (occurrence < 0 || occurrence >= n || seen[occurrence]) {
        throw new IllegalArgumentException("navigation preorder is not a permutation");
      }
      seen[occurrence] = true;
      if (payload.occurrenceToPreorder()[occurrence] != preorder) {
        throw new IllegalArgumentException("navigation preorder inverse mismatch");
      }
    }

    int[] computedChildCounts = new int[n];
    for (int parent = 0; parent < n; parent++) {
      int ordinal = 0;
      for (int child = document.firstChildOccurrence(parent);
          child >= 0;
          child = document.nextSiblingOccurrence(child)) {
        if (payload.childOrdinals()[child] != ordinal) {
          throw new IllegalArgumentException("navigation child ordinal mismatch");
        }
        ordinal++;
      }
      computedChildCounts[parent] = ordinal;
    }
    if (!Arrays.equals(computedChildCounts, payload.childCounts())) {
      throw new IllegalArgumentException("navigation child count mismatch");
    }

    int root = document.rootOccurrence();
    if (payload.preorderToOccurrence()[0] != root) {
      throw new IllegalArgumentException("navigation root preorder mismatch");
    }
    for (int index = n - 1; index >= 0; index--) {
      int occurrence = payload.preorderToOccurrence()[index];
      int expectedSize = 1;
      int nextPreorder = index + 1;
      for (int child = document.firstChildOccurrence(occurrence);
          child >= 0;
          child = document.nextSiblingOccurrence(child)) {
        if (payload.occurrenceToPreorder()[child] != nextPreorder) {
          throw new IllegalArgumentException("navigation child preorder mismatch");
        }
        expectedSize = Math.addExact(expectedSize, payload.subtreeSizes()[child]);
        nextPreorder = Math.addExact(nextPreorder, payload.subtreeSizes()[child]);
      }
      if (payload.subtreeSizes()[occurrence] != expectedSize) {
        throw new IllegalArgumentException("navigation subtree size mismatch");
      }
    }

    for (int occurrence = 0; occurrence < n; occurrence++) {
      int parent = document.parentOccurrence(occurrence);
      if (payload.ancestors()[occurrence] != parent) {
        throw new IllegalArgumentException("navigation ancestor level zero mismatch");
      }
    }
    for (int level = 1; level < expectedLevels; level++) {
      int previous = (level - 1) * n;
      int base = level * n;
      for (int occurrence = 0; occurrence < n; occurrence++) {
        int half = payload.ancestors()[previous + occurrence];
        int expected = half < 0 ? -1 : payload.ancestors()[previous + half];
        if (payload.ancestors()[base + occurrence] != expected) {
          throw new IllegalArgumentException("navigation ancestor table mismatch");
        }
      }
    }

    if (payload.powers()[0] != 1L) {
      throw new IllegalArgumentException("navigation path power origin mismatch");
    }
    for (int length = 1; length <= n; length++) {
      if (payload.powers()[length] != payload.powers()[length - 1] * PATH_BASE) {
        throw new IllegalArgumentException("navigation path powers mismatch");
      }
    }

    MIndexASTPool pool = document.root().pool();
    int[] kindCounts = new int[Tree.Kind.values().length];
    int[] atomCounts = new int[payload.atomLimit() + 1];
    for (int preorder = 0; preorder < n; preorder++) {
      int occurrence = payload.preorderToOccurrence()[preorder];
      int atom = document.atomHandleAtOccurrence(occurrence);
      if (atom < 1 || atom > payload.atomLimit()) {
        throw new IllegalArgumentException("navigation atom limit mismatch");
      }
      int kind = pool.kind(atom).ordinal();
      kindCounts[kind]++;
      atomCounts[atom]++;
      if (Byte.toUnsignedInt(payload.preorderKinds()[preorder]) != kind) {
        throw new IllegalArgumentException("navigation preorder kind mismatch");
      }

      int parent = document.parentOccurrence(occurrence);
      long parentKind = parent < 0 ? 0L : payload.kindPathHashes()[parent];
      long parentExact = parent < 0 ? 0L : payload.exactPathHashes()[parent];
      long expectedKind =
          parentKind * PATH_BASE
              + avalanche(pool.kind(atom).name().hashCode() ^ KIND_SEED);
      long expectedExact =
          parentExact * PATH_BASE
              + avalanche(pool.exactHash64(atom) ^ EXACT_SEED);
      if (payload.kindPathHashes()[occurrence] != expectedKind
          || payload.exactPathHashes()[occurrence] != expectedExact) {
        throw new IllegalArgumentException("navigation path hash mismatch");
      }
    }

    validateOffsetsAndPosting(
        payload.kindOffsets(), payload.occurrencesByKind(), n, kindCounts,
        occurrence -> pool.kind(document.atomHandleAtOccurrence(occurrence)).ordinal());
    validateOffsetsAndPosting(
        payload.atomOffsets(), payload.occurrencesByAtom(), n, atomCounts,
        occurrence -> document.atomHandleAtOccurrence(occurrence));
  }

  @FunctionalInterface
  private interface IntKey {
    int key(int value);
  }

  private static void validateOffsetsAndPosting(
      int[] offsets,
      int[] postings,
      int expectedTotal,
      int[] counts,
      IntKey key) {
    if (offsets[0] != 0 || offsets[offsets.length - 1] != expectedTotal) {
      throw new IllegalArgumentException("navigation posting offset total mismatch");
    }
    for (int index = 0; index < counts.length; index++) {
      if (offsets[index] > offsets[index + 1]
          || offsets[index + 1] - offsets[index] != counts[index]) {
        throw new IllegalArgumentException("navigation posting offset mismatch");
      }
      for (int cursor = offsets[index]; cursor < offsets[index + 1]; cursor++) {
        if (key.key(postings[cursor]) != index) {
          throw new IllegalArgumentException("navigation posting key mismatch");
        }
      }
    }
  }


  public static MIndexASTPrecompute build(MIndexASTDocument document) {
    Objects.requireNonNull(document, "document");
    int n = document.occurrenceCount();
    if (n == 0) throw new IllegalArgumentException("AST document has no occurrences");

    int[] childCounts = new int[n];
    int[] childOrdinals = new int[n];
    Arrays.fill(childOrdinals, -1);
    int maxChildren = 0;
    for (int parent = 0; parent < n; parent++) {
      int ordinal = 0;
      for (int child = document.firstChildOccurrence(parent);
          child >= 0;
          child = document.nextSiblingOccurrence(child)) {
        childOrdinals[child] = ordinal++;
      }
      childCounts[parent] = ordinal;
      maxChildren = Math.max(maxChildren, ordinal);
    }

    int[] preorderToOccurrence = new int[n];
    int[] occurrenceToPreorder = new int[n];
    int[] stack = new int[n];
    int[] childScratch = new int[Math.max(1, maxChildren)];
    int top = 0;
    int preorder = 0;
    stack[top++] = document.rootOccurrence();
    while (top != 0) {
      int occurrence = stack[--top];
      preorderToOccurrence[preorder] = occurrence;
      occurrenceToPreorder[occurrence] = preorder++;

      int count = 0;
      for (int child = document.firstChildOccurrence(occurrence);
          child >= 0;
          child = document.nextSiblingOccurrence(child)) {
        childScratch[count++] = child;
      }
      for (int index = count - 1; index >= 0; index--) stack[top++] = childScratch[index];
    }
    if (preorder != n) throw new IllegalArgumentException("occurrence tree is disconnected");

    int[] subtreeSizes = new int[n];
    Arrays.fill(subtreeSizes, 1);
    for (int index = n - 1; index > 0; index--) {
      int occurrence = preorderToOccurrence[index];
      int parent = document.parentOccurrence(occurrence);
      subtreeSizes[parent] = Math.addExact(subtreeSizes[parent], subtreeSizes[occurrence]);
    }

    int levels = Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(n));
    int[] ancestors = new int[Math.multiplyExact(levels, n)];
    Arrays.fill(ancestors, -1);
    for (int occurrence = 0; occurrence < n; occurrence++) {
      ancestors[occurrence] = document.parentOccurrence(occurrence);
    }
    for (int level = 1; level < levels; level++) {
      int previousBase = (level - 1) * n;
      int base = level * n;
      for (int occurrence = 0; occurrence < n; occurrence++) {
        int half = ancestors[previousBase + occurrence];
        ancestors[base + occurrence] = half < 0 ? -1 : ancestors[previousBase + half];
      }
    }

    long[] powers = new long[n + 1];
    powers[0] = 1L;
    for (int index = 1; index <= n; index++) powers[index] = powers[index - 1] * PATH_BASE;

    long[] kindPathHashes = new long[n];
    long[] exactPathHashes = new long[n];
    byte[] preorderKinds = new byte[n];
    MIndexASTPool pool = document.root().pool();
    int kinds = Tree.Kind.values().length;
    int[] kindCounts = new int[kinds];
    int atomLimit = pool.size();
    int[] atomCounts = new int[atomLimit + 1];

    for (int index = 0; index < n; index++) {
      int occurrence = preorderToOccurrence[index];
      int parent = document.parentOccurrence(occurrence);
      int atomHandle = document.atomHandleAtOccurrence(occurrence);
      Tree.Kind kind = pool.kind(atomHandle);
      int kindOrdinal = kind.ordinal();
      preorderKinds[index] = (byte) kindOrdinal;
      kindCounts[kindOrdinal]++;
      atomCounts[atomHandle]++;

      long kindFeature = avalanche(kind.name().hashCode() ^ KIND_SEED);
      long exactFeature = avalanche(pool.exactHash64(atomHandle) ^ EXACT_SEED);
      long parentKind = parent < 0 ? 0L : kindPathHashes[parent];
      long parentExact = parent < 0 ? 0L : exactPathHashes[parent];
      kindPathHashes[occurrence] = parentKind * PATH_BASE + kindFeature;
      exactPathHashes[occurrence] = parentExact * PATH_BASE + exactFeature;
    }

    int[] kindOffsets = prefixOffsets(kindCounts);
    int[] occurrencesByKind = new int[n];
    int[] kindCursor = Arrays.copyOf(kindOffsets, kinds);
    for (int occurrence = 0; occurrence < n; occurrence++) {
      int kind = pool.kind(document.atomHandleAtOccurrence(occurrence)).ordinal();
      occurrencesByKind[kindCursor[kind]++] = occurrence;
    }

    int[] atomOffsets = prefixOffsets(atomCounts);
    int[] occurrencesByAtom = new int[n];
    int[] atomCursor = Arrays.copyOf(atomOffsets, atomCounts.length);
    for (int occurrence = 0; occurrence < n; occurrence++) {
      int atom = document.atomHandleAtOccurrence(occurrence);
      occurrencesByAtom[atomCursor[atom]++] = occurrence;
    }

    return new MIndexASTPrecompute(
        document,
        preorderToOccurrence,
        occurrenceToPreorder,
        subtreeSizes,
        childCounts,
        childOrdinals,
        levels,
        ancestors,
        powers,
        kindPathHashes,
        exactPathHashes,
        preorderKinds,
        kindOffsets,
        occurrencesByKind,
        atomLimit,
        atomOffsets,
        occurrencesByAtom);
  }

  public MIndexASTDocument document() {
    return document;
  }

  /** Universal process-level language/DSL AST spec that governs this specialized Java tree. */
  public MIndexASTSpecPrecompute astSpec() {
    return document.root().astSpec();
  }

  /** Builds the optional O(1)-query Euler/RMQ LCA accelerator for this same document. */
  public MIndexASTLcaSparseTable constantTimeLca() {
    return MIndexASTLcaSparseTable.build(document);
  }

  /** Builds the optional source-offset interval accelerator for this document. */
  public MIndexASTSourceIndex sourceIndex() {
    return MIndexASTSourceIndex.build(document);
  }

  public int occurrenceCount() {
    return occurrenceCount;
  }

  public int preorderOccurrence(int preorderIndex) {
    return preorderToOccurrence[Objects.checkIndex(preorderIndex, occurrenceCount)];
  }

  public int preorderIndex(int occurrence) {
    return occurrenceToPreorder[checkOccurrence(occurrence)];
  }

  public int subtreeSize(int occurrence) {
    return subtreeSizes[checkOccurrence(occurrence)];
  }

  public int subtreeEndPreorderExclusive(int occurrence) {
    int checked = checkOccurrence(occurrence);
    return Math.addExact(occurrenceToPreorder[checked], subtreeSizes[checked]);
  }

  public int[] subtreeOccurrences(int occurrence) {
    int checked = checkOccurrence(occurrence);
    int start = occurrenceToPreorder[checked];
    return Arrays.copyOfRange(preorderToOccurrence, start, start + subtreeSizes[checked]);
  }

  public int childCount(int occurrence) {
    return childCounts[checkOccurrence(occurrence)];
  }

  public int childOrdinal(int occurrence) {
    return childOrdinals[checkOccurrence(occurrence)];
  }

  public boolean isAncestor(int ancestor, int descendant) {
    int checkedAncestor = checkOccurrence(ancestor);
    int checkedDescendant = checkOccurrence(descendant);
    int start = occurrenceToPreorder[checkedAncestor];
    int descendantPreorder = occurrenceToPreorder[checkedDescendant];
    return descendantPreorder >= start
        && descendantPreorder < start + subtreeSizes[checkedAncestor];
  }

  /** Returns self for distance 0 and -1 when the requested ancestor is above the root. */
  public int kthAncestor(int occurrence, int distance) {
    int current = checkOccurrence(occurrence);
    if (distance < 0) throw new IllegalArgumentException("negative ancestor distance");
    int remaining = distance;
    int level = 0;
    while (remaining != 0 && current >= 0) {
      if ((remaining & 1) != 0) {
        if (level >= levels) return -1;
        current = ancestors[level * occurrenceCount + current];
      }
      remaining >>>= 1;
      level++;
    }
    return current;
  }

  public int lowestCommonAncestor(int left, int right) {
    int a = checkOccurrence(left);
    int b = checkOccurrence(right);
    if (isAncestor(a, b)) return a;
    if (isAncestor(b, a)) return b;

    int current = a;
    for (int level = levels - 1; level >= 0; level--) {
      int candidate = ancestors[level * occurrenceCount + current];
      if (candidate >= 0 && !isAncestor(candidate, b)) current = candidate;
    }
    return document.parentOccurrence(current);
  }

  public int distance(int left, int right) {
    int lca = lowestCommonAncestor(left, right);
    return document.depth(left) + document.depth(right) - 2 * document.depth(lca);
  }

  /** Path is ordered left -> LCA -> right. */
  public int[] pathOccurrences(int left, int right) {
    int lca = lowestCommonAncestor(left, right);
    int length = Math.addExact(distance(left, right), 1);
    int[] result = new int[length];
    int write = 0;
    for (int current = left; current != lca; current = document.parentOccurrence(current)) {
      result[write++] = current;
    }
    result[write++] = lca;

    int rightCount = document.depth(right) - document.depth(lca);
    int tail = result.length - 1;
    int current = right;
    for (int i = 0; i < rightCount; i++) {
      result[tail--] = current;
      current = document.parentOccurrence(current);
    }
    return result;
  }

  public int[] occurrences(Tree.Kind kind) {
    Objects.requireNonNull(kind, "kind");
    int ordinal = kind.ordinal();
    return Arrays.copyOfRange(
        occurrencesByKind, kindOffsets[ordinal], kindOffsets[ordinal + 1]);
  }

  public int[] occurrences(MIndexAST atom) {
    Objects.requireNonNull(atom, "atom");
    if (atom.pool() != pool) throw new IllegalArgumentException("atom belongs to different pool");
    int handle = atom.handle();
    if (handle > atomLimit) return new int[0];
    return Arrays.copyOfRange(
        occurrencesByAtom, atomOffsets[handle], atomOffsets[handle + 1]);
  }

  public int preorderKindOrdinal(int preorderIndex) {
    return Byte.toUnsignedInt(preorderKinds[Objects.checkIndex(preorderIndex, occurrenceCount)]);
  }

  public byte[] preorderKindOrdinals() {
    return preorderKinds.clone();
  }

  /**
   * Candidate hash for the inclusive ancestor -> descendant Tree.Kind path.
   *
   * <p>Hash equality is a filter, never proof.</p>
   */
  public long kindPathHash(int ancestor, int descendant) {
    return pathHash(kindPathHashes, ancestor, descendant);
  }

  /** Candidate hash over canonical atom exact hashes along an ancestor -> descendant path. */
  public long exactPathHash(int ancestor, int descendant) {
    return pathHash(exactPathHashes, ancestor, descendant);
  }

  public long primitivePayloadBytes() {
    return Integer.BYTES
            * (long)
                (preorderToOccurrence.length
                    + occurrenceToPreorder.length
                    + subtreeSizes.length
                    + childCounts.length
                    + childOrdinals.length
                    + ancestors.length
                    + kindOffsets.length
                    + occurrencesByKind.length
                    + atomOffsets.length
                    + occurrencesByAtom.length)
        + Long.BYTES
            * (long) (powers.length + kindPathHashes.length + exactPathHashes.length)
        + preorderKinds.length;
  }

  private long pathHash(long[] hashes, int ancestor, int descendant) {
    int checkedAncestor = checkOccurrence(ancestor);
    int checkedDescendant = checkOccurrence(descendant);
    if (!isAncestor(checkedAncestor, checkedDescendant)) {
      throw new IllegalArgumentException("first occurrence is not an ancestor of second");
    }
    int parent = document.parentOccurrence(checkedAncestor);
    long before = parent < 0 ? 0L : hashes[parent];
    int length = document.depth(checkedDescendant) - document.depth(checkedAncestor) + 1;
    return hashes[checkedDescendant] - before * powers[length];
  }

  private int checkOccurrence(int occurrence) {
    return Objects.checkIndex(occurrence, occurrenceCount);
  }

  private static int[] prefixOffsets(int[] counts) {
    int[] offsets = new int[counts.length + 1];
    for (int index = 0; index < counts.length; index++) {
      offsets[index + 1] = Math.addExact(offsets[index], counts[index]);
    }
    return offsets;
  }

  private static long avalanche(long value) {
    long z = value;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }
}
