// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import org.junit.jupiter.api.Test;

/** Recipe-generated; do not hand-edit. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/junit/java/test/PmemAtomCorrectnessJUnitTest.java blob 5ca643f45d85; recipe m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
public final class PmemAtomCorrectnessJUnitTest {
    @Test
    void test_PmemSdk_capabilities() throws Exception { PmemAtomCases.verify("capabilities"); }

    @Test
    void test_PmemSdk_open() throws Exception { PmemAtomCases.verify("open"); }

    @Test
    void test_PmemRegion_length() throws Exception { PmemAtomCases.verify("length"); }

    @Test
    void test_PmemRegion_fileOffset() throws Exception { PmemAtomCases.verify("fileOffset"); }

    @Test
    void test_PmemRegion_path() throws Exception { PmemAtomCases.verify("path"); }

    @Test
    void test_PmemRegion_access() throws Exception { PmemAtomCases.verify("access"); }

    @Test
    void test_PmemRegion_backend() throws Exception { PmemAtomCases.verify("backend"); }

    @Test
    void test_PmemRegion_persistenceGranularity() throws Exception { PmemAtomCases.verify("persistenceGranularity"); }

    @Test
    void test_PmemRegion_isOpen() throws Exception { PmemAtomCases.verify("isOpen"); }

    @Test
    void test_PmemRegion_read() throws Exception { PmemAtomCases.verify("read"); }

    @Test
    void test_PmemRegion_write() throws Exception { PmemAtomCases.verify("write"); }

    @Test
    void test_PmemRegion_persist() throws Exception { PmemAtomCases.verify("persist"); }

    @Test
    void test_PmemRegion_close() throws Exception { PmemAtomCases.verify("close"); }

    @Test
    void test_PmemTransfer_copy() throws Exception { PmemAtomCases.verify("copy"); }

    @Test
    void test_PmemTransfer_equalRange() throws Exception { PmemAtomCases.verify("equalRange"); }

    @Test
    void test_PmemTransfer_copyIfDifferent() throws Exception { PmemAtomCases.verify("copyIfDifferent"); }

    @Test
    void test_PmemSearch_indexOf() throws Exception { PmemAtomCases.verify("indexOf"); }

    @Test
    void test_PmemSearch_lowerBoundLongLE() throws Exception { PmemAtomCases.verify("lowerBoundLongLE"); }

    @Test
    void test_PmemSearch_upperBoundLongLE() throws Exception { PmemAtomCases.verify("upperBoundLongLE"); }

}
