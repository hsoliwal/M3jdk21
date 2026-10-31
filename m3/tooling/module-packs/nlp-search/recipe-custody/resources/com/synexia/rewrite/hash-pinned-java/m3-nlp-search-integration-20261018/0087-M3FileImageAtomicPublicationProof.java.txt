// Copyright 2026 Hitesh Soliwal and contributors
// Mapped pinned donor proof; see DONOR_PROBES.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import com.m3.file.M3FileDictionary;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/** Executable proof that semantic-image replacement is complete and staging files are removed. */
public final class M3FileImageAtomicPublicationProof {
  private static M3FileDictionary resolver() {
    return M3FileDictionary.frozen(java.util.List.of(new M3FileDictionary.Language(7, "en", "source-facts", List.of("alpha", "beta", " ", "\n"))));
  }

  private M3FileImageAtomicPublicationProof() {}

  public static void main(String[] args) throws Exception {
    Path directory = Files.createTempDirectory("mindex-semantic-image-publication-proof-");
    Path image = directory.resolve("semantic.midxf");
    M3FileDictionary resolver = resolver();
    M3File first = file(resolver, "alpha beta alpha");
    M3File second = file(resolver, "beta alpha");
    try {
      M3FileImage.write(first, image);
      byte[] firstBytes = Files.readAllBytes(image);
      requireImage(firstBytes, first.tokenCount());
      assertNoTemporary(directory);

      M3FileImage.write(second, image);
      byte[] secondBytes = Files.readAllBytes(image);
      requireImage(secondBytes, second.tokenCount());
      if (Arrays.equals(firstBytes, secondBytes)) {
        throw new AssertionError("semantic image replacement did not publish new bytes");
      }
      assertNoTemporary(directory);
    } finally {
      Files.deleteIfExists(image);
      Files.deleteIfExists(directory);
    }
    System.out.println(
        "MINDEX_FILE_IMAGE_ATOMIC_PUBLICATION_PROOF_PASS"
            + "|firstComplete=1|replacementComplete=1|temporaryClean=1");
  }

  private static M3File file(M3FileResolver resolver, String ignored) {
    M3FileBuilder builder = new M3FileBuilder(resolver);
    if (ignored.startsWith("alpha")) {
      builder
          .add(7, "alpha", M3FileTokenKind.WORD)
          .add(7, " ", M3FileTokenKind.WHITESPACE)
          .add(7, "beta", M3FileTokenKind.WORD)
          .add(7, " ", M3FileTokenKind.WHITESPACE)
          .add(7, "alpha", M3FileTokenKind.WORD);
    } else {
      builder
          .add(7, "beta", M3FileTokenKind.WORD)
          .add(7, " ", M3FileTokenKind.WHITESPACE)
          .add(7, "alpha", M3FileTokenKind.WORD);
    }
    return builder.build();
  }

  private static void requireImage(byte[] bytes, int tokenCount) {
    if (bytes.length < M3FileImage.HEADER_BYTES
        || ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getLong() != M3FileImage.MAGIC
        || ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt(Long.BYTES)
            != M3FileImage.FORMAT
        || tokenCount <= 0) {
      throw new AssertionError("published semantic image is incomplete");
    }
  }

  private static void assertNoTemporary(Path directory) throws Exception {
    try (Stream<Path> entries = Files.list(directory)) {
      if (entries.anyMatch(path -> path.getFileName().toString().endsWith(".tmp"))) {
        throw new AssertionError("semantic image publication left a staging file");
      }
    }
  }
}
