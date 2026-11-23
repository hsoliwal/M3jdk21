/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.fs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * One directory or archive entry, with immutable encountered metadata and lazy,
 * owner-scoped content. This is the preferred entry API for M3FS traversal.
 * The existing {@link MFile} implementation implements this interface directly;
 * the stream factories reuse {@link MFiles} and do not wrap entry objects.
 *
 * <pre>{@code
 * try (Stream<String> names = M3File.stream(root,
 *         entry -> entry.isFile() && entry.name().endsWith(".txt"),
 *         M3File::relativeName)) {
 *     names.forEach(System.out::println);
 * }
 * }</pre>
 *
 * <p>Always close the traversal, including after short-circuit operations.
 * Metadata survives close; content access requires its owner to remain open.
 * Traversal requests metadata and opens payload streams only on explicit content
 * operations. Provider resolution and archive mounting may themselves perform I/O.
 * This is not a filesystem snapshot. A directory root is excluded, and a file
 * root is emitted. Local archive paths remain files unless an explicit provider
 * address or {@link MFiles.Plan#enterArchives(Predicate, int)} selects expansion.
 * Output filtering does not prune directories; use {@link MFiles.Plan#descendInto}
 * for pruning. Provider encounter order and errors are preserved.</p>
 */
public interface M3File extends MFileContent {
    /** Returns the decoded final name component. */
    String name();

    /**
     * Returns the display path relative to the traversal root, using {@code /}.
     * Automatic archive boundaries use {@code !/}; literal names can collide
     * with this notation. Use {@link #sourceAddress()} for provider identity.
     */
    String relativeName();

    /** Returns the provider URI captured when encountered. */
    URI uri();

    /** Returns the resolvable provider address, potentially containing caller credentials. */
    String sourceAddress();

    /** Returns whether the provider permits content. */
    boolean isFile();

    /** Returns whether the provider permits children. */
    boolean isDirectory();

    /** Returns the provider's symbolic-link classification. */
    boolean isSymbolicLink();

    /** Returns the captured size, or empty for unknown size or an entry without content. */
    OptionalLong size();

    /** Opens an owned traversal of a local or non-default NIO path. */
    static Stream<M3File> stream(Path root) throws IOException {
        return MFiles.stream(root).map(entry -> entry);
    }

    /** Opens an owned provider traversal, for example {@code zip:file:///tmp/data.zip!/}. */
    static Stream<M3File> stream(String providerAddress) throws IOException {
        return MFiles.stream(providerAddress).map(entry -> entry);
    }

    /** Opens an owned traversal using an existing immutable provider/traversal plan. */
    static Stream<M3File> stream(MFiles.Plan plan) throws IOException {
        return Objects.requireNonNull(plan, "plan").stream().map(entry -> entry);
    }

    /** Filters entries before applying the lazy transformer; close the returned stream. */
    static <R> Stream<R> stream(Path root, Predicate<? super M3File> filter,
            Function<? super M3File, ? extends R> transform) throws IOException {
        return MFiles.stream(root, filter, transform);
    }

    /** Filters provider entries before applying the lazy transformer. */
    static <R> Stream<R> stream(String providerAddress, Predicate<? super M3File> filter,
            Function<? super M3File, ? extends R> transform) throws IOException {
        return MFiles.stream(providerAddress, filter, transform);
    }

    /** Adds an output filter and lazy transformer to an existing immutable plan. */
    static <R> Stream<R> stream(MFiles.Plan plan, Predicate<? super M3File> filter,
            Function<? super M3File, ? extends R> transform) throws IOException {
        Objects.requireNonNull(transform, "transform");
        return Objects.requireNonNull(plan, "plan").filter(filter).stream().map(transform);
    }
}
