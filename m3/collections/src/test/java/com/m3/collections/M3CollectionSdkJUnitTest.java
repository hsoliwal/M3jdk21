/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entrance to the Java21 SDK differential oracle. */
final class M3CollectionSdkJUnitTest {
    @Test void contracts() { M3CollectionSdkSelfTest.main(new String[0]); }
}
