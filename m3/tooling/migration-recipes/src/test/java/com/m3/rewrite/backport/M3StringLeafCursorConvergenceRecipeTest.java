// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

class M3StringLeafCursorConvergenceRecipeTest {
  private static final String ROOT =
      "/com/m3/rewrite/backport/m3string-leaf-cursor-20261021/";

  @Test
  void replacesBothPinnedOwnersGeneratesSharedCursorAndReachesFixedPoint() {
    M3StringLeafCursorConvergenceRecipe recipe = new M3StringLeafCursorConvergenceRecipe();
    List<SourceFile> before =
        List.of(
            java(
                "src/java.base/share/classes/java/lang/M3StringPool.java",
                resource("M3StringPool.before.java.txt")),
            java(
                "src/java.base/share/classes/java/lang/M3StringTokenRegex.java",
                resource("M3StringTokenRegex.before.java.txt")));

    var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
    List<SourceFile> after =
        first.getChangeset().getAllResults().stream()
            .map(result -> result.getAfter())
            .sorted(Comparator.comparing(file -> file.getSourcePath().toString()))
            .toList();
    assertEquals(3, after.size());
    assertEquals(
        List.of(
            "src/java.base/share/classes/java/lang/M3StringLeafCursor.java",
            "src/java.base/share/classes/java/lang/M3StringPool.java",
            "src/java.base/share/classes/java/lang/M3StringTokenRegex.java"),
        after.stream().map(file -> normalized(file.getSourcePath())).toList());

    var second = recipe.run(new InMemoryLargeSourceSet(after), context(), 1);
    assertTrue(second.getChangeset().getAllResults().isEmpty());
  }

  @Test
  void refusesUnknownPoolPreimage() {
    String changed =
        resource("M3StringPool.before.java.txt")
            .replace(
                "private static boolean sameLeafSequence(",
                "private static boolean changedSameLeafSequence(");
    List<SourceFile> input =
        List.of(
            java("src/java.base/share/classes/java/lang/M3StringPool.java", changed),
            java(
                "src/java.base/share/classes/java/lang/M3StringTokenRegex.java",
                resource("M3StringTokenRegex.before.java.txt")));

    assertThrows(
        RuntimeException.class,
        () ->
            new M3StringLeafCursorConvergenceRecipe()
                .run(new InMemoryLargeSourceSet(input), context(), 1));
  }

  private static SourceFile java(String path, String source) {
    List<SourceFile> parsed =
        JavaParser.fromJavaVersion()
            .build()
            .parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), source)),
                null,
                context())
            .toList();
    if (parsed.size() != 1) throw new AssertionError("fixture parse count");
    return parsed.getFirst();
  }

  private static InMemoryExecutionContext context() {
    return new InMemoryExecutionContext(error -> {
      throw new AssertionError(error);
    });
  }

  private static String resource(String name) {
    try (var stream =
        M3StringLeafCursorConvergenceRecipeTest.class.getResourceAsStream(ROOT + name)) {
      if (stream == null) throw new AssertionError("missing fixture " + name);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException failure) {
      throw new AssertionError(failure);
    }
  }

  private static String normalized(Path path) {
    return path.normalize().toString().replace('\\', '/');
  }
}
