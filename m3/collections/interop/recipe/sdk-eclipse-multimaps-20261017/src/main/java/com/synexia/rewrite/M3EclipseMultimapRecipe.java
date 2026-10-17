/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
package com.synexia.rewrite;

/** Reuses the proven hash-pinned OpenRewrite ScanningRecipe; no new rewrite engine. */
public final class M3EclipseMultimapRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/sdk-eclipse-multimap-20261017/";
    }

    @Override public String getDisplayName() {
        return "Receive three Eclipse multimap interfaces over existing M3 indexed owners";
    }

    @Override public String getDescription() {
        return "Qualify exact source-sealed Java21 Eclipse list/set/bag multimap additions, "
                + "keeping the already admitted M3 arrays, offsets, states and JNI ABI.";
    }
}
