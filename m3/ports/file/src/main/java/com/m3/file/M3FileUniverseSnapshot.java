// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import com.m3.file.M3TextMetrics;
import com.m3.file.M3Hash256;
import com.m3.file.M3FileText;
import com.m3.file.M3TextFacts;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Immutable Universe-level projection over ordered detached M3File snapshots. */
public final class M3FileUniverseSnapshot {
  private final List<File> files;
  private final M3FileText indexedText;
  private final int[] utf16Starts;
  private final long[] utf8Starts;
  private final int[] codePointStarts;
  private final int[] tokenStarts;
  private final long byteCount;
  private final int tokenCount;
  private final M3FileUniverseTokenIndex tokenIndex;

  private M3FileUniverseSnapshot(List<File> files, M3FileResolver resolver) {
    this(files, resolver, null);
  }

  private M3FileUniverseSnapshot(
      List<File> files,
      M3FileResolver resolver,
      M3FileUniverseTokenIndexImage.Data precomputedTokenIndex) {
    this.files = List.copyOf(Objects.requireNonNull(files, "files"));
    this.utf16Starts = new int[Math.addExact(this.files.size(), 1)];
    this.utf8Starts = new long[Math.addExact(this.files.size(), 1)];
    this.codePointStarts = new int[Math.addExact(this.files.size(), 1)];
    this.tokenStarts = new int[Math.addExact(this.files.size(), 1)];
    M3FileText composed = new M3FileText(Objects.requireNonNull(resolver, "resolver"), new long[0]);
    long bytes = 0L;
    int tokens = 0;
    for (int ordinal = 0; ordinal < this.files.size(); ordinal++) {
      File file = this.files.get(ordinal);
      if (file.ordinal() != ordinal) {
        throw new IllegalArgumentException("Universe snapshot file ordinals are not contiguous");
      }
      M3FileSnapshot snapshot = file.snapshot();
      M3FileText snapshotText = snapshot.indexedText(resolver);
      if (snapshotText.resolver() != resolver) {
        throw new IllegalArgumentException("Universe snapshots use different resolver spaces");
      }
      composed = composed.concat(snapshotText);
      utf16Starts[ordinal + 1] =
          Math.addExact(utf16Starts[ordinal], snapshotText.length());
      M3FileSourceFactsSnapshot facts = snapshot.sourceFactsSnapshot();
      utf8Starts[ordinal + 1] = Math.addExact(utf8Starts[ordinal], facts.utf8Length());
      codePointStarts[ordinal + 1] =
          Math.addExact(codePointStarts[ordinal], facts.codePointLength());
      bytes =
          Math.addExact(bytes, facts.byteFactsSnapshot().byteCount());
      tokens = Math.addExact(tokens, snapshot.primitiveSnapshot().tokenCount());
      tokenStarts[ordinal + 1] = tokens;
    }
    this.indexedText = composed;
    this.byteCount = bytes;
    this.tokenCount = tokens;
    if (precomputedTokenIndex == null) {
      this.tokenIndex = M3FileUniverseTokenIndex.freeze(this.files, this.tokenStarts, resolver);
    } else {
      int[] persistedTokenStarts = precomputedTokenIndex.tokenStarts();
      long[] persistedFingerprints = precomputedTokenIndex.fileFingerprints();
      if (!Arrays.equals(this.tokenStarts, persistedTokenStarts)
          || persistedFingerprints.length != this.files.size()) {
        throw new IllegalArgumentException("persisted Universe token-index shape mismatch");
      }
      for (int ordinal = 0; ordinal < this.files.size(); ordinal++) {
        long actual = M3FileImage.contentFingerprint64(this.files.get(ordinal).snapshot(), resolver);
        if (actual != persistedFingerprints[ordinal]) {
          throw new IllegalArgumentException("persisted Universe token-index file binding mismatch");
        }
      }
      this.tokenIndex =
          M3FileUniverseTokenIndex.restore(
              this.files,
              this.tokenStarts,
              precomputedTokenIndex.resolverTokenIds(),
              precomputedTokenIndex.postingStarts(),
              precomputedTokenIndex.globalTokenOrdinals(),
              precomputedTokenIndex.lookupGeometry(),
              resolver);
    }
  }

  /** Compose an ordered replay result without copying any resolver-owned token spelling. */
  public static M3FileUniverseSnapshot from(
      M3FileUniverseEventReader.SnapshotSummary summary, M3FileResolver resolver) {
    return from(summary, resolver, null);
  }

  static M3FileUniverseSnapshot from(
      M3FileUniverseEventReader.SnapshotSummary summary,
      M3FileResolver resolver,
      M3FileUniverseTokenIndexImage.Data precomputedTokenIndex) {
    Objects.requireNonNull(summary, "summary");
    List<File> files = new ArrayList<>(summary.files().size());
    for (M3FileUniverseEventReader.SnapshotFile file : summary.files()) {
      files.add(new File(file.ordinal(), file.path(), file.snapshot()));
    }
    return new M3FileUniverseSnapshot(files, resolver, precomputedTokenIndex);
  }

  public List<File> files() {
    return files;
  }

  public int fileCount() {
    return files.size();
  }

  public File fileAt(int ordinal) {
    return files.get(Objects.checkIndex(ordinal, files.size()));
  }

  /** Logical indexed text in Universe order, retained as resolver-owned reference composition. */
  public M3FileText indexedText() {
    return indexedText;
  }

  /** Resolver space that authenticates every detached token id and derived fingerprint. */
  public M3FileResolver resolver() {
    return indexedText.resolver();
  }

  public int utf16Start(int ordinal) {
    return utf16Starts[Objects.checkIndex(ordinal, files.size())];
  }

  public int utf16End(int ordinal) {
    int checked = Objects.checkIndex(ordinal, files.size());
    return utf16Starts[checked + 1];
  }

  public int utf16Length() {
    return utf16Starts[utf16Starts.length - 1];
  }

  public long byteCount() {
    return byteCount;
  }

  public int tokenCount() {
    return tokenCount;
  }

  /**
   * Exact SHA-256 for one global Universe token ordinal, projected from the retained per-file
   * source-facts lanes without rereading or rescanning any source.
   *
   * <p>Like the per-file facade, this operation fails closed when the Universe was composed from
   * snapshots without physical-source facts.</p>
   */
  public M3Hash256 tokenSha256At(int globalTokenIndex) {
    int global = Objects.checkIndex(globalTokenIndex, tokenCount);
    int fileOrdinal = fileOrdinalForToken(global);
    int local = global - tokenStarts[fileOrdinal];
    return files.get(fileOrdinal).snapshot().tokenSha256At(local);
  }

  /**
   * Computes exact UTF-8 SHA-256 for a global token-boundary range without reopening any source.
   *
   * <p>The range remains a segmented resolver-owned M3FileText projection. Empty files and
   * ranges crossing file boundaries retain the same logical UTF-16 and UTF-8 coordinates.</p>
   */
  public M3FileUniverseTokenRangeSha256 tokenRangeSha256(int startToken, int endToken) {
    Objects.checkFromToIndex(startToken, endToken, tokenCount);
    int startUtf16 = utf16BoundaryForToken(startToken);
    int endUtf16 = utf16BoundaryForToken(endToken);
    return new M3FileUniverseTokenRangeSha256(
        startToken,
        endToken,
        utf8BoundaryForToken(startToken),
        utf8BoundaryForToken(endToken),
        indexedText.substring(startUtf16, endUtf16).sha256(StandardCharsets.UTF_8));
  }

  /**
   * Detaches every retained coordinate for one global token, including its exact one-pass SHA-256
   * value. No source payload or complete text is reread or rescanned.
   */
  public M3FileUniverseTokenSpan tokenSpanAt(int globalTokenIndex) {
    int global = Objects.checkIndex(globalTokenIndex, tokenCount);
    int fileOrdinal = fileOrdinalForToken(global);
    int local = global - tokenStarts[fileOrdinal];
    M3FileSnapshot snapshot = files.get(fileOrdinal).snapshot();
    M3FilePrimitiveSnapshot primitive = snapshot.primitiveSnapshot();
    M3FileSourceFactsSnapshot facts = snapshot.sourceFactsSnapshot();
    int utf16Start = primitive.utf16StartAt(local);
    int utf16End = primitive.utf16EndAt(local);
    long utf8Start = facts.utf8Offset(local);
    long utf8End = facts.utf8Offset(local + 1);
    int codePointStart = facts.codePointOffset(local);
    int codePointEnd = facts.codePointOffset(local + 1);
    return new M3FileUniverseTokenSpan(
        global,
        fileOrdinal,
        local,
        Math.addExact(utf16Starts[fileOrdinal], utf16Start),
        Math.addExact(utf16Starts[fileOrdinal], utf16End),
        Math.addExact(utf8Starts[fileOrdinal], utf8Start),
        Math.addExact(utf8Starts[fileOrdinal], utf8End),
        Math.addExact(codePointStarts[fileOrdinal], codePointStart),
        Math.addExact(codePointStarts[fileOrdinal], codePointEnd),
        snapshot.tokenSha256At(local));
  }

  /** Returns one Universe token as a resolver-owned M3FileText without joined-text materialization. */
  public M3FileText tokenAt(int globalTokenIndex) {
    int global = Objects.checkIndex(globalTokenIndex, tokenCount);
    int fileOrdinal = fileOrdinalForToken(global);
    int local = global - tokenStarts[fileOrdinal];
    M3FilePrimitiveSnapshot primitive = files.get(fileOrdinal).snapshot().primitiveSnapshot();
    int start = Math.addExact(utf16Starts[fileOrdinal], primitive.utf16StartAt(local));
    int end = Math.addExact(utf16Starts[fileOrdinal], primitive.utf16EndAt(local));
    return indexedText.substring(start, end);
  }

  /** Frozen resolver-ID lexicon and global occurrence postings derived from this snapshot. */
  public M3FileUniverseTokenIndex tokenIndex() {
    return tokenIndex;
  }

  public M3TextMetrics metrics() {
    return indexedText.metrics();
  }

  public M3TextFacts precomputedFacts() {
    return indexedText.precomputedFacts();
  }

  public M3Hash256 sha256(Charset charset) {
    return indexedText.sha256(Objects.requireNonNull(charset, "charset"));
  }

  public long writeUtf8To(WritableByteChannel target) throws IOException {
    return indexedText.writeUtf8To(target);
  }

  private int utf16BoundaryForToken(int globalToken) {
    Objects.checkIndex(globalToken, tokenCount + 1);
    if (globalToken == tokenCount) return utf16Length();
    if (files.isEmpty()) return 0;
    int fileOrdinal = fileOrdinalForToken(globalToken);
    int local = globalToken - tokenStarts[fileOrdinal];
    return Math.addExact(
        utf16Starts[fileOrdinal],
        files.get(fileOrdinal).snapshot().primitiveSnapshot().utf16StartAt(local));
  }

  private long utf8BoundaryForToken(int globalToken) {
    Objects.checkIndex(globalToken, tokenCount + 1);
    if (globalToken == tokenCount) return byteCount;
    if (files.isEmpty()) return 0L;
    int fileOrdinal = fileOrdinalForToken(globalToken);
    int local = globalToken - tokenStarts[fileOrdinal];
    return Math.addExact(
        utf8Starts[fileOrdinal],
        files.get(fileOrdinal).snapshot().sourceFactsSnapshot().utf8Offset(local));
  }

  private int fileOrdinalForToken(int globalToken) {
    Objects.checkIndex(globalToken, tokenCount);
    int low = 0;
    int high = files.size();
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (tokenStarts[middle + 1] <= globalToken) low = middle + 1;
      else high = middle;
    }
    return low;
  }

  /** One file boundary and its already-detached canonical snapshot. */
  public record File(int ordinal, String path, M3FileSnapshot snapshot) {
    public File {
      if (ordinal < 0) throw new IllegalArgumentException("negative Universe ordinal");
      path = Objects.requireNonNull(path, "path");
      snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }
  }
}
