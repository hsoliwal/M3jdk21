// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

class M3StringMatchesJavadocRepairRecipeTest {
    @Test
    void exactPreimageTransformsAndSecondPassIsFixedPoint() {
        var errors=new java.util.ArrayList<Throwable>();
        var ctx=new InMemoryExecutionContext(errors::add);
        SourceFile before=parse(M3StringMatchesJavadocRepairRecipe.beforeImage(),ctx);
        var first=new M3StringMatchesJavadocRepairRecipe()
                .run(new InMemoryLargeSourceSet(List.of(before)),ctx);
        assertTrue(errors.isEmpty(),errors.toString());
        var changes=first.getChangeset().getAllResults();
        assertEquals(1,changes.size());
        SourceFile after=changes.getFirst().getAfter();
        assertEquals(M3StringMatchesJavadocRepairRecipe.afterImage(),after.printAll());
        assertTrue(after.printAll().contains("*/\n    public boolean matches(String regex)"));
        var second=new M3StringMatchesJavadocRepairRecipe()
                .run(new InMemoryLargeSourceSet(List.of(after)),ctx);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
        assertFalse(new M3StringMatchesJavadocRepairRecipe().publicApiChangeAuthority());
        assertFalse(new M3StringMatchesJavadocRepairRecipe().behaviorChangeAuthority());
        assertFalse(new M3StringMatchesJavadocRepairRecipe().promotionAuthority());
    }
    private static SourceFile parse(String source, InMemoryExecutionContext ctx) {
        try (var parsed=JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(M3StringMatchesJavadocRepairRecipe.TARGET),source)),
                null,ctx)) {
            return parsed.toList().getFirst();
        }
    }
}
