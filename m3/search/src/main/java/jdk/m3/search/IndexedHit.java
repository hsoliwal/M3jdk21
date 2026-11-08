/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/index/IndexedHit.java (blob 3666c4001c97) by M3_SEARCH_RELOCATE v4.
package jdk.m3.search;

import java.net.URI;
import java.time.Instant;

public record IndexedHit(
        String documentId,
        String contentHash,
        URI sourceUri,
        String title,
        SourceKind sourceKind,
        String providerId,
        Instant retrievedAt,
        Instant publishedAt,
        float score) {
}
