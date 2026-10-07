// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Fully precomputed regex execution image plus compact ASCII DFA projections.
 *
 * <p>This is additive to the v1 portable and v2 execution-image contracts. Restoring the contained
 * {@link MIndexRegexProgram.ExecutionImage} requires no parser or derived-table rebuild, while the
 * compact DFA images let JNI/CUDA consume pre-grouped ASCII equivalence classes without rediscovery.</p>
 */
public record MIndexRegexAcceleratedImage(
    MIndexRegexProgram.ExecutionImage executionImage,
    MIndexCompactAsciiDfaImage anchoredCompactAsciiDfa,
    MIndexCompactAsciiDfaImage searchCompactAsciiDfa) {

  public MIndexRegexAcceleratedImage {
    executionImage = Objects.requireNonNull(executionImage, "executionImage");
    validatePair(
        executionImage.anchoredAsciiDfa(),
        anchoredCompactAsciiDfa,
        "anchored");
    validatePair(
        executionImage.searchAsciiDfa(),
        searchCompactAsciiDfa,
        "search");
  }

  public static MIndexRegexAcceleratedImage from(MIndexRegexProgram program) {
    MIndexRegexProgram checked = Objects.requireNonNull(program, "program");
    MIndexRegexProgram.ExecutionImage execution = checked.executionImage();
    return new MIndexRegexAcceleratedImage(
        execution,
        execution.anchoredAsciiDfa() == null
            ? null
            : MIndexCompactAsciiDfaImage.from(execution.anchoredAsciiDfa()),
        execution.searchAsciiDfa() == null
            ? null
            : MIndexCompactAsciiDfaImage.from(execution.searchAsciiDfa()));
  }

  public MIndexRegexProgram restoreProgram() {
    return MIndexRegexProgram.fromExecutionImage(executionImage);
  }

  public String rootHash() {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      update(digest, "MINDEX_REGEX_ACCELERATED_IMAGE_V3");
      update(digest, executionImage.rootHash());
      update(digest, anchoredCompactAsciiDfa == null ? "" : anchoredCompactAsciiDfa.rootHash());
      update(digest, searchCompactAsciiDfa == null ? "" : searchCompactAsciiDfa.rootHash());
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }

  private static void validatePair(
      MIndexRegexProgram.AsciiDfaImage dense,
      MIndexCompactAsciiDfaImage compact,
      String lane) {
    if ((dense == null) != (compact == null)) {
      throw new IllegalArgumentException(lane + " compact/dense DFA presence differs");
    }
    if (dense != null) compact.requireEquivalent(dense);
  }

  private static void update(MessageDigest digest, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
    digest.update(bytes);
  }
}
