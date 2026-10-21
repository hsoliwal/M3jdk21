// Copyright 2026 Hitesh Soliwal and contributors
// Mapped pinned donor proof; see DONOR_PROBES.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileDictionary;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** Executable proof that the store delegates one sidecar publication and leaves no staging file. */
public final class M3FileSourceFactsStoreSinglePublicationProof {
  private static M3FileDictionary resolver() {
    return M3FileDictionary.frozen(java.util.List.of(new M3FileDictionary.Language(7, "en", "source-facts", List.of("alpha", "beta", " ", "\n"))));
  }

  private M3FileSourceFactsStoreSinglePublicationProof() {}

  public static void main(String[] args) throws Exception {
    Path directory = Files.createTempDirectory("mindex-source-facts-store-proof-");
    Path source = directory.resolve("source.txt");
    Path factsDirectory = directory.resolve("facts");
    try {
      Files.writeString(source, "alpha beta\nalpha", StandardCharsets.UTF_8);
      M3File file = M3FileStreamIndexer.plainText(source, resolver(), 7);
      M3FileSourceFactsKey key = M3FileSourceFactsKey.of(source.toString(), file);
      try (M3FileSourceFactsStore store = new M3FileSourceFactsStore(factsDirectory)) {
        store.write(key, file);
        if (!store.contains(key)) throw new AssertionError("store did not publish sidecar");
        try (Stream<Path> entries = Files.list(factsDirectory)) {
          if (entries.anyMatch(path -> path.getFileName().toString().endsWith(".tmp"))) {
            throw new AssertionError("store left a staging file");
          }
        }
        M3File restored = store.read(key, file);
        if (restored == null
            || !restored.sourceFacts().contentSha256().equals(file.sourceFacts().contentSha256())) {
          throw new AssertionError("single-publication sidecar did not round-trip");
        }
        if (!store.invalidate(key) || store.contains(key)) {
          throw new AssertionError("store invalidation contract failed");
        }
      }
    } finally {
      Files.deleteIfExists(directory.resolve("facts").resolve("unused"));
      Files.deleteIfExists(source);
      Files.deleteIfExists(directory.resolve("facts"));
      Files.deleteIfExists(directory);
    }
    System.out.println(
        "MINDEX_FILE_SOURCE_FACTS_STORE_SINGLE_PUBLICATION_PROOF_PASS"
            + "|published=1|roundTrip=1|temporaryClean=1|invalidated=1");
  }
}
