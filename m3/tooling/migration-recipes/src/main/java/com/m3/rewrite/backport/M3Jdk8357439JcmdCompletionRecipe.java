// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import com.synexia.rewrite.M3HashPinnedTextSnapshotRecipe;
import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * JDK-8357439: Add Bash Autocompletion for jcmd.
 *
 * <p>Upstream: openjdk/jdk commit 8549d1896054dd230ba3038c83bce23b10dcda22.
 * Target JDK21 preimages are both absent. The exact upstream postimages are carried by the
 * hash-pinned text crate and remain governed by OpenJDK's original file license headers.</p>
 */
public final class M3Jdk8357439JcmdCompletionRecipe extends Recipe {
    public static final String JBS = "JDK-8357439";
    public static final String UPSTREAM_COMMIT =
            "8549d1896054dd230ba3038c83bce23b10dcda22";
    public static final String CRATE = "jdk-8357439-jcmd-completion";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8357439 jcmd Bash completion";
    }

    @Override
    public String getDescription() {
        return "Replays the exact two-file OpenJDK JDK-8357439 postimage only when the JDK21 "
                + "target preimages are absent or already equal to the pinned output.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jcmd",
                "tooling",
                "hash-pinned",
                "candidate-only");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedTextSnapshotRecipe(CRATE));
    }
}
