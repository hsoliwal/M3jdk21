# M3-SPEC-PRECOMPUTE: frozen ASCII root dispatch

Status: **candidate-only**. Target: existing M3JDK21 `M3StaticGrammarLexicon`, stacked on #2314; Synexia canonical hash-pinned recipe stacked on #15415. This is **not** a second lexer, pattern engine or JNI runtime. It reuses `M3StaticGrammarLexicon`, `M3GrammarTokenProjection`, and the existing independent C17 JNI exact-token oracle.

## Contract and custody

* Public source, binary and JNI signatures remain unchanged. Only a private immutable primitive lane and private transition implementation can change. No `java.base`/HotSpot/GC change.
* Build grammar as before; both output tables and digest MUST be identical for all candidate lexicons. The new `int[128]` fastpath exists only if the root has at least 8 ASCII edges **including control-character literals such as NUL**; otherwise it is absent. Missing slots use -1 (never a node ID).
* State zero and ASCII `char` use direct table lookup; every other state and non-ASCII edge use the unchanged sorted-array binary search.
* Compile only from previously validated, sorted source rows; primitive lane has no retained per-lexeme Node/Entry. Input source `CharSequence` is neither copied nor retained.
* Existing `M3GrammarTokenProjection` remains owner of token-span and admission geometry; contexts, Unicode escape translation, source generation and token ordering remain external.
* No blind rename, move, merge, API replacement or source equality assertion. Before/after full SHA-256 and Git blobs bind exactly one changed Java owner.

## Category-first donors (no challenge code copied)

LeetCode 208 (Trie), 14 (Longest Common Prefix), 28 (Find First Occurrence); HackerRank Tries Contacts and No Prefix Set; GeeksforGeeks Trie Data Structure and Pattern Searching, plus existing first-party Synexia #14943 root lookup concept. These provide only independently evaluated algorithm problem categories/test shapes. Existing original M3 source is the implementation authority. Java 21 JLS §3 remains the lexical semantic authority.

## Verification order

1. Read full original Java source and JNI oracle, record original SHA-256, inventory existing scoped targets.
2. Review this contract/donor matrix BEFORE code patch.
3. Patch only private primitive storage and transition; compile with `javac --release 21 -Xlint:all -Werror`.
4. Run original Java and JNI oracles, then exhaustive UTF-16, randomized independent reference, high-root/low-root and all-role differential, repeatably with `-Xcheck:jni` and `-Xint -Xcheck:jni`.
5. Source-seal canonical Synexia Maven/OpenRewrite recipe; real SDK fixed-point/refusal, Maven, JUnit and source-built M3JDK21/JNI platform matrix remain HOLD until truly executed.
6. Benchmark before-and-after with the same deterministic allocation/cpu workload; no production speed claim without measurement and broad gate completion.

## Acceptance threshold

Correctness parity is mandatory. Small lexicons MUST avoid the 512-byte retained ASCII table. Optimized compilation MUST be deterministic and immutable. All source hashes and proof logs must be present. Release promotion is explicitly not authorized by this draft.
