/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 * An M3-owned Eclipse Collections API implementation, not a renamed vendor class.
 */
package com.m3.collections.interop;

import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;

/**
 * Correctly named Eclipse Collections factories; never shadows org.eclipse.*
 * types. Every returned collection owns an independent M3-backed index store.
 */
public final class M3EclipseMultimaps {
    private M3EclipseMultimaps() { }

    public static <K, V> MutableListMultimap<K, V> list() {
        return new M3EclipseListMultimap<>();
    }

    public static <K, V> MutableSetMultimap<K, V> set() {
        return new M3EclipseSetMultimap<>();
    }

    public static <K, V> MutableBagMultimap<K, V> bag() {
        return new M3EclipseBagMultimap<>();
    }
}
