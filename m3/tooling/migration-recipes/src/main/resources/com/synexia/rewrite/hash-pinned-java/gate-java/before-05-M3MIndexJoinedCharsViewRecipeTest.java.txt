// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.java.Assertions.java;

/** Actual OpenRewrite scheduler tests; no disabled type validation or stubs. */
class M3MIndexJoinedCharsViewRecipeTest implements RewriteTest {
  private static final String[] NAMES = {"FrozenBytes.java", "FrozenChars.java",
      "MIndexJoinedBytes.java", "MIndexJoinedChars.java", "MIndexJoinedStorageIntern.java", "MIndexJoinedStreams.java"};
  private static final String PREFIX = "synexia-indexstring/src/main/java/com/synexia/indexstring/";
  private static String read(String name) {
    try (InputStream in = M3MIndexJoinedCharsViewRecipeTest.class.getResourceAsStream("/com/synexia/rewrite/m3port/" + name)) {
      if (in == null) throw new IllegalStateException(name);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException error) { throw new IllegalStateException(error); }
  }
  private static SourceSpecs[] sources(boolean alreadyApplied) {
    SourceSpecs[] files = new SourceSpecs[NAMES.length];
    for (int i = 0; i < NAMES.length; i++) {
      String name = NAMES[i];
      boolean owner = name.equals("MIndexJoinedChars.java");
      String before = read(name + (owner && alreadyApplied ? ".after.txt" : ".before.txt"));
      files[i] = owner && !alreadyApplied
          ? java(before, read(name + ".after.txt"), spec -> spec.path(PREFIX + name))
          : java(before, spec -> spec.path(PREFIX + name));
    }
    return files;
  }
  @Test void actualSchedulerReplayAndFixedPoint() {
    rewriteRun(spec -> spec.recipe(new M3MIndexJoinedCharsViewRecipe()).cycles(2)
        .expectedCyclesThatMakeChanges(1), sources(false));
  }
  @Test void alreadyAppliedIsUnchanged() {
    rewriteRun(spec -> spec.recipe(new M3MIndexJoinedCharsViewRecipe()), sources(true));
  }
  @Test void missingClosureRefused() {
    var recipe = new M3MIndexJoinedCharsViewRecipe();
    var context = new InMemoryExecutionContext();
    assertThrows(IllegalStateException.class, () -> recipe.generate(recipe.getInitialValue(context), context));
  }
  @Test void changedDependencyRefusedDuringScan() {
    var recipe = new M3MIndexJoinedCharsViewRecipe();
    var context = new InMemoryExecutionContext();
    var state = recipe.getInitialValue(context);
    var changed = PlainText.builder().text(read("FrozenChars.java.before.txt") + " // changed")
        .sourcePath(Path.of(PREFIX + "FrozenChars.java")).build();
    assertThrows(IllegalStateException.class, () -> recipe.getScanner(state).visit(changed, context));
  }
}
