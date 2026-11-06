/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/index/QueryPreparationStats.java (blob 48e4c83b7df6) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

/** Snapshot of cache lookups and retained entries, never query text or results. */
public record M3QueryPreparationStats(long hits, long misses, long evictions, int entries) {
}
