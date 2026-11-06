import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Leaf5Diff : FASTSEARCH_REDUCE_DIFFERENTIAL v1. Runs the same seeded cases through the donor
 * (com.synexia.fastsearch.knowledge.*) and the relocated (jdk.m3.search.M3*) kernels in one JVM and compares a
 * per-case digest: Utf8Fnv1a64 hash/update, SimHash64 of/hamming, FeatureHashVectorizer vectors (raw float
 * bits), LuhnSummarizer rank/summarize, DocumentDeduplicator decision sequences over near-duplicate streams, and
 * EvidenceSynthesizer summaries with citations. Text mixes ASCII words, Latin-1, CJK, supplementary code points,
 * unpaired surrogates and sentence punctuation. Prints one PASS line with the case count and a combined digest.
 */
public final class Leaf5Diff {
    private static final String[] WORDS = {"search", "index", "query", "token", "graph", "vector", "hash", "near",
            "duplicate", "summary", "evidence", "the", "a", "of", "and", "café", "naïve", "検索", "索引", "😀",
            "\uD800", "x", "M3", "jdk", "rope", "lane", "bitset", "kernel", "Luhn", "simhash"};

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 20261106L;
        int cases = args.length > 1 ? Integer.parseInt(args[1]) : 20_000;
        long all = 17L;
        for (int c = 0; c < cases; c++) {
            long d = donor(seed + c), m = relocated(seed + c);
            if (d != m) throw new AssertionError("mismatch case " + c + " seed " + (seed + c));
            all = all * 1_000_003L + d;
        }
        System.out.println("Leaf5Diff PASS cases=" + cases + " digest=" + Long.toHexString(all));
    }

    static String text(SplittableRandom r, int sentences) {
        StringBuilder b = new StringBuilder();
        for (int s = 0; s < sentences; s++) {
            int n = 1 + r.nextInt(14);
            for (int w = 0; w < n; w++) {
                if (w > 0) b.append(r.nextInt(9) == 0 ? ", " : " ");
                String word = WORDS[r.nextInt(WORDS.length)];
                b.append(r.nextInt(7) == 0 ? word.toUpperCase(java.util.Locale.ROOT) : word);
            }
            b.append(r.nextInt(5) == 0 ? "? " : r.nextInt(4) == 0 ? "! " : ". ");
        }
        return b.toString();
    }

    static String mutate(SplittableRandom r, String t) {
        String[] parts = t.split(" ", -1);
        for (int i = 0; i < 1 + r.nextInt(3); i++) parts[r.nextInt(parts.length)] = WORDS[r.nextInt(WORDS.length)];
        return String.join(" ", parts);
    }

    static long mix(long h, long v) { return (h ^ v) * 0x100000001b3L + 0x9e3779b97f4a7c15L; }

    static long str(long h, String s) {
        return mix(h, s == null ? -1L : java.util.Arrays.hashCode(s.getBytes(StandardCharsets.UTF_8)) * 31L + s.length());
    }

    static long donor(long seed) {
        SplittableRandom r = new SplittableRandom(seed);
        long h = 1L;
        String t = text(r, 1 + r.nextInt(8));
        int a = r.nextInt(t.length() + 1), b = a + r.nextInt(t.length() - a + 1);
        h = mix(h, com.synexia.fastsearch.knowledge.util.Utf8Fnv1a64.hash(t, a, b));
        h = mix(h, com.synexia.fastsearch.knowledge.util.Utf8Fnv1a64.update(r.nextLong(), t, a, b));
        h = mix(h, com.synexia.fastsearch.knowledge.util.Utf8Fnv1a64.updateByte(r.nextLong(), r.nextInt(256)));
        long s1 = com.synexia.fastsearch.knowledge.reduce.SimHash64.of(t), s2 = com.synexia.fastsearch.knowledge.reduce.SimHash64.of(mutate(r, t));
        h = mix(mix(mix(h, s1), s2), com.synexia.fastsearch.knowledge.reduce.SimHash64.hamming(s1, s2));
        for (float f : new com.synexia.fastsearch.knowledge.index.FeatureHashVectorizer(16 << r.nextInt(7)).vector(t)) h = mix(h, Float.floatToRawIntBits(f));
        var luhn = new com.synexia.fastsearch.knowledge.reduce.LuhnSummarizer(1 + r.nextInt(3), 1 + r.nextInt(5));
        for (var s : luhn.rank(t, 1 + r.nextInt(4))) {
            h = str(mix(mix(mix(mix(h, s.index()), Double.doubleToRawLongBits(s.score())), s.significantWords()), s.spanWords()), s.text());
        }
        h = str(h, luhn.summarize(t, 1 + r.nextInt(3)));
        var dedup = new com.synexia.fastsearch.knowledge.reduce.DocumentDeduplicator(r.nextInt(17));
        var hits = new ArrayList<com.synexia.fastsearch.knowledge.reduce.EvidenceSynthesizer.EvidenceDocument>();
        String prev = t;
        for (int i = 0; i < 1 + r.nextInt(6); i++) {
            String body = r.nextInt(3) == 0 ? prev : r.nextBoolean() ? mutate(r, prev) : text(r, 1 + r.nextInt(5));
            prev = body;
            var kind = com.synexia.fastsearch.knowledge.model.SourceKind.values()[r.nextInt(7)];
            URI uri = URI.create("https://ex.test/" + (seed & 0xff) + "/" + i);
            Instant at = Instant.ofEpochSecond(1_700_000_000L + i);
            var doc = com.synexia.fastsearch.knowledge.model.KnowledgeDocument.create(uri, "t" + i, null, kind, "p", at, null, body, Map.of("k", "v" + i));
            h = str(str(mix(h, dedup.classify(doc).ordinal()), doc.documentId()), doc.contentHash());
            hits.add(new com.synexia.fastsearch.knowledge.reduce.EvidenceSynthesizer.EvidenceDocument(
                    new com.synexia.fastsearch.knowledge.index.IndexedHit(doc.documentId(), doc.contentHash(), uri, doc.title(), kind, "p", at, null, r.nextFloat()), body));
        }
        var syn = new com.synexia.fastsearch.knowledge.reduce.EvidenceSynthesizer(luhn).synthesize(WORDS[r.nextInt(WORDS.length)] + " " + WORDS[r.nextInt(WORDS.length)], hits, 1 + r.nextInt(5));
        h = str(h, syn.summary());
        for (var e : syn.evidence()) h = str(str(str(mix(mix(h, e.citation()), Double.doubleToRawLongBits(e.score())), e.sourceUri()), e.title()), e.sentence());
        return h;
    }

    static long relocated(long seed) {
        SplittableRandom r = new SplittableRandom(seed);
        long h = 1L;
        String t = text(r, 1 + r.nextInt(8));
        int a = r.nextInt(t.length() + 1), b = a + r.nextInt(t.length() - a + 1);
        h = mix(h, jdk.m3.search.M3Utf8Fnv1a64.hash(t, a, b));
        h = mix(h, jdk.m3.search.M3Utf8Fnv1a64.update(r.nextLong(), t, a, b));
        h = mix(h, jdk.m3.search.M3Utf8Fnv1a64.updateByte(r.nextLong(), r.nextInt(256)));
        long s1 = jdk.m3.search.M3SimHash64.of(t), s2 = jdk.m3.search.M3SimHash64.of(mutate(r, t));
        h = mix(mix(mix(h, s1), s2), jdk.m3.search.M3SimHash64.hamming(s1, s2));
        for (float f : new jdk.m3.search.M3FeatureHashVectorizer(16 << r.nextInt(7)).vector(t)) h = mix(h, Float.floatToRawIntBits(f));
        var luhn = new jdk.m3.search.M3LuhnSummarizer(1 + r.nextInt(3), 1 + r.nextInt(5));
        for (var s : luhn.rank(t, 1 + r.nextInt(4))) {
            h = str(mix(mix(mix(mix(h, s.index()), Double.doubleToRawLongBits(s.score())), s.significantWords()), s.spanWords()), s.text());
        }
        h = str(h, luhn.summarize(t, 1 + r.nextInt(3)));
        var dedup = new jdk.m3.search.M3DocumentDeduplicator(r.nextInt(17));
        var hits = new ArrayList<jdk.m3.search.M3EvidenceSynthesizer.EvidenceDocument>();
        String prev = t;
        for (int i = 0; i < 1 + r.nextInt(6); i++) {
            String body = r.nextInt(3) == 0 ? prev : r.nextBoolean() ? mutate(r, prev) : text(r, 1 + r.nextInt(5));
            prev = body;
            var kind = jdk.m3.search.M3SourceKind.values()[r.nextInt(7)];
            URI uri = URI.create("https://ex.test/" + (seed & 0xff) + "/" + i);
            Instant at = Instant.ofEpochSecond(1_700_000_000L + i);
            var doc = jdk.m3.search.M3KnowledgeDocument.create(uri, "t" + i, null, kind, "p", at, null, body, Map.of("k", "v" + i));
            h = str(str(mix(h, dedup.classify(doc).ordinal()), doc.documentId()), doc.contentHash());
            hits.add(new jdk.m3.search.M3EvidenceSynthesizer.EvidenceDocument(
                    new jdk.m3.search.M3IndexedHit(doc.documentId(), doc.contentHash(), uri, doc.title(), kind, "p", at, null, r.nextFloat()), body));
        }
        var syn = new jdk.m3.search.M3EvidenceSynthesizer(luhn).synthesize(WORDS[r.nextInt(WORDS.length)] + " " + WORDS[r.nextInt(WORDS.length)], hits, 1 + r.nextInt(5));
        h = str(h, syn.summary());
        for (var e : syn.evidence()) h = str(str(str(mix(mix(h, e.citation()), Double.doubleToRawLongBits(e.score())), e.sourceUri()), e.title()), e.sentence());
        return h;
    }
}
