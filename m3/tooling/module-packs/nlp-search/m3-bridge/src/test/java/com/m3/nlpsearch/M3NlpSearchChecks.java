/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.nlpsearch;

import com.m3.runtime.capability.SourceArtifact;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.TokenSample;
import opennlp.tools.tokenize.TokenizerFactory;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.CollectionObjectStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

/** Real donor model/index checks with source-backed byte and composite-text ingress. */
public final class M3NlpSearchChecks {
    private static int checks;

    private M3NlpSearchChecks() { }

    private record Composite(String left, String right) implements CharSequence {
        Composite(String text, int split) {
            this(text.substring(0, split), text.substring(split));
        }

        @Override public int length() { return left.length() + right.length(); }

        @Override public char charAt(int index) {
            Objects.checkIndex(index, length());
            return index < left.length() ? left.charAt(index) : right.charAt(index - left.length());
        }

        @Override public CharSequence subSequence(int start, int end) {
            throw new AssertionError("input must not be sliced");
        }

        @Override public String toString() {
            throw new AssertionError("input must not be flattened");
        }
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail);
    }

    private static void expect(Class<? extends Throwable> type, Action action) throws Exception {
        try {
            action.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getName() + ", got " + failure);
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static String sealOracle(String text) throws Exception {
        // CharBuffer writes exact raw UTF-16 units; a charset encoder would replace lone surrogates.
        ByteBuffer bytes = ByteBuffer.allocate(text.length() * Character.BYTES);
        bytes.asCharBuffer().put(text);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.array()));
    }

    private static void compositeAndSealContracts() throws Exception {
        String[] fixtures = {"", "ab12 ++ X", "alpha beta", "caf\u00e9 \u0301x",
                "A\ud83d\ude00B", "x\ud800y\udc00z", "a\u0000b\r\n c\u00a0d"};
        for (String text : fixtures) {
            Span[] wanted = SimpleTokenizer.INSTANCE.tokenizePos(text);
            String seal = sealOracle(text);
            for (int split = 0; split <= text.length(); split++) {
                var composite = new Composite(text, split);
                check(Arrays.equals(wanted, M3NlpSearch.tokenSpans(composite)), "token offsets");
                check(Arrays.equals(wanted, M3NlpSearch.tokenSpans(composite, () -> { })),
                        "checkpoint token offsets");
                check(seal.equals(M3NlpSearch.utf16Sha256(composite)), "exact UTF-16 source seal");
                Document document = M3NlpSearch.document(URI.create("urn:m3:composite"), composite, seal);
                try (Reader reader = document.getField(M3NlpSearch.TEXT_FIELD).readerValue()) {
                    for (int index = 0; index < text.length(); index++) {
                        check(reader.read() == text.charAt(index), "borrowed reader code unit");
                    }
                    check(reader.read() == -1, "reader EOF");
                }
                check(document.get(M3NlpSearch.TEXT_FIELD) == null, "source text not stored");
            }
        }
        check(Arrays.equals(M3NlpSearch.tokenSpans("ab12 ++ X"),
                new Span[] {new Span(0, 2), new Span(2, 4), new Span(5, 7), new Span(8, 9)}),
                "independent ASCII token boundary example");
        check(M3NlpSearch.utf16Sha256("").equals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"), "empty seal");
        check(!M3NlpSearch.utf16Sha256("A").equals(SourceArtifact.copyOf("ascii", new byte[] {65}).root()),
                "file-byte identity differs from UTF-16 identity");
        expect(IllegalArgumentException.class, () -> M3NlpSearch.document(
                URI.create("urn:m3:bad-seal"), "abc", "0".repeat(64)));
        expect(NullPointerException.class, () -> M3NlpSearch.tokenSpans(null));
        AtomicInteger checkpoints = new AtomicInteger();
        expect(CancellationException.class, () -> M3NlpSearch.tokenSpans(
                new Composite("a".repeat(600), 301), () -> {
                    if (checkpoints.incrementAndGet() == 3) throw new CancellationException("fixture");
                }));
        check(checkpoints.get() == 3, "checkpoint reaches real scanner");
        Thread.currentThread().interrupt();
        try {
            expect(CancellationException.class, () -> M3NlpSearch.tokenSpans("abc"));
            check(Thread.currentThread().isInterrupted(), "tokenizer preserves interrupt status");
        } finally {
            Thread.interrupted();
        }
    }

    private static void modelRoundTripThroughSourceSpan() throws Exception {
        List<TokenSample> samples = new ArrayList<>();
        for (int repeat = 0; repeat < 16; repeat++) {
            samples.add(new TokenSample("Cats, run.", new Span[] {
                    new Span(0, 4), new Span(4, 5), new Span(6, 9), new Span(9, 10)}));
            samples.add(new TokenSample("Dogs! Stay.", new Span[] {
                    new Span(0, 4), new Span(4, 5), new Span(6, 10), new Span(10, 11)}));
        }
        TrainingParameters parameters = TrainingParameters.defaultParams();
        parameters.put("Iterations", 20);
        parameters.put("Cutoff", 1);
        parameters.put("DataIndexer", "OnePass");
        TokenizerModel model;
        try (var training = new CollectionObjectStream<>(samples)) {
            model = TokenizerME.train(training, new TokenizerFactory("eng", null, false, null), parameters);
        }
        var encoded = new ByteArrayOutputStream();
        model.serialize(encoded);
        byte[] modelBytes = encoded.toByteArray();
        byte[] framed = new byte[modelBytes.length + 6];
        Arrays.fill(framed, (byte) 91);
        System.arraycopy(modelBytes, 0, framed, 3, modelBytes.length);
        SourceArtifact owner = SourceArtifact.copyOf("tokenizer-model-frame", framed);
        SourceArtifact.Span modelSpan = owner.span(3, modelBytes.length);
        String root = owner.root();
        String spanSeal = modelSpan.sha256();
        TokenizerModel reloaded;
        try (var input = M3SourceInputStream.open(modelSpan)) {
            reloaded = new TokenizerModel(input);
        }
        TokenizerME originalTokenizer = new TokenizerME(model);
        TokenizerME loadedTokenizer = new TokenizerME(reloaded);
        for (String text : List.of("Cats, run.", "Dogs! Stay.", "Hello, world!")) {
            check(Arrays.equals(originalTokenizer.tokenizePos(text), loadedTokenizer.tokenizePos(text)),
                    "actual trained model reload predictions");
        }
        try (var truncated = M3SourceInputStream.open(modelSpan.slice(0, modelBytes.length / 2))) {
            expect(IOException.class, () -> new TokenizerModel(truncated));
        }
        try (var fresh = M3SourceInputStream.open(modelSpan)) {
            check(Arrays.equals(fresh.readAllBytes(), modelBytes), "model bytes reopen exactly");
        }
        check(root.equals(owner.root()) && spanSeal.equals(modelSpan.sha256()), "model source seals stable");
    }

    private static int count(IndexSearcher searcher, String field, String term) throws IOException {
        return searcher.count(new TermQuery(new Term(field, term)));
    }

    private static void persistedIndexRoundTrip(Path path) throws Exception {
        if (Files.exists(path) || Files.isSymbolicLink(path)) {
            throw new IllegalArgumentException("index directory must be absent: " + path);
        }
        Files.createDirectory(path);
        URI source = URI.create("file:///fixture/A/../Source.java");
        URI other = URI.create("file:///fixture/Source.java");
        String text = "Alpha beta caf\u00e9 \ud83d\ude00 gamma";
        String seal = sealOracle(text);
        try (var directory = FSDirectory.open(path); var analyzer = new StandardAnalyzer();
                var writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            for (int split = 0; split <= text.length(); split++) {
                M3NlpSearch.update(writer, source, new Composite(text, split), seal);
            }
            Document direct = M3NlpSearch.document(other, "delta");
            try (Reader reader = direct.getField(M3NlpSearch.TEXT_FIELD).readerValue()) {
                check(reader != null, "document exposes its reader for explicit cleanup");
                writer.addDocument(direct);
            }
            expect(IllegalArgumentException.class, () -> M3NlpSearch.update(
                    writer, source, "tampered", seal));
            try (var reader = DirectoryReader.open(writer)) {
                check(reader.numDocs() == 2, "URI-keyed update is idempotent; exact URI spelling retained");
                var searcher = new IndexSearcher(reader);
                check(count(searcher, M3NlpSearch.TEXT_FIELD, "alpha") == 1, "composite text indexed");
                check(count(searcher, M3NlpSearch.TEXT_FIELD, "tampered") == 0, "bad seal writes nothing");
            }
            check(writer.isOpen(), "adapter leaves caller writer open");
            check(writer.hasUncommittedChanges(), "caller still owns pending commit");
            writer.commit();
        }
        try (var directory = FSDirectory.open(path); var reader = DirectoryReader.open(directory)) {
            var searcher = new IndexSearcher(reader);
            check(reader.numDocs() == 2, "persisted document count");
            check(count(searcher, M3NlpSearch.SOURCE_URI_FIELD, source.toString()) == 1, "exact URI term");
            check(count(searcher, M3NlpSearch.SOURCE_URI_FIELD, other.toString()) == 1, "distinct normalized URI");
            var hits = searcher.search(new TermQuery(new Term(M3NlpSearch.TEXT_FIELD, "alpha")), 1);
            check(hits.scoreDocs.length == 1, "reopened search hit");
            Document stored = reader.storedFields().document(hits.scoreDocs[0].doc);
            check(source.toString().equals(stored.get(M3NlpSearch.SOURCE_URI_FIELD)), "stored source URI");
            check(seal.equals(stored.get(M3NlpSearch.SOURCE_UTF16_SHA256_FIELD)), "stored exact source seal");
            check(stored.getField(M3NlpSearch.SOURCE_UTF16_LENGTH_FIELD).numericValue().intValue()
                    == text.length(), "stored UTF-16 length");
            check(stored.get(M3NlpSearch.TEXT_FIELD) == null, "no duplicate stored source payload");
        }
        try (var directory = FSDirectory.open(path); var analyzer = new StandardAnalyzer();
                var writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            M3NlpSearch.update(writer, source, new Composite("epsilon", 3), sealOracle("epsilon"));
            writer.commit();
        }
        try (var directory = FSDirectory.open(path); var reader = DirectoryReader.open(directory)) {
            var searcher = new IndexSearcher(reader);
            check(reader.numDocs() == 2, "new source generation replaces its URI document");
            check(count(searcher, M3NlpSearch.TEXT_FIELD, "alpha") == 0, "old generation absent");
            check(count(searcher, M3NlpSearch.TEXT_FIELD, "epsilon") == 1, "new generation searchable");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("one absent index-directory path required");
        compositeAndSealContracts();
        modelRoundTripThroughSourceSpan();
        persistedIndexRoundTrip(Path.of(args[0]).toAbsolutePath());
        System.out.println("M3_NLP_SEARCH_PASS checks=" + checks
                + " model_reload=PASS disk_index_reopen=PASS input_flatten_calls=0");
    }
}
