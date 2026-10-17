/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Qualifies canonical mapping through the existing bounded hosted compiler worker. */
public final class M3CanonicalMappingHostedWorkflowRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/";
    }
    @Override public String getDisplayName() {
        return "Qualify canonical mapping with preserved hosted compiler recipe proofs";
    }
    @Override public String getDescription() {
        return "Retains the immutable predecessor workflow SDK and existing compiler proofs, " +
                "adds mapping and successor recipe qualification, and preserves bounded execution controls.";
    }
}
