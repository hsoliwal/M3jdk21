/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit entrypoints for the independently executable Java21 comparison and fault oracles. */
final class LongLongAtomicCompactionTest {
    @Test void deterministicFailureCannotPartiallyDelete() {
        AtomicDeleteFailureOracle.main(new String[0]);
    }

    @Test void forcedTombstoneCompactionRetainsExactPhysicalState() throws Exception {
        LongMapCompactionSuccessOracle.main(new String[0]);
    }

    @Test void seededOperationsRetainExactIncumbentPhysicalState() throws Exception {
        LongLongSharedTrace.main(new String[0]);
    }
}
