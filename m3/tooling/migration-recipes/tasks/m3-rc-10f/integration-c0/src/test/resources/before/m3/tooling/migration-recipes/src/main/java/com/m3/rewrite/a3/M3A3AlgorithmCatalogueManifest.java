// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import java.util.LinkedHashMap;
import java.util.Map;

/** Exact source-image manifest for the A3 algorithm catalogue bridge. */
final class M3A3AlgorithmCatalogueManifest {

    record Target(
            String before,
            String after,
            String beforeResource,
            String afterResource,
            boolean required) {}

    private M3A3AlgorithmCatalogueManifest() {}

    static Map<String, Target> targets() {
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
}
