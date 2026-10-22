// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;
/** Source-sealed provider bridge; never mutates upstream donor collection public contracts. */
public final class M3GuavaStreamsRecipe extends Recipe {
    public static final String CRATE = "m3-guava-stream-bridge-20261018";
    @Override public String getDisplayName() { return "Install M3GuavaStreams M3JDK21 stream bridge"; }
    @Override public String getDescription() { return "Install typed M3 stream bridge and provider regression tests with SHA-256 admission."; }
    @Override public Set<String> getTags() { return Set.of("m3jdk21", "stream", "guava", "fork", "source-sealed"); }
    @Override public List<Recipe> getRecipeList() { return List.of(new M3HashPinnedJavaSnapshotRecipe(CRATE)); }
    @Override public int maxCycles() { return 1; }
}
