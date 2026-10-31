// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact-source installer; compiler/runtime gates remain external to source custody. */
public final class M3CompositeSummaryRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/composite-summary/";
    }
    @Override public String getDisplayName() {
        return "Install M3 composite UTF-16 summaries and structural operation repairs";
    }
    @Override public String getDescription() {
        return "Reuses the canonical source-seal engine for Java/JNI summary candidates, "
                + "rejecting drift without granting semantic equivalence or promotion.";
    }
}
