// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.lang.instrument.Instrumentation;

/** Test-only VM size measurement; the returned int[] has no referenced children. */
public final class HeapAgent {
    private static volatile Instrumentation instrumentation;
    private HeapAgent() { }
    public static void premain(String arguments, Instrumentation agent) { instrumentation = agent; }
    static long retainedPrimitiveArrayBytes(int[] array) {
        if (instrumentation == null) throw new IllegalStateException("qualification javaagent missing");
        return instrumentation.getObjectSize(array);
    }
}
