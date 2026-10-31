// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/** Stable bit assignments for allocation-free lazy-operation event selection. */
public final class M3LazyEventBits {
    public static final long NONE = 0L;
    public static final long BEFORE_TOUCH = 1L << 0;
    public static final long AFTER_TOUCH = 1L << 1;
    public static final long CACHE_HIT = 1L << 2;
    public static final long CACHE_MISS = 1L << 3;
    public static final long BEFORE_LOAD = 1L << 4;
    public static final long AFTER_LOAD = 1L << 5;
    public static final long LOAD_FAILED = 1L << 6;
    public static final long CANCELLED = 1L << 7;
    public static final long STATE_CHANGED = 1L << 8;
    public static final long INVALIDATED = 1L << 9;
    public static final long RANGE_BEGIN = 1L << 10;
    public static final long RANGE_END = 1L << 11;
    public static final long PROVIDER_BEGIN = 1L << 12;
    public static final long PROVIDER_END = 1L << 13;
    public static final long PROGRESS = 1L << 14;
    public static final long ALL = (1L << 15) - 1L;

    private M3LazyEventBits() {
    }
}
