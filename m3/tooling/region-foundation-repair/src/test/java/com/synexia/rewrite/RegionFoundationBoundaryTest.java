// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;

final class RegionFoundationBoundaryTest {
    private Path base() { return Path.of(System.getProperty("m3.base")); }

    @Test void reverseJavaReceiptRestoresExactPreimage() throws Exception {
        var forward = RegionFoundationInputs.candidates(base()).getFirst();
        var reverse = new RegionFoundationInputs.Pair(forward.path(), forward.after(), forward.before());
        var result = RegionFoundationReplay.changes(RegionFoundationReplay.javaRecipe(reverse),
                RegionFoundationReplay.parseJava(Map.of(reverse.path(), reverse.before())));
        assertEquals(Map.of(forward.path(), forward.before()), result);
        assertTrue(RegionFoundationReplay.changes(RegionFoundationReplay.javaRecipe(reverse),
                RegionFoundationReplay.parseJava(result)).isEmpty());
    }

    @Test void syntaxSelectionDoesNotTransformOpaqueMethodText() throws Exception {
        var pair = RegionFoundationInputs.candidates(base()).getFirst();
        String opaque = "\n/* public boolean regionMatches(int toffset, String other, int ooffset, int len) {\n"
                + "        M3String leftM3 = m3();\n} */\n";
        assertEquals(pair.after() + opaque, RegionFoundationInputs.stringCandidate(pair.before() + opaque));
        assertEquals(pair.after() + opaque, RegionFoundationInputs.stringCandidate(pair.after() + opaque));
    }

    @Test void namedCompositeProducesTheSameFourFilesAndConverges() throws Exception {
        var pairs = RegionFoundationInputs.candidates(base());
        var recipe = new M3JdkRegionFoundationRepair(base().toString());
        var sources = new ArrayList<SourceFile>(RegionFoundationReplay.parseJava(
                Map.of(pairs.getFirst().path(), pairs.getFirst().before())));
        Map<String, String> text = new LinkedHashMap<>();
        for (var pair : pairs.subList(1, pairs.size())) text.put(pair.path(), pair.before());
        sources.addAll(RegionFoundationReplay.textSources(text));
        var result = RegionFoundationReplay.changes(recipe, sources);
        assertEquals(4, result.size());
        for (var pair : pairs) assertEquals(pair.after(), result.get(pair.path()));
        assertNotNull(result.get(RegionFoundationReplay.TEST));
        var next = new ArrayList<SourceFile>(RegionFoundationReplay.parseJava(
                Map.of(RegionFoundationInputs.STRING, result.get(RegionFoundationInputs.STRING))));
        text.clear();
        result.forEach((path, source) -> { if (!path.equals(RegionFoundationInputs.STRING)) text.put(path, source); });
        next.addAll(RegionFoundationReplay.textSources(text));
        assertTrue(RegionFoundationReplay.changes(recipe, next).isEmpty());
    }
}
