// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite;

/** Target-specific binding; canonical source-seal implementation is mirrored unchanged. */
public final class M3MaskProjectionRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() { return "/com/synexia/rewrite/mask-projection/"; }
    @Override public String getDisplayName() { return "Refine M3JDK mask-driven projections"; }
    @Override public String getDescription() {
        return "Refines target-owned immutable index reuse and bounded JNI mask-first transfers.";
    }
}
