// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 admission of the relocated class-store oracle M3ClassSpaceSelfTest (recipe class-space-20261107; M3JDK-authored
 * module template, test scope). The donor crate ran the same main from Maven verify through exec-maven-plugin; the
 * module runs it in the test phase, Java-only (the optional JNI lane runs in differential.sh).
 */
final class M3ClassSpaceJUnitTest {
    @Test
    void classStoreOracle() throws Exception {
        M3ClassSpaceSelfTest.main(new String[0]);
    }
}
