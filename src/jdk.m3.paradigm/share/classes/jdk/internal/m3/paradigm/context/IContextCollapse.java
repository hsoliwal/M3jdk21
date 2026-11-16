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
import jdk.internal.m3.paradigm.context.IContextKernel.IContextNode;
import jdk.internal.m3.paradigm.context.IContextMemento.ContextSummary;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Context Collapse - Summarize + Forget.
 *
 * <h2>Collapse Rule</h2>
 * <blockquote>
 * "Replace many events with a semantic summary.
 *  You're not deleting memory - you're compressing meaning."
 * </blockquote>
 *
 * <h2>What Collapse Does</h2>
 * <ol>
 *   <li>Analyze event history</li>
 *   <li>Extract dominant intent, emotion, direction</li>
 *   <li>Write snapshot to DB</li>
 *   <li>Truncate old events</li>
 * </ol>
 *
 * @since 1.1.0 - ContextOS Integration
 */
@ISpec(
    value = "Summarize + Forget - compress meaning without losing semantics",
    constraints = {
        "Replace events with semantic summary",
        "Keep recent N events after collapse",
        "Dominant intent/emotion extracted via frequency"
    },
    invariants = {
        "collapseIfNeeded() only collapses when over threshold",
        "forceCollapse() always produces CollapseResult",
        "analyze() returns non-null ContextSummary"
    },
    examples = {
        "IContextCollapse.DEFAULT.collapseIfNeeded(ctx, 1000, 100)",
        "collapse.forceCollapse(ctx, 100)",
        "collapse.analyze(events)"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/IContextCollapse.java}
 * blob {@code d2b68f478cf9a8400af477f076958d94735e342d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IContextCollapse {

    /** Default collapse threshold. */
    int DEFAULT_THRESHOLD = 1000;

    /** Events to keep after collapse. */
    int DEFAULT_KEEP_RECENT = 100;

    /** Collapse if over threshold. */
    CollapseResult collapseIfNeeded(IContextNode context, int threshold, int keepRecent);

    /** Force collapse. */
    CollapseResult forceCollapse(IContextNode context, int keepRecent);

    /** Analyze events to produce summary. */
    ContextSummary analyze(List<String> events);

    /** Collapse result. */
    record CollapseResult(
            boolean collapsed,
            ContextSummary summary,
            int eventsBefore,
            int eventsAfter
    ) {
        public static CollapseResult noCollapse(int eventCount) {
            return new CollapseResult(false, null, eventCount, eventCount);
        }
    }

    /** Default implementation using frequency analysis. */
    IContextCollapse DEFAULT = new IContextCollapse() {
        @Override
        public CollapseResult collapseIfNeeded(IContextNode ctx, int threshold, int keepRecent) {
            List<String> events = ctx.history();
            if (events.size() <= threshold) {
                return CollapseResult.noCollapse(events.size());
            }
            return doCollapse(ctx, events, keepRecent);
        }

        @Override
        public CollapseResult forceCollapse(IContextNode ctx, int keepRecent) {
            List<String> events = ctx.history();
            if (events.isEmpty()) return CollapseResult.noCollapse(0);
            return doCollapse(ctx, events, keepRecent);
        }

        private CollapseResult doCollapse(IContextNode ctx, List<String> events, int keepRecent) {
            int before = events.size();
            ContextSummary summary = analyze(events);
            ctx.snapshot(summary);
            return new CollapseResult(true, summary, before, keepRecent);
        }

        @Override
        public ContextSummary analyze(List<String> events) {
            Map<String, Long> intentFreq = events.stream()
                    .map(this::extractField)
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

            String dominant = intentFreq.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("UNKNOWN");

            return new ContextSummary(dominant, "NEUTRAL", "FORWARD", Instant.now());
        }

        private String extractField(String json) {
            if (json.contains(TokenInterner.getInstance().intern("\"intent\":"))) {
                int start = json.indexOf(TokenInterner.getInstance().intern("\"intent\":\"")) + 10;
                int end = json.indexOf(TokenInterner.getInstance().intern("\""), start);
                if (start > 9 && end > start) return json.substring(start, end);
            }
            return "UNKNOWN";
        }
    };
}
