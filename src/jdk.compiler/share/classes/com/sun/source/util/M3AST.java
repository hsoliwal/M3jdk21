/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.sun.source.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.m3.M3ASTPC;
import com.sun.tools.javac.m3.M3ASTTextFacts;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import javax.lang.model.element.TypeElement;

/**
 * An opt-in, compilation-owned view of javac's existing frontend work.
 *
 * <p>This compiler API does not parse, attribute, or persist a second tree.
 * Consumers run serially at the end of each parse or type-analysis event. A
 * {@link Unit} is valid only on the callback thread and only until those
 * callbacks return. Retain an immutable projection, not a Unit, Tree, Trees,
 * Element, TypeMirror, or compiler task, for subsequent or out-of-process use.
 *
 * <p>A projection key has identity semantics and is memoized only within one
 * event. Different types, phases, compilation tasks, and source generations
 * never share projected values automatically. A source digest is not a key
 * for reusing attributed facts across classpaths, toolchains, or processors.
 * Existing M3 file/AST cache providers own any bounded persistent snapshots.
 * An enabled handle seals the exact character input once per parse, retaining
 * only URI/digest metadata after the parse callbacks; later source requests
 * reject edits made since parsing.
 *
 * <p>Register all consumers before starting the task. Closing this handle
 * removes its listener and releases compiler references. Task.call() closes
 * it at the COMPILATION event; callers using parse(), analyze(), and generate()
 * separately must close it themselves. Such closure reports completed=false
 * because those methods do not emit COMPILATION events.
 */
public interface M3AST extends AutoCloseable {
    /** Obtain the single M3 handle owned by this javac task. */
    static M3AST open(JavacTask task) {
        return M3ASTPC.open(task, false);
    }

    /**
     * Obtain the handle, optionally retaining comments/end positions and
     * disabling parser string folding before parsing starts. Lossless tree
     * conversion additionally requires a compatible converter and type context.
     */
    static M3AST open(JavacTask task, boolean retainSyntax) {
        return M3ASTPC.open(task, retainSyntax);
    }

    /**
     * Whether this task's already-parsed options select the builtin M3AST plugin.
     * This query creates no M3 handle, listener, compiler or source work. It
     * reports selection, not successful plugin initialization or compilation.
     * Call on the task's owning thread while its compiler context is available.
     *
     * @param task the original javac task whose options have been processed
     * @return whether the existing plugin options select the exact name M3AST
     * @throws NullPointerException if task is null
     * @throws IllegalArgumentException if task is not a javac implementation
     * @throws IllegalStateException if the task context has been released or
     *         its parsed arguments are unavailable
     */
    static boolean isPluginSelected(JavacTask task) {
        return M3ASTPC.isPluginSelected(task);
    }

    /** Register a consumer once, by object identity, before the first event. */
    void addConsumer(Consumer consumer);

    /** Obtain counters; these are event/work counts, not performance claims. */
    Summary summary();

    /**
     * Idempotently release this session on the task thread. If called inside
     * a callback, invalidates its view and stops dispatch to later consumers.
     */
    @Override void close();

    /** The currently valid compiler facts. */
    enum Phase { PARSED, ANALYZED }

    /** A projection of this exact live event. */
    @FunctionalInterface
    interface Factory<T> {
        T create(Unit unit) throws Exception;
    }

    /**
     * A caller-owned reusable operation identity. Reuse the same instance to
     * share an operation among consumers. Equal names alone do not share work.
     */
    final class Projection<T> {
        private final String name;
        private final Factory<T> factory;

        public Projection(String name, Factory<T> factory) {
            this.name = Objects.requireNonNull(name, "name");
            this.factory = Objects.requireNonNull(factory, "factory");
        }

        public String name() { return name; }

        /** Called by the compilation owner after memoization admission. */
        public T create(Unit unit) throws Exception { return factory.create(unit); }
    }

    /**
     * Retained character source. The hash covers exact UTF-16 code units in
     * big-endian order, including unpaired surrogates; it is not a byte-file hash.
     */
    record Source(URI uri, String text, String utf16Sha256) {
        public Source {
            Objects.requireNonNull(uri, "uri");
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(utf16Sha256, "utf16Sha256");
        }
    }

    /**
     * Fixed-size lexical geometry of one exact source range.
     *
     * <p>On M3JDK the values are projected from canonical M3 String facts without rescanning the
     * range. On a stock-JDK parser qualification they are produced by the exact compatibility
     * scanner. Signals are candidate filters only; exact String operations remain authoritative.</p>
     */
    record TextFacts(
            int utf16Length,
            int utf8Length,
            int modifiedUtf8Length,
            int codePointCount,
            int unpairedSurrogateCount,
            int javaHash,
            int hash31Power,
            long bitSignal64,
            boolean ascii,
            boolean latin1,
            int asciiUpperHash,
            int asciiLowerHash,
            int asciiTitleHash,
            long prefix4,
            long suffix4,
            long bigramSignal64,
            long trigramSignal64,
            int trimStart,
            int trimEnd,
            int stripStart,
            int stripEnd,
            long ownerStructuralHash64,
            long coordinate,
            boolean m3Backed,
            boolean signalsAvailable) {

        public boolean mayContainUnits(long requiredSignal64) {
            return !signalsAvailable
                    || (bitSignal64 & requiredSignal64) == requiredSignal64;
        }

        public boolean mayContainBigrams(long requiredSignal64) {
            return !signalsAvailable
                    || (bigramSignal64 & requiredSignal64) == requiredSignal64;
        }

        public boolean mayContainTrigrams(long requiredSignal64) {
            return !signalsAvailable
                    || (trigramSignal64 & requiredSignal64) == requiredSignal64;
        }

        private static TextFacts fromInternal(M3ASTTextFacts.Facts facts) {
            return new TextFacts(
                    facts.utf16Length(),
                    facts.utf8Length(),
                    facts.modifiedUtf8Length(),
                    facts.codePointCount(),
                    facts.unpairedSurrogateCount(),
                    facts.javaHash(),
                    facts.hash31Power(),
                    facts.bitSignal64(),
                    facts.ascii(),
                    facts.latin1(),
                    facts.asciiUpperHash(),
                    facts.asciiLowerHash(),
                    facts.asciiTitleHash(),
                    facts.prefix4(),
                    facts.suffix4(),
                    facts.bigramSignal64(),
                    facts.trigramSignal64(),
                    facts.trimStart(),
                    facts.trimEnd(),
                    facts.stripStart(),
                    facts.stripEnd(),
                    facts.ownerStructuralHash64(),
                    facts.coordinate(),
                    facts.m3Backed(),
                    facts.signalsAvailable());
        }
    }

    /** A scoped reference to the actual compiler tree, with lazy projections. */
    interface Unit {
        Phase phase();
        CompilationUnitTree tree();
        Trees trees();
        /** The analyzed top-level type, or null for a PARSED event. */
        TypeElement analyzedType();
        /** Unique in-memory compilation identity; not a portable semantic key. */
        Object contextIdentity();
        /** Uses the exact parsed source at PARSE; later reads are checked against its source seal. */
        Source source() throws IOException;

        /**
         * Lexical facts for an exact half-open UTF-16 range of the sealed parsed source.
         *
         * <p>The default keeps third-party Unit implementations source-compatible. M3JDK reuses
         * M3 String facts through the internal shared-secret; stock JDKs use the exact fallback.</p>
         */
        default TextFacts textFacts(int beginIndex, int endIndex) throws IOException {
            Source retained = source();
            return TextFacts.fromInternal(
                    M3ASTTextFacts.of(retained.text(), beginIndex, endIndex));
        }

        /**
         * Compute once per identity and event. Null and thrown exceptions are
         * memoized too. Recursive requests for the same key are refused.
         */
        <T> T project(Projection<T> projection) throws Exception;
    }

    /** Service-provider or explicitly registered frontend consumer. */
    interface Consumer {
        default void parsed(Unit unit) throws Exception { }
        default void analyzed(Unit unit) throws Exception { }
        default void finished(Summary summary) throws Exception { }
    }

    /** Immutable, per-compilation operation counts and completion disposition. */
    record Summary(long parsedUnits, long analyzedTypes, long capturedSources, long sourceReads,
                   long projectionComputations, long projectionHits,
                   int compilerErrors, boolean completed, boolean closed,
                   boolean consumerFailed) { }
}
