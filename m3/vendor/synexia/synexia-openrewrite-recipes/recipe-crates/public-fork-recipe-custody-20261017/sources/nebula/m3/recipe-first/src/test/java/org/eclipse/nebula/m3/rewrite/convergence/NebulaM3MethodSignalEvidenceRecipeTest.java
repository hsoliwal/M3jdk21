// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

class NebulaM3MethodSignalEvidenceRecipeTest {

    @Test
    void exactImagesArePinnedAndJavaRoundTrip() {
        var targets = NebulaM3MethodSignalEvidenceRecipe.targetManifest();
        assertEquals(11, targets.size());
        targets.forEach(
                (path, target) -> {
                    String after =
                            NebulaM3MethodSignalEvidenceRecipe.sourceImage(
                                    target.afterResource());
                    assertEquals(
                            target.after(),
                            NebulaM3MethodSignalEvidenceRecipe.gitBlob(after),
                            path);
                    assertEquals(
                            after,
                            parse(path, after, new InMemoryExecutionContext()).printAll(),
                            path);
                    if (target.beforeResource() != null) {
                        String before =
                                NebulaM3MethodSignalEvidenceRecipe.sourceImage(
                                        target.beforeResource());
                        assertEquals(
                                target.before(),
                                NebulaM3MethodSignalEvidenceRecipe.gitBlob(before),
                                path);
                    }
                });
    }

    @Test
    void exactPreimagesTransformAndSecondPassIsFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var targets = NebulaM3MethodSignalEvidenceRecipe.targetManifest();
        List<SourceFile> before =
                targets.entrySet().stream()
                        .filter(entry -> entry.getValue().beforeResource() != null)
                        .map(
                                entry ->
                                        parse(
                                                entry.getKey(),
                                                NebulaM3MethodSignalEvidenceRecipe.sourceImage(
                                                        entry.getValue().beforeResource()),
                                                context))
                        .toList();

        var first =
                new NebulaM3MethodSignalEvidenceRecipe()
                        .run(new InMemoryLargeSourceSet(before), context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(11, first.getChangeset().getAllResults().size());

        Map<String, SourceFile> after = new LinkedHashMap<>();
        first.getChangeset().getAllResults().forEach(
                result -> {
                    SourceFile source = result.getAfter();
                    assertTrue(source != null);
                    after.put(
                            source.getSourcePath().toString().replace('\\', '/'),
                            source);
                });
        assertEquals(targets.keySet(), after.keySet());
        targets.forEach(
                (path, target) ->
                        assertEquals(
                                target.after(),
                                NebulaM3MethodSignalEvidenceRecipe.gitBlob(
                                        after.get(path).printAll()),
                                path));

        var repeat =
                new NebulaM3MethodSignalEvidenceRecipe()
                        .run(
                                new InMemoryLargeSourceSet(
                                        List.copyOf(after.values())),
                                context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void foreignSourceIsNoOpAndAuthorityRemainsClosed() {
        var context = new InMemoryExecutionContext();
        SourceFile foreign =
                parse(
                        "src/main/java/example/Other.java",
                        "package example; final class Other {}\n",
                        context);
        var run =
                new NebulaM3MethodSignalEvidenceRecipe()
                        .run(new InMemoryLargeSourceSet(List.of(foreign)), context);
        assertTrue(run.getChangeset().getAllResults().isEmpty());

        var recipe = new NebulaM3MethodSignalEvidenceRecipe();
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.nativeExecutionAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    private static SourceFile parse(
            String path,
            String text,
            InMemoryExecutionContext context) {
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), text)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            assertEquals(1, files.size());
            return files.getFirst();
        }
    }
}
