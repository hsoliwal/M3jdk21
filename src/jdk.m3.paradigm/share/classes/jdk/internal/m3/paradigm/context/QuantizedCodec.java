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
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Bounded QCU8/v1 binary data codec; checksum is integrity evidence, not authentication.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/quantized/QuantizedCodec.java}
 * blob {@code f6f7c2e5787fe2ec602ee94ba70b883c3be86620} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class QuantizedCodec {
    public static final int ENVELOPE_BYTES = 84;
    private static final int MAGIC = 0x38554351;
    private QuantizedCodec() { }

    public static byte[] encode(QuantizedSchema schema, QuantizedContextValues value) {
        Objects.requireNonNull(schema, "schema").validate(value);
        byte[] result = new byte[ENVELOPE_BYTES + value.size()];
        ByteBuffer out = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(MAGIC).putInt(1).put(schema.digest()).putLong(value.flags()).putInt(value.size());
        out.put(value.strengthsCopy());
        out.put(checksum(result, result.length - 32));
        return result;
    }

    /** Defensively captures caller bytes. Returns data only, never a live cell or executable object. */
    public static QuantizedContextValues decode(QuantizedSchema schema, byte[] encoded) {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(encoded, "encoded");
        if (encoded.length < ENVELOPE_BYTES || encoded.length > ENVELOPE_BYTES + QuantizedSchema.MAX_SLOTS) {
            throw new IllegalArgumentException("QCU8 size bound");
        }
        byte[] data = encoded.clone();
        ByteBuffer in = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (in.getInt() != MAGIC || in.getInt() != 1) throw new IllegalArgumentException("QCU8 header/version");
        byte[] schemaDigest = new byte[32];
        in.get(schemaDigest);
        if (!MessageDigest.isEqual(schemaDigest, schema.digest())) {
            throw new IllegalArgumentException("QCU8 schema mismatch");
        }
        long flags = in.getLong();
        int count = in.getInt();
        if (count != schema.size() || data.length != ENVELOPE_BYTES + count) {
            throw new IllegalArgumentException("QCU8 count/trailing data");
        }
        byte[] stored = Arrays.copyOfRange(data, data.length - 32, data.length);
        if (!MessageDigest.isEqual(stored, checksum(data, data.length - 32))) {
            throw new IllegalArgumentException("QCU8 checksum mismatch");
        }
        if ((flags & ~schema.allowedFlags()) != 0L) throw new IllegalArgumentException("QCU8 undeclared flags");
        byte[] strengths = new byte[count];
        in.get(strengths);
        return QuantizedContextValues.copyOf(flags, strengths);
    }

    /** Identity of schema plus raw flags/strengths, without timestamps or generated context ids. */
    public static String root(QuantizedSchema schema, QuantizedContextValues value) {
        byte[] encoded = encode(schema, value);
        return HexFormat.of().formatHex(encoded, encoded.length - 32, encoded.length);
    }

    static byte[] checksum(byte[] data, int length) {
        MessageDigest hash = sha256();
        hash.update(data, 0, length);
        return hash.digest();
    }

    static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
