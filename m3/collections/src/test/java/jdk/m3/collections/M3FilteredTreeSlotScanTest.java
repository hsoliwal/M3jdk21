/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;
import org.junit.jupiter.api.Test;
/** Runs the independent Java-21 differential oracle in the receiving product. */
final class M3FilteredTreeSlotScanTest {
    @Test void exactPublicEntryOracle() {
        M3FilteredTreeSlotScanProof.main(new String[0]);
    }
}
