# Migration acceptance matrix and contributor work packets

Status: all items below are **requirements/open work** unless an exact-head receipt is attached by a later implementation PR. Source existence, a filename census and historical candidate receipts are not acceptance.

## Evidence notation

For every gate record:
- source commit and target commit
- mapping IDs and route A, B or C
- exact command, toolchain/image hashes, OS/architecture and VM flags
- passed / failed / blocked / not-run / not-applicable, with counts
- artifact URL and reviewer disposition
- cold/warm and preparation/runtime distinction for performance results

No tests or builds were run for this documentation PR. The separate #6 results remain attributed to their pinned candidate, not this documentation branch or combined master.

## Family matrix

| Work packet | Contract to preserve | Required evidence / unresolved work |
| --- | --- | --- |
| Immutable chars/bytes and local pool | Defensive ingress; exact code units/bytes; owner lifetime; collision-safe admission | Caller mutation, collisions, concurrent admission, weak/bounded eviction, live-view retention and cache accounting |
| Joined chars/bytes and slices | Reference-only payload reuse; geometry normalization; explicit materialization | Nested/empty/repeated joins, same geometry reuse, split surrogates, range overflow, deep append chains, retention and destination-copy counts |
| Canonical resolver MIndexString | Resolver/language coordinates and lexical-key semantics | Cross-resolver equality/hash decisions, empty tokens, language switching, unknown tokens and explicit String boundaries |
| Runtime/experiment MIndexString | Separate pool/token namespaces and compatibility behavior | Conversion receipt parity both directions; unpaired surrogates; pool close/eviction; no local-ID substitution |
| SubMIndexString families | Existing stricter surrogate-safe view versus general UTF-16 slice | Pin both contracts; test every boundary in supplementary pairs; specify target adapters and exceptions |
| MatIndexString families | Rows/columns/lanes, order, builders/maps/pools and actual coordinate contracts | Distinguish root and mat packages; dimensions, empty/ragged inputs, overflow, stable iteration, equality/hash and serialization |
| MIndexAtomStore | Immutable primitive lanes, payload presence, resolver and domain | Freeze/admission, malformed rows, cross-owner references, ordered children, hash collisions, corruption and lifetime |
| AST | Root/domain/spec validation, child order, parser-neutral adapters | Donor adapter parity, cycle rejection, sharing, deep trees, partial AST handling, source coordinates and image round-trip |
| DAG | Node identity, edge mode/kind/type/payload, topological and CSR indexes | Cycles, duplicate/missing nodes, edge multiplicity/order, deterministic topology, cross-version IDs and graph images |
| Interaction/semantic tables | Exact role × role × mode meaning; separate storage family | Exhaustive table comparisons; enum evolution; unknown persisted values; no ordinal reinterpretation |
| Object family | Object handles/shapes/state/program ownership | Cross-pool IDs, adapter admission, flags, state/version transitions, stale handles and deterministic programs |
| Path family | Qualified owner, segment types, root/relative semantics | Multiple path-family reconciliation, escaping/normalization, coordinate remapping, index/codec round-trip |
| Algorithm family | Descriptor/catalog/provider/execution distinctions | Input/output types, preconditions, deterministic results, error/budget contracts and provider registration |
| DataStructure family | Topology, mutation/versioning and traversal | Alias/lifetime, order, cycles where permitted, structural equality, updates and persistence |
| Tool family | Registry, request/result and execution boundary | Provider identity, capability checks, failures, cancellation, service descriptors; no accidental execution during lookup |
| Library family | Entry ownership, dependency and version model | Catalog identity, dependency conflicts, invalid entries, import/export provenance |
| Framework family | Snapshot/index/plan/receipt relationships | Framework version binding, provider graph, precompute invalidation and equivalent replay |
| Collections and primitive indexes | Key domains, ordering and null/absent behavior | Collisions, equality/hash, mutation exposure, iterators, compaction and representation-independent results |
| Search / regex / fuzzy / prepared plans | Exact oracle versus candidate filter; engine/flag/region semantics | Seam matches, captures/replacement, backreferences or fallback, Unicode, approximate false positives/negatives and cache invalidation |
| Language/model/word facets | Text identity distinct from semantics/vocabulary coordinates | Tokenization and normalization policy, vocabulary/image versions, unknown words, multilingual parity; standalone Word owner remains unconfirmed |
| Shared lexicon / shared arrays | Canonical file/image authority and generation/prefix binding | Corrupt/truncated/stale files, immutable publication, concurrent readers, restart, cross-JVM backing evidence and safe local fallback |
| Sidecars / filesystem precompute | Derived, discardable metadata; never canonical identity | Index corruption fallback, prefix/tail handling, geometry lanes, pagination, source-row IDs and output-array bounds |
| Compiler / Maven-plugin integration | Resolved semantics, source maps and reversible transformations | Evaluation order, overloads, nulls, reflection/identity/serialization boundaries, drift refusal and idempotence |
| JNI / Jini / GPU/native adapters | Exact ownership and ABI; optional acceleration | No pointer-after-release, failed acquisitions, lifetime/close races, charset/MUTF-8 differences, 64-bit offsets and fallback parity |
| Viewers / Swing / Eclipse adapters | Projection identities, paging and interaction | Stable row/version mapping, selection/filter behavior, cancellation, stale windows, thread confinement and no accidental full materialization |
| Complete modified JDK | Coherent String representation across Java and VM | Bootstrap; flag-off/on; interpreter/JIT; GC; intrinsics; JNI/JVMTI; CDS/JFR/JVMCI; serviceability; full applicable conformance |

## Recommended contributor sequence

### Packet 0: reconcile before moving anything

Identify actual target files and runtime modes on the exact master tree. Reconcile #3/#5/#6 and convergence history. Recover or link historical evidence without asserting files are present just because a PR merged. Complete inventory outside the eight-subtree filename census, including tests, schemas, native and generated artifacts.

Exit: reviewed source/target baseline and a bounded first owner selection. No broad renames.

### Packet 1: immutable ownership and conversions

Decide the established owner behind each P0/canonical/runtime/experiment facade. Preserve exact UTF-16 and explicit materialization. Add cross-owner conversion contracts and lifetime tests.

Exit: one agreed storage authority per identity domain, no copied payload on joins where promised, and documented compatibility boundaries. Source admission copies are measured separately from joins.

### Packet 2: structures and semantic families

Carry atom store, AST, DAG and interaction semantics together. Map objects, paths, algorithms, structures, tools, libraries and frameworks to real source contracts. Use specialized stores where their identity/lifetime differs; do not force every family into the text pool.

Exit: per-family mapping, adapters and tests; all unresolved family coverage remains listed.

### Packet 3: prepared work and shared storage

Move safe precomputed facts and search plans with their validity keys. Shared backing is canonical; optional sidecars and GPU buffers are disposable. Test immutable generation transitions and pressure.

Exit: exact-answer parity with caches disabled/enabled/evicted and recorded retained-memory budgets.

### Packet 4: compiler integration

Lower only proven compatible operations, preserving original evaluation and public API boundaries. Carry tests and source maps. Refuse unsupported input rather than guessing.

Exit: differential behavior on transformed/untransformed programs and recipe replay/rollback evidence.

### Packet 5: complete JDK integration

Implement and validate the coherent VM representation only after the lower-level contracts are stable. Build private complete images, do not patch an installed JDK in place. Unsupported modes must remain explicit.

Exit: exact-image acceptance for every advertised mode; historical interpreter-only receipts do not qualify compiled mode.

## Known source issues must not be hidden by migration

- Duplicate m3-mindex-native-string-admission profiles occur in synexia-indexstring/pom.xml at the documented source pin. Keep build-configuration remediation separate and rerun the real build after it is resolved
- Runtime SubMIndexString rejects a split surrogate endpoint while the general code-unit slicing contract permits it. This requires a compatibility decision, not a blind class rename
- PR #7554 at 4574363a63c74c52183eb2deae134163d409816f has an output languages parameter shadowing the stored lane in copyLiteralGeometryPage. The assignment reads the output buffer at the source-row index; wrong metadata or out-of-bounds access is possible. Documented by source inspection, not fixed/tested here. Require nonzero-page and mixed-language regression tests before reuse
- Source CI/source download authentication failures are not semantic test passes and do not authorize public export of private implementation files
- #6's two enabled StringJoiner test failures remain failures even if reduced allocation explains the OOME expectation difference. Resolve the contract and tests through review; do not remove them to claim green

## Performance acceptance

Report admission, preparation, joins, queries, materialization and cleanup separately. Name input distribution, segment count/depth, language/charset, reuse, cache budget, concurrency, GC, warmup and forks. Include cold end-to-end CPU, warm operation CPU, allocations, retained heap/native/mapped memory and throughput/latency.

A content-ID lookup may be constant after admission inside one namespace; hashing incoming bytes is not. Descriptor construction is not universally constant. A page-sharing demonstration is not a CPU benchmark. Allocation savings alone do not prove speedup. The requested CPU improvement remains a target until representative end-to-end measurements on the exact image establish it.

## Definition of completion

“All migrated” means every in-scope source symbol and dependent surface has a reviewed disposition; every accepted mapping has exact-head evidence; old compatibility commitments and licenses are satisfied; every enhancement in the reviewed source range has an explicit disposition; and all advertised routes/modes pass their required gates.

Anything else is a partial milestone and should be named that way.
