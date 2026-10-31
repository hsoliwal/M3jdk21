// Copyright 2026 Hitesh Soliwal and contributors
// Mapped pinned donor proof; see DONOR_PROBES.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileDictionary;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Executable proof that MIDXEVT2 rehydrates the complete file snapshot without source reread. */
public final class M3FileEventReaderRehydrateProof {
  private M3FileEventReaderRehydrateProof() {}

  public static void main(String[] args) throws Exception {
    Path source = Files.createTempFile("mindex-event-rehydrate-", ".java");
    try {
      Files.writeString(
          source,
          "alpha " + "\u00CE\u00B1" + " alpha\r\n// note\nbeta + 42",
          StandardCharsets.UTF_8);
      M3FileDictionary resolver = new M3FileDictionary();
      ByteArrayOutputStream encoded = new ByteArrayOutputStream();
      M3File expected;
      try (M3FileEventWriter writer = new M3FileEventWriter(encoded)) {
        expected = M3FileStreamIndexer.codeLike(source, resolver, 7, writer, true);
        require(writer.isFinished(), "stream writer did not finish");
      }
      byte[] eventBytes = encoded.toByteArray();
      Files.delete(source);

      M3File actual =
          M3FileEventReader.readM3File(
              new ByteArrayInputStream(eventBytes), resolver, true);
      require(expected.materialize().equals(actual.materialize()), "materialized text changed");
      require(expected.tokenCount() == actual.tokenCount(), "token count changed");
      require(expected.uniqueTokenCount() == actual.uniqueTokenCount(), "lexicon count changed");
      require(expected.lineCount() == actual.lineCount(), "line count changed");
      require(
          Arrays.equals(
              expected.snapshot().lexiconSnapshot().tokenIds(),
              actual.snapshot().lexiconSnapshot().tokenIds()),
          "lexicon IDs changed");
      require(
          Arrays.equals(
              expected.snapshot().primitiveSnapshot().localHandles(),
              actual.snapshot().primitiveSnapshot().localHandles()),
          "occurrence handles changed");
      require(
          Arrays.equals(
              expected.snapshot().primitiveSnapshot().rollingPrefix(),
              actual.snapshot().primitiveSnapshot().rollingPrefix()),
          "rolling prefix lane changed");
      require(
          Arrays.equals(
              expected.sourceFacts().byteFacts().byteHistogram(),
              actual.sourceFacts().byteFacts().byteHistogram()),
          "byte histogram changed");
      require(
          expected.sourceFacts().contentSha256().equals(actual.sourceFacts().contentSha256()),
          "content SHA changed");
      require(
          Arrays.equals(
              expected.sourceFacts().snapshot().tokenSha256Bytes(),
              actual.sourceFacts().snapshot().tokenSha256Bytes()),
          "token SHA lane changed");

      byte[] tampered = eventBytes.clone();
      tampered[tampered.length - 1] ^= 1;
      try {
        M3FileEventReader.readM3File(
            new ByteArrayInputStream(tampered), resolver, true);
        throw new AssertionError("tampered event was accepted");
      } catch (java.io.IOException expectedFailure) {
        // The existing authenticated MIDXEVT2 reader remains the admission gate.
      }

      System.out.println(
          "MINDEX_FILE_EVENT_REHYDRATE_PASS"
              + "|wireFormat=MIDXEVT2|sourceRead=0|lexicon=1|primitive=1"
              + "|linePostings=1|byteFacts=1|sourceFacts=1|rolling=1|sha=1|tamper=1");
    } finally {
      Files.deleteIfExists(source);
    }
    require(!Files.exists(source), "temporary source was not deleted");
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
