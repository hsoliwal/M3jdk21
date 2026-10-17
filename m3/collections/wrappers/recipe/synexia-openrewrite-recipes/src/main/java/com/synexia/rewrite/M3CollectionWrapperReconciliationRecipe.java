// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

/** Resource binding for the existing Java and native/text snapshot owners. */
public final class M3CollectionWrapperReconciliationRecipe extends Recipe {
    public static final String CRATE = "m3-collection-wrapper-reconciliation-20261017";
    public static final String CUSTODY = "m3-collection-wrapper-custody-20261017";

    @Option(displayName = "Sealed wrapper crate", description = "Reviewed runtime or authoring custody packet.",
            example = "m3-collection-wrapper-reconciliation-20261017")
    private final String crateName;

    public M3CollectionWrapperReconciliationRecipe() { this(CRATE); }

    @JsonCreator
    public M3CollectionWrapperReconciliationRecipe(@JsonProperty("crateName") String crateName) {
        if (!CRATE.equals(crateName) && !CUSTODY.equals(crateName)) {
            throw new IllegalArgumentException("unreviewed wrapper crate");
        }
        this.crateName = crateName;
    }

    public String getCrateName() { return crateName; }

    @Override public String getDisplayName() { return "Reconcile existing M3 collection wrappers"; }

    @Override public String getDescription() {
        return "Applies reviewed per-file Java and JNI/text postimages to exact target preimages, "
                + "preserving current primitive owners and the established Synexia recipe authority.";
    }

    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedJavaSnapshotRecipe(crateName),
                new M3HashPinnedTextSnapshotRecipe(crateName));
    }

    @Override public int maxCycles() { return 1; }
}
