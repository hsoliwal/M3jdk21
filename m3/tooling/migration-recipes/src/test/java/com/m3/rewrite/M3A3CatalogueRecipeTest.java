// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

class M3A3CatalogueRecipeTest {

    @Test
    void exactPreimagesProduceReviewedPacketAndSecondPassIsFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> before =
                List.of(
                        java(
                                "m3/tooling/a3/src/main/java/com/m3/a3/A3.java",
                                M3A3CatalogueRecipe.resource("before-A3.java"),
                                context),
                        text(
                                "m3/tooling/a3/README.md",
                                M3A3CatalogueRecipe.resource("before-README.md")),
                        text(
                                "m3/docs/a3.md",
                                M3A3CatalogueRecipe.resource("before-a3.md")));

        var first =
                new M3A3CatalogueRecipe()
                        .run(new InMemoryLargeSourceSet(before), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var results = first.getChangeset().getAllResults();
        assertEquals(7, results.size());

        Map<String, SourceFile> after = new HashMap<>();
        for (SourceFile source : before) {
            after.put(source.getSourcePath().toString().replace('\\', '/'), source);
        }
        results.forEach(
                result -> {
                    SourceFile postimage = result.getAfter();
                    assertTrue(postimage != null);
                    after.put(
                            postimage.getSourcePath().toString().replace('\\', '/'),
                            postimage);
                });

        assertEquals(7, after.size());
        assertTrue(
                after.get("m3/tooling/a3/src/main/java/com/m3/a3/A3.java")
                        .printAll()
                        .contains("case \"cat\""));
        assertTrue(
                after.get("m3/tooling/a3/src/main/java/com/m3/a3/A3Cat.java")
                        .printAll()
                        .contains("source-available donor must remain REFERENCE_ONLY"));
        assertTrue(
                after.get("m3/docs/openrewrite-recipe-catalogue.md")
                        .printAll()
                        .contains("Pinned GitHub donor evidence"));

        var second =
                new M3A3CatalogueRecipe()
                        .run(
                                new InMemoryLargeSourceSet(
                                        after.values().stream()
                                                .sorted(
                                                        java.util.Comparator.comparing(
                                                                value ->
                                                                        value.getSourcePath()
                                                                                .toString()))
                                                .toList()),
                                context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void unexpectedPreimageFailsClosedAndRecipeHasNoProductAuthority() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> drift =
                List.of(
                        java(
                                "m3/tooling/a3/src/main/java/com/m3/a3/A3.java",
                                "package com.m3.a3; public final class A3 {}\n",
                                context),
                        text(
                                "m3/tooling/a3/README.md",
                                M3A3CatalogueRecipe.resource("before-README.md")),
                        text(
                                "m3/docs/a3.md",
                                M3A3CatalogueRecipe.resource("before-a3.md")));

        boolean threw = false;
        try {
            new M3A3CatalogueRecipe()
                    .run(new InMemoryLargeSourceSet(drift), context);
        } catch (IllegalStateException expected) {
            threw = true;
        }
        assertTrue(threw || !errors.isEmpty());

        M3A3CatalogueRecipe recipe = new M3A3CatalogueRecipe();
        assertFalse(recipe.productMutationAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void reviewedResourceBlobsMatchManifest() {
        String manifest = M3A3CatalogueRecipe.resource("manifest.tsv");
        int rows = 0;
        for (String line : manifest.lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] cells = line.split("\t", -1);
            assertEquals(5, cells.length);
            String text = M3A3CatalogueRecipe.resource(cells[4]);
            assertEquals(cells[3], M3A3CatalogueRecipe.gitBlob(text));
            rows++;
        }
        assertEquals(7, rows);
    }

    private static SourceFile java(
            String path,
            String source,
            InMemoryExecutionContext context) {
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path), source)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            assertEquals(1, files.size());
            assertEquals(source, files.getFirst().printAll());
            return files.getFirst();
        }
    }

    private static PlainText text(String path, String source) {
        return PlainText.builder()
                .sourcePath(Path.of(path))
                .text(source)
                .build();
    }
}
