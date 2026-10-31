// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;
import com.m3.file.M3FileTextBuilder;
import com.m3.file.M3Hash256;
import java.nio.charset.Charset;

import com.m3.file.M3FileText;
import java.util.Objects;
import java.util.function.IntConsumer;

/** Zero-copy ordinal projection over selected token kinds in one immutable M3File. */
public final class M3FileProjection {
  private static final int SEMANTIC_MASK =
      bit(M3FileTokenKind.WORD)
          | bit(M3FileTokenKind.NUMBER)
          | bit(M3FileTokenKind.QUOTED)
          | bit(M3FileTokenKind.SYMBOL)
          | bit(M3FileTokenKind.OTHER);
  private static final int TRIVIA_MASK =
      bit(M3FileTokenKind.WHITESPACE)
          | bit(M3FileTokenKind.NEWLINE)
          | bit(M3FileTokenKind.COMMENT);

  private final M3File file;
  private final int kindMask;
  private final int[] sourceTokenPositions;
  private volatile M3FileText indexedText;

  private M3FileProjection(M3File file, int kindMask) {
    this.file = Objects.requireNonNull(file, "file");
    this.kindMask = kindMask;
    int count = 0;
    for (int token = 0; token < file.tokenCount(); token++) {
      if ((kindMask & bit(file.tokenKindAt(token))) != 0) count++;
    }
    sourceTokenPositions = new int[count];
    int at = 0;
    for (int token = 0; token < file.tokenCount(); token++) {
      if ((kindMask & bit(file.tokenKindAt(token))) != 0) sourceTokenPositions[at++] = token;
    }
  }

  public static M3FileProjection ofKinds(
      M3File file, M3FileTokenKind... selectedKinds) {
    return new M3FileProjection(file, maskOf(selectedKinds));
  }

  public static M3FileProjection semantic(M3File file) {
    return new M3FileProjection(file, SEMANTIC_MASK);
  }

  public static M3FileProjection trivia(M3File file) {
    return new M3FileProjection(file, TRIVIA_MASK);
  }

  public M3File file() { return file; }

  public int kindMask() { return kindMask; }

  public boolean includes(M3FileTokenKind kind) {
    return (kindMask & bit(Objects.requireNonNull(kind, "kind"))) != 0;
  }

  public int tokenCount() { return sourceTokenPositions.length; }

  public int sourceTokenIndexAt(int ordinal) {
    return sourceTokenPositions[Objects.checkIndex(ordinal, sourceTokenPositions.length)];
  }

  public int localHandleAt(int ordinal) {
    return file.localHandleAt(sourceTokenIndexAt(ordinal));
  }

  public M3FileText tokenAt(int ordinal) {
    return file.tokenAt(sourceTokenIndexAt(ordinal));
  }

  /** Returns selected occurrence atoms as one resolver-owned indexed projection. */
  public M3FileText asM3String() {
    M3FileText cached = indexedText;
    if (cached != null) return cached;
    M3FileTextBuilder builder =
        new M3FileTextBuilder(file.lexicon().resolver(), sourceTokenPositions.length);
    for (int sourceToken : sourceTokenPositions) {
      builder.append(file.tokenAt(sourceToken));
    }
    M3FileText built = builder.build();
    synchronized (this) {
      if (indexedText == null) indexedText = built;
      return indexedText;
    }
  }

  /** Computes the selected projection digest through the file text descriptor over ordinary String owners. */
  public M3Hash256 sha256(Charset charset) {
    return asM3String().sha256(Objects.requireNonNull(charset, "charset"));
  }

  public M3FileTokenKind tokenKindAt(int ordinal) {
    return file.tokenKindAt(sourceTokenIndexAt(ordinal));
  }

  public int resolverTokenCountAt(int ordinal) {
    return file.resolverTokenCountAt(sourceTokenIndexAt(ordinal));
  }

  public long resolverTokenIdAt(int ordinal, int resolverOrdinal) {
    return file.resolverTokenIdAt(sourceTokenIndexAt(ordinal), resolverOrdinal);
  }

  public int sourceOrdinalOf(int sourceTokenIndex) {
    Objects.checkIndex(sourceTokenIndex, file.tokenCount());
    int at = lowerBound(sourceTokenPositions, sourceTokenIndex);
    return at < sourceTokenPositions.length && sourceTokenPositions[at] == sourceTokenIndex
        ? at
        : -1;
  }

  public int firstOrdinalAtOrAfterSourceToken(int sourceTokenIndex) {
    if (sourceTokenIndex < 0 || sourceTokenIndex > file.tokenCount()) {
      throw new IndexOutOfBoundsException("source token " + sourceTokenIndex);
    }
    return lowerBound(sourceTokenPositions, sourceTokenIndex);
  }

  public M3FileSpan sourceCoveringSpan(int fromInclusive, int toExclusive) {
    Objects.checkFromToIndex(fromInclusive, toExclusive, tokenCount());
    if (fromInclusive == toExclusive) {
      int boundary =
          fromInclusive == tokenCount()
              ? file.tokenCount()
              : sourceTokenPositions[fromInclusive];
      return file.span(boundary, boundary);
    }
    return file.span(
        sourceTokenPositions[fromInclusive],
        Math.addExact(sourceTokenPositions[toExclusive - 1], 1));
  }

  public void forEachSourceToken(IntConsumer consumer) {
    Objects.requireNonNull(consumer, "consumer");
    for (int sourceToken : sourceTokenPositions) consumer.accept(sourceToken);
  }

  public long retainedPayloadBytes() {
    return Integer.BYTES * (long) sourceTokenPositions.length;
  }

  /** Detaches the selected kind mask and ordered source-token lane for JNI/serializer consumers. */
  public M3FileProjectionSnapshot snapshot() {
    return new M3FileProjectionSnapshot(kindMask, sourceTokenPositions);
  }

  static int semanticKindMask() {
    return SEMANTIC_MASK;
  }

  static int triviaKindMask() {
    return TRIVIA_MASK;
  }

  static int maskOf(M3FileTokenKind... selectedKinds) {
    Objects.requireNonNull(selectedKinds, "selectedKinds");
    int mask = 0;
    for (M3FileTokenKind kind : selectedKinds) {
      mask |= bit(Objects.requireNonNull(kind, "selected kind"));
    }
    return mask;
  }

  private static int bit(M3FileTokenKind kind) {
    return 1 << kind.ordinal();
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
}
