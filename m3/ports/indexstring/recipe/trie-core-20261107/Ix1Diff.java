import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Supplier;

/**
 * Ix1Diff : INDEXSTRING_IX1_DIFFERENTIAL v1. The donor trie core (com.synexia.indexstring, compiled with stubs for
 * the donor-only monitor, native loader and MIndexString types) against the relocated M3JDK owners
 * (jdk.internal.m3.indexstring). Each round builds the same random literal set (UTF-16 units drawn from a small
 * alphabet plus isolated surrogates, empty literals, duplicate literals with equal and different payloads, a state
 * budget that sometimes trips) on both sides. It then compares refusals, tree shape counts, exact / findFirst /
 * visitMatches (full and stopped early), every navigation coordinate, steps, stateFor and longestPrefixPayload in
 * both directions, payload pages, bounded Levenshtein selections and precomputed rank sets, and an image round
 * trip. Each observation is folded into a per-side digest; any difference is a mismatch.
 */
public final class Ix1Diff {
    static final char[] UNITS = {'a', 'b', 'c', 'd', 'a', 'b', 'é', '\ud800', '\udc00'};
    static long dd = 17, rd = 17;
    static long checks, mismatches;

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
        for (int i = r.nextInt(max + 1); i > 0; i--) b.append(UNITS[r.nextInt(r.nextInt(4) == 0 ? UNITS.length : 4)]);
        return b.toString();
    }

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 20261107L;
        int rounds = args.length > 1 ? Integer.parseInt(args[1]) : 20_000;
        SplittableRandom r = new SplittableRandom(seed);
        for (int round = 0; round < rounds; round++) {
            int n = r.nextInt(25), maxStates = r.nextInt(10) == 0 ? 1 + r.nextInt(12) : 1000;
            var db = com.synexia.indexstring.MIndexLiteralSearchTree.builder(maxStates);
            var rb = jdk.internal.m3.indexstring.MIndexLiteralSearchTree.builder(maxStates);
            List<String> lits = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                String lit = r.nextInt(12) == 0 && !lits.isEmpty() ? lits.get(r.nextInt(lits.size())) : text(r, 6);
                int payload = r.nextInt(20) == 0 ? lits.size() - 1 : lits.size();
                lits.add(lit);
                eq("add", run(() -> { db.add(lit, payload); return "ok"; }), run(() -> { rb.add(lit, payload); return "ok"; }));
            }
            com.synexia.indexstring.MIndexLiteralSearchTree d;
            jdk.internal.m3.indexstring.MIndexLiteralSearchTree t;
            try { d = db.freeze(); } catch (RuntimeException e) { d = null; }
            try { t = rb.freeze(); } catch (RuntimeException e) { t = null; }
            eq("freeze", d == null, t == null);
            if (d == null || t == null) continue;
            final var D = d;
            final var T = t;
            eq("shape", List.of(D.stateCount(), D.edgeCount(), D.directAddressStateCount(), D.binaryStateCount(),
                    D.linearStateCount(), D.primitivePayloadBytes()),
                    List.of(T.stateCount(), T.edgeCount(), T.directAddressStateCount(), T.binaryStateCount(),
                    T.linearStateCount(), T.primitivePayloadBytes()));
            List<String> queries = new ArrayList<>();
            for (int q = 0; q < 8; q++) queries.add(r.nextInt(3) == 0 && !lits.isEmpty() ? lits.get(r.nextInt(lits.size())) + text(r, 3) : text(r, 12));
            for (String q : queries) {
                eq("exact", D.exact(q), T.exact(q));
                var fd = D.findFirst(q);
                var ft = T.findFirst(q);
                eq("findFirst", fd.payload() + ":" + fd.startUtf16() + ":" + fd.endUtf16(), ft.payload() + ":" + ft.startUtf16() + ":" + ft.endUtf16());
                int stop = r.nextInt(6);
                StringBuilder vd = new StringBuilder(), vt = new StringBuilder();
                long cd = D.visitMatches(q, (p, s, e) -> { vd.append(p).append(':').append(s).append(':').append(e).append(' '); return vd.length() < stop * 8 || stop == 0; });
                long ct = T.visitMatches(q, (p, s, e) -> { vt.append(p).append(':').append(s).append(':').append(e).append(' '); return vt.length() < stop * 8 || stop == 0; });
                eq("visitMatches", cd + " " + vd, ct + " " + vt);
            }
            var nd = D.navigation();
            var nt = T.navigation();
            eq("nav", List.of(nd.stateCount(), nd.terminalCount(), nd.primitivePayloadBytes(), D.primitivePayloadBytes()),
                    List.of(nt.stateCount(), nt.terminalCount(), nt.primitivePayloadBytes(), T.primitivePayloadBytes()));
            for (int s = 0; s < nd.stateCount(); s++) {
                eq("state", List.of(nd.parentState(s), nd.depth(s), nd.terminalPayload(s), nd.firstRank(s), nd.endRank(s),
                        nd.minimumLength(s), nd.maximumLength(s), nd.descendantCount(s)),
                        List.of(nt.parentState(s), nt.depth(s), nt.terminalPayload(s), nt.firstRank(s), nt.endRank(s),
                        nt.minimumLength(s), nt.maximumLength(s), nt.descendantCount(s)));
                char u = UNITS[r.nextInt(UNITS.length)];
                final int st = s;
                eq("step", run(() -> nd.step(st, u)), run(() -> nt.step(st, u)));
                int off = r.nextInt(3), len = r.nextInt(4);
                eq("payloads", run(() -> nd.payloads(st, off, len, null)), run(() -> nt.payloads(st, off, len, null)));
            }
            for (int k = 0; k < nd.terminalCount(); k++) eq("rank", nd.stateAtRank(k) + ":" + nd.payloadAtRank(k), nt.stateAtRank(k) + ":" + nt.payloadAtRank(k));
            for (String q : queries) {
                int b = r.nextInt(q.length() + 1), e = b + r.nextInt(q.length() - b + 1);
                eq("stateFor", nd.stateFor(q), nt.stateFor(q));
                for (int dir = 0; dir < 2; dir++) {
                    var ddir = com.synexia.indexstring.MIndexTrieNavigation.Direction.values()[dir];
                    var tdir = jdk.internal.m3.indexstring.MIndexTrieNavigation.Direction.values()[dir];
                    eq("stateForRange", run(() -> nd.stateFor(q, b, e, ddir, null)), run(() -> nt.stateFor(q, b, e, tdir, null)));
                    eq("longestPrefix", run(() -> nd.longestPrefixPayload(q, b, e, ddir, null)), run(() -> nt.longestPrefixPayload(q, b, e, tdir, null)));
                }
                int k = r.nextInt(3);
                String sd = run(() -> {
                    var sel = nd.select(com.synexia.indexstring.MIndexTrieNavigation.boundedLevenshtein(q, k));
                    var pm = nd.precompute(com.synexia.indexstring.MIndexTrieNavigation.boundedLevenshtein(q, k));
                    StringBuilder ranks = new StringBuilder();
                    for (int i = 0; i < pm.size(); i++) ranks.append(pm.rankAt(i)).append(',');
                    return Arrays.toString(sel.payloads()) + " " + sel.stats() + " " + ranks;
                });
                String stt = run(() -> {
                    var sel = nt.select(jdk.internal.m3.indexstring.MIndexTrieNavigation.boundedLevenshtein(q, k));
                    var pm = nt.precompute(jdk.internal.m3.indexstring.MIndexTrieNavigation.boundedLevenshtein(q, k));
                    StringBuilder ranks = new StringBuilder();
                    for (int i = 0; i < pm.size(); i++) ranks.append(pm.rankAt(i)).append(',');
                    return Arrays.toString(sel.payloads()) + " " + sel.stats() + " " + ranks;
                });
                eq("levenshtein", sd, stt);
            }
            var di = com.synexia.indexstring.MIndexLiteralSearchTree.fromImage(D.image());
            var ti = jdk.internal.m3.indexstring.MIndexLiteralSearchTree.fromImage(T.image());
            for (String q : queries) eq("image", di.exact(q) + ":" + di.findFirst(q).payload(), ti.exact(q) + ":" + ti.findFirst(q).payload());
        }
        System.out.println("INDEXSTRING_IX1_DIFFERENTIAL seed=" + seed + " rounds=" + rounds + " checks=" + checks
                + " mismatches=" + mismatches + " digest donor=" + Long.toHexString(dd) + " relocated=" + Long.toHexString(rd));
        if (mismatches != 0) System.exit(1);
    }
}
