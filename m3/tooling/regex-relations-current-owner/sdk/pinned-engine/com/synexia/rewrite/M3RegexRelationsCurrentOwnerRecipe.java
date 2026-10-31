// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.synexia.rewrite;

/** Current-owner continuation; emits a staged candidate without granting runtime admission. */
public final class M3RegexRelationsCurrentOwnerRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-regex-relations-current-owner-20261017/";
    }
    @Override public String getDisplayName() {
        return "Restore the current M3 regex relation consumer";
    }
    @Override public String getDescription() {
        return "Restores the bounded relation consumer on one exact current owner while preserving "
                + "its existing caches, native advance, rolling fallback and unchanged custody inputs.";
    }
}
