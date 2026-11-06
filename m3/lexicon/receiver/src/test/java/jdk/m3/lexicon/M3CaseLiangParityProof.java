// SPDX-License-Identifier: Apache-2.0
package jdk.m3.lexicon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Independent scalar case and naive weighted-pattern cut oracles. */
public final class M3CaseLiangParityProof {
    private M3CaseLiangParityProof() { }
    private static long checks;

    private static void require(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }

    private static void refuse(Runnable task) {
        try { task.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("invalid grammar admitted");
    }

    private static boolean scalar(M3SeparatorKind kind, int b, int a, int c) {
        if (kind == M3SeparatorKind.CAMEL) {
            return a == Character.UPPERCASE_LETTER
                    && (b == Character.LOWERCASE_LETTER || b == Character.DECIMAL_DIGIT_NUMBER);
        }
        return a != b && !(a == Character.LOWERCASE_LETTER && b == Character.UPPERCASE_LETTER)
                || c == Character.LOWERCASE_LETTER && a == Character.UPPERCASE_LETTER
                && b == Character.UPPERCASE_LETTER;
    }

    private static int[] independentLiang(String word, List<String> patterns, int left, int right) {
        String dotted = "." + word.toLowerCase(java.util.Locale.ROOT) + ".";
        int[] scores = new int[dotted.length() + 1];
        for (String raw : patterns) {
            StringBuilder glyphs = new StringBuilder();
            int[] weights = new int[raw.length() + 1];
            int gap = 0;
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (c >= '0' && c <= '9') weights[gap] = c - '0';
                else { glyphs.append(c); gap++; }
            }
            String fragment = glyphs.toString();
            for (int i = 0; i + fragment.length() <= dotted.length(); i++) {
                if (!dotted.startsWith(fragment, i)) continue;
                for (int j = 0; j <= gap; j++) {
                    scores[i + j] = Math.max(scores[i + j], weights[j]);
                }
            }
        }
        int[] cuts = new int[word.length() + 1];
        for (int j = left; j <= word.length() - right; j++) {
            cuts[j] = scores[j + 1] & 1;
        }
        return cuts;
    }

    public static void main(String[] args) {
        for (M3SeparatorKind kind : List.of(M3SeparatorKind.CAMEL, M3SeparatorKind.PASCAL)) {
            for (int b = -1; b <= 30; b++) for (int a = -1; a <= 30; a++) for (int c = -1; c <= 30; c++) {
                require(M3WindowBoundarySummary.boundary(kind, b, a, c) == scalar(kind, b, a, c),
                        "exact original character-type transition");
            }
            for (int invalid : new int[] {-42, -2, 31, 900}) {
                require(M3WindowBoundarySummary.boundary(kind, invalid, 1, 2)
                        == scalar(kind, invalid, 1, 2), "original invalid-type fallback");
            }
        }
        Random rand = new Random(0xCA5E2026L);
        char[] alphabet = {'a', 'B', 'z', 'P', '0', '9', '.', '_', '-', '\u00e9', '\ud83d', '\ude00'};
        for (int iteration = 0; iteration < 18_000; iteration++) {
            int n = rand.nextInt(60);
            StringBuilder b = new StringBuilder(n);
            for (int i = 0; i < n; i++) b.append(alphabet[rand.nextInt(alphabet.length)]);
            String text = b.toString();
            int p = rand.nextInt(n + 1), q = rand.nextInt(n + 1);
            int from = Math.min(p, q), to = Math.max(p, q);
            for (M3SeparatorKind kind : List.of(M3SeparatorKind.CAMEL, M3SeparatorKind.PASCAL)) {
                var summary = M3WindowBoundarySummary.of(kind, text.substring(0, from))
                        .combine(M3WindowBoundarySummary.of(kind, text.substring(from, to)))
                        .combine(M3WindowBoundarySummary.of(kind, text.substring(to)));
                require(summary.length() == n, "composed length");
                long[] bits = summary.finish();
                for (int j = 0; j < n; j++) {
                    int previous = j == 0 ? -1 : Character.getType(text.charAt(j - 1));
                    int at = Character.getType(text.charAt(j));
                    int following = j == n - 1 ? -1 : Character.getType(text.charAt(j + 1));
                    require(M3BitLanes.get(bits, j) == (j > 0 && scalar(kind, previous, at, following)),
                            "case seam parity");
                }
            }
        }
        List<String> seed = List.of("a1b", "a2b", ".a3b", "in3t", "o1r", "a1b.");
        var one = M3LiangPatternPrecompute.compile("en-us", seed, 1, 1);
        var reversed = new ArrayList<>(seed);
        Collections.reverse(reversed);
        require(one.sha256().equals(M3LiangPatternPrecompute.compile("EN-US", reversed, 1, 1).sha256()),
                "canonical language/grammar digest");
        for (String word : List.of("ab", "aba", "into", "orbit", "foobar", "")) {
            long[] bits = one.candidateBits(word);
            int[] expected = independentLiang(word, seed, 1, 1);
            for (int i = 0; i <= word.length(); i++) {
                require(M3BitLanes.get(bits, i) == (expected[i] == 1), "simple Liang overlap");
            }
        }
        refuse(() -> M3LiangPatternPrecompute.compile("en", List.of("a11b"), 1, 1));
        refuse(() -> M3LiangPatternPrecompute.compile("en", List.of("a1\u00e9"), 1, 1));
        refuse(() -> M3LiangPatternPrecompute.compile("en", List.of("a-b"), 1, 1));
        refuse(() -> M3LiangPatternPrecompute.compile("bad tag", seed, 1, 1));
        refuse(() -> one.candidateBits("ab-cd"));
        refuse(() -> one.candidateBits("\ud83d\ude00"));
        refuse(() -> one.candidateBits("z".repeat(4097)));
        for (int trial = 0; trial < 28_000; trial++) {
            int n = rand.nextInt(35);
            StringBuilder word = new StringBuilder();
            for (int i = 0; i < n; i++) word.append((char) ('a' + rand.nextInt(5)));
            List<String> patterns = new ArrayList<>();
            for (int i = 0, count = 1 + rand.nextInt(13); i < count; i++) {
                StringBuilder pattern = new StringBuilder();
                if (rand.nextInt(3) == 0) pattern.append('.');
                for (int j = 0, length = 1 + rand.nextInt(5); j < length; j++) {
                    if (rand.nextBoolean()) pattern.append((char) ('0' + rand.nextInt(10)));
                    pattern.append((char) ('a' + rand.nextInt(5)));
                }
                if (rand.nextBoolean()) pattern.append((char) ('0' + rand.nextInt(10)));
                if (rand.nextInt(3) == 0) pattern.append('.');
                patterns.add(pattern.toString());
            }
            int left = 1 + rand.nextInt(3), right = 1 + rand.nextInt(3);
            var image = M3LiangPatternPrecompute.compile("en", patterns, left, right);
            String str = word.toString();
            int[] reference = independentLiang(str, patterns, left, right);
            long[] result = image.candidateBits(str);
            for (int i = 0; i <= n; i++) require(M3BitLanes.get(result, i) == (reference[i] == 1),
                    "independent Liang weighted-max oracle");
            int first = rand.nextInt(n + 1), second = rand.nextInt(n + 1);
            int a = Math.min(first, second), z = Math.max(first, second);
            var cursor = new M3AtomCursor(List.of(str.substring(0, a), str.substring(a, z), str.substring(z)));
            require(Arrays.equals(result, image.candidateBits(cursor)), "atom cursor no-flatten parity");
        }
        System.out.println("M3_CASE_LIANG_PARITY_PASS checks=" + checks);
    }
}
