<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Monotone encounter-window refinement

Status: candidate authoring for the canonical Synexia recipe; no product promotion.

Baseline repository: `hsoliwal/M3jdk21`.
Baseline commit: `a4415af418a9b0f21a06cd5801d238577fbb6eb7`.
Owner: `m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java`.
Baseline SHA-256: `7bf939ea374ce6c0b13c403f5e154c0f8d376f9cddb8e623cf00ef299183a77e`.
Baseline Git blob: `d18055008f6eabbd6603e080ff32f8d5a1da8b60`.

## Existing contract and bounded change

The owner stores one privately snapshotted long payload and primitive physical
index arrays. Slices and filters preserve encounter order and stable occurrence
identity. Ascending and descending sorts are stable signed-long orders; equal
values retain the current encounter order. No mutable caller array is retained,
no element callback executes during sorting, and existing views never change.
Public signatures, Order tags, mask behavior and Java/JNI ABI remain unchanged.

The existing method already shares same-order views and reverses equal-value
groups for a known opposite order. Those atoms stay unchanged. Only ENCOUNTER
windows with at least two rows gain an adjacent-value monotonicity check. It
uses the existing address kernel, stops at the first inversion, and falls back
to the byte-for-byte existing stable merge body on failure. Signed comparisons
do not subtract long values, avoiding overflow.

A successful full-index-window check shares the immutable index. A successful
partial-window check copies only the selected index range once, retaining the
baseline's detached-directory size instead of pinning a potentially enormous
directory for a tiny result. The payload ownership is unchanged in both cases.

## Alternatives and preserved negative knowledge

- Retain the current merge as the fallback for arbitrary encounter order.
- Retain the already-qualified group reversal for known opposite order.
- Reject unconditional index sharing for partial windows: it can extend
  retention of unrelated index entries beyond baseline sorted behavior.
- Reject marking every encounter snapshot sorted at construction: this changes
  exposed Order metadata and adds work to unrelated callers.
- Reject general comparator or live-wrapper adaptation: mutable values and
  effectful comparators/predicates require separate callback/invalidation proof.
- Reject importing full TimSort or challenge solutions for this small atom.

Prior source decisions consulted:
`m3/collections/algorithm-catalog/stable-order-restoration-20261020/CANDIDATES.tsv`
(ORDER-001 retains group reversal; ORDER-002 preserves general stable merge),
and `wrapper-convergence-20261018/ACCEPTED_ATOMS.tsv`.
Known COW, read-only endpoint, comparator-representative and Deque-equality
repairs already have separate PRs; this refinement does not overwrite them.

## Verification plan retained before execution

Run the exact unchanged baseline and candidate production closures against one
independent stable JDK reference oracle. Cover exhaustive short value sequences,
all slice boundaries, explicit extremes, stable ties, repeated sorts, direction
changes, full-width byte masks, mask-selected monotone subsequences, public
array isolation and optional required-native selection under `-Xcheck:jni`.
Compare public/protected JVM descriptors. Record any unexecuted gate explicitly.

Benchmark baseline/candidate in alternating fresh processes. Record cold first
sort, warmed repetitions, full monotone ascending/descending/equal data, random
data, first/last inversion, and partial-window cases. Keep snapshot preparation
outside warm-sort measurements, record allocation, thread/process CPU, wall
time, and observational heap/RSS separately. No general speed claim is implied.

Full source-built JDK, installed module, all native/platform and broader
collection-family acceptance remain separate gates. Ordered receiving stays
STRING; this is maintenance of an existing collection proving-ground owner.

## Executed finite qualification

The original baseline and candidate were compiled with Java 21, strict lint and
fatal warnings. The actual source closure is M3LongIndexProjection,
M3LongAddressKernel and M3IndexMaskCompactor. The unchanged original JNI mask
compactor was compiled as C17 with fatal warnings and fatal UBSan checks.

Each baseline/candidate, mixed/interpreted, Java/required-native oracle run passed
1,836,766 assertions over the same 132,070 scenarios and produced digest
`30748f642d55955a`. These eight executions replay the same scenario set; they are
not eight disjoint test populations. The existing projection selftest passed
20,706 checks for each variant. Public/protected JVM descriptors and both
generated JNI headers are byte-identical. Strict native runs use `-Xcheck:jni`.
The new JUnit adapter remains separately subject to the owning Maven build.

## Resource decision

The controlled workload uses 32768 source rows, three alternating process pairs,
SerialGC, 256 MiB fixed heap and batch compilation. Each initial process records
one cold first sort, 80 warmup sorts and three 80-operation samples. Nine warm
samples per variant/category are summarized; these are measurements on this
host, not a universal throughput claim. Raw local receipts identify commands.

For ascending, descending and all-equal full windows, median warm thread CPU
ratios are 0.0261, 0.0290 and 0.0266, and allocated bytes per operation fall from
262208 to 32. Partial 16384-row windows fall from 131136 to 65584 bytes while
retaining their detached 16384-entry index. Full mask-selected monotone windows
share their already bounded 10923-entry index. Random and early-inversion
inputs retain identical allocation and the original merge fallback. The late
inversion adds a full failed scan: median CPU ratio 1.0175, with no allocation
improvement. This negative result remains part of the selection record.

An initial 16-row slice measurement with only 80 warmup calls reported ratio
1.417. A targeted follow-up increased warmup and each sample to 10000 operations,
without changing production bytes: median 266.47 ns to 162.70 ns, ratio 0.611,
192 to112 allocated bytes. Both results remain recorded. The unchanged
known-order control also varies, demonstrating that small timing differences
cannot be attributed entirely to this atom. Heap after requested GC and Linux
RSS are observations, not exact retained-size or native-memory proofs.

## Admission

The source and bounded contract evidence support receipt as an existing-owner
performance refinement. Canonical source-sealed SDK application, fixed point,
drift refusal, final receiving commit and final owning-build evidence are
recorded by the convergence recipe. This file does not assert those steps ran.
The STRING stage remains unchanged. Consumer opportunities and wrapper repairs
require their own exact source contracts and admissions.
