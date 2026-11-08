/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

final class IndexedTableReentrantSortTest {
    @Test void comparatorCannotPublishStalePhysicalIndexes() {
        IndexedTableReentrantSortContract.main(new String[0]);
    }
}
