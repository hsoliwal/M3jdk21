// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Source-sealed inverse restoration; native execution and performance require separate proof. */
public final class M3StateRelationSparseC11InverseRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/state-relation-sparse-c11-20261020/inverse/";
    }
    @Override public String getDisplayName() {
        return "Restore the exact positional StateRelation C11 atom";
    }
    @Override public String getDescription() {
        return "Transforms one sealed C owner while preserving four exact Java, header and proof owners, "
                + "rejecting source drift, missing or duplicate ownership.";
    }
}
