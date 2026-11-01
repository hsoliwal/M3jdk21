/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.compiler.profiles;

/** Portable observation kinds admitted by the M3 execution-profile artifact contract. */
public enum M3ExecutionProfileKind {
    /** One canonical caller-to-callee edge was observed. */
    CALL_EDGE,
    /** One canonical JVM class initialization was observed. */
    CLASS_INITIALIZE,
    /** One canonical JVM class load was observed. */
    CLASS_LOAD,
    /** One canonical JVM method compilation was observed. */
    COMPILED_METHOD,
    /** One canonical interface participated in dynamic proxy creation. */
    DYNAMIC_PROXY_INTERFACE,
    /** One canonical native library identity was loaded. */
    NATIVE_LIBRARY_LOAD,
    /** One canonical member was accessed reflectively. */
    REFLECTIVE_MEMBER,
    /** One canonical resource identity was accessed. */
    RESOURCE_ACCESS,
    /** One canonical service-provider identity was observed. */
    SERVICE_PROVIDER,
    /** One producer-defined startup boundary was observed. */
    STARTUP_MARKER
}
