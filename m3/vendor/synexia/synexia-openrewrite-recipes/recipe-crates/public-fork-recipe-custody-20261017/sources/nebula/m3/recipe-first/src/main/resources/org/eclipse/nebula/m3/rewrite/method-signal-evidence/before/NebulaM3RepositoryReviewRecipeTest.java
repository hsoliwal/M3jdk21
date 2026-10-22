// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

class NebulaM3RepositoryReviewRecipeTest {
    @Test
    void reviewCompositionPreservesDeclaredOrderAndSingleCycle() {
        var recipe = new NebulaM3RepositoryReviewRecipe();
        assertEquals(1, recipe.maxCycles());
        assertEquals(
                List.of(NebulaM3InventoryRecipe.class,
                        NebulaM3FastSearchReviewRecipe.class,
                        NebulaM3JavaBeforeJniReviewRecipe.class),
                recipe.getRecipeList().stream().map(Object::getClass).toList());
    }

    @Test
    void everyCategoryEmitsSerialPlatformThenDonorOrder() {
        for (var category : NebulaM3FastSearchReviewPolicy.Category.values()) {
            var rows = NebulaM3FastSearchReviewPolicy.passes(category).stream()
                    .map(pass -> new NebulaM3FastSearchReviewTable.Row("Candidate.java", pass))
                    .toList();
            assertEquals(List.of(1, 2, 3, 4),
                    rows.stream().map(NebulaM3FastSearchReviewTable.Row::getPassOrder).toList());
            assertEquals(List.of("LEETCODE", "HACKERRANK", "GEEKSFORGEEKS", "GITHUB_DONOR"),
                    rows.stream().map(NebulaM3FastSearchReviewTable.Row::getEvidenceSource).toList());
            assertTrue(rows.stream().allMatch(row -> "READ_ONLY_EVIDENCE".equals(row.getAuthority())));
        }
    }

    @Test
    void actualCompositePreservesSourceAndKeepsNativeAuthorityClosed() {
        String source = """
                package p;
                public class Candidate {
                    private native int nativeCall();
                    public int find(String text) {
                        return text.indexOf("x");
                    }
                }
                """;
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> parsed;
        try (var stream = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of("widgets/demo/src/p/Candidate.java"), source)),
                null, context)) {
            parsed = stream.toList();
        }
        assertEquals(1, parsed.size());
        assertTrue(parsed.getFirst() instanceof J.CompilationUnit);
        assertEquals(source, parsed.getFirst().printAll());
        var run = new NebulaM3RepositoryReviewRecipe().run(
                new InMemoryLargeSourceSet(parsed), context);
        assertTrue(errors.isEmpty(), () -> errors.toString());
        assertTrue(run.getChangeset().getAllResults().isEmpty());
        List<NebulaM3InventoryTable.Row> inventory = run.getDataTableRows(NebulaM3InventoryTable.class);
        assertEquals(1, inventory.size());
        List<NebulaM3FastSearchReviewTable.Row> search = run.getDataTableRows(NebulaM3FastSearchReviewTable.class);
        assertEquals(List.of(1, 2, 3, 4),
                search.stream().map(NebulaM3FastSearchReviewTable.Row::getPassOrder).toList());
        assertTrue(search.stream().allMatch(row -> "READ_ONLY_EVIDENCE".equals(row.getAuthority())));
        List<NebulaM3JavaBeforeJniReviewTable.Row> nativeReview = run.getDataTableRows(NebulaM3JavaBeforeJniReviewTable.class);
        assertEquals(1, nativeReview.size());
        assertEquals(1, nativeReview.getFirst().getNativeMethods());
        assertTrue(nativeReview.getFirst().isJavaOracleRequired());
        assertTrue(nativeReview.getFirst().isDifferentialCorpusRequired());
        assertTrue(nativeReview.getFirst().isLifecycleFallbackRequired());
        assertFalse(nativeReview.getFirst().isNativeExecutionAuthority());
        assertFalse(nativeReview.getFirst().isPromotionAuthority());
    }
}
