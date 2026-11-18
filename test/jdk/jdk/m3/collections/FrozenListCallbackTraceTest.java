/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary FrozenList untrusted source iterator callback trace stays identical
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenListCallbackTraceTest
 */
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import jdk.internal.m3.collections.FrozenList;

public final class FrozenListCallbackTraceTest {
    private static long trace;
    private static int iterators;
    private static int hasNext;
    private static int next;
    private static int signatures;

    public static void main(String[] args) {
        for (int count : new int[] {0, 1, 2, 7, 8, 9, 10, 11, 15, 16, 64, 256, 1024}) {
            exercise(count);
        }
        System.out.println("FrozenListCallbackTraceTest PASS: " + signatures + " signatures");
    }

    private static void exercise(int count) {
        trace = 0xcbf29ce484222325L;
        iterators = 0;
        hasNext = 0;
        next = 0;
        AbstractCollection<Integer> source = new AbstractCollection<>() {
            @Override public int size() {
                throw new AssertionError("untrusted size() callback");
            }
            @Override public Iterator<Integer> iterator() {
                iterators++;
                trace = Long.rotateLeft(trace ^ 7, 3);
                return new Iterator<>() {
                    private int cursor;
                    @Override public boolean hasNext() {
                        hasNext++;
                        trace = Long.rotateLeft(trace ^ (cursor * 17L + 11), 5);
                        return cursor < count;
                    }
                    @Override public Integer next() {
                        next++;
                        trace = Long.rotateLeft(trace ^ (cursor * 19L + 13), 7);
                        if (cursor == count) throw new NoSuchElementException();
                        return cursor++ & 15;
                    }
                };
            }
        };
        FrozenList<Integer> result = FrozenList.copyOf(source);
        if (result.size() != count) throw new AssertionError("list size");
        System.out.println(count + "\t" + iterators + "\t" + hasNext + "\t" + next
                + "\t" + Long.toUnsignedString(trace, 16));
        signatures++;
    }
}
