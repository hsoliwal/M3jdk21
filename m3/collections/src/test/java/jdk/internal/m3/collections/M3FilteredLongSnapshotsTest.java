/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import org.junit.jupiter.api.Test;

/** JNI is an explicit opt-in lane, never silently claimed by this JUnit test. */
final class M3FilteredLongSnapshotsTest {
    @Test void pureJavaOracle() {
        M3FilteredLongSnapshotsContract.run(M3FilteredLongSnapshots.Backend.JAVA);
    }
    @Test void nativeWhenExplicitlySupplied() {
        String library = System.getProperty("m3.filtered.native.absolutePath");
        if (library != null && !library.isBlank()) {
            System.load(library);
            M3FilteredLongSnapshotsContract.run(M3FilteredLongSnapshots.Backend.JNI_REQUIRED);
        }
    }
}
