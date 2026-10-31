# Synexia actual-Xmx receiver for M3JDK21 (2026-10-14)

This is a **non-promoting evidence receiver**, not another memory or
widget manager. Synexia PR #11020 adds read-only `M3MemoryPressure.current()`
and `describe()` to its existing Java/JNI pressure owner.

M3JDK21 already owns the same core heap measurement at
`com.m3.collections.M3ObjectResidency.HeapSnapshot.current()`. The
source-sealed receiver adds only a target JUnit oracle and a pending
provenance receipt. It deliberately does not mutate java.base, HotSpot, Swing,
SWT, or any production collection code.

Repository receiving order remains:
`STRING -> ARRAYS -> COLLECTIONS -> AST_COMPILER -> REMAINING_FAMILIES`.
This receipt does not advance that order.

The target local thin recipe `M3XmxReceiverRecipe` composes the existing
`M3Jdk21HashPinnedSnapshotRecipe` and
`M3Jdk21HashPinnedTextSnapshotRecipe`, rather than forking a new engine.
Canonical resource contents and targets are byte-identical. Source lock
records donor PR, commit, licensing mode and explicitly false promotion.

Verification needed: actual target module/JUnit, source fixed point,
full target migration/reactor checks, platform and resident-heap/native
resource measurements. Hosted statuses require executed steps, not
merely a workflow conclusion.
