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

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

/**
 * IGlyph - Simple LLM-understandable symbols for semantic compression.
 *
 * <p>From Grand Java Patterns Proof:
 * <pre>
 *   Glyphs compress meaning so even small models can reason.
 *   Intent / Emotion / Correctness / Direction
 *
 *   These are intentionally minimal.
 * </pre>
 *
 * <p>Use cases:
 * <ul>
 *   <li>NLP output encoding</li>
 *   <li>Transcript analysis</li>
 *   <li>Memory ingestion</li>
 *   <li>LLM-friendly markers</li>
 * </ul>
 *
 * @since Grand Java Patterns Proof - NLP Section
 */
@ISpec(
    value = "LLM-understandable symbols for semantic compression",
    constraints = {
        "Glyphs compress meaning for small model reasoning",
        "Four dimensions: Intent, Emotion, Correctness, Direction",
        "Symbols are intentionally minimal for token efficiency"
    },
    invariants = {
        "symbol() returns non-null short representation",
        "description() returns non-null human-readable text"
    },
    examples = {
        "IGlyph.Intent.ASK.symbol() // returns '?'",
        "GlyphTuple.neutral().toSymbols() // returns '.-+>'"
    },
    model = Model.ModelType.HAIKU,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/IGlyph.java}
 * blob {@code 0253f7af6552b80dbf2cb7dd8d72e4be274d61ae} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IGlyph {

    /**
     * Get the glyph symbol (short representation).
     *
     * @return symbol string
     */
    String symbol();

    /**
     * Get human-readable description.
     *
     * @return description
     */
    String description();

    // --- GLYPH ENUMS ---

    /** Intent glyphs - what is the speaker trying to do? */
    enum Intent implements IGlyph {
        ASK("?", "Asking a question"),
        STATE(".", "Making a statement"),
        COMMAND("!", "Issuing a command"),
        REFLECT("~", "Reflecting or thinking");

        private final String symbol;
        private final String description;

        Intent(String symbol, String description) {
            this.symbol = symbol;
            this.description = description;
        }

        @Override
        public String symbol() { return symbol; }
        @Override
        public String description() { return description; }
    }

    /** Emotion glyphs - emotional state detection. */
    enum Emotion implements IGlyph {
        CALM("_", "Calm and composed"),
        CONFUSED("?!", "Confused or uncertain"),
        INTENSE("!!", "Intense or strong feeling"),
        NEUTRAL("-", "Neutral tone");

        private final String symbol;
        private final String description;

        Emotion(String symbol, String description) {
            this.symbol = symbol;
            this.description = description;
        }

        @Override
        public String symbol() { return symbol; }
        @Override
        public String description() { return description; }
    }

    /** Correctness glyphs - validity assessment. */
    enum Correctness implements IGlyph {
        VALID("+", "Valid and correct"),
        UNCERTAIN("~", "Uncertain or ambiguous"),
        INVALID("-", "Invalid or incorrect");

        private final String symbol;
        private final String description;

        Correctness(String symbol, String description) {
            this.symbol = symbol;
            this.description = description;
        }

        @Override
        public String symbol() { return symbol; }
        @Override
        public String description() { return description; }
    }

    /** Direction glyphs - conversation flow direction. */
    enum Direction implements IGlyph {
        FORWARD(">", "Moving forward"),
        BACKWARD("<", "Going back or reviewing"),
        STALLED("|", "Stopped or waiting");

        private final String symbol;
        private final String description;

        Direction(String symbol, String description) {
            this.symbol = symbol;
            this.description = description;
        }

        @Override
        public String symbol() { return symbol; }
        @Override
        public String description() { return description; }
    }

    // --- GLYPH TUPLE ---

    /** GlyphTuple - combined glyph state. */
    record GlyphTuple(
            Intent intent,
            Emotion emotion,
            Correctness correctness,
            Direction direction
    ) {
        /**
         * Get compact symbol representation.
         *
         * @return concatenated symbols
         */
        public String toSymbols() {
            return intent.symbol() + emotion.symbol() +
                    correctness.symbol() + direction.symbol();
        }

        /**
         * Create default neutral tuple.
         *
         * @return neutral glyph tuple
         */
        public static GlyphTuple neutral() {
            return new GlyphTuple(
                    Intent.STATE,
                    Emotion.NEUTRAL,
                    Correctness.VALID,
                    Direction.FORWARD
            );
        }
    }
}
