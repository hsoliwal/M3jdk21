/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Thin JUnit entry points for the independently frozen, standalone owner oracle. */
public final class M3StagedLaneBehaviorTest {
    @Test void intInt() { M3StagedLaneBehaviorContract.intInt(); }
    @Test void intLong() { M3StagedLaneBehaviorContract.intLong(); }
    @Test void identity() { M3StagedLaneBehaviorContract.identity(); }
    @Test void longHash() { M3StagedLaneBehaviorContract.longHash(); }
    @Test void sortedMap() throws Exception { M3StagedLaneBehaviorContract.sortedMap(); }
    @Test void patricia() { M3StagedLaneBehaviorContract.patricia(); }
    @Test void pagedSet() throws Exception { M3StagedLaneBehaviorContract.pagedSet(); }
}
