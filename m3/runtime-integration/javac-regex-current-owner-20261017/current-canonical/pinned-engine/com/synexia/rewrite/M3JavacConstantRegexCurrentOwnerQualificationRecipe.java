// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/**
 * Source-sealed current-owner qualification; production/compiler authority stays unchanged.
 * Reuses existing JDK-only compiler facts and additive CharSequence contract preparation.
 */
public final class M3JavacConstantRegexCurrentOwnerQualificationRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/";
    }
    @Override public String getDisplayName() {
        return "Qualify current javac constant regex owners without production changes";
    }
    @Override public String getDescription() {
        return "Adds a source-pinned four-file JDK21 qualification of current compiler facts and " +
                "CharSequence regex contracts while preserving production owners and historical proofs.";
    }
}
