/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary New stream delegates, retained indexed behavior, and bounded nullable keyed folding
 * @modules jdk.m3.collections java.base/java.util.function.ext
 * @run main/othervm ObjectStreamCombinatorsContract
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import jdk.m3.collections.ObjectStream;

/** Public-owner contract tests using actual stream and collection implementations. */
public final class ObjectStreamCombinatorsContract {
    private static int checks;

    private record Item(String key, int value) { }
    private record NumericItem(Integer key, int value) { }

    private ObjectStreamCombinatorsContract() { }

    public static void main(String[] args) {
        wrapperBoundaries();
        replayLiveAndOneShot();
        eagerValidationAndLazyTraversal();
        legacyIndexedSizeAndCountElision();
        delegatedFailureClosure();
        nullableFoldAndEncounterOrder();
        foldBudgetsAndValidation();
        foldFailureIdentity();
        differentialFold();
        System.out.println("OBJECT_STREAM_COMBINATORS_PASS checks=" + checks);
    }

    private static List<Function<ObjectStream<Integer>, ObjectStream<String>>> operations() {
        return List.of(
                input -> input.pairwise((a, b) -> a + "/" + b),
                input -> input.mapIndexed((value, index) -> index + ":" + value),
                input -> input.intersperse(-1).map(Object::toString),
                input -> input.mapWhile(value -> value < 4
                        ? Optional.of(value.toString()) : Optional.empty()));
    }

    private static List<Function<ObjectStream<Integer>, ObjectStream<String>>> newDelegatingOperations() {
        return List.of(
                input -> input.pairwise((a, b) -> a + "/" + b),
                input -> input.mapWhile(value -> value < 4
                        ? Optional.of(value.toString()) : Optional.empty()));
    }

    private static List<String> expected(int operation, List<Integer> input) {
        List<String> result = new ArrayList<>();
        switch (operation) {
            case 0 -> {
                for (int i = 1; i < input.size(); i++) {
                    result.add(input.get(i - 1) + "/" + input.get(i));
                }
            }
            case 1 -> {
                for (int i = 0; i < input.size(); i++) result.add(i + ":" + input.get(i));
            }
            case 2 -> {
                for (int i = 0; i < input.size(); i++) {
                    if (i != 0) result.add("-1");
                    result.add(input.get(i).toString());
                }
            }
            case 3 -> {
                for (int value : input) {
                    if (value >= 4) break;
                    result.add(Integer.toString(value));
                }
            }
            default -> throw new AssertionError("unknown operation");
        }
        return result;
    }

    private static void wrapperBoundaries() {
        equal(List.of(), ObjectStream.<Integer>snapshot(List.of())
                .pairwise(Integer::sum).toBoundedList(0), "empty adjacent pairs");
        equal(List.of(), ObjectStream.snapshot(List.of(1))
                .pairwise(Integer::sum).toBoundedList(0), "singleton adjacent pairs");
        equal(Arrays.asList("1/null", "null/3"),
                ObjectStream.snapshot(Arrays.asList(1, null, 3))
                        .pairwise((a, b) -> a + "/" + b).toBoundedList(2),
                "pairwise retains null elements");
        equal(Arrays.asList(null, null), ObjectStream.snapshot(List.of(1, 2, 3))
                .pairwise((a, b) -> (String) null).toBoundedList(2), "null pairwise results");
        equal(List.of(), ObjectStream.<Integer>snapshot(List.of())
                .intersperse(null).toBoundedList(0), "empty intersperse");
        equal(List.of(1), ObjectStream.snapshot(List.of(1))
                .intersperse(null).toBoundedList(1), "singleton intersperse");
        equal(Arrays.asList(1, null, 2), ObjectStream.snapshot(List.of(1, 2))
                .intersperse(null).toBoundedList(3), "nullable separator");
        equal(Arrays.asList("0:null", "1:2"), ObjectStream.snapshot(Arrays.asList(null, 2))
                .mapIndexed((value, index) -> index + ":" + value).toBoundedList(2),
                "indexed mapping retains null inputs");
        equal(Arrays.asList(null, null), ObjectStream.snapshot(List.of(1, 2))
                .mapIndexed((value, index) -> (String) null).toBoundedList(2),
                "indexed mapping permits null outputs");
        equal(List.of(), ObjectStream.snapshot(List.of(1, 2))
                .mapWhile(value -> Optional.<Integer>empty()).toBoundedList(0),
                "first empty optional stops the traversal");
    }

    private static void replayLiveAndOneShot() {
        var operations = operations();
        for (int i = 0; i < operations.size(); i++) {
            var operation = operations.get(i);
            CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(1, 2, 3));
            ObjectStream<String> live = operation.apply(ObjectStream.live(values));
            check(live.replayable(), "live wrapper is replayable");
            equal(0, values.opens.get(), "constructing a wrapper does not open its source");
            equal(expected(i, values), live.toBoundedList(32), "first live evaluation");
            values.add(4);
            equal(expected(i, values), live.toBoundedList(32), "live replay observes mutation");
            equal(2, values.opens.get(), "each live replay opens its own traversal");
            equal(2, values.closes.get(), "each live replay closes its own traversal");

            List<Integer> captured = new ArrayList<>(values);
            ObjectStream<String> snapshot = operation.apply(ObjectStream.snapshot(values));
            values.add(5);
            equal(expected(i, captured), snapshot.toBoundedList(32), "snapshot remains detached");
            equal(expected(i, captured), snapshot.toBoundedList(32), "snapshot replays from zero");

            AtomicInteger closed = new AtomicInteger();
            ObjectStream<String> once = operation.apply(ObjectStream.once(
                    Stream.of(1, 2, 3, 4).onClose(closed::incrementAndGet)));
            check(!once.replayable(), "one-shot wrapper remains one-shot");
            equal(0, closed.get(), "building a one-shot wrapper does not close input");
            equal(expected(i, List.of(1, 2, 3, 4)), once.toBoundedList(32), "one-shot output");
            equal(1, closed.get(), "one-shot output closes its source");
            expect(IllegalStateException.class, once::count);
            equal(1, closed.get(), "failed replay cannot close source twice");
        }
    }

    private static void eagerValidationAndLazyTraversal() {
        CloseTrackingList<Integer> invalidValues = new CloseTrackingList<>(List.of(1, 2));
        var invalid = ObjectStream.live(invalidValues);
        expect(NullPointerException.class, () -> invalid.pairwise(null));
        expect(NullPointerException.class, () -> invalid.mapIndexed(null));
        expect(NullPointerException.class, () -> invalid.mapWhile(null));
        invalid.intersperse(null);
        equal(0, invalidValues.opens.get(), "mapper validation precedes source opening");

        for (var operation : operations()) {
            CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(1, 2, 3));
            AtomicInteger reads = new AtomicInteger();
            var plan = operation.apply(ObjectStream.live(values).peek(v -> reads.incrementAndGet()));
            try (Stream<String> opened = plan.stream()) {
                check(!opened.isParallel(), "delegated traversal remains sequential");
                equal(1, values.opens.get(), "opening wrapper acquires one upstream traversal");
                equal(0, reads.get(), "opening wrapper does not traverse source elements");
            }
            equal(1, values.closes.get(), "closing unconsumed wrapper closes upstream");
            equal(0, reads.get(), "unused close does not pull input");
        }

        AtomicInteger pairReads = new AtomicInteger();
        AtomicInteger pairCalls = new AtomicInteger();
        equal(List.of(3), ObjectStream.snapshot(List.of(1, 2, 3, 4))
                .peek(v -> pairReads.incrementAndGet()).pairwise((a, b) -> {
                    pairCalls.incrementAndGet();
                    return a + b;
                }).limit(1).toBoundedList(1), "short-circuit adjacent pair");
        equal(2, pairReads.get(), "one pair consumes two elements");
        equal(1, pairCalls.get(), "one pair invokes mapper once");

        AtomicInteger indexedReads = new AtomicInteger();
        List<Long> indexes = new ArrayList<>();
        equal(2L, ObjectStream.snapshot(List.of(4, 5, 6))
                .peek(v -> indexedReads.incrementAndGet()).mapIndexed((value, index) -> {
                    indexes.add(index);
                    return value;
                }).limit(2).count(), "indexed short-circuit count");
        equal(List.of(0L, 1L), indexes, "primitive encounter indexes start at zero");
        equal(2, indexedReads.get(), "indexed mapping does not over-consume");

        AtomicInteger separatorReads = new AtomicInteger();
        equal(List.of(1, 0), ObjectStream.snapshot(List.of(1, 2, 3))
                .peek(v -> separatorReads.incrementAndGet()).intersperse(0)
                .limit(2).toBoundedList(2), "separator short circuit");
        equal(2, separatorReads.get(), "separator retains exactly one following element");

        AtomicInteger whileReads = new AtomicInteger();
        AtomicInteger whileCalls = new AtomicInteger();
        var stopped = ObjectStream.snapshot(List.of(1, 2, 3, 4))
                .peek(v -> whileReads.incrementAndGet()).mapWhile(value -> {
                    whileCalls.incrementAndGet();
                    return value == 3 ? Optional.<Integer>empty() : Optional.of(value * 2);
                });
        try (Stream<Integer> opened = stopped.stream()) {
            Iterator<Integer> iterator = opened.iterator();
            equal(2, iterator.next(), "first optional result");
            equal(4, iterator.next(), "second optional result");
            check(!iterator.hasNext() && !iterator.hasNext(), "empty optional latches termination");
        }
        equal(3, whileReads.get(), "terminating input is consumed but later input is untouched");
        equal(3, whileCalls.get(), "terminated mapper is not reinvoked by exhaustion checks");
    }

    private static void legacyIndexedSizeAndCountElision() {
        CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(4, 5, 6));
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger calls = new AtomicInteger();
        List<Long> indexes = new ArrayList<>();
        var indexed = ObjectStream.live(values).peek(value -> reads.incrementAndGet())
                .mapIndexed((value, index) -> {
                    calls.incrementAndGet();
                    indexes.add(index);
                    return value;
                });
        try (Stream<Integer> opened = indexed.stream()) {
            Spliterator<Integer> cursor = opened.spliterator();
            check(cursor.hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED),
                    "accepted indexed map retains sized source metadata");
            equal(3L, cursor.getExactSizeIfKnown(), "accepted indexed map retains exact source size");
        }
        equal(0, calls.get(), "inspecting size does not invoke indexed mapper");
        // The accepted mapIndexed body is a JDK map stage: a sized Stream.count()
        // may elide both peek and mapper. ObjectStream.count() separately traverses.
        try (Stream<Integer> opened = indexed.stream()) {
            equal(3L, opened.count(), "sized JDK count retains accepted result");
        }
        equal(0, reads.get(), "sized JDK count retains accepted source elision");
        equal(0, calls.get(), "sized JDK count retains accepted mapper elision");
        equal(2, values.closes.get(), "metadata and count traversals both close");
        equal(List.of(4, 5, 6), indexed.toBoundedList(3), "later materialization still maps each value");
        equal(List.of(0L, 1L, 2L), indexes, "later materialization starts at the original zero index");
        equal(3, calls.get(), "materialization invokes mapper exactly once per value");
        equal(3, values.closes.get(), "materialization closes the third traversal");
    }

    private static void delegatedFailureClosure() {
        // Factory-construction ownership is asserted for the two new delegates.
        // Legacy mapIndexed and intersperse keep their accepted implementation.
        for (var operation : newDelegatingOperations()) {
            CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(1, 2, 3));
            RuntimeException closing = new IllegalStateException("factory close failure");
            values.closeFailure = closing;
            values.consumeOnOpen = true;
            Throwable failure = capture(() -> operation.apply(ObjectStream.live(values)).stream());
            check(failure instanceof IllegalStateException, "used input rejects factory construction");
            equal(1, values.closes.get(), "failed delegated construction closes acquired input");
            equal(1, failure.getSuppressed().length, "construction retains close suppression");
            check(failure.getSuppressed()[0] == closing, "construction close identity");
        }

        for (int stage = 0; stage < 3; stage++) {
            CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(1, 2, 3));
            RuntimeException closing = new IllegalStateException("traversal close failure");
            values.closeFailure = closing;
            Throwable marker = stage == 1 ? new AssertionError("indexed mapper error")
                    : new IllegalArgumentException("mapper failure");
            var input = ObjectStream.live(values);
            ObjectStream<Integer> result = switch (stage) {
                case 0 -> input.pairwise((a, b) -> fail(marker));
                case 1 -> input.mapIndexed((value, index) -> fail(marker));
                case 2 -> input.mapWhile(value -> fail(marker));
                default -> throw new AssertionError();
            };
            Throwable actual = capture(() -> result.toBoundedList(8));
            check(actual == marker, "mapper failure identity");
            equal(1, values.closes.get(), "failed traversal closes input once");
            equal(1, actual.getSuppressed().length, "failed traversal retains close suppression");
            check(actual.getSuppressed()[0] == closing, "traversal close identity");
        }
        CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(1, 2));
        expect(NullPointerException.class, () -> ObjectStream.live(values)
                .mapWhile(value -> null).toBoundedList(2));
        equal(1, values.closes.get(), "null optional refusal closes source");
    }

    private static void nullableFoldAndEncounterOrder() {
        String firstB = new String("b");
        List<Item> input = Arrays.asList(new Item(firstB, 1), new Item("a", 2),
                new Item(null, 3), new Item(new String("b"), 4), new Item("a", 5),
                new Item(null, 6), new Item("b", 7));
        CloseTrackingList<Item> values = new CloseTrackingList<>(input);
        AtomicInteger keys = new AtomicInteger();
        AtomicInteger seeds = new AtomicInteger();
        AtomicInteger folders = new AtomicInteger();
        Map<String, Integer> result = ObjectStream.live(values).foldByKey(item -> {
            keys.incrementAndGet();
            return item.key();
        }, 3, () -> {
            seeds.incrementAndGet();
            return null;
        }, (Integer state, Item item) -> {
            folders.incrementAndGet();
            return item.value() == 3 || item.value() == 5 ? null
                    : (state == null ? 0 : state) + item.value();
        });
        equal(Arrays.asList("b", "a", null), new ArrayList<>(result.keySet()),
                "first-key encounter order with a null key");
        check(result.keySet().iterator().next() == firstB, "first equal key object retained");
        equal(12, result.get("b"), "replacement integer state accumulated in source order");
        check(result.containsKey("a") && result.get("a") == null,
                "null replacement state remains an existing key");
        equal(6, result.get(null), "null initial state is replaced without reseeding");
        equal(3, seeds.get(), "one initializer per admitted key, including nullable states");
        equal(input.size(), keys.get(), "one key extraction per element");
        equal(input.size(), folders.get(), "one folder invocation per element");
        equal(1, values.closes.get(), "result is returned only after source closure");
        expect(UnsupportedOperationException.class, () -> result.put("x", 1));
        expect(UnsupportedOperationException.class, () -> result.remove("a"));
        expect(UnsupportedOperationException.class,
                () -> result.entrySet().iterator().next().setValue(9));

        AtomicInteger nullSeeds = new AtomicInteger();
        Map<String, String> nulls = ObjectStream.snapshot(Arrays.asList(null, "x", null))
                .foldByKey(value -> value, 2, () -> {
                    nullSeeds.incrementAndGet();
                    return null;
                }, (state, value) -> value);
        equal(Arrays.asList(null, "x"), new ArrayList<>(nulls.keySet()), "null payload keys");
        check(nulls.containsKey(null) && nulls.get(null) == null, "null payload state");
        equal(2, nullSeeds.get(), "null state is not treated as absent");
    }

    private static void foldBudgetsAndValidation() {
        CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(1));
        var plan = ObjectStream.live(values);
        expect(NullPointerException.class, () -> plan.foldByKey(null, 1, () -> 0, Integer::sum));
        expect(NullPointerException.class,
                () -> plan.foldByKey(value -> value, 1, null, Integer::sum));
        expect(NullPointerException.class,
                () -> plan.foldByKey(value -> value, 1, () -> 0, null));
        expect(IllegalArgumentException.class,
                () -> plan.foldByKey(value -> value, -1, () -> 0, Integer::sum));
        equal(0, values.opens.get(), "fold validation happens before source opening");

        AtomicInteger seeds = new AtomicInteger();
        Map<Integer, Integer> empty = ObjectStream.<Integer>snapshot(List.of())
                .foldByKey(value -> value, 0, () -> seeds.incrementAndGet(), Integer::sum);
        check(empty.isEmpty(), "zero key budget admits an empty source");
        equal(0, seeds.get(), "empty fold has no seed");
        expect(UnsupportedOperationException.class, empty::clear);

        AtomicInteger keys = new AtomicInteger();
        AtomicInteger folders = new AtomicInteger();
        CloseTrackingList<Integer> bounded = new CloseTrackingList<>(List.of(1, 2, 1, 3, 4));
        expect(IllegalStateException.class, () -> ObjectStream.live(bounded)
                .foldByKey(value -> {
                    keys.incrementAndGet();
                    return value;
                }, 2, () -> {
                    seeds.incrementAndGet();
                    return null;
                }, (Integer state, Integer value) -> {
                    folders.incrementAndGet();
                    return null;
                }));
        equal(4, keys.get(), "rejected key is extracted once and no later key is touched");
        equal(2, seeds.get(), "over-budget key never receives a seed");
        equal(3, folders.get(), "duplicates at full budget still fold; new key is refused");
        equal(1, bounded.closes.get(), "budget refusal closes input");

        CloseTrackingList<Integer> zero = new CloseTrackingList<>(List.of(1));
        AtomicInteger zeroSeeds = new AtomicInteger();
        expect(IllegalStateException.class, () -> ObjectStream.live(zero)
                .foldByKey(value -> value, 0, () -> zeroSeeds.incrementAndGet(), Integer::sum));
        equal(0, zeroSeeds.get(), "nonempty zero-budget fold refuses before initialization");
        equal(1, zero.closes.get(), "zero-budget refusal closes input");
    }

    private static void foldFailureIdentity() {
        for (int failureStage = 0; failureStage < 3; failureStage++) {
            final int stage = failureStage;
            CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(1, 2));
            RuntimeException closing = new IllegalStateException("fold close failure");
            values.closeFailure = closing;
            Throwable marker = stage == 1 ? new AssertionError("seed error")
                    : new IllegalArgumentException("fold callback failure");
            int[] calls = new int[3];
            Throwable actual = capture(() -> ObjectStream.live(values).foldByKey(value -> {
                calls[0]++;
                if (stage == 0) return fail(marker);
                return value;
            }, 2, () -> {
                calls[1]++;
                if (stage == 1) return fail(marker);
                return 0;
            }, (Integer state, Integer value) -> {
                calls[2]++;
                return fail(marker);
            }));
            check(actual == marker, "fold callback failure identity");
            equal(1, values.closes.get(), "fold callback failure closes input once");
            equal(1, actual.getSuppressed().length, "fold close failure is suppressed");
            check(actual.getSuppressed()[0] == closing, "fold close failure identity");
            equal(1, calls[0], "failure stops before the second element");
            equal(stage >= 1 ? 1 : 0, calls[1], "seed follows successful key extraction");
            equal(stage == 2 ? 1 : 0, calls[2], "folder follows successful seeding");
        }
        CloseTrackingList<Integer> values = new CloseTrackingList<>(List.of(1));
        RuntimeException closeOnly = new IllegalStateException("successful fold cannot close");
        values.closeFailure = closeOnly;
        Throwable actual = capture(() -> ObjectStream.live(values)
                .foldByKey(value -> value, 1, () -> 0, Integer::sum));
        check(actual == closeOnly, "failed input closure prevents publishing a successful map");
    }

    private static void differentialFold() {
        Random random = new Random(20261118L);
        for (int trial = 0; trial < 150; trial++) {
            List<NumericItem> items = new ArrayList<>();
            List<Integer> order = new ArrayList<>();
            int count = random.nextInt(45);
            for (int i = 0; i < count; i++) {
                int rawKey = random.nextInt(5);
                Integer key = rawKey == 4 ? null : rawKey;
                items.add(new NumericItem(key, random.nextInt(31) - 15));
                if (!order.contains(key)) order.add(key);
            }
            Map<Integer, Integer> expected = new LinkedHashMap<>();
            for (Integer key : order) {
                Integer state = 100;
                for (NumericItem item : items) {
                    if (Objects.equals(key, item.key())) {
                        state = item.value() % 7 == 0 ? null
                                : (state == null ? 1 : state) - item.value();
                    }
                }
                expected.put(key, state);
            }
            AtomicInteger seeds = new AtomicInteger();
            Map<Integer, Integer> actual = ObjectStream.snapshot(items).foldByKey(
                    NumericItem::key, 5, () -> {
                        seeds.incrementAndGet();
                        return 100;
                    }, (state, item) -> item.value() % 7 == 0 ? null
                            : (state == null ? 1 : state) - item.value());
            equal(expected, actual, "independent per-key scan oracle trial " + trial);
            equal(order, new ArrayList<>(actual.keySet()), "differential encounter order");
            equal(order.size(), seeds.get(), "differential nullable seed count");
        }
    }

    private static final class CloseTrackingList<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;
        private final AtomicInteger opens = new AtomicInteger();
        private final AtomicInteger closes = new AtomicInteger();
        private RuntimeException closeFailure;
        private boolean consumeOnOpen;

        private CloseTrackingList(Collection<? extends E> source) { super(source); }

        @Override public Stream<E> stream() {
            opens.incrementAndGet();
            Stream<E> source = super.stream().onClose(() -> {
                closes.incrementAndGet();
                if (closeFailure != null) throw closeFailure;
            });
            if (consumeOnOpen) source.count();
            return source;
        }
    }

    private static <T> T fail(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        throw (Error) failure;
    }

    private static Throwable capture(Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException | Error failure) {
            return failure;
        }
        throw new AssertionError("expected failure");
    }

    private static void expect(Class<? extends Throwable> expected, Runnable operation) {
        Throwable failure = capture(operation);
        check(expected.isInstance(failure), "expected " + expected + ", received " + failure);
    }

    private static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual),
                message + ": expected=" + expected + ", actual=" + actual);
    }

    private static void check(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }
}
