/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Focused Maven/JUnit wrappers around the deterministic Java 21 contract oracle. */
final class M3FilteredViewsTest {
    @Test void collectionAndSet() { M3FilteredViewsContract.setAndCollectionContracts(); }
    @Test void navigableSet() { M3FilteredViewsContract.navigableSetContracts(); }
    @Test void navigableMap() { M3FilteredViewsContract.navigableMapContracts(); }
    @Test void randomizedDivergence() { M3FilteredViewsContract.randomizedContracts(); }
}
