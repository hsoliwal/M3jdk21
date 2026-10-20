// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A traversal of {@link M3File} entries over the lanes of one {@link M3FileTree}.
 *
 * <p>Implementations supply two methods: the owning {@link #tree()} and an id-level
 * {@link #idSpliterator()}. Everything else is a default method: ids stay ints until a caller asks
 * for an entry, and entries are facades from {@link M3FileTree#file(int)} (DOOP). Encounter order is
 * the id spliterator's order; parallel streams split it.</p>
 */
public interface M3FileTraversal {
    /** Tree whose lanes back this traversal. */
    M3FileTree tree();

    /** Fresh id-level spliterator; each call starts a new traversal. */
    Spliterator.OfInt idSpliterator();

    /** Row ids, no facades. */
    default IntStream ids() {
        return StreamSupport.intStream(idSpliterator(), false);
    }

    /** Facade spliterator with the id spliterator's characteristics. */
    default Spliterator<M3File> spliterator() {
        return new M3FileSpliterator(tree(), idSpliterator());
    }

    default Stream<M3File> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    default Stream<M3File> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    default <R> Stream<R> map(Function<? super M3File, ? extends R> mapper) {
        return stream().map(mapper);
    }

    default void forEach(Consumer<? super M3File> action) {
        spliterator().forEachRemaining(action);
    }

    default <R, A> R collect(Collector<? super M3File, A, R> collector) {
        return stream().collect(collector);
    }

    default List<M3File> toList() {
        return stream().toList();
    }

    /** Count without facades; exact without traversal when the id spliterator is SIZED. */
    default long count() {
        return ids().count();
    }

    /** Cold: materialised paths. */
    default Stream<Path> paths() {
        return stream().map(M3File::toPath);
    }

    default Optional<M3File> first() {
        return stream().findFirst();
    }
}
