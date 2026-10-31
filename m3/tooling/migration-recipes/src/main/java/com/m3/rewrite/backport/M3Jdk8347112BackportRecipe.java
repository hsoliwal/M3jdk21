// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible adaptation of upstream JDK-8347112.
 *
 * <p>The upstream donor is openjdk/jdk commit
 * {@code b221cb6ba138672802644f37eebf368521a0a6f4}. M3JDK21 deliberately splits the
 * compatible leaf: recursive {@code doc-files} copying becomes the default and
 * {@code -excludedocfilessubdir *} excludes all nested directories, while the Java 21
 * {@code -docfilessubdirs} option remains accepted with its existing processing contract.
 *
 * <p>The Java and text portions are separate hash-pinned atoms so structured Java remains an
 * OpenRewrite Java LST and documentation/resources remain exact UTF-8 text.
 */
public final class M3Jdk8347112BackportRecipe extends Recipe {
    public static final String UPSTREAM_COMMIT =
            "b221cb6ba138672802644f37eebf368521a0a6f4";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8347112 compatible javadoc doc-files behavior";
    }

    @Override
    public String getDescription() {
        return "Replays the Java-21-compatible JDK-8347112 split from exact JDK21 preimages: "
                + "recursive doc-files copying by default, wildcard exclusion, retained legacy option.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jdk-8347112",
                "javadoc",
                "tooling",
                "hash-pinned",
                "compatibility-split",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe("jdk27-javadoc-8347112-java"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk27-javadoc-8347112-text"));
    }
}
