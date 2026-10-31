/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.m3jdk.tools.search;

import com.m3.file.M3File;
import com.m3.file.M3FileDictionary;
import com.m3.file.M3FileResolver;
import com.m3.file.M3FileStreamIndexer;
import com.m3.file.M3FileTokenizer;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import opennlp.tools.tokenize.M3Tokenizers;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.M3TextReaders;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 * A source-search consumer of the actual javac M3AST frontend.
 *
 * <p>The caller owns the writer, M3 file resolver, analyzer configuration and commit policy.
 * Register one instance per compilation before parsing. This consumer retains no Unit, Tree,
 * Trees, Element or compiler task. Its projection key belongs to the instance and is memoized
 * by the compiler only for the current callback event. Analysis events do not reindex the same
 * source once for every top-level type.</p>
 *
 * <p>The text field uses the writer's actual analyzer over a borrowed M3File text reader.
 * The token field consumes the actual OpenNLP simple-tokenizer spans as a Lucene token stream.
 * Token character buffers and Lucene UTF-8 term bytes are explicit format boundaries; the
 * complete source is not stored again in the index. Only source URI, exact compiler UTF-16
 * digest and bounded AST metadata are stored. A digest identifies source characters, not
 * classpath/type-analysis equivalence.</p>
 */
public final class M3SourceIndex implements M3AST.Consumer {
    public static final String URI_FIELD = "uri";
    public static final String HASH_FIELD = "utf16Sha256";
    public static final String TEXT_FIELD = "text";
    public static final String TOKEN_FIELD = "token";
    public static final String PACKAGE_FIELD = "package";
    public static final String TYPE_FIELD = "type";
    private static final FieldType TOKEN_TYPE = tokenType();

    private static FieldType tokenType() {
        FieldType type = new FieldType(TextField.TYPE_NOT_STORED);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        type.freeze();
        return type;
    }

    private final IndexWriter writer;
    private final M3FileResolver resolver;
    private final int languageId;
    private final Runnable checkpoint;
    private final M3AST.Projection<Captured> sourceProjection;
    private long indexedSources;
    private long indexedTokens;

    public M3SourceIndex(IndexWriter writer, M3FileResolver resolver, int languageId) {
        this(writer, resolver, languageId, null);
    }

    /** A supplied checkpoint may report progress or throw cancellation; it never owns the task. */
    public M3SourceIndex(IndexWriter writer, M3FileResolver resolver, int languageId,
                         Runnable checkpoint) {
        this.writer = Objects.requireNonNull(writer, "writer");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        if (languageId < 0 || languageId > 0xffff) {
            throw new IllegalArgumentException("languageId must fit unsigned 16 bits");
        }
        this.languageId = languageId;
        this.checkpoint = checkpoint;
        this.sourceProjection = new M3AST.Projection<>("M3SourceIndex.source", this::capture);
    }

    private record Captured(M3File file, String uri, String digest,
                            String packageName, List<String> types) {}

    private Captured capture(M3AST.Unit unit) throws IOException {
        poll();
        M3AST.Source source = unit.source();
        JavaFileObject input = unit.tree().getSourceFile();
        // javac may wrap a caller JavaFileObject. A wrapped input falls back to exact sealed text;
        // the same resolver preserves canonical token identities and owns all payload admission.
        M3File file = input instanceof SourceFile existing ? existing.file
                : M3FileTokenizer.codeLike(resolver, languageId, source.text());
        CharSequence text = file.asM3String();
        if (text.length() != source.text().length()) {
            throw new IOException("M3File does not match the compiler source length");
        }
        for (int i = 0; i < text.length(); i++) {
            if ((i & 1023) == 0) poll();
            if (text.charAt(i) != source.text().charAt(i)) {
                throw new IOException("M3File does not match compiler UTF-16 at " + i);
            }
        }
        String packageName = unit.tree().getPackageName() == null
                ? "" : unit.tree().getPackageName().toString();
        List<String> types = new ArrayList<>();
        for (Tree declaration : unit.tree().getTypeDecls()) {
            if (declaration instanceof ClassTree type) {
                String name = type.getSimpleName().toString();
                types.add(packageName.isEmpty() ? name : packageName + "." + name);
            }
        }
        return new Captured(file, source.uri().toString(), source.utf16Sha256(),
                packageName, List.copyOf(types));
    }

    @Override
    public void parsed(M3AST.Unit unit) throws Exception {
        if (unit.phase() != M3AST.Phase.PARSED) {
            throw new IllegalArgumentException("source indexing requires the PARSED event");
        }
        Captured source = unit.project(sourceProjection);
        CharSequence text = source.file().asM3String();
        Span[] spans = M3Tokenizers.simpleSpans(text, this::poll);
        try (Reader reader = M3TextReaders.reader(text);
             TokenStream tokens = new SpanTokens(text, spans, this::poll)) {
            Document doc = new Document();
            doc.add(new StringField(URI_FIELD, source.uri(), Field.Store.YES));
            doc.add(new StringField(HASH_FIELD, source.digest(), Field.Store.YES));
            doc.add(new StringField(PACKAGE_FIELD, source.packageName(), Field.Store.YES));
            for (String type : source.types()) {
                doc.add(new StringField(TYPE_FIELD, type, Field.Store.YES));
            }
            doc.add(new StoredField("utf16Length", text.length()));
            doc.add(new StoredField("tokenCount", spans.length));
            doc.add(new TextField(TEXT_FIELD, reader));
            doc.add(new Field(TOKEN_FIELD, tokens, TOKEN_TYPE));
            poll();
            writer.updateDocument(new Term(URI_FIELD, source.uri()), doc);
        }
        indexedSources++;
        indexedTokens += spans.length;
    }

    public record Statistics(long indexedSources, long indexedTokens) {}

    /** Operation counts for this consumer; no compiler reference or source snapshot is retained. */
    public Statistics statistics() {
        return new Statistics(indexedSources, indexedTokens);
    }

    private void poll() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("source indexing interrupted");
        }
        if (checkpoint != null) checkpoint.run();
    }

    /** Standard text analysis used by the command-line indexer and its text query parser. */
    public static Analyzer analyzer() {
        return new StandardAnalyzer();
    }

    /** Parse Lucene query syntax with the same analyzer used to index the text field. */
    public static Query textQuery(String query, Analyzer analyzer) throws ParseException {
        return new QueryParser(TEXT_FIELD, Objects.requireNonNull(analyzer, "analyzer"))
                .parse(Objects.requireNonNull(query, "query"));
    }

    /** Exact case-sensitive phrase under the current OpenNLP simple-tokenizer configuration. */
    public static Query tokenPhrase(CharSequence text) {
        Span[] spans = M3Tokenizers.simpleSpans(text);
        if (spans.length == 0) return new MatchNoDocsQuery("empty OpenNLP token sequence");
        PhraseQuery.Builder query = new PhraseQuery.Builder();
        for (Span span : spans) {
            query.add(new Term(TOKEN_FIELD,
                    new BytesRef(new Range(text, span.getStart(), span.getEnd()))));
        }
        return query.build();
    }

    /** A bounded borrowed term view; Lucene owns the UTF-8 encoding boundary. */
    private record Range(CharSequence source, int start, int end) implements CharSequence {
        Range {
            Objects.requireNonNull(source, "source");
            Objects.checkFromToIndex(start, end, source.length());
        }
        @Override public int length() { return end - start; }
        @Override public char charAt(int index) {
            Objects.checkIndex(index, length());
            return source.charAt(start + index);
        }
        @Override public CharSequence subSequence(int from, int to) {
            Objects.checkFromToIndex(from, to, length());
            return new Range(source, start + from, start + to);
        }
        @Override public String toString() {
            throw new UnsupportedOperationException("term view is not a String materializer");
        }
    }

    private static final class SpanTokens extends TokenStream {
        private CharSequence text;
        private Span[] spans;
        private final int length;
        private final Runnable checkpoint;
        private int next;
        private final CharTermAttribute term = addAttribute(CharTermAttribute.class);
        private final OffsetAttribute offsets = addAttribute(OffsetAttribute.class);
        private final PositionIncrementAttribute increment = addAttribute(PositionIncrementAttribute.class);
        private final PositionLengthAttribute positionLength = addAttribute(PositionLengthAttribute.class);
        private final TypeAttribute type = addAttribute(TypeAttribute.class);

        SpanTokens(CharSequence text, Span[] spans, Runnable checkpoint) {
            this.text = text;
            this.spans = spans;
            this.length = text.length();
            this.checkpoint = checkpoint;
        }
        @Override public void reset() throws IOException {
            super.reset();
            if (text == null) throw new IllegalStateException("token stream is closed");
            next = 0;
        }
        @Override public boolean incrementToken() {
            checkpoint.run();
            if (next == spans.length) return false;
            clearAttributes();
            Span span = spans[next++];
            int count = span.length();
            char[] buffer = term.resizeBuffer(count);
            for (int i = 0; i < count; i++) {
                if ((i & 1023) == 0) checkpoint.run();
                buffer[i] = text.charAt(span.getStart() + i);
            }
            term.setLength(count);
            offsets.setOffset(span.getStart(), span.getEnd());
            increment.setPositionIncrement(1);
            positionLength.setPositionLength(1);
            type.setType("word");
            return true;
        }
        @Override public void end() throws IOException {
            super.end();
            offsets.setOffset(length, length);
        }
        @Override public void close() {
            text = null;
            spans = null;
        }
    }

    /** Read a UTF-8 file into the complete M3File owner before giving that snapshot to javac. */
    public static JavaFileObject source(Path path, M3FileResolver resolver, int languageId)
            throws IOException {
        Path input = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        return source(input.toUri(), M3FileStreamIndexer.codeLike(input, resolver, languageId));
    }

    /** The caller owns the immutable M3File and its resolver for the complete compiler task. */
    public static JavaFileObject source(URI uri, M3File file) {
        return new SourceFile(Objects.requireNonNull(uri, "uri"), Objects.requireNonNull(file, "file"));
    }

    private static final class SourceFile extends SimpleJavaFileObject {
        private final M3File file;
        SourceFile(URI uri, M3File file) {
            super(uri, Kind.SOURCE);
            this.file = file;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return file.asM3String();
        }
        @Override public Reader openReader(boolean ignoreEncodingErrors) {
            return M3TextReaders.reader(file.asM3String());
        }
    }

    /**
     * index INDEX SOURCE.java... | search INDEX QUERY | tokens INDEX EXACT_PHRASE
     *
     * <p>Indexing parses the snapshots without annotation processors or class generation.
     * Only an explicit successful commit publishes updates; syntax failure keeps the prior index.
     * Search uses a newly opened directory reader and prints score, source URI and UTF-16 digest.</p>
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 3 || !List.of("index", "search", "tokens").contains(args[0])) {
            throw new IllegalArgumentException("index INDEX SOURCE.java... | search INDEX QUERY | tokens INDEX EXACT_PHRASE");
        }
        Path index = Path.of(args[1]);
        if (args[0].equals("index")) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) throw new IllegalStateException("jdk.compiler is required");
            M3FileDictionary dictionary = new M3FileDictionary();
            List<JavaFileObject> inputs = new ArrayList<>();
            for (int i = 2; i < args.length; i++) inputs.add(source(Path.of(args[i]), dictionary, 0));
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null);
                 Analyzer analyzer = analyzer(); FSDirectory directory = FSDirectory.open(index);
                 IndexWriter writer = new IndexWriter(directory,
                         new IndexWriterConfig(analyzer).setCommitOnClose(false))) {
                JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                        List.of("-proc:none"), null, inputs);
                M3SourceIndex consumer = new M3SourceIndex(writer, dictionary, 0);
                try (M3AST ast = M3AST.open(task, true)) {
                    ast.addConsumer(consumer);
                    task.parse();
                }
                long errors = diagnostics.getDiagnostics().stream()
                        .filter(d -> d.getKind() == Diagnostic.Kind.ERROR).count();
                if (errors != 0) throw new IOException("source parse errors: " + errors);
                writer.commit();
                System.out.println("M3_SOURCE_INDEX sources=" + consumer.statistics().indexedSources()
                        + " tokens=" + consumer.statistics().indexedTokens());
            }
        } else {
            String expression = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            try (Analyzer analyzer = analyzer(); FSDirectory directory = FSDirectory.open(index);
                 DirectoryReader reader = DirectoryReader.open(directory)) {
                Query query = args[0].equals("tokens") ? tokenPhrase(expression) : textQuery(expression, analyzer);
                IndexSearcher searcher = new IndexSearcher(reader);
                for (var hit : searcher.search(query, 20).scoreDocs) {
                    Document document = searcher.storedFields().document(hit.doc);
                    System.out.println(hit.score + "\t" + document.get(URI_FIELD) + "\t" + document.get(HASH_FIELD));
                }
            }
        }
    }
}
