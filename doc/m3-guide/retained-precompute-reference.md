<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->

# Retained typed precompute: keys, scopes, passes and plans

[Guide](README.md) · [Exact source and member coverage](retained-precompute-reference-coverage.md) · [Retained regex](retained-regex-reference.md) · [Runtime owners](runtime-owners.md)

This reference covers the eight Java sources in the public retained
`m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute`
package at `363c02caa2e5af1df9460b927ecd78f6a9dbcefb`. It specifies the inspected
source contract, including package-private support and limitations. Examples,
compilation, tests, repository scripts, policy verifiers, native code and
benchmarks were **NOT_RUN in this documentation authoring/review pass**.
Hosted exact-commit CI is separate evidence and is not reported by that status.

## 1. Responsibility and receiving boundary

The package supplies typed artifact identities, explicit context identity,
pass callbacks, graph planning and budget helpers. It contains **no execution
engine, cache owner, persistence codec, public context factory or package test
source**. A plan's `executionOrder()` is data for a caller; there is no
`plan.execute(...)` in these files. No class here automatically computes,
attaches, saves or restores facts on `String` or `M3String`.

The retained subtree has no `pom.xml`, `module-info.java` or `package-info.java`.
It is not a complete compilable module or an admitted JDK runtime dependency.
`Context` and `Limits` import `com.synexia.job.IProgressMonitor`, whose body is
not present in this retained subtree. That is a bounded source-closure
observation, not a claim about every repository root or external harness.
The [M3 reactor][reactor] lists separate projects, while the
[shell build][core-build] compiles `core/src` and named core tests. Neither is
an execution receipt for this package.

Keep these owners distinct:

- This retained `com.synexia.indexstring.precompute` package describes generic
  identity, pass and DAG mechanics
- The [retained regex reference](retained-regex-reference.md) owns regex facts,
  exact execution, request/cache identity and image persistence
- The [indexed-string bridge](indexed-string-reference.md) covers the separate
  `m3/ports/indexstring` adaptation and selected `com.m3.precompute` calls
- The [frozen-text compatibility fixture](frozen-text-compatibility-reference.md)
  covers isolated test-only retained text snapshots
- The [runtime-owner guide](runtime-owners.md) covers canonical JDK storage and
  target integration; a similarly named retained class is not that owner

The [vendor record][vendor] and [borrowing contract][borrowing] preserve multiple
pinned snapshots and canonical reusable ownership. They require receiving and
runtime qualification separately. This page changes no admission, phase, source,
license, recipe or policy. Original Apache notices and all OpenJDK/third-party
rights remain unchanged; public source custody is not blanket relicensing or
permission to import additional donor material.

## 2. Identity is explicit and layered

| Owner | What it identifies | What it does not establish |
|---|---|---|
| `MIndexPrecomputeKey<T>` | Text ID, Java `Class<T>` and positive schema version | Input source, implementation revision or result contents |
| `MIndexPrecomputeScope` | Declared specification, semantics, lexicon, compiler, binding and source digest | Actual correctness/completeness of those declarations |
| `MIndexPrecomputePass<T>` | Output/dependency keys and implementation revision | Purity or deterministic behavior of arbitrary code |
| `MIndexPrecomputePlan` | Frozen graph metadata and a fingerprint over selected identity fields | Scope identity, execution results, cache entries or callback bytecode |
| `MIndexPrecomputeArtifact<T>` | Typed value plus supplied input/content digests and retained weight | Recomputed hash, independently measured size or deep immutability |

### Typed keys and class-loader identity

[`MIndexPrecomputeKey`][key] accepts a non-null, nonblank ID, a non-null class
and `schemaVersion >= 1`. The ID is kept exactly: there is no trimming, ASCII
restriction or explicit NUL rejection. `id()`, `type()` and `schemaVersion()`
return those values. `canonicalName()` concatenates ID, NUL, `type.getName()`,
NUL and the decimal version; `toString()` instead uses `id:SimpleName:vN` as a
human-readable representation, not a decoder or persistence format.

`compareTo` orders by ID, then class **name**, then numeric schema version.
`equals` and `hashCode` include the actual `Class` object. Two same-named classes
from different loaders can compare as zero while their keys are unequal.
Plan lookup returns `-1` when its encountered comparison-equal key is unequal;
it does not search all such ties. Duplicate-output detection compares adjacent
keys by equality. Callers must therefore avoid comparator-equivalent,
Class-distinct keys in one graph and must not treat canonical names as portable
cross-loader type authority. Null comparison arguments fail with
`NullPointerException`; equality with null or another kind of object is false.

### Scope fields and revisions

[`MIndexPrecomputeScope`][scope] is a record with six components:
`specificationRevision`, `semanticProfile`, `lexiconSnapshot`,
`compilerRevision`, `bindingSnapshot` and `sourceSha256`. The first five must be
non-null and nonblank, but remain otherwise unnormalized strings. The last
must be exactly 64 hexadecimal characters and is normalized to lowercase.
Malformed length/hex is rejected; null is rejected separately. Record accessors
return these stored fields; generated equality/hash/string behavior remains
record behavior, not a fingerprint comparison override.

The [pass contract][pass] requires every external identity that can change a
result to be represented by the scope, and a stable implementation/mechanics
revision when pass behavior changes. The code cannot discover omitted input,
mutable owner/resolver state, locale/provider behavior, compiler dependencies
or changed callback code. If these affect a pass, the producer must give them
stable content/revision identity in the appropriate scope fields and/or its
implementation revision. A timestamp or an unchanging label alone is not a
proof that behavior is bound.

### Exact digest encodings

The source uses SHA-256 with lowercase hexadecimal output. A missing SHA-256
provider is wrapped by the package helper as `AssertionError`.

| Operation | Exact bytes supplied to SHA-256 |
|---|---|
| `Scope.sha256(byte[])`, `Passes.sha256(byte[])` | Raw supplied bytes, without framing; null rejected |
| `Passes.sha256Utf16(CharSequence)` | Eight-byte big-endian UTF-16 code-unit count, followed by each code unit high byte then low byte |
| `Scope.fingerprint()` | Each field length-framed in the same UTF-16 form, in order: `MIndexPrecomputeScope/v1`, specification, semantics, lexicon, compiler, binding, normalized source-digest **text** |
| Package-private `Scope.tokenDigest(long[])` | Eight-byte big-endian element count, followed by each token's eight-byte big-endian bit pattern |
| `Plan.fingerprint()` | The separately defined graph encoding in section 4 |

There is no UTF-8 conversion, code-point normalization or surrogate replacement
in the UTF-16 paths. An unpaired surrogate contributes its literal two-byte
code unit. `sha256Utf16` reads the `CharSequence` directly and repeatedly calls
`length()`; it does not snapshot a mutable sequence. Raw-byte and token helpers
also do not clone their input before reading it. Keep input stable throughout
hashing. These helpers are not authenticated commitments: a hash is an address,
not an exact semantic-equivalence or matching proof.

## 3. Pass callbacks and artifacts

### The interface and factory wrappers

[`MIndexPrecomputePass<T>`][pass] declares `output()`,
`implementationRevision()`, `compute(context)`, `retainedBytes(value)` and
`contentSha256(value)`. Its default `dependencies()` is the empty immutable
list. The interface describes deterministic passes and declared dependency
access; it does not wrap arbitrary implementations with enforcement.

[`MIndexPrecomputePasses.of(...)`][passes] returns a private `FunctionalPass`
record. Construction requires a non-null output, nonblank revision, non-null
callback functions and a non-null dependency list with no null elements. The
list is copied, while the output key and callback objects are retained.
`fixed(...)` delegates to `of` with no dependencies. Here “fixed” means this
empty dependency list: it neither memoizes a result nor promises a constant
value or independence from `context.scope()`.

The wrapper's generated output/revision/dependencies accessors implement the
corresponding interface members. Each `compute` call invokes the retained
function, rejects a null result and checks it with `output.type().cast`.
The wrapper does not check the context argument before invoking the function.
`retainedBytes` casts the supplied value, invokes its callback and rejects a
negative result. `contentSha256` casts the value, invokes its callback and
validates/normalizes the returned digest. For these two methods a null value can
reach the callback because `Class.cast(null)` itself accepts null; there is no
independent non-null value guard there. Wrong non-null types fail with
`ClassCastException`. Callback exceptions propagate; there is no retry,
rollback, automatic caching or exception-to-negative-result conversion.

### Artifact validation and ownership

[`MIndexPrecomputeArtifact<T>`][artifact] is a record whose compact constructor
checks, in order: non-null key; non-null value and class cast; valid input
digest; valid content digest; nonnegative `retainedBytes`. Its five record
accessors expose those stored values; record equality/hash/string methods are
generated and include component behavior.

The two digests serve different roles. `inputSha256` labels the supplied input
identity; `contentSha256` labels the supplied output content. The constructor
checks hexadecimal shape but recomputes neither. It does not compare input
identity against any scope/plan, compare content identity against the value,
or call a pass's size function. The weight can be zero and has no maximum here.

Final record fields freeze references, not arbitrary `T`. Arrays, mutable
objects and callback-captured state remain the producer's responsibility.
There is no payload clone, immutability verifier or disposal of a resource held
by `T`. Preserve stable value contents, digest meaning and retained-size
accounting for the entire period in which downstream work relies on them.

## 4. Building and inspecting a graph

[`MIndexPrecomputePlan`][plan] retains sorted key/pass arrays, CSR dependency
lanes, a topological order and a precomputed fingerprint. `Builder` is a public
static nested class with an implicit public constructor; `builder()` is its
convenience factory. `add(pass)` rejects null, appends the same pass reference
and returns the builder. It does not execute or validate all pass metadata.

`build()` performs the following source-defined steps:

1. Copy the builder list to a pass array and sort it by `output()` key
2. Read and retain each output key, require a nonblank implementation revision
   and reject adjacent equal output keys
3. Read each dependency list, copy it to validate list/elements, and total its
   size with `Math.addExact` into a prefix-offset array
4. Read the pass dependency lists again, resolve every key with binary search,
   reject missing and self dependencies, sort row numbers and reject duplicates
5. Form a deterministic topological order with reverse dependency lanes and a
   primitive min-heap of ready rows; reject a cycle if no row is ready before
   all rows are emitted
6. Construct the plan and calculate its graph fingerprint

Dependencies are direct edges, not a transitive closure. For row `r`, the
private `dependencyStart[r]..dependencyStart[r+1]` interval addresses the flat
`dependencyRows` lane. The public `dependencyRows(r)` returns a copy of just
that row's sorted dependency rows. `executionOrder()` returns a clone. Invalid
public row indices fail through `Objects.checkIndex`; `keyAt(r)` and
`passAt(r)` return the retained key and **original pass object**, respectively.
`size()` reports key count and `fingerprint()` returns the stored digest.

With stable keys and compatible class identity, the min-heap always emits the
smallest currently ready row, hence the smallest ready key. This is not an
arbitrary insertion-order schedule. An empty builder is accepted and produces
size zero and an empty order. The plan has no executor, futures or parallel
scheduling policy.

### Freeze boundary and failure behavior

The topology arrays are private and public array-returning methods copy them.
The plan does not retain the builder's list, but the caller's builder remains
usable and retains its own passes; `build()` does not clear or consume it.
Later builder additions do not add rows to an existing plan.

Pass implementations and captured state are not frozen. `output()` can be
called during sorting and again during key collection. `dependencies()` is
called in separate sizing and resolution passes; the temporary validation copy
is not the list subsequently used for resolution. `implementationRevision()`
is read during validation and again while fingerprinting. Keep all of these
stable for the whole build and thereafter when pass behavior is consumed.
Mutation or concurrent changes are unsupported and can produce other failures
or inconsistent metadata; the source does not promise to turn every such
change into a clean validation exception.

For stable inputs, duplicate output/dependency, missing dependency, self-edge
and cycle refusals are `IllegalArgumentException`; null metadata is rejected
by the relevant null/list operations. Dependency-count and reverse-prefix
addition use `Math.addExact` and can throw `ArithmeticException`. Allocation
failures are not caught. There is no success object on a failed build, no
`compute`/`retainedBytes`/`contentSha256` callback invocation, no resource cleanup
and no automatic `Limits` admission.
In particular, this builder takes no `MIndexPrecomputeLimits` and enforces none
of its node/edge/cell/work/byte maxima.

### Graph fingerprint

The private [fingerprint encoder][plan-fingerprint] writes:

1. Length-framed UTF-16 `MIndexPrecomputePlan/v1`
2. Eight-byte big-endian number of keys
3. For each sorted row: framed canonical key name, framed current pass
   implementation revision, eight-byte dependency count, and framed canonical
   names of the sorted direct dependency rows

This encoding does not include scope fields, callback bytecode/captured state,
computed values, result hashes, retained weights or class-loader identity.
The execution order is derivable from the graph and is not separately encoded.
The fingerprint is calculated once; mutating a retained pass later does not
refresh it. Equal fingerprints do not establish equal callbacks, authenticated
inputs or semantic equivalence. Persistent reuse needs the missing execution
owner's complete identity/validation contract as well.

### Planning-only example

The following illustrates source-level construction and row ordering. It was
inspected, **not compiled or executed**, and is not a standalone module or an
execution-engine example. A real receiving environment must resolve the full
public dependency/build closure before using these types.

```java
var textKey = MIndexPrecomputeKey.of("z.text", String.class, 1);
var lengthKey = MIndexPrecomputeKey.of("a.length", Integer.class, 1);
var textPass = MIndexPrecomputePasses.fixed(
    textKey, "literal-v1", context -> "abc",
    value -> 2L * value.length(), MIndexPrecomputePasses::sha256Utf16);
var lengthPass = MIndexPrecomputePasses.of(
    lengthKey, "utf16-length-v1", java.util.List.of(textKey),
    context -> context.require(textKey).length(),
    value -> 0L,
    value -> MIndexPrecomputePasses.sha256Utf16(value.toString()));
var plan = MIndexPrecomputePlan.builder().add(lengthPass).add(textPass).build();
// Sorted rows: a.length = 0, z.text = 1
// dependencyRows(0): [1]; executionOrder(): [1, 0]
```

Those size callbacks are illustrative accounting declarations, not measured
JVM sizes. This snippet does not run either `compute` callback, instantiate a
context, calculate an artifact's input hash or write a cache entry.

## 5. Context exposes only available direct dependencies

[`MIndexPrecomputeContext`][context] has a **package-private** constructor.
It requires non-null scope, limits, plan and artifact array, checks `passRow`
against plan size, and normalizes a null monitor with `IProgressMonitor.noop()`.
It retains the artifact array without cloning or checking its length/content
against the graph. The absent execution owner must populate the correct rows
and maintain their publication/lifetime discipline. A malformed array can fail
on indexing; “read-only view” does not mean an immutable snapshot of all values.

`scope()`, `limits()` and `monitor()` return the retained objects. Dependency
access is checked against the current pass row's direct CSR interval:

| Access | Unknown key or undeclared row | Declared row with null artifact | Declared row with an artifact |
|---|---|---|---|
| `contains(key)` | `false` | `false` | `true` |
| `artifact(key)` | `IllegalArgumentException` identifying the undeclared dependency | `IllegalStateException` reporting it is not computed | New typed artifact wrapper |
| `require(key)` | Same failure as `artifact` | Same failure as `artifact` | The wrapper's value |

All three reject a null key. A transitive predecessor is not accessible unless
it is also declared directly, and the current pass's own output is not
implicitly available. `contains` only tests presence/permission; it does not
cast the stored payload or authenticate its metadata.

`artifact` casts the stored value to the **requested** key's class, copies its
digest strings/weight into a newly validated artifact wrapper and returns that
wrapper. It does not compare the stored artifact's key with the requested key,
verify scope/input/content identity, clone the payload or re-execute a pass.
Correct row placement is therefore a producer obligation. Accessors perform no
automatic cancellation check or task begin/done. This context is not the
separate owner/resolver/context-cache invalidation machinery of other packages.

## 6. Limits, primitive accounting and work lifecycle

[`MIndexPrecomputeLimits`][limits] is a record of declared budgets, with default
values `maxNodes=1_000_000`, `maxEdges=8_000_000`, `maxCells=4_000_000`,
`maxWork=100_000_000` and `maxBytes=268_435_456`. Construction requires positive
node/cell/work/byte maxima and a nonnegative edge maximum. Record accessors and
generated equality/hash/string methods are ordinary record behavior.

The source comment limits byte estimates to primitive arrays and excludes JVM
headers and caller-owned data. There is no general memory measurer, native
allocator, shared-array dependency, summed graph-byte estimator or enforcement
of callback allocations in these eight files.

Package-private support provides:

- `bytes(n)`: reject negative values or values above `maxBytes`; zero is allowed
- `arrayLength(n)`: reject values outside `0..Integer.MAX_VALUE-8`, then narrow
  to `int`; it does not allocate or establish sufficient memory
- `Work`: a package-private `AutoCloseable` work counter over `maxWork`

`Work` construction chooses a no-op monitor for null, reads `limits.maxWork()`,
checks cancellation and begins a task using `IProgressMonitor.UNKNOWN`.
A failure before successful construction has no automatic `done()` in this
constructor. Each `charge(amount)` checks cancellation first, rejects a
negative amount or `amount > maximum-used`, increments `used`, then calls
`monitor.worked(amount)`. Zero is permitted. The subtraction guard prevents a
successful charge from overflowing the admitted total, assuming its state is
used as intended. If `worked` throws, the increment has already occurred;
there is no rollback.

`close()` checks cancellation in a `try` and calls `done()` in `finally`, so
`done()` is attempted even when that final cancellation check throws. Monitor
exceptions propagate; `done()` itself can throw. There is no closed flag,
idempotence guard, synchronization or automatic charging on arbitrary work.
Repeated close attempts call the monitor again, and this helper does not reject
charging after close. Callers must bracket its intended lifetime and charge all
relevant work. The imported monitor implementation and its exact cancellation
exception behavior are outside this retained source closure. No other class
in this eight-file package constructs `Work` or automatically applies these
helpers to a plan or pass.

## 7. Cached facts, persistence and exact proof stay separate

The only non-package caller of these eight types found in the inspected
retained Java subtree is [`MIndexRegexPrecomputeKey`][regex-key], using
`Passes.sha256Utf16` for expression and runtime identity. That helper constructs
an `fs.M3PrecomputeKey`; it does not run an `MIndexPrecomputePlan`.
The separate [`MIndexRegexPrecompute`][regex-precompute] drives
`M3PrecomputeCache`, codec and store operations. See the
[retained regex reference](retained-regex-reference.md) for cache-hit validation,
explicit unsupported results, compiler failures, exact execution tables and
save/load boundaries. The sibling filesystem package is a dependency boundary,
not an additional set of fully documented primary owners here.

The regex key's fixed Program/codec owner fields still contain the original v7
hashes. Public successor receipts describe later Program and codec bytes;
those fixed fields do not automatically hash current code. Its request contract
requires `compilerRevision` and `codecRevision` to bind implementation and all
behavior-bearing dependencies. Generic scope/pass revision strings have the
same caller-declaration limitation: construction cannot prove their completeness.

In this generic package:

- `Artifact` has no encoded form, save/load method or negative-result state
- `Plan` freezes a schedule but neither executes it nor caches its artifacts
- No method combines scope, plan and dependencies into `inputSha256`
- No method verifies a supplied output digest against `T`, restores resource
  lifetimes, chooses an eviction policy or ensures cold/warm parity
- A typed or content-addressed candidate fact is not an exact positive proof
  of a regex match, text equality, parser result or any other domain conclusion

Consumers must preserve the original exact semantic authority. If they store
approximate/candidate metadata, they must carry its meaning and refusal/unknown
states explicitly, use sound rejection rules, and perform the required exact
verification before accepting a positive result. This package does not supply
such domain algorithms or proof rules.

## 8. Public lineage and evidence limits

All eight sources match their [v7 export rows 18–25][v7-export], which record
donor revision `80d288951148548ef6aa4c95df9cb31a438aac16`. That revision is public
receipt metadata; this reference reads the public M3JDK21 files, not a private
donor checkout. The companion records exact Git blob and SHA-256 identities.

The [v7 README][v7-readme] distinguishes historical source custody, importer
qualification and open receiving gates. Its [qualification record][v7-proof]
reports bounded host AST/importer evidence and expressly excludes portable
source-complete receiving, actual consumer wiring, matched JDK/jtreg,
performance and full-family qualification. It is not a test report for these
eight classes. The [Program successor authority][program-successor] and
[codec recovery][codec-recovery]/[nested-slice receipt][codec-slice] preserve
additive lineage and bounded receiving status. This page neither rewrites their
historical payloads nor substitutes another candidate for that lineage.

No generic-package test body exists in the retained `src/test` tree at this
pin. The five physically retained tests/proofs are AST-specific and are not
claimed as coverage here. All runtime/build/example execution for this reference
is **NOT_RUN in this documentation authoring/review pass**. Hosted CI for a
published commit must be checked separately and is not implied by this status. The complete eight-source/member map establishes documentation
routing only; class-loader edge cases, mutable callbacks, malformed producer
rows, cancellation, concurrency, budgets and persistence integration still need
appropriately admitted behavioral evidence before product reliance.

[artifact]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeArtifact.java#L7-L20
[context]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeContext.java#L8-L76
[key]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeKey.java#L12-L74
[limits]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeLimits.java#L6-L54
[pass]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePass.java#L6-L28
[passes]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePasses.java#L14-L98
[plan]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L19-L272
[plan-fingerprint]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputePlan.java#L244-L271
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/pom.xml#L12-L37
[core-build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/build.sh#L4-L20
[vendor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/README.md#L34-L79
[borrowing]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache2-borrowing-contract.md#L61-L110
[v7-export]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/export.tsv#L18-L25
[v7-readme]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/README.md#L4-L12
[v7-proof]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/evidence/QUALIFICATION.json#L1-L56
[program-successor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/successors/adjacent-finite-runs-20261007/CURRENT_ACTUAL_AUTHORITY.md#L4-L20
[codec-recovery]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/qualification/regex-copy-recovery-20261011.json
[codec-slice]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/qualification/regex-nested-slice-20261011.json
[regex-key]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecomputeKey.java#L11-L39
[regex-precompute]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecompute.java#L18-L110
[scope]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexPrecomputeScope.java#L9-L73
