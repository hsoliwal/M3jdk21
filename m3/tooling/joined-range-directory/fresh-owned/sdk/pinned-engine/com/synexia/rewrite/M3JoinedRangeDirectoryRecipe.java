// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite;

/** Existing prefix-directory routing; no public interface or backing ownership change. */
public final class M3JoinedRangeDirectoryRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/joined-range-directory-20261020/forward/";
    }
    @Override public String getDisplayName() {
        return "Route joined ranges with the existing prefix directory";
    }
    @Override public String getDescription() {
        return "Replays one exact joined-range refinement, "
                + "preserving four source owners and all public range/backing contracts.";
    }
}
