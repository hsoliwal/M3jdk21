<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Trigram query and exact membership facts

[Guide](README.md) · [Pattern integration](bootstrap-pattern-query-contracts.md) · [String facts](bootstrap-string-precompute.md) · [Mapped backing](mapped-string-storage-reference.md)

This reference covers the complete [M3TQ.java](../../src/java.base/share/classes/jdk/internal/mindex/M3TQ.java) source body: the `M3TQ` query, `Op`, `Facts`, the temporary backing reader and all composition/search/rendering helpers. The reviewed public source is `e7d21edf0f998c3c53ab0d0909972a4a97dfc471`, blob `533a07b77061e757d476627557413f6df1067d34`, unchanged from the P03 inventory at `278b221e8365ebeb6095a3a5643b38a0564258b5`.

`TQ` means Trigram Query. This owner describes necessary conditions on UTF-16 three-unit membership, not class metadata search and not a serialized query image. It is internal to the unexported `jdk.internal.mindex` package of `java.base`. There is no dependency on donor modules or an optional native backend in M3TQ. Public `String`, `Pattern` and `Matcher` remain the API authorities.

The current source and actual tests below were read. No repository build, compiler, recipe/policy verifier, Java/native test, benchmark or runtime was executed for this documentation pass. Exact source membership is not runtime qualification or full-family acceptance.

## Necessary conditions and code-unit identity

`trigram(char first,char second,char third)` packs the three unsigned UTF-16 units into a nonnegative 48-bit `long`: first in bits 32–47, second in 16–31, third in 0–15. It does not decode Unicode code points, normalize, case-fold, encode UTF-8 or combine surrogate pairs. NUL, unpaired surrogates and seam-split pairs are ordinary code units for this representation.

For a correctly derived requirement and facts for the exact eligible text/range, a false query can reject a candidate. A true query only says the necessary membership condition holds. Sorted unique trigrams lose position, order and multiplicity, so they cannot establish text equality, a substring, regex acceptance, capture groups, match counts or spans. For example, `abc-bcd` contains both trigrams required by `abcd` without containing that substring. Facts for a longer repeated needle can be a subset of a shorter value's facts.

The arbitrary `LongPredicate` entry point does not itself verify exact membership or prove that the query is a sound requirement for any regex. Those obligations belong to its caller/producer. Use a stable, side-effect-free membership predicate for the intended necessary-condition semantics. The factories normalize and deduplicate conditions; they do not preserve arbitrary callback effects as if evaluating the caller's original Boolean expression.

## Query construction and normalization

The enum values are `ALL`, `NONE`, `AND`, `OR`. Private static singleton queries implement ALL/NONE with empty trigram arrays and empty immutable subquery lists. The private constructor requires its inputs, clones/sorts the trigram array and takes `List.copyOf(subqueries)`. There is no public arbitrary-array constructor.

| Entry point | Exact behavior |
| --- | --- |
| `all()`, `none()` | Return shared always-admit/always-reject query objects |
| `fromExact(List<String>)` | Requires the list; empty alternatives yield NONE. Each visited string is required non-null. Any visited alternative shorter than three UTF-16 units makes the result ALL, because that alternative can match without a trigram. Otherwise every alternative is an AND of its distinct trigrams, and the alternatives are OR-combined |
| `and(List<M3TQ>)` | AND normalization; empty conjunction is ALL; NONE absorbs, ALL is omitted |
| `or(List<M3TQ>)` | OR normalization; empty disjunction is NONE; ALL absorbs, NONE is omitted |
| Private `combine(Op,List<M3TQ>)` | Rejects a non-AND/OR operation, requires input and each reached term, flattens children with the same operation and lifts opposite-operation singleton-trigram leaves into the local key list. Sorts and deduplicates local keys; keeps other children in encounter order. No remaining terms returns the identity; exactly one child and no local keys returns that child |

Validation is traversal-dependent: `fromExact` returns at the first short alternative, and `combine` returns at an absorbing ALL/NONE. A later null element may therefore never be examined. The factories do not promise eager validation of every list element. Mutating a caller's list after a successful construction cannot change the stored immutable list, but concurrent input mutation during construction is not synchronized.

Repeated local keys disappear; structurally equal nested subqueries are not globally deduplicated. There is no distributive rewrite, language-equivalence check, global interner, prepared-query registry or persistent identifier. `M3TQ` and `Facts` do not override `equals`/`hashCode`; object identity is distinct from observationally equal query/fact content.

`uniqueTrigrams(CharSequence)` is a package-private construction helper: it requires the sequence, allocates at most length−2 raw keys, reads each overlapping triple, sorts and deduplicates. Unlike the single-pass fact scanner, it may reread a unit in adjacent triples. Its current `fromExact` use receives immutable Strings. It has no independent budget parameter.

## Query inspection and evaluation order

| Entry point/retained field | Contract |
| --- | --- |
| Private final `op`, `trigrams`, `subqueries` | Immutable query structure after construction; local trigrams sorted numerically and nested list immutable |
| `op()` | Returns the enum |
| `trigrams()` | Defensive copy, including empty arrays |
| `subqueries()` | Returns the stored immutable list; its query objects are immutable |
| Package-private `trigramsForReadOnlyKernel()` | Returns the actual private array to trusted same-package code under its read-only contract; this is not the defensive public export |
| Package-private `subqueriesForReadOnlyKernel()` | Returns the same immutable list |
| `hasConstraints()` | `op != ALL`; NONE is constrained even though it has zero terms |
| `termCount()` | Counts local key entries plus recursive child term counts with `Math.addExact`; ALL/NONE count as zero. It counts repeated child occurrences and is not the number of distinct keys across the whole graph |
| `test(LongPredicate)` | Requires the predicate even for ALL/NONE. ALL returns true and NONE false without invoking it. AND tests local sorted keys before children and stops at the first false; OR uses the same local-before-child order and stops at the first true. Child evaluation is recursive in list order |
| `testPrecomputed(Facts)` | Requires facts and delegates to `test`; it does not add an exact regex verifier |
| `toString()`, private `render`, private `quote` | ALL/NONE render literally. Other queries render quoted three-unit keys joined by ` & ` or ` | `, with child queries parenthesized. Quotes/control units are not escaped; this is diagnostic text, not a parser-compatible interchange format |

Callback exceptions propagate at the visited term; no recovery, replay, callback cache or automatic fallback is installed. Normalization can change callback count/order from an unnormalized expression. Reused children can be visited repeatedly; `test`, `termCount` and rendering have no query-node, recursion-depth, unfolded-work or cancellation budget. Very deeply nested alternating queries can exhaust stack/resources. Consumer-level limits, such as the compiled-node admission checks in `M3PatternQuery`, are separate from this owner and do not constitute a general M3TQ complexity bound.

## Fact admission and input lifetime

All three `precompute` overloads return immutable, caller-owned `Facts`. Admission budgets are numbers of UTF-16 units, not bytes, resident memory, unique-key limits or elapsed time.

1. `precompute(CharSequence value,int maxUtf16Units)` requires the source, reads its length, rejects negative length/budget or length above budget, then scans the whole sequence
2. `precompute(CharSequence value,int from,int to,int maxUtf16Units)` requires the source, validates the half-open range with `Objects.checkFromToIndex(from,to,value.length())`, checks the range-length budget, then scans only `[from,to)`
3. `precompute(M3StringBacking backing,long id,int from,int to,int maxUtf16Units)` requires the backing and creates a temporary anonymous `CharSequence` that delegates `length` to `backing.length(id)` and `charAt` to `backing.charAt(id,index)`. Its `subSequence` throws `UnsupportedOperationException`, and preparation never calls it. It delegates admission/scanning to the range overload

Null, range and budget checks precede any `charAt` read and the key-array allocation. They do not precede the source's `length()` call; the backing overload also creates its small temporary reader before delegated validation. Length, backing-ID and backing-lifetime errors propagate according to those underlying methods. A failed source read does not return partial facts, but there is no rollback of caller-supplied `CharSequence` side effects.

`scanFacts` allocates `long[max(0,length−2)]`, reads each admitted unit exactly once with a rolling 48-bit window, keeps at most two prefix/suffix units, sorts the keys and uses `compactFactKeys` to remove duplicates. No out-of-range unit, `subSequence`, String materialization, encoding or backing view is needed. Cold work is O(n) reading plus key sorting; the temporary/output arrays can exceed the final unique-key footprint. Empty and one-/two-unit values have no keys but retain the length/boundary information needed for later concatenation.

The caller must keep the input stable throughout the scan. A backing must remain open and immutable throughout it; no lock, lease, retain/release, close or generation capture is added by M3TQ. Returned facts do **not** retain the sequence, backing, ID, owner, generation, from/to coordinates, regex flags or compiled query. Associate reuse externally with the exact immutable owner/generation/value/range and applicable query options. Equal numeric IDs in unrelated owners are not shared identity; equal hash values do not prove a safe association.

Facts for a range describe that isolated range. Transparent lookarounds or another consumer that can observe text outside it need separately justified constraints/scope. A closed/replaced backing cannot make old facts an authority for a new value simply because the coordinate is unchanged. Already prepared facts remain standalone immutable numbers, but validity of their association is the caller's obligation.

## Facts surface and adaptive containment

`Facts` is public/static/final and implements `LongPredicate`. Its private constructor adopts freshly owned keys; no public constructor accepts mutable arrays. Private final fields are `utf16Length`, packed `prefix`, packed `suffix`, and sorted unique `keys`. There is no arbitrary slice-recovery API or retained spelling store.

| Entry point | Contract |
| --- | --- |
| `utf16Length()` | Length of the exact prepared/concatenated value in code units |
| `keyCount()` | Distinct trigram count, not occurrence count |
| `keys()` | Defensive array clone |
| `test(long key)` | Binary-search membership in the exact key set. Values outside the nonnegative 48-bit domain simply miss; there is no separate range exception |
| `containsAll(Facts required)` | Requires the argument; same object or empty requirement returns true. Fewer source distinct keys than required returns false. Otherwise uses the exact adaptive set test below |
| `concat(Facts right,int maxUtf16Units)` | Ordered metadata-only composition, described next |

For `containsAll`, `searchDepth = 32 − numberOfLeadingZeros(sourceKeyCount)`. If `2L * requiredKeyCount * searchDepth < sourceKeyCount`, it tests each required key with binary search, with a first-source-key singleton shortcut. Otherwise it performs a two-pointer merge. These paths are O(m log n) and O(n+m) respectively and do not allocate new fact arrays or read source payload. Predicate membership and subset truth are exact for these sets; substring/regex acceptance remains only a necessary condition.

There is no length comparison in `containsAll`. An empty required key set succeeds regardless of its represented length, and order/multiplicity have already been discarded. Do not use `containsAll` as equality or as a complete minimum-length/substring test. Consumers that need such checks must apply them separately.

## Metadata-only concatenation

`Facts.concat` first requires the right facts, uses `Math.addExact` for combined UTF-16 length and applies the budget **before** identity shortcuts or output allocation. Integer-domain overflow throws `ArithmeticException`; invalid/exceeded budget throws `IllegalArgumentException`. Empty-left returns the exact right object; empty-right returns this object after those checks.

For two nonempty values, composition retains the first two and last two combined units and computes at most two cross-boundary keys:

- left has at least two units: its last two plus right's first
- right has at least two units: left's last plus right's first two

The private `first` helper extracts a fact's first unit correctly for one-unit versus longer values. `seamKeys` builds/sorts the seam array. `mergeFactKeys` uses checked sum-of-array-length capacity, performs a three-way merge of left/right/seam keys, deduplicates and trims the output as needed. This is O(k) merge work and O(k) temporary/output storage for the participating keys; no original text is read. Joined surrogate pairs are still processed as the two exact UTF-16 units, with all three-unit seams accounted for.

Composition order matters: `left.concat(right)` describes the ordered concatenation, not a commutative set union. The retained boundaries are enough for exact trigram-set composition but not for arbitrary text reconstruction. There is no global concat cache, all-pairs precomputation, mutation of either operand, callback, native call or close obligation. Unchecked repeated growth can still exhaust heap despite each accepted length fitting an integer budget.

## Cache, format and integration boundaries

M3TQ itself has no global cache, mutable fact cache, schema/version field, byte serialization, decoder, checksum admission, `open`, JNI declaration or native-library loading. `ALL`/`NONE` are shared constants; all other retention is caller-owned query/fact state. The immutable query/fact objects can be reused without internal mutation; constructing/scanning against a mutable input or using a stateful predicate requires caller coordination.

The three different neighboring contracts remain distinct:

- [Mapped String storage](mapped-string-storage-reference.md) validates actual on-disk headers/commits/records and manages mapping/view lifetime. M3TQ only invokes an already obtained backing's `length`/`charAt`
- [Pattern query/Boolean-plan contracts](bootstrap-pattern-query-contracts.md) describe compiled JDK node admission, flags, region/input-bound Matcher fact reuse, failure-state behavior, exact fallback and the separate admitted full-match Boolean plan. M3TQ has no regex parser and does not dispatch Matcher itself
- [Bootstrap String precompute](bootstrap-string-precompute.md) describes String owner/range facts and other search/position/regex helpers. M3TQ membership is not a substitute for those exact operation contracts or a second canonical String payload

The older [TQ packet README](../../m3/tooling/tq/README.md) describes the historical standalone port and says it does not insert guards into Pattern/Matcher. That statement scopes the original packet; current Pattern/Matcher integration is documented by the separately reviewed source-based guide above. Do not infer current absence of integration from a historical port receipt, or broad acceptance of every later regex query from the presence of M3TQ.

## Complete declared-helper map

The preceding surface tables cover every public method, `Facts` method, enum and retained field. The remaining internal declarations are:

| Declaration | Role |
| --- | --- |
| Private `M3TQ(Op,long[],List<M3TQ>)` | Null checks, array clone/sort and immutable child-list copy |
| `combine` | AND/OR normalization and traversal-dependent refusal/short-circuit validation |
| `scanFacts` | Single cold code-unit scan and boundary collection |
| `requireFactBudget` | Rejects negative length/budget and length above budget |
| `compactFactKeys` | Deduplicates a sorted owned key array; keeps original when already unique or length <2 |
| `mergeFactKeys` | Checked capacity, three sorted-input merge, deduplication and optional trim |
| `uniqueTrigrams` | Unbudgeted overlapping-triple construction helper |
| `Facts(int,int,int,long[])`, `first`, `seamKeys` | Private ownership transfer and concatenation boundary geometry |
| Anonymous backing reader `length`, `charAt`, `subSequence` | Delegation without view export; unsupported slice materialization |
| `render`, `quote` | Diagnostic allocation and unescaped UTF-16 rendering |
| `trigramsForReadOnlyKernel`, `subqueriesForReadOnlyKernel` | Trusted package-private inspection, with raw-array immutability obligation |

Ordinary generated enum/Object operations do not add an image schema or value-equality contract. No method catches input/predicate exceptions or allocation failure and substitutes ALL, NONE or an exact-match result. Refusal/fallback of an optional optimization must be implemented by its actual consumer.

## Actual tests and reproduction boundaries

| Read source | What the source checks; not a fresh passing-run claim |
| --- | --- |
| [M3TQFactsTest](../../test/jdk/jdk/internal/mindex/M3TQFactsTest.java) | Empty/short/ordinary/surrogate/NUL/high-unit seam examples; 5,000 seeded random pairs; composed versus directly prepared key arrays/lengths; substring-key subset checks; defensive export; whole/range budgets and range bounds |
| [M3TQTest](../../test/jdk/java/lang/String/M3TQTest.java) | A real bootstrap-loaded `java.base` owner; bounded exhaustive and fixed corpora; all ranges/splits; fenced one-read-per-admitted-unit scans and zero warm payload reads; exact set oracle; literal regex no-false-negative checks; false-positive/order/multiplicity examples; AND/OR truth tables/identities; defensive query/fact exports; refusals before character reads; real mapped owner/range use and required test JNI parity |
| [M3TQSparseContainmentTest](../../test/jdk/java/lang/String/M3TQSparseContainmentTest.java) | 10,000 seeded sparse/dense set-oracle trials; large singleton extremes, empty/null checks and warm payload fences. Reflective access to the private Facts constructor builds artificial valid sorted key sets; that is test scaffolding, not a public arbitrary-facts admission API |
| [TQTest tooling suite](../../m3/tooling/tq/src/test/java/com/m3/tq/TQTest.java) | Exact recipe replay/fixed point, drift/missing/duplicate/wrong-parser/tamper refusal, strict Java/native compilation, java.base-only dependency check, interpreter/mixed/C1/C2 requests, mapped regressions, and mutants for native Unicode, seam omission and incorrect containment |
| [TQSyncTest](../../m3/tooling/tq-sync/src/test/java/com/m3/tq/TQSyncTest.java) | Its 24 text recipe postimages, exact bytes, fixed point, drift/missing/duplicate refusal and unrelated-file preservation. The number 24 here is that recipe manifest's output count, not this guide's 24-source P03 package census |

The production owner is pure Java. `M3TQTest` explicitly loads its native **test oracle**, and its jtreg header uses `main/native`; omitting that library is not a successful production fallback test. Native-oracle equality does not mean M3TQ silently selects a native backend. Test fixtures using mapped files also depend on the mapped-owner fixture and its own admission contract.

[verify-image.sh](../../m3/tooling/tq/verify-image.sh) requires `M3_TEST_JDK` to point at a matching fastdebug fork image, compiles Java test/JNI headers with that image and a Linux g++ oracle, and runs without patching `java.base`. It requests interpreter, mixed, C1, C2 and M3-enabled/noncompact configurations, checks warm-read markers and passes the explicit M3-interpreter allowance to its compilation-log checker for M3-enabled configurations. A label such as `m3-c2` is not proof that the experimental M3 String runtime executed in C2; preserve the runtime's interpreter-only boundary and inspect the actual exact-run logs.

The tooling suite's stock-JDK patch-module modes, old README receipts and current matched-image CI are different evidence. None was executed or established as current passing evidence here. No broad performance, allocation-free construction, complete regex-language, malformed serialized-query, full jtreg/TCK, GC/CDS/JVMTI/JFR or platform-matrix claim follows from these test bodies. The current [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md) and [name map](../../m3/docs/name-mapping.json) retain the ordered receiving gates and reject treating candidate facts or hashes as exact semantic proof.
