// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

import com.m3.rewrite.scope.M3EditScope;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

class M3ExactFileRefactorRecipeTest implements RewriteTest {
  private static final String TARGET = "example/src/main/java/example/Sample.java";
  private static final String OTHER = "example/src/main/java/example/Other.java";
  private static final String BEFORE =
      """
      package example;

      final class Sample {
        private static int sum(int left, int right) {
          return left + right;
        }
      }
      """;
  private static final String AFTER =
      """
      package example;

      final class Sample {
        private static int sum(int left, int right) {
          int m3PureIntReturnAtom = left + right;
          return m3PureIntReturnAtom;
        }
      }
      """;
  private static final String OTHER_SOURCE =
      """
      package example;

      final class Other {
        static int identity(int value) {
          return value;
        }
      }
      """;

  private static M3ExactFileRefactorRecipe recipe() {
    return new M3ExactFileRefactorRecipe(
        TARGET,
        BEFORE,
        AFTER,
        "atom.pure-int-return",
        "pattern.named-behavioral-leaf",
        "iop.behavioral-leaf");
  }

  @Test
  void exactFileTransformsOnceAndThenReachesFixedPoint() {
    rewriteRun(
        spec -> spec.recipe(recipe()).cycles(2).expectedCyclesThatMakeChanges(1),
        java(BEFORE, AFTER, source -> source.path(TARGET)),
        java(OTHER_SOURCE, source -> source.path(OTHER)));
  }

  @Test
  void alreadyAppliedTargetIsUnchanged() {
    rewriteRun(spec -> spec.recipe(recipe()), java(AFTER, source -> source.path(TARGET)));
  }

  @Test
  void recipeCarriesExactScopeAndSemanticPatternMetadata() {
    M3ExactFileRefactorRecipe recipe = recipe();
    assertEquals(M3EditScope.FILE, recipe.editScope().scope());
    assertEquals(TARGET, recipe.targetPath());
    assertTrue(recipe.editScope().allows(TARGET));
    assertFalse(recipe.editScope().allows(OTHER));
    assertEquals("atom.pure-int-return", recipe.atomId());
    assertEquals("pattern.named-behavioral-leaf", recipe.patternId());
    assertEquals("iop.behavioral-leaf", recipe.iopRole());
    assertEquals(64, recipe.beforeSha256().length());
    assertEquals(64, recipe.afterSha256().length());
    assertFalse(recipe.beforeSha256().equals(recipe.afterSha256()));
  }

  @Test
  void changedTargetPreimageFailsClosed() {
    InMemoryExecutionContext context = new InMemoryExecutionContext();
    J.CompilationUnit changed =
        (J.CompilationUnit)
            JavaParser.fromJavaVersion()
                .build()
                .parse(context, BEFORE.replace("left + right", "left - right"))
                .findFirst()
                .orElseThrow();
    changed = changed.withSourcePath(Path.of(TARGET));
    J.CompilationUnit candidate = changed;
    assertThrows(
        IllegalStateException.class, () -> recipe().getVisitor().visit(candidate, context));
  }

  @Test
  void invalidDescriptorIsRejectedBeforeExecution() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new M3ExactFileRefactorRecipe(TARGET, BEFORE, BEFORE, "atom.same", "pattern.same", "iop.same"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new M3ExactFileRefactorRecipe(TARGET, BEFORE, AFTER, "bad id!", "pattern.ok", "iop.ok"));
  }
}
