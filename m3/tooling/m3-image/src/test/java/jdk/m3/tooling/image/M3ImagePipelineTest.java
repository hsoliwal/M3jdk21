/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Maven/JUnit binding for the dependency-free executable suite. */
class M3ImagePipelineTest {
    @Test
    void completePipeline() throws Exception {
        assertTrue(M3ImageSelfTest.runAll() >= 20);
    }
}
