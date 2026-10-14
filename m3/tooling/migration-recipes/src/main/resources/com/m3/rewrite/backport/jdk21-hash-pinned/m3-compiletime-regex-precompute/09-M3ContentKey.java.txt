// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

/**
 * Spelling-free content key of a (pattern, flags) pair: UTF-16 length, flags and two
 * independent 64-bit hashes over the UTF-16 units. Class or call-site identity never enters the
 * key, so the same constant regex in any class or jar resolves to one image entry.
 *
 * <p>{@code h1} is FNV-1a 64 over UTF-16 units; {@code h2} is a multiply-xorshift polynomial
 * finished with the MurmurHash3 {@code fmix64} avalanche. The harvester proves there is no
 * collision of either hash inside an image; a runtime lookup requires length, flags and both
 * hashes to agree.</p>
 */
public record M3ContentKey(long h1, long h2, int length, int flags) {
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final long GOLDEN = 0x9e3779b97f4a7c15L;

    /** Key of {@code pattern} compiled with {@code flags}. */
    public static M3ContentKey of(CharSequence pattern, int flags) {
        long a = FNV_OFFSET ^ flags;
        long b = GOLDEN * (flags + 1L);
        int n = pattern.length();
        for (int i = 0; i < n; i++) {
            char c = pattern.charAt(i);
            a = (a ^ c) * FNV_PRIME;
            b = (b + c) * GOLDEN;
            b ^= b >>> 29;
        }
        return new M3ContentKey(a ^ n, fmix64(b ^ ((long) n << 32 | flags)), n, flags);
    }

    static long fmix64(long value) {
        long k = value;
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }
}
