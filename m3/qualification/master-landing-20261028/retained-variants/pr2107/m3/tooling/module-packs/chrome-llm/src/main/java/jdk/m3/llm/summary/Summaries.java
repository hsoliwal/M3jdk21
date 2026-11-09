// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.summary;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ProgressMonitor;

/**
 * M3JDK entry points for the existing deterministic summary implementation.
 * Inputs retain the original record schema and outputs retain evidence IDs,
 * method scores, citations and semantic roots from {@link Summary}.
 */
public final class Summaries {
    private Summaries() { }

    /**
     * The five score families already retained in each summary node's
     * {@code method_scores} map. They are all computed by the existing fused
     * algorithm; this enum identifies their results without selecting a new mode.
     */
    public enum Method {
        LUHN("luhn"), SUMBASIC("sumbasic"), EDMUNDSON("edmundson"),
        LEXRANK("lexrank"), QUERY("query");

        private final String scoreKey;

        Method(String scoreKey) { this.scoreKey = scoreKey; }

        /** Returns the exact key in a summary node's existing score map. */
        public String scoreKey() { return scoreKey; }
    }

    /**
     * Immutable hierarchy budgets accepted by {@link Summary#hierarchical}.
     * A sentence remains an indivisible evidence atom, so a single sentence can
     * exceed {@code chunkCharacters}. The underlying 4096-sentence group guard
     * remains authoritative; these fields do not promise an overall input limit.
     *
     * @param chunkCharacters soft Unicode code-point budget per leaf chunk, at least 256
     * @param leafSentences maximum selected sentences per leaf, at least one
     * @param parentSentences maximum selected sentences per parent, at least one
     * @param fanIn maximum child nodes reduced per parent, at least two
     */
    public record Options(int chunkCharacters, int leafSentences,
                          int parentSentences, int fanIn) {
        private static final Options DEFAULTS = new Options(4096, 3, 5, 4);

        /** Validates the same numeric minima as the underlying implementation. */
        public Options {
            if (chunkCharacters < 256 || leafSentences < 1 || parentSentences < 1 || fanIn < 2) {
                throw new IllegalArgumentException("invalid summary budgets");
            }
        }

        /** Returns the immutable 4096-code-point, 3/5-sentence, four-child defaults. */
        public static Options defaults() { return DEFAULTS; }
    }

    /**
     * Computes the original hierarchical result using no local progress state.
     * Calling-thread interruption is still observed at operation boundaries.
     */
    public static Map<String, Object> hierarchical(List<Map<String, Object>> records,
                                                   String query, Options options) {
        return hierarchical(records, query, options, ProgressMonitor.NONE);
    }

    /**
     * Computes the original hierarchical result with a borrowed progress monitor.
     * The caller must keep the records and their maps stable throughout the call.
     * The returned graph uses the existing caller-owned summary schema.
     *
     * <p>A null monitor means {@link ProgressMonitor#NONE}. The method credits
     * one unit after top-level argument validation and one after successful
     * summary computation. It does not begin or complete a borrowed monitor.
     * Cancellation is checked before computation, after computation, and after
     * both progress callbacks. The underlying ranking algorithm is unchanged:
     * it is not interrupted between those boundaries, and cancellation latency
     * can therefore include one complete summary computation. Monitor callback
     * failures and summary exceptions propagate to the caller.
     *
     * @param records stable caller-owned records in the existing Summary schema
     * @param query query text used by the existing query and cue scores
     * @param options validated hierarchy budgets
     * @param monitor borrowed progress monitor, or null for NONE
     * @return the unmodified result of the existing hierarchical algorithm
     * @throws NullPointerException if records, query or options is null
     * @throws java.util.concurrent.CancellationException if boundary cancellation is observed
     */
    public static Map<String, Object> hierarchical(List<Map<String, Object>> records,
                                                   String query, Options options,
                                                   ProgressMonitor monitor) {
        ProgressMonitor progress = monitor == null ? ProgressMonitor.NONE : monitor;
        progress.checkCancelled();
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(options, "options");
        progress.worked(1);
        progress.checkCancelled();
        Map<String, Object> result = Summary.hierarchical(records, query,
                options.chunkCharacters(), options.leafSentences(),
                options.parentSentences(), options.fanIn());
        progress.checkCancelled();
        progress.worked(1);
        progress.checkCancelled();
        return result;
    }
}
