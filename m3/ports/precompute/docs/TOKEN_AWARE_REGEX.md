<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 token-aware regular expressions — bounded Java 21 receiver

Status: **target-owned precompute implementation, focused Java proof; JNI and java.base promotion NOT claimed**.

The input is a previously constructed lexical / NLP / AST token tape rather than the raw
UTF-16 characters. The canonical storage is exactly four parallel primitive arrays:

- `int[] ids`: stable lexical/token dictionary IDs (nonnegative)
- `int[] kinds`: unsigned-by-convention 32-bit token classification masks
- `long[] starts`, `long[] ends`: half-open UTF-16 source coordinates

`M3TokenTape.capture()` defensively copies only these lanes. `slice()` shares them
using half-open offsets; no token, Map.Entry or node object is retained for each occurrence.
M3String/M3AST may supply these lanes without materializing a whole composite String.
The stored token ID must come from the caller's canonical lexicon/AST identity; this
class does **not** tokenize or intern text on its own.

## Pattern language (deliberately distinct from java.util.regex.Pattern)

| Pattern | Meaning |
| --- | --- |
| `#7` | exact token ID 7 |
| `%0x2` | any token with every required kind bit |
| `!%0x8` | any token with none of the excluded kind bits |
| `[id=7,all=0x2,none=0xc]` | combined token ID + positive/negative kind masks |
| `.` | any one token |
| `(...)`, `a|b`, `?`, `*`, `+` | noncapturing group, alternatives, token quantifiers |
| `^` / `$` | optional outer tape start/end anchors |

Whitespace in the pattern separates token expressions and is not matched as a source
character. Token masks can distinguish identifier, keyword, comment and string literal
tokens **when** the upstream lexer provides accurate kind flags. For example, an identifier
rule can use `[all=0x1,none=0xc]` to exclude comment/string classifications.

```java
int[] tokenIds = {7, 8, 8, 9};
int[] kindMasks = {2, 1, 4, 16};   // caller-defined bit positions
long[] starts = {0, 3, 100, 3000000000L};
long[] ends = {2, 4, 105, 3000000001L};
M3TokenTape tape = M3TokenTape.capture(tokenIds, kindMasks, starts, ends);

M3TokenRegex pattern = M3TokenRegex.compile("#7 [id=8,all=1,none=0xc]");
M3TokenRegex.Match match = pattern.find(tape);
// match: tokens [0,2), source UTF-16 [0,4)
```

## Implementation and cost

Compiler: bounded syntax parser -> primitive indexed syntax arena -> Glushkov
follow-position bitsets -> immutable compiled `int[]` selector lanes and flattened
`long[]` transition mask lanes. No per-input-token objects are allocated during
matching. Each invocation owns two `int[]` active-start buffers; the public result
record is created only when a match is returned.

A `matches(tape)` call requires a whole token sequence match. `find(tape)`
returns the **leftmost-longest** match in tokens (and the corresponding UTF-16
source span), unlike Java Pattern's default leftmost-first alternatives. This is
a separate grammar and not a drop-in Pattern/Matcher replacement. Empty matches
have zero-width source coordinates.

Compilation bounds: maximum 8,192 pattern characters, 1,024 expression nodes and
512 token positions; excess input is rejected. Execution is non-backtracking and
bounded by O(input_tokens × (positions + compiled_follow_edges)), with O(positions)
per invocation of primitive scratch storage. `primitiveBytes()` reports only retained
compiled primitive payload, not JVM object headers.

No named or numeric capture groups, lookarounds, backreferences, Unicode character
regex, token attribute maps or nested raw-source regex are admitted by this first
grammar. Such expressions are rejected rather than pretending to preserve
Java regex semantics. Generic raw-character regex continues to use authoritative
`java.util.regex.Pattern/Matcher` unchanged.

## Donor and algorithm catalogue

| Source / problem category | Reuse decision |
| --- | --- |
| Stanford CoreNLP TokensRegex | Token-sequence + annotated token matching model; reference only |
| Square T-Rex | Rich token-attribute pattern language / limits; reference only; no code copied |
| NLTK TokenSearcher | NLP token-boundary behavior reference only |
| Glushkov / followpos position automaton | Independently implemented with flat primitive lanes |
| LeetCode 10, HackerRank regex, GeeksforGeeks NFA/DFA | Taxonomy/complexity comparisons only; no challenge source copied |
| Existing M3RegexDerivative / M3RegexDfaEffects | Reuse conceptual bounded precompute authority; character alphabet remains separate |
| Existing M3Index/M3AST lexical geometry | Intended token producer; no runtime dependency or duplicate tokenization |

Useful references: https://github.com/square/t-rex ,
https://nlp.stanford.edu/software/tokensregex.html ,
https://www.nltk.org/api/nltk.text.TokenSearcher.html .

## Focused evidence and pending gates

Locally exercised with JDK 21.0.12.1:

```sh
javac -Xlint:all -Werror -d /tmp/m3-tokenclasses \
  m3/ports/precompute/src/main/java/com/m3/precompute/M3TokenTape.java \
  m3/ports/precompute/src/main/java/com/m3/precompute/M3TokenRegex.java \
  m3/ports/precompute/src/test/java/com/m3/precompute/M3TokenRegexSelfTest.java
java -cp /tmp/m3-tokenclasses com.m3.precompute.M3TokenRegexSelfTest
# M3TokenRegexSelfTest PASS
```

This proof includes randomized differential matches against Java Pattern for a
bijective small token alphabet, explicit leftmost-longest substring oracle,
kind-mask/identity checks, defensive source snapshotting, slice boundaries,
UTF-16 coordinates beyond 2GB, invalid grammar refusal, and a 150,000-token scan.

The owning Maven module additionally runs `M3TokenRegexTest` via JUnit. **Not
yet proven:** full reactor/JDK build, M3AST real lexer integration, capture
semantics, native C11/JNI parity, 100MB real tokenized JSON throughput,
GC/heap/performance measurements, or compiler/HotSpot promotion. A pattern
catalogue/reference alone never authorizes a product phase advance.
