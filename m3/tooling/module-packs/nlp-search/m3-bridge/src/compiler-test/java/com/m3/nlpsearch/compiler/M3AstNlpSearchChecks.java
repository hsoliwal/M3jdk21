/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.nlpsearch.compiler;

import com.m3.nlpsearch.M3NlpSearch;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.ByteBuffersDirectory;

/** Requires the real M3AST implementation and parser-retention hook in the executing compiler. */
public final class M3AstNlpSearchChecks {
    private static int checks;

    private M3AstNlpSearchChecks() { }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail);
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String text;
        private int reads;

        Source(URI uri, String text) {
            super(uri, Kind.SOURCE);
            this.text = text;
        }

        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads++;
            return text;
        }
    }

    private static final class MemoryFiles extends ForwardingJavaFileManager<StandardJavaFileManager> {
        MemoryFiles(StandardJavaFileManager owner) {
            super(owner);
        }

        @Override public JavaFileObject getJavaFileForOutput(Location location, String className,
                JavaFileObject.Kind kind, FileObject sibling) {
            URI uri = URI.create("mem:///" + className.replace('.', '/') + kind.extension);
            return new SimpleJavaFileObject(uri, kind) {
                @Override public OutputStream openOutputStream() {
                    return new ByteArrayOutputStream();
                }
            };
        }
    }

    private record Run(boolean successful, RuntimeException failure, M3AST.Summary summary,
            int sourceReads) { }

    private static Run compile(Source source, List<M3AST.Consumer> consumers) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "executing image supplies a compiler");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = new MemoryFiles(compiler.getStandardFileManager(diagnostics, null, null))) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("-proc:none", "-g:none"), null, List.of(source));
            M3AST shared = M3AST.open(task);
            boolean successful = false;
            RuntimeException failure = null;
            try (shared) {
                for (M3AST.Consumer consumer : consumers) shared.addConsumer(consumer);
                try {
                    successful = task.call();
                } catch (RuntimeException caught) {
                    failure = caught;
                }
            }
            if (failure == null) {
                check(successful, "actual compilation failed: " + diagnostics.getDiagnostics());
            }
            return new Run(successful, failure, shared.summary(), source.reads);
        }
    }

    private static int count(IndexWriter writer, String term) throws IOException {
        try (var reader = DirectoryReader.open(writer)) {
            return new IndexSearcher(reader).count(new TermQuery(new Term(M3NlpSearch.TEXT_FIELD, term)));
        }
    }

    private static boolean causedBy(Throwable failure, Throwable expected) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current == expected) return true;
        }
        return false;
    }

    private static void sharedProjectionAndNextCompilation() throws Exception {
        URI uri = URI.create("mem:///fixture/Fixture.java");
        String text = "package fixture; /* caf\u00e9 \ud83d\ude00 */ class First {} class Second {}";
        try (var directory = new ByteBuffersDirectory(); var analyzer = new StandardAnalyzer();
                var writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            var key = M3AstNlpSearchConsumer.projection(writer);
            List<M3AstNlpSearchConsumer.Annotations> first = new ArrayList<>();
            List<M3AstNlpSearchConsumer.Annotations> second = new ArrayList<>();
            AtomicInteger analyzed = new AtomicInteger();
            // Deliberate test-only escape proves the existing owner's post-callback refusal.
            AtomicReference<M3AST.Unit> escaped = new AtomicReference<>();
            Run run = compile(new Source(uri, text), List.of(
                    new M3AstNlpSearchConsumer(key, first::add),
                    new M3AstNlpSearchConsumer(key, second::add),
                    new M3AST.Consumer() {
                        @Override public void parsed(M3AST.Unit unit) { escaped.set(unit); }
                        @Override public void analyzed(M3AST.Unit unit) { analyzed.incrementAndGet(); }
                    }));
            check(run.failure() == null && run.successful(), "actual compilation succeeds");
            check(run.summary().parsedUnits() == 1, "one original parse");
            check(run.summary().analyzedTypes() == 2 && analyzed.get() == 2, "original type analyses");
            check(run.summary().capturedSources() == 1, "actual parser retained one source");
            check(run.summary().sourceReads() == 0 && run.sourceReads() == 1, "no source reread");
            check(run.summary().projectionComputations() == 1, "shared projection computed once");
            check(run.summary().projectionHits() == 1, "second consumer used same event value");
            check(run.summary().closed() && run.summary().completed(), "compilation releases handle");
            check(!run.summary().consumerFailed() && run.summary().compilerErrors() == 0,
                    "successful compiler disposition");
            check(first.size() == 1 && second.size() == 1 && first.getFirst() == second.getFirst(),
                    "same detached projection delivered to both sinks");
            var annotations = first.getFirst();
            check(annotations.sourceUri().equals(uri), "source URI detached exactly");
            check(annotations.utf16Sha256().equals(M3NlpSearch.utf16Sha256(text)), "actual compiler seal");
            check(annotations.utf16Length() == text.length(), "source code-unit length");
            check(annotations.tokens().equals(Arrays.asList(SimpleTokenizer.INSTANCE.tokenizePos(text))),
                    "real OpenNLP annotations from parsed text");
            try {
                annotations.tokens().clear();
                throw new AssertionError("mutable annotation list");
            } catch (UnsupportedOperationException expected) {
                checks++;
            }
            try {
                escaped.get().source();
                throw new AssertionError("escaped compiler Unit remained usable");
            } catch (IllegalStateException expected) {
                checks++;
            } finally {
                escaped.set(null);
            }
            check(writer.isOpen() && writer.hasUncommittedChanges(), "caller retains writer and commit");
            check(count(writer, "first") == 1 && count(writer, "second") == 1,
                    "parsed source indexed with real Lucene analyzer");

            String nextText = "package fixture; class Third {}";
            List<M3AstNlpSearchConsumer.Annotations> next = new ArrayList<>();
            Run nextRun = compile(new Source(uri, nextText),
                    List.of(new M3AstNlpSearchConsumer(key, next::add)));
            check(nextRun.failure() == null && nextRun.summary().projectionComputations() == 1,
                    "operation key does not reuse projections across compilation tasks");
            check(nextRun.summary().projectionHits() == 0, "new task has no prior event cache");
            check(next.size() == 1 && !next.getFirst().utf16Sha256().equals(annotations.utf16Sha256()),
                    "changed source generation has its own seal");
            try (var reader = DirectoryReader.open(writer)) {
                check(reader.numDocs() == 1, "URI replacement preserves one current generation");
            }
            check(count(writer, "first") == 0 && count(writer, "third") == 1, "new parse replaced old text");
            check(annotations.tokens().equals(Arrays.asList(SimpleTokenizer.INSTANCE.tokenizePos(text))),
                    "earlier detached annotations remain usable and unchanged");
        }
    }

    private static void sinkFailureRetainsCallerWrite() throws Exception {
        try (var directory = new ByteBuffersDirectory(); var analyzer = new StandardAnalyzer();
                var writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            var marker = new IllegalStateException("expected sink failure after update");
            var consumer = new M3AstNlpSearchConsumer(M3AstNlpSearchConsumer.projection(writer), value -> {
                throw marker;
            });
            Run run = compile(new Source(URI.create("mem:///Failure.java"), "class Failure {}"),
                    List.of(consumer));
            check(causedBy(run.failure(), marker), "actual M3AST propagates sink failure");
            check(run.summary().closed() && run.summary().consumerFailed(), "failed consumer closes session");
            check(writer.isOpen() && count(writer, "failure") == 1,
                    "already successful update remains under caller transaction policy");
        }
    }

    private static void cancellationBeforeIndexUpdate() throws Exception {
        try (var directory = new ByteBuffersDirectory(); var analyzer = new StandardAnalyzer();
                var writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            var marker = new CancellationException("expected tokenizer cancellation");
            AtomicInteger deliveries = new AtomicInteger();
            var projection = M3AstNlpSearchConsumer.projection(writer, () -> { throw marker; });
            var consumer = new M3AstNlpSearchConsumer(projection, value -> deliveries.incrementAndGet());
            Run run = compile(new Source(URI.create("mem:///Cancelled.java"), "class Cancelled {}"),
                    List.of(consumer));
            check(causedBy(run.failure(), marker), "actual tokenizer cancellation propagates");
            check(run.summary().closed() && run.summary().consumerFailed(), "cancelled callback closes session");
            check(deliveries.get() == 0, "no sink result after cancellation");
            try (var reader = DirectoryReader.open(writer)) {
                check(reader.numDocs() == 0, "tokenizer failure precedes index write");
            }
            check(writer.isOpen(), "cancelled compiler does not close caller writer");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments expected");
        sharedProjectionAndNextCompilation();
        sinkFailureRetainsCallerWrite();
        cancellationBeforeIndexUpdate();
        System.out.println("M3_AST_NLP_SEARCH_PASS checks=" + checks
                + " compiler=ACTUAL_M3AST second_parse=NONE");
    }
}
