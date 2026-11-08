/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.ObjectSignalReferenceModel;
import jdk.internal.m3.collections.ObjectSignalSelfTest;
/** Owning-module JUnit test of the relocated object first-pass signal family (recipe object-signal-20261107). */
final class M3ObjectSignalTest {
    @Test void donorOracleStringEqualsContainsMasksAndBulkParity() throws Exception {
        ObjectSignalSelfTest.main(new String[0]);
    }

    @Test void naivePerRowReferenceModel() throws Exception {
        ObjectSignalReferenceModel.main(new String[0]);
    }
}
