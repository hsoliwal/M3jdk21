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
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable sparse difference, bound to complete schema/preimage/postimage roots; no write authority.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/quantized/QuantizedPatch.java}
 * blob {@code 4cbc7da9facc16e5945c2acfc5ab099eb32171c3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class QuantizedPatch {
    private final QuantizedSchema schema;
    private final String beforeRoot;
    private final String afterRoot;
    private final long beforeFlags;
    private final long afterFlags;
    private final int[] slots;
    private final byte[] before;
    private final byte[] after;
    private final String root;

    private QuantizedPatch(QuantizedSchema schema, String beforeRoot, String afterRoot,
            long beforeFlags, long afterFlags, int[] slots, byte[] before, byte[] after) {
        this.schema = schema;
        this.beforeRoot = beforeRoot;
        this.afterRoot = afterRoot;
        this.beforeFlags = beforeFlags;
        this.afterFlags = afterFlags;
        this.slots = slots;
        this.before = before;
        this.after = after;
        ByteBuffer identity = ByteBuffer.allocate(120 + 6 * slots.length).order(ByteOrder.LITTLE_ENDIAN);
        identity.putInt(0x31504451).put(schema.digest());
        identity.put(HexFormat.of().parseHex(beforeRoot)).put(HexFormat.of().parseHex(afterRoot));
        identity.putLong(beforeFlags).putLong(afterFlags).putInt(slots.length);
        for (int i = 0; i < slots.length; i++) identity.putInt(slots[i]).put(before[i]).put(after[i]);
        root = HexFormat.of().formatHex(QuantizedCodec.checksum(identity.array(), identity.position()));
    }

    public static QuantizedPatch between(
            QuantizedSchema schema, QuantizedContextValues before, QuantizedContextValues after) {
        Objects.requireNonNull(schema, "schema").validate(before);
        schema.validate(after);
        int count = 0;
        for (int i = 0; i < before.size(); i++) if (before.strengthU8(i) != after.strengthU8(i)) count++;
        int[] slots = new int[count];
        byte[] oldBytes = new byte[count];
        byte[] newBytes = new byte[count];
        int cursor = 0;
        for (int i = 0; i < before.size(); i++) {
            if (before.strengthU8(i) != after.strengthU8(i)) {
                slots[cursor] = i;
                oldBytes[cursor] = (byte) before.strengthU8(i);
                newBytes[cursor++] = (byte) after.strengthU8(i);
            }
        }
        return new QuantizedPatch(schema, QuantizedCodec.root(schema, before), QuantizedCodec.root(schema, after),
                before.flags(), after.flags(), slots, oldBytes, newBytes);
    }

    public String root() { return root; }
    public String schemaRoot() { return schema.root(); }
    public String beforeRoot() { return beforeRoot; }
    public String afterRoot() { return afterRoot; }
    public int changedCount() { return slots.length; }
    public int[] changedSlots() { return slots.clone(); }
    public byte[] beforeBytes() { return before.clone(); }
    public byte[] afterBytes() { return after.clone(); }
    public long changedFlags() { return beforeFlags ^ afterFlags; }

    /** Reapplying to the complete postimage returns it unchanged; any other stale input is rejected. */
    public QuantizedContextValues apply(QuantizedSchema suppliedSchema, QuantizedContextValues value) {
        if (!schemaRoot().equals(Objects.requireNonNull(suppliedSchema, "schema").root())) {
            throw new IllegalArgumentException("patch schema mismatch");
        }
        String current = QuantizedCodec.root(schema, value);
        if (afterRoot.equals(current)) return value;
        if (!beforeRoot.equals(current)) throw new IllegalArgumentException("patch preimage mismatch");
        QuantizedContextValues result = value.withBatch(afterFlags, slots, after);
        if (!afterRoot.equals(QuantizedCodec.root(schema, result))) throw new IllegalStateException("patch postimage");
        return result;
    }

    /** Private immutable arrays may be shared because every external accessor copies them. */
    public QuantizedPatch inverse() {
        return new QuantizedPatch(schema, afterRoot, beforeRoot, afterFlags, beforeFlags, slots, after, before);
    }
}
