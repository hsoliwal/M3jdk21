/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/reduce/DocumentDeduplicatorConcurrencyTest.java (blob 9c3404847fe6) by M3_SEARCH_RELOCATE v4.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DocumentDeduplicatorConcurrencyTest {
    @Test
    void concurrentlyArrivingNearDuplicatesAdmitOnlyOne() throws Exception {
        M3DocumentDeduplicator dedupe = new M3DocumentDeduplicator(3);
        M3KnowledgeDocument left = document("urn:left",
                "Java search indexing keeps immutable evidence fast and searchable.");
        M3KnowledgeDocument right = document("urn:right",
                "Java search indexing keeps immutable evidence fast and searchable!");

        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> { classify(dedupe, left, start, accepted); return null; });
            var second = executor.submit(() -> { classify(dedupe, right, start, accepted); return null; });
            start.countDown();
            first.get();
            second.get();
        }

        assertEquals(1, accepted.get());
        assertEquals(1, dedupe.exactCount());
        assertEquals(4L, dedupe.bandValueCount());
    }

    private static void classify(
            M3DocumentDeduplicator dedupe,
            M3KnowledgeDocument document,
            CountDownLatch start,
            AtomicInteger accepted) throws Exception {
        start.await();
        if (dedupe.classify(document) == M3DocumentDeduplicator.Decision.ACCEPT) {
            accepted.incrementAndGet();
        }
    }

    private static M3KnowledgeDocument document(String uri, String text) {
        return M3KnowledgeDocument.create(
                URI.create(uri),
                "test",
                "text/plain",
                M3SourceKind.LOCAL_FILE,
                "test",
                Instant.EPOCH,
                null,
                text,
                Map.of());
    }
}
