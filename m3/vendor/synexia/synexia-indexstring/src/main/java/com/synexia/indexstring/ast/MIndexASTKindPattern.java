// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.Objects;

/** Immutable KMP pattern over precomputed AST {@link Tree.Kind} ordinals. */
public final class MIndexASTKindPattern {
  private final byte[] ordinals;
  private final int[] failure;

  private MIndexASTKindPattern(byte[] ordinals, int[] failure) {
    this.ordinals = ordinals;
    this.failure = failure;
  }

  public static MIndexASTKindPattern of(Tree.Kind... kinds) {
    Objects.requireNonNull(kinds, "kinds");
    if (kinds.length == 0) throw new IllegalArgumentException("empty AST kind pattern");
    byte[] ordinals = new byte[kinds.length];
    for (int index = 0; index < kinds.length; index++) {
      ordinals[index] = (byte) Objects.requireNonNull(kinds[index], "kind").ordinal();
    }
    int[] failure = new int[ordinals.length];
    for (int index = 1, matched = 0; index < ordinals.length; index++) {
      while (matched > 0 && ordinals[index] != ordinals[matched]) matched = failure[matched - 1];
      if (ordinals[index] == ordinals[matched]) matched++;
      failure[index] = matched;
    }
    return new MIndexASTKindPattern(ordinals, failure);
  }

  public int length() {
    return ordinals.length;
  }

  public Tree.Kind kindAt(int index) {
    return Tree.Kind.values()[Byte.toUnsignedInt(ordinals[Objects.checkIndex(index, ordinals.length)])];
  }

  /**
   * Returns occurrence IDs at the start of every match in the document preorder stream.
   *
   * <p>Preorder adjacency is a serialized-tree pattern, not necessarily an ancestor path.</p>
   */
  public int[] findAll(MIndexASTPrecompute precompute) {
    Objects.requireNonNull(precompute, "precompute");
    int[] scratch = new int[Math.max(1, precompute.occurrenceCount())];
    int count = 0;
    int matched = 0;
    for (int preorder = 0; preorder < precompute.occurrenceCount(); preorder++) {
      byte value = (byte) precompute.preorderKindOrdinal(preorder);
      while (matched > 0 && value != ordinals[matched]) matched = failure[matched - 1];
      if (value == ordinals[matched]) matched++;
      if (matched == ordinals.length) {
        int start = preorder - ordinals.length + 1;
        scratch[count++] = precompute.preorderOccurrence(start);
        matched = failure[matched - 1];
      }
    }
    return Arrays.copyOf(scratch, count);
  }

  public boolean matchesPath(MIndexASTPrecompute precompute, int ancestor, int descendant) {
    Objects.requireNonNull(precompute, "precompute");
    int[] path = precompute.pathOccurrences(ancestor, descendant);
    if (path.length != ordinals.length) return false;
    for (int index = 0; index < path.length; index++) {
      int atomHandle = precompute.document().atomHandleAtOccurrence(path[index]);
      if (precompute.document().root().pool().kind(atomHandle).ordinal()
          != Byte.toUnsignedInt(ordinals[index])) {
        return false;
      }
    }
    return true;
  }

  public int[] failureTable() {
    return failure.clone();
  }

  public long primitivePayloadBytes() {
    return ordinals.length + Integer.BYTES * (long) failure.length;
  }

  byte ordinalAt(int index) {
    return ordinals[index];
  }
}
