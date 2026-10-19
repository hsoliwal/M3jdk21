// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** A named, bounded application of the existing sealed Java snapshot engine. */
public final class M3ProgressScopeLintRecipe extends Recipe {
    @Override public String getDisplayName() { return "M3 borrowed progress scope source lint prerequisites"; }
    @Override public String getDescription() {
        return "Adds exactly five method-local source-retained try-warning annotations to four sealed owners after public documentation qualification.";
    }
    @Override public Set<String> getTags() {
        return Set.of("synexia", "m3", "recipe-first", "contract-preserving", "candidate-only");
    }
    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedJavaSnapshotRecipe("m3-progress-scope-lint-20261018"));
    }
    @Override public int maxCycles() { return 1; }
}
