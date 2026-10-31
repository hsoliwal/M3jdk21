package com.synexia.mindex.experiment;

/** Java String polynomial-hash composition; overflow is the specified int arithmetic. */
public final class StringHashes {
    private StringHashes() { }
    public static int append(int leftHash, int rightHash, int rightUtf16Length) {
        if (rightUtf16Length < 0) { throw new IllegalArgumentException("negative length"); }
        int power = 1;
        int factor = 31;
        for (int n = rightUtf16Length; n != 0; n >>>= 1) {
            if ((n & 1) != 0) { power *= factor; }
            factor *= factor;
        }
        return leftHash * power + rightHash;
    }
}
