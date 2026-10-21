<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Token-aware regex: M3JDK21 isolated Java receiver (2026-10-18)

**Current disposition:** ISOLATED_PRECOMPUTE_CANDIDATE; NOT installed in java.base,
not a replacement for java.util.regex.Pattern/Matcher, no new JNI ABI,
no lexer/AST importer, and not an admitted M3String runtime optimization.
The canonical ordered delivery remains String -> arrays -> collections -> AST/compiler;
the receiver status is deliberately unchanged.

## Why a distinct token program

Character regex matches characters; word boundaries and named captures do **not**
make it token- or syntax-aware. Only a lexer/parser carrying token kinds, canonical
lexeme identifiers and lexical states can distinguish an identifier from the same
characters inside a string literal or comment.

T-Rex (https://github.com/square/t-rex, Apache-2.0) is a **conceptual grammar
and API donor**: predicate-bearing token sequences, repetition and alternating
token matches. NLTK TokenSearcher is a separate conceptual token-search reference.
No T-Rex/CoreNLP/NLTK source or grammar was copied. M3-specific array and owner
layout is authored for this candidate.

## Owner and representation

- **Target:** com.m3.precompute.M3TokenRegex, isolated in m3/ports/precompute.
- **Canonical upstream recipe:** com.synexia.rewrite.M3Jdk21TokenRegexReceiverRecipe
  in hsoliwal/com.synexia, source-sealed template root
  /com/synexia/rewrite/m3jdk21-token-regex/.
- **Input:** M3TokenRegex.Tape retains a strong reference to one caller-provided
  canonical source owner and snapshots five aligned primitive arrays:
  kind[], atom[], mask[], UTF16-start[] and UTF16-end[]. It owns no spelling,
  AST node, per-occurrence object or copied character buffer. Source UTF-16
  offsets are half-open and range-validated.
- **Compiled plan:** int[] opcode, next, alternative, kind, atom, requiredMask,
  forbiddenMask; bounded to 8,192 NFA states. No per-token or per-match entry
  objects. Per-invocation primitive visitation/epsilon-closure arrays allow
  concurrent callers to share one immutable plan.
- **Output:** one Span on successful user API call: [tokenFrom,tokenTo) and
  [utf16From,utf16To) against the pinned source owner. No source materialization.

## Explicit subset grammar

Atomic token patterns: \`[]\` or \`.\` for any token, \`[kind=5]\` for lexical
type, \`[atom=29]\` for a canonical M3 lexeme ID, and
\`[kind=5,atom=29,all=0x4,none=0x8]\` for combined type/atom/state constraints.
Operators: concatenation, grouping \`(...)\`, alternation \`|\`, optional \`?\`,
zero-or-more \`*\`, one-or-more \`+\`, and token-sequence anchors \`^\` and \`$\`.
All numeric kinds and atoms are nonnegative signed int IDs; masks are unsigned
32-bit flags. Whitespace in the **pattern** separates operators and does not
consume a source token.

\`matches(tape)\` covers the whole token sequence. \`find(tape, fromToken)\` finds
the leftmost-longest token interval in linear progress over source ordinals;
nullable patterns can match zero tokens, so clients must advance after an empty
result. String lexical equivalence, Unicode case folding, attributes keyed by
named strings, capture groups, lookarounds, backreferences, conditional syntax,
persistent effect-image reuse and token text regex are NOT implemented.

Illustration: lexer recognizes \`x="x";x\` as
IDENT / EQUALS / STRING_LITERAL / SEMICOLON / IDENT.
\`[kind=IDENT_ID,atom=X_ATOM]\` matches the two identifiers, never the quoted
"x" even if the lexer gives all three the same atom identifier. To forbid
crossing comment or literal tokens, the lexer MUST retain their token kinds
rather than silently drop them from the tape.

## Complexity and limits

The matcher uses an epsilon-closure Thompson NFA without character-level
backtracking; source tokens advance monotonically. Worst-case work is bounded
by O(token_count * compiled_state_count) with potential epsilon revisit overhead;
allocation is O(compiled_state_count) per operation. It does not guarantee a
universal performance win or Java Matcher-compatible match selection. Large
tapes have five metadata int arrays, roughly 20 bytes/token before array headers
and GC/source costs; budget source metadata and avoid per-token heap carriers.
A 100 MB file parsing benchmark, incremental token tape updates, segment-aware
M3String owner coordinates and M3AST token adapters remain required target work.

## Qualification packet

| Gate | Status |
|---|---|
| Java source candidate + exhaustive small-alphabet differential JUnit | COMMITTED / EXECUTION PENDING |
| 60,001-token hostile repetition fixture | COMMITTED / EXECUTION PENDING |
| Source SHA-256 and source-sealed Synexia recipe templates | COMMITTED; recipe run pending |
| OpenRewrite fixed-point / stale-preimage refusal JUnit | COMMITTED / EXECUTION PENDING |
| javac21 / Maven precompute module | PENDING CI |
| Full M3 String/AST lexer and offsets parity | PENDING |
| JDK jtreg Pattern/Matcher equivalence | NOT_APPLICABLE; public JDK owner unchanged |
| JNI C/native | NOT_APPLICABLE; Java-only candidate |
| JMH CPU/heap and multi-MB/100MB workload | PENDING |

### Source custody

- Source postimage SHA-256:
  \`47be591a396c021465fe2bb72bd6893ca80dc1e0407d0f70785bfee2ecacc3e3\`.
- Test postimage SHA-256:
  \`d611e922df0a6222c5b2964c9b662eef85b933cc96353e5d255e46ba1b1cb4a5\`.
- Both entries use \`ABSENT\` preimages and reject drift. Follow canonical
  Inline -> Atomize -> Patternize -> Comment transformations, then Maven/OpenRewrite
  proof, before any java.base/Pattern integration.

Neither the optional M3 TokenTape source owner, nor the imported T-Rex grammar,
is a substitute for the existing M3String ownership/canonical precompute gates.
