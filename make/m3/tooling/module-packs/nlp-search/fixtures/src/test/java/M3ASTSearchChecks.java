/* Copyright 2026 Hitesh Soliwal and Contributors. SPDX-License-Identifier: Apache-2.0 */
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.search.M3File;
import jdk.m3.search.M3FileSourceFacts;
import jdk.m3.search.M3Search;
import jdk.m3.search.compiler.M3ASTSearch;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

/** Real javac/M3AST and Lucene integration; no alternate parser or test support stubs. */
public final class M3ASTSearchChecks {
    private static final String SOURCE = "/* 😀 class Pretend { } */\n"
            + "class Demo {\n"
            + "  static final int BASE = 3;\n"
            + "  String words = \"\"\"\n"
            + "    class TextGhost { int buried; }\n"
            + "    \"\"\";\n"
            + "  int add(int left, int right) {\n"
            + "    int sum = left + right;\n"
            + "    return sum + BASE;\n"
            + "  }\n"
            + "  static class Inner { long value; }\n"
            + "}\n";
    private static final String CONTEXT_A = M3FileSourceFacts.contextSha256(
            List.of("M3ASTSearchChecks", "syntax namespace A", "Java 21"));
    private static final String CONTEXT_B = M3FileSourceFacts.contextSha256(
            List.of("M3ASTSearchChecks", "syntax namespace B", "Java 21"));
    private static int checks;
    private static Path work;

    private M3ASTSearchChecks() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("one fresh work directory required");
        work = Files.createDirectory(Path.of(args[0]).toAbsolutePath());
        Path disk = work.resolve("Demo.java");
        byte[] originalBytes = SOURCE.getBytes(StandardCharsets.UTF_8);
        Files.write(disk, originalBytes);
        M3File frozen = M3File.read(disk, StandardCharsets.UTF_8, originalBytes.length);
        // A subsequent disk edit must not change the already admitted compiler input or byte evidence.
        Files.writeString(disk, "class ChangedOnDisk {}", StandardCharsets.UTF_8);
        check(frozen.source().equals(SOURCE), "source remains frozen after disk change");
        check(frozen.byteSha256().equals(hexSha(originalBytes)), "original byte digest independently verified");
        check(frozen.getCharContent(false) == frozen.source(), "compiler receives the frozen String owner");

        try (var directory = FSDirectory.open(work.resolve("index"));
             var analyzer = new StandardAnalyzer();
             var writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            snapshotAndClassfileParity(frozen, writer);
            replacementAndContextIsolation(frozen, writer);
            refusalBeforeReplacement(frozen, writer);
            writer.commit();
        }
        try (var directory = FSDirectory.open(work.resolve("index"));
             var reader = DirectoryReader.open(directory)) {
            check(reader.numDocs() == 16, "committed context/source groups reopen intact");
        }
        System.out.println("M3_AST_SEARCH_PASS checks=" + checks
                + " realCompiler=true exactDeclarationOracle=true byteEvidence=true"
                + " classfileParity=true atomicGroupRefusals=true");
    }

    private static void snapshotAndClassfileParity(M3File frozen, IndexWriter writer) throws Exception {
        M3ASTSearch projector = new M3ASTSearch(CONTEXT_A, 10000, List.of(frozen), null);
        AtomicReference<M3ASTSearch.Snapshot> retained = new AtomicReference<>();
        AtomicReference<M3AST.Unit> escaped = new AtomicReference<>();
        M3AST.Consumer first = new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                M3ASTSearch.Snapshot snapshot = projector.project(unit);
                retained.set(snapshot);
                escaped.set(unit);
                check(snapshot.source() == frozen, "frozen original file identity preserved");
                check(snapshot.source().source() == unit.source().text(), "same exact parser String");
                check(snapshot.facts().phase().equals("PARSED"), "syntax phase is explicit");
                check(snapshot.facts().providerId().equals("jdk.m3.search.compiler"), "projection provider");
                check(snapshot.facts().schemaVersion() == 1, "projection schema");
                check(snapshot.facts().contextSha256().equals(CONTEXT_A), "context evidence preserved");
                check(snapshot.javacUtf16Sha256().equals(rawUtf16Sha(SOURCE)), "javac raw UTF-16BE digest");
                check(!snapshot.javacUtf16Sha256().equals(snapshot.facts().sourceSha256()),
                        "raw javac digest differs from domain-separated source identity");
            }
        };
        M3AST.Consumer second = new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                check(projector.project(unit) == retained.get(), "one memoized projection in the PARSED event");
            }
        };
        M3AST.Summary summary = compile(frozen, "instrumented", List.of(first, second, projector.consumer(writer)));
        check(summary.projectionComputations() == 1 && summary.projectionHits() == 2,
                "two consumers and writer reuse one event-local computation");
        check(summary.parsedUnits() == 1 && summary.capturedSources() == 1, "single actual parse/source capture");
        check(summary.closed() && summary.completed() && !summary.consumerFailed(), "successful owner cleanup");
        M3ASTSearch.Snapshot snapshot = retained.get();
        check(snapshot != null, "snapshot delivered");
        expect(IllegalStateException.class, () -> escaped.get().tree());
        expect(IllegalStateException.class, () -> projector.project(escaped.get()));
        exactDeclarations(snapshot);
        check(snapshot.source().source().equals(SOURCE), "immutable source usable after compiler callback");
        check(snapshot.source().byteSha256().equals(frozen.byteSha256()), "original byte evidence outlives callback");
        check(group(writer, frozen, CONTEXT_A).size() == snapshot.size() + 1, "one source row plus declaration rows");
        check(count(writer, and(groupQuery(frozen, CONTEXT_A), new TermQuery(new Term(M3Search.TEXT, "textghost")))) == 1,
                "full text is indexed in the single source row");
        check(count(writer, and(groupQuery(frozen, CONTEXT_A), new TermQuery(new Term(M3ASTSearch.NAME, "TextGhost")))) == 0,
                "text-block fake declaration is not an AST declaration");

        compile(frozen, "uninstrumented", List.of());
        Map<String, byte[]> before = classfiles(work.resolve("uninstrumented"));
        Map<String, byte[]> after = classfiles(work.resolve("instrumented"));
        check(before.keySet().equals(Set.of("Demo.class", "Demo$Inner.class")), "ordinary expected class set");
        check(before.keySet().equals(after.keySet()), "instrumentation preserves emitted class set");
        for (String name : before.keySet()) {
            check(Arrays.equals(before.get(name), after.get(name)), "ordinary class bytes unchanged: " + name);
        }
    }

    private record Expected(String kind, int start, int end) { }

    /** Explicit source coordinates, not another invocation of the projector being tested. */
    private static void exactDeclarations(M3ASTSearch.Snapshot snapshot) {
        Map<String, Expected> expected = new HashMap<>();
        int demo = SOURCE.indexOf("class Demo");
        int inner = SOURCE.indexOf("static class Inner");
        int method = SOURCE.indexOf("int add(");
        expected.put("Demo", new Expected("CLASS", demo, SOURCE.lastIndexOf('}') + 1));
        expected.put("Inner", new Expected("CLASS", inner, SOURCE.indexOf('}', inner) + 1));
        expected.put("add", new Expected("METHOD", method, SOURCE.indexOf("  }\n", method) + 3));
        expected.put("BASE", range("static final int BASE = 3;"));
        expected.put("words", new Expected("VARIABLE", SOURCE.indexOf("String words"),
                SOURCE.indexOf("\"\"\";", SOURCE.indexOf("String words")) + 4));
        expected.put("left", range("int left"));
        expected.put("right", range("int right"));
        expected.put("sum", range("int sum = left + right;"));
        expected.put("value", range("long value;"));
        check(snapshot.size() == expected.size(), "exact real declaration count excludes comments and text blocks");
        check(snapshot.visitedNodes() > snapshot.size() && snapshot.visitedNodes() < 10000,
                "bounded traversal includes non-declaration syntax");
        for (int i = 0; i < snapshot.size(); i++) {
            Expected declaration = expected.remove(snapshot.name(i));
            check(declaration != null, "known unique declaration: " + snapshot.name(i));
            check(snapshot.kind(i).equals(declaration.kind()), "declaration kind: " + snapshot.name(i));
            check(snapshot.start(i) == declaration.start() && snapshot.end(i) == declaration.end(),
                    "exact UTF-16 range: " + snapshot.name(i));
        }
        check(expected.isEmpty(), "every explicitly expected declaration found");
        check(demo != SOURCE.codePointCount(0, demo), "supplementary prefix distinguishes UTF-16 from code-point offsets");
        expect(IllegalArgumentException.class, () -> new M3ASTSearch(CONTEXT_A, 0));
        expect(IndexOutOfBoundsException.class, () -> snapshot.start(-1));
        expect(IndexOutOfBoundsException.class, () -> snapshot.end(snapshot.size()));
        var iterator = snapshot.documents().iterator();
        int rows = 0;
        while (iterator.hasNext()) {
            Document document = iterator.next();
            check(document.get(M3Search.BYTE_SHA256).equals(snapshot.source().byteSha256()), "byte digest in every row");
            check(document.get(M3ASTSearch.JAVAC_SHA256).equals(snapshot.javacUtf16Sha256()), "javac digest in every row");
            rows++;
        }
        check(rows == snapshot.size() + 1, "immutable snapshot documents remain iterable after compilation");
        expect(java.util.NoSuchElementException.class, iterator::next);
    }

    private static Expected range(String text) {
        int start = SOURCE.indexOf(text);
        if (start < 0) throw new AssertionError("invalid explicit fixture range: " + text);
        return new Expected("VARIABLE", start, start + text.length());
    }

    private static void replacementAndContextIsolation(M3File frozen, IndexWriter writer) throws Exception {
        List<String> originalA = group(writer, frozen, CONTEXT_A);
        compile(frozen, "second-context", List.of(new M3ASTSearch(CONTEXT_B, 10000, List.of(frozen), null).consumer(writer)));
        check(group(writer, frozen, CONTEXT_A).equals(originalA), "same source in another context preserves first context");
        check(group(writer, frozen, CONTEXT_B).size() == 10, "separate context has complete original group");
        M3File other = M3File.of(work.resolve("Other.java").toUri(), "class Other { int item; }");
        compile(other, "other-source", List.of(new M3ASTSearch(CONTEXT_A, 10000, List.of(other), null).consumer(writer)));
        List<String> otherGroup = group(writer, other, CONTEXT_A);
        check(otherGroup.size() == 3, "different source has one source row and two declarations");
        M3File replacement = M3File.of(frozen.toUri(), "class Demo { int replacement; }");
        compile(replacement, "replacement", List.of(new M3ASTSearch(CONTEXT_A, 10000, List.of(replacement), null).consumer(writer)));
        check(group(writer, replacement, CONTEXT_A).size() == 3, "whole old source group replaced, not appended");
        check(group(writer, frozen, CONTEXT_B).size() == 10, "replacement does not delete another context");
        check(group(writer, other, CONTEXT_A).equals(otherGroup), "replacement does not delete another source");
        check(count(writer, and(groupQuery(replacement, CONTEXT_A), new TermQuery(new Term(M3ASTSearch.NAME, "BASE")))) == 0,
                "old declaration rows removed from replaced group");
        check(count(writer, and(groupQuery(frozen, CONTEXT_B), new TermQuery(new Term(M3ASTSearch.NAME, "BASE")))) == 1,
                "other context retains the old declaration");
        check(count(writer, and(groupQuery(replacement, CONTEXT_A), new TermQuery(new Term(M3ASTSearch.NAME, "replacement")))) == 1,
                "new declaration row installed");
    }

    private static void refusalBeforeReplacement(M3File original, IndexWriter writer) throws Exception {
        List<String> before = group(writer, original, CONTEXT_A);
        M3File proposed = M3File.of(original.toUri(), "class Demo { int shouldNeverReplace; }");
        expect(IllegalArgumentException.class, () -> new M3ASTSearch(CONTEXT_A, 1000, List.of(proposed, proposed), null));

        M3ASTSearch budget = new M3ASTSearch(CONTEXT_A, 1, List.of(proposed), null);
        expect(IllegalArgumentException.class, () -> compile(proposed, "refused-budget", List.of(budget.consumer(writer))));
        check(group(writer, original, CONTEXT_A).equals(before), "node-budget refusal leaves existing group intact");

        AtomicInteger checkpoints = new AtomicInteger();
        CancellationException cancellation = new CancellationException("host cancelled bounded AST traversal");
        M3ASTSearch cancelled = new M3ASTSearch(CONTEXT_A, 1000, List.of(proposed), () -> {
            if (checkpoints.incrementAndGet() == 3) throw cancellation;
        });
        check(expect(CancellationException.class, () -> compile(proposed, "refused-cancel", List.of(cancelled.consumer(writer))))
                == cancellation, "host cancellation retains its exception identity through javac");
        check(checkpoints.get() == 3, "cancellation happened during traversal");
        check(group(writer, original, CONTEXT_A).equals(before), "mid-capture cancellation leaves existing group intact");

        AtomicBoolean late = new AtomicBoolean();
        CancellationException lateCancellation = new CancellationException("cancelled after projection");
        M3ASTSearch cached = new M3ASTSearch(CONTEXT_A, 1000, List.of(proposed), () -> {
            if (late.get()) throw lateCancellation;
        });
        M3AST.Consumer prepare = new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                check(cached.project(unit).size() == 2, "capture succeeded before late cancellation");
                late.set(true);
            }
        };
        check(expect(CancellationException.class, () -> compile(proposed, "refused-late-cancel",
                List.of(prepare, cached.consumer(writer)))) == lateCancellation,
                "memoized projection still checks cancellation immediately before writer update");
        check(group(writer, original, CONTEXT_A).equals(before), "late cancellation leaves existing group intact");

        M3File falselyBound = M3File.of(proposed.toUri(), new String(proposed.source()));
        check(falselyBound.source().equals(proposed.source()) && falselyBound.source() != proposed.source(),
                "equal characters have distinct frozen owners in mismatch fixture");
        M3ASTSearch mismatch = new M3ASTSearch(CONTEXT_A, 1000, List.of(falselyBound), null);
        expect(IllegalArgumentException.class, () -> compile(proposed, "refused-source-owner", List.of(mismatch.consumer(writer))));
        check(group(writer, original, CONTEXT_A).equals(before), "different frozen source owner cannot assert original byte identity");
    }

    /** Empty consumers uses an ordinary, uninstrumented task for classfile comparison. */
    private static M3AST.Summary compile(M3File source, String output, List<M3AST.Consumer> consumers) throws Exception {
        Path classes = Files.createDirectory(work.resolve(output));
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "actual compiler available");
        try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("-proc:none", "-g:none", "-encoding", "UTF-8", "-d", classes.toString()),
                    null, List.of(source));
            M3AST session = consumers.isEmpty() ? null : M3AST.open(task);
            if (session != null) for (M3AST.Consumer consumer : consumers) session.addConsumer(consumer);
            try {
                check(task.call(), "normal javac compilation succeeds: " + diagnostics.getDiagnostics());
                check(diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR),
                        "no compiler error diagnostics");
                return session == null ? null : session.summary();
            } finally {
                if (session != null) session.close();
            }
        }
    }

    private static Map<String, byte[]> classfiles(Path directory) throws IOException {
        Map<String, byte[]> result = new TreeMap<>();
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                result.put(directory.relativize(path).toString(), Files.readAllBytes(path));
            }
        }
        return result;
    }

    private static Query groupQuery(M3File source, String context) {
        return and(new TermQuery(new Term(M3Search.URI, source.toUri().toASCIIString())),
                new TermQuery(new Term(M3Search.CONTEXT, context)));
    }

    private static Query and(Query left, Query right) {
        return new BooleanQuery.Builder().add(left, BooleanClause.Occur.FILTER)
                .add(right, BooleanClause.Occur.FILTER).build();
    }

    private static int count(IndexWriter writer, Query query) throws IOException {
        try (var reader = DirectoryReader.open(writer)) {
            return new IndexSearcher(reader).count(query);
        }
    }

    private static List<String> group(IndexWriter writer, M3File source, String context) throws IOException {
        try (var reader = DirectoryReader.open(writer)) {
            var searcher = new IndexSearcher(reader);
            var matches = searcher.search(groupQuery(source, context), 100);
            List<String> result = new ArrayList<>();
            for (var hit : matches.scoreDocs) {
                Document row = reader.storedFields().document(hit.doc);
                result.add(row.get(M3Search.KIND) + ":" + row.get(M3ASTSearch.NAME) + ":"
                        + row.get(M3Search.SOURCE_SHA256) + ":" + row.get(M3Search.BYTE_SHA256)
                        + ":" + row.get(M3ASTSearch.JAVAC_SHA256));
            }
            result.sort(String::compareTo);
            return result;
        }
    }

    private static String hexSha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String rawUtf16Sha(String source) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < source.length(); i++) {
            digest.update((byte) (source.charAt(i) >>> 8));
            digest.update((byte) source.charAt(i));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    @FunctionalInterface private interface Action { void run() throws Exception; }

    /** javac preserves consumer failures as causes of its normal callback wrapper. */
    private static <T extends Throwable> T expect(Class<T> type, Action action) {
        try {
            action.run();
        } catch (Throwable failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (type.isInstance(cause)) {
                    checks++;
                    return type.cast(cause);
                }
            }
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
