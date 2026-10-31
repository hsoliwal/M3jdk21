// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Production SMFILE01 admission through existing canonical manifest relations. */
public final class M3QualifiedColdFileTest {
  static int checks;
  interface Action { void run() throws Exception; }
  static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
  static void refuse(Action action) throws Exception {
    try { action.run(); throw new AssertionError("expected refusal"); }
    catch (IOException | IllegalArgumentException expected) { checks++; }
  }
  static void delete(Path path) throws Exception {
    try (var walk = Files.walk(path)) {
      for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
    }
  }
  public static void main(String[] args) throws Exception {
    Path root = Files.createTempDirectory("m3-qualified-cold-");
    try {
      Path text = root.resolve("input.txt");
      Files.writeString(text, args.length == 2 ? "zaaz" : "za z\nunknown\t");
      M3ColdFileFixture fixture = M3ColdFileFixture.fromSource(text);
      Path semantic = root.resolve("cold.bin"), facts = root.resolve("facts.bin");
      Files.write(semantic, fixture.semanticImage()); Files.write(facts, fixture.sourceFactsImage());
      var sender = new M3FileSystemStore(root.resolve("sender"));
      M3ObjectRef original = sender.createMIndexFileManifest(sender.putRaw(text),
          sender.putMIndexFileImage(semantic), sender.putMIndexFileSourceFactsImage(facts));
      String seal = args.length == 2 ? args[0] : "0123456789abcdef".repeat(4);
      String receiverSeal = args.length == 2 ? args[1] : seal;
      M3ObjectRef published = sender.withMIndexLexiconQualifications(original, Map.of(7, seal));
      check(!original.id().equals(published.id()));
      check(sender.withMIndexLexiconQualifications(original, Map.of(7, seal)).id().equals(published.id()));
      // Transfer the existing content-addressed object tree, no new image or wire encoding.
      Path receiverRoot = root.resolve("receiver");
      try (var walk = Files.walk(root.resolve("sender"))) {
        for (Path source : walk.toList()) {
          Path target = receiverRoot.resolve(root.resolve("sender").relativize(source));
          if (Files.isDirectory(source)) Files.createDirectories(target);
          else Files.copy(source, target);
        }
      }
      var receiver = new M3FileSystemStore(receiverRoot);
      int[] starts = {0, 3}, ends = {fixture.tokenCount(), fixture.tokenCount()};
      var ranges = receiver.sha256MIndexFileTokenRangesQualified(published,
          language -> M3ColdFileFixture.GENERATION, language -> receiverSeal, starts, ends);
      check(ranges.size() == 2);
      check(ranges.get(0).sha256().equals(M3ContentId.of(M3ColdFileFixture.sha(fixture.raw))));
      refuse(() -> receiver.sha256MIndexFileTokenRangesQualified(published,
          language -> M3ColdFileFixture.GENERATION, language -> "f".repeat(64), starts, ends));
      refuse(() -> receiver.sha256MIndexFileTokenRangesQualified(published,
          language -> 1L, language -> seal, starts, ends));
      refuse(() -> receiver.sha256MIndexFileTokenRangesQualified(original,
          language -> M3ColdFileFixture.GENERATION, language -> seal, starts, ends));
      M3ObjectRef extra = sender.withMIndexLexiconQualifications(original, Map.of(7, seal, 8, seal));
      refuse(() -> sender.sha256MIndexFileTokenRangesQualified(extra,
          language -> M3ColdFileFixture.GENERATION, language -> seal, starts, ends));
      refuse(() -> sender.withMIndexLexiconQualifications(original, Map.of(-1, seal)));
      refuse(() -> sender.withMIndexLexiconQualifications(original, Map.of(7, "A".repeat(64))));
      // Legacy API deliberately remains available and retains its prior contract.
      check(receiver.sha256MIndexFileTokenRanges(original, starts, ends).size() == 2);
      System.out.println("QUALIFIED_COLD_FILE_PASS checks=" + checks);
    } finally { delete(root); }
  }
}

