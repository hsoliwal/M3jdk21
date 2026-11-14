// SPDX-License-Identifier: Apache-2.0
// Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.llm.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, level-indexed learned codes associated with an exact model input.
 *
 * <p>Codes are indices in versioned numerical codebooks. They are neither canonical text IDs nor
 * lexicon concepts. The exact source remains in {@link M3ModelInput}; this class makes no
 * lossless-decoding, model-accuracy or source-substitution claim. Flat primitive lanes retain no
 * per-code objects. Caller-owned results can be discarded independently of canonical text.</p>
 */
public final class M3NcmCodes {
  private final M3ModelInput input;
  private final String codebookRoot;
  private final int[] levelOffsets;
  private final int[] codebookSizes;
  private final int[] codes;
  private final String rootHash;

  private M3NcmCodes(
      M3ModelInput input, String codebookRoot, int[] levelOffsets,
      int[] codebookSizes, int[] codes, long maxRetainedBytes) {
    this.input = Objects.requireNonNull(input, "input");
    this.codebookRoot = M3ModelInput.requireRoot(codebookRoot, "codebookRoot");
    Objects.requireNonNull(levelOffsets, "levelOffsets");
    Objects.requireNonNull(codebookSizes, "codebookSizes");
    Objects.requireNonNull(codes, "codes");
    long arrayBytes = Math.multiplyExact((long) Integer.BYTES,
        Math.addExact((long) levelOffsets.length,
            Math.addExact((long) codebookSizes.length, (long) codes.length)));
    if (maxRetainedBytes < 0 || Math.addExact(arrayBytes, input.retainedBytes()) > maxRetainedBytes) {
      throw new IllegalArgumentException("NCM metadata exceeds the supplied byte budget");
    }
    if (codebookSizes.length == 0 || levelOffsets.length != (long) codebookSizes.length + 1L) {
      throw new IllegalArgumentException("one offset interval is required per codebook level");
    }
    // Copy first so later caller mutation cannot change validation, identity or published codes.
    this.levelOffsets = levelOffsets.clone();
    this.codebookSizes = codebookSizes.clone();
    this.codes = codes.clone();
    validate();
    M3ModelInput.ExactHash hash =
        new M3ModelInput.ExactHash("SYNEXIA_MINDEX_NCM_CODES_V1");
    hash.text(input.rootHash());
    hash.text(this.codebookRoot);
    hash.integer(this.codebookSizes.length);
    for (int value : this.levelOffsets) hash.integer(value);
    for (int value : this.codebookSizes) hash.integer(value);
    hash.integer(this.codes.length);
    for (int value : this.codes) hash.integer(value);
    rootHash = hash.finish();
  }

  public static M3NcmCodes of(
      M3ModelInput input, String codebookRoot, int[] levelOffsets,
      int[] codebookSizes, int[] codes) {
    return of(input, codebookRoot, levelOffsets, codebookSizes, codes, Long.MAX_VALUE);
  }

  /** Explicit admission budget for owned code lanes; the input String remains shared. */
  public static M3NcmCodes of(
      M3ModelInput input, String codebookRoot, int[] levelOffsets,
      int[] codebookSizes, int[] codes, long maxRetainedBytes) {
    return new M3NcmCodes(
        input, codebookRoot, levelOffsets, codebookSizes, codes, maxRetainedBytes);
  }

  public M3ModelInput input() { return input; }
  public String codebookRoot() { return codebookRoot; }
  public String rootHash() { return rootHash; }
  public int levelCount() { return codebookSizes.length; }
  public int codeCount() { return codes.length; }

  public int levelLength(int level) {
    int index = Objects.checkIndex(level, levelCount());
    return levelOffsets[index + 1] - levelOffsets[index];
  }

  public int codeAt(int level, int position) {
    int index = Objects.checkIndex(level, levelCount());
    return codes[levelOffsets[index] + Objects.checkIndex(position, levelLength(index))];
  }

  public int codebookSize(int level) {
    return codebookSizes[Objects.checkIndex(level, levelCount())];
  }

  public int[] copyCodes() { return codes.clone(); }
  public int[] copyLevelOffsets() { return levelOffsets.clone(); }
  public int[] copyCodebookSizes() { return codebookSizes.clone(); }

  /** Owned primitive arrays; excludes shared text payload, digest Strings and object headers. */
  public long retainedBytes() {
    return input.retainedBytes() + (long) Integer.BYTES
        * ((long) levelOffsets.length + codebookSizes.length + codes.length);
  }

  /** Input matching uses exact text and namespace validation after digest screening. */
  public boolean matches(M3ModelInput expectedInput, String expectedCodebookRoot) {
    return codebookRoot.equals(expectedCodebookRoot) && input.matches(expectedInput);
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof M3NcmCodes value
        && rootHash.equals(value.rootHash)
        && matches(value.input, value.codebookRoot)
        && Arrays.equals(levelOffsets, value.levelOffsets)
        && Arrays.equals(codebookSizes, value.codebookSizes)
        && Arrays.equals(codes, value.codes);
  }

  @Override
  public int hashCode() { return rootHash.hashCode(); }

  private void validate() {
    if (levelOffsets[0] != 0 || levelOffsets[levelCount()] != codes.length) {
      throw new IllegalArgumentException("level offsets must cover the entire code array");
    }
    int previousLength = 0;
    for (int level = 0; level < levelCount(); level++) {
      int start = levelOffsets[level];
      int end = levelOffsets[level + 1];
      if (start < 0 || end <= start || end > codes.length) {
        throw new IllegalArgumentException("each level must have a nonempty bounded code interval");
      }
      int length = end - start;
      if (length < previousLength) {
        throw new IllegalArgumentException("NCM level lengths must be nondecreasing");
      }
      previousLength = length;
      int size = codebookSizes[level];
      if (size <= 0) throw new IllegalArgumentException("codebooks must be nonempty");
      for (int index = start; index < end; index++) {
        if (codes[index] < 0 || codes[index] >= size) {
          throw new IllegalArgumentException("code index is outside its level's codebook");
        }
      }
    }
  }
}
