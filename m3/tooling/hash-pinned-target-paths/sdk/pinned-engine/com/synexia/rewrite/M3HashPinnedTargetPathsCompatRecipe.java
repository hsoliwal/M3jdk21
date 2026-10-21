// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact current-output consumer repair; generated source replay grants no promotion authority. */
public final class M3HashPinnedTargetPathsCompatRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/hash-pinned-target-paths-20261020/";
    }
    @Override public String getDisplayName() {
        return "Restore the expected canonical target path accessor";
    }
    @Override public String getDescription() {
        return "Transforms one sealed provider with six unchanged owning test and crate inputs, "
                + "rejecting source drift, missing or duplicate ownership.";
    }
}
