// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.search;

import java.io.Reader;
import java.util.List;
import java.util.Objects;
import opennlp.tools.tokenize.M3Tokenizers;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.M3TextReaders;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;

/**
 * Additive ingress to the original Apache engines. An immutable source owns the text;
 * token results retain only UTF-16 coordinates. Lucene's analyzer, codecs, index formats,
 * model ownership and ordinary String APIs keep their existing contracts.
 *
 * <p>On M3JDK, a String uses the existing internal java.lang.M3String owner. Optional
 * modules use its public String/CharSequence boundary. They never import that internal
 * type or create a second text interner. General CharSequence inputs are borrowed and
 * must remain stable through reader consumption or the complete tokenization call.</p>
 */
public final class M3Search {
    public static final String KEY = "_m3_key";
    public static final String URI = "_m3_uri";
    public static final String CONTEXT = "_m3_context_sha256";
    public static final String SOURCE_SHA256 = "_m3_source_sha256";
    public static final String BYTE_SHA256 = "_m3_byte_sha256";
    public static final String UTF16_LENGTH = "_m3_utf16_length";
    public static final String KIND = "_m3_kind";
    public static final String TEXT = "_m3_text";

    private M3Search() { }

    /** Borrow an immutable view; the original Lucene reader performs no flattening. */
    public static Reader reader(CharSequence source) {
        return M3TextReaders.reader(Objects.requireNonNull(source, "source"));
    }

    public static Reader reader(M3File source) {
        return reader(Objects.requireNonNull(source, "source").source());
    }

    public static Span[] spans(CharSequence source) { return spans(source, null); }

    /** Checkpoints are optional host cancellation/progress callbacks, not retained listeners. */
    public static Span[] spans(CharSequence source, Runnable checkpoint) {
        return M3Tokenizers.simpleSpans(Objects.requireNonNull(source, "source"), checkpoint);
    }

    public static Tokens tokens(M3File source) { return tokens(source, null); }

    /** Bind token offsets to the exact file snapshot that produced them. */
    public static Tokens tokens(M3File source, Runnable checkpoint) {
        Objects.requireNonNull(source, "source");
        return new Tokens(source, spans(source.source(), checkpoint));
    }

    /**
     * Build a caller-owned Lucene document. Text is analyzed from a reader and is not stored
     * a second time. URI, context, decoded-source digest and original-byte digest remain
     * separate fields. The caller owns IndexWriter, transactions, commits and durability.
     */
    public static Document document(M3File source, String contextSha256) {
        Document document = metadata(source, contextSha256, "source");
        document.add(new TextField(TEXT, reader(source)));
        return document;
    }

    /** Metadata common to source and compiler projection documents; no source text copy. */
    public static Document metadata(M3File source, String contextSha256, String kind) {
        Objects.requireNonNull(source, "source");
        return metadata(M3FileSourceFacts.capture(source.toUri().toASCIIString(), source.source(),
                contextSha256, "jdk.m3.search", 1, "SOURCE"), source.byteSha256(), kind);
    }

    /** Reuse precomputed source facts when emitting several metadata records for one source. */
    public static Document metadata(M3FileSourceFacts facts, String byteSha256, String kind) {
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(kind, "kind");
        if (byteSha256 != null) M3FileSourceFacts.requireSha256(byteSha256, "byteSha256");
        if (kind.isBlank()) throw new IllegalArgumentException("kind must not be blank");
        Document result = new Document();
        stored(result, KEY, sourceKey(facts.sourceUri(), facts.contextSha256()));
        stored(result, URI, facts.sourceUri());
        stored(result, CONTEXT, facts.contextSha256());
        stored(result, SOURCE_SHA256, facts.sourceSha256());
        if (byteSha256 != null) stored(result, BYTE_SHA256, byteSha256);
        stored(result, KIND, kind);
        result.add(new StoredField(UTF16_LENGTH, facts.utf16Length()));
        return result;
    }

    /** A stable replacement key for one source URI within one explicitly identified context. */
    public static Term sourceTerm(M3File source, String contextSha256) {
        return new Term(KEY, sourceKey(source, contextSha256));
    }

    private static String sourceKey(M3File source, String contextSha256) {
        Objects.requireNonNull(source, "source");
        return sourceKey(source.toUri().toASCIIString(), contextSha256);
    }

    private static String sourceKey(String sourceUri, String contextSha256) {
        M3FileSourceFacts.requireSha256(contextSha256, "contextSha256");
        return M3FileSourceFacts.contextSha256(
                List.of("M3_SEARCH_SOURCE_KEY_1", sourceUri, contextSha256));
    }

    private static void stored(Document document, String name, String value) {
        document.add(new StringField(name, value, Field.Store.YES));
    }

    /** Source-bound spans. Token Strings are materialized only when the caller requests a token. */
    public static final class Tokens {
        private final M3File source;
        private final Span[] spans;

        private Tokens(M3File source, Span[] ownedSpans) {
            this.source = source;
            this.spans = ownedSpans;
        }

        public M3File source() { return source; }
        public int size() { return spans.length; }
        public Span span(int index) { return spans[Objects.checkIndex(index, spans.length)]; }
        public CharSequence token(int index) {
            Span range = span(index);
            return source.source().subSequence(range.getStart(), range.getEnd());
        }
    }
}
