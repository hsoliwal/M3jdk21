/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.m3;

import com.synexia.rewrite.M3GpuToolJoinRecipe;

/** Source-sealed five-provider prepared bitmap/offset views; existing recipe kernel reused. */
public final class M3FiveLibraryPreparedRowsRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/five-library-prepared-rows-20261018/";
    }
    @Override public String getDisplayName() {
        return "Prepare five-family M3 filtered and sorted row index views";
    }
    @Override public String getDescription() {
        return "Adds immutable cached per-occurrence bit algebra with existing M3 payload/" +
                " JNI owners and JDK/Guava/Eclipse/Spring/Apache contract-specific factories" +
                " from exact SHA-256 sealed templates; existing owners remain unchanged.";
    }
}
