/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Retain the existing Synexia OpenRewrite source-sealed generator.
 * Only four ABSENT Java files are installed; no existing provider/source API
 * or original five-provider recipe postimage is changed. */
public final class M3FiveProviderDirectSpringRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/five-provider-direct-spring-20261017/";
    }
    @Override public String getDisplayName() {
        return "Receive direct M3 Spring MultiValueMap into five-provider SDK";
    }
    @Override public String getDescription() {
        return "Install M3 array-backed Spring MultiValueMap, cross-provider typed factories and JUnit contracts from sealed templates, with no duplicate storage owner.";
    }
}
