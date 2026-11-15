/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Preserve primitive heap batch, duplicate, callback and lazy certification contracts
 * @modules jdk.m3.collections/jdk.m3.collections
 *          jdk.m3.collections/jdk.internal.m3.collections
 * @run main TheAlgorithmsHeapRefinementTest
 */

import jdk.m3.collections.LongLongPriorityQueue;
import jdk.internal.m3.collections.LongLazyPriorityFrontier;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;

/** Target-owned public-behavior oracles for the existing primitive heap owners. */
public final class TheAlgorithmsHeapRefinementTest {
    private long checks;
    private final MessageDigest digest;
    private final byte[] observation = new byte[Long.BYTES];

    private TheAlgorithmsHeapRefinementTest() {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private void equal(long expected, long actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + ": " + expected + " != " + actual);
        for (int i = 0; i < observation.length; i++) observation[i] = (byte) (actual >>> (8 * i));
        digest.update(observation);
    }

    private void require(boolean condition, String label) {
        equal(1, condition ? 1 : 0, label);
    }

    private void fails(Class<? extends Throwable> kind, Runnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("expected " + kind.getName(), failure);
        }
        throw new AssertionError("missing " + kind.getName());
    }

    private record Pair(long value, long priority) { }

    private static Comparator<Pair> pairOrder(boolean minFirst) {
        Comparator<Pair> order = Comparator.comparingLong(Pair::priority).thenComparingLong(Pair::value);
        return minFirst ? order : order.reversed();
    }

    private void queue(boolean minFirst) {
        Random random = new Random(0x5441505155455545L + (minFirst ? 1 : 0));
        LongLongPriorityQueue queue = new LongLongPriorityQueue(1, minFirst);
        PriorityQueue<Pair> oracle = new PriorityQueue<>(pairOrder(minFirst));
        for (int step = 0; step < 12_000; step++) {
            int action = random.nextInt(10);
            if (action < 4) {
                long value = random.nextInt(41) - 20L;
                long priority = random.nextInt(17) - 8L;
                require(queue.offer(value, priority), "offer");
                oracle.add(new Pair(value, priority));
            } else if (action < 7) {
                int count = step % 29 == 0 ? 129 : random.nextInt(8);
                long[] values = new long[count];
                long[] priorities = new long[count];
                for (int i = 0; i < count; i++) {
                    values[i] = random.nextInt(41) - 20L;
                    priorities[i] = (i & 31) == 0 ? Long.MIN_VALUE : random.nextInt(17) - 8L;
                    oracle.add(new Pair(values[i], priorities[i]));
                }
                equal(count, queue.offerAll(values, priorities), "batch count");
            } else if (action == 7 && !oracle.isEmpty()) {
                Pair expected = oracle.remove();
                queue.poll((v, p) -> {
                    equal(expected.value(), v, "poll value");
                    equal(expected.priority(), p, "poll priority");
                });
            } else if (action == 8 && !oracle.isEmpty()) {
                oracle.remove();
                long value = random.nextLong(), priority = random.nextLong();
                queue.replaceTop(value, priority);
                oracle.add(new Pair(value, priority));
            } else if (action == 9 && (step & 127) == 0) {
                queue.clear();
                oracle.clear();
            }
            equal(oracle.size(), queue.size(), "queue size");
            equal(queue.capacity() * 16L, queue.payloadBytes(), "paired lanes");
            // Capacity is public: include it in the before/after observation root.
            equal(queue.capacity(), queue.capacity(), "capacity observation");
            if (!oracle.isEmpty()) {
                equal(oracle.peek().value(), queue.peekValue(), "peek value");
                equal(oracle.peek().priority(), queue.peekPriority(), "peek priority");
            }
        }
        while (!oracle.isEmpty()) {
            Pair expected = oracle.remove();
            queue.poll((v, p) -> {
                equal(expected.value(), v, "drain value");
                equal(expected.priority(), p, "drain priority");
            });
        }
        fails(NoSuchElementException.class, queue::peekValue);
        fails(NullPointerException.class, () -> queue.poll(null));
        fails(IllegalArgumentException.class, () -> queue.offerAll(new long[1], new long[0]));
        equal(0, queue.size(), "failed batch size");
    }

    private void admission() {
        List<String> batchTrace = new ArrayList<>();
        List<String> singleTrace = new ArrayList<>();
        LongLongPriorityQueue batch = new LongLongPriorityQueue(1, true,
                (v, p, tv, tp, n) -> {
                    batchTrace.add(v + ":" + p + ":" + tv + ":" + tp + ":" + n);
                    return (v & 3) != 0;
                });
        LongLongPriorityQueue single = new LongLongPriorityQueue(1, true,
                (v, p, tv, tp, n) -> {
                    singleTrace.add(v + ":" + p + ":" + tv + ":" + tp + ":" + n);
                    return (v & 3) != 0;
                });
        long[] values = {4, 3, 3, 2, 1, 0, Long.MIN_VALUE, Long.MAX_VALUE};
        long[] priorities = {8, 6, 6, 4, 2, 0, Long.MIN_VALUE, Long.MAX_VALUE};
        int accepted = 0;
        for (int i = 0; i < values.length; i++) if (single.offer(values[i], priorities[i])) accepted++;
        equal(accepted, batch.offerAll(values, priorities), "filtered batch count");
        require(batchTrace.equals(singleTrace), "admission root/callback order");
        while (!single.isEmpty()) {
            equal(single.peekValue(), batch.peekValue(), "filtered value");
            equal(single.peekPriority(), batch.peekPriority(), "filtered priority");
            single.poll((v, p) -> { });
            batch.poll((v, p) -> { });
        }
    }

    private static long score(long id) {
        if (id == Long.MIN_VALUE || id == Long.MAX_VALUE) return id;
        return ((id * 31L) % 113L) - 56L;
    }

    private record Ranked(long id, long exact, long serial) { }

    private void frontier() {
        Random random = new Random(0x54414c415a594850L);
        long[] calls = {0};
        LongLazyPriorityFrontier queue = new LongLazyPriorityFrontier(1, id -> {
            calls[0]++;
            return score(id);
        });
        Comparator<Ranked> order = Comparator.comparingLong(Ranked::exact).reversed()
                .thenComparingLong(Ranked::id).thenComparingLong(Ranked::serial);
        PriorityQueue<Ranked> oracle = new PriorityQueue<>(order);
        long serial = 0;
        for (int step = 0; step < 6000; step++) {
            if (oracle.isEmpty() || random.nextInt(10) < 7) {
                long id = step % 73 == 0 ? Long.MIN_VALUE : random.nextInt(21) - 10L;
                long exact = score(id);
                long upper = exact == Long.MIN_VALUE ? exact : exact + random.nextInt(1000);
                long before = calls[0];
                if ((step & 7) == 0) {
                    queue.offerAll(new long[] {id, id}, new long[] {upper, upper});
                    oracle.add(new Ranked(id, exact, serial++));
                    oracle.add(new Ranked(id, exact, serial++));
                } else {
                    queue.offer(id, upper);
                    oracle.add(new Ranked(id, exact, serial++));
                }
                equal(before, calls[0], "lazy offer must not score");
            } else {
                Ranked expected = oracle.remove();
                equal(expected.id(), queue.peekId(), "lazy peek ID");
                long evaluated = calls[0];
                equal(expected.exact(), queue.peekPriority(), "lazy peek score");
                equal(expected.id(), queue.pollId(), "lazy poll ID");
                equal(evaluated, calls[0], "certified top not rescored");
            }
            equal(oracle.size(), queue.size(), "frontier size");
            equal(calls[0], queue.evaluations(), "exact evaluation accounting");
        }
        while (!oracle.isEmpty()) {
            Ranked expected = oracle.remove();
            equal(expected.exact(), queue.peekPriority(), "lazy drain score");
            equal(expected.id(), queue.pollId(), "lazy drain ID");
        }
        equal(serial, queue.evaluations(), "each occurrence evaluated once");
        queue.clear();
        equal(0, queue.evaluations(), "clear resets evaluations");
        queue.offer(Long.MAX_VALUE, Long.MAX_VALUE);
        equal(Long.MAX_VALUE, queue.pollId(), "signed maximum");
        fails(NoSuchElementException.class, queue::peekId);

        int[] attempts = {0};
        LongLazyPriorityFrontier retry = new LongLazyPriorityFrontier(id -> {
            if (attempts[0]++ == 0) throw new IllegalStateException("retry");
            return id;
        });
        retry.offer(7, 20);
        fails(IllegalStateException.class, retry::pollId);
        equal(1, retry.size(), "failed score retains entry");
        equal(0, retry.evaluations(), "failed score not published");
        equal(7, retry.pollId(), "retry result");
        LongLazyPriorityFrontier invalid = new LongLazyPriorityFrontier(id -> 21);
        invalid.offer(1, 20);
        fails(IllegalArgumentException.class, invalid::pollId);
        equal(1, invalid.size(), "bound violation retains entry");
        equal(0, invalid.evaluations(), "bound violation unresolved");
        LongLazyPriorityFrontier[] reentrant = new LongLazyPriorityFrontier[1];
        reentrant[0] = new LongLazyPriorityFrontier(id -> {
            reentrant[0].clear();
            return 0;
        });
        reentrant[0].offer(1, 1);
        fails(IllegalStateException.class, reentrant[0]::peekId);
        equal(1, reentrant[0].size(), "callback reentry retains entry");
    }

    public static String run() {
        TheAlgorithmsHeapRefinementTest proof = new TheAlgorithmsHeapRefinementTest();
        proof.queue(true);
        proof.queue(false);
        proof.admission();
        proof.frontier();
        return "checks=" + proof.checks + ",sha256=" + HexFormat.of().formatHex(proof.digest.digest());
    }

    public static void main(String[] args) {
        System.out.println("TheAlgorithmsHeapRefinementTest PASS " + run());
    }
}
