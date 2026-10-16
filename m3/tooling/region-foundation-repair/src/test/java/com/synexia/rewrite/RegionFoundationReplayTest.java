// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class RegionFoundationReplayTest {
    private Path base() { return Path.of(System.getProperty("m3.base")); }

    @Test void exactJavaPairAndFixedPoint() throws Exception {
        var pair = RegionFoundationInputs.candidates(base()).getFirst();
        assertEquals(Map.of(pair.path(), pair.after()), RegionFoundationReplay.changes(
                RegionFoundationReplay.javaRecipe(pair), RegionFoundationReplay.parseJava(Map.of(pair.path(), pair.before()))));
        assertTrue(RegionFoundationReplay.changes(RegionFoundationReplay.javaRecipe(pair),
                RegionFoundationReplay.parseJava(Map.of(pair.path(), pair.after()))).isEmpty());
        assertEquals(pair.after(), RegionFoundationInputs.stringCandidate(pair.after()));
    }

    @Test void javaDriftRefusedWithoutOutput() throws Exception {
        var pair = RegionFoundationInputs.candidates(base()).getFirst();
        assertThrows(AssertionError.class, () -> RegionFoundationReplay.changes(RegionFoundationReplay.javaRecipe(pair),
                RegionFoundationReplay.parseJava(Map.of(pair.path(), pair.before() + "\n// drift\n"))));
    }

    @Test void textAndAbsentTestGenerationFixedPointAndInputOrder() throws Exception {
        var pairs = RegionFoundationInputs.candidates(base());
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put(pairs.get(2).path(), pairs.get(2).before());
        sources.put(pairs.get(1).path(), pairs.get(1).before());
        var result = RegionFoundationReplay.changes(RegionFoundationReplay.textRecipe(), RegionFoundationReplay.textSources(sources));
        assertEquals(3, result.size());
        assertEquals(pairs.get(1).after(), result.get(pairs.get(1).path()));
        assertEquals(pairs.get(2).after(), result.get(pairs.get(2).path()));
        assertEquals(Files.readString(Path.of("templates/M3RegionMatchesNonPositive.java")), result.get(RegionFoundationReplay.TEST));
        assertTrue(RegionFoundationReplay.changes(RegionFoundationReplay.textRecipe(), RegionFoundationReplay.textSources(result)).isEmpty());
        Map<String, String> reversed = new LinkedHashMap<>();
        sources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> reversed.put(e.getKey(), e.getValue()));
        assertEquals(result, RegionFoundationReplay.changes(RegionFoundationReplay.textRecipe(), RegionFoundationReplay.textSources(reversed)));
    }

    @Test void eachTextDriftMissingAndAddedTestConflictIsRefused() throws Exception {
        var pairs = RegionFoundationInputs.candidates(base());
        Map<String, String> sources = new LinkedHashMap<>();
        for (var p : pairs.subList(1, 3)) sources.put(p.path(), p.before());
        for (String path : sources.keySet()) {
            Map<String, String> drift = new LinkedHashMap<>(sources);
            drift.put(path, drift.get(path) + "\n");
            assertThrows(AssertionError.class, () -> RegionFoundationReplay.changes(RegionFoundationReplay.textRecipe(),
                    RegionFoundationReplay.textSources(drift)));
        }
        assertThrows(AssertionError.class, () -> RegionFoundationReplay.changes(RegionFoundationReplay.textRecipe(), java.util.List.of()));
        sources.put(RegionFoundationReplay.TEST, "class Wrong {}\n");
        assertThrows(AssertionError.class, () -> RegionFoundationReplay.changes(RegionFoundationReplay.textRecipe(),
                RegionFoundationReplay.textSources(sources)));
    }

    @Test void malformedSourceAndMissingOverloadsRefused() {
        assertThrows(IllegalArgumentException.class, () -> RegionFoundationInputs.stringCandidate("class String {"));
        assertThrows(IllegalArgumentException.class, () -> RegionFoundationInputs.stringCandidate("class String {}"));
    }

    @Test void upstreamAndExistingFoundationPayloadRemainsUnchanged() throws Exception {
        var pairs = RegionFoundationInputs.candidates(base());
        String before = pairs.get(1).before();
        String after = pairs.get(1).after();
        assertEquals(before.substring(before.indexOf("  foundation:\n")), after.substring(after.indexOf("  foundation:\n")));
        assertEquals(pairs.getFirst().before().substring(0, pairs.getFirst().before().indexOf("package java.lang;")),
                pairs.getFirst().after().substring(0, pairs.getFirst().after().indexOf("package java.lang;")));
    }
}
