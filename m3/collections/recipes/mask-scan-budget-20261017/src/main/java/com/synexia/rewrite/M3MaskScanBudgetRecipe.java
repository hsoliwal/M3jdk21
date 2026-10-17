/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;
/** Existing source-sealed recipe, with an additive source-budget API and shared native scan atom. */
public final class M3MaskScanBudgetRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() { return "/com/synexia/rewrite/mask-scan-budget-20261017/"; }
    @Override public String getDisplayName() { return "Bound M3 mask source scanning without changing legacy scans"; }
    @Override public String getDescription() { return "Adds cancellation-aware budgeted index-mask pages from exact source-sealed templates."; }
}
