/*******************************************************************************
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
package jdk.m3.collections;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Optional JNI bulk primitive top-K for <em>certified exact signed-long keys only</em>.
 * Unlike {@link M3MarkerTopKIndex}, this API cannot invoke a Java exact-tie comparator
 * and MUST NOT be used with unverified/approximate @Sortable ranking signals.
 * Caller owns direct buffers in native byte order; IDs and keys must remain unchanged
 * until the native call completes. The library is loaded only on explicit demand.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/M3MarkerTopKNative.java}
 * blob {@code 1b9fa79838d33857c70477a8d82c375140a26163} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3MarkerTopKNative {
    private M3MarkerTopKNative() { }

    /** Direct input buffers contain int IDs and long keys indexed by those IDs. */
    public static int[] smallestCertifiedNativeRequired(
            ByteBuffer idBytes, ByteBuffer keyByIdBytes, int count, int k) {
        requireDirect(idBytes, "idBytes");
        requireDirect(keyByIdBytes, "keyByIdBytes");
        if (count < 0 || k < 0 || k > count
                || (long) count * Integer.BYTES > idBytes.capacity()
                || keyByIdBytes.capacity() % Long.BYTES != 0) {
            throw new IllegalArgumentException("invalid direct-buffer geometry");
        }
        if (k == 0) return new int[0];
        System.loadLibrary("m3_marker_topk");
        return nativeSmallest(idBytes, keyByIdBytes, count, k);
    }

    private static void requireDirect(ByteBuffer buffer, String name) {
        Objects.requireNonNull(buffer, name);
        if (!buffer.isDirect() || buffer.order() != ByteOrder.nativeOrder()) {
            throw new IllegalArgumentException(name + " must be a native-order direct buffer");
        }
    }

    private static native int[] nativeSmallest(ByteBuffer idBytes, ByteBuffer keys,
            int count, int k);
}
