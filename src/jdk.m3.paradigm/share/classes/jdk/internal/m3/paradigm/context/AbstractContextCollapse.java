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

import jdk.internal.m3.collections.PlaceholderLogger;

import jdk.internal.m3.collections.TokenInterner;
import jdk.internal.m3.paradigm.context.IContextKernel.IContextNode;
import jdk.internal.m3.paradigm.context.IContextMemento.ContextSummary;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AbstractContextCollapse - Base implementation for context collapse (Summarize + Forget).
 *
 * <p>Implements the Collapse Rule: "Replace many events with a semantic summary.
 * You're not deleting memory - you're compressing meaning."
 *
 * <p>What Collapse Does:
 * <ol>
 *   <li>Analyze event history</li>
 *   <li>Extract dominant intent, emotion, direction</li>
 *   <li>Write snapshot to DB</li>
 *   <li>Truncate old events</li>
 * </ol>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses can override:
 * <ul>
 *   <li>{@link #extractDominantIntent(List)} - Custom intent extraction</li>
 *   <li>{@link #extractDominantEmotion(List)} - Custom emotion extraction</li>
 *   <li>{@link #extractDirection(List)} - Custom direction extraction</li>
 *   <li>{@link #shouldCollapse(List, int)} - Custom collapse decision</li>
 * </ul>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/AbstractContextCollapse.java}
 * blob {@code a7e35ac9d143b87616ad04efe91b0152fc2553f1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractContextCollapse implements IContextCollapse {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractContextCollapse.class);


    /** Create a context collapse. */
    protected AbstractContextCollapse() {
    }

    @Override
    public CollapseResult collapseIfNeeded(IContextNode context, int threshold, int keepRecent) {
        List<String> events = context.history();
        if (!shouldCollapse(events, threshold)) {
            return CollapseResult.noCollapse(events.size());
        }
        return doCollapse(context, events, keepRecent);
    }

    @Override
    public CollapseResult forceCollapse(IContextNode context, int keepRecent) {
        List<String> events = context.history();
        if (events.isEmpty()) {
            return CollapseResult.noCollapse(0);
        }
        return doCollapse(context, events, keepRecent);
    }

    @Override
    public ContextSummary analyze(List<String> events) {
        String dominantIntent = extractDominantIntent(events);
        String dominantEmotion = extractDominantEmotion(events);
        String direction = extractDirection(events);
        return new ContextSummary(dominantIntent, dominantEmotion, direction, Instant.now());
    }

    /** Perform the actual collapse. */
    protected CollapseResult doCollapse(IContextNode context, List<String> events, int keepRecent) {
        int before = events.size();
        ContextSummary summary = analyze(events);
        context.snapshot(summary);
        return new CollapseResult(true, summary, before, keepRecent);
    }

    // --- Template Methods (Override for Custom Behavior) ---

    /**
     * Determine if collapse should occur.
     *
     * @param events the event history
     * @param threshold collapse threshold
     * @return true if should collapse
     */
    protected boolean shouldCollapse(List<String> events, int threshold) {
        return events.size() > threshold;
    }

    /**
     * Extract dominant intent from events.
     *
     * @param events the event history
     * @return dominant intent
     */
    protected String extractDominantIntent(List<String> events) {
        return extractDominantField(events, "intent");
    }

    /**
     * Extract dominant emotion from events.
     *
     * @param events the event history
     * @return dominant emotion
     */
    protected String extractDominantEmotion(List<String> events) {
        return extractDominantField(events, "emotion");
    }

    /**
     * Extract direction from events.
     *
     * @param events the event history
     * @return direction (default: FORWARD)
     */
    protected String extractDirection(List<String> events) {
        return "FORWARD";
    }

    /**
     * Extract dominant field value using frequency analysis.
     *
     * @param events the event history
     * @param fieldName the field to extract
     * @return most frequent value
     */
    protected String extractDominantField(List<String> events, String fieldName) {
        Map<String, Long> frequency = events.stream()
                .map(json -> extractJsonField(json, fieldName))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        return frequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
    }

    /**
     * Extract a field value from JSON string.
     *
     * @param json the JSON string
     * @param fieldName the field to extract
     * @return field value or "UNKNOWN"
     */
    protected String extractJsonField(String json, String fieldName) {
        String pattern = TokenInterner.getInstance().intern("\"") + fieldName + TokenInterner.getInstance().intern("\":\"");
        int start = json.indexOf(pattern);
        if (start < 0) {
            return "UNKNOWN";
        }
        start += pattern.length();
        int end = json.indexOf(TokenInterner.getInstance().intern("\""), start);
        if (end > start) {
            return json.substring(start, end);
        }
        return "UNKNOWN";
    }
}
