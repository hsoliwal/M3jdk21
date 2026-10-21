/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

/** Maven/OpenRewrite source-sealed Guava BiMap SDK handoff; no parallel transform engine. */
public final class M3GuavaBiMapRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-guava-bimap-20261017/";
    }
    @Override public String getDisplayName() {
        return "Receive M3 array-indexed Guava BiMap with verified names and live views";
    }
    @Override public String getDescription() {
        return "Preserve the original Guava BiMap API; install the M3 owner, factory, mappings and differential tests only on exact, sealed preimages.";
    }
}
