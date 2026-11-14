// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections.apache.list;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

/** Independent list model and array/thread invariant oracle for the actual fork. */
public final class TreeListIndexTraversalContract {
    private static long checks;
    private static long digest;
    private static final Object[] VALUES = new Object[65];
    static {
        for (int i = 1; i < VALUES.length; i++) VALUES[i] = Integer.valueOf(i - 33);
    }
    private TreeListIndexTraversalContract() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void mix(long value) {
        digest = (digest ^ value) * 0x100000001b3L;
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static long valueId(Object value) {
        return value == null ? Long.MIN_VALUE : ((Integer) value).longValue();
    }

    private static final class Shape {
        private final Object[] values;
        private final int[] left, right, relative;
        private final byte[] heights, state;
        private final boolean[] seen;
        private final int[] order;
        private final List<Object> model;
        private int count;

        Shape(TreeList<Object> actual, List<Object> model) throws Exception {
            this.model = model;
            Object slots = field(TreeList.class, "slots").get(actual);
            Class<?> type = slots.getClass();
            values = (Object[]) field(type, "values").get(slots);
            left = (int[]) field(type, "left").get(slots);
            right = (int[]) field(type, "right").get(slots);
            relative = (int[]) field(type, "relativePosition").get(slots);
            heights = (byte[]) field(type, "heights").get(slots);
            state = (byte[]) field(type, "state").get(slots);
            int used = field(type, "used").getInt(slots);
            seen = new boolean[used];
            order = new int[model.size()];
            check(left.length == values.length && right.length == values.length
                    && relative.length == values.length && heights.length == values.length
                    && state.length == values.length, "parallel lane capacities");
            int root = field(TreeList.class, "root").getInt(actual);
            check((root == -1) == model.isEmpty(), "root emptiness");
            if (root != -1) walk(root, 0);
            check(count == model.size(), "every logical entry visited once");
            for (int i = 0; i < order.length; i++) {
                int slot = order[i];
                if ((state[slot] & 1) != 0)
                    check(left[slot] == (i == 0 ? -1 : order[i - 1]), "previous thread");
                if ((state[slot] & 2) != 0)
                    check(right[slot] == (i + 1 == order.length ? -1 : order[i + 1]), "next thread");
            }
            int freeCount = 0;
            for (int slot = field(type, "free").getInt(slots); slot != -1; slot = left[slot]) {
                check(slot >= 0 && slot < used && !seen[slot], "free chain disjoint and acyclic");
                seen[slot] = true;
                check(state[slot] == 0 && values[slot] == null, "free slot releases payload");
                freeCount++;
            }
            check(freeCount + count == used, "all used slots are live or free");
            check(field(type, "detached").getInt(slots) == -1, "no detached live update");
            mix(root); mix(used); mix(values.length);
            for (int i = 0; i < values.length; i++) {
                mix(valueId(values[i])); mix(left[i]); mix(right[i]);
                mix(relative[i]); mix(heights[i]); mix(state[i]);
            }
        }

        private int walk(int slot, int parentPosition) {
            check(slot >= 0 && slot < seen.length && !seen[slot], "tree links acyclic");
            seen[slot] = true;
            check((state[slot] & 4) != 0, "reachable slot is live");
            int absolute = parentPosition + relative[slot];
            int lh = (state[slot] & 1) == 0 ? walk(left[slot], absolute) : -1;
            check(absolute == count && absolute < model.size(), "relative rank matches in-order index");
            check(values[slot] == model.get(count), "slot payload identity");
            order[count++] = slot;
            int rh = (state[slot] & 2) == 0 ? walk(right[slot], absolute) : -1;
            check(Math.abs(lh - rh) <= 1 && heights[slot] == Math.max(lh, rh) + 1,
                    "AVL balance and stored height");
            return heights[slot];
        }
    }

    private static void state(TreeList<Object> actual, List<Object> model) throws Exception {
        check(actual.size() == model.size(), "logical size");
        Object[] array = actual.toArray();
        check(array.length == model.size(), "array result length");
        ListIterator<Object> forward = actual.listIterator();
        ListIterator<Object> reverse = actual.listIterator(actual.size());
        for (int i = 0; i < model.size(); i++) {
            Object expected = model.get(i);
            check(actual.get(i) == expected && array[i] == expected, "indexed result identity");
            check(forward.hasNext() && forward.next() == expected, "forward threaded traversal");
            check(reverse.hasPrevious() && reverse.previous() == model.get(model.size() - 1 - i),
                    "reverse threaded traversal");
            mix(valueId(expected));
        }
        check(!forward.hasNext() && !reverse.hasPrevious(), "iterator exhaustion");
        new Shape(actual, model);
    }

    private static void boundaries() throws Exception {
        int[] sizes = {0, 1, 2, 3, 7, 8, 9, 15, 16, 17, 31, 32, 33,
                255, 256, 257, 1023, 1024, 1025, 8191, 8192, 8193};
        for (int n : sizes) {
            ArrayList<Object> model = new ArrayList<>();
            for (int i = 0; i < n; i++) model.add(VALUES[i % VALUES.length]);
            TreeList<Object> actual = new TreeList<>(model);
            state(actual, model);
            for (int bad : new int[]{Integer.MIN_VALUE, -1, n, Integer.MAX_VALUE}) {
                try { actual.get(bad); throw new AssertionError("accepted bad index " + bad); }
                catch (IndexOutOfBoundsException expected) {
                    check(expected.getMessage().equals("Invalid index:" + bad + ", size=" + n),
                            "index failure message");
                }
            }
            if (n > 1) {
                int half = n / 2;
                actual.subList(0, half).clear(); model.subList(0, half).clear();
                ArrayList<Object> refill = new ArrayList<>();
                for (int i = 0; i < half; i++) refill.add(VALUES[(i + 7) % VALUES.length]);
                actual.addAll(refill); model.addAll(refill);
                state(actual, model);
            }
            actual.clear(); model.clear();
            state(actual, model);
        }
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d335f545245454cL);
        TreeList<Object> actual = new TreeList<>();
        ArrayList<Object> model = new ArrayList<>();
        for (int step = 0; step < 40_000; step++) {
            Object value = VALUES[random.nextInt(VALUES.length)];
            int n = model.size();
            int action = random.nextInt(9);
            if (n > 512) action = 1;
            switch (action) {
            case 0:
                int at = random.nextInt(n + 1);
                actual.add(at, value); model.add(at, value);
                break;
            case 1:
                if (n != 0) {
                    int index = random.nextInt(n);
                    check(actual.remove(index) == model.remove(index), "remove result identity");
                }
                break;
            case 2:
                if (n != 0) {
                    int index = random.nextInt(n);
                    check(actual.set(index, value) == model.set(index, value), "set result identity");
                }
                break;
            case 3:
                if (n != 0) {
                    int index = random.nextInt(n);
                    check(actual.get(index) == model.get(index), "random get identity");
                }
                break;
            case 4:
                int index = random.nextInt(n + 1);
                ListIterator<Object> ai = actual.listIterator(index), mi = model.listIterator(index);
                if (index != 0 && random.nextBoolean()) {
                    check(ai.previous() == mi.previous(), "mutation iterator previous");
                    if (random.nextBoolean()) { ai.remove(); mi.remove(); }
                    else { ai.set(value); mi.set(value); }
                } else if (index != n) {
                    check(ai.next() == mi.next(), "mutation iterator next");
                    if (random.nextBoolean()) { ai.remove(); mi.remove(); }
                    else { ai.set(value); mi.set(value); }
                }
                ai.add(value); mi.add(value);
                check(ai.nextIndex() == mi.nextIndex(), "iterator position after add");
                break;
            case 5:
                List<Object> additions = Arrays.asList(value, null, VALUES[random.nextInt(VALUES.length)]);
                check(actual.addAll(additions) == model.addAll(additions), "bulk append result");
                break;
            case 6:
                if (n != 0) {
                    int start = random.nextInt(n), end = start + random.nextInt(n - start + 1);
                    actual.subList(start, end).clear(); model.subList(start, end).clear();
                }
                break;
            case 7:
                Iterator<Object> stale = actual.iterator();
                actual.add(value); model.add(value);
                try { stale.next(); throw new AssertionError("stale iterator accepted"); }
                catch (ConcurrentModificationException expected) { checks++; }
                break;
            default:
                if (step % 257 == 0) { actual.clear(); model.clear(); }
                else check(actual.contains(value) == model.contains(value), "membership parity");
            }
            if (step % 64 == 0) state(actual, model);
        }
        state(actual, model);
    }

    private static final class ProbeList extends TreeList<Object> {
        private int calls;
        @Override public Object get(int index) { calls++; return super.get(index); }
    }

    private static final class SerializableChild extends TreeList<Object> implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private final String marker = "retained";
    }

    private static void subclassAndSerialization() throws Exception {
        ProbeList probe = new ProbeList();
        probe.add(VALUES[1]); probe.add(VALUES[2]);
        check(probe.get(0) == VALUES[1] && probe.calls == 1, "subclass get dispatch");
        check(probe.listIterator().next() == VALUES[1] && probe.calls == 1,
                "iterator retains direct owner traversal");
        check(probe.remove(0) == VALUES[1] && probe.calls == 2, "remove retains virtual get call");
        for (TreeList<Object> value : List.of(new TreeList<>(), new TreeList<>(List.of(VALUES[1])))) {
            try (ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream())) {
                try { out.writeObject(value); throw new AssertionError("invented serialization support"); }
                catch (NotSerializableException expected) {
                    check(expected.getMessage().equals(TreeList.class.getName()), "serialization rejection identity");
                }
            }
        }
        SerializableChild child = new SerializableChild();
        child.add(new Object());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(child); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            SerializableChild copy = (SerializableChild) in.readObject();
            check(copy.isEmpty() && copy.marker.equals("retained"), "nonserial parent construction contract");
        }
    }

    public static void main(String[] args) throws Exception {
        checks = 0; digest = 0xcbf29ce484222325L;
        boundaries(); randomized(); subclassAndSerialization();
        System.out.println("TREE_LIST_INDEX_TRAVERSAL_PASS checks=" + checks
                + " digest=" + Long.toUnsignedString(digest, 16));
    }
}
