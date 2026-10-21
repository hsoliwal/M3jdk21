/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.nlpsearch;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import opennlp.tools.tokenize.M3Tokenizers;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.M3TextReaders;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

/**
 * Stable-text ingress into the existing Lucene and OpenNLP owners. Callers must
 * keep CharSequence backing immutable through tokenization, sealing and indexing.
 * No input toString/subSequence, parser, source store, or analyzer policy is added.
 */
public final class M3NlpSearch {
    public static final String SOURCE_URI_FIELD = "m3.source.uri";
    public static final String SOURCE_UTF16_SHA256_FIELD = "m3.source.utf16.sha256";
    public static final String SOURCE_UTF16_LENGTH_FIELD = "m3.source.utf16.length";
    public static final String TEXT_FIELD = "m3.source.text";

    private M3NlpSearch() { }

    /** Exact UTF-16-BE code-unit identity, including unpaired surrogate code units. */
    public static String utf16Sha256(CharSequence text) {
        Objects.requireNonNull(text, "text");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int length = text.length();
            for (int index = 0; index < length; index++) {
                char value = text.charAt(index);
                digest.update((byte) (value >>> 8));
                digest.update((byte) value);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 not available", unavailable);
        }
    }

    /** Delegate exact existing SimpleTokenizer UTF-16 spans without flattening text. */
    public static Span[] tokenSpans(CharSequence text) {
        return M3Tokenizers.simpleSpans(text);
    }

    /** Preserve the existing tokenizer checkpoint, interruption and configuration semantics. */
    public static Span[] tokenSpans(CharSequence text, Runnable checkpoint) {
        return M3Tokenizers.simpleSpans(text, checkpoint);
    }

    /**
     * Create a single-use borrowed Reader document with its computed exact source seal.
     * Index once or close the text field's reader; source text is not stored in the index.
     */
    public static Document document(URI uri, CharSequence text) {
        Objects.requireNonNull(uri, "uri");
        String seal = utf16Sha256(text);
        return createDocument(uri, text.length(), seal, M3TextReaders.reader(text));
    }

    /**
     * Verify the supplied canonical lowercase UTF-16-BE seal before creating a reader.
     * The supplied seal is never accepted as a byte-file SHA or without recomputation.
     */
    public static Document document(URI uri, CharSequence text, String expectedUtf16Sha256) {
        Objects.requireNonNull(uri, "uri");
        String seal = verifiedSeal(text, expectedUtf16Sha256);
        return createDocument(uri, text.length(), seal, M3TextReaders.reader(text));
    }

    /**
     * Update one current source document by its exact, unnormalized URI string.
     * Closes only this operation's reader. The caller owns IndexWriter lifecycle,
     * analyzer selection, commits and rollback, including failed compilations.
     */
    public static void update(IndexWriter writer, URI uri, CharSequence text,
            String expectedUtf16Sha256) throws IOException {
        Objects.requireNonNull(writer, "writer");
        Objects.requireNonNull(uri, "uri");
        String seal = verifiedSeal(text, expectedUtf16Sha256);
        try (Reader reader = M3TextReaders.reader(text)) {
            Document document = createDocument(uri, text.length(), seal, reader);
            writer.updateDocument(new Term(SOURCE_URI_FIELD, uri.toString()), document);
        }
    }

    private static String verifiedSeal(CharSequence text, String expectedUtf16Sha256) {
        Objects.requireNonNull(expectedUtf16Sha256, "expectedUtf16Sha256");
        String actual = utf16Sha256(text);
        if (!actual.equals(expectedUtf16Sha256)) {
            throw new IllegalArgumentException("UTF-16-BE source SHA-256 mismatch");
        }
        return actual;
    }

    private static Document createDocument(URI uri, int length, String seal, Reader reader) {
        Document document = new Document();
        document.add(new StringField(SOURCE_URI_FIELD, uri.toString(), Field.Store.YES));
        document.add(new StringField(SOURCE_UTF16_SHA256_FIELD, seal, Field.Store.YES));
        document.add(new StoredField(SOURCE_UTF16_LENGTH_FIELD, length));
        document.add(new TextField(TEXT_FIELD, reader));
        return document;
    }
}
