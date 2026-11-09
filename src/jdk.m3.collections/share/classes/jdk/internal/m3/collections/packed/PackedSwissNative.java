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
package jdk.internal.m3.collections.packed;

/**
 * Explicit batch JNI peer for Abseil-derived portable Swiss control-byte classification.
 *
 * <p>The host owns native-library loading. This class is package-private so no public collection
 * API depends on JNI. Ordinary single-key table operations stay on the Java hot path; JNI exists
 * for bulk/native callers and parity testing where crossing cost can be amortized.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedSwissNative.java}
 * blob {@code 17518b76ef666cf7d10611435360beea093d53e7} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class PackedSwissNative {
    static final int MASKS_PER_GROUP = 4;

    private PackedSwissNative() { }

    /**
     * Writes four 64-bit lane-MSB masks per group: match, empty, empty-or-deleted, full.
     */
    static void classifyGroups(
            byte[] controls,
            int fromGroup,
            int groupCount,
            byte fingerprint,
            long[] output,
            int outputFrom) {
        classifyGroups0(controls, fromGroup, groupCount, fingerprint, output, outputFrom);
    }

    private static native void classifyGroups0(
            byte[] controls,
            int fromGroup,
            int groupCount,
            byte fingerprint,
            long[] output,
            int outputFrom);
}
