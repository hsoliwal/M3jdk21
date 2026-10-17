// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Synexia-owned successor for the existing java.util CollectionViews receiver. */
public final class M3JavaUtilViewContractsRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/java-util-view-contracts/";
    }
    @Override public String getDisplayName() { return "M3 java.util view contracts"; }
    @Override public String getDescription() {
        return "Preserves typed filtering metadata, bounded counts and the JNI verifier's complete validation.";
    }
}
