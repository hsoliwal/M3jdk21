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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/**
 * Stream collection, mapping, parallel, partitioning, and grouping operations.
 *
 * @since ContextOS 1.0
 * @see StreamOps
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/StreamCollectors.java}
 * blob {@code 11322d1f9ac0c7dd89e84783022d68e05e7de942} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StreamCollectors {
    private static final PlaceholderLogger log = PlaceholderLogger.of(StreamCollectors.class);


    private StreamCollectors() {}

    // --- COLLECTION CONVERSION ---

    /** Collect stream to list. */
    public static <T> List<T> toList(Stream<T> stream) {
        return stream == null ? List.of() : stream.toList();
    }

    /** Collect stream to mutable list. */
    public static <T> List<T> toMutableList(Stream<T> stream) {
        return stream == null ? new ArrayList<>() : stream.collect(Collectors.toCollection(ArrayList::new));
    }

    /** Collect stream to set. */
    public static <T> Set<T> toSet(Stream<T> stream) {
        return stream == null ? Set.of() : stream.collect(Collectors.toSet());
    }

    /** Collect stream to mutable set. */
    public static <T> Set<T> toMutableSet(Stream<T> stream) {
        return stream == null ? new HashSet<>() : stream.collect(Collectors.toCollection(HashSet::new));
    }

    /** Varargs to list. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> List<T> listOf(T... items) {
        if (items == null || items.length == 0) {
            return new ArrayList<>();
        }
        return Arrays.stream(items).collect(Collectors.toCollection(ArrayList::new));
    }

    /** Varargs to list, filtering nulls. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> List<T> listOfNonNull(T... items) {
        if (items == null || items.length == 0) {
            return new ArrayList<>();
        }
        return Arrays.stream(items)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Varargs to set. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> Set<T> setOf(T... items) {
        if (items == null || items.length == 0) {
            return new HashSet<>();
        }
        return Arrays.stream(items).collect(Collectors.toCollection(HashSet::new));
    }

    /** Varargs to set, filtering nulls. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T> Set<T> setOfNonNull(T... items) {
        if (items == null || items.length == 0) {
            return new HashSet<>();
        }
        return Arrays.stream(items)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    // --- MAP OPERATIONS ---

    /** Map varargs to list. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T, R> List<R> map(Function<T, R> mapper, T... items) {
        if (items == null || items.length == 0) {
            return new ArrayList<>();
        }
        return Arrays.stream(items).map(mapper).collect(Collectors.toCollection(ArrayList::new));
    }

    /** Map varargs to list, filtering nulls from result. */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    public static <T, R> List<R> mapNonNull(Function<T, R> mapper, T... items) {
        if (items == null || items.length == 0) {
            return new ArrayList<>();
        }
        return Arrays.stream(items)
                .map(mapper)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // --- DIRECT MAPPING (Java 21 - Code Shrinking) ---

    /** Maps a collection directly to an immutable list. */
    public static <T, R> List<R> mapToList(Collection<T> items, Function<T, R> mapper) {
        return StreamOps.of(items).map(mapper).toList();
    }

    /** Maps a collection directly to a mutable list. */
    public static <T, R> List<R> mapToMutableList(Collection<T> items, Function<T, R> mapper) {
        return toMutableList(StreamOps.of(items).map(mapper));
    }

    /** Filters a collection directly to an immutable list. */
    public static <T> List<T> filterToList(Collection<T> items, Predicate<T> predicate) {
        return StreamOps.of(items).filter(predicate).toList();
    }

    /** Filters a collection directly to a mutable list. */
    public static <T> List<T> filterToMutableList(Collection<T> items, Predicate<T> predicate) {
        return toMutableList(StreamOps.of(items).filter(predicate));
    }

    /** Maps and filters a collection in one pass. */
    public static <T, R> List<R> mapFilterToList(Collection<T> items, Function<T, R> mapper, Predicate<R> predicate) {
        return StreamOps.of(items).map(mapper).filter(predicate).toList();
    }

    // --- PARALLEL PROCESSING (Java 21 Virtual Threads) ---

    /** Maps items in parallel using virtual threads. */
    public static <T, R> List<R> mapParallel(Collection<T> items, Function<T, R> mapper) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<R>> futures = items.stream()
                    .map(item -> executor.submit(() -> mapper.apply(item)))
                    .toList();

            return futures.stream()
                    .map(StreamCollectors::getQuietly)
                    .flatMap(Optional::stream)
                    .toList();
        }
    }

    /** Maps items in parallel, filtering nulls from results. */
    public static <T, R> List<R> mapParallelNonNull(Collection<T> items, Function<T, R> mapper) {
        return mapParallel(items, mapper).stream().filter(Objects::nonNull).toList();
    }

    // --- PARTITIONING ---

    /** Partitions items by predicate (true/false groups). */
    public static <T> Map<Boolean, List<T>> partitionBy(Collection<T> items, Predicate<T> predicate) {
        if (items == null) {
            return Map.of(true, List.of(), false, List.of());
        }
        return StreamOps.of(items).collect(Collectors.partitioningBy(predicate));
    }

    // --- GROUPING OPERATIONS ---

    /** Group items by key. */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> items, Function<T, K> keyExtractor) {
        if (items == null) {
            return Map.of();
        }
        return StreamOps.of(items).collect(Collectors.groupingBy(keyExtractor));
    }

    /** Group items by key with value mapping. */
    public static <T, K, V> Map<K, List<V>> groupBy(
            Collection<T> items, Function<T, K> keyExtractor, Function<T, V> valueMapper) {
        if (items == null) {
            return Map.of();
        }
        return StreamOps.of(items)
                .collect(Collectors.groupingBy(keyExtractor, Collectors.mapping(valueMapper, Collectors.toList())));
    }

    /** Group items by key, counting occurrences. */
    public static <T, K> Map<K, Long> groupByCount(Collection<T> items, Function<T, K> keyExtractor) {
        if (items == null) {
            return Map.of();
        }
        return StreamOps.of(items).collect(Collectors.groupingBy(keyExtractor, Collectors.counting()));
    }

    /** Group items by key into sets. */
    public static <T, K> Map<K, Set<T>> groupByToSet(Collection<T> items, Function<T, K> keyExtractor) {
        if (items == null) {
            return Map.of();
        }
        return StreamOps.of(items).collect(Collectors.groupingBy(keyExtractor, Collectors.toSet()));
    }

    // --- INDEX MAP OPERATIONS ---

    /** Create index map from items. */
    public static <T, K> Map<K, T> toMap(Collection<T> items, Function<T, K> keyExtractor) {
        if (items == null) {
            return Map.of();
        }
        return StreamOps.of(items).collect(Collectors.toMap(keyExtractor, Function.identity(), (a, b) -> a));
    }

    /** Create index map from items with value mapper. */
    public static <T, K, V> Map<K, V> toMap(
            Collection<T> items, Function<T, K> keyExtractor, Function<T, V> valueMapper) {
        if (items == null) {
            return Map.of();
        }
        return StreamOps.of(items).collect(Collectors.toMap(keyExtractor, valueMapper, (a, b) -> a));
    }

    /** Create index map from items (LinkedHashMap - preserves order). */
    public static <T, K> Map<K, T> toLinkedMap(Collection<T> items, Function<T, K> keyExtractor) {
        if (items == null) {
            return Map.of();
        }
        return StreamOps.of(items)
                .collect(Collectors.toMap(keyExtractor, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    // --- UTILITY ---

    // FLLDCIM: MRL — FM-36 return null eliminated; Optional<T> preserves contract
    private static <T> Optional<T> getQuietly(Future<T> future) {
        try {
            return Optional.ofNullable(future.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }
}
