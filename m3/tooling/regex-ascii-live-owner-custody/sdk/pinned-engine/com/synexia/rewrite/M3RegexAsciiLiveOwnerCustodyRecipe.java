// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Exact helper custody refinement; all original lexical SDK inputs stay unchanged. */
public final class M3RegexAsciiLiveOwnerCustodyRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/regex-ascii-live-owner-custody-20261020/forward/";
    }
    @Override public String getDisplayName() {
        return "Prepare exact lexical live-owner custody";
    }
    @Override public String getDescription() {
        return "Replays three reviewed host, document and workflow postimages, "
                + "with two original SDK KEEP seals and no compiler, test or mapping mutation.";
    }
}
