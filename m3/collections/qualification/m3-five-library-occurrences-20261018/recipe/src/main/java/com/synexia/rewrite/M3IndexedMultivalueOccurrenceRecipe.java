/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Hash-sealed serial M3JDK21 existing-owner refinement; no parallel rewrite engine. */
public final class M3IndexedMultivalueOccurrenceRecipe extends M3GpuToolJoinRecipe {
    public M3IndexedMultivalueOccurrenceRecipe() { }
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-five-library-occurrences-20261018/";
    }
    @Override public String getDisplayName() {
        return "Integrate M3JDK21 indexed multivalue occurrence mutation";
    }
    @Override public String getDescription() {
        return "Source-sealed Object[] payload and int[]/byte[] index-state owner changes.";
    }
}
