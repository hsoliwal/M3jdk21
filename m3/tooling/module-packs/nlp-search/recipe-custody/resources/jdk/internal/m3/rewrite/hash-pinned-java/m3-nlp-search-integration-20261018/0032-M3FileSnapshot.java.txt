// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;
import com.m3.file.M3FileTextBuilder;
import com.m3.file.M3Hash256;
import java.nio.charset.Charset;

import com.m3.file.M3FileResolver;
import com.m3.file.M3FileText;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** One immutable transfer packet joining the already-computed file projections. */
public final class M3FileSnapshot {
  private final M3FileLexiconSnapshot lexicon;
  private final M3FilePrimitiveSnapshot primitive;
  private final M3FileSourceFactsSnapshot sourceFacts;

  M3FileSnapshot(
      M3FileLexiconSnapshot lexicon,
      M3FilePrimitiveSnapshot primitive,
      M3FileSourceFactsSnapshot sourceFacts) {
    this.lexicon = Objects.requireNonNull(lexicon, "lexicon");
    this.primitive = Objects.requireNonNull(primitive, "primitive");
    if (lexicon.uniqueTokenCount() != primitive.uniqueTokenCount()
        || primitive.tokenCount() != primitive.postingCount()) {
      throw new IllegalArgumentException("M3File snapshot component shape mismatch");
    }
    if (sourceFacts != null
        && (sourceFacts.tokenCount() != primitive.tokenCount()
            || sourceFacts.uniqueTokenCount() != primitive.uniqueTokenCount())) {
      throw new IllegalArgumentException("M3File source-facts snapshot shape mismatch");
    }
    this.sourceFacts = sourceFacts;
  }

  public M3FileLexiconSnapshot lexiconSnapshot() {
    return lexicon;
  }

  public M3FilePrimitiveSnapshot primitiveSnapshot() {
    return primitive;
  }

  public boolean hasSourceFacts() {
    return sourceFacts != null;
  }

  /** Returns the source-side projection, or fails closed when the file was not physical-source admitted. */
  public M3FileSourceFactsSnapshot sourceFactsSnapshot() {
    if (sourceFacts == null) {
      throw new IllegalStateException("M3File snapshot has no physical-source facts");
    }
    return sourceFacts;
  }

  /**
   * Reconstruct the logical indexed text in occurrence order for the supplied resolver space.
   *
   * <p>Each unique file-local lexicon row is materialized once; occurrence order then composes
   * those immutable M3FileText values by reference. No token spelling or joined String payload
   * is stored in the snapshot.</p>
   */
  public M3FileText indexedText(M3FileResolver resolver) {
    Objects.requireNonNull(resolver, "resolver");
    return composeIndexedText(resolver, primitive.localHandles(), null);
  }

  /**
   * Composes one detached ordered source-token selection from the retained lexicon IDs.
   *
   * <p>The resolver is explicit because this snapshot is intentionally detached from a live
   * resolver object. The selected lane is validated before any IDs are exposed.</p>
   */
  M3FileText indexedText(M3FileResolver resolver, int[] sourceTokenPositions) {
    Objects.requireNonNull(resolver, "resolver");
    return composeIndexedText(
        resolver, primitive.localHandles(), Objects.requireNonNull(sourceTokenPositions, "sourceTokenPositions"));
  }

  private M3FileText composeIndexedText(
      M3FileResolver resolver, int[] localHandles, int[] sourceTokenPositions) {
    long[] ids = lexicon.tokenIds();
    int[] starts = lexicon.tokenStarts();
    int expected = sourceTokenPositions == null ? localHandles.length : sourceTokenPositions.length;
    M3FileTextBuilder text = new M3FileTextBuilder(resolver, expected);
    if (sourceTokenPositions == null) {
      for (int handle : localHandles) {
        int checked = Objects.checkIndex(handle, lexicon.uniqueTokenCount());
        for (int ordinal = starts[checked]; ordinal < starts[checked + 1]; ordinal++) {
          text.appendId(ids[ordinal]);
        }
      }
    } else {
      int previous = -1;
      for (int sourceToken : sourceTokenPositions) {
        if (sourceToken <= previous) {
          throw new IllegalArgumentException("source token positions must be strictly increasing");
        }
        int position = Objects.checkIndex(sourceToken, localHandles.length);
        int handle = Objects.checkIndex(localHandles[position], lexicon.uniqueTokenCount());
        for (int ordinal = starts[handle]; ordinal < starts[handle + 1]; ordinal++) {
          text.appendId(ids[ordinal]);
        }
        previous = sourceToken;
      }
    }
    return text.build();
  }

  /** Exact SHA-256 for one detached source token using the retained source-facts lane. */
  public M3Hash256 tokenSha256At(int tokenIndex) {
    int checkedToken = Objects.checkIndex(tokenIndex, primitive.tokenCount());
    return sourceFactsSnapshot().tokenSha256ForLocalHandle(primitive.localHandleAt(checkedToken));
  }

  /** Computes the detached indexed-text digest without reopening the source. */
  public M3Hash256 sha256(M3FileResolver resolver, Charset charset) {
    return indexedText(resolver).sha256(Objects.requireNonNull(charset, "charset"));
  }

  /**
   * Computes exact SHA-256 for a detached token-boundary range.
   *
   * <p>The resolver is explicit because this transfer packet intentionally does not retain a live
   * resolver. The source-facts coordinates and lexicon IDs are already detached; no source read or
   * joined String is performed.</p>
   */
  public M3FileTokenRangeSha256 tokenRangeSha256(
      M3FileResolver resolver, int startToken, int endToken) {
    M3FileResolver checkedResolver = Objects.requireNonNull(resolver, "resolver");
    Objects.checkFromToIndex(startToken, endToken, primitive.tokenCount());
    M3FileSourceFactsSnapshot facts = sourceFactsSnapshot();
    int[] localHandles = primitive.localHandles();
    long[] ids = lexicon.tokenIds();
    int[] starts = lexicon.tokenStarts();
    // Delegate encoding to the file text descriptor over ordinary String owners so a UTF-16 surrogate
    // pair split across adjacent token atoms is encoded exactly once.
    M3FileTextBuilder rangeBuilder =
        new M3FileTextBuilder(checkedResolver, endToken - startToken);
    for (int token = startToken; token < endToken; token++) {
      int handle = Objects.checkIndex(localHandles[token], lexicon.uniqueTokenCount());
      for (int ordinal = starts[handle]; ordinal < starts[handle + 1]; ordinal++) {
        rangeBuilder.appendId(ids[ordinal]);
      }
    }
    M3FileText range = rangeBuilder.build();
    return new M3FileTokenRangeSha256(
        startToken,
        endToken,
        facts.utf8Offset(startToken),
        facts.utf8Offset(endToken),
        range.sha256(StandardCharsets.UTF_8));
  }

  /** Approximate retained primitive payload, excluding Java object headers. */
  public long retainedPayloadBytes() {
    long lexiconBytes =
        Long.BYTES * (long) lexicon.resolverTokenCount()
            + Integer.BYTES * (long) (lexicon.uniqueTokenCount() + 1);
    long total = Math.addExact(lexiconBytes, primitive.retainedPayloadBytes());
    return sourceFacts == null
        ? total
        : Math.addExact(total, sourceFacts.retainedPayloadBytes());
  }
}
