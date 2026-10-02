// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Actual Java parser/generation checks; source recipe tests separately run the full scheduler. */
class InstallIndexStringCompatibilityTest {
  private static String read(String name) {
    try (InputStream in = InstallIndexStringCompatibilityTest.class.getResourceAsStream("/com/m3/rewrite/port-v2/" + name)) {
      if (in == null) throw new IllegalStateException(name);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException error) { throw new IllegalStateException(error); }
  }
  private static void add(InstallIndexStringCompatibility recipe, InstallIndexStringCompatibility.State state,
      InMemoryExecutionContext ctx, String path, String text) {
    recipe.getScanner(state).visit(PlainText.builder().sourcePath(Path.of(path)).text(text).build(), ctx);
  }
  @Test void generateEightAndReachFixedPoint() {
    var recipe = new InstallIndexStringCompatibility(); var ctx = new InMemoryExecutionContext();
    var state = recipe.getInitialValue(ctx); add(recipe, state, ctx, "pom.xml", read("pom.xml.txt"));
    var generated = recipe.generate(state, ctx); assertEquals(8, generated.size());
    for (var file : generated) recipe.getScanner(state).visit(file, ctx);
    assertEquals(0, recipe.generate(state, ctx).size());
  }
  @Test void refuseMissingPom() {
    var recipe = new InstallIndexStringCompatibility(); var ctx = new InMemoryExecutionContext();
    assertThrows(IllegalStateException.class, () -> recipe.generate(recipe.getInitialValue(ctx), ctx));
  }
  @Test void refuseMixedAndDivergentState() {
    var recipe = new InstallIndexStringCompatibility(); var ctx = new InMemoryExecutionContext();
    var state = recipe.getInitialValue(ctx); add(recipe, state, ctx, "pom.xml", read("pom.xml.txt"));
    add(recipe, state, ctx, "src/main/java/com/synexia/indexstring/FrozenChars.java", read("FrozenChars.java.txt"));
    assertThrows(IllegalStateException.class, () -> recipe.generate(state, ctx));
    add(recipe, state, ctx, "src/main/java/com/synexia/indexstring/FrozenChars.java", "// divergent");
    assertThrows(IllegalStateException.class, () -> recipe.generate(state, ctx));
  }
}
