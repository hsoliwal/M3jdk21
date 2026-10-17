/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Strict child of the existing String successor-chain family; candidate only. */
public final class M3StringUtf8SuccessorChainRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-string-utf8-successor-chain-20261017/";
    }
    @Override public String getDisplayName() {
        return "M3 String UTF8 successor chain";
    }
    @Override public String getDescription() {
        return "Admit the exact third UTF8 source transition after the historical precompute pin repair. "
                + "Preserve immutable mapping, successor receipts and runtime holds.";
    }
}
