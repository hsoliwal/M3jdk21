// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import java.util.LinkedHashMap;
import java.util.Map;

/** Exact source-image manifest for the A3 serial FILE work recipe. */
final class M3A3SerialFileWorkManifest {

    record Target(
            String before,
            String after,
            String beforeResource,
            String afterResource,
            boolean required) {}

    private M3A3SerialFileWorkManifest() {}

    static Map<String, Target> targets() {
        LinkedHashMap<String, Target> result = new LinkedHashMap<>();
        result.put(
                "src/main/java/com/m3/a3/A3.java",
                new Target(
                        "cc8473795b7023cce15a604b3c7164531f0e49fc",
                        "4fa702edb44c9a064c640c16610a3749f35f54ca",
                        "before/A3.java",
                        "after/A3.java",
                        true));
        result.put(
                "src/main/java/com/m3/a3/A3Inv.java",
                new Target(
                        "e76f590d4c8de784a3281fa37abec0c46a3ef020",
                        "d04ab86f0ada309c68f8ac7f0d14411932a3f2f9",
                        "before/A3Inv.java",
                        "after/A3Inv.java",
                        true));
        result.put(
                "src/main/java/com/m3/a3/A3Work.java",
                new Target(
                        "ABSENT",
                        "210b73b14fe3b3d06bd2cdcb6c950c79f23cf577",
                        null,
                        "after/A3Work.java",
                        false));
        result.put(
                "src/main/java/com/m3/a3/A3WorkTsv.java",
                new Target(
                        "ABSENT",
                        "7aea6d61a634ab6902de9aa394c103ef70a3f0b1",
                        null,
                        "after/A3WorkTsv.java",
                        false));
        result.put(
                "src/main/java/com/m3/a3/A3WorkValues.java",
                new Target(
                        "ABSENT",
                        "c64a61241d443d0822986118a297041fe3a532fe",
                        null,
                        "after/A3WorkValues.java",
                        false));
        result.put(
                "src/test/java/com/m3/a3/A3WorkTest.java",
                new Target(
                        "ABSENT",
                        "b2472eec623ec981570336564d98886f7b68bbcf",
                        null,
                        "after/A3WorkTest.java",
                        false));
        return Map.copyOf(result);
    }
}
