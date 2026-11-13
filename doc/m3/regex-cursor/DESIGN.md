<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 regex cursor: regex over M3String chunks and token-id arrays

Status: DESIGN + proving ground (`m3/ports/regex-cursor`, Apache-2.0). No `src/java.base` change.
Branch `m3/regex-cursor-20261018`, base master `2f2beb2eff7a`.
Donor ledger: [`m3/ports/regex-cursor/FOSS_REUSE_DECISION.tsv`](../../ports/regex-cursor/FOSS_REUSE_DECISION.tsv).

## 1. Problem

`java.util.regex.Matcher` reads its input through `CharSequence.charAt`. For an M3String
(owner + packed `start<<32|length` coordinate over an AVL tuple DAG of atoms) each `charAt`
descends the DAG, and the bulk paths (`String.value()`, the JNI `nativeCharShadow`) materialize
the whole text. The user law (feedback-string-composite-regex-mechanical) is: exhaust the
mechanical precompute gates first, then walk chunks with a cursor, and materialize last.

helix-editor/regex-cursor (MIT OR Apache-2.0) is the pattern donor: a port of Rust
`regex-automata` in which every engine (meta, hybrid lazy DFA, PikeVM) reads the haystack
through a `Cursor` (`chunk()`, `advance()`, `backtrack()`, `offset()`, `total_bytes()`) so a rope
is never flattened. Rust is never compiled here (Java-only law); the shapes are ported.

## 2. Chunk cursor over M3String

Contract `M3ChunkCursor` (the donor `Cursor` + `Input.move_to`, re-expressed over UTF-16 units):

| Donor | M3 | Rule |
|---|---|---|
| `chunk() -> &[u8]` | `chunkLength()`, `unit(i)` | no array is exposed; `unit(i)` reads the atom payload in place |
| `advance()` / `backtrack()` | same | `false` at the end/start and the chunk is unchanged |
| `offset()` | `offset()` | absolute logical start of the current chunk |
| `total_bytes()` | `totalLength()` | constant for the cursor's life |
| `Input::move_to` (linear, TODO) | `seek(position)` | O(tree height) descent; `seek(total)` lands on the last chunk |
| `utf8_aware()` | quit units | chunk ends may split a surrogate pair; surrogates are quit units (section 4) |

`M3StringCursor` walks the terminal atom ranges of the tuple DAG in logical order, exactly the
descent of `java.lang.M3String.CodeUnitCursor.seek` (master `M3String.java` L579-622):
from the root `(owner, start, length)` go left if `position < left.length()` else right, clipping
the visible run at every level, until the owner is an atom. The cursor state is five primitives
plus one atom reference (`atom`, `atomOffset`, `chunkBegin`, `chunkEnd`, `total`): no stack
object, no text copy, no native pointer copy. `advance()` = `seek(chunkEnd)`,
`backtrack()` = `seek(chunkBegin - 1)`; both are O(height) and the AVL `concatBalanced` keeps the
height O(log atoms). A stack-carried O(1) amortized step is next leaf C2.

Proving-ground mirror: `M3RopeString` (owner + packed long coordinate, the M3String invariant),
`M3RopeAtom` (payload bytes with storage width 1|2 big-endian, the stand-in for
`M3StringAtom.address`), `M3RopeTuple` (left/right coordinates, height, OR-composed unit-presence
facts). The mirror never offers `toString()` over the text: the only way to read it is a cursor.

`M3CharSequenceCursor` adapts any `CharSequence` with an explicit cut plan (`int[]` chunk ends),
used for flat Strings (one chunk) and for random chunk splits in tests (including 1-unit chunks).

## 3. Gate ladder before any engine

`M3CursorRegex` executes in this order and stops at the first decision:

1. **Facts gate** (precompute, O(1)): `M3CursorFacts.unitPresenceMask()` is the OR-composed
   64-bit unit-presence set of the source (bit `unit & 63`), composed on tuple nodes like
   `M3StringFacts.compose`. If a unit of the required literal has no bit, there is no match.
   In java.base this rung is `jdk.internal.mindex.M3TQ` trigram absence on the M3String facts.
2. **Literal engine**: a pure literal pattern never reaches an automaton;
   `M3CursorLiteralSearch` is the whole engine (start/end, find iteration, reverse search).
3. **Prefix literal**: when every match starts with literal `L`, candidates come from the
   literal search and each is confirmed by the anchored leftmost-first DFA (no reverse pass).
4. **Required literal**: when every match contains `L`, one literal scan from `from` proves
   absence for the remainder of the haystack (cached per matcher).
5. **Lazy DFA** (`M3CursorDfa`) for the assertion-free regular subset: forward unanchored
   leftmost-first scan gives the end, reverse anchored all-match scan gives the start.
6. **PikeVM** (`M3CursorPikeVm`) for captures (bounded to `[start, end]` of the DFA match) and
   for patterns with assertions (`^`, `$`, `\A`, `\z`, `\Z`).
7. **Stock fallback**: everything else runs stock `java.util.regex` over
   `M3CursorCharSequence`, a `CharSequence` view whose `charAt` seeks the cursor. Even the
   fallback does not copy the text; `toString()` on the view is the only materialization and
   is counted by `M3CursorProbe`.

`M3CursorLiteralSearch` replaces the donor's `(needle-1)`-byte carry buffer (`literal.rs`) with
a KMP state carried across chunk boundaries: a needle that spans any number of chunks is found
without copying. While the state is 0 the inner loop is a first-unit skip inside the chunk (the
memchr analogue). Reverse search uses the reversed needle and `backtrack()`.

## 4. Lazy DFA (regular subset)

Subset compiled by `M3RegexSubsetParser` (anything else is refused to the stock engine):
literals, `\Q..\E`, escapes `\t \n \r \f \a \e \xhh \uhhhh` and escaped punctuation,
`.` (no line terminators unless DOTALL), classes `[...]` with ranges and negation, `\d \w \s` and
negations (ASCII, JDK default), groups `(...)` and `(?:...)`, alternation, greedy and lazy
`* + ? {n} {n,} {n,m}`, flags `0` and `DOTALL`. Refused: backreferences, lookaround, possessive
and atomic groups, nested/intersected classes, inline flags, CASE_INSENSITIVE, MULTILINE,
COMMENTS, UNICODE_*, `\b \B \G`, named groups, and quantifiers over a nullable body (the
empty-iteration rules of the JDK `Loop` node differ from Thompson closure semantics).

Semantics (Rust `regex-automata` determinization, algorithm only):
- Thompson NFA in int lanes (`op`, `out`, `out2`, `arg`; class ranges in one `int[]` pool),
  splits ordered by priority (greedy prefers the body, lazy prefers the exit).
- Unanchored search compiles the lazy `(?s:.)*?` prefix into the NFA at the lowest priority.
- Leftmost-first: a DFA state is the *ordered* list of NFA class states; while computing the
  closure, the first MATCH state truncates every lower-priority state. Scanning records the last
  matching position until the dead state; that is the leftmost-first end.
- The start is the smallest `s >= from` reachable by the reverse NFA, anchored at the end, in
  all-match mode; this equals the leftmost start because any earlier start would itself be a
  match.
- `matches()` uses the anchored all-match DFA and tests the state at the end.
- Units are mapped to equivalence classes: a `byte[256]` table below 256 and a binary search
  over class boundaries above; each class has a representative unit.
- **Quit units**: the surrogate range `D800-DFFF` is its own class whose transition is QUIT, so
  every result is computed over surrogate-free text (where UTF-16 units equal code points);
  meeting a surrogate hands the search to the stock engine over the cursor view.

DOOP layout (no per-match objects; lanes grow only on cold cache misses):

| Lane | Type | Content |
|---|---|---|
| `trans` | `int[]` | `state * classCount + class` -> next state (`-1` unknown, `0` dead, `1` quit) |
| `matchBits` | `long[]` | one bit per DFA state |
| `pool`, `poolStart`, `poolLength` | `int[]` | ordered NFA state lists per DFA state |
| `table` | `int[]` | open-addressing intern table over pool hashes |
| `dense`, `sparse`, `stack` | `int[]` | closure scratch (sparse set) |

A DFA cache over `maxStates` (default 4096) gives up to the PikeVM, then the stock engine.

## 5. PikeVM (captures, assertions)

Priority-ordered thread lists (sparse sets) with capture slots in `int[states * slots]`
lanes, closure by an explicit int stack with restore frames (RE2/Rust shape). Leftmost-first:
the first thread reaching MATCH at a position cuts lower-priority threads. `matches()` keeps
only threads that reach MATCH at the end. Assertions read at most two units around the
position through `seek`; surrogates quit as in the DFA. Group spans are returned as int
coordinates into the source; a group string is never built.

## 6. Token-aware matching (second PR)

`M3TokenCursor` is the same chunk contract over interned token-id lanes (`int[]` chunks;
TIL/lexicon ids). `M3TokenPattern` compiles an angle-bracket notation (NLTK `TokenSearcher`
notation, T-Rex algebra) into the same NFA/DFA/PikeVM: `<17>` one id, `<3-9>` an id range,
`<4,8,15>` a set, `<#IDENT>` a named class from the lexicon class table, `<!...>` negation,
`<.>` any token, plus groups, alternation and quantifiers. The alphabet is the token id, so a
match is always a whole token span: it cannot cross a token boundary or land inside a literal or
comment token (exclude them with `<!#COMMENT>` or by class). No quit units on token alphabets.
Oracle: each token id `t` maps to the private-use unit `U+E000 + t` and the pattern to an
equivalent JDK regex; stock `Pattern` decides (the NLTK flattening is used only in tests).
Reconcile with M3RX7 TokenIndex (PR #1338), the java.base token-sequence scaffold PR #1397
(receives Synexia #12396) and PRs #1369/#1370/#1372/#1378/#1380 later.

Shipped on branch `m3/regex-cursor-token-20261018`: `M3TokenCursor` (lanes of `int[]`, empty lanes
skipped, OR-composed presence facts), `M3TokenPatternParser`, `M3TokenPattern`. Quantifiers over a
pattern that can match no token are rejected; captures inside loops follow PikeVM semantics
(last iteration). square/t-rex atoms (Synexia #12398 run `20261017-ui-donor-atomize`, 452
leaves) are mapped construct by construct in `m3/ports/regex-cursor/TREX_PATTERN_MAP.tsv`; gaps:
lexicon-backed regex classes, class intersection, numeric attribute predicates, same-span
conjunction. t-rex states Apache-2.0 only in its README (no LICENSE file), so it stays
LICENSE_REVIEW_REQUIRED: pattern use only, no code or test text copied.

## 7. java.base path (recipe-first)

The proving ground needs no java.base edit. Receiving order when the gates pass:
1. Synexia recipe crate `synexia-openrewrite-recipes/crates/m3-string-consumers/regex-cursor/`
   (hash-pinned postimages of a package-private `java.lang.M3StringChunkCursor` over the real
   `M3StringOwner/M3StringTuple/M3StringAtom`, reading atoms with `charAt` on the scalar owner).
2. `M3JdkHandoff` packet with preimage/postimage hashes, then a thin receiver in
   `java.util.regex.Matcher` behind `M3_JOINED_STRINGS`: when the input is an M3String and
   `m3Tq` gates are inconclusive, run the cursor engines before the `CharSequence` path.
3. jtreg `test/jdk/java/util/regex` flag on and off, differential receipts, CPU/heap evidence.

## 8. Proof obligations

- Differential vs stock `Pattern/Matcher`: seeded random patterns from the subset grammar x
  seeded inputs x random chunk plans (1-unit chunks, boundary-spanning matches), comparing every
  `find()` start/end/group, `matches()`, `lookingAt()`; plus the frozen
  `Regex10kMatrixMasteryCorpus` (10,000 x 64, root `e7614598...`) mirrored from
  `m3/ports/precompute` M3RegexOracleCorpus (the 552-case mastery file was not reachable in
  this session).
- Allocation probe: `M3CursorProbe` counts view materializations (must stay 0) and fallbacks;
  `ThreadMXBean` allocated bytes over warmed `find()` loops must be 0 on the cursor path.
- JaCoCo line coverage target 99% for `com.m3.regexcursor`.

## 9. Non-claims

No java.base change, no JNI, no claim of whole-JDK regex parity; unsupported syntax is
refused, not approximated. Performance numbers are not claimed by this leaf.

## 10. Next leaves

C2 stack-carried O(1) cursor step; C3 DFA cache clearing (Rust reset) instead of give-up;
C4 `\b`/MULTILINE assertions in the DFA via look-behind bits; C5 CASE_INSENSITIVE via folded
classes; C6 java.base recipe crate + handoff (section 7); C7 per-atom transfer rows (DFA state x
atom id -> state) cached on `M3StringAtom`; TOKEN-REGEX-RECONCILE with M3RX7.
