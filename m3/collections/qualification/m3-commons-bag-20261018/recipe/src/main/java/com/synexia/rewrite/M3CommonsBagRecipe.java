/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

/** Reuses the established Synexia M3GpuToolJoinRecipe and SHA-256 manifest. */
public final class M3CommonsBagRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-commons-bag-20261018/";
    }
    @Override public String getDisplayName() {
        return "Generate Apache Commons Bag over existing M3CountedBag";
    }
    @Override public String getDescription() {
        return "Adds the genuine Apache Bag API implementation over the first-party M3 indexed counted owner, with donor differential and five-library tests.";
    }
}
