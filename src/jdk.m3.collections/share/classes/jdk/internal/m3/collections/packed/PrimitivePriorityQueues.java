/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import java.util.NoSuchElementException;

/**
 * Primitive priority queues backed by primitive binary min-heaps.
 *
 * <p>No wrapper object and no heap node is retained per element.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PrimitivePriorityQueues.java}
 * blob {@code c7c5ce8c42e77aeb2ba5fdc94e3c7f2437c06ddc} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PrimitivePriorityQueues {
    private static final int DEFAULT_CAPACITY = 8;

    private PrimitivePriorityQueues() { }

    private abstract static class Heap {
        int size;
        public final int size() { return size; }
        public final boolean isEmpty() { return size == 0; }
        public void clear() { size = 0; }
        final void requireElement() {
            if (size == 0) throw new NoSuchElementException();
        }
        static int grow(int current) {
            return PackedFlatArrays.growPowerOfTwo(current);
        }
        static int capacity(int expectedSize) {
            return PackedFlatArrays.powerOfTwoCapacity(expectedSize, DEFAULT_CAPACITY);
        }
    }

    /** False sorts before true; two counters are denser than a heap. */
    public static final class BooleanMinHeap extends Heap {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public BooleanMinHeap() {
        }

        private int falseCount;
        private int trueCount;

        public void add(boolean value) {
            if (size == Integer.MAX_VALUE) throw new IllegalStateException("boolean heap size exceeded");
            if (value) trueCount++;
            else falseCount++;
            size++;
        }

        public boolean minOr(boolean fallback) {
            if (size == 0) return fallback;
            return falseCount == 0;
        }

        public boolean removeMin() {
            requireElement();
            size--;
            if (falseCount != 0) {
                falseCount--;
                return false;
            }
            trueCount--;
            return true;
        }

        @Override public void clear() {
            falseCount = 0;
            trueCount = 0;
            super.clear();
        }

        public boolean[] toSortedArray() {
            boolean[] result = new boolean[size];
            for (int i = falseCount; i < result.length; i++) result[i] = true;
            return result;
        }
    }

    public static final class ByteMinHeap extends Heap {
        private byte[] values;

        public ByteMinHeap() { this(DEFAULT_CAPACITY); }

        public ByteMinHeap(int expectedSize) {
            values = new byte[capacity(expectedSize)];
        }

        public void add(byte value) {
            ensureCapacity();
            int index = size++;
            values[index] = value;
            while (index > 0) {
                int parent = PackedHeapAtoms.parent(index);
                if (!less(values[index], values[parent])) break;
                byte swap = values[parent];
                values[parent] = values[index];
                values[index] = swap;
                index = parent;
            }
        }

        public byte minOr(byte fallback) {
            return size == 0 ? fallback : values[0];
        }

        public byte removeMin() {
            requireElement();
            byte result = values[0];
            byte tail = values[--size];
            if (size != 0) {
                values[0] = tail;
                int index = 0;
                while (true) {
                    int left = PackedHeapAtoms.left(index);
                    if (left >= size) break;
                    int right = PackedHeapAtoms.right(index);
                    int child = right < size && less(values[right], values[left]) ? right : left;
                    if (!less(values[child], values[index])) break;
                    byte swap = values[index];
                    values[index] = values[child];
                    values[child] = swap;
                    index = child;
                }
            }
            return result;
        }

        public byte[] toSortedArray() {
            ByteMinHeap copy = new ByteMinHeap(size);
            copy.values = java.util.Arrays.copyOf(values, Math.max(DEFAULT_CAPACITY, values.length));
            copy.size = size;
            byte[] result = new byte[size];
            for (int i = 0; i < result.length; i++) result[i] = copy.removeMin();
            return result;
        }

        private static boolean less(byte left, byte right) {
            return left < right;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            values = java.util.Arrays.copyOf(values, grow(values.length));
        }
    }


    public static final class CharMinHeap extends Heap {
        private char[] values;

        public CharMinHeap() { this(DEFAULT_CAPACITY); }

        public CharMinHeap(int expectedSize) {
            values = new char[capacity(expectedSize)];
        }

        public void add(char value) {
            ensureCapacity();
            int index = size++;
            values[index] = value;
            while (index > 0) {
                int parent = PackedHeapAtoms.parent(index);
                if (!less(values[index], values[parent])) break;
                char swap = values[parent];
                values[parent] = values[index];
                values[index] = swap;
                index = parent;
            }
        }

        public char minOr(char fallback) {
            return size == 0 ? fallback : values[0];
        }

        public char removeMin() {
            requireElement();
            char result = values[0];
            char tail = values[--size];
            if (size != 0) {
                values[0] = tail;
                int index = 0;
                while (true) {
                    int left = PackedHeapAtoms.left(index);
                    if (left >= size) break;
                    int right = PackedHeapAtoms.right(index);
                    int child = right < size && less(values[right], values[left]) ? right : left;
                    if (!less(values[child], values[index])) break;
                    char swap = values[index];
                    values[index] = values[child];
                    values[child] = swap;
                    index = child;
                }
            }
            return result;
        }

        public char[] toSortedArray() {
            CharMinHeap copy = new CharMinHeap(size);
            copy.values = java.util.Arrays.copyOf(values, Math.max(DEFAULT_CAPACITY, values.length));
            copy.size = size;
            char[] result = new char[size];
            for (int i = 0; i < result.length; i++) result[i] = copy.removeMin();
            return result;
        }

        private static boolean less(char left, char right) {
            return left < right;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            values = java.util.Arrays.copyOf(values, grow(values.length));
        }
    }


    public static final class ShortMinHeap extends Heap {
        private short[] values;

        public ShortMinHeap() { this(DEFAULT_CAPACITY); }

        public ShortMinHeap(int expectedSize) {
            values = new short[capacity(expectedSize)];
        }

        public void add(short value) {
            ensureCapacity();
            int index = size++;
            values[index] = value;
            while (index > 0) {
                int parent = PackedHeapAtoms.parent(index);
                if (!less(values[index], values[parent])) break;
                short swap = values[parent];
                values[parent] = values[index];
                values[index] = swap;
                index = parent;
            }
        }

        public short minOr(short fallback) {
            return size == 0 ? fallback : values[0];
        }

        public short removeMin() {
            requireElement();
            short result = values[0];
            short tail = values[--size];
            if (size != 0) {
                values[0] = tail;
                int index = 0;
                while (true) {
                    int left = PackedHeapAtoms.left(index);
                    if (left >= size) break;
                    int right = PackedHeapAtoms.right(index);
                    int child = right < size && less(values[right], values[left]) ? right : left;
                    if (!less(values[child], values[index])) break;
                    short swap = values[index];
                    values[index] = values[child];
                    values[child] = swap;
                    index = child;
                }
            }
            return result;
        }

        public short[] toSortedArray() {
            ShortMinHeap copy = new ShortMinHeap(size);
            copy.values = java.util.Arrays.copyOf(values, Math.max(DEFAULT_CAPACITY, values.length));
            copy.size = size;
            short[] result = new short[size];
            for (int i = 0; i < result.length; i++) result[i] = copy.removeMin();
            return result;
        }

        private static boolean less(short left, short right) {
            return left < right;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            values = java.util.Arrays.copyOf(values, grow(values.length));
        }
    }


    public static final class IntMinHeap extends Heap {
        private int[] values;

        public IntMinHeap() { this(DEFAULT_CAPACITY); }

        public IntMinHeap(int expectedSize) {
            values = new int[capacity(expectedSize)];
        }

        public void add(int value) {
            ensureCapacity();
            int index = size++;
            values[index] = value;
            while (index > 0) {
                int parent = PackedHeapAtoms.parent(index);
                if (!less(values[index], values[parent])) break;
                int swap = values[parent];
                values[parent] = values[index];
                values[index] = swap;
                index = parent;
            }
        }

        public int minOr(int fallback) {
            return size == 0 ? fallback : values[0];
        }

        public int removeMin() {
            requireElement();
            int result = values[0];
            int tail = values[--size];
            if (size != 0) {
                values[0] = tail;
                int index = 0;
                while (true) {
                    int left = PackedHeapAtoms.left(index);
                    if (left >= size) break;
                    int right = PackedHeapAtoms.right(index);
                    int child = right < size && less(values[right], values[left]) ? right : left;
                    if (!less(values[child], values[index])) break;
                    int swap = values[index];
                    values[index] = values[child];
                    values[child] = swap;
                    index = child;
                }
            }
            return result;
        }

        public int[] toSortedArray() {
            IntMinHeap copy = new IntMinHeap(size);
            copy.values = java.util.Arrays.copyOf(values, Math.max(DEFAULT_CAPACITY, values.length));
            copy.size = size;
            int[] result = new int[size];
            for (int i = 0; i < result.length; i++) result[i] = copy.removeMin();
            return result;
        }

        private static boolean less(int left, int right) {
            return left < right;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            values = java.util.Arrays.copyOf(values, grow(values.length));
        }
    }


    public static final class LongMinHeap extends Heap {
        private long[] values;

        public LongMinHeap() { this(DEFAULT_CAPACITY); }

        public LongMinHeap(int expectedSize) {
            values = new long[capacity(expectedSize)];
        }

        public void add(long value) {
            ensureCapacity();
            int index = size++;
            values[index] = value;
            while (index > 0) {
                int parent = PackedHeapAtoms.parent(index);
                if (!less(values[index], values[parent])) break;
                long swap = values[parent];
                values[parent] = values[index];
                values[index] = swap;
                index = parent;
            }
        }

        public long minOr(long fallback) {
            return size == 0 ? fallback : values[0];
        }

        public long removeMin() {
            requireElement();
            long result = values[0];
            long tail = values[--size];
            if (size != 0) {
                values[0] = tail;
                int index = 0;
                while (true) {
                    int left = PackedHeapAtoms.left(index);
                    if (left >= size) break;
                    int right = PackedHeapAtoms.right(index);
                    int child = right < size && less(values[right], values[left]) ? right : left;
                    if (!less(values[child], values[index])) break;
                    long swap = values[index];
                    values[index] = values[child];
                    values[child] = swap;
                    index = child;
                }
            }
            return result;
        }

        public long[] toSortedArray() {
            LongMinHeap copy = new LongMinHeap(size);
            copy.values = java.util.Arrays.copyOf(values, Math.max(DEFAULT_CAPACITY, values.length));
            copy.size = size;
            long[] result = new long[size];
            for (int i = 0; i < result.length; i++) result[i] = copy.removeMin();
            return result;
        }

        private static boolean less(long left, long right) {
            return left < right;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            values = java.util.Arrays.copyOf(values, grow(values.length));
        }
    }


    public static final class FloatMinHeap extends Heap {
        private float[] values;

        public FloatMinHeap() { this(DEFAULT_CAPACITY); }

        public FloatMinHeap(int expectedSize) {
            values = new float[capacity(expectedSize)];
        }

        public void add(float value) {
            ensureCapacity();
            int index = size++;
            values[index] = value;
            while (index > 0) {
                int parent = PackedHeapAtoms.parent(index);
                if (!less(values[index], values[parent])) break;
                float swap = values[parent];
                values[parent] = values[index];
                values[index] = swap;
                index = parent;
            }
        }

        public float minOr(float fallback) {
            return size == 0 ? fallback : values[0];
        }

        public float removeMin() {
            requireElement();
            float result = values[0];
            float tail = values[--size];
            if (size != 0) {
                values[0] = tail;
                int index = 0;
                while (true) {
                    int left = PackedHeapAtoms.left(index);
                    if (left >= size) break;
                    int right = PackedHeapAtoms.right(index);
                    int child = right < size && less(values[right], values[left]) ? right : left;
                    if (!less(values[child], values[index])) break;
                    float swap = values[index];
                    values[index] = values[child];
                    values[child] = swap;
                    index = child;
                }
            }
            return result;
        }

        public float[] toSortedArray() {
            FloatMinHeap copy = new FloatMinHeap(size);
            copy.values = java.util.Arrays.copyOf(values, Math.max(DEFAULT_CAPACITY, values.length));
            copy.size = size;
            float[] result = new float[size];
            for (int i = 0; i < result.length; i++) result[i] = copy.removeMin();
            return result;
        }

        private static boolean less(float left, float right) {
            return Float.compare(left, right) < 0;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            values = java.util.Arrays.copyOf(values, grow(values.length));
        }
    }


    public static final class DoubleMinHeap extends Heap {
        private double[] values;

        public DoubleMinHeap() { this(DEFAULT_CAPACITY); }

        public DoubleMinHeap(int expectedSize) {
            values = new double[capacity(expectedSize)];
        }

        public void add(double value) {
            ensureCapacity();
            int index = size++;
            values[index] = value;
            while (index > 0) {
                int parent = PackedHeapAtoms.parent(index);
                if (!less(values[index], values[parent])) break;
                double swap = values[parent];
                values[parent] = values[index];
                values[index] = swap;
                index = parent;
            }
        }

        public double minOr(double fallback) {
            return size == 0 ? fallback : values[0];
        }

        public double removeMin() {
            requireElement();
            double result = values[0];
            double tail = values[--size];
            if (size != 0) {
                values[0] = tail;
                int index = 0;
                while (true) {
                    int left = PackedHeapAtoms.left(index);
                    if (left >= size) break;
                    int right = PackedHeapAtoms.right(index);
                    int child = right < size && less(values[right], values[left]) ? right : left;
                    if (!less(values[child], values[index])) break;
                    double swap = values[index];
                    values[index] = values[child];
                    values[child] = swap;
                    index = child;
                }
            }
            return result;
        }

        public double[] toSortedArray() {
            DoubleMinHeap copy = new DoubleMinHeap(size);
            copy.values = java.util.Arrays.copyOf(values, Math.max(DEFAULT_CAPACITY, values.length));
            copy.size = size;
            double[] result = new double[size];
            for (int i = 0; i < result.length; i++) result[i] = copy.removeMin();
            return result;
        }

        private static boolean less(double left, double right) {
            return Double.compare(left, right) < 0;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            values = java.util.Arrays.copyOf(values, grow(values.length));
        }
    }

}
