/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Emits the current source-custody continuation without changing runtime acceptance. */
public final class M3RuntimeCurrentCustodyRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-runtime-current-custody-20261017/";
    }
    @Override public String getDisplayName() {
        return "Retain historical runtime replay and qualify exact current source custody";
    }
    @Override public String getDescription() {
        return "Preserves immutable historical manifest and patch owners, generates the bounded " +
                "current-cohort applier, tests and independent hosted proof, and refuses drift.";
    }
}
