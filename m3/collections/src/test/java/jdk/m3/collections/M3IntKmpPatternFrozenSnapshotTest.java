// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import jdk.m3.collections.text.IntKmpPattern;

/** Contract oracle for exactly the frozen KMP constructor/failure-table atom. */
final class M3IntKmpPatternFrozenSnapshotTest {
    private static final int FUZZ_CASES = 4_096;
    private static final int RACE_PATTERN_SIZE = 16_384;
    private static final int RACE_TRIALS = 96;

    @Test
    void callerMutationCannotAlterRetainedPatternOrFailure() {
        int[] source = {1, 2, 1, 2, 1, 3, 1, 2, 1};
        int[] frozen = source.clone();
        IntKmpPattern compiled = new IntKmpPattern(source);
        Arrays.fill(source, -1);
        assertArrayEquals(frozen, compiled.copyPattern());
        assertArrayEquals(referenceFailure(frozen), compiled.copyFailureFunction());
        assertEquals((long) frozen.length * Integer.BYTES * 2L, compiled.payloadBytes());
    }

    @Test
    void failureTableAndOverlappingMatchesAgreeWithIndependentOracle() {
        Random random = new Random(0x713c0b2L);
        for (int round = 0; round < FUZZ_CASES; round++) {
            int[] text = new int[random.nextInt(128)];
            int[] input = new int[random.nextInt(64)];
            for (int i = 0; i < text.length; i++) {
                text[i] = (round % 7 == 0) ? 0 : random.nextInt(11) - 5;
            }
            for (int i = 0; i < input.length; i++) {
                input[i] = (round % 13 == 0) ? 0 : random.nextInt(11) - 5;
            }
            int[] snapshot = input.clone();
            IntKmpPattern compiled = new IntKmpPattern(input);
            assertArrayEquals(referenceFailure(snapshot), compiled.copyFailureFunction());
            List<Integer> expected = referenceMatches(text, snapshot);
            assertEquals(expected.size(), compiled.count(text));
            assertEquals(expected.isEmpty() ? -1 : expected.getFirst().intValue(),
                    compiled.indexOf(text));
            List<Integer> actual = new ArrayList<>();
            compiled.scan(text, (from, to) -> {
                assertEquals(snapshot.length, to - from);
                actual.add(from);
            });
            assertEquals(expected, actual);
            Arrays.fill(input, Integer.MIN_VALUE);
            assertArrayEquals(snapshot, compiled.copyPattern());
            assertArrayEquals(referenceFailure(snapshot), compiled.copyFailureFunction());
        }
    }

    @Test
    void racingCallerCannotMakeFailureInconsistentWithRetainedSnapshot() throws Exception {
        int[] input = new int[RACE_PATTERN_SIZE];
        Arrays.fill(input, 1);
        AtomicBoolean running = new AtomicBoolean(true);
        Thread mutator = Thread.ofPlatform().start(() -> {
            while (running.get()) {
                for (int i = 0; i < input.length; i += 11) input[i] ^= 1;
            }
        });
        try {
            for (int trial = 0; trial < RACE_TRIALS; trial++) {
                IntKmpPattern compiled = new IntKmpPattern(input);
                assertArrayEquals(referenceFailure(compiled.copyPattern()),
                        compiled.copyFailureFunction(), "retained snapshot trial " + trial);
            }
        } finally {
            running.set(false);
            mutator.join();
        }
        assertTrue(!mutator.isAlive(), "mutation thread must terminate");
    }

    private static int[] referenceFailure(int[] input) {
        int[] result = new int[input.length];
        int matched = 0;
        for (int i = 1; i < input.length; i++) {
            while (matched > 0 && input[i] != input[matched]) {
                matched = result[matched - 1];
            }
            if (input[i] == input[matched]) matched++;
            result[i] = matched;
        }
        return result;
    }

    private static List<Integer> referenceMatches(int[] text, int[] pattern) {
        List<Integer> found = new ArrayList<>();
        if (pattern.length == 0) {
            for (int i = 0; i <= text.length; i++) found.add(i);
            return found;
        }
        for (int start = 0; start <= text.length - pattern.length; start++) {
            boolean matches = true;
            for (int i = 0; i < pattern.length; i++) {
                if (text[start + i] != pattern[i]) {
                    matches = false;
                    break;
                }
            }
            if (matches) found.add(start);
        }
        return found;
    }
}
