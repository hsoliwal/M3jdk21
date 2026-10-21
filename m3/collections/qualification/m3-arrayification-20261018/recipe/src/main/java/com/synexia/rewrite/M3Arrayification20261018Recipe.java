// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Serial, source-sealed M3 arrayification recipe reusing the admitted OpenRewrite engine. */
public final class M3Arrayification20261018Recipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/arrayification-20261018/";
    }
    @Override public String getDisplayName() {
        return "Arrayify five-library M3 collection indexes using reviewed challenge categories";
    }
    @Override public String getDescription() {
        return "Refine the existing M3 five-provider indexed SoA owner through checked array/index/offset/mask atoms.";
    }
}
