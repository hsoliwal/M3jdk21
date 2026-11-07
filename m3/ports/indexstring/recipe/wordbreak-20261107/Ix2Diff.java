import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Supplier;

/**
 * Ix2Diff : INDEXSTRING_IX2_DIFFERENTIAL v1. The donor vocabulary tree and word-break plan (com.synexia.indexstring,
 * real donor MIndexVocabularySearchTree, MIndexWordBreakPlan, trie core and MIndexStringBitOps; the donor
 * precomputation and MIndexString are stubs that keep the donor's row rules and spelling equality) against the
 * relocated M3JDK owners (jdk.internal.m3.indexstring; vocabulary compiled from String spellings). Each round draws a
 * vocabulary (small alphabet, isolated surrogates, empty and duplicate words) and inputs built from vocabulary words
 * plus noise. It compares refusals, ranks, exact/prefix/suffix/longest lookups before and after trie preparation,
 * prefix/suffix ranges and affix rows, and every word-break answer: canSegment, the legacy first-predecessor
 * segmentation and the minimum segmentation (boundaries, rows, segment and vocabulary spellings), including the
 * refusals of tight per-query limits.
 */
public final class Ix2Diff {
    static final char[] UNITS = {'a', 'b', 'c', 'a', 'b', 'é', '\ud800', '\udc00'};
    static long dd = 19, rd = 19, checks, mismatches;

    static void eq(String what, Object d, Object r) {
        checks++;
        String a = String.valueOf(d), b = String.valueOf(r);
        dd = dd * 31 + a.hashCode();
        rd = rd * 31 + b.hashCode();
        if (!a.equals(b) && mismatches++ < 10) System.out.println("MISMATCH " + what + " donor=" + a + " relocated=" + b);
    }

    static String run(Supplier<Object> s) {
        try {
            Object v = s.get();
            return v instanceof int[] a ? Arrays.toString(a) : String.valueOf(v);
        } catch (RuntimeException e) {
            return "THROW " + e.getClass().getSimpleName();
        }
    }

    static String text(SplittableRandom r, int max) {
        StringBuilder b = new StringBuilder();
        for (int i = r.nextInt(max + 1); i > 0; i--) b.append(UNITS[r.nextInt(r.nextInt(5) == 0 ? UNITS.length : 3)]);
        return b.toString();
    }

    static String describe(com.synexia.indexstring.MIndexWordBreakPlan.Result x) {
        StringBuilder b = new StringBuilder(x.toString()).append(Arrays.toString(x.boundaries())).append(Arrays.toString(x.vocabularyRows()));
        for (int i = 0; i < x.segmentCount(); i++) b.append('|').append(x.segmentView(i)).append('=').append(x.vocabularyValue(i));
        return b.append(x.segmentViews().size()).toString().replace("MIndexWordBreakResult", "R");
    }

    static String describe(jdk.internal.m3.indexstring.MIndexWordBreakPlan.Result x) {
        StringBuilder b = new StringBuilder(x.toString()).append(Arrays.toString(x.boundaries())).append(Arrays.toString(x.vocabularyRows()));
        for (int i = 0; i < x.segmentCount(); i++) b.append('|').append(x.segmentView(i)).append('=').append(x.vocabularyValue(i));
        return b.append(x.segmentViews().size()).toString().replace("MIndexWordBreakResult", "R");
    }

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 20261107L;
        int rounds = args.length > 1 ? Integer.parseInt(args[1]) : 20_000;
        SplittableRandom r = new SplittableRandom(seed);
        long segmentable = 0;
        for (int round = 0; round < rounds; round++) {
            List<String> words = new ArrayList<>();
            for (int i = r.nextInt(14); i > 0; i--) words.add(r.nextInt(8) == 0 && !words.isEmpty() ? words.get(r.nextInt(words.size())) : text(r, 4));
            var dv = com.synexia.indexstring.MIndexStringPrecomputation.compile(words.stream().map(com.synexia.indexstring.MIndexString::new).toList());
            var dt = dv.vocabularyTree();
            var rt = jdk.internal.m3.indexstring.MIndexVocabularySearchTree.compile(words);
            eq("size", dt.size(), rt.size());
            for (int row = 0; row < dt.size(); row++) {
                eq("row", dv.valueAt(row) + ":" + dt.lexicalRankOf(row) + ":" + dt.reverseRankOf(row) + ":" + dt.rowAtLexicalRank(row) + ":" + dt.rowAtReverseRank(row),
                        rt.valueAt(row) + ":" + rt.lexicalRankOf(row) + ":" + rt.reverseRankOf(row) + ":" + rt.rowAtLexicalRank(row) + ":" + rt.rowAtReverseRank(row));
            }
            List<String> inputs = new ArrayList<>();
            for (int q = 0; q < 8; q++) {
                StringBuilder b = new StringBuilder();
                for (int k = r.nextInt(6); k > 0; k--) b.append(!words.isEmpty() && r.nextInt(4) != 0 ? words.get(r.nextInt(words.size())) : text(r, 2));
                inputs.add(b.toString());
            }
            for (int phase = 0; phase < 2; phase++) {
                for (String q : inputs) {
                    String p = q.substring(0, Math.min(q.length(), r.nextInt(3)));
                    String s = q.substring(Math.max(0, q.length() - r.nextInt(3)));
                    eq("exactRow" + phase, dt.exactRow(q), rt.exactRow(q));
                    eq("prefixRows" + phase, run(() -> dt.prefixRows(p)), run(() -> rt.prefixRows(p)));
                    eq("suffixRows" + phase, run(() -> dt.suffixRows(s)), run(() -> rt.suffixRows(s)));
                    eq("longestPrefixRow" + phase, dt.longestPrefixRow(q), rt.longestPrefixRow(q));
                    eq("longestSuffixRow" + phase, dt.longestSuffixRow(q), rt.longestSuffixRow(q));
                    if (phase == 1) {
                        int lo = r.nextInt(3), hi = lo + r.nextInt(4);
                        eq("prefixRange", run(() -> dt.prefixRange(p, null)), run(() -> rt.prefixRange(p, null)));
                        eq("suffixRange", run(() -> dt.suffixRange(s, null)), run(() -> rt.suffixRange(s, null)));
                        eq("affixRows", run(() -> dt.affixRows(p, s, lo, hi, null)), run(() -> rt.affixRows(p, s, lo, hi, null)));
                    }
                }
                if (phase == 0) {
                    eq("precomputeTries", run(() -> { dt.precomputeTries(); return dt.hasPrecomputedTries(); }),
                            run(() -> { rt.precomputeTries(); return rt.hasPrecomputedTries(); }));
                    if (!dt.hasPrecomputedTries() || !rt.hasPrecomputedTries()) break;
                }
            }
            String dc = run(() -> com.synexia.indexstring.MIndexWordBreakPlan.compile(dv).maximumWordLength());
            String rc = run(() -> jdk.internal.m3.indexstring.MIndexWordBreakPlan.compile(rt).maximumWordLength());
            eq("compile", dc, rc);
            if (dc.startsWith("THROW")) continue;
            var dp = com.synexia.indexstring.MIndexWordBreakPlan.compile(dv);
            var rp = jdk.internal.m3.indexstring.MIndexWordBreakPlan.compile(rt);
            for (String q : inputs) {
                var dq = new com.synexia.indexstring.MIndexString(q);
                eq("canSegment", dp.canSegment(dq), rp.canSegment(q));
                eq("segment", run(() -> describe(dp.segment(dq))), run(() -> describe(rp.segment(q))));
                eq("minimum", run(() -> describe(dp.minimumSegments(dq))), run(() -> describe(rp.minimumSegments(q))));
                if (dp.canSegment(dq)) segmentable++;
                long ws = r.nextInt(400), tr = r.nextInt(60);
                eq("limited", run(() -> describe(dp.minimumSegments(dq, new com.synexia.indexstring.MIndexWordBreakPlan.Limits(ws, tr), null))),
                        run(() -> describe(rp.minimumSegments(q, new jdk.internal.m3.indexstring.MIndexWordBreakPlan.Limits(ws, tr), null))));
                eq("limitedLegacy", run(() -> describe(dp.segment(dq, new com.synexia.indexstring.MIndexWordBreakPlan.Limits(ws, tr), null))),
                        run(() -> describe(rp.segment(q, new jdk.internal.m3.indexstring.MIndexWordBreakPlan.Limits(ws, tr), null))));
            }
        }
        System.out.println("INDEXSTRING_IX2_DIFFERENTIAL seed=" + seed + " rounds=" + rounds + " checks=" + checks
                + " segmentable_inputs=" + segmentable + " mismatches=" + mismatches
                + " digest donor=" + Long.toHexString(dd) + " relocated=" + Long.toHexString(rd));
        if (mismatches != 0) System.exit(1);
    }
}
