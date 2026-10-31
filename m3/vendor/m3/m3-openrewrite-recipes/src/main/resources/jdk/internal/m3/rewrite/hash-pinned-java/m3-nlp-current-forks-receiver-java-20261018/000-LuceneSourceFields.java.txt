/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.nlp.search;

import java.util.Objects;
import org.apache.lucene.analysis.M3TextReaders;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;

/** Lucene fields with source provenance and retrievable original UTF-16 term-vector offsets. */
public final class LuceneSourceFields {
    public static final String SOURCE_ID = "_m3_source_id";
    public static final String SOURCE_SHA256 = "_m3_source_utf16_sha256";
    public static final String BYTE_SHA256 = "_m3_source_byte_sha256";
    public static final String AST_FINGERPRINT = "_m3_ast_fingerprint";
    public static final String AST_SOURCE_SHA256 = "_m3_ast_source_sha256";
    private static final FieldType TEXT_TYPE;

    static {
        FieldType type = new FieldType();
        type.setTokenized(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        type.setStoreTermVectors(true);
        type.setStoreTermVectorPositions(true);
        type.setStoreTermVectorOffsets(true);
        type.freeze();
        TEXT_TYPE = type;
    }

    private LuceneSourceFields() { }

    /**
     * Create one Lucene document for one IndexWriter add/update operation. The caller owns the
     * Analyzer and source lifetime. Its tokenizer must report original UTF-16 offsets; a
     * transforming CharFilter must provide Lucene offset correction. Source characters are read
     * through Lucene's existing Reader owner, and are not duplicated into a stored text field.
     */
    public static Document document(SourceDocument source, String textField) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(textField, "textField");
        if (textField.isEmpty() || textField.startsWith("_m3_")) {
            throw new IllegalArgumentException("Text field must be nonempty and outside reserved _m3_ names");
        }
        Document document = new Document();
        document.add(new StringField(SOURCE_ID, source.id(), Field.Store.YES));
        document.add(new StringField(SOURCE_SHA256, source.sourceSha256(), Field.Store.YES));
        if (source.byteSha256() != null) {
            document.add(new StoredField(BYTE_SHA256, source.byteSha256()));
        }
        if (source.ast() != null) {
            document.add(new StoredField(AST_FINGERPRINT, source.ast().fingerprint()));
            document.add(new StoredField(AST_SOURCE_SHA256, source.ast().sourceSha256()));
        }
        document.add(new Field(textField, M3TextReaders.reader(source.text()), TEXT_TYPE));
        return document;
    }
}
