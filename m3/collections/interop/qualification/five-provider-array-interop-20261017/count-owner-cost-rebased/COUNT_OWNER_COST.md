<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Rebased count-owner cost diagnostic

The unchanged frozen cost harness compiled strictly with Java 21 and completed **160 checked samples in exactly one JVM execution** against the diagnostic classes for target `f3c615693c41bac1bf6ff49ba53e83f85425bca0`, including the proposed count owner and two minimal constructor repairs. This is finite diagnostic evidence, not named-recipe application, a product performance gate or promotion evidence.

M3 fresh admission allocated more cumulative bytes than the three pinned donors in this workload. All existing-count update samples and M3 flag-write samples allocated zero bytes. Flag selection allocated its detached snapshot and primitive metadata. CPU ranges overlap and vary; no general speed or retained-memory advantage follows from this run.

## Bound execution inputs

- Frozen target: `f3c615693c41bac1bf6ff49ba53e83f85425bca0`.
- Unchanged harness: `../../contract/CountOwnerCostProbe.java`, SHA-256 `f2bd640ea83bb9ab328720fcd862ccdfc714808cc48cf571204fd1b956bdae3d`.
- Measured compiled directory: `/tmp/m3-five-provider-current-master-20261017/classes`.
- Measured directory inventory: **258 emitted `.class` files**, SHA-256 `072924009cc1b8a7b9ac282494d3e876b7756a8d0527093fa86fc0ca92aa50e5`.
- Complete ordered probe/core/JAR classpath closure: SHA-256 `cfd5265b3f3c66f70026d499e7a459b6f1b8d7c4c2eb37309ed54ecf87e4cacd`.
- Exact donor artifacts: Guava **33.4.8-jre**, Apache Commons Collections **4.6.0**, Eclipse Collections and API **11.1.0**. The receipt also binds the unchanged annotation JARs.
- Compiler: OpenJDK **21.0.12.1+1-LTS**, `--release 21 -Xlint:all,-processing -Werror`; no diagnostics.
- Runtime: one JVM, G1, `-Xms512m -Xmx512m`, Linux amd64 shared container, AMD EPYC 9V74 host and eight-CPU quota.

The inventory hashes every emitted class by relative path and SHA-256. The measured class directory and harness were checked before and after execution and remained unchanged. Only the harness was compiled for this run; M3ObjectCounts was loaded from the specified full diagnostic class directory. No replacement owner was compiled alongside the probe. The receipt retains the complete inventories and exact compile/run command arrays.

## Identical measurement method

The workload and source are unchanged from the final prior-base harness. Precreated `Key(id)` records and flag words are outside the measured regions. Counts begin at `1 + (keyId & 3)`. Six warmup rounds cover both 4,096 and 65,536 keys before eight measured rounds per size. Provider order reverses on alternate rounds. No sample was discarded, no forced GC was requested, and there was no extra timing sweep or optimization.

Fresh admission constructs and fills a default-capacity owner with warmed code. It measures a fresh owner, not JVM/class-loading startup. Each existing-count update performs four rounds of add-two/remove-two for every key, eight count actions per key, in the deterministic permutation `(position*2053+17)&(keys-1)`. Counts remain positive and no distinct key is added or removed in that phase.

CPU is current-thread CPU time. Allocation is cumulative calling-thread allocated bytes; it includes discarded growth arrays and is **not retained-heap size**. Meter overhead is not subtracted. GC/JIT work on other threads is excluded, and wall time may include scheduling and pauses. Correctness checks occur outside measured regions.

## Fresh owner admission

CPU is milliseconds, median (minimum–maximum) across all eight samples. Byte allocations are identical across the eight final samples within each row.

| Distinct keys | Owner | CPU ms, median (range) | Cumulative allocated bytes |
| ---: | --- | ---: | ---: |
| 4,096 | M3ObjectCounts | 0.193 (0.188–0.545) | 416,408 |
| 4,096 | Guava LinkedHashMultiset | 0.147 (0.137–0.175) | 295,104 |
| 4,096 | Apache HashMultiSet | 0.122 (0.118–0.495) | 262,328 |
| 4,096 | Eclipse HashBag | 0.078 (0.074–0.120) | 131,528 |
| 65,536 | M3ObjectCounts | 2.375 (1.521–4.244) | 7,026,112 |
| 65,536 | Guava LinkedHashMultiset | 2.198 (1.162–3.781) | 4,718,848 |
| 65,536 | Apache HashMultiSet | 2.031 (0.979–3.051) | 4,194,552 |
| 65,536 | Eclipse HashBag | 1.561 (0.735–2.111) | 2,097,736 |

Default-capacity growth is part of admission cost. Removing per-key count wrappers does not guarantee lower cumulative admission allocation when several primitive/reference arrays grow. No retained-object-graph measurement was made. M3 includes insertion-order metadata and application flag columns; Apache HashMultiSet and Eclipse HashBag do not offer the same feature combination. Eclipse already has a primitive counter owner.

## Updates to existing keys

| Distinct keys | Owner | Count actions per sample | CPU ms, median (range) | Allocated bytes |
| ---: | --- | ---: | ---: | ---: |
| 4,096 | M3ObjectCounts | 32,768 | 0.184 (0.176–0.202) | 0 |
| 4,096 | Guava LinkedHashMultiset | 32,768 | 0.173 (0.138–0.206) | 0 |
| 4,096 | Apache HashMultiSet | 32,768 | 0.190 (0.149–0.240) | 0 |
| 4,096 | Eclipse HashBag | 32,768 | 0.113 (0.110–0.146) | 0 |
| 65,536 | M3ObjectCounts | 524,288 | 6.381 (5.431–11.104) | 0 |
| 65,536 | Guava LinkedHashMultiset | 524,288 | 5.965 (4.948–7.594) | 0 |
| 65,536 | Apache HashMultiSet | 524,288 | 6.767 (5.336–8.436) | 0 |
| 65,536 | Eclipse HashBag | 524,288 | 5.898 (3.782–6.538) | 0 |

Zero allocation is an observation for this warmed existing-key workload. It does not cover distinct-key churn, views, iterators, bulk operations, overflow or all provider methods.

## M3 flags

Each write sample performs four writes per key. Selection requires bit 0, forbids bit 1 and uses the existing Java mask backend; an unrelated sign bit is also present in the data. Every selection constructs primitive metadata and a detached unmodifiable result list. The donor multisets/bags lack equivalent application flag columns, so these phases have no donor comparison.

| Distinct keys | Phase | Actions / keys scanned | CPU ms, median (range) | Allocated bytes | Selected keys |
| ---: | --- | ---: | ---: | ---: | ---: |
| 4,096 | flags_write | 16,384 | 0.591 (0.100–0.782) | 0 | 0 |
| 4,096 | flags_select | 4,096 | 0.084 (0.076–0.111) | 89,048 | 1,365 |
| 65,536 | flags_write | 262,144 | 2.601 (1.662–2.801) | 0 | 0 |
| 65,536 | flags_select | 65,536 | 1.069 (0.907–1.738) | 1,488,592 | 21,845 |

## Contract checks and scope

Every measured count state is checked at every key, including distinct size and total count. At 4,096 keys, the total count is 10,240 and the weighted count checksum is 20,981,760. At 65,536 keys, the values are 163,840 and 5,368,872,960. Selections contain 1,365 and 21,845 keys, with weighted selection checksums 2,796,203 and 715,827,883. Selection content/order and unchanged multiplicity revision after flag writes are checked. All 160 sample assertions passed.

The earlier results remain in `../count-owner-cost/`, explicitly labeled as prior-base evidence for `4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`. Their raw samples and metrics are retained. Their CPU values and this run come from separate JVM executions; differences cannot be assigned to the rebase, and the runs are not pooled into a speed comparison.

This is one shared-container JVM, finite warmup, two sizes and a narrow positive-count workload. It does not measure cold startup, JNI/native memory, retained heap, concurrency, presizing, null/collision-heavy keys, complete removals, provider views, bulk operations or overflow. Finite warmup does not prove final compilation tiers. The class directory is a diagnostic candidate; final named-recipe execution and product/native gates remain independent.

## Files

- `count-owner-cost-samples.csv`: all 160 measured rows plus JVM metadata.
- `count-owner-cost-receipt.json`: complete measured class/JAR closure, unchanged harness hash, commands, environment, raw grouped samples and limitations.
- `../../contract/CountOwnerCostProbe.java`: unchanged canonical source; no runtime source addition.
