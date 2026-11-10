/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Immutable schema-ordinal reference table with JDK-style tiny specializations.
 *
 * <p>Zero, one and two mappings avoid Object[] allocation. Three or more mappings
 * use one Object[] indexed by shared schema ordinal plus one presence word.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextSlotTable.java}
 * blob {@code 0f40bba74db7a85615bfeedc7d02beb07469eaef} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract sealed class SlotTable<V>
        permits SlotTable.Empty, SlotTable.One,
        SlotTable.Two, SlotTable.Dense {

    @FunctionalInterface
    public interface SlotConsumer<V> {
        void accept(int slot, V value);
    }

    final SlotSchema schema;
    final long present;

    SlotTable(SlotSchema schema, long present) {
        this.schema = Objects.requireNonNull(schema, "schema");
        schema.requireCompatible(present);
        this.present = present;
    }

    public static <V> SlotTable<V> empty(SlotSchema schema) {
        return new Empty<>(schema);
    }

    public final SlotSchema schema() {
        return schema;
    }

    public final long presentMask() {
        return present;
    }

    public final int size() {
        return Long.bitCount(present);
    }

    public final boolean isEmpty() {
        return present == 0L;
    }

    public final boolean containsSlot(int slot) {
        Objects.checkIndex(slot, schema.size());
        return (present & (1L << slot)) != 0L;
    }

    public final Optional<V> get(String exactName) {
        V value = getOrNull(exactName);
        return value == null ? Optional.empty() : Optional.of(value);
    }

    public final V getOrNull(String exactName) {
        int slot = schema.slot(exactName);
        return slot < 0 ? null : getOrNull(slot);
    }

    public final Optional<V> get(int slot) {
        V value = getOrNull(slot);
        return value == null ? Optional.empty() : Optional.of(value);
    }

    public abstract V getOrNull(int slot);

    public abstract SlotTable<V> with(int slot, V value);

    public final SlotTable<V> with(
            String exactName,
            V value) {
        int slot = schema.slot(exactName);
        if (slot < 0) {
            throw new IllegalArgumentException(
                    "Unknown exact context slot: " + exactName);
        }
        return with(slot, value);
    }

    public abstract SlotTable<V> without(int slot);

    public abstract void forEachSlot(SlotConsumer<? super V> consumer);

    /** Compatibility boundary; hot loops should use forEachSlot to avoid Integer boxing. */
    public final void forEach(
            BiConsumer<Integer, ? super V> consumer) {
        BiConsumer<Integer, ? super V> checked =
                Objects.requireNonNull(consumer, "consumer");
        forEachSlot(checked::accept);
    }

    static final class Empty<V> extends SlotTable<V> {
        Empty(SlotSchema schema) {
            super(schema, 0L);
        }

        @Override
        public V getOrNull(int slot) {
            Objects.checkIndex(slot, schema.size());
            return null;
        }

        @Override
        public SlotTable<V> with(int slot, V value) {
            Objects.checkIndex(slot, schema.size());
            return new One<>(
                    schema,
                    slot,
                    Objects.requireNonNull(value, "value"));
        }

        @Override
        public SlotTable<V> without(int slot) {
            Objects.checkIndex(slot, schema.size());
            return this;
        }

        @Override
        public void forEachSlot(
                SlotConsumer<? super V> consumer) {
            Objects.requireNonNull(consumer, "consumer");
        }
    }

    static final class One<V> extends SlotTable<V> {
        private final int slot;
        private final V value;

        One(SlotSchema schema, int slot, V value) {
            super(schema, 1L << slot);
            this.slot = slot;
            this.value = value;
        }

        @Override
        public V getOrNull(int requested) {
            Objects.checkIndex(requested, schema.size());
            return requested == slot ? value : null;
        }

        @Override
        public SlotTable<V> with(
                int requested,
                V replacement) {
            Objects.checkIndex(requested, schema.size());
            V checked = Objects.requireNonNull(
                    replacement, "value");
            if (requested == slot) {
                return Objects.equals(value, checked)
                        ? this
                        : new One<>(schema, slot, checked);
            }
            return new Two<>(
                    schema, slot, value, requested, checked);
        }

        @Override
        public SlotTable<V> without(int requested) {
            Objects.checkIndex(requested, schema.size());
            return requested == slot
                    ? new Empty<>(schema)
                    : this;
        }

        @Override
        public void forEachSlot(
                SlotConsumer<? super V> consumer) {
            Objects.requireNonNull(
                    consumer, "consumer").accept(slot, value);
        }
    }

    static final class Two<V> extends SlotTable<V> {
        private final int slot0;
        private final V value0;
        private final int slot1;
        private final V value1;

        Two(
                SlotSchema schema,
                int a,
                V av,
                int b,
                V bv) {
            super(schema, (1L << a) | (1L << b));
            if (a < b) {
                slot0 = a;
                value0 = av;
                slot1 = b;
                value1 = bv;
            } else {
                slot0 = b;
                value0 = bv;
                slot1 = a;
                value1 = av;
            }
        }

        @Override
        public V getOrNull(int slot) {
            Objects.checkIndex(slot, schema.size());
            if (slot == slot0) return value0;
            if (slot == slot1) return value1;
            return null;
        }

        @Override
        public SlotTable<V> with(
                int slot,
                V value) {
            Objects.checkIndex(slot, schema.size());
            V checked = Objects.requireNonNull(value, "value");
            if (slot == slot0) {
                return Objects.equals(value0, checked)
                        ? this
                        : new Two<>(
                                schema,
                                slot,
                                checked,
                                slot1,
                                value1);
            }
            if (slot == slot1) {
                return Objects.equals(value1, checked)
                        ? this
                        : new Two<>(
                                schema,
                                slot0,
                                value0,
                                slot,
                                checked);
            }
            Object[] values = new Object[schema.size()];
            values[slot0] = value0;
            values[slot1] = value1;
            values[slot] = checked;
            return new Dense<>(
                    schema,
                    present | (1L << slot),
                    values);
        }

        @Override
        public SlotTable<V> without(int slot) {
            Objects.checkIndex(slot, schema.size());
            if (slot == slot0) {
                return new One<>(schema, slot1, value1);
            }
            if (slot == slot1) {
                return new One<>(schema, slot0, value0);
            }
            return this;
        }

        @Override
        public void forEachSlot(
                SlotConsumer<? super V> consumer) {
            SlotConsumer<? super V> checked =
                    Objects.requireNonNull(consumer, "consumer");
            checked.accept(slot0, value0);
            checked.accept(slot1, value1);
        }
    }

    static final class Dense<V>
            extends SlotTable<V> {
        private final Object[] values;

        Dense(
                SlotSchema schema,
                long present,
                Object[] values) {
            super(schema, present);
            this.values = values;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V getOrNull(int slot) {
            Objects.checkIndex(slot, schema.size());
            return containsSlot(slot) ? (V) values[slot] : null;
        }

        @Override
        public SlotTable<V> with(
                int slot,
                V value) {
            Objects.checkIndex(slot, schema.size());
            V checked = Objects.requireNonNull(value, "value");
            if (containsSlot(slot)
                    && Objects.equals(values[slot], checked)) {
                return this;
            }
            Object[] copy = values.clone();
            copy[slot] = checked;
            return new Dense<>(
                    schema,
                    present | (1L << slot),
                    copy);
        }

        @SuppressWarnings("unchecked")
        @Override
        public SlotTable<V> without(int slot) {
            Objects.checkIndex(slot, schema.size());
            if (!containsSlot(slot)) {
                return this;
            }
            long nextPresent = present & ~(1L << slot);
            int nextSize = Long.bitCount(nextPresent);
            if (nextSize == 0) {
                return new Empty<>(schema);
            }
            if (nextSize <= 2) {
                SlotTable<V> result =
                        new Empty<>(schema);
                long remaining = nextPresent;
                while (remaining != 0L) {
                    int at = Long.numberOfTrailingZeros(
                            remaining);
                    result = result.with(
                            at,
                            (V) values[at]);
                    remaining &= remaining - 1L;
                }
                return result;
            }
            Object[] copy = values.clone();
            copy[slot] = null;
            return new Dense<>(
                    schema, nextPresent, copy);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void forEachSlot(
                SlotConsumer<? super V> consumer) {
            SlotConsumer<? super V> checked =
                    Objects.requireNonNull(consumer, "consumer");
            long remaining = present;
            while (remaining != 0L) {
                int slot = Long.numberOfTrailingZeros(
                        remaining);
                checked.accept(
                        slot,
                        (V) values[slot]);
                remaining &= remaining - 1L;
            }
        }
    }
}
