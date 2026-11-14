/* ******************************************************************************
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
/**
 * Core stream creation, coalesce, match, concat, and distinct operations.
 *
 * <p>
 * All methods are null-safe. For collection/mapping ops see
 * {@link StreamCollectors}, for aggregation see {@link StreamAggregates}, for
 * sequence ops see {@link StreamSequences}.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * import static jdk.internal.m3.collections.StreamOps.*;
 *
 * Stream<String> s = ofNonNull(a, b, c);
 * Optional<String> first = firstNonNull(a, b, c);
 * String result = coalesce("default", maybeNull1, maybeNull2);
 * }</pre>
 *
 * @since ContextOS 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/StreamOps.java}
 * blob {@code 492460eeb1f187bb12b3e488d1768721c36be3b5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StreamOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(StreamOps.class);


    private StreamOps() {}

    // --- VARARGS STREAM CREATION ---

    /** Create stream from varargs (null-safe). */
    @SafeVarargs
    public static <T> Stream<T> of(T... items) {
        if (items == null || items.length == 0) {
            return Stream.empty();
        }
        return IntStream.range(0, items.length).mapToObj(index -> items[index]);
    }

    /** Create stream from varargs, filtering nulls. */
    @SafeVarargs
    public static <T> Stream<T> ofNonNull(T... items) {
        if (items == null || items.length == 0) {
            return Stream.empty();
        }
        return IntStream.range(0, items.length).mapToObj(index -> items[index]).filter(Objects::nonNull);
    }

    /** Create stream from collection (null-safe). */
    public static <T> Stream<T> of(Collection<T> c) {
        return c == null ? Stream.empty() : c.stream();
    }

    /** Create stream from collection, filtering nulls. */
    public static <T> Stream<T> ofNonNull(Collection<T> c) {
        return of(c).filter(Objects::nonNull);
    }

    /** Create stream from iterable. */
    public static <T> Stream<T> of(Iterable<T> iterable) {
        if (iterable == null) return Stream.empty();
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /** Create stream from iterator. */
    public static <T> Stream<T> of(Iterator<T> iterator) {
        if (iterator == null) return Stream.empty();
        Iterable<T> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /** Create stream from optional. */
    public static <T> Stream<T> of(Optional<T> opt) {
        return opt == null ? Stream.empty() : opt.stream();
    }

    // --- FIRST/COALESCE ---

    /** Get first non-null from varargs. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> Optional<T> firstNonNull(T... items) {
        if (items == null) {
            return Optional.empty();
        }
        return Arrays.stream(items).filter(Objects::nonNull).findFirst();
    }

    /** Coalesce - return first non-null or default. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> T coalesce(T defaultVal, T... items) {
        if (items == null) {
            return defaultVal;
        }
        return Arrays.stream(items).filter(Objects::nonNull).findFirst().orElse(defaultVal);
    }

    /** Get first matching predicate. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> Optional<T> firstMatching(Predicate<T> pred, T... items) {
        if (items == null) {
            return Optional.empty();
        }
        return Arrays.stream(items).filter(pred).findFirst();
    }

    // --- ANY/ALL/NONE MATCH (varargs) ---

    /** Check if any items match predicate. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> boolean anyMatch(Predicate<T> pred, T... items) {
        if (items == null) {
            return false;
        }
        return Arrays.stream(items).anyMatch(pred);
    }

    /** Check if all items match predicate. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> boolean allMatch(Predicate<T> pred, T... items) {
        if (items == null) {
            return true;
        }
        return Arrays.stream(items).allMatch(pred);
    }

    /** Check if no items match predicate. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> boolean noneMatch(Predicate<T> pred, T... items) {
        if (items == null) {
            return true;
        }
        return Arrays.stream(items).noneMatch(pred);
    }

    /** Check if any items are null. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> boolean anyNull(T... items) {
        if (items == null) {
            return false;
        }
        return Arrays.stream(items).anyMatch(Objects::isNull);
    }

    /** Check if all items are null. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> boolean allNull(T... items) {
        if (items == null) {
            return true;
        }
        return Arrays.stream(items).allMatch(Objects::isNull);
    }

    /** Check if no items are null. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> boolean noneNull(T... items) {
        if (items == null) {
            return true;
        }
        return Arrays.stream(items).noneMatch(Objects::isNull);
    }

    // --- CONCATENATION ---

    /** Concatenate streams. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> Stream<T> concat(Stream<T>... streams) {
        if (streams == null) {
            return Stream.empty();
        }
        return Arrays.stream(streams).filter(Objects::nonNull).flatMap(Function.identity());
    }

    /** Concatenate collections to stream. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> Stream<T> concat(Collection<T>... collections) {
        if (collections == null) {
            return Stream.empty();
        }
        return Arrays.stream(collections).filter(Objects::nonNull).flatMap(Collection::stream);
    }

    // --- DISTINCT/UNIQUE ---

    /** Distinct by key extractor. */
    public static <T, K> Predicate<T> distinctByKey(Function<T, K> keyExtractor) {
        Set<K> seen = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        return t -> seen.add(keyExtractor.apply(t));
    }

    /** Get distinct items by key. */
    @SafeVarargs
    public static <T, K> List<T> distinctBy(Function<T, K> keyExtractor, T... items) {
        if (items == null || items.length == 0) {
            return StreamCollectors.toMutableList(Stream.empty());
        }
        return StreamCollectors.toMutableList(
                IntStream.range(0, items.length).mapToObj(index -> items[index]).filter(distinctByKey(keyExtractor)));
    }

    /**
     * Command-line interface for StreamOps utilities.
     *
     * <p>
     * Provides interactive access to core stream operations including creation,
     * coalesce, matching, and distinct operations.
     *
     * <h4>Usage</h4>
     *
     * <pre>{@code
     * java jdk.internal.m3.collections.StreamOps list
     * java jdk.internal.m3.collections.StreamOps coalesce "default" null "" "value"
     * java jdk.internal.m3.collections.StreamOps firstNonNull null "" "first" "second"
     * java jdk.internal.m3.collections.StreamOps anyNull "a" null "c"
     * java jdk.internal.m3.collections.StreamOps noneNull "a" "b" "c"
     * }</pre>
     *
     * @param args
     *            command-line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1); // FM-40: acceptable in CLI main()
        }

        try {
            String command = args[0].toLowerCase();
            switch (command) {
                case "list" -> printOperations();
                case "coalesce" -> {
                    if (args.length < 2) {
                        log.error("coalesce requires at least a default value");
                        System.exit(1);
                    }
                    String defaultVal = args[1];
                    String[] items = Arrays.copyOfRange(args, 2, args.length);
                    String result = coalesce(defaultVal, items);
                    log.info("Coalesce result: {}", result);
                }
                case "firstnonnull" -> {
                    if (args.length < 2) {
                        log.error("firstNonNull requires at least one item");
                        System.exit(1);
                    }
                    String[] items = Arrays.copyOfRange(args, 1, args.length);
                    Optional<String> result = firstNonNull(items);
                    log.info("First non-null: {}", result.orElse("(none)"));
                }
                case "anynull" -> {
                    if (args.length < 2) {
                        log.error("anyNull requires at least one item");
                        System.exit(1);
                    }
                    String[] items = Arrays.copyOfRange(args, 1, args.length);
                    boolean result = anyNull(items);
                    log.info("Any null: {}", result);
                }
                case "allnull" -> {
                    if (args.length < 2) {
                        log.error("allNull requires at least one item");
                        System.exit(1);
                    }
                    String[] items = Arrays.copyOfRange(args, 1, args.length);
                    boolean result = allNull(items);
                    log.info("All null: {}", result);
                }
                case "nonenull" -> {
                    if (args.length < 2) {
                        log.error("noneNull requires at least one item");
                        System.exit(1);
                    }
                    String[] items = Arrays.copyOfRange(args, 1, args.length);
                    boolean result = noneNull(items);
                    log.info("None null: {}", result);
                }
                default -> {
                    log.error("Unknown command: {}", command);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            log.error("Error executing command", e);
            System.exit(1);
        }
    }

    private static void printUsage() {
        log.info("Usage: java jdk.internal.m3.collections.StreamOps <command> [args...]");

        log.info("Commands:");
        log.info("  list                          - List all available operations");
        log.info("  coalesce <default> <items...> - Return first non-null or default");
        log.info("  firstNonNull <items...>       - Get first non-null item");
        log.info("  anyNull <items...>            - Check if any items are null");
        log.info("  allNull <items...>            - Check if all items are null");
        log.info("  noneNull <items...>           - Check if no items are null");
    }

    private static void printOperations() {
        log.info("StreamOps - Core stream creation and operations");

        log.info("Stream Creation:");
        log.info("  of(T...)                - Create stream from varargs (null-safe)");
        log.info("  ofNonNull(T...)         - Create stream filtering nulls");
        log.info("  of(Collection)          - Create stream from collection");
        log.info("  of(Iterable)            - Create stream from iterable");
        log.info("  of(Iterator)            - Create stream from iterator");
        log.info("  of(Optional)            - Create stream from optional");

        log.info("First/Coalesce:");
        log.info("  firstNonNull(T...)      - Get first non-null from varargs");
        log.info("  coalesce(T, T...)       - Return first non-null or default");
        log.info("  firstMatching(Pred, T...) - Get first matching predicate");

        log.info("Matching:");
        log.info("  anyMatch(Pred, T...)    - Check if any items match");
        log.info("  allMatch(Pred, T...)    - Check if all items match");
        log.info("  noneMatch(Pred, T...)   - Check if no items match");
        log.info("  anyNull(T...)           - Check if any items are null");
        log.info("  allNull(T...)           - Check if all items are null");
        log.info("  noneNull(T...)          - Check if no items are null");

        log.info("Concatenation:");
        log.info("  concat(Stream...)       - Concatenate streams");
        log.info("  concat(Collection...)   - Concatenate collections");

        log.info("Distinct:");
        log.info("  distinctByKey(Function) - Predicate for distinct by key");
        log.info("  distinctBy(Function, T...) - Get distinct items by key");
    }
}
