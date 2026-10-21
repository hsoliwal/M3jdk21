// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

class M3RecipeMemoryPolicyRecipeTest {
  private static final String ROOT =
      "/com/m3/rewrite/backport/mop-recipe-memory-policy-20261021/";

  @Test
  void replacesCanonicalWorkflowGeneratesVerifierAndReachesFixedPoint() {
    M3RecipeMemoryPolicyRecipe recipe = new M3RecipeMemoryPolicyRecipe();
    SourceFile before =
        PlainText.builder()
            .sourcePath(Path.of(".github/workflows/m3-recipe-ownership.yml"))
            .text(resource("m3-recipe-ownership.before.yml.txt"))
            .build();

    var first = recipe.run(new InMemoryLargeSourceSet(List.of(before)), context(), 1);
    List<SourceFile> after =
        first.getChangeset().getAllResults().stream()
            .map(result -> result.getAfter())
            .sorted(Comparator.comparing(file -> file.getSourcePath().toString()))
            .toList();

    assertEquals(2, after.size());
    assertEquals(
        List.of(
            ".github/workflows/m3-recipe-ownership.yml",
            "m3/runtime-integration/verify-recipe-memory.py"),
        after.stream().map(file -> normalized(file.getSourcePath())).toList());

    String workflow =
        after.stream()
            .filter(file -> normalized(file.getSourcePath()).endsWith("m3-recipe-ownership.yml"))
            .findFirst().orElseThrow().printAll();
    String verifier =
        after.stream()
            .filter(file -> normalized(file.getSourcePath()).endsWith("verify-recipe-memory.py"))
            .findFirst().orElseThrow().printAll();

    assertTrue(workflow.contains("fetch-depth: 0"));
    assertTrue(workflow.contains("verify-recipe-memory.py"));
    assertTrue(workflow.contains("--base"));
    assertTrue(workflow.contains("m3/**/src/main/**"));

    assertTrue(verifier.contains("runtime/native source changed without a changed Maven/OpenRewrite recipe owner"));
    assertTrue(verifier.contains("recipe source changed without append-only RECIPE_MEMORY.tsv update"));
    assertTrue(verifier.contains("recipe memory rewrote prior rows"));

    var second = recipe.run(new InMemoryLargeSourceSet(after), context(), 1);
    assertTrue(second.getChangeset().getAllResults().isEmpty());
  }

  private static InMemoryExecutionContext context() {
    return new InMemoryExecutionContext(error -> {
      throw new AssertionError(error);
    });
  }

  private static String resource(String name) {
    try (var stream = M3RecipeMemoryPolicyRecipeTest.class.getResourceAsStream(ROOT + name)) {
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
