/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.apache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Runs the seeded L075 differential: Commons Flat3Map 4.5.0 against the facade and against the core. */
final class M3Flat3MapCommonsDifferentialTest {
    @Test
    void facadeMatchesCommons() throws IOException {
        M3Flat3MapDifferential.Result result = M3Flat3MapDifferential.run("full", M3Flat3MapDifferential.OPS, null);
        System.out.println(result);
        assertEquals(M3Flat3MapDifferential.OPS, result.lines());
        assertTrue(result.identical(), result::toString);
    }

    @Test
    void coreMatchesCommonsOnSharedSubset() throws IOException {
        M3Flat3MapDifferential.Result result = M3Flat3MapDifferential.run("subset", M3Flat3MapDifferential.OPS, null);
        System.out.println(result);
        assertTrue(result.lines() > 0);
        assertTrue(result.identical(), result::toString);
    }
}
