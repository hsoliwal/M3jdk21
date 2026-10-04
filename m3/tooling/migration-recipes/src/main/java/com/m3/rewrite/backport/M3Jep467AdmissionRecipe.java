// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Replays only the JEP 467 control-plane admission decision.
 *
 * <p>The two child FILE atoms refine catalogue rationale and work-queue scope. They do not apply
 * any OpenJDK product source. Product materialization remains a separate LIBRARY_API packet.</p>
 */
public final class M3Jep467AdmissionRecipe extends Recipe {
    @Override public String getDisplayName() {
        return "Classify JEP 467 Markdown documentation comments";
    }

    @Override public String getDescription() {
        return "Replays the exact JEP 467 catalogue and queue admission state while preserving "
                + "the 250-file product packet as a separate later authority boundary.";
    }

    @Override public Set<String> getTags() {
        return Set.of("m3", "jdk21", "jep-467", "admission", "recipe-first",
                "file-atoms", "module-join", "candidate-only");
    }

    @Override public int maxCycles() { return 1; }

    @Override public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedTextSnapshotRecipe("jep467-admission-catalogue"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jep467-admission-queue"));
    }
}
