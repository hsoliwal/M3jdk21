# M3-SPEC-PRECOMPUTE / complete projection to token cursor

Date: 2026-11-02. Stage: candidate-only, Synexia recipe-first.

## Owner boundaries and immutable contracts

The original `M3GrammarTokenProjection` is the sole bridge between producer-owned lexer token spans and immutable M3 language-spec token IDs. It must not lex or reparse source, read outside checked half-open UTF-16 spans, retain source or arrays, or change the existing sparse `project` and strict `projectComplete` contracts. The original `M3TokenCursor` is the sole cursor owner; the original `M3TokenPattern` remains the regex owner. Do not introduce another parser, regex engine, or public JDK API.

New package-private `projectCompleteCursor(...)` is a single safe boundary: it calls existing `projectComplete` (full geometry and admission, reject unknown/skip ID 0), hands the **newly allocated and never publicly exposed** primitive ID lane directly to the existing `M3TokenCursor.of` constructor, and returns the cursor. No second ID array, retained input text, per-token object, or JNI pointer. Exposing the array would invalidate ownership, since the existing cursor shares caller-provided arrays and precomputes presence bits; keeping the array private preserves fact validity across later source-generation mutation. Concurrent operations require independent mutable cursors; the immutable grammar remains shareable.

Input source must represent one authoritative parser/lexer generation. If the source changes during tokenization itself, the producer must reject/reproject; this leaf does not add concurrent source mutation semantics. If the source changes **after** a complete cursor is constructed, the cursor retains its originally projected token IDs. Any skipped/unknown token, bad span, or admission error refuses atomically before cursor publication. An empty complete stream produces an empty cursor. All public Java signatures and JNI exports remain unchanged.

## Deterministic verification obligations

1. Inventory full original Java files and Git blob identities; preserve pre/post SHA-256 and branch ancestry.
2. Documentation precedes source; use canonical source-sealed Maven/OpenRewrite recipe and idempotent fixed point / occupied target refusal.
3. `javac --release 21 -Xlint:all -Werror` then standalone seeded differential oracle. Check complete positive IDs, gaps, Unicode, independent cursor navigation and presence bits; exceptions for unknown/skip, geometry and null; previous `projectComplete` regression unchanged.
4. Check that mutation of a *source after construction* cannot mutate the cursor's tokens or precomputed presence; independently allocated cursor instances do not alias each other or expose the owned ID lane. Test no phantom `<0>` symbol.
5. Run existing Java projection proof and C17 JNI oracle in `-Xcheck:jni` and `-Xint -Xcheck:jni` modes; public `javap -public -s` before/after must match.
6. Maven/JUnit/OpenRewrite SDK, full M3JDK21/jtreg/HotSpot/GC, cross-OS ABI and performance remain HOLD until actually executed. No production performance claim.

## Algorithm decision

Copy **no** LeetCode, HackerRank or GeeksforGeeks submissions or editorials. Their problem categories only inform test shapes. KMP and token regex remain existing M3 implementation owners. The algorithm is validated zero-copy *ownership transfer* of a freshly generated primitive lane, not a new trie or automaton.
