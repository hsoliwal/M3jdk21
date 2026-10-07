// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.MatIndexString;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * One immutable source-file occurrence table over canonical MIndexAST atoms.
 *
 * <p>Atoms form a deduplicated DAG. Occurrence rows preserve the original tree: source range,
 * parent, first child, next sibling and parser depth. This separation allows one immutable atom to
 * occur at many source locations without contaminating canonical identity with offsets.
 */
public final class MIndexASTDocument implements MIndexASTOccurrenceSource {
  private final MatIndexString path;
  private final byte[] sourceUtf16Sha256;
  private final MIndexAST root;
  private final int rootOccurrence;
  private final int[] atomHandles;
  private final long[] starts;
  private final long[] ends;
  private final int[] parents;
  private final int[] firstChildren;
  private final int[] nextSiblings;
  private final int[] depths;
  private final List<MIndexASTDiagnostic> diagnostics;

  MIndexASTDocument(
      MatIndexString path,
      byte[] sourceUtf16Sha256,
      MIndexAST root,
      int rootOccurrence,
      int[] atomHandles,
      long[] starts,
      long[] ends,
      int[] parents,
      int[] firstChildren,
      int[] nextSiblings,
      int[] depths,
      List<MIndexASTDiagnostic> diagnostics) {
    this.path = Objects.requireNonNull(path, "path");
    this.sourceUtf16Sha256 = Objects.requireNonNull(sourceUtf16Sha256, "sourceUtf16Sha256").clone();
    if (this.sourceUtf16Sha256.length != 32) {
      throw new IllegalArgumentException("sourceUtf16Sha256 must contain 32 bytes");
    }
    this.root = Objects.requireNonNull(root, "root");
    this.rootOccurrence = rootOccurrence;
    this.atomHandles = Objects.requireNonNull(atomHandles, "atomHandles").clone();
    this.starts = Objects.requireNonNull(starts, "starts").clone();
    this.ends = Objects.requireNonNull(ends, "ends").clone();
    this.parents = Objects.requireNonNull(parents, "parents").clone();
    this.firstChildren = Objects.requireNonNull(firstChildren, "firstChildren").clone();
    this.nextSiblings = Objects.requireNonNull(nextSiblings, "nextSiblings").clone();
    this.depths = Objects.requireNonNull(depths, "depths").clone();
    this.diagnostics = List.copyOf(diagnostics);

    int size = this.atomHandles.length;
    if (this.starts.length != size
        || this.ends.length != size
        || this.parents.length != size
        || this.firstChildren.length != size
        || this.nextSiblings.length != size
        || this.depths.length != size) {
      throw new IllegalArgumentException("occurrence lane lengths differ");
    }
    Objects.checkIndex(rootOccurrence, size);
    if (this.atomHandles[rootOccurrence] != root.handle()) {
      throw new IllegalArgumentException("root occurrence does not reference root atom");
    }
    if (this.parents[rootOccurrence] != -1 || this.depths[rootOccurrence] != 0) {
      throw new IllegalArgumentException("root occurrence must have parent=-1 and depth=0");
    }

    int poolSize = root.pool().size();
    for (int occurrence = 0; occurrence < size; occurrence++) {
      int atomHandle = this.atomHandles[occurrence];
      if (atomHandle < 1 || atomHandle > poolSize) {
        throw new IllegalArgumentException(
            "occurrence references unknown atom handle " + atomHandle);
      }
      if (this.depths[occurrence] < 0) {
        throw new IllegalArgumentException("negative occurrence depth");
      }
      long start = this.starts[occurrence];
      long end = this.ends[occurrence];
      if (start >= 0 && end >= 0 && end < start) {
        throw new IllegalArgumentException("occurrence end precedes start");
      }

      int parent = this.parents[occurrence];
      if (occurrence != rootOccurrence) {
        if (parent <= occurrence || parent >= size) {
          throw new IllegalArgumentException("non-root occurrence must point to a later parent");
        }
        if (this.depths[parent] != this.depths[occurrence] - 1) {
          throw new IllegalArgumentException("occurrence depth does not match parent depth");
        }
      }

      int firstChild = this.firstChildren[occurrence];
      if (firstChild != -1 && (firstChild < 0 || firstChild >= occurrence)) {
        throw new IllegalArgumentException("first child must precede its parent");
      }
      int nextSibling = this.nextSiblings[occurrence];
      if (nextSibling != -1 && (nextSibling <= occurrence || nextSibling >= size)) {
        throw new IllegalArgumentException("next sibling must follow the current occurrence");
      }
    }
  }

  public MatIndexString path() {
    return path;
  }

  /**
   * SHA-256 over logical UTF-16 code units in big-endian order.
   *
   * <p>This hashes the Java source character sequence seen by javac, not the original file bytes.
   * Encoding/BOM differences that decode to the same characters therefore produce the same hash.
   */
  public byte[] sourceUtf16Sha256() {
    return sourceUtf16Sha256.clone();
  }

  @Override
  public String sourceUtf16Sha256Hex() {
    return HexFormat.of().formatHex(sourceUtf16Sha256);
  }

  /** Existing append-only pool owns immutable atom identity independently of source positions. */
  @Override
  public Object syntaxIdentityDomain() { return root.pool(); }

  public MIndexAST root() {
    return root;
  }

  @Override
  public long rootExactHash64() {
    return root.exactHash64();
  }

  @Override
  public long rootStructuralHash64() {
    return root.structuralHash64();
  }

  @Override
  public long rootLogicHash64() {
    return root.logicHash64();
  }

  public MIndexLanguageSpec spec() {
    return root.spec();
  }

  public MIndexLanguageRule ruleAtOccurrence(int occurrence) {
    return atomAtOccurrence(occurrence).rule();
  }

  public int rootOccurrence() {
    return rootOccurrence;
  }

  public int occurrenceCount() {
    return atomHandles.length;
  }

  /** Builds an immutable opt-in navigation/search snapshot for this occurrence tree. */
  public MIndexASTPrecompute precompute() {
    return MIndexASTPrecompute.build(this);
  }

  /** Builds an immutable source-position lookup index for editor/cursor queries. */
  public MIndexASTSourceIndex sourceIndex() {
    return MIndexASTSourceIndex.build(this);
  }

  public MIndexAST atomAtOccurrence(int occurrence) {
    Objects.checkIndex(occurrence, atomHandles.length);
    return root.pool().value(atomHandles[occurrence]);
  }

  @Override
  public long startPosition(int occurrence) {
    return starts[Objects.checkIndex(occurrence, starts.length)];
  }

  @Override
  public long endPosition(int occurrence) {
    return ends[Objects.checkIndex(occurrence, ends.length)];
  }

  @Override
  public int parentOccurrence(int occurrence) {
    return parents[Objects.checkIndex(occurrence, parents.length)];
  }

  @Override
  public int firstChildOccurrence(int occurrence) {
    return firstChildren[Objects.checkIndex(occurrence, firstChildren.length)];
  }

  @Override
  public int nextSiblingOccurrence(int occurrence) {
    return nextSiblings[Objects.checkIndex(occurrence, nextSiblings.length)];
  }

  @Override
  public int depth(int occurrence) {
    return depths[Objects.checkIndex(occurrence, depths.length)];
  }

  public List<MIndexASTDiagnostic> diagnostics() {
    return diagnostics;
  }

  public boolean hasErrors() {
    return diagnostics.stream().anyMatch(d -> d.kind() == javax.tools.Diagnostic.Kind.ERROR);
  }

  public int[] childOccurrences(int occurrence) {
    Objects.checkIndex(occurrence, atomHandles.length);
    int count = 0;
    for (int child = firstChildren[occurrence]; child >= 0; child = nextSiblings[child]) count++;
    int[] result = new int[count];
    int index = 0;
    for (int child = firstChildren[occurrence]; child >= 0; child = nextSiblings[child]) {
      result[index++] = child;
    }
    return result;
  }

  public int[] occurrencesOf(MIndexAST atom) {
    Objects.requireNonNull(atom, "atom");
    if (atom.pool() != root.pool())
      throw new IllegalArgumentException("atom belongs to different pool");
    int count = 0;
    for (int handle : atomHandles) if (handle == atom.handle()) count++;
    int[] result = new int[count];
    int index = 0;
    for (int occurrence = 0; occurrence < atomHandles.length; occurrence++) {
      if (atomHandles[occurrence] == atom.handle()) result[index++] = occurrence;
    }
    return result;
  }

  public int[] findOccurrences(MIndexASTQuery query) {
    Objects.requireNonNull(query, "query");
    if (query.pool() != root.pool())
      throw new IllegalArgumentException("query belongs to different pool");
    int[] scratch = new int[atomHandles.length];
    int count = 0;
    for (int occurrence = 0; occurrence < atomHandles.length; occurrence++) {
      if (query.matchesHandle(atomHandles[occurrence])) scratch[count++] = occurrence;
    }
    return Arrays.copyOf(scratch, count);
  }

  @Override
  public int atomHandleAtOccurrence(int occurrence) {
    return atomHandles[Objects.checkIndex(occurrence, atomHandles.length)];
  }
}
