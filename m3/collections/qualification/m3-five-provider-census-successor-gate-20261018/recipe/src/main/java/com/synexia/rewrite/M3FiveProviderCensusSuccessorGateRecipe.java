/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;
/** One exact, behavior-preserving workflow guard transition over verified source genealogy. */
public final class M3FiveProviderCensusSuccessorGateRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-five-provider-census-successor-gate-20261018/";
    }
    @Override public String getDisplayName() {
        return "Allow only explicitly sealed source-census successor revisions";
    }
    @Override public String getDescription() {
        return "Retains historical exact postimage checks while admitting one pinned, exact "
                + "M3 five-provider indexed-materialization successor manifest.";
    }
}
