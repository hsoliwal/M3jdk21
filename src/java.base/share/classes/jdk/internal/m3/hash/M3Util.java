/*
 * Changed by Synexia (2026-10-31): zero-allocation-hashing 0.16 (Apache License 2.0) absorbed into java.base
 * as jdk.internal.m3.hash; the changes are listed in src/java.base/share/legal/zero-allocation-hashing.md.
 */
package jdk.internal.m3.hash;

import java.nio.ByteBuffer;


import static java.nio.ByteOrder.*;

final class M3Util {

    static void checkArrayOffs(final int arrayLength, final int off, final int len) {
        if (len < 0 || off < 0 || off + len > arrayLength || off + len < 0)
            throw new IndexOutOfBoundsException();
    }
}
