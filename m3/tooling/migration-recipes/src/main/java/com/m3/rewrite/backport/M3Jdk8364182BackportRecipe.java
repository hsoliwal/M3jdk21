// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible backport of OpenJDK JDK-8364182.
 *
 * <p>The upstream donor is {@code openjdk/jdk@f2f8828188f45d16344c82adfbf951f7409b8825}.
 * M3JDK21 keeps the upstream diagnostic behavior but retains Java 21's Security Manager-era JMX
 * permission model by requiring {@code java.security.SecurityPermission("getProperty.*")} for the
 * new command. Java and HotSpot/native text remain separate hash-pinned atoms.
 */
public final class M3Jdk8364182BackportRecipe extends Recipe {
    public static final String UPSTREAM_COMMIT =
            "f2f8828188f45d16344c82adfbf951f7409b8825";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8364182 VM.security_properties";
    }

    @Override
    public String getDescription() {
        return "Replays the Java-21-compatible VM.security_properties diagnostic command from "
                + "exact JDK21 preimages, including the retained Java21 security permission.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jdk-8364182",
                "jcmd",
                "serviceability",
                "hotspot",
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
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk27-security-properties-8364182-java"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk27-security-properties-8364182-native"));
    }
}
