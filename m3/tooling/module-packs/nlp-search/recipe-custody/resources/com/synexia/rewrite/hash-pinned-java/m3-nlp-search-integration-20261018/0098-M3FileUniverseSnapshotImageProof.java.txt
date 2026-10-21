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

public final class M3FileUniverseSnapshotImageProof {
  private static int checks;
  private M3FileUniverseSnapshotImageProof() {}

  public static void main(String[] args) throws Exception {
    M3FileDictionary resolver = M3FileDictionary.frozen(java.util.List.of(new M3FileDictionary.Language(7, "en", "universe-image-contract-v1", List.of("alpha"))));
    Path directory = Files.createTempDirectory("mindex-universe-image-proof-");
    Path source = directory.resolve("alpha.txt");
    Path image = directory.resolve("universe-image");
    Files.writeString(source, "alpha", StandardCharsets.UTF_8);
    M3File file = M3FileStreamIndexer.plainText(source, resolver, 7);
    M3FileUniverseSnapshot universe = M3FileUniverseSnapshot.from(
        new M3FileUniverseEventReader.SnapshotSummary(List.of(
            new M3FileUniverseEventReader.SnapshotFile(
                0, source.toAbsolutePath().normalize().toString(), file.snapshot()))),
        resolver);
    check(universe.resolver() == resolver, "live resolver changed");

    M3FileUniverseSnapshotImage.write(universe, image);
    byte[] manifestBefore = Files.readAllBytes(image.resolve("manifest.midxu"));
    byte[] semanticBefore = Files.readAllBytes(image.resolve("file-00000000.midxf"));
    Files.delete(source);

    M3FileUniverseSnapshot restored = M3FileUniverseSnapshotImage.read(image, resolver);
    check(restored.resolver() == resolver, "restored resolver changed");
    check(restored.sha256(StandardCharsets.UTF_8).equals(universe.sha256(StandardCharsets.UTF_8)),
        "restored text changed");
    check(restored.tokenSha256At(0).equals(universe.tokenSha256At(0)), "token SHA changed");
    check(Arrays.equals(manifestBefore, Files.readAllBytes(image.resolve("manifest.midxu"))),
        "manifest changed during replay");
    check(Arrays.equals(semanticBefore, Files.readAllBytes(image.resolve("file-00000000.midxf"))),
        "semantic child changed during replay");

    byte[] tampered = Files.readAllBytes(image.resolve("manifest.midxu"));
    tampered[tampered.length - 1] ^= 0x01;
    Files.write(image.resolve("manifest.midxu"), tampered);
    boolean rejected = false;
    try {
      M3FileUniverseSnapshotImage.read(image, resolver);
    } catch (java.io.IOException expected) {
      rejected = true;
    }
    check(rejected, "tampered manifest accepted");

    Files.deleteIfExists(image.resolve("manifest.midxu"));
    Files.deleteIfExists(image.resolve("file-00000000.midxf"));
    Files.deleteIfExists(image.resolve("file-00000000.midxfs"));
    Files.deleteIfExists(image.resolve(M3FileUniverseTokenIndexImage.NAME));
    Files.deleteIfExists(image);
    Files.deleteIfExists(source);
    Files.deleteIfExists(directory);
    System.out.println("MINDEX_FILE_UNIVERSE_SNAPSHOT_IMAGE_PASS"
        + "|universe=1|resolver=1|fingerprintParity=1|manifestResolver=1"
        + "|tokenSha=1|detached=1|tamper=1|noFormatChange=1|checks=" + checks);
  }

  private static void check(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }
}
