// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.rewrite;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Canonical Synexia owner for the M3JDK21 A3 serial FILE-work source-sealed packet. */
public final class M3Jdk21A3SerialFileWorkRecipe extends M3Jdk21A3SourceSealedRecipe {
    @Override
    protected String resourceRoot() {
        return "/jdk/internal/m3/rewrite/m3jdk21-a3-serial-file-work/";
    }

    @Override
    protected Map<String, Target> targetManifest() {
        LinkedHashMap<String, Target> result = new LinkedHashMap<>();
        result.put(
                "src/main/java/jdk/m3/a3/A3.java",
                new Target(
                        "17d55680840d31d469859b96581aca641102f9a0",
                        "c3ba9cdbaff00ff9f7a9e84585c42cca48861a75",
                        "before/A3.java",
                        "after/A3.java",
                        true));
        result.put(
                "src/main/java/jdk/m3/a3/A3Inv.java",
                new Target(
                        "f1e883e5800182ff96e63241b9e89746a6fab193",
                        "480c66539e44e468962503f6baf986162649ed0b",
                        "before/A3Inv.java",
                        "after/A3Inv.java",
                        true));
        result.put(
                "src/main/java/jdk/m3/a3/A3Work.java",
                new Target(
                        "ABSENT",
                        "2d36b813fcb8a72ab956c66adb1b22e6a4ace31b",
                        null,
                        "after/A3Work.java",
                        false));
        result.put(
                "src/main/java/jdk/m3/a3/A3WorkTsv.java",
                new Target(
                        "ABSENT",
                        "5d1683303df4a414e8ab33717459d56ec443a50a",
                        null,
                        "after/A3WorkTsv.java",
                        false));
        result.put(
                "src/main/java/jdk/m3/a3/A3WorkValues.java",
                new Target(
                        "ABSENT",
                        "8304da2ea255999d2d804286363ece27ca3ab1bc",
                        null,
                        "after/A3WorkValues.java",
                        false));
        result.put(
                "src/test/java/jdk/m3/a3/A3WorkTest.java",
                new Target(
                        "ABSENT",
                        "7c5329614af93ea536a3efa77994ab4157c3b9ff",
                        null,
                        "after/A3WorkTest.java",
                        false));
        return Map.copyOf(result);
    }

    @Override
    public String getDisplayName() {
        return "Install M3JDK21 A3 serial FILE-work planner";
    }

    @Override
    public String getDescription() {
        return "Synexia-owned exact source-sealed A3 serial FILE-work packet consumed by "
                + "hsoliwal/M3jdk21.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia", "m3", "m3jdk21", "a3", "file-local", "serial",
                "recipe-first", "source-sealed", "candidate-only");
    }
}
