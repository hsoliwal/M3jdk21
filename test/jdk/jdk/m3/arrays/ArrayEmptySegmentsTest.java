/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.arrays;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ArrayEmptySegmentsTest {
    @Test
    void preservesJoinedCoordinatesEmptyDescriptorsAndNativeProjection() {
        String library = System.getProperty("m3.arrays.native.path");
        if (library != null && !library.isBlank()) ArrayNative.load(Path.of(library));
        ArrayEmptySegmentsSelfTest.runContract();
    }
}
