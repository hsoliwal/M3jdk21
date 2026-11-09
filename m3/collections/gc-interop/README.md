<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21: opt-in compiler/GC residency bridge

This is an optional Java 21 java.management interoperability JAR, not a replacement
for java.base, M3 collections, HotSpot reachability or Synexia compiler/lease owners.
The existing M3 collections core retains its java.base-only dependency.

M3GcUsePlan classifies externally VERIFIED exact-site Synexia compiler evidence.
Unknown, externally observable, and unproven reference lifetimes are refused.
A plan by itself NEVER authorizes reclamation of a Java object.

M3GcNotifications.enable() subscribes to supported collector MXBeans. Notifications
only increment an atomic epoch. They NEVER invoke eviction or allocation, run a
heap walk, hold JNI references, or execute application callbacks inside the
notification handler. M3GcPressureGate coalesces epochs and invokes its
Runnable only at an application-defined, safe operation boundary.
Failure preserves an unacknowledged epoch; reentrant polls do not double-call.

Integration with the existing parent-owned M3ObjectResidency arena:

    M3GcNotifications.enable();
    M3GcPressureGate gc = new M3GcPressureGate(arena::signalMemoryPressure);
    // At an application boundary, outside the GC callback:
    gc.applyAtSafePoint();
    arena.use(slot, estimatedBytes, value -> value.toString().length());

The owner can retainProjection/releaseProjection for generation-checked borrows;
ordinary JVM references, JNI roots, weak/phantom observers, cycles, safepoints
and object reclamation remain solely under HotSpot GC authority.

Strict local Java21 source verification: sh verify.sh
The pinned source receipt and 19 checks ran under Serial, Parallel, G1, ZGC and
Shenandoah. No source-built HotSpot, full jtreg, Maven/OpenRewrite SDK, JNI,
perf, or cross-platform proof is claimed. This bounded receiver remains
draft until canonical Synexia recipe and whole-product admission are qualified.
