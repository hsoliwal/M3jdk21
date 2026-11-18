/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Verify offset arrays, indexed maps, adjacent maps, stable run masks and lazy zipped streams
 * @modules jdk.m3.collections
 * @run main/othervm ObjectStreamArrayZipTest
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import jdk.m3.collections.ObjectStream;

public final class ObjectStreamArrayZipTest {
    private static long checks;
    private ObjectStreamArrayZipTest() { }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static void eq(Object expected, Object actual, String label) {
        check(Objects.equals(expected, actual), label + ": expected=" + expected + " actual=" + actual);
    }
    private static void fails(Class<? extends Throwable> kind, Runnable task) {
        checks++;
        try { task.run(); }
        catch (Throwable error) {
            if (kind.isInstance(error)) return;
            throw new AssertionError("wrong exception type", error);
        }
        throw new AssertionError("missing exception " + kind);
    }

    private static void windowsAndIndexed() {
        Integer[] values = {99, null, 1, 1, 2, 3, 88};
        ObjectStream<Integer> window = ObjectStream.arrayWindow(values, 1, 5);
        eq(Arrays.asList(null, 1, 1, 2, 3), window.toBoundedList(5), "initial live window");
        values[3] = 10;
        eq(Arrays.asList(null, 1, 10, 2, 3), window.toBoundedList(5), "visible mutation");
        eq(List.of("0:null", "1:1", "2:10"), window
                .mapIndexed((value, index) -> index + ":" + value)
                .limit(3).toBoundedList(3), "primitive index map");
        eq(List.of("0:null", "1:1"), window
                .mapIndexed((value, index) -> index + ":" + value)
                .limit(2).toBoundedList(2), "reopened index resets to zero");
        fails(IndexOutOfBoundsException.class,
                () -> ObjectStream.arrayWindow(values, Integer.MAX_VALUE, 1));
        fails(IndexOutOfBoundsException.class, () -> ObjectStream.arrayWindow(values, -1, 1));
        fails(IndexOutOfBoundsException.class, () -> ObjectStream.arrayWindow(values, 1, -1));
        fails(NullPointerException.class, () -> ObjectStream.arrayWindow(null, 0, 0));
        eq(List.of(), ObjectStream.arrayWindow(values, 2, 0).toBoundedList(0), "empty window");
    }

    private static void pairsAndRuns() {
        eq(List.of("null:1", "1:10", "10:2"), ObjectStream
                .arrayWindow(new Integer[] {null,1,10,2,3}, 0, 5)
                .mapAdjacent((a,b) -> a + ":" + b).limit(3).toBoundedList(3),
                "one previous value");
        eq(List.of(), ObjectStream.snapshot(List.of(1)).mapAdjacent(Integer::sum)
                .toBoundedList(0), "singleton has no pair");
        eq(Arrays.asList(1,2,null,3), ObjectStream
                .snapshot(Arrays.asList(1,1,2,2,null,null,3,3))
                .distinctAdjacent().toBoundedList(4), "adjacent equality including null");
        eq(List.of(1,2,1), ObjectStream.snapshot(List.of(1,1,2,2,1,1))
                .distinctAdjacent().toBoundedList(3), "later repeats retained");
    }

    private static void zipAndClosure() {
        eq(List.of("1x", "2y"), ObjectStream.zip(
                ObjectStream.snapshot(List.of(1,2,3)),
                ObjectStream.snapshot(List.of("x","y")),
                (a,b) -> a + b).toBoundedList(2), "shortest source bound");
        eq(List.of(), ObjectStream.zip(ObjectStream.snapshot(List.<Integer>of()),
                ObjectStream.snapshot(List.of(1,2)), Integer::sum).toBoundedList(0),
                "empty left source");
        var oneShot = ObjectStream.once(Stream.of(3, 4));
        eq(List.of(9), ObjectStream.zip(oneShot, ObjectStream.once(Stream.of(6)),
                Integer::sum).toBoundedList(1), "one-shot zip");
        fails(IllegalStateException.class, oneShot::count);
        AtomicInteger closesA = new AtomicInteger();
        AtomicInteger closesB = new AtomicInteger();
        var first = ObjectStream.once(Stream.of(1,2,3).onClose(closesA::incrementAndGet));
        var second = ObjectStream.once(Stream.of(8,9).onClose(closesB::incrementAndGet));
        eq(List.of(9), ObjectStream.zip(first, second, Integer::sum)
                .limit(1).toBoundedList(1), "short circuit zip");
        eq(1, closesA.get(), "left close");
        eq(1, closesB.get(), "right close");
        var exhausted = ObjectStream.once(Stream.of(99));
        exhausted.count();
        AtomicInteger closedOnOpenFailure = new AtomicInteger();
        var left = ObjectStream.once(Stream.of(1).onClose(closedOnOpenFailure::incrementAndGet));
        fails(IllegalStateException.class,
                () -> ObjectStream.zip(left, exhausted, Integer::sum).count());
        eq(1, closedOnOpenFailure.get(), "close first when second opener refuses");
    }

    private static void randomizedComposition() {
        Random random = new Random(0x0b5e71c51eaL);
        for (int trial = 0; trial < 160; trial++) {
            int length = random.nextInt(120);
            Integer[] data = new Integer[length + 5];
            for (int i = 0; i < length; i++) {
                data[i + 2] = random.nextInt(8) == 0 ? null : random.nextInt(15) - 7;
            }
            var stream = ObjectStream.arrayWindow(data, 2, length);
            var ref = Arrays.asList(data).subList(2, length + 2);
            List<String> indexed = stream.mapIndexed((v,i) -> i + ":" + v).toBoundedList(length);
            for (int i = 0; i < length; i++) {
                eq(i + ":" + ref.get(i), indexed.get(i), "indexed ordinal trial " + trial);
            }
            List<String> adjacent = new ArrayList<>();
            for (int i = 1; i < length; i++) {
                adjacent.add(ref.get(i-1) + ":" + ref.get(i));
            }
            eq(adjacent, stream.mapAdjacent((a,b) -> a + ":" + b)
                    .toBoundedList(Math.max(0, length - 1)), "adjacent trial " + trial);
            List<Integer> runs = new ArrayList<>();
            for (Integer value : ref) {
                if (runs.isEmpty() || !Objects.equals(runs.get(runs.size()-1), value)) {
                    runs.add(value);
                }
            }
            eq(runs, stream.distinctAdjacent().toBoundedList(length), "adjacent distinct trial " + trial);
            List<Integer> other = new ArrayList<>();
            for (int i = 0; i < length/2; i++) other.add(i);
            List<String> pairRef = new ArrayList<>();
            for (int i = 0; i < other.size(); i++) pairRef.add(ref.get(i) + ":" + other.get(i));
            eq(pairRef, ObjectStream.zip(stream, ObjectStream.snapshot(other),
                    (a,b) -> a + ":" + b).toBoundedList(other.size()),
                    "zip trial " + trial);
        }
    }

    public static void main(String[] args) {
        windowsAndIndexed();
        pairsAndRuns();
        zipAndClosure();
        randomizedComposition();
        System.out.println("ObjectStreamArrayZipTest PASS checks=" + checks);
    }
}
