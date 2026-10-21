/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/**
 * Receive already-qualified Synexia M3 BiMap into current M3JDK21 master.
 * Only new files; source-sealed OpenRewrite PlainText results, no second engine.
 * Any subsequent existing-owner refinement must use an attributed Java LST recipe.
 */
public final class M3BiMapMainlineRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-bimap-mainline-20261017/";
    }
    @Override public String getDisplayName() {
        return "Receive qualified M3 Guava BiMap into canonical product SDK";
    }
    @Override public String getDescription() {
        return "Install five exact prequalified Java and test-only C source postimages while refusing occupied and drifting targets.";
    }
}
