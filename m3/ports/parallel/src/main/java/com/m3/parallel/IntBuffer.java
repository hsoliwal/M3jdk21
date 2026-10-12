/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Fixed-length off-heap {@code int} buffer in native byte order.
 *
 * <p>Element access is absolute and never moves a cursor, so distinct indices may be written concurrently by distinct
 * kernel work-items. Equality is identity; use {@link #contentEquals(IntBuffer)} to compare contents. Storage is a
 * direct {@link ByteBuffer} released by the garbage collector.</p>
 */
public final class IntBuffer {
    private final java.nio.IntBuffer view;
    private final int length;

    private IntBuffer(int length) {
        if (length < 0 || length > Integer.MAX_VALUE / Integer.BYTES) {
            throw new IllegalArgumentException("length out of range: " + length);
        }
        this.length = length;
        this.view = ByteBuffer.allocateDirect(length * Integer.BYTES).order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    /**
     * Allocates a zero-filled buffer.
     *
     * @param length number of {@code int} elements, {@code >= 0}
     * @return a new buffer
     */
    public static IntBuffer allocate(int length) {
        return new IntBuffer(length);
    }

    /**
     * Allocates a buffer holding a copy of {@code values}.
     *
     * @param values source elements; copied, not retained
     * @return a new buffer
     */
    public static IntBuffer of(int... values) {
        Objects.requireNonNull(values, "values");
        IntBuffer buffer = new IntBuffer(values.length);
        buffer.copyFrom(values);
        return buffer;
    }

    /**
     * Returns the number of elements.
     *
     * @return the fixed length
     */
    public int length() {
        return length;
    }

    /**
     * Reads one element.
     *
     * @param index element index
     * @return the element
     * @throws IndexOutOfBoundsException if {@code index} is outside {@code [0, length)}
     */
    public int get(int index) {
        return view.get(index);
    }

    /**
     * Writes one element.
     *
     * @param index element index
     * @param value new value
     * @throws IndexOutOfBoundsException if {@code index} is outside {@code [0, length)}
     */
    public void set(int index, int value) {
        view.put(index, value);
    }

    /**
     * Copies every element into {@code target}.
     *
     * @param target destination; its length must equal {@link #length()}
     */
    public void copyTo(int[] target) {
        requireLength(Objects.requireNonNull(target, "target").length);
        view.get(0, target);
    }

    /**
     * Copies every element from {@code source}.
     *
     * @param source source elements; its length must equal {@link #length()}
     */
    public void copyFrom(int[] source) {
        requireLength(Objects.requireNonNull(source, "source").length);
        view.put(0, source);
    }

    /**
     * Copies every element from another buffer of the same length.
     *
     * @param source source buffer
     */
    public void copyFrom(IntBuffer source) {
        requireLength(Objects.requireNonNull(source, "source").length);
        view.put(0, source.view, 0, length);
    }

    /**
     * Returns a heap copy of the contents.
     *
     * @return a new array of length {@link #length()}
     */
    public int[] toArray() {
        int[] copy = new int[length];
        view.get(0, copy);
        return copy;
    }

    /**
     * Returns an independent off-heap copy.
     *
     * @return a new buffer with equal contents
     */
    public IntBuffer copy() {
        IntBuffer copy = new IntBuffer(length);
        copy.copyFrom(this);
        return copy;
    }

    /**
     * Compares lengths and every element.
     *
     * @param other buffer to compare with
     * @return {@code true} when both buffers hold the same elements
     */
    public boolean contentEquals(IntBuffer other) {
        return other != null && other.length == length && view.equals(other.view);
    }

    /**
     * Returns the first index whose elements differ, or {@code -1} when the contents are equal.
     *
     * @param other buffer of the same length
     * @return first differing index or {@code -1}
     */
    public int mismatch(IntBuffer other) {
        requireLength(Objects.requireNonNull(other, "other").length);
        return view.mismatch(other.view);
    }

    @Override
    public String toString() {
        return "IntBuffer[length=" + length + "]";
    }

    private void requireLength(int otherLength) {
        if (otherLength != length) {
            throw new IllegalArgumentException("length mismatch: expected " + length + " but was " + otherLength);
        }
    }
}
