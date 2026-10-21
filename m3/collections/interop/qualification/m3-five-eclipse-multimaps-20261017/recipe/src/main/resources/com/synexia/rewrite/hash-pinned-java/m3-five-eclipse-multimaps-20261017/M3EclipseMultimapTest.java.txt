/* SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors. */
package com.m3.collections.interop;

import org.junit.jupiter.api.Test;

/** Actual Eclipse 11.1.0 provider contract oracle for both M3-backed multimaps. */
final class M3EclipseMultimapTest {
    @Test void listValues() { M3EclipseListMultimapSelfTest.main(new String[0]); }
    @Test void setValues() { M3EclipseSetMultimapSelfTest.main(new String[0]); }
}
