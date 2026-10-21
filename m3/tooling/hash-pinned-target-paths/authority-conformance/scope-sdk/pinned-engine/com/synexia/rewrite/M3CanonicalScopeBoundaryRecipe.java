// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact source-sealed scope candidate; canonical/product admission remains external. */
public final class M3CanonicalScopeBoundaryRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() { return "/com/synexia/rewrite/canonical-scope-boundary-20261020/"; }
    @Override public String getDisplayName() { return "Canonical scope boundary correction"; }
    @Override public String getDescription() { return "Replay one exact canonical scope owner and one unchanged enum with complete source custody."; }
}
