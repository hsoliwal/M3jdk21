// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.m3.tools.cpu.CpuTools;
import com.m3.tools.cpu.CpuToolsCli;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;
import jdk.m3.io.M3File;
import jdk.m3.text.ExecutionResult;
import jdk.m3.text.Limits;
import jdk.m3.text.Node;
import jdk.m3.text.Plan;
import jdk.m3.text.Receipt;
import jdk.m3.text.TextFrame;

/** Composition, admission, replay, literal-search oracle and linked CLI integration. */
public final class M3CpuToolsTest {
    private static int checks;
    private M3CpuToolsTest() { }

    public static void main(String[] args) throws Exception {
        Path work = Files.createTempDirectory("m3-cpu-integration-");
        try {
            registryAndOperations();
            dagAndReplay();
            budgetsAndCancellation();
            fileAndLiteral(work);
            cli(work);
            System.out.println("M3_CPU_INTEGRATION_PASS checks=" + checks
                    + " storage=" + Boolean.getBoolean("m3.test.requireStorage"));
        } finally {
            try (var paths = Files.walk(work)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void registryAndOperations() {
        var session = CpuTools.session();
        var ids = session.registry().bindings().stream().map(b -> b.id()).toList();
        check(ids.size() == 22, "exact registry admission");
        check(ids.equals(ids.stream().sorted().distinct().toList()), "stable unique operation inventory");
        check(CpuTools.implementationSha256().matches("[0-9a-f]{64}"), "actual module-set seal");
        check(session.registry().sha256().equals(CpuTools.session().registry().sha256()), "repeatable registry");
        check(!session.registry().sha256().equals(CpuTools.session(new Limits(4, 4, 10, 1000), null)
                .registry().sha256()), "binding identity includes budgets");
        long modules = ModuleLayer.boot().modules().stream().map(Module::getName)
                .filter(n -> n.startsWith("com.m3.tools.") || n.startsWith("jdk.m3.")).count();
        check(modules == 20, "all twenty tool modules resolved");

        expectText("b\r\na\rb\n", List.of("text.identity"), "b\r\na\rb\n");
        expectText("b\na\nb\n", List.of("text.sort", "text.uniq"), "a\nb\n");
        expectText("b\na\nc\n", List.of("text.sort.reverse"), "c\nb\na\n");
        expectText("cafe\u0301 beta caf\u00e9\n", List.of("text.normalize.nfc", "text.words", "text.frequency"),
                "beta\t1\ncaf\u00e9\t2\n");
        expectText("\uff21\uff22\uff23\r\n", List.of("text.normalize.nfkc"), "ABC\r\n");
        expectText("a\ta\na\\b\na\ta\n", List.of("text.frequency"), "a\\ta\t2\na\\\\b\t1\n");
        expectText("A\ud83d\ude42B", List.of("text.ngram.2"), "A\\ud83d\t1\n\ud83d\ude42\t1\n\\ude42B\t1\n");
        expectText("A\ud83d\ude42B", List.of("text.ngram.3"), "A\ud83d\ude42\t1\n\ud83d\ude42B\t1\n");
        check(CpuTools.escape("\\uD800\ud800\t\r\n").equals("\\\\uD800\\ud800\\t\\r\\n"), "unambiguous escaped UTF-16 fields");
        expectText("abc", List.of("hash.sha256.utf8"),
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad\n");
        expectText("0\n127\n128\n16384\n9223372036854775807\n", List.of("codec.varint.encode"),
                "00\n7f\n8001\n808001\nffffffffffffffff7f\n");
        expectText("0\n127\n128\n16384\n9223372036854775807\n",
                List.of("codec.varint.encode", "codec.varint.decode"),
                "0\n127\n128\n16384\n9223372036854775807\n");
        refuseOperation("codec.varint.encode", "9223372036854775808\n");
        refuseOperation("codec.varint.encode", "-1\n");
        refuseOperation("codec.varint.decode", "8000\n");
        refuseOperation("codec.varint.decode", "ffffffffffffffffff01\n");
        expectText("4\n0\t2\n1\t2\n2\t3\n", List.of("graph.topology"), "0\n1\n2\n3\n");
        expectText("0\n", List.of("graph.topology"), "");
        refuseOperation("graph.topology", "2\n0\t1\n1\t0\n");
        refuseOperation("graph.topology", "2\n0\t2\n");
        refuseOperation("graph.topology", "2\n0 1\n");
        expectText("5\t0\n0\t1\t9\n0\t2\t2\n2\t1\t3\n1\t3\t1\n",
                List.of("graph.shortest-paths"), "0\t0\n1\t5\n2\t2\n3\t6\n4\t9223372036854775807\n");
        refuseOperation("graph.shortest-paths", "2\t0\n0\t1\t-1\n");
        refuseOperation("graph.shortest-paths", "2\t2\n");
        expectText("alpha beta\nbeta gamma\n", List.of("graph.tsort"), "alpha\nbeta\ngamma\n");
        expectText("1\n2\n3\n4\n", List.of("statistics.moments"),
                "count\t4\nmean\t2.5\npopulationVariance\t1.25\nmin\t1.0\nmax\t4.0\n");
        refuseOperation("statistics.moments", "NaN\n");
        refuseOperation("statistics.moments", "Infinity\n");
        expectText("name,age\r\nAda,36\r\n", List.of("structured.csv.first-column"), "name\nAda\n");
        expectText("name\tage\nAda\t36\n", List.of("structured.tsv.first-column"), "name\nAda\n");
        expectText("B=two\nA=one\n", List.of("structured.env"), "A\tone\nB\ttwo\n");
        expectText("x\ny\n", List.of("text.number"), "1\tx\n2\ty\n");
        expectText("a".repeat(81) + "\n", List.of("text.fold.80"), "a".repeat(80) + "\na\n");
    }

    private static void dagAndReplay() {
        var session = CpuTools.session();
        var identity = session.registry().require("text.identity");
        var reverse = session.registry().require("text.sort.reverse");
        var a = Node.of("a", identity);
        var b = Node.of("b", reverse);
        var c = Node.of("c", identity, "a", "b");
        var plan = new Plan(List.of(c, b, a), "c");
        var input = TextFrame.fromString("a\nb\n");
        var result = session.execute(plan, input);
        check(result.output().text().equals("a\nb\nb\na\n"), "ordered fan-in uses incumbent DAG");
        check(result.receipt().steps().size() == 3, "one receipt for each executed atom");
        check(session.replay(plan, input, result).equals(result), "exact re-execution");
        check(session.execute(new Plan(List.of(a, c, b), "c"), input).equals(result), "input node order has no semantic effect");
        expect(IllegalArgumentException.class, () -> session.replay(plan, TextFrame.fromString("b\na\n"), result));
        var receipt = result.receipt();
        var steps = new ArrayList<>(receipt.steps());
        java.util.Collections.reverse(steps);
        var forged = new ExecutionResult(result.output(), new Receipt(receipt.planSha256(), receipt.registrySha256(),
                receipt.policySha256(), receipt.inputSha256(), receipt.outputSha256(), steps));
        expect(IllegalStateException.class, () -> session.replay(plan, input, forged));
        var monitor = new CountMonitor();
        var checked = CpuTools.session(Limits.standard(), monitor);
        expect(IllegalArgumentException.class, () -> checked.execute(new Plan(List.of(
                new Node("a", identity.id(), "0".repeat(64), List.of())), "a"), input));
        expect(IllegalArgumentException.class, () -> checked.execute(new Plan(List.of(
                Node.of("a", identity, "b"), Node.of("b", identity, "a")), "a"), input));
        expect(IllegalArgumentException.class, () -> checked.execute(new Plan(List.of(Node.of("a", identity),
                Node.of("unused", identity)), "a"), input));
        check(monitor.messages == 0 && monitor.work == 0, "whole graph refusal precedes any atom invocation");
        expect(IllegalArgumentException.class, () -> session.pipeline(List.of("missing")));
        expect(IllegalArgumentException.class, () -> session.pipeline(List.of()));
    }

    private static void budgetsAndCancellation() {
        var input = TextFrame.fromString("one two three");
        var small = CpuTools.session(new Limits(4, 3, 2, 100), null);
        expect(IllegalArgumentException.class, () -> small.execute(small.pipeline(List.of("text.words")), input));
        var bytes = CpuTools.session(new Limits(4, 3, 5, 2), null);
        expect(IllegalArgumentException.class, () -> bytes.execute(bytes.pipeline(List.of("text.identity")), input));
        var edges = CpuTools.session(new Limits(4, 0, 5, 100), null);
        expect(IllegalArgumentException.class, () -> edges.execute(edges.pipeline(List.of("graph.topology")),
                TextFrame.fromString("2\n0\t1\n")));
        expect(IllegalArgumentException.class, () -> small.pipeline(List.of("text.identity", "text.identity",
                "text.identity", "text.identity", "text.identity")));
        var monitor = new CountMonitor();
        var session = CpuTools.session(Limits.standard(), monitor);
        var plan = session.pipeline(List.of("text.identity", "text.sort"));
        monitor.cancelled = true;
        expect(CancellationException.class, () -> session.execute(plan, input));
        check(monitor.messages == 0, "cancel before operation");
        monitor.cancelled = false;
        monitor.cancelAfterWork = true;
        expect(CancellationException.class, () -> session.execute(plan, input));
        check(monitor.work == 1 && monitor.messages == 1, "cancel between atoms without success receipt");
        check(monitor.finished == 0, "session never completes borrowed monitor");
    }

    private static void fileAndLiteral(Path work) throws Exception {
        Path path = work.resolve("source.txt");
        Files.writeString(path, "ababa\ud83d\ude42\r\n", StandardCharsets.UTF_8);
        M3File source = M3File.load(path);
        var session = CpuTools.session();
        var result = session.execute(List.of("text.identity"), source);
        check(CpuTools.sourceReceipt(source, result).matches("[0-9a-f]{64}"), "source-bound DAG receipt");
        check(CpuTools.sourceReceipt(source, result).equals(CpuTools.sourceReceipt(source, result)), "repeat source seal");
        Path copied = work.resolve("copied.txt");
        Files.copy(path, copied);
        check(!CpuTools.sourceReceipt(source, result).equals(CpuTools.sourceReceipt(M3File.load(copied), result)),
                "source receipt binds file location as well as bytes");
        Path editTarget = work.resolve("diff-target.txt");
        Files.writeString(editTarget, "abXaba\ud83d\ude42\r\n", StandardCharsets.UTF_8);
        M3File target = M3File.load(editTarget);
        var diff = CpuTools.diff(source, target, 1);
        check(diff.complete() && diff.distance() == 1
                && diff.patch().changes().size() == 1, "bounded minimal UTF-16 file edit");
        check(diff.apply(source).equals(target.text())
                && CpuTools.verifyDiff(source, target, 1, diff, null), "source-bound minimal patch and replay");
        var indexed = CpuTools.lineDiff(source, target, 2);
        check(indexed.complete() && indexed.distance() == 2
                && indexed.patch().sourceLines() == 1 && indexed.patch().targetLines() == 1,
                "line indexed edit replaces one full source line");
        check(indexed.apply(source).equals(target.text())
                && CpuTools.verifyLineDiff(source, target, 2, indexed, null),
                "line indexed replay reconstructs exact source text");
        check(!CpuTools.verifyLineDiff(source, target, 3, indexed, null)
                && !CpuTools.verifyLineDiff(M3File.load(copied), target, 2, indexed, null),
                "line receipt seals edit budget and URI");
        var missedLine = CpuTools.lineDiff(source, target, 1);
        check(!missedLine.complete() && missedLine.patch().changes().isEmpty(),
                "line edit distance cannot claim a partial patch");
        expect(IllegalStateException.class, () -> missedLine.apply(source));
        check(!CpuTools.verifyDiff(source, target, 2, diff, null), "edit budget included in receipt");
        check(!CpuTools.verifyDiff(M3File.load(copied), target, 1, diff, null),
                "original file URI included in receipt");
        var missed = CpuTools.diff(source, target, 0);
        check(!missed.complete() && missed.distance() == -1 && missed.patch().changes().isEmpty(),
                "insufficient edit budget is explicitly incomplete");
        expect(IllegalStateException.class, () -> missed.apply(source));
        var diffMonitor = new CountMonitor();
        check(CpuTools.diff(source, target, 1, diffMonitor).sha256().equals(diff.sha256())
                && diffMonitor.work == 1 && diffMonitor.finished == 0,
                "borrowed diff monitor and stable receipt");
        diffMonitor.cancelled = true;
        expect(CancellationException.class, () -> CpuTools.diff(source, target, 1, diffMonitor));
        var indexedMonitor = new CountMonitor();
        check(CpuTools.lineDiff(source, target, 2, indexedMonitor).sha256().equals(indexed.sha256())
                && indexedMonitor.work == 1 && indexedMonitor.finished == 0,
                "line indexed progress is borrowed and deterministic");
        indexedMonitor.cancelled = true;
        expect(CancellationException.class, () -> CpuTools.lineDiff(source, target, 2, indexedMonitor));
        Path mergeOtherPath = work.resolve("merge-independent.txt");
        Files.writeString(mergeOtherPath, "#ababa\ud83d\ude42\r\n", StandardCharsets.UTF_8);
        M3File mergeOther = M3File.load(mergeOtherPath);
        var combined = CpuTools.merge(source, target, mergeOther, 1);
        check(combined.mergeable() && combined.conflictCount() == 0
                && combined.apply(source).equals("#abXaba\ud83d\ude42\r\n"),
                "two nonoverlapping file edits preserve their superset");
        check(CpuTools.verifyMerge(source, target, mergeOther, 1, combined, null),
                "three-way M3File receipt replays exact scripts");
        check(!CpuTools.verifyMerge(source, mergeOther, target, 1, combined, null)
                && !CpuTools.verifyMerge(M3File.load(copied), target, mergeOther, 1, combined, null),
                "left/right ordering and original base location are source-bound");
        Path mergeConflictPath = work.resolve("merge-conflict.txt");
        Files.writeString(mergeConflictPath, "abYaba\ud83d\ude42\r\n", StandardCharsets.UTF_8);
        var conflict = CpuTools.merge(source, target, M3File.load(mergeConflictPath), 1);
        check(!conflict.mergeable() && conflict.conflictCount() != 0,
                "identical-position divergent inserts require review");
        expect(IllegalStateException.class, () -> conflict.apply(source));
        check(!CpuTools.merge(source, target, mergeOther, 0).result().complete(),
                "budget exhaustion cannot silently choose a side");
        var mergeMonitor = new CountMonitor();
        check(CpuTools.merge(source, target, mergeOther, 1, mergeMonitor).sha256().equals(combined.sha256())
                && mergeMonitor.work == 1 && mergeMonitor.finished == 0,
                "three-way progress is borrowed and deterministic");
        mergeMonitor.cancelled = true;
        expect(CancellationException.class, () -> CpuTools.merge(source, target, mergeOther, 1, mergeMonitor));
        Path lineBasePath = work.resolve("line-base.txt");
        Path lineLeftPath = work.resolve("line-left.txt");
        Path lineRightPath = work.resolve("line-right.txt");
        Path lineConflictPath = work.resolve("line-conflict.txt");
        Files.writeString(lineBasePath, "first\r\nsecond\n", StandardCharsets.UTF_8);
        Files.writeString(lineLeftPath, "FIRST\r\nsecond\n", StandardCharsets.UTF_8);
        Files.writeString(lineRightPath, "first\r\nSECOND\n", StandardCharsets.UTF_8);
        Files.writeString(lineConflictPath, "OTHER\r\nsecond\n", StandardCharsets.UTF_8);
        M3File lineBase = M3File.load(lineBasePath), lineLeft = M3File.load(lineLeftPath),
                lineRight = M3File.load(lineRightPath);
        var lineMerged = CpuTools.lineMerge(lineBase, lineLeft, lineRight, 2);
        check(lineMerged.mergeable() && lineMerged.result().mergedChanges().size() == 2
                && lineMerged.apply(lineBase).equals("FIRST\r\nSECOND\n"),
                "line-granular independent edits preserve CRLF and LF");
        check(CpuTools.verifyLineMerge(lineBase, lineLeft, lineRight, 2, lineMerged, null)
                && !CpuTools.verifyLineMerge(lineBase, lineRight, lineLeft, 2, lineMerged, null)
                && !CpuTools.verifyLineMerge(M3File.load(copied), lineLeft, lineRight, 2, lineMerged, null),
                "ordered source identity and line budget seal three-way line receipt");
        var lineConflict = CpuTools.lineMerge(lineBase, lineLeft, M3File.load(lineConflictPath), 2);
        check(!lineConflict.mergeable() && lineConflict.conflictCount() == 1,
                "same-base-line replacements conflict explicitly");
        expect(IllegalStateException.class, () -> lineConflict.apply(lineBase));
        check(!CpuTools.lineMerge(lineBase, lineLeft, lineRight, 1).result().complete(),
                "insufficient line budget never returns guessed merge");
        var lineMergeMonitor = new CountMonitor();
        check(CpuTools.lineMerge(lineBase, lineLeft, lineRight, 2, lineMergeMonitor).sha256()
                .equals(lineMerged.sha256()) && lineMergeMonitor.work == 1
                && lineMergeMonitor.finished == 0,
                "line merge uses a borrowed monitor without completing it");
        lineMergeMonitor.cancelled = true;
        expect(CancellationException.class,
                () -> CpuTools.lineMerge(lineBase, lineLeft, lineRight, 2, lineMergeMonitor));
        Files.writeString(path, "changed on disk");
        check(session.execute(List.of("text.identity"), source).equals(result), "execution uses admitted snapshot without reopen");
        var changed = M3File.load(path);
        expect(IllegalArgumentException.class, () -> CpuTools.sourceReceipt(changed, result));
        var zeroLines = CpuTools.session(new Limits(4, 4, 0, 100), null);
        expect(IllegalArgumentException.class, () -> zeroLines.execute(List.of("text.identity"), source));
        Path mixed = work.resolve("mixed-endings.txt");
        Files.writeString(mixed, "a\r\nb\rc\n");
        M3File threeLines = M3File.load(mixed);
        var twoLines = CpuTools.session(new Limits(4, 4, 2, 100), null);
        expect(IllegalArgumentException.class, () -> twoLines.execute(List.of("text.identity"), threeLines));
        var hits = CpuTools.literal(source, "aba", 1);
        check(Arrays.equals(hits.positions(), new int[]{0}) && !hits.complete(), "overlapping capped prefix");
        int[] mutable = hits.positions();
        mutable[0] = 100;
        check(hits.positions()[0] == 0, "immutable match positions");
        check(CpuTools.verifyLiteral(source, "aba", 1, hits, null), "search re-execution receipt");
        check(!CpuTools.verifyLiteral(source, "aba", 2, hits, null), "cap is receipt-bound");
        check(!CpuTools.verifyLiteral(changed, "aba", 1, hits, null), "source is receipt-bound");
        List<String> patterns = List.of("aba", "ba", "\ud83d\ude42");
        var many = CpuTools.multiLiteral(source, patterns, 3);
        check(many.size() == 3 && !many.complete(), "multi-literal capped prefix");
        check(many.hits().get(0).equals(new com.m3.tools.text.search.MultiLiteralSearch.Hit(0, 0, 3))
                && many.hits().get(1).equals(new com.m3.tools.text.search.MultiLiteralSearch.Hit(1, 1, 3)),
                "multi-literal suffix and UTF-16 coordinates");
        check(CpuTools.verifyMultiLiteral(source, patterns, 3, many, null), "multi-literal replay");
        check(!CpuTools.verifyMultiLiteral(source, patterns, 4, many, null), "multi-literal cap binding");
        check(!CpuTools.verifyMultiLiteral(changed, patterns, 3, many, null), "multi-literal file binding");
        List<M3File> searchableFiles = List.of(source, target, changed);
        List<String> corpusPatterns = List.of("aba", "ba", "\ud83d\ude42");
        var corpus = CpuTools.corpusLiteral(searchableFiles, corpusPatterns, 100);
        check(corpus.complete() && corpus.scannedFiles() == 3 && corpus.size() > 5,
                "one prepared automaton scans all M3File text owners without joining sources");
        check(corpus.hit(0).equals(new com.m3.tools.text.search.MultiLiteralSearch.CorpusHit(0, 0, 0, 3))
                && corpus.hit(1).equals(new com.m3.tools.text.search.MultiLiteralSearch.CorpusHit(0, 1, 1, 3)),
                "global file order and local UTF-16 coordinates preserve overlapping literal order");
        check(CpuTools.verifyCorpusLiteral(searchableFiles, corpusPatterns, 100, corpus, null)
                && corpus.sha256().matches("[0-9a-f]{64}"),
                "all ordered file hashes, query identity and result coordinates reexecute");
        check(!CpuTools.verifyCorpusLiteral(List.of(target, source, changed), corpusPatterns, 100, corpus, null)
                && !CpuTools.verifyCorpusLiteral(List.of(M3File.load(copied), target, changed),
                corpusPatterns, 100, corpus, null)
                && !CpuTools.verifyCorpusLiteral(searchableFiles, List.of("ba", "aba"), 100, corpus, null),
                "file ordinals, same-byte alternate URI, query order are receipt-bound");
        var cappedCorpus = CpuTools.corpusLiteral(searchableFiles, corpusPatterns, 2);
        check(!cappedCorpus.complete() && cappedCorpus.scannedFiles() == 1
                && cappedCorpus.hits().equals(corpus.hits().subList(0, 2)),
                "one-extra-hit truncation preserves exact global search prefix");
        var emptyCorpus = CpuTools.corpusLiteral(List.of(), corpusPatterns, 0);
        check(emptyCorpus.complete() && emptyCorpus.scannedFiles() == 0 && emptyCorpus.size() == 0,
                "empty corpus has no false incomplete result");
        var corpusMonitor = new CountMonitor();
        check(CpuTools.corpusLiteral(searchableFiles, corpusPatterns, 100, corpusMonitor).sha256()
                .equals(corpus.sha256()) && corpusMonitor.work == 1 && corpusMonitor.finished == 0,
                "corpus result deterministic with borrowed progress monitor");
        corpusMonitor.cancelled = true;
        expect(CancellationException.class,
                () -> CpuTools.corpusLiteral(searchableFiles, corpusPatterns, 100, corpusMonitor));
        var grams = CpuTools.gramIndex(source, 2);
        check(grams.sha256().matches("[0-9a-f]{64}") && grams.gramSize() == 2
                && grams.windowCount() == source.length() - 1, "source-bound indexed gram receipt");
        check(Arrays.equals(grams.positions("ab"), new int[]{0, 2})
                && Arrays.equals(grams.positions("ba"), new int[]{1, 3})
                && Arrays.equals(grams.positions("\ud83d\ude42"), new int[]{5})
                && grams.frequency("ab") == 2, "physical UTF-16 gram coordinates");
        check(grams.counts().equals(com.m3.tools.text.ngram.NGrams.count(source.text(), 2)),
                "gram source oracle parity");
        int[] mutableGrams = grams.positions("ab");
        mutableGrams[0] = 999;
        check(grams.positions("ab")[0] == 0, "gram primitive positions detached");
        check(CpuTools.verifyGramIndex(source, 2, grams, null), "exact gram replay");
        check(!CpuTools.verifyGramIndex(source, 3, grams, null), "gram size receipt binding");
        check(!CpuTools.verifyGramIndex(changed, 2, grams, null), "gram file byte binding");
        check(!CpuTools.verifyGramIndex(M3File.load(copied), 2, grams, null),
                "gram file URI binding for identical bytes");
        expect(IllegalArgumentException.class, () -> CpuTools.gramIndex(source, 0));
        expect(IllegalArgumentException.class, () -> CpuTools.gramIndex(source, 65));
        expect(NullPointerException.class, () -> CpuTools.gramIndex(null, 2));
        var lexicon = CpuTools.affixIndex(source);
        check(lexicon.sha256().matches("[0-9a-f]{64}")
                && lexicon.lexiconSha256().matches("[0-9a-f]{64}"), "file-bound lexicon identity");
        check(lexicon.tokenCount() == 1 && lexicon.distinctCount() == 1
                && lexicon.wordAt(0).equals("ababa") && lexicon.contains("ababa"),
                "reuse JDK word breaking without a second payload owner");
        check(lexicon.countPrefix("aba") == 1 && lexicon.countSuffix("ba") == 1,
                "file lexicon prefix/suffix queries");
        check(lexicon.prefix("a", 0).totalMatches() == 1 && !lexicon.prefix("a", 0).complete(),
                "file lexicon capped query");
        check(CpuTools.verifyAffixIndex(source, lexicon, null), "exact lexicon replay");
        check(!CpuTools.verifyAffixIndex(changed, lexicon, null), "lexicon file byte binding");
        check(!CpuTools.verifyAffixIndex(M3File.load(copied), lexicon, null),
                "lexicon file URI binding for identical bytes");
        expect(NullPointerException.class, () -> CpuTools.affixIndex(null));
        var signal = CpuTools.signature(source, 16);
        check(signal.sha256().matches("[0-9a-f]{64}"), "signature receipt format");
        check(CpuTools.verifySignature(source, 16, signal, null), "signature exact-lane replay");
        check(!CpuTools.verifySignature(changed, 16, signal, null), "signature source binding");
        check(!CpuTools.verifySignature(source, 15, signal, null), "signature lane binding");
        long[] mutableSignature = signal.signature().minHash64();
        mutableSignature[0] ^= 1L;
        check(!Arrays.equals(signal.signature().minHash64(), mutableSignature),
                "signature primitive lanes are detached");
        List<M3File> rankedSources = List.of(source, changed);
        var ranking = CpuTools.rank(rankedSources, List.of("ababa"), 1);
        check(ranking.size() == 1 && ranking.totalMatches() == 1 && ranking.complete()
                && ranking.hit(0).documentId() == 0 && ranking.hit(0).score() > 0.0d,
                "source-bound BM25 score and document ordinal");
        check(ranking.sha256().matches("[0-9a-f]{64}"), "BM25 source receipt format");
        check(CpuTools.verifyRank(rankedSources, List.of("ababa"), 1, ranking, null),
                "BM25 exact rank and receipt replay");
        check(!CpuTools.verifyRank(List.of(changed, source), List.of("ababa"), 1, ranking, null),
                "BM25 receipt binds corpus order");
        check(!CpuTools.verifyRank(List.of(M3File.load(copied), changed), List.of("ababa"), 1, ranking, null),
                "BM25 receipt binds source URI even for byte-identical documents");
        check(!CpuTools.verifyRank(rankedSources, List.of("ababa", "ababa"), 1, ranking, null),
                "BM25 receipt binds exact query multiplicity despite set-like scoring");
        var cappedRanking = CpuTools.rank(rankedSources, List.of("ababa"), 0);
        check(cappedRanking.size() == 0 && cappedRanking.totalMatches() == 1 && !cappedRanking.complete(),
                "BM25 zero-cap completeness proof");
        expect(IllegalArgumentException.class, () -> CpuTools.rank(rankedSources, List.of("ababa"), -1));
        expect(NullPointerException.class, () -> CpuTools.rank(Arrays.asList(source, null), List.of("a"), 1));
        expect(IllegalArgumentException.class, () -> CpuTools.multiLiteral(source, List.of(""), 2));
        expect(IllegalArgumentException.class, () -> CpuTools.literal(source, "", -1));
        expect(IllegalArgumentException.class, () -> CpuTools.literal(source, "", 1_000_001));
        CountMonitor cancelled = new CountMonitor();
        cancelled.cancelled = true;
        expect(CancellationException.class, () -> CpuTools.literal(source, "aba", 2, cancelled));
        expect(CancellationException.class, () -> CpuTools.multiLiteral(source, patterns, 2, cancelled));
        expect(CancellationException.class, () -> CpuTools.signature(source, 16, cancelled));
        expect(CancellationException.class, () -> CpuTools.gramIndex(source, 2, cancelled));
        expect(CancellationException.class, () -> CpuTools.affixIndex(source, cancelled));
        expect(CancellationException.class, () -> CpuTools.rank(rankedSources, List.of("ababa"), 1, cancelled));
        cancelled.cancelled = false;
        cancelled.cancelAfterWork = true;
        expect(CancellationException.class, () -> CpuTools.literal(source, "aba", 2, cancelled));

        Random random = new Random(612721L);
        String[] alphabet = {"a", "b", "\ud83d\ude42", "\u03c0", "\r\n", "\u0000"};
        for (int round = 0; round < 240; round++) {
            StringBuilder text = new StringBuilder();
            for (int i = random.nextInt(60); i > 0; i--) text.append(alphabet[random.nextInt(alphabet.length)]);
            String spelling = text.toString();
            Files.writeString(path, spelling, StandardCharsets.UTF_8);
            M3File file = M3File.load(path);
            String needle;
            if (round % 3 == 0) needle = "";
            else if (round % 3 == 1 && !spelling.isEmpty()) {
                int start = random.nextInt(spelling.length());
                needle = spelling.substring(start, Math.min(spelling.length(), start + random.nextInt(5) + 1));
            } else needle = "aba";
            List<Integer> oracle = new ArrayList<>();
            for (int i = 0; i <= spelling.length() - needle.length(); i++) {
                boolean matches = true;
                for (int j = 0; j < needle.length(); j++) {
                    if (spelling.charAt(i + j) != needle.charAt(j)) { matches = false; break; }
                }
                if (matches) oracle.add(i);
            }
            for (int cap : new int[]{0, 1, 3, 100}) {
                var actual = CpuTools.literal(file, needle, cap);
                check(Arrays.equals(actual.positions(), oracle.stream().limit(cap).mapToInt(Integer::intValue).toArray()),
                        "independent exhaustive UTF-16 literal oracle");
                check(actual.complete() == (oracle.size() <= cap), "exact truncation bit");
                check(CpuTools.verifyLiteral(file, needle, cap, actual, null), "random source receipt replay");
            }
        }
        requireStorage(source.text());
        requireStorage(result.output().text());
        requireStorage(String.join("|", source.text(), source.text()));
    }

    private static void cli(Path work) throws Exception {
        check(invoke("--help").code == 0, "CLI help");
        check(invoke().code == 2 && invoke("--missing").code == 2, "CLI usage failure");
        var first = invoke("--list");
        check(first.code == 0 && first.output.lines().count() == 22, "CLI exact operation listing");
        check(first.equals(invoke("--list")), "repeatable CLI listing");
        check(invoke("--self-test").equals(invoke("--self-test")), "repeatable CLI self-test");
        check(invoke("--self-test").code == 0, "CLI self-test succeeds");
        Path text = work.resolve("cli.txt");
        Files.writeString(text, "b\na\nb\n");
        var run = invoke("--run", "text.sort,text.uniq", text.toString());
        check(run.code == 0 && run.output.equals("a\nb\n") && run.error.contains("receipt\tdag="), "CLI M3File pipeline");
        check(run.equals(invoke("--run", "text.sort,text.uniq", text.toString())), "CLI source receipt repeat");
        var find = invoke("--find", "b", text.toString(), "1");
        check(find.code == 0 && find.output.equals("0\n") && find.error.contains("complete=false"), "CLI explicit search truncation");
        Path patterns = work.resolve("needles.txt");
        Files.writeString(patterns, "a\nb\n");
        var findAny = invoke("--find-any", patterns.toString(), text.toString(), "2");
        check(findAny.code == 0 && findAny.output.equals("1\t0\t1\n0\t2\t3\n")
                && findAny.error.contains("complete=false") && findAny.error.contains("coordinates=UTF-16"),
                "CLI multi-literal source/needle receipts");
        check(findAny.equals(invoke("--find-any", patterns.toString(), text.toString(), "2")),
                "CLI repeated multi-literal identity");
        var signature = invoke("--signature", text.toString(), "16");
        check(signature.code == 0 && signature.output.contains("simhash64\t")
                && signature.output.lines().filter(line -> line.startsWith("minhash64\t")).count() == 16
                && signature.error.contains("approximate=true"), "CLI signature evidence");
        check(signature.equals(invoke("--signature", text.toString(), "16")), "CLI repeated signature");
        var invalid = invoke("--run", "missing", work.resolve("absent.txt").toString());
        check(invalid.code == 1 && invalid.error.contains("unknown binding"), "binding refusal precedes file read");
        Files.writeString(text, "1\n0\t1\n");
        var invalidGraph = invoke("--run", "graph.topology", text.toString());
        check(invalidGraph.code == 1 && invalidGraph.output.isEmpty()
                && invalidGraph.error.contains("CPU_TOOLS_REFUSED"), "invalid graph is a CLI refusal, not an uncaught exception");
        Path java = work.resolve("CliSource.java");
        Files.writeString(java, "class CliSource { int value; int sum(int a) { return a + value; } }\n");
        var ast = invoke("--ast", java.toString());
        check(ast.code == 0 && ast.output.contains("method\tsum\t1\tfalse")
                && ast.error.contains("receipt\tast=") && ast.error.contains("phase=PARSED"), "CLI real M3AST syntax inventory");
        check(ast.equals(invoke("--ast", java.toString())), "CLI AST source and fact receipt repeat");
        check(!Files.exists(work.resolve("CliSource.class")), "CLI parse emits no classes");
    }

    private record CliResult(int code, String output, String error) { }
    private static CliResult invoke(String... args) {
        var output = new ByteArrayOutputStream();
        var error = new ByteArrayOutputStream();
        int code;
        try (var out = new PrintStream(output, true, StandardCharsets.UTF_8);
             var err = new PrintStream(error, true, StandardCharsets.UTF_8)) {
            code = CpuToolsCli.run(args, out, err);
        }
        return new CliResult(code, output.toString(StandardCharsets.UTF_8), error.toString(StandardCharsets.UTF_8));
    }

    private static void requireStorage(String text) throws Exception {
        if (!Boolean.getBoolean("m3.test.requireStorage")) return;
        var field = String.class.getDeclaredField("m3");
        field.setAccessible(true);
        check(!text.isEmpty() && field.get(text) != null, "actual M3String storage, not only API presence");
    }

    private static void expectText(String input, List<String> operations, String expected) {
        var session = CpuTools.session();
        var plan = session.pipeline(operations);
        var frame = TextFrame.fromString(input);
        var result = session.execute(plan, frame);
        check(result.output().text().equals(expected), operations + " golden output; actual=" + CpuTools.escape(result.output().text()));
        check(session.replay(plan, frame, result).equals(result), "operator output and receipt replay " + operations);
    }

    private static void refuseOperation(String operation, String text) {
        var session = CpuTools.session();
        expect(IllegalArgumentException.class, () -> session.execute(session.pipeline(List.of(operation)), TextFrame.fromString(text)));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void expect(Class<? extends Throwable> type, Checked operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName() + " but got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static final class CountMonitor implements ProgressMonitor {
        boolean cancelled;
        boolean cancelAfterWork;
        int messages;
        long work;
        int finished;
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void message(String ignored) { messages++; }
        @Override public void worked(long amount) { work += amount; if (cancelAfterWork) cancelled = true; }
        @Override public void done() { finished++; }
    }
}
