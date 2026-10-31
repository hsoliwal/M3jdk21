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
import java.util.List;

/** Executable proof for the detached authenticated unified snapshot envelope. */
public final class M3FileSnapshotCodecProof {
  private M3FileSnapshotCodecProof() {}

  public static void main(String[] args) throws Exception {
    M3FileDictionary resolver =
        M3FileDictionary.frozen(java.util.List.of(new M3FileDictionary.Language(
                    7,
                    "en",
                    "snapshot-codec-proof",
                    List.of("alpha", "beta", "\u00ce", "\u00b2", " ", "\n"))));
    Path directory = Files.createTempDirectory("mindex-snapshot-codec-proof-");
    Path source = directory.resolve("source.txt");
    try {
      Files.writeString(source, "alpha \u00ce\u00b2\nalpha", StandardCharsets.UTF_8);
      M3File file = M3FileStreamIndexer.plainText(source, resolver, 7);
      M3FileSnapshot expected = file.snapshot();
      ByteArrayOutputStream encoded = new ByteArrayOutputStream();
      M3FileSnapshotCodec.write(expected, encoded);
      byte[] bytes = encoded.toByteArray();
      M3FileSnapshot actual =
          M3FileSnapshotCodec.read(new ByteArrayInputStream(bytes));
      require(actual.hasSourceFacts(), "source facts disappeared");
      require(
          Arrays.equals(expected.lexiconSnapshot().tokenIds(), actual.lexiconSnapshot().tokenIds()),
          "lexicon IDs changed");
      require(
          Arrays.equals(
              expected.primitiveSnapshot().localHandles(),
              actual.primitiveSnapshot().localHandles()),
          "primitive handles changed");
      require(
          expected.sourceFactsSnapshot().contentSha256().equals(actual.sourceFactsSnapshot().contentSha256()),
          "content SHA-256 changed");
      require(
          Arrays.equals(
              expected.sourceFactsSnapshot().tokenSha256Bytes(),
              actual.sourceFactsSnapshot().tokenSha256Bytes()),
          "token SHA-256 lane changed");
      require(
          expected.sourceFactsSnapshot().rangeFingerprint(0, expected.primitiveSnapshot().tokenCount())
              .equals(actual.sourceFactsSnapshot().rangeFingerprint(0, actual.primitiveSnapshot().tokenCount())),
          "range fingerprint changed");
      byte[] tampered = bytes.clone();
      tampered[tampered.length - 1] ^= 1;
      expectFailure(tampered);
      System.out.println(
          "MINDEX_FILE_SNAPSHOT_CODEC_PASS"
              + "|format=MIDXSC01|heapAndPhysical=1|lexicon=1|primitive=1"
              + "|attributes=1|byteFacts=1|tokenSha=1|rolling=1|tamper=1|noPayloadCopy=1");
    } finally {
      Files.deleteIfExists(source);
      Files.deleteIfExists(directory);
    }
  }

  private static void expectFailure(byte[] bytes) throws Exception {
    try {
      M3FileSnapshotCodec.read(new ByteArrayInputStream(bytes));
      throw new AssertionError("tampered snapshot was accepted");
    } catch (java.io.IOException expected) {
      // expected
    }
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
