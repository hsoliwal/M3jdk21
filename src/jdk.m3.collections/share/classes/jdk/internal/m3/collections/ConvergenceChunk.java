/*******************************************************************************
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
package jdk.internal.m3.collections;

import java.util.Set;

/**
 * Convergence Chunk - text chunk with hash, signals, and score.
 *
 * <p>
 * Chunks are the unit of memory convergence. Each chunk represents a segment of
 * text that has been processed and scored for retention.
 * </p>
 *
 * <h2>Properties</h2>
 * <ul>
 * <li><b>hash</b> - Structural hash (FNV-1a) for identity</li>
 * <li><b>text</b> - The actual text content</li>
 * <li><b>signals</b> - Extracted structural signals (keywords, patterns)</li>
 * <li><b>score</b> - Retention score (decays over passes)</li>
 * </ul>
 *
 * <h2>Invariants</h2>
 * <ul>
 * <li>Chunks are immutable</li>
 * <li>Hash is deterministic (same text → same hash)</li>
 * <li>Score is in range [0.0, 1.0+]</li>
 * <li>Signals are derived from text (CPU extraction)</li>
 * </ul>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @param hash
 *            structural hash (FNV-1a)
 * @param text
 *            the text content
 * @param signals
 *            extracted structural signals
 * @param score
 *            retention score
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/ConvergenceChunk.java}
 * blob {@code 19413f3a41966c92af748f7574d303a6572656d2} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record ConvergenceChunk(long hash, String text, Set<String> signals, double score) {

    // --- INTERNED STRING CONSTANTS ---
    private static final TokenInterner INTERNER = TokenInterner.getInstance();

    private static final String FORMAT_COMPACT = INTERNER.intern("⚡ %016x [%.2f] %s");
    private static final String ELLIPSIS = INTERNER.intern("...");

    /** Maximum text length for compact display. */
    private static final int MAX_TEXT_LENGTH = 40;

    /**
     * Creates a compact single-line representation.
     *
     * <p>
     * Format: "⚡ {hash16} [{score}] {text40}"
     * </p>
     *
     * @return compact representation
     */
    public String toCompactLine() {
        String truncated = text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) + ELLIPSIS : text;
        return String.format(java.util.Locale.ROOT, FORMAT_COMPACT, hash, score, truncated);
    }

    /**
     * Creates a chunk with adjusted score.
     *
     * @param newScore
     *            the new score
     * @return new chunk with updated score
     */
    public ConvergenceChunk withScore(double newScore) {
        return new ConvergenceChunk(hash, text, signals, newScore);
    }

    /**
     * Checks if this chunk should be retained (score above threshold).
     *
     * @param threshold
     *            minimum score for retention
     * @return true if score >= threshold
     */
    public boolean shouldRetain(double threshold) {
        return score >= threshold;
    }

    /**
     * Creates a decayed version of this chunk.
     *
     * @param decayFactor
     *            multiplicative decay (0.0 to 1.0)
     * @return new chunk with decayed score
     */
    public ConvergenceChunk decay(double decayFactor) {
        return new ConvergenceChunk(hash, text, signals, score * decayFactor);
    }
}
