<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->

# Versioned bitmap-view invalidation: exact receiver qualification

## Behavior repaired

A revision supplier, source collection or predicate can call `VersionedView.invalidate()` reentrantly while `current()` captures a filtered snapshot. The original owner cleared its cache but then unconditionally published the just-built cache, erasing that invalidation.

The received implementation keeps one monitor-guarded generation counter. Invalidation advances it and clears the cache; capture publishes only if the generation is unchanged. The current call returns its already-computed detached result. It does not retry the source, predicate or revision callbacks. A subsequent access rebuilds after invalidation.

The existing source-revision checks, detached snapshots and old iterators retain their behavior. Manual cache invalidation alone does not invalidate a retained snapshot or an iterator whose source revision has not changed. Sorting and JNI owners are unchanged.

This is a correction to the existing `m3/collections` owner in M3JDK21. It adds no runtime dependency on Synexia and makes no full-JDK or whole-Eclipse acceptance claim.

## Exact source custody

- Executed qualification baseline: `hsoliwal/M3jdk21@d6d76b563050fc13c4fa79bc494edf29f1614b37`.
- Receiving admission base: `hsoliwal/M3jdk21@73f59218dc4ef1a3ef79aad990e77273db2a2d60`. The complete comparison from the executed baseline changes three documentation files only; collection sources, native sources, tests and the catalogue are unchanged.
- Canonical recipe and qualification: `hsoliwal/com.synexia@ae862e424195f3a3fe6b66d27c1e74640a3aa139`.
- Canonical crate: `synexia-openrewrite-recipes/recipe-crates/m3-collection-view-invalidation-20261020`.
- Named Java recipe: `com.synexia.m3.CollectionViewInvalidation20261020`.
- Transformation owner: unchanged `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`, Git blob `135a28d0cec9ea988607cdef6caa1f0fe5115d75`.
- The existing unpublished Synexia convergence candidate was reused with exact hashes and attributed in the canonical source packet; its seven-target composite recipe was not applied to this two-file receiver.

| Product source | Before Git blob | After Git blob |
| --- | --- | --- |
| `src/main/java/com/m3/collections/M3LongBitmapFilteredSnapshot.java` | `f82ffe51ababbdd9b663ceb2b113d7efc9e121e0` | `385d800b0dff1eb7f437e9d7958602f81339aee3` |
| `src/test/java/com/m3/collections/M3LongBitmapFilteredSnapshotSelfTest.java` | `a714930c92090556127fa2559c2d91f58fdae434` | `5556240c3f9c51878dcc565e1e29307d8575d220` |

Both paths are relative to `m3/collections/`. The public/protected API and native source/ABI remain unchanged. Java source materialization used actual attributed OpenRewrite Java trees and exact source seals, not a raw-text Java rewrite.

## Executed gates

| Gate | Observed result |
| --- | --- |
| Original receiver compilation | All 268 production Java files compiled to 507 classes under Java 21 with warnings treated as errors. |
| Native build | The two unchanged mask-compactor/window C owners compiled as C17 with warnings treated as errors and fatal UBSan; 11 exported JNI symbols matched generated headers. |
| Baseline behavior | Four original contract programs passed in Java and real JNI modes. |
| Defect discriminator | The emitted regression compiled against the original implementation and failed at the expected reentrant-invalidation assertion. |
| Candidate behavior | The same four original compiled contract programs passed against the candidate in both modes. |
| Received regression | 119,502 checks in Java; 135,760 checks with real JNI. Both runs passed under `-Xcheck:jni`. These are assertion/check counts, not counts of independent test programs. |
| API and JNI custody | The two affected public descriptor dumps matched exactly; all 12 generated JNI headers were identical. The existing native library was reused unchanged. |
| Actual OpenRewrite SDK | Six tests, zero failures, errors or skips. Exact named application, four mixed before/after states, fixed point, serial/reversed/two-worker replay, drift/missing/duplicate/non-Java refusal and attributed call sites passed. |
| Recipe output discriminator | A coherent no-change manifest failed the frozen two-result assertion. The raw expected failure is retained. |
| Input freshness | The tested source inventories and emitted output hashes were checked again after execution. |

The first candidate SDK run exposed an incorrect harness assumption about scheduler-added `RecipesThatMadeChanges` markers. Its failure and diagnosis are retained separately. The corrected harness checks preservation of input markers plus exact named/leaf recipe lineage; no production postimage or transformation owner changed.

The product qualification began from those already-emitted exact files while that harness correction was being completed. Its original receipt records that timing. A later binding connects the final green SDK receipt to the same output hashes; the original receipt has not been rewritten to imply a different chronology.

## Bounded resource observation

The observation used actual Java Instrumentation object sizes, thread allocation/CPU counters, and the genuine native library on Temurin 21.0.12.1+1 with compressed references, eight-byte object alignment, Serial GC and identical input/VM flags. The input had 1,024 source values and 512 admitted occurrences.

| Observation | Original | Candidate |
| --- | ---: | ---: |
| Shallow `VersionedView` | 32 bytes | 40 bytes |
| Shallow per-snapshot cache | 32 bytes | 32 bytes |
| Owned cache/snapshot graph | 7 objects / 10,528 bytes | 7 objects / 10,528 bytes |
| Predicate calls during warmed cached reads | 0 | 0 |
| Allocated bytes, cached-read batches 0 / 1 / 2 (two million reads each) | 0 / 0 / 0 | 672 / 0 / 0 |
| Allocated bytes for 1,000 forced rebuilds | 24,104,000 | 24,104,000 |
| Predicate calls for those rebuilds | 1,024,000 | 1,024,000 |
| Allocated bytes for 1,000 unchanged native snapshot operations | 15,864,000 | 15,864,000 |

The graph excludes the view itself, shared source/callbacks, shared enum constants, VM metadata and native memory. The additional guard costs eight bytes per view on this measured VM; the snapshot storage graph does not change. The first candidate cached-read batch's 672 allocated bytes are retained as an observation, without attributing them to a particular source instruction.

These are bounded observations from one process per implementation. Raw wall/CPU time, heap, RSS and native-memory records are retained without a universal throughput, zero-allocation, GC, or full-runtime claim.

## Algorithm catalogue

The existing `m3/collections/algorithm-catalog/collection-candidate-comparison-20261017.tsv` is extended additively with the incumbent, selected generation guard, rejected callback retry and insufficient monitor-only alternatives. The existing category/donor inventory remains intact.

Rebuild complexity stays O(n) for the source/capture contract; unchanged-epoch cache lookup stays O(1) plus the caller's revision supplier. The selected guard adds one primitive field and no retained per-element nodes or wrappers. Automatic retry is rejected because it changes callback counts/effects and can fail to terminate when callbacks invalidate every pass. Locking alone is insufficient because Java intrinsic monitors permit same-thread reentry.

GitHub donors and the existing Synexia catalogue were inspected first. LeetCode, HackerRank and GeeksforGeeks references remain category/adversarial-input evidence; no challenge statements, editorials or submission implementations were imported. Existing per-file copyright and Apache-2.0 notices remain authoritative.

## Qualification limits

This work qualifies the selected existing module owner and the stated Java/JNI closure on Linux x86-64. It does not certify all 254 collection test sources, all native owners, every interleaving, every VM layout, other operating systems, the full Maven reactor, a newly built M3JDK21 image, jtreg across the product, or hosted CI. Those gates are separately scoped and are not inferred from this result. The repository's receiving phase and existing String/array/collection promotion rules are unchanged.

The change is submitted for review; no default branch or release is modified by this packet.

## Review and inspectable evidence

An independent exact-source review found no Critical or Important issue within
this bounded repair. Additional retained Java 21 witnesses exercise invalidation
inside the source callback, a nested snapshot that must remain cached, source
exception identity, and a coordinated concurrent invalidator. The first two fail
against the original owner and pass against the received owner. They are retained
qualification witnesses, distinct from the permanent product self-test main.

`evidence.json` contains the exact 109 task files from the published canonical
source commit, with their original path, mode, UTF-8 bytes, SHA-256 and Git blob.
It includes actual SDK reports, selected Java/JNI results, resource observations,
the original failures and diagnoses, source snapshots, catalogue decision, review
and source-freshness comparison. The two shared canonical metadata files are
outside this archive. Content fields serve as review evidence and are never
loaded as runtime source. Canonical recipe engines stay in their existing owner.

The later receiver-metadata recipe qualifies this README, this archive and the
additive catalogue append through its own exact Text SDK admission. Its execution
and publication records belong to the companion canonical pull request; they are
not recursively embedded into the bytes being qualified.
