/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Restores one omitted test resource from the byte-exact existing compiler authority. */
public final class M3JavacRegexOwnerFixtureRecoveryRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/";
    }
    @Override public String getDisplayName() {
        return "Recover the exact javac regex compiler owner fixture";
    }
    @Override public String getDescription() {
        return "Adds the missing whole-owner fixture while preserving the existing compiler owner, "
                + "original three JUnit tests and historical manifest/source-seal authority.";
    }
}
