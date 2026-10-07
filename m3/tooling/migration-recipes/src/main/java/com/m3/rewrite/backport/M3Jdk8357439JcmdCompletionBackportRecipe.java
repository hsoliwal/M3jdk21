// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Exact Java-21-compatible replay unit for upstream JDK-8357439.
 *
 * <p>Upstream donor: openjdk/jdk commit
 * {@code 8549d1896054dd230ba3038c83bce23b10dcda22}. Both target paths were absent in
 * the JDK21 baseline, so this recipe is a two-file additive UTF-8 text crate. The already-merged
 * product bytes are byte-identical to the upstream Git blobs recorded in
 * {@code m3/backports/recipes/jdk-8357439/manifest.tsv}.</p>
 */
public final class M3Jdk8357439JcmdCompletionBackportRecipe extends Recipe {
    public static final String UPSTREAM_COMMIT =
            "8549d1896054dd230ba3038c83bce23b10dcda22";
    public static final String CRATE = "jdk27-jcmd-8357439";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8357439 jcmd bash completion";
    }

    @Override
    public String getDescription() {
        return "Replays the exact additive jdk.jcmd bash-completion build rule and script "
                + "from ABSENT JDK21 preimages using a hash-pinned UTF-8 text crate.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jdk-8357439",
                "jcmd",
                "tooling",
                "bash-completion",
                "hash-pinned",
                "additive",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }
}
