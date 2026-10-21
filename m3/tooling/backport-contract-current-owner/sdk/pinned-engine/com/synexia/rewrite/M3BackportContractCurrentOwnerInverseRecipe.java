// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact reviewed backport contract fixtures; production policy and planning stay unchanged. */
public final class M3BackportContractCurrentOwnerInverseRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/backport-contract-current-owner-20261019/inverse/";
    }
    @Override public String getDisplayName() {
        return "Recover the exact current backport contract test fixtures";
    }
    @Override public String getDescription() {
        return "Replays three reviewed inverse test/document postimages and five exact KEEP inputs, "
                + "without changing production policy, planner or release authority.";
    }
}
