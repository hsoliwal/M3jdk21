<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# MAT primitive exports: complete module qualification

This receiving record extends the [earlier scoped proof](mat-owned-exports-20261112.md) and retains its exact source/recipe lineage. The existing sparse vector adopts only arrays freshly allocated inside its factory, while its public constructor stays defensive. Sorted and direct-layout integer-map exports traverse their existing aligned value/occupancy arrays. Public/protected signatures, the evolved builder, hash-layout behavior, module exports and native ABI are unchanged.

## Executed target evidence

Both the baseline and candidate compiled the complete `jdk.m3.collections` module: 1,185 Java sources and 2,081 class files with Java 21, `--release 21 -Xlint:all,-module -Werror`. The same 815 current collection test sources compiled against both versions. Each default Java cohort discovered 2,846 tests: 2,691 succeeded, zero failed, 16 were individually skipped, 135 assumption-aborted, and four other tests were unstarted. One container was skipped and one aborted; neither run had a failed container. The existing 21 JNI-dependent class exclusions remain explicit.

The MAT oracle passed 212,968 checks in each of 12 runs: both versions, module path and linked component image, in normal, interpreted and C1 modes. Real jmods include the existing resource and legal notices. The linked component image contains `java.base` and `jdk.m3.collections`, has no Synexia runtime dependency and keeps MAT packages unexported. It reuses the host Temurin 21.0.13+9 VM; this is not a source-built complete M3JDK21/HotSpot image.

## Measured resource result

| Exact workload | Before bytes/op | Candidate bytes/op |
|---|---:|---:|
| Sparse factory: 4,096 values, 1,024 explicit | 16,480 | 8,256 |
| SORTED map: 24 exported values | 224 | 112 |
| DIRECT map: 512 values, contiguous or with holes | 4,128 | 2,064 |
| Unchanged HASH control: 512 values | 4,128 | 4,128 |

Instrumented directly owned object/array graphs were unchanged; the sparse result remained 8,256 bytes. These observations support reduced temporary allocation. CPU timings were shared-host diagnostics while another test cohort ran, and the unchanged HASH control also moved. Peak process RSS did not improve. No portable throughput, latency, whole-heap or process-memory claim is made.

## Explicit gate disposition

The original canonical Synexia provenance checker was executed after retrieving all 193 exact indexed members (122,245 rows), and it failed on inherited records. The first failure is a missing canonical SPDX header in `20261104-ap5-surviving-history-recovery.tsv`. A full raw audit also found 2,901 unsupported classifications across 66 failing shards and 239 conflicting duplicate occurrences covering 12 paths. None identifies this new MAT dataset. Classifications control admission, so a blanket reclassification or discarded history is not an acceptable repair.

The collection component is qualified by the evidence above, while the PR remains draft on that global provenance hold. Full M3JDK21/HotSpot release, full native profile, cross-platform, canonical naming reconciliation and ordered phase admission are not claimed. The global String -> arrays -> collections sequence remains unchanged.

Exact source commits, hashes, commands and conditional outcomes are bound by [the passive module receipt](mat-owned-exports-module-20261112.json). Canonical Maven/OpenRewrite source, private proof tools, original logs and correction memory remain in Synexia; this target contains only executed product/test/build outputs and passive receiving evidence.
