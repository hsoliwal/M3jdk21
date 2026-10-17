/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;
/** The existing Synexia source-sealed OpenRewrite engine, bound to exact five-provider source-census postimages. */
public final class M3FiveProviderCurrentCensusRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-five-provider-current-census-20261017/";
    }
    @Override public String getDisplayName() { return "Five-provider current-source SDK admission across 34 indexed owners"; }
    @Override public String getDescription() {
        return "Seals 34 existing M3 SDK interop Java owners and tests while preserving the original five historical source identities.";
    }
}
