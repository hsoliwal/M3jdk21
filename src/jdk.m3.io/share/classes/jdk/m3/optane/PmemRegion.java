// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

/** Versioned low-level PMem mapping contract generated from recipes/pmem-api.tsv. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemRegion.java blob b7e090aec696; recipe make/m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
public interface PmemRegion extends AutoCloseable {
    /** Length in bytes, not limited to an int. */
    long length();
    /** Original byte offset of the mapping in the file. */
    long fileOffset();
    /** File used for this mapping. */
    java.nio.file.Path path();
    /** Access mode set when mapping was opened. */
    PmemAccess access();
    /** Actual backend; AUTO never appears in this result. */
    PmemBackend backend();
    /** Effective durability granularity, not atomicity. */
    PmemGranularity persistenceGranularity();
    /** True until successful close. */
    boolean isOpen();
    /** Read into an owned heap array; bounds checked before JNI. */
    void read(long offset, byte[] target, int arrayOffset, int count);
    /** Write from a heap array; disallowed on read-only mappings. */
    void write(long offset, byte[] source, int arrayOffset, int count);
    /** Complete durability synchronization of a range; not a transaction. */
    void persist(long offset, long count) throws java.io.IOException;
    /** Idempotent close; blocks until Java API operations finish. */
    @Override void close() throws java.io.IOException;
}
