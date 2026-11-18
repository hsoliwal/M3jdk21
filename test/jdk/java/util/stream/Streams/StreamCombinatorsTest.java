/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Lazy stream combinators preserve values, bounds, ownership, and encounter order
 * @modules java.base/java.util.stream:open
 *          java.base/java.util.function.ext
 * @run main StreamCombinatorsTest
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.stream.Streams;

public class StreamCombinatorsTest {
    private static final Comparator<Integer> NULLABLE_ORDER =
            Comparator.nullsFirst(Comparator.naturalOrder());
    private static int checks;

    public static void main(String[] args) throws Exception {
        basicValuesAndNulls();
        deterministicDifferentialCases();
        metadataAndExecutionMode();
        constructionAndSourceOwnership();
        parallelStatefulConstructionIsLazy();
        shortCircuitAndInfiniteInputs();
        mapWhileTermination();
        distinctKeyBudget();
        stableSortedMerge();
        callbackAndTraversalFailures();
        closePropagation();
        validationAndReusedInputs();
        indexedOverflow();
        System.out.println("StreamCombinatorsTest checks=" + checks);
    }

    private static void basicValuesAndNulls() {
        equal(Streams.pairwise(Stream.<Integer>empty(), Integer::sum).toList(),
                List.of(), "pairwise empty");
        equal(Streams.pairwise(Stream.of(9), Integer::sum).toList(),
                List.of(), "pairwise singleton has no partial pair");
        equal(Streams.pairwise(Stream.of(1, 4, 9, 16), (a, b) -> b - a).toList(),
                List.of(3, 5, 7), "pairwise overlapping adjacency");
        equal(Streams.pairwise(Stream.of(null, "a", null), (a, b) -> a).toList(),
                Arrays.asList(null, "a"), "pairwise nullable input and result");
        equal(Streams.mapIndexed(Stream.of("a", null, "c"),
                (value, index) -> index + ":" + value).toList(),
                List.of("0:a", "1:null", "2:c"), "indexed nullable values");
        equal(Streams.mapIndexed(Stream.of(1, 2), (value, index) -> (String) null).toList(),
                Arrays.asList(null, null), "indexed nullable mapped result");
        equal(Streams.mapIndexed(Stream.of(0, 1, 2, 3).filter(x -> x % 2 == 1),
                (value, index) -> value + ":" + index).toList(),
                List.of("1:0", "3:1"), "index belongs to current pipeline stage");
        equal(Streams.intersperse(Stream.<String>empty(), null).toList(),
                List.of(), "intersperse empty");
        equal(Streams.intersperse(Stream.of("a"), null).toList(),
                List.of("a"), "intersperse singleton");
        equal(Streams.intersperse(Stream.of("a", null, "b"), null).toList(),
                Arrays.asList("a", null, null, null, "b"), "nullable separator and elements");
        equal(Streams.mapWhile(Stream.of(1, 2, 0, 3),
                value -> value == 0 ? Optional.<String>empty() : Optional.of("v" + value)).toList(),
                List.of("v1", "v2"), "mapWhile excludes first rejection and suffix");
        equal(Streams.mapWhile(Stream.of(null, "x"),
                value -> Optional.of(value == null ? "null" : value)).toList(),
                List.of("null", "x"), "mapWhile accepts nullable source values");
        equal(Streams.distinctBy(Stream.of("a", "B", "A", "b", "c"),
                value -> value.toLowerCase(java.util.Locale.ROOT), 3).toList(),
                List.of("a", "B", "c"), "distinctBy keeps first value for each key");
        equal(Streams.distinctBy(Stream.of(null, null, 1, null, 1),
                Function.identity(), 2).toList(), Arrays.asList(null, 1), "distinctBy null key");
        equal(Streams.mergeSorted(Stream.of(null, 1, 3), Stream.of(null, 2, 3),
                NULLABLE_ORDER).toList(), Arrays.asList(null, null, 1, 2, 3, 3),
                "merge nullable elements and duplicates");

        // A subtype source and supertype callback remain usable without casts.
        Stream<Number> widened = Streams.pairwise(Stream.of(1, 2),
                (Number a, Number b) -> a.intValue() + b.intValue());
        equal(widened.toList(), List.of(3), "pairwise source/callback variance");
        Stream<Number> indexed = Streams.mapIndexed(Stream.of(1, 2),
                (Number value, long index) -> value.longValue() + index);
        equal(indexed.toList(), List.of(1L, 3L), "indexed source/callback variance");
    }

    private static void deterministicDifferentialCases() {
        Random random = new Random(0x5EED_18A6L);
        for (int iteration = 0; iteration < 160; iteration++) {
            List<Integer> values = randomValues(random, iteration % 49);
            boolean parallel = (iteration & 1) != 0;

            List<String> adjacent = new ArrayList<>();
            List<String> indexed = new ArrayList<>();
            List<Integer> separated = new ArrayList<>();
            List<Integer> prefix = new ArrayList<>();
            List<Integer> unique = new ArrayList<>();
            List<Integer> seenKeys = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                Integer value = values.get(i);
                if (i > 0) {
                    adjacent.add(pairLabel(values.get(i - 1), value));
                    separated.add(null);
                }
                indexed.add(i + ":" + value);
                separated.add(value);
                Integer key = keyOf(value);
                if (!seenKeys.contains(key)) {
                    seenKeys.add(key);
                    unique.add(value);
                }
            }
            for (Integer value : values) {
                if (value == null || value < 0) break;
                prefix.add(value * 3);
            }

            equal(mode(Streams.pairwise(input(values, parallel),
                    StreamCombinatorsTest::pairLabel), parallel).toList(), adjacent,
                    "random pairwise " + iteration);
            equal(mode(Streams.mapIndexed(input(values, parallel),
                    (value, index) -> index + ":" + value), parallel).toList(), indexed,
                    "random indexed " + iteration);
            equal(mode(Streams.intersperse(input(values, parallel), null), parallel).toList(),
                    separated, "random intersperse " + iteration);
            equal(mode(Streams.mapWhile(input(values, parallel), value ->
                    value == null || value < 0 ? Optional.<Integer>empty()
                            : Optional.of(value * 3)), parallel).toList(), prefix,
                    "random mapWhile " + iteration);
            equal(mode(Streams.distinctBy(input(values, parallel),
                    StreamCombinatorsTest::keyOf, 4), parallel).toList(), unique,
                    "random distinctBy " + iteration);

            List<Integer> left = new ArrayList<>(values);
            List<Integer> right = randomValues(random, (iteration * 7) % 43);
            left.sort(NULLABLE_ORDER);
            right.sort(NULLABLE_ORDER);
            List<Integer> merged = new ArrayList<>(left);
            merged.addAll(right);
            merged.sort(NULLABLE_ORDER);
            equal(mode(Streams.mergeSorted(input(left, parallel), input(right, !parallel),
                    NULLABLE_ORDER), parallel).toList(), merged, "random merge " + iteration);
        }
    }

    private static void metadataAndExecutionMode() throws Exception {
        for (Factory factory : factories()) {
            for (boolean ordered : new boolean[] {true, false}) {
                Probe<Integer> probe = new Probe<>(List.of(1, 2, 3), ordered);
                try (Stream<Integer> output = factory.make.apply(probe.stream().parallel())) {
                    check(!output.isParallel(), factory.name + " initially sequential");
                    check(probe.elements == 0, factory.name + " metadata does not traverse");
                    Spliterator<Integer> split = output.parallel().spliterator();
                    check(split.trySplit() == null, factory.name + " remains unsplittable");
                    check(split.getExactSizeIfKnown() == -1, factory.name + " unknown exact size");
                    check((split.characteristics() & (Spliterator.SIZED | Spliterator.SUBSIZED
                            | Spliterator.DISTINCT)) == 0, factory.name + " conservative metadata");
                    boolean merging = factory.name.equals("mergeSorted");
                    check(split.hasCharacteristics(Spliterator.ORDERED) == (ordered || merging),
                            factory.name + " encounter-order metadata");
                    check(split.hasCharacteristics(Spliterator.SORTED) == merging,
                            factory.name + " sortedness metadata");
                    check(split.hasCharacteristics(Spliterator.NONNULL)
                            == factory.name.equals("mapWhile"), factory.name + " non-null metadata");
                    if (!merging) expect(IllegalStateException.class, split::getComparator);
                    expect(NullPointerException.class, () -> split.tryAdvance(null));
                    check(probe.elements == 0, factory.name + " null consumer fails before traversal");
                    List<Integer> got = new ArrayList<>();
                    split.forEachRemaining(got::add);
                    check(!split.tryAdvance(got::add) && !split.tryAdvance(got::add),
                            factory.name + " stable exhaustion");
                    check(split.trySplit() == null, factory.name + " exhausted source does not split");
                    expect(NullPointerException.class, () -> split.tryAdvance(null));
                }
            }
        }
        try (Stream<Integer> output = Streams.mergeSorted(Stream.of(1), Stream.of(2), NULLABLE_ORDER)) {
            check(output.spliterator().getComparator() == NULLABLE_ORDER,
                    "merge retains supplied comparator identity");
        }
        // Distinct keys do not imply Object.equals-distinct values.
        EqualValue first = new EqualValue(1);
        EqualValue second = new EqualValue(2);
        equal(Streams.distinctBy(Stream.of(first, second), value -> value.key, 2).toList(),
                List.of(first, second), "different keys can retain equals-equal values");
        check(Streams.distinctBy(Stream.of(first, second), value -> value.key, 2)
                .distinct().count() == 1, "downstream distinct is not incorrectly elided");
    }

    private static void constructionAndSourceOwnership() throws Exception {
        for (Factory factory : factories()) {
            Probe<Integer> probe = new Probe<>(List.of(1, 2, 3), true);
            Stream<Integer> source = probe.stream();
            Stream<Integer> output = factory.make.apply(source);
            check(probe.attempts == 0 && probe.elements == 0,
                    factory.name + " construction performs no traversal");
            expect(IllegalStateException.class, source::spliterator);
            check(probe.closed == 0, factory.name + " acquiring source does not close it");
            output.close();
            check(probe.closed == 1 && probe.elements == 0,
                    factory.name + " close-before-use propagates without traversal");
        }

        int[] callbacks = {0};
        List<Stream<?>> pending = List.of(
                Streams.pairwise(Stream.of(1, 2), (a, b) -> { callbacks[0]++; return a + b; }),
                Streams.mapIndexed(Stream.of(1), (a, i) -> { callbacks[0]++; return a; }),
                Streams.mapWhile(Stream.of(1), a -> { callbacks[0]++; return Optional.of(a); }),
                Streams.distinctBy(Stream.of(1), a -> { callbacks[0]++; return a; }, 1),
                Streams.mergeSorted(Stream.of(1), Stream.of(2),
                        (a, b) -> { callbacks[0]++; return Integer.compare(a, b); }));
        check(callbacks[0] == 0, "construction invokes no mapper/key/comparator callbacks");
        pending.forEach(Stream::close);
        check(callbacks[0] == 0, "close-before-use invokes no callbacks");
    }

    private static void shortCircuitAndInfiniteInputs() {
        Probe<Integer> adjacent = new Probe<>(List.of(1, 2, 3, 4, 5), true);
        equal(Streams.pairwise(adjacent.stream(), Integer::sum).limit(2).toList(),
                List.of(3, 5), "pairwise short circuit values");
        check(adjacent.elements == 3, "pairwise consumes only needed adjacent prefix");
        Probe<Integer> index = new Probe<>(List.of(1, 2, 3), true);
        equal(Streams.mapIndexed(index.stream(), (value, position) -> position).limit(1).toList(),
                List.of(0L), "indexed short circuit");
        check(index.elements == 1, "indexed pulls one value for one output");
        Probe<Integer> separated = new Probe<>(List.of(1, 2, 3), true);
        equal(Streams.intersperse(separated.stream(), 0).limit(2).toList(),
                List.of(1, 0), "separator short circuit");
        check(separated.elements <= 2, "separator read-ahead bounded by one element");
        Probe<Integer> unique = new Probe<>(List.of(1, 2, 3), true);
        equal(Streams.distinctBy(unique.stream(), Function.identity(), 2).limit(2).toList(),
                List.of(1, 2), "distinct budget checked only for demanded prefix");
        check(unique.elements == 2, "distinct limit does not pull extra key");

        equal(Streams.pairwise(Stream.iterate(0, x -> x + 1), Integer::sum)
                .limit(6).toList(), List.of(1, 3, 5, 7, 9, 11), "infinite adjacent source");
        equal(Streams.mapIndexed(Stream.iterate(20, x -> x + 1), (value, position) -> position)
                .limit(6).toList(), List.of(0L, 1L, 2L, 3L, 4L, 5L), "infinite indexed source");
        equal(Streams.intersperse(Stream.iterate(0, x -> x + 1), -1).limit(6).toList(),
                List.of(0, -1, 1, -1, 2, -1), "infinite intersperse source");
        equal(Streams.mapWhile(Stream.iterate(0, x -> x + 1),
                x -> x < 4 ? Optional.of(x) : Optional.<Integer>empty()).toList(),
                List.of(0, 1, 2, 3), "mapWhile terminates infinite source");
        equal(Streams.distinctBy(Stream.iterate(0, x -> x + 1), x -> x % 3, 3)
                .limit(3).toList(), List.of(0, 1, 2), "bounded demand on infinite distinct source");
        equal(Streams.mergeSorted(Stream.iterate(0, x -> x + 2),
                Stream.iterate(1, x -> x + 2), Integer::compare).limit(6).toList(),
                List.of(0, 1, 2, 3, 4, 5), "infinite sorted merge");

        for (Factory factory : factories()) {
            Probe<Integer> probe = new Probe<>(List.of(1, 2, 3), true);
            try (Stream<Integer> output = factory.make.apply(probe.stream())) {
                check(output.limit(0).count() == 0, factory.name + " zero demand");
                check(probe.elements == 0, factory.name + " zero demand reads no elements");
            }
        }
    }

    private static void parallelStatefulConstructionIsLazy() throws Exception {
        for (Factory factory : factories()) {
            for (String operation : List.of("sorted", "distinct", "takeWhile")) {
                AtomicInteger traversed = new AtomicInteger();
                AtomicInteger predicates = new AtomicInteger();
                AtomicInteger closes = new AtomicInteger();
                Stream<Integer> upstream = Stream.of(1, 2, 2, 3).parallel()
                        .peek(value -> traversed.incrementAndGet())
                        .onClose(closes::incrementAndGet);
                Stream<Integer> source = switch (operation) {
                    case "sorted" -> upstream.sorted();
                    case "distinct" -> upstream.distinct();
                    case "takeWhile" -> upstream.takeWhile(value -> {
                        predicates.incrementAndGet();
                        return value <= 2;
                    });
                    default -> throw new AssertionError(operation);
                };
                try (Stream<Integer> output = factory.make.apply(source)) {
                    check(!output.isParallel(), factory.name + " normalizes " + operation + " output mode");
                    check(traversed.get() == 0 && predicates.get() == 0,
                            factory.name + " does not evaluate parallel upstream " + operation
                                    + " during construction");
                    expect(IllegalStateException.class, source::spliterator);
                }
                check(traversed.get() == 0 && predicates.get() == 0,
                        factory.name + " close-before-use does not evaluate " + operation);
                check(closes.get() == 1,
                        factory.name + " closes parallel upstream " + operation + " exactly once");
            }
        }
    }

    private static void mapWhileTermination() throws Exception {
        Probe<Integer> probe = new Probe<>(List.of(1, 2, 0, 9), true);
        List<Integer> calls = new ArrayList<>();
        try (Stream<Integer> output = Streams.mapWhile(probe.stream(), value -> {
            calls.add(value);
            return value == 0 ? Optional.empty() : Optional.of(value * 2);
        })) {
            Spliterator<Integer> split = output.spliterator();
            List<Integer> actual = new ArrayList<>();
            split.forEachRemaining(actual::add);
            equal(actual, List.of(2, 4), "mapWhile prefix result");
            equal(calls, List.of(1, 2, 0), "mapWhile sees rejection once");
            check(probe.elements == 3, "mapWhile consumes rejecting element");
            check(!split.tryAdvance(actual::add) && !split.tryAdvance(actual::add),
                    "mapWhile latches termination");
            check(probe.elements == 3 && calls.size() == 3, "mapWhile never resumes after empty");
        }
        int[] callsNull = {0};
        try (Stream<Integer> output = Streams.<Integer, Integer>mapWhile(Stream.of(1, 2), value -> {
            callsNull[0]++;
            return null;
        })) {
            expect(NullPointerException.class, output::toList);
            check(callsNull[0] == 1, "null Optional fails without mapping later input");
        }
    }

    private static void distinctKeyBudget() throws Exception {
        equal(Streams.distinctBy(Stream.<Integer>empty(), Function.identity(), 0).toList(),
                List.of(), "zero distinct budget permits empty input");
        try (Stream<Integer> output = Streams.distinctBy(Stream.of(1), Function.identity(), 0)) {
            expect(IllegalStateException.class, output::toList);
        }
        equal(Streams.distinctBy(Stream.of(1, 1, 2, 1, 2), Function.identity(), 2).toList(),
                List.of(1, 2), "duplicates consume no additional key budget");
        int[] calls = {0};
        try (Stream<Integer> output = Streams.distinctBy(Stream.of(1, 1, 2, 2, 3), value -> {
            calls[0]++;
            return value;
        }, 2)) {
            Spliterator<Integer> split = output.spliterator();
            List<Integer> actual = new ArrayList<>();
            check(split.tryAdvance(actual::add), "first key below budget");
            check(split.tryAdvance(actual::add), "second key at budget");
            expect(IllegalStateException.class, () -> split.tryAdvance(actual::add));
            equal(actual, List.of(1, 2), "exceeding key is never emitted");
            check(calls[0] == 5, "extractor called once per examined occurrence");
        }
        List<Tagged> values = List.of(new Tagged(2, "first-2"), new Tagged(1, "first-1"),
                new Tagged(2, "last-2"), new Tagged(3, "first-3"), new Tagged(1, "last-1"));
        equal(Streams.distinctBy(values.stream(), value -> new CollisionKey(value.key), 3).toList(),
                List.of(values.get(0), values.get(1), values.get(3)),
                "hash collisions do not collapse distinct keys");
    }

    private static void stableSortedMerge() {
        Tagged a = new Tagged(1, "left-a");
        Tagged b = new Tagged(1, "left-b");
        Tagged c = new Tagged(1, "right-a");
        Tagged d = new Tagged(1, "right-b");
        Tagged e = new Tagged(2, "left-c");
        Tagged f = new Tagged(2, "right-c");
        equal(Streams.mergeSorted(Stream.of(a, b, e), Stream.of(c, d, f),
                Comparator.comparingInt(Tagged::key)).toList(), List.of(a, b, c, d, e, f),
                "merge is stable with left-first comparator ties");
        equal(Streams.mergeSorted(Stream.<Integer>empty(), Stream.of(1, 2),
                Integer::compare).toList(), List.of(1, 2), "empty left merge");
        equal(Streams.mergeSorted(Stream.of(1, 2), Stream.<Integer>empty(),
                Integer::compare).toList(), List.of(1, 2), "empty right merge");
        equal(Streams.mergeSorted(Stream.of(9, 4, 0), Stream.of(8, 4, -1),
                Comparator.reverseOrder()).toList(), List.of(9, 8, 4, 4, 0, -1),
                "descending comparator defines merge order");
        Probe<Integer> left = new Probe<>(List.of(0, 2, 4, 6), true);
        Probe<Integer> right = new Probe<>(List.of(1, 3, 5, 7), true);
        equal(Streams.mergeSorted(left.stream(), right.stream(), Integer::compare).limit(3).toList(),
                List.of(0, 1, 2), "merge short-circuit prefix");
        check(left.elements + right.elements <= 5, "merge retains at most one pending head per input");
    }

    private static void callbackAndTraversalFailures() throws Exception {
        RuntimeException callback = new RuntimeException("callback witness");
        try (Stream<Integer> output = Streams.pairwise(Stream.of(1, 2), (a, b) -> { throw callback; })) {
            check(expect(RuntimeException.class, output::toList) == callback, "pair mapper exception identity");
        }
        try (Stream<Integer> output = Streams.mapIndexed(Stream.of(1), (a, i) -> { throw callback; })) {
            check(expect(RuntimeException.class, output::toList) == callback, "indexed mapper exception identity");
        }
        try (Stream<Integer> output = Streams.mapWhile(Stream.of(1), a -> { throw callback; })) {
            check(expect(RuntimeException.class, output::toList) == callback, "mapWhile mapper exception identity");
        }
        try (Stream<Integer> output = Streams.distinctBy(Stream.of(1), a -> { throw callback; }, 1)) {
            check(expect(RuntimeException.class, output::toList) == callback, "key extractor exception identity");
        }
        try (Stream<Integer> output = Streams.mergeSorted(Stream.of(1), Stream.of(2),
                (a, b) -> { throw callback; })) {
            check(expect(RuntimeException.class, output::toList) == callback, "comparator exception identity");
        }
        for (Factory factory : factories()) {
            RuntimeException failure = new RuntimeException(factory.name + " source failure");
            Probe<Integer> probe = new Probe<>(List.of(1, 2), true);
            probe.failure = failure;
            Stream<Integer> output = factory.make.apply(probe.stream());
            check(expect(RuntimeException.class, output::toList) == failure,
                    factory.name + " source exception identity");
            check(probe.closed == 0, factory.name + " traversal failure is not implicit close");
            output.close();
            check(probe.closed == 1, factory.name + " explicit close after traversal failure");

            try (Stream<Integer> consuming = factory.make.apply(Stream.of(1, 2))) {
                Spliterator<Integer> split = consuming.spliterator();
                check(expect(RuntimeException.class, () -> split.tryAdvance(value -> { throw callback; }))
                        == callback, factory.name + " downstream exception identity");
            }
        }
    }

    private static void closePropagation() throws Exception {
        for (Factory factory : factories()) {
            Probe<Integer> probe = new Probe<>(List.of(1, 2), true);
            Stream<Integer> output = factory.make.apply(probe.stream());
            output.toList();
            check(probe.closed == 0, factory.name + " exhaustion does not close source");
            output.close();
            output.close();
            check(probe.closed == 1, factory.name + " source closes exactly once");
            expect(IllegalStateException.class, output::toList);
        }
        List<String> order = new ArrayList<>();
        RuntimeException first = new RuntimeException("first close");
        RuntimeException second = new RuntimeException("second close");
        Stream<Integer> output = Streams.mergeSorted(
                Stream.of(1).onClose(() -> { order.add("first"); throw first; }),
                Stream.of(2).onClose(() -> { order.add("second"); throw second; }),
                Integer::compare);
        check(expect(RuntimeException.class, output::close) == first, "merge first close failure identity");
        equal(order, List.of("first", "second"), "merge attempts close in source order");
        check(Arrays.equals(first.getSuppressed(), new Throwable[] {second}),
                "merge second close failure suppressed");
        output.close();
        check(order.size() == 2, "failed close remains idempotent");
        RuntimeException same = new RuntimeException("shared close failure");
        Stream<Integer> shared = Streams.mergeSorted(Stream.of(1).onClose(() -> { throw same; }),
                Stream.of(2).onClose(() -> { throw same; }), Integer::compare);
        check(expect(RuntimeException.class, shared::close) == same, "merge does not self-suppress");
        check(same.getSuppressed().length == 0, "shared exception has no self-suppression");

        for (Factory factory : factories()) {
            RuntimeException failure = new RuntimeException(factory.name + " close witness");
            List<String> closeOrder = new ArrayList<>();
            Stream<Integer> source = Stream.of(1, 2).onClose(() -> {
                closeOrder.add("source");
                throw failure;
            });
            Stream<Integer> result = factory.make.apply(source).onClose(() -> closeOrder.add("result"));
            check(expect(RuntimeException.class, result::close) == failure,
                    factory.name + " close exception preserved");
            equal(closeOrder, List.of("source", "result"), factory.name + " later close handler still runs");
        }
    }

    private static void validationAndReusedInputs() throws Exception {
        expect(NullPointerException.class, () -> Streams.pairwise(null, (a, b) -> a));
        expect(NullPointerException.class, () -> Streams.mapIndexed(null, (a, i) -> a));
        expect(NullPointerException.class, () -> Streams.intersperse(null, 0));
        expect(NullPointerException.class, () -> Streams.mapWhile(null, Optional::ofNullable));
        expect(NullPointerException.class, () -> Streams.distinctBy(null, Function.identity(), 1));
        expect(NullPointerException.class, () -> Streams.mergeSorted(null, Stream.of(1), Integer::compare));
        expect(NullPointerException.class, () -> Streams.mergeSorted(Stream.of(1), null, Integer::compare));

        Stream<Integer> pairSource = Stream.of(1, 2);
        expect(NullPointerException.class, () -> Streams.pairwise(pairSource, null));
        check(pairSource.count() == 2, "pairwise mapper checked before source acquisition");
        Stream<Integer> indexSource = Stream.of(1);
        expect(NullPointerException.class, () -> Streams.mapIndexed(indexSource, null));
        check(indexSource.count() == 1, "indexed mapper checked before source acquisition");
        Stream<Integer> whileSource = Stream.of(1);
        expect(NullPointerException.class, () -> Streams.mapWhile(whileSource, null));
        check(whileSource.count() == 1, "mapWhile mapper checked before source acquisition");
        Stream<Integer> keySource = Stream.of(1);
        expect(NullPointerException.class, () -> Streams.distinctBy(keySource, null, 1));
        check(keySource.count() == 1, "distinct key extractor checked before source acquisition");
        Stream<Integer> negativeSource = Stream.of(1);
        expect(IllegalArgumentException.class, () -> Streams.distinctBy(negativeSource, Function.identity(), -1));
        check(negativeSource.count() == 1, "negative budget checked before source acquisition");
        Stream<Integer> mergeLeft = Stream.of(1);
        Stream<Integer> mergeRight = Stream.of(2);
        expect(NullPointerException.class, () -> Streams.mergeSorted(mergeLeft, mergeRight, null));
        check(mergeLeft.count() == 1 && mergeRight.count() == 1,
                "merge comparator checked before either source acquisition");
        Stream<Integer> same = Stream.of(1, 2);
        expect(IllegalArgumentException.class, () -> Streams.mergeSorted(same, same, Integer::compare));
        check(same.count() == 2, "merge same-reference refusal preserves unused source");

        for (Factory factory : factories()) {
            Stream<Integer> used = Stream.of(1, 2);
            used.count();
            expect(IllegalStateException.class, () -> factory.make.apply(used));
            Stream<Integer> closed = Stream.of(1, 2);
            closed.close();
            expect(IllegalStateException.class, () -> factory.make.apply(closed));
        }
        int[] closes = {0};
        Stream<Integer> first = Stream.of(1).onClose(() -> closes[0]++);
        Stream<Integer> second = Stream.of(2);
        second.count();
        expect(IllegalStateException.class, () -> Streams.mergeSorted(first, second, Integer::compare));
        check(closes[0] == 0, "failed merge construction retains caller resource ownership");
        first.close();
        check(closes[0] == 1, "caller can close source after failed merge construction");
    }

    private static void indexedOverflow() throws Exception {
        List<Long> calls = new ArrayList<>();
        try (Stream<Long> output = Streams.mapIndexed(Stream.of("a", "b", "c"), (value, index) -> {
            calls.add(index);
            return index;
        })) {
            Spliterator<Long> split = output.spliterator();
            setNextIndex(split, Long.MAX_VALUE - 1);
            List<Long> actual = new ArrayList<>();
            check(split.tryAdvance(actual::add) && split.tryAdvance(actual::add),
                    "largest representable indices are emitted");
            equal(actual, List.of(Long.MAX_VALUE - 1, Long.MAX_VALUE), "long index boundary values");
            expect(ArithmeticException.class, () -> split.tryAdvance(actual::add));
            equal(calls, actual, "overflow occurs before invoking mapper with wrapped index");
        }
        try (Stream<Long> output = Streams.mapIndexed(Stream.of("last"), (value, index) -> index)) {
            Spliterator<Long> split = output.spliterator();
            setNextIndex(split, Long.MAX_VALUE);
            check(split.tryAdvance(value -> check(value == Long.MAX_VALUE, "last valid index")),
                    "last valid element advances");
            check(!split.tryAdvance(value -> { throw new AssertionError("extra result"); }),
                    "exhaustion after maximum index is not overflow");
        }
    }

    // This one white-box hook reaches an otherwise infeasible 2^63-element
    // boundary. All operator values, ordering and lifetimes above are black-box.
    private static void setNextIndex(Spliterator<?> split, long value) throws Exception {
        Field field = split.getClass().getDeclaredField("index");
        check(field.getType() == long.class, "index state remains primitive long");
        field.setAccessible(true);
        field.setLong(split, value);
    }

    private static List<Factory> factories() {
        return List.of(
                new Factory("pairwise", source -> Streams.pairwise(source, Integer::sum)),
                new Factory("mapIndexed", source -> Streams.mapIndexed(source, (value, index) -> value)),
                new Factory("intersperse", source -> Streams.intersperse(source, -1)),
                new Factory("mapWhile", source -> Streams.mapWhile(source, Optional::ofNullable)),
                new Factory("distinctBy", source -> Streams.distinctBy(source, Function.identity(), 16)),
                new Factory("mergeSorted", source -> Streams.mergeSorted(source, Stream.empty(), Integer::compare)));
    }

    private static <T> Stream<T> input(List<T> values, boolean parallel) {
        return parallel ? values.parallelStream() : values.stream();
    }

    private static <T> Stream<T> mode(Stream<T> values, boolean parallel) {
        return parallel ? values.parallel() : values;
    }

    private static List<Integer> randomValues(Random random, int size) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < size; i++) values.add(random.nextInt(7) == 0 ? null : random.nextInt(9) - 4);
        return values;
    }

    private static String pairLabel(Integer first, Integer second) {
        return first == null && second == null ? null : first + ":" + second;
    }

    private static Integer keyOf(Integer value) {
        return value == null ? null : Math.floorMod(value, 3);
    }

    private record Factory(String name, UnaryOperator<Stream<Integer>> make) { }
    private record Tagged(int key, String label) { }

    private static final class CollisionKey {
        private final int value;
        CollisionKey(int value) { this.value = value; }
        @Override public int hashCode() { return 1; }
        @Override public boolean equals(Object other) {
            return other instanceof CollisionKey key && value == key.value;
        }
    }

    private static final class EqualValue {
        private final int key;
        EqualValue(int key) { this.key = key; }
        @Override public int hashCode() { return 0; }
        @Override public boolean equals(Object other) { return other instanceof EqualValue; }
    }

    private static final class Probe<T> implements Spliterator<T> {
        private final List<T> values;
        private final boolean ordered;
        private int position;
        private int attempts;
        private int elements;
        private int closed;
        private RuntimeException failure;

        Probe(List<T> values, boolean ordered) {
            this.values = values;
            this.ordered = ordered;
        }

        Stream<T> stream() { return StreamSupport.stream(this, false).onClose(() -> closed++); }

        @Override public boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action);
            attempts++;
            if (failure != null) throw failure;
            if (position == values.size()) return false;
            T value = values.get(position++);
            elements++;
            action.accept(value);
            return true;
        }

        @Override public Spliterator<T> trySplit() { return null; }
        @Override public long estimateSize() { return values.size() - position; }
        @Override public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL
                    | Spliterator.DISTINCT | (ordered ? Spliterator.ORDERED | Spliterator.SORTED : 0);
        }
        @Override public Comparator<? super T> getComparator() {
            if (!ordered) throw new IllegalStateException();
            return null;
        }
    }

    private static void equal(Object actual, Object expected, String message) {
        check(Objects.equals(actual, expected), message + "; expected=" + expected + ", actual=" + actual);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        checks++;
        try {
            action.run();
        } catch (Throwable actual) {
            if (type.isInstance(actual)) return type.cast(actual);
            throw new AssertionError("expected " + type.getName() + ", got " + actual, actual);
        }
        throw new AssertionError("expected " + type.getName());
    }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }
}
