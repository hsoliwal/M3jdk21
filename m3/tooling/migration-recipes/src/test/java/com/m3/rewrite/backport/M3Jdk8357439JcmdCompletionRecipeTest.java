// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3HashPinnedTextSnapshotRecipe;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3Jdk8357439JcmdCompletionRecipeTest {
    @Test
    void metadataPinsExactUpstreamIdentityAndHashPinnedChild() {
        var recipe = new M3Jdk8357439JcmdCompletionRecipe();

        assertEquals("JDK-8357439", M3Jdk8357439JcmdCompletionRecipe.JBS);
        assertEquals(
                "8549d1896054dd230ba3038c83bce23b10dcda22",
                M3Jdk8357439JcmdCompletionRecipe.UPSTREAM_COMMIT);
        assertEquals(
                "jdk-8357439-jcmd-completion",
                M3Jdk8357439JcmdCompletionRecipe.CRATE);
        assertTrue(recipe.getDisplayName().contains("JDK-8357439"));
        assertTrue(recipe.getDescription().contains("exact two-file"));
        assertTrue(recipe.getTags().contains("backport"));
        assertTrue(recipe.getTags().contains("candidate-only"));

        List<org.openrewrite.Recipe> children = recipe.getRecipeList();
        assertEquals(1, children.size());
        var text = (M3HashPinnedTextSnapshotRecipe) children.getFirst();
        assertEquals(
                M3Jdk8357439JcmdCompletionRecipe.CRATE,
                text.getCrateName());
    }
}
