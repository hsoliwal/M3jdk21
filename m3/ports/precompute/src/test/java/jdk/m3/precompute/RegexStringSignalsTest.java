// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class RegexStringSignalsTest {
    @Test
    void plainLiteralFactsAreExactAndRemainCandidateOnly() {
        var fact = RegexStringSignals.analyze("needle", "xxneedlexx");
        assertTrue(fact.plainLiteral());
        assertEquals(2, fact.literalFindIndex());
        assertFalse(fact.literalFullMatch());
        assertTrue(fact.features().isEmpty());
        assertFalse(fact.semanticAuthority());
        assertFalse(fact.mutationAuthority());
        assertFalse(fact.promotionAuthority());
        assertTrue(fact.rootSha256().matches("[0-9a-f]{64}"));
        assertTrue(
                RegexStringSignals.verifyPlainLiteralAgainstJdk(
                                "needle", "xxneedlexx")
                        .passed());
    }

    @Test
    void generalRegexFeaturesNeverPretendToBeExecutableFacts() {
        var fact = RegexStringSignals.analyze(
                "(?i)(?=while)[a-z]+\\d{2}|(foo)\\1",
                "while42");
        assertFalse(fact.plainLiteral());
        assertEquals(Integer.MIN_VALUE, fact.literalFindIndex());
        assertTrue(fact.features().contains(RegexStringSignals.Feature.GROUP));
        assertTrue(fact.features().contains(RegexStringSignals.Feature.LOOKAROUND));
        assertTrue(fact.features().contains(RegexStringSignals.Feature.CHAR_CLASS));
        assertTrue(fact.features().contains(RegexStringSignals.Feature.QUANTIFIER));
        assertTrue(fact.features().contains(RegexStringSignals.Feature.ALTERNATION));
        assertTrue(fact.features().contains(RegexStringSignals.Feature.BACKREFERENCE));
        assertFalse(
                RegexStringSignals.verifyPlainLiteralAgainstJdk(
                                "(?=while)while", "while")
                        .applicable());
    }

    @Test
    void snapshotIsDeterministicAndProgressIsBounded() {
        List<RegexStringSignals.Pair> pairs =
                List.of(
                        new RegexStringSignals.Pair("a", "a"),
                        new RegexStringSignals.Pair("a", "ba"),
                        new RegexStringSignals.Pair("[a-z]+", "abc"),
                        new RegexStringSignals.Pair("\\Awhile\\z", "while"));
        CountingProgress progress = new CountingProgress();
        var first = RegexStringSignals.analyze(pairs, progress);
        var second = RegexStringSignals.analyze(pairs, Progress.none());

        assertEquals(first, second);
        assertEquals(4, first.facts().size());
        assertEquals(2, first.plainLiteralFacts());
        assertEquals(4, progress.work);
        assertEquals(1, progress.beginCalls);
        assertEquals(1, progress.doneCalls);
        assertTrue(first.rootSha256().matches("[0-9a-f]{64}"));
    }

    private static final class CountingProgress implements Progress {
        long work;
        int beginCalls;
        int doneCalls;

        @Override
        public void begin(String task, long totalWork) {
            beginCalls++;
            assertEquals(4, totalWork);
        }

        @Override
        public void worked(long delta) {
            work += delta;
        }

        @Override
        public void done() {
            doneCalls++;
        }
    }
}
