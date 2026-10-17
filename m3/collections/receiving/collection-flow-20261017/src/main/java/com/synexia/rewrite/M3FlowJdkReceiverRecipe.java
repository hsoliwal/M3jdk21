// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.synexia.rewrite;

/** Target-specific resource binding; the existing source-seal owner remains unchanged. */
public final class M3FlowJdkReceiverRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() { return "/com/synexia/rewrite/flow-jdk21/"; }
    @Override public String getDisplayName() { return "Receive M3 collection flow in Java 21"; }
    @Override public String getDescription() {
        return "Receives exact qualified flow and sorted-index atoms without replacing JDK contracts or existing wrapper owners.";
    }
}
