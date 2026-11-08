/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Focused Maven/JUnit wrappers around the deterministic Java 21 contract oracle. */
final class FilteredViewsTest {
    @Test void collectionAndSet() { FilteredViewsContract.setAndCollectionContracts(); }
    @Test void navigableSet() { FilteredViewsContract.navigableSetContracts(); }
    @Test void navigableMap() { FilteredViewsContract.navigableMapContracts(); }
    @Test void randomizedDivergence() { FilteredViewsContract.randomizedContracts(); }
}
