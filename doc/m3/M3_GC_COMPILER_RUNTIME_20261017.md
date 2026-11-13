<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 compiler, runtime and GC coordination (initial receiver)

## Existing source authority

- Synexia M3ReferenceFlow and M3StaticUsePlan own compiler-attributed alias,
  escape, lifetime and effect proofs. Do not duplicate their analyzers.
- Synexia M3GcMetadataAdmission and M3GcBackingAdmission keep GC-sensitive
  reference paths conservative.
- M3JDK21 M3ObjectResidency already owns primitive parent state, int[] borrow
  counts, generation-fenced handles and signalMemoryPressure().
- This opt-in java.management adapter adds GC epoch feedback at ordinary
  application boundaries and conservative compile-fact mode selection.

## Correctness

Compiler facts cannot be inferred from annotation text or static use-site count.
The site-bound proof must include unknown calls, observed identity, monitors,
reflection, JNI, serialization, reference processing and exceptional escape.
Unchecked data cannot authorize reclamation. Shared backing uses the existing
cooperative lease protocol, and HotSpot owns all Java-object reachability.

GC notifications are merely hints. No application resource cleanup runs from
the notification handler. A failed safe-boundary pressure action is retried.
The existing Java base and ordinary collection behavior are unchanged.

## Next distinct source-pinned phases, not implemented in this PR

1. Reconcile Synexia withLease, lazy-backed retain/release and tryRetire
   additively using a canonical Maven/OpenRewrite recipe and exact preimages.
2. Bind source-verified compiler facts to targeted M3JDK21 call sites.
3. Add opt-in HotSpot shallow-object-size diagnostics from existing
   oopDesc::size_given_klass and array layout, not a duplicate heap walker.
4. Prototype collector-aware side-table instrumentation behind a VM flag;
   retain all JVM-managed roots, GC barriers, C1/C2 deopt and JNI behavior.
5. Require jtreg/JMM/JNI and JDK debug-image verification under G1, Parallel,
   Serial, ZGC, and Shenandoah before proposing collector internals changes.

This phase DOES NOT implement global Java reference counting, rewrite
java.lang.Object, modify GC barriers or prove a performance improvement.
