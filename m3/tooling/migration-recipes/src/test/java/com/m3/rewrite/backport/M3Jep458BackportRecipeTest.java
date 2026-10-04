// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class M3Jep458BackportRecipeTest {
    private static final String JAVA_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-jep458-multifile-java/";
    private static final String TEXT_ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned-text/jdk22-jep458-multifile-text/";

    @Test
    void compositeRecipePinsDonorCratesAndMultiModuleScope() {
        Recipe recipe = new M3Jep458BackportRecipe();

        assertEquals(
                "517b1788198fc325961df61161f9b365c7b2524e",
                M3Jep458BackportRecipe.UPSTREAM_COMMIT);
        assertEquals(2, recipe.getRecipeList().size());

        var javaRecipe = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class, recipe.getRecipeList().get(0));
        var textRecipe = assertInstanceOf(
                M3Jdk21HashPinnedTextSnapshotRecipe.class, recipe.getRecipeList().get(1));

        assertEquals("jdk22-jep458-multifile-java", javaRecipe.getCrateName());
        assertEquals("jdk22-jep458-multifile-text", textRecipe.getCrateName());
        assertEquals(
                M3EditScope.MULTI_MODULE,
                M3RecipeScopeRegistry.require(M3Jep458BackportRecipe.class).declaredScope());
        assertTrue(recipe.getTags().contains("multi-module"));
        assertTrue(recipe.getTags().contains("hash-pinned"));
    }

    @Test
    void reviewedPostimagesAreAlreadyACompositeFixedPoint() throws Exception {
        M3Jep458BackportRecipe recipe = new M3Jep458BackportRecipe();
        List<SourceFile> sources = new ArrayList<>();
        sources.addAll(javaPostimages());
        sources.addAll(textPostimages());

        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(sources), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void driftedReviewedPostimageFailsClosed() throws Exception {
        M3Jep458BackportRecipe recipe = new M3Jep458BackportRecipe();
        List<SourceFile> sources = new ArrayList<>();
        sources.addAll(javaPostimages());
        sources.addAll(textPostimages());

        SourceFile first = sources.getFirst();
        sources.set(
                0,
                PlainText.builder()
                        .sourcePath(first.getSourcePath())
                        .text(first.printAll() + "// drift\n")
                        .build());

        assertThrows(
                RuntimeException.class,
                () -> recipe.run(new InMemoryLargeSourceSet(sources), context(), 1)
                        .getChangeset()
                        .getAllResults());
    }

    private static List<SourceFile> javaPostimages() throws Exception {
        List<SourceFile> result = new ArrayList<>();
        for (ManifestRow row : manifest(JAVA_ROOT)) {
            String text = resource(JAVA_ROOT + row.resource());
            result.addAll(
                    JavaParser.fromJavaVersion()
                            .build()
                            .parseInputs(
                                    List.of(Parser.Input.fromString(Path.of(row.path()), text)),
                                    null,
                                    context())
                            .toList());
        }
        return result;
    }

    private static List<SourceFile> textPostimages() throws Exception {
        List<SourceFile> result = new ArrayList<>();
        for (ManifestRow row : manifest(TEXT_ROOT)) {
            result.add(
                    PlainText.builder()
                            .sourcePath(Path.of(row.path()))
                            .text(resource(TEXT_ROOT + row.resource()))
                            .build());
        }
        return result;
    }

    private static List<ManifestRow> manifest(String root) throws Exception {
        List<ManifestRow> rows = new ArrayList<>();
        for (String line : resource(root + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\\t", -1);
            rows.add(new ManifestRow(cells[0], cells[3]));
        }
        return List.copyOf(rows);
    }

    private record ManifestRow(String path, String resource) {}

    private static String resource(String name) throws IOException {
        try (var input = M3Jep458BackportRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) throw new IOException("missing resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
