// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact current source custody; numeric C11 and JNI execution require separate proof. */
public final class M3StateRelationCurrentC11Recipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/state-relation-current-c11-20261019/";
    }
    @Override public String getDisplayName() {
        return "Verify the exact current StateRelation C11 source closure";
    }
    @Override public String getDescription() {
        return "Preserves five exact current Java, C, header and proof owners, rejecting drift, "
                + "missing or duplicate ownership without generating product source.";
    }
}
