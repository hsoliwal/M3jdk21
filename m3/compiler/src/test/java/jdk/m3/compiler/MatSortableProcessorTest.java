// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 admission of the relocated donor contract SortableProcessorContract (real javac generation through the
 * relocated processor against the relocated annotations of m3-collections) and of MatSortableProcessorOracle over the
 * relocated processor (recipe mat-sortable-processor-20261107; M3JDK-authored, test scope). Work directories are created
 * under java.io.tmpdir.
 */
final class MatSortableProcessorTest {
    @Test
    void donorSortableProcessorContract() throws Exception {
        SortableProcessorContract.main(new String[0]);
    }

    @Test
    void processorOracleCorpus() throws Exception {
        MatSortableProcessorOracle.main(new String[] {MatSortableProcessor.class.getName()});
    }
}
