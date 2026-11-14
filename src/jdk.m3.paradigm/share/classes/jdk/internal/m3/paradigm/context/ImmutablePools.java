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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-global registry of bounded immutable pools.
 *
 * <p>The registry is deliberately fail-closed. Known JDK immutable value
 * classes, enums and deeply immutable records are pooled automatically.
 * Arbitrary classes are never guessed immutable merely because they are final
 * or expose a stable-looking {@code toString()}. Such classes must register an
 * explicit {@link ImmutableCodec}; implementing {@link Immutable}
 * documents the semantic promise but does not weaken the codec proof
 * obligation.
 *
 * <p>Pool hits turn equality of admitted canonical values into identity
 * equality for callers that keep the interned reference. The registry does not
 * claim O(1) construction of canonical keys: canonicalization cost depends on
 * the value schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextImmutablePools.java}
 * blob {@code a79a5a01320ecbd362e74ef588f18224d384c41a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ImmutablePools {

    public static final int DEFAULT_MAX_ENTRIES = 65_536;

    private static final ConcurrentHashMap<Class<?>, ContextImmutablePool<?>> POOLS =
            new ConcurrentHashMap<>();

    static {
        registerText(String.class, "string/utf8");
        registerFixed(
                Integer.class,
                "integer/i32be",
                value -> ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
        registerFixed(
                Long.class,
                "long/i64be",
                value -> ByteBuffer.allocate(Long.BYTES).putLong(value).array());
        registerFixed(
                Short.class,
                "short/i16be",
                value -> ByteBuffer.allocate(Short.BYTES).putShort(value).array());
        registerFixed(Byte.class, "byte/i8", value -> new byte[] {value});
        registerFixed(
                Boolean.class,
                "boolean/i8",
                value -> new byte[] {(byte) (value ? 1 : 0)});
        registerFixed(
                Character.class,
                "char/u16be",
                value -> ByteBuffer.allocate(Character.BYTES).putChar(value).array());
        registerFixed(
                Double.class,
                "double/ieee754-i64be",
                value -> ByteBuffer.allocate(Long.BYTES)
                        .putLong(Double.doubleToLongBits(value))
                        .array());
        registerFixed(
                Float.class,
                "float/ieee754-i32be",
                value -> ByteBuffer.allocate(Integer.BYTES)
                        .putInt(Float.floatToIntBits(value))
                        .array());
        registerFixed(
                BigInteger.class,
                "big-integer/twos-complement",
                BigInteger::toByteArray);
        registerText(BigDecimal.class, "big-decimal/to-string");
        registerFixed(
                UUID.class,
                "uuid/u128be",
                value -> ByteBuffer.allocate(16)
                        .putLong(value.getMostSignificantBits())
                        .putLong(value.getLeastSignificantBits())
                        .array());
        registerText(URI.class, "uri/string");
        registerText(Instant.class, "instant/iso8601");
        registerText(Duration.class, "duration/iso8601");
        registerText(LocalDate.class, "local-date/iso8601");
        registerText(LocalTime.class, "local-time/iso8601");
        registerText(LocalDateTime.class, "local-date-time/iso8601");
        registerText(OffsetDateTime.class, "offset-date-time/iso8601");
        registerText(ZonedDateTime.class, "zoned-date-time/iso8601");
    }

    private ImmutablePools() {
    }

    public static <T> ContextImmutablePool<T> register(
            ImmutableCodec<T> codec,
            int maxEntries) {
        Objects.requireNonNull(codec, "codec");
        ContextImmutablePool<T> created =
                new ContextImmutablePool<>(codec, maxEntries);
        ContextImmutablePool<?> prior =
                POOLS.putIfAbsent(codec.type(), created);
        if (prior == null) {
            return created;
        }
        @SuppressWarnings("unchecked")
        ContextImmutablePool<T> typed =
                (ContextImmutablePool<T>) prior;
        if (!typed.codec().schema().equals(codec.schema())) {
            throw new IllegalStateException(
                    "immutable pool already registered for "
                            + codec.type().getName()
                            + " with schema "
                            + typed.codec().schema());
        }
        return typed;
    }

    public static <T> ContextImmutablePool<T> register(
            ImmutableCodec<T> codec) {
        return register(codec, DEFAULT_MAX_ENTRIES);
    }

    /**
     * Returns a registered pool, lazily adding safe enum/record codecs.
     */
    public static <T> Optional<ContextImmutablePool<T>> tryPool(Class<T> type) {
        Objects.requireNonNull(type, "type");
        ContextImmutablePool<?> existing = POOLS.get(type);
        if (existing == null && type.isEnum()) {
            existing = registerEnum(type);
        }
        if (existing == null
                && type.isRecord()
                && isDeeplyImmutableRecord(type, new HashSet<>())) {
            existing = registerRecord(type);
        }
        @SuppressWarnings("unchecked")
        ContextImmutablePool<T> typed =
                (ContextImmutablePool<T>) existing;
        return Optional.ofNullable(typed);
    }

    public static <T> ContextImmutablePool<T> pool(Class<T> type) {
        return tryPool(type)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No immutable codec registered for " + type.getName()));
    }

    /**
     * Opportunistically interns proven immutable values; unknown or unproved
     * types pass through unchanged.
     */
    @SuppressWarnings("unchecked")
    public static <T> T intern(T value) {
        if (value == null) {
            return null;
        }
        Class<T> type = (Class<T>) value.getClass();
        return tryPool(type)
                .map(pool -> pool.intern(value))
                .orElse(value);
    }

    /** Fails closed when the value type has no immutable codec. */
    @SuppressWarnings("unchecked")
    public static <T> T requireIntern(T value) {
        Objects.requireNonNull(value, "value");
        Class<T> type = (Class<T>) value.getClass();
        return pool(type).intern(value);
    }

    /** Whether the type can be pooled without caller-supplied proof. */
    public static boolean isAutomaticallyPoolable(Class<?> type) {
        Objects.requireNonNull(type, "type");
        return POOLS.containsKey(type)
                || type.isEnum()
                || (type.isRecord()
                        && isDeeplyImmutableRecord(type, new HashSet<>()));
    }

    public static Map<Class<?>, ContextImmutablePool<?>> snapshot() {
        return Map.copyOf(POOLS);
    }

    private static <T> void registerText(Class<T> type, String schema) {
        register(
                ImmutableCodec.of(
                        type,
                        schema,
                        value -> value.toString()
                                .getBytes(StandardCharsets.UTF_8)),
                DEFAULT_MAX_ENTRIES);
    }

    private static <T> void registerFixed(
            Class<T> type,
            String schema,
            java.util.function.Function<? super T, byte[]> encoder) {
        register(
                ImmutableCodec.of(type, schema, encoder),
                DEFAULT_MAX_ENTRIES);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ContextImmutablePool<?> registerEnum(Class<T> type) {
        Class<? extends Enum> enumType = type.asSubclass(Enum.class);
        return register(
                ImmutableCodec.of(
                        enumType,
                        "enum/name-v1",
                        value -> value.name()
                                .getBytes(StandardCharsets.UTF_8)),
                DEFAULT_MAX_ENTRIES);
    }

    private static <T> ContextImmutablePool<?> registerRecord(Class<T> type) {
        return register(
                ImmutableCodec.of(
                        type,
                        "record/fields-v1",
                        ImmutablePools::recordBytes),
                DEFAULT_MAX_ENTRIES);
    }

    private static byte[] recordBytes(Object record) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                writeRecord(output, record);
            }
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException("in-memory record encoding failed", impossible);
        }
    }

    private static void writeRecord(DataOutputStream output, Object record)
            throws IOException {
        Class<?> type = record.getClass();
        output.writeUTF(type.getName());
        RecordComponent[] components = type.getRecordComponents();
        output.writeInt(components.length);
        for (RecordComponent component : components) {
            output.writeUTF(component.getName());
            Object value;
            try {
                Field field = type.getDeclaredField(component.getName());
                if (!field.canAccess(record) && !field.trySetAccessible()) {
                    throw new IllegalAccessException("record field is not accessible");
                }
                value = field.get(record);
            } catch (IllegalAccessException | NoSuchFieldException failure) {
                throw new IllegalStateException(
                        "Cannot read immutable record component "
                                + type.getName()
                                + "#"
                                + component.getName(),
                        failure);
            }
            writeValue(output, component.getType(), value);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void writeValue(
            DataOutputStream output,
            Class<?> declaredType,
            Object value) throws IOException {
        if (value == null) {
            output.writeByte(0);
            return;
        }
        output.writeByte(1);
        Class<?> runtimeType = value.getClass();
        output.writeUTF(runtimeType.getName());

        if (runtimeType.isRecord()) {
            writeRecord(output, value);
            return;
        }
        if (runtimeType.isEnum()) {
            output.writeUTF(((Enum) value).name());
            return;
        }

        ContextImmutablePool pool = POOLS.get(runtimeType);
        if (pool == null) {
            throw new IllegalArgumentException(
                    "Record component "
                            + declaredType.getTypeName()
                            + " is not backed by an immutable codec");
        }
        byte[] encoded = pool.codec().canonicalBytes(value);
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private static boolean isDeeplyImmutableRecord(
            Class<?> type,
            Set<Class<?>> visiting) {
        if (!type.isRecord()) {
            return false;
        }
        if (!visiting.add(type)) {
            return false;
        }
        try {
            for (RecordComponent component : type.getRecordComponents()) {
                Class<?> componentType = boxed(component.getType());
                if (POOLS.containsKey(componentType) || componentType.isEnum()) {
                    continue;
                }
                if (componentType.isRecord()
                        && isDeeplyImmutableRecord(componentType, visiting)) {
                    continue;
                }
                return false;
            }
            return true;
        } finally {
            visiting.remove(type);
        }
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == boolean.class) return Boolean.class;
        if (type == char.class) return Character.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        return type;
    }
}
