// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3BulkExecutorNameMappingRecipeTest {
    @Test
    void insertsOnceAndThenReachesFixedPoint() {
        PlainText source = PlainText.builder()
                .sourcePath(Path.of(M3BulkExecutorNameMappingRecipe.PATH))
                .text("{\n  \"family_name_mapping\": [\n    {\n      \"source_family\": \"existing\"\n    }\n  ],\n  \"recipe_authority\": {}\n}\n")
                .build();
        M3BulkExecutorNameMappingRecipe recipe = new M3BulkExecutorNameMappingRecipe();
        var first = recipe.run(
                new InMemoryLargeSourceSet(List.of(source)),
                new InMemoryExecutionContext(),
                1);
        assertEquals(1, first.getChangeset().getAllResults().size());
        String after = first.getChangeset().getAllResults().getFirst().getAfter().printAll();
        assertTrue(after.contains(M3BulkExecutorNameMappingRecipe.MARKER));
        PlainText applied = source.withText(after);
        var second = recipe.run(
                new InMemoryLargeSourceSet(List.of(applied)),
                new InMemoryExecutionContext(),
                1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
        assertFalse(recipe.promotionAuthority());
    }
}
