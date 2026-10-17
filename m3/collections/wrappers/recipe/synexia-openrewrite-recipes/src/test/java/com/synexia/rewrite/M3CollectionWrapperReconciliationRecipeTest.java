// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Qualification of this exact packet through the existing SDK owners. */
class M3CollectionWrapperReconciliationRecipeTest {
  @TempDir Path temporary;

  @Test void delegatesToExistingOwnersWithDisjointSealedPaths() {
    var recipes = new M3CollectionWrapperReconciliationRecipe().getRecipeList();
    assertEquals(2, recipes.size());
    var java = assertInstanceOf(M3HashPinnedJavaSnapshotRecipe.class, recipes.get(0));
    var text = assertInstanceOf(M3HashPinnedTextSnapshotRecipe.class, recipes.get(1));
    assertEquals(M3CollectionWrapperReconciliationRecipe.CRATE, java.getCrateName());
    assertEquals(java.getCrateName(), text.getCrateName());
    assertTrue(Collections.disjoint(java.targetPaths(), text.targetPaths()));
    assertTrue(java.targetPaths().contains("m3/collections/src/main/java/com/m3/collections/M3LongSortedViews.java"));
    assertThrows(IllegalArgumentException.class, () -> new M3CollectionWrapperReconciliationRecipe("arbitrary"));
  }

  @Test void sdkProducesTypedPostimagesPreservesDecoysAndReachesFixedPoint() {
    var inputs = new ArrayList<>(M3CollectionWrapperReconciliationApply.beforeSources());
    SourceFile decoy = M3CollectionWrapperReconciliationApply.source("other/src/main/java/Decoy.java",
        "class Decoy { String s = \"filtered(source, predicate)\"; /* size() { count++; } */ }\n");
    inputs.add(decoy);
    var first = M3CollectionWrapperReconciliationApply.run(inputs);
    assertEquals(M3CollectionWrapperReconciliationApply.targets().size(), first.changes().size());
    assertPostimages(first.sources());
    assertSame(decoy, first.sources().stream().filter(s -> s.getSourcePath().equals(decoy.getSourcePath()))
        .findFirst().orElseThrow());
    assertTrue(M3CollectionWrapperReconciliationApply.run(first.sources()).changes().isEmpty());
  }

  @Test void serialReverseAndTwoWorkersGiveIdenticalSourceBytes() throws Exception {
    var inputs = M3CollectionWrapperReconciliationApply.beforeSources();
    var reversed = new ArrayList<>(inputs);
    Collections.reverse(reversed);
    var serial = texts(M3CollectionWrapperReconciliationApply.run(inputs).sources());
    try (var pool = Executors.newFixedThreadPool(2)) {
      var a = pool.submit(() -> texts(M3CollectionWrapperReconciliationApply.run(inputs).sources()));
      var b = pool.submit(() -> texts(M3CollectionWrapperReconciliationApply.run(reversed).sources()));
      assertEquals(serial, a.get());
      assertEquals(serial, b.get());
    }
  }

  @Test void javaDriftAndMissingPreimagesRefuseBeforeDiskWrites() throws IOException {
    populateBefore();
    var target = required("java");
    Path path = temporary.resolve(target.path());
    Files.writeString(path, Files.readString(path) + "// concurrent work\n");
    var observed = tree();
    assertThrows(RuntimeException.class, () -> M3CollectionWrapperReconciliationApply.apply(temporary));
    assertEquals(observed, tree());
    Files.delete(path);
    observed = tree();
    assertThrows(RuntimeException.class, () -> M3CollectionWrapperReconciliationApply.apply(temporary));
    assertEquals(observed, tree());
  }

  @Test void textDriftRefusesTheWholeDiskApplicationEvenAfterJavaGeneration() throws IOException {
    populateBefore();
    Path path = temporary.resolve(required("text").path());
    Files.writeString(path, Files.readString(path) + "\nconcurrent work\n");
    var observed = tree();
    assertThrows(RuntimeException.class, () -> M3CollectionWrapperReconciliationApply.apply(temporary));
    assertEquals(observed, tree());
  }

  @Test void duplicateNonJavaAndOccupiedJavaTargetsRefuse() {
    var originals = M3CollectionWrapperReconciliationApply.beforeSources();
    SourceFile first = originals.stream().filter(J.CompilationUnit.class::isInstance).findFirst().orElseThrow();
    var duplicate = new ArrayList<>(originals);
    duplicate.add(first);
    assertThrows(RuntimeException.class, () -> M3CollectionWrapperReconciliationApply.run(duplicate));
    var wrongKind = new ArrayList<>(originals);
    wrongKind.remove(first);
    wrongKind.add(PlainText.builder().sourcePath(first.getSourcePath()).text(first.printAll()).build());
    assertThrows(RuntimeException.class, () -> M3CollectionWrapperReconciliationApply.run(wrongKind));
    var absent = M3CollectionWrapperReconciliationApply.targets().stream()
        .filter(t -> t.kind().equals("java") && t.before().equals("ABSENT")).findFirst().orElseThrow();
    var occupied = new ArrayList<>(originals);
    occupied.add(M3CollectionWrapperReconciliationApply.source(absent.path(),
        "class ConcurrentWork { String untouched = \"current owner\"; }\n"));
    assertThrows(RuntimeException.class, () -> M3CollectionWrapperReconciliationApply.run(occupied));
  }

  @Test void partialResumeAndSecondFilesystemApplicationAreFixedPoints() throws IOException {
    populateBefore();
    var partial = M3CollectionWrapperReconciliationApply.targets().stream()
        .filter(t -> t.before().equals("ABSENT")).findFirst().orElseThrow();
    Path path = temporary.resolve(partial.path());
    Files.createDirectories(path.getParent());
    Files.writeString(path, M3CollectionWrapperReconciliationApply.resource(partial.kind(), partial.resource()));
    var first = M3CollectionWrapperReconciliationApply.apply(temporary);
    assertEquals(M3CollectionWrapperReconciliationApply.targets().size() - 1, first.changes());
    assertTrue(first.receipt().contains("\tSDK_EXECUTION\t"));
    var after = tree();
    var again = M3CollectionWrapperReconciliationApply.apply(temporary);
    assertEquals(0, again.changes());
    assertEquals(after, tree());
  }

  @Test void symlinkedTargetParentsAndAliasedReceiptsRefuse(@TempDir Path outside) throws IOException {
    populateBefore();
    Path source = temporary.resolve(required("java").path());
    var before = tree();
    Path alias = outside.resolve("receipt-alias.tsv");
    Files.createLink(alias, source);
    for (Path receipt : List.of(source, alias, temporary.resolve("new-receipt.tsv"))) {
      assertThrows(IllegalStateException.class, () -> M3CollectionWrapperReconciliationApply.main(
          new String[]{temporary.toString(), receipt.toString()}));
      assertEquals(before, tree());
    }
    Path moved = outside.resolve("owner.java");
    Files.move(source, moved);
    Files.createSymbolicLink(source, moved);
    assertThrows(IllegalStateException.class, () -> M3CollectionWrapperReconciliationApply.apply(temporary));
  }

  @Test void catalogueSearchRetainsCategoriesAndUsesLiteralMatching() {
    var all = M3CollectionWrapperReconciliationApply.catalogue("");
    assertTrue(all.size() > 12);
    for (String line : all) if (!line.startsWith("#") && !line.isBlank()) assertEquals(8, line.split("\t", -1).length);
    assertEquals(M3CollectionWrapperReconciliationApply.catalogue("SIZE"),
        M3CollectionWrapperReconciliationApply.catalogue("size"));
    assertTrue(M3CollectionWrapperReconciliationApply.catalogue("flatmap").stream()
        .anyMatch(line -> line.contains("M3MappedViews")));
    assertTrue(M3CollectionWrapperReconciliationApply.catalogue(".*").stream().allMatch(line -> line.startsWith("#")));
  }

  @Test void fileBlockingAnAbsentTargetParentRefusesBeforeAnyWrite() throws IOException {
    populateBefore();
    Path blocked = temporary.resolve("m3/collections/wrappers/src/test");
    Files.createDirectories(blocked.getParent());
    Files.writeString(blocked, "existing non-directory owner\n");
    var before = tree();
    assertThrows(IllegalStateException.class, () -> M3CollectionWrapperReconciliationApply.apply(temporary));
    assertEquals(before, tree());
  }

  private static M3CollectionWrapperReconciliationApply.Target required(String kind) {
    return M3CollectionWrapperReconciliationApply.targets().stream()
        .filter(t -> t.kind().equals(kind) && !t.before().equals("ABSENT")).findFirst().orElseThrow();
  }

  private void populateBefore() throws IOException {
    for (var source : M3CollectionWrapperReconciliationApply.beforeSources()) {
      Path path = temporary.resolve(source.getSourcePath());
      Files.createDirectories(path.getParent());
      Files.writeString(path, source.printAll(), StandardCharsets.UTF_8);
    }
  }

  private Map<String, String> tree() throws IOException {
    var result = new TreeMap<String, String>();
    try (var paths = Files.walk(temporary)) {
      for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
        result.put(temporary.relativize(path).toString(), Files.readString(path));
      }
    }
    return result;
  }

  private static Map<String, String> texts(List<SourceFile> sources) {
    var result = new TreeMap<String, String>();
    for (SourceFile source : sources) result.put(M3CollectionWrapperReconciliationApply.path(source), source.printAll());
    return result;
  }

  private static void assertPostimages(List<SourceFile> sources) {
    var byPath = new TreeMap<String, SourceFile>();
    for (SourceFile source : sources) byPath.put(M3CollectionWrapperReconciliationApply.path(source), source);
    for (var target : M3CollectionWrapperReconciliationApply.targets()) {
      var source = byPath.get(target.path());
      assertNotNull(source, target.path());
      if (target.kind().equals("java")) assertInstanceOf(J.CompilationUnit.class, source);
      assertEquals(M3CollectionWrapperReconciliationApply.resource(target.kind(), target.resource()), source.printAll());
      assertEquals(target.after(), M3CollectionWrapperReconciliationApply.sha(source.printAll()));
    }
  }
}
