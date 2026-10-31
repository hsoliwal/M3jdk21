/*
 * Changed by Synexia (2026-10-31): zero-allocation-hashing 0.16 (Apache License 2.0) absorbed into java.base
 * as jdk.internal.m3.hash; the changes are listed in src/java.base/share/legal/zero-allocation-hashing.md.
 */
package jdk.internal.m3.hash;


class M3Maths {
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
        final long lhs_l = lhs & 0xFFFFFFFFL;
        final long lhs_h = lhs >>> 32;
        final long rhs_l = rhs & 0xFFFFFFFFL;
        final long rhs_h = rhs >>> 32;
        final long lo_lo = lhs_l * rhs_l;
        final long hi_lo = lhs_h * rhs_l;
        final long lo_hi = lhs_l * rhs_h;
        final long hi_hi = lhs_h * rhs_h;

        // Add the products together. This will never overflow.
        final long cross = (lo_lo >>> 32) + (hi_lo & 0xFFFFFFFFL) + lo_hi;
        final long upper = (hi_lo >>> 32) + (cross >>> 32) + hi_hi;
        final long lower = (cross << 32) | (lo_lo & 0xFFFFFFFFL);
        return lower ^ upper;
    }

    long unsignedLongMulHighImp(final long lhs, final long rhs) {
        // The Grade School method of multiplication is a hair faster in Java, primarily used here
        // because the implementation is simpler.
        final long lhs_l = lhs & 0xFFFFFFFFL;
        final long lhs_h = lhs >>> 32;
        final long rhs_l = rhs & 0xFFFFFFFFL;
        final long rhs_h = rhs >>> 32;
        final long lo_lo = lhs_l * rhs_l;
        final long hi_lo = lhs_h * rhs_l;
        final long lo_hi = lhs_l * rhs_h;
        final long hi_hi = lhs_h * rhs_h;

        // Add the products together. This will never overflow.
        final long cross = (lo_lo >>> 32) + (hi_lo & 0xFFFFFFFFL) + lo_hi;
        final long upper = (hi_lo >>> 32) + (cross >>> 32) + hi_hi;
        return upper;
    }
}

class M3MathsJDK9 extends M3Maths {
    // Math.multiplyHigh() is intrinsified from JDK 10. But JDK 9 is out of life, we always prefer
    // this version to the scalar one.
    @Override
    long unsignedLongMulXorFoldImp(final long lhs, final long rhs) {
        final long upper = Math.multiplyHigh(lhs, rhs) + ((lhs >> 63) & rhs) + ((rhs >> 63) & lhs);
        final long lower = lhs * rhs;
        return lower ^ upper;
    }
    @Override
    long unsignedLongMulHighImp(final long lhs, final long rhs) {
        return Math.multiplyHigh(lhs, rhs) + ((lhs >> 63) & rhs) + ((rhs >> 63) & lhs);
    }
}
