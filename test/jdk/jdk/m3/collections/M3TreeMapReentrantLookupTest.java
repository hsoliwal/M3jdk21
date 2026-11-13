/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the re-entrant lookup (slot-reuse ABA) contract of M3TreeMap and M3TreeSet under the Maven suite. */
//FLLDCIM: MRL - recipe TREEMAP_ABA_GUARD v1, make/m3/collections/qualification/treemap-aba-guard-20261107 (review of #2641)
final class M3TreeMapReentrantLookupTest {
    @Test void comparatorMutationDuringLookupFailsFastOrLinearizes() {
        M3TreeMapReentrantLookupSelfTest.main(new String[0]);
    }
}
