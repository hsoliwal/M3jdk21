package com.m3.affix;
final class M3StringFacts {
    final long bitSignal64;
    M3StringFacts(long b) { bitSignal64 = b; }
    static long codeUnitSignal(char u) {
        int folded = u >= 'A' && u <= 'Z' ? u | 0x20 : u;
        return (1L << (mix(folded) & 63)) | (1L << (mix(folded ^ 0x9e3779b9) & 63));
    }
    private static int mix(int v) {
        v ^= v >>> 16; v *= 0x7feb352d; v ^= v >>> 15; v *= 0x846ca68b;
        return v ^ (v >>> 16);
    }
}
