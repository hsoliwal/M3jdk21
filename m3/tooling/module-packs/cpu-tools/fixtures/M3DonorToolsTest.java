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
import com.m3.tools.text.index.Bm25Ranker;
import com.m3.tools.text.index.InvertedIndex;
import com.m3.tools.text.ngram.NGramIndex;
import com.m3.tools.text.ngram.NGrams;
import com.m3.tools.text.normalize.UnicodeNormalizer;
import com.m3.tools.text.regex.RegexMatcher;
import com.m3.tools.text.search.ExactSearch;
import com.m3.tools.text.search.MultiLiteralSearch;
import com.m3.tools.text.similarity.EditDistance;
import com.m3.tools.text.similarity.TextSignatures;
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
import java.util.LinkedHashMap;
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
        text(); ngramIndex(); multiSearchAndSignatures(); graphs(); postings(); ranking(); numeric(); priority(); refusals();
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

    /** Independent substring enumeration proves rolling-hash candidates never decide equality. */
    private static void ngramIndex() throws Exception {
        rejects(NullPointerException.class, () -> NGrams.prepare(null, 2));
        rejects(NullPointerException.class, () -> NGrams.prepare("ab", 2, null));
        rejects(IllegalArgumentException.class, () -> NGrams.prepare("ab", 0));
        rejects(IllegalArgumentException.class, () -> NGrams.prepare("ab", 65));
        rejects(IllegalArgumentException.class, () -> NGrams.prepare(
                "a".repeat(NGramIndex.MAX_SOURCE_UTF16 + 1), 2));
        var empty = NGrams.prepare("", 3);
        check(empty.windowCount() == 0 && empty.distinctCount() == 0 && empty.counts().isEmpty(),
                "empty positional index");
        check(NGrams.overlap(empty, empty).coefficient() == 1.0d, "empty gram multiset");
        check(NGrams.overlap(NGrams.prepare("abc", 4), NGrams.prepare("", 4)).union() == 0,
                "short string overlap");
        rejects(IllegalArgumentException.class,
                () -> NGrams.overlap(NGrams.prepare("a", 1), NGrams.prepare("a", 2)));
        rejects(NullPointerException.class, () -> empty.frequency(null));
        rejects(NullPointerException.class, () -> empty.positions(null));
        rejects(NullPointerException.class, () -> empty.forEachPosition(0, null));
        rejects(IndexOutOfBoundsException.class, () -> empty.gramAt(0));
        rejects(IllegalArgumentException.class,
                () -> new NGramIndex.Overlap(2, 1));
        java.util.concurrent.atomic.AtomicInteger checkpoints = new java.util.concurrent.atomic.AtomicInteger();
        rejects(IllegalStateException.class, () -> NGrams.prepare("x".repeat(30_000), 1, () -> {
            if (checkpoints.incrementAndGet() == 2) throw new IllegalStateException("cancel");
        }));
        check(checkpoints.get() == 2, "indexed cancellation inside scan");
        var dense = NGrams.prepare("a".repeat(70_000), 64);
        check(dense.distinctCount() == 1 && dense.frequency("a".repeat(64)) == 69_937,
                "single parent-owned dense posting lane");
        check(dense.positions("a".repeat(64)).length == 69_937, "dense positional visit");

        for (int trial = 0; trial < 1_000; trial++) {
            String left = randomText(76), right = randomText(76);
            int n = 1 + RANDOM.nextInt(12);
            NGramIndex indexed = NGrams.prepare(left, n);
            NGramIndex other = NGrams.prepare(right, n);
            Map<String, List<Integer>> positions = new LinkedHashMap<>();
            for (int i = 0; i + n <= left.length(); i++) {
                positions.computeIfAbsent(left.substring(i, i + n), key -> new ArrayList<>()).add(i);
            }
            Map<String, Integer> expected = new TreeMap<>();
            positions.forEach((gram, offsets) -> expected.put(gram, offsets.size()));
            check(indexed.windowCount() == Math.max(0, left.length() - n + 1), "gram window count");
            check(indexed.distinctCount() == expected.size(), "gram primitive distinct count");
            check(indexed.counts().equals(expected) && indexed.counts().equals(NGrams.count(left, n)),
                    "gram legacy-map parity including unpaired surrogates");
            check(indexed.gramSize() == n && indexed.frequency("too long to be this gram") == 0,
                    "gram query length contract");
            for (int id = 0; id < indexed.distinctCount(); id++) {
                String gram = indexed.gramAt(id);
                List<Integer> expectedOffsets = positions.get(gram);
                int[] expectedArray = expectedOffsets.stream().mapToInt(Integer::intValue).toArray();
                check(indexed.firstOffset(id) == expectedArray[0] && indexed.frequencyAt(id) == expectedArray.length,
                        "first source offset and physical frequency");
                check(Arrays.equals(indexed.positions(gram), expectedArray), "collision-checked gram offsets");
                List<Integer> visited = new ArrayList<>();
                indexed.forEachPosition(id, visited::add);
                check(visited.equals(expectedOffsets), "allocation-free visitor order");
                int[] detached = indexed.positions(gram);
                detached[0] = Integer.MIN_VALUE;
                check(indexed.positions(gram)[0] == expectedArray[0], "gram offsets are detached");
            }
            rejects(UnsupportedOperationException.class, () -> indexed.counts().put("not-in-index", 42));
            Map<String, Integer> rightCounts = new TreeMap<>();
            for (int at = 0; at + n <= right.length(); at++) {
                rightCounts.merge(right.substring(at, at + n), 1, Integer::sum);
            }
            long intersection = 0;
            for (var entry : expected.entrySet()) {
                intersection += Math.min(entry.getValue(), rightCounts.getOrDefault(entry.getKey(), 0));
            }
            long union = (long) indexed.windowCount() + other.windowCount() - intersection;
            NGramIndex.Overlap overlap = NGrams.overlap(indexed, other);
            check(overlap.intersection() == intersection && overlap.union() == union,
                    "exact multiset overlap oracle");
            check(Double.compare(overlap.coefficient(), union == 0 ? 1.0d : intersection / (double) union) == 0,
                    "overlap coefficient contract");
            check(NGrams.overlap(other, indexed).equals(overlap), "overlap symmetry");
        }
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

    /** Independent nested-loop matching oracle; never calls the automaton in the witness. */
    private static List<MultiLiteralSearch.Hit> bruteMulti(String text, List<String> patterns) {
        List<MultiLiteralSearch.Hit> matches = new ArrayList<>();
        for (int end = 1; end <= text.length(); end++) {
            for (int id = 0; id < patterns.size(); id++) {
                String pattern = patterns.get(id);
                int start = end - pattern.length();
                if (start >= 0 && text.startsWith(pattern, start)) {
                    matches.add(new MultiLiteralSearch.Hit(id, start, end));
                }
            }
        }
        matches.sort(Comparator.comparingInt(MultiLiteralSearch.Hit::end)
                .thenComparing(Comparator.comparingInt(
                        (MultiLiteralSearch.Hit hit) -> patterns.get(hit.patternId()).length()).reversed())
                .thenComparingInt(MultiLiteralSearch.Hit::patternId));
        return matches;
    }

    private static void multiSearchAndSignatures() throws Exception {
        var empty = MultiLiteralSearch.prepare(List.of());
        check(empty.stateCount() == 1 && empty.find("ab", 0).complete(), "empty pattern set");
        var duplicates = MultiLiteralSearch.prepare(List.of("aba", "ba", "aba", "a", "\ud83d", "\ude00"));
        var expected = bruteMulti("aba\ud83d\ude00aba", List.of("aba", "ba", "aba", "a", "\ud83d", "\ude00"));
        check(duplicates.find("aba\ud83d\ude00aba", 100).hits().equals(expected), "duplicate suffix and split surrogate");
        check(duplicates.sha256().equals(MultiLiteralSearch.prepare(
                List.of("aba", "ba", "aba", "a", "\ud83d", "\ude00")).sha256()), "plan SHA replay");
        check(duplicates.sha256().equals(MultiLiteralSearch.prepare(
                List.of("aba", "ba", "aba", "a", "\ud83d", "\ude00"), () -> { }).sha256()),
                "checkpointed plan preserves exact automaton fingerprint");
        int[] preparationChecks = {0};
        rejects(IllegalStateException.class, () -> MultiLiteralSearch.prepare(
                List.of("x".repeat(12_000)), () -> {
                    if (++preparationChecks[0] == 5) throw new IllegalStateException("cancel during trie construction");
                }));
        check(preparationChecks[0] == 5, "mid-construction checkpoint executes");
        rejects(NullPointerException.class, () -> MultiLiteralSearch.prepare(List.of("a"), null));
        rejects(IllegalArgumentException.class, () -> MultiLiteralSearch.prepare(List.of("")));
        rejects(NullPointerException.class, () -> MultiLiteralSearch.prepare(Arrays.asList("a", null)));
        rejects(IllegalArgumentException.class, () -> duplicates.find("a", -1));
        rejects(IllegalArgumentException.class, () -> duplicates.find("a", MultiLiteralSearch.MAX_HITS + 1));
        rejects(IllegalArgumentException.class, () -> MultiLiteralSearch.prepare(
                java.util.Collections.nCopies(MultiLiteralSearch.MAX_PATTERNS + 1, "a")));
        rejects(IllegalArgumentException.class, () -> MultiLiteralSearch.prepare(
                List.of("a".repeat(MultiLiteralSearch.MAX_PATTERN_UNITS + 1))));
        rejects(IllegalArgumentException.class, () -> empty.find(
                "a".repeat(MultiLiteralSearch.MAX_TEXT_UNITS + 1), 0));
        rejects(IllegalStateException.class, () -> duplicates.find("abc", 1, () -> {
            throw new IllegalStateException("cancelled");
        }));

        String[] tokens = {"", "a", "b", "Aa", "BB", "\ud83d", "\ude00", "\u00e9", "\u0301"};
        for (int trial = 0; trial < 450; trial++) {
            String text = randomText(65);
            int patternCount = RANDOM.nextInt(1, 12);
            List<String> patterns = new ArrayList<>();
            for (int i = 0; i < patternCount; i++) {
                String pattern;
                if (i > 0 && RANDOM.nextInt(4) == 0) pattern = patterns.get(RANDOM.nextInt(i));
                else if (!text.isEmpty() && RANDOM.nextBoolean()) {
                    int from = RANDOM.nextInt(text.length());
                    int to = from + 1 + RANDOM.nextInt(Math.min(5, text.length() - from));
                    pattern = text.substring(from, to);
                } else pattern = randomText(5) + "a";
                patterns.add(pattern);
            }
            var prepared = MultiLiteralSearch.prepare(patterns);
            List<MultiLiteralSearch.Hit> oracle = bruteMulti(text, patterns);
            for (int cap : new int[]{0, 1, 2, 7, 250}) {
                var result = prepared.find(text, cap);
                check(result.size() == Math.min(cap, oracle.size()), "multi-literal hit count");
                check(result.complete() == (oracle.size() <= cap), "multi-literal completeness");
                check(result.hits().equals(oracle.subList(0, Math.min(cap, oracle.size()))),
                        "multi-literal independent UTF-16 oracle");
                for (int i = 0; i < result.size(); i++) check(result.hit(i).equals(oracle.get(i)),
                        "detached ordinal projection");
            }
        }

        rejects(IllegalArgumentException.class, () -> TextSignatures.compute(List.of("a"), 0));
        rejects(NullPointerException.class, () -> TextSignatures.compute(List.of("a"), 4, null));
        int[] hashingChecks = {0};
        rejects(IllegalStateException.class, () -> TextSignatures.compute(
                List.of("x".repeat(12_000)), 4, () -> {
                    if (++hashingChecks[0] == 5) throw new IllegalStateException("cancel during SHA-256");
                }));
        check(hashingChecks[0] == 5, "mid-token UTF-16 hashing checkpoint executes");
        int[] tokenizerChecks = {0};
        check(WordTokenizer.words("Hello, 42 world", () -> tokenizerChecks[0]++)
                .equals(WordTokenizer.words("Hello, 42 world")) && tokenizerChecks[0] >= 3,
                "tokenizer checkpoint preserves BreakIterator semantics");
        rejects(NullPointerException.class, () -> WordTokenizer.words("ok", null));
        rejects(IllegalArgumentException.class, () -> TextSignatures.compute(List.of("a"), 129));
        rejects(IllegalArgumentException.class, () -> TextSignatures.compute(
                java.util.Collections.nCopies(TextSignatures.MAX_TOKENS + 1, "a"), 1));
        rejects(NullPointerException.class, () -> TextSignatures.compute(Arrays.asList("a", null), 1));
        rejects(IllegalArgumentException.class, () -> TextSignatures.compute(
                List.of("a".repeat(TextSignatures.MAX_TOKEN_UNITS + 1)), 1));
        var absent = TextSignatures.compute(List.of(), 32);
        check(absent.tokenCount() == 0 && absent.distinctCount() == 0
                && absent.simHash64() == 0 && absent.minHash64()[0] == -1L, "empty signatures");
        check(TextSignatures.estimatedJaccard(absent, absent) == 1.0, "empty set similarity");
        check(TextSignatures.estimatedJaccard(absent, TextSignatures.compute(List.of("x"), 32)) == 0.0,
                "empty to nonempty similarity");
        rejects(IllegalArgumentException.class, () -> TextSignatures.estimatedJaccard(
                absent, TextSignatures.compute(List.of("x"), 16)));

        for (int trial = 0; trial < 500; trial++) {
            List<String> words = new ArrayList<>();
            for (int i = RANDOM.nextInt(60); i > 0; i--) words.add(tokens[RANDOM.nextInt(tokens.length)]);
            var first = TextSignatures.compute(words, 32);
            var replay = TextSignatures.compute(words, 32);
            check(first.tokenCount() == words.size(), "signature token count");
            check(first.distinctCount() == Set.copyOf(words).size(), "signature distinct count");
            check(first.simHash64() == replay.simHash64()
                    && Arrays.equals(first.minHash64(), replay.minHash64()), "signature replay");
            Collections.reverse(words);
            var shuffled = TextSignatures.compute(words, 32);
            check(first.simHash64() == shuffled.simHash64()
                    && Arrays.equals(first.minHash64(), shuffled.minHash64()), "order independent signatures");
            var unique = new ArrayList<>(new TreeSet<>(words));
            check(Arrays.equals(first.minHash64(), TextSignatures.compute(unique, 32).minHash64()),
                    "MinHash set semantics ignore multiplicity");
            check(TextSignatures.hamming64(first, replay) == 0
                    && TextSignatures.estimatedJaccard(first, replay) == 1.0, "identical signal semantics");
            long[] projected = first.minHash64();
            projected[0] ^= 1L;
            check(!Arrays.equals(projected, first.minHash64()), "immutable primitive lanes");
        }
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
                int[] wantedFrequency = Arrays.stream(wanted)
                        .map(id -> Collections.frequency(documents.get(id), term)).toArray();
                check(Arrays.equals(index.termFrequencies(term), wantedFrequency), "primitive posting frequency");
                check(index.documentFrequency(term) == wanted.length, "document frequency");
                Map<Integer, Integer> visited = new TreeMap<>();
                index.forEachPosting(term, visited::put);
                for (int at = 0; at < wanted.length; at++) {
                    check(visited.get(wanted[at]) == wantedFrequency[at], "primitive posting visitor");
                }
                int[] returned = index.documents(term);
                if (returned.length > 0) returned[0] = -1;
                check(Arrays.equals(index.documents(term), wanted), "detached posting");
                int[] frequencies = index.termFrequencies(term);
                if (frequencies.length > 0) frequencies[0] = -1;
                check(Arrays.equals(index.termFrequencies(term), wantedFrequency), "detached term frequency");
            }
            check(index.documentCount() == documents.size(), "document-count lane");
            check(index.totalTokenCount() == documents.stream().mapToInt(List::size).sum(),
                    "total token lane");
            for (int id = 0; id < documents.size(); id++) {
                check(index.documentLength(id) == documents.get(id).size(), "document-length lane");
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

    /** Oracle uses independent per-document term scans, never the index or its posting lanes. */
    private static List<Bm25Ranker.Hit> bruteBm25(List<? extends List<String>> documents,
                                                  List<String> query, Bm25Ranker.Parameters parameters) {
        int documentCount = documents.size();
        int tokens = documents.stream().mapToInt(List::size).sum();
        double average = documentCount == 0 ? 0.0d : tokens / (double) documentCount;
        double[] scores = new double[documentCount];
        for (String term : new TreeSet<>(query)) {
            int df = 0;
            for (List<String> document : documents) if (document.contains(term)) df++;
            if (df == 0) continue;
            double idf = StrictMath.log1p((documentCount - df + 0.5d) / (df + 0.5d));
            for (int id = 0; id < documentCount; id++) {
                int frequency = Collections.frequency(documents.get(id), term);
                if (frequency == 0) continue;
                double normalized = parameters.k1() * (1.0d - parameters.b()
                        + parameters.b() * documents.get(id).size() / average);
                scores[id] += idf * frequency * (parameters.k1() + 1.0d) / (frequency + normalized);
            }
        }
        List<Bm25Ranker.Hit> ranked = new ArrayList<>();
        for (int id = 0; id < scores.length; id++) {
            if (scores[id] > 0.0d) ranked.add(new Bm25Ranker.Hit(id, scores[id]));
        }
        ranked.sort(Comparator.comparingDouble(Bm25Ranker.Hit::score).reversed()
                .thenComparingInt(Bm25Ranker.Hit::documentId));
        return ranked;
    }

    private static void ranking() throws Exception {
        Bm25Ranker.Parameters defaults = Bm25Ranker.Parameters.defaults();
        check(defaults.k1() == 1.2d && defaults.b() == 0.75d, "published BM25 parameters");
        rejects(IllegalArgumentException.class, () -> new Bm25Ranker.Parameters(-0.1d, 0.75d));
        rejects(IllegalArgumentException.class, () -> new Bm25Ranker.Parameters(Double.NaN, 0.75d));
        rejects(IllegalArgumentException.class, () -> new Bm25Ranker.Parameters(1.2d, 1.01d));
        rejects(NullPointerException.class, () -> Bm25Ranker.prepare(List.of(Arrays.asList("a", null))));
        rejects(IllegalArgumentException.class, () -> Bm25Ranker.prepare(
                Collections.nCopies(Bm25Ranker.MAX_DOCUMENTS + 1, List.of("a"))));
        var empty = Bm25Ranker.prepare(List.of());
        check(empty.rank(List.of("a"), 0).complete() && empty.rank(List.of("a"), 10).size() == 0,
                "empty corpus");
        var identical = Bm25Ranker.prepare(List.of(List.of("a"), List.of("a")));
        check(identical.rank(List.of("a"), 2).hits().get(0).documentId() == 0
                && identical.rank(List.of("a"), 2).hits().get(1).documentId() == 1,
                "stable equal-score document ordinal");
        rejects(IllegalArgumentException.class, () -> identical.rank(List.of("a"), -1));
        rejects(NullPointerException.class, () -> identical.rank(Arrays.asList("a", null), 1));
        rejects(IllegalStateException.class, () -> Bm25Ranker.prepare(
                List.of(List.of("a")), defaults, () -> { throw new IllegalStateException("cancel prepare"); }));
        rejects(IllegalStateException.class, () -> identical.rank(List.of("a"), 1,
                () -> { throw new IllegalStateException("cancel scoring"); }));

        String[] vocabulary = {"", "a", "b", "beta", "\ud83d\ude00", "\ud83d", "\u0301"};
        for (int trial = 0; trial < 360; trial++) {
            List<List<String>> documents = new ArrayList<>();
            for (int id = 0, count = RANDOM.nextInt(12); id < count; id++) {
                List<String> document = new ArrayList<>();
                for (int token = 0, words = RANDOM.nextInt(35); token < words; token++) {
                    document.add(vocabulary[RANDOM.nextInt(vocabulary.length)]);
                }
                documents.add(document);
            }
            List<String> query = new ArrayList<>();
            for (int id = 0, count = RANDOM.nextInt(10); id < count; id++) {
                query.add(vocabulary[RANDOM.nextInt(vocabulary.length)]);
            }
            Bm25Ranker.Parameters parameters = trial % 3 == 0
                    ? new Bm25Ranker.Parameters(0.0d, 1.0d)
                    : trial % 3 == 1 ? new Bm25Ranker.Parameters(2.5d, 0.0d) : defaults;
            Bm25Ranker.Prepared prepared = Bm25Ranker.prepare(documents, parameters);
            List<Bm25Ranker.Hit> oracle = bruteBm25(documents, query, parameters);
            check(prepared.sha256().matches("[0-9a-f]{64}"), "exact corpus fingerprint");
            check(prepared.sha256().equals(Bm25Ranker.prepare(documents, parameters).sha256()),
                    "deterministic corpus fingerprint");
            for (int cap : new int[]{0, 1, 3, 10, 100}) {
                Bm25Ranker.Result ranked = prepared.rank(query, cap);
                check(ranked.size() == Math.min(cap, oracle.size()), "BM25 bounded top-K size");
                check(ranked.totalMatches() == oracle.size() && ranked.complete() == (oracle.size() <= cap),
                        "BM25 exact completeness and total-matches count");
                for (int id = 0; id < ranked.size(); id++) {
                    Bm25Ranker.Hit actual = ranked.hit(id), wanted = oracle.get(id);
                    check(actual.documentId() == wanted.documentId(), "BM25 independent ranking ordinal");
                    check(Math.abs(actual.score() - wanted.score()) <= 1.0e-11d,
                            "BM25 independent numeric oracle");
                }
            }
            var stable = prepared.rank(query, 100);
            Collections.reverse(query);
            check(stable.hits().equals(prepared.rank(query, 100).hits()),
                    "BM25 order-independent query set and summation");
            for (List<String> document : documents) document.clear();
            check(stable.hits().equals(prepared.rank(query, 100).hits()), "BM25 detached input lists");
        }
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
