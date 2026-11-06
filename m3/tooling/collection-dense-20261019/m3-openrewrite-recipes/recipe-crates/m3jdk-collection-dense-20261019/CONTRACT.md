<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Integrated collection dense refinement

The target is M3JDK21 java.base, continuing public PR 1435 at
2717d156ad2aba73dfdfcfc6a58b59a82371798e. This is an incremental stacked review;
it neither duplicates the integrated factories nor merges their pending parent.
Canonical transformation ownership remains Synexia. No runtime layer or dependency is added.

## Frozen contracts

Public/protected interfaces, private JNI names, validation order, exception classes,
callback order, shallow element identity, stable sorting, immutable snapshots,
offset/reverse windows and live filtered/sorted view behavior remain unchanged.
Mask extraction requires stable input; existing best-effort count-drift detection remains.
Each predicate executes exactly once per captured element before sorting. The owned
snapshot array may be retained when every element passes; no caller-owned array is retained.
The existing source.toArray contract supplies an independent Object[] snapshot.
JNI validates bounds in 64-bit arithmetic, uses fixed stack buffers, retains no heap
pointers and invokes no Java predicate/comparator. Java remains the default backend.

## Admitted internal changes

1. Count already-evaluated accepted snapshot elements while setting the existing mask.
   Reuse the captured reference array if count equals input length. Partial selection
   continues through the existing Java/JNI index selector. Do not change sorting.
2. After counting, dispatch a fully selected requested range to consecutive-index fill.
   Recount its words before filling to preserve best-effort count-drift detection.
   Partial selections retain the original sparse loop without a per-word density branch.
3. Apply the same whole-range dispatch in JNI, using the existing bounded output chunks.
   Keep original validation, allocation, exception, native fallback and sparse paths.

## Prior work and donor decisions

The existing collection catalogue records OpenJDK BitSet/Long word traversal and
ArrayList selection/compaction, Eclipse FastList comparison, LeetCode Remove Element,
HackerRank Dynamic Array, and GeeksforGeeks stable sorting. Reuse those recorded
decisions and existing target code. No challenge or foreign library source is copied.
The public parent already discloses the exact mask/snapshot mechanisms; this pass is
routine maintenance of that public source. Target GPL/Classpath and test GPL notices
remain intact; new canonical authoring material retains Apache-2.0 attribution.

## Verification order and boundary

Inventory and documentation -> recipe templates -> diff/static lint -> compile ->
actual Maven/OpenRewrite tests and materialization -> generated-source lint -> Java/C
compile -> contract tests -> native/runtime checks -> allocation/CPU diagnostics.
Record pre/post SHA-256, source Git identities, every command/result and failed attempt.
Do not edit generated outputs. Recipe refusal and fixed point are mandatory.
Baseline/candidate run the same existing tests and added dense boundary corpus.
No timing assertion grants admission. Full product image, other platforms and the global
String -> arrays -> collections promotion remain pending. This is a finite refinement,
not completion of all Eclipse, JDK or third-party collection families.
