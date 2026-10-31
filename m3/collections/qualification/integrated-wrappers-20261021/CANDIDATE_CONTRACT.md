<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Frozen candidate contracts

Pinned M3JDK21: 33d2e6ae8048c436bd55b951d9f7dbfab2f3efcb.
Pinned canonical Synexia: 408373f4101fdca2fb3625bec8af1234e5fd3e26.
These are proposed candidates; none is accepted until executed qualification.

## Existing integrated owners

1. jdk.internal.m3.M3IndexedArray: select without predicate and without state awareness may copy the private order[0,size) under the existing synchronized monitor. All predicate/state-aware loops, epochs and comparator calls remain exact. Stable sort keeps its comparison/epoch loop and only bulk-copies unopposed tails. Private growth/state behavior is not changed.
2. jdk.internal.m3.M3IntAlgorithms: lonelyDense/lonelySparse may return their newly allocated unexposed result when all positions are used; partial selection still trims. Caller input and earlier returned arrays remain independent. Values, encounter order, extreme-int guards and error precedence remain exact.
3. java.util.CollectionViews: isEmpty may scan backing.iterator directly rather than allocate an intermediate filtered iterator. First accepted occurrence, predicate call order/count, exception propagation and empty behavior must be byte-for-byte trace equivalent. Existing membership/addAll/spliterator changes from other PRs are outside this atom.

## Existing SDK/proving owners

4. M3IndexMaskCompactor C: required+excluded int/long window emission may skip payload reads for rejected chunks and trim only the rejected prefix of a nonempty chunk. Reuse the existing two-mask predicate to find the first accepted ordinal; read the remaining suffix, without a reverse last-match scan or predicate fusion. Two-pass state observation, mask/geometry validation, stability, exact detached output, count mismatch errors and unchanged JNI symbols remain locked. A zero first-pass count keeps its existing immediate return. No native heap allocation, critical pin or callback is added.
5. M3LongBitmapFilteredSnapshot: use a private occurrence-index BitSet plus exact selected count for JAVA windows of at least 64 positions, reusing filteredOrdinals. Keep the existing byte path below one full logical word and for NATIVE_REQUIRED, avoiding extra small-window BitSet-object overhead. Predicate once per occurrence, physical bitmap writes, order, duplicates, backend admission and native-required route remain exact. Measure both sides of the 64-position boundary and sparse/dense workloads; the candidate stays rejected if resource gates regress materially. The immutable projection owns its payload and directory; a caller's mutable or subclassed BitSet never enters this path.
6. Previously published M3ArrayifiedIndexOps rotation/selector/merge-tail output can be reused only after requalifying the current selector and engine; its old KEEP seal cannot be waived.

## Evidence and stop rules

New test oracles are authored under templates before generating target tests. Existing tests remain immutable. All product mutation must be emitted by a canonical Maven/OpenRewrite recipe using exact preimage/postimage seals. Baseline and candidate use identical protected fixtures. Compare API/JNI symbols, deterministic callback/output tapes, borrowed-array hashes and actual runtime outputs. Measure allocation and JNI transferred bytes separately from CPU timings.

Compile integrated source inside java.base and run its real original tests; report patch-module execution distinctly from a full source-built/installed JDK. The complete five-family/JCK/platform program is not admitted by this finite pass. Preserve previous rejected snapshot-sort evidence and existing relevant PR lineage.
