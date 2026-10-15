<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Retained AST navigation, queries and candidate reuse

[Guide](README.md) · [Source and assertion coverage](retained-ast-navigation-reference-coverage.md) · [AST ownership](retained-ast-ownership-reference.md) · [Runtime boundaries](runtime-owners.md)

This reference documents ten complete source bodies in the retained
`com.synexia.indexstring.ast` subtree at public M3JDK21 commit
`363c02caa2e5af1df9460b927ecd78f6a9dbcefb`. Their Git blobs are unchanged at
`fb1e2f78ae4000f34adbed4a7fb91a69dd5ef8e0`. The companion records exact hashes,
every declared entry-point family, supporting callers and test assertions.

This is source-derived documentation, not a source-complete module, new public
JDK API, parser replacement or runtime admission. The retained directory has no
own `pom.xml` or `module-info.java`. The [receiver README][vendor-readme] and
[AST intake][intake-readme] explicitly retain incomplete source custody. The
separate indexed-string bridge, frozen-text compatibility fixture and canonical
`java.lang.String`/M3String runtime owners are different surfaces. All tests,
builds, repository verifiers, native loads and benchmarks for this documentation
authoring/review pass are **NOT_RUN**. Hosted CI remains separate evidence for
its exact commit; no hosted result is asserted as an authoring or review run here.

## 1. Choose the coordinate system first

| Coordinate or value | Owner and domain | What it does not identify |
| --- | --- | --- |
| Canonical atom handle | Positive integer in one `MIndexASTPool`; immutable subtree content is shared by repeated appearances | A source location, occurrence row or portable process-independent handle |
| Occurrence ID | Zero-based row in one `MIndexASTDocument`; parent/child/sibling and source positions belong to that document | A globally unique node or canonical atom identity |
| Preorder index | Zero-based traversal position produced by `MIndexASTPrecompute` | The occurrence ID itself; document rows are not preorder |
| Source offset | javac position in the parsed character sequence, carried as a `long`; use Java UTF-16 character offsets | A UTF-8 byte offset, code-point index, line/column pair or offset into a different source revision |
| Hash / SimHash | Candidate feature of content, structure, normalized labels or a path | Exact source equality, compiler semantics or permission to reuse generated work |
| Pool posting / query result | Atom handles bounded by a particular pool snapshot | Every source occurrence or later atom added to the pool |

The [document][document-geometry] copies its occurrence lanes and source digest.
Its root retains the pool; the pool, label resolver and source identity have their
own lifetimes. The document retains a path and SHA-256 of big-endian UTF-16 code
units, not the full source text. The [parser][parser-source] supplies javac
`SourcePositions` without translating them into bytes or code points. It calls
`task.parse()`, not semantic attribution or compilation. Source navigation does
not add compiler analysis or incremental text editing.

`document.precompute()`, `document.sourceIndex()`, `pool.precompute()`,
`precompute.constantTimeLca()` and `precompute.sourceIndex()` each build a new
projection. These methods do not memoize. The separate cache `Resident` can retain
optional projections; its [upgrade path][resident-upgrade] is the caller that
reuses existing ones. See [AST ownership](retained-ast-ownership-reference.md)
for parser, pool, edit-session, cache, eviction and close contracts.

## 2. Occurrence navigation: `MIndexASTPrecompute`

[Construction][nav-build] freezes a document's child order, preorder permutation,
inverse permutation, subtree sizes, child counts/ordinals, binary-lifting
ancestors, path hashes, byte-sized kind ordinals and kind/atom postings. A root
appears first in preorder even though its document occurrence ID need not be zero.

| Entry points | Result and boundary |
| --- | --- |
| `build(document)` | Nonnull document; rejects zero occurrences and a traversal count different from the document count. Uses checked addition/multiplication for several array and subtree dimensions |
| `document()`, `astSpec()` | Retain/return the same document and its root's spec projection; no source copy or new grammar parser |
| `occurrenceCount()` | Frozen document row count |
| `preorderOccurrence(i)`, `preorderIndex(o)` | Convert between the two coordinate systems; out-of-range coordinates throw `IndexOutOfBoundsException` |
| `subtreeSize(o)`, `subtreeEndPreorderExclusive(o)` | Inclusive-root size and exclusive end in preorder; subtree interval is `[preorderIndex(o), end)` |
| `subtreeOccurrences(o)` | New array of occurrence IDs in subtree preorder, including `o` |
| `childCount(o)`, `childOrdinal(o)` | Count of direct occurrence children; zero-based sibling ordinal. Construction assigns the root ordinal `-1` |
| `isAncestor(a, d)` | Inclusive ancestry: an occurrence is its own ancestor |
| `kthAncestor(o, distance)` | Self at distance zero; `-1` above root; negative distance throws `IllegalArgumentException` after occurrence validation |
| `lowestCommonAncestor(a, b)` | Validated document coordinates, binary-lifting result; self/ancestor cases return directly |
| `distance(a, b)` | Number of occurrence-tree edges through the LCA, not source-character distance |
| `pathOccurrences(a, b)` | New array ordered `a → LCA → b`, inclusive; accepts arbitrary two occurrences |
| `occurrences(kind)` | Nonnull kind; new array in increasing occurrence-ID order, not preorder |
| `occurrences(atom)` | Nonnull atom in the identical pool object; otherwise `IllegalArgumentException`. New array of matching rows; an atom added beyond the captured atom limit yields an empty array |
| `preorderKindOrdinal(i)`, `preorderKindOrdinals()` | Unsigned ordinal at a checked preorder position; whole-stream accessor returns a clone |
| `kindPathHash(a, d)`, `exactPathHash(a, d)` | Candidate hash for the inclusive ancestor-to-descendant path. Both coordinates are checked; a nonancestor pair throws `IllegalArgumentException` |
| `constantTimeLca()`, `sourceIndex()` | Independently construct the optional projections described below |
| `primitivePayloadBytes()` | Sum of primitive-array payload sizes; excludes document, pool, object headers and transient work |

[Navigation methods][nav-api] retain no caller arrays. Arrays returned by public
search/traversal methods are detached and caller-mutable. Candidate path hashes
use rolling 64-bit arithmetic, with ordinary wraparound; `exactPathHash` hashes the
sequence of each path atom's exact-subtree hash. It is still not exact comparison
of a path or of source text.

Building uses occurrence-tree traversal plus an ancestor table with
`max(1, 32 - numberOfLeadingZeros(n))` levels. Binary-lifting ancestor/LCA queries
are logarithmic in the occurrence count; producing a path or result array still
allocates and visits the returned elements. Kind and atom postings allow lookup
without rescanning the whole document. These are implementation work bounds,
not measured time/allocation improvements.

### Navigation restoration is validation, with a bounded scope

The package-private `Snapshot` is a codec support type. Its constructor/accessors
hold raw array references within that package boundary. `snapshot()` clones the
live arrays; `restore(document, payload)` [validates and clones again][nav-restore].
It checks:

- All array lengths, occurrence count, expected ancestor levels and atom limit
- A valid preorder permutation and inverse, the root's first position, each
  child's ordinal/count, contiguous child subtree placement and subtree sizes
- Parent level zero and every binary-lifting recurrence
- Rolling powers, per-occurrence kind ordinals and both path-hash recurrences
- Kind/atom posting offset totals, per-key cardinalities and each entry's key

Do not broaden that last check into uniqueness or sortedness of restored posting
entries: `validateOffsetsAndPosting` does not explicitly check either. It also
does not explicitly validate the root's stored child ordinal. Build-produced
images have the ordering/root sentinel described above; the restore checks are
exactly the listed checks, not a proof against every adversarial image. Invalid
geometry normally throws `IllegalArgumentException`; bad referenced coordinates
can also trigger index exceptions and checked arithmetic can throw
`ArithmeticException`. The codec does not translate all of these into I/O errors.

## 3. Optional Euler/RMQ LCA: `MIndexASTLcaSparseTable`

[Construction][lca-build] walks the document iteratively, recording an Euler tour
of `2*n - 1` visits, first visit per occurrence, document depths, floor-log table
and a flattened range-minimum sparse table. Equal-depth minima choose the left
candidate. `lowestCommonAncestor(left, right)` checks both occurrence IDs,
normalizes their first-visit interval and compares two precomputed minima.

- `build(document)` rejects null and zero occurrences; checked Euler/table size
  arithmetic can overflow. A mismatched actual Euler-tour length throws
  `IllegalStateException`
- `document()`, `eulerLength()` and `levels()` expose the retained document and
  geometry. `primitivePayloadBytes()` counts its five `int[]` lanes
- Construction/storage are `O(n log n)`; LCA lookup is `O(1)`. This does not make
  path materialization constant-time or remove the retained document/pool
- `MIndexASTPrecompute` already provides binary-lifting LCA. Choose the additional
  table for repeated LCA lookups only when its extra memory is acceptable

Its package-private [snapshot/restore support][lca-restore] clones on export and
publication. Validation checks nonempty document, checked expected dimensions,
occurrence ranges, root start, parent-child adjacency between successive Euler
entries, matching depths, each occurrence's `1 + directChildCount` visit count,
first-visit positions, logarithm recurrence, all valid RMQ cells and zero padding.
It checks those properties; it does not independently regenerate and compare the
complete traversal. Size overflow during restore is wrapped as
`IllegalArgumentException`.

The public retained test `MIndexASTLcaCodecTest` invokes a different owner,
`MIndexASTLcaCodec`, whose body is absent from this retained subtree. Its existence
does not establish a reviewed LCA wire format or a passing codec result here.

## 4. Cursor lookup: `MIndexASTSourceIndex`

[Build and lookup][source-api] index only document ranges with `start >= 0` and
`end >= start`. Unknown/negative endpoints are skipped. A zero-width `[x, x)`
range is indexed but contains no offset. No source-length argument is available;
the index does not prove that an otherwise nonnegative endpoint lies within the
original source. It also does not require nested source ranges to follow tree
parentage.

The arrays sort by start offset, then document depth, then occurrence ID, all
ascending. A power-of-two max-end tree prunes blocks. After sorting, construction
rejects a tree that would need to grow beyond the guarded size with
`IllegalStateException`; ordinary allocation failures are not converted into a
smaller projection.

The [sort runs before that size guard][source-sort] and uses unchecked `int`
arithmetic for `start + width`, `start + block` and `start += block`. For a count
above `2^30`, if the large allocations succeed and execution reaches the
`width = 2^29` pass, its second block starts at `2^30` and `start + block` wraps.
Earlier allocation failure is also possible. This is a source-only extreme-count
caveat, **NOT_RUN**, not a reproduced failure or a claim about pool-snapshot
reachability under its separate capacity constraints.

| Entry point | Concrete behavior |
| --- | --- |
| `build(document)` | Nonnull document; owns new starts, ends, occurrence and max-end arrays |
| `document()` | Returns the same retained document |
| `indexedOccurrenceCount()` | Number of valid ranges, possibly less than total occurrences |
| `deepestContaining(offset)` | Returns the maximum-depth occurrence satisfying `start <= offset < end`; negative offsets, empty index or no containing interval return `-1` |
| `containingPath(offset)` | Fresh root-to-deepest parent chain, or empty array. Ancestors are included through parent links even if their own source ranges are unknown or do not contain the offset |
| `firstStartingAtOrAfter(offset)` | Lower-bound lookup returns the first sorted range whose start is at least the offset, else `-1`; negative offsets are allowed and can select the first indexed range |
| `primitivePayloadBytes()` | Three long-array payloads plus the occurrence int-array payload; no full source text or document/pool heap accounting |

At equal maximum depth, `deepestContaining` keeps the first match in its
right-to-left scan of sorted ranges. That means later start wins, then larger
occurrence ID for equal start/depth. `firstStartingAtOrAfter` instead uses the
first range in ascending sort order, so a tied start selects smaller depth, then
smaller occurrence ID. These are implementation tie rules, not semantic node
priority. The tree's `end >= offset` pruning is deliberately only a candidate
check; the final containment check uses strict `offset < end`.

A point lookup can inspect many overlapping intervals. Do not describe all cursor
queries as logarithmic merely because binary search and a max tree are involved.

The package-private [Payload boundary][source-restore] is especially important:
`snapshot(index)` clones arrays and `restore(document, payload)` clones them again,
but restore only checks the two objects for null. It does **not** check lengths,
ordering, tree geometry, document membership or source identity. Its comment
assigns validation to an external codec; that comment is not a validator. The
navigation codec below does not serialize or restore `SourceIndex.Payload`.

## 5. Canonical DAG search: `MIndexASTPoolPrecompute`

A [pool snapshot][pool-build] captures `limit = pool.size()` and
`labelLimit = pool.labels().size()`, then creates kind and label postings, reverse
child-to-parent postings and sorted exact/structural/logic hash pairs. This is a
snapshot of canonical atoms across that pool, not a document selection. Shared
subtrees appear once as atom handles even if they occur repeatedly in one or more
documents.

| Public member family | Return, order and failure |
| --- | --- |
| `snapshot(pool)` | Nonnull pool; captures fixed atom/label limits. Rejects observed labels beyond the label limit and more than `Integer.MAX_VALUE` reverse edges |
| `pool()`, `atomCount()`, `labelCount()` | Original pool object and captured limits, not its future counts |
| `handles(kind)` | Nonnull kind; fresh ascending atom handles |
| `handlesByLabelHandle(label)` | Fresh handles; label `0` means no label. A negative or beyond-snapshot label yields an empty array |
| `parents(handle)`, `incomingParentCount(handle)` | Incoming edge postings/count within the captured snapshot. Handle must be in `[1, limit]`, otherwise `IllegalArgumentException` |
| `handlesByExactHash`, `handlesByStructuralHash`, `handlesByLogicHash` | Fresh handles for the supplied 64-bit candidate hash, ascending within the equal-key group |
| `exactHashGroups`, `structuralHashGroups`, `logicHashGroups` | Fresh outer/inner arrays for equal-hash groups of at least the requested count. Minimum below two throws `IllegalArgumentException` |
| `find(query)` | Nonnull query compiled against the identical pool object; different pool throws `IllegalArgumentException`. Returns fresh matching atom handles |
| `primitivePayloadBytes()` | Nine int-array and three long-array payloads only |

Reverse-parent postings preserve one entry per child edge. A parent using the
same child twice appears twice, so `incomingParentCount` is not a count of distinct
parents. Hash pairs sort by unsigned 64-bit key, then handle. Group order follows
that unsigned key order; equal-key members use increasing handles. These are
candidate groups, not confirmed equality classes.

### Query planning selects one posting list

[`find` and its planner][pool-find] start with all captured handles, then consider
root kind, label, exact hash, structural hash and logic hash, in that order.
They select only a strictly shorter available list; ties retain the earlier
choice. A specified label outside the frozen label limit selects an empty source.
There is no multi-posting intersection, learned optimizer, query cache, query
limit or SimHash index. Every selected candidate is rechecked by
`query.matchesHandle`, then copied into the result. Build-produced lists make
results increasing atom handles.

Snapshot creation does not acquire a lock around the entire pool/label capture.
Existing pool atoms are immutable and pool admission uses its own synchronization,
but the projection does not promise one atomic transaction over pool growth and
label growth. Later atoms are excluded. Later label interning by a query builder
can therefore produce a label the old snapshot correctly treats as absent.

### Pool image restoration is a separate support path

The package-private [Snapshot/restore code][pool-restore] is not the navigation
codec's image. `snapshot()` materializes every retained label to a `String`,
computes 32-byte subtree identities, and clones all search arrays. `restore`
requires each limit in `[0, 16_000_000]` and **exact equality** with current live
pool and label counts; it does not restore into a larger append-only pool.

After shape and monotonic-offset checks it remaps old atom handles using live
SHA-256 identities and old label handles using materialized text. Missing/duplicate
live identities, null persisted labels, stale labels or invalid handles fail with
`IllegalArgumentException`. It validates kind/label membership without duplicate
mapped handles, recomputes reverse-parent edge multiplicities/postings, and checks
every hash/handle pair against the live pool with uniqueness and ordering. Some
persisted arrays are cloned and some are rebuilt/remapped. No caller owns a live
mutable lane afterward.

Those support functions establish the checks visible here, not a public pool
wire format, cryptographic source authentication or complete hostile-input proof.
The navigation codec does not call them. Recomputed label text, transient maps,
SHA work and copied arrays are preparation/materialization costs absent from
`primitivePayloadBytes()`.

## 6. Query predicates: `MIndexASTQuery`

[`builder(pool)`][query-builder] rejects null and returns a mutable builder.
`build()` copies its fields into a final query retaining that same pool. Later
builder changes do not change the compiled query. The builder is not synchronized.
`pool()` returns the original reference; `matches(atom)` rejects null or an atom
from a different pool, then tests the handle. It can test a later atom in that
same pool, even though an older `PoolPrecompute.find` cannot enumerate it.

| Builder member | Default / accepted setting / side effect |
| --- | --- |
| `rootKind(kind)` | No root restriction until set; null rejected; later calls replace the value |
| `label(CharSequence)` | No label restriction by default. Null rejected; empty means handle zero, otherwise interns text in the pool |
| `label(MIndexString)` | Null rejected; delegates to pool label interning, which requires the same resolver. Unlike the CharSequence overload, it has no explicit empty-to-zero shortcut |
| `noLabel()` | Requires handle zero; replaces any earlier label restriction |
| `requireFlags(mask)`, `forbidFlags(mask)` | Initially zero; successive calls OR their masks. No separate flag-width validation in the builder |
| `requireCategory(category)` | Initially none; ORs each nonnull category's bit; all requested bits must be present on the root kind's language rule |
| `exactHash`, `structuralHash`, `logicHash` | Disabled until set; any long accepted; each later call replaces that lane |
| `simHashNear(hash, maxDistance)` | Disabled until set; accepts inclusive distance `[0,64]`, otherwise `IllegalArgumentException` |
| `expandedNodeCount(min,max)`, `depth(min,max)`, `expandedLeafCount(min,max)` | Default `[1,Integer.MAX_VALUE]`; minimum must be at least one and maximum at least minimum, otherwise `IllegalArgumentException` |
| `containsKind(kind)` | No subtree-kind restriction by default; nonnull kind; later call replaces the earlier kind rather than accumulating a set |
| `build()` | Overlapping required/forbidden flags cause `IllegalStateException`; otherwise creates the immutable query |

The query's depth is the atom's expanded-subtree depth (leaf depth one), not the
document occurrence's root-relative depth (root depth zero). Expanded node/leaf
counts count repeated child uses, not unique pool handles.

The [matching order][query-match] is root kind → exact label handle → required
flags → forbidden flags → root category bits → exact/structural/logic hashes →
SimHash threshold → inclusive expanded-node/depth/leaf ranges → subtree-kind
signal → actual `containsKind` traversal. A failed check short-circuits later
work. Category checks refer to the root rule, not every descendant. Subtree-kind
membership includes the root. The pool's kind signal can collide, so a positive
signal is followed by traversal before that predicate succeeds.

“All predicates verified” means verified according to these definitions. A query
that asks only for hash equality has **not** performed `contentEquals`,
`structurallyEquals` or `normalizedLogicEquals`. A positive SimHash/hash query is
still a candidate for a later exact/contract check.

## 7. Exact comparisons and reuse keys have different authority

[`MIndexASTDiff.compare(left, right)`][diff] rejects null, then delegates to the
actual AST comparison methods in this order:

| Result classification | Performed comparison |
| --- | --- |
| `IDENTICAL` | `contentEquals` succeeded |
| `LABEL_ONLY` | Exact comparison failed; structural and normalized-logic comparisons succeeded |
| `NORMALIZED_LOGIC_CHANGED` | Exact comparison failed; structural comparison succeeded; normalized-logic comparison failed |
| `STRUCTURE_CHANGED` | Structural comparison failed |

The returned `Result` carries the classification, left/right exact, structural
and logic hashes, SimHash Hamming distance and left/right expanded node counts.
The public record constructor does not validate consistency; callers can construct
records that did not come from `compare`.

The [pool comparison implementation][pool-equality] first uses the corresponding
hash as a rejection filter, then compares packed kind/flag metadata, ordered
children and the labels selected by its comparison mode. Exact mode compares all
labels; structural mode ignores labels; logic mode compares only primitive-type
and `*_LITERAL` labels. These are syntactic relations, not a proof of equivalent
Java execution, identifier binding, type resolution, source formatting or original
file bytes. A same-pool/same-handle shortcut is canonical atom identity.

[`MIndexASTReuseKey`][reuse] is a seven-component primitive record:
`exactHash64`, `structuralHash64`, `logicHash64`, `simHash64`,
`expandedNodeCount`, `depth`, `expandedLeafCount`. Its constructor rejects negative
node counts, depth below `-1` and leaf count below `-1`; it accepts zero and does
not establish that the fields describe a real atom.

- `from(ast)` rejects null and copies all seven atom features
- `from(result, LEFT/RIGHT)` rejects null arguments and copies hashes/node count,
  but uses SimHash `0` and unknown depth/leaf count `-1`. The diff's distance cannot
  reconstruct either side's SimHash
- `exactCandidate(other)` requires the same shape plus equal exact and structural
  hashes; `normalizedLogicCandidate` uses structural and logic hashes;
  `structuralCandidate` uses structural hash alone
- Same shape means equal expanded node count; depth/leaf counts must match only
  when **both** sides provide nonnegative values
- `hammingDistance(other)` is `bitCount(this.simHash64 ^ other.simHash64)`;
  `approximateCandidate(other, max)` checks inclusive `[0,64]` threshold and does
  **not** impose the shape test
- `classify(other, max)` returns the first of `EXACT`, `NORMALIZED_LOGIC`,
  `STRUCTURE`, `APPROXIMATE`, `NONE`. Its threshold validation happens only if the
  approximate branch is reached; a stronger candidate can return even for an
  out-of-range threshold
- Package-private `classifyWithHammingDistance` validates both supplied distances
  in `[0,64]` first, then uses local hash/shape branches and only the supplied
  distance for the approximate branch

All comparison arguments above reject null. Equal zero placeholder SimHashes
from diff-derived keys do not imply proximity. No candidate class authorizes
publishing reused output; verify the required exact syntax relation and the
consumer's semantic/effect contract separately.

## 8. Kind-stream and path patterns

### `MIndexASTKindPattern`

[`of(kinds...)`][kind-pattern] rejects a null array, null element or empty pattern,
then copies kind ordinals into bytes and builds a KMP failure table. Input-array
mutation afterward does not change the pattern. `length()` reports pattern
length; `kindAt(i)` checks bounds and decodes the unsigned ordinal.

`findAll(precompute)` rejects null, scans the **entire preorder kind stream** and
returns fresh occurrence IDs at match starts, in scan order. KMP fallback allows
overlapping matches. Preorder adjacency can cross sibling/subtree boundaries;
it is not necessarily a parent-child path or an exact subtree match.

`matchesPath(precompute, ancestor, descendant)` uses
`precompute.pathOccurrences` and compares the whole returned path's kinds. Despite
its parameter names, it does **not** enforce ancestor/descendant direction; an
arbitrary left-to-LCA-to-right path is supported by the called method. Length must
match exactly. Invalid coordinates throw via navigation; otherwise mismatch is
`false`. `failureTable()` returns a clone. `primitivePayloadBytes()` counts the
ordinal bytes plus failure ints; package-private `ordinalAt` supplies raw bytes to
the automaton compiler.

### `MIndexASTKindAutomaton`

[`compile(patterns...)`][kind-automaton] accepts 1–64 nonnull patterns. Null array
or element fails with `NullPointerException`; zero/more than 64 patterns fail with
`IllegalArgumentException`. Checked state/alphabet dimension arithmetic can throw
`ArithmeticException`. Construction builds a trie, breadth-first failure links,
inherited output masks and a dense transition for every `Tree.Kind` symbol, then
trims storage to actual states. Root is state zero. Duplicate patterns still have
different pattern indexes/output bits.

`patternCount()` and `stateCount()` report frozen geometry.
`scanPreorder(precompute)` rejects null and returns an unmodifiable `List<Match>`.
It emits by increasing ending preorder position; matches at the same end emit by
increasing pattern index. Each public `Match` record has `patternIndex`,
`startPreorder`, `endPreorderExclusive` and `startOccurrence`. The record's public
constructor adds no validation; records constructed by the scan have those derived
coordinates. The output can be much larger than the number of occurrences because
multiple/overlapping patterns can match each position.

Package-private `alphabetSizeForNative`, `transitionsForNative`,
`outputMasksForNative` and `patternLengthsForNative` expose a scalar and cloned
arrays. Their names do not load JNI or prove a native provider. The Java scan
uses the same preorder coordinate model as KMP.

`imageRootSha256()` [hashes][automaton-identity] the fixed V1 marker, alphabet size,
state count, length-prefixed transitions/failures/output masks/pattern lengths in
big-endian primitive form. It identifies the compiled image, not document source,
execution results or semantic equivalence of arbitrary programs. No document or
host path is retained by the automaton. SHA-256 unavailability becomes
`ExceptionInInitializerError`. Payload bytes include transition/failure/length
ints and output longs, not match objects or construction scratch arrays.

## 9. Navigation cold image: `MIndexASTPrecomputeCodec`

This owner serializes **only `MIndexASTPrecompute` navigation arrays** against an
already-loaded document. It does not serialize AST atoms, source text, source
interval indexes, the LCA sparse table or pool-search images. It is not
`MIndexASTCodec` or `MIndexASTLcaCodec`.

### Writer and exact envelope

[`write(key, languageId, index, output)`][codec-write] rejects null arguments and
negative language ID (`IOException`). Before writing, it checks the key's source
reference against the materialized document path, UTF-16 source digest against
the document digest, and AST-spec fingerprint against the root spec. It snapshots
navigation and calls navigation restore for geometry validation.

The wire sequence is:

1. ASCII `MIDXNAV1`, int version `1`, int projection code `2`, int language ID
2. Length-prefixed UTF-8 strings: source reference, source SHA text, Java language
   spec identity; then long AST-spec fingerprint
3. Int occurrence count, int ancestor levels, int atom limit
4. Length-prefixed int arrays: preorder-to-occurrence, occurrence-to-preorder,
   subtree sizes, child counts, child ordinals, ancestors
5. Length-prefixed long arrays: powers, kind-path hashes, exact-path hashes
6. Exactly `occurrenceCount` preorder-kind bytes, with no extra length prefix
7. Length-prefixed int arrays: kind offsets, occurrences by kind, atom offsets,
   occurrences by atom
8. A 32-byte SHA-256 digest of every preceding envelope/payload byte

`DataOutputStream` writes integers/longs big-endian. Text fields are limited to
1 MiB of UTF-8 bytes. Writer calls `flush()` before appending the digest, but does
not flush again afterward and does not close the caller's stream. An I/O or later
field-size failure can leave partial output. The caller owns final flushing,
closure, atomic file publication and removal of incomplete output.

The language argument must be nonnegative, but this writer does not compare it
to `document.root().pool().languageId()`. It is an envelope/caller binding, not an
independent inference of the parsed language.

### Reader limits and identity

[`read(key, languageId, document, input)`][codec-read] rejects null and negative
language ID, verifies magic/version/projection and requires the stored language
to equal the caller's argument. It reads bounded text, then requires matching
source reference, source digest, document spec identity and key fingerprint; it
also rechecks the key against the loaded document.

Before allocating navigation lanes, header checks require:

- Occurrence count equals the document's count and is in `[1,16_000_000]`
- Levels equal the computed value for that count and are in `[1,32]`
- Atom limit is nonnegative, at most `16_000_000`, and no greater than the loaded
  pool's current size

Each array's encoded length must exactly match the expected geometry. The reader
checks the payload digest and rejects any trailing byte, so this is a whole-stream
image reader, not a concatenated-frame reader. Truncation propagates I/O failure.
Text decoding uses Java UTF-8 `String` construction rather than a strict decoder
configured to report every malformed byte sequence.

Finally it calls `MIndexASTPrecompute.restore`, so those geometry checks and
unchecked exceptions remain relevant after checksum validation. It neither
reparses source nor remaps atom handles. It verifies occurrence handles against
the supplied document/pool; it does not persist a portable pool identity. The
separate pool restore path above must not be silently attributed to this codec.

The writer does not call the reader's `validateHeader`, so its occurrence/atom
caps are not a symmetric writer admission check. The reader's bounds also do not
supply a caller-selected total byte/heap budget: several large arrays, the
validation scratch and restore clones can coexist. No resource/performance claim
is established by the numeric limits alone.

The source calls this an “authenticated” image. Its mechanism is an unkeyed
SHA-256 checksum plus identity and geometry checks, not a signature or MAC from a
trusted producer. A valid checksum is not independent proof that every coordinate
is correct, nor a security boundary against a producer that can recompute it.
The historical qualification explicitly leaves valid-checksum/wrong-geometry
refusal unqualified.

## 10. Ownership, materialization and concurrency checklist

- Navigation, source index and LCA objects retain their document, which retains
  its root pool. Pool projections and queries retain the pool directly. Reuse
  keys, diff results and kind patterns/automata retain primitive values/arrays
  without retaining the compared document
- Public result arrays are fresh/copies, not live mutation views. Automaton match
  lists are unmodifiable. Package-private snapshot records/classes are trusted
  support containers, not generally deep-immutable public serialization objects
- The index methods have no close/release API and do not register themselves in a
  global cache. Dropping a cache entry does not revoke a client-held projection
- Public read operations do not mutate their frozen array lanes. Builders are
  mutable. Snapshot building, label interning, pool growth, stream use and cache
  publication have separate synchronization/ownership requirements; do not infer
  a globally atomic snapshot or full thread-safety proof from final index fields
- Query label building may intern text; cross-pool exact comparisons and pool
  cold snapshots/restores may materialize labels. Navigation/path/scan arrays,
  match records, hashing and codec restore allocate. “Primitive” does not mean
  allocation-free or a zero-copy serialization path
- `primitivePayloadBytes()` reports the implemented primitive-array arithmetic,
  not deep retained heap, peak allocation, native memory or an eviction weight
  guarantee. Retained pool/labels and temporarily duplicated lanes are additional.
  Navigation, LCA, pool, source-index and automaton implementations add several
  array lengths as `int` before casting the aggregate to `long`. Extreme aggregate
  lengths can overflow that intermediate sum without a checked exception, so the
  returned value is not an exact accounting guarantee at arbitrary dimensions

### Inspected usage sketch, not executed

Given an already-created `MIndexASTDocument document`, these existing APIs keep
occurrence and atom results separate:

```java
MIndexASTPrecompute navigation = document.precompute();
int[] plusOccurrences = navigation.occurrences(Tree.Kind.PLUS);
MIndexASTPool pool = document.root().pool();
MIndexASTQuery query = MIndexASTQuery.builder(pool)
    .rootKind(Tree.Kind.PLUS)
    .containsKind(Tree.Kind.INT_LITERAL)
    .build();
int[] matchingAtomHandles = pool.precompute().find(query);
int[] matchingOccurrenceIds = document.findOccurrences(query);
```

This sketch omits imports and document construction; it is not a standalone
compilation claim for the incomplete retained subtree. A query over a repeated
canonical subtree may produce one atom handle and several occurrence IDs. Add
`contentEquals`, the required normalized comparison, or a separate consumer proof
before treating candidate hashes as reusable compiler output.

## 11. Evidence, ownership and remaining qualification

The public [export manifest][export] records the ten sources under their original
paths at donor revision `7d2133f1412a9e7295296c3f86baae577bb3251c`; current public
bytes match its SHA-256 rows. This establishes retained-byte lineage, not an
independent review of a different repository's unpublished bodies. The
[preserved NOTICE][notice] attributes AST algorithm references including
Euler/RMQ, KMP and interval/search ideas; preserve that statement and the actual
per-file licenses rather than treating it as a new license grant or source copy.

The dated [qualification receipt][qualification] reports four original
`MIndexASTPrecomputeTest` methods, 21 importer unit tests and eight byte-intake
cases over 44 payloads. The four AST methods exercise occurrence navigation,
cursor lookup, pool postings/query candidates and KMP/Aho-Corasick scans. The
[coverage companion](retained-ast-navigation-reference-coverage.md) maps the
assertions and the two codec-test bodies without transferring PASS to them.

That receipt explicitly excludes source-complete receiver build, actual target
consumer wiring, valid-checksum/wrong-geometry cold images, cache/edit/resident
lifecycle classes, full seed/repository verification, Maven/JaCoCo gates,
JNI/providers, matched JDK/jtreg/platforms, performance and full-family completion.
None was run or closed by this reference.

The [M3 reactor][reactor], [import verifier POM][import-pom], [foundation script][foundation]
and [indexed-string bridge POM][bridge-pom] are distinct build routes. They do not
make this retained AST source directory a compiled runtime module. Under the
[porting invariant][policy], the ordered receiving stage remains String
qualification; AST/compiler promotion needs separate format/consumer and target
proof, and compiler-only dependencies cannot be pulled into `java.base`.

[automaton-identity]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindAutomaton.java#L175-L223
[bridge-pom]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/ports/indexstring/pom.xml
[codec-read]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecomputeCodec.java#L86-L171
[codec-write]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecomputeCodec.java#L36-L84
[diff]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDiff.java#L1-L57
[document-geometry]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDocument.java#L31-L179
[export]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv
[foundation]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/build.sh
[import-pom]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/pom.xml
[intake-readme]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/README.md
[kind-automaton]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindAutomaton.java#L1-L230
[kind-pattern]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTKindPattern.java#L1-L90
[lca-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTLcaSparseTable.java#L94-L163
[lca-restore]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTLcaSparseTable.java#L165-L194
[nav-api]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L561-L767
[nav-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L429-L559
[nav-restore]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPrecompute.java#L211-L426
[notice]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/NOTICE
[parser-source]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTParser.java#L66-L187
[policy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3jdk21-porting-invariant.md
[pool-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L95-L185
[pool-equality]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L289-L723
[pool-find]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L562-L649
[pool-restore]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPoolPrecompute.java#L187-L494
[qualification]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/evidence/QUALIFICATION.json
[query-builder]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTQuery.java#L149-L285
[query-match]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTQuery.java#L65-L111
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/pom.xml
[resident-upgrade]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L210-L385
[reuse]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTReuseKey.java#L1-L151
[source-api]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSourceIndex.java#L37-L134
[source-restore]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSourceIndex.java#L136-L165
[vendor-readme]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/README.md
[source-sort]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTSourceIndex.java#L201-L279
