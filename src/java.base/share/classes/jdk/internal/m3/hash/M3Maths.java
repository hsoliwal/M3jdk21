/*
 * Changed by Synexia (2026-10-31): zero-allocation-hashing 0.16 (Apache License 2.0) absorbed into java.base
 * as jdk.internal.m3.hash; the changes are listed in src/java.base/share/legal/zero-allocation-hashing.md.
 */
package jdk.internal.m3.hash;


class M3Maths {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** The low 32-bit half of a long. */
    private static final long LOW_32_MASK = 0xFFFFFFFFL;
    /** Distance between the low and the high 32-bit half of a long. */
    private static final int HALF_SHIFT = 32;

    private static final M3Maths INSTANCE;

    static {
        boolean hasMultiplyHigh = true;
        try {
          Math.multiplyHigh(0, 0);
        } catch (final Throwable ignore) {
          hasMultiplyHigh = false;
        }
        INSTANCE = hasMultiplyHigh ? new M3MathsJDK9() : new M3Maths();
    }

    public static long unsignedLongMulXorFold(final long lhs, final long rhs) {
        return INSTANCE.unsignedLongMulXorFoldImp(lhs, rhs);
    }
    public static long unsignedLongMulHigh(final long lhs, final long rhs) {
        return INSTANCE.unsignedLongMulHighImp(lhs, rhs);
    }

    long unsignedLongMulXorFoldImp(final long lhs, final long rhs) {
        // The Grade School method of multiplication is a hair faster in Java, primarily used here
        // because the implementation is simpler.
        final long lhs_l = lhs & LOW_32_MASK;
        final long lhs_h = lhs >>> HALF_SHIFT;
        final long rhs_l = rhs & LOW_32_MASK;
        final long rhs_h = rhs >>> HALF_SHIFT;
        final long lo_lo = lhs_l * rhs_l;
        final long hi_lo = lhs_h * rhs_l;
        final long lo_hi = lhs_l * rhs_h;
        final long hi_hi = lhs_h * rhs_h;

        // Add the products together. This will never overflow.
        final long cross = (lo_lo >>> HALF_SHIFT) + (hi_lo & LOW_32_MASK) + lo_hi;
        final long upper = (hi_lo >>> HALF_SHIFT) + (cross >>> HALF_SHIFT) + hi_hi;
        final long lower = (cross << HALF_SHIFT) | (lo_lo & LOW_32_MASK);
        return lower ^ upper;
    }

    long unsignedLongMulHighImp(final long lhs, final long rhs) {
        // The Grade School method of multiplication is a hair faster in Java, primarily used here
        // because the implementation is simpler.
        final long lhs_l = lhs & LOW_32_MASK;
        final long lhs_h = lhs >>> HALF_SHIFT;
        final long rhs_l = rhs & LOW_32_MASK;
        final long rhs_h = rhs >>> HALF_SHIFT;
        final long lo_lo = lhs_l * rhs_l;
        final long hi_lo = lhs_h * rhs_l;
        final long lo_hi = lhs_l * rhs_h;
        final long hi_hi = lhs_h * rhs_h;

        // Add the products together. This will never overflow.
        final long cross = (lo_lo >>> HALF_SHIFT) + (hi_lo & LOW_32_MASK) + lo_hi;
        final long upper = (hi_lo >>> HALF_SHIFT) + (cross >>> HALF_SHIFT) + hi_hi;
        return upper;
    }
}

class M3MathsJDK9 extends M3Maths {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** x >> 63 is all ones for a negative x: the correction from the signed to the unsigned high product. */
    private static final int SIGN_SHIFT = 63;

    // Math.multiplyHigh() is intrinsified from JDK 10. But JDK 9 is out of life, we always prefer
    // this version to the scalar one.
    @Override
    long unsignedLongMulXorFoldImp(final long lhs, final long rhs) {
        final long upper = Math.multiplyHigh(lhs, rhs) + ((lhs >> SIGN_SHIFT) & rhs) + ((rhs >> SIGN_SHIFT) & lhs);
        final long lower = lhs * rhs;
        return lower ^ upper;
    }
    @Override
    long unsignedLongMulHighImp(final long lhs, final long rhs) {
        return Math.multiplyHigh(lhs, rhs) + ((lhs >> SIGN_SHIFT) & rhs) + ((rhs >> SIGN_SHIFT) & lhs);
    }
}
