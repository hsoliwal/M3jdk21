<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 fixed-grammar literal precompute (Java 21)

## Status and ownership

This is an opt-in isolated M3JDK21 regex-cursor **candidate**, not a replacement lexer,
M3String owner, HotSpot integration, Java public API, or complete Java grammar.
The existing `M3TokenCursor`, `M3TokenPattern`, and `M3CursorLiteralSearch` retain
responsibility for token-sequence regex and chunk-spanning literal search. The new
`M3StaticGrammarLexicon` owns only deterministic, immutable exact lexeme recognition
from an externally versioned language grammar. Its Java and JNI proofs are bounded.

## Contract: M3-SPEC-PRECOMPUTE-LEXEME

1. A language identifier, a grammar revision and a set of literal-to-token-ID rows
   form one immutable specification snapshot. Input order does not change the result.
2. A compiled snapshot consists only of primitive arrays and immutable string metadata.
   Tree nodes and maps used during compilation are temporary and are not retained.
3. All runtime lookups are allocation-free against `CharSequence` UTF-16 code units;
   never stringify, intern or copy the incoming source. The longest-prefix result
   is a packed `(tokenId, endExclusive)` pair; exact-match lookup additionally
   requires the token's complete span to match.
4. Token IDs are positive and unique within the lexicon; duplicate lexemes,
   conflicting IDs and null/empty lexemes are rejected. A contextual token's
   grammar role is metadata, **not** an unconditional Java keyword claim.
5. The caller, not this class, owns Unicode escape translation, source offset
   mapping, identifier boundaries, comments and syntactic context. In particular,
   `forbidden` is not a Java `for` keyword and raw `\\u0066or` must not be
   decoded by a post-tokenization lexicon. JLS 21 section 3 is the authority.
6. No changes to the `M3ChunkCursor` contract, `M3String`, `M3TokenPattern`,
   parser public API, JNI exported product ABI or `java.base` are allowed.
7. Native implementation is **test-only oracle** using JNI and ordinary UTF-16
   JDK Strings. Java remains the product candidate. No native acceleration or
   performance advantage is claimed.
8. LeetCode 208 (trie) and 28 (KMP), HackerRank Contacts and GeeksforGeeks
   trie/pattern-search topics are problem-category references, not copied source.

## Algorithm

Sort candidate lexemes in exact `String.compareTo` UTF-16 order, build a temporary
trie and flatten it breadth-first to four primitive lanes: `start[]` (`nodes+1`),
`symbol[]` (UTF-16 units), `next[]` (node ids), `terminal[]` (token IDs), plus
`contextual[]` metadata. Each node's outgoing transitions are sorted and binary
searched, with maximum munch remembered for prefix lookup. Independent complete
input and configuration values are hashed through length-prefixed UTF-16 SHA-256;
the digest is a source identity, not a semantic equality substitute.

## Acceptance / test order

- Inventory existing source paths and corresponding Synexia recipe owners.
- Source-seal and execute canonical Maven/OpenRewrite recipe (ABSENT-before,
  exact fixed point, occupied-target refusal) before product promotion.
- Execute `javac --release 21 -Xlint:all -Werror` on the actual Java files.
- JUnit and standalone deterministic differential test: exhaustive short
  alphabets, seeded random inputs, Unicode, suffixes, duplicate refusals,
  snapshot order, concurrent reads, and non-translation of raw escapes.
- Compile test-only JNI C17, execute native-vs-Java exact lookup parity.
- Full Maven/Surefire, JaCoCo >= 0.99, JDK/jtreg, JNI platform matrix and
  latency/allocation benchmarks before acceptance into compiler/M3String.

## Scope lock

All changes are additive and confined to `m3/ports/regex-cursor/**` and a named
Synexia canonical recipe crate. No direct `develop`, `master`, JDK core or user
workspace modification; no merges, rebases, deletions or inferred donor licenses.
