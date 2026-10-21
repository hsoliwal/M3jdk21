/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.m3jdk.tools.search.probe;

import com.m3.file.M3File;
import com.m3.file.M3FileDictionary;
import com.m3.file.M3FileText;
import com.m3.file.M3FileTokenizer;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.Trees;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import opennlp.tools.tokenize.M3Tokenizers;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.m3jdk.tools.search.M3SourceIndex;

/** Exercises the real named modules, compiler callbacks and persisted Lucene index. */
public final class M3SourceIndexProbe {
    private static long assertions;
    private static long tokenPostings;
    private static long expiredAccesses;

    private M3SourceIndexProbe() {}

    @FunctionalInterface private interface Work { void run() throws Exception; }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static Throwable expect(Class<? extends Throwable> type, Work work, String message)
            throws Exception {
        try { work.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), message + ": " + failure);
            return failure;
        }
        throw new AssertionError(message + ": no exception");
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }

    private static String utf16Hash(CharSequence text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            digest.update((byte) (value >>> 8));
            digest.update((byte) value);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void sameChars(CharSequence actual, String expected, String message) {
        check(actual.length() == expected.length(), message + " length");
        for (int i = 0; i < expected.length(); i++) {
            check(actual.charAt(i) == expected.charAt(i), message + " char " + i);
        }
    }

    private static String read(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[7];
        for (int count; (count = reader.read(buffer)) != -1;) result.append(buffer, 0, count);
        return result.toString();
    }

    private static void moduleAndOwnershipContracts() {
        check(M3SourceIndex.class.getModule().getName().equals("org.m3jdk.tools.search"), "frontend named module");
        check(M3File.class.getModule().getName().equals("org.m3jdk.file"), "complete file module");
        check(M3AST.class.getModule().getName().equals("jdk.compiler"), "actual compiler owner");
        check(Analyzer.class.getModule().getName().equals("org.apache.lucene.core"), "actual Lucene module");
        check(M3Tokenizers.class.getModule().getName().equals("org.apache.opennlp.tools"), "actual OpenNLP module");
        for (java.lang.reflect.Field field : M3SourceIndex.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            Class<?> type = field.getType();
            check(!M3AST.Unit.class.isAssignableFrom(type)
                    && !Tree.class.isAssignableFrom(type)
                    && !Trees.class.isAssignableFrom(type)
                    && !Element.class.isAssignableFrom(type)
                    && !JavacTask.class.isAssignableFrom(type)
                    && !JavaCompiler.class.isAssignableFrom(type), "no compiler retention: " + field.getName());
        }
        for (java.lang.reflect.Field field : M3FileText.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            check(field.getType() != String.class && field.getType() != char[].class
                    && field.getType() != byte[].class, "file view has no character payload: " + field.getName());
        }
    }

    private static final class Observer implements M3AST.Consumer {
        private final Map<URI, String> expected;
        // Deliberately retained only by this negative lifetime fixture, never by production code.
        private final List<M3AST.Unit> expired = new ArrayList<>();
        private final M3AST.Projection<String> projection =
                new M3AST.Projection<>("fixture.source", unit -> unit.source().utf16Sha256());
        private final M3SourceIndex consumer;
        private int parsed;
        private int analyzed;
        private int finished;

        Observer(Map<URI, String> expected, M3SourceIndex consumer) {
            this.expected = expected;
            this.consumer = consumer;
        }

        @Override public void parsed(M3AST.Unit unit) throws Exception {
            parsed++;
            check(unit.phase() == M3AST.Phase.PARSED, "parsed phase");
            check(unit.analyzedType() == null, "no attributed type at parse");
            check(unit.tree() != null && unit.trees() != null, "real compiler tree and Trees");
            String original = expected.get(unit.source().uri());
            check(original != null, "source URI bound to compiler input");
            sameChars(unit.source().text(), original, "compiler snapshot");
            check(unit.source().utf16Sha256().equals(utf16Hash(original)), "exact raw UTF-16 compiler digest");
            check(unit.project(projection).equals(unit.project(projection)), "projection memoized in same event");
            AtomicReference<Throwable> foreign = new AtomicReference<>();
            Thread other = new Thread(() -> {
                try { unit.tree(); }
                catch (Throwable failure) { foreign.set(failure); }
            }, "M3SourceIndexProbe-foreign-callback");
            other.start();
            other.join();
            check(foreign.get() instanceof IllegalStateException, "live callback rejects foreign thread");
            expired.add(unit);
        }

        @Override public void analyzed(M3AST.Unit unit) throws Exception {
            analyzed++;
            check(unit.phase() == M3AST.Phase.ANALYZED && unit.analyzedType() != null, "actual type-analysis event");
            expect(IllegalArgumentException.class, () -> consumer.parsed(unit), "parse-only consumer rejects analyzed event");
            expired.add(unit);
        }

        @Override public void finished(M3AST.Summary summary) {
            finished++;
            check(summary.closed(), "finished after owner close");
        }

        void assertExpired() throws Exception {
            for (M3AST.Unit unit : expired) {
                List<Work> accesses = List.of(unit::phase, unit::tree, unit::trees,
                        unit::analyzedType, unit::contextIdentity, unit::source,
                        () -> unit.project(projection), () -> consumer.parsed(unit));
                for (Work access : accesses) {
                    expect(IllegalStateException.class, access, "expired callback access");
                    expiredAccesses++;
                }
            }
            expired.clear();
        }
    }

    private static int count(IndexSearcher searcher, Query query) throws IOException {
        return searcher.count(query);
    }

    private static Document unique(IndexSearcher searcher, URI uri) throws IOException {
        var hits = searcher.search(new TermQuery(new Term(M3SourceIndex.URI_FIELD, uri.toString())), 2);
        check(hits.totalHits.value == 1, "one current document per source URI");
        return searcher.storedFields().document(hits.scoreDocs[0].doc);
    }

    private record Position(int ordinal, int start, int end) {}

    private static void verifyTokenCoordinates(DirectoryReader reader, URI uri, String original)
            throws Exception {
        IndexSearcher searcher = new IndexSearcher(reader);
        int document = searcher.search(new TermQuery(new Term(M3SourceIndex.URI_FIELD, uri.toString())), 1).scoreDocs[0].doc;
        Span[] oracle = M3Tokenizers.simpleSpans(original);
        Map<BytesRef, List<Position>> expected = new TreeMap<>();
        for (int i = 0; i < oracle.length; i++) {
            Span span = oracle[i];
            BytesRef term = new BytesRef(original.substring(span.getStart(), span.getEnd()));
            expected.computeIfAbsent(term, ignored -> new ArrayList<>())
                    .add(new Position(i, span.getStart(), span.getEnd()));
        }
        Terms terms = MultiTerms.getTerms(reader, M3SourceIndex.TOKEN_FIELD);
        check(terms != null && terms.hasPositions() && terms.hasOffsets(), "stored OpenNLP positions and UTF-16 offsets");
        TermsEnum iterator = terms.iterator();
        for (Map.Entry<BytesRef, List<Position>> entry : expected.entrySet()) {
            check(iterator.seekExact(entry.getKey()), "OpenNLP term exists");
            PostingsEnum postings = iterator.postings(null, PostingsEnum.OFFSETS);
            check(postings.advance(document) == document, "term belongs to expected source");
            check(postings.freq() == entry.getValue().size(), "term frequency");
            for (Position position : entry.getValue()) {
                check(postings.nextPosition() == position.ordinal(), "token ordinal");
                check(postings.startOffset() == position.start(), "token UTF-16 start");
                check(postings.endOffset() == position.end(), "token UTF-16 end");
                tokenPostings++;
            }
        }
    }

    private static void frontendAndIndex(Path root) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "system compiler available");
        M3FileDictionary dictionary = new M3FileDictionary();
        String original = "package demo;\r\n/** Source \ud83d\ude00 */\r\n"
                + "class Example { String label = \"Atlas Beta café \ud801\udc00\"; }\r\n"
                + "class Sidekick { int amount = 42; }\n";
        Path physical = root.resolve("Example.java");
        Files.writeString(physical, original, StandardCharsets.UTF_8);
        JavaFileObject snapshot = M3SourceIndex.source(physical, dictionary, 0);
        check(snapshot.getCharContent(false) instanceof M3FileText, "physical ingress exposes complete M3File text");
        sameChars(snapshot.getCharContent(false), original, "M3File UTF-8 ingress");
        try (Reader reader = snapshot.openReader(false)) {
            check(read(reader).equals(original), "Lucene reader over M3File source");
        }
        Files.writeString(physical, "class ChangedOnDisk {}\n", StandardCharsets.UTF_8);
        sameChars(snapshot.getCharContent(false), original, "snapshot independent of later disk change");

        String malformed = "package demo; // isolated " + '\ud800' + " / " + '\udfff'
                + " and paired \ud83d\ude80\nclass Raw { String tag = \"RawMarker\"; }\n";
        URI memoryUri = URI.create("memory:///Raw.java");
        M3File memory = M3FileTokenizer.codeLike(dictionary, 0, malformed);
        JavaFileObject raw = M3SourceIndex.source(memoryUri, memory);
        sameChars(raw.getCharContent(false), malformed, "M3File malformed UTF-16 snapshot");
        check(!utf16Hash(malformed).equals(utf16Hash(malformed.replace('\ud800', '\ufffd'))), "surrogate replacement changes identity");
        Map<URI, String> originals = new HashMap<>();
        originals.put(snapshot.toUri(), original);
        originals.put(memoryUri, malformed);

        Path index = root.resolve("index");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
             Analyzer analyzer = M3SourceIndex.analyzer();
             FSDirectory directory = FSDirectory.open(index);
             IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer).setCommitOnClose(false))) {
            AtomicInteger polls = new AtomicInteger();
            M3SourceIndex first = new M3SourceIndex(writer, dictionary, 0, polls::incrementAndGet);
            M3SourceIndex second = new M3SourceIndex(writer, dictionary, 0);
            Observer observer = new Observer(originals, first);
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics, List.of("-proc:none"), null, List.of(snapshot, raw));
            M3AST ast = M3AST.open(task, true);
            try (ast) {
                ast.addConsumer(first);
                ast.addConsumer(second);
                ast.addConsumer(observer);
                task.parse();
                task.analyze();
                check(first.statistics().indexedSources() == 2 && second.statistics().indexedSources() == 2,
                        "independent consumer instances index each parse once");
                check(observer.parsed == 2 && observer.analyzed == 3, "two sources and three actual top-level analysis events");
                M3AST.Summary summary = ast.summary();
                check(summary.parsedUnits() == 2 && summary.analyzedTypes() == 3, "compiler event counters");
                check(summary.capturedSources() == 2 && summary.sourceReads() == 0, "exact sealed parse source reused");
                check(summary.projectionComputations() == 6 && summary.projectionHits() == 2,
                        "per-instance projections do not share equal names; explicit projection does memoize");
                observer.assertExpired();
                check(!DirectoryReader.indexExists(directory), "consumer has not published a commit");
            }
            check(observer.finished == 1 && ast.summary().closed() && !ast.summary().consumerFailed(), "normal owner close");
            check(!ast.summary().completed(), "parse/analyze does not claim completed code generation");
            check(diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR), "sources analyze without errors");
            check(polls.get() > 2, "cooperative progress checkpoints");
            check(first.statistics().indexedTokens() == M3Tokenizers.simpleSpans(original).length
                    + M3Tokenizers.simpleSpans(malformed).length, "OpenNLP token counts");

            Document marker = new Document();
            marker.add(new StringField("caller", "owned-writer", Field.Store.YES));
            writer.addDocument(marker);
            writer.commit();
            check(DirectoryReader.indexExists(directory), "caller can use and commit writer after session closes");

            JavaFileObject cancelledInput = M3SourceIndex.source(URI.create("memory:///Cancelled.java"),
                    M3FileTokenizer.codeLike(dictionary, 0, "class Cancelled {}"));
            M3SourceIndex cancelled = new M3SourceIndex(writer, dictionary, 0,
                    () -> { throw new CancellationException("fixture cancellation"); });
            JavacTask cancelledTask = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null, List.of(cancelledInput));
            M3AST cancelledAst = M3AST.open(cancelledTask, true);
            try (cancelledAst) {
                cancelledAst.addConsumer(cancelled);
                Throwable failure = expect(RuntimeException.class, cancelledTask::parse, "compiler transports cancellation");
                check(hasCause(failure, CancellationException.class), "original cancellation preserved");
                check(cancelledAst.summary().consumerFailed() && cancelledAst.summary().closed(), "compiler releases failed consumer session");
                check(cancelled.statistics().indexedSources() == 0, "cancelled operation did not index");
            }
            writer.commit();
            check(writer.getDocStats().numDocs == 3, "cancellation leaves caller writer usable and existing documents intact");
        }

        try (Analyzer analyzer = M3SourceIndex.analyzer(); FSDirectory directory = FSDirectory.open(index);
             DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            check(reader.numDocs() == 3, "persisted two sources plus caller document");
            check(count(searcher, M3SourceIndex.textQuery("atlas", analyzer)) == 1, "actual StandardAnalyzer and query parser search");
            check(count(searcher, M3SourceIndex.textQuery("ChangedOnDisk", analyzer)) == 0, "index is bound to parsed snapshot");
            check(count(searcher, M3SourceIndex.tokenPhrase("Atlas Beta")) == 1, "OpenNLP phrase positions");
            check(count(searcher, M3SourceIndex.tokenPhrase("atlas Beta")) == 0, "OpenNLP token search remains case sensitive");
            check(count(searcher, M3SourceIndex.tokenPhrase("")) == 0, "empty phrase has no hits");
            Borrowed query = new Borrowed("Atlas Beta");
            check(count(searcher, M3SourceIndex.tokenPhrase(query)) == 1 && query.reads > 0, "borrowed query never materializes source String");
            for (Map.Entry<URI, String> entry : originals.entrySet()) {
                Document document = unique(searcher, entry.getKey());
                check(document.get(M3SourceIndex.HASH_FIELD).equals(utf16Hash(entry.getValue())), "persisted exact compiler UTF-16 digest");
                check(document.get(M3SourceIndex.PACKAGE_FIELD).equals("demo"), "package read from actual compiler AST");
                check(document.getField("utf16Length").numericValue().intValue() == entry.getValue().length(), "persisted UTF-16 length");
                check(document.getField("tokenCount").numericValue().intValue() == M3Tokenizers.simpleSpans(entry.getValue()).length, "persisted span count");
                check(document.get(M3SourceIndex.TEXT_FIELD) == null && document.get(M3SourceIndex.TOKEN_FIELD) == null,
                        "index retains term formats, no second stored full-source payload");
                verifyTokenCoordinates(reader, entry.getKey(), entry.getValue());
            }
            Document main = unique(searcher, snapshot.toUri());
            check(Arrays.equals(main.getValues(M3SourceIndex.TYPE_FIELD), new String[]{"demo.Example", "demo.Sidekick"}), "top-level type metadata");
        }
        try {
            Thread.currentThread().interrupt();
            expect(CancellationException.class, () -> M3SourceIndex.tokenPhrase("Atlas"), "interrupted query");
            check(Thread.currentThread().isInterrupted(), "interruption flag retained");
        } finally { Thread.interrupted(); }
    }

    private static final class Borrowed implements CharSequence {
        private final String backing;
        private int reads;
        Borrowed(String backing) { this.backing = backing; }
        @Override public int length() { return backing.length(); }
        @Override public char charAt(int index) { reads++; return backing.charAt(index); }
        @Override public CharSequence subSequence(int start, int end) { throw new AssertionError("query subSequence"); }
        @Override public String toString() { throw new AssertionError("query toString"); }
    }

    private static String cli(String... args) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            M3SourceIndex.main(args);
        } finally { System.setOut(original); }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void cliCommits(Path root) throws Exception {
        Path file = root.resolve("CliInput.java");
        Path index = root.resolve("cli-index");
        String before = "class CliInput { String text = \"BeforeMarker\"; }\n";
        String after = "class CliInput { String text = \"ReleaseMarker\"; }\n";
        Files.writeString(file, before, StandardCharsets.UTF_8);
        check(cli("index", index.toString(), file.toString()).startsWith("M3_SOURCE_INDEX sources=1 tokens="), "CLI successful parse publishes explicit commit");
        check(cli("search", index.toString(), "beforemarker").contains(file.toUri().toString()), "CLI searches reopened disk index");
        Files.writeString(file, after, StandardCharsets.UTF_8);
        cli("index", index.toString(), file.toString());
        check(cli("search", index.toString(), "BeforeMarker").isEmpty(), "URI update removes old terms");
        String hits = cli("tokens", index.toString(), "ReleaseMarker");
        check(hits.contains(file.toUri().toString()) && hits.contains(utf16Hash(after)), "CLI exact OpenNLP query finds updated source and digest");
        Files.writeString(file, "class CliInput { // UnpublishedOmega\n void broken( }", StandardCharsets.UTF_8);
        expect(IOException.class, () -> cli("index", index.toString(), file.toString()), "CLI syntax failure");
        check(cli("tokens", index.toString(), "ReleaseMarker").equals(hits), "failed CLI batch keeps prior committed index");
        check(cli("search", index.toString(), "UnpublishedOmega").isEmpty(), "failed batch does not publish new terms");
        try (FSDirectory directory = FSDirectory.open(index); DirectoryReader reader = DirectoryReader.open(directory)) {
            check(reader.numDocs() == 1, "CLI source update retains one document");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("M3SourceIndexProbe takes no arguments");
        Path root = Files.createTempDirectory("m3-source-index-probe-");
        try {
            moduleAndOwnershipContracts();
            frontendAndIndex(root);
            cliCommits(root);
            try (var files = Files.walk(root)) {
                check(files.noneMatch(path -> path.getFileName().toString().endsWith(".class")), "parse/analyze and CLI produce no class files");
            }
            System.out.println("M3_SOURCE_INDEX_PASS assertions=" + assertions + " token_postings=" + tokenPostings
                    + " expired_accesses=" + expiredAccesses + " named_modules=5 parsed_sources=2 analyzed_types=3 cli_rollback=PASS");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}
