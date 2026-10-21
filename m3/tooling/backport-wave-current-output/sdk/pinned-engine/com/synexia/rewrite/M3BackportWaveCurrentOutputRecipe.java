// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact current-output consumer repair; generated source replay grants no promotion authority. */
public final class M3BackportWaveCurrentOutputRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/backport-wave-current-output-20261020/";
    }
    @Override public String getDisplayName() {
        return "Receive the qualified current backport wave hash consumer";
    }
    @Override public String getDescription() {
        return "Transforms one sealed workflow with nine unchanged owning inputs and historical receipt, "
                + "rejecting source drift, missing or duplicate ownership.";
    }
}
