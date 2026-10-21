// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite;

/** Existing prefix-directory routing; no public interface or backing ownership change. */
public final class M3JoinedRangeDirectoryInverseRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/joined-range-directory-20261020/inverse/";
    }
    @Override public String getDisplayName() {
        return "Recover the exact original joined-range scan";
    }
    @Override public String getDescription() {
        return "Recovers one exact original joined-range implementation, "
                + "preserving four source owners and all public range/backing contracts.";
    }
}
