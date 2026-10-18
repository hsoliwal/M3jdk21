/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

/** Serial, source-sealed OpenRewrite receiver for five-provider indexed materializations. */
public final class M3FiveProviderIndexedProjectionsRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-five-provider-indexed-projections-20261018/";
    }
    @Override public String getDisplayName() {
        return "Compose M3 indexed state-mask snapshots into five vendor collection contracts";
    }
    @Override public String getDescription() {
        return "Four preimage-sealed successor files: one M3 physical-index owner, a real-provider "
                + "differential oracle, full 34-file source census and its fail-closed verification.";
    }
}
