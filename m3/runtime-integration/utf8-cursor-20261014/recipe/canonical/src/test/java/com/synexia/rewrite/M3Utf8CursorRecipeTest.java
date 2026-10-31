// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Genuine SDK gates; standalone kernel/atom proof never substitutes for these tests. */
final class M3Utf8CursorRecipeTest {
  private static final String CRATE = "m3jdk21-utf8-cursor-20261014";
  private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
  private static final String PATH =
      "src/java.base/share/classes/java/lang/M3String.java";

  @Test
  void exactTargetAndPreimageRemainSealed() {
    var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
    assertEquals(List.of(PATH), recipe.targetPaths());
    assertEquals("d4dcb3f3320e42a977001254cdd9ac58105cea0a427839ec81d945912476943f",
        recipe.preimageManifest().get(PATH));
    assertEquals(1, recipe.maxCycles());
  }

  @Test
  void sdkAppliesExactOutputThenHasNoChanges() {
    var context = context();
    var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
    SourceFile before = parse(PATH, resource("M3String.before.java.txt"), context);
    var results = recipe.run(new InMemoryLargeSourceSet(List.of(before)), context, 1)
        .getChangeset().getAllResults();
    assertEquals(1, results.size());
    SourceFile after = results.getFirst().getAfter();
    assertNotNull(after);
    assertEquals(resource("M3String.java.txt"), after.printAll());
    assertEquals(PATH, after.getSourcePath().toString().replace('\\', '/'));
    assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(after)), context, 1)
        .getChangeset().getAllResults().isEmpty());
  }

  @Test
  void refusesMissingDuplicateAndDriftBeforeGeneration() {
    var context = context();
    var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
    var missing = recipe.getInitialValue(context);
    assertThrows(IllegalStateException.class, () -> recipe.generate(missing, context));
    SourceFile before = parse(PATH, resource("M3String.before.java.txt"), context);
    var duplicate = recipe.getInitialValue(context);
    var scanner = recipe.getScanner(duplicate);
    scanner.visit(before, context);
    scanner.visit(before, context);
    assertTrue(assertThrows(IllegalStateException.class, () -> recipe.generate(duplicate, context))
        .getMessage().contains("duplicate target"));
    var drift = recipe.getInitialValue(context);
    recipe.getScanner(drift).visit(parse(PATH, before.printAll() + "// drift\n", context), context);
    assertTrue(assertThrows(IllegalStateException.class, () -> recipe.generate(drift, context))
        .getMessage().contains("source drift"));
  }

  @Test
  void identicalTextIsNotPermissionToSkipTheJavaParser() {
    var context = context();
    var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
    var inventory = recipe.getInitialValue(context);
    SourceFile text = PlainText.builder().sourcePath(Path.of(PATH))
        .text(resource("M3String.before.java.txt")).build();
    recipe.getScanner(inventory).visit(text, context);
    assertTrue(assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, context))
        .getMessage().contains("not a Java compilation unit"));
  }

  @Test
  void unrelatedJavaOwnerIsNotModified() {
    var context = context();
    var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
    SourceFile before = parse(PATH, resource("M3String.before.java.txt"), context);
    SourceFile unrelated = parse("src/main/java/Untouched.java", "class Untouched {}\n", context);
    var results = recipe.run(new InMemoryLargeSourceSet(List.of(unrelated, before)), context, 1)
        .getChangeset().getAllResults();
    assertEquals(1, results.size());
    assertNotNull(results.getFirst().getAfter());
    assertEquals(PATH, results.getFirst().getAfter().getSourcePath().toString().replace('\\', '/'));
  }

  private static SourceFile parse(String path, String source, InMemoryExecutionContext context) {
    var parsed = JavaParser.fromJavaVersion().build().parseInputs(
        List.of(Parser.Input.fromString(Path.of(path), source)), null, context).toList();
    assertEquals(1, parsed.size());
    assertEquals(source, parsed.getFirst().printAll());
    return parsed.getFirst();
  }

  private static InMemoryExecutionContext context() {
    return new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
  }

  private static String resource(String name) {
    try (var input = M3Utf8CursorRecipeTest.class.getResourceAsStream(ROOT + name)) {
      if (input == null) throw new AssertionError("missing resource: " + name);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException failure) {
      throw new AssertionError(failure);
    }
  }
}
