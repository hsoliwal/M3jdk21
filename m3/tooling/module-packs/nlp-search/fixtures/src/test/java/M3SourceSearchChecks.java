// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.m3.search.M3File;
import jdk.m3.search.M3FileSourceFacts;
import jdk.m3.search.M3Search;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

/** Real source snapshots, Apache text ingress, and a committed/reopened disk index. */
public final class M3SourceSearchChecks {
    private static int checks;
    private static final String UNICODE = "A\u00e9\uD83D\uDE00Z";
    private static final String UNICODE_SOURCE_SHA =
            "cf792b83bfcda5dc8cba50da30a839a2b15468d30284bdb5542ed7c544834f0f";

    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static <T extends Throwable> T expect(
            Class<T> type, ThrowingAction action, String description) throws Exception {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError(description + ": wrong exception", failure);
        }
        throw new AssertionError(description + ": no exception");
    }

    private static String bytesSha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String read(Reader source) throws IOException {
        try (source) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[3];
            for (int count; (count = source.read(buffer, 0, buffer.length)) != -1;) {
                result.append(buffer, 0, count);
            }
            return result.toString();
        }
    }

    private record Segmented(String left, String right) implements CharSequence {
        @Override public int length() { return left.length() + right.length(); }
        @Override public char charAt(int index) {
            Objects.checkIndex(index, length());
            return index < left.length() ? left.charAt(index) : right.charAt(index - left.length());
        }
        @Override public CharSequence subSequence(int start, int end) {
            throw new AssertionError("borrowed input must not be sliced");
        }
        @Override public String toString() {
            throw new AssertionError("borrowed input must not be flattened");
        }
    }

    private static void digestVectors() {
        // Frozen vectors were independently calculated with Python hashlib, struct.pack(">I"),
        // and encode("utf-16-be", "surrogatepass") from the documented V1 framing contract.
        String[][] vectors = {
            {"", "682d087c89bab38303a5e9be7b30808459818b069fca4a1b4cbc061e92aee961"},
            {"abc", "caf733ad3f8dd6c5b51a341d689e59081bcda3fcc57486c7369e74a8cf7c80f6"},
            {UNICODE, UNICODE_SOURCE_SHA},
            {"\uD800", "0c3b4a91b92e479f3a428d22ecb9252c7d58a86ef6bebe6a7a8873649a379bd4"},
            {"\uD801", "e2e1885c71be6dda36592d9511cb7b7445f7264983b6ce3eedabd0c22219d571"},
            {"\uFFFD", "ba33c2e382a0c0b0e3fd906e19dc2bae3120942054747b27a293cad4e34ef0a8"},
            {"\uD800X\uDC00", "bad97217361d28ca077f7c1753df80335dbae0e831eab147f61bcf9b7c1c04ba"}
        };
        for (String[] vector : vectors) {
            check(M3FileSourceFacts.sourceSha256(vector[0]).equals(vector[1]),
                    "exact UTF-16 source digest vector");
        }
        check(!M3FileSourceFacts.contextSha256(List.of("ab", "c"))
                .equals(M3FileSourceFacts.contextSha256(List.of("a", "bc"))),
                "context framing prevents concatenation ambiguity");
        check(!M3FileSourceFacts.contextSha256(List.of("first", "second"))
                .equals(M3FileSourceFacts.contextSha256(List.of("second", "first"))),
                "ordered compiler context retains order");
        check(!M3FileSourceFacts.sourceSha256("")
                .equals(M3FileSourceFacts.contextSha256(List.of(""))),
                "source and context hash domains differ");
    }

    private static void fileSnapshots(Path work) throws Exception {
        byte[] utf8 = UNICODE.getBytes(StandardCharsets.UTF_8);
        byte[] utf16 = UNICODE.getBytes(StandardCharsets.UTF_16LE);
        Path first = work.resolve("Utf8.java"), second = work.resolve("Utf16.java");
        Files.write(first, utf8);
        Files.write(second, utf16);
        M3File a = M3File.read(first, StandardCharsets.UTF_8, utf8.length);
        M3File b = M3File.read(second, StandardCharsets.UTF_16LE, utf16.length);
        check(a.source().equals(UNICODE) && b.source().equals(UNICODE), "explicit decoding");
        check(a.source().length() == 5, "UTF-16 length includes both surrogate units");
        check(a.sourceSha256().equals(UNICODE_SOURCE_SHA)
                && b.sourceSha256().equals(UNICODE_SOURCE_SHA), "encoding-independent source identity");
        check(a.byteSha256().equals(bytesSha(utf8)), "original UTF-8 byte identity");
        check(b.byteSha256().equals(bytesSha(utf16)), "original UTF-16LE byte identity");
        check(!a.byteSha256().equals(b.byteSha256()), "different encodings retain different byte hashes");
        check(!a.byteSha256().equals(a.sourceSha256()), "raw byte and framed source identity differ");
        check(a.toUri().equals(first.toAbsolutePath().normalize().toUri()), "file URI");
        expect(IOException.class, () -> M3File.read(first, StandardCharsets.UTF_8, utf8.length - 1),
                "one byte over budget");
        expect(IOException.class, () -> M3File.read(first, StandardCharsets.UTF_8, UNICODE.length()),
                "budget measures bytes, not characters");
        expect(IllegalArgumentException.class, () -> M3File.read(first, StandardCharsets.UTF_8, -1),
                "negative byte budget");
        expect(IOException.class, () -> M3File.read(first, StandardCharsets.UTF_8, 0),
                "nonempty zero-budget input");
        Path empty = work.resolve("Empty.java");
        Files.write(empty, new byte[0]);
        M3File zero = M3File.read(empty, StandardCharsets.UTF_8, 0);
        check(zero.source().isEmpty() && zero.byteSha256().equals(bytesSha(new byte[0])),
                "empty zero-budget snapshot");
        Path malformed = work.resolve("Malformed.java");
        Files.write(malformed, new byte[] {(byte) 0xc3});
        expect(CharacterCodingException.class,
                () -> M3File.read(malformed, StandardCharsets.UTF_8, 1),
                "strict UTF-8 decoder rejects truncated multibyte input");
        Files.write(malformed, new byte[] {65});
        expect(CharacterCodingException.class,
                () -> M3File.read(malformed, StandardCharsets.UTF_16LE, 1),
                "strict UTF-16 decoder rejects an odd byte count");
        Files.writeString(first, "new disk content", StandardCharsets.UTF_8);
        check(a.source().equals(UNICODE), "snapshot remains stable after disk edit");
        check(read(a.openReader(false)).equals(UNICODE)
                && a.getCharContent(true).equals(UNICODE), "compiler observes the same snapshot");
        check(a.byteSha256().equals(bytesSha(utf8)), "disk edits do not relabel old byte identity");
        check(!M3File.read(first, StandardCharsets.UTF_8, 1024).sourceSha256().equals(a.sourceSha256()),
                "reread observes a new source identity");

        StringBuilder mutable = new StringBuilder("Alpha 12!");
        URI uri = URI.create("mem:///src/Mutable.java");
        M3File snapshot = M3File.of(uri, mutable);
        M3Search.Tokens tokens = M3Search.tokens(snapshot);
        mutable.replace(0, mutable.length(), "Beta 99?");
        check(snapshot.source().equals("Alpha 12!") && snapshot.byteSha256() == null,
                "mutable character input is snapshotted without fabricated byte provenance");
        check(tokens.source() == snapshot, "token result retains the exact source object");
        check(tokens.size() == 3 && tokens.token(0).toString().equals("Alpha")
                && tokens.token(1).toString().equals("12") && tokens.token(2).toString().equals("!"),
                "tokens stay attached to the snapshot after producer mutation");
        check(tokens.span(0).getStart() == 0 && tokens.span(0).getEnd() == 5,
                "token offsets refer to the original source");
        M3File replacement = M3File.of(uri, mutable);
        check(tokens.source() != replacement && !snapshot.source().equals(replacement.source()),
                "later same-URI snapshot cannot retarget existing tokens");
        expect(IndexOutOfBoundsException.class, () -> tokens.token(-1), "negative token position");
        expect(IndexOutOfBoundsException.class, () -> tokens.span(tokens.size()), "token end bound");
        check(read(M3Search.reader(snapshot)).equals(snapshot.source()), "M3File reader ingress");
    }

    private static void borrowedIngress() throws Exception {
        List<String> cases = List.of("", "Alpha 12!", "x \t y\r\nz", UNICODE,
                "A\uD83D\uDE00B", "\uD800X\uDC00", "e\u0301 \u4e2d\u6587", "\u0000a");
        for (String text : cases) {
            Span[] oracle = SimpleTokenizer.INSTANCE.tokenizePos(text);
            for (int split = 0; split <= text.length(); split++) {
                Segmented source = new Segmented(text.substring(0, split), text.substring(split));
                check(read(M3Search.reader(source)).equals(text), "borrowed reader UTF-16 parity");
                check(Arrays.equals(M3Search.spans(source), oracle), "borrowed Apache span parity");
            }
        }
        AtomicInteger checkpoints = new AtomicInteger();
        String longText = "alpha 12 ".repeat(1024);
        Segmented large = new Segmented(longText.substring(0, 7), longText.substring(7));
        check(Arrays.equals(M3Search.spans(large, checkpoints::incrementAndGet),
                SimpleTokenizer.INSTANCE.tokenizePos(longText)), "checkpointed tokenization parity");
        check(checkpoints.get() > 0, "host receives tokenization checkpoints");
        RuntimeException cancelled = new RuntimeException("fixture cancellation");
        RuntimeException observed = expect(RuntimeException.class,
                () -> M3Search.spans(large, () -> { throw cancelled; }), "host cancellation");
        check(observed == cancelled, "host cancellation identity propagates");
    }

    private static Query inContext(String text, String context) {
        return new BooleanQuery.Builder()
                .add(new TermQuery(new Term(M3Search.TEXT, text)), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term(M3Search.CONTEXT, context)), BooleanClause.Occur.FILTER)
                .build();
    }

    private static M3File diskSource(Path file, String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return M3File.read(file, StandardCharsets.UTF_8, 4096);
    }

    private static void assertStored(Document stored, M3File source, String context) {
        check(stored.get(M3Search.URI).equals(source.toUri().toASCIIString()), "stored source URI");
        check(stored.get(M3Search.CONTEXT).equals(context), "stored context identity");
        check(stored.get(M3Search.SOURCE_SHA256).equals(source.sourceSha256()), "stored source hash");
        check(Objects.equals(stored.get(M3Search.BYTE_SHA256), source.byteSha256()),
                "stored original byte hash");
        check(stored.getField(M3Search.UTF16_LENGTH).numericValue().intValue() == source.source().length(),
                "stored UTF-16 coordinate length");
        check(stored.get(M3Search.KIND).equals("source"), "stored document kind");
        check(stored.get(M3Search.KEY).equals(M3Search.sourceTerm(source, context).text()),
                "document and replacement term agree");
        check(stored.getFields(M3Search.TEXT).length == 0, "full source text is not stored again");
    }

    private static void diskSearch(Path work) throws Exception {
        String contextA = M3FileSourceFacts.contextSha256(List.of("fixture-project-A", "release=21"));
        String contextB = M3FileSourceFacts.contextSha256(List.of("fixture-project-B", "release=21"));
        Path sourcePath = work.resolve("Indexed.java");
        M3File original = diskSource(sourcePath, "scarlet original common");
        M3File revision = diskSource(sourcePath, "emerald revision common");
        M3File otherContext = diskSource(sourcePath, "cobalt alternate common");
        M3File otherUri = M3File.of(URI.create("mem:///src/Other.java"), "amber separate common");
        check(M3Search.sourceTerm(original, contextA).equals(M3Search.sourceTerm(revision, contextA)),
                "same source URI/context has a stable replacement key across edits");
        check(!M3Search.sourceTerm(revision, contextA).equals(M3Search.sourceTerm(otherContext, contextB)),
                "same URI in a different context has a separate replacement key");
        check(!M3Search.sourceTerm(revision, contextA).equals(M3Search.sourceTerm(otherUri, contextA)),
                "different source URIs have separate keys");
        Document input = M3Search.document(original, contextA);
        check(input.getFields(M3Search.TEXT).length == 1
                && !input.getField(M3Search.TEXT).fieldType().stored(),
                "exactly one non-stored text analysis field");
        Path indexPath = work.resolve("index");
        try (var directory = FSDirectory.open(indexPath); var analyzer = new StandardAnalyzer();
                var writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            writer.updateDocument(M3Search.sourceTerm(original, contextA), input);
            writer.commit();
        }
        try (var directory = FSDirectory.open(indexPath); var reader = DirectoryReader.open(directory)) {
            check(new IndexSearcher(reader).count(inContext("scarlet", contextA)) == 1,
                    "initial disk commit is searchable after reopening");
        }
        try (var directory = FSDirectory.open(indexPath); var analyzer = new StandardAnalyzer();
                var writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
            writer.updateDocument(M3Search.sourceTerm(revision, contextA), M3Search.document(revision, contextA));
            writer.updateDocument(M3Search.sourceTerm(revision, contextA), M3Search.document(revision, contextA));
            writer.updateDocument(M3Search.sourceTerm(otherContext, contextB),
                    M3Search.document(otherContext, contextB));
            writer.updateDocument(M3Search.sourceTerm(otherUri, contextA), M3Search.document(otherUri, contextA));
            writer.commit();
        }
        // Every writer, analyzer and previous directory handle is closed before this read.
        try (var directory = FSDirectory.open(indexPath); var reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            check(reader.numDocs() == 3 && searcher.count(new MatchAllDocsQuery()) == 3,
                    "repeated same-key updates replace rather than accumulate");
            check(searcher.count(new TermQuery(new Term(M3Search.TEXT, "scarlet"))) == 0,
                    "old revision has disappeared");
            check(searcher.count(inContext("emerald", contextA)) == 1, "new revision survives reopening");
            check(searcher.count(inContext("emerald", contextB)) == 0, "context query isolation");
            check(searcher.count(inContext("cobalt", contextB)) == 1, "other context retains its own revision");
            check(searcher.count(inContext("common", contextA)) == 2
                    && searcher.count(inContext("common", contextB)) == 1, "context groups remain separate");
            for (var item : List.of(new Object[] {revision, contextA},
                    new Object[] {otherContext, contextB}, new Object[] {otherUri, contextA})) {
                M3File source = (M3File) item[0];
                String context = (String) item[1];
                var hits = searcher.search(new TermQuery(M3Search.sourceTerm(source, context)), 2);
                check(hits.scoreDocs.length == 1, "one committed document per source/context");
                assertStored(searcher.storedFields().document(hits.scoreDocs[0].doc), source, context);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("one fresh work directory is required");
        Path work = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(work);
        try (var existing = Files.list(work)) {
            if (existing.findAny().isPresent()) throw new IllegalArgumentException("work directory must be empty");
        }
        digestVectors();
        fileSnapshots(work);
        borrowedIngress();
        diskSearch(work);
        System.out.println("M3_SOURCE_SEARCH_PASS checks=" + checks + " committedDocuments=3");
    }
}
