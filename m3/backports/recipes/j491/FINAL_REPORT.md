# JEP 491 materialization substrate

This pass converts the prior inventory-only JEP491 lane into executable M3 substrate without claiming the 246-file VM change is safe to promote.

The exact OpenJDK implementation denominator is pinned at 246 paths from JDK-8338383. A deterministic classifier maps each path to CPU architecture, shared HotSpot, java.base, native, JVMTI, JFR, SA, or proof-only test domains. A reusable commit-patch atomizer preserves the exact upstream parent-to-implementation patch while testing each FILE atom against the current M3JDK21 target tree in isolation. It never mutates the checkout.

Mechanically applicable modified/added atoms receive candidate postimages and hashes. Deletes and renames remain explicit review operations. Context conflicts, occupied additions, missing targets and drifted deletions receive typed dispositions rather than guessed equivalence.

The hosted workflow also inventories intersections for five pinned post-integration fixes. It does not automatically absorb them.

Product materialization, per-architecture HotSpot builds, virtual-thread monitor/JVMTI/JFR/SA jtreg, JNI critical-section pinning controls, stress tests and performance evidence remain mandatory before any compatibility/promotion claim.
