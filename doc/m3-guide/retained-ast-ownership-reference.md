<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->

# Retained AST ownership and lifetime reference

This reference describes nine retained Java source owners in `com.synexia.indexstring.ast` at M3JDK21 revision `363c02caa2e5af1df9460b927ecd78f6a9dbcefb`. It explains source behavior, including incomplete and failure paths; it does not establish a supported runtime API or a source-complete build. The [coverage companion](retained-ast-ownership-reference-coverage.md) identifies every primary owner, member group, test and custody hash.

The retained AST package contains 37 production Java files, not the entire donor AST family. The containing retained `synexia-indexstring` subtree has no `pom.xml` or `module-info.java` and lacks required text/resolver, grammar, context and cache owners. The [vendor README][vendor] and [delivery policy][delivery] distinguish these original-package custody snapshots from compiled product code. The separately built `ports/indexstring` bridge, frozen-text compatibility fixture, and canonical `java.lang.String`/`M3String` owners remain different surfaces; see [indexed-string ownership](indexed-string-reference.md), [runtime owners](runtime-owners.md) and [custody package inventory](packages/custody-01.md).

During this documentation authoring/review, compilation, tests, repository scripts, native/library loads, benchmarks and JDK runtime qualification are **NOT_RUN**. Hosted CI results, if any, belong to their exact commits and reported checks and are separate from this source inspection. Historical receipts are attributed only to their stated scope.

## 1. Five identities and their owners

| Value | Identity and retained state | What it does not establish |
| --- | --- | --- |
| `MIndexAST` | Final pool reference plus stable positive integer handle; `equals` requires the same pool object and handle | Cross-pool equality, source position, semantic Java equivalence |
| `MIndexASTPool` | Append-only atom arenas, ordered child-handle arena, canonical label-pool reference, language ID, interning table and derived scalar metadata | An evicting owner, source file, bounded session memory, parser or runtime admission |
| `MIndexASTDocument` | One source path, logical UTF-16 digest, root and copied occurrence/position/topology lanes, copied diagnostics list | Original file bytes, retained complete source text, attribution or executable validity |
| `MIndexASTPartialCache.Key` | Trimmed source reference, lowercase 64-hex UTF-16 digest, AST-spec fingerprint | File lease, authenticated source retrieval, pool identity or equality of full source characters |
| `Resident` / derived index | Strong references to a document and optional immutable projections; a new resident represents an upgrade | Ownership transfer away from active callers, global cache coherence or immediate reclamation |

One atom may appear at several document occurrences and in several documents sharing a pool. Pool-local handles must not be treated as globally meaningful IDs. `MIndexASTOccurrenceSource.syntaxIdentityDomain()` defaults to the source object, isolating reuse; `MIndexASTDocument` overrides it with its root pool. The interface permits a shared domain only when each handle has immutable syntax for that owner's lifetime, and explicitly forbids substituting a hash, name or evicting-cache generation. Its other methods expose the source digest, three root hashes and occurrence coordinates without parser or rewrite authority. [Atom identity][atom], [document identity][document], [occurrence membrane][occurrence]

### Immutable values, mutable retention

Atom fields do not mutate. Published pool rows remain stable, but the pool and its label owner can grow; retaining one atom retains its pool, which can include unrelated atoms. The public `labels()` accessor exposes that owner rather than a detached copy. The pool has no `close`, removal, reset or per-document reclamation operation. A document retains the pool through its root. Derived navigation/source indexes likewise retain their document; pool precompute retains its pool and a bounded snapshot limit. Their internal query/codec contracts are routed separately through the [retained navigation reference](retained-ast-navigation-reference.md).

`precompute()` on a pool and `precompute()` / `sourceIndex()` on a document invoke builders each time; these methods do not memoize an instance locally. `Resident` can retain their results. Sharing immutable values therefore differs from sharing a bounded cache entry or from maintaining a permanently current index over a growing pool. [Pool access/builders][pool-access], [document access/builders][document-access], [resident projections][resident]

## 2. Canonical pool admission and atom operations

Both pool constructors reject null ownership inputs and negative language IDs. The resolver constructor creates a `MatIndexStringPool`; the other retains the supplied label pool. `languageId` is used for label interning, while `spec()` always returns `MIndexLanguageSpecs.java21()`. It does not select a different grammar by language ID. Initial node lanes have length 16; child storage and interning slots start at 32. Metadata packs the `Tree.Kind` ordinal in eight bits and flags in the remaining 56; initialization rejects a runtime with more than 256 kinds. [Pool construction][pool-construct]

Admission has these boundaries:

- `internLabel(CharSequence)` delegates text admission with the pool language ID. The `MIndexString` overload requires resolver object identity and interns its tuple. These text owners are dependencies absent from this retained subtree, so their complete storage and concurrency contracts are not inferred here
- `atom` overloads accept no label, a `CharSequence`, `MIndexString` or `MatIndexString`. Null labels map to handle zero in all labelled overloads; only the `CharSequence` overloads also explicitly collapse empty text to zero. A nonnull `MatIndexString` must belong to the exact label pool
- Public child arrays are converted to new integer arrays. Null children and children from another AST pool are rejected. Raw integer child admission is package-private and validates every existing handle
- The synchronized admission core rejects null kind/children, kinds whose minimum release exceeds the Java specification release, flags outside 56 bits and unknown label handles. It checks label/flag policy: unlabelled rules reject nonzero labels; `NONE` requires zero flags; `BOOLEAN` requires 0/1; modifier bits must be in the spec mask; enum-plus-one encoding must fit the accepted integer bound. It does not validate arbitrary child sequences against a Java grammar or prove semantic validity. Enum policy does not validate the ordinal against an individual enum's constants
- Interning uses a hash to select candidates, then compares packed metadata, exact label handle and the complete ordered child-handle sequence. A hit returns another tiny wrapper for the existing handle. A miss reserves capacity, copies children, fills metadata and publishes the new positive `size` last

[Labels and overloads][pool-admission], [payload and child checks][pool-payload], [exact interning][pool-intern]

The append path is synchronized; arena references, size and child size are volatile. Ordinary accessors are not synchronized snapshots of the entire pool. The code supports stable published rows over append growth, not a blanket guarantee that external label operations, multi-call observations or all failed allocations are transactional. Arena growth and `Math.addExact` can fail. Child storage/count is updated before every derived count is computed; no rollback is implemented if later computation fails. A failed parse/session attempt can also have already admitted labels and atoms. No recovery from genuine VM exhaustion is claimed. [Admission ordering][pool-admission], [growth and bounds][pool-growth]

### Atom metadata and equality

`pool`, `handle`, `kind`, `flags`, label accessors and child operations are views over pool-owned rows. `value(handle)` creates a wrapper after checking `1 <= handle <= size`; child ordinals are checked. `label()` returns an optional canonical `MatIndexString`; `labelMIndexString()` builds a compact wrapper over its tuple. `childHandles()` is a lazy stream over the immutable children. `toString()` materializes the label when present.

`spec`, `ruleIndex`, `rule`, `astSpec`, production plans/dispatch and formal-expression spec delegate through shared language metadata; `ruleIndex` is the kind ordinal. Category, label/flag policy, operator status/precedence/associativity, minimum release and preview accessors report those tables. `hasModifier`, `booleanFlag` and `enumFlagOrdinal` reject the wrong flag policy. The enum accessor returns `-1` for zero and otherwise decodes `flags - 1`. These tables and grammar dependencies are owned by the [retained specification reference](retained-ast-specification-reference.md), not by each atom. [Atom methods][atom]

| Operation | Source-level meaning |
| --- | --- |
| `exactHash64` | Kind-name-derived metadata, flags, label feature, ordered child exact hashes |
| `structuralHash64` | Kind/flags and ordered child shapes, ignoring every label |
| `logicHash64` | Kind/flags/child logic hashes, retaining labels only for primitive types and kinds ending `_LITERAL` |
| `simHash64` | Locality-sensitive fingerprint, only an approximate candidate signal |
| `kindSignal64` / `mayContainKind` | 64-bit kind signal; a negative rules out that kind, a positive may collide |
| `containsKind` | Signal rejection followed by exact kind traversal |
| `expandedNodeCount`, `depth`, `expandedLeafCount` | Expanded tree metrics, counting repeated DAG children on each edge; a leaf has depth one |
| `contentEquals`, `structurallyEquals`, `normalizedLogicEquals` | Selected hash rejection followed by paired traversal of metadata, ordered children and the applicable labels |
| `sha256` | On-demand recursive tree digest using kind names, flags, labelled/unlabelled marker, length-prefixed UTF-8 label, child count and child digests |
| `reuseKey` | Delegation to a separate candidate-key owner; no new equality authority |

Cross-pool label comparison checks a compact label hash and materialized text. Exact equality does not accept hash equality alone. Structural/normalized logic equality is a syntax comparison, not name binding, type checking, effect equivalence or safe rewrite permission. The digest allocates recursively without memoizing per-atom digests; no stack/work bound or performance claim follows. `equals`/`hashCode`, unlike the content comparators, use owner object identity and handle. [Comparison and digest][pool-compare], [hashes and metrics][pool-hashes]

## 3. Parsing, partial trees and document construction

`MIndexASTParser` retains a nonnull pool. `parse(String, CharSequence)` requires nonnull path/source and obtains stable input by retaining `String`, `MIndexString` or `MatIndexString`, otherwise calling `toString()`. This is not synchronization with a concurrently changing arbitrary `CharSequence`. The indexed overload materializes only the path through `asString()` and forwards the indexed source. A JDK compiler must be available through `ToolProvider`; otherwise parsing throws `IllegalStateException`.

The invocation is `--release <pool.spec().release()> -proc:none`, currently the Java 21 spec. It invokes `JavacTask.parse()` only, not attribution, analysis or code generation. Exactly one nonempty compilation unit is required. Syntax errors may yield a partial document with diagnostics; they are not automatically thrown as a cache admission failure. A no-error parse does not prove semantic Java validity. The source object uses an `mindex` URI, replaces backslashes with slashes and ensures a leading slash; it does not read the filesystem. Invalid URI construction is reported as `IllegalArgumentException`. `IOException` from parsing propagates. [Parser entry points][parser-entry], [source object][parser-source]

The private `Builder` traverses javac trees, atomizing children before their parent. It records occurrences in postorder, so children precede parents and sibling rows increase. Per-tree frames carry fresh child atom and occurrence arrays; the occurrence builder fills parent/first-child/next-sibling lanes with `-1` sentinels. Position lanes use javac `SourcePositions` and can retain unavailable/negative positions. The final document stores the digest of logical Java UTF-16 units, high byte then low byte, not encoded input-file bytes. Encoding/BOM differences that decode to the same characters therefore need not change the digest. [Builder and labels][parser-builder], [occurrence builder][parser-occurrences]

Labels preserve selected names (identifiers, member selections/references, methods, classes, variables, type parameters, statement labels and break/continue labels), primitive type names, and literal values. Literal values are obtained from javac and stringified, not retained as their original lexical spelling; an absent/empty resulting label becomes zero. Comments, formatting and whole subtree source text are not atom identity. Flags encode modifiers, static imports, and reference/case/lambda/module enum modes. Modifier ordinals beyond the lane throw. These are syntax extraction rules, not a lossless source serializer. [Label/flag extraction][parser-builder]

Diagnostics preserve kind, line, column, start/end, code and `Locale.ROOT` message. Code/message text is interned in the shared label pool, so even diagnostic and path labels contribute to pool retention. `MIndexASTDiagnostic` checks nonnull kind/code/message but imposes no extra bounds on positions. Record component accessors and generated value methods follow Java record semantics. [Diagnostic conversion][parser-entry], [diagnostic record][diagnostic]

### Document validation and API

The package-private document constructor clones the 32-byte digest and every primitive lane and copies the diagnostics list. It checks equal lane lengths, valid root occurrence, root handle equality, root parent `-1` and depth zero; every atom must be in the current pool range. Depths must be nonnegative. It rejects `end < start` only when both are nonnegative. Nonroot parents must be later in the table and have depth one lower; a first child must be earlier, and a next sibling later, or be `-1`. These checks are not a complete proof that all child/sibling links reproduce the atom's child list or that each row occurs exactly once in traversal. [Document construction][document-construct]

`path`, `root`, `spec`, root hash accessors and `sourceUtf16Sha256Hex` expose identity; the digest-byte getter clones again. Occurrence accessors bounds-check their row. `childOccurrences` creates a new array by following the sibling chain. `occurrencesOf` rejects a foreign-pool atom, then scans handles. `findOccurrences` rejects a foreign-pool query, then runs that query for each occurrence. `diagnostics` returns the copied list and `hasErrors` tests for `Diagnostic.Kind.ERROR`. No source-edit or mutation operation is exposed. [Document accessors and searches][document-access]

## 4. Cache values, keys, loaders and weights

The partial cache is a strong-reference residency owner with independent entry and weight limits. The three constructors use the default estimator/default pressure policy or supplied callbacks; the first two choose LRU. The six-argument constructor and two `windowTinyLfu` factories select opt-in Window-TinyLFU. All ownership/callback/policy references must be nonnull and resident weights must be strictly positive. Underlying constructor/budget behavior is delegated to cache implementations absent from the retained subtree. [Construction and factories][cache-construction]

`Key` trims `sourceRef` and hash strings, rejects empty text and any hash not lowercase 64-hex, and accepts a caller-supplied `long` fingerprint. `java21` supplies the current spec fingerprint. Use `java21Source(ref, source)` for a Java `String` source: `java21(ref, String)` means an already-computed hash, whereas the `CharSequence` overload hashes text. Key hashing directly reads the supplied characters and does not snapshot them. [Key and callback interfaces][cache-key]

`SourceLoader` retrieves caller-owned source; `Loader` returns a document. `isolatedJava21Loader` creates a fresh exact resolver and AST pool for each invocation, exchanging cross-file atom sharing for potentially reclaimable per-load segments. This loader alone does not compare the returned document with the key; the cache performs validation. `DetailLoader` can hydrate a requested `Resident`; its default one-argument load asks for `DOCUMENT`, and its default upgrade builds projections in heap. No file, SQLite store or durable cache is created by these interfaces. References in notices to tiered/cold-storage owners do not make those missing implementations available. [Loader contracts][cache-key], [isolated/session factories][cache-construction]

`Detail` is a set of requests rather than a cumulative ordinal level:

- `DOCUMENT` requires the base document only
- `NAVIGATION`, `SOURCE_INDEX` and `CONSTANT_TIME_LCA` each require that one optional projection; none requires the other two
- `FULL` requires all three plus pool precompute

Resident accessors expose optionals. Default upgrades preserve the same document, retain existing projections and build only missing requested ones. Package-private `with...` factories/replacements enable hydration; they check nonnull inputs but do not independently verify projection/document correspondence. Custom detail loaders must provide correct, satisfying projections: cache validation checks the document hash/spec, not every projection or the returned detail after a custom upgrade. [Resident construction, satisfaction and upgrades][resident]

`estimatedRetainedBytes()` computes `128 + 36*occurrences + 80*poolAtoms + 4*childHandles + 96*labels + 8*labelTokens + 32`, then adds each present projection's reported primitive bytes using exact arithmetic and returns at least one. It is an estimate over the pool's current counts, not allocation accounting, an exact live-heap size or a bound on shared ownership. A shared growing pool can change the estimate after admission; a custom weigher can also be invoked more than once by the policy adapter. Immutable resident fields do not freeze mutable external accounting. [Estimator][resident]

## 5. Loads, upgrades, failures and close

`get(key)` requests `DOCUMENT`; `get(key, detail)` checks open state, reads residency, joins or owns a per-key miss flight, then performs a per-`(key, detail)` upgrade if needed. A miss owner rechecks residency after winning the flight, calls either loader interface, validates UTF-16 digest and AST-spec fingerprint, checks open state again, attempts admission, enforces the active budget, completes the future and removes its flight. The cache does **not** compare `document.path()` with `key.sourceRef()`, reread source on a hit, or compare complete source characters with a digest. Loader/source-reference correctness remains caller responsibility. [Get and fresh paths][cache-read], [load flight][cache-flight], [validation][cache-tail]

A requested load can succeed without retention: an overweight/frequency-rejected resident is returned to owner and joined callers, and a successfully admitted resident can be removed by the active pressure trim before return. The next miss can load again. Ordinary upgrades return a new resident and attempt replacement; a rejected upgrade is still returned to the requester. The retained old entry's fate follows the backend's outcome, not an unconditional replacement promise.

Miss flights are keyed by `Key`, so different detail requests can join the same initial load and subsequently upgrade. Upgrade flights are keyed by both key and detail; distinct details may compute concurrently from different bases, and replacements are not merged by a per-key compare-and-set. Do not infer globally monotonic detail accumulation from immutable resident values. Callbacks run on the owner caller, without an executor managed by this cache; recursively requesting the same active key/detail from its own callback can wait on its own future. No callback reentrancy/deadlock guard is implemented. [Flight algorithms][cache-flight]

| Path | Exception, waiter and counter behavior |
| --- | --- |
| Miss owner `Exception` | Increments `loadFailures`, preserves an existing `LoadException` or wraps with the key, completes joiners exceptionally, removes the flight |
| Miss owner `Error` | Completes joiners exceptionally with that error, increments `loadFailures`, rethrows the identical error, removes the flight |
| Upgrade owner `Exception` | Creates a `LoadException`, completes joiners and removes the flight; does not increment `loadFailures` |
| Upgrade owner `Error` | Completes joiners with the error and rethrows it; no failed-upgrade counter increment is implemented |
| Joined waiter interruption | Restores that waiter's interrupt flag and throws `LoadException`; does not cancel the owner |
| Joined exceptional future | Propagates an existing `LoadException`, otherwise wraps its cause with the key |
| Cancelled future | Wrapped in `LoadException` with cancellation cause |

Owner callback interruption is caught through the ordinary exception path; unlike interrupted waiting in `await`, that path has no explicit interrupt restoration. Successful miss/load admission processing increments `loads`; successful upgrade processing increments `upgrades`; rejected upgrades increment `upgradeRejections`. These are distinct from backend hit/miss statistics. [Flight and await handling][cache-flight], [await and validation][cache-tail]

### Fresh verification and invalidation

`getFresh(key)` always calls `loader.load(key)` independently of retained entries and in-flight maps, validates the resulting document, checks open state and returns a new document-only resident without admission or weighing. With a `DetailLoader`, the one-argument loader contract supplies the document. It neither replaces nor invalidates the old resident. It requires live retrieval by the loader and, for file custody, a caller-held file lease. Its `Exception` path increments `loadFailures` and wraps; it has no `Error` catch or corresponding failed-load increment for an escaping error. [Fresh load][cache-read]

`getIfPresent` delegates a residency `get`; `contains` delegates membership; `invalidate` removes one resident; `invalidateSource` trims the reference and removes matching resident keys; `clear` removes residents; `keysInLruOrder` delegates backend ordering. These check open state. Invalidation and clear do not cancel flights or establish a generation barrier, so an in-progress load/upgrade can later re-admit. They do not revoke previously returned objects. [Residency operations][cache-residency]

`close()` sets an atomic terminal flag once, clears residents, cancels the currently enumerated load/upgrade futures and clears both flight maps. New public reads/rebalance operations fail their open check. It does not interrupt a loader task by owning its thread, close a loader/source, or free callers' references. `snapshot`, policy/hard-limit/admission/active-budget accessors remain callable without `ensureOpen`.

Close and admission are separate operations: the second open check is not atomic with `put`, and flight registration is not atomic with close's enumeration. Therefore the source does not establish a linearizable no-repopulation guarantee for every close race. Deterministic tests cover particular Error/cancellation schedules; they do not establish all concurrency schedules or genuine exhaustion recovery. [Close and flight ordering][cache-flight]

## 6. Pressure and residency policy boundaries

`HeapUsage` requires `0 <= used <= max` and positive max. `current()` uses the management bean's maximum, then committed bytes, then at least one byte of used space as fallback. `ratio()` is used/max. `PressurePolicy` requires strictly increasing thresholds in `(0,1]` and descending positive retention fractions no greater than one. Classification rejects nonfinite/out-of-range ratios. Defaults are:

| Level | Threshold | Fraction of configured limits |
| --- | --- | --- |
| NORMAL | Below 0.70 | 1.00 |
| ELEVATED | At least 0.70 | 0.75 |
| HIGH | At least 0.82 | 0.50 |
| CRITICAL | At least 0.90 | 0.25 |
| EMERGENCY | At least 0.96 | 0.00 |

All `rebalance` overloads converge on the supplied heap sample, desired fraction in `[0,1]`, and optional minimum pressure level. The greater pressure level wins; the smaller of its fraction and desired fraction applies to both hard entry/weight limits. Positive fractions floor each scaled result with a minimum of one; zero produces zero. The cache stores that temporary budget and trims; every later successful admission reapplies it. A later rebalance may relax it. This is caller-driven sampling, not an automatic pressure thread or proof against allocation failure. `PressureResult` retains the heap, level, targets, trim and before/after snapshots. [Pressure types][cache-pressure-types], [rebalance][cache-rebalance]

`Snapshot` contains backend LRU-shaped statistics plus loads, failures, uncached-oversize loads, upgrades and upgrade rejections. `AdmissionSnapshot` contains policy, frequency admissions/rejections/comparisons, sketch decays/bytes and window fraction; nonnegative counters and finite `[0,1)` fraction are checked. `frequencyAdmissionRejections()` is a separate counter for outer cache rejection outcomes. Record accessors and generated value methods expose these immutable snapshots, not a transactionally consistent observation of all concurrent counters. [Snapshot records][cache-snapshots], [diagnostics accessors][cache-observe]

The package-private `MIndexASTResidentCache` selects one backend and delegates get/put/remove/removeIf/clear/trim/contains/order/size/weight/limits/snapshots. Its `PutOutcome` rejects negative admitted weight, null trim and admitted-plus-rejected contradictions. LRU forwards to `WeightedLRUCache`, maps a nonadmitted result to oversize rejection, and returns zero frequency diagnostics. Window-TinyLFU uses `WeightedWindowTinyLfuCache` with its default window fraction and protected fraction `0.80`, synchronizes operational methods, maps segmented trim/removal results, and maintains logical LRU-shaped counters. Its admission sketch hashes source digest plus spec fingerprint, omitting the source reference; this is a frequency hint, while actual key identity remains the record. [Resident adapter][adapter]

`WeightedLRUCache`, `WeightedSegmentedLRUCache` and `WeightedWindowTinyLfuCache` are not retained implementations here. The adapter's forwarding code and source comments do not independently prove their amortized complexity, eviction ordering, thread safety, replacement atomicity, sketch properties or retained-byte precision. The public cache's LRU-shaped return types expose that dependency rather than eliminate it.

## 7. Retained single-source edit sessions

`retainedJava21Session` creates a distinct `MIndexASTEditSession`, not a cache `Loader`. It fixes a trimmed source reference, nonnegative language ID, source callback and `Limits`: nonnegative maximum source UTF-16 length/attempted-unit total and at least one parse attempt. Its lazily created parser and exact resolver/pool survive changed-source loads. The session retains only its current source string, key and document plus that growing arena; callers can retain older documents. Workload limits are not byte-precise heap limits. [Session declaration and limits][session-head]

A serial load performs these steps under the session monitor:

1. Require open state, reject reentrant load and require a cancellation supplier; validate source reference and current Java AST fingerprint
2. Check cancellation/interruption, increment `sourceReads` and invoke the source callback
3. Recheck open state, enforce per-source length, retain a `String` or copy other character sequences with cancellation checkpoints every 4096 units and a final length check; equal-length concurrent mutation is not independently detected
4. Recompute the UTF-16 key with checked character access and require exact key equality
5. If the current key matches, also require full `String.equals`; a digest collision is rejected and exact reuse returns the same document, increments `exactReuses` and consumes no parse attempt
6. Otherwise admit the attempted length/parse budget, create the pool/parser if needed and perform a full-source javac parse
7. After parse, check cancellation/open state and document digest/spec custody, then assign current document/source/key and increment the published-version count

Unchanged source is still retrieved and hashed on every load. Changed source is fully reparsed; reuse is canonical atom sharing in the same pool, not incremental parsing. Budgeted attempts are consumed before parsing and are not refunded after failure/cancellation. Pool additions are not rolled back. Syntax-error documents may be published with diagnostics. Current document/source/key assignments precede `Math.incrementExact(publishedVersions)`, so a version-counter overflow can leave changed current state without a new version or replacement notification; publication is not a rollback transaction. This is source ordering, not an executed overflow test. [Serial load][session-load], [budget and stable input][session-tail]

Cancellation cannot preempt a blocked source callback or javac; checkpoints refuse publication after they return. `CancellationException` and `InterruptedException` increment cancellations, and caught `InterruptedException` restores the interrupt flag. Other `Exception` increments failures. `Error` is not counted by those catches, although the `loading` guard is cleared in `finally`. `Statistics` reports source reads, parse attempts, attempted units, published versions, exact reuses, failures, cancellations, current retained pool atom count and closed state. After close the pool count is zero because the session reference is cleared, not because all aliases have been reclaimed. [Session failure and observation][session-load]

`load(key)` uses context progress; the named `loadWithProgress` adapts an `IProgressMonitor` (null means no-op), and `loadWithContextProgress` resolves/uses a context monitor. Begin/worked/done callbacks run outside the serial core; cancellation queries run at guarded checkpoints. Begin precedes the `try`, so a throwing begin callback does not invoke done. Worked/done can fail after publication without rolling it back, and a throwing done callback can replace an earlier exception. A direct `BooleanSupplier` load uses no context progress wrapper. Notification follows publication, outside the lock, only when the version changed; exact reuse sends no replacement event. Events carry `UPDATED | SNAPSHOT_REPLACED | SOURCE_CHANGED`, old/new versions, the source reference and old/new snapshots. The retained context/progress dependencies are absent, so listener delivery/failure semantics beyond these call sites are not established. Concurrent notifications are not serialized by the load lock. [Progress and publication][session-progress]

`current()` returns the current snapshot under synchronization. Idempotent close clears source loader, parser, pool and current source/key/document. It waits for an ordinary synchronized load rather than preempting it; a callback can close reentrantly, and the outer load defers a pending clear notification until the lock is released. A clear event uses null next snapshot, version values `-1`, and updated/snapshot-replaced bits without source-changed. Returned aliases remain valid. A listener failure occurs after state assignment and is not a rollback mechanism. [Close and events][session-close]

## 8. Evidence and remaining gates

The [AST intake manifest][manifest] maps original donor paths at revision `7d2133f1412a9e7295296c3f86baae577bb3251c` to unchanged public receiver paths, with SHA-256 and `Apache-2.0` / `APACHE_SOURCE` declarations. [Provenance][provenance] binds original inputs and preserves notices. This is source lineage, not runtime/edit/cache acceptance.

The [historical qualification][qualification] reports four original navigation tests and bounded importer cases over 44 payloads: 37 production sources, five tests and two license/notice entries. It explicitly excludes portable source-complete receiver build, actual consumer wiring, AST cache/edit/resident lifecycle, native/JNI, matched JDK/jtreg/platforms, performance and full-family completion. The cache tests below are retained source assertions, not extra executed evidence:

- [Partial-cache tests][cache-tests]: LRU order, active-caller lifetime after eviction, source digest refusal, syntax-error caching, projection upgrades, pressure budgets, same-key miss sharing, overweight serving, fresh-source refusal and independent fresh loads
- [Deterministic Error tests][error-tests]: owner Error identity, waiter envelopes, flight cleanup/retry, close before/after Error and interrupted-waiter behavior; explicitly synthetic injection, not actual VM exhaustion

There is no dedicated retained edit-session test in the five-file public test inventory. Current testing, source-complete dependency closure, detailed custom-loader/backend behavior and concurrency/resource qualification remain separate work. Missing implementations are not supplied by importing statements from donor documentation.

### License and module boundaries

Preserve the original per-file Apache-2.0 SPDX headers, [LICENSE][license], [NOTICE][notice], [module third-party notices][third-party] and intake attribution. Those public notices record architectural lineage and other independently scoped dependencies; their presence is not proof that all mentioned classes or donors were imported. The repository's [Apache reuse policy][reuse-policy], [borrowing contract][borrowing] and [porting invariant][porting] keep reusable donor ownership, target integration and qualification separate. OpenJDK-owned files retain their licenses; the Classpath Exception is not a blanket source-relicensing or runtime-admission grant. This documentation changes none of those boundaries.

[atom]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexAST.java#L13-L278
[pool-construct]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L23-L86
[pool-access]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L88-L122
[pool-admission]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L124-L220
[pool-payload]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L420-L464
[pool-intern]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L466-L491
[pool-compare]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L289-L418
[pool-hashes]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L493-L557
[pool-growth]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPool.java#L559-L724
[document]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDocument.java#L10-L267
[document-construct]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDocument.java#L31-L112
[document-access]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDocument.java#L114-L266
[diagnostic]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTDiagnostic.java#L8-L23
[occurrence]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTOccurrenceSource.java#L4-L43
[parser-entry]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTParser.java#L48-L152
[parser-builder]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTParser.java#L154-L242
[parser-occurrences]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTParser.java#L244-L345
[parser-source]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTParser.java#L347-L370
[cache-key]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L34-L122
[cache-pressure-types]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L124-L207
[resident]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L209-L418
[cache-snapshots]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L420-L498
[cache-construction]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L518-L629
[cache-read]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L631-L673
[cache-residency]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L675-L704
[cache-rebalance]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L706-L764
[cache-observe]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L766-L821
[cache-flight]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L823-L937
[cache-tail]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTPartialCache.java#L939-L1038
[adapter]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTResidentCache.java#L12-L469
[session-head]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTEditSession.java#L18-L79
[session-progress]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTEditSession.java#L81-L158
[session-load]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTEditSession.java#L160-L223
[session-close]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTEditSession.java#L225-L258
[session-tail]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTEditSession.java#L260-L303
[cache-tests]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTPartialCacheTest.java#L1-L411
[error-tests]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTPartialCacheErrorTest.java#L1-L213
[manifest]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/export.tsv
[provenance]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/PROVENANCE.json
[qualification]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/ast-navigation-20261007/evidence/QUALIFICATION.json
[vendor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/README.md
[delivery]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-delivery.md
[license]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/LICENSE
[notice]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/NOTICE
[third-party]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/THIRD_PARTY_NOTICES.md
[reuse-policy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache-reuse.md
[borrowing]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache2-borrowing-contract.md
[porting]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3jdk21-porting-invariant.md
