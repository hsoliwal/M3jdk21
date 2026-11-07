// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * JUnit endpoints of the relocated M3PagedArray/M3MemoryPressure self-test mains (residency fold, recipe
 * residency-paged-array-20261107). The optional JNI sampler (libm3_residency) is deferred, so the sampling self test
 * runs its library-absent mode "java"; its fixture, legacy and native modes need the deferred test-only JNI ABI.
 */
final class M3PagedArrayResidencyTest {
    @Test
    void pressurePolicyAdmissionCadenceAndPhysicalScaling() throws Exception {
        M3PressurePolicyTest.main(new String[0]);
    }

    @Test
    void pageContainerReuseRequiredByDefault() throws Exception {
        M3PagedArrayBackingReuseTest.main(new String[0]);
    }

    @Test
    void pageContainerBehaviourWithoutTheReuseRequirement() throws Exception {
        M3PagedArrayBackingReuseTest.main(new String[] {"behavior"});
    }

    @Test
    void samplingWithoutTheOptionalLibrary() throws Exception {
        assertFalse(M3MemoryPressure.nativeAvailable(), "libm3_residency is deferred: no library on this path");
        M3MemoryPressureSamplingTest.main(new String[] {"java"});
    }

    @Test
    void describeReportsTheRunningJvm() {
        M3MemoryPressureReportTest.main(new String[0]);
        M3MemoryPressure.Snapshot sample = M3MemoryPressure.current();
        assertEquals(Runtime.getRuntime().maxMemory(), sample.maxHeap());
        assertEquals(-1L, sample.physicalAvailable());
        assertEquals(-1L, sample.physicalTotal());
    }
}
