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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Shared exact-name schema. Slot positions are caller-declared, not sorted or inferred.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/quantized/QuantizedSchema.java}
 * blob {@code e40cfe12c6471e36cbcc327baa71e013d8c9ab78} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class QuantizedSchema {
    public static final int MAX_SLOTS = 65_536;
    private final String name;
    private final int version;
    private final String[] flags;
    private final String[] strengths;
    private final byte[] digest;

    public QuantizedSchema(String name, int version, String[] flags, String[] strengths) {
        this.name = validName(name);
        if (version < 1) throw new IllegalArgumentException("positive schema version required");
        this.version = version;
        this.flags = names(flags, Long.SIZE);
        this.strengths = names(strengths, MAX_SLOTS);
        MessageDigest hash = QuantizedCodec.sha256();
        add(hash, "synexia.quantized-schema/v1");
        add(hash, name);
        number(hash, version);
        sequence(hash, this.flags);
        sequence(hash, this.strengths);
        digest = hash.digest();
    }

    public String name() { return name; }
    public int version() { return version; }
    public int flagCount() { return flags.length; }
    public int size() { return strengths.length; }
    public String root() { return HexFormat.of().formatHex(digest); }
    public byte[] digest() { return digest.clone(); }
    public String flagName(int index) { return flags[index]; }
    public String strengthName(int index) { return strengths[index]; }
    public int flagIndex(String exactName) { return index(flags, exactName); }
    public int strengthIndex(String exactName) { return index(strengths, exactName); }

    /** Full bit 63 is valid when 64 flags are declared; no reserved sentinel bit. */
    public long allowedFlags() { return flags.length == Long.SIZE ? -1L : (1L << flags.length) - 1L; }

    public void validate(QuantizedContextValues value) {
        Objects.requireNonNull(value, "value");
        if (value.size() != size()) throw new IllegalArgumentException("schema slot count mismatch");
        if ((value.flags() & ~allowedFlags()) != 0L) throw new IllegalArgumentException("undeclared flag bit");
    }

    private static int index(String[] names, String name) {
        Objects.requireNonNull(name, "name");
        for (int i = 0; i < names.length; i++) if (names[i].equals(name)) return i;
        throw new IllegalArgumentException("unknown exact schema name: " + name);
    }

    private static String[] names(String[] input, int limit) {
        Objects.requireNonNull(input, "names");
        if (input.length > limit) throw new IllegalArgumentException("schema dimension limit");
        String[] result = input.clone();
        HashSet<String> seen = new HashSet<>();
        for (String value : result) {
            if (!seen.add(validName(value))) throw new IllegalArgumentException("duplicate schema name");
        }
        return result;
    }

    private static String validName(String value) {
        Objects.requireNonNull(value, "name");
        if (value.isBlank() || !value.equals(value.strip()) || value.length() > 256
                || !StandardCharsets.UTF_8.newEncoder().canEncode(value)
                || value.getBytes(StandardCharsets.UTF_8).length > 256) {
            throw new IllegalArgumentException("invalid schema name");
        }
        return value;
    }

    private static void sequence(MessageDigest hash, String[] values) {
        number(hash, values.length);
        for (String value : values) add(hash, value);
    }

    private static void add(MessageDigest hash, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        number(hash, bytes.length);
        hash.update(bytes);
    }

    private static void number(MessageDigest hash, int value) {
        hash.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }
}
