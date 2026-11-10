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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Explicit tagged values for a context cell. No reflection, class loading or Java
 * serialization. Supported containers are copied; cycles and unknown types fail closed.
 * Numeric kinds, decimal scale, negative zero and binary bytes are retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/memory/MementoValues.java}
 * blob {@code 839773ab96f1e4874051dd785d69d1e5af48c597} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MementoValues {
    private static final int MAX_DEPTH = 64;
    private static final int MAX_VALUE_BYTES = 700_000;
    private static final String PREFIX = "cmv1:";

    private MementoValues() { }

    /** Encodes supported data only; each encoded value fits the cell byte limit. */
    public static String encode(Object value) {
        try {
            byte[] bytes = encodeBytes(value, 0, new IdentityHashMap<>());
            return PREFIX + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException exception) {
            throw new IllegalStateException("In-memory value encoding failed", exception);
        }
    }

    /** Returns fresh detached values. Maps, lists and sets are unmodifiable. */
    public static Object decode(String encoded) {
        Objects.requireNonNull(encoded, "encoded");
        if (!encoded.startsWith(PREFIX) || encoded.length() > MementoCodec.MAX_TEXT_BYTES) {
            throw new IllegalArgumentException("Unsupported or oversized context value");
        }
        byte[] bytes = Base64.getDecoder().decode(encoded.substring(PREFIX.length()));
        if (bytes.length > MAX_VALUE_BYTES) { throw new IllegalArgumentException("Oversized value"); }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            Object value = read(in, 0);
            if (in.read() != -1) { throw new IllegalArgumentException("Trailing value data"); }
            return value;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid context value", exception);
        }
    }

    private static byte[] encodeBytes(Object value, int depth, IdentityHashMap<Object, Boolean> path)
            throws IOException {
        checkDepth(depth);
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes)) {
            write(out, value, depth, path);
            out.flush();
            if (bytes.size() > MAX_VALUE_BYTES) { throw new IllegalArgumentException("Oversized value"); }
            return bytes.toByteArray();
        }
    }

    private static void write(DataOutputStream out, Object value, int depth,
            IdentityHashMap<Object, Boolean> path) throws IOException {
        checkDepth(depth);
        if (value == null) { out.writeByte('0'); }
        else if (value instanceof String text) { text(out, 's', text); }
        else if (value instanceof Boolean flag) { out.writeByte('b'); out.writeBoolean(flag); }
        else if (value instanceof Byte number) { out.writeByte('y'); out.writeByte(number); }
        else if (value instanceof Short number) { out.writeByte('h'); out.writeShort(number); }
        else if (value instanceof Integer number) { out.writeByte('i'); out.writeInt(number); }
        else if (value instanceof Long number) { out.writeByte('l'); out.writeLong(number); }
        else if (value instanceof Float number) {
            if (!Float.isFinite(number)) { throw new IllegalArgumentException("Non-finite float"); }
            out.writeByte('f'); out.writeFloat(number);
        } else if (value instanceof Double number) {
            if (!Double.isFinite(number)) { throw new IllegalArgumentException("Non-finite double"); }
            out.writeByte('d'); out.writeDouble(number);
        } else if (value instanceof BigInteger number) { text(out, 'I', number.toString()); }
        else if (value instanceof BigDecimal number) { text(out, 'D', number.toString()); }
        else if (value instanceof UUID id) { text(out, 'u', id.toString()); }
        else if (value instanceof Instant instant) { text(out, 't', instant.toString()); }
        else if (value instanceof Duration duration) { text(out, 'v', duration.toString()); }
        else if (value instanceof Character character) { out.writeByte('c'); out.writeChar(character); }
        else if (value instanceof MementoReference reference) {
            text(out, 'r', reference.contextId()); out.writeLong(reference.sourceRevision());
        } else if (value instanceof byte[] binary) {
            if (binary.length > MAX_VALUE_BYTES - 5) { throw new IllegalArgumentException("Oversized binary"); }
            out.writeByte('x'); out.writeInt(binary.length); out.write(binary);
        } else {
            writeContainer(out, value, depth, path);
        }
    }

    private static void writeContainer(DataOutputStream out, Object value, int depth,
            IdentityHashMap<Object, Boolean> path) throws IOException {
        if (path.put(value, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Cyclic value; use an explicit context reference");
        }
        try {
            if (value instanceof Map<?, ?> map) {
                checkCount(map.size());
                TreeMap<String, Object> sorted = new TreeMap<>();
                map.forEach((key, item) -> {
                    if (!(key instanceof String text)) {
                        throw new IllegalArgumentException("Only String map keys are supported");
                    }
                    sorted.put(text, item);
                });
                out.writeByte('m'); out.writeInt(sorted.size());
                int bytes = 0;
                for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                    MementoCodec.writeText(out, entry.getKey());
                    byte[] child = encodeBytes(entry.getValue(), depth + 1, path);
                    bytes = Math.addExact(bytes, child.length + entry.getKey().length() * 3 + 4);
                    if (bytes > MAX_VALUE_BYTES) { throw new IllegalArgumentException("Oversized map"); }
                    out.write(child);
                }
            } else if (value instanceof List<?> list) {
                checkCount(list.size());
                out.writeByte('a'); out.writeInt(list.size());
                int bytes = 0;
                for (Object item : list) {
                    byte[] child = encodeBytes(item, depth + 1, path);
                    bytes = Math.addExact(bytes, child.length);
                    if (bytes > MAX_VALUE_BYTES) { throw new IllegalArgumentException("Oversized list"); }
                    out.write(child);
                }
            } else if (value instanceof Set<?> set) {
                checkCount(set.size());
                List<byte[]> sorted = new ArrayList<>();
                int bytes = 0;
                for (Object item : set) {
                    if (!(item instanceof String)) {
                        throw new IllegalArgumentException("Set values must be String tokens");
                    }
                    byte[] child = encodeBytes(item, depth + 1, path);
                    bytes = Math.addExact(bytes, child.length);
                    if (bytes > MAX_VALUE_BYTES) { throw new IllegalArgumentException("Oversized set"); }
                    sorted.add(child);
                }
                sorted.sort(java.util.Arrays::compareUnsigned);
                out.writeByte('e'); out.writeInt(sorted.size());
                for (byte[] child : sorted) { out.write(child); }
            } else {
                throw new IllegalArgumentException("Unsupported value type: " + value.getClass().getName()
                        + "; provide an explicit data projection");
            }
        } finally { path.remove(value); }
    }

    private static Object read(DataInputStream in, int depth) throws IOException {
        checkDepth(depth);
        return switch (in.readUnsignedByte()) {
            case '0' -> null;
            case 's' -> MementoCodec.readText(in);
            case 'b' -> readBoolean(in);
            case 'y' -> in.readByte();
            case 'h' -> in.readShort();
            case 'i' -> in.readInt();
            case 'l' -> in.readLong();
            case 'f' -> finiteFloat(in.readFloat());
            case 'd' -> finiteDouble(in.readDouble());
            case 'I' -> new BigInteger(MementoCodec.readText(in));
            case 'D' -> new BigDecimal(MementoCodec.readText(in));
            case 'u' -> UUID.fromString(MementoCodec.readText(in));
            case 't' -> Instant.parse(MementoCodec.readText(in));
            case 'v' -> Duration.parse(MementoCodec.readText(in));
            case 'c' -> in.readChar();
            case 'r' -> new MementoReference(MementoCodec.readText(in), in.readLong());
            case 'x' -> readBinary(in);
            case 'm' -> readMap(in, depth);
            case 'a' -> readList(in, depth);
            case 'e' -> readSet(in, depth);
            default -> throw new IllegalArgumentException("Unknown context value tag");
        };
    }

    private static Map<String, Object> readMap(DataInputStream in, int depth) throws IOException {
        int count = in.readInt(); checkCount(count);
        Map<String, Object> values = new TreeMap<>();
        for (int index = 0; index < count; index++) {
            String key = MementoCodec.readText(in);
            if (values.containsKey(key)) { throw new IllegalArgumentException("Duplicate value key"); }
            values.put(key, read(in, depth + 1));
        }
        return Collections.unmodifiableMap(values);
    }

    private static List<Object> readList(DataInputStream in, int depth) throws IOException {
        int count = in.readInt(); checkCount(count);
        List<Object> values = new ArrayList<>();
        for (int index = 0; index < count; index++) { values.add(read(in, depth + 1)); }
        return Collections.unmodifiableList(values);
    }

    private static Set<String> readSet(DataInputStream in, int depth) throws IOException {
        int count = in.readInt(); checkCount(count);
        Set<String> values = new LinkedHashSet<>();
        for (int index = 0; index < count; index++) {
            Object item = read(in, depth + 1);
            if (!(item instanceof String text) || !values.add(text)) {
                throw new IllegalArgumentException("Invalid token set");
            }
        }
        return Collections.unmodifiableSet(values);
    }

    private static byte[] readBinary(DataInputStream in) throws IOException {
        int size = in.readInt();
        if (size < 0 || size > MAX_VALUE_BYTES || size > in.available()) {
            throw new IllegalArgumentException("Invalid binary length");
        }
        return in.readNBytes(size);
    }

    private static boolean readBoolean(DataInputStream in) throws IOException {
        int value = in.readUnsignedByte();
        if (value > 1) { throw new IllegalArgumentException("Invalid boolean"); }
        return value == 1;
    }

    private static float finiteFloat(float number) {
        if (!Float.isFinite(number)) { throw new IllegalArgumentException("Non-finite float"); }
        return number;
    }

    private static double finiteDouble(double number) {
        if (!Double.isFinite(number)) { throw new IllegalArgumentException("Non-finite double"); }
        return number;
    }

    private static void text(DataOutputStream out, int tag, String value) throws IOException {
        out.writeByte(tag); MementoCodec.writeText(out, value);
    }

    private static void checkDepth(int depth) {
        if (depth > MAX_DEPTH) { throw new IllegalArgumentException("Value nesting exceeds limit"); }
    }

    private static void checkCount(int count) {
        if (count < 0 || count > MementoCodec.MAX_CELLS) {
            throw new IllegalArgumentException("Invalid container size");
        }
    }
}
