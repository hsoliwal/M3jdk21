// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// Modified 2026-10-21: cover mixed exact states and tampered-postimage refusal.
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

class M3FileByteEventFactRecipeTest {
  private static final String ROOT =
      "/com/m3/rewrite/backport/m3file-byte-event-fact-20261021/";

  @Test
  void preservesDefensivePayloadAndRemovesProgressCloneThenReachesFixedPoint() {
    List<SourceFile> before =
        List.of(
            java("m3/ports/file/src/main/java/com/m3/file/M3FileStreamEvents.java",
                resource("M3FileStreamEvents.before.java.txt")),
            java("m3/ports/file/src/main/java/com/m3/file/M3FileStreamTasks.java",
                resource("M3FileStreamTasks.before.java.txt")));

    M3FileByteEventFactRecipe recipe = new M3FileByteEventFactRecipe();
    var first = recipe.run(new InMemoryLargeSourceSet(before), context(), 1);
    List<SourceFile> after =
        first.getChangeset().getAllResults().stream()
            .map(result -> result.getAfter())
            .sorted(Comparator.comparing(file -> file.getSourcePath().toString()))
            .toList();
    assertEquals(2, after.size());

    String events =
        after.stream()
            .filter(file -> file.getSourcePath().toString().endsWith("M3FileStreamEvents.java"))
            .findFirst().orElseThrow().printAll();
    String tasks =
        after.stream()
            .filter(file -> file.getSourcePath().toString().endsWith("M3FileStreamTasks.java"))
            .findFirst().orElseThrow().printAll();

    assertTrue(events.contains("public int byteCount()"));
    assertTrue(events.contains("return payload.length;"));
    assertTrue(events.contains("public byte[] payload()"));
    assertTrue(events.contains("return payload.clone();"));
    assertTrue(tasks.contains("long length = chunk.byteCount();"));
    assertTrue(!tasks.contains("chunk.payload().length"));

    var second = recipe.run(new InMemoryLargeSourceSet(after), context(), 1);
    assertTrue(second.getChangeset().getAllResults().isEmpty());
  }

  @Test
  void retainsPinnedPostimageBesideARequiredPreimage() {
    List<SourceFile> input = List.of(
        java("m3/ports/file/src/main/java/com/m3/file/M3FileStreamEvents.java",
            resource("M3FileStreamEvents.after.java.txt")),
        java("m3/ports/file/src/main/java/com/m3/file/M3FileStreamTasks.java",
            resource("M3FileStreamTasks.before.java.txt")));
    var run = new M3FileByteEventFactRecipe()
        .run(new InMemoryLargeSourceSet(input), context(), 1);
    var changes = run.getChangeset().getAllResults();
    assertEquals(1, changes.size());
    assertEquals("m3/ports/file/src/main/java/com/m3/file/M3FileStreamTasks.java",
        changes.getFirst().getAfter().getSourcePath().toString());
    assertEquals(resource("M3FileStreamTasks.after.java.txt"),
        changes.getFirst().getAfter().printAll());
  }

  @Test
  void refusesTamperedPostimage() {
    List<SourceFile> input = List.of(
        java("m3/ports/file/src/main/java/com/m3/file/M3FileStreamEvents.java",
            resource("M3FileStreamEvents.after.java.txt") + "// unreviewed change\n"),
        java("m3/ports/file/src/main/java/com/m3/file/M3FileStreamTasks.java",
            resource("M3FileStreamTasks.after.java.txt")));
    assertThrows(RuntimeException.class, () -> new M3FileByteEventFactRecipe()
        .run(new InMemoryLargeSourceSet(input), context(), 1));
  }

  private static SourceFile java(String path, String source) {
    List<SourceFile> parsed =
        JavaParser.fromJavaVersion().build().parseInputs(
            List.of(Parser.Input.fromString(Path.of(path), source)), null, context()).toList();
    if (parsed.size() != 1) throw new AssertionError("fixture parse count");
    return parsed.getFirst();
  }

  private static InMemoryExecutionContext context() {
    return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
  }

  private static String resource(String name) {
    try (var stream = M3FileByteEventFactRecipeTest.class.getResourceAsStream(ROOT + name)) {
      if (stream == null) throw new AssertionError("missing fixture " + name);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException failure) {
      throw new AssertionError(failure);
    }
  }
}
