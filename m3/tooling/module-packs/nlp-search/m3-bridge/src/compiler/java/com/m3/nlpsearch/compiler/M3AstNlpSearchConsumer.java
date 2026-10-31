/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.nlpsearch.compiler;

import com.m3.nlpsearch.M3NlpSearch;
import com.sun.source.util.M3AST;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import opennlp.tools.util.Span;
import org.apache.lucene.index.IndexWriter;

/**
 * Consumes the actual javac PARSED event through its existing M3AST projection.
 * The optional compiler module requires a supplied M3 compiler, not an API stub.
 */
public final class M3AstNlpSearchConsumer implements M3AST.Consumer {
    private final M3AST.Projection<Annotations> projection;
    private final Consumer<? super Annotations> sink;

    /** Detached source identity and token coordinates; never retains source text or compiler objects. */
    public record Annotations(URI sourceUri, String utf16Sha256, int utf16Length,
            List<Span> tokens) {
        public Annotations {
            Objects.requireNonNull(sourceUri, "sourceUri");
            Objects.requireNonNull(utf16Sha256, "utf16Sha256");
            if (utf16Length < 0) throw new IllegalArgumentException("negative UTF-16 length");
            tokens = List.copyOf(tokens);
            for (Span token : tokens) {
                Objects.checkFromToIndex(token.getStart(), token.getEnd(), utf16Length);
            }
        }
    }

    /**
     * Reusing this same operation-key instance across consumers memoizes source/index work
     * within one PARSED event only. Sink delivery remains per consumer.
     */
    public M3AstNlpSearchConsumer(M3AST.Projection<Annotations> projection,
            Consumer<? super Annotations> sink) {
        this.projection = Objects.requireNonNull(projection, "projection");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /** Create a projection without a caller checkpoint; existing interruption checks remain. */
    public static M3AST.Projection<Annotations> projection(IndexWriter writer) {
        return projection(writer, null);
    }

    /**
     * Read Unit.source once, annotate its exact text, verify its source seal, and index it.
     * The caller owns the writer and transaction policy. A sink failure after a successful
     * update does not undo that pending write; M3AST retains its ordinary failure behavior.
     */
    public static M3AST.Projection<Annotations> projection(IndexWriter writer,
            Runnable checkpoint) {
        Objects.requireNonNull(writer, "writer");
        return new M3AST.Projection<>("com.m3.nlpsearch.parsed-source.v1", unit -> {
            if (unit.phase() != M3AST.Phase.PARSED) {
                throw new IllegalArgumentException("NLP/search projection requires a PARSED event");
            }
            M3AST.Source source = unit.source();
            Span[] tokens = M3NlpSearch.tokenSpans(source.text(), checkpoint);
            M3NlpSearch.update(writer, source.uri(), source.text(), source.utf16Sha256());
            return new Annotations(source.uri(), source.utf16Sha256(),
                    source.text().length(), List.of(tokens));
        });
    }

    @Override
    public void parsed(M3AST.Unit unit) throws Exception {
        sink.accept(unit.project(projection));
    }
}
