// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-algo/src/test/java/com/synexia/algorithms/agent/TopKByHammingHeapSortProofTest.java blob 3e9f4ae5fcb4; recipe m3/indexdb/recipe/hamming-topk-20261106/relocate.py v2
class HammingTopKContractTest {
    @Test void independentJava21HeapAndFullSortOracle() {
        HammingTopKContract.main(new String[0]);
    }
}
