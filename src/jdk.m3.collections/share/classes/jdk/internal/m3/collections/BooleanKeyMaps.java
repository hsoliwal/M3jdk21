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

import java.util.Objects;

/**
 * Dense boolean-key maps.
 *
 * <p>A boolean key has a closed cardinality of two. A hash table is therefore
 * unnecessary overhead: every map is exactly two value slots plus two
 * presence bits. No key objects, wrapper values, entry nodes or hash buckets
 * are retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/BooleanKeyMaps.java}
 * blob {@code 9fd6351cb0ae4cde5df42c8041ad01bc1caf2658} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class BooleanKeyMaps {
    private BooleanKeyMaps() { }

    @FunctionalInterface public interface BooleanObjectConsumer<V> { void accept(boolean key, V value); }
    @FunctionalInterface public interface BooleanBooleanConsumer { void accept(boolean key, boolean value); }
    @FunctionalInterface public interface BooleanByteConsumer { void accept(boolean key, byte value); }
    @FunctionalInterface public interface BooleanCharConsumer { void accept(boolean key, char value); }
    @FunctionalInterface public interface BooleanShortConsumer { void accept(boolean key, short value); }
    @FunctionalInterface public interface BooleanIntConsumer { void accept(boolean key, int value); }
    @FunctionalInterface public interface BooleanLongConsumer { void accept(boolean key, long value); }
    @FunctionalInterface public interface BooleanFloatConsumer { void accept(boolean key, float value); }
    @FunctionalInterface public interface BooleanDoubleConsumer { void accept(boolean key, double value); }

    public abstract static class Presence {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public Presence() {
        }

        private byte present;

        public final boolean containsKey(boolean key) {
            return (present & mask(key)) != 0;
        }

        public final int size() {
            return Integer.bitCount(present & 0x3);
        }

        public final boolean isEmpty() {
            return (present & 0x3) == 0;
        }

        public void clear() {
            present = 0;
        }

        protected final void mark(boolean key) {
            present = (byte) (present | mask(key));
        }

        protected final void unmark(boolean key) {
            present = (byte) (present & ~mask(key));
        }

        protected static int mask(boolean key) {
            return key ? 0x2 : 0x1;
        }
    }

    /** Boolean key to object value. Null values are rejected. */
    public static final class ObjectMap<V> extends Presence {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public ObjectMap() {
        }

        private V falseValue;
        private V trueValue;

        public V get(boolean key) {
            return containsKey(key) ? value(key) : null;
        }

        public V getIfAbsent(boolean key, V fallback) {
            return containsKey(key) ? value(key) : fallback;
        }

        public void put(boolean key, V value) {
            Objects.requireNonNull(value, "value");
            if (key) trueValue = value;
            else falseValue = value;
            mark(key);
        }

        public V remove(boolean key) {
            if (!containsKey(key)) return null;
            V previous = value(key);
            if (key) trueValue = null;
            else falseValue = null;
            unmark(key);
            return previous;
        }

        public void forEachKeyValue(BooleanObjectConsumer<? super V> consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (containsKey(false)) consumer.accept(false, falseValue);
            if (containsKey(true)) consumer.accept(true, trueValue);
        }

        @Override public void clear() {
            falseValue = null;
            trueValue = null;
            super.clear();
        }

        private V value(boolean key) {
            return key ? trueValue : falseValue;
        }
    }

    /** Boolean key to boolean value, packed into two presence bits + two value bits. */
    public static final class BooleanMap extends Presence {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public BooleanMap() {
        }

        private byte values;

        public boolean get(boolean key) {
            return getIfAbsent(key, false);
        }

        public boolean getIfAbsent(boolean key, boolean fallback) {
            return containsKey(key) ? (values & valueMask(key)) != 0 : fallback;
        }

        public void put(boolean key, boolean value) {
            int mask = valueMask(key);
            values = value ? (byte) (values | mask) : (byte) (values & ~mask);
            mark(key);
        }

        public void remove(boolean key) {
            values = (byte) (values & ~valueMask(key));
            unmark(key);
        }

        public void forEachKeyValue(BooleanBooleanConsumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (containsKey(false)) consumer.accept(false, get(false));
            if (containsKey(true)) consumer.accept(true, get(true));
        }

        @Override public void clear() {
            values = 0;
            super.clear();
        }

        private static int valueMask(boolean key) {
            return key ? 0x2 : 0x1;
        }
    }

    public static final class ByteMap extends Presence {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public ByteMap() {
        }

        private byte falseValue;
        private byte trueValue;
        public byte get(boolean key) { return getIfAbsent(key, (byte) 0); }
        public byte getIfAbsent(boolean key, byte fallback) {
            return containsKey(key) ? (key ? trueValue : falseValue) : fallback;
        }
        public void put(boolean key, byte value) {
            if (key) trueValue = value; else falseValue = value; mark(key);
        }
        public void remove(boolean key) {
            if (key) trueValue = 0; else falseValue = 0; unmark(key);
        }
        public void forEachKeyValue(BooleanByteConsumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (containsKey(false)) consumer.accept(false, falseValue);
            if (containsKey(true)) consumer.accept(true, trueValue);
        }
        @Override public void clear() { falseValue = 0; trueValue = 0; super.clear(); }
    }

    public static final class CharMap extends Presence {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public CharMap() {
        }

        private char falseValue;
        private char trueValue;
        public char get(boolean key) { return getIfAbsent(key, '\0'); }
        public char getIfAbsent(boolean key, char fallback) {
            return containsKey(key) ? (key ? trueValue : falseValue) : fallback;
        }
        public void put(boolean key, char value) {
            if (key) trueValue = value; else falseValue = value; mark(key);
        }
        public void remove(boolean key) {
            if (key) trueValue = 0; else falseValue = 0; unmark(key);
        }
        public void forEachKeyValue(BooleanCharConsumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (containsKey(false)) consumer.accept(false, falseValue);
            if (containsKey(true)) consumer.accept(true, trueValue);
        }
        @Override public void clear() { falseValue = 0; trueValue = 0; super.clear(); }
    }

    public static final class ShortMap extends Presence {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public ShortMap() {
        }

        private short falseValue;
        private short trueValue;
        public short get(boolean key) { return getIfAbsent(key, (short) 0); }
        public short getIfAbsent(boolean key, short fallback) {
            return containsKey(key) ? (key ? trueValue : falseValue) : fallback;
        }
        public void put(boolean key, short value) {
            if (key) trueValue = value; else falseValue = value; mark(key);
        }
        public void remove(boolean key) {
            if (key) trueValue = 0; else falseValue = 0; unmark(key);
        }
        public void forEachKeyValue(BooleanShortConsumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (containsKey(false)) consumer.accept(false, falseValue);
            if (containsKey(true)) consumer.accept(true, trueValue);
        }
        @Override public void clear() { falseValue = 0; trueValue = 0; super.clear(); }
    }

    public static final class IntMap extends Presence {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public IntMap() {
        }

        private int falseValue;
        private int trueValue;
        public int get(boolean key) { return getIfAbsent(key, 0); }
        public int getIfAbsent(boolean key, int fallback) {
            return containsKey(key) ? (key ? trueValue : falseValue) : fallback;
        }
        public void put(boolean key, int value) {
            if (key) trueValue = value; else falseValue = value; mark(key);
        }
        public void remove(boolean key) {
            if (key) trueValue = 0; else falseValue = 0; unmark(key);
        }
        public void forEachKeyValue(BooleanIntConsumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (containsKey(false)) consumer.accept(false, falseValue);
            if (containsKey(true)) consumer.accept(true, trueValue);
        }
        @Override public void clear() { falseValue = 0; trueValue = 0; super.clear(); }
    }

    public static final class LongMap extends Presence {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public LongMap() {
        }

        private long falseValue;
        private long trueValue;
        public long get(boolean key) { return getIfAbsent(key, 0L); }
        public long getIfAbsent(boolean key, long fallback) {
            return containsKey(key) ? (key ? trueValue : falseValue) : fallback;
        }
        public void put(boolean key, long value) {
            if (key) trueValue = value; else falseValue = value; mark(key);
        }
        public void remove(boolean key) {
            if (key) trueValue = 0L; else falseValue = 0L; unmark(key);
        }
        public void forEachKeyValue(BooleanLongConsumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (containsKey(false)) consumer.accept(false, falseValue);
            if (containsKey(true)) consumer.accept(true, trueValue);
        }
        @Override public void clear() { falseValue = 0L; trueValue = 0L; super.clear(); }
    }

    public static final class FloatMap extends Presence {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public FloatMap() {
        }

        private float falseValue;
        private float trueValue;
        public float get(boolean key) { return getIfAbsent(key, 0.0f); }
        public float getIfAbsent(boolean key, float fallback) {
            return containsKey(key) ? (key ? trueValue : falseValue) : fallback;
        }
        public void put(boolean key, float value) {
            if (key) trueValue = value; else falseValue = value; mark(key);
        }
        public void remove(boolean key) {
            if (key) trueValue = 0.0f; else falseValue = 0.0f; unmark(key);
        }
        public void forEachKeyValue(BooleanFloatConsumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (containsKey(false)) consumer.accept(false, falseValue);
            if (containsKey(true)) consumer.accept(true, trueValue);
        }
        @Override public void clear() { falseValue = 0.0f; trueValue = 0.0f; super.clear(); }
    }

    public static final class DoubleMap extends Presence {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public DoubleMap() {
        }

        private double falseValue;
        private double trueValue;
        public double get(boolean key) { return getIfAbsent(key, 0.0d); }
        public double getIfAbsent(boolean key, double fallback) {
            return containsKey(key) ? (key ? trueValue : falseValue) : fallback;
        }
        public void put(boolean key, double value) {
            if (key) trueValue = value; else falseValue = value; mark(key);
        }
        public void remove(boolean key) {
            if (key) trueValue = 0.0d; else falseValue = 0.0d; unmark(key);
        }
        public void forEachKeyValue(BooleanDoubleConsumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (containsKey(false)) consumer.accept(false, falseValue);
            if (containsKey(true)) consumer.accept(true, trueValue);
        }
        @Override public void clear() { falseValue = 0.0d; trueValue = 0.0d; super.clear(); }
    }
}
