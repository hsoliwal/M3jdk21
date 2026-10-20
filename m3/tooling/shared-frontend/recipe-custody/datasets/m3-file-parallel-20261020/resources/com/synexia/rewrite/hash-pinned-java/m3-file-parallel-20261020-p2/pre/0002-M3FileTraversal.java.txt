// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
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

    /** {@code du -b}: bytes of the regular files, from the size lanes; no I/O. */
    default long bytes() {
        M3FileTree tree = tree();
        return ids().filter(id -> tree.flag(M3FileFlag.REGULAR, id)).mapToLong(id -> Math.max(0, tree.size(id))).sum();
    }

    /** {@code wc} over the regular files, one shadow pass each; progress units are bytes. */
    default M3FileWordCount wc(long maxBytes, M3FileProgress progress) throws InterruptedIOException {
        M3FileTree tree = tree();
        int[] files = ids().filter(id -> tree.flag(M3FileFlag.REGULAR, id)).toArray();
        progress.begin("wc", bytes());
        long lines = 0;
        long words = 0;
        long bytes = 0;
        long failed = 0;
        for (int id : files) {
            M3FileWcShadow counts = M3FileShadow.wc();
            if (observe(tree, id, maxBytes, progress, counts)) {
                lines += counts.lines();
                words += counts.words();
                bytes += counts.bytes();
            } else {
                failed++;
            }
        }
        progress.done();
        return new M3FileWordCount(files.length - failed, lines, words, bytes, failed);
    }

    /**
     * {@code grep}: lines of the regular text files matching {@code pattern} ({@link java.util.regex.Matcher#find}),
     * in traversal order; files containing NUL bytes are skipped as binary. One shadow pass per file.
     */
    default List<M3FileMatch> grep(Pattern pattern, long maxBytes, M3FileProgress progress) throws InterruptedIOException {
        Objects.requireNonNull(pattern, "pattern");
        M3FileTree tree = tree();
        int[] files = ids().filter(id -> tree.flag(M3FileFlag.REGULAR, id)).toArray();
        progress.begin("grep " + pattern, bytes());
        List<M3FileMatch> matches = new ArrayList<>();
        for (int id : files) {
            M3FileGrepShadow lines = new M3FileGrepShadow(tree.file(id), pattern);
            boolean[] binary = {false};
            M3FileShadow nulProbe = (block, offset, length) -> {
                for (int i = offset; i < offset + length && !binary[0]; i++) binary[0] = block[i] == 0;
            };
            if (observe(tree, id, maxBytes, progress, nulProbe, M3FileShadow.utf8(lines)) && !binary[0]) {
                matches.addAll(lines.matches());
            }
        }
        progress.done();
        return matches;
    }

    private static boolean observe(M3FileTree tree, int id, long maxBytes, M3FileProgress progress,
            M3FileShadow... shadows) throws InterruptedIOException {
        try {
            tree.file(id).observe(maxBytes, progress.split(Math.max(0, tree.size(id))), shadows);
            tree.clearFailure(id);
            return true;
        } catch (InterruptedIOException canceled) {
            throw canceled;
        } catch (IOException failed) {
            tree.fail(id, failed);
            return false;
        }
    }
}
