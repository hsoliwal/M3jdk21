// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;
import com.m3.file.M3FileTextBuilder;
import com.m3.file.M3Hash256;
import java.nio.charset.Charset;

import com.m3.file.M3FileText;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Immutable file image as a compact stream of file-local M3FileText handles.
 *
 * <p>Canonical text remains owned by the existing M3FileResolver/M3FileText machinery. A file
 * stores one copy of each unique M3FileText sequence in its local lexicon, then represents every
 * occurrence with an adaptive 8/16/32-bit local handle. Freeze also builds primitive CSR postings,
 * first/last occurrence lanes, UTF-16 prefix offsets and line starts.</p>
 */
public final class M3File {
  private final M3FileLexicon lexicon;
  private final M3FileTokenStore tokens;
  private final byte[] kinds;
  private final int[] utf16Offsets;
  private final int[] lineStarts;
  private final int[] frequencies;
  private final int[] firstOccurrences;
  private final int[] lastOccurrences;
  private final int[] postingStarts;
  private final int[] postings;
  private final M3FileSourceFacts sourceFacts;
  private final long[] rollingPrefix;
  private final long[] rollingPowers;
  private final M3FileText indexedText;

  private M3File(
      M3FileLexicon lexicon,
      M3FileTokenStore tokens,
      byte[] kinds,
      int[] utf16Offsets,
      int[] lineStarts,
      int[] frequencies,
      int[] firstOccurrences,
      int[] lastOccurrences,
      int[] postingStarts,
      int[] postings,
      M3FileSourceFacts sourceFacts,
      M3FileText indexedText,
      long[] rollingPrefix,
      long[] rollingPowers) {
    this.lexicon = lexicon;
    this.tokens = tokens;
    this.kinds = kinds;
    this.utf16Offsets = utf16Offsets;
    this.lineStarts = lineStarts;
    this.frequencies = frequencies;
    this.firstOccurrences = firstOccurrences;
    this.lastOccurrences = lastOccurrences;
    this.postingStarts = postingStarts;
    this.postings = postings;
    this.sourceFacts = sourceFacts;
    this.indexedText = Objects.requireNonNull(indexedText, "indexedText");
    this.rollingPrefix = rollingPrefix;
    this.rollingPowers = rollingPowers;
  }

  static M3File freeze(
      M3FileLexicon lexicon,
      int[] localHandles,
      byte[] kinds,
      int[] utf16Offsets,
      int size,
      int[] lineStarts) {
    return freeze(lexicon, localHandles, kinds, utf16Offsets, size, lineStarts, null);
  }

  static M3File freeze(
      M3FileLexicon lexicon,
      int[] localHandles,
      byte[] kinds,
      int[] utf16Offsets,
      int size,
      int[] lineStarts,
      M3FileSourceFacts sourceFacts) {
    Objects.requireNonNull(lexicon, "lexicon");
    Objects.requireNonNull(localHandles, "localHandles");
    Objects.checkFromIndexSize(0, size, localHandles.length);
    if (!lexicon.isFrozen()) throw new IllegalStateException("file lexicon must be frozen");

    int[] frequencies = new int[lexicon.size()];
    int[] first = new int[lexicon.size()];
    int[] last = new int[lexicon.size()];
    Arrays.fill(first, -1);
    Arrays.fill(last, -1);
    for (int position = 0; position < size; position++) {
      int handle = Objects.checkIndex(localHandles[position], lexicon.size());
      frequencies[handle]++;
      if (first[handle] < 0) first[handle] = position;
      last[handle] = position;
    }

    int[] postingStarts = new int[lexicon.size() + 1];
    for (int handle = 0; handle < lexicon.size(); handle++) {
      postingStarts[handle + 1] =
          Math.addExact(postingStarts[handle], frequencies[handle]);
    }
    int[] postings = new int[size];
    int[] cursors = Arrays.copyOf(postingStarts, lexicon.size());
    for (int position = 0; position < size; position++) {
      int handle = localHandles[position];
      postings[cursors[handle]++] = position;
    }

    return freezePrecomputed(
        lexicon,
        localHandles,
        kinds,
        utf16Offsets,
        size,
        lineStarts,
        frequencies,
        first,
        last,
        postingStarts,
        postings,
        sourceFacts,
        null,
        null);
  }

  /**
   * Freezes admission-time occurrence facts without rescanning the token lane.
   *
   * <p>The caller has already computed frequency/first/last and posting membership while tokens
   * arrived. This method validates and packs those facts into the existing immutable representation.</p>
   */
  static M3File freezePrecomputed(
      M3FileLexicon lexicon,
      int[] localHandles,
      byte[] kinds,
      int[] utf16Offsets,
      int size,
      int[] lineStarts,
      int[] frequencies,
      int[] firstOccurrences,
      int[] lastOccurrences,
      int[] postingStarts,
      int[] postings,
      M3FileSourceFacts sourceFacts) {
    return freezePrecomputed(
        lexicon,
        localHandles,
        kinds,
        utf16Offsets,
        size,
        lineStarts,
        frequencies,
        firstOccurrences,
        lastOccurrences,
        postingStarts,
        postings,
        sourceFacts,
        null,
        null);
  }

  static M3File freezePrecomputed(
      M3FileLexicon lexicon,
      int[] localHandles,
      byte[] kinds,
      int[] utf16Offsets,
      int size,
      int[] lineStarts,
      int[] frequencies,
      int[] firstOccurrences,
      int[] lastOccurrences,
      int[] postingStarts,
      int[] postings,
      M3FileSourceFacts sourceFacts,
      long[] rollingPrefix,
      long[] rollingPowers) {
    Objects.requireNonNull(lexicon, "lexicon");
    Objects.requireNonNull(localHandles, "localHandles");
    Objects.requireNonNull(kinds, "kinds");
    Objects.requireNonNull(utf16Offsets, "utf16Offsets");
    Objects.requireNonNull(lineStarts, "lineStarts");
    Objects.requireNonNull(frequencies, "frequencies");
    Objects.requireNonNull(firstOccurrences, "firstOccurrences");
    Objects.requireNonNull(lastOccurrences, "lastOccurrences");
    Objects.requireNonNull(postingStarts, "postingStarts");
    Objects.requireNonNull(postings, "postings");
    Objects.checkFromIndexSize(0, size, localHandles.length);
    Objects.checkFromIndexSize(0, size, kinds.length);
    Objects.checkFromIndexSize(0, size + 1, utf16Offsets.length);
    if (!lexicon.isFrozen()) throw new IllegalStateException("file lexicon must be frozen");

    int unique = lexicon.size();
    if (frequencies.length != unique
        || firstOccurrences.length != unique
        || lastOccurrences.length != unique
        || postingStarts.length != unique + 1
        || postings.length != size) {
      throw new IllegalArgumentException("precomputed M3File occurrence lane shape mismatch");
    }
    if (postingStarts[0] != 0 || postingStarts[unique] != size) {
      throw new IllegalArgumentException("precomputed M3File posting sentinels are invalid");
    }

    long[] checkedRollingPrefix = null;
    long[] checkedRollingPowers = null;
    if ((rollingPrefix == null) != (rollingPowers == null)) {
      throw new IllegalArgumentException("rolling lanes must be supplied together");
    }
    if (rollingPrefix != null) {
      if (rollingPrefix.length != size + 1 || rollingPowers.length != size + 1) {
        throw new IllegalArgumentException("precomputed M3File rolling lane shape mismatch");
      }
      long prefix = 0L;
      long power = 1L;
      for (int position = 0; position <= size; position++) {
        if (rollingPrefix[position] != prefix || rollingPowers[position] != power) {
          throw new IllegalArgumentException("precomputed M3File rolling lane mismatch");
        }
        if (position == size) break;
        long token = Integer.toUnsignedLong(localHandles[position]) + 1L;
        prefix = prefix * M3FileRollingIndex.BASE + token;
        power *= M3FileRollingIndex.BASE;
      }
      checkedRollingPrefix = Arrays.copyOf(rollingPrefix, size + 1);
      checkedRollingPowers = Arrays.copyOf(rollingPowers, size + 1);
    }

    for (int handle = 0; handle < unique; handle++) {
      int startPosting = postingStarts[handle];
      int endPosting = postingStarts[handle + 1];
      if (startPosting < 0 || endPosting < startPosting || endPosting > size) {
        throw new IllegalArgumentException("precomputed M3File posting range is invalid");
      }
      int frequency = frequencies[handle];
      if (frequency != endPosting - startPosting) {
        throw new IllegalArgumentException("precomputed M3File frequency/posting mismatch");
      }
      if (frequency == 0) {
        if (firstOccurrences[handle] != -1 || lastOccurrences[handle] != -1) {
          throw new IllegalArgumentException("empty precomputed posting row has first/last");
        }
      } else {
        if (firstOccurrences[handle] != postings[startPosting]
            || lastOccurrences[handle] != postings[endPosting - 1]) {
          throw new IllegalArgumentException("precomputed M3File first/last mismatch");
        }
        int previous = -1;
        for (int at = startPosting; at < endPosting; at++) {
          int position = postings[at];
          if (position <= previous || position < 0 || position >= size) {
            throw new IllegalArgumentException("precomputed M3File postings are not ordered");
          }
          if (localHandles[position] != handle) {
            throw new IllegalArgumentException(
                "precomputed M3File posting references another local handle");
          }
          previous = position;
        }
      }
    }

    M3FileTokenStore tokens = M3FileTokenStore.freeze(localHandles, size, unique);
    M3FileTextBuilder indexedText = new M3FileTextBuilder(lexicon.resolver(), size);
    for (int position = 0; position < size; position++) {
      indexedText.append(lexicon.valueAt(localHandles[position]));
    }
    return new M3File(
        lexicon,
        tokens,
        Arrays.copyOf(kinds, size),
        Arrays.copyOf(utf16Offsets, size + 1),
        lineStarts.clone(),
        frequencies.clone(),
        firstOccurrences.clone(),
        lastOccurrences.clone(),
        postingStarts.clone(),
        postings.clone(),
        sourceFacts,
        indexedText.build(),
        checkedRollingPrefix,
        checkedRollingPowers);
  }

  public M3FileLexicon lexicon() {
    return lexicon;
  }

  /** Direct heap facade for the immutable flattened lexicon lanes. */
  public M3FileLexiconSnapshot lexiconSnapshot() {
    return lexicon.snapshot();
  }

  /** Return one typed transfer packet over the already-computed file projections. */
  public M3FileSnapshot snapshot() {
    return new M3FileSnapshot(
        lexiconSnapshot(), primitiveSnapshot(), sourceFacts == null ? null : sourceFacts.snapshot());
  }

  /** Whether this file was admitted directly from a physical source stream. */
  public boolean hasSourceFacts() {
    return sourceFacts != null;
  }

  /**
   * Source-side byte, timestamp, attribute and digest facts.
   *
   * @throws IllegalStateException when this file was built from an in-memory token source
   */
  public M3FileSourceFacts sourceFacts() {
    if (sourceFacts == null) {
      throw new IllegalStateException("M3File has no physical-source facts");
    }
    return sourceFacts;
  }

  /** Attach a verified source-facts sidecar without rebuilding token or rolling-index lanes. */
  M3File withSourceFacts(M3FileSourceFacts facts) {
    M3FileSourceFacts checked = Objects.requireNonNull(facts, "facts");
    if (checked.tokenCount() != tokenCount()
        || checked.uniqueTokenCount() != uniqueTokenCount()) {
      throw new IllegalArgumentException("source facts do not match M3File shape");
    }
    return new M3File(
        lexicon,
        tokens,
        kinds,
        utf16Offsets,
        lineStarts,
        frequencies,
        firstOccurrences,
        lastOccurrences,
        postingStarts,
        postings,
        checked,
        indexedText,
        rollingPrefix,
        rollingPowers);
  }

  public int tokenCount() {
    return tokens.size();
  }

  public int uniqueTokenCount() {
    return lexicon.size();
  }

  public int tokenWidthBytes() {
    return tokens.widthBytes();
  }

  public int utf16Length() {
    return utf16Offsets[utf16Offsets.length - 1];
  }

  public int lineCount() {
    return lineStarts.length;
  }

  public int localHandleAt(int tokenIndex) {
    return tokens.get(tokenIndex);
  }

  public M3FileText tokenAt(int tokenIndex) {
    return lexicon.valueAt(localHandleAt(tokenIndex));
  }

  /**
   * Returns the complete occurrence stream as resolver-owned indexed text.
   *
   * <p>The result composes retained token atoms; it does not reopen source bytes or create a
   * second spelling array.</p>
   */
  public M3FileText asM3String() {
    return indexedText;
  }

  /** Computes the complete indexed-text digest through the file text descriptor over ordinary String owners. */
  public M3Hash256 sha256(Charset charset) {
    return asM3String().sha256(Objects.requireNonNull(charset, "charset"));
  }

  /** Number of resolver-native IDs that form the canonical token at this file position. */
  public int resolverTokenCountAt(int tokenIndex) {
    return lexicon.tokenCount(localHandleAt(tokenIndex));
  }

  /** Resolver-native ID without materializing an M3FileText wrapper. */
  public long resolverTokenIdAt(int tokenIndex, int ordinal) {
    return lexicon.tokenIdAt(localHandleAt(tokenIndex), ordinal);
  }

  public M3FileTokenKind tokenKindAt(int tokenIndex) {
    return M3FileTokenKind.fromCode(
        kinds[Objects.checkIndex(tokenIndex, kinds.length)]);
  }

  /** Joins the retained one-pass token lanes into the existing immutable stream-event record. */
  public M3FileStreamEvents.Token tokenSnapshot(int tokenIndex) {
    int index = Objects.checkIndex(tokenIndex, tokenCount());
    M3FileSourceFacts facts = sourceFacts();
    int localHandle = localHandleAt(index);
    return new M3FileStreamEvents.Token(
        index,
        localHandle,
        tokenKindAt(index),
        utf16Offset(index),
        utf16Offset(index + 1),
        facts.utf8Offset(index),
        facts.utf8Offset(index + 1),
        facts.codePointOffset(index),
        facts.codePointOffset(index + 1),
        facts.tokenSha256ForLocalHandle(localHandle).bytes());
  }

  public int utf16Offset(int tokenBoundary) {
    if (tokenBoundary < 0 || tokenBoundary > tokenCount()) {
      throw new IndexOutOfBoundsException("token boundary " + tokenBoundary);
    }
    return utf16Offsets[tokenBoundary];
  }

  /** Greatest token boundary at or before the requested UTF-16 source coordinate. */
  public int tokenBoundaryAtOrBeforeUtf16(int utf16Offset) {
    if (utf16Offset < 0 || utf16Offset > utf16Length()) {
      throw new IndexOutOfBoundsException("UTF-16 offset " + utf16Offset);
    }
    return upperBound(utf16Offsets, 0, utf16Offsets.length, utf16Offset) - 1;
  }

  /** Smallest token boundary at or after the requested UTF-16 source coordinate. */
  public int tokenBoundaryAtOrAfterUtf16(int utf16Offset) {
    if (utf16Offset < 0 || utf16Offset > utf16Length()) {
      throw new IndexOutOfBoundsException("UTF-16 offset " + utf16Offset);
    }
    return lowerBound(utf16Offsets, 0, utf16Offsets.length, utf16Offset);
  }

  public int lineStartUtf16(int lineIndex) {
    return lineStarts[Objects.checkIndex(lineIndex, lineStarts.length)];
  }

  public int lineOfUtf16Offset(int utf16Offset) {
    if (utf16Offset < 0 || utf16Offset > utf16Length()) {
      throw new IndexOutOfBoundsException("UTF-16 offset " + utf16Offset);
    }
    int low = 0;
    int high = lineStarts.length;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (lineStarts[middle] <= utf16Offset) low = middle + 1;
      else high = middle;
    }
    return Math.max(0, low - 1);
  }

  public int localHandle(M3FileText value) {
    return lexicon.find(Objects.requireNonNull(value, "value"));
  }

  /** Scalar lookup without allocating an M3FileText query wrapper. */
  public int localHandle(int languageId, CharSequence token) {
    return lexicon.find(languageId, Objects.requireNonNull(token, "token"));
  }

  public int occurrenceCount(int localHandle) {
    return frequencies[requireLocalHandle(localHandle)];
  }

  public int firstOccurrence(int localHandle) {
    return firstOccurrences[requireLocalHandle(localHandle)];
  }

  public int lastOccurrence(int localHandle) {
    return lastOccurrences[requireLocalHandle(localHandle)];
  }

  public int occurrenceCount(M3FileText value) {
    int handle = localHandle(value);
    return handle < 0 ? 0 : occurrenceCount(handle);
  }

  public int firstOccurrence(M3FileText value) {
    int handle = localHandle(value);
    return handle < 0 ? -1 : firstOccurrence(handle);
  }

  public int lastOccurrence(M3FileText value) {
    int handle = localHandle(value);
    return handle < 0 ? -1 : lastOccurrence(handle);
  }

  public int firstOccurrence(int localHandle, int fromToken) {
    int handle = requireLocalHandle(localHandle);
    if (fromToken < 0 || fromToken > tokenCount()) {
      throw new IndexOutOfBoundsException("from token " + fromToken);
    }
    int from = postingStarts[handle];
    int to = postingStarts[handle + 1];
    int at = lowerBound(postings, from, to, fromToken);
    return at == to ? -1 : postings[at];
  }

  public int firstOccurrence(M3FileText value, int fromToken) {
    int handle = localHandle(value);
    return handle < 0 ? -1 : firstOccurrence(handle, fromToken);
  }

  public int lastOccurrence(int localHandle, int beforeTokenExclusive) {
    int handle = requireLocalHandle(localHandle);
    if (beforeTokenExclusive < 0 || beforeTokenExclusive > tokenCount()) {
      throw new IndexOutOfBoundsException("before token " + beforeTokenExclusive);
    }
    int from = postingStarts[handle];
    int to = postingStarts[handle + 1];
    int at = lowerBound(postings, from, to, beforeTokenExclusive) - 1;
    return at < from ? -1 : postings[at];
  }

  public int lastOccurrence(M3FileText value, int beforeTokenExclusive) {
    int handle = localHandle(value);
    return handle < 0 ? -1 : lastOccurrence(handle, beforeTokenExclusive);
  }

  /** Exact posting position by local handle and posting ordinal. */
  public int occurrenceAt(int localHandle, int ordinal) {
    int handle = requireLocalHandle(localHandle);
    int start = postingStarts[handle];
    int count = postingStarts[handle + 1] - start;
    return postings[start + Objects.checkIndex(ordinal, count)];
  }

  public void forEachOccurrence(int localHandle, IntConsumer consumer) {
    Objects.requireNonNull(consumer, "consumer");
    int handle = requireLocalHandle(localHandle);
    for (int at = postingStarts[handle]; at < postingStarts[handle + 1]; at++) {
      consumer.accept(postings[at]);
    }
  }

  public void forEachOccurrence(M3FileText value, IntConsumer consumer) {
    Objects.requireNonNull(consumer, "consumer");
    int handle = localHandle(value);
    if (handle >= 0) forEachOccurrence(handle, consumer);
  }

  public M3FilePattern preparePattern(M3FileText... values) {
    return M3FilePattern.prepare(this, values);
  }

  public int indexOf(M3FilePattern pattern) {
    return indexOf(pattern, 0);
  }

  /** Deterministic KMP search over local integer handles. */
  public int indexOf(M3FilePattern pattern, int fromToken) {
    requirePattern(pattern);
    if (fromToken < 0 || fromToken > tokenCount()) {
      throw new IndexOutOfBoundsException("from token " + fromToken);
    }
    if (!pattern.presentInFile()) return -1;
    if (pattern.length() == 0) return fromToken;

    int matched = 0;
    for (int at = fromToken; at < tokenCount(); at++) {
      int value = tokens.get(at);
      while (matched > 0 && value != pattern.handleAt(matched)) {
        matched = pattern.failureAt(matched - 1);
      }
      if (value == pattern.handleAt(matched)) matched++;
      if (matched == pattern.length()) return at - pattern.length() + 1;
    }
    return -1;
  }

  /**
   * Finds up to maxMatches overlapping matches without boxing. Empty patterns yield boundaries.
   */
  public int[] findAll(M3FilePattern pattern, int maxMatches) {
    requirePattern(pattern);
    if (maxMatches < 0) throw new IllegalArgumentException("negative maxMatches");
    if (maxMatches == 0 || !pattern.presentInFile()) return new int[0];
    int[] matches = new int[Math.min(maxMatches, Math.max(1, tokenCount()))];
    int count = 0;

    if (pattern.length() == 0) {
      int limit = Math.min(maxMatches, tokenCount() + 1);
      matches = new int[limit];
      for (int i = 0; i < limit; i++) matches[i] = i;
      return matches;
    }

    int matched = 0;
    for (int at = 0; at < tokenCount() && count < maxMatches; at++) {
      int value = tokens.get(at);
      while (matched > 0 && value != pattern.handleAt(matched)) {
        matched = pattern.failureAt(matched - 1);
      }
      if (value == pattern.handleAt(matched)) matched++;
      if (matched == pattern.length()) {
        if (count == matches.length) {
          matches =
              Arrays.copyOf(
                  matches,
                  Math.min(
                      maxMatches, matches.length + (matches.length >>> 1) + 1));
        }
        matches[count++] = at - pattern.length() + 1;
        matched = pattern.failureAt(matched - 1);
      }
    }
    return Arrays.copyOf(matches, count);
  }

  public M3FileSpan span(int startToken, int endToken) {
    return new M3FileSpan(this, startToken, endToken);
  }

  /**
   * Return one defensive export of the already-frozen primitive structure lanes.
   *
   * <p>The snapshot is an opt-in JNI/structural-serializer boundary. It does not retain source
   * text, create another storage authority, or alter MIDX/MIDXEVT2 bytes.</p>
   */
  public M3FilePrimitiveSnapshot primitiveSnapshot() {
    int[] localHandles = new int[tokens.size()];
    for (int position = 0; position < localHandles.length; position++) {
      localHandles[position] = tokens.get(position);
    }
    return new M3FilePrimitiveSnapshot(
        localHandles,
        kinds,
        utf16Offsets,
        lineStarts,
        frequencies,
        firstOccurrences,
        lastOccurrences,
        postingStarts,
        postings,
        rollingPrefix,
        rollingPowers);
  }

  /** Builds an optional O(1)-window rolling fingerprint index with exact collision confirmation. */
  public M3FileRollingIndex rollingIndex() {
    return new M3FileRollingIndex(this);
  }

  /** Whether the rolling lanes were derived during ordered admission rather than on query. */
  public boolean hasPrecomputedRollingIndex() {
    return rollingPrefix != null;
  }

  long[] precomputedRollingPrefix() {
    return rollingPrefix;
  }

  long[] precomputedRollingPowers() {
    return rollingPowers;
  }

  /** Compiles many prepared patterns into one primitive Aho-Corasick matcher. */
  public M3FileMultiPattern prepareMultiPattern(M3FilePattern... patterns) {
    return M3FileMultiPattern.compile(this, patterns);
  }

  /** Optional primitive frequency ranking for promotion, diagnostics and top-K queries. */
  public M3FileFrequencyIndex frequencyIndex() {
    return new M3FileFrequencyIndex(this);
  }

  /** Optional primitive token-kind posting index. */
  public M3FileKindIndex kindIndex() {
    return new M3FileKindIndex(this);
  }

  /** Zero-copy ordinal projection selecting exactly the requested token kinds. */
  public M3FileProjection projection(M3FileTokenKind... selectedKinds) {
    return M3FileProjection.ofKinds(this, selectedKinds);
  }

  /** Semantic tokens only: words, numbers, quoted values, symbols and other tokens. */
  public M3FileProjection semanticProjection() {
    return M3FileProjection.semantic(this);
  }

  /** Whitespace, newlines and comments only. */
  public M3FileProjection triviaProjection() {
    return M3FileProjection.trivia(this);
  }

  public void appendRangeTo(int startToken, int endToken, StringBuilder out) {
    Objects.requireNonNull(out, "out");
    Objects.checkFromToIndex(startToken, endToken, tokenCount());
    for (int position = startToken; position < endToken; position++) {
      lexicon.appendTo(tokens.get(position), out);
    }
  }

  public String materialize() {
    StringBuilder out = new StringBuilder(utf16Length());
    appendRangeTo(0, tokenCount(), out);
    return out.toString();
  }

  public long retainedPayloadBytes() {
    long retained =
        lexicon.retainedPayloadBytes()
            + tokens.retainedPayloadBytes()
            + kinds.length
            + Integer.BYTES
                * (long)
                    (utf16Offsets.length
                        + lineStarts.length
                        + frequencies.length
                        + firstOccurrences.length
                        + lastOccurrences.length
                        + postingStarts.length
                        + postings.length);
    if (rollingPrefix != null) {
      retained =
          Math.addExact(
              retained,
              Long.BYTES * (long) (rollingPrefix.length + rollingPowers.length));
    }
    return sourceFacts == null
        ? retained
        : Math.addExact(retained, sourceFacts.retainedPayloadBytes());
  }

  @Override
  public String toString() {
    return materialize();
  }

  private void requirePattern(M3FilePattern pattern) {
    Objects.requireNonNull(pattern, "pattern");
    if (pattern.file() != this) {
      throw new IllegalArgumentException(
          "prepared pattern belongs to a different M3File");
    }
  }

  private int requireLocalHandle(int localHandle) {
    return Objects.checkIndex(localHandle, lexicon.size());
  }

  private static int lowerBound(int[] values, int from, int to, int target) {
    int low = from;
    int high = to;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (values[middle] < target) low = middle + 1;
      else high = middle;
    }
    return low;
  }

  private static int upperBound(int[] values, int from, int to, int target) {
    int low = from;
    int high = to;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (values[middle] <= target) low = middle + 1;
      else high = middle;
    }
    return low;
  }
}
