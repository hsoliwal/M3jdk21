/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/model/SourceKind.java (blob c9eab4f9c7c9) by M3_SEARCH_RELOCATE v4.
package jdk.m3.search;

/** Origin class for a canonical search-compiler document. */
public enum M3SourceKind {
    LIVE_WEB,
    WIKIPEDIA_DUMP,
    WARC,
    LOCAL_FILE,
    MEDIA,
    CODE,
    API
}
