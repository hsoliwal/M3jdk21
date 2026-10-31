// Copyright 2026 Hitesh Soliwal and contributors
// Mapped pinned donor proof; see DONOR_PROBES.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileDictionary;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Executable proof for admission-time rolling lanes and the unchanged lazy default. */
public final class M3FileRollingPrecomputeProof {
  private M3FileRollingPrecomputeProof() {}

  public static void main(String[] args) throws Exception {
    M3FileDictionary resolver = new M3FileDictionary();
    M3File admitted = buildWithRollingLanes(resolver);
    verifyRollingQueries(admitted, "builder");
    verifyCapacityGrowth(resolver);

    Path source = Files.createTempFile("mindex-file-rolling-precompute-", ".txt");
    try {
      Files.writeString(source, "x y z x y z q", StandardCharsets.UTF_8);
      M3File streamed =
          M3FileStreamIndexer.plainText(
              source, resolver, 0, M3FileStreamEvents.noOp(), true);
      require(streamed.hasSourceFacts(), "stream admission did not retain source facts");
      require(streamed.hasPrecomputedRollingIndex(), "stream admission did not precompute lanes");
      require("x y z x y z q".equals(streamed.materialize()), "stream materialization changed");
      verifyRollingQueries(streamed, "stream");
    } finally {
      Files.deleteIfExists(source);
    }
    require(!Files.exists(source), "temporary source was not deleted");

    M3File eager =
        M3FileTokenizer.plainText(resolver, 0, "x y z x y z q", true);
    verifyRollingQueries(eager, "charSequence");

    M3File lazy = M3FileTokenizer.plainText(resolver, 0, "x y z");
    require(!lazy.hasPrecomputedRollingIndex(), "default tokenizer changed to eager rolling storage");
    require(lazy.rollingIndex().retainedPayloadBytes() > 0L, "lazy rolling payload was not allocated");

    System.out.println(
        "MINDEX_FILE_ROLLING_PRECOMPUTE_PASS"
            + "|builder=1|resized=1|stream=1|charSequence=1|shared=1|exactConfirmation=1|defaultLazy=1|temporaryClean=1");
  }

  private static M3File buildWithRollingLanes(M3FileDictionary resolver) {
    M3FileBuilder builder = new M3FileBuilder(resolver).enableRollingIndex();
    add(builder, "x", M3FileTokenKind.WORD);
    add(builder, " ", M3FileTokenKind.WHITESPACE);
    add(builder, "y", M3FileTokenKind.WORD);
    add(builder, " ", M3FileTokenKind.WHITESPACE);
    add(builder, "z", M3FileTokenKind.WORD);
    add(builder, " ", M3FileTokenKind.WHITESPACE);
    add(builder, "x", M3FileTokenKind.WORD);
    add(builder, " ", M3FileTokenKind.WHITESPACE);
    add(builder, "y", M3FileTokenKind.WORD);
    add(builder, " ", M3FileTokenKind.WHITESPACE);
    add(builder, "z", M3FileTokenKind.WORD);
    add(builder, " ", M3FileTokenKind.WHITESPACE);
    add(builder, "q", M3FileTokenKind.WORD);
    return builder.build();
  }

  private static void add(
      M3FileBuilder builder, String token, M3FileTokenKind kind) {
    builder.add(0, token, kind);
  }

  private static void verifyCapacityGrowth(M3FileDictionary resolver) {
    M3FileBuilder builder = new M3FileBuilder(resolver).enableRollingIndex();
    for (int index = 0; index < 96; index++) {
      add(
          builder,
          (index & 1) == 0 ? "x" : " ",
          (index & 1) == 0 ? M3FileTokenKind.WORD : M3FileTokenKind.WHITESPACE);
    }
    M3File file = builder.build();
    require(file.tokenCount() == 96, "rolling lane growth changed token count");
    require(file.rollingIndex().sameTokens(0, 64, 32), "grown rolling lanes changed values");
    require(file.rollingIndex().retainedPayloadBytes() == 0L, "grown lanes were not shared");
  }

  private static void verifyRollingQueries(M3File file, String label) {
    require(file.hasPrecomputedRollingIndex(), label + " lanes missing");
    M3FileRollingIndex rolling = file.rollingIndex();
    require(rolling.retainedPayloadBytes() == 0L, label + " rolling view duplicated shared lanes");
    require(rolling.sameTokens(0, 6, 5), label + " equal token spans rejected");
    require(!rolling.sameTokens(0, 1, 1), label + " unequal token spans accepted");
    require(
        rolling.hash(0, 5) == rolling.hash(6, 11),
        label + " equal token spans produced different candidates");
    require(rolling.indexOf(file.span(0, 5), 1) == 6, label + " exact-confirming search missed span");
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
