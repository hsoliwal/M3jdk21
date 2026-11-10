/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import jdk.internal.m3.collections.TokenInterner;
import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Session Consolidator - Consolidates multiple session transcripts.
 *
 * <p>Used to recover lost context by consolidating session histories
 * into a coherent index that can be searched and navigated.
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Lost context recovery after session timeout</li>
 *   <li>Building searchable session index</li>
 *   <li>Identifying recurring themes across sessions</li>
 *   <li>Tracking project progress over time</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ISessionConsolidator consolidator = SessionConsolidator.create();
 *
 * // Add sessions to consolidate
 * consolidator.addSession(sessionPath1);
 * consolidator.addSession(sessionPath2);
 *
 * // Build consolidated index
 * ConsolidatedIndex index = consolidator.consolidate();
 *
 * // Search for lost context
 * List<IndexEntry> results = index.search("authentication flow");
 * }</pre>
 *
 * @since Grand Java Patterns Proof - Skill Section
 */
@ISpec(
    value = "Session consolidator - recovers lost context by building searchable index",
    constraints = {
        "Sessions added incrementally before consolidation",
        "Index is persistent and loadable",
        "Search supports topic and text queries"
    },
    invariants = {
        "consolidate() produces non-null ConsolidatedIndex",
        "saveIndex() persists index to file",
        "loadIndex() restores from file"
    },
    examples = {
        "consolidator.addSession(path)",
        "consolidator.consolidate()",
        "index.search(\"auth\")"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.BETA
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/skill/ISessionConsolidator.java}
 * blob {@code 03af86f1df02d790586d65af9cab3485e4b747f2} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ISessionConsolidator {

    /**
     * Add a session transcript to be consolidated.
     *
     * @param transcriptPath path to transcript file
     */
    void addSession(Path transcriptPath);

    /**
     * Add multiple sessions.
     *
     * @param transcriptPaths list of transcript paths
     */
    void addSessions(List<Path> transcriptPaths);

    /**
     * Consolidate all added sessions into an index.
     *
     * @return consolidated index
     */
    ConsolidatedIndex consolidate();

    /**
     * Save consolidated index to file.
     *
     * @param outputPath output file path
     */
    void saveIndex(Path outputPath);

    /**
     * Load previously consolidated index.
     *
     * @param indexPath index file path
     * @return loaded index
     */
    ConsolidatedIndex loadIndex(Path indexPath);

    /** Consolidated session index. */
    interface ConsolidatedIndex {

        /**
         * Search for entries matching query.
         *
         * @param query search query
         * @return matching entries
         */
        List<IndexEntry> search(String query);

        /**
         * Get all entries in chronological order.
         *
         * @return all entries
         */
        List<IndexEntry> allEntries();

        /**
         * Get entries by topic.
         *
         * @param topic topic name
         * @return entries with that topic
         */
        List<IndexEntry> byTopic(String topic);

        /**
         * Get all unique topics.
         *
         * @return topic list with counts
         */
        Map<String, Integer> topics();

        /**
         * Get timeline of sessions.
         *
         * @return timeline entries
         */
        List<TimelineEntry> timeline();

        /**
         * Get summary statistics.
         *
         * @return statistics
         */
        IndexStats stats();
    }

    /**
     * Index entry representing a consolidated piece of information.
     *
     * @param id unique entry ID
     * @param sessionId originating session ID
     * @param timestamp when this occurred
     * @param topic extracted topic
     * @param content content summary
     * @param intent detected intent
     * @param emotion detected emotion
     * @param relevance relevance score (0-1)
      * @author Synexia
     */
    record IndexEntry(
            String id,
            String sessionId,
            Instant timestamp,
            String topic,
            String content,
            String intent,
            ISessionAnalyzer.Emotion emotion,
            double relevance
    ) {}

    /**
     * Timeline entry for session visualization.
     *
     * @param sessionId session identifier
     * @param startTime session start
     * @param endTime session end
     * @param summary session summary
     * @param heat session heat level
     * @param direction session direction
     */
    record TimelineEntry(
            String sessionId,
            Instant startTime,
            Instant endTime,
            String summary,
            ISessionAnalyzer.Heat heat,
            ISessionAnalyzer.Direction direction
    ) {}

    /**
     * Index statistics.
     *
     * @param totalSessions number of sessions
     * @param totalEntries number of index entries
     * @param uniqueTopics number of unique topics
     * @param dateRange date range covered
     * @param avgSessionLength average session length in minutes
     */
    record IndexStats(
            int totalSessions,
            int totalEntries,
            int uniqueTopics,
            String dateRange,
            double avgSessionLength
    ) {}
}
