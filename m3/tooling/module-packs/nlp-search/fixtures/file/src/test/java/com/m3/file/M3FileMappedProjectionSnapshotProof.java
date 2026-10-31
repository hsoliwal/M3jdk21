// Copyright 2026 Hitesh Soliwal and contributors
// Mapped pinned donor proof; see DONOR_PROBES.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileDictionary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Executable proof for heap/mapped detached projection parity on the current develop image. */
public final class M3FileMappedProjectionSnapshotProof {
  private M3FileMappedProjectionSnapshotProof() {}

  public static void main(String[] args) throws Exception {
    M3FileDictionary space =
        M3FileDictionary.frozen(java.util.List.of(new M3FileDictionary.Language(
                    7,
                    "en",
                    "mapped-projection-current-v1",
                    java.util.List.of("alpha", " ", "+", "2", "\n", "// note", "\"quoted\"", "\t"))));
    M3File file =
        M3FileTokenizer.codeLike(
            space, 7, "alpha + 2\n// note\n\"quoted\"\t");
    M3FileProjectionSnapshot heapSemantic = file.semanticProjection().snapshot();
    M3FileProjectionSnapshot heapTrivia = file.triviaProjection().snapshot();
    M3FileProjectionSnapshot heapEmpty = M3FileProjection.ofKinds(file).snapshot();

    Path directory = Files.createTempDirectory("mindex-mapped-projection-snapshot-proof-");
    Path image = directory.resolve("source.midxf");
    Path secondImage = directory.resolve("second.midxf");
    try {
      M3FileImage.write(file, image);
      M3FileImage.write(file, secondImage);
      byte[] imageBytes = Files.readAllBytes(image);
      require(Arrays.equals(imageBytes, Files.readAllBytes(secondImage)), "image is not deterministic");
      try (MappedM3File mapped = MappedM3File.open(image, space)) {
        assertSame(heapSemantic, mapped.semanticProjectionSnapshot(), "semantic");
        assertSame(heapTrivia, mapped.triviaProjectionSnapshot(), "trivia");
        assertSame(heapEmpty, mapped.projectionSnapshot(), "empty");
        assertSame(
            heapSemantic,
            mapped.projectionSnapshot(
                M3FileTokenKind.WORD,
                M3FileTokenKind.NUMBER,
                M3FileTokenKind.QUOTED,
                M3FileTokenKind.SYMBOL,
                M3FileTokenKind.OTHER),
            "selected semantic");
        int[] detached = mapped.semanticProjectionSnapshot().sourceTokenPositions();
        if (detached.length > 0) {
          detached[0] = -1;
          require(
              mapped.semanticProjectionSnapshot().sourceTokenIndexAt(0)
                  == heapSemantic.sourceTokenIndexAt(0),
              "mapped snapshot is not defensive");
        }
      }
      require(Arrays.equals(imageBytes, Files.readAllBytes(image)), "mapped read changed image bytes");
    } finally {
      Files.deleteIfExists(secondImage);
      Files.deleteIfExists(image);
      Files.deleteIfExists(directory);
    }
    System.out.println(
        "MINDEX_FILE_MAPPED_PROJECTION_PRIMITIVE_SNAPSHOT_CURRENT_DEVELOP_PASS"
            + "|heap=1|mapped=1|mask=1|semantic=1|trivia=1|empty=1"
            + "|ordering=1|defensive=1|image=1|format=unchanged");
  }

  private static void assertSame(
      M3FileProjectionSnapshot expected,
      M3FileProjectionSnapshot actual,
      String label) {
    require(expected.kindMask() == actual.kindMask(), label + " mask changed");
    require(
        Arrays.equals(expected.sourceTokenPositions(), actual.sourceTokenPositions()),
        label + " positions changed");
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
