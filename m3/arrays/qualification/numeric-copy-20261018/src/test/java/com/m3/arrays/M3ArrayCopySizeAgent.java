/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Adapted from the canonical m3jdk21-size-views-20261017 SizeAgent: package/type only.
 */
package com.m3.arrays;

import java.lang.instrument.Instrumentation;

public final class M3ArrayCopySizeAgent {
    static Instrumentation instrumentation;

    private M3ArrayCopySizeAgent() { }

    public static void premain(String options, Instrumentation value) {
        instrumentation = value;
    }
}
