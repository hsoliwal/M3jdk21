// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Source-sealed, contract-local algebra for DOOP original-ordinal byte-mask views.
 * Reuses canonical SHA-256 admission, Java LST validation, and drift/fixed-point proof.
 */
public final class M3Jdk21LongMaskAlgebraRecipe extends Recipe {
    public static final String CRATE = "m3jdk21-long-mask-algebra-20261017";

    @Override public String getDisplayName() {
        return "Use exact-size source-row selection and composable unsigned-byte masks";
    }

    @Override public String getDescription() {
        return "Refine the existing M3LongMaskRows owner with forbidden masks, "
                + "source-ordinal rank/select, and a zero-intermediate Java index path. "
                + "Keep native selector and other collection contracts unchanged.";
    }

    @Override public Set<String> getTags() {
        return Set.of("synexia", "m3jdk21", "doop", "recipe-first", "array-offsets",
                "unsigned-byte-mask", "rank-select", "java21", "jni-reuse");
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedJavaSnapshotRecipe(CRATE));
    }

    @Override public int maxCycles() { return 1; }
}
