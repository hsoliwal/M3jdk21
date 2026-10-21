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

/*
 * Modified 2026-10-21 by Synexia (Hitesh Soliwal and contributors): added
 * immutable, explicitly partial build facts from the original M3AST event.
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.m3.M3ASTPC;
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

    /**
     * Project partial build facts from this original ANALYZED event. Consumers
     * share one result by using this entry point; no parsing, attribution or
     * source reread is performed. The result never certifies a complete build
     * dependency graph or authorizes skipping a compiler or plugin execution.
     *
     * @throws IllegalStateException if the unit is not a live ANALYZED event
     * @throws IllegalArgumentException if the unit is not owned by this compiler
     * @throws Exception if the event projection cannot be captured
     */
    static BuildFacts buildFacts(Unit unit) throws Exception {
        return M3ASTPC.buildFacts(Objects.requireNonNull(unit, "unit"));
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
     * Detached facts for one analyzed top-level type and its visited nested,
     * local and anonymous declarations. Names use JVM binary-name spelling.
     * Resolved member and constant uses include the declaring type even when
     * javac can inline the value into bytecode. Same-event declared names are
     * omitted from referenced names. Both name lanes are sorted and unique.
     *
     * <p>The source seal is the original parser's exact UTF-16 generation, not
     * a filesystem byte hash or a portable semantic-context key. These facts
     * omit negative/wildcard lookup dependencies, processor inputs, output
     * ownership and plugin effects. They describe an analysis event, including
     * a possible failed compilation, rather than its successful publication.
     * Other top-level types in the source have separate analysis events.
     */
    record BuildFacts(URI sourceUri, int sourceLength, String sourceUtf16Sha256,
                      String analyzedBinaryName, String[] declaredBinaryNames,
                      String[] referencedBinaryNames, boolean hasUnresolvedSymbols) {
        public static final String SCHEMA = "M3AST_BUILD_FACTS_V1";

        public BuildFacts {
            Objects.requireNonNull(sourceUri, "sourceUri");
            Objects.requireNonNull(sourceUtf16Sha256, "sourceUtf16Sha256");
            Objects.requireNonNull(analyzedBinaryName, "analyzedBinaryName");
            if (sourceLength < 0 || sourceUtf16Sha256.length() != 64
                    || analyzedBinaryName.isEmpty())
                throw new IllegalArgumentException("invalid build fact source identity");
            for (int i = 0; i < sourceUtf16Sha256.length(); i++) {
                char c = sourceUtf16Sha256.charAt(i);
                if (!(c >= '0' && c <= '9') && !(c >= 'a' && c <= 'f'))
                    throw new IllegalArgumentException("invalid build fact source digest");
            }
            declaredBinaryNames = names(declaredBinaryNames);
            referencedBinaryNames = names(referencedBinaryNames);
        }

        @Override public String[] declaredBinaryNames() { return declaredBinaryNames.clone(); }
        @Override public String[] referencedBinaryNames() { return referencedBinaryNames.clone(); }

        /** This event projection deliberately cannot certify complete dependency coverage. */
        public boolean completeDependencyGraph() { return false; }

        private static String[] names(String[] input) {
            String[] values = Objects.requireNonNull(input, "binary names").clone();
            String previous = null;
            for (String value : values) {
                if (value == null || value.isEmpty()
                        || (previous != null && previous.compareTo(value) >= 0))
                    throw new IllegalArgumentException("noncanonical build fact names");
                previous = value;
            }
            return values;
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
