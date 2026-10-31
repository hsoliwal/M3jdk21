// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/** Cross-owner reason for lazy work without coupling to a UI, tree, cache or storage framework. */
public enum M3LazyOperation {
    READ,
    TOUCH,
    LOAD,
    PRELOAD,
    RENDER,
    SELECT,
    EXPAND,
    EDIT,
    NAVIGATE,
    PREFETCH,
    API_OBJECT,
    PAGE,
    CHILD_PROBE,
    ROOT_COUNT,
    ROOT_KEY,
    CHILD_COUNT,
    CHILD_KEY,
    INVALIDATE
}
