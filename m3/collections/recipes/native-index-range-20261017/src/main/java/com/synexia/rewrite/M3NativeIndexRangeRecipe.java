/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;
/** Reuses Synexia's exact-source recipe; no new transformation engine or product runtime. */
public final class M3NativeIndexRangeRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() { return "/com/synexia/rewrite/native-index-range-20261017/"; }
    @Override public String getDisplayName() { return "Refine M3 native index windows and stable sort reuse"; }
    @Override public String getDescription() {
        return "Refines existing native mask compaction and immutable index-window composition from source-sealed templates.";
    }
}
