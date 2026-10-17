/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;
/** The existing Synexia source-sealed OpenRewrite engine, bound to five-library BiMap postimages. */
public final class M3FiveProviderBidiMapsRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-five-provider-bimaps-20261017/";
    }
    @Override public String getDisplayName() { return "Five-provider SDK bidirectional maps on M3 array/index owners"; }
    @Override public String getDescription() {
        return "Qualifies source-sealed Guava and Apache interface adapters, preserving the existing M3 JDK, Eclipse and Spring contract owners.";
    }
}
