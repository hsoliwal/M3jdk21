// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

class M3HierarchicalDocumentationRecipeTest {
    private static ExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
    private static PlainText text(String path, String body) {
        return PlainText.builder().id(Tree.randomId()).sourcePath(Path.of(path)).text(body).build();
    }
    private static SourceFile java(String body) {
        return JavaParser.fromJavaVersion().build().parse(body).findFirst().orElseThrow()
                .withSourcePath(Path.of("feature/src/main/java/p/C.java"));
    }
    private static List<SourceFile> fixture() {
        return List.of(text("pom.xml", "<project/>"),
                text("feature/pom.xml", "<project/>"),
                java("package p; class C { int m() { return 1; } }"));
    }
    private static Map<String, SourceFile> accumulate(List<SourceFile> start,
                                                       M3HierarchicalDocumentationRecipe recipe) {
        var result = recipe.run(new InMemoryLargeSourceSet(start), context());
        Map<String, SourceFile> all = new TreeMap<>();
        for (SourceFile source : start) all.put(source.getSourcePath().toString(), source);
        result.getChangeset().getAllResults().forEach(change -> {
            if (change.getAfter() != null) all.put(change.getAfter().getSourcePath().toString(), change.getAfter());
        });
        return all;
    }
    @Test void createsAllLevelsWithoutChangingSourcesAndReplaysAtFixedPoint() {
        var recipe = new M3HierarchicalDocumentationRecipe("fixture-project", "fixture-repository");
        var before = fixture();
        var all = accumulate(before, recipe);
        assertEquals(16, all.size()); // 3 sources, 13 generated L0–L5 documents.
        for (SourceFile original : before) {
            assertEquals(original.printAll(), all.get(original.getSourcePath().toString()).printAll());
        }
        assertTrue(all.containsKey(".m3/generated-docs/L0/feature/src/main/java/p/C.java.md"));
        assertTrue(all.containsKey(".m3/generated-docs/L1/feature/production/java/p.md"));
        assertTrue(all.get(".m3/generated-docs/L0/feature/src/main/java/p/C.java.md").printAll().contains("method:m/"));
        assertEquals(0, recipe.run(new InMemoryLargeSourceSet(new ArrayList<>(all.values())),context())
                .getChangeset().getAllResults().size());
    }
    @Test void sourceDriftChangesOnlyDerivedDocsAndUnownedOutputIsRejected() {
        var recipe = new M3HierarchicalDocumentationRecipe("fixture-project", "fixture-repository");
        var all = accumulate(fixture(), recipe);
        all.put("feature/src/main/java/p/C.java", java("package p; class C { int m() { return 2; } }"));
        var changed = recipe.run(new InMemoryLargeSourceSet(new ArrayList<>(all.values())), context());
        assertFalse(changed.getChangeset().getAllResults().isEmpty());
        all.put(".m3/generated-docs/L0/feature/src/main/java/p/C.java.md",
                text(".m3/generated-docs/L0/feature/src/main/java/p/C.java.md", "Human authored, no M3 marker"));
        assertThrows(RuntimeException.class,
                () -> recipe.run(new InMemoryLargeSourceSet(new ArrayList<>(all.values())), context()));
    }
    @Test void duplicatePathAndSilentDeletionAreRejected() {
        var recipe = new M3HierarchicalDocumentationRecipe("project", "repo");
        var fixture = fixture();
        var duplicate = new ArrayList<>(fixture);
        duplicate.add(fixture.getFirst());
        assertThrows(RuntimeException.class,
                () -> recipe.run(new InMemoryLargeSourceSet(duplicate), context()));
        var all = accumulate(fixture, recipe);
        all.remove("feature/src/main/java/p/C.java");
        assertThrows(RuntimeException.class,
                () -> recipe.run(new InMemoryLargeSourceSet(new ArrayList<>(all.values())), context()));
    }
}
