// Copyright 2026 Hitesh Soliwal and contributors
// Mapped pinned donor proof; see DONOR_PROBES.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileDictionary;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Focused proof for authenticated source-facts attachment to mapped snapshots. */
public final class M3FileMappedSourceFactsProof {
  private static int checks;

  private M3FileMappedSourceFactsProof() {}

  public static void main(String[] args) throws Exception {
    M3FileDictionary resolver =
        M3FileDictionary.frozen(java.util.List.of(new M3FileDictionary.Language(
                7, "en", "mapped-source-facts-v1", List.of("alpha"))));

    Path directory = Files.createTempDirectory("mindex-mapped-source-facts-");
    Path source = directory.resolve("alpha.txt");
    Path coldImage = directory.resolve("alpha.midxf");
    Path factsImage = directory.resolve("alpha.midxf.facts");
    Path tamperedFacts = directory.resolve("alpha.midxf.facts.tampered");
    Files.writeString(source, "alpha", StandardCharsets.UTF_8);

    M3File admitted = M3FileStreamIndexer.plainText(source, resolver, 7);
    M3FileImage.write(admitted, coldImage);
    M3FileSourceFactsImage.write(admitted, factsImage);
    byte[] coldBefore = Files.readAllBytes(coldImage);

    M3FileSnapshot detached;
    try (MappedM3File mapped = MappedM3File.open(coldImage, resolver)) {
      detached = mapped.snapshotWithSourceFacts(factsImage);
      check(detached.hasSourceFacts(), "mapped snapshot lost source facts");
      check(
          detached.sourceFactsSnapshot().contentSha256()
              .equals(admitted.sourceFacts().contentSha256()),
          "mapped metadata digest differs");
      check(
          detached.sourceFactsSnapshot().attributesAfterRead().normalizedPath()
              .equals(source.toAbsolutePath().normalize().toString()),
          "mapped metadata path differs");
      check(
          detached.tokenRangeSha256(resolver, 0, mapped.tokenCount()).sha256()
              .equals(admitted.sourceFacts().contentSha256()),
          "mapped detached range digest differs");
      check(Arrays.equals(coldBefore, Files.readAllBytes(coldImage)),
          "sidecar attachment changed cold image");
    }
    check(Arrays.equals(coldBefore, Files.readAllBytes(coldImage)),
        "mapped close changed cold image");

    Files.delete(source);
    Files.delete(factsImage);
    check(
        detached.sourceFactsSnapshot().contentSha256()
            .equals(admitted.sourceFacts().contentSha256()),
        "detached metadata did not survive source/sidecar deletion");
    check(
        detached.tokenRangeSha256(resolver, 0, detached.primitiveSnapshot().tokenCount())
            .sha256()
            .equals(admitted.sourceFacts().contentSha256()),
        "detached range did not survive source/sidecar deletion");

    // Recreate the sidecar and prove its authenticated checksum rejects mutation.
    M3FileSourceFactsImage.write(admitted, factsImage);
    byte[] bytes = Files.readAllBytes(factsImage);
    bytes[bytes.length - 1] ^= 0x01;
    Files.write(tamperedFacts, bytes);
    boolean rejected = false;
    try (MappedM3File mapped = MappedM3File.open(coldImage, resolver)) {
      mapped.snapshotWithSourceFacts(tamperedFacts);
    } catch (java.io.IOException expected) {
      rejected = true;
    }
    check(rejected, "tampered source-facts sidecar was accepted");

    Files.deleteIfExists(tamperedFacts);
    Files.deleteIfExists(factsImage);
    Files.deleteIfExists(coldImage);
    Files.deleteIfExists(source);
    Files.deleteIfExists(directory);

    System.out.println(
        "MINDEX_FILE_MAPPED_SOURCE_FACTS_PASS"
            + "|mapped=1|sidecar=1|metadata=1|detached=1|sourceClosed=1"
            + "|tamperRejected=1|noFormatChange=1|checks=" + checks);
  }

  private static void check(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }
}
