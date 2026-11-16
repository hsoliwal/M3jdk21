// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

/**
 * Typed, immutable structural projections backed entirely by one InternSpace.
 * No second interning owner, Java object pooling, native pointers or class identity
 * substitution is introduced. All handles are meaningful only inside this space.
 *
 * <p>Tuple kinds are domain-separated. The caller assigns stable resolution scope
 * identifiers and MUST NOT recycle them while cached type handles are live.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/main/java/com/synexia/common/intern/M3InternShapes.java blob af98b1c63deb; recipe m3/intern/recipe/intern-space-20261106/relocate.py v4
public final class InternShapes {
    private static final int TYPE = 0x4d335459;   // M3TY, existing internType kind
    private static final int METHOD = 0x4d334d54; // M3MT
    private static final int RECORD = 0x4d335243; // M3RC
    private static final int LONGS = 0x4d334c47;  // M3LG
    private static final int MASK = 0x4d334d4b;   // M3MK
    private static final int TOKENS = 0x4d33544b; // M3TK
    private final InternSpace space;

    public InternShapes(InternSpace space) {
        this.space = Objects.requireNonNull(space, "space");
    }

    /**
     * Share a method's immutable symbolic shape, never java.lang.reflect.Method
     * identity. The return and every argument type must belong to this loader scope.
     */
    public long internMethod(long loaderScope, String name, long returnType, long... parameterTypes) {
        requireScope(loaderScope);
        long[] parameters = Objects.requireNonNull(parameterTypes, "parameterTypes").clone();
        requireType(returnType, loaderScope);
        for (long parameter : parameters) requireType(parameter, loaderScope);
        long[] record = new long[parameters.length + 3];
        record[0] = loaderScope;
        record[1] = space.internString(Objects.requireNonNull(name, "name"));
        record[2] = returnType;
        System.arraycopy(parameters, 0, record, 3, parameters.length);
        return space.shapes().intern(METHOD, record);
    }

    /**
     * Ordered field-name/type pairs. Java record component order is observable,
     * so the input is NEVER sorted or treated as an unordered set.
     */
    public long internRecord(long loaderScope, long recordType, String[] fieldNames, long[] fieldTypes) {
        requireScope(loaderScope);
        String[] names = Objects.requireNonNull(fieldNames, "fieldNames").clone();
        long[] types = Objects.requireNonNull(fieldTypes, "fieldTypes").clone();
        if (names.length != types.length) throw new IllegalArgumentException("record field counts differ");
        requireType(recordType, loaderScope);
        long[] record = new long[Math.addExact(2, Math.multiplyExact(names.length, 2))];
        record[0] = loaderScope;
        record[1] = recordType;
        for (int i = 0; i < names.length; i++) {
            requireType(types[i], loaderScope);
            record[2 + 2 * i] = space.internString(Objects.requireNonNull(names[i], "fieldName"));
            record[3 + 2 * i] = types[i];
        }
        return space.shapes().intern(RECORD, record);
    }

    /** Frozen long[] represented in a shared byte blob. Returns a shape handle. */
    public long internFrozenLongs(long[] values) {
        long[] copy = Objects.requireNonNull(values, "values").clone();
        byte[] bytes = new byte[Math.multiplyExact(copy.length, Long.BYTES)];
        for (int i = 0; i < copy.length; i++) {
            long v = copy[i];
            int at = i * Long.BYTES;
            for (int b = 7; b >= 0; b--) {
                bytes[at + b] = (byte) v;
                v >>>= 8;
            }
        }
        return space.shapes().intern(LONGS, space.blobs().intern(bytes), copy.length);
    }

    /** Independent mutable output. Never leaks a shared underlying byte array. */
    public long[] copyFrozenLongs(long handle) {
        long[] values = ofKind(handle, LONGS, 2);
        if (values[1] < 0 || values[1] > Integer.MAX_VALUE / 8) {
            throw new IllegalStateException("invalid long shape size");
        }
        byte[] bytes = space.blobs().copy(values[0]);
        if (bytes.length != values[1] * Long.BYTES) throw new IllegalStateException("long shape length mismatch");
        long[] answer = new long[(int) values[1]];
        for (int i = 0; i < answer.length; i++) {
            long v = 0;
            for (int b = 0; b < Long.BYTES; b++) v = (v << 8) | (bytes[i * 8 + b] & 255L);
            answer[i] = v;
        }
        return answer;
    }

    /** Canonical logical bit mask: trailing zero words have no semantic meaning. */
    public long internBitMask(BitSet value) {
        long[] normalized = Objects.requireNonNull(value, "value").toLongArray();
        return space.shapes().intern(MASK, space.blobs().intern(bytesOf(normalized)), normalized.length);
    }

    public BitSet copyBitMask(long handle) {
        long[] fields = ofKind(handle, MASK, 2);
        if (fields[1] < 0 || fields[1] > Integer.MAX_VALUE / 8) {
            throw new IllegalStateException("invalid mask length");
        }
        byte[] bytes = space.blobs().copy(fields[0]);
        if (bytes.length != fields[1] * 8) throw new IllegalStateException("mask length mismatch");
        long[] words = new long[(int) fields[1]];
        for (int i = 0; i < words.length; i++) {
            for (int b = 0; b < 8; b++) words[i] = (words[i] << 8) | (bytes[i * 8 + b] & 255L);
        }
        return BitSet.valueOf(words);
    }

    /** Exact token order in a caller-specified lexicon; never crosses lexicon scopes. */
    public long internTokens(long lexiconScope, String... tokens) {
        requireScope(lexiconScope);
        String[] input = Objects.requireNonNull(tokens, "tokens").clone();
        long[] record = new long[input.length + 1];
        record[0] = lexiconScope;
        for (int i = 0; i < input.length; i++) {
            record[i + 1] = space.internString(Objects.requireNonNull(input[i], "token"));
        }
        return space.shapes().intern(TOKENS, record);
    }

    public String[] copyTokens(long handle) {
        long[] record = ofKind(handle, TOKENS, -1);
        String[] tokens = new String[record.length - 1];
        for (int i = 0; i < tokens.length; i++) tokens[i] = space.string(record[i + 1]);
        return tokens;
    }

    private void requireType(long id, long scope) {
        if (space.shapes().kind(id) != TYPE || space.shapes().members(id)[0] != scope) {
            throw new IllegalArgumentException("type belongs to another resolution scope");
        }
    }

    private static void requireScope(long scope) {
        if (scope < 0) throw new IllegalArgumentException("negative scope");
    }

    private long[] ofKind(long id, int expected, int size) {
        if (space.shapes().kind(id) != expected) throw new IllegalArgumentException("wrong shape kind");
        long[] values = space.shapes().members(id);
        if (size >= 0 && values.length != size) throw new IllegalStateException("invalid shape arity");
        if (size < 0 && values.length == 0) throw new IllegalStateException("missing scope");
        return values;
    }

    private static byte[] bytesOf(long[] values) {
        byte[] bytes = new byte[Math.multiplyExact(values.length, 8)];
        for (int i = 0; i < values.length; i++) {
            long v = values[i];
            for (int j = 7; j >= 0; j--) {
                bytes[8 * i + j] = (byte) v;
                v >>>= 8;
            }
        }
        return bytes;
    }

    // Read-only composite interning (user directive 2026-11-16: "all read only composites allow the same interning").
    // Same owner, same handles: a deeply immutable composite becomes a domain-separated tuple of its members' handles.
    private static final int STR = 0x4d335352;   // M3SR
    private static final int LIST = 0x4d334c53;  // M3LS
    private static final int SET = 0x4d335345;   // M3SE
    private static final int MAP = 0x4d334d50;   // M3MP
    private static final int RVAL = 0x4d335256;  // M3RV
    private static final int ENUM = 0x4d33454e;  // M3EN
    private static final int PRIM = 0x4d335052;  // M3PR
    private static final int NULLV = 0x4d334e4c; // M3NL
    private static final int MAX_DEPTH = 1024;
    private static final String IMMUTABLE_PREFIX = "java.util.ImmutableCollections$";

    /**
     * Interns any read-only composite value: null, String, boxed primitives, enums, records, {@link Internable}
     * values and the immutable lists, sets and maps of {@code List.of}, {@code Set.of} and {@code Map.of}.
     *
     * @param loaderScope resolution scope of record and enum types met inside the value
     * @param value the value
     * @return its handle; equal values have equal handles in this space
     * @throws IllegalArgumentException if the value or a member is mutable or not a read-only composite
     */
    public long internValue(long loaderScope, Object value) {
        requireScope(loaderScope);
        return value(loaderScope, value, 0);
    }

    /**
     * Interns an immutable list; element order is significant.
     *
     * @param loaderScope resolution scope of record and enum types met inside the list
     * @param list an immutable list ({@code List.of}, {@code List.copyOf})
     * @return its handle
     * @throws IllegalArgumentException if the list is not immutable (use {@code internSnapshot})
     */
    public long internList(long loaderScope, java.util.List<?> list) {
        requireScope(loaderScope);
        requireImmutable(list);
        return list(loaderScope, list, 0);
    }

    /**
     * Interns an immutable set; element order is not significant.
     *
     * @param loaderScope resolution scope of record and enum types met inside the set
     * @param set an immutable set ({@code Set.of}, {@code Set.copyOf})
     * @return its handle
     * @throws IllegalArgumentException if the set is not immutable (use {@code internSnapshot})
     */
    public long internSet(long loaderScope, java.util.Set<?> set) {
        requireScope(loaderScope);
        requireImmutable(set);
        return set(loaderScope, set, 0);
    }

    /**
     * Interns an immutable map; entry order is not significant.
     *
     * @param loaderScope resolution scope of record and enum types met inside the map
     * @param map an immutable map ({@code Map.of}, {@code Map.copyOf})
     * @return its handle
     * @throws IllegalArgumentException if the map is not immutable (use {@code internSnapshot})
     */
    public long internMap(long loaderScope, java.util.Map<?, ?> map) {
        requireScope(loaderScope);
        requireImmutable(map);
        return map(loaderScope, map, 0);
    }

    /**
     * Interns a record value: its type plus its component values in declaration order.
     *
     * @param loaderScope resolution scope of the record type
     * @param value the record
     * @return its handle
     * @throws IllegalArgumentException if a component is not readable or not a read-only composite
     */
    public long internRecordValue(long loaderScope, Record value) {
        requireScope(loaderScope);
        return recordValue(loaderScope, Objects.requireNonNull(value, "value"), 0);
    }

    /**
     * Interns an enum constant by its declaring type and name.
     *
     * @param loaderScope resolution scope of the enum type
     * @param value the constant
     * @return its handle
     */
    public long internEnum(long loaderScope, Enum<?> value) {
        requireScope(loaderScope);
        return enumValue(loaderScope, Objects.requireNonNull(value, "value"));
    }

    /**
     * Interns an immutable snapshot of a read-only view (for example an unmodifiable view over a mutable list):
     * the view is copied with {@code Set.copyOf} when it is a set and {@code List.copyOf} otherwise.
     *
     * @param loaderScope resolution scope of record and enum types met inside the view
     * @param view the read-only view
     * @return the handle of the snapshot
     */
    public long internSnapshot(long loaderScope, java.util.Collection<?> view) {
        requireScope(loaderScope);
        Objects.requireNonNull(view, "view");
        if (view instanceof java.util.Set<?> s) {
            return set(loaderScope, java.util.Set.copyOf(s), 0);
        }
        return list(loaderScope, java.util.List.copyOf(view), 0);
    }

    /**
     * Interns an immutable snapshot of a read-only map view, copied with {@code Map.copyOf}.
     *
     * @param loaderScope resolution scope of record and enum types met inside the view
     * @param view the read-only view
     * @return the handle of the snapshot
     */
    public long internSnapshot(long loaderScope, java.util.Map<?, ?> view) {
        requireScope(loaderScope);
        return map(loaderScope, java.util.Map.copyOf(Objects.requireNonNull(view, "view")), 0);
    }

    private long value(long scope, Object v, int depth) {
        if (depth > MAX_DEPTH) throw new IllegalArgumentException("composite too deep");
        if (v == null) return space.shapes().intern(NULLV);
        if (v instanceof String s) return space.shapes().intern(STR, space.internString(s));
        if (v instanceof Boolean b) return prim(1, b ? 1 : 0);
        if (v instanceof Byte b) return prim(2, b);
        if (v instanceof Short s) return prim(3, s);
        if (v instanceof Character c) return prim(4, c);
        if (v instanceof Integer i) return prim(5, i);
        if (v instanceof Long l) return prim(6, l);
        if (v instanceof Float f) return prim(7, Float.floatToIntBits(f));
        if (v instanceof Double d) return prim(8, Double.doubleToLongBits(d));
        if (v instanceof Enum<?> e) return enumValue(scope, e);
        if (v instanceof Record r) return recordValue(scope, r, depth);
        if (v instanceof Internable i) return i.internInto(this);
        if (v instanceof java.util.List<?> l && isImmutable(l)) return list(scope, l, depth);
        if (v instanceof java.util.Set<?> s && isImmutable(s)) return set(scope, s, depth);
        if (v instanceof java.util.Map<?, ?> m && isImmutable(m)) return map(scope, m, depth);
        throw new IllegalArgumentException("not a read-only composite: " + v.getClass().getName());
    }

    private long prim(int tag, long bits) {
        return space.shapes().intern(PRIM, tag, bits);
    }

    private long list(long scope, java.util.List<?> l, int depth) {
        long[] m = new long[l.size()];
        int i = 0;
        for (Object e : l) m[i++] = value(scope, e, depth + 1);
        return space.shapes().intern(LIST, m);
    }

    private long set(long scope, java.util.Set<?> s, int depth) {
        long[] m = new long[s.size()];
        int i = 0;
        for (Object e : s) m[i++] = value(scope, e, depth + 1);
        Arrays.sort(m);
        return space.shapes().intern(SET, m);
    }

    private long map(long scope, java.util.Map<?, ?> m, int depth) {
        long[][] pairs = new long[m.size()][];
        int i = 0;
        for (java.util.Map.Entry<?, ?> e : m.entrySet()) {
            pairs[i++] = new long[] {value(scope, e.getKey(), depth + 1), value(scope, e.getValue(), depth + 1)};
        }
        Arrays.sort(pairs, (a, b) -> Long.compare(a[0], b[0]));
        long[] flat = new long[Math.multiplyExact(pairs.length, 2)];
        for (int k = 0; k < pairs.length; k++) {
            flat[2 * k] = pairs[k][0];
            flat[2 * k + 1] = pairs[k][1];
        }
        return space.shapes().intern(MAP, flat);
    }

    private long enumValue(long scope, Enum<?> e) {
        long type = space.internType(scope, descriptor(e.getDeclaringClass()));
        return space.shapes().intern(ENUM, type, space.internString(e.name()));
    }

    private long recordValue(long scope, Record r, int depth) {
        Class<?> c = r.getClass();
        java.lang.reflect.RecordComponent[] rc = c.getRecordComponents();
        long[] m = new long[rc.length + 1];
        m[0] = space.internType(scope, descriptor(c));
        for (int i = 0; i < rc.length; i++) {
            Object component;
            try {
                // records are rarely public; their accessors are readable whenever the package is open to java.base
                // (always for the class path), otherwise the record is refused below
                java.lang.reflect.Method accessor = rc[i].getAccessor();
                accessor.setAccessible(true);
                component = accessor.invoke(r);
            } catch (ReflectiveOperationException | java.lang.reflect.InaccessibleObjectException ex) {
                throw new IllegalArgumentException("record component not readable: " + rc[i].getName(), ex);
            }
            m[i + 1] = value(scope, component, depth + 1);
        }
        return space.shapes().intern(RVAL, m);
    }

    private static String descriptor(Class<?> c) {
        return "L" + c.getName().replace('.', '/') + ";";
    }

    private static boolean isImmutable(Object o) {
        return o.getClass().getName().startsWith(IMMUTABLE_PREFIX);
    }

    private static void requireImmutable(Object o) {
        Objects.requireNonNull(o, "value");
        if (!isImmutable(o)) {
            throw new IllegalArgumentException("not an immutable composite; use internSnapshot: " + o.getClass().getName());
        }
    }
}
