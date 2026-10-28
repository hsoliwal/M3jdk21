<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Primitive array/mask receiving contract

M3JDK21 is the integrated Java/HotSpot/JNI SDK and runtime owner. This pass refines existing collection implementations; it adds no M3-on-JDK layer, payload hierarchy, mandatory Synexia runtime or JNI callback bridge.

Pinned product input: `065e3626aac2de62d2f531aec3bed2fca533c9f9`.
Canonical recipe workspace: `hsoliwal/com.synexia`, branch `aix/collections-array-masks-20261020`, task crate `synexia-openrewrite-recipes/recipes/keyed-api-20261015/collections-array-masks-20261020`.

## Locked contracts

M3ArrayList removal retains encounter order, duplicate/null/reference identity, callback count/order, exception identity, structural-modification rejection, live nonstructural set effects, tail-reference release, capacity, serialization and public/protected signatures. Its exact preimage Git blob is `250905f67429fbedfb4d6f652ea6e90a516cb59c`.

M3IndexedListSnapshot masked sorting retains physical occurrence identity, immutable source views, stable comparator behavior and exact callback tape. Reuse the fresh selected int[] and the existing private sortPositions atom, following the already existing filteredSorted ownership pattern. Its exact preimage Git blob is `217700023c2a6456a505b07b311290f8da8c02e2`.

Existing M3IndexMaskCompactor and M3IndexPathMasks remain current-source dependencies. The original path-mask C17/JNI selector is reused unchanged. No new native symbol or production API is admitted. Existing production owner names are unchanged; this is private refinement, not a new naming-map migration.

## Admission

Canonical source-sealed Maven/OpenRewrite recipes must emit exact target source and regression tests. Retain pre/post SHA-256, source drift/refusal, partial-state convergence, fixed point and immutable original/candidate oracles. Execute diff -> source lint -> compile -> tests -> runtime, then compare API and JNI headers. Keep failures and source-epoch distinctions explicit.

Bounded component/JNI proof and resource measurements do not replace the original collection-module suite, source-built JDK image, hosted acceptance or all-fork coverage. No merge or global receiving-phase advancement is authorized by this receipt. The full all-collection atomize/patternize/arrayify/catalogue/IOP programme remains outstanding outside the exact qualified paths.
