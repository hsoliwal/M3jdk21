// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** A named, bounded application of the existing sealed Java snapshot engine. */
public final class M3JavaBaseDoclintRecipe extends Recipe {
    @Override public String getDisplayName() { return "M3 java.base public documentation prerequisites"; }
    @Override public String getDescription() {
        return "Repairs only Javadoc comments on twelve sealed public java.base source owners; executable syntax, APIs and compiler policy are preserved.";
    }
    @Override public Set<String> getTags() {
        return Set.of("synexia", "m3", "recipe-first", "contract-preserving", "candidate-only");
    }
    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedJavaSnapshotRecipe("m3-java-base-doclint-20261018"));
    }
    @Override public int maxCycles() { return 1; }
}
