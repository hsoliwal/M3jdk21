// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import com.m3.tools.codec.varint.VarInts;
import com.m3.tools.graph.paths.ShortestPaths;
import com.m3.tools.graph.topology.TopologicalOrder;
import com.m3.tools.stats.streaming.RunningStatistics;
import com.m3.tools.text.diff.BoundedDiff;
import com.m3.tools.text.diff.LineDiff;
import com.m3.tools.text.diff.LineThreeWayMerge;
import com.m3.tools.text.diff.ThreeWayMerge;
import com.m3.tools.text.frequency.TermFrequency;
import com.m3.tools.text.index.Bm25Ranker;
import com.m3.tools.text.ngram.NGramIndex;
import com.m3.tools.text.ngram.NGrams;
import com.m3.tools.text.normalize.UnicodeNormalizer;
import com.m3.tools.text.search.AffixIndex;
import com.m3.tools.text.search.ExactSearch;
import com.m3.tools.text.search.MultiLiteralSearch;
import com.m3.tools.text.similarity.TextSignatures;
import com.m3.tools.text.tokenize.WordTokenizer;
import com.m3.tools.verify.sha256.Sha256;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleReader;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.function.Consumer;
import jdk.m3.io.M3File;
import jdk.m3.structured.StructuredTextTools;
import jdk.m3.text.Atom;
import jdk.m3.text.Binding;
import jdk.m3.text.Canonical;
import jdk.m3.text.Capability;
import jdk.m3.text.ExecutionResult;
import jdk.m3.text.Limits;
import jdk.m3.text.M3UnixRuntime;
import jdk.m3.text.Node;
import jdk.m3.text.Plan;
import jdk.m3.text.Registry;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextLine;
import jdk.m3.text.TextTools;

/**
 * CPU-tool bindings over the existing M3 text DAG and separately named algorithm modules.
 * String/CharSequence remains the public boundary to M3String; no second string pool exists here.
 * The existing Limits bounds input/output frames and plan geometry, not process RSS or arbitrary
 * Java instruction counts. Cancellation is cooperative at operation and emission boundaries.
 */
public final class CpuTools {
    private CpuTools() { }

    public static Session session() { return session(Limits.standard(), null); }

    public static Session session(Limits limits, ProgressMonitor monitor) {
        return new Session(limits, monitor);
    }

    /**
     * Immutable binding set and policy. The borrowed monitor is captured at construction;
     * callers sharing a Session across threads must supply a thread-safe monitor.
     * No operation completes a borrowed monitor. Every execution uses the incumbent runtime.
     */
    public static final class Session {
        private final Limits limits;
        private final ProgressMonitor monitor;
        private final Registry registry;
        private final M3UnixRuntime runtime;

        private Session(Limits limits, ProgressMonitor monitor) {
            this.limits = Objects.requireNonNull(limits, "limits");
            this.monitor = effective(monitor);
            this.monitor.checkCancelled();
            this.registry = bindings(limits, this.monitor);
            this.runtime = new M3UnixRuntime(registry, limits, Set.of(Capability.TEXT));
        }

        public Registry registry() { return registry; }
        public Limits limits() { return limits; }

        /** Builds a linear composition in the existing DAG representation. */
        public Plan pipeline(List<String> operations) {
            Objects.requireNonNull(operations, "operations");
            if (operations.isEmpty() || operations.size() > limits.maxNodes()) {
                throw new IllegalArgumentException("pipeline node budget");
            }
            List<Node> nodes = new ArrayList<>(operations.size());
            String previous = null;
            for (int i = 0; i < operations.size(); i++) {
                monitor.checkCancelled();
                String id = "step-" + i;
                Binding binding = registry.require(operations.get(i));
                nodes.add(previous == null ? Node.of(id, binding) : Node.of(id, binding, previous));
                previous = id;
            }
            return new Plan(nodes, previous);
        }

        public ExecutionResult execute(Plan plan, TextFrame input) {
            monitor.checkCancelled();
            ExecutionResult result = runtime.execute(Objects.requireNonNull(plan, "plan"), input);
            monitor.checkCancelled();
            return result;
        }

        /** Reads no file again: every stage consumes this immutable decoded source snapshot. */
        public ExecutionResult execute(List<String> operations, M3File source) {
            Plan plan = pipeline(operations);
            return execute(plan, frame(source, limits, monitor));
        }

        /** Re-executes the same bindings and compares exact frames plus all receipt fields. */
        public ExecutionResult replay(Plan plan, TextFrame input, ExecutionResult expected) {
            monitor.checkCancelled();
            ExecutionResult result = runtime.replay(plan, input, expected);
            monitor.checkCancelled();
            return result;
        }
    }

    private static Registry bindings(Limits limits, ProgressMonitor monitor) {
        List<Binding> result = new ArrayList<>();
        bind(result, "text.identity", "Retain original line content and CR/LF/CRLF endings", limits, monitor,
                (input, output) -> input.lines().forEach(output));
        bind(result, "text.sort", "Stable natural UTF-16 line-content ordering", limits, monitor,
                (input, output) -> TextTools.sort(input, false, limits).lines().forEach(output));
        bind(result, "text.sort.reverse", "Stable reverse UTF-16 line-content ordering", limits, monitor,
                (input, output) -> TextTools.sort(input, true, limits).lines().forEach(output));
        bind(result, "text.uniq", "Collapse adjacent equal-content lines under the existing TextTools contract", limits, monitor,
                (input, output) -> TextTools.uniq(input, limits).lines().forEach(output));
        bind(result, "text.normalize.nfc", "JDK Unicode NFC; preserve resulting line endings", limits, monitor,
                (input, output) -> TextFrame.fromString(UnicodeNormalizer.normalize(input.text(), Normalizer.Form.NFC))
                        .lines().forEach(output));
        bind(result, "text.normalize.nfkc", "JDK Unicode NFKC; preserve resulting line endings", limits, monitor,
                (input, output) -> TextFrame.fromString(UnicodeNormalizer.nfkc(input.text())).lines().forEach(output));
        bind(result, "text.words", "Locale.ROOT JDK word breaks; retain spans containing a letter or digit; LF output", limits, monitor,
                (input, output) -> WordTokenizer.words(input.text()).forEach(word -> line(output, word)));
        bind(result, "text.frequency", "Count each input line as one exact term; sorted term TAB count output", limits, monitor,
                (input, output) -> counts(TermFrequency.count(input.lines().stream().map(TextLine::content).toList()), output));
        bind(result, "text.ngram.2", "Count overlapping UTF-16 code-unit bigrams; sorted escaped gram TAB count output", limits, monitor,
                (input, output) -> counts(NGrams.count(input.text(), 2), output));
        bind(result, "text.ngram.3", "Count overlapping UTF-16 code-unit trigrams; sorted escaped gram TAB count output", limits, monitor,
                (input, output) -> counts(NGrams.count(input.text(), 3), output));
        bind(result, "text.fold.80", "Existing code-point fold at width 80", limits, monitor,
                (input, output) -> TextTools.fold(input, 80, limits).lines().forEach(output));
        bind(result, "text.number", "Existing nl with first=1 and step=1", limits, monitor,
                (input, output) -> TextTools.nl(input, 1L, 1L, limits).lines().forEach(output));
        bind(result, "graph.topology", "First line vertex-count; remaining lines from TAB to; deterministic integer topological order", limits, monitor,
                (input, output) -> topology(input, output, limits, monitor));
        bind(result, "graph.shortest-paths", "First line vertex-count TAB source; edges from TAB to TAB nonnegative-weight; node TAB distance; MAX means unreachable-or-overflow", limits, monitor,
                (input, output) -> paths(input, output, limits, monitor));
        bind(result, "graph.tsort", "Existing TextTools tsort over whitespace-separated named edges", limits, monitor,
                (input, output) -> TextTools.tsort(input, limits).lines().forEach(output));
        bind(result, "statistics.moments", "One finite double per line; count/mean/population-variance/min/max using existing Welford owner", limits, monitor,
                (input, output) -> moments(input, output, monitor));
        bind(result, "hash.sha256.utf8", "SHA-256 of exact reassembled frame UTF-8 bytes, including line endings", limits, monitor,
                (input, output) -> line(output, Sha256.utf8(input.text())));
        bind(result, "codec.varint.encode", "One decimal in [0,Long.MAX_VALUE] per line -> canonical hexadecimal LEB128", limits, monitor,
                (input, output) -> input.lines().forEach(value -> line(output,
                        HexFormat.of().formatHex(VarInts.encode(Long.parseLong(value.content()))))));
        bind(result, "codec.varint.decode", "One hexadecimal canonical LEB128 in [0,Long.MAX_VALUE] per line -> decimal", limits, monitor,
                (input, output) -> input.lines().forEach(value -> line(output,
                        Long.toString(VarInts.decode(HexFormat.of().parseHex(value.content()))))));
        bind(result, "structured.csv.first-column", "Existing strict CSV projection of column zero with LF record output", limits, monitor,
                (input, output) -> StructuredTextTools.selectDelimited(input, ',', new int[]{0}, limits).lines().forEach(output));
        bind(result, "structured.tsv.first-column", "Existing strict TSV projection of column zero with LF record output", limits, monitor,
                (input, output) -> StructuredTextTools.selectDelimited(input, '\t', new int[]{0}, limits).lines().forEach(output));
        bind(result, "structured.env", "Existing strict dotenv subset -> sorted escaped key TAB value rows; no expansion", limits, monitor,
                (input, output) -> StructuredTextTools.dotenv(input, limits).entrySet().stream()
                        .sorted(Map.Entry.comparingByKey()).forEach(entry -> line(output,
                                escape(entry.getKey()) + "\t" + escape(entry.getValue()))));
        return new Registry(result);
    }

    private static void bind(List<Binding> bindings, String id, String definition, Limits limits,
                             ProgressMonitor monitor, Atom atom) {
        bindings.add(new Binding(id, definition + "\nlimits=" + limits.sha256(), Implementation.ROOT,
                Set.of(Capability.TEXT), (input, output) -> {
                    monitor.checkCancelled();
                    limits.check(input);
                    monitor.message(id);
                    atom.execute(input, value -> {
                        monitor.checkCancelled();
                        output.accept(value);
                    });
                    monitor.checkCancelled();
                    monitor.worked(1);
                }));
    }

    private static void counts(Map<String, Integer> values, Consumer<TextLine> output) {
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                line(output, escape(entry.getKey()) + "\t" + entry.getValue()));
    }

    private static void line(Consumer<TextLine> output, String content) { output.accept(new TextLine(content, "\n")); }

    /** Admit line geometry before TextFrame allocates a retained row for every source line. */
    private static TextFrame frame(M3File source, Limits limits, ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        monitor.checkCancelled();
        if (source.byteLength() > limits.maxBytes()) throw new IllegalArgumentException("source byte budget");
        String text = source.text();
        int lines = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if ((i & 8191) == 0) monitor.checkCancelled();
            char c = text.charAt(i);
            if (c == '\r' || c == '\n') {
                if (++lines > limits.maxLines()) throw new IllegalArgumentException("source line budget");
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                start = i + 1;
            }
        }
        if (start < text.length() && ++lines > limits.maxLines()) throw new IllegalArgumentException("source line budget");
        monitor.checkCancelled();
        TextFrame result = TextFrame.fromString(text);
        monitor.checkCancelled();
        return result;
    }

    /** Escaped TSV field, reversible without confusing a gram's newline with a record boundary. */
    public static String escape(String value) {
        Objects.requireNonNull(value, "value");
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> result.append("\\\\");
                case '\t' -> result.append("\\t");
                case '\r' -> result.append("\\r");
                case '\n' -> result.append("\\n");
                default -> {
                    if (Character.isHighSurrogate(c) && i + 1 < value.length()
                            && Character.isLowSurrogate(value.charAt(i + 1))) {
                        result.append(c).append(value.charAt(++i));
                    } else if (Character.isSurrogate(c)) {
                        result.append("\\u");
                        for (int shift = 12; shift >= 0; shift -= 4) {
                            result.append("0123456789abcdef".charAt((c >>> shift) & 15));
                        }
                    } else result.append(c);
                }
            }
        }
        return result.toString();
    }

    private static int vertices(TextFrame input, Limits limits, boolean withSource) {
        if (input.lines().isEmpty()) throw new IllegalArgumentException("graph header required");
        String[] header = fields(input.lines().getFirst(), withSource ? 2 : 1);
        int vertices = Integer.parseInt(header[0]);
        if (vertices < (withSource ? 1 : 0) || vertices > Math.min(1_000_000, limits.maxLines())) {
            throw new IllegalArgumentException("vertex budget");
        }
        if (input.lines().size() - 1 > limits.maxEdges()) throw new IllegalArgumentException("graph edge budget");
        return vertices;
    }

    private static String[] fields(TextLine line, int count) {
        String[] values = line.content().split("\t", -1);
        if (values.length != count) throw new IllegalArgumentException("expected " + count + " tab-separated fields");
        return values;
    }

    private static void topology(TextFrame input, Consumer<TextLine> output, Limits limits, ProgressMonitor monitor) {
        int vertices = vertices(input, limits, false);
        List<TopologicalOrder.Arc> edges = new ArrayList<>(input.lines().size() - 1);
        for (int i = 1; i < input.lines().size(); i++) {
            monitor.checkCancelled();
            String[] row = fields(input.lines().get(i), 2);
            int from = Integer.parseInt(row[0]);
            int to = Integer.parseInt(row[1]);
            endpoints(vertices, from, to);
            edges.add(new TopologicalOrder.Arc(from, to));
        }
        for (int vertex : TopologicalOrder.sort(vertices, edges)) line(output, Integer.toString(vertex));
    }

    private static void paths(TextFrame input, Consumer<TextLine> output, Limits limits, ProgressMonitor monitor) {
        int vertices = vertices(input, limits, true);
        int source = Integer.parseInt(fields(input.lines().getFirst(), 2)[1]);
        if (source < 0 || source >= vertices) throw new IllegalArgumentException("source outside graph");
        List<ShortestPaths.Edge> edges = new ArrayList<>(input.lines().size() - 1);
        for (int i = 1; i < input.lines().size(); i++) {
            monitor.checkCancelled();
            String[] row = fields(input.lines().get(i), 3);
            int from = Integer.parseInt(row[0]);
            int to = Integer.parseInt(row[1]);
            endpoints(vertices, from, to);
            edges.add(new ShortestPaths.Edge(from, to, Long.parseLong(row[2])));
        }
        long[] distances = ShortestPaths.distances(vertices, edges, source);
        for (int i = 0; i < distances.length; i++) line(output, i + "\t" + distances[i]);
    }

    private static void endpoints(int vertices, int from, int to) {
        if (from < 0 || from >= vertices || to < 0 || to >= vertices) {
            throw new IllegalArgumentException("edge endpoint outside graph");
        }
    }

    private static void moments(TextFrame input, Consumer<TextLine> output, ProgressMonitor monitor) {
        RunningStatistics stats = new RunningStatistics();
        for (TextLine line : input.lines()) {
            monitor.checkCancelled();
            stats.accept(Double.parseDouble(line.content()));
        }
        line(output, "count\t" + stats.count());
        line(output, "mean\t" + stats.mean());
        line(output, "populationVariance\t" + stats.populationVariance());
        line(output, "min\t" + stats.min());
        line(output, "max\t" + stats.max());
    }

    /** Immutable, source-bound exact-search result. Truncation is explicit. */
    public static final class Search {
        private final int[] positions;
        private final boolean complete;
        private final String sha256;

        private Search(int[] positions, boolean complete, String sha256) {
            this.positions = positions.clone();
            this.complete = complete;
            this.sha256 = sha256;
        }
        public int[] positions() { return positions.clone(); }
        public int size() { return positions.length; }
        public boolean complete() { return complete; }
        public String sha256() { return sha256; }
    }

    public static Search literal(M3File source, String needle, int maxHits) {
        return literal(source, needle, maxHits, null);
    }

    /** Exact UTF-16 matching across M3String components, including overlaps and empty needles. */
    public static Search literal(M3File source, String needle, int maxHits, ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(needle, "needle");
        if (maxHits < 0 || maxHits > 1_000_000) throw new IllegalArgumentException("match budget");
        if (needle.length() > M3File.MAX_BYTES) throw new IllegalArgumentException("needle UTF-16 budget");
        ProgressMonitor progress = effective(monitor);
        progress.checkCancelled();
        // One additional observation distinguishes a complete result from a capped prefix.
        int[] observed = ExactSearch.positions(source.text(), needle, maxHits + 1);
        progress.checkCancelled();
        boolean complete = observed.length <= maxHits;
        int[] positions = complete ? observed : Arrays.copyOf(observed, maxHits);
        Canonical hash = new Canonical("m3-cpu-literal-result-v1").text(Implementation.ROOT)
                .text(source.uri().toASCIIString()).text(source.rawSha256()).text(source.utf16Sha256())
                .number(source.length()).number(maxHits).number(complete ? 1 : 0);
        // UTF-16 framing also represents a legal Java needle containing isolated surrogates.
        hash.number(needle.length());
        for (int i = 0; i < needle.length(); i++) {
            if ((i & 8191) == 0) progress.checkCancelled();
            hash.number(needle.charAt(i));
        }
        hash.number(positions.length);
        for (int i = 0; i < positions.length; i++) {
            if ((i & 8191) == 0) progress.checkCancelled();
            hash.number(positions[i]);
        }
        progress.worked(1);
        progress.checkCancelled();
        return new Search(positions, complete, hash.finish());
    }

    /** A receipt is evidence of identity; this explicit independent re-execution checks it. */
    public static boolean verifyLiteral(M3File source, String needle, int maxHits, Search expected,
                                        ProgressMonitor monitor) {
        Objects.requireNonNull(expected, "expected");
        Search actual = literal(source, needle, maxHits, monitor);
        return actual.complete == expected.complete && actual.sha256.equals(expected.sha256)
                && Arrays.equals(actual.positions, expected.positions);
    }

    /** A compact multi-literal result backed by the existing separate text-search module. */
    public static final class MultiSearch {
        private final MultiLiteralSearch.Result result;
        private final String sha256;

        private MultiSearch(MultiLiteralSearch.Result result, String sha256) {
            this.result = result;
            this.sha256 = sha256;
        }
        public int size() { return result.size(); }
        public boolean complete() { return result.complete(); }
        public MultiLiteralSearch.Hit hit(int index) { return result.hit(index); }
        public List<MultiLiteralSearch.Hit> hits() { return result.hits(); }
        public String sha256() { return sha256; }
    }

    public static MultiSearch multiLiteral(M3File source, List<String> literals, int maxHits) {
        return multiLiteral(source, literals, maxHits, null);
    }

    /**
     * One immutable M3File snapshot, one prepared UTF-16 multi-needle search.
     * The receipt includes raw-byte and UTF-16 identities, source location,
     * literal order, cap, completeness, and every emitted coordinate.
     */
    public static MultiSearch multiLiteral(M3File source, List<String> literals, int maxHits,
                                           ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        ProgressMonitor progress = effective(monitor);
        progress.checkCancelled();
        MultiLiteralSearch.Prepared search = MultiLiteralSearch.prepare(literals, progress::checkCancelled);
        MultiLiteralSearch.Result found = search.find(source.text(), maxHits, progress::checkCancelled);
        Canonical hash = new Canonical("m3-cpu-multi-literal-result-v1").text(Implementation.ROOT)
                .text(source.uri().toASCIIString()).text(source.rawSha256()).text(source.utf16Sha256())
                .number(source.length()).number(source.byteLength())
                .text(search.sha256()).number(maxHits).number(found.complete() ? 1 : 0)
                .number(found.size());
        for (int i = 0; i < found.size(); i++) {
            if ((i & 4095) == 0) progress.checkCancelled();
            var hit = found.hit(i);
            hash.number(hit.patternId()).number(hit.start()).number(hit.end());
        }
        progress.worked(1);
        progress.checkCancelled();
        return new MultiSearch(found, hash.finish());
    }

    /** Independent exact re-execution; an identity receipt alone is not semantic proof. */
    public static boolean verifyMultiLiteral(M3File source, List<String> literals, int maxHits,
                                             MultiSearch expected, ProgressMonitor monitor) {
        Objects.requireNonNull(expected, "expected");
        MultiSearch actual = multiLiteral(source, literals, maxHits, monitor);
        return actual.complete() == expected.complete() && actual.sha256().equals(expected.sha256())
                && actual.hits().equals(expected.hits());
    }

    /** One bounded immutable lexicon from the existing JDK word-break owner. */
    public static final int MAX_AFFIX_SOURCE_UTF16 = 262_144;

    public static final class FileAffixIndex {
        private final AffixIndex index;
        private final String sha256;

        private FileAffixIndex(AffixIndex index, String sha256) {
            this.index = index;
            this.sha256 = sha256;
        }

        public int tokenCount() { return index.inputCount(); }
        public int distinctCount() { return index.distinctCount(); }
        public String wordAt(int ordinal) { return index.wordAt(ordinal); }
        public boolean contains(String word) { return index.contains(word); }
        public int countPrefix(String prefix) { return index.countPrefix(prefix); }
        public int countSuffix(String suffix) { return index.countSuffix(suffix); }
        public AffixIndex.Result prefix(String prefix, int maxHits) { return index.prefix(prefix, maxHits); }
        public AffixIndex.Result suffix(String suffix, int maxHits) { return index.suffix(suffix, maxHits); }
        public AffixIndex.Result prefix(String prefix, int maxHits, ProgressMonitor monitor) {
            return index.prefix(prefix, maxHits, effective(monitor)::checkCancelled);
        }
        public AffixIndex.Result suffix(String suffix, int maxHits, ProgressMonitor monitor) {
            return index.suffix(suffix, maxHits, effective(monitor)::checkCancelled);
        }
        public String lexiconSha256() { return index.sha256(); }
        public String sha256() { return sha256; }
    }

    public static FileAffixIndex affixIndex(M3File source) {
        return affixIndex(source, null);
    }

    /**
     * One authoritative M3File snapshot; no file reopen or separate token pool.
     * Lexicon identity includes canonical UTF-16 words; the result receipt
     * additionally commits to the source URI, raw bytes and UTF-16 contents.
     */
    public static FileAffixIndex affixIndex(M3File source, ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        ProgressMonitor progress = effective(monitor);
        progress.checkCancelled();
        if (source.length() > MAX_AFFIX_SOURCE_UTF16) {
            throw new IllegalArgumentException("affix source UTF-16 budget");
        }
        AffixIndex index = AffixIndex.prepare(
                WordTokenizer.words(source.text(), progress::checkCancelled), progress::checkCancelled);
        Canonical hash = new Canonical("m3-cpu-file-affix-index-v1").text(Implementation.ROOT)
                .text(source.uri().toASCIIString()).text(source.rawSha256()).text(source.utf16Sha256())
                .number(source.byteLength()).number(source.length())
                .text(index.sha256()).number(index.inputCount()).number(index.distinctCount());
        progress.worked(1);
        progress.checkCancelled();
        return new FileAffixIndex(index, hash.finish());
    }

    /** Re-execution checks both the source receipt and all canonical dictionary IDs. */
    public static boolean verifyAffixIndex(M3File source, FileAffixIndex expected,
                                           ProgressMonitor monitor) {
        Objects.requireNonNull(expected, "expected");
        ProgressMonitor progress = effective(monitor);
        FileAffixIndex actual = affixIndex(source, progress);
        if (!actual.sha256.equals(expected.sha256)
                || actual.tokenCount() != expected.tokenCount()
                || actual.distinctCount() != expected.distinctCount()) return false;
        for (int i = 0; i < actual.distinctCount(); i++) {
            if ((i & 4095) == 0) progress.checkCancelled();
            if (!actual.wordAt(i).equals(expected.wordAt(i))) return false;
        }
        progress.checkCancelled();
        return true;
    }

    /** The existing M3File snapshots own both sides; the patch owns no file descriptor. */
    public static final class FileDiff {
        private final BoundedDiff.Patch patch;
        private final String sha256;

        private FileDiff(BoundedDiff.Patch patch, String sha256) {
            this.patch = patch;
            this.sha256 = sha256;
        }

        public BoundedDiff.Patch patch() { return patch; }
        public boolean complete() { return patch.complete(); }
        public int distance() { return patch.distance(); }
        public String sha256() { return sha256; }
        public String apply(M3File original) { return patch.apply(Objects.requireNonNull(original).text()); }
    }

    public static FileDiff diff(M3File source, M3File target, int maxEdits) {
        return diff(source, target, maxEdits, null);
    }

    /**
     * Bounded exact UTF-16 edit script with a receipt for two ordered, immutable
     * M3File snapshots. A distance-budget miss remains explicitly incomplete.
     */
    public static FileDiff diff(M3File source, M3File target, int maxEdits, ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        ProgressMonitor progress = effective(monitor);
        progress.checkCancelled();
        BoundedDiff.Patch patch = BoundedDiff.diff(
                source.text(), target.text(), maxEdits, progress::checkCancelled);
        Canonical hash = new Canonical("m3-cpu-file-bounded-diff-v1").text(Implementation.ROOT)
                .text(source.uri().toASCIIString()).text(source.rawSha256()).text(source.utf16Sha256())
                .number(source.byteLength()).number(source.length())
                .text(target.uri().toASCIIString()).text(target.rawSha256()).text(target.utf16Sha256())
                .number(target.byteLength()).number(target.length())
                .number(maxEdits).number(patch.complete() ? 1 : 0).number(patch.distance())
                .text(patch.sourceSha256()).text(patch.targetSha256()).number(patch.changes().size());
        for (BoundedDiff.Change change : patch.changes()) {
            progress.checkCancelled();
            hash.number(change.start()).number(change.end()).number(change.inserted().length());
            for (int index = 0; index < change.inserted().length(); index++) {
                if ((index & 8191) == 0) progress.checkCancelled();
                hash.number(change.inserted().charAt(index));
            }
        }
        progress.worked(1);
        progress.checkCancelled();
        return new FileDiff(patch, hash.finish());
    }

    /** Replay recomputes the whole edit script and both source-bound identities. */
    public static boolean verifyDiff(M3File source, M3File target, int maxEdits,
                                     FileDiff expected, ProgressMonitor monitor) {
        Objects.requireNonNull(expected, "expected");
        FileDiff actual = diff(source, target, maxEdits, monitor);
        return actual.sha256().equals(expected.sha256())
                && actual.complete() == expected.complete()
                && actual.distance() == expected.distance()
                && actual.patch().changes().equals(expected.patch().changes());
    }

    /** Exact line-oriented patch; retains no source file handle or line text dictionary. */
    public static final class FileLineDiff {
        private final LineDiff.Patch patch;
        private final String sourceUri;
        private final String sourceRawSha256;
        private final String sourceUtf16Sha256;
        private final String sha256;

        private FileLineDiff(LineDiff.Patch patch, M3File source, String sha256) {
            this.patch = patch;
            this.sourceUri = source.uri().toASCIIString();
            this.sourceRawSha256 = source.rawSha256();
            this.sourceUtf16Sha256 = source.utf16Sha256();
            this.sha256 = sha256;
        }

        public LineDiff.Patch patch() { return patch; }
        public boolean complete() { return patch.complete(); }
        public int distance() { return patch.distance(); }
        public String sha256() { return sha256; }

        /** Validate file URI and both admitted identities before UTF-16 materialization. */
        public String apply(M3File original) {
            Objects.requireNonNull(original, "original");
            if (!sourceUri.equals(original.uri().toASCIIString())
                    || !sourceRawSha256.equals(original.rawSha256())
                    || !sourceUtf16Sha256.equals(original.utf16Sha256())) {
                throw new IllegalArgumentException("line patch source identity mismatch");
            }
            return patch.apply(original.text());
        }
    }

    public static FileLineDiff lineDiff(M3File source, M3File target, int maxEdits) {
        return lineDiff(source, target, maxEdits, null);
    }

    /**
     * A source-sealed line edit script with exact line terminators. The bounded
     * shortest-edit engine is reused on indexed line IDs, not input characters.
     * The returned result is never a partial patch on an edit-budget miss.
     */
    public static FileLineDiff lineDiff(M3File source, M3File target, int maxEdits,
                                        ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        ProgressMonitor progress = effective(monitor);
        progress.checkCancelled();
        LineDiff.Patch patch = LineDiff.diff(source.text(), target.text(),
                maxEdits, progress::checkCancelled);
        Canonical hash = new Canonical("m3-cpu-file-line-diff-v1").text(Implementation.ROOT);
        for (M3File file : List.of(source, target)) {
            progress.checkCancelled();
            hash.text(file.uri().toASCIIString()).text(file.rawSha256()).text(file.utf16Sha256())
                    .number(file.byteLength()).number(file.length());
        }
        hash.number(maxEdits).number(patch.complete() ? 1 : 0).number(patch.distance())
                .number(patch.sourceLines()).number(patch.targetLines())
                .text(patch.sourceSha256()).text(patch.targetSha256())
                .number(patch.changes().size());
        for (LineDiff.LineChange change : patch.changes()) {
            progress.checkCancelled();
            hash.number(change.startLine()).number(change.endLine())
                    .number(change.startUtf16()).number(change.endUtf16())
                    .number(change.insertedLines()).number(change.inserted().length());
            for (int index = 0; index < change.inserted().length(); index++) {
                if ((index & 8191) == 0) progress.checkCancelled();
                hash.number(change.inserted().charAt(index));
            }
        }
        progress.worked(1);
        progress.checkCancelled();
        return new FileLineDiff(patch, source, hash.finish());
    }

    /** Re-execution proves both admitted file identities and all line coordinates. */
    public static boolean verifyLineDiff(M3File source, M3File target, int maxEdits,
                                         FileLineDiff expected, ProgressMonitor monitor) {
        Objects.requireNonNull(expected, "expected");
        FileLineDiff actual = lineDiff(source, target, maxEdits, monitor);
        return actual.sha256().equals(expected.sha256())
                && actual.complete() == expected.complete()
                && actual.distance() == expected.distance()
                && actual.patch().changes().equals(expected.patch().changes());
    }

    /** Exact base/left/right source-bound result; owns no file handles or new String pool. */
    public static final class FileMerge {
        private final ThreeWayMerge.Result result;
        private final String sourceUri;
        private final String sourceRawSha256;
        private final String sourceUtf16Sha256;
        private final String sha256;

        private FileMerge(ThreeWayMerge.Result result, M3File source, String sha256) {
            this.result = result;
            this.sourceUri = source.uri().toASCIIString();
            this.sourceRawSha256 = source.rawSha256();
            this.sourceUtf16Sha256 = source.utf16Sha256();
            this.sha256 = sha256;
        }

        public ThreeWayMerge.Result result() { return result; }
        public ThreeWayMerge.Status status() { return result.status(); }
        public boolean mergeable() { return result.mergeable(); }
        public int conflictCount() { return result.conflictCount(); }
        public String sha256() { return sha256; }

        /** Rejects stale and location-substituted source snapshots before materialization. */
        public String apply(M3File original) {
            Objects.requireNonNull(original, "original");
            if (!sourceUri.equals(original.uri().toASCIIString())
                    || !sourceRawSha256.equals(original.rawSha256())
                    || !sourceUtf16Sha256.equals(original.utf16Sha256())) {
                throw new IllegalArgumentException("three-way merge base identity mismatch");
            }
            return result.merged();
        }
    }

    public static FileMerge merge(M3File base, M3File left, M3File right, int maxEdits) {
        return merge(base, left, right, maxEdits, null);
    }

    /**
     * Conservative three-way combination of the two independently bounded
     * source-relative edit scripts. Conflicts never produce a partial file.
     */
    public static FileMerge merge(M3File base, M3File left, M3File right,
                                  int maxEdits, ProgressMonitor monitor) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        ProgressMonitor progress = effective(monitor);
        progress.checkCancelled();
        ThreeWayMerge.Result merged = ThreeWayMerge.merge(
                base.text(), left.text(), right.text(), maxEdits, progress::checkCancelled);
        Canonical hash = new Canonical("m3-cpu-file-three-way-merge-v1").text(Implementation.ROOT);
        for (M3File source : List.of(base, left, right)) {
            progress.checkCancelled();
            hash.text(source.uri().toASCIIString()).text(source.rawSha256()).text(source.utf16Sha256())
                    .number(source.byteLength()).number(source.length());
        }
        hash.number(maxEdits).text(merged.status().name())
                .number(merged.leftPatch().distance()).number(merged.rightPatch().distance())
                .text(merged.leftPatch().sourceSha256()).text(merged.leftPatch().targetSha256())
                .text(merged.rightPatch().sourceSha256()).text(merged.rightPatch().targetSha256());
        hashMergeChanges(hash, merged.leftPatch().changes(), progress);
        hashMergeChanges(hash, merged.rightPatch().changes(), progress);
        hash.number(merged.conflictCount()).number(merged.conflictsComplete() ? 1 : 0);
        for (ThreeWayMerge.Conflict conflict : merged.conflicts()) {
            hashMergeChanges(hash, List.of(conflict.left(), conflict.right()), progress);
        }
        hashMergeChanges(hash, merged.mergedChanges(), progress);
        if (merged.mergeable()) {
            String output = merged.merged();
            hash.number(output.length());
            for (int index = 0; index < output.length(); index++) {
                if ((index & 8191) == 0) progress.checkCancelled();
                hash.number(output.charAt(index));
            }
        }
        progress.worked(1);
        progress.checkCancelled();
        return new FileMerge(merged, base, hash.finish());
    }

    /** Independently recomputes all three file identities, scripts and witnesses. */
    public static boolean verifyMerge(M3File base, M3File left, M3File right, int maxEdits,
                                      FileMerge expected, ProgressMonitor monitor) {
        Objects.requireNonNull(expected, "expected");
        FileMerge actual = merge(base, left, right, maxEdits, monitor);
        return actual.sha256().equals(expected.sha256())
                && actual.status() == expected.status()
                && actual.result().mergedChanges().equals(expected.result().mergedChanges())
                && actual.result().conflicts().equals(expected.result().conflicts());
    }

    private static void hashMergeChanges(Canonical hash, List<BoundedDiff.Change> changes,
                                         ProgressMonitor progress) {
        hash.number(changes.size());
        for (BoundedDiff.Change change : changes) {
            progress.checkCancelled();
            hash.number(change.start()).number(change.end()).number(change.inserted().length());
            for (int unit = 0; unit < change.inserted().length(); unit++) {
                if ((unit & 8191) == 0) progress.checkCancelled();
                hash.number(change.inserted().charAt(unit));
            }
        }
    }

    /** Exact three-snapshot line merge with independently source-sealed line edit scripts. */
    public static final class FileLineMerge {
        private final LineThreeWayMerge.Result result;
        private final String sourceUri;
        private final String sourceRawSha256;
        private final String sourceUtf16Sha256;
        private final String sha256;

        private FileLineMerge(LineThreeWayMerge.Result result, M3File source, String sha256) {
            this.result = result;
            this.sourceUri = source.uri().toASCIIString();
            this.sourceRawSha256 = source.rawSha256();
            this.sourceUtf16Sha256 = source.utf16Sha256();
            this.sha256 = sha256;
        }

        public LineThreeWayMerge.Result result() { return result; }
        public ThreeWayMerge.Status status() { return result.status(); }
        public boolean mergeable() { return result.mergeable(); }
        public int conflictCount() { return result.conflictCount(); }
        public String sha256() { return sha256; }

        /** Refuse a relocated or changed base before returning any merged text. */
        public String apply(M3File original) {
            Objects.requireNonNull(original, "original");
            if (!sourceUri.equals(original.uri().toASCIIString())
                    || !sourceRawSha256.equals(original.rawSha256())
                    || !sourceUtf16Sha256.equals(original.utf16Sha256())) {
                throw new IllegalArgumentException("line three-way base identity mismatch");
            }
            return result.merged();
        }
    }

    public static FileLineMerge lineMerge(M3File base, M3File left, M3File right, int maxEdits) {
        return lineMerge(base, left, right, maxEdits, null);
    }

    /**
     * Independent line-level shortest scripts share the admitted base String.
     * No file is reopened, no second text owner is retained, and ambiguous edits
     * produce conflict witnesses rather than a partially applied result.
     */
    public static FileLineMerge lineMerge(M3File base, M3File left, M3File right,
                                          int maxEdits, ProgressMonitor monitor) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        ProgressMonitor progress = effective(monitor);
        progress.checkCancelled();
        LineThreeWayMerge.Result merged = LineThreeWayMerge.merge(
                base.text(), left.text(), right.text(), maxEdits, progress::checkCancelled);
        Canonical hash = new Canonical("m3-cpu-file-line-three-way-merge-v1").text(Implementation.ROOT);
        for (M3File source : List.of(base, left, right)) {
            progress.checkCancelled();
            hash.text(source.uri().toASCIIString()).text(source.rawSha256()).text(source.utf16Sha256())
                    .number(source.byteLength()).number(source.length());
        }
        hash.number(maxEdits).text(merged.status().name());
        for (LineDiff.Patch patch : List.of(merged.leftPatch(), merged.rightPatch())) {
            progress.checkCancelled();
            hash.number(patch.sourceLength()).number(patch.targetLength())
                    .number(patch.sourceLines()).number(patch.targetLines()).number(patch.distance())
                    .text(patch.sourceSha256()).text(patch.targetSha256());
            hashLineMergeChanges(hash, patch.changes(), progress);
        }
        hash.number(merged.conflictCount()).number(merged.conflictsComplete() ? 1 : 0);
        for (LineThreeWayMerge.Conflict conflict : merged.conflicts()) {
            hashLineMergeChanges(hash, List.of(conflict.left(), conflict.right()), progress);
        }
        hashLineMergeChanges(hash, merged.mergedChanges(), progress);
        if (merged.mergeable()) {
            String output = merged.merged();
            hash.number(output.length());
            for (int index = 0; index < output.length(); index++) {
                if ((index & 8191) == 0) progress.checkCancelled();
                hash.number(output.charAt(index));
            }
        }
        progress.worked(1);
        progress.checkCancelled();
        return new FileLineMerge(merged, base, hash.finish());
    }

    /** Replay and compare the ordered three-file identity and every line hunk. */
    public static boolean verifyLineMerge(M3File base, M3File left, M3File right, int maxEdits,
                                          FileLineMerge expected, ProgressMonitor monitor) {
        Objects.requireNonNull(expected, "expected");
        FileLineMerge actual = lineMerge(base, left, right, maxEdits, monitor);
        return actual.sha256().equals(expected.sha256())
                && actual.status() == expected.status()
                && actual.result().mergedChanges().equals(expected.result().mergedChanges())
                && actual.result().conflicts().equals(expected.result().conflicts());
    }

    private static void hashLineMergeChanges(Canonical hash, List<LineDiff.LineChange> changes,
                                             ProgressMonitor progress) {
        hash.number(changes.size());
        for (LineDiff.LineChange change : changes) {
            progress.checkCancelled();
            hash.number(change.startLine()).number(change.endLine())
                    .number(change.startUtf16()).number(change.endUtf16())
                    .number(change.insertedLines()).number(change.inserted().length());
            for (int unit = 0; unit < change.inserted().length(); unit++) {
                if ((unit & 8191) == 0) progress.checkCancelled();
                hash.number(change.inserted().charAt(unit));
            }
        }
    }

    /** Indexed UTF-16 characters, in source-offset order, rather than tokenized words. */
    public static final int MAX_GRAM_SOURCE_UTF16 = 262_144;

    public static final class GramIndex {
        private final NGramIndex index;
        private final String sha256;

        private GramIndex(NGramIndex index, String sha256) {
            this.index = index;
            this.sha256 = sha256;
        }
        public int gramSize() { return index.gramSize(); }
        public int windowCount() { return index.windowCount(); }
        public int distinctCount() { return index.distinctCount(); }
        public int frequency(String gram) { return index.frequency(gram); }
        public int[] positions(String gram) { return index.positions(gram); }
        public Map<String, Integer> counts() { return index.counts(); }
        public String sha256() { return sha256; }
    }

    public static GramIndex gramIndex(M3File source, int gramSize) {
        return gramIndex(source, gramSize, null);
    }

    /**
     * Exact source-bound positional n-grams. The receipt commits to the M3File
     * location, raw bytes, UTF-16 identity, gram size and every output offset.
     * Rolling hashes are only candidate filters; source identity is not inferred.
     */
    public static GramIndex gramIndex(M3File source, int gramSize, ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        ProgressMonitor progress = effective(monitor);
        progress.checkCancelled();
        if (source.length() > MAX_GRAM_SOURCE_UTF16) {
            throw new IllegalArgumentException("gram index source UTF-16 budget");
        }
        NGramIndex index = NGrams.prepare(source.text(), gramSize, progress::checkCancelled);
        Canonical hash = new Canonical("m3-cpu-file-gram-index-v1").text(Implementation.ROOT)
                .text(source.uri().toASCIIString()).text(source.rawSha256()).text(source.utf16Sha256())
                .number(source.byteLength()).number(source.length()).number(gramSize)
                .number(index.windowCount()).number(index.distinctCount());
        for (int ordinal = 0; ordinal < index.distinctCount(); ordinal++) {
            progress.checkCancelled();
            String gram = index.gramAt(ordinal);
            hash.number(gram.length());
            for (int unit = 0; unit < gram.length(); unit++) hash.number(gram.charAt(unit));
            hash.number(index.frequencyAt(ordinal));
            index.forEachPosition(ordinal, offset -> {
                progress.checkCancelled();
                hash.number(offset);
            });
        }
        progress.worked(1);
        progress.checkCancelled();
        return new GramIndex(index, hash.finish());
    }

    /** Fresh computation checks the full positional result and exact file-bound receipt. */
    public static boolean verifyGramIndex(M3File source, int gramSize, GramIndex expected,
                                          ProgressMonitor monitor) {
        Objects.requireNonNull(expected, "expected");
        GramIndex actual = gramIndex(source, gramSize, monitor);
        if (!actual.sha256().equals(expected.sha256()) || actual.windowCount() != expected.windowCount()
                || actual.distinctCount() != expected.distinctCount()) return false;
        for (String gram : expected.counts().keySet()) {
            if (actual.frequency(gram) != expected.frequency(gram)
                    || !Arrays.equals(actual.positions(gram), expected.positions(gram))) return false;
        }
        return true;
    }

    /** Signature inputs use the existing word breaker; this tighter cap bounds eager tokenization. */
    public static final int MAX_SIGNATURE_SOURCE_UTF16 = 262_144;

    public record SignatureResult(TextSignatures.Signature signature, String sha256) {
        public SignatureResult { Objects.requireNonNull(signature); Objects.requireNonNull(sha256); }
    }

    public static SignatureResult signature(M3File source, int lanes) {
        return signature(source, lanes, null);
    }

    /**
     * Computes candidate-only SimHash/MinHash signals. It cannot prove two
     * sources equal; exact M3File identities remain authoritative.
     */
    public static SignatureResult signature(M3File source, int lanes, ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        ProgressMonitor progress = effective(monitor);
        progress.checkCancelled();
        if (source.length() > MAX_SIGNATURE_SOURCE_UTF16) {
            throw new IllegalArgumentException("signature source UTF-16 budget");
        }
        TextSignatures.Signature value = TextSignatures.compute(
                WordTokenizer.words(source.text(), progress::checkCancelled), lanes,
                progress::checkCancelled);
        progress.checkCancelled();
        Canonical hash = new Canonical("m3-cpu-text-signature-result-v1").text(Implementation.ROOT)
                .text(source.uri().toASCIIString()).text(source.rawSha256()).text(source.utf16Sha256())
                .number(source.length()).number(lanes).number(value.tokenCount())
                .number(value.distinctCount()).number(value.simHash64());
        for (long candidate : value.minHash64()) hash.number(candidate);
        progress.worked(1);
        progress.checkCancelled();
        return new SignatureResult(value, hash.finish());
    }

    /** Replays the signature and compares its exact primitive lanes and file-bound receipt. */
    public static boolean verifySignature(M3File source, int lanes, SignatureResult expected,
                                          ProgressMonitor monitor) {
        Objects.requireNonNull(expected, "expected");
        SignatureResult actual = signature(source, lanes, monitor);
        return actual.sha256().equals(expected.sha256())
                && actual.signature().simHash64() == expected.signature().simHash64()
                && Arrays.equals(actual.signature().minHash64(), expected.signature().minHash64());
    }

    /** Source-bound ranking over the incumbent UTF-16 tokenizer and primitive inverted index. */
    public static final class Ranking {
        private final Bm25Ranker.Result ranked;
        private final String sha256;

        private Ranking(Bm25Ranker.Result ranked, String sha256) {
            this.ranked = ranked;
            this.sha256 = sha256;
        }
        public int size() { return ranked.size(); }
        public int totalMatches() { return ranked.totalMatches(); }
        public boolean complete() { return ranked.complete(); }
        public Bm25Ranker.Hit hit(int ordinal) { return ranked.hit(ordinal); }
        public List<Bm25Ranker.Hit> hits() { return ranked.hits(); }
        public String sha256() { return sha256; }
    }

    public static final int MAX_RANK_SOURCE_UTF16 = 4 * 1024 * 1024;

    public static Ranking rank(List<M3File> sources, List<String> query, int maxResults) {
        return rank(sources, query, maxResults, null);
    }

    /**
     * Admission binds every ordered M3File URI/raw-byte/UTF-16 identity, the
     * exact query tokens, prepared corpus SHA, result cap and scored ordinals.
     * BM25 scores are approximate relevance signals, never source equality.
     */
    public static Ranking rank(List<M3File> sources, List<String> query, int maxResults,
                               ProgressMonitor monitor) {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(query, "query");
        if (sources.size() > Bm25Ranker.MAX_DOCUMENTS) {
            throw new IllegalArgumentException("rank source-count budget");
        }
        List<M3File> snapshot = List.copyOf(sources);
        List<String> exactQuery = List.copyOf(query);
        ProgressMonitor progress = effective(monitor);
        progress.checkCancelled();
        Canonical hash = new Canonical("m3-cpu-bm25-ranked-sources-v1").text(Implementation.ROOT)
                .number(snapshot.size());
        List<List<String>> documents = new ArrayList<>(snapshot.size());
        int sourceUnits = 0;
        for (int id = 0; id < snapshot.size(); id++) {
            progress.checkCancelled();
            M3File source = snapshot.get(id);
            sourceUnits = Math.addExact(sourceUnits, source.length());
            if (sourceUnits > MAX_RANK_SOURCE_UTF16) {
                throw new IllegalArgumentException("rank combined UTF-16 source budget");
            }
            hash.number(id).text(source.uri().toASCIIString()).text(source.rawSha256())
                    .text(source.utf16Sha256()).number(source.byteLength()).number(source.length());
            documents.add(WordTokenizer.words(source.text(), progress::checkCancelled));
        }
        Bm25Ranker.Prepared corpus = Bm25Ranker.prepare(
                documents, Bm25Ranker.Parameters.defaults(), progress::checkCancelled);
        Bm25Ranker.Result ranked = corpus.rank(exactQuery, maxResults, progress::checkCancelled);
        hash.text(corpus.sha256()).number(maxResults).number(ranked.totalMatches())
                .number(ranked.complete() ? 1 : 0).number(exactQuery.size());
        for (String token : exactQuery) {
            progress.checkCancelled();
            hash.number(token.length());
            for (int i = 0; i < token.length(); i++) {
                if ((i & 4095) == 0) progress.checkCancelled();
                hash.number(token.charAt(i));
            }
        }
        hash.number(ranked.size());
        for (int id = 0; id < ranked.size(); id++) {
            progress.checkCancelled();
            Bm25Ranker.Hit hit = ranked.hit(id);
            hash.number(hit.documentId()).number(Double.doubleToLongBits(hit.score()));
        }
        progress.worked(1);
        progress.checkCancelled();
        return new Ranking(ranked, hash.finish());
    }

    /** Deterministic re-execution checks the source-bound receipt and exact output lanes. */
    public static boolean verifyRank(List<M3File> sources, List<String> query, int maxResults,
                                     Ranking expected, ProgressMonitor monitor) {
        Objects.requireNonNull(expected, "expected");
        Ranking actual = rank(sources, query, maxResults, monitor);
        return actual.sha256().equals(expected.sha256())
                && actual.totalMatches() == expected.totalMatches()
                && actual.hits().equals(expected.hits());
    }

    /**
     * Binds an existing DAG receipt to an admitted file's bytes, characters and location.
     * The check prevents attaching a receipt for different input text to this source.
     * This is an integrity receipt, not a signature or a substitute for replay.
     */
    public static String sourceReceipt(M3File source, ExecutionResult result) {
        return sourceReceipt(source, result, Limits.standard(), null);
    }

    /** Same source binding with explicit input budgets and cooperative cancellation. */
    public static String sourceReceipt(M3File source, ExecutionResult result, Limits limits, ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(limits, "limits");
        ProgressMonitor progress = effective(monitor);
        if (!frame(source, limits, progress).sha256().equals(result.receipt().inputSha256())) {
            throw new IllegalArgumentException("source does not match execution input");
        }
        String receipt = new Canonical("m3-cpu-file-execution-v1").text(Implementation.ROOT)
                .text(source.uri().toASCIIString()).text(source.rawSha256()).text(source.utf16Sha256())
                .number(source.byteLength()).number(source.length()).text(result.receipt().sha256()).finish();
        progress.worked(1);
        progress.checkCancelled();
        return receipt;
    }

    private static ProgressMonitor effective(ProgressMonitor monitor) {
        return monitor == null ? ProgressMonitors.current() : monitor;
    }

    /** Digest of actual linked tool class/resource bytes, computed once per class-loader lifetime. */
    public static String implementationSha256() { return Implementation.ROOT; }

    private static final class Implementation {
        private static final String ROOT = root();
        private static String root() {
            Module module = CpuTools.class.getModule();
            ModuleLayer layer = module.getLayer();
            if (!module.isNamed() || layer == null) throw new IllegalStateException("named CPU-tools module required");
            List<String> names = new ArrayList<>(module.getDescriptor().requires().stream().map(r -> r.name())
                    .filter(name -> name.startsWith("com.m3.tools.") || name.startsWith("jdk.m3.")).toList());
            names.add(module.getName());
            names.sort(String::compareTo);
            Canonical root = new Canonical("m3-cpu-module-set-v1").text(System.getProperty("java.runtime.version"))
                    .text(System.getProperty("java.vm.version")).number(names.size());
            for (String name : names) {
                var resolved = layer.configuration().findModule(name).orElseThrow();
                root.text(name);
                try (ModuleReader reader = resolved.reference().open(); var entries = reader.list()) {
                    List<String> resources = entries.filter(entry -> !entry.endsWith("/")).sorted().toList();
                    root.number(resources.size());
                    for (String resource : resources) {
                        ByteBuffer bytes = reader.read(resource).orElseThrow(() -> new IOException("missing module resource"));
                        try {
                            MessageDigest digest = MessageDigest.getInstance("SHA-256");
                            digest.update(bytes.duplicate());
                            root.text(resource).text(HexFormat.of().formatHex(digest.digest()));
                        } finally { reader.release(bytes); }
                    }
                } catch (IOException failure) { throw new UncheckedIOException(failure); }
                catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
            }
            return root.finish();
        }
    }
}
