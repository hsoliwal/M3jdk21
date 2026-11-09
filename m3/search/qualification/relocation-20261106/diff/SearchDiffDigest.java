package jdk.m3.search;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;

/**
 * M3_SEARCH_DIFF v1: one seeded input stream through every relocated kernel; each result (or exception class) is
 * folded into a SHA-256 digest. The same source, with names mapped back, runs against the com.synexia originals;
 * equal digests = equal behaviour on the whole stream. Args: seeds cases-per-seed.
 */
public final class SearchDiffDigest {
    static MessageDigest md;
    static long rows;

    static void fold(Supplier<Object> f) {
        String out;
        try {
            Object v = f.get();
            out = v instanceof int[] ? Arrays.toString((int[]) v) : String.valueOf(v);
        } catch (RuntimeException e) {
            out = "E:" + e.getClass().getName();
        }
        md.update(out.getBytes(StandardCharsets.UTF_8));
        md.update((byte) '\n');
        rows++;
    }

    static String text(Random r, int max, int alphabet) {
        char[] c = new char[r.nextInt(max + 1)];
        for (int i = 0; i < c.length; i++) c[i] = (char) ('a' + r.nextInt(alphabet));
        return new String(c);
    }

    public static void main(String[] args) throws Exception {
        md = MessageDigest.getInstance("SHA-256");
        int seeds = Integer.parseInt(args[0]), cases = Integer.parseInt(args[1]);
        M3SearchEngine engine = new M3JavaAdaptiveSearchEngine();
        for (int seed = 1; seed <= seeds; seed++) {
            Random r = new Random(seed);
            for (int c = 0; c < cases; c++) {
                int alphabet = 1 + r.nextInt(4);
                byte[] hay = text(r, 300, alphabet).getBytes(StandardCharsets.ISO_8859_1);
                byte[] needle = text(r, 1 + r.nextInt(12), alphabet).getBytes(StandardCharsets.ISO_8859_1);
                if (needle.length == 0) needle = new byte[] {'a'};
                final byte[] h = hay, n = needle;
                M3CompiledSearch cs;
                try {
                    cs = engine.compile(n);
                } catch (RuntimeException e) {
                    fold(() -> "compile:" + e.getClass().getName());
                    continue;
                }
                for (int from : new int[] {-1, 0, 1, r.nextInt(h.length + 2), h.length, h.length + 1}) {
                    fold(() -> cs.indexOf(h, from));
                    fold(() -> cs.lastIndexOf(h, from));
                }
                cs.close();
                String src = text(r, 120, alphabet), req = text(r, 6, alphabet), seq = text(r, 5, alphabet);
                fold(() -> M3MinimumCoverWindow.javaSpan(src, req));
                fold(() -> M3MinimumSubsequenceWindow.find(src, seq));
                int wl = 1 + r.nextInt(3), wc = r.nextInt(4);
                String[] words = new String[wc];
                for (int i = 0; i < wc; i++) {
                    char[] w = new char[wl];
                    for (int j = 0; j < wl; j++) w[j] = (char) ('a' + r.nextInt(alphabet));
                    words[i] = new String(w);
                }
                fold(() -> M3FixedWordConcatenation.find(src, words));
                String[] pats = new String[r.nextInt(5)];
                for (int i = 0; i < pats.length; i++) pats[i] = text(r, 6, alphabet);
                fold(() -> M3BatchSubsequenceMatch.count(src, pats));
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : md.digest()) hex.append(String.format("%02x", b));
        System.out.println("M3_SEARCH_DIFF rows=" + rows + " sha256=" + hex);
    }
}
