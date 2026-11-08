// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package census3;

import java.util.List;
import jdk.m3.arrays.M3ArgumentRoleBits;
import jdk.m3.arrays.M3StringTypeBits;

/** Owner side: jdk.m3.arrays M3StringTypeBits and M3ArgumentRoleBits with the received atoms. */
public final class OwnerBits implements Census3BitsDifferential.Side {
    // constants are read reflectively: a compile-time constant would be inlined into this class and hide the owner value
    public int numeric() { return constant(M3StringTypeBits.class, "NUMERIC"); }
    public int typeKnown() { return constant(M3StringTypeBits.class, "KNOWN_MASK"); }
    public List<String> typeNames(int bits) { return M3StringTypeBits.names(bits); }
    public boolean typeHas(int bits, int required) { return M3StringTypeBits.has(bits, required); }
    public boolean typeHasAny(int bits, int mask) { return M3StringTypeBits.hasAny(bits, mask); }
    public int roleKnown() { return constant(M3ArgumentRoleBits.class, "KNOWN_MASK"); }
    public List<String> roleNames(int bits) { return M3ArgumentRoleBits.names(bits); }
    public boolean roleHas(int bits, int required) { return M3ArgumentRoleBits.has(bits, required); }
    public boolean roleHasAny(int bits, int mask) { return M3ArgumentRoleBits.hasAny(bits, mask); }
    public void roleRequireKnown(int bits) { M3ArgumentRoleBits.requireKnown(bits); }

    private static int constant(Class<?> type, String name) {
        try {
            return type.getField(name).getInt(null);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
