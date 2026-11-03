# M3 token cursor borrowed-lane facts and immutable-owned facts (2026-11-02)

Source owners: `M3TokenCursor`, `M3CursorMatcher`, `M3GrammarTokenProjection`.
This is a candidate-only, Java 21/C17 JNI-independent correctness fix.

## Invariants

- Existing public `M3TokenCursor.of(int[])` and `lanes(int[][])` **share** all nonempty inner arrays by identity and retain their existing input validation and navigation behavior.
- Because these publicly borrowed arrays can change after admission, precomputed unit-presence facts cannot remain authoritative. `unitPresenceMask()` must reflect current nonnegative symbols, and must conservatively report UNKNOWN if a caller introduces a negative ID after admission.
- A borrowed token cursor must **not** enter the matcher required-literal presence or substring gate. Both can become stale across calls, including after a previous miss. Regex matching must inspect current cursor contents and preserve legal zero IDs.
- A *fresh* primitive ID array produced by the existing fail-closed `projectComplete` method is not exposed to callers. The package-private owned factory can conservatively pin that lane as immutable; only that route may reuse cached presence/literal gates. The owner does not clone the complete projection or re-lex the source.
- The source-generation contract: caller mutation of source/geometry **during** projection is unsupported; after successful ownership transfer, the frozen projected token values must not change when the source or input geometry changes.
- Keep all prior public Java signatures, public API semantics for immutable inputs, all JNI exported signatures, `java.base`, and default branches unchanged. No new lexer, token regex engine, or donor implementation.
- A cursor is thread confined for navigation; immutable grammar can be shared. Concurrent mutation of borrowed arrays during matching is outside the supported consistency contract (the tests change arrays between operations only).

## Verification sequence

Inventory & exact pre-hash -> documentation -> reviewed patch -> strict Java 21 lint/compile -> differential tests -> compatibility and existing projections -> JNI unchanged oracle -> real Maven/OpenRewrite + JUnit + JDK/jtreg full acceptance.

LeetCode #211 (dynamic dictionary queries), HackerRank Tries Contacts (mutable indexed state), and GeeksforGeeks trie search are **category/adversarial test inspirations only**. Reuse existing M3 regex engines and original Java21/JNI source owners, no third-party code copied.
