// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

/** Host API support, not proof that Optane/PMem or DAX hardware is installed. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemCapabilities.java blob 15b8edf63490; recipe make/m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
public record PmemCapabilities(boolean nativeAvailable, boolean libpmem2Compiled,
                               int osPageSize, String platform) {}
