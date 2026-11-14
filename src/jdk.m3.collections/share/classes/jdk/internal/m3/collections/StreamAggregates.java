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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
/**
 * Stream aggregation, frequency, joining, and reduction operations.
 *
 * @since ContextOS 1.0
 * @see StreamOps
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/StreamAggregates.java}
 * blob {@code 96a3cc4e96c29f2a4d3dacbba682492132788d12} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StreamAggregates {
    private static final PlaceholderLogger log = PlaceholderLogger.of(StreamAggregates.class);


    private StreamAggregates() {}

    // --- AGGREGATION OPERATIONS ---

    /** Sum integers. */
    public static int sumInt(Collection<Integer> items) {
        return StreamOps.of(items)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /** Sum longs. */
    public static long sumLong(Collection<Long> items) {
        return StreamOps.of(items)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
    }

    /** Sum doubles. */
    public static double sumDouble(Collection<Double> items) {
        return StreamOps.of(items)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    /** Sum by extractor. */
    public static <T> int sumIntBy(Collection<T> items, ToIntFunction<T> extractor) {
        return StreamOps.of(items).filter(Objects::nonNull).mapToInt(extractor).sum();
    }

    /** Sum by extractor (long). */
    public static <T> long sumLongBy(Collection<T> items, ToLongFunction<T> extractor) {
        return StreamOps.of(items).filter(Objects::nonNull).mapToLong(extractor).sum();
    }

    /** Sum by extractor (double). */
    public static <T> double sumDoubleBy(Collection<T> items, ToDoubleFunction<T> extractor) {
        return StreamOps.of(items)
                .filter(Objects::nonNull)
                .mapToDouble(extractor)
                .sum();
    }

    /** Average of integers. */
    public static OptionalDouble avgInt(Collection<Integer> items) {
        return StreamOps.of(items)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average();
    }

    /** Average of doubles. */
    public static OptionalDouble avgDouble(Collection<Double> items) {
        return StreamOps.of(items)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
    }

    /** Min value. */
    public static <T extends Comparable<T>> Optional<T> min(Collection<T> items) {
        return StreamOps.of(items).filter(Objects::nonNull).min(Comparator.naturalOrder());
    }

    /** Max value. */
    public static <T extends Comparable<T>> Optional<T> max(Collection<T> items) {
        return StreamOps.of(items).filter(Objects::nonNull).max(Comparator.naturalOrder());
    }

    /** Min by comparator. */
    public static <T> Optional<T> minBy(Collection<T> items, Comparator<T> comparator) {
        return StreamOps.of(items).filter(Objects::nonNull).min(comparator);
    }

    /** Max by comparator. */
    public static <T> Optional<T> maxBy(Collection<T> items, Comparator<T> comparator) {
        return StreamOps.of(items).filter(Objects::nonNull).max(comparator);
    }

    /** Min by key extractor. */
    public static <T, U extends Comparable<U>> Optional<T> minBy(Collection<T> items, Function<T, U> keyExtractor) {
        return StreamOps.of(items).filter(Objects::nonNull).min(Comparator.comparing(keyExtractor));
    }

    /** Max by key extractor. */
    public static <T, U extends Comparable<U>> Optional<T> maxBy(Collection<T> items, Function<T, U> keyExtractor) {
        return StreamOps.of(items).filter(Objects::nonNull).max(Comparator.comparing(keyExtractor));
    }

    // --- FREQUENCY OPERATIONS ---

    /** Count frequency of each item. */
    public static <T> Map<T, Long> frequencies(Collection<T> items) {
        if (items == null) {
            return Map.of();
        }
        return StreamOps.of(items).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /** Get most frequent item. */
    public static <T> Optional<T> mostFrequent(Collection<T> items) {
        return frequencies(items).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /** Get least frequent item. */
    public static <T> Optional<T> leastFrequent(Collection<T> items) {
        return frequencies(items).entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /** Get items appearing more than N times. */
    public static <T> List<T> frequentItems(Collection<T> items, long minCount) {
        return frequencies(items).entrySet().stream()
                .filter(e -> e.getValue() >= minCount)
                .map(Map.Entry::getKey)
                .toList();
    }

    // --- JOINING OPERATIONS ---

    /** Join strings with delimiter. */
    public static String join(Collection<String> items, String delimiter) {
        return StreamOps.of(items).filter(Objects::nonNull).collect(Collectors.joining(delimiter));
    }

    /** Join strings with delimiter, prefix, and suffix. */
    public static String join(Collection<String> items, String delimiter, String prefix, String suffix) {
        return StreamOps.of(items).filter(Objects::nonNull).collect(Collectors.joining(delimiter, prefix, suffix));
    }

    /** Join items with delimiter (using toString). */
    public static <T> String joinToString(Collection<T> items, String delimiter) {
        return StreamOps.of(items)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining(delimiter));
    }

    /** Join items with delimiter and mapper. */
    public static <T> String joinToString(Collection<T> items, String delimiter, Function<T, String> mapper) {
        return StreamOps.of(items).filter(Objects::nonNull).map(mapper).collect(Collectors.joining(delimiter));
    }

    // --- REDUCTION OPERATIONS ---

    /** Reduce with identity. */
    public static <T> T reduce(Collection<T> items, T identity, java.util.function.BinaryOperator<T> accumulator) {
        return StreamOps.of(items).reduce(identity, accumulator);
    }

    /** Reduce without identity. */
    public static <T> Optional<T> reduce(Collection<T> items, java.util.function.BinaryOperator<T> accumulator) {
        return StreamOps.of(items).reduce(accumulator);
    }
}
