/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Exact fixture policy only; immutable donor bytes and hygiene commands remain owned. */
public final class M3QualificationFixtureWhitespacePolicyRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/";
    }
    @Override public String getDisplayName() {
        return "Preserve exact qualification fixture whitespace";
    }
    @Override public String getDescription() {
        return "Admit only four exact blank-at-eol fixture exceptions after read-only SDK seals, "
                + "preserving donor bytes and all existing hygiene commands.";
    }
}
