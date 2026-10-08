/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Enum set backed only by presence words: one bit per constant, no per-element object.
 *
 * <p>This is an M3 proving-ground backend for the {@link java.util.EnumSet} shape, not a
 * replacement for it. Membership is one bit per ordinal in a {@code long[]} sized to the enum's
 * universe, so the set retains no element references; iteration is ordinal order as in the JDK.
 * Key handling mirrors {@link java.util.EnumSet}: {@code add(null)} throws
 * {@link NullPointerException}, a constant of another enum type throws {@link ClassCastException}
 * on {@code add} and is simply absent for {@code contains}/{@code remove}. Adapted from the
 * first-party Synexia donor {@code com.synexia.primitives.LongBitSet} (word operations) and the
 * ordinal universe handling of {@link M3EnumLongMap}.</p>
 */
public final class M3EnumSet<E extends Enum<E>> {
    private final Class<E> elementType;
    private final E[] universe;
    private final long[] words;
    private int size;

    private M3EnumSet(Class<E> elementType) {
        this.elementType = Objects.requireNonNull(elementType, "elementType");
        E[] constants = elementType.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException("elementType is not an enum: " + elementType.getName());
        }
        universe = constants;
        words = new long[Math.max(1, (constants.length + 63) >>> 6)];
    }

    /** Empty set over the whole universe of {@code elementType}. */
    public static <E extends Enum<E>> M3EnumSet<E> noneOf(Class<E> elementType) {
        return new M3EnumSet<>(elementType);
    }

    /** Set containing every constant of {@code elementType}. */
    public static <E extends Enum<E>> M3EnumSet<E> allOf(Class<E> elementType) {
        M3EnumSet<E> set = new M3EnumSet<>(elementType);
        set.fillAll();
        return set;
    }

    /** Set containing the given constants. */
    @SafeVarargs
    public static <E extends Enum<E>> M3EnumSet<E> of(E first, E... rest) {
        Objects.requireNonNull(first, "first");
        M3EnumSet<E> set = new M3EnumSet<>(first.getDeclaringClass());
        set.add(first);
        for (E element : rest) {
            set.add(element);
        }
        return set;
    }

    /** Copy with the same universe and members. */
    public static <E extends Enum<E>> M3EnumSet<E> copyOf(M3EnumSet<E> source) {
        M3EnumSet<E> set = new M3EnumSet<>(source.elementType);
        System.arraycopy(source.words, 0, set.words, 0, source.words.length);
        set.size = source.size;
        return set;
    }

    /** Set containing every constant that is absent from {@code source}. */
    public static <E extends Enum<E>> M3EnumSet<E> complementOf(M3EnumSet<E> source) {
        M3EnumSet<E> set = copyOf(source);
        set.complement();
        return set;
    }

    /** Set containing the constants from {@code from} to {@code to}, inclusive, by ordinal. */
    public static <E extends Enum<E>> M3EnumSet<E> range(E from, E to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.getDeclaringClass() != to.getDeclaringClass() || from.compareTo(to) > 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        M3EnumSet<E> set = new M3EnumSet<>(from.getDeclaringClass());
        for (int ordinal = from.ordinal(); ordinal <= to.ordinal(); ordinal++) {
            set.words[ordinal >>> 6] |= 1L << (ordinal & 63);
        }
        set.size = to.ordinal() - from.ordinal() + 1;
        return set;
    }

    public Class<E> elementType() {
        return elementType;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Retained primitive payload: the presence words. */
    public long payloadBytes() {
        return (long) words.length * Long.BYTES;
    }

    public boolean contains(Object element) {
        if (!(element instanceof Enum<?> constant) || constant.getDeclaringClass() != elementType) {
            return false;
        }
        int ordinal = constant.ordinal();
        return (words[ordinal >>> 6] & (1L << (ordinal & 63))) != 0L;
    }

    public boolean add(E element) {
        int ordinal = ordinal(element);
        long bit = 1L << (ordinal & 63);
        int word = ordinal >>> 6;
        if ((words[word] & bit) != 0L) {
            return false;
        }
        words[word] |= bit;
        size++;
        return true;
    }

    public boolean remove(Object element) {
        if (!(element instanceof Enum<?> constant) || constant.getDeclaringClass() != elementType) {
            return false;
        }
        int ordinal = constant.ordinal();
        long bit = 1L << (ordinal & 63);
        int word = ordinal >>> 6;
        if ((words[word] & bit) == 0L) {
            return false;
        }
        words[word] &= ~bit;
        size--;
        return true;
    }

    public boolean containsAll(M3EnumSet<E> other) {
        checkUniverse(other);
        for (int i = 0; i < words.length; i++) {
            if ((other.words[i] & ~words[i]) != 0L) {
                return false;
            }
        }
        return true;
    }

    public boolean addAll(M3EnumSet<E> other) {
        checkUniverse(other);
        return combine(other, 0);
    }

    public boolean removeAll(M3EnumSet<E> other) {
        checkUniverse(other);
        return combine(other, 1);
    }

    public boolean retainAll(M3EnumSet<E> other) {
        checkUniverse(other);
        return combine(other, 2);
    }

    public void clear() {
        Arrays.fill(words, 0L);
        size = 0;
    }

    /** Flips membership of every constant in the universe. */
    public void complement() {
        for (int i = 0; i < words.length; i++) {
            words[i] = ~words[i];
        }
        maskTail();
        size = universe.length - size;
    }

    /** Visits members in ordinal order; keys come from the shared enum universe. */
    public void forEach(Consumer<? super E> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {
            long word = words[wordIndex];
            while (word != 0L) {
                int ordinal = (wordIndex << 6) + Long.numberOfTrailingZeros(word);
                consumer.accept(universe[ordinal]);
                word &= word - 1L;
            }
        }
    }

    /** Members in ordinal order. */
    @SuppressWarnings("unchecked")
    public E[] toArray() {
        E[] result = (E[]) java.lang.reflect.Array.newInstance(elementType, size);
        int[] cursor = {0};
        forEach(element -> result[cursor[0]++] = element);
        return result;
    }

    /** Snapshot of the presence words. */
    public long[] toLongArray() {
        return words.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof M3EnumSet<?> set
                && set.elementType == elementType
                && Arrays.equals(set.words, words);
    }

    @Override
    public int hashCode() {
        return 31 * elementType.hashCode() + Arrays.hashCode(words);
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }

    private boolean combine(M3EnumSet<E> other, int operation) {
        boolean changed = false;
        for (int i = 0; i < words.length; i++) {
            long before = words[i];
            long after = switch (operation) {
                case 0 -> before | other.words[i];
                case 1 -> before & ~other.words[i];
                default -> before & other.words[i];
            };
            if (after != before) {
                words[i] = after;
                changed = true;
            }
        }
        if (changed) {
            recount();
        }
        return changed;
    }

    private void fillAll() {
        Arrays.fill(words, -1L);
        maskTail();
        size = universe.length;
    }

    private void maskTail() {
        int used = universe.length & 63;
        if (used != 0) {
            words[words.length - 1] &= (1L << used) - 1L;
        } else if (universe.length == 0) {
            words[0] = 0L;
        }
    }

    private void recount() {
        int count = 0;
        for (long word : words) {
            count += Long.bitCount(word);
        }
        size = count;
    }

    private void checkUniverse(M3EnumSet<E> other) {
        Objects.requireNonNull(other, "other");
        if (other.elementType != elementType) {
            throw new ClassCastException(other.elementType.getName() + " != " + elementType.getName());
        }
    }

    private int ordinal(E element) {
        Objects.requireNonNull(element, "element");
        if (element.getDeclaringClass() != elementType) {
            throw new ClassCastException(element.getDeclaringClass().getName() + " != " + elementType.getName());
        }
        return element.ordinal();
    }
}
