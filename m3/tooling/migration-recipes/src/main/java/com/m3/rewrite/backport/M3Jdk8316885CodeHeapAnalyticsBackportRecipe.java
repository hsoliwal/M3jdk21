// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible backport of OpenJDK JDK-8316885.
 *
 * <p>The donor change is a diagnostics-only HotSpot leaf: detailed
 * {@code Compiler.CodeHeap_Analytics} requests that lack a preceding aggregate step explain the
 * missing prerequisite instead of returning silently. No Java language, class-file, public Java
 * API, JNI/JVMTI, GC or JIT semantics are changed.</p>
 */
public final class M3Jdk8316885CodeHeapAnalyticsBackportRecipe extends Recipe {
    public static final String UPSTREAM_COMMIT =
            "1230aed61d286fe9c09f46e2bab626d0e8fe0273";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8316885 CodeHeap analytics missing-aggregate diagnostic";
    }

    @Override
    public String getDescription() {
        return "Composes two exact HotSpot FILE replay atoms plus one focused Java jtreg atom; "
                + "the MODULE join remains candidate-only until build/jtreg proof.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jdk-8316885",
                "jcmd",
                "hotspot",
                "serviceability",
                "diagnostics",
                "hash-pinned",
                "file-atoms",
                "dag-composable",
                "module-scope",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk22-codeheap-analytics-8316885-cpp"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk22-codeheap-analytics-8316885-hpp"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk22-codeheap-analytics-8316885-java"));
    }
}
