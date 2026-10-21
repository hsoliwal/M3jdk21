/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;
/** Same admitted hash-sealed OpenRewrite SourceFile recipe; only the manifest changes. */
public final class M3GuavaSetMultimapRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-guava-setmultimap-20261018/";
    }
    @Override public String getDisplayName() { return "M3 Guava SetMultimap over indexed owners"; }
    @Override public String getDescription() {
        return "Add source-sealed Guava SetMultimap and real-provider/JNI tests through existing M3 array/index owners.";
    }
}
