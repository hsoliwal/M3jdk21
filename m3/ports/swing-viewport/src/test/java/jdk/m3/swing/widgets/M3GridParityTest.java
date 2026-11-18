/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import org.junit.jupiter.api.Test;

/** Headless native Grid gate for the existing Swing viewport reactor. */
class M3GridParityTest {
    @Test
    void frozenColumnsHeaderSelectionAndMillionRows() throws Exception {
        M3GridContractSmoke.main(new String[0]);
    }
    @Test
    void frozenTopBottomRowsAndMillionRowWindow() throws Exception {
        M3GridPinnedRowsSmoke.main(new String[0]);
    }
    @Test
    void mergedBodyCellSpans() throws Exception {
        M3GridSpansSmoke.main(new String[0]);
    }
    @Test
    void sparseCollapsibleRowGroups() throws Exception {
        M3GridGroupsSmoke.main(new String[0]);
    }
    @Test
    void sparseGroupMappingMatchesDenseOracle() throws Exception {
        M3GridGroupsMappingSmoke.main(new String[0]);
    }
    @Test
    void serializedSparseGroupProjectionRoundTrip() throws Exception {
        M3GridGroupsSerializationSmoke.main(new String[0]);
    }
    @Test
    void groupedColumnHeaderPixelsAndWideViewport() throws Exception {
        M3GridColumnGroupsSmoke.main(new String[0]);
    }
    @Test
    void groupedColumnHeaderSerialization() throws Exception {
        M3GridColumnGroupsSerializationSmoke.main(new String[0]);
    }

    @Test
    void summaryAndDetailColumnVisibility() throws Exception {
        M3GridColumnSummarySmoke.main(new String[0]);
    }
    @Test
    void summaryAndDetailSerialization() throws Exception {
        M3GridColumnSummarySerializationSmoke.main(new String[0]);
    }
    @Test
    void summaryAndDetailLargeVirtualGrid() throws Exception {
        M3GridColumnSummaryScale.main(new String[0]);
    }
}
