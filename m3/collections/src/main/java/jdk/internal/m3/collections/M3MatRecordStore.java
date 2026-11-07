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
package jdk.internal.m3.collections;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable columnar store for one Java record type.
 * Frozen state retains one compact column per component and no record object per row.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatRecordStore.java}
 * blob {@code 4e7df5eaf02f4b419f9ce682d5160f40ea4549d0} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatRecordStore<R extends Record> {
    private interface Column {
        Object value(int row);
        M3MatBits equalTo(Object target);
        long payloadBytes();
    }

    private final Class<R> type;
    private final RecordComponent[] components;
    private final Constructor<R> constructor;
    private final Column[] columns;
    private final int rows;
    private final M3MatLimits limits;

    private M3MatRecordStore(
            Class<R> type,
            RecordComponent[] components,
            Constructor<R> constructor,
            Column[] columns,
            int rows,
            M3MatLimits limits) {
        this.type = type;
        this.components = components;
        this.constructor = constructor;
        this.columns = columns;
        this.rows = rows;
        this.limits = limits;
    }

    /** Compatibility path retaining the historical effectively-unbounded contract. */
    public static <R extends Record> M3MatRecordStore<R> of(Class<R> type, List<R> records) {
        return of(type, records, M3MatLimits.UNBOUNDED);
    }

    /** Preferred production path with an explicit row/pool budget. */
    public static <R extends Record> M3MatRecordStore<R> of(
            Class<R> type, List<R> records, M3MatLimits limits) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(limits, "limits");
        if (!type.isRecord()) throw new IllegalArgumentException("not a record: " + type.getName());
        limits.rows(records.size());
        RecordComponent[] components = type.getRecordComponents();
        Constructor<R> ctor = constructor(type, components);
        Object[][] transientValues = new Object[components.length][records.size()];
        for (int row = 0; row < records.size(); row++) {
            R record = Objects.requireNonNull(records.get(row), "record");
            if (record.getClass() != type) throw new IllegalArgumentException("record subtype mismatch");
            for (int component = 0; component < components.length; component++) {
                try {
                    transientValues[component][row] =
                            components[component].getAccessor().invoke(record);
                } catch (ReflectiveOperationException failure) {
                    throw new IllegalArgumentException(
                            "record accessor failed: " + components[component].getName(), failure);
                }
            }
        }
        Column[] columns = new Column[components.length];
        for (int component = 0; component < components.length; component++) {
            columns[component] =
                    column(components[component].getType(), transientValues[component], limits);
        }
        return new M3MatRecordStore<>(
                type, components, ctor, columns, records.size(), limits);
    }

    public Class<R> type() { return type; }
    public int rows() { return rows; }
    public int components() { return components.length; }
    public M3MatLimits limits() { return limits; }

    public String componentName(int index) {
        return components[Objects.checkIndex(index, components.length)].getName();
    }

    public R get(int row) {
        Objects.checkIndex(row, rows);
        Object[] arguments = new Object[columns.length];
        for (int component = 0; component < columns.length; component++) {
            arguments[component] = columns[component].value(row);
        }
        try {
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("record reconstruction failed", failure);
        }
    }

    public M3MatBits equal(String component, Object target) {
        return columns[index(component)].equalTo(target);
    }

    public long payloadBytes() {
        long total = 0;
        for (Column column : columns) total = Math.addExact(total, column.payloadBytes());
        return total;
    }

    private int index(String name) {
        Objects.requireNonNull(name, "name");
        for (int index = 0; index < components.length; index++) {
            if (components[index].getName().equals(name)) return index;
        }
        throw new IllegalArgumentException("unknown component: " + name);
    }

    private static <R extends Record> Constructor<R> constructor(
            Class<R> type, RecordComponent[] components) {
        Class<?>[] types = new Class<?>[components.length];
        for (int index = 0; index < types.length; index++) {
            types[index] = components[index].getType();
        }
        try {
            Constructor<R> constructor = type.getDeclaredConstructor(types);
            if (!constructor.canAccess(null)) constructor.setAccessible(true);
            return constructor;
        } catch (ReflectiveOperationException | SecurityException failure) {
            throw new IllegalArgumentException("record constructor inaccessible", failure);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Column column(Class<?> type, Object[] values, M3MatLimits limits) {
        if (type == boolean.class) {
            M3MatBits.Builder bits = M3MatBits.builder(values.length);
            for (int index = 0; index < values.length; index++) {
                bits.set(index, (Boolean) values[index]);
            }
            M3MatBits frozen = bits.build();
            return new Column() {
                @Override public Object value(int row) { return frozen.get(row); }
                @Override public M3MatBits equalTo(Object target) {
                    if (!(target instanceof Boolean value)) {
                        throw new IllegalArgumentException("boolean target");
                    }
                    return value ? frozen : frozen.not();
                }
                @Override public long payloadBytes() { return frozen.payloadBytes(); }
            };
        }

        if (type == byte.class || type == short.class
                || type == char.class || type == int.class) {
            int[] data = new int[values.length];
            for (int index = 0; index < data.length; index++) {
                Object value = values[index];
                data[index] =
                        type == char.class ? (Character) value : ((Number) value).intValue();
            }
            M3MatIntVector vector = new M3MatIntVector(data);
            return new Column() {
                @Override public Object value(int row) {
                    int value = vector.get(row);
                    if (type == byte.class) return (byte) value;
                    if (type == short.class) return (short) value;
                    if (type == char.class) return (char) value;
                    return value;
                }
                @Override public M3MatBits equalTo(Object target) {
                    int value =
                            type == char.class
                                    ? (Character) target
                                    : ((Number) target).intValue();
                    return vector.equalTo(value);
                }
                @Override public long payloadBytes() { return vector.payloadBytes(); }
            };
        }

        if (type == long.class || type == float.class || type == double.class) {
            long[] data = new long[values.length];
            for (int index = 0; index < data.length; index++) {
                if (type == float.class) {
                    data[index] =
                            Integer.toUnsignedLong(
                                    Float.floatToRawIntBits((Float) values[index]));
                } else if (type == double.class) {
                    data[index] = Double.doubleToRawLongBits((Double) values[index]);
                } else {
                    data[index] = (Long) values[index];
                }
            }
            M3MatLongVector vector = new M3MatLongVector(data);
            return new Column() {
                @Override public Object value(int row) {
                    long value = vector.get(row);
                    if (type == float.class) return Float.intBitsToFloat((int) value);
                    if (type == double.class) return Double.longBitsToDouble(value);
                    return value;
                }
                @Override public M3MatBits equalTo(Object target) {
                    long value =
                            type == float.class
                                    ? Integer.toUnsignedLong(
                                            Float.floatToRawIntBits((Float) target))
                                    : type == double.class
                                            ? Double.doubleToRawLongBits((Double) target)
                                            : (Long) target;
                    return vector.equalTo(value);
                }
                @Override public long payloadBytes() { return vector.payloadBytes(); }
            };
        }

        if (type == UUID.class) {
            UUID[] data = new UUID[values.length];
            for (int index = 0; index < data.length; index++) {
                data[index] = (UUID) values[index];
            }
            M3MatUuidVector vector = new M3MatUuidVector(data);
            return new Column() {
                @Override public Object value(int row) { return vector.get(row); }
                @Override public M3MatBits equalTo(Object target) {
                    if (target != null && !(target instanceof UUID)) {
                        throw new IllegalArgumentException("UUID target");
                    }
                    return vector.equalTo((UUID) target);
                }
                @Override public long payloadBytes() { return vector.payloadBytes(); }
            };
        }

        if (type.isEnum()) {
            Class enumType = type;
            M3MatEnumVector enumVector = new M3MatEnumVector(enumType, values.length);
            for (int index = 0; index < values.length; index++) {
                enumVector.set(index, (Enum) values[index]);
            }
            return new Column() {
                @Override public Object value(int row) { return enumVector.get(row); }
                @Override public M3MatBits equalTo(Object target) {
                    return enumVector.equalTo((Enum) target);
                }
                @Override public long payloadBytes() { return enumVector.payloadBytes(); }
            };
        }

        M3MatCodec<Object> codec = M3MatCodecs.of((Class<Object>) type);
        codec = M3MatCodecs.nullable(codec);
        M3MatPool.Builder<Object> builder = M3MatPool.builder(codec, limits);
        for (Object value : values) builder.add(value);
        M3MatPool<Object> pool = builder.build();
        int[] ids = new int[values.length];
        for (int index = 0; index < ids.length; index++) {
            ids[index] = pool.idOf(values[index]);
        }
        M3MatIdVector<Object> vector = new M3MatIdVector<>(pool, ids);
        return new Column() {
            @Override public Object value(int row) { return vector.get(row).value(); }
            @Override public M3MatBits equalTo(Object target) {
                int id = pool.idOf(target);
                if (id < 0) return new M3MatBits(values.length);
                return vector.equalTo(pool.ref(id));
            }
            @Override public long payloadBytes() {
                return pool.payloadBytes() + vector.payloadBytes();
            }
        };
    }
}
