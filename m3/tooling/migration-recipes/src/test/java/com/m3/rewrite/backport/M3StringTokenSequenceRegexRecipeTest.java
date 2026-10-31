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

class M3StringTokenSequenceRegexRecipeTest {
  @Test
  void generatesJavaNativeAndJtregOutputsThenReachesFixedPoint() {
    M3StringTokenSequenceRegexRecipe recipe = new M3StringTokenSequenceRegexRecipe();
    var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    var first = recipe.run(new InMemoryLargeSourceSet(List.of()), context, 1);
    List<SourceFile> generated =
        first.getChangeset().getAllResults().stream()
            .map(result -> result.getAfter())
            .sorted(Comparator.comparing(file -> file.getSourcePath().toString()))
            .toList();
    assertEquals(3, generated.size());
    assertEquals(
        List.of(
            "src/java.base/share/classes/java/lang/M3StringTokenRegex.java",
            "src/java.base/share/native/libjava/M3StringTokenRegex.c",
            "test/jdk/java/lang/String/M3StringTokenRegexTest.java"),
        generated.stream().map(file -> file.getSourcePath().toString().replace('\\', '/')).toList());

    var second = recipe.run(new InMemoryLargeSourceSet(generated), context, 1);
    assertTrue(second.getChangeset().getAllResults().isEmpty());
  }
}
