// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.rewrite;

/** Existing Swing host lifetime; the canonical source-seal owner retains admission. */
public final class M3SwingConvergence20261019 extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/jdk/internal/m3/rewrite/m3-swing-convergence-20261019/";
    }

    @Override public String getDisplayName() {
        return "Converge M3JDK21 Swing viewport ownership";
    }

    @Override public String getDescription() {
        return "Receives the existing viewport binding lifetime repair with unchanged "
                + "public interfaces and real Swing event qualification.";
    }
}
