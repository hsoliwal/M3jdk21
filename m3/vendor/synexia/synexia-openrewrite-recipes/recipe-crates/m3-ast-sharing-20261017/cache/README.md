<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 AST sharing: isolated cache and source identity

This crate qualifies the source/context binding adapter over the existing Synexia
`TieredSwapCache`. It does not create another LRU, parser, AST payload owner or store.
The canonical implementation belongs to `synexia-mindex/compiler`; the existing
`synexia-m3index-compiler` coordinate exposes that owner additively. M3JDK21 receives
the same adapter through explicit package-mapped, SHA-256-sealed postimages in its
independent Apache-2.0 compile-time module.

## Frozen contract

- Creating a cache or deferred reference performs no source parsing or store reads.
- IDs are caller-owned nonnegative ordinals. A SHA-256 is never truncated into an ID.
- Every request and durable frame binds the exact source URI, SHA-256 over raw UTF-16
  code units, UTF-16 length, context SHA-256, provider identity, schema and phase.
- The source facts object does not retain another copy of the source text. Original
  file bytes/encoding are a separate fact; a digest of compiler characters is never
  reported as a digest of the original byte stream.
- A store is pluggable through the existing `SwapStore` interface. Real persistence
  uses `SqliteSwapStore` and its existing JNI implementation.
- Codec-accounted byte/entry ceilings, LRU, lazy reload, pressure adjustment, protected
  active leases and close refusal come from `TieredSwapCache` unchanged. These budgets
  do not bound javac's live heap, caller-held values or process RSS.
- Wrong source, provider, phase, schema, context, ordinal, checksum, malformed frame,
  loader failure and store failure must refuse reuse. No raw javac tree is persisted.
- Concurrent first access serializes through the existing cache and executes one
  successful loader. Failed loads remain retryable. Closing a reference's successful
  first lease permits cold reload without retaining its original loader.

## Inspected donor owners

All paths below were inspected at Synexia
`e244d68fdee88d4a406be48e4ff1d31b60a17988` before candidate generation.

| Existing owner | Reuse decision |
| --- | --- |
| `synexia-mindex/runtime/.../swap/TieredSwapCache.java` | Exact source custody; owns bounded residency and leases |
| `synexia-mindex/runtime/.../swap/SwapStore.java` | Exact source custody; pluggable immutable record contract |
| `synexia-mindex/runtime/.../swap/SwapBudgetPlane.java` | Exact source custody; pressure budget remains canonical |
| `synexia-mindex/runtime/.../swap/SqliteSwapStore.java` | Exact source custody; native durable storage |
| `synexia-mindex/runtime/.../ast/partial/MIndexPartialAstKey.java` | Contract reference for source/profile/environment isolation |
| `synexia-mindex/runtime/.../ast/partial/MIndexPartialAstCache.java` | Existing partial-projection SLRU retained; no replacement |
| `synexia-mindex/runtime/.../MIndexAstSwapWorkspace.java` | Existing AST archive/working-set lifecycle retained |
| `synexia-indexstring/.../file/MIndexFileSourceFacts.java` | Source-byte facts remain distinct from compiler UTF-16 facts |
| `synexia-indexstring/.../file/MIndexFileSourceFactsKey.java` | Its 64-bit content fingerprint is not compiler-source authority |

Public problem catalogues supply LRU, single-flight and immutable content-identity
test categories only. No challenge statement, editorial or solution body is copied.
The existing first-party owner already supplies the implementation; no external cache
donor is needed for this gap. No throughput, heap or complete JDK integration claim is
made by the finite contract suite.

## Recipe and evidence

The real `M3HashPinnedJavaSnapshotRecipe` is the Java authoring owner;
`M3HashPinnedTextSnapshotRecipe` owns native/build metadata custody. Their exact shared
sources are compiled from the canonical recipe module, not reimplemented here.
Reviewed postimages are inputs. Materialization occurs only after SDK admission,
source/template refusal, fixed-point and Java/JNI behavior tests execute.

The target receiving order and its still-pending JDK/runtime gates remain separate
from this task's reverse-order implementation request.

## Executed qualification

The cache lane passed the real OpenRewrite 8.17.1 SDK suite (4 tests) and the Java 21
runtime suite (14 tests, including 2 actual SQLite JNI tests), with no failures,
errors or skips. The final native library was rebuilt with both `javac -h` headers
forced into the strict C17 compilation, and all 16 exports in the unchanged shared
native unit matched those declarations. The 2 JNI behavior tests then passed against
that rebuilt library. Machine-readable receipts and original test summaries live in
`evidence/`.

The deterministic contract cases include 24 concurrent requests on 8 workers with
one successful loader/write; 2,048 cold records under a 2-entry / 8,192-declared-byte
resident budget; and a 128-record real SQLite close/reopen corpus. Tests exercise
active leases during pressure reduction, demand-only restoration, immutable record
conflicts, source/context/provider/schema/phase/ordinal isolation, corruption,
malformed lengths, loader/store failure, and same-ID recursive load refusal.

The first native export audit refused its incomplete inventory: the shared C unit
also owns four `JniSwapPageBinaryOps` exports. The counterexample is retained in
`evidence/counterexamples/abi-export-inventory/`. The corrected proof reads that
existing Java declaration and its two exact dependencies solely to generate the
complete native ABI. Those sibling operations are not added to the cache's Java
runtime dependency closure, and the native donor remains byte-for-byte unchanged.

## Reproduction

Use JDK 21 and Maven 3.9.9 (or a compatible Maven installation), with the configured
repository settings/truststore required by the execution environment. Run from this
crate in the pinned canonical Synexia tree:

```sh
python3 prepare_crate.py
mvn -f pom.xml test
python3 build_native_proof.py --jdk-home /path/to/jdk-21 \
  --sqlite-source /path/to/sqlite-amalgamation-3510300
mvn -f pom-runtime.xml test
```

The native proof consumes the official SQLite 3.51.3 amalgamation archive at
`https://www.sqlite.org/2026/sqlite-amalgamation-3510300.zip`. It checks the published
`sqlite3.c` SHA3-256
`32d5424f97e0a7fc5ed2f6335afbb58be4e0298bd7117a34e39d345ff13d859e`, the source ID,
and the reviewed archive's header SHA-256 before compiling. SQLite is a test
toolchain input here; the runtime uses the existing pluggable native store boundary.
This native proof currently qualifies Linux only.

After both suites pass, the actual recipe can materialize its sealed source set:

```sh
mvn -f pom.xml test -Dm3.cache.materialize=true \
  -Dm3.cache.synexia=/absolute/canonical/synexia \
  -Dm3.cache.target=/absolute/target/M3jdk21
```

The recipe scans existing target paths, refuses unknown drift, applies the exact
postimages and verifies a second-run fixed point. `prepare_crate.py` seals reviewed
inputs; it never writes product source paths.

## Source and payload ownership

`M3FileSourceFacts` stores metadata only. `M3ASTWorkspaceCache<T>` delegates residency,
pressure, lease protection and I/O to `TieredSwapCache<Frame<T>>`; its frame adds full
identity validation around a consumer-owned codec. Successful deferred references
drop their source loader immediately. Stable identity means the caller ordinal and
full facts key survive spill/reload; a reloaded Java object may be a new instance.

The byte ceiling is a codec-declared residency accounting plane. Live javac units,
active caller values, codec scratch space, source resolver custody, SQLite page
cache and process RSS have separate lifetimes and are not covered by that ceiling.
