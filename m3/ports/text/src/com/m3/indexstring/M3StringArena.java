/* Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0 */
package com.m3.indexstring;

import com.synexia.indexstring.FrozenByteInterner;
import com.synexia.indexstring.FrozenChars;

/** Explicit local admission through the existing bounded immutable byte owner. */
public final class M3StringArena {
    private final FrozenByteInterner atoms;
    public M3StringArena(int maxEntries, long maxPayloadBytes) {
        atoms = new FrozenByteInterner(maxEntries, maxPayloadBytes);
    }
    /** Immutable String hits are compared before allocating a payload. */
    public M3String fromString(String text) {
        return new M3String(new FrozenChars[]{FrozenChars.fromUtf16Bytes(atoms.internUtf16(text))});
    }
    public int cachedAtoms() { return atoms.size(); }
    /** Heap payload only; excludes map/key/descriptor headers and live uncached atoms. */
    public long retainedPayloadBytes() { return atoms.payloadBytes(); }
    public void clear() { atoms.clear(); }
}
