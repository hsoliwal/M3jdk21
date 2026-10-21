// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

class M3MopStreamTraversalRecipeTest {
  @Test
  void generatesThreeExactAdditiveOutputsAndReachesFixedPoint() {
    M3MopStreamTraversalRecipe recipe = new M3MopStreamTraversalRecipe();
    var first =
        recipe.run(
            new InMemoryLargeSourceSet(List.of()),
            new InMemoryExecutionContext(error -> {
              throw new AssertionError(error);
            }),
            1);
    List<SourceFile> generated =
        first.getChangeset().getAllResults().stream()
            .map(result -> result.getAfter())
            .sorted(Comparator.comparing(file -> file.getSourcePath().toString()))
            .toList();

    assertEquals(3, generated.size());
    assertEquals(
        List.of(
            "m3/ports/file/src/main/java/com/m3/file/M3FileDelta.java",
            "m3/ports/file/src/main/java/com/m3/file/M3FileStreamTasks.java",
            "m3/ports/file/src/main/java/com/m3/file/M3FileUniverseTraversal.java"),
        generated.stream().map(file -> file.getSourcePath().toString().replace('\\', '/')).toList());

    var second =
        recipe.run(
            new InMemoryLargeSourceSet(generated),
            new InMemoryExecutionContext(error -> {
              throw new AssertionError(error);
            }),
            1);
    assertTrue(second.getChangeset().getAllResults().isEmpty());
  }
}
