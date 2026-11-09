<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# FileIndex cache repair and packed delta workspace

The existing `FastChangeDetector.updateCache` computes a state but never publishes it. This task repairs that omission; the five inspected historical method bodies have the same omission, so no historical restoration is claimed. The original size/mtime/xxHash heuristic remains best effort. It does not prove file immutability, detect all same-size/same-mtime rewrites, or atomically bind an asynchronously supplied SHA-256 to a filesystem snapshot.

`DeltaGenerator` used quadratic int matrices while describing linear-space Myers diff. Its script remains deterministic LCS with the exact original insertion-on-tie backtracking policy. A single `int[]` row and one bit per cell replace the full LCS matrix. The separate substitution-aware distance uses a row of `min(n,m)+1` ints, adapting the Apache Commons Text approach while preserving `oldHash.equals(newHash)` when transposed. No public method, native symbol or dependency edge changes.

For 5,000 by 5,000 elements, the original matrix has 100,040,004 bytes of int payload. The new script workspace has 3,145,004 bytes of primitive payload; distance uses 20,004 bytes. Headers, inputs and output records are additional. Script reconstruction still has quadratic direction-bit storage and quadratic comparisons. Lists with nonconstant indexed access retain that cost. This is not a linear-time algorithm or a throughput benchmark.

## Reproduce

Use Java 21 and Maven 3.9.11 with the existing source-built `com.synexia.build:m3-rewrite-java21-docs:8.17.1-m3.1` parser provider installed from `synexia-openrewrite-recipes/crates/m3-jdk-handoff/string-history/parser`. Keep its existing source custody; do not substitute a parser stub.

```sh
mvn -o -f synexia-openrewrite-recipes/crates/m3-fileindex-cache-delta-20261031/pom.xml clean verify
```

The crate compiles 11 actual FileIndex sources and five unchanged recipe/support sources. It runs existing ContentIndex tests, new cache/delta tests, and real OpenRewrite snapshot tests. No mock runtime dependency replaces an existing owner. The SDK writes exact candidate outputs under the crate's `target/executed`; hashes must match the manifests before those bytes replace the production files. Registry registration uses the existing text snapshot recipe and preserves all prior rows.

## Evidence and scope

`EVIDENCE.json` binds source revisions, emitted hashes, tool versions, commands and raw logs. The final offline clean verification ran 23 tests with no failures/errors/skips. The delta corpus covers 16,129 exhaustive binary sequence pairs plus 300 seeded sequential-list pairs. Both actual-code subprocess probes pass with `-Xmx48m`; both original matrix paths failed with `OutOfMemoryError`. Five public/protected class descriptor listings are unchanged. Recipe proofs include no-change replay, all mixed pre/post states and both input orders, and missing/duplicate/drift/representation refusals.

The normal FileIndex lifecycle was attempted in the bounded checkout and stopped at the unavailable `synexia-bom:0.1.0-SNAPSHOT` model and consequent missing managed versions. Full module/reactor, CI, canonical provenance-ledger admission and Windows integration are pending. The scoped provenance shard is candidate evidence, not whole-ledger acceptance. Keep this PR draft. No JNI acceleration or ABI change is claimed for these Java fixes.

`HISTORY.json` records the bounded historical sources; `CORRECTION_MEMORY.json` retains failures and recipe corrections. Donor decisions are in `tools/short-flag-query/reviews/20261031-fileindex-cache-delta.tsv`. The complete repository audit and existing convergence owners guided this task; unrelated M3 String, collection, native and filesystem delivery remains separately qualified work.
