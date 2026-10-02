// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.donor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

final class M3PinnedDonorInlineRecipeTest {
    @Test
    void pinnedM3IndexDbCrateCarriesProvenanceAndReplaysToFixedPoint() {
        var recipe = new M3PinnedDonorInlineRecipe("m3indexdb-artifact");
        var donor = recipe.donor();

        assertEquals("m3indexdb-artifact", recipe.crateName());
        assertEquals("https://github.com/hsoliwal/com.synexia", donor.repository());
        assertEquals("bade19f37311f367101cc44ae44491476f2865ca", donor.revision());
        assertEquals("Apache-2.0", donor.license());
        assertEquals("com.m3.indexdb", donor.targetNamespace());
        assertEquals("donor-inline", donor.dagId());
        assertEquals(64, donor.sourceSha256().length());
        assertTrue(recipe.getRecipeList().getFirst() instanceof M3HashPinnedJavaSnapshotRecipe);

        var first = recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 3);
        var results = first.getChangeset().getAllResults();
        assertEquals(2, results.size());

        List<SourceFile> generated = results.stream().map(result -> result.getAfter()).toList();
        assertTrue(generated.stream().allMatch(java.util.Objects::nonNull));
        assertTrue(generated.stream()
                .map(file -> file.getSourcePath().toString().replace('\\', '/'))
                .toList()
                .containsAll(List.of(
                        "src/main/java/com/m3/indexdb/M3IndexDbArtifact.java",
                        "src/test/java/com/m3/indexdb/M3IndexDbArtifactTest.java")));

        var second = recipe.run(new InMemoryLargeSourceSet(generated), context(), 3);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void crateValidationRejectsUnknownCrates() {
        assertFalse(new M3PinnedDonorInlineRecipe("m3indexdb-artifact").getTags().isEmpty());
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new M3PinnedDonorInlineRecipe("../bad"));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new M3PinnedDonorInlineRecipe("missing-crate"));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
