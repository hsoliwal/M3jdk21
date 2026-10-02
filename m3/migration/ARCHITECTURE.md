# MIndex -> M3 migration architecture

## Objective

Move the MIndex/MatIndex capability family into the M3 architecture without erasing existing contracts, while making later Synexia enhancements replayable through pinned source-to-target mappings.

The migration has three independent integration routes sharing one semantic ownership model:

1. **Route A — explicit stock-JVM view.** Application code opts into an immutable M3 text value. Ordinary String materialization is an explicit compatibility boundary.
2. **Route B — compiler lowering.** Eligible source operations lower to the same canonical text/atom owners. Unresolved semantics refuse transformation or fall back to ordinary Java.
3. **Route C — complete modified JDK.** java.lang.String may use MIndex-backed storage only inside a matched full JDK image with separate VM/JNI/JVMTI/GC/CDS/serviceability gates.

JNI/Jini is an optional backend across the routes, not a fourth route.

## Canonical ownership

### Text

The source semantic owner is `com.synexia.indexstring.MIndexString` plus its `IndexResolver`, exact resolver, tuple, immutable storage, range and compiler-runtime collaborators.

A logical value is exact UTF-16 content represented by canonical owner-scoped atoms/ranges. These identities remain distinct:
- UTF-16 text equality
- owner/namespace/generation atom identity
- normalized composition identity
- Java object identity / String.intern identity

Numeric IDs from different owners are never interchangeable. Hash equality is never proof of text equality.

### Ranges

`com.synexia.indexstring.SubMIndexString` is the String-compatible source range owner because it permits arbitrary UTF-16 code-unit ranges.

The legacy `com.synexia.mindex.SubMIndexString` remains a separate compatibility contract because it rejects ranges that split surrogate pairs. It cannot be consolidated by renaming.

### Structural atoms

`MIndexAtomStore` owns primitive structural atom rows. `MIndexAst` and `MIndexDag` are projections over that storage. Existing `MIndexCanonicalBridge` adapters are reused for legacy structural families.

Application/tooling concerns such as interaction semantics, donor catalogues, Maven/OpenRewrite transformation logic and inventory engines stay outside `java.base`.

## Storage invariant

Logical text is immutable canonical storage plus ordered retained ranges.

- Mutable Java arrays are copied on admission unless exclusive ownership is enforceable.
- Joining already-owned atoms never allocates a duplicate joined character/byte payload.
- Descriptor metadata, balancing nodes, indexes and bounded caches may still allocate.
- Slicing retains backing owners and changes coordinates.
- Length/offset overflow is rejected before publication.
- Equal text with different segmentation must have equal Java hashes.
- Live values must remain valid after optional lookup-cache eviction.
- Shared persistent data stores offsets/versions/generations, never Java references or process pointers.

Shared mapped lexicon backing is a separate capability from VM-local unknown-text interning. Current Route A still copies records out of the existing M3 mapped image; direct mapped backing remains an open gate.

## Layering

### Bootstrap/runtime kernel

Allowed: dependency-minimal immutable storage, exact UTF-16 operations, validated shared formats, local fallback, VM integration required by Route C.

Not allowed as accidental bootstrap dependencies: Maven, OpenRewrite, donor catalogues, network loading, application registries, language-analysis frameworks, benchmark harnesses.

### Tooling

Reuse:
- `m3-java-inventory` for inventory, SHA/structural/SimHash signals
- `m3-java-contracts` for contract atoms, snapshots and recipe receipts
- `m3-fast-search` for algorithm catalogue and optional search/JNI experiments
- `synexia-openrewrite-recipes` for semantic Java transformations

## Route A implementation in this branch

`com.m3.text.M3String` is an explicit immutable CharSequence over the existing M3 piece/range substrate. It provides:
- weak VM-local canonical `fromString` admission
- payload-retaining joins and slices
- composed Java String hash across concatenation
- exact UTF-16 equality/ordering
- code-point operations across seams
- independent mutable outputs
- exact KMP literal search across seams
- java.util.regex consumption as CharSequence
- explicit `asString()` materialization

It intentionally does not claim String.intern identity, transparent JVM replacement, direct shared mapped lexicon backing, or complete String API parity yet.

## Route B requirements

Port/replay the existing compiler owners only after semantic admission proves:
- evaluation order and side effects
- null behavior and exceptions
- overload resolution and constants
- invokedynamic concatenation
- transformed/untransformed caller boundaries
- escaping values and identity-sensitive operations

Every transform must be source-pinned, deterministic, idempotent, drift-refusing and reversible.

## Route C requirements

The current inspected candidate is M3jdk21 PR #6. It must remain opt-in until:
- complete current-head JDK image builds
- enabled upstream failures are resolved or compatibility is explicitly justified
- compiled segmented execution is validated
- JNI/JVMTI/GC/dedup/CDS/reflection/serviceability gates pass
- full applicable jtreg/JCK is executed

Historical receipts do not transfer to a later candidate automatically.

## Precomputation and search

Facts are scoped by owner and input:
- atom facts
- composition facts
- pattern facts
- pattern-plus-input-context facts

Java String hash composition is permitted with powers of 31 and 32-bit overflow. Cryptographic hashes and SimHash remain separate identities/signals.

Approximate or accelerated filters may reject impossible candidates but cannot prove exact regex matches. RE2/J, GPU and JNI engines are optional backends with exact CPU fallback for supported contracts.

## Migration control

The authoritative migration-control files are under `m3/migration/`:
- exact baseline pins
- contract-aware mapping manifest
- file-level inventory shards
- declarative recipe pre/post hashes and rollback preimage
- acceptance/coverage documents
- durable resume procedure

A future port compares current source and target to the last synchronized pins, computes the affected mapping/dependency closure, refuses unresolved divergence, replays the applicable recipe, executes exact-head tests, and updates mappings and evidence in the same change.
