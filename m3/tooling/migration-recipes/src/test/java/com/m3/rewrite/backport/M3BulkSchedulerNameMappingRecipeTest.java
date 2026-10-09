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

final class M3BulkSchedulerNameMappingRecipeTest {
    @Test
    void upgradesExactPriorEntryAndThenReachesFixedPoint() {
        PlainText source = PlainText.builder()
                .sourcePath(Path.of(M3BulkSchedulerNameMappingRecipe.PATH))
                .text("{\n  \"family_name_mapping\": [\n"
                        + M3BulkSchedulerNameMappingRecipe.OLD
                        + "\n  ]\n}\n")
                .build();
        M3BulkSchedulerNameMappingRecipe recipe =
                new M3BulkSchedulerNameMappingRecipe();
        var first = recipe.run(
                new InMemoryLargeSourceSet(List.of(source)),
                new InMemoryExecutionContext(),
                1);
        assertEquals(1, first.getChangeset().getAllResults().size());
        String after =
                first.getChangeset().getAllResults().getFirst().getAfter().printAll();
        assertTrue(after.contains(M3BulkSchedulerNameMappingRecipe.NEW));
        var second = recipe.run(
                new InMemoryLargeSourceSet(List.of(source.withText(after))),
                new InMemoryExecutionContext(),
                1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
        assertFalse(recipe.promotionAuthority());
    }
}
