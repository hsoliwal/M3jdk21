<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 stable typed-signal domain Top-K

This is a **direct modification to the M3JDK21 product's existing** `M3SignalColumns` and a new internal primitive index/heap atom. It is not M3 + JDK, an external runtime library or a new JNI ABI. Source inputs and outputs are sealed in canonical Synexia crate `m3-typed-signal-topk-20261022`; the original three-layer dependency stack remains `M3SignalColumns #1803` → `SourceSignalImage #1809` → this Top-K candidate. These branches are **reviewable**, not falsely claimed merged.

The existing public `M3SignalColumns.compare` still returns its diagnostic sign/decisive-column/gap/evidence record. Private stable mergesort now uses `orderOnly` to obtain only a primitive sign, avoiding per-comparison `Comparison` allocations while keeping the exact typed, NaN, long-precision and missing-last order.

`M3SignalTopK` accepts an explicit caller-selected precedence of existing column IDs; one `int[]` worst-first row heap and one `int[]` occurrence ordinal heap are the entire transient storage. Equal-key source occurrences preserve encounter order, including repeated physical IDs. Physical state filters are 32-bit word masks; the bounded heap consumes eligible rows directly without allocating an n-length filtered directory. No Java object, Entry/Node, user comparator or JNI handle is stored or crosses native code.

**Real gates:** strict Java21 original source and standalone differential, JUnit in the owning Maven module, canonical Maven/OpenRewrite first application/fixed point/preimage refusal, full source-built JDK, JNI/HotSpot/JTreg/JCK, ABI, CPU/retained heap/native RSS and cross-platform. The earlier isolated algorithm kernel passed 23,302 checks; this exact newly received test file must run separately before any product PASS is claimed. The original M3ProgressiveSignalEvidence remains the only metric-bound/certificate authority.

The global String → arrays → collections → AST/compiler receiving order has not been declared complete.
