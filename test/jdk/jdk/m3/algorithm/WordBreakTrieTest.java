// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * wordBreak by reversed-trie walk against the previous substring lookup (kept below as
 * {@code reference}): the same result and the same monitor call sequence (beginTask total, every
 * worked tick, done), also when the monitor cancels after a random number of ticks.
 */
//FLLDCIM: LEETCODIFY — differential proof for wordBreak
final class WordBreakTrieTest {

    /** Records every call; cancels after {@code cancelAfter} ticks (never if negative). */
    static final class Recorder implements ProgressMonitor {
        final StringBuilder calls = new StringBuilder();
        final int cancelAfter;
        int ticks;

        Recorder(int cancelAfter) {
            this.cancelAfter = cancelAfter;
        }

        @Override public void beginTask(String name, int totalWork) {
            calls.append("begin ").append(name).append(' ').append(totalWork).append(';');
        }
        @Override public void worked(int work) {
            ticks += work;
        }
        @Override public boolean isCanceled() {
            return cancelAfter >= 0 && ticks >= cancelAfter;
        }
        @Override public void done() {
            calls.append("ticks ").append(ticks).append(";done;");
        }
    }

    /** The previous implementation, verbatim apart from its name. */
    static boolean reference(String value, List<String> dictionary, ProgressMonitor monitor) {
        Set<String> words = new HashSet<>(dictionary);
        int maxLength = words.stream().mapToInt(String::length).max().orElse(0);
        AlgorithmSupport.begin(monitor, "word-break",
                (long) value.length() * Math.max(1, maxLength));
        try {
            boolean[] reachable = new boolean[value.length() + 1];
            reachable[0] = true;
            for (int end = 1; end <= value.length(); end++) {
                int startMin = Math.max(0, end - maxLength);
                for (int start = end - 1; start >= startMin; start--) {
                    AlgorithmSupport.tick(monitor);
                    if (reachable[start] && words.contains(value.substring(start, end))) {
                        reachable[end] = true;
                        break;
                    }
                }
            }
            return reachable[value.length()];
        } finally {
            monitor.done();
        }
    }

    interface Call {
        boolean run(String value, List<String> dictionary, ProgressMonitor monitor);
    }

    static String outcome(Call call, String value, List<String> dictionary, int cancelAfter) {
        Recorder recorder = new Recorder(cancelAfter);
        String result;
        try {
            result = "value " + call.run(value, dictionary, recorder);
        } catch (RuntimeException e) {
            result = "threw " + e.getClass().getName() + ": " + e.getMessage();
        }
        return result + " | " + recorder.calls;
    }

    static String word(Random random, int alphabet, int maxLength, char base) {
        char[] chars = new char[random.nextInt(maxLength + 1)];
        for (int i = 0; i < chars.length; i++) chars[i] = (char) (base + random.nextInt(alphabet));
        return new String(chars);
    }

    @Test
    void sameResultsAndMonitorCalls() {
        char[] bases = {'a', '一', '￰'};
        for (int seed = 0; seed < 4_000; seed++) {
            Random random = new Random(seed);
            int alphabet = 1 + random.nextInt(seed % 3 == 0 ? 3 : 8);
            char base = bases[seed % bases.length];
            List<String> dictionary = new ArrayList<>();
            for (int i = random.nextInt(12); i > 0; i--) {
                dictionary.add(word(random, alphabet, 6, base));
            }
            String value = word(random, alphabet, seed % 40 == 0 ? 400 : 30, base);
            int cancelAfter = random.nextInt(4) == 0 ? random.nextInt(60) : -1;
            assertEquals(outcome(WordBreakTrieTest::reference, value, dictionary, cancelAfter),
                    outcome(DynamicProgrammingAlgorithms::wordBreak, value, dictionary,
                            cancelAfter),
                    "seed " + seed + " " + dictionary + " / " + value);
        }
    }

    @Test
    void nullsFailAsBefore() {
        List<String> withNull = new ArrayList<>(List.of("a"));
        withNull.add(null);
        String[][] cases = {{"ab", "list"}, {null, "list"}, {"ab", "null"}, {"ab", "withNull"}};
        for (String[] c : cases) {
            List<String> dictionary = switch (c[1]) {
                case "null" -> null;
                case "withNull" -> withNull;
                default -> List.of("a", "b");
            };
            assertEquals(outcome(WordBreakTrieTest::reference, c[0], dictionary, -1),
                    outcome(DynamicProgrammingAlgorithms::wordBreak, c[0], dictionary, -1),
                    c[0] + " / " + c[1]);
        }
    }
}
