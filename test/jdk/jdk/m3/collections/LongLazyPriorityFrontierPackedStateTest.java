/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Dynamic offset heap and packed resolution bits preserve lazy priority semantics
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm LongLazyPriorityFrontierPackedStateTest
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongUnaryOperator;
import jdk.internal.m3.collections.LongLazyPriorityFrontier;

public final class LongLazyPriorityFrontierPackedStateTest {
    private static long checks;
    private static long callbackFingerprint;

    public static void main(String[] args) {
        for (int seed = 0; seed < 80; seed++) randomTrace(seed);
        longBatchAcrossWordBoundaries();
        failuresAreNonDestructive();
        invalidArguments();
        System.out.printf("LongLazyPriorityFrontierPackedStateTest PASS: %d assertions fingerprint=%016x%n",
                checks, callbackFingerprint);
    }

    private static void randomTrace(int seed) {
        Random random = new Random(0xB17B17B17L + seed * 8191L);
        TraceScorer scorer = new TraceScorer();
        LongLazyPriorityFrontier actual = new LongLazyPriorityFrontier(seed % 7, scorer);
        ArrayList<Candidate> expected = new ArrayList<>();
        long serial = 0;
        for (int step = 0; step < 2900; step++) {
            int action = random.nextInt(1000);
            if (action < 430 && expected.size() < 350) {
                long id = randomId(random);
                long exact = exactScore(id);
                long bound = optimistic(exact, random);
                actual.offer(id, bound);
                expected.add(new Candidate(id, exact, serial++));
            } else if (action < 570 && expected.size() < 350) {
                int count = random.nextInt(Math.min(13, 351 - expected.size()));
                long[] ids = new long[count], bounds = new long[count];
                for (int i = 0; i < count; i++) {
                    ids[i] = randomId(random);
                    bounds[i] = optimistic(exactScore(ids[i]), random);
                    expected.add(new Candidate(ids[i], exactScore(ids[i]), serial++));
                }
                actual.offerAll(ids, bounds);
                Arrays.fill(ids, Long.MIN_VALUE + 1);
                Arrays.fill(bounds, Long.MIN_VALUE + 1);
            } else if (action < 770 && !expected.isEmpty()) {
                Candidate best = best(expected);
                long id = actual.peekId();
                long score = actual.peekPriority();
                check(id == best.id, "peek stable best ID");
                check(score == best.exact, "peek certified score");
                long before = actual.evaluations();
                check(actual.peekId() == id && actual.peekPriority() == score,
                        "repeat peek idempotent");
                check(actual.evaluations() == before, "repeat peek does not reevaluate");
            } else if (action < 955 && !expected.isEmpty()) {
                Candidate best = best(expected);
                check(actual.pollId() == best.id, "poll exact best");
                check(expected.remove(best), "remove oracle best identity");
            } else if (action >= 985) {
                actual.clear();
                expected.clear();
                serial = 0;
                scorer.resetEpoch();
                check(actual.evaluations() == 0, "clear resets evaluations");
            } else {
                check(actual.isEmpty() == expected.isEmpty(), "empty state");
            }
            check(actual.size() == expected.size(), "heap size");
            check(actual.isEmpty() == expected.isEmpty(), "heap empty");
            check(actual.evaluations() == scorer.epochCalls, "one count per successful scorer call");
            check(actual.payloadBytes() >= (long) (expected.size()) * 3 * Long.BYTES,
                    "aligned dynamic payload");
        }
        while (!expected.isEmpty()) {
            Candidate best = best(expected);
            check(actual.pollId() == best.id, "final drain sorted");
            expected.remove(best);
        }
        callbackFingerprint = Long.rotateLeft(callbackFingerprint, 7) ^ scorer.fingerprint;
    }

    private static void longBatchAcrossWordBoundaries() {
        final int size = 32769;
        TraceScorer scorer = new TraceScorer();
        LongLazyPriorityFrontier actual = new LongLazyPriorityFrontier(1, scorer);
        long[] ids = new long[size], bounds = new long[size];
        for (int id = 0; id < size; id++) {
            ids[id] = id;
            bounds[id] = exactScore(id);
        }
        actual.offerAll(ids, bounds);
        Arrays.fill(ids, -1L);
        Arrays.fill(bounds, Long.MIN_VALUE);
        check(actual.size() == size, "large batch sizes");
        Integer[] expected = new Integer[size];
        for (int id = 0; id < size; id++) expected[id] = id;
        Arrays.sort(expected, Comparator.<Integer>comparingLong(LongLazyPriorityFrontierPackedStateTest::exactScore)
                .reversed().thenComparingInt(id -> id));
        for (int i = 0; i < size; i++) {
            check(actual.peekId() == expected[i], "large sorted peek");
            check(actual.peekPriority() == exactScore(expected[i]), "large exact peek");
            check(actual.pollId() == expected[i], "large sorted poll");
        }
        check(actual.isEmpty(), "batch drained");
        check(actual.evaluations() == size, "each batch score evaluated exactly once");
        callbackFingerprint = Long.rotateLeft(callbackFingerprint, 7) ^ scorer.fingerprint;
        actual.clear();
        check(actual.payloadBytes() > 0, "clear retains dynamic capacity");
        check(actual.evaluations() == 0, "clear reset");
        actual.offer(42, exactScore(42));
        check(actual.pollId() == 42, "rebuild after clear");
    }

    private static void failuresAreNonDestructive() {
        AtomicReference<LongLazyPriorityFrontier> reference = new AtomicReference<>();
        boolean[] reenter = {true};
        LongUnaryOperator reentrant = id -> {
            if (reenter[0]) reference.get().offer(100, 100);
            return exactScore(id);
        };
        LongLazyPriorityFrontier heap = new LongLazyPriorityFrontier(reentrant);
        reference.set(heap);
        heap.offer(7, 100);
        expect(IllegalStateException.class, heap::peekId);
        check(heap.size() == 1 && heap.evaluations() == 0, "reentry preserved state");
        reenter[0] = false;
        check(heap.peekId() == 7 && heap.peekPriority() == exactScore(7),
                "retry after reentry");
        check(heap.pollId() == 7 && heap.isEmpty(), "reentrant completion");

        LongLazyPriorityFrontier violatesBound = new LongLazyPriorityFrontier(id -> 12);
        violatesBound.offer(8, 11);
        expect(IllegalArgumentException.class, violatesBound::peekId);
        expect(IllegalArgumentException.class, violatesBound::pollId);
        check(violatesBound.size() == 1 && violatesBound.evaluations() == 0,
                "optimistic bound refusal preserved");
        violatesBound.clear();
        check(violatesBound.isEmpty(), "clear after rejected scorer");

        LongLazyPriorityFrontier throwsScorer = new LongLazyPriorityFrontier(id -> {
            throw new ArithmeticException("scorer failure");
        });
        throwsScorer.offer(1, 50);
        expect(ArithmeticException.class, throwsScorer::peekPriority);
        check(throwsScorer.size() == 1 && throwsScorer.evaluations() == 0,
                "throwing scorer preserved unresolved state");
    }

    private static void invalidArguments() {
        expect(IllegalArgumentException.class,
                () -> new LongLazyPriorityFrontier(-1, id -> id));
        expect(NullPointerException.class,
                () -> new LongLazyPriorityFrontier(null));
        LongLazyPriorityFrontier queue = new LongLazyPriorityFrontier(id -> id);
        expect(NoSuchElementException.class, queue::peekId);
        expect(NoSuchElementException.class, queue::peekPriority);
        expect(NoSuchElementException.class, queue::pollId);
        expect(NullPointerException.class, () -> queue.offerAll(null, new long[0]));
        expect(NullPointerException.class, () -> queue.offerAll(new long[0], null));
        expect(IllegalArgumentException.class,
                () -> queue.offerAll(new long[2], new long[1]));
        queue.offerAll(new long[0], new long[0]);
        check(queue.size() == 0, "empty batch");
    }

    private static Candidate best(ArrayList<Candidate> list) {
        Candidate winner = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            Candidate next = list.get(i);
            if (next.exact > winner.exact
                    || (next.exact == winner.exact && next.id < winner.id)
                    || (next.exact == winner.exact && next.id == winner.id
                        && next.serial < winner.serial)) winner = next;
        }
        return winner;
    }

    private static long randomId(Random random) {
        return switch (random.nextInt(12)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> -1L;
            case 3 -> 0L;
            default -> random.nextInt(2001) - 1000;
        };
    }

    private static long exactScore(long id) {
        if (id == Long.MIN_VALUE) return Long.MIN_VALUE;
        if (id == Long.MAX_VALUE) return Long.MAX_VALUE;
        return id % 257L;
    }

    private static long optimistic(long score, Random random) {
        int slack = random.nextInt(801);
        return score > Long.MAX_VALUE - slack ? Long.MAX_VALUE : score + slack;
    }

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) throw new AssertionError(reason + " at assertion " + checks);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("expected " + type + " got " + error, error);
        }
        throw new AssertionError("expected " + type);
    }

    private record Candidate(long id, long exact, long serial) { }

    private static final class TraceScorer implements LongUnaryOperator {
        long epochCalls;
        long fingerprint;
        @Override
        public long applyAsLong(long id) {
            epochCalls++;
            fingerprint = Long.rotateLeft(fingerprint ^ id, 11) * 0x9E3779B97F4A7C15L;
            return exactScore(id);
        }
        void resetEpoch() { epochCalls = 0; }
    }
}
