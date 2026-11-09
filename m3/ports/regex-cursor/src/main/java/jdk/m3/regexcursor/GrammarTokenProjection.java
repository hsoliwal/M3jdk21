// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.Objects;

/**
 * Source-generation-local projection of existing lexer spans onto a frozen grammar image.
 *
 * <p>This is not a lexer. The authoritative parser supplies disjoint, ordered, half-open UTF-16
 * token spans and a code-token admission lane. The projector neither reparses source nor creates
 * substring objects: it only uses {@link M3StaticGrammarLexicon#exact(CharSequence, int, int)}.
 * Skipped and unknown entries remain zero in the primitive output lane, which is NOT
 * a complete {@link M3TokenCursor} input: token regex patterns can match literal {@code <0>}.
 * Use {@link #projectComplete(M3StaticGrammarLexicon, CharSequence, int[], int[], byte[])}
 * for a token cursor, or retain {@link #project} for intentional sparse classification.
 * Contextual keywords are only candidates; semantic classification belongs to the parser.</p>
 *
 * <p>No source, input lane, projection result or JNI/native pointer is cached or retained.
 * A source-generation change requires reprojection by the authoritative owner.</p>
 */
final class GrammarTokenProjection {
    static final byte SKIP = 0;
    static final byte FIXED_CANDIDATE = 1;

    private GrammarTokenProjection() { }

    /**
     * Fail-closed admission of only fully known tokens into the token regex alphabet.
     * Preserves the existing {@link #project} geometry, source and admission contract.
     * Unlike sparse projection, rejects skipped or unknown zero-ID tokens before
     * returning the lane: zero is a legal token-regex symbol, not a safe hole marker.
     * Does not retain, re-lex, flatten or mutate any caller-owned inputs.
     *
     * @throws IllegalArgumentException on any unknown/skipped token
     */
    static int[] projectComplete(StaticGrammarLexicon grammar, CharSequence source,
            int[] starts, int[] ends, byte[] admission) {
        int[] ids = project(grammar, source, starts, ends, admission);
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == 0) {
                throw new IllegalArgumentException("incomplete grammar token at index " + i);
            }
        }
        return ids;
    }

    /**
     * Bind one complete lexical generation directly to the existing token-regex cursor.
     * The newly projected primitive lane is handed to the cursor once and never exposed
     * to the caller. This preserves the cursor's precomputed presence facts: unlike a
     * caller-supplied {@code int[]}, no external alias can subsequently mutate its IDs.
     * The underlying text and incoming span/admission arrays are not retained or copied.
     *
     * <p>Admission is fail-closed through {@link #projectComplete}: zero cannot become
     * a phantom {@code <0>} token-regex match. An empty generation remains valid.</p>
     */
    static TokenCursor projectCompleteCursor(StaticGrammarLexicon grammar,
            CharSequence source, int[] starts, int[] ends, byte[] admission) {
        return TokenCursor.ownedProjection(projectComplete(grammar, source, starts, ends, admission));
    }

    /**
     * Project exact lexeme IDs into one new primitive lane, preserving token index alignment.
     * Zero denotes no admitted exact lexeme; it is not a canonical lexeme dictionary ID.
     *
     * @throws IllegalArgumentException for mismatched lanes, invalid state or invalid span geometry
     * @throws NullPointerException for null arguments
     */
    static int[] project(StaticGrammarLexicon grammar, CharSequence source,
            int[] starts, int[] ends, byte[] admission) {
        Objects.requireNonNull(grammar, "grammar");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(ends, "ends");
        Objects.requireNonNull(admission, "admission");
        if (starts.length != ends.length || starts.length != admission.length) {
            throw new IllegalArgumentException("token lane lengths differ");
        }
        final int sourceLength = source.length();
        int previousEnd = 0;
        // Full geometry admission before any output; no caller array is ever modified.
        for (int token = 0; token < starts.length; token++) {
            int from = starts[token];
            int to = ends[token];
            byte mode = admission[token];
            if (from < previousEnd || from > to || to > sourceLength || from < 0
                    || (mode != SKIP && mode != FIXED_CANDIDATE)
                    || (mode == FIXED_CANDIDATE && from == to)) {
                throw new IllegalArgumentException("invalid token geometry at index " + token);
            }
            previousEnd = to;
        }
        int[] projected = new int[starts.length];
        for (int token = 0; token < projected.length; token++) {
            if (admission[token] == FIXED_CANDIDATE) {
                int id = grammar.exact(source, starts[token], ends[token]);
                if (id > 0) projected[token] = id;
            }
        }
        return projected;
    }
}
