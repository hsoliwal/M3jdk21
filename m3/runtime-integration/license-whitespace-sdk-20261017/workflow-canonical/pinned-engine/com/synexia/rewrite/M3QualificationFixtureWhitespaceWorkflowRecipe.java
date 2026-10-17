/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Qualifies the exact source-owned policy SDK without changing its emitted policy files. */
public final class M3QualificationFixtureWhitespaceWorkflowRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-qualification-fixture-whitespace-workflow-20261017/";
    }
    @Override public String getDisplayName() {
        return "Qualify exact license-fixture whitespace SDK";
    }
    @Override public String getDescription() {
        return "Appends two bounded qualification and evidence steps after existing JNI parity, " +
                "preserving every existing workflow byte and the original source-sealed provider.";
    }
}
