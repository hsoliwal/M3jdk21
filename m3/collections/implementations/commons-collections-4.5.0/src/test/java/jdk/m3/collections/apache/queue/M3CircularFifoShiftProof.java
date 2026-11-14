// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections.apache.queue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

/** Independent ordered-model checks, run unchanged against baseline and delivery. */
//FLLDCIM: RELOCATE - donor blob c78d0a13148c at ca1aafc4d94f; recipe m3/collections/qualification/census-g5-20261106/relocate.py v4
public final class M3CircularFifoShiftProof {
    private static long checks;
    private static final Field ELEMENTS;
    private static final Field START;
    private static final Field END;

    static {
        try {
            ELEMENTS = CircularFifoQueue.class.getDeclaredField("elements");
            START = CircularFifoQueue.class.getDeclaredField("start");
            END = CircularFifoQueue.class.getDeclaredField("end");
            ELEMENTS.setAccessible(true);
            START.setAccessible(true);
            END.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private M3CircularFifoShiftProof() { }

    private static void same(final Object expected, final Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void raises(final Class<? extends Throwable> type, final Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (failure.getClass() == type) {
                return;
            }
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static CircularFifoQueue<Integer> state(final int capacity, final int head,
            final int size) {
        final CircularFifoQueue<Integer> queue = new CircularFifoQueue<>(capacity);
        for (int i = 0; i < head; i++) {
            queue.add(-1);
            queue.remove();
        }
        for (int i = 0; i < size; i++) {
            queue.add(i);
        }
        return queue;
    }

    private static void verify(final CircularFifoQueue<Integer> queue,
            final List<Integer> model) throws Exception {
        same(model.size(), queue.size());
        same(model.isEmpty(), queue.isEmpty());
        same(model.size() == queue.maxSize(), queue.isAtFullCapacity());
        same(false, queue.isFull());
        same(model.isEmpty() ? null : model.get(0), queue.peek());
        same(model, Arrays.asList(queue.toArray()));
        for (int i = 0; i < model.size(); i++) {
            same(model.get(i), queue.get(i));
        }
        final Object[] cells = (Object[]) ELEMENTS.get(queue);
        final int head = START.getInt(queue);
        same((head + model.size()) % cells.length, END.getInt(queue));
        for (int i = 0; i < cells.length; i++) {
            final int logical = (i - head + cells.length) % cells.length;
            same(logical < model.size() ? model.get(logical) : null, cells[i]);
        }
    }

    private static void exhaustive() throws Exception {
        for (int capacity = 1; capacity <= 32; capacity++) {
            for (int head = 0; head < capacity; head++) {
                for (int size = 1; size <= capacity; size++) {
                    for (int ordinal = 0; ordinal < size; ordinal++) {
                        final CircularFifoQueue<Integer> queue = state(capacity, head, size);
                        final List<Integer> model = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            model.add(i);
                        }
                        final Iterator<Integer> iterator = queue.iterator();
                        raises(IllegalStateException.class, iterator::remove);
                        for (int i = 0; i <= ordinal; i++) {
                            same(i, iterator.next());
                        }
                        iterator.remove();
                        model.remove(ordinal);
                        raises(IllegalStateException.class, iterator::remove);
                        verify(queue, model);
                        for (int i = ordinal; i < model.size(); i++) {
                            same(true, iterator.hasNext());
                            same(model.get(i), iterator.next());
                        }
                        same(false, iterator.hasNext());
                        raises(NoSuchElementException.class, iterator::next);
                        queue.add(99);
                        model.add(99);
                        verify(queue, model);
                    }
                }
            }
        }
    }

    private static void repeatedAndRandom() throws Exception {
        final Random random = new Random(0x4d335348494654L);
        for (final int capacity : new int[] {1, 2, 3, 7, 16, 31, 32, 63, 64, 65, 127}) {
            final CircularFifoQueue<Integer> queue = new CircularFifoQueue<>(capacity);
            final List<Integer> model = new ArrayList<>();
            for (int step = 0; step < 4000; step++) {
                final int operation = random.nextInt(5);
                if (operation <= 1) {
                    final int value = random.nextInt(17);
                    if (model.size() == capacity) {
                        model.remove(0);
                    }
                    model.add(value);
                    same(true, queue.add(value));
                } else if (operation == 2) {
                    same(model.isEmpty() ? null : model.remove(0), queue.poll());
                } else if (operation == 3) {
                    final Iterator<Integer> iterator = queue.iterator();
                    int position = 0;
                    while (iterator.hasNext()) {
                        same(model.get(position), iterator.next());
                        if (random.nextBoolean()) {
                            iterator.remove();
                            model.remove(position);
                        } else {
                            position++;
                        }
                    }
                } else if (random.nextInt(9) == 0) {
                    queue.clear();
                    model.clear();
                }
                verify(queue, model);
            }
        }
    }

    private static final class CountingQueue extends CircularFifoQueue<Integer> {
        private static final long serialVersionUID = 1L;
        private int calls;
        private CountingQueue() {
            super(4);
        }
        @Override public Integer remove() {
            calls++;
            return super.remove();
        }
    }

    private static void boundaries() throws Exception {
        raises(IllegalArgumentException.class, () -> new CircularFifoQueue<>(0));
        final CountingQueue queue = new CountingQueue();
        raises(NullPointerException.class, () -> queue.add(null));
        raises(NoSuchElementException.class, queue::remove);
        queue.calls = 0;
        queue.add(1);
        queue.add(2);
        queue.add(3);
        final Iterator<Integer> iterator = queue.iterator();
        same(1, iterator.next());
        iterator.remove();
        same(1, queue.calls);
        same(2, iterator.next());
        same(3, iterator.next());
        iterator.remove();
        same(1, queue.calls);
        verify(queue, Arrays.asList(2));
        raises(NoSuchElementException.class, () -> queue.get(-1));
        raises(NoSuchElementException.class, () -> queue.get(1));
        for (int head = 0; head < 7; head++) {
            for (int ordinal = 0; ordinal < 7; ordinal++) {
                final CircularFifoQueue<Integer> original = state(7, head, 7);
                final Iterator<Integer> cursor = original.iterator();
                for (int i = 0; i <= ordinal; i++) {
                    cursor.next();
                }
                cursor.remove();
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                    output.writeObject(original);
                }
                try (ObjectInputStream input = new ObjectInputStream(
                        new ByteArrayInputStream(bytes.toByteArray()))) {
                    final Object copy = input.readObject();
                    same(original.toString(), copy.toString());
                    same(original.maxSize(), ((CircularFifoQueue<?>) copy).maxSize());
                }
            }
        }
    }

    public static void main(final String[] arguments) throws Exception {
        if (arguments.length != 0) {
            throw new IllegalArgumentException("no arguments");
        }
        exhaustive();
        repeatedAndRandom();
        boundaries();
        System.out.println("PASS M3_CIRCULAR_SHIFT checks=" + checks);
    }
}
