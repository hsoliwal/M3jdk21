<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Compile-time regex analysis, image and gate contracts

This reference covers all six production source bodies in the constant-regex
analysis/codec/consumer family at [public source pin
`8758991645037126985d56f16a208fec8a1eef38`](https://github.com/hsoliwal/M3jdk21/tree/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime):
`M3PatternNodes`, `M3RegexRequirement`, `M3ContentKey`,
`M3ConstantRegexImageWriter`, `M3ConstantRegexImage`, and `M3ConstantRegexGate`.
The [source map](compiletime-source-coverage.md) routes all 19 package owners;
[harvest and differential receipts](compiletime-harvest-reference.md) describe
the producer and finite evidence pipeline.

**Verification: NOT_RUN.** Source bodies, two direct JUnit classes and bounded
call paths were read; no build, test, example, benchmark, custody verifier or
JDK runtime integration ran for this documentation. The [existing design and
2026-10-13 measurements](../../m3/docs/m3-compiletime-precompute.md) remain
historical evidence with their original corpus, host and limits.

## Layer and authority boundary

This is the standalone Java-21 classpath artifact
`com.m3:m3-compiletime-precompute:0.1.0-SNAPSHOT`. Its [POM](../../m3/ports/compiletime/pom.xml)
depends on ASM, ASM tree/analysis, the separate `com.m3:m3-precompute` artifact,
and test-scoped JUnit. Surefire disables the module path and opens
`java.base/java.util.regex` for reflective build-time analysis. The project has
no `module-info.java`, parent POM or module list, and is not an entry in the
[root control reactor's module/profile module lists](../../m3/pom.xml).
Follow the explicit dependency-first [build recipe](../../m3/ports/compiletime/README.md);
this page neither adds reactor membership nor establishes a JPMS or bootstrap
receiver.

| Stage | Result | Required interpretation |
| --- | --- | --- |
| `M3RegexRequirement` | Necessary literal formula or a narrowly admitted pure literal | A true general formula means only a candidate; the stock engine decides the match |
| `M3ContentKey` | Two hashes, UTF-16 length and original compile flags | An image lookup key, not spelling equality or authentication |
| `M3ConstantRegexImageWriter` | Packed lanes and digests | Encodes caller-supplied analyses; does not independently prove them |
| `M3ConstantRegexImage` | Retained byte view and decoded entries | Header/digest/aggregate-geometry checks, not an exhaustive hostile-image parser |
| `M3ConstantRegexGate` | First-find/matches booleans and replacement/split results | Explicit port wrapper with mutable evaluation scratch, not a replacement `Matcher` |

The gate does not modify `Pattern.compile`, install a JDK receiver, invoke an
OpenRewrite recipe, or use the stored trigram lane. A future bootstrap consumer
is a separate authority and compatibility decision; see the
[bootstrap Pattern/query contracts](bootstrap-pattern-query-contracts.md).

## Requirement analysis and result construction

[`M3RegexRequirement`](../../m3/ports/compiletime/src/main/java/com/m3/compiletime/M3RegexRequirement.java)
is a static utility with a private constructor. Its public entry points are:

| Member | Source-derived contract |
| --- | --- |
| `analyze(String pattern, int flags)` | Calls stock `Pattern.compile`; catches `IllegalArgumentException` into `INVALID` with `Req.ALL`, false admission and the exception class's simple name as reason; otherwise delegates to the compiled-pattern overload |
| `analyze(Pattern compiled)` | Reads the compiled tree, effective flags and spelling; returns classification/formula/admission/reason as below |
| `Kind` | `PURE_LITERAL`, `REQUIRED`, `UNCONSTRAINED`, `REFUSED`, `INVALID`, in this ordinal order; the image stores the ordinal |
| `Req(Op op, String literal, List<Req> terms)` | Public record constructor and accessors retain supplied references; no compact-constructor validation or defensive list copy |
| `Req.Op` | `ALL`, `LIT`, `AND`, `OR`; formula operators, distinct from the serialized numeric opcodes |
| `Req.ALL` | Shared `ALL` record with empty literal and empty immutable terms |
| `Req.lit(text)` | Literal record using `text` and empty immutable terms; no null/empty/surrogate validation |
| `Req.and(input)`, `Req.or(input)` | Flatten one level of the same operator; AND removes ALL terms; OR containing ALL returns ALL; zero remaining terms gives ALL, one returns that existing term, multiple use `List.copyOf` |
| `Req.atomCount()` | Counts literal occurrences recursively, not distinct literal spellings; a LIT contributes one |
| `Analysis(Kind kind, Req requirement, boolean matcherStateAdmitted, String reason)` | Public record constructor/accessors retain supplied values; no consistency or null validation |

Record-generated equality, hashing and text rendering are ordinary Java record
operations, not validation. Constructing `Analysis(PURE_LITERAL, Req.ALL, ...)`
or a malformed/cyclic caller-owned `Req.terms` list does not become sound merely
because a public constructor accepts it. Only the analyzer's own successful
path supplies the stated classification invariants. The factories do not
repair arbitrary nested record graphs.

The String overload's comment says it never throws for bad input, but its catch
is specifically `IllegalArgumentException`: null, resource exhaustion and other
failures are not converted into `INVALID`. The Pattern overload also assumes a
nonnull, usable compiled pattern. This reference does not broaden those catch
boundaries.

### Classification and candidate semantics

1. `CANON_EQ` in `compiled.flags()` refuses before reflection, with `Req.ALL`,
   false admission and reason `CANON_EQ`
2. An unavailable reflective view refuses as `node tree not opened`; unknown
   reachable node classes or graph-node-budget failure refuse with the
   admission reason
3. A tree that is exactly one case-sensitive slice ending at `lastAccept` is
   `PURE_LITERAL` only when its decoded spelling is nonempty and contains no
   surrogate UTF-16 unit
4. A single `CharProperty` immediately followed by `lastAccept` can be
   `PURE_LITERAL` through a separate cross-check: no effective CASE_INSENSITIVE,
   COMMENTS, CANON_EQ or LITERAL flag; `M3RegexShape` decodes exactly one
   non-surrogate character as LITERAL; the compiled stock pattern matches that
   character. This is narrower than accepting every one-character predicate
5. Otherwise the conservative walker yields `UNCONSTRAINED` for ALL or
   `REQUIRED` for a nontrivial formula. Reason is `walk`

For REQUIRED, each LIT tests presence somewhere in the input; AND/OR combines
those tests. The formula does not encode their order, multiplicity, adjacency
between separate atoms, anchors, captures or other regex conditions. For example, the formula
for `foo\d+bar` can hold for `bar foo1`, yet the regex can fail. False proves
absence only under the analyzer/codec/key/lifetime invariants described here.
True is not proof of a match.

The private `Walker.sequence/step/alternation/flush` implementation accumulates
case-sensitive slice units into runs, keeps them adjacent across group/branch
connectors, ANDs sequential requirements, ORs alternatives, and treats an empty
alternative as ALL. A loop/curly body contributes only when its minimum is at
least one. Other known nodes add no requirement. Encountering a bare Loop
aborts that chain to ALL. Its separate 4,096-step budget also degrades that
chain to ALL, rather than necessarily returning `Kind.REFUSED`; the graph
admission budget and this walk budget are different checks. `hasSurrogate`
checks UTF-16 units, not code-point validity, so supplementary literals can
remain requirements without being admitted as complete pure-literal decisions.

### Matcher-state metadata is narrower and separate

`matcherStateAdmitted` is true only if graph admission found no member of the
require-end family and the compiled search root is a `Start` or `BnM` instance.
`Dollar`, `UnixDollar`, `Bound`, `GraphemeBound` and `Neg` families clear it.
This is metadata for a potential receiver's state boundary, not a claim about
all `Matcher` observations. The current gate selects only on `Kind` and does
not consult this bit. Its public API exposes no regions, bounds, captures,
match positions, successive-find state, `hitEnd` or `requireEnd`.

## Reflective Pattern-tree dependency

[`M3PatternNodes`](../../m3/ports/compiletime/src/main/java/com/m3/compiletime/M3PatternNodes.java)
is package-private. It builds one static reflective view with VarHandles into
private JDK classes; it is not a public portable Pattern-tree API. Build-time
analysis needs `--add-opens java.base/java.util.regex=ALL-UNNAMED` before the
view initializes. Construction catches `ReflectiveOperationException` and
`RuntimeException`, stores null on failure and does not retry later.

| Internal member group | Role and ownership |
| --- | --- |
| `Admission(known, matcherStateSafe, reason)`, `ABORT`, `instance()` | Admission result, sequence-abort sentinel and possibly-null singleton |
| Constructor, `create`, `type`, `typeUnchecked`, `field` | Resolve named Pattern classes and fields; private implementation coupling may fail on a changed JDK |
| `root`, `matchRoot`, `next` | Read actual retained node references; no snapshot of a Pattern graph |
| `terminal`, `isStartOrBnM` | Recognize accept/LastNode/LookBehindEnd nodes and search roots |
| `caseSensitiveSlice` | Returns the underlying `int[]` for the Slice family; caller does not own it and must not mutate it |
| `emptyWidthConnector`, `isBranch`, `branchAtoms`, `branchConn` | GroupHead/GroupTail/BranchConn handling; `branchAtoms` copies the array prefix but retains the referenced nodes |
| `isProlog`, `prologLoop`, `isLoop`, `loopBody`, `loopMin` | Loop linkage and repetition-minimum access |
| `isCurlyFamily`, `curlyAtom`, `curlyMin` | Curly/GroupCurly linkage and repetition-minimum access |
| `pureLiteral`, `singleCharThenAccept` | Exact structural fast-path recognizers used above |
| `admit`, `children` | Identity-deduplicated worklist traversal, reachability over next/branch/inner nodes, node budget and state restrictions |

The closed membership table contains LastNode, Start, Begin, End, Caret,
UnixCaret, LastMatch, LineEnding, CharProperty, XGrapheme, SliceNode, Ques,
CharPropertyGreedy, Curly, GroupCurly, BranchConn, Branch, GroupHead, GroupTail,
Prolog, Loop, BackRef, CIBackRef, First, Pos, Behind, NotBehind,
LookBehindEndNode, BnM, Dollar, UnixDollar, Bound, GraphemeBound and Neg.
Membership uses `isInstance` for those classes, plus exact Node, so subclasses
of listed classes are accepted. It is not a comparison against an exhaustive
list of exact class names. `children` follows branch alternatives/connector,
or the selected Ques/Curly/GroupCurly/Prolog/Loop/lookaround inner node, together
with `next`. This JDK-specific implementation and the producer fingerprint do
not constitute qualification of every future JDK node layout.

## Content identity and collision scope

[`M3ContentKey`](../../m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ContentKey.java)
is a public record with components/accessors `h1`, `h2`, `length`, `flags`.
Its canonical constructor accepts arbitrary values; record equality is equality
of those four values, not a text comparison.

`of(CharSequence pattern, int flags)` reads `length()` and every `charAt()`
without making a snapshot. Supply stable contents; null or custom-sequence
failures propagate. Both hashes include compile flags and UTF-16 units:

- h1 starts at `0xcbf29ce484222325L ^ flags`, repeatedly applies
  `(a ^ char) * 0x100000001b3L`, then XORs the UTF-16 length
- h2 starts at `0x9e3779b97f4a7c15L * (flags + 1L)`, repeatedly multiplies
  `(b + char)` by the same golden constant then XORs `b >>> 29`; the final
  input to package-private `fmix64` is `b ^ ((long) length << 32 | flags)`
- `fmix64` performs the three shift-XOR stages and two multiplications shown
  in the source; Java long overflow is part of the hash computation

No class, method, jar or call-site identity participates. Writer collision
checking is stricter than a combined-key check: any repeated h1 **or** h2 in
its supplied list throws, including a repeated identical item. This is a
finite input-list rejection check, not a proof that the hash functions cannot
collide. Lookup compares both hashes, length and flags but stores no key
spelling for a final comparison. A collision from outside the written input
set is not excluded by that finite check. The hashes and unkeyed SHA-256
trailer are not authenticity or hostile-input trust credentials.

## Image writer and exact packed layout

[`M3ConstantRegexImageWriter`](../../m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexImageWriter.java)
has a private constructor, public `Item(String pattern, int flags,
Analysis analysis)` record and public static `write(List<Item>, int[]
inputSetSha256)` and `sha256Ints(String manifest)` methods.

`write` creates a fresh builder, processes items in list order, assigns entry
ordinals in that order and assigns globally deduplicated atom ordinals on
first encounter. It does not sort or deduplicate items. Determinism therefore
requires the same ordered inputs, analysis values, manifest digest and producer
Pattern.class bytes. The caller's Item/Analysis/Req values are trusted;
`write` does not reanalyze patterns or validate consistency, formula arity,
24-bit operands, nulls or maximum sizes. The input digest must provide at least
eight ints: `System.arraycopy` copies the first eight without a length-equals-8
check. Size arithmetic/allocation uses int arithmetic without an explicit
large-input overflow budget. Allocation/stack errors are not normal refusals.

All serialized words are little-endian int32. The logical format name
`M3CTRX01` is not an instruction to write eight literal ASCII bytes: the first
two words are numeric constants `0x4d334354`, `0x52583031` serialized in that
little-endian order.

| Lane / word index | Content |
| --- | --- |
| Header 0..3 | MAGIC, MAGIC2, version 1, header length 32 |
| Header 4..9 | Entry count, table slots, program-int count, atom count, unit-int count, trigram count |
| Header 10 | Zero in writer output |
| Header 11..18 | Producer `java/util/regex/Pattern.class` SHA-256 represented as eight big-endian digest ints, then serialized as little-endian words |
| Header 19..26 | Supplied input-set digest in the same int representation |
| Header 27..31 | Zero in writer output |
| Table, `slots` ints | Entry ordinal + 1; zero empty; start `(int) h1 & (slots - 1)`, linear probing |
| Entries, 8 ints each | h1 low/high, h2 low/high, UTF-16 key length, compile flags, kind ordinal OR `0x10` if matcher-state-admitted, program offset |
| Program | Each entry starts with instruction count; then postfix instructions `(opcode << 24) \| operand`; zero count means no constraint |
| Atoms, 4 ints each | UTF-16 unit offset, unit length, trigram offset, trigram count |
| Units | Concatenated derived literal atoms; two UTF-16 units per int, low half first; final unused half remains zero |
| Trigrams, 2 ints each | Low/high words for `(c0 << 32) \| (c1 << 16) \| c2`, unique and sorted within each atom |
| Trailer, 8 ints | SHA-256 of all preceding bytes, represented as digest ints and serialized little-endian |

The writer's slot formula is
`highestOneBit(max(2, items.size() * 2 - 1)) << 1` (four slots for empty or
single-item lists under ordinary sizes). Entries refer to the global atom
lane. Opcodes are ATOM=1, AND=2, OR=3 with 24-bit operands; AND/OR operands are
term counts. `encode/emit` write a counted program even for ALL. Derived atom
text can equal a pure literal's pattern text, but there is no separate stored
key-spelling lane.

Private `build/assemble/header` arrange the lanes and digests. Package-private
`uniqueTrigrams` returns an empty array below three units and otherwise sorts
and deduplicates all sliding UTF-16 triples. `sha256Ints(ByteBuffer)` hashes
its remaining bytes through a duplicate; `toInts(byte[])` groups digest bytes
as big-endian ints. Public `sha256Ints(String)` first UTF-8 encodes the manifest.
The public String digest helper does not validate manifest syntax.
An unreadable producer class becomes `IllegalStateException` in the writer.

## Image admission, lookup and retained ownership

[`M3ConstantRegexImage`](../../m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexImage.java)
exposes:

| Public member | Contract |
| --- | --- |
| `Refusal` | Final checked `IOException` subtype; its String constructor is package-private |
| `Entry(kind, matcherStateAdmitted, program, atoms, atomTrigrams)` | Public record and component accessors; arrays are exposed directly, with no defensive-copy/validation constructor |
| `open(Path)` | Opens a READ channel, maps the whole file READ_ONLY and delegates to verify; channel closes, mapping remains retained by the image |
| `verify(ByteBuffer)` | Uses a little-endian duplicate and an IntBuffer view of the input's remaining region; returns an image retaining that storage |
| `entryCount()` | Reads the header count from retained storage |
| `inputSetSha256()` | Copies eight header ints into a fresh array; does not compare them with an independently expected manifest |
| `lookup(String pattern, int flags)` | Hashes the supplied key; probes at most `slots` positions, stops on zero, checks both hashes/length/flags and decodes a matching entry; miss is `Optional.empty()` |

Admission checks the remaining byte length is at least 160 and divisible by
four; numeric magic/version/header length; trailer digest over the preceding
remaining bytes; current Pattern.class fingerprint; and aggregate lane-end
arithmetic equal to the IntBuffer limit with `Integer.bitCount(slots) == 1`.
The package-private fingerprint helper reads the executing JDK's
`Pattern.class` resource, not every nested Pattern class or all runtime code.

There is **no exhaustive prepass** establishing nonnegative counts, checked
arithmetic, valid table ordinals, each entry/program/atom range, valid Kind or
opcode, stack arity/balance, all reserved bits/words, valid literal-mode shape
or trigram consistency. The constructor's aggregate geometry uses unchecked
int arithmetic. The one-bit test alone is not an explicit positive-slot
range check. A well-checksummed malformed image can survive these aggregate
checks and fail later with unchecked exceptions or incorrect semantics.
`Refusal` is not guaranteed for every malformed or hostile input. Digest
verification detects unchanged-checksum byte drift; anyone able to rewrite
content can also recompute an unkeyed digest. Restrict input images to the
intended trusted producer contract rather than treating verify as a security
boundary.

`verify` does not copy bytes, freeze a mutable buffer or retain a read-only
view by construction. Changing the caller's position/limit later does not
change the duplicate's geometry, but changing shared underlying bytes can
change a previously verified image without re-verification. Keep the backing
storage stable for its whole lifetime. `open` supplies a read-only mapping;
that is not a promise that another process cannot change the mapped file.
There is no close/unmap method or explicit mapping-lifetime control.

Private `matches/decode/atomText/atomTrigrams` read from retained storage.
Each successful lookup allocates a new instruction array, String array,
trigram arrays and decoded atom Strings. It counts ATOM *occurrences*, copies
each occurrence's atom, and rewrites global atom IDs to occurrence-local
indices. Separate lookups have separate decoded arrays, but each Entry's
accessors and public constructor expose mutable array references. Mutating an
Entry does not rewrite the mapped lanes; it can invalidate uses of that Entry.
Record-generated Entry equality/hashing use the array references rather than a
deep comparison of their elements. No caching or allocation-free lookup
guarantee follows from memory mapping.

## Gate calls, fallback and concurrency

[`M3ConstantRegexGate`](../../m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexGate.java)
retains a Pattern, one selected mode, decoded arrays and a boolean scratch
stack sized to the instruction count. The private constructor chooses PURE
for `PURE_LITERAL`, GATED for `REQUIRED`, otherwise PASS. It takes arrays from
its freshly looked-up Entry without another defensive copy. It does not check
`matcherStateAdmitted`, use trigram arrays, or independently validate programs.

| Public member | Contract and limit |
| --- | --- |
| `IMAGE_PROPERTY` | `m3.precompute.constantRegex.image` |
| `configuredImage()` | Explicitly reads that property; absent/blank returns empty; Path/open/verify `IOException` or `RuntimeException` inside the try returns empty. The initial property read is outside that catch |
| `compile(regex, flags, image)` | Stock-compiles first, preserving stock compile exceptions, then calls `of` with the original flags argument |
| `of(pattern, compileFlags, image)` | Uses `pattern.pattern()` plus the caller's original compile flags for lookup; null image gives PASS. Caller must supply the flags actually used to compile the Pattern |
| `pattern()` | Returns the retained Pattern |
| `hit()` | True only for PURE or GATED; image entry presence alone is insufficient |
| `find(CharSequence)` | On String: PURE uses `indexOf(literal) >= 0`; GATED with false formula returns false. All remaining paths allocate/use a fresh stock Matcher and call its first `find()` |
| `matches(CharSequence)` | On String: PURE uses equality; GATED with false formula returns false. Remaining paths use fresh stock Matcher.matches |
| `replaceAll(String, replacement)` | For non-PASS with false formula returns the original input reference; otherwise delegates to stock Matcher.replaceAll, including PURE positive cases |
| `split(String)` | For non-PASS with false formula returns a new one-element array containing input; otherwise calls stock Pattern.split with its default limit |
| `holds(String)` | Runs the stored postfix program using `String.indexOf`; empty program returns true. It is a necessary-condition query, not a general match result |

`Pattern.flags()` can include embedded flag effects such as `(?m)` and is not
necessarily the external compile argument. Supplying it in place of the
original flags can cause a miss or select a key for a different compile
context. The API does not prove that the caller paired the Pattern and flags
correctly. `compile` supplies the correct argument itself.

Calling `compile` or `of` does **not** automatically call `configuredImage`.
A caller must explicitly load/select the image and pass it. The property is a
loader input, not global installation. A missing/refused configured image
falls back through Optional.empty, while explicit verify/open/lookup failures
have their own propagation boundaries. In particular, an unchecked decode
failure is not caught by `of` merely because the image once verified.

`holds` evaluates ATOM instructions, and treats any non-ATOM opcode as AND
only when it equals OP_AND, otherwise as OR. It does not revalidate the
instruction set or stack shape. Soundness therefore relies on a consistent
producer and immutable live storage before decode. Unsupported/non-String
inputs to find/matches go to stock; they are not coerced to String. Null and
replacement-error behavior follows whichever actual branch executes rather
than a separate argument-validation layer.

The shared boolean `stack` is rewritten for each `holds` evaluation. **Do not
share one gate across concurrent or reentrant evaluations without external
serialization; use separate gates per concurrent worker.** Pattern's own
sharing properties do not make gate scratch thread-safe. Even PURE gates use
that stack when replaceAll/split call holds. No monitor, ThreadLocal, locking
or caller-supplied workspace is provided. There is no explicit close method.

The pure String find/matches branches avoid creating a Matcher in their source
path. Required-prune booleans and replacement return paths also avoid it,
whereas pruned split still allocates an array. These are branch observations,
not measured allocation/latency promises. The gate always stock-compiles a
Pattern through `compile`; it does not eliminate that initial compile cost.

## Tests and caller evidence read

| Evidence body | What its assertions/calls cover | What remains unproved here |
| --- | --- | --- |
| [M3RegexRequirementTest](../../m3/ports/compiletime/src/test/java/com/m3/compiletime/M3RegexRequirementTest.java), five tests | Slice literals, escapes, LITERAL/COMMENTS/CASE_INSENSITIVE, supplementary text, single-character cross-check, conservative AND/OR/optional/repeat/lookbehind rules, admission bit, invalid/CANON_EQ, and split-fastpath helper | Unopened-package initialization, graph/walk budget edges, changed JDK layouts, arbitrary record inputs and all flag/surrogate combinations |
| [M3ConstantRegexImageTest](../../m3/ports/compiletime/src/test/java/com/m3/compiletime/M3ConstantRegexImageTest.java), four tests | Repeat-write byte equality; key/flags lookup and decoded atoms/trigrams/input digest; four chosen byte offsets and a short buffer rejected; changed producer fingerprint rejected after trailer recomputation; bounded stock comparisons for find/matches/replaceAll/split, miss/null image and embedded compile flags | Exhaustive byte corruption, malicious well-checksummed lanes, hash collisions, buffer mutation, races, non-String/null/error edges, resource limits and all Matcher state |
| [M3SiteRecorder](../../m3/ports/compiletime/src/main/java/com/m3/compiletime/M3SiteRecorder.java) and [M3InventoryState](../../m3/ports/compiletime/src/main/java/com/m3/compiletime/M3InventoryState.java) | Classifies each retained distinct regex key once, stores Analysis, site/API/unit counters; split fastpaths and unknown flags are separate accounting paths | Whole-application constant discovery or semantic rewriting; see [inventory reference](compiletime-inventory-reference.md) |
| [Harvester](compiletime-harvest-reference.md#harvester-outputs-and-self-check) and [differential driver](compiletime-harvest-reference.md#differential-inputs-and-comparison-domains) | Writer/readback kind/atom-count self-check, finite sampled stock comparisons and separate oracle-corpus candidate checks | Universal soundness, deployment integration, current executed results or hostile-input qualification |

All nine direct JUnit methods are **read, NOT_RUN**. The image test name
`anyByteDriftIsRefused` does not mean it tries every byte offset. No dedicated
M3PatternNodes or M3ContentKey test class appears in the complete pinned project
test tree; their behavior is reached indirectly in the shown tests. Source
body coverage and documentation routing do not close the remaining semantic,
resource, concurrency or runtime-admission work.

## Pinned source index

All links below use the source pin named at the top; blob identities are Git
blob SHA-1 values, not runtime receipts.

| Source owner | Git blob |
| --- | --- |
| [M3PatternNodes](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3PatternNodes.java) | `0b640e6355aef525203648919d0da63ed32bdbec` |
| [M3RegexRequirement](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3RegexRequirement.java) | `bc7f55e9ea68d210263ae82bc8dab403ea63ef70` |
| [M3ContentKey](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ContentKey.java) | `734e35504bacb787a1e47bc7e39fac7212b1e810` |
| [M3ConstantRegexImageWriter](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexImageWriter.java) | `c19bd540361a8f4e3e2e7333103791bb6deb910c` |
| [M3ConstantRegexImage](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexImage.java) | `0b4a62e543ab2b6ddfe794edd1ec4ee2a83db319` |
| [M3ConstantRegexGate](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexGate.java) | `a8d3b1301d51c10aacab1fa4ce6bd9c2c17075ab` |
