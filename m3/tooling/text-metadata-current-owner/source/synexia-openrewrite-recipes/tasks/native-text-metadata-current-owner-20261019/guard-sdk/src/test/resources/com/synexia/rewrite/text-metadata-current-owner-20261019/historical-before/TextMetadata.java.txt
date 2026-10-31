// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/** Exact composable Java String metadata algebra for frozen lexical coordinates. */
public final class TextMetadata {
    private TextMetadata() {}

    public static int pow31(int utf16Length) {
        if (utf16Length < 0) throw new IllegalArgumentException("utf16Length");
        int result = 1;
        int base = 31;
        int exponent = utf16Length;
        while (exponent != 0) {
            if ((exponent & 1) != 0) result *= base;
            base *= base;
            exponent >>>= 1;
        }
        return result;
    }

    public static int concatHash(int leftHash, int rightHash, int rightUtf16Length) {
        return leftHash * pow31(rightUtf16Length) + rightHash;
    }
}
