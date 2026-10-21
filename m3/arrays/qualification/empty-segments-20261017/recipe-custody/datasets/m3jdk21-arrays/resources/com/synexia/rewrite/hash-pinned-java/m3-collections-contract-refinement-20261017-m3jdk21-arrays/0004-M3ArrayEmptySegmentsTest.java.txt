/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.arrays;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M3ArrayEmptySegmentsTest {
    @Test
    void preservesJoinedCoordinatesEmptyDescriptorsAndNativeProjection() {
        String library = System.getProperty("m3.arrays.native.path");
        if (library != null && !library.isBlank()) M3ArrayNative.load(Path.of(library));
        M3ArrayEmptySegmentsSelfTest.runContract();
    }
}
