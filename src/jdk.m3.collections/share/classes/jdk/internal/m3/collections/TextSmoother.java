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

import java.util.Locale;

/**
 * TextSmoother - NLP smoothing hook for text preprocessing.
 *
 * <p>
 * From ChatGPT Discrete Multi-Pass Convergence: "NLP never writes memory. NLP
 * never bypasses hashing. NLP only nudges scoring and consolidation."
 * </p>
 *
 * <h2>SPECIFICATION</h2>
 *
 * <h3>Purpose</h3>
 * <ul>
 * <li>Lemma normalization</li>
 * <li>Phrase collapsing</li>
 * <li>Noise removal</li>
 * <li>Token stabilization</li>
 * </ul>
 *
 * <h3>Critical Rules</h3>
 * <ul>
 * <li>NLP never writes memory</li>
 * <li>NLP never bypasses hashing</li>
 * <li>NLP only nudges scoring and consolidation</li>
 * <li>Must be PURE (same input = same output)</li>
 * </ul>
 *
 * <h3>Implementations</h3>
 * <ul>
 * <li>{@link #identity()} - No-op (default)</li>
 * <li>{@link #normalize()} - Basic normalization</li>
 * <li>{@link #removePunctuation()} - Clean punctuation</li>
 * <li>OpenNLP-backed - Sentence/token normalization (external)</li>
 * <li>Ollama-backed - LLM-based rewriting (external)</li>
 * </ul>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @see "ConvergenceChunker"
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/TextSmoother.java}
 * blob {@code 0b2d4d9c3aabbc9c19ab069ffb5fe7ab52444cc2} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@FunctionalInterface
public interface TextSmoother {

    /**
     * Smooths input text for chunking.
     *
     * <p>
     * Must be a PURE function: same input produces same output.
     * </p>
     *
     * @param input
     *            raw text
     * @return smoothed text
     */
    String smooth(String input);

    // --- FACTORY METHODS ---

    /**
     * Identity smoother (no-op).
     *
     * @return identity smoother
     */
    static TextSmoother identity() {
        return input -> input;
    }

    /**
     * Basic normalization smoother.
     * <ul>
     * <li>Lowercase</li>
     * <li>Collapse whitespace</li>
     * <li>Trim</li>
     * </ul>
     *
     * @return normalizing smoother
     */
    static TextSmoother normalize() {
        return input -> {
            if (input == null) {
                return "";
            }
            return input.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        };
    }

    /**
     * Punctuation removal smoother.
     *
     * @return punctuation-removing smoother
     */
    static TextSmoother removePunctuation() {
        return input -> {
            if (input == null) {
                return "";
            }
            return input.replaceAll("[^a-zA-Z0-9\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        };
    }

    /**
     * Combined normalize + remove punctuation.
     *
     * @return combined smoother
     */
    static TextSmoother full() {
        return normalize().andThen(removePunctuation());
    }

    // --- COMPOSITION ---

    /**
     * Composes this smoother with another (this then after).
     *
     * @param after
     *            smoother to apply after this one
     * @return composed smoother
     */
    default TextSmoother andThen(TextSmoother after) {
        return input -> after.smooth(this.smooth(input));
    }

    /**
     * Composes another smoother before this one (before then this).
     *
     * @param before
     *            smoother to apply before this one
     * @return composed smoother
     */
    default TextSmoother compose(TextSmoother before) {
        return input -> this.smooth(before.smooth(input));
    }
}
