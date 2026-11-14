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
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/main/java/com/synexia/common/intern/M3InternShapes.java blob af98b1c63deb; recipe make/m3/intern/recipe/intern-space-20261106/relocate.py v4
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
}
