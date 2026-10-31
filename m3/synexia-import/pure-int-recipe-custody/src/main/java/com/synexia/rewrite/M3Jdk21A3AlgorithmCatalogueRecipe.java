// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Canonical Synexia owner for the M3JDK21 A3 algorithm-catalogue source-sealed packet. */
public final class M3Jdk21A3AlgorithmCatalogueRecipe extends M3Jdk21A3SourceSealedRecipe {
    @Override
    protected String resourceRoot() {
        return "/com/synexia/rewrite/m3jdk21-a3-algorithm-catalogue/";
    }

    @Override
    protected Map<String, Target> targetManifest() {
        LinkedHashMap<String, Target> result = new LinkedHashMap<>();
        result.put(
                "src/main/java/com/m3/a3/A3.java",
                new Target(
                        "4fa702edb44c9a064c640c16610a3749f35f54ca",
                        "39ea70adc237392244d02eb3b9d1bf05b368bcf0",
                        "before/A3.java",
                        "after/A3.java",
                        true));
        result.put(
                "src/main/java/com/m3/a3/A3Plan.java",
                new Target(
                        "19b90c3f9064d55d28a1d2dbb9395ac0aefac966",
                        "624bdceb4a3b1dc3e8aa32c43da42152b6f54042",
                        "before/A3Plan.java",
                        "after/A3Plan.java",
                        true));
        result.put(
                "src/main/java/com/m3/a3/A3Alg.java",
                new Target(
                        "ABSENT",
                        "bd0a88ba3c240510e57c51fb37c689205e02c9ec",
                        null,
                        "after/A3Alg.java",
                        false));
        result.put(
                "src/test/java/com/m3/a3/A3PlanTest.java",
                new Target(
                        "3e186021ae1b8997f5bb01ddb08f9b32bf5ae7a9",
                        "1570299bc0a09012b2752143d7d2861521801104",
                        "before/A3PlanTest.java",
                        "after/A3PlanTest.java",
                        true));
        result.put(
                "src/test/java/com/m3/a3/A3AlgTest.java",
                new Target(
                        "ABSENT",
                        "c59fda9b5d97c98c5682de6aeff50aa508182ef7",
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
