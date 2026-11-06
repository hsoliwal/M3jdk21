// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Opt-in raw-format compression atoms of M3JDK (package jdk.m3.compression): the LZ4 block-format encoder and the
 * Snappy raw-format encoder (no frames, no checksums, no codec routing) with their bounded decoders, their reusable
 * bounded byte sink, the four-byte match-candidate index (direct-mapped, with a package-private four-way variant), the
 * format-neutral LZ byte kernels and the format-neutral cache of deterministic compressed chunks. Relocated by recipe
 * from the first-party donor (recipe/compression-20261106). Java 21; java.base only.
 */
module org.m3jdk.compression {
    exports jdk.m3.compression;
}
