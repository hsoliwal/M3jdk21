// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact proof-host and runtime custody refinement; original product SDK stays unchanged. */
public final class M3BackportAppliedOwnerCustodyRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/backport-applied-owner-custody-20261020/forward/";
    }
    @Override public String getDisplayName() {
        return "Prepare exact applied-owner proof custody";
    }
    @Override public String getDescription() {
        return "Replays six reviewed proof-host, runtime, metadata, document and workflow postimages, "
                + "with two original SDK KEEP seals and no production policy or release-authority change.";
    }
}
