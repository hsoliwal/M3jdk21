// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Named dataset over the unchanged, source-sealed canonical recipe engine. */
public final class M3IntegratedUtilitiesRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3jdk21-integrated-utilities-20261018/";
    }
    @Override public String getDisplayName() {
        return "Receive integrated M3JDK21 utility and collection source";
    }
    @Override public String getDescription() {
        return "Receives exact target-owned utility, collection, JNI, catalogue and verification "
                + "postimages through existing source-seal admission and actual OpenRewrite Results.";
    }
}
