// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
/**
 * Optional, owner-neutral Memory Mode planning and mapped-read hints, and the fail-closed file-backed persistent-memory
 * region family (M3PmemSdk, M3PmemRegion, M3PmemTransfer, M3PmemSearch; FILE backend in Java, POSIX and optional PMDK
 * backends through the JNI library m3pmemsdk). No PMDK source or library is part of the module.
 */
module org.m3jdk.optane {
    exports jdk.m3.optane;
}
