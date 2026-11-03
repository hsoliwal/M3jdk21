<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 grammar-to-token projection: single parse / frozen lexical state

## Contract — M3-SPEC-TOKEN-PROJECTION / 2026-11-02

The original source lexer/compiler is authoritative. This opt-in leaf consumes a
single source generation and existing half-open UTF-16 token coordinate lanes,
without parsing, re-lexing or splitting the source. It is not a new Java 21 keyword
table, grammar engine, lexer, regex matcher, or global cache.

1. **Inputs.** A previously compiled immutable `M3StaticGrammarLexicon`, one
   stable CharSequence source snapshot, token start/end primitive `int[]` lanes,
   and a primitive eligibility `byte[]` lane. All arrays must be non-null and
   equal-length. Coordinates are 0 <= start <= end <= source.length; rows are
   ordered and non-overlapping (gaps are legal). A zero-width row is permitted
   only when its eligibility is SKIP (e.g. EOF). Eligibility is SKIP=0 or
   FIXED_CANDIDATE=1 only; other values are rejected before computing any output.
2. **Ownership.** The producer owns source, span and eligibility lanes, including
   context, comments, string/character/text-block boundaries, Unicode escape
   preprocessing, and source-generation identity. The projection does not
   normalize escapes, guess lexical boundaries, infer Java keyword status from
   prefixes, or infer contextual-keyword syntactic roles.
3. **Output.** Exactly one `int[]` primitive lane of token IDs is returned;
   0 indicates not admitted or no exact grammar lexeme. A nonzero ID is exactly
   `grammar.exact(source, start, end)`, never longest-prefix, so `forbidden`
   does not become `for`. Existing coordinates and source are never copied or
   retained. Consumers can pass this lane to the existing `M3TokenCursor.of`;
   they must preserve the original token IDs and source spans independently.
4. **Fail closed.** Invalid geometry/classes are refused before output allocation;
   no input lane is ever modified. A mutable CharSequence must not be changed
   while the projection runs. Recompute after source generation changes; never
   retain the ID lane across source generations without the owner's validation.
5. **Implementation.** Product candidate is pure Java 21, with no JNI calls in
   the hot path. Tests reuse the already-existing M3StaticGrammarLexicon test-only
   native C oracle to compare admitted spans; no duplicate C lexical matcher,
   persistent JNI pointers, native product export or java.base mutation.
6. **Reuse and collision controls.** Synexia merged PR #3313 already owns its
   Java21 language-spec image (51 reserved, 17 contextual keywords, literal
   tokens). MIndexGrammarLexer/MIndexTokenStream already own lexical offsets;
   new tables or parsers must not be introduced. M3JDK21 PR #2312 owns the
   fixed-lexeme primitive trie, M3TokenCursor and M3TokenPattern own token regex.
7. **API.** Package-private additive leaf only, no existing public/protected
   signatures, serialized formats, JNI ABI, or compiler contracts changed.
8. **Verification stages.** Exact source/document inventory -> canonical
   Synexia hash-pinned recipe and fixed-point/occupied-target refusal -> Java
   `--release 21 -Xlint:all -Werror` -> deterministic and seeded differential
   oracle -> JNI `-Xcheck:jni` and `-Xint -Xcheck:jni` -> module Maven/JUnit ->
   full JDK/jtreg -> multi-OS and performance before promotion.

## Algorithm / space

Preflight is O(token count), then for each eligible full token span call the
existing flat-array lexicon's `exact` transition walk. One `int[n]` lane is
allocated per *source projection*, not per token. No per-record Entry, Node,
subSequence, String or regex compilation is created. Performance improvement
versus the existing compiler token path is **unproven** until actual benchmark.

## Donor-category rule

LeetCode #208 exact trie and #28 KMP, HackerRank Contacts / No Prefix Set,
GeeksforGeeks Implement Trie are used as problem and refusal-category
references only. Original Synexia/M3 Java owns implementation; no third-party
challenge code or editorial is copied. JLS21 chapter 3 owns lexical semantics.
