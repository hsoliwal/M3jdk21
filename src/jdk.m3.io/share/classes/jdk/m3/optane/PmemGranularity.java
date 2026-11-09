// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

/**
 * Effective persistence category; this is not a guarantee of atomicity.
 * FILE reports PAGE as a coarse file-I/O category: force(true) requests file
 * persistence but does not measure physical PMem store granularity or page size.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemGranularity.java blob c1c5b345d460; recipe m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
public enum PmemGranularity { BYTE, CACHE_LINE, PAGE }
