// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The frozen corpus reproduces the Synexia roots, the oracle is deterministic and tied to stock
 * {@link Pattern} by an independent sample, the literal-shape candidate is sound over all
 * 640,000 pairs, and an unsound candidate is reported with a bounded, ordered ledger.
 */
final class M3RegexOracleHarnessTest {
    /** Pinned from the first run on JDK 21.0.9; a different value means Pattern or signal drift. */
    private static final String EXPECTED_ORACLE_ROOT =
            "71d2f235a33ee3a91980d49ed06ad069f89728697e056318af0522ecc0820adf";
    private static final int SENTINEL_STRIDE = 67;

    private static M3RegexOracleHarness harness;

    private static M3RegexOracleHarness harness() {
        if (harness == null) {
            harness = M3RegexOracleHarness.compile(null);
        }
        return harness;
    }

    @Test
    void corpusReproducesTheSynexiaRoots() {
        List<String> expressions = M3RegexOracleCorpus.expressions();
        List<String> inputs = M3RegexOracleCorpus.strings();
        assertEquals(M3RegexOracleCorpus.REGEX_COUNT, expressions.size());
        assertEquals(M3RegexOracleCorpus.REGEX_COUNT, expressions.stream().distinct().count());
        assertEquals(M3RegexOracleCorpus.STRING_COUNT, inputs.size());
        assertEquals(M3RegexOracleCorpus.STRING_COUNT, inputs.stream().distinct().count());
        assertEquals(M3RegexOracleCorpus.REGEX_ROOT, M3RegexOracleCorpus.regexRoot(expressions));
        assertEquals(M3RegexOracleCorpus.STRING_ROOT, M3RegexOracleCorpus.stringRoot(inputs));
        assertEquals(M3RegexOracleCorpus.MATRIX_ROOT, M3RegexOracleCorpus.matrixRoot(expressions, inputs));
        assertEquals("a", expressions.get(0));
        assertEquals("", inputs.get(0));
        assertEquals(M3RegexOracleCorpus.REGEX_PER_SHARD, M3RegexOracleCorpus.shard(expressions, 9).size());
        assertThrows(IllegalArgumentException.class, () -> M3RegexOracleCorpus.shard(expressions, 10));
        assertThrows(IllegalArgumentException.class, () -> M3RegexOracleCorpus.shard(inputs, 0));
    }

    @Test
    void oracleIsDeterministicCompilesEveryRegexAndAgreesWithStockPattern() {
        M3RegexOracleHarness oracle = harness();
        assertEquals(M3RegexOracleCorpus.REGEX_COUNT, oracle.regexCount());
        assertEquals(M3RegexOracleCorpus.STRING_COUNT, oracle.stringCount());
        assertEquals(640_000L, oracle.pairCount());
        assertEquals(M3RegexOracleCorpus.SHARD_COUNT, oracle.shardCount());
        assertEquals(M3RegexOracleCorpus.MATRIX_ROOT, oracle.matrixRoot());
        assertTrue(oracle.oracleRoot().matches("[0-9a-f]{64}"));
        for (int regex = 0; regex < oracle.regexCount(); regex++) {
            assertTrue(oracle.regexValid(regex), oracle.expression(regex));
        }
        for (int regex = 0; regex < oracle.regexCount(); regex += SENTINEL_STRIDE) {
            Pattern pattern = Pattern.compile(oracle.expression(regex));
            for (int string = 0; string < oracle.stringCount(); string++) {
                String input = oracle.input(string);
                assertEquals(pattern.matcher(input).matches(), oracle.matches(regex, string));
                assertEquals(pattern.matcher(input).lookingAt(), oracle.lookingAt(regex, string));
                assertEquals(pattern.matcher(input).find(), oracle.find(regex, string));
            }
        }
        assertEquals(oracle.oracleRoot(), M3RegexOracleHarness.compile(null).oracleRoot());
        System.out.println("M3_REGEX_ORACLE_HARNESS matrixRoot=" + oracle.matrixRoot()
                + " oracleRoot=" + oracle.oracleRoot() + " pairs=" + oracle.pairCount());
        if (!EXPECTED_ORACLE_ROOT.isEmpty()) {
            assertEquals(EXPECTED_ORACLE_ROOT, oracle.oracleRoot());
        }
    }

    @Test
    void literalShapeCandidateIsSoundOverTheWholeMatrix() {
        M3RegexOracleHarness oracle = harness();
        var baseline = oracle.check("never", M3RegexOracleHarness.neverPrunes(), 8);
        assertTrue(baseline.sound());
        assertEquals(0L, baseline.pruned());
        assertTrue(baseline.findTrue() > 0L && baseline.findTrue() < baseline.pairs());
        var report = oracle.check("literal-shape", M3RegexOracleHarness.literalShape(), 8);
        assertTrue(report.sound(), () -> "unsound=" + report.unsound() + " " + report.retained());
        assertTrue(report.pruned() > 0L);
        assertEquals(baseline.findTrue(), report.findTrue());
        assertEquals(oracle.pairCount(), report.pairs());
        assertEquals(report.root(), oracle.check("literal-shape", M3RegexOracleHarness.literalShape(), 8).root());
        assertEquals(report, new M3RegexOracleHarness.Report(report.oracleRoot(), report.candidateName(),
                report.pairs(), report.findTrue(), report.pruned(), report.unsound(), report.retained(), ""));
        System.out.println("M3_REGEX_ORACLE_HARNESS literal-shape pruned=" + report.pruned()
                + " findTrue=" + report.findTrue() + " reportRoot=" + report.root());
    }

    @Test
    void unsoundCandidateIsReportedWithBoundedOrderedLedger() {
        M3RegexOracleHarness oracle = harness();
        var report = oracle.check("always", (regex, input) -> M3RegexOracleHarness.Verdict.CANNOT_MATCH, 5);
        assertFalse(report.sound());
        assertEquals(report.findTrue(), report.unsound());
        assertEquals(report.pairs() - report.findTrue(), report.pruned());
        assertEquals(5, report.retained().size());
        for (int i = 1; i < report.retained().size(); i++) {
            var previous = report.retained().get(i - 1);
            var current = report.retained().get(i);
            assertTrue(previous.regexOrdinal() < current.regexOrdinal()
                    || previous.regexOrdinal() == current.regexOrdinal()
                    && previous.stringOrdinal() < current.stringOrdinal());
            assertTrue(oracle.find(current.regexOrdinal(), current.stringOrdinal()));
        }
        assertEquals(report.root(), oracle.check("always",
                (regex, input) -> M3RegexOracleHarness.Verdict.CANNOT_MATCH, 5).root());
        assertThrows(IllegalArgumentException.class, () -> new M3RegexOracleHarness.Report(
                report.oracleRoot(), report.candidateName(), report.pairs(), report.findTrue(),
                report.pruned(), report.unsound(), report.retained(), "0".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> oracle.check("neg", M3RegexOracleHarness.neverPrunes(), -1));
    }
}
