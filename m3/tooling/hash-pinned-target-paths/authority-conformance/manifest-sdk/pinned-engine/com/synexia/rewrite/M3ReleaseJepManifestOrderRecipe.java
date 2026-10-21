// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact one-resource correction; reusable source-sealed providers remain unchanged. */
public final class M3ReleaseJepManifestOrderRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/release-jep-manifest-order-20261020/";
    }
    @Override public String getDisplayName() { return "Repair ordinal authority manifest input"; }
    @Override public String getDescription() { return "Preserves every complete source-bound row; rejects drift, missing and duplicate ownership."; }
}
