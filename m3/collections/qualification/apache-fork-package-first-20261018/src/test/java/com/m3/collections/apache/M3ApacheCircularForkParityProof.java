// SPDX-License-Identifier: Apache-2.0
package com.m3.collections.apache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

/** Existing M3 bit-state/offset ring versus original and package-relocated Apache rings. */
public final class M3ApacheCircularForkParityProof {
    private static long checks;
    private M3ApacheCircularForkParityProof() { }

    private static void eq(Object expected, Object actual, String what) {
        checks++;
        if (!Objects.equals(expected, actual))
            throw new AssertionError(what + " expected=" + expected + " got=" + actual);
    }
    private static void equalArray(Object[] expected, Object[] actual, String what) {
        checks++;
        if (!Arrays.equals(expected, actual))
            throw new AssertionError(what + " expected=" + Arrays.toString(expected)
                    + " got=" + Arrays.toString(actual));
    }
    private static Object serialized(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {out.writeObject(value);}
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return in.readObject();
        }
    }
    private static void compare(
            org.apache.commons.collections4.queue.CircularFifoQueue<Integer> original,
            com.m3.collections.apache.queue.CircularFifoQueue<Integer> relocated,
            M3CircularFifoQueue<Integer> target, int max) {
        eq(original.size(), relocated.size(), "relocated size");
        eq(original.size(), target.size(), "M3 size");
        eq(max, relocated.maxSize(), "relocated max");
        eq(max, target.maxSize(), "M3 max");
        eq(original.isFull(), relocated.isFull(), "relocated isFull");
        eq(original.isFull(), target.isFull(), "M3 isFull");
        eq(original.isAtFullCapacity(), relocated.isAtFullCapacity(), "relocated at capacity");
        eq(original.isAtFullCapacity(), target.isAtFullCapacity(), "M3 at capacity");
        eq(original.peek(), relocated.peek(), "relocated peek");
        eq(original.peek(), target.peek(), "M3 peek");
        equalArray(original.toArray(), relocated.toArray(), "relocated order");
        equalArray(original.toArray(), target.toArray(), "M3 order");
        for (int index = 0; index < original.size(); index++) {
            eq(original.get(index), relocated.get(index), "relocated logical index");
            eq(original.get(index), target.get(index), "M3 logical index");
        }
    }
    private static void removeIterator(
            org.apache.commons.collections4.queue.CircularFifoQueue<Integer> original,
            com.m3.collections.apache.queue.CircularFifoQueue<Integer> relocated,
            M3CircularFifoQueue<Integer> target, int ordinal) {
        Iterator<Integer> a = original.iterator();
        Iterator<Integer> b = relocated.iterator();
        Iterator<Integer> c = target.iterator();
        for (int i = 0; i <= ordinal; i++) {
            Integer expected = a.next();
            eq(expected, b.next(), "relocated iterator");
            eq(expected, c.next(), "M3 iterator");
        }
        a.remove(); b.remove(); c.remove();
    }
    private static void run() throws Exception {
        Random random = new Random(0x4d33415041434845L);
        for (int size : new int[]{1, 2, 3, 7, 16, 31, 64}) {
            var original = new org.apache.commons.collections4.queue.CircularFifoQueue<Integer>(size);
            var relocated = new com.m3.collections.apache.queue.CircularFifoQueue<Integer>(size);
            var target = new M3CircularFifoQueue<Integer>(size);
            for (int step = 0; step < 600; step++) {
                switch (random.nextInt(8)) {
                    case 0, 1, 2 -> {
                        int value = random.nextInt(41) - 20;
                        boolean expected = original.add(value);
                        eq(expected, relocated.add(value), "relocated add");
                        eq(expected, target.add(value), "M3 add");
                    }
                    case 3 -> {
                        Integer expected = original.poll();
                        eq(expected, relocated.poll(), "relocated poll");
                        eq(expected, target.poll(), "M3 poll");
                    }
                    case 4 -> {
                        if (!original.isEmpty()) {
                            Integer expected = original.remove();
                            eq(expected, relocated.remove(), "relocated remove");
                            eq(expected, target.remove(), "M3 remove");
                        }
                    }
                    case 5 -> {
                        if (!original.isEmpty()) {
                            removeIterator(original, relocated, target, random.nextInt(original.size()));
                        }
                    }
                    case 6 -> {
                        if (random.nextInt(9) == 0) {
                            original.clear(); relocated.clear(); target.clear();
                        }
                    }
                    default -> {
                        eq(original.peek(), relocated.peek(), "relocated peek");
                        eq(original.peek(), target.peek(), "M3 peek");
                    }
                }
                compare(original, relocated, target, size);
                if (step % 163 == 0) {
                    equalArray(((org.apache.commons.collections4.queue.CircularFifoQueue<?>)
                            serialized(original)).toArray(),
                            ((com.m3.collections.apache.queue.CircularFifoQueue<?>)
                            serialized(relocated)).toArray(), "relocated serialization");
                    equalArray(((org.apache.commons.collections4.queue.CircularFifoQueue<?>)
                            serialized(original)).toArray(),
                            ((M3CircularFifoQueue<?>) serialized(target)).toArray(),
                            "M3 serialization");
                }
            }
        }
    }
    private static void errors() {
        var a = new org.apache.commons.collections4.queue.CircularFifoQueue<Integer>(2);
        var b = new com.m3.collections.apache.queue.CircularFifoQueue<Integer>(2);
        var c = new M3CircularFifoQueue<Integer>(2);
        boolean first = false, second = false, third = false;
        try {a.add(null);} catch (NullPointerException ex) {first = true;}
        try {b.add(null);} catch (NullPointerException ex) {second = true;}
        try {c.add(null);} catch (NullPointerException ex) {third = true;}
        eq(true, first, "Apache null");
        eq(first, second, "relocated null");
        eq(first, third, "M3 null");
        first = false; second = false; third = false;
        try {a.remove();} catch (NoSuchElementException ex) {first = true;}
        try {b.remove();} catch (NoSuchElementException ex) {second = true;}
        try {c.remove();} catch (NoSuchElementException ex) {third = true;}
        eq(true, first, "Apache empty");
        eq(first, second, "relocated empty");
        eq(first, third, "M3 empty");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no args");
        errors(); run();
        System.out.println("PASS M3_APACHE_FORK_QUEUE_PARITY checks=" + checks);
    }
}
