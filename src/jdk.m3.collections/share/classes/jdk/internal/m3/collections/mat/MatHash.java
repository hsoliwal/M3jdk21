/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.mat;

import java.nio.ByteBuffer;

/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatHash.java}
 * blob {@code ed5e061f3e91c48146473939b42acdc026902517} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class MatHash {
    private MatHash() { }

    static long fnv64(byte[] bytes) {
        long hash = 0xcbf29ce484222325L;
        for (byte value : bytes) {
            hash ^= Byte.toUnsignedLong(value);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    static long fnv64(byte[] bytes, int offset, int length) {
        if (offset < 0 || length < 0 || (long) offset + length > bytes.length) {
            throw new IndexOutOfBoundsException("invalid hash range");
        }
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < length; index++) {
            hash ^= Byte.toUnsignedLong(bytes[offset + index]);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    static long fnv64(ByteBuffer bytes, int offset, int length) {
        if (offset < 0 || length < 0 || (long) offset + length > bytes.limit()) {
            throw new IndexOutOfBoundsException("invalid hash range");
        }
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < length; index++) {
            hash ^= Byte.toUnsignedLong(bytes.get(offset + index));
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    static int fold32(long hash) {
        long mixed = hash ^ (hash >>> 33);
        mixed *= 0xff51afd7ed558ccdl;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53l;
        mixed ^= mixed >>> 33;
        return (int) (mixed ^ (mixed >>> 32));
    }

    static int mix(int value) {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        return value ^ (value >>> 16);
    }
}
