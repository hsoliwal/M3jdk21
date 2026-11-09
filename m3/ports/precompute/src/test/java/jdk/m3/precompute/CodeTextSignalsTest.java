// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CodeTextSignalsTest {
    @Test
    void separatesNaturalCodeAndRegexText() {
        var natural = CodeTextSignals.compile("the quick brown fox");
        var code =
                CodeTextSignals.compile(
                        "while (value != 0) { value &= value - 1; count++; return count; }");
        var regex =
                CodeTextSignals.compile(
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

        List<CodeTextSignals.Snapshot> batch =
                CodeTextSignals.compileBatch(
                        values,
                        CodeTextSignalBatchJava.INSTANCE,
                        CodeTextSignalBatch.Limits.DEFAULT,
                        null);

        assertEquals(values.size(), batch.size());
        for (int index = 0; index < values.size(); index++) {
            CodeTextSignals.Snapshot scalar = CodeTextSignals.compile(values.get(index));
            assertEquals(scalar.lexicalPacked(), batch.get(index).lexicalPacked());
            assertEquals(scalar.codeScore(), batch.get(index).codeScore());
            assertEquals(scalar.regexScore(), batch.get(index).regexScore());
            assertEquals(scalar.text().contentHash64(), batch.get(index).text().contentHash64());
        }
    }

    @Test
    void regexShapeIsExposedAsCandidateEvidenceOnly() {
        assertEquals(
                RegexShape.Kind.STRICT_EXACT,
                CodeTextSignals.compile("\\Aabc\\z").regexShape());
        assertEquals(
                RegexShape.Kind.GENERAL,
                CodeTextSignals.compile("a+").regexShape());
    }
}
