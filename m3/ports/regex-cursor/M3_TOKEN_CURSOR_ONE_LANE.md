# M3TokenCursor single-lane construction optimization — candidate 2026-11-02

## Original source and invariant

* Owner: `hsoliwal/M3jdk21`, `m3/ports/regex-cursor/src/main/java/jdk/m3/regexcursor/M3TokenCursor.java`.
* Source Git blob before: `7823ae41c829e7b5bde50a850cfe90c42c3cabd8` (verified against GitHub and the supplied archive). The canonical `M3TokenCursor` is reused; there must be no alternate cursor or lexer.
* Existing `of(int[])` behavior: reject null, construct one borrowed input lane, discard that lane if empty; validate all token IDs are nonnegative; calculate presence-mask exactly once; live input alias remains borrowed for all non-empty lanes. Empty cursor must have 0 logical chunks and 0 total units. `lanes(int[][])` retains its unchanged multi-lane normalization and validation.
* Original `M3GrammarTokenProjection.projectCompleteCursor` passes a new positive-only primitive ID lane to `of`; this must remain the only complete-projection admission and must preserve its source-generation ownership boundary.
* Change is private implementation only: bypass `Stream.map/filter/toArray` when the input is already exactly one lane. Preserve null exception (`tokens`), negative-token refusal, empty-navigation semantics, binary-search seek semantics, public method and ABI signatures, no product native entry points.

## Source and verification order

1. Inventory current code and source hashes; create only a new candidate branch after matching ancestry and absence of overlapping owners.
2. Document before code. Capture original source and exact modified output SHA-256 and Git blobs.
3. Serial patch: change only `of(int[])`; preserve `lanes` and every other method byte-for-byte.
4. Lint and compile `--release 21 -Xlint:all -Werror` before tests.
5. Independent deterministic differential test: Java oracles (including null, negative, empty, large ID, all UTF16 irrelevant here), navigation/seek, presence-mask, live aliased lane semantics, split-vs-single representation, 100k deterministic rounds, cross-generation isolation when used through `M3GrammarTokenProjection`.
6. `javap -public -s` bytecode signatures identical. Measure thread-allocated bytes/call and latency before/after without claiming unconditional speedup or lower total GC impact.
7. Original JNI C17 and prior projection proofs run after compilation; no JNI modifications. Add exact Synexia hash-pinned Maven/OpenRewrite recipe with fixed-point and occupied-target tests, then PRs on named feature branches, no merge.
8. Complete-source JDK21/jtreg, real Maven/OpenRewrite SDK, GC/HotSpot, cross-platform JNI and application performance remain HOLD unless executed.

## Donors and licenses

* LeetCode #208 (Trie), #28 (Find First Occurrence), #211 (Word Dictionary): problem-shape references only. Existing M3TokenPattern, M3CursorLiteralSearch and M3TokenCursor are canonical code owners; no challenge solutions copied.
* HackerRank Contacts and No Prefix Set: category ideas for invalid/empty and fast repeated queries, not copied code.
* GeeksforGeeks Trie Insert and Search / Java Array vs Stream: algorithms and test category only, no direct code reuse.
* OpenJDK 21 Java collections and streams: standard observable semantics authority; actual implementation is independently written against existing Apache-2.0 M3 code.
