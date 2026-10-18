// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Named, source-sealed OpenRewrite recipe reusing the existing Synexia engine. */
public final class M3FiveProviderCountsRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/five-provider-count-recovery-20261018/";
    }
    @Override public String getDisplayName() {
        return "Reconcile five-provider indexed M3 shared count and flag owners";
    }
    @Override public String getDescription() {
        return "Restore a previously qualified M3 primitive owner, reuse existing M3CountedBag across real providers, and refuse preimage drift.";
    }
}
