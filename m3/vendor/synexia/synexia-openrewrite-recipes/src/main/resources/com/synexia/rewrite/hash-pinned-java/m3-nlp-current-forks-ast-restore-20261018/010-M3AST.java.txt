// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import java.util.Objects;

/** M3-named source/syntax view over the existing scoped immutable MIndex document. */
public final class M3AST {
    private final MIndexASTDocument document;
    public M3AST(MIndexASTDocument document) {
        this.document = Objects.requireNonNull(document, "document");
    }
    /** Original scoped document and atom identities; no atom copy or second pool is introduced. */
    public MIndexASTDocument asMIndex() { return document; }
    public String source() { return document.source(); }
    public String sourceName() { return document.sourceName(); }
    public String sourceSha256() { return document.sourceUtf16Sha256(); }
    public String fingerprint() { return document.root().fingerprint(); }
    public int occurrenceCount() { return document.occurrences().size(); }
    public String kind(int occurrence) { return document.occurrences().get(occurrence).atom().kind().toString(); }
    public int start(int occurrence) { return document.occurrences().get(occurrence).start(); }
    public int end(int occurrence) { return document.occurrences().get(occurrence).end(); }
}
