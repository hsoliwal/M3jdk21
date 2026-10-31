// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact source admission; runtime and native execution remain separate proofs. */
public final class M3TextMetadataCurrentOwnerRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/text-metadata-current-owner-20261019/";
    }
    @Override public String getDisplayName() {
        return "Verify the exact current TextMetadata JNI source tuple";
    }
    @Override public String getDescription() {
        return "Preserves six exact current owners, rejecting historical, mixed, missing, "
                + "duplicate or unknown inputs without rewriting production code.";
    }
}
