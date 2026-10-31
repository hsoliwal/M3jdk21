// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays.packed;

import java.lang.instrument.Instrumentation;
import java.util.Objects;

/** Supplies JVM-specific shallow object-size estimates to the runtime evidence probe. */
public final class MemoryAgent {
    private static volatile Instrumentation instrumentation;

    private MemoryAgent() {
    }

    /**
     * Receives instrumentation when this class is loaded as a Java agent.
     *
     * @param arguments optional agent arguments, which are unused
     * @param instance the instrumentation instance supplied by the JVM
     */
    public static void premain(String arguments, Instrumentation instance) {
        instrumentation = Objects.requireNonNull(instance, "instance");
    }

    /**
     * Returns the JVM's implementation-specific estimate for one object's shallow size.
     *
     * @param object the object to measure
     * @return estimated shallow bytes, including this JVM's applicable object overhead
     * @throws NullPointerException if object is null
     * @throws IllegalStateException if the agent was not loaded
     */
    public static long getObjectSize(Object object) {
        Objects.requireNonNull(object, "object");
        Instrumentation current = instrumentation;
        if (current == null) {
            throw new IllegalStateException("Load MemoryAgent with -javaagent before probing");
        }
        return current.getObjectSize(object);
    }
}
