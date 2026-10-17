/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

/** Adds finite hosted compiler qualification by reusing the existing bounded proof worker. */
public final class M3JavacRegexHostedReceivingWorkflowRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/m3-javac-regex-hosted-receiving-qualification-20261017/";
    }
    @Override public String getDisplayName() {
        return "Qualify donated compiler SDKs and JDK21 contracts in the existing receiving workflow";
    }
    @Override public String getDescription() {
        return "Adds one bounded independent compiler qualification job while preserving all existing " +
                "release jobs and strengthening the existing summary without changing product runtime.";
    }
}
