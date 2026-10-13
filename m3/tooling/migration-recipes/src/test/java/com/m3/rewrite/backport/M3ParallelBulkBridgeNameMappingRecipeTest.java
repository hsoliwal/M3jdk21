// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3ParallelBulkBridgeNameMappingRecipeTest {
    private static final String OLD =
            "{\n  \"family_name_mapping\": [\n    {\n"
                    + "      \"source_family\": \"existing\"\n    }"
                    + "\n\n  ],\n  \"recipe_authority\": {}\n}\n";

    @Test
    void addsOnlyOptionalMappingAndThenReachesFixedPoint() {
        PlainText source = file(OLD);
        var recipe = new M3ParallelBulkBridgeNameMappingRecipe();
        var first = recipe.run(new InMemoryLargeSourceSet(List.of(source)),
                new InMemoryExecutionContext(), 1);
        assertEquals(1, first.getChangeset().getAllResults().size());
        String after = first.getChangeset().getAllResults().getFirst().getAfter().printAll();
        assertTrue(after.contains(M3ParallelBulkBridgeNameMappingRecipe.MARKER));
        assertTrue(after.contains("\"source_family\": \"existing\""));
        assertTrue(after.contains("\"recipe_authority\": {}"));
        var replay = new M3ParallelBulkBridgeNameMappingRecipe().run(
                new InMemoryLargeSourceSet(List.of(file(after))),
                new InMemoryExecutionContext(), 1);
        assertTrue(replay.getChangeset().getAllResults().isEmpty());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void ignoresOtherSourcePathsWithoutMutation() {
        PlainText other = PlainText.builder().sourcePath(Path.of("not/name-mapping.json"))
                .text(OLD).build();
        var run = new M3ParallelBulkBridgeNameMappingRecipe().run(
                new InMemoryLargeSourceSet(List.of(other)),
                new InMemoryExecutionContext(), 1);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
    }

    private static PlainText file(String source) {
        return PlainText.builder()
                .sourcePath(Path.of(M3ParallelBulkBridgeNameMappingRecipe.PATH))
                .text(source).build();
    }
}
