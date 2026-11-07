/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.M3ObjectSignalReferenceModel;
import jdk.internal.m3.collections.M3ObjectSignalSelfTest;
/** Owning-module JUnit test of the relocated object first-pass signal family (recipe object-signal-20261107). */
final class M3ObjectSignalTest {
    @Test void donorOracleStringEqualsContainsMasksAndBulkParity() throws Exception {
        M3ObjectSignalSelfTest.main(new String[0]);
    }

    @Test void naivePerRowReferenceModel() throws Exception {
        M3ObjectSignalReferenceModel.main(new String[0]);
    }
}
