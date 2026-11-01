/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.rewrite;
/** Single named source-sealed OpenRewrite rule, extending the incumbent M3 scanner. */
public final class M3ArrayMaskSparseRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/jdk/internal/m3/rewrite/m3-array-mask-sparse-20261018/";
    }
    @Override public String getDisplayName() { return "M3JDK21 java.base sparse bit-word compaction"; }
    @Override public String getDescription() {
        return "Refine one exact JDK-owned ArrayList/Vector mask atom without changing its public contracts.";
    }
}
