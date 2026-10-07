// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.RemoveUnusedImports;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/** The one reviewed upstream import recipe is admitted; unknown recipe names remain forbidden. */
final class M3ImportAdmissionTest implements RewriteTest {
    @Test
    void removesOnlyUnusedImportsAndReachesFixedPoint() {
        rewriteRun(spec -> spec.recipe(new RemoveUnusedImports()).cycles(2)
                        .expectedCyclesThatMakeChanges(1),
                java("""
                        import java.util.List;
                        import java.util.Set;
                        class Imported {
                            Set<String> values = Set.of("kept");
                        }
                        """, """
                        import java.util.Set;
                        class Imported {
                            Set<String> values = Set.of("kept");
                        }
                        """));
    }

    @Test
    void intentionallyUnresolvedInputRetainsEveryImport() {
        // This is a negative applicability input, NOT a type-valid program admitted for mutation.
        // No RewriteTest type-validation setting is changed and all existing gates remain enabled.
        String source = "import java.util.List; class Partial { MissingType value; }";
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<SourceFile> files = JavaParser.fromJavaVersion().build().parse(context, source).toList();
        assertEquals(1, files.size());
        assertTrue(files.getFirst() instanceof J.CompilationUnit);
        Recipe recipe = new RemoveUnusedImports();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(files), context, 1)
                .getChangeset().getAllResults().isEmpty());
        assertEquals(source, files.getFirst().printAll());
        var policy = M3RecipeScopeRegistry.require(RemoveUnusedImports.class);
        assertEquals(M3EditScope.FILE, policy.resolve(List.of("src/Partial.java")));
        assertFalse(M3RecipeScopeRegistry.registered("org.openrewrite.java.NotReviewed"));
    }
}
