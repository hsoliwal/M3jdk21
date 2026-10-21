// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
//
// Lineage (leaf R6-AUTOMATON, run 20261017-synexia-to-public): contract shape learned from helix-editor/regex-cursor
//   0.1.5 (commit fd4c51c73587981219daf9a36184aabbddae78ee, src/cursor.rs blob ca55f27b1f69d44222d7d4e62ef0a5ee8499015e,
//   dual-licensed MIT OR Apache-2.0); Rust donor, pattern only (Java-only law): no code copied. UTF-16 units replace bytes,
//   surrogateAware() replaces utf8_aware(). Intended implementor: the existing java.lang.M3String.CodeUnitCursor
//   (M3String.java L579-622 on M3jdk21 master 970a34350eb), extended in the next leaf to expose its contiguous run and keep
//   its descent path in lanes; this leaf adds no second tuple cursor.
package jdk.internal.mindex;

/**
 * Chunked UTF-16 view of a discontiguous text (tuple atoms, ropes) for automaton runners. The cursor always rests on one
 * chunk; runners read units in place and carry automaton state across chunk moves, so a match spanning chunks never
 * needs a contiguous copy. Internal; not a public JDK API.
 *
 * <p>Contract: {@link #chunkLength()} is positive unless the whole text is empty; {@link #advance()} and
 * {@link #backtrack()} move to the next / previous chunk and return {@code true}, or return {@code false} at the ends
 * without changing the current chunk; {@link #chunkOffset()} is the offset of the current chunk in UTF-16 units.
 */
public interface M3ChunkCursor {
    /** Units in the current chunk; zero only when the text is empty. */
    int chunkLength();

    /** Unit {@code index} of the current chunk, {@code 0 <= index < chunkLength()}. */
    char unit(int index);

    /** Offset of the current chunk from the start of the text, in UTF-16 units. */
    long chunkOffset();

    /** Total text length in UTF-16 units; independent of the cursor position. */
    long totalLength();

    /** Moves to the next chunk; false (and no move) at the last chunk. */
    boolean advance();

    /** Moves to the previous chunk; false (and no move) at the first chunk. */
    boolean backtrack();

    /**
     * True if chunk edges never split a surrogate pair. Code-unit automata do not need it (state carries across the
     * edge); code-point consumers and word boundaries do.
     */
    boolean surrogateAware();
}
