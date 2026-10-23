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
import java.util.Map;
import org.openrewrite.config.Environment;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Genuine SDK gates; standalone kernel/atom proof never substitutes for these tests. */
final class M3SingleByteCursorRecipeTest {
  private static final String CRATE = "m3jdk21-single-byte-cursor-20261023";
  private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
  private static final String PATH =
      "src/java.base/share/classes/java/lang/M3String.java";

  private static final String TEST_PATH =
      "test/jdk/java/lang/String/M3StringSingleByteCursorTest.java";

  @Test
  void exactTargetAndPreimageRemainSealed() {
    var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
    assertEquals(List.of(PATH, TEST_PATH), recipe.targetPaths());
    assertEquals("ABSENT", recipe.preimageManifest().get(TEST_PATH));
    assertEquals("b16c1d8478f24f3e10b7249ea3d9264dca8b90fb93e3e77c3800d42fda69615f",
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
    assertEquals(2, results.size());
    var expected = Map.of(PATH, resource("M3String.java.txt"),
        TEST_PATH, resource("M3StringSingleByteCursorTest.java.txt"));
    var afterFiles = results.stream().map(result -> result.getAfter()).toList();
    for (var after : afterFiles) {
      assertNotNull(after);
      assertEquals(expected.get(after.getSourcePath().toString().replace('\\', '/')), after.printAll());
    }
    assertTrue(recipe.run(new InMemoryLargeSourceSet(afterFiles), context, 1)
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
    var occupied = recipe.getInitialValue(context);
    var scannerForOccupied = recipe.getScanner(occupied);
    scannerForOccupied.visit(before, context);
    scannerForOccupied.visit(parse(TEST_PATH, "class M3StringSingleByteCursorTest {}\n", context), context);
    assertTrue(assertThrows(IllegalStateException.class, () -> recipe.generate(occupied, context))
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
    assertEquals(2, results.size());
    assertEquals(List.of(PATH, TEST_PATH), results.stream()
        .map(result -> result.getAfter().getSourcePath().toString().replace('\\', '/')).sorted().toList());
  }

  @Test
  void namedRecipeActivatesTheSealedCrate() {
    var activated = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
        .build().activateRecipes("com.synexia.rewrite.M3Jdk21SingleByteCursorReuse");
    var declared = activated.getRecipeList().getFirst();
    assertEquals("com.synexia.rewrite.M3Jdk21SingleByteCursorReuse", declared.getName());
    var configured = declared.getRecipeList().getFirst();
    assertTrue(configured instanceof M3HashPinnedJavaSnapshotRecipe);
    assertEquals(CRATE, ((M3HashPinnedJavaSnapshotRecipe) configured).getCrateName());
    var context = context();
    var before = parse(PATH, resource("M3String.before.java.txt"), context);
    assertEquals(2, activated.run(new InMemoryLargeSourceSet(List.of(before)), context, 1)
        .getChangeset().getAllResults().size());
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
    try (var input = M3SingleByteCursorRecipeTest.class.getResourceAsStream(ROOT + name)) {
      if (input == null) throw new AssertionError("missing resource: " + name);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException failure) {
      throw new AssertionError(failure);
    }
  }
}
