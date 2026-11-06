// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.rewrite;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Canonical Synexia owner for the M3JDK 21 A3 algorithm-catalogue source-sealed packet. */
public final class M3Jdk21A3AlgorithmCatalogueRecipe extends M3Jdk21A3SourceSealedRecipe {
    @Override
    protected String resourceRoot() {
        return "/jdk/internal/m3/rewrite/m3jdk21-a3-algorithm-catalogue/";
    }

    @Override
    protected Map<String, Target> targetManifest() {
        LinkedHashMap<String, Target> result = new LinkedHashMap<>();
        result.put(
                "src/main/java/jdk/m3/a3/A3.java",
                new Target(
                        "c3ba9cdbaff00ff9f7a9e84585c42cca48861a75",
                        "5a67b42d7a6e63de2f9d2a344eadaeaa60c8864f",
                        "before/A3.java",
                        "after/A3.java",
                        true));
        result.put(
                "src/main/java/jdk/m3/a3/A3Plan.java",
                new Target(
                        "77acb89b81a4d948371b3a443a285615047ad256",
                        "552a46632bd34e13d70f1936448de18c018071c4",
                        "before/A3Plan.java",
                        "after/A3Plan.java",
                        true));
        result.put(
                "src/main/java/jdk/m3/a3/A3Alg.java",
                new Target(
                        "ABSENT",
                        "906b6353f7ceba3d97122fb25a5d5793e0183b37",
                        null,
                        "after/A3Alg.java",
                        false));
        result.put(
                "src/test/java/jdk/m3/a3/A3PlanTest.java",
                new Target(
                        "4fe10060e31b23cd778f69c63033f27e7fc72d83",
                        "1c660d64d7a86eb44e2cc96b544a782c34e47bf8",
                        "before/A3PlanTest.java",
                        "after/A3PlanTest.java",
                        true));
        result.put(
                "src/test/java/jdk/m3/a3/A3AlgTest.java",
                new Target(
                        "ABSENT",
                        "3f15648966654e6d39f4e4faadd4b8fb6c1b4f35",
                        null,
                        "after/A3AlgTest.java",
                        false));
        return Map.copyOf(result);
    }

    @Override
    public String getDisplayName() {
        return "Install M3JDK21 A3 algorithm catalogue bridge";
    }

    @Override
    public String getDescription() {
        return "Synexia-owned exact source-sealed A3 algorithm-catalogue packet consumed by "
                + "hsoliwal/M3jdk21.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia", "m3", "m3jdk21", "a3", "algorithm", "catalogue",
                "recipe-first", "source-sealed", "candidate-only");
    }
}
