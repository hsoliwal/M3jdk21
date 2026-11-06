// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Scoped leases for M3 logical handles: explicit lease, pin, pool and JNI-borrow lanes over primitive arrays, with
 * an optional JNI scan/concat lane (library m3_scoped_leases). The package is jdk.m3.jni because the area name
 * native is a Java keyword.
 */
module org.m3jdk.jni {
  // jdk.m3.util.M3RuntimeHeap (m3/ports/collections); its artifact m3-collection-port-wave1 has no module
  // descriptor, so this is its automatic module name until the port declares one
  requires m3.collection.port.wave1;
  exports jdk.m3.jni;
}
