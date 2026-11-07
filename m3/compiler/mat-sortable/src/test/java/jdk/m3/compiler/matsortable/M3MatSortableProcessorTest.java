// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.matsortable;

import org.junit.jupiter.api.Test;

/**
 * JUnit 5 admission of the relocated donor contract M3SortableProcessorContract (real javac generation through the
 * relocated processor against the relocated annotations of m3-collections) and of M3MatSortableProcessorOracle over the
 * relocated processor (recipe mat-sortable-processor-20261107; M3JDK-authored, test scope). Work directories are created
 * under java.io.tmpdir.
 */
final class M3MatSortableProcessorTest {
    @Test
    void donorSortableProcessorContract() throws Exception {
        M3SortableProcessorContract.main(new String[0]);
    }

    @Test
    void processorOracleCorpus() throws Exception {
        M3MatSortableProcessorOracle.main(new String[] {M3MatSortableProcessor.class.getName()});
    }
}
