// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact helper custody refinement; all original lexical SDK inputs stay unchanged. */
public final class M3RegexAsciiLiveOwnerCustodyInverseRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/regex-ascii-live-owner-custody-20261020/inverse/";
    }
    @Override public String getDisplayName() {
        return "Recover exact prior lexical helper custody";
    }
    @Override public String getDescription() {
        return "Recovers three exact prior host, document and workflow bodies, "
                + "with two original SDK KEEP seals and no compiler, test or mapping mutation.";
    }
}
