// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.rewrite;

/** Current-source successor; the existing manifest owner retains all admission. */
public final class M3ViewConvergence20261019 extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/jdk/internal/m3/rewrite/m3-view-convergence-20261019/";
    }

    @Override public String getDisplayName() {
        return "Converge M3JDK21 collection and viewport atoms";
    }

    @Override public String getDescription() {
        return "Receives source-sealed stable index ordering and checked viewport atoms "
                + "while retaining current collection contracts and Java/JNI ownership.";
    }
}

