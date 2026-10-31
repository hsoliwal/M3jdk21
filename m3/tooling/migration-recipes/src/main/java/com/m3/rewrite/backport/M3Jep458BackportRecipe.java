// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible recovery of JEP 458, Launch Multi-File Source-Code Programs.
 *
 * <p>The upstream donor is openjdk/jdk commit
 * {@code 517b1788198fc325961df61161f9b365c7b2524e}. The Java-21 adaptation
 * preserves the legacy {@code com.sun.tools.javac.launcher.Main}, substitutes JDK21
 * {@code MainMethodFinder} semantics, replaces post-21 {@code List.getFirst()} usage,
 * and imports only the minimal Preview access hooks required by the new memory launcher.
 *
 * <p>The packet is split into structured Java and exact UTF-8 text/native atoms. Neither
 * subrecipe is allowed to infer compatibility or delete JDK21 source.
 */
public final class M3Jep458BackportRecipe extends Recipe {
    public static final String UPSTREAM_COMMIT =
            "517b1788198fc325961df61161f9b365c7b2524e";

    @Override
    public String getDisplayName() {
        return "Backport JEP 458 multi-file source launcher";
    }

    @Override
    public String getDescription() {
        return "Replays the Java-21-compatible JEP 458 launcher graph from exact current-master "
                + "preimages while preserving the legacy Main launcher compatibility class.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-458",
                "source-launcher",
                "multi-file",
                "hash-pinned",
                "multi-module",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe("jdk22-jep458-multifile-java"),
                new M3Jdk21HashPinnedTextSnapshotRecipe("jdk22-jep458-multifile-text"));
    }
}
