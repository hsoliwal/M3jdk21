/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Thin JUnit entry points for the independently frozen, standalone owner oracle. */
public final class StagedLaneBehaviorTest {
    @Test void intInt() { StagedLaneBehaviorContract.intInt(); }
    @Test void intLong() { StagedLaneBehaviorContract.intLong(); }
    @Test void identity() { StagedLaneBehaviorContract.identity(); }
    @Test void longHash() { StagedLaneBehaviorContract.longHash(); }
    @Test void sortedMap() throws Exception { StagedLaneBehaviorContract.sortedMap(); }
    @Test void patricia() { StagedLaneBehaviorContract.patricia(); }
    @Test void pagedSet() throws Exception { StagedLaneBehaviorContract.pagedSet(); }
}
