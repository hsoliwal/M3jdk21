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
package jdk.internal.m3.collections;

import java.util.Arrays;

/**
 * Sparse int-indexed double vector using sorted int[] indices + double[] values.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntDoubleSparseVector.java}
 * blob {@code 53040c4a819fa26e6c65453ce54af879a9dc793b} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class IntDoubleSparseVector {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(int index, double value);
    }

    private int[] indices;
    private double[] values;
    private int size;

    public IntDoubleSparseVector() { this(8); }

    public IntDoubleSparseVector(int initialNonZero) {
        if (initialNonZero < 0) throw new IllegalArgumentException("initialNonZero < 0");
        int capacity = Math.max(1, initialNonZero);
        indices = new int[capacity];
        values = new double[capacity];
    }

    public int nonZeroCount() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() {
        return (long) indices.length * Integer.BYTES
                + (long) values.length * Double.BYTES;
    }

    public double get(int index) {
        if (index < 0) throw new IndexOutOfBoundsException(index);
        int position = Arrays.binarySearch(indices, 0, size, index);
        return position < 0 ? 0.0d : values[position];
    }

    public double set(int index, double value) {
        if (index < 0) throw new IndexOutOfBoundsException(index);
        int position = Arrays.binarySearch(indices, 0, size, index);
        if (position >= 0) {
            double old = values[position];
            if (value == 0.0d) removeAt(position);
            else values[position] = value;
            return old;
        }
        if (value == 0.0d) return 0.0d;
        int insertion = -position - 1;
        ensure(size + 1);
        System.arraycopy(indices, insertion, indices, insertion + 1, size - insertion);
        System.arraycopy(values, insertion, values, insertion + 1, size - insertion);
        indices[insertion] = index;
        values[insertion] = value;
        size++;
        return 0.0d;
    }

    public double addTo(int index, double delta) {
        double next = get(index) + delta;
        set(index, next);
        return next;
    }

    public double dot(double[] dense) {
        double result = 0.0d;
        for (int i = 0; i < size; i++) {
            int index = indices[i];
            if (index >= dense.length) {
                throw new IllegalArgumentException("dense vector too short for index " + index);
            }
            result += values[i] * dense[index];
        }
        return result;
    }

    public double squaredNorm() {
        double result = 0.0d;
        for (int i = 0; i < size; i++) result += values[i] * values[i];
        return result;
    }

    public void scale(double factor) {
        if (factor == 0.0d) {
            clear();
            return;
        }
        for (int i = 0; i < size; i++) values[i] *= factor;
    }

    public void forEach(EntryConsumer consumer) {
        for (int i = 0; i < size; i++) consumer.accept(indices[i], values[i]);
    }

    public void clear() {
        Arrays.fill(indices, 0, size, 0);
        Arrays.fill(values, 0, size, 0.0d);
        size = 0;
    }

    private void removeAt(int position) {
        int moved = size - position - 1;
        if (moved > 0) {
            System.arraycopy(indices, position + 1, indices, position, moved);
            System.arraycopy(values, position + 1, values, position, moved);
        }
        size--;
        indices[size] = 0;
        values[size] = 0.0d;
    }

    private void ensure(int required) {
        if (required <= indices.length) return;
        int capacity = indices.length + (indices.length >>> 1) + 1;
        if (capacity < required) capacity = required;
        indices = Arrays.copyOf(indices, capacity);
        values = Arrays.copyOf(values, capacity);
    }
}
