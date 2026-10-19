/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.nlp.search.test;

import com.m3.compiletime.ast.M3AST;
import com.m3.compiletime.ast.M3File;
import com.m3.compiletime.ast.M3FileSourceFacts;
import com.m3.text.LocalM3Arena;
import com.m3.text.M3StringPiece;
import com.synexia.mindex.MIndexInt8MatmulPlan;
import com.synexia.mindex.MIndexTensorBackend;
import com.synexia.mindex.MIndexTensorRuntime;
import com.synexia.mindex.ast.MIndexASTLimits;
import com.synexia.mindex.ast.MIndexASTPool;
import com.synexia.mindex.compiler.ast.MIndexJavaAST;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import jdk.m3.nlp.search.LuceneSourceFields;
import jdk.m3.nlp.search.SourceDocument;
import jdk.m3.nlp.search.SourceSpans;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/** Exercises real disk search, the original AST pool, and source-sharing analysis. */
public final class SourceIntegrationProbe {
    private SourceIntegrationProbe() { }

    public static void main(String[] args) throws Exception {
        Path scratch = Path.of(args[0]);
        Files.createDirectories(scratch);
        require(MIndexTensorRuntime.forBackend("m3-source-probe").backendId().equals("m3-source-probe"),
                "Named packaging lost the original external backend service contract");
        MIndexInt8MatmulPlan tensor = MIndexInt8MatmulPlan.compile(new byte[] {2, 3}, 2, 1);
        require(Arrays.equals(MIndexTensorRuntime.forBackend("m3-source-probe")
                .multiply(tensor, new byte[] {4, 5}, 1, new int[] {7}), new int[] {30}),
                "External backend lost the existing exact integer execution contract");
        String text = "class Example { String message = \"Hello \ud83d\ude00 world\"; "
                + "int value = 21; int answer() { return value + 21; } }\n";
        MIndexASTPool pool = new MIndexASTPool(MIndexASTLimits.defaults());
        M3AST ast = new M3AST(MIndexJavaAST.parse("Example.java", text, pool, 21));
        Path file = scratch.resolve("Example.java");
        Files.writeString(file, text, StandardCharsets.UTF_8);
        M3File input = M3File.read(file, StandardCharsets.UTF_8, 4096);
        SourceDocument fromFile = SourceDocument.of(input, ast);
        require(fromFile.text() == input.source(), "M3File snapshot owner lost");
        require(fromFile.ast() == ast, "Existing AST owner lost");
        require(fromFile.byteSha256().equals(input.byteSha256()), "Original byte digest lost");
        require(fromFile.sourceSha256().equals(input.sourceSha256()), "Source digest changed");
        require(!fromFile.sourceSha256().equals(fromFile.byteSha256()), "Byte/source identity conflated");
        expect(IllegalArgumentException.class, () -> SourceDocument.of(
                M3File.of(URI.create("mem:///Other.java"), "class Other {}"), ast));

        LocalM3Arena arena = new LocalM3Arena();
        int seam = text.indexOf('\ud83d') + 1;
        M3StringPiece composite = M3StringPiece.join(
                arena.copyUtf16(text.substring(0, seam).toCharArray()),
                arena.copyUtf16(text.substring(seam).toCharArray()));
        SourceDocument fromM3 = SourceDocument.of("m3:Example.java", composite, ast);
        require(fromM3.text() == composite, "M3 immutable piece was replaced");
        require(fromM3.sourceSha256().equals(input.sourceSha256()), "Composite digest differs");
        require(fromM3.openNlpDocument().text() == composite, "OpenNLP flattened the M3 source");

        SourceDocument guarded = SourceDocument.ofImmutableView("guard:Example.java", noFlatten(text), ast);
        require(guarded.openNlpDocument().text() == guarded.text(), "OpenNLP view identity changed");
        SourceSpans spans = SourceSpans.simpleTokens(guarded);
        Span[] expected = SimpleTokenizer.INSTANCE.tokenizePos(text);
        compare(spans, expected, text);
        for (int i = 0; i < spans.size(); i++) {
            require(spans.astOccurrence(i) == bruteForce(ast, spans.start(i), spans.end(i)),
                    "Token lost its smallest containing AST occurrence");
        }
        Random random = new Random(0x4d334f4646534554L);
        for (int i = 0; i < 2_000; i++) {
            int start = random.nextInt(text.length() + 1);
            int end = start + random.nextInt(text.length() - start + 1);
            require(guarded.astOccurrence(start, end) == bruteForce(ast, start, end),
                    "AST interval lookup differs from exact occurrence scan");
            require(guarded.view(start, end).toString().equals(text.substring(start, end)),
                    "Source range changed UTF-16 coordinates");
        }
        expect(IndexOutOfBoundsException.class, () -> guarded.view(-1, 0));
        expect(IndexOutOfBoundsException.class, () -> guarded.astOccurrence(0, text.length() + 1));
        expect(IllegalArgumentException.class, () -> SourceSpans.of(guarded, new Span[] {
                new Span(2, 4), new Span(1, 2)}));
        expect(IllegalArgumentException.class, () -> LuceneSourceFields.document(
                guarded, LuceneSourceFields.SOURCE_ID));

        try (FSDirectory directory = FSDirectory.open(scratch.resolve("lucene-index"));
             StandardAnalyzer analyzer = new StandardAnalyzer()) {
            try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
                writer.addDocument(LuceneSourceFields.document(guarded, "source"));
                writer.commit();
            }
            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                var hits = searcher.search(new TermQuery(new Term("source", "hello")), 5);
                require(hits.totalHits.value() == 1, "Real Lucene query did not find source");
                int document = hits.scoreDocs[0].doc;
                var stored = reader.storedFields().document(document);
                require(stored.get(LuceneSourceFields.SOURCE_ID).equals(guarded.id()), "Source id lost");
                require(stored.get(LuceneSourceFields.SOURCE_SHA256).equals(guarded.sourceSha256()),
                        "Stored source digest differs");
                require(stored.get(LuceneSourceFields.AST_FINGERPRINT).equals(ast.fingerprint()),
                        "Stored AST fingerprint differs");
                var terms = reader.termVectors().get(document, "source");
                require(terms != null && terms.hasPositions() && terms.hasOffsets(),
                        "Term vectors lost positions or offsets");
                var iterator = terms.iterator();
                require(iterator.seekExact(new BytesRef("hello")), "Term vector missing hello");
                PostingsEnum postings = iterator.postings(null, PostingsEnum.ALL);
                require(postings.nextDoc() != PostingsEnum.NO_MORE_DOCS, "No term positions");
                postings.nextPosition();
                int start = postings.startOffset();
                int end = postings.endOffset();
                require(start == text.indexOf("Hello") && end == start + 5,
                        "Lucene changed source UTF-16 coordinates");
                int occurrence = guarded.astOccurrence(start, end);
                require(occurrence >= 0 && ast.kind(occurrence).equals("STRING_LITERAL"),
                        "Search hit did not map to original AST literal");
            }
        }
        for (int iteration = 0; iteration < 300; iteration++) {
            char[] chars = new char[random.nextInt(256)];
            for (int i = 0; i < chars.length; i++) chars[i] = (char) random.nextInt(65_536);
            String oracle = new String(chars);
            SourceDocument source = SourceDocument.ofImmutableView("random:" + iteration, noFlatten(oracle));
            compare(SourceSpans.simpleTokens(source), SimpleTokenizer.INSTANCE.tokenizePos(oracle), oracle);
            require(source.sourceSha256().equals(M3FileSourceFacts.sourceSha256(oracle)), "Random digest changed");
            require(source.astOccurrence(0, oracle.length()) == -1, "AST invented for plain source");
        }
        System.out.println("SourceIntegrationProbe PASS: M3File/M3String/M3AST sharing; 2000 AST span oracles; "
                + "300 Unicode token oracles; disk indexing/query/term-vector offsets");
    }

    private static void compare(SourceSpans actual, Span[] expected, String text) {
        require(actual.size() == expected.length, "Token count changed");
        int[] starts = actual.starts();
        int[] ends = actual.ends();
        for (int i = 0; i < expected.length; i++) {
            require(actual.start(i) == expected[i].getStart() && actual.end(i) == expected[i].getEnd(),
                    "OpenNLP token coordinates differ");
            require(actual.view(i).toString().equals(text.substring(starts[i], ends[i])), "Token text differs");
        }
        Arrays.fill(starts, -1);
        Arrays.fill(ends, -1);
        if (actual.size() != 0) require(actual.start(0) >= 0, "Caller mutated retained span plane");
    }

    /** Test-only external service; the test driver supplies its ordinary META-INF/services entry. */
    public static final class ExternalBackend implements MIndexTensorBackend {
        public ExternalBackend() { }
        @Override public String id() { return "m3-source-probe"; }
        @Override public boolean available() { return true; }
        @Override public int[] multiplyInt8(MIndexInt8MatmulPlan plan, byte[] left, int rows, int[] bias) {
            return plan.multiply("java", left, rows, bias);
        }
    }

    private static int bruteForce(M3AST ast, int start, int end) {
        int winner = -1;
        int width = Integer.MAX_VALUE;
        for (int i = 0; i < ast.occurrenceCount(); i++) {
            int lower = ast.start(i);
            int upper = ast.end(i);
            if (lower < 0 || upper < lower || lower > start || upper < end) continue;
            int candidate = upper - lower;
            if (winner < 0 || candidate < width) { winner = i; width = candidate; }
        }
        return winner;
    }

    private static CharSequence noFlatten(String text) {
        return new CharSequence() {
            @Override public int length() { return text.length(); }
            @Override public char charAt(int index) { return text.charAt(index); }
            @Override public CharSequence subSequence(int start, int end) {
                throw new AssertionError("Source owner must not be asked to slice");
            }
            @Override public String toString() { throw new AssertionError("Whole source was flattened"); }
        };
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> expected, Runnable operation) {
        try { operation.run(); }
        catch (Throwable actual) {
            if (expected.isInstance(actual)) return;
            throw new AssertionError("Unexpected failure", actual);
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }
}
