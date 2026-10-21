<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Shared mask-window kernel contract
Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

## Existing owners only
M3IndexMaskCompactor remains the Java/JNI selection owner. M3LongIndexProjection and M3LongAddressKernel remain unchanged consumers. Live predicate/comparator wrappers remain distinct from immutable index snapshots. No new public class, public method, JNI export, collection payload owner or parsing layer is introduced.

## Admitted private substitutions
1. Java full-array int and long selectors and independent-offset selectors share one private emission kernel per primitive width. Preserve each public boundary and validation order, all-selected defensive copies and native-required dispatch. Count routines share the existing offset count loop.
2. The existing C int range kernel accepts both row and state offsets; full/range/window exports keep their signatures and validation, then delegate to that kernel. The existing window count loop is reused.
3. Native int/long emission scans rejected prefix marks before fetching row payloads. A completely rejected chunk still reads/checks states but does not fetch row IDs. A nonempty chunk compacts into its existing primitive row buffer, never a second accepted-value buffer. Zero mask never reads states.

## Locked behavior
Unsigned masks 0..255; original null/range/mask exception classes/messages/precedence; stable selected occurrence order; duplicate/signed/extreme row IDs; independent row/state offsets; detached exact-size output; no input mutation or retained arrays; missing-native refusal; legacy range capability path. Existing best-effort changed-count detection remains in both native passes. Input arrays must remain stable during calls: not a concurrent snapshot or linearizable API.

## Evidence to execute
Exact Git blob and SHA256 baseline admission; original-source corpus; generated-source corpus with same trace; all masks, 0/1/255/256/257/511/512/513 chunk boundaries, offset padding, sparse rejected chunks, all/none selected and high bit; original sorted/sliced projection integration; private-native hostile inputs; unchanged javap public descriptors and all seven current JNI exports; real -Xcheck:jni; strict C17 and fatal UBSan; test-only JNI call counters and injected state drift; recipe prewrite drift/corrupt/missing/occupied/symlink refusals and second-apply timestamps.

## Verification order
Each candidate source pass: patch -> lint/syntax -> compile -> tests -> runtime. Native -fsyntax-only precedes shared-library compilation. Java strict lint compilation is recorded separately before bytecode build. Mutation experiments are isolated variants, each independently linted and compiled before execution. A failed gate blocks downstream stages for that variant.

## Boundaries
No measured throughput, build-system acceleration, whole SDK/JDK/GC/platform completion, or String-to-collections phase advancement is inferred from this task. A deterministic native call-count reduction is distinct from elapsed-time evidence. Maven/OpenRewrite execution is unverified until an actual runner succeeds.

## Forward-preserved master additions
selectPositions, identityPositions, NativePositions and both positions JNI exports remain in their current owners. Their behavior is included in the existing fixture matrix. The pre-positions binary must trigger the unchanged Java capability fallback, not lose native-required semantics.

## 2026-10-21 retained current atoms

Preserve required/excluded logical-position selection, selectedCount overload, its
capability/refusal boundary and all nine current JNI exports. Reuse the current long
prefix compaction. The unchanged current M3LongIndexProjection consumer is pinned and
compiled. See the current reconciliation supplement for exact preservation and runtime evidence.

Preserve the additive four-argument selectLong required/excluded overload from PR#1638; its body is byte-identical and its fallback to the existing required-only selector is differentially exercised.
