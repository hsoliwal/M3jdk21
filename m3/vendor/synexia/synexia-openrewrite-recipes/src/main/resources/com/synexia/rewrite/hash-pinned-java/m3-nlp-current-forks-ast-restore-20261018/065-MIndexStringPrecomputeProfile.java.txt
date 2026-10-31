// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/** Bit-profile selecting immutable per-string precompute lanes. */
public record MIndexStringPrecomputeProfile(int mask) {
    public static final int HISTOGRAM = 1;
    public static final int PREFIX_FUNCTION = 1 << 1;
    public static final int Z_FUNCTION = 1 << 2;
    public static final int PALINDROME = 1 << 3;
    public static final int ROLLING_HASH = 1 << 4;
    public static final int SKETCH = 1 << 5;
    public static final int SUFFIX_ARRAY = 1 << 6;

    /** Metadata-only image: canonical identity, lengths, hashes, flags and conversions. */
    public static final MIndexStringPrecomputeProfile METADATA =
            new MIndexStringPrecomputeProfile(0);
    public static final MIndexStringPrecomputeProfile CORE =
            new MIndexStringPrecomputeProfile(HISTOGRAM);
    /** Lightweight exact UTF-16 range fingerprints for compiler/JDK shadow reuse. */
    public static final MIndexStringPrecomputeProfile RANGE_HASH =
            new MIndexStringPrecomputeProfile(ROLLING_HASH);
    /** Reusable KMP failure geometry for a canonical literal needle. */
    public static final MIndexStringPrecomputeProfile LITERAL_PATTERN =
            new MIndexStringPrecomputeProfile(PREFIX_FUNCTION);
    public static final MIndexStringPrecomputeProfile SEARCH =
            new MIndexStringPrecomputeProfile(HISTOGRAM | PREFIX_FUNCTION | Z_FUNCTION
                    | PALINDROME | ROLLING_HASH | SKETCH);
    public static final MIndexStringPrecomputeProfile INDEXED =
            new MIndexStringPrecomputeProfile(SEARCH.mask | SUFFIX_ARRAY);
    public static final MIndexStringPrecomputeProfile FULL = INDEXED;

    public MIndexStringPrecomputeProfile {
        int known = HISTOGRAM | PREFIX_FUNCTION | Z_FUNCTION | PALINDROME
                | ROLLING_HASH | SKETCH | SUFFIX_ARRAY;
        if ((mask & ~known) != 0) throw new IllegalArgumentException("unknown precompute lane");
    }

    public boolean has(int lane) { return (mask & lane) == lane; }

    public boolean includes(MIndexStringPrecomputeProfile other) {
        return (mask & other.mask) == other.mask;
    }

    public MIndexStringPrecomputeProfile union(MIndexStringPrecomputeProfile other) {
        return new MIndexStringPrecomputeProfile(mask | other.mask);
    }
}
