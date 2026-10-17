// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/* Local patch-module qualification only. The target JDK binds these symbols in libjava. */
package jdk.internal.mindex;
public final class M3RegexPreparedBatchTestSupport {
    private M3RegexPreparedBatchTestSupport() { }
    public static void load(String absolutePath) { System.load(absolutePath); }
    public static native void failAllocationAfter(long successfulAllocations);
    public static native long liveAllocations();
}
