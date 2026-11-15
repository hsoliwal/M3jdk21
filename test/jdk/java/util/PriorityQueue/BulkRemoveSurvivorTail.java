/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary Preserve PriorityQueue bulk removal callback order, suffix identity and failures
 * @modules java.base/java.util:open
 * @run main BulkRemoveSurvivorTail
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.Predicate;

public class BulkRemoveSurvivorTail {
    // Frozen from the exact original owner before the candidate was materialized.
    private static final String EXPECTED = "6f2acfc5f528b91e76a335fb76b5ac26b9356bfae0d1f672d29c95085c3e2141";
    private static final Field QUEUE;
    private static Trace active;
    private static int checks;
    private static int cases;
    private static long predicates;
    private static long comparisons;
    private static final StringBuilder roots = new StringBuilder();

    static {
        try {
            QUEUE = PriorityQueue.class.getDeclaredField("queue");
            QUEUE.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] arguments) throws Exception {
        for (int size : new int[] {0, 1, 2, 3, 7, 16, 63, 64, 65, 127, 257}) {
            for (int mode = 0; mode < 4; mode++) {
                for (int pattern = 0; pattern < 8; pattern++) {
                    for (int operation = 0; operation < 3; operation++) {
                        exercise(size, mode, pattern, operation, 0, 0);
                    }
                }
            }
        }
        for (int size : new int[] {1, 3, 17, 65}) {
            for (int mode = 0; mode < 4; mode++) {
                for (int operation = 0; operation < 3; operation++) {
                    for (int fault = 1; fault <= 6; fault++) {
                        for (int at : new int[] {0, size / 2, size - 1}) {
                            exercise(size, mode, 2, operation, fault, at);
                        }
                    }
                }
            }
        }
        nullArguments();
        selfMembership();
        String root = hash(roots.toString());
        check(EXPECTED.equals(root), "behavior root differs: expected=" + EXPECTED + " actual=" + root);
        int boundaryCases = maskWordBoundaries();
        System.out.println("PRIORITY_QUEUE_BULK_REMOVE_PASS cases=" + cases + " checks=" + checks
                + " boundaryCases=" + boundaryCases + " predicates=" + predicates
                + " comparisons=" + comparisons + " root=" + root);
    }

    private static void exercise(int size, int mode, int pattern, int operation,
                                 int fault, int at) throws Exception {
        String name = size + ":" + mode + ":" + pattern + ":" + operation + ":" + fault + ":" + at;
        PriorityQueue<Item> queue = mode == 0 ? new PriorityQueue<>(size + 5)
                : new PriorityQueue<>(size + 5, new Ordering(mode));
        Random random = new Random(0x50117L + size * 31L + mode);
        for (int id = 0; id < size; id++) {
            queue.add(new Item(id, random.nextInt(11)));
        }
        Object[] before = queue.toArray();
        Object[] backing = (Object[]) QUEUE.get(queue);
        Iterator<Item> stale = queue.iterator();
        Trace trace = new Trace(queue, before, pattern, fault, at);
        boolean changed = false;
        String failure = "none";
        active = trace;
        try {
            if (operation == 0) {
                changed = queue.removeIf(trace);
            } else {
                AbstractCollection<Item> membership = new AbstractCollection<>() {
                    public int size() { return before.length; }
                    public Iterator<Item> iterator() { return List.<Item>of().iterator(); }
                    public boolean contains(Object value) {
                        boolean selected = trace.test((Item) value);
                        return operation == 1 ? selected : !selected;
                    }
                };
                changed = operation == 1 ? queue.removeAll(membership) : queue.retainAll(membership);
            }
        } catch (RuntimeException problem) {
            failure = problem.getClass().getName() + ":" + problem.getMessage();
        } finally {
            active = null;
        }
        trace.events.append("result=").append(changed).append(" failure=").append(failure)
                .append(" size=").append(queue.size()).append(" array=").append(Arrays.toString(queue.toArray()))
                .append(" backing=").append(Arrays.toString((Object[]) QUEUE.get(queue))).append('\n');
        try {
            stale.next();
            throw new AssertionError("bulk operation did not invalidate prior iterator: " + name);
        } catch (ConcurrentModificationException expected) {
            checks++;
        }
        if (fault == 0) {
            check(failure.equals("none"), name + " unexpected " + failure);
            check(trace.visits == size, name + " predicate count");
            List<Item> survivors = new ArrayList<>();
            for (int i = 0; i < before.length; i++) {
                if (!selected(pattern, i, size)) survivors.add((Item) before[i]);
            }
            check(changed == (survivors.size() != size), name + " changed result");
            check(queue.size() == survivors.size(), name + " size");
            check(QUEUE.get(queue) == backing, name + " backing identity");
            for (int i = queue.size(); i < backing.length; i++) {
                check(backing[i] == null, name + " retained removed reference at " + i);
            }
            for (Item item : queue) {
                check(survivors.stream().anyMatch(value -> value == item), name + " survivor identity");
            }
            byte[] serialized = serialize(queue);
            trace.events.append("serialized=").append(hash(serialized)).append('\n');
            PriorityQueue<?> restored;
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
                restored = (PriorityQueue<?>) input.readObject();
            }
            check(restored.size() == queue.size(), name + " serialized size");
            trace.events.append("restored=").append(Arrays.toString(restored.toArray())).append('\n');
            Item previous = null;
            while (!queue.isEmpty()) {
                Item item = queue.remove();
                check(previous == null || compare(mode, previous, item) <= 0, name + " priority order");
                check(survivors.remove(item), name + " unexpected or repeated survivor");
                trace.events.append("poll=").append(item).append('\n');
                previous = item;
            }
            check(survivors.isEmpty(), name + " missing survivor");
        }
        predicates += trace.visits;
        comparisons += trace.compares;
        record(name, trace.events.toString());
    }

    private static boolean selected(int pattern, int index, int size) {
        return switch (pattern) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> index == 0;
            case 3 -> index == size / 2;
            case 4 -> index == size - 1;
            case 5 -> (index & 1) == 0;
            case 6 -> index == 1 || index == 63 || index == 64 || index == 65;
            case 7 -> index < size / 4;
            default -> throw new AssertionError(pattern);
        };
    }

    private static int maskWordBoundaries() {
        int tested = 0;
        for (int size : new int[] {63, 64, 65, 127, 128, 129, 257}) {
            for (int first : new int[] {0, 1, 31, 63, 64}) {
                if (first >= size) continue;
                for (int distance : new int[] {0, 1, 63, 64, 65, 127, 128}) {
                    int second = first + distance;
                    PriorityQueue<Integer> queue = new PriorityQueue<>();
                    for (int i = 0; i < size; i++) queue.add(i);
                    int[] visits = {0};
                    check(queue.removeIf(value -> {
                        visits[0]++;
                        return value == first || value == second;
                    }), "boundary removal result");
                    check(visits[0] == size, "boundary predicate count");
                    for (int i = 0; i < size; i++) {
                        if (i != first && i != second) {
                            check(queue.remove() == i, "cross-word survivor/order at " + i
                                    + " first=" + first + " second=" + second + " size=" + size);
                        }
                    }
                    check(queue.isEmpty(), "boundary removal left an element");
                    tested++;
                }
            }
        }
        return tested;
    }

    private static final class Trace implements Predicate<Item> {
        final PriorityQueue<Item> queue;
        final Object[] before;
        final int pattern;
        final int fault;
        final int at;
        final StringBuilder events = new StringBuilder();
        int visits;
        int compares;
        boolean compareMutation;

        Trace(PriorityQueue<Item> queue, Object[] before, int pattern, int fault, int at) {
            this.queue = queue;
            this.before = before;
            this.pattern = pattern;
            this.fault = fault;
            this.at = at;
        }

        public boolean test(Item item) {
            int index = visits++;
            events.append("P:").append(index).append(':').append(item)
                    .append(':').append(queue.size()).append(':').append(queue.peek())
                    .append(':').append(Arrays.toString(queue.toArray())).append('\n');
            if (fault == 0) {
                Object[] snapshot = queue.toArray();
                check(Arrays.equals(snapshot, before), "predicate observed premature movement");
                check(item == before[index], "predicate identity or order changed");
                check(queue.spliterator().getExactSizeIfKnown() == before.length, "read reentrancy size");
            }
            if (index == at) {
                if (fault == 1) throw new CallbackFailure("predicate@" + index);
                if (fault == 2) queue.offer(new Item(1000, -1));
                if (fault == 3) queue.clear();
                if (fault == 5 && before.length > 1) ((Item) before[before.length - 1]).key = -9;
            }
            return selected(pattern, index, before.length);
        }

        void comparison(Item left, Item right) {
            int index = compares++;
            events.append("C:").append(index).append(':').append(left).append(':').append(right)
                    .append(':').append(queue.size()).append(':').append(Arrays.toString(queue.toArray())).append('\n');
            if (fault == 4 && index == at) throw new CallbackFailure("comparator@" + index);
            if (fault == 6 && index == at && !compareMutation) {
                compareMutation = true;
                queue.offer(new Item(1001, 19));
            }
        }
    }

    private static final class Item implements Comparable<Item>, Serializable {
        private static final long serialVersionUID = 1L;
        final int id;
        int key;
        Item(int id, int key) { this.id = id; this.key = key; }
        public int compareTo(Item other) {
            if (active != null) active.comparison(this, other);
            return Integer.compare(key, other.key);
        }
        public String toString() { return id + ":" + key; }
    }

    private static final class Ordering implements Comparator<Item>, Serializable {
        private static final long serialVersionUID = 1L;
        final int mode;
        Ordering(int mode) { this.mode = mode; }
        public int compare(Item left, Item right) {
            if (active != null) active.comparison(left, right);
            return BulkRemoveSurvivorTail.compare(mode, left, right);
        }
    }

    private static int compare(int mode, Item left, Item right) {
        return mode == 3 ? 0 : mode == 2 ? Integer.compare(right.key, left.key)
                : Integer.compare(left.key, right.key);
    }

    private static final class CallbackFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        CallbackFailure(String message) { super(message); }
    }

    private static void nullArguments() throws Exception {
        for (int operation = 0; operation < 3; operation++) {
            PriorityQueue<Integer> queue = new PriorityQueue<>(List.of(2, 1, 3));
            Iterator<Integer> iterator = queue.iterator();
            try {
                if (operation == 0) queue.removeIf(null);
                else if (operation == 1) queue.removeAll(null);
                else queue.retainAll(null);
                throw new AssertionError("missing NullPointerException");
            } catch (NullPointerException expected) {
                checks++;
            }
            check(iterator.next() == 1 && queue.size() == 3, "null input changed queue");
            record("null:" + operation, Arrays.toString(queue.toArray()));
        }
    }

    private static void selfMembership() throws Exception {
        PriorityQueue<Integer> retained = new PriorityQueue<>(List.of(9, 4, 7, 1));
        check(!retained.retainAll(retained), "retainAll self");
        check(retained.size() == 4, "retainAll self size");
        check(retained.removeAll(retained) && retained.isEmpty(), "removeAll self");
        record("self-membership", "retained-all then removed-all");
    }

    private static byte[] serialize(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        return bytes.toByteArray();
    }

    private static void record(String name, String observations) throws Exception {
        String entry = name + " " + hash(observations) + "\n";
        roots.append(entry);
        cases++;
        if (Boolean.getBoolean("pq.trace")) System.out.print(entry);
    }

    private static String hash(String value) throws Exception {
        return hash(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String hash(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
