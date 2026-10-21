<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Filtered primitive deque insertion dispatch

`DequeView.addFirst` and `addLast` perform the existing admission, then invoke
the matching backing add once. The old implementation discarded a refusing
offer's boolean result. Backing exception identity and any partial owner effects
now follow the throwing add contract.

The existing primitive payload, view fields, interfaces, callback admission,
offer results, queue aliases, signed-long values and live order remain intact.
This correction adds no metadata object, payload copy, JNI crossing or runtime
dependency. It reuses the established `M3FilteredDeques` dispatch atom.

The protected `M3FilteredDequeDispatchProof` contains separately runnable original
failure witnesses, exact callback/exception checks and deterministic live workloads
over the actual `M3LongArrayDeque`. `M3FilteredDequeDispatchTest` is its real JUnit
entry point. The corresponding canonical source packet contains execution logs,
source seals, declaration/JNI header comparisons and unresolved gates.

Canonical recipe: `com.synexia.rewrite.M3Jdk21FilteredDequeDispatchRecipe`, task
`synexia-openrewrite-recipes/recipes/jdk21-filtered-deque-dispatch-20261018` in
`hsoliwal/com.synexia`. It delegates to the existing unchanged
`M3GpuToolJoinRecipe` and `M3GpuToolJoinManifest`; the product is received SDK output.
Input target pin: `a3e88976ee8664d1ef5d0cade96aec1206b2133d`.

The packet was reconstructed after a workspace reset. Only its fresh execution
receipts apply to the reconstructed sources; historical test counts are not reused.
Keep the receiver draft until its separately recorded canonical source-built SDK,
owning reactor/custom JDK and hosted exact-head gates execute. This bounded
collection correction does not advance the active String receiving stage or
qualify native runtime behavior. No speedup is claimed.
