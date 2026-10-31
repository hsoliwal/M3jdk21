<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Regex precompute, trial images and oracle contracts

This reference covers the complete public source bodies of `M3RegexShape`,
`M3RegexStringSignals`, `M3RegexStringTrialImage`, `M3RegexOracleCorpus` and
`M3RegexOracleHarness` in the independent `com.m3:m3-precompute` module, pinned to
[M3JDK21 `13a70cdbbe7b0abc75df18c78e1ca044e0c74240`](https://github.com/hsoliwal/M3jdk21/tree/13a70cdbbe7b0abc75df18c78e1ca044e0c74240/m3/ports/precompute).
The [source index](#pinned-source-index) records exact blob identities. Public
constructors, methods, record components and enum values are described below;
source-derived behavior is not an executed qualification result.

**Verification: NOT_RUN.** Four direct JUnit test bodies and the separate
`M3PatternQueryDifferential` driver were inspected. No Java example, Maven build,
JUnit/jtreg/native test, policy/recipe verifier or benchmark was executed for this
documentation. This is not whole-package completion or JDK runtime admission.

## Choose the right layer

| Owner | Preparation and retained result | What it does not establish |
| --- | --- | --- |
| `M3RegexShape` | Textual recognition of a nonempty literal, optionally bounded by `\A` / `\z` | General syntax validity, flags-aware matching, capture/state parity |
| `M3RegexStringSignals` | One-pair facts or ordered fact snapshots, with literal `String.indexOf` / equality results and feature observations | General-regex outcomes, mutation or promotion authority |
| `M3RegexStringTrialImage` | Eager finite Cartesian matrix: JDK `Pattern` outcomes plus approximate ranking signals | A new regex engine, runtime matching of unseen input, persistence/restore validation |
| `M3RegexOracleCorpus` | Regenerated ordered 10,000-expression × 64-input corpus and digest functions | Regex semantics or exhaustive dialect coverage |
| `M3RegexOracleHarness` | Ten trial-image shards, corpus-level root, candidate-pruning contradiction reports | A universal soundness proof, candidate implementation identity or operational performance evidence |

`Pattern` from the executing JDK is the authority for trial `matches`,
`lookingAt` and first `find` booleans. These methods are executed during
preparation. Subsequent image/harness queries read stored primitive cells; they
do not run the engine again. No image query exposes capture groups, match spans,
repeated-find progression, regions, bounds, replacement/split, `hitEnd` or
`requireEnd` state. The separate differential driver covers some additional
observations, not all of `Matcher`.

Code-text scores, SimHash distance, estimated Jaccard and difficulty rankings
nominate cases for further work. They cannot prove a regex match or a safe
rejection. A candidate claiming `CANNOT_MATCH` is checked against the oracle by
the harness; `MAY_MATCH` is not an assertion that a match exists.

See the [batch reference](precompute-batch-reference.md) for packed lexical
signals and JNI lifetimes, and [base precompute contracts](m3-packages.md#precompute-api-contracts)
for metrics, text histograms/sketches, edit-distance plans and progress.

## Literal shapes and dialect limits

[`M3RegexShape`](../../m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexShape.java)
accepts a nonnull `CharSequence` in `analyze(regex)` and calls `toString()` once.
A mutable/custom input must produce a stable, faithful spelling during that
conversion. The result retains that `String` and a decoded literal, not a
compiled `Pattern`.

`Kind` declares `LITERAL`, `STRICT_PREFIX`, `STRICT_SUFFIX`, `STRICT_EXACT`,
`GENERAL`. A leading `\A` and/or trailing `\z` is peeled off, and the remaining
body must decode to a **nonempty** literal:

- Ordinary non-metacharacters are admitted
- Escaped `. * + ? ( ) [ ] { } | ^ $` and backslash are admitted as literal units
- Balanced `\Q...\E` regions are decoded as quoted text
- Surrogates are admitted only as adjacent high/low pairs, including inside quoted text
- Regex operators, `^` / `$`, classes, groups, alternation, control/class/Unicode/hex escapes, trailing escapes, unclosed quotes, empty bodies and broken surrogate units produce `GENERAL`

This is a deliberately narrower recognizer than Java regex. `GENERAL` means
“no specialization recognized”; it does not mean invalid regex. `analyze` does
not call `Pattern.compile`, throw `PatternSyntaxException` for general syntax,
or accept an external flags argument. Do not apply its result under
case-insensitive, literal, comments or other flags as though it were
flags-aware. Inline flag syntax also remains general.

| Public method | Contract |
| --- | --- |
| `analyze(regex)` | New shape; null input gives `NullPointerException`; conversion failures propagate |
| `expression()` | Retained original spelling |
| `kind()` | One of the five enum values above |
| `specialized()` | Exactly `kind != GENERAL` |
| `literal()` | Decoded nonempty spelling for specialized kinds; `IllegalStateException` for `GENERAL` |

For a zero-flag shape, the trial image compares these predicted modes with the
actual JDK modes and fails the whole compilation on a disagreement:

| Kind | `matches` prediction | `lookingAt` prediction | `find` prediction |
| --- | --- | --- | --- |
| `LITERAL` | Input equals literal | Input starts with literal | Input contains literal |
| `STRICT_PREFIX` | Input equals literal | Input starts with literal | Input starts with literal |
| `STRICT_SUFFIX` | Input equals literal | Input equals literal | Input ends with literal |
| `STRICT_EXACT` | Input equals literal | Input equals literal | Input equals literal |

These are full-input/default-region comparisons. Their source implementation and
finite parity test assertions are documented here; exhaustive regex equivalence
is not claimed.

## Regex/String fact packets

[`M3RegexStringSignals`](../../m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexStringSignals.java)
uses immutable `String` arguments. Its `plainLiteral` classification is **not**
`M3RegexShape.specialized()`: it admits any spelling containing none of
`\ . ^ $ | ? * + ( ) [ ] { }`, including the empty string. It does not perform the
shape recognizer's surrogate validation. Quoted/escaped literals and strict
anchors therefore receive different treatment in these two APIs.

### Fact construction and accessors

`analyze(pattern, subject)` rejects nulls via `Pair`, scans the pattern for
features, computes `String.indexOf(pattern)` and `String.equals(pattern)` only
when `plainLiteral` is true, and returns `Fact`. It does not compile general
regexes or validate their syntax. The exact facts describe these `String`
operations; the explicit parity helper below is the JDK check for that pair.

`Feature` declares, in returned enum order: `ESCAPE`, `DOT`, `CHAR_CLASS`,
`GROUP`, `ALTERNATION`, `QUANTIFIER`, `ANCHOR`, `LOOKAROUND`, `BACKREFERENCE`,
`UNICODE_CLASS`, `INLINE_FLAG`, `QUOTED_REGION`. Feature detection is a lexical
scan, not a parser: backslash marks `ESCAPE` and consumes its next unit; `1`–`9`,
`p`/`P` and `Q` additionally mark backreference, Unicode class and quoted region.
It notices selected `(?...)` prefixes, `[` and metacharacters but does not keep
full class/quote/group state. Observations may describe punctuation inside
quoted/class text, and do not enumerate every valid Java regex construct.

| Public record / components (accessors use these names) | Factory result and direct-constructor checks |
| --- | --- |
| `Pair(pattern, subject)` | Both nonnull `String`s; no copying or syntax check |
| `Fact(patternSha256, subjectSha256, patternUtf16Units, patternCodePoints, subjectUtf16Units, subjectCodePoints, patternAscii, subjectAscii, subjectHasLineTerminator, features, plainLiteral, literalFindIndex, literalFullMatch, rootSha256)` | Three SHA fields must be lowercase 64-hex strings; four lengths nonnegative; `features` copied with `List.copyOf`; nonliteral `literalFindIndex` must be `Integer.MIN_VALUE` |
| `Snapshot(facts, plainLiteralFacts, rootSha256)` | Nonempty copied, nonnull-element fact list; count in `[0, facts.size()]`; root lowercase 64-hex |
| `LiteralParity(applicable, findMatched, findStart, expectedFindStart, fullMatched, expectedFullMatched, rootSha256)` | Nonapplicable starts must both be `Integer.MIN_VALUE`; root lowercase 64-hex |

Generated record accessors expose the listed values; generated equality/hash/
string representation use those components. Copied lists are immutable, and
null list elements are refused. **Direct construction does not authenticate the
facts:** constructors do not recompute roots, derive flags/counts, verify actual
literal count, bound a literal index against a subject, or generally establish
cross-field consistency. SHA-shaped text alone is not a verified content seal.

For factory-produced `Fact` values:

- Lengths are Java UTF-16-unit length and `String.codePointCount`; unpaired units count as individual code points
- ASCII means every code point is at most `0x7f`; empty strings satisfy it
- Line terminators mean LF, CR, NEL (`0x85`), line separator (`0x2028`) or paragraph separator (`0x2029`)
- Literal find returns a UTF-16 offset, `-1` if absent; an empty pattern uses ordinary `String.indexOf("")` behavior
- Nonliteral find is `Integer.MIN_VALUE`, not `-1`; its factory-produced full-match flag is false because no executable fact was calculated
- `Fact` and `Snapshot` each expose `semanticAuthority()`, `mutationAuthority()` and `promotionAuthority()`, all unconditionally false

### Ordered snapshots, parity and hashing

`analyze(List<Pair>, monitor)` shallow-copies the list, rejects null pairs and
requires 1–100,000 pairs. It preserves pair order and duplicates, analyzes each
pair, counts plain-literal facts and derives a snapshot root. The bound is pair
count only: there is no character-byte budget, per-pair time limit or cap on
individual `String` length. A null monitor means `M3Progress.none()`.

Progress begins with the pair count, checks cancellation at indices divisible
by 256 including zero, reports one worked unit per fact and checks again before
return. `done()` runs in `finally` after successful entry past `begin`; a
throwing `begin` has no paired `done`. Cancellation cannot interrupt an ongoing
single-pair analysis. Callback exceptions propagate; a throwing `done` can
replace an earlier exception.

`verifyPlainLiteralAgainstJdk(pattern, subject)` first analyzes the pair. For
nonliterals it returns `applicable=false`, both start sentinels `Integer.MIN_VALUE`
and false booleans, without compiling the supplied regex. `LiteralParity.passed()` returns
true for such a nonapplicable result; this is **not a tested match**. For literals
the helper performs `Pattern.compile(pattern)` twice, using one matcher for
first `find`/start (`-1` on absence), another for `matches`, both with flags zero.
It compares them with the stored String facts and throws `IllegalStateException`
on disagreement. Pattern/allocation/runtime failures otherwise propagate.
`passed()` for applicable records compares find presence, start and full-match
booleans; hand-authored records need not have been checked against any engine.

Pattern/subject digests hash the exact UTF-16 units as high byte then low byte,
without a BOM; unpaired units remain distinct. Fact and parity roots use
versioned domain strings and UTF-8 fields, each preceded by a four-byte
big-endian encoded-byte length. Facts include the two UTF-16 hashes, geometry,
flags, ordered features (names separated by U+001F), classification and literal
results. Snapshot roots frame `M3_REGEX_STRING_SNAPSHOT_V2`, pair count, then
fact roots in order. Content/order identity is not owner-object, slice-range,
Unicode-normalization or JDK-version identity.

## Eager trial image compilation

[`M3RegexStringTrialImage`](../../m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexStringTrialImage.java)
compiles all requested regex × input pairs eagerly. Its private constructor and
private arrays retain only primitive facts and a root string. Original source
texts, `Pattern`s, matchers and signal snapshots are preparation inputs/scratch,
not retained image fields. There is no close/dispose/native resource owned by
the resulting image.

### Specifications, flags and limits

`RegexSpec(expression, flags)` requires a nonnull expression and rejects flag
bits outside `UNIX_LINES`, `CASE_INSENSITIVE`, `COMMENTS`, `MULTILINE`, `LITERAL`,
`DOTALL`, `UNICODE_CASE`, `CANON_EQ`, `UNICODE_CHARACTER_CLASS` with
`IllegalArgumentException`. `RegexSpec(expression)` supplies zero flags.
`expression()` and `flags()` are record accessors. This validates the bit mask,
not regex syntax or safety/cost. Regex meaning remains the executing JDK's
`Pattern` meaning, including inline flags.

`Limits(maxRegexes, maxStrings, maxPairs, maxUtf16Units, maxPrimitiveBytes)` exposes
those five record accessors. Counts/pairs/bytes must be positive; the UTF-16 cap
may be zero but not negative. `Limits.DEFAULT` is:

| Budget | Default |
| --- | ---: |
| Regex rows | 4,096 |
| String rows | 65,536 |
| Cartesian pairs | 4,000,000 |
| Sum of input expression and subject UTF-16 units | 16,000,000 |
| Accounted retained primitive image bytes | 96 MiB (`96L << 20`) |

`compile(regexes, strings, monitor)` selects Java lexical signals and default
limits. `compile(regexes, strings, signalBatch, limits, monitor)` uses the explicit
provider/limits. Both lists and their elements must be nonnull; provider and
limits must be nonnull. Empty row lists are refused. Limits are checked before
Cartesian array allocation:

1. Regex and string row counts against their caps
2. Exact long product `R * S` against `maxPairs` and `Integer.MAX_VALUE`
3. Exact sum of expression/input `length()` against `maxUtf16Units`
4. `31 * R + 8 * S + 7 * R * S` against `maxPrimitiveBytes`

Budget failures are `IllegalArgumentException`; exact-arithmetic failures
propagate as `ArithmeticException`. `primitivePayloadBytes()` later returns the
same formula. It counts retained arrays only: regex flags/validity/shape/error,
regex/string scores, per-regex mode counts/class masks, pair outcome/sketch
bytes and pair difficulty ints. It excludes object/array headers, root string,
lists, text, Pattern internals, histogram/sketch preparation, JNI scratch and
ranking allocations. These limits are not a whole-call heap/CPU/time bound.

Preparation runs `M3CodeTextSignals.compileBatch` separately for regex texts and
input texts, using batch limits of `maxPrimitiveBytes` transfer bytes,
`maxUtf16Units` packed units and `max(R,S)` rows. Each batch also applies its own
transfer formula (`2 * packed units + 16 * rows`) and JVM geometry checks, so a
trial geometry that passes the four checks can still fail a provider check.
The batch facade allocates/fills its packed arrays before provider validation.

### Input stability, engine work and cancellation

List copies stabilize membership, **not mutable `CharSequence` contents**.
Subject `length()`/`charAt()` and `toString()` are used at different preparation
stages; `toString()` is called again for valid pairs and root construction.
Supply immutable Strings or externally stabilize faithful custom sequences for
the entire compilation. The API does not enforce an atomic text snapshot,
owner/range binding, invalidation or concurrent mutation protocol.

For each regex, compilation records the textual shape regardless of flags,
then calls `Pattern.compile(expression, flags)`. A `PatternSyntaxException`
marks the row invalid and retains its `getIndex()`; it does not fail the whole
image. Other errors are not broadly caught. Syntax-error index `-1` can mean a
valid row or an unavailable error position, so always read `regexValid`.

For each valid pair, one matcher performs `matches`, `reset`, `lookingAt`,
`reset`, first `find`. With flags exactly zero and a specialized shape, its
three results must agree with the shape predictions or compilation throws
`IllegalStateException`. A nonzero-flag row may still have a specialized
`regexKind`; the parity shortcut is disabled and the label is not a flags-aware
execution guarantee. Invalid rows still receive similarity/difficulty facts;
they are not silently treated as ordinary false matches.

Progress starts with `R*S + R + S`. The two signal batches use **no-op monitors**,
then report `R` and `S` units to the outer monitor. Shape/Pattern compilation has
no cancellation check. The pair loop checks at flattened ordinals divisible by
256 including zero, reports one worked unit per pair, then checks once before
root construction. There is no cancellation during root hashing, a matcher
operation, feature preparation or a custom provider call. `done()` uses the
same begin-before-try/finally behavior described above. No regex timeout or
protection against a pathological Pattern execution is provided.

## Trial queries, masks and ranking

Row coordinates are zero-based. Pair ordinal is `regexRow * stringCount +
stringRow`; decoding is integer division/modulus by string count. Row/pair
bounds are explicitly checked with `IndexOutOfBoundsException`.

| Public query | Result / exception contract |
| --- | --- |
| `regexCount()`, `stringCount()`, `pairCount()` | Dimensions; pair count is an int equal to retained cell count |
| `rootHash()`, `primitivePayloadBytes()` | Derived root and retained primitive accounting described here |
| `regexValid(r)`, `regexFlags(r)`, `regexSyntaxErrorIndex(r)`, `regexKind(r)` | Validity, requested external flags, Pattern error index and textual shape; flags do not include a separately discovered inline-flag mask |
| `regexCodeScore(r)`, `regexRegexScore(r)`, `stringCodeScore(s)`, `stringRegexScore(s)` | Stored lexical heuristic scores, not syntax or semantic decisions |
| `regexMatchesCount(r)`, `regexLookingAtCount(r)`, `regexFindCount(r)` | Number of input rows whose respective mode succeeded; all zero for invalid regexes |
| `regexOutcomeClassMask(r)` | Unsigned eight-bit set of observed whole-cell outcome values; zero for invalid rows |
| `regexOutcomeDiversity(r)` | Bit count of that class-set mask |
| `regexDifficultyScore(r)` | Saturated regex ranking score described below |
| `matches(r,s)`, `lookingAt(r,s)`, `find(r,s)` | Stored booleans; invalid regex row throws `IllegalStateException` before string-row validation |
| `simHashDistance(r,s)` | Unsigned stored Hamming distance, normally 0–64 from the text sketch; available for invalid regex rows |
| `estimatedJaccard(r,s)` | Unsigned stored byte divided by 255; available for invalid regex rows |
| `difficultyScore(r,s)` | Stored pair ranking score, also available for invalid rows; expanded-limit overflow caveat below |
| `regexRowOfPair(p)`, `stringRowOfPair(p)` | Decode a valid flattened ordinal; reject out-of-range ordinals |

There are **two distinct masks**. Public mode constants are `MATCHES=1`,
`LOOKING_AT=2`, `FIND=4`. Internally a cell ORs successful mode bits. A regex's
outcome-class mask instead sets `1 << cellBits` for each valid pair: an all-false
cell contributes bit 0 (`1`), while a three-mode-success cell contributes bit 7
(`128`). Test `mask & (1 << (MATCHES | LOOKING_AT | FIND))`, not `mask & MATCHES`,
to ask whether some input succeeded in all three modes. No public raw-cell-mask
getter is declared.

Similarity compares regex **spelling** with input text, not the language denoted
by the regex. The base MinHash estimate uses 16 lanes (with special handling of
empty texts); the trial rounds `estimate * 255` to an integer byte. Thus its
returned fraction is quantized again and need not equal the original estimate.
Provider output shape is checked by the batch facade, but a custom provider's
semantic parity is not independently certified by trial compilation.

Under default limits, pair difficulty is the upper-capped sum of regex regex-score, input code-score,
24 for `GENERAL` (otherwise 4), 32 for invalid regexes, `64 - SimHashDistance`,
and integer `JaccardByte / 4`; add 16 if one or two modes succeed, 12 for
`string.likelyCode()` and 12 for `regex.likelyRegex()`. Both likely predicates
mean score at least 8. Regex difficulty is saturated
`regexRegexScore + regexCodeScore / 4 + 64 * outcomeDiversity + (invalid ? 32 : 0)`.
The scores are formulas, not measured runtime cost or probabilities.

The pair formula first adds `regex.regexScore() + string.codeScore()` as an
**int**, before a later term promotes the sum to long. The default text budget
keeps the derived keyword/lexical scores below this overflow boundary. Expanded
caller limits can permit an intermediate negative overflow; the final
`Math.min(Integer.MAX_VALUE, score)` is an upper cap, not a nonnegative clamp or
an overflow repair. Do not generalize the finite test's nonnegative-score
assertions to every custom limit. Regex-row difficulty instead introduces long
arithmetic in its initial addition.

`topPairs(limit)`, `topRegexRows(limit)` and `topPairsForRegex(regexRow, limit)`
return **new mutable int arrays** of flattened pair ordinals, regex-row
ordinals and flattened pair ordinals respectively. The per-regex result is not
a list of string-row ordinals; decode it explicitly. All order by decreasing
score with lower original ordinal first on ties. Negative limits are refused
with `IllegalArgumentException`; the result is truncated to available size.
Per-regex row validation happens before limit validation. Even limit zero
allocates/sorts the full relevant long-key array: all pairs, all regexes or all
strings in the selected row. These are full-sort operations, not bounded-memory
streaming top-k, and expose no monitor.

## Frozen corpus and oracle harness

### Corpus generation and roots

[`M3RegexOracleCorpus`](../../m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexOracleCorpus.java)
declares `REGEX_COUNT=10_000`, `STRING_COUNT=64`, `SHARD_COUNT=10`,
`REGEX_PER_SHARD=1_000`, plus `REGEX_ROOT`, `STRING_ROOT`, `MATRIX_ROOT`.
`expressions()` regenerates the ordered 10 bases × 10 repeats × 10 wrappers ×
10 tails, checks cardinality, uniqueness and pinned regex digest, then returns
an immutable list. `strings()` similarly regenerates 4 prefixes × 4 cores ×
4 suffixes, checks 64 distinct inputs and the pinned string digest, and returns
an immutable list. They are not memoized accessors. Drift throws
`IllegalStateException` with cardinality/root markers.

The generated cases include literals, selected classes/quantifiers/groups,
anchors, quoting, newline, NUL, `é` and code-looking text. They do not enumerate
all Java regex syntax, flag combinations, Unicode, malformed inputs or lengths.

`shard(expressions, shard)` requires list size 10,000 and shard in `[0,10)` or
throws `IllegalArgumentException`. It returns `subList(shard*1000,
(shard+1)*1000)`: a view, not a defensive copy or hash/uniqueness validation of
an arbitrary caller list. Its mutability/backing behavior follows that list.
Null inputs are not supported.

`regexRoot(regexes)` and `stringRoot(inputs)` accept arbitrary ordered lists and
hash a domain line followed by one `UTF16Length:spelling\n` row per element.
`matrixRoot(regexes, inputs)` hashes the matrix domain, those two roots and
literal `3` (the three trial modes), newline-delimited. They derive roots; they
do not enforce corpus count/uniqueness. All use SHA-256 over Java UTF-8 encoded
material, returning lowercase hex.

| Public pinned constant | Value |
| --- | --- |
| `REGEX_ROOT` | `215d0e2d2fdf7b8491cd749fdae29ef254c5bec467e47d898e3158d26617b8a6` |
| `STRING_ROOT` | `4785a7e1ab13050473f5b42f4a31fb241ff1adba6f60dd1fae1d3a5e140ef069` |
| `MATRIX_ROOT` | `e7614598149534e42ea1ff5af8c00dfea1c440ff3d6d94864123974f8c11f998` |

### Harness compilation and access

[`M3RegexOracleHarness.compile(monitor)`](../../m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexOracleHarness.java)
regenerates those lists and constructs ten 1,000 × 64 trial images with default
limits, Java signals and zero-flag `RegexSpec`s. This is 640,000 pairs and three
mode observations per pair, not 640,000 independent regex programs. The same
monitor is passed to each shard, producing ten separate trial begin/done cycles;
there is no aggregate harness progress scope or caller-adjustable limits/provider.
Failure aborts construction without a partial harness result.

Public queries are:

- `regexCount()`, `stringCount()`, `pairCount()` (long), `shardCount()`
- `matrixRoot()` (the fixed corpus constant), `oracleRoot()` (derived below)
- `shard(shardOrdinal)` (the retained image), `expression(regexOrdinal)`, `input(stringOrdinal)` (immutable spellings)
- `regexValid(regexOrdinal)`, `matches(regexOrdinal,stringOrdinal)`, `lookingAt(...)`, `find(...)`, delegated to the appropriate shard

Global regex ordinal is mapped with division/remainder by 1,000. These APIs do
not wrap every index with one uniform validator: list/array bounds exceptions or
the trial row checks propagate. Invalid regex outcomes, if present, retain the
trial refusal contract. The stored shard is safely encapsulated by its public
scalar/ranking APIs; no backing arrays are exposed.

`oracleRoot` hashes `M3_REGEX_ORACLE_HARNESS_V1`, the matrix constant, shard count
and each shard root in order. It covers trial outcomes **and ranking data**. It
can change when the executing Pattern implementation or signals change. It
does not explicitly encode a JDK build or provider identity.

### Candidate checks and reports

`Verdict` declares `CANNOT_MATCH` and `MAY_MATCH`. Functional interface
`Candidate.classify(String regex, String input)` supplies one verdict per pair.
`neverPrunes()` always returns `MAY_MATCH`. `literalShape()` analyzes each regex
for every callback and uses contains/startsWith/endsWith/equals according to
shape; `GENERAL` returns `MAY_MATCH`. It has no flags parameter or general-regex
fallback of its own. Its intended scope here is the harness's zero-flag corpus.

`check(candidateName, candidate, maxRetained)`:

1. Rejects null candidate and negative retention cap before iteration
2. Traverses every pair in regex-major, input-minor order, counting true `find` cells
3. Calls the candidate once per pair; only `CANNOT_MATCH` triggers pruning classification (even a null returned verdict follows the non-pruning branch)
4. Increments `pruned` if the claim agrees with false `find`; increments `unsound` if `find` is true
5. Keeps only the first `maxRetained` contradictions, without stopping remaining checks
6. Returns a root-bound `Report`; null `candidateName` is rejected by that constructor after callbacks have already run

Candidate exceptions abort the check. There is no monitor, cancellation token,
callback timeout or callback-effect restriction. The cap bounds retained
mismatch objects, not work. `pruned` excludes unsound claims; it is not the total
number of `CANNOT_MATCH` verdicts.

`Mismatch(regexOrdinal, stringOrdinal)` is a public record with those accessors;
its direct constructor does not validate signs or dimensions.
`Report(oracleRoot, candidateName, pairs, findTrue, pruned, unsound, retained, root)`
exposes those eight record accessors and `sound()`, exactly `unsound == 0`.
It requires nonnull `oracleRoot` and `candidateName`, copies the mismatch list (rejecting null elements),
and derives an expected SHA-256 from a versioned domain, root/name, decimal
counts and ordered ordinal pairs. Null/blank `root` is filled with the expected
value; a supplied nonmatching root throws `IllegalArgumentException`.

This is stronger than the fact-record constructors' format-only root checks,
but is still not report authenticity. It does not validate root/name format,
nonnegative/coherent counts, mismatch coordinate bounds/order or candidate
implementation identity. The name is a caller label. A direct constructor can
seal inconsistent data, and identical reports do not certify identical candidate
code. `sound()` means no recorded contradiction in this finite check; it is not
universal soundness outside this corpus, flags, execution or callback behavior.

## Encoding identity, persistence and invalidation limits

The fact packet's pattern/subject digests preserve every UTF-16 unit. Trial
image roots instead frame expression and subject **UTF-8 encodings** with
four-byte big-endian byte lengths; root fields include row counts, expression
flags, validity/shape/error, all exposed scores/counts/class masks, raw outcome/
Hamming/Jaccard bytes and big-endian difficulty ints. The domain is
`M3_REGEX_STRING_TRIAL_IMAGE_V2`. Input order and flags participate in identity;
limits, monitor, provider name and JDK build do not. Different providers with
equal resulting facts/outcomes can produce the same root.

Java's UTF-8 conversion replaces malformed surrogate sequences. Accordingly,
the trial root's text framing and the corpus's arbitrary-list root helpers are
not injective encodings of arbitrary UTF-16 spellings. Additional hashed fields
do not justify claiming a universal exact-UTF-16 identity guarantee. None of
these content hashes proves semantic equality or binds canonical M3String owner,
generation, range or backing-object identity.

There is **no public encode/decode, serialized image loader, schema admission,
restore validator or persistent cache API among these five owners**. A root
hash is not a serialized format. No checksum/malformed-file rejection or
post-load recompilation proof can be attributed to `M3RegexStringTrialImage`.
The checks actually present are input/flag/geometry checks, Pattern syntax
classification, zero-flag specialized parity, corpus generator drift checks,
fact-record format checks and report-root recomputation. Caller persistence,
invalidation and reuse against another corpus/JDK would need an explicit
separately qualified protocol; this document does not invent one.

## Build, native boundary and evidence

The module [POM](../../m3/ports/precompute/pom.xml) targets Java 21, has JUnit
Jupiter 5.10.2 as a test dependency, and compiles with `-Xlint:all -Werror`,
annotation processing disabled. These five production owners import only JDK
APIs and same-module owners. Optional Synexia OpenRewrite recipe dependencies
are build-plugin profile tooling, not production runtime dependencies.

Default trial compilation uses `M3CodeTextSignalBatchJava.INSTANCE`. The explicit
provider overload can use `M3CodeTextSignalBatchNative.loadRequired()`; it does
not automatically choose native or fall back after an explicit native failure.
Native loading uses the configured path, path-file or library lookup and may
fail. The inspected C header/JNI bridge expose lexical code-text and similarity
signals, **no regex matching entry point**. JNI copies bounded row descriptors
and UTF-16 contents into call-scoped C scratch; regex engine work stays in Java.
An `accelerated()` label or equal source formulas is not a measured speedup or
an executed platform parity result.

The `native` Maven profile configures/builds CMake and runs CTest, then supplies
the generated native path-file to JUnit. CMake requires JNI headers and a C11
compiler. Read the module's [build instructions](../../m3/ports/precompute/README.md#build)
and [native boundary reference](precompute-batch-reference.md#native-opt-in-loading-and-resource-lifetime)
before running. No command below was executed here:

```sh
mvn -B -ntp -f m3/ports/precompute/pom.xml \
  -Dtest=M3RegexShapeTest,M3RegexStringSignalsTest,M3RegexStringTrialImageTest,M3RegexOracleHarnessTest test
mvn -B -ntp -f m3/ports/precompute/pom.xml -Pnative \
  -Dtest=M3RegexStringTrialImageTest,M3NativeParityTest test
```

### Inspected test assertions and remaining gaps

| Source test, NOT_RUN | Assertions actually present | Important limits |
| --- | --- | --- |
| [M3RegexShapeTest](../../m3/ports/precompute/src/test/java/com/m3/precompute/M3RegexShapeTest.java) | Four shape kinds, escaped punctuation, quoting, supplementary pair; empty/syntax/control-escape/broken-surrogate refusal | Finite examples; no flags-aware API or exhaustive Pattern equivalence |
| [M3RegexStringSignalsTest](../../m3/ports/precompute/src/test/java/com/m3/precompute/M3RegexStringSignalsTest.java) | One literal's index/parity and false authority flags; selected general features; deterministic ordered snapshot and basic progress totals | No malformed-surrogate literal parity, constructor-corruption matrix, 100,000 boundary or throwing/canceling monitor assertions |
| [M3RegexStringTrialImageTest](../../m3/ports/precompute/src/test/java/com/m3/precompute/M3RegexStringTrialImageTest.java) | 16 × 16 finite cases against Pattern; invalid-row refusal; score ranges; global/regex ordering; repeated/reordered roots; one pair-budget refusal | Case-insensitive, multiline and dotall examples, not all accepted flags; no direct mask/count/per-regex-top validation, all budget edges, mutable inputs, cancellation, ranking memory or hostile Pattern cost |
| Same trial test's native method | Java/native roots and top-pair arrays compared when a native path/path-file is set | Method returns immediately when neither property is set; ordinary test completion is not evidence that JNI executed |
| [M3RegexOracleHarnessTest](../../m3/ports/precompute/src/test/java/com/m3/precompute/M3RegexOracleHarnessTest.java) | Corpus roots/counts/shard rejection; validity of all generated regexes; all-input Pattern spot checks every 67th expression; repeat-root assertion; never-prune/literal-shape/always-prune reports; bounded ordered ledger and wrong-root/negative-cap refusal | Corpus-derived, not universal soundness; expected oracle root is labeled in source as pinned from JDK 21.0.9, not an execution result from this pass |
| [M3PatternQueryDifferential](../../m3/ports/precompute/src/test/java/com/m3/precompute/M3PatternQueryDifferential.java) | Standalone `main`: String versus custom CharSequence first-find result/spans/`hitEnd`/`requireEnd`, digest and optional reflective M3TQ counters | Not a JUnit `@Test`; must compare actual stock and patched-JDK executions; unavailable reflection yields `gate=ABSENT`, not proof of a gate |

Further qualification must separate source checks from executed Java/native
parity, source/recipe fixed point, target JDK/jtreg behavior and cold/warm CPU,
retained heap and process-memory evidence. No simulated flag, test name, finite
corpus count, copied provenance file or successful documentation check substitutes
for those receipts. The two public regex receiver verifier scripts were read;
they check policy/provenance/hash-pinned source relations, not engine execution,
and were NOT_RUN here. The full-borrow receiver verifier was also NOT_RUN for
this source-only documentation pass.

## Pinned source index

All five full bodies were fetched from the public repository at the pin above,
verified against their Git blobs and matched to the package inventory. Direct
tests/build/native support were read at that pin; existing public support caches
were reused only where the current tree blob matched. No private donor source
was used. Public target provenance records preserve the Synexia lineage without
turning it into a runtime dependency or an admission claim.

| Production file in `m3/ports/precompute/src/main/java/com/m3/precompute/` | Git blob |
| --- | --- |
| `M3RegexShape.java` | `ff989cfa42d1fe29853a4850d474a8c007974718` |
| `M3RegexStringSignals.java` | `e2b62034d7ba9de17fefb8c156b185a9c2011f13` |
| `M3RegexStringTrialImage.java` | `ffce8a213e92ed460b0d50074c02dd8eb941aeb9` |
| `M3RegexOracleCorpus.java` | `cbafa1bca87f81f125386cadd97d7cf2c61f53f8` |
| `M3RegexOracleHarness.java` | `64dd809ace88e97f91067b64ddcb1b1f422a23b8` |

Policy and lineage: [AGENTS](../../AGENTS.md),
[porting invariant](../../m3/docs/m3jdk21-porting-invariant.md),
[naming map](../../m3/docs/name-mapping.json),
[full-family pin](../../m3/compatibility/synexia-full-family-pin.tsv),
[catalogue](../../m3/docs/synexia-full-family-catalogue.json),
[current receiver snapshot](../../m3/synexia-import/current-full-borrow/README.md),
[regex/String provenance](../../m3/ports/precompute/REGEX_STRING_V2_PROVENANCE.tsv),
[oracle provenance](../../m3/ports/precompute/REGEX_ORACLE_HARNESS_PROVENANCE.tsv).
Preserve first-party Apache-2.0 attribution and existing OpenJDK/third-party
rights. Documentation does not advance the ordered receiving phase or establish
new source-reuse, promotion or public JDK API authority.
