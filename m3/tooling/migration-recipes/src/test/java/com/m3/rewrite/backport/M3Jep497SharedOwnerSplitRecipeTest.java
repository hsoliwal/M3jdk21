// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3Jep497SharedOwnerSplitRecipeTest {
    private static final String WORKFLOW=".github/workflows/m3-jep497-mldsa-inventory.yml";
    private static final String README="m3/backports/recipes/jep-497-mldsa/README.md";
    private static final String MATERIALIZE="m3/backports/recipes/jep-497-mldsa/MATERIALIZE_PATHS.txt";
    private static final String SHARED="m3/backports/recipes/jep-497-mldsa/SHARED_OWNER_PATHS.txt";

    @Test void exactPriorInventoryTransformsAndReachesFixedPoint() {
        var recipe=new M3Jep497SharedOwnerSplitRecipe();
        List<SourceFile> before=List.of(
                text(WORKFLOW,recipe.before(WORKFLOW)),
                text(README,recipe.before(README)),
                text(MATERIALIZE,recipe.before(MATERIALIZE)));
        var first=recipe.run(new InMemoryLargeSourceSet(before),context(),1).getChangeset().getAllResults();
        assertEquals(4,first.size());
        List<SourceFile> after=new ArrayList<>();
        for(Result r:first)if(r.getAfter()!=null)after.add(r.getAfter());
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after),context(),1).getChangeset().getAllResults().isEmpty());
        assertEquals(3,recipe.after(MATERIALIZE).lines().filter(s->!s.isBlank()).count());
        assertEquals(4,recipe.after(SHARED).lines().filter(s->!s.isBlank()).count());
        assertFalse(recipe.after(MATERIALIZE).contains("KnownOIDs"));
        assertTrue(recipe.after(SHARED).contains("KnownOIDs"));
        assertTrue(recipe.after(WORKFLOW).contains("M3Jep497SharedOwnerSplitRecipeTest"));
    }

    @Test void hasNoProductOrPromotionAuthority(){
        var recipe=new M3Jep497SharedOwnerSplitRecipe();
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.promotionAuthority());
        assertEquals(4,recipe.targetPaths().size());
    }

    private static PlainText text(String path,String content){return PlainText.builder().sourcePath(Path.of(path)).text(content).build();}
    private static InMemoryExecutionContext context(){return new InMemoryExecutionContext(error->{throw new AssertionError(error);});}
}
