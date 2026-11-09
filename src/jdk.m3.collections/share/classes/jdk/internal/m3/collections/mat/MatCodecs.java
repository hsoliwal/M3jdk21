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
package jdk.internal.m3.collections.mat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exact built-in canonical codecs and bounded reflective enum/record discovery.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatCodecs.java}
 * blob {@code 418994fab452e7928a15c81044119322c56b2105} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatCodecs {
    private MatCodecs() { }

    public static final MatCodec<Boolean> BOOLEAN = fixed("synex:boolean/1", Boolean.class,
            (v, s) -> s.u8(v ? 1 : 0), src -> {
                int value = src.u8();
                if (value > 1) throw new IllegalArgumentException("boolean payload");
                return value == 1;
            });
    public static final MatCodec<Byte> BYTE = fixed("synex:int8/1", Byte.class,
            (v, s) -> s.u8(v & 0xff), src -> (byte) src.u8());
    public static final MatCodec<Short> SHORT = fixed("synex:int16/1", Short.class,
            (v, s) -> s.i16(v), MatSource::i16);
    public static final MatCodec<Character> CHAR = fixed("synex:char16/1", Character.class,
            (v, s) -> s.u16(v), MatSource::u16);
    public static final MatCodec<Integer> INT = fixed("synex:int32/1", Integer.class,
            (v, s) -> s.i32(v), MatSource::i32);
    public static final MatCodec<Long> LONG = fixed("synex:int64/1", Long.class,
            (v, s) -> s.i64(v), MatSource::i64);
    public static final MatCodec<Float> FLOAT_RAW = fixed("synex:float32-raw/1", Float.class,
            (v, s) -> s.i32(Float.floatToRawIntBits(v)), src -> Float.intBitsToFloat(src.i32()));
    public static final MatCodec<Double> DOUBLE_RAW = fixed("synex:float64-raw/1", Double.class,
            (v, s) -> s.i64(Double.doubleToRawLongBits(v)), src -> Double.longBitsToDouble(src.i64()));
    public static final MatCodec<String> STRING = fixed("synex:string-utf8/1", String.class,
            (v, s) -> s.utf8(v), MatSource::utf8);
    public static final MatCodec<byte[]> BYTES = new MatCodec<>() {
        @Override public String typeId() { return "synex:bytes/1"; }
        @Override public Class<byte[]> javaType() { return byte[].class; }
        @Override public void encode(byte[] value, MatSink sink) { sink.blob(value); }
        @Override public byte[] decode(MatSource source) { return source.blob(); }
    };
    public static final MatCodec<BigInteger> BIG_INTEGER = fixed("synex:bigint/1", BigInteger.class,
            (v, s) -> s.blob(v.toByteArray()), src -> new BigInteger(src.blob()));
    public static final MatCodec<BigDecimal> BIG_DECIMAL = new MatCodec<>() {
        @Override public String typeId() { return "synex:bigdecimal/1"; }
        @Override public Class<BigDecimal> javaType() { return BigDecimal.class; }
        @Override public void encode(BigDecimal value, MatSink sink) {
            sink.i32(value.scale()).blob(value.unscaledValue().toByteArray());
        }
        @Override public BigDecimal decode(MatSource source) {
            int scale = source.i32();
            return new BigDecimal(new BigInteger(source.blob()), scale);
        }
    };
    public static final MatCodec<UUID> UUID_CODEC = fixed("synex:uuid/1", UUID.class,
            (v, s) -> {
                s.i64(v.getMostSignificantBits());
                s.i64(v.getLeastSignificantBits());
            }, src -> new UUID(src.i64(), src.i64()));
    public static final MatCodec<Instant> INSTANT = fixed("synex:instant/1", Instant.class,
            (v, s) -> {
                s.i64(v.getEpochSecond());
                s.i32(v.getNano());
            }, src -> Instant.ofEpochSecond(src.i64(), src.i32()));

    private static final ConcurrentHashMap<Class<?>, MatCodec<?>> CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<Set<Class<?>>> RESOLVING =
            ThreadLocal.withInitial(HashSet::new);

    static {
        for (MatCodec<?> codec : new MatCodec<?>[] {
                BOOLEAN, BYTE, SHORT, CHAR, INT, LONG, FLOAT_RAW, DOUBLE_RAW,
                STRING, BYTES, BIG_INTEGER, BIG_DECIMAL, UUID_CODEC, INSTANT
        }) {
            CACHE.put(codec.javaType(), codec);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> MatCodec<T> of(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Class<?> boxed = box(type);
        MatCodec<?> existing = CACHE.get(boxed);
        if (existing != null) return (MatCodec<T>) existing;
        if (boxed.isEnum()) {
            MatCodec<?> codec = enumCodecUnchecked(boxed);
            MatCodec<?> race = CACHE.putIfAbsent(boxed, codec);
            return (MatCodec<T>) (race == null ? codec : race);
        }
        if (boxed.isRecord()) {
            MatCodec<?> generated = generatedCodec(boxed);
            if (generated != null) {
                MatCodec<?> race = CACHE.putIfAbsent(boxed, generated);
                return (MatCodec<T>) (race == null ? generated : race);
            }
            Set<Class<?>> resolving = RESOLVING.get();
            if (!resolving.add(boxed)) {
                throw new IllegalArgumentException(
                        "recursive record codec graph is not canonicalizable: " + boxed.getName());
            }
            try {
                MatCodec<?> codec = MatRecordCodec.create((Class<? extends Record>) boxed);
                MatCodec<?> race = CACHE.putIfAbsent(boxed, codec);
                return (MatCodec<T>) (race == null ? codec : race);
            } finally {
                resolving.remove(boxed);
                if (resolving.isEmpty()) RESOLVING.remove();
            }
        }
        throw new IllegalArgumentException("no canonical codec for " + type.getName());
    }

    public static <T> MatCodec<T> nullable(MatCodec<T> delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return new MatCodec<>() {
            @Override public String typeId() { return "synex:nullable/1(" + delegate.typeId() + ")"; }
            @Override public Class<T> javaType() { return delegate.javaType(); }
            @Override public void encode(T value, MatSink sink) {
                if (value == null) {
                    sink.u8(0);
                } else {
                    sink.u8(1);
                    delegate.encode(value, sink);
                }
            }
            @Override public T decode(MatSource source) {
                int state = source.u8();
                if (state == 0) return null;
                if (state != 1) throw new IllegalArgumentException("nullable state");
                return delegate.decode(source);
            }
            @Override public byte[] encode(T value) {
                MatSink sink = new MatSink();
                encode(value, sink);
                return sink.toByteArray();
            }
        };
    }

    private static MatCodec<?> generatedCodec(Class<?> type) {
        String generatedName = generatedCodecClassName(type);
        try {
            Class<?> generatedType = Class.forName(generatedName, true, type.getClassLoader());
            Object candidate = generatedType.getField("INSTANCE").get(null);
            if (!(candidate instanceof MatCodec<?> codec)) {
                throw new IllegalStateException(
                        "generated MAT codec INSTANCE is not M3MatCodec: " + generatedName);
            }
            if (codec.javaType() != type) {
                throw new IllegalStateException(
                        "generated MAT codec Java type mismatch: " + generatedName);
            }
            return codec;
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(
                    "cannot load generated MAT codec: " + generatedName, failure);
        }
    }

    private static String generatedCodecClassName(Class<?> type) {
        StringBuilder simple = new StringBuilder();
        Class<?> current = type;
        while (current != null) {
            if (simple.length() > 0) simple.insert(0, '_');
            simple.insert(0, current.getSimpleName());
            current = current.getEnclosingClass();
        }
        String packageName = type.getPackageName();
        String name = simple + "MatCodec";
        return packageName.isEmpty() ? name : packageName + "." + name;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static MatCodec<?> enumCodecUnchecked(Class<?> type) {
        return enumCodec((Class) type);
    }

    public static <E extends Enum<E>> MatCodec<E> enumCodec(Class<E> type) {
        Objects.requireNonNull(type, "type");
        if (!type.isEnum()) throw new IllegalArgumentException("not enum");
        return new MatCodec<>() {
            @Override public String typeId() { return "synex:enum/1:" + type.getName(); }
            @Override public Class<E> javaType() { return type; }
            @Override public void encode(E value, MatSink sink) { sink.utf8(value.name()); }
            @Override public E decode(MatSource source) { return Enum.valueOf(type, source.utf8()); }
        };
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        throw new IllegalArgumentException("unsupported primitive " + type);
    }

    @FunctionalInterface private interface Encoder<T> { void encode(T value, MatSink sink); }
    @FunctionalInterface private interface Decoder<T> { T decode(MatSource source); }

    private static <T> MatCodec<T> fixed(
            String id, Class<T> type, Encoder<T> encoder, Decoder<T> decoder) {
        return new MatCodec<>() {
            @Override public String typeId() { return id; }
            @Override public Class<T> javaType() { return type; }
            @Override public void encode(T value, MatSink sink) { encoder.encode(value, sink); }
            @Override public T decode(MatSource source) { return decoder.decode(source); }
        };
    }
}
