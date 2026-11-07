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
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

/**
 * Explicit JNI peer for {@link M3PackedLongMaskPage}; the caller loads the library.
 * Never auto-dispatches native code, pins callbacks or promises zero-copy arrays.
 * Exact exception types and output semantics are checked against the Java oracle.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongMaskPageNative.java}
 * blob {@code 80bf3da5996ba2d861fc08d53f21a892443dad09} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongMaskPageNative {
    private M3PackedLongMaskPageNative() { }

    public static int copyPage(long[] source, int from, int to, long[] decisions,
            int skip, int limit, long[] output, int outputFrom) {
        return copyPage0(source, from, to, decisions, skip, limit, output, outputFrom);
    }

    private static native int copyPage0(long[] source, int from, int to, long[] decisions,
            int skip, int limit, long[] output, int outputFrom);
}
