// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class M3CodeTextSignalsTest {
    @Test
    void separatesNaturalCodeAndRegexText() {
        var natural = M3CodeTextSignals.compile("the quick brown fox");
        var code =
                M3CodeTextSignals.compile(
                        "while (value != 0) { value &= value - 1; count++; return count; }");
        var regex =
                M3CodeTextSignals.compile(
                        "^while\\s*\\(value\\s*!=\\s*0\\).*[;{}]+$");

        assertFalse(natural.likelyCode());
        assertFalse(natural.likelyRegex());
        assertTrue(code.likelyCode());
        assertTrue(code.javaKeywordHits() >= 2);
        assertTrue(code.semicolonCount() >= 2);
        assertTrue(code.braceCount() >= 2);
        assertTrue(regex.likelyRegex());
        assertTrue(regex.backslashCount() >= 4);
        assertTrue(regex.regexMetaCount() >= 8);
        assertTrue(code.simHashDistance(regex) > 0);
        assertTrue(code.estimatedJaccard(regex) >= 0.0);
        assertTrue(code.estimatedJaccard(regex) <= 1.0);
    }

    @Test
    void batchCompilationMatchesScalarPacking() {
        List<String> values =
                List.of(
                        "",
                        "plain text",
                        "x -> x + 1",
                        "Type::method",
                        "if (x > 0) { return x; }",
                        "^[a-z]+\\d{2}$",
                        "unicode\u2003space");

        List<M3CodeTextSignals.Snapshot> batch =
                M3CodeTextSignals.compileBatch(
                        values,
                        M3CodeTextSignalBatchJava.INSTANCE,
                        M3CodeTextSignalBatch.Limits.DEFAULT,
                        null);

        assertEquals(values.size(), batch.size());
        for (int index = 0; index < values.size(); index++) {
            M3CodeTextSignals.Snapshot scalar = M3CodeTextSignals.compile(values.get(index));
            assertEquals(scalar.lexicalPacked(), batch.get(index).lexicalPacked());
            assertEquals(scalar.codeScore(), batch.get(index).codeScore());
            assertEquals(scalar.regexScore(), batch.get(index).regexScore());
            assertEquals(scalar.text().contentHash64(), batch.get(index).text().contentHash64());
        }
    }

    @Test
    void regexShapeIsExposedAsCandidateEvidenceOnly() {
        assertEquals(
                M3RegexShape.Kind.STRICT_EXACT,
                M3CodeTextSignals.compile("\\Aabc\\z").regexShape());
        assertEquals(
                M3RegexShape.Kind.GENERAL,
                M3CodeTextSignals.compile("a+").regexShape());
    }
}
