/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Results — Standardized result records: Outcome, Timed, Scored, Stats, Paged, Named, Versioned, Pair, Triple.
 *
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/Results.java}
 * blob {@code 0c1047d08e0e19c7d129304985b0eafa24fb5b99} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class Results {
    private Results() {}

    /** Success/failure with optional error. */
    public record Outcome<T>(T value, boolean success, String error) {
        public static <T> Outcome<T> ok(T v) { return new Outcome<>(v, true, null); }
        public static <T> Outcome<T> fail(String err) { return new Outcome<>(null, false, err); }
        public static <T> Outcome<T> fail(Throwable e) { return new Outcome<>(null, false, e != null ? e.getMessage() : "Unknown error"); }
        public static <T> Outcome<T> ofNullable(T v, String errIfNull) { return v != null ? ok(v) : fail(errIfNull); }
        public static <T> Outcome<T> tryGet(Supplier<T> s) { try { return ok(s.get()); } catch (Exception e) { return fail(e); } }
        public <U> Outcome<U> map(Function<T, U> f) { return success ? ok(f.apply(value)) : fail(error); }
        public T orElse(T def) { return success ? value : def; }
        public Optional<T> toOptional() { return success ? Optional.ofNullable(value) : Optional.empty(); }
    }

    /** Value with execution duration in ms. */
    public record Timed<T>(T value, long durationMs) {
        private static final long NANOS_PER_MS = 1_000_000;
        private static final double MS_PER_SEC = 1000.0;
        public static <T> Timed<T> measure(Supplier<T> s) { long t = System.currentTimeMillis(); T r = s.get(); return new Timed<>(r, System.currentTimeMillis() - t); }
        public static <T> Timed<T> measureNano(Supplier<T> s) { long t = System.nanoTime(); T r = s.get(); return new Timed<>(r, (System.nanoTime() - t) / NANOS_PER_MS); }
        public double durationSec() { return durationMs / MS_PER_SEC; }
        public <U> Timed<U> map(Function<T, U> f) { return new Timed<>(f.apply(value), durationMs); }
    }

    /** Item with score, sortable descending. */
    public record Scored<T>(T item, double score) implements Comparable<Scored<T>> {
        @Override public int compareTo(Scored<T> o) { return Double.compare(o.score, this.score); }
        public static <T> Scored<T> zero(T item) { return new Scored<>(item, 0.0); }
        public <U> Scored<U> map(Function<T, U> f) { return new Scored<>(f.apply(item), score); }
        public Scored<T> withScoreMultiplied(double m) { return new Scored<>(item, score * m); }
        public Scored<T> withScoreOffset(double o) { return new Scored<>(item, score + o); }
    }

    /** Count statistics for batch operations. */
    public record Stats(int total, int successful, int failed) {
        public static Stats empty() { return new Stats(0, 0, 0); }
        public static Stats of(int total, int ok) { return new Stats(total, ok, total - ok); }
        public double successRate() { return total > 0 ? (double) successful / total : 0.0; }
        public double failureRate() { return total > 0 ? (double) failed / total : 0.0; }
        public boolean allSucceeded() { return failed == 0 && successful == total; }
        public boolean hasFailed() { return failed > 0; }
        public Stats plus(Stats o) { return new Stats(total + o.total, successful + o.successful, failed + o.failed); }
    }

    /** Paginated list with metadata (0-indexed page). */
    public record Paged<T>(List<T> items, int page, int pageSize, long totalItems) {
        public Paged { items = items != null ? List.copyOf(items) : List.of(); }
        public int totalPages() { return pageSize > 0 ? (int) Math.ceil((double) totalItems / pageSize) : 0; }
        public boolean hasNext() { return page < totalPages() - 1; }
        public boolean hasPrevious() { return page > 0; }
        public boolean isFirst() { return page == 0; }
        public boolean isLast() { return page >= totalPages() - 1; }
        public boolean isEmpty() { return items == null || items.isEmpty(); }
        public int itemCount() { return items != null ? items.size() : 0; }
        public <U> Paged<U> map(Function<T, U> f) { return new Paged<>(items != null ? items.stream().map(f).toList() : List.of(), page, pageSize, totalItems); }
        public static <T> Paged<T> empty(int pageSize) { return new Paged<>(List.of(), 0, pageSize, 0); }
    }

    /** Value with name and optional description. */
    public record Named<T>(String name, T value, String description) {
        public static <T> Named<T> of(String name, T value) { return new Named<>(name, value, null); }
        public <U> Named<U> map(Function<T, U> f) { return new Named<>(name, f.apply(value), description); }
    }

    /** Value with version number and timestamp. */
    public record Versioned<T>(T value, long version, Instant timestamp) {
        public static <T> Versioned<T> of(T v, long ver) { return new Versioned<>(v, ver, Instant.now()); }
        public static <T> Versioned<T> initial(T v) { return of(v, 1L); }
        public Versioned<T> next(T nv) { return new Versioned<>(nv, version + 1, Instant.now()); }
        public <U> Versioned<U> map(Function<T, U> f) { return new Versioned<>(f.apply(value), version, timestamp); }
    }

    /** Simple pair of two values. */
    public record Pair<A, B>(A first, B second) {
        public static <A, B> Pair<A, B> of(A a, B b) { return new Pair<>(a, b); }
        public <C> Pair<C, B> mapFirst(Function<A, C> f) { return new Pair<>(f.apply(first), second); }
        public <C> Pair<A, C> mapSecond(Function<B, C> f) { return new Pair<>(first, f.apply(second)); }
        public Pair<B, A> swap() { return new Pair<>(second, first); }
    }

    /** Simple triple of three values. */
    public record Triple<A, B, C>(A first, B second, C third) {
        public static <A, B, C> Triple<A, B, C> of(A a, B b, C c) { return new Triple<>(a, b, c); }
        public Pair<A, B> toPair() { return new Pair<>(first, second); }
    }
}
