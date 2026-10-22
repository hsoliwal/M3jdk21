// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.xml.XmlParser;

/** Whole DAG replay proof: opt-in XML profile precedes the FILE-local Java validation atom. */
final class NebulaM3BackportIntegrityConvergenceRecipeTest {
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/backport-dag-integrity/";

    @Test
    void twoLeafDagIsDeterministicAndCandidateOnly() {
        var recipe = new NebulaM3BackportIntegrityConvergenceRecipe();
        var leaves = recipe.getRecipeList();

        assertEquals(1, recipe.maxCycles());
        assertEquals(2, leaves.size());
        assertEquals(NebulaM3BackportDagMavenProfileRecipe.class,
                leaves.get(0).getClass());
        assertEquals(NebulaM3BackportDagIntegrityRecipe.class,
                leaves.get(1).getClass());
        assertTrue(leaves.stream().allMatch(leaf -> leaf.maxCycles() == 1));
        assertTrue(recipe.getTags().contains("candidate-only"));
        assertFalse(recipe.getTags().contains("promotion-authority"));
    }

    @Test
    void exactPomsAndJavaReplaysComposeToASecondPassFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);

        SourceFile pom = xml(
                "m3/recipe-first/pom.xml",
                resource("pom.before.xml.txt"),
                context);
        SourceFile plan = java(
                NebulaM3BackportDagIntegrityRecipe.PATH,
                resource("NebulaM3BackportPlan.before.java.txt"),
                context);

        var first = new NebulaM3BackportIntegrityConvergenceRecipe()
                .run(new InMemoryLargeSourceSet(List.of(pom, plan)), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = first.getChangeset().getAllResults();
        assertEquals(2, changes.size());
        Set<String> paths = changes.stream()
                .map(result -> result.getAfter().getSourcePath().toString().replace('\\', '/'))
                .collect(Collectors.toSet());
        assertEquals(
                Set.of("m3/recipe-first/pom.xml",
                        NebulaM3BackportDagIntegrityRecipe.PATH),
                paths);

        var after = changes.stream().map(result -> result.getAfter()).toList();
        var fixedErrors = new ArrayList<Throwable>();
        var second = new NebulaM3BackportIntegrityConvergenceRecipe()
                .run(new InMemoryLargeSourceSet(after),
                        new InMemoryExecutionContext(fixedErrors::add));
        assertTrue(fixedErrors.isEmpty(), fixedErrors.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    private static SourceFile java(
            String path, String content, InMemoryExecutionContext context) {
        List<SourceFile> files = JavaParser.fromJavaVersion().build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), content)), null, context)
                .toList();
        assertEquals(1, files.size());
        return files.getFirst();
    }

    private static SourceFile xml(
            String path, String content, InMemoryExecutionContext context) {
        List<SourceFile> files = XmlParser.builder().build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), content)), null, context)
                .toList();
        assertEquals(1, files.size());
        return files.getFirst();
    }

    private static String resource(String name) {
        try (var input =
                NebulaM3BackportIntegrityConvergenceRecipeTest.class
                        .getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException("missing DAG fixture: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
