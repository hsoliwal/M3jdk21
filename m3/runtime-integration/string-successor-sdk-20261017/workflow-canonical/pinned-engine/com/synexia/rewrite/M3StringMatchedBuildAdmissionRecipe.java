/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Existing String-family workflow seals; no full-JDK output admission is granted. */
public final class M3StringMatchedBuildAdmissionRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-string-matched-build-admission-20261017/";
    }
    @Override public String getDisplayName() {
        return "M3 String bounded SDK and matched-build admission";
    }
    @Override public String getDescription() {
        return "Bind the existing bounded SDK harness and preserve both existing pipelines. "
                + "Keep matched complete-JDK execution explicitly held until measured output "
                + "authorization is recorded; do not grant runtime or phase promotion.";
    }
}
