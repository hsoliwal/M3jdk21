// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import com.m3.file.M3FileText;
import java.util.Arrays;
import java.util.Objects;

/**
 * Single-pass mutable admission builder that freezes to an immutable M3File.
 *
 * <p>Frequency, first/last occurrence and per-handle posting links are updated at admission time.
 * Freeze only packs those already-derived lanes into immutable CSR storage; it does not rescan the
 * token lane to rediscover them.</p>
 */
public final class M3FileBuilder {
  private final M3FileLexicon lexicon;
  private int[] localHandles = new int[64];
  private byte[] kinds = new byte[64];
  private int[] utf16Offsets = new int[65];
  private int[] lineStarts = new int[16];
  private int[] nextOccurrence = new int[64];

  private int[] frequencies = new int[64];
  private int[] firstOccurrences = filled(64, -1);
  private int[] lastOccurrences = filled(64, -1);
  private int[] postingHeads = filled(64, -1);
  private int[] postingTails = filled(64, -1);
  private long[] rollingPrefix;
  private long[] rollingPowers;

  private int size;
  private int utf16Length;
  private int lineCount = 1;
  private boolean pendingCarriageReturn;
  private boolean built;

  public M3FileBuilder(M3FileResolver resolver) {
    lexicon = new M3FileLexicon(Objects.requireNonNull(resolver, "resolver"));
    lineStarts[0] = 0;
    Arrays.fill(nextOccurrence, -1);
  }

  public M3FileResolver resolver() {
    return lexicon.resolver();
  }

  public int tokenCount() {
    return size;
  }

  public int uniqueTokenCount() {
    return lexicon.size();
  }

  /** Returns the resolver-native sequence already retained by an admitted local handle. */
  long[] resolverTokenIds(int localHandle) {
    return lexicon.tokenIds(localHandle);
  }

  public int utf16Length() {
    return utf16Length;
  }

  /**
   * Enables ordered admission-time rolling lanes before the first token is added.
   *
   * <p>The default remains lazy to preserve the builder's existing memory footprint. When enabled,
   * {@link M3File#rollingIndex()} reuses these lanes without a second token-lane pass.</p>
   */
  public M3FileBuilder enableRollingIndex() {
    requireOpen();
    if (size != 0) throw new IllegalStateException("rolling index must be enabled before tokens");
    if (rollingPrefix == null) {
      rollingPrefix = new long[localHandles.length + 1];
      rollingPowers = new long[localHandles.length + 1];
      rollingPowers[0] = 1L;
    }
    return this;
  }

  public boolean rollingIndexEnabled() {
    return rollingPrefix != null;
  }

  public M3FileBuilder add(M3FileText token, M3FileTokenKind kind) {
    addAndGetLocalHandle(token, kind);
    return this;
  }

  /**
   * Admits an already-materialized resolver-native token and returns its file-local handle.
   *
   * <p>This is the event-replay counterpart of scalar admission. The token IDs remain owned by
   * the resolver-backed {@link M3FileText}; the builder only updates the existing occurrence,
   * line, posting and optional rolling lanes.</p>
   */
  int addAndGetLocalHandle(M3FileText token, M3FileTokenKind kind) {
    requireOpen();
    Objects.requireNonNull(token, "token");
    Objects.requireNonNull(kind, "kind");
    if (token.resolver() != resolver()) {
      throw new IllegalArgumentException("M3FileText belongs to a different resolver");
    }
    return append(lexicon.intern(token), token, kind);
  }

  /**
   * Admits one scalar token directly from a CharSequence slice.
   *
   * <p>The resolver synchronously canonicalizes the slice; neither the builder nor the frozen file
   * retains the CharSequence object.</p>
   */
  public M3FileBuilder add(
      int languageId, CharSequence token, M3FileTokenKind kind) {
    addAndGetLocalHandle(languageId, token, kind);
    return this;
  }

  /**
   * Package-local admission primitive for stream compilers that need the assigned local handle
   * while deriving sidecar facts from the same token traversal.
   */
  int addAndGetLocalHandle(
      int languageId, CharSequence token, M3FileTokenKind kind) {
    requireOpen();
    Objects.requireNonNull(token, "token");
    Objects.requireNonNull(kind, "kind");
    int handle = lexicon.intern(languageId, token);
    append(handle, token, kind);
    return handle;
  }

  private int append(int handle, CharSequence token, M3FileTokenKind kind) {
    ensureTokenCapacity(size + 1);
    ensureHandleCapacity(handle + 1);

    int position = size;
    localHandles[position] = handle;
    kinds[position] = kind.code();
    utf16Offsets[position] = utf16Length;
    nextOccurrence[position] = -1;

    int tokenLength = token.length();
    scanLineBreaks(token, utf16Length);
    utf16Length = Math.addExact(utf16Length, tokenLength);

    if (rollingPrefix != null) {
      long value = Integer.toUnsignedLong(handle) + 1L;
      rollingPrefix[position + 1] = rollingPrefix[position] * M3FileRollingIndex.BASE + value;
      rollingPowers[position + 1] = rollingPowers[position] * M3FileRollingIndex.BASE;
    }

    if (frequencies[handle] == 0) {
      firstOccurrences[handle] = position;
      postingHeads[handle] = position;
    } else {
      int tail = postingTails[handle];
      if (tail < 0) throw new IllegalStateException("posting tail missing for local handle " + handle);
      nextOccurrence[tail] = position;
    }
    frequencies[handle] = Math.incrementExact(frequencies[handle]);
    lastOccurrences[handle] = position;
    postingTails[handle] = position;

    size++;
    utf16Offsets[size] = utf16Length;
    return handle;
  }

  public M3File build() {
    return build(null);
  }

  M3File build(M3FileSourceFacts sourceFacts) {
    requireOpen();
    built = true;
    if (pendingCarriageReturn) {
      appendLineStart(utf16Length);
      pendingCarriageReturn = false;
    }
    lexicon.freeze();

    int unique = lexicon.size();
    ensureHandleCapacity(unique);
    int[] postingStarts = new int[unique + 1];
    for (int handle = 0; handle < unique; handle++) {
      postingStarts[handle + 1] =
          Math.addExact(postingStarts[handle], frequencies[handle]);
    }

    int[] postings = new int[size];
    for (int handle = 0; handle < unique; handle++) {
      int cursor = postingStarts[handle];
      int position = postingHeads[handle];
      while (position >= 0) {
        postings[cursor++] = position;
        position = nextOccurrence[position];
      }
      if (cursor != postingStarts[handle + 1]) {
        throw new IllegalStateException("posting count mismatch for local handle " + handle);
      }
    }

    return M3File.freezePrecomputed(
        lexicon,
        localHandles,
        kinds,
        utf16Offsets,
        size,
        Arrays.copyOf(lineStarts, lineCount),
        Arrays.copyOf(frequencies, unique),
        Arrays.copyOf(firstOccurrences, unique),
        Arrays.copyOf(lastOccurrences, unique),
        postingStarts,
        postings,
        sourceFacts,
        rollingPrefix == null ? null : Arrays.copyOf(rollingPrefix, size + 1),
        rollingPowers == null ? null : Arrays.copyOf(rollingPowers, size + 1));
  }

  private void scanLineBreaks(CharSequence token, int baseOffset) {
    for (int i = 0; i < token.length(); i++) {
      char unit = token.charAt(i);
      if (pendingCarriageReturn) {
        if (unit == '\n') {
          appendLineStart(Math.addExact(baseOffset, i + 1));
          pendingCarriageReturn = false;
          continue;
        }
        appendLineStart(Math.addExact(baseOffset, i));
        pendingCarriageReturn = false;
      }

      if (unit == '\r') {
        pendingCarriageReturn = true;
      } else if (unit == '\n') {
        appendLineStart(Math.addExact(baseOffset, i + 1));
      }
    }
  }

  private void appendLineStart(int offset) {
    if (lineCount == lineStarts.length) {
      lineStarts = Arrays.copyOf(lineStarts, grownLength(lineStarts.length, lineCount + 1));
    }
    lineStarts[lineCount++] = offset;
  }

  private void ensureTokenCapacity(int required) {
    if (required <= localHandles.length) return;
    int old = localHandles.length;
    int next = grownLength(old, required);
    localHandles = Arrays.copyOf(localHandles, next);
    kinds = Arrays.copyOf(kinds, next);
    utf16Offsets = Arrays.copyOf(utf16Offsets, next + 1);
    nextOccurrence = Arrays.copyOf(nextOccurrence, next);
    Arrays.fill(nextOccurrence, old, next, -1);
    if (rollingPrefix != null) {
      rollingPrefix = Arrays.copyOf(rollingPrefix, next + 1);
      rollingPowers = Arrays.copyOf(rollingPowers, next + 1);
    }
  }

  private void ensureHandleCapacity(int required) {
    if (required <= frequencies.length) return;
    int old = frequencies.length;
    int next = grownLength(old, required);
    frequencies = Arrays.copyOf(frequencies, next);
    firstOccurrences = Arrays.copyOf(firstOccurrences, next);
    lastOccurrences = Arrays.copyOf(lastOccurrences, next);
    postingHeads = Arrays.copyOf(postingHeads, next);
    postingTails = Arrays.copyOf(postingTails, next);
    Arrays.fill(firstOccurrences, old, next, -1);
    Arrays.fill(lastOccurrences, old, next, -1);
    Arrays.fill(postingHeads, old, next, -1);
    Arrays.fill(postingTails, old, next, -1);
  }

  private void requireOpen() {
    if (built) throw new IllegalStateException("M3FileBuilder already built");
  }

  private static int[] filled(int length, int value) {
    int[] values = new int[length];
    Arrays.fill(values, value);
    return values;
  }

  private static int grownLength(int oldLength, int required) {
    int next = oldLength + (oldLength >>> 1) + 1;
    return Math.max(required, next);
  }
}
