// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

/**
 * Four-valued truth used by internal M3 reasoning precompute.
 *
 * <p>The two low bits are asserted-positive and asserted-negative respectively.</p>
 */
public enum M3Truth {
    UNKNOWN(0),
    TRUE(1),
    FALSE(2),
    BOTH(3);

    private final byte bits;

    M3Truth(int bits) {
        this.bits = (byte) bits;
    }

    byte bits() {
        return bits;
    }

    public boolean assertedTrue() {
        return (bits & 1) != 0;
    }

    public boolean assertedFalse() {
        return (bits & 2) != 0;
    }

    static M3Truth fromBits(int bits) {
        return switch (bits & 3) {
            case 0 -> UNKNOWN;
            case 1 -> TRUE;
            case 2 -> FALSE;
            case 3 -> BOTH;
            default -> throw new AssertionError();
        };
    }
}
