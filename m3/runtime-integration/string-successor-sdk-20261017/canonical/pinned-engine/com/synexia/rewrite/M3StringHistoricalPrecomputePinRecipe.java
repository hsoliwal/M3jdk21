/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Strict child of the existing String successor-chain family; candidate only. */
public final class M3StringHistoricalPrecomputePinRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-string-historical-precompute-pin-20261017/";
    }
    @Override public String getDisplayName() {
        return "M3 String historical precompute target pin";
    }
    @Override public String getDescription() {
        return "Validate the historical PRECOMPUTE_CACHE target independently of the V2 live owner. "
                + "Preserve immutable mapping, successor receipts and runtime holds.";
    }
}
