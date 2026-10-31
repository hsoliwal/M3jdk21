// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Java 21 differential semantics for token-kind/atom/mask/UTF-16 tape matching. */
final class M3TokenRegexTest {
    private static M3TokenRegex.Tape code() {
        // x="x";x : the quoted x is ONE string-literal token, not an identifier.
        return new M3TokenRegex.Tape("x=\"x\";x", 7,
                new int[] {1, 2, 3, 4, 1},
                new int[] {5, 0, 5, 0, 5},
                new int[] {0, 0, 4, 0, 0},
                new int[] {0, 1, 2, 5, 6},
                new int[] {1, 2, 5, 6, 7});
    }

    @Test void tokenGrammarRespectsLiteralAndCommentKinds() {
        M3TokenRegex plan = M3TokenRegex.compile("[kind=1,atom=5]");
        M3TokenRegex.Tape tape = code();
        assertFalse(plan.matches(tape));
        assertEquals(new M3TokenRegex.Span(0, 1, 0, 1), plan.find(tape));
        assertEquals(new M3TokenRegex.Span(4, 5, 6, 7), plan.find(tape, 1));
        assertNull(plan.find(tape, 5));
        assertSame(tape.owner(), tape.owner());
        assertTrue(M3TokenRegex.compile("[kind=1][kind=2][kind=3]")
                .find(tape).equals(new M3TokenRegex.Span(0, 3, 0, 5)));
        assertEquals(new M3TokenRegex.Span(2, 3, 2, 5),
                M3TokenRegex.compile("[all=0x4,kind=3]").find(tape));
        assertNull(M3TokenRegex.compile("[kind=1,all=0x4]").find(tape));
        assertEquals(new M3TokenRegex.Span(0, 5, 0, 7),
                M3TokenRegex.compile("^ .* $").find(tape));
    }

    @Test void groupingAlternationAndGreedyLeftmostLongestAreTokenBased() {
        var tape = M3TokenRegex.Tape.ofKinds("abc", 3,
                new int[] {1, 1, 2}, new int[] {0, 1, 2}, new int[] {1, 2, 3});
        assertTrue(M3TokenRegex.compile("([kind=1]|[kind=2])+").matches(tape));
        assertEquals(new M3TokenRegex.Span(0, 2, 0, 2),
                M3TokenRegex.compile("[kind=1]+").find(tape));
        assertEquals(new M3TokenRegex.Span(0, 3, 0, 3),
                M3TokenRegex.compile("([kind=1]|[kind=1][kind=1])* [kind=2]").find(tape));
        assertNull(M3TokenRegex.compile("^[kind=2]").find(tape));
        assertTrue(M3TokenRegex.compile("([kind=1]+)?[kind=2]").find(tape) != null);
        assertTrue(M3TokenRegex.compile("([kind=1]|[kind=2])*").matches(tape));
        assertEquals(new M3TokenRegex.Span(3, 3, 3, 3),
                M3TokenRegex.compile("$").find(tape, 3));
        assertEquals(new M3TokenRegex.Span(0, 0, 0, 0),
                M3TokenRegex.compile("").find(tape));
    }

    @Test void maskHighBitAndSnapshotIsolation() {
        int[] kind = {1}, atom = {7}, mask = {0x80000000}, start = {2}, end = {5};
        Object owner = new Object();
        var tape = new M3TokenRegex.Tape(owner, 7, kind, atom, mask, start, end);
        Arrays.fill(kind, 9);
        Arrays.fill(atom, 9);
        Arrays.fill(mask, 0);
        Arrays.fill(start, 0);
        Arrays.fill(end, 0);
        assertSame(owner, tape.owner());
        assertEquals(1, tape.kind(0));
        assertEquals(7, tape.atom(0));
        assertEquals(0x80000000, tape.stateMask(0));
        assertEquals(new M3TokenRegex.Span(0, 1, 2, 5),
                M3TokenRegex.compile("[kind=1,atom=7,all=0x80000000,none=0x4]").find(tape));
        assertNull(M3TokenRegex.compile("[all=0x4]").find(tape));
    }

    @Test void strictInputAndPatternRefusal() {
        assertThrows(IllegalArgumentException.class,
                () -> new M3TokenRegex.Tape(new Object(), 1, new int[1], new int[0],
                        new int[1], new int[1], new int[1]));
        assertThrows(IllegalArgumentException.class,
                () -> new M3TokenRegex.Tape(new Object(), 1, new int[] {1}, new int[] {0},
                        new int[] {0}, new int[] {1}, new int[] {2}));
        for (String invalid : new String[] {"abc", "(", ")", "**", "[x=1]", "[kind=]",
                "[kind=-1]", "[all=0x4,none=0x4]", "[kind=1,kind=2]", "[all=zz]", "["}) {
            assertThrows(IllegalArgumentException.class, () -> M3TokenRegex.compile(invalid),
                    "accepting invalid syntax " + invalid);
        }
        assertThrows(IllegalArgumentException.class,
                () -> M3TokenRegex.compile(".".repeat(4097)));
        assertThrows(IllegalArgumentException.class,
                () -> M3TokenRegex.compile("(".repeat(65) + ")" .repeat(65)));
        var empty = M3TokenRegex.Tape.ofKinds(new Object(), 0, new int[0], new int[0], new int[0]);
        assertTrue(M3TokenRegex.compile("").matches(empty));
        assertFalse(M3TokenRegex.compile(".").matches(empty));
        assertNotNull(M3TokenRegex.compile("^$").find(empty));
        assertThrows(IndexOutOfBoundsException.class, () -> M3TokenRegex.compile("").find(empty, 1));
    }

    @Test @Timeout(12)
    void ambiguousRepetitionIsBoundedNfaRatherThanExponentialBacktracking() {
        int n = 60_000;
        int[] kind = new int[n + 1];
        int[] start = new int[n + 1];
        int[] end = new int[n + 1];
        Arrays.fill(kind, 1);
        kind[n] = 2;
        for (int i = 0; i < kind.length; i++) {
            start[i] = i;
            end[i] = i + 1;
        }
        var tape = M3TokenRegex.Tape.ofKinds(new Object(), kind.length, kind, start, end);
        var plan = M3TokenRegex.compile("^([kind=1]|[kind=1][kind=1])*[kind=2]$");
        assertTrue(plan.matches(tape));
        assertEquals(new M3TokenRegex.Span(0, n + 1, 0, n + 1), plan.find(tape));
    }

    @Test
    void boundedGrammarAgreesWithJavaRegexFullMatchOracleOnTokenAlphabet() {
        String[][] cases = {
                {"", ""},
                {"a", "[kind=1]"},
                {"b", "[kind=2]"},
                {"a*b", "[kind=1]*[kind=2]"},
                {"(a|b)+", "([kind=1]|[kind=2])+"},
                {"a?(ba)*", "[kind=1]?([kind=2][kind=1])*"},
                {"(aa|a)*b", "([kind=1][kind=1]|[kind=1])*[kind=2]"},
                {"^(a|b)*b$", "^([kind=1]|[kind=2])*[kind=2]$"},
                {".*a.", ".*[kind=1]."}
        };
        for (int count = 0; count <= 8; count++) {
            for (int value = 0; value < (1 << count); value++) {
                int[] kinds = new int[count], starts = new int[count], ends = new int[count];
                StringBuilder raw = new StringBuilder(count);
                for (int i = 0; i < count; i++) {
                    kinds[i] = (value & (1 << i)) == 0 ? 1 : 2;
                    raw.append(kinds[i] == 1 ? 'a' : 'b');
                    starts[i] = i;
                    ends[i] = i + 1;
                }
                var tape = M3TokenRegex.Tape.ofKinds(raw, count, kinds, starts, ends);
                for (String[] mapping : cases) {
                    assertEquals(java.util.regex.Pattern.matches(mapping[0], raw),
                            M3TokenRegex.compile(mapping[1]).matches(tape),
                            "regex " + mapping[0] + " input " + raw);
                }
            }
        }
    }
}
