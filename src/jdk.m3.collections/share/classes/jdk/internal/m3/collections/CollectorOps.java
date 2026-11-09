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
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/CollectorOps.java}
 * blob {@code 59042060f99111b80bfb965b435be25410e37546} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CollectorOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(CollectorOps.class);


    private CollectorOps() {}

    private static final class IndexedAccumulator<T> {
        private int index;
        private final List<Map.Entry<Integer, T>> entries = new ArrayList<>();
    }

    private static final class SkipAccumulator<T> {
        private int index;
        private final List<T> values = new ArrayList<>();
    }

    public static <T> Collector<T, ?, List<T>> toList() {
        return Collectors.toList();
    }

    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return Collectors.toUnmodifiableList();
    }

    public static <T> Collector<T, ?, Set<T>> toSet() {
        return Collectors.toSet();
    }

    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return Collectors.toUnmodifiableSet();
    }

    public static <T> Collector<T, ?, LinkedHashSet<T>> toLinkedHashSet() {
        return Collectors.toCollection(LinkedHashSet::new);
    }

    public static <T extends Comparable<T>> Collector<T, ?, TreeSet<T>> toTreeSet() {
        return Collectors.toCollection(TreeSet::new);
    }

    public static <T, K> Collector<T, ?, Map<K, T>> toMapBy(Function<T, K> keyMapper) {
        return Collectors.toMap(keyMapper, Function.identity(), (a, b) -> a);
    }

    public static <T, K, V> Collector<T, ?, Map<K, V>> toMap(Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper, (a, b) -> a);
    }

    public static <T, K, V> Collector<T, ?, Map<K, V>> toMapMergeLast(
            Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper, (a, b) -> b);
    }

    public static <T, K> Collector<T, ?, LinkedHashMap<K, T>> toLinkedMapBy(Function<T, K> keyMapper) {
        return Collectors.toMap(keyMapper, Function.identity(), (a, b) -> a, LinkedHashMap::new);
    }

    public static <T, K> Collector<T, ?, ConcurrentHashMap<K, T>> toConcurrentMapBy(Function<T, K> keyMapper) {
        return Collectors.toMap(keyMapper, Function.identity(), (a, b) -> a, ConcurrentHashMap::new);
    }

    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupBy(Function<T, K> classifier) {
        return Collectors.groupingBy(classifier);
    }

    public static <T, K, V> Collector<T, ?, Map<K, List<V>>> groupBy(
            Function<T, K> classifier, Function<T, V> valueMapper) {
        return Collectors.groupingBy(classifier, Collectors.mapping(valueMapper, Collectors.toList()));
    }

    public static <T, K> Collector<T, ?, Map<K, Set<T>>> groupByToSet(Function<T, K> classifier) {
        return Collectors.groupingBy(classifier, Collectors.toSet());
    }

    public static <T, K> Collector<T, ?, Map<K, Long>> groupByCount(Function<T, K> classifier) {
        return Collectors.groupingBy(classifier, Collectors.counting());
    }

    public static <T, K> Collector<T, ?, LinkedHashMap<K, List<T>>> groupByLinked(Function<T, K> classifier) {
        return Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList());
    }

    public static <T> Collector<T, ?, Map<Boolean, List<T>>> partitionBy(Predicate<T> predicate) {
        return Collectors.partitioningBy(predicate);
    }

    public static Collector<CharSequence, ?, String> joining() {
        return Collectors.joining();
    }

    public static Collector<CharSequence, ?, String> joining(String delimiter) {
        return Collectors.joining(delimiter);
    }

    public static Collector<CharSequence, ?, String> joining(String delimiter, String prefix, String suffix) {
        return Collectors.joining(delimiter, prefix, suffix);
    }

    public static <T> Collector<T, ?, String> joiningToString(String delimiter) {
        return Collectors.mapping(Object::toString, Collectors.joining(delimiter));
    }

    public static Collector<Integer, ?, Integer> summingInt() {
        return Collectors.summingInt(Integer::intValue);
    }

    public static <T> Collector<T, ?, Integer> summingIntBy(ToIntFunction<T> mapper) {
        return Collectors.summingInt(mapper);
    }

    public static Collector<Long, ?, Long> summingLong() {
        return Collectors.summingLong(Long::longValue);
    }

    public static <T> Collector<T, ?, Long> summingLongBy(ToLongFunction<T> mapper) {
        return Collectors.summingLong(mapper);
    }

    public static Collector<Double, ?, Double> summingDouble() {
        return Collectors.summingDouble(Double::doubleValue);
    }

    public static <T> Collector<T, ?, Double> summingDoubleBy(ToDoubleFunction<T> mapper) {
        return Collectors.summingDouble(mapper);
    }

    public static Collector<Integer, ?, Double> averagingInt() {
        return Collectors.averagingInt(Integer::intValue);
    }

    public static <T> Collector<T, ?, Double> averagingIntBy(ToIntFunction<T> mapper) {
        return Collectors.averagingInt(mapper);
    }

    public static Collector<Long, ?, Double> averagingLong() {
        return Collectors.averagingLong(Long::longValue);
    }

    public static <T> Collector<T, ?, Double> averagingLongBy(ToLongFunction<T> mapper) {
        return Collectors.averagingLong(mapper);
    }

    public static Collector<Double, ?, Double> averagingDouble() {
        return Collectors.averagingDouble(Double::doubleValue);
    }

    public static <T> Collector<T, ?, Double> averagingDoubleBy(ToDoubleFunction<T> mapper) {
        return Collectors.averagingDouble(mapper);
    }

    // Min/Max collectors
    public static <T extends Comparable<T>> Collector<T, ?, Optional<T>> min() {
        return Collectors.minBy(Comparator.naturalOrder());
    }

    public static <T extends Comparable<T>> Collector<T, ?, Optional<T>> max() {
        return Collectors.maxBy(Comparator.naturalOrder());
    }

    public static <T> Collector<T, ?, Optional<T>> minBy(Comparator<T> comparator) {
        return Collectors.minBy(comparator);
    }

    public static <T> Collector<T, ?, Optional<T>> maxBy(Comparator<T> comparator) {
        return Collectors.maxBy(comparator);
    }

    public static <T, U extends Comparable<U>> Collector<T, ?, Optional<T>> minBy(Function<T, U> keyExtractor) {
        return Collectors.minBy(Comparator.comparing(keyExtractor));
    }

    public static <T, U extends Comparable<U>> Collector<T, ?, Optional<T>> maxBy(Function<T, U> keyExtractor) {
        return Collectors.maxBy(Comparator.comparing(keyExtractor));
    }

    // Count collectors
    public static <T> Collector<T, ?, Long> counting() {
        return Collectors.counting();
    }

    public static <T> Collector<T, ?, Integer> countingInt() {
        return Collectors.collectingAndThen(Collectors.counting(), Long::intValue);
    }

    // First/Last collectors
    public static <T> Collector<T, ?, Optional<T>> first() {
        return Collectors.reducing((a, b) -> a);
    }

    public static <T> Collector<T, ?, Optional<T>> last() {
        return Collectors.reducing((a, b) -> b);
    }

    public static <T> Collector<T, ?, T> firstOrDefault(T defaultValue) {
        return Collectors.collectingAndThen(first(), opt -> opt.orElse(defaultValue));
    }

    public static <T> Collector<T, ?, T> lastOrDefault(T defaultValue) {
        return Collectors.collectingAndThen(last(), opt -> opt.orElse(defaultValue));
    }

    // Filtering collectors
    public static <T> Collector<T, ?, List<T>> filtering(Predicate<T> predicate) {
        return Collectors.filtering(predicate, Collectors.toList());
    }

    public static <T, A, R> Collector<T, ?, R> filtering(Predicate<T> predicate, Collector<T, A, R> downstream) {
        return Collectors.filtering(predicate, downstream);
    }

    // Mapping collectors
    public static <T, U> Collector<T, ?, List<U>> mapping(Function<T, U> mapper) {
        return Collectors.mapping(mapper, Collectors.toList());
    }

    public static <T, U, A, R> Collector<T, ?, R> mapping(Function<T, U> mapper, Collector<U, A, R> downstream) {
        return Collectors.mapping(mapper, downstream);
    }

    // FlatMapping collectors
    public static <T, U> Collector<T, ?, List<U>> flatMapping(Function<T, Stream<U>> mapper) {
        return Collectors.flatMapping(mapper, Collectors.toList());
    }

    public static <T, U, A, R> Collector<T, ?, R> flatMapping(
            Function<T, Stream<U>> mapper, Collector<U, A, R> downstream) {
        return Collectors.flatMapping(mapper, downstream);
    }

    // Distinct collector
    public static <T> Collector<T, ?, List<T>> distinct() {
        return Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new);
    }

    public static <T, K> Collector<T, ?, List<T>> distinctBy(Function<T, K> keyExtractor) {
        return Collector.of(
                LinkedHashMap<K, T>::new,
                (map, t) -> map.putIfAbsent(keyExtractor.apply(t), t),
                (m1, m2) -> {
                    m2.forEach(m1::putIfAbsent);
                    return m1;
                },
                m -> new ArrayList<>(m.values()));
    }

    // Teeing collector (combine two collectors)
    public static <T, R1, R2, R> Collector<T, ?, R> teeing(
            Collector<T, ?, R1> downstream1, Collector<T, ?, R2> downstream2, BiFunction<R1, R2, R> merger) {
        return Collectors.teeing(downstream1, downstream2, merger);
    }

    // Reducing collectors
    public static <T> Collector<T, ?, Optional<T>> reducing(BinaryOperator<T> op) {
        return Collectors.reducing(op);
    }

    public static <T> Collector<T, ?, T> reducing(T identity, BinaryOperator<T> op) {
        return Collectors.reducing(identity, op);
    }

    public static <T, U> Collector<T, ?, U> reducing(U identity, Function<T, U> mapper, BinaryOperator<U> op) {
        return Collectors.reducing(identity, mapper, op);
    }

    // Statistics collectors
    public static Collector<Integer, ?, IntSummaryStatistics> summarizingInt() {
        return Collectors.summarizingInt(Integer::intValue);
    }

    public static <T> Collector<T, ?, IntSummaryStatistics> summarizingIntBy(ToIntFunction<T> mapper) {
        return Collectors.summarizingInt(mapper);
    }

    public static Collector<Long, ?, LongSummaryStatistics> summarizingLong() {
        return Collectors.summarizingLong(Long::longValue);
    }

    public static <T> Collector<T, ?, LongSummaryStatistics> summarizingLongBy(ToLongFunction<T> mapper) {
        return Collectors.summarizingLong(mapper);
    }

    public static Collector<Double, ?, DoubleSummaryStatistics> summarizingDouble() {
        return Collectors.summarizingDouble(Double::doubleValue);
    }

    public static <T> Collector<T, ?, DoubleSummaryStatistics> summarizingDoubleBy(ToDoubleFunction<T> mapper) {
        return Collectors.summarizingDouble(mapper);
    }

    // CollectingAndThen (transform final result)
    public static <T, A, R, RR> Collector<T, A, RR> andThen(Collector<T, A, R> collector, Function<R, RR> finisher) {
        return Collectors.collectingAndThen(collector, finisher);
    }

    // Frequency map
    public static <T> Collector<T, ?, Map<T, Long>> frequencies() {
        return Collectors.groupingBy(Function.identity(), Collectors.counting());
    }

    public static <T, K> Collector<T, ?, Map<K, Long>> frequenciesBy(Function<T, K> keyExtractor) {
        return Collectors.groupingBy(keyExtractor, Collectors.counting());
    }

    // Concurrent collectors
    public static <T, K> Collector<T, ?, ConcurrentMap<K, List<T>>> groupByConcurrent(Function<T, K> classifier) {
        return Collectors.groupingByConcurrent(classifier);
    }

    public static <T, K, V> Collector<T, ?, ConcurrentMap<K, V>> toConcurrentMap(
            Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return Collectors.toConcurrentMap(keyMapper, valueMapper, (a, b) -> a);
    }

    // Boolean collectors
    public static <T> Collector<T, ?, Boolean> allMatch(Predicate<T> predicate) {
        return Collectors.reducing(true, predicate::test, Boolean::logicalAnd);
    }

    public static <T> Collector<T, ?, Boolean> anyMatch(Predicate<T> predicate) {
        return Collectors.reducing(false, predicate::test, Boolean::logicalOr);
    }

    public static <T> Collector<T, ?, Boolean> noneMatch(Predicate<T> predicate) {
        return Collectors.collectingAndThen(anyMatch(predicate), b -> !b);
    }

    // Partition with downstream
    public static <T, A, R> Collector<T, ?, Map<Boolean, R>> partitionBy(
            Predicate<T> predicate, Collector<T, A, R> downstream) {
        return Collectors.partitioningBy(predicate, downstream);
    }

    // Grouping with downstream
    public static <T, K, A, R> Collector<T, ?, Map<K, R>> groupBy(
            Function<T, K> classifier, Collector<T, A, R> downstream) {
        return Collectors.groupingBy(classifier, downstream);
    }

    public static <T, K, A, R, M extends Map<K, R>> Collector<T, ?, M> groupBy(
            Function<T, K> classifier, Supplier<M> mapFactory, Collector<T, A, R> downstream) {
        return Collectors.groupingBy(classifier, mapFactory, downstream);
    }

    // Indexed collector (with index)
    public static <T> Collector<T, ?, Map<Integer, T>> indexed() {
        return Collector.of(
                () -> new int[] {0},
                (arr, t) -> arr[0]++,
                (a, b) -> a,
                arr -> Map.of(),
                Collector.Characteristics.IDENTITY_FINISH);
    }

    public static <T> Collector<T, ?, List<Map.Entry<Integer, T>>> withIndex() {
        return Collector.of(
                () -> new IndexedAccumulator<T>(),
                (acc, t) -> {
                    acc.entries.add(Map.entry(acc.index, t));
                    acc.index++;
                },
                (a, b) -> a,
                acc -> acc.entries);
    }

    // Chunking collector
    public static <T> Collector<T, ?, List<List<T>>> chunked(int size) {
        return Collector.of(
                ArrayList::new,
                (list, t) -> {
                    if (list.isEmpty() || list.getLast().size() >= size) list.add(new ArrayList<>());
                    list.getLast().add(t);
                },
                (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                });
    }

    // Take/Skip collectors
    public static <T> Collector<T, ?, List<T>> take(int n) {
        return Collector.of(
                ArrayList::new,
                (list, t) -> {
                    if (list.size() < n) list.add(t);
                },
                (l1, l2) -> {
                    for (T t : l2) if (l1.size() < n) l1.add(t); // NFL — collector combiner, size-bounded accumulation
                    return l1;
                });
    }

    public static <T> Collector<T, ?, List<T>> skip(int n) {
        return Collector.of(
                () -> new SkipAccumulator<T>(),
                (acc, t) -> {
                    if (acc.index >= n) acc.values.add(t);
                    acc.index++;
                },
                (a, b) -> a,
                acc -> acc.values);
    }

    // Reverse collector
    public static <T> Collector<T, ?, List<T>> reversing() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            Collections.reverse(list);
            return list;
        });
    }

    // Shuffling collector
    public static <T> Collector<T, ?, List<T>> shuffling() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            Collections.shuffle(list);
            return list;
        });
    }

    public static <T> Collector<T, ?, List<T>> shuffling(Random random) {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            Collections.shuffle(list, random);
            return list;
        });
    }

    // Sorting collectors
    public static <T extends Comparable<T>> Collector<T, ?, List<T>> sorting() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            Collections.sort(list);
            return list;
        });
    }

    public static <T> Collector<T, ?, List<T>> sortingBy(Comparator<T> comparator) {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            list.sort(comparator);
            return list;
        });
    }

    public static <T, U extends Comparable<U>> Collector<T, ?, List<T>> sortingBy(Function<T, U> keyExtractor) {
        return sortingBy(Comparator.comparing(keyExtractor));
    }

    /**
     * Command-line interface for CollectorOps demonstrations.
     *
     * <p>
     * Usage:
     *
     * <pre>
     * java jdk.internal.m3.collections.CollectorOps &lt;command&gt; [args]
     *
     * Commands:
     *   list                  - List all available operations
     *   demo                  - Run demonstration of collectors
     *
     * Examples:
     *   java jdk.internal.m3.collections.CollectorOps demo
     * </pre>
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
                case "demo" -> {
                    List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
                    log.info("Demo data: {}", numbers);

                    List<Integer> evenNumbers =
                            numbers.stream().filter(n -> n % 2 == 0).collect(toList());
                    log.info("Even numbers: {}", evenNumbers);

                    Set<Integer> uniqueSet = numbers.stream().collect(toSet());
                    log.info("Unique set: {}", uniqueSet);

                    int sum = numbers.stream().collect(summingInt());
                    log.info("Sum: {}", sum);

                    double avg = numbers.stream().collect(averagingInt());
                    log.info("Average: {}", avg);

                    Map<Boolean, List<Integer>> partitioned = numbers.stream().collect(partitionBy(n -> n % 2 == 0));
                    log.info("Partitioned (even/odd): {}", partitioned);

                    String joined = numbers.stream().map(Object::toString).collect(joining(", "));
                    log.info("Joined: {}", joined);
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
        log.info("""
				Usage: java jdk.internal.m3.collections.CollectorOps <command> [args]

				Commands:
				  list                  - List all available operations
				  demo                  - Run demonstration of collectors

				Examples:
				  java jdk.internal.m3.collections.CollectorOps demo
				""");
    }

    private static void printOperations() {
        log.info("""
				CollectorOps - Stream Collector Utilities

				Basic Collectors:
				  toList, toSet, toUnmodifiableList, toLinkedHashSet, toTreeSet

				Map Collectors:
				  toMapBy, toMap, toLinkedMapBy, toConcurrentMapBy

				Grouping:
				  groupBy, groupByToSet, groupByCount, groupByLinked

				Partitioning:
				  partitionBy

				String Joining:
				  joining, joiningToString

				Numeric:
				  summingInt, summingLong, summingDouble
				  averagingInt, averagingLong, averagingDouble
				  min, max, minBy, maxBy

				Counting:
				  counting, countingInt

				Special:
				  first, last, distinct, distinctBy
				  filtering, mapping, flatMapping
				  teeing, reducing, chunked
				  take, skip, reversing, shuffling, sorting
				""");
    }
}
