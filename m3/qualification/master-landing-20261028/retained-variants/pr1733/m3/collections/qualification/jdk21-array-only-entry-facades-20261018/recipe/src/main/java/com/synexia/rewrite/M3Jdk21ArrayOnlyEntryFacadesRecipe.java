/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

/** The existing source-sealed OpenRewrite engine, applied to M3 JDK21 array-only facade atoms. */
public final class M3Jdk21ArrayOnlyEntryFacadesRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/jdk21-array-only-entry-facades-20261018/";
    }
    @Override public String getDisplayName() {
        return "Generate the M3 JDK21 array-only LFU and API-created Entry facades";
    }
    @Override public String getDescription() {
        return "Preserve raw JDK source custody while adding source-sealed, array-only "
                + "LFU metadata and opt-in Map.Entry API boundary snapshots.";
    }
}
