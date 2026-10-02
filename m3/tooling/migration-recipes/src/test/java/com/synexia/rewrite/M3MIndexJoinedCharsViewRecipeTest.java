// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Actual OpenRewrite scheduler tests; no disabled type validation or stubs. */
class M3MIndexJoinedCharsViewRecipeTest {
  private static final String[] NAMES = {"FrozenBytes.java", "FrozenChars.java",
      "MIndexJoinedBytes.java", "MIndexJoinedChars.java", "MIndexJoinedStorageIntern.java", "MIndexJoinedStreams.java"};
  private static final String PREFIX = "synexia-indexstring/src/main/java/com/synexia/indexstring/";
  private static String read(String name) {
    try (InputStream in = M3MIndexJoinedCharsViewRecipeTest.class.getResourceAsStream("/com/synexia/rewrite/m3port/" + name)) {
      if (in == null) throw new IllegalStateException(name);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException error) { throw new IllegalStateException(error); }
  }
  private static List<SourceFile> sources(boolean alreadyApplied, InMemoryExecutionContext context) {
    String[] raw = new String[NAMES.length];
    for (int i = 0; i < NAMES.length; i++) {
      String name = NAMES[i];
      boolean owner = name.equals("MIndexJoinedChars.java");
      raw[i] = read(name + (owner && alreadyApplied ? ".after.txt" : ".before.txt"));
    }
    List<SourceFile> files = new ArrayList<>();
    for (SourceFile parsed : JavaParser.fromJavaVersion().build().parse(context, raw).toList()) {
      J.CompilationUnit unit = assertInstanceOf(J.CompilationUnit.class, parsed);
      String name = unit.getSourcePath().getFileName().toString();
      String exact = read(name + (name.equals("MIndexJoinedChars.java") && alreadyApplied ? ".after.txt" : ".before.txt"));
      assertEquals(exact, unit.printAll());
      files.add(unit.withSourcePath(Path.of(PREFIX + name)));
    }
    assertEquals(NAMES.length, files.size());
    return files;
  }
  @Test void actualSchedulerReplayAndFixedPoint() {
    var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    var files = sources(false, context);
    var recipe = new M3MIndexJoinedCharsViewRecipe();
    var changes = recipe.run(new InMemoryLargeSourceSet(files), context).getChangeset().getAllResults();
    assertEquals(1, changes.size());
    SourceFile after = changes.getFirst().getAfter();
    assertEquals(read("MIndexJoinedChars.java.after.txt"), after.printAll());
    files.replaceAll(file -> file.getSourcePath().equals(after.getSourcePath()) ? after : file);
    assertEquals(0, recipe.run(new InMemoryLargeSourceSet(files), context).getChangeset().getAllResults().size());
  }
  @Test void alreadyAppliedIsUnchanged() {
    var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    assertEquals(0, new M3MIndexJoinedCharsViewRecipe().run(
        new InMemoryLargeSourceSet(sources(true, context)), context).getChangeset().getAllResults().size());
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
