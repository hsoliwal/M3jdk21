// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 admission of StaticUsePlanDump (recipe static-use-plan-20261107; M3JDK-authored, test scope): the whole
 * fact space and the loop-bound shapes against the reference model in the test phase; the differential gate runs the
 * same driver against the donor build.
 */
final class StaticUsePlanDumpTest {
    @Test
    void factSpaceAndLoopBoundsAgreeWithTheReferenceModel() throws Exception {
        StaticUsePlanDump.main(new String[0]);
    }
}
