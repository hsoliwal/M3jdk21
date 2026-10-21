// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact proof-host and runtime custody refinement; original product SDK stays unchanged. */
public final class M3BackportAppliedOwnerCustodyInverseRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/backport-applied-owner-custody-20261020/inverse/";
    }
    @Override public String getDisplayName() {
        return "Recover exact prior proof-host custody";
    }
    @Override public String getDescription() {
        return "Recovers six exact prior proof-host, runtime, metadata, document and workflow bodies, "
                + "with two original SDK KEEP seals and no production policy or release-authority change.";
    }
}
