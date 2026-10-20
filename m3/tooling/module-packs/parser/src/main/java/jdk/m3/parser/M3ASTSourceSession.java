// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Objects;

/**
 * One original parser result, zero additional parser invocations, lazy indexed
 * projections. The caller provides an already parsed JDT/javac/NetBeans/Rewrite/
 * Gradle/JSON/C or C++ tree and a sealed M3File. This owner cannot parse text.
 *
 * <p>Only one derived primitive M3ASTIndex is retained. The original frontend
 * reference is released immediately after the projection succeeds or fails.
 * Multiple tools requesting the index receive exactly the same instance.
 *
 * <p>A supplied source/context generation label is an identity discriminator,
 * not proof of reusable semantic equivalence. Type bindings also depend on the
 * complete dependency/artifact/compiler-option/processor/OS environment.
 */
public final class M3ASTSourceSession<T> implements AutoCloseable {
    @FunctionalInterface
    public interface Projection<T> {
        /** Consume the *existing* original tree, never invoke a parser. */
        M3ASTIndex derive(M3File source, T original) throws Exception;
    }

    private final M3File source;
    private final String generation;
    private T original;
    private Projection<T> projection;
    private M3ASTIndex cached;
    private RuntimeException failure;
    private int computations;
    private boolean closed;

    private M3ASTSourceSession(M3File source, String generation,
                               T original, Projection<T> projection) {
        this.source = Objects.requireNonNull(source, "original M3File");
        if (generation == null || generation.isBlank()) {
            throw new IllegalArgumentException("explicit source + dependency context generation required");
        }
        this.generation = generation;
        this.original = Objects.requireNonNull(original, "original parsed tree");
        this.projection = Objects.requireNonNull(projection, "source-to-index projector");
    }

    public static <T> M3ASTSourceSession<T> fromParsed(M3File source, String generation,
                                                       T original, Projection<T> projection) {
        return new M3ASTSourceSession<>(source, generation, original, projection);
    }

    public M3File source() { return source; }
    public String generation() { return generation; }

    /**
     * Compute only on the first request. No clone, extra parse, serialized
     * node graph or repeat projection is permitted. Failures are memoized.
     */
    public synchronized M3ASTIndex index() {
        requireOpen();
        if (cached != null) return cached;
        if (failure != null) throw failure;
        Projection<T> action = projection;
        T tree = original;
        computations++;
        try {
            M3ASTIndex value = Objects.requireNonNull(action.derive(source, tree), "M3 index projection");
            if (value.source() != source) {
                throw new IllegalArgumentException("foreign source owner from parser projection");
            }
            cached = value;
            return value;
        } catch (Exception error) {
            failure = error instanceof RuntimeException runtime ? runtime
                    : new IllegalStateException("original AST projection failed", error);
            throw failure;
        } finally {
            original = null;
            projection = null;
        }
    }

    /** No AST is even projected when an indexed view is not requested. */
    public synchronized int projectionComputations() { return computations; }
    public synchronized boolean projected() { return cached != null; }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("M3 source generation is closed");
    }

    /** Drop all owned references, never close the caller's original parser. */
    @Override public synchronized void close() {
        closed = true;
        original = null;
        projection = null;
        cached = null;
        failure = null;
    }
}
