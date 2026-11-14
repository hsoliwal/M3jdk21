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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
/**
 * Stream sequence operations: zip, flatMap, window, take/drop, sort, find,
 * count, interleave.
 *
 * @since ContextOS 1.0
 * @see StreamOps
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/StreamSequences.java}
 * blob {@code 2778315449c231a1c0a533ea0e74090e8c587d5e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StreamSequences {
    private static final PlaceholderLogger log = PlaceholderLogger.of(StreamSequences.class);


    private StreamSequences() {}

    // --- ZIP OPERATIONS ---

    /** Zip two lists together. */
    public static <A, B> List<Map.Entry<A, B>> zip(List<A> a, List<B> b) {
        if (a == null || b == null) {
            return List.of();
        }
        int size = Math.min(a.size(), b.size());
        return IntStream.range(0, size)
                .mapToObj(i -> Map.entry(a.get(i), b.get(i)))
                .toList();
    }

    /** Zip two lists with combiner. */
    public static <A, B, R> List<R> zipWith(List<A> a, List<B> b, BiFunction<A, B, R> combiner) {
        if (a == null || b == null) {
            return List.of();
        }
        int size = Math.min(a.size(), b.size());
        return IntStream.range(0, size)
                .mapToObj(i -> combiner.apply(a.get(i), b.get(i)))
                .toList();
    }

    /** Zip with index. */
    public static <T> List<Map.Entry<Integer, T>> zipWithIndex(Collection<T> items) {
        if (items == null) {
            return List.of();
        }
        List<T> list = items instanceof List<T> l ? l : new ArrayList<>(items);
        return IntStream.range(0, list.size())
                .mapToObj(i -> Map.entry(i, list.get(i)))
                .toList();
    }

    // --- FLAT MAP OPERATIONS ---

    /** Flat map collection to list. */
    public static <T, R> List<R> flatMapToList(Collection<T> items, Function<T, ? extends Collection<R>> mapper) {
        return StreamOps.of(items).flatMap(t -> StreamOps.of(mapper.apply(t))).toList();
    }

    /** Flat map collection to set. */
    public static <T, R> Set<R> flatMapToSet(Collection<T> items, Function<T, ? extends Collection<R>> mapper) {
        return StreamCollectors.toSet(StreamOps.of(items).flatMap(t -> StreamOps.of(mapper.apply(t))));
    }

    /** Flat map optional values. */
    public static <T, R> List<R> flatMapOptional(Collection<T> items, Function<T, Optional<R>> mapper) {
        return StreamOps.of(items).map(mapper).flatMap(Optional::stream).toList();
    }

    // --- WINDOW/SLIDING OPERATIONS ---

    /** Create sliding windows. */
    public static <T> List<List<T>> sliding(List<T> list, int windowSize) {
        if (list == null || list.size() < windowSize || windowSize <= 0) {
            return List.of();
        }
        return IntStream.rangeClosed(0, list.size() - windowSize)
                .mapToObj(i -> list.subList(i, i + windowSize))
                .<List<T>>map(ArrayList::new)
                .toList();
    }

    /** Create sliding windows with step. */
    public static <T> List<List<T>> sliding(List<T> list, int windowSize, int step) {
        if (list == null || list.size() < windowSize || windowSize <= 0 || step <= 0) {
            return List.of();
        }
        return IntStream.iterate(0, i -> i <= list.size() - windowSize, i -> i + step)
                .<List<T>>mapToObj(i -> new ArrayList<>(list.subList(i, i + windowSize)))
                .toList();
    }

    /** Get pairs (windows of 2). */
    public static <T> List<Map.Entry<T, T>> pairs(List<T> list) {
        if (list == null || list.size() < 2) {
            return List.of();
        }
        return IntStream.range(0, list.size() - 1)
                .mapToObj(i -> Map.entry(list.get(i), list.get(i + 1)))
                .toList();
    }

    // --- PARTITIONING ---

    /** Partitions a list into chunks of specified size. */
    public static <T> List<List<T>> partition(List<T> list, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Partition size must be > 0");
        }
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        int totalSize = list.size();
        int numChunks = (totalSize + size - 1) / size;

        return IntStream.range(0, numChunks)
                .mapToObj(i -> list.subList(i * size, Math.min((i + 1) * size, totalSize)))
                .toList();
    }

    /** Partitions a collection into chunks of specified size. */
    public static <T> List<List<T>> partition(Collection<T> items, int size) {
        if (items == null) {
            return List.of();
        }
        if (items instanceof List<T> list) {
            return partition(list, size);
        }
        return partition(new ArrayList<>(items), size);
    }

    // --- TAKE/DROP OPERATIONS ---

    /** Take first N items. */
    public static <T> List<T> take(Collection<T> items, int n) {
        return StreamOps.of(items).limit(n).toList();
    }

    /** Drop first N items. */
    public static <T> List<T> drop(Collection<T> items, int n) {
        return StreamOps.of(items).skip(n).toList();
    }

    /** Take last N items. */
    public static <T> List<T> takeLast(List<T> list, int n) {
        if (list == null || n <= 0) {
            return List.of();
        }
        int start = Math.max(0, list.size() - n);
        return new ArrayList<>(list.subList(start, list.size()));
    }

    /** Drop last N items. */
    public static <T> List<T> dropLast(List<T> list, int n) {
        if (list == null || n <= 0) {
            return list == null ? List.of() : new ArrayList<>(list);
        }
        int end = Math.max(0, list.size() - n);
        return new ArrayList<>(list.subList(0, end));
    }

    /** Take while predicate is true. */
    public static <T> List<T> takeWhile(Collection<T> items, Predicate<T> predicate) {
        return StreamOps.of(items).takeWhile(predicate).toList();
    }

    /** Drop while predicate is true. */
    public static <T> List<T> dropWhile(Collection<T> items, Predicate<T> predicate) {
        return StreamOps.of(items).dropWhile(predicate).toList();
    }

    // --- SORTING OPERATIONS ---

    /** Sort by natural order. */
    public static <T extends Comparable<T>> List<T> sorted(Collection<T> items) {
        return StreamOps.of(items).sorted().toList();
    }

    /** Sort by comparator. */
    public static <T> List<T> sorted(Collection<T> items, Comparator<T> comparator) {
        return StreamOps.of(items).sorted(comparator).toList();
    }

    /** Sort by key extractor. */
    public static <T, U extends Comparable<U>> List<T> sortedBy(Collection<T> items, Function<T, U> keyExtractor) {
        return StreamOps.of(items).sorted(Comparator.comparing(keyExtractor)).toList();
    }

    /** Sort by key extractor (descending). */
    public static <T, U extends Comparable<U>> List<T> sortedByDesc(Collection<T> items, Function<T, U> keyExtractor) {
        return StreamOps.of(items)
                .sorted(Comparator.comparing(keyExtractor).reversed())
                .toList();
    }

    /** Reverse list. */
    public static <T> List<T> reversed(List<T> list) {
        if (list == null) {
            return List.of();
        }
        return list.reversed();
    }

    // --- FIND OPERATIONS ---

    /** Find first matching. */
    public static <T> Optional<T> findFirst(Collection<T> items, Predicate<T> predicate) {
        return StreamOps.of(items).filter(predicate).findFirst();
    }

    /** Find any matching. */
    public static <T> Optional<T> findAny(Collection<T> items, Predicate<T> predicate) {
        return StreamOps.of(items).filter(predicate).findAny();
    }

    /** Find last matching. */
    public static <T> Optional<T> findLast(List<T> list, Predicate<T> predicate) {
        if (list == null) {
            return Optional.empty();
        }
        return list.reversed().stream().filter(predicate).findFirst();
    }

    /** Find index of first matching. */
    public static <T> int findIndex(List<T> list, Predicate<T> predicate) {
        if (list == null) {
            return -1;
        }
        return IntStream.range(0, list.size())
                .filter(i -> predicate.test(list.get(i)))
                .findFirst()
                .orElse(-1);
    }

    /** Find index of last matching. */
    public static <T> int findLastIndex(List<T> list, Predicate<T> predicate) {
        if (list == null) {
            return -1;
        }
        return IntStream.iterate(list.size() - 1, i -> i >= 0, i -> i - 1)
                .filter(i -> predicate.test(list.get(i)))
                .findFirst()
                .orElse(-1);
    }

    // --- COUNT/CHECK OPERATIONS ---

    /** Count items matching predicate. */
    public static <T> long count(Collection<T> items, Predicate<T> predicate) {
        return StreamOps.of(items).filter(predicate).count();
    }

    /** Count non-null items. */
    public static <T> long countNonNull(Collection<T> items) {
        return StreamOps.of(items).filter(Objects::nonNull).count();
    }

    /** Check if collection is empty or null. */
    public static <T> boolean isEmpty(Collection<T> items) {
        return items == null || items.isEmpty();
    }

    /** Check if collection is not empty. */
    public static <T> boolean isNotEmpty(Collection<T> items) {
        return items != null && !items.isEmpty();
    }

    /** Check if collection has exactly N items. */
    public static <T> boolean hasSize(Collection<T> items, int size) {
        return items != null && items.size() == size;
    }

    /** Check if collection contains item. */
    public static <T> boolean contains(Collection<T> items, T item) {
        return items != null && items.contains(item);
    }

    /** Check if any match predicate. */
    public static <T> boolean anyMatch(Collection<T> items, Predicate<T> predicate) {
        return StreamOps.of(items).anyMatch(predicate);
    }

    /** Check if all match predicate. */
    public static <T> boolean allMatch(Collection<T> items, Predicate<T> predicate) {
        return StreamOps.of(items).allMatch(predicate);
    }

    /** Check if none match predicate. */
    public static <T> boolean noneMatch(Collection<T> items, Predicate<T> predicate) {
        return StreamOps.of(items).noneMatch(predicate);
    }

    // --- INDEX OPERATIONS ---

    /** Convert to positional index map. */
    public static <T> Map<Integer, T> toIndexMap(Collection<T> items) {
        if (items == null) {
            return Map.of();
        }
        List<T> list = items instanceof List<T> l ? l : new ArrayList<>(items);
        return IntStream.range(0, list.size()).boxed().collect(Collectors.toMap(Function.identity(), list::get));
    }

    /** Convert to reverse index map. */
    public static <T> Map<T, Integer> toReverseIndexMap(Collection<T> items) {
        if (items == null) {
            return Map.of();
        }
        List<T> list = items instanceof List<T> l ? l : new ArrayList<>(items);
        return IntStream.range(0, list.size())
                .boxed()
                .collect(Collectors.toMap(list::get, Function.identity(), (a, b) -> a));
    }

    // --- INTERLEAVE/MERGE ---

    /** Interleave two lists. */
    public static <T> List<T> interleave(List<T> a, List<T> b) {
        if (a == null && b == null) {
            return List.of();
        }
        if (a == null) {
            return new ArrayList<>(b);
        }
        if (b == null) {
            return new ArrayList<>(a);
        }

        int maxSize = Math.max(a.size(), b.size());
        List<T> listA = a;
        List<T> listB = b;
        return IntStream.range(0, maxSize)
                .boxed()
                .flatMap(i -> Stream.of(i < listA.size() ? listA.get(i) : null, i < listB.size() ? listB.get(i) : null))
                .filter(Objects::nonNull)
                .toList();
    }

    /** Merge sorted lists. */
    public static <T extends Comparable<T>> List<T> mergeSorted(List<T> a, List<T> b) {
        List<T> result = new ArrayList<>();
        if (a != null) {
            result.addAll(a);
        }
        if (b != null) {
            result.addAll(b);
        }
        Collections.sort(result);
        return result;
    }
}
