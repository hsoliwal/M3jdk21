<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Bounded count-owner cost observation

The final probe compiled with Java 21 and `-Xlint:all,-processing -Werror` and completed **160 checked samples** against the exact Guava 33.4.8-jre, Apache Commons Collections 4.6.0 and Eclipse Collections 11.1.0 artifacts. This is an isolated diagnostic of the reviewed M3 count-owner template and pre-existing corrected core classes; it is not the final named-recipe application or a product performance gate.

For this workload, M3 admission allocated more cumulative bytes than all three donors, and its median count-operation CPU times were higher. Existing-count updates and M3 flag writes allocated zero bytes in every final sample. M3 flag selection deliberately allocates a new metadata/result snapshot. These observations support a finite resource envelope, not a speedup claim.

## Workload and meters

One OpenJDK 21.0.12.1+1-LTS JVM ran in the shared Linux amd64 container on an AMD EPYC 9V74 host, with an eight-CPU quota, a 512 MiB initial/maximum heap and G1. Every key and flag word was created before measurement. Counts start at `1 + (keyId & 3)`, so the same positive bounded-count operations have common semantics in all four owners. Keys use integer IDs and their record hash; the harness does not allocate caller payload keys in measured regions.

Six warmup rounds cover both sizes before the first measured sample. Each size then has eight samples, alternating the entire provider order forward and backward. No round was discarded and no forced GC was requested. Fresh admission means a fresh default-capacity owner after code warmup; it does not mean cold JVM startup. Construction includes intermediate growth allocations. Each update sample performs four rounds of add-two/remove-two on every existing key, eight count actions per key, using a deterministic permuted traversal. Counts never reach zero during updates.

CPU is current-thread CPU time; allocation is cumulative bytes allocated by that thread. Neither is retained-heap size. Meter overhead is not subtracted. Compiler/GC work on other threads is excluded, while wall time includes any scheduling/pauses. Full count verification and result checksums happen outside the timed regions.

## Fresh owner admission

CPU values are milliseconds, shown as median (minimum–maximum), across eight samples. Allocation is the exact median bytes; all eight final samples in each row have the same byte count. One sample admits all distinct keys.

| Distinct keys | Owner | CPU ms, median (range) | Allocated bytes |
| ---: | --- | ---: | ---: |
| 4,096 | M3ObjectCounts | 0.135 (0.117–0.185) | 416,408 |
| 4,096 | Guava LinkedHashMultiset | 0.072 (0.066–0.144) | 295,104 |
| 4,096 | Apache HashMultiSet | 0.064 (0.061–0.128) | 262,328 |
| 4,096 | Eclipse HashBag | 0.079 (0.056–0.111) | 131,528 |
| 65,536 | M3ObjectCounts | 1.672 (1.483–2.034) | 7,026,112 |
| 65,536 | Guava LinkedHashMultiset | 1.225 (1.166–1.731) | 4,718,848 |
| 65,536 | Apache HashMultiSet | 1.225 (1.070–1.400) | 4,194,552 |
| 65,536 | Eclipse HashBag | 0.892 (0.844–1.256) | 2,097,736 |

M3 retains parallel primitive count/flag columns and stable insertion-order metadata. Its default geometric growth copies several arrays. The measured bytes include discarded intermediate arrays, so removing per-key count wrappers does not by itself imply lower total allocation during construction. This probe did not measure the retained object graph after collection.

## Updates to existing keys

| Distinct keys | Owner | Count actions per sample | CPU ms, median (range) | Allocated bytes |
| ---: | --- | ---: | ---: | ---: |
| 4,096 | M3ObjectCounts | 32,768 | 0.216 (0.177–0.359) | 0 |
| 4,096 | Guava LinkedHashMultiset | 32,768 | 0.120 (0.111–0.197) | 0 |
| 4,096 | Apache HashMultiSet | 32,768 | 0.112 (0.111–0.211) | 0 |
| 4,096 | Eclipse HashBag | 32,768 | 0.117 (0.108–0.172) | 0 |
| 65,536 | M3ObjectCounts | 524,288 | 5.777 (5.408–6.255) | 0 |
| 65,536 | Guava LinkedHashMultiset | 524,288 | 4.028 (3.943–5.764) | 0 |
| 65,536 | Apache HashMultiSet | 524,288 | 4.002 (3.921–4.225) | 0 |
| 65,536 | Eclipse HashBag | 524,288 | 3.604 (3.510–3.876) | 0 |

The zero-allocation result applies to these warmed, bounded count updates on existing keys. It does not cover adding/removing distinct keys, iterator projections, provider view creation, bulk operations or overflow paths.

## M3 application flags

Each write sample makes four flag writes per key. The selection query requires bit 0 and forbids bit 1; an unrelated sign bit is also present in some inputs. Each selection call builds its own primitive slot metadata and detached unmodifiable result list using the existing Java mask kernel. The donor bags/multisets have no equivalent application flag columns, so there is no donor timing comparison for this phase.

| Distinct keys | Phase | Actions / keys scanned | CPU ms, median (range) | Allocated bytes | Selected keys |
| ---: | --- | ---: | ---: | ---: | ---: |
| 4,096 | flags_write | 16,384 | 0.552 (0.102–0.750) | 0 | 0 |
| 4,096 | flags_select | 4,096 | 0.078 (0.062–0.108) | 89,048 | 1,365 |
| 65,536 | flags_write | 262,144 | 2.888 (2.132–8.808) | 0 | 0 |
| 65,536 | flags_select | 65,536 | 0.933 (0.896–1.021) | 1,488,592 | 21,845 |

Flag write CPU varied substantially, including an 8.808 ms sample at 65,536 keys. The raw records retain that sample and every other observation. No latency percentile or stable throughput claim follows from this small run.

## Structural evidence and contract checks

| Owner | Pinned source storage observation |
| --- | --- |
| M3ObjectCounts | Existing M3 stable key slots plus primitive `int[]` count and flag columns. No count wrapper is retained for each key. |
| Guava LinkedHashMultiset 33.4.8-jre | `LinkedHashMap<E, Count>`; admitting a distinct key creates its `Count`. |
| Apache HashMultiSet 4.6.0 | `Map<E, MutableInteger>`; a distinct key has a mutable counter wrapper. |
| Eclipse HashBag 11.1.0 | A primitive `MutableObjectIntMap` owner already stores primitive counts. |

Source paths and SHA-256 values for these observations are recorded in `count-owner-cost-receipt.json`. The table describes source structure, not an estimate of retained byte sizes. M3 and Guava preserve insertion order; Apache HashMultiSet and Eclipse HashBag do not provide that same ordering contract. M3 also carries flags. These feature differences limit interpretation of timing comparisons.

Every measured count state was checked at every key and against distinct/total counts. At 4,096 keys the total is 10,240 and the weighted count checksum is 20,981,760. At 65,536 keys they are 163,840 and 5,368,872,960. The flag selections contain 1,365 and 21,845 keys, with weighted selection checksums 2,796,203 and 715,827,883. Selection order/content and the unchanged multiplicity revision after flag writes are checked. No assertion failed.

## Warmup sensitivity and limits

An initial version warmed and measured each size before moving to the next. That run observed 524,288 median allocated bytes for Eclipse updates at 4,096 keys and zero at 65,536. This is consistent with warmup/JIT escape-analysis sensitivity; no compiler trace was captured to identify an exact optimization. The final harness therefore warms the entire size corpus first. Both raw runs are retained, with their distinct source hashes and warmup plans in the receipt. Within either run, no samples were filtered.

This is one JVM process with finite warmup, two sizes and a narrow positive-count workload. It does not cover cold JVM/class loading, JNI, native memory, retained heap, concurrency, presizing, collision-heavy/null inputs, complete removals, views, bulk operations or overflow. Separate differential tests own provider edge behavior. Finite warmup does not prove every method has reached its final compilation tier. The sample ranges and raw data are the evidence; they do not establish general ranking or scaling.

## Reproduction assets

- `CountOwnerCostProbe.java`: frozen, standalone contract harness; it accepts no arguments and checks donor artifact metadata before measurement.
- `count-owner-cost-samples.csv`: all 160 final samples plus JVM metadata.
- `count-owner-cost-warmup-sensitivity.csv`: all 160 initial samples retained to show the warmup-plan sensitivity.
- `count-owner-cost-receipt.json`: exact source and artifact hashes, binary core inventory, environment, structured compile/run commands, full grouped samples and limitations.

The recorded compile command copies only the reviewed count-owner template and this harness to an isolated scratch directory, then compiles against the corrected core class directory and exact Maven artifacts. The run command uses `-Xms512m -Xmx512m -XX:+UseG1GC`. To replay elsewhere, resolve the same hashes and substitute local paths; do not substitute a donor version or claim the isolated class directory as the final recipe-applied product.
