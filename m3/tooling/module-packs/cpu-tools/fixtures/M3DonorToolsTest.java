// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Independent contract witnesses for the 16 pinned CPU-tool receiving modules.
import com.m3.tools.codec.varint.VarInts;
import com.m3.tools.constraint.interval.Intervals;
import com.m3.tools.graph.paths.ShortestPaths;
import com.m3.tools.graph.topology.TopologicalOrder;
import com.m3.tools.plan.priority.StablePriorityQueue;
import com.m3.tools.stats.streaming.RunningStatistics;
import com.m3.tools.text.diff.TextDelta;
import com.m3.tools.text.frequency.TermFrequency;
import com.m3.tools.text.index.InvertedIndex;
import com.m3.tools.text.ngram.NGrams;
import com.m3.tools.text.normalize.UnicodeNormalizer;
import com.m3.tools.text.regex.RegexMatcher;
import com.m3.tools.text.search.ExactSearch;
import com.m3.tools.text.similarity.EditDistance;
import com.m3.tools.text.tokenize.WordTokenizer;
import com.m3.tools.verify.sha256.Sha256;
import java.math.BigInteger;
import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class M3DonorToolsTest {
    private static long assertions;
    private static final SplittableRandom RANDOM = new SplittableRandom(20261018);
    private M3DonorToolsTest() { }

    public static void main(String[] args) throws Exception {
        text(); graphs(); postings(); numeric(); priority(); refusals();
        System.out.println("M3_DONOR_TOOLS_PASS assertions=" + assertions
                + " tools=16 graph_exhaustive=531 graph_random=600 priority_operations=30000");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private static void rejects(Class<? extends Throwable> type, Checked action) throws Exception {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getName() + ", got " + failure);
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static String randomText(int maximum) {
        char[] alphabet = {'a', 'b', ' ', '\n', '\r', '\0', '\u00e9', '\u0301', '\ud83d', '\ude00'};
        char[] result = new char[RANDOM.nextInt(maximum + 1)];
        for (int i = 0; i < result.length; i++) result[i] = alphabet[RANDOM.nextInt(alphabet.length)];
        return new String(result);
    }

    private static void text() {
        String[] corpus = {"", "Hello, 42 world", "e\u0301 \uff21\u030a", "a\r\nb\n",
                "\ud83d\ude00a\ud83d", "\0ab ab", "alpha beta"};
        for (String value : corpus) {
            for (Normalizer.Form form : Normalizer.Form.values())
                check(UnicodeNormalizer.normalize(value, form).equals(Normalizer.normalize(value, form)), "normalization");
            check(UnicodeNormalizer.nfkc(value).equals(Normalizer.normalize(value, Normalizer.Form.NFKC)), "NFKC");
            BreakIterator boundaries = BreakIterator.getWordInstance(Locale.ROOT);
            boundaries.setText(value);
            List<String> expected = new ArrayList<>();
            int start = boundaries.first();
            for (int end = boundaries.next(); end != BreakIterator.DONE; start = end, end = boundaries.next()) {
                String token = value.substring(start, end);
                boolean word = false;
                for (int at = 0; at < token.length();) {
                    int point = token.codePointAt(at);
                    word |= Character.isLetterOrDigit(point); at += Character.charCount(point);
                }
                if (word) expected.add(token);
            }
            check(WordTokenizer.words(value).equals(expected), "word boundaries");
            for (String regex : new String[]{"", "a+", "(?=a)", "(ab)|b", "\\X", "[\\s\\S]"}) {
                for (int limit : new int[]{0, 1, 3, 100}) {
                    var matcher = Pattern.compile(regex).matcher(value);
                    var actual = RegexMatcher.find(regex, value, limit);
                    int count = 0;
                    while (count < limit && matcher.find()) {
                        var hit = actual.get(count++);
                        check(hit.start() == matcher.start() && hit.end() == matcher.end()
                                && hit.group().equals(matcher.group()), "regex offsets/groups");
                    }
                    check(actual.size() == count, "regex limit");
                }
            }
        }
        for (int trial = 0; trial < 500; trial++) {
            String left = randomText(24), right = randomText(12);
            int limit = RANDOM.nextInt(20);
            List<Integer> positions = new ArrayList<>();
            for (int at = 0; at <= left.length() && positions.size() < limit; at++)
                if (left.startsWith(right, at)) positions.add(at);
            check(Arrays.equals(ExactSearch.positions(left, right, limit),
                    positions.stream().mapToInt(Integer::intValue).toArray()), "literal positions");
            for (int bound = 0; bound <= 5; bound++)
                check(EditDistance.within(left, right, bound) == Math.min(distance(left, right), bound + 1), "Levenshtein");
            var delta = TextDelta.changedSpan(left, right);
            String rebuilt = left.substring(0, delta.fromStart()) + right.substring(delta.toStart(), delta.toEnd())
                    + left.substring(delta.fromEnd());
            check(rebuilt.equals(right), "delta reconstruction");
            check(delta.fromStart() == delta.toStart(), "delta prefix coordinates");
            if (delta.fromStart() < Math.min(left.length(), right.length()))
                check(left.charAt(delta.fromStart()) != right.charAt(delta.toStart()), "maximal common prefix");
            for (int n = 1; n <= 5; n++) {
                Map<String, Integer> expected = new TreeMap<>();
                for (int at = 0; at + n <= left.length(); at++)
                    expected.compute(left.substring(at, at + n), (key, count) -> count == null ? 1 : count + 1);
                check(NGrams.count(left, n).equals(expected), "n-gram counts");
            }
            List<String> tokens = Arrays.asList(left.split(" ", -1));
            Map<String, Long> counts = tokens.stream().collect(java.util.stream.Collectors.groupingBy(
                    java.util.function.Function.identity(), java.util.stream.Collectors.counting()));
            var actual = TermFrequency.count(tokens);
            check(actual.size() == counts.size(), "term cardinality");
            counts.forEach((term, count) -> check(actual.get(term).longValue() == count, "term count"));
        }
        check(Sha256.utf8("").equals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"), "empty SHA");
        check(Sha256.utf8("abc").equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"), "abc SHA");
        check(Sha256.utf8("\ud83d\ude00").equals(Sha256.hex(new byte[]{(byte) 0xf0, (byte) 0x9f,
                (byte) 0x98, (byte) 0x80})), "UTF8 byte identity");
    }

    private static int distance(String left, String right) {
        int[][] matrix = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) matrix[i][0] = i;
        for (int j = 0; j <= right.length(); j++) matrix[0][j] = j;
        for (int i = 1; i <= left.length(); i++) for (int j = 1; j <= right.length(); j++) {
            matrix[i][j] = Math.min(matrix[i - 1][j] + 1, Math.min(matrix[i][j - 1] + 1,
                    matrix[i - 1][j - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1)));
        }
        return matrix[left.length()][right.length()];
    }

    private static void graphs() {
        for (int vertices = 0; vertices <= 3; vertices++) {
            for (int mask = 0; mask < (1 << (vertices * vertices)); mask++) {
                List<TopologicalOrder.Arc> arcs = new ArrayList<>();
                for (int from = 0; from < vertices; from++) for (int to = 0; to < vertices; to++)
                    if ((mask & (1 << (from * vertices + to))) != 0) arcs.add(new TopologicalOrder.Arc(from, to));
                topology(vertices, arcs);
            }
        }
        for (int trial = 0; trial < 600; trial++) {
            int vertices = 1 + RANDOM.nextInt(10);
            List<TopologicalOrder.Arc> arcs = new ArrayList<>();
            List<ShortestPaths.Edge> edges = new ArrayList<>();
            for (int i = 0, count = RANDOM.nextInt(50); i < count; i++) {
                int from = RANDOM.nextInt(vertices), to = RANDOM.nextInt(vertices);
                arcs.add(new TopologicalOrder.Arc(from, to));
                long weight = switch (RANDOM.nextInt(12)) {
                    case 0 -> Long.MAX_VALUE;
                    case 1 -> Long.MAX_VALUE - 1;
                    default -> RANDOM.nextInt(20);
                };
                edges.add(new ShortestPaths.Edge(from, to, weight));
            }
            topology(vertices, arcs);
            int source = RANDOM.nextInt(vertices);
            check(Arrays.equals(ShortestPaths.distances(vertices, edges, source), bellman(vertices, edges, source)), "Dijkstra");
            Collections.reverse(arcs); topology(vertices, arcs);
        }
        check(Arrays.equals(ShortestPaths.distances(3, List.of(new ShortestPaths.Edge(0, 1, Long.MAX_VALUE - 1),
                new ShortestPaths.Edge(1, 2, 1)), 0), new long[]{0, Long.MAX_VALUE - 1, Long.MAX_VALUE}), "sentinel");
        List<TopologicalOrder.Arc> path = new ArrayList<>();
        for (int i = 0; i < 4095; i++) path.add(new TopologicalOrder.Arc(i, i + 1));
        int[] order = TopologicalOrder.sort(4096, path);
        for (int i = 0; i < order.length; i++) check(order[i] == i, "deep iterative topology");
    }

    private static void topology(int vertices, List<TopologicalOrder.Arc> arcs) {
        // Independent repeated full edge scans; no CSR, priority queue or shared heap code.
        boolean[] done = new boolean[vertices]; int[] expected = new int[vertices];
        int written = 0;
        while (written < vertices) {
            int selected = -1;
            for (int candidate = 0; candidate < vertices; candidate++) {
                if (done[candidate]) continue;
                boolean ready = true;
                for (var arc : arcs) if (arc.to() == candidate && !done[arc.from()]) { ready = false; break; }
                if (ready) { selected = candidate; break; }
            }
            if (selected < 0) break;
            expected[written++] = selected; done[selected] = true;
        }
        try {
            int[] actual = TopologicalOrder.sort(vertices, arcs);
            check(written == vertices, "cycle accepted");
            check(Arrays.equals(actual, expected), "lexicographic Kahn order");
        } catch (IllegalArgumentException cyclic) {
            check(written != vertices, "acyclic graph rejected");
        }
    }

    private static long[] bellman(int vertices, List<ShortestPaths.Edge> edges, int source) {
        BigInteger[] distance = new BigInteger[vertices]; distance[source] = BigInteger.ZERO;
        for (int pass = 0; pass < vertices; pass++) {
            boolean changed = false;
            for (var edge : edges) {
                if (distance[edge.from()] == null) continue;
                BigInteger candidate = distance[edge.from()].add(BigInteger.valueOf(edge.weight()));
                if (distance[edge.to()] == null || candidate.compareTo(distance[edge.to()]) < 0) {
                    distance[edge.to()] = candidate; changed = true;
                }
            }
            if (!changed) break;
        }
        long[] result = new long[vertices]; BigInteger maximum = BigInteger.valueOf(Long.MAX_VALUE);
        for (int i = 0; i < vertices; i++)
            result[i] = distance[i] == null || distance[i].compareTo(maximum) >= 0 ? Long.MAX_VALUE : distance[i].longValueExact();
        return result;
    }

    private static void postings() throws Exception {
        String[] vocabulary = {"", "a", "b", "Aa", "BB", "\u00e9", "\ud83d\ude00", "\ud83d"};
        for (int trial = 0; trial < 400; trial++) {
            List<List<String>> documents = new ArrayList<>();
            Map<String, TreeSet<Integer>> expected = new TreeMap<>();
            for (int id = 0, count = RANDOM.nextInt(20); id < count; id++) {
                List<String> document = new ArrayList<>();
                for (int j = 0, words = RANDOM.nextInt(30); j < words; j++) {
                    String term = vocabulary[RANDOM.nextInt(vocabulary.length)];
                    document.add(term); expected.computeIfAbsent(term, key -> new TreeSet<>()).add(id);
                }
                documents.add(document);
            }
            InvertedIndex index = InvertedIndex.build(documents);
            check(index.terms().equals(expected.keySet()), "term set");
            for (String term : vocabulary) {
                int[] wanted = expected.getOrDefault(term, new TreeSet<>()).stream().mapToInt(Integer::intValue).toArray();
                check(Arrays.equals(index.documents(term), wanted), "posting list");
                int[] returned = index.documents(term);
                if (returned.length > 0) returned[0] = -1;
                check(Arrays.equals(index.documents(term), wanted), "detached posting");
            }
            for (List<String> document : documents) document.clear();
            check(index.terms().equals(expected.keySet()), "input containers detached");
            rejects(UnsupportedOperationException.class, () -> index.terms().remove("missing"));
            rejects(NullPointerException.class, () -> index.terms().contains(null));
        }
        rejects(NullPointerException.class, () -> InvertedIndex.build(List.of(Arrays.asList("a", null))));
        rejects(UnsupportedOperationException.class, () -> NGrams.count("abc", 1).put("x", 1));
        rejects(UnsupportedOperationException.class, () -> TermFrequency.count(List.of("a")).clear());
        check(Arrays.stream(InvertedIndex.class.getDeclaredFields()).noneMatch(
                field -> Map.class.isAssignableFrom(field.getType())), "no retained map entry structure");
    }

    private static void numeric() throws Exception {
        for (long value : new long[]{0, 1, 127, 128, 16383, 16384, Integer.MAX_VALUE, Long.MAX_VALUE}) varint(value);
        for (int i = 0; i < 10000; i++) varint(RANDOM.nextLong() & Long.MAX_VALUE);
        for (byte[] bad : new byte[][]{{}, {(byte) 0x80}, {(byte) 0x80, 0}, {0, 1},
                {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                        (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80}})
            rejects(IllegalArgumentException.class, () -> VarInts.decode(bad));
        for (int trial = 0; trial < 1000; trial++) {
            long a = RANDOM.nextInt(-15, 16), b = a + RANDOM.nextInt(12);
            long c = RANDOM.nextInt(-15, 16), d = c + RANDOM.nextInt(12);
            var first = new Intervals.Span(a, b); var second = new Intervals.Span(c, d);
            boolean expected = false;
            for (long point = -15; point < 28; point++) if (a <= point && point < b && c <= point && point < d) expected = true;
            check(Intervals.intersects(first, second) == expected, "half-open overlap");
            check(Intervals.conflicting(List.of(first), second).equals(expected ? List.of(0) : List.of()), "conflicts");
        }
        check(!Intervals.intersects(new Intervals.Span(Long.MIN_VALUE, 0), new Intervals.Span(0, Long.MAX_VALUE)), "extreme interval");
        RunningStatistics empty = new RunningStatistics();
        check(empty.count() == 0 && Double.isNaN(empty.mean()) && Double.isNaN(empty.populationVariance()), "empty statistics");
        for (int trial = 0; trial < 200; trial++) {
            double[] samples = new double[1 + RANDOM.nextInt(100)];
            RunningStatistics statistics = new RunningStatistics();
            double sum = 0, min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < samples.length; i++) {
                double value = RANDOM.nextInt(-1000, 1001);
                samples[i] = value; statistics.accept(value); sum += value; min = Math.min(min, value); max = Math.max(max, value);
            }
            double mean = sum / samples.length, variance = 0;
            for (double value : samples) variance += (value - mean) * (value - mean);
            variance /= samples.length;
            check(statistics.count() == samples.length && Math.abs(statistics.mean() - mean) < 1e-9, "Welford mean");
            check(Math.abs(statistics.populationVariance() - variance) < 1e-7, "Welford variance");
            check(statistics.min() == min && statistics.max() == max, "extrema");
            long count = statistics.count();
            rejects(IllegalArgumentException.class, () -> statistics.accept(Double.NaN));
            check(statistics.count() == count, "rejected sample unchanged");
        }
    }

    private static void varint(long value) {
        byte[] encoded = VarInts.encode(value);
        check(VarInts.decode(encoded) == value, "varint roundtrip");
        BigInteger oracle = BigInteger.ZERO;
        for (int i = 0; i < encoded.length; i++)
            oracle = oracle.add(BigInteger.valueOf(encoded[i] & 127).shiftLeft(7 * i));
        check(oracle.equals(BigInteger.valueOf(value)), "LEB128 value");
        check(encoded.length == Math.max(1, (64 - Long.numberOfLeadingZeros(value) + 6) / 7), "minimal LEB128");
    }

    private record Offered(Object value, double score, long sequence) { }

    private static void priority() throws Exception {
        StablePriorityQueue<Object> actual = new StablePriorityQueue<>();
        PriorityQueue<Offered> oracle = new PriorityQueue<>(Comparator.comparingDouble(Offered::score).thenComparingLong(Offered::sequence));
        for (int i = 0; i < 30000; i++) {
            if (oracle.isEmpty() || RANDOM.nextInt(5) < 3) {
                Object value = new Object();
                double score = switch (RANDOM.nextInt(12)) {
                    case 0 -> -0.0;
                    case 1 -> 0.0;
                    case 2 -> Double.MAX_VALUE;
                    case 3 -> -Double.MAX_VALUE;
                    default -> RANDOM.nextInt(-20, 21);
                };
                actual.offer(value, score); oracle.add(new Offered(value, score, i));
            } else check(actual.poll() == oracle.remove().value(), "stable priority identity");
            check(actual.size() == oracle.size(), "queue size");
        }
        while (!oracle.isEmpty()) check(actual.poll() == oracle.remove().value(), "priority drain");
        check(actual.poll() == null && actual.size() == 0, "empty priority");
        rejects(NullPointerException.class, () -> actual.offer(null, 0));
        rejects(IllegalArgumentException.class, () -> actual.offer(new Object(), Double.NaN));
        rejects(IllegalArgumentException.class, () -> actual.offer(new Object(), Double.POSITIVE_INFINITY));
        check(actual.size() == 0, "refused offers atomic");
        check(Arrays.stream(StablePriorityQueue.class.getDeclaredFields()).noneMatch(
                field -> java.util.Collection.class.isAssignableFrom(field.getType())), "no retained Entry queue");
    }

    private static void refusals() throws Exception {
        rejects(NullPointerException.class, () -> UnicodeNormalizer.nfkc(null));
        rejects(NullPointerException.class, () -> WordTokenizer.words(null));
        rejects(PatternSyntaxException.class, () -> RegexMatcher.find("[", "", 0));
        rejects(IllegalArgumentException.class, () -> RegexMatcher.find("a", "a", -1));
        rejects(IllegalArgumentException.class, () -> ExactSearch.positions("", "", -1));
        rejects(IllegalArgumentException.class, () -> NGrams.count("x", 0));
        rejects(IllegalArgumentException.class, () -> EditDistance.within("", "", -1));
        rejects(IllegalArgumentException.class, () -> VarInts.encode(-1));
        rejects(IllegalArgumentException.class, () -> VarInts.decode(null));
        rejects(IllegalArgumentException.class, () -> TopologicalOrder.sort(-1, List.of()));
        rejects(IndexOutOfBoundsException.class, () -> TopologicalOrder.sort(1, List.of(new TopologicalOrder.Arc(0, 1))));
        rejects(IllegalArgumentException.class, () -> ShortestPaths.distances(2, List.of(new ShortestPaths.Edge(0, 1, -1)), 0));
        rejects(IllegalArgumentException.class, () -> new Intervals.Span(1, 0));
        rejects(NullPointerException.class, () -> Sha256.hex(null));
    }
}
