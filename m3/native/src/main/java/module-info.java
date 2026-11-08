// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Scoped leases for M3 logical handles: explicit lease, pin, pool and JNI-borrow lanes over primitive arrays, with
 * an optional JNI scan/concat lane (library m3_scoped_leases). The package is jdk.m3.jni because the area name
 * native is a Java keyword.
 */
module org.m3jdk.jni {
  // jdk.m3.collections.RuntimeHeap: one home for the former port owners since 2026-11-06
  // (CONSOLIDATE_PORT_OWNERS v1); m3-collections declares the module jdk.m3.collections
  requires jdk.m3.collections;
  exports jdk.m3.jni;
}
