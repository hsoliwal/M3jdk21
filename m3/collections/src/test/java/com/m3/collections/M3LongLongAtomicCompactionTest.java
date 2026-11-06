/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit entrypoints for the independently executable Java21 comparison and fault oracles. */
final class M3LongLongAtomicCompactionTest {
    @Test void deterministicFailureCannotPartiallyDelete() {
        M3AtomicDeleteFailureOracle.main(new String[0]);
    }

    @Test void forcedTombstoneCompactionRetainsExactPhysicalState() throws Exception {
        M3LongMapCompactionSuccessOracle.main(new String[0]);
    }

    @Test void seededOperationsRetainExactIncumbentPhysicalState() throws Exception {
        M3LongLongSharedTrace.main(new String[0]);
    }
}
