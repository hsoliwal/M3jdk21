/* *****************************************************************************
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

import jdk.m3.collections.LongDeque;

import java.util.NoSuchElementException;

/**
 * Primitive FIFO/deque family backed by one primitive ring array.
 *
 * <p>No wrapper object or node is retained per element. Capacities are powers
 * of two so head/tail arithmetic is a mask operation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PrimitiveDeques.java}
 * blob {@code ac5e6b4afa372c3a48e034c03e01aeea5c52801d} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections receipt-aware batch (v6, RECEIPT_MAP): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PrimitiveDeques {
    private static final int DEFAULT_CAPACITY = 8;

    private PrimitiveDeques() { }

    private abstract static class Ring {
        int head;
        int size;

        public final int size() { return size; }
        public final boolean isEmpty() { return size == 0; }

        public final void clear() {
            head = 0;
            size = 0;
        }

        final int index(int logical, int capacity) {
            return PackedRingAtoms.powerOfTwoIndex(head, logical, capacity);
        }

        final void requireElement() {
            if (size == 0) throw new NoSuchElementException();
        }

        final void resetHeadIfEmpty() {
            head = PackedRingAtoms.normalizeHeadAfterRemoval(head, size);
        }

        static int previous(int index, int capacity) {
            return PackedRingAtoms.powerOfTwoPrevious(index, capacity);
        }

        static int next(int index, int capacity) {
            return PackedRingAtoms.powerOfTwoNext(index, capacity);
        }

        static int grow(int current) {
            return PackedFlatArrays.growPowerOfTwo(current);
        }

        static int capacity(int expectedSize) {
            return PackedFlatArrays.powerOfTwoCapacity(expectedSize, DEFAULT_CAPACITY);
        }
    }

    public static final class BooleanDeque extends Ring {
        private boolean[] values;

        public BooleanDeque() { this(DEFAULT_CAPACITY); }

        public BooleanDeque(int expectedSize) {
            values = new boolean[capacity(expectedSize)];
        }

        public void addFirst(boolean value) {
            ensureCapacity();
            head = previous(head, values.length);
            values[head] = value;
            size++;
        }

        public void addLast(boolean value) {
            ensureCapacity();
            values[index(size, values.length)] = value;
            size++;
        }

        public boolean removeFirst() {
            requireElement();
            boolean value = values[head];
            head = next(head, values.length);
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public boolean removeLast() {
            requireElement();
            int index = index(size - 1, values.length);
            boolean value = values[index];
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public boolean firstOr(boolean fallback) {
            return size == 0 ? fallback : values[head];
        }

        public boolean lastOr(boolean fallback) {
            return size == 0 ? fallback : values[index(size - 1, values.length)];
        }

        public boolean[] toArray() {
            boolean[] result = new boolean[size];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, result);
            return result;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            boolean[] next = new boolean[grow(values.length)];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, next);
            values = next;
            head = 0;
        }
    }


    public static final class ByteDeque extends Ring {
        private byte[] values;

        public ByteDeque() { this(DEFAULT_CAPACITY); }

        public ByteDeque(int expectedSize) {
            values = new byte[capacity(expectedSize)];
        }

        public void addFirst(byte value) {
            ensureCapacity();
            head = previous(head, values.length);
            values[head] = value;
            size++;
        }

        public void addLast(byte value) {
            ensureCapacity();
            values[index(size, values.length)] = value;
            size++;
        }

        public byte removeFirst() {
            requireElement();
            byte value = values[head];
            head = next(head, values.length);
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public byte removeLast() {
            requireElement();
            int index = index(size - 1, values.length);
            byte value = values[index];
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public byte firstOr(byte fallback) {
            return size == 0 ? fallback : values[head];
        }

        public byte lastOr(byte fallback) {
            return size == 0 ? fallback : values[index(size - 1, values.length)];
        }

        public byte[] toArray() {
            byte[] result = new byte[size];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, result);
            return result;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            byte[] next = new byte[grow(values.length)];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, next);
            values = next;
            head = 0;
        }
    }


    public static final class CharDeque extends Ring {
        private char[] values;

        public CharDeque() { this(DEFAULT_CAPACITY); }

        public CharDeque(int expectedSize) {
            values = new char[capacity(expectedSize)];
        }

        public void addFirst(char value) {
            ensureCapacity();
            head = previous(head, values.length);
            values[head] = value;
            size++;
        }

        public void addLast(char value) {
            ensureCapacity();
            values[index(size, values.length)] = value;
            size++;
        }

        public char removeFirst() {
            requireElement();
            char value = values[head];
            head = next(head, values.length);
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public char removeLast() {
            requireElement();
            int index = index(size - 1, values.length);
            char value = values[index];
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public char firstOr(char fallback) {
            return size == 0 ? fallback : values[head];
        }

        public char lastOr(char fallback) {
            return size == 0 ? fallback : values[index(size - 1, values.length)];
        }

        public char[] toArray() {
            char[] result = new char[size];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, result);
            return result;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            char[] next = new char[grow(values.length)];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, next);
            values = next;
            head = 0;
        }
    }


    public static final class ShortDeque extends Ring {
        private short[] values;

        public ShortDeque() { this(DEFAULT_CAPACITY); }

        public ShortDeque(int expectedSize) {
            values = new short[capacity(expectedSize)];
        }

        public void addFirst(short value) {
            ensureCapacity();
            head = previous(head, values.length);
            values[head] = value;
            size++;
        }

        public void addLast(short value) {
            ensureCapacity();
            values[index(size, values.length)] = value;
            size++;
        }

        public short removeFirst() {
            requireElement();
            short value = values[head];
            head = next(head, values.length);
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public short removeLast() {
            requireElement();
            int index = index(size - 1, values.length);
            short value = values[index];
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public short firstOr(short fallback) {
            return size == 0 ? fallback : values[head];
        }

        public short lastOr(short fallback) {
            return size == 0 ? fallback : values[index(size - 1, values.length)];
        }

        public short[] toArray() {
            short[] result = new short[size];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, result);
            return result;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            short[] next = new short[grow(values.length)];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, next);
            values = next;
            head = 0;
        }
    }


    public static final class IntDeque extends Ring {
        private int[] values;

        public IntDeque() { this(DEFAULT_CAPACITY); }

        public IntDeque(int expectedSize) {
            values = new int[capacity(expectedSize)];
        }

        public void addFirst(int value) {
            ensureCapacity();
            head = previous(head, values.length);
            values[head] = value;
            size++;
        }

        public void addLast(int value) {
            ensureCapacity();
            values[index(size, values.length)] = value;
            size++;
        }

        public int removeFirst() {
            requireElement();
            int value = values[head];
            head = next(head, values.length);
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public int removeLast() {
            requireElement();
            int index = index(size - 1, values.length);
            int value = values[index];
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public int firstOr(int fallback) {
            return size == 0 ? fallback : values[head];
        }

        public int lastOr(int fallback) {
            return size == 0 ? fallback : values[index(size - 1, values.length)];
        }

        public int[] toArray() {
            int[] result = new int[size];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, result);
            return result;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            int[] next = new int[grow(values.length)];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, next);
            values = next;
            head = 0;
        }
    }


    public static final class LongDeque extends Ring {
        private long[] values;

        public LongDeque() { this(DEFAULT_CAPACITY); }

        public LongDeque(int expectedSize) {
            values = new long[capacity(expectedSize)];
        }

        public void addFirst(long value) {
            ensureCapacity();
            head = previous(head, values.length);
            values[head] = value;
            size++;
        }

        public void addLast(long value) {
            ensureCapacity();
            values[index(size, values.length)] = value;
            size++;
        }

        public long removeFirst() {
            requireElement();
            long value = values[head];
            head = next(head, values.length);
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public long removeLast() {
            requireElement();
            int index = index(size - 1, values.length);
            long value = values[index];
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public long firstOr(long fallback) {
            return size == 0 ? fallback : values[head];
        }

        public long lastOr(long fallback) {
            return size == 0 ? fallback : values[index(size - 1, values.length)];
        }

        public long[] toArray() {
            long[] result = new long[size];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, result);
            return result;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            long[] next = new long[grow(values.length)];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, next);
            values = next;
            head = 0;
        }
    }


    public static final class FloatDeque extends Ring {
        private float[] values;

        public FloatDeque() { this(DEFAULT_CAPACITY); }

        public FloatDeque(int expectedSize) {
            values = new float[capacity(expectedSize)];
        }

        public void addFirst(float value) {
            ensureCapacity();
            head = previous(head, values.length);
            values[head] = value;
            size++;
        }

        public void addLast(float value) {
            ensureCapacity();
            values[index(size, values.length)] = value;
            size++;
        }

        public float removeFirst() {
            requireElement();
            float value = values[head];
            head = next(head, values.length);
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public float removeLast() {
            requireElement();
            int index = index(size - 1, values.length);
            float value = values[index];
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public float firstOr(float fallback) {
            return size == 0 ? fallback : values[head];
        }

        public float lastOr(float fallback) {
            return size == 0 ? fallback : values[index(size - 1, values.length)];
        }

        public float[] toArray() {
            float[] result = new float[size];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, result);
            return result;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            float[] next = new float[grow(values.length)];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, next);
            values = next;
            head = 0;
        }
    }


    public static final class DoubleDeque extends Ring {
        private double[] values;

        public DoubleDeque() { this(DEFAULT_CAPACITY); }

        public DoubleDeque(int expectedSize) {
            values = new double[capacity(expectedSize)];
        }

        public void addFirst(double value) {
            ensureCapacity();
            head = previous(head, values.length);
            values[head] = value;
            size++;
        }

        public void addLast(double value) {
            ensureCapacity();
            values[index(size, values.length)] = value;
            size++;
        }

        public double removeFirst() {
            requireElement();
            double value = values[head];
            head = next(head, values.length);
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public double removeLast() {
            requireElement();
            int index = index(size - 1, values.length);
            double value = values[index];
            size--;
            resetHeadIfEmpty();
            return value;
        }

        public double firstOr(double fallback) {
            return size == 0 ? fallback : values[head];
        }

        public double lastOr(double fallback) {
            return size == 0 ? fallback : values[index(size - 1, values.length)];
        }

        public double[] toArray() {
            double[] result = new double[size];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, result);
            return result;
        }

        private void ensureCapacity() {
            if (size < values.length) return;
            double[] next = new double[grow(values.length)];
            PackedFlatArrays.copyLogicalRing(values, head, size, values.length, next);
            values = next;
            head = 0;
        }
    }

}
