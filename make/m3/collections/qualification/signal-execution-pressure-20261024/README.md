<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Product receiver — signal Boolean mask fusion and M3 cancellation

The M3JDK21 original owning classes are refined **inside** the existing modified JDK product.
Synexia retains all reusable Maven/OpenRewrite recipes and source/postimage seals; this
public receiving packet contains only generated Java21 postimages, target JUnit, CI and
passive proof receipts. No Synexia runtime dependency, new compiler, separate JDK layer,
new JNI export or new per-row Entry/Node store.

The original `M3SignalExactRangeIndex` now offers package-private exact range-to-bitmask
selection over its existing primitive coordinate lanes (never a second value table).
The original `M3SignalBooleanQuery` now fuses range selection into primitive membership
masks without per-predicate `int[]` materialization. Existing public `and`, `or` and
`Compiled.evaluate(version,SHA)` remain source compatible. An additive
`Compiled.evaluate(version,SHA,M3ProgressMonitor)` overload uses the existing owning M3
progress/cancellation SPI, checks cancellation during long predicate scans and bulk
bitmask operations, credits completed postfix instructions only, and does not publish
partial results or call `done()` on cancellation/failure.

Exact semantic scope: stored typed coordinates, not Java object equality or automatic
heuristic-to-fact elevation. A source-image SHA or quality descriptor alone never proves
semantic equivalence, dynamic call graph closure or soundness of a predicted bound.
Externally changed data, JNI/native changes, reflection and unknown dependencies require
upstream conservative epoch invalidation. Returning a result cannot maintain a snapshot
lock after method return. Monitor callbacks may be reentrant and are revalidated.

## Verification scope

- Local strict Java21 `javac --release 21 -proc:none -Xlint:all -Werror`
  passed against exact modified owning query/range source and source-compatible
  test dependency doubles (not full product reactor).
- Existing Boolean postfix regression: **61,543 assertions** in normal and -Xint.
- Focused new progress/fusion regression: **19 assertions** in normal and -Xint,
  covering cancellation before work, during a 131,085-row primitive scan, after an
  instruction, reentrant invalidation, completed task credits, source version and
  no result on failure.
- Exact source Git blob + SHA-256 manifest readback, actual Synexia OpenRewrite
  SDK first-apply/fixed-point/drift-refusal, owning M3JDK21 Maven/JUnit CI,
  installed JDK/jmods, jtreg/JCK, JNI/HotSpot/GC/OS/CPU/heap benchmarks remain
  independent admission gates; unrun/queued means PENDING and PR remains draft.
