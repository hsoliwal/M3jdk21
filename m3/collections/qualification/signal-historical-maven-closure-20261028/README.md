<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Historical signal Maven/JUnit owner closure — 2026-10-28

M3JDK21 is one integrated modified Java 21 SDK. Canonical source-changing
recipes and correction memory remain in Synexia. This product handoff contains
only **three existing historical CI workflows**, **three existing test-only
Maven qualification POMs**, and this passive proof receipt. No product
Java/JNI, public method, original JUnit selector or test implementation changes.

The current exact collections source has already passed the independent
[consolidated source+JUnit workflow 40540706129]
(https://github.com/hsoliwal/M3jdk21/actions/runs/40540706129):
29 strong source receipts; strict Java21 real-source compilation; all 13
standalone deterministic oracles normal and -Xint; actual first-party vendored
JUnit+arrays; 13 original JUnit classes with zero failure/error/skips.

The older optional TopK, domain and progressive-evidence jobs independently
failed because the original `com.m3.arrays` module was not installed before
running Maven and their **test-only** POMs did not declare a dependency on the
original `com.m3:m3-arrays:0.1.0-SNAPSHOT` module. In
`module-info.java`, `com.m3.collections` requires `com.m3.arrays` — this
is not an optional dependency. Tests must not be bypassed merely because a
historical optional build refers to a missing module. The approved change
uses the existing original vendored JUnit module and first-party arrays
Maven install, then executes the **same original test selectors** in both the
customized Maven owner reactor and alternate test-only JUnit POMs.

**Separate historical source-integrity failures are deliberately NOT hidden.**
TopK/domain/progressive source-hygiene jobs retain their dated ancestry and
source-SHA manifests and may continue refusing on a current merged feature
branch. Original Eclipse Collections files in their broad
`git diff --check origin/historical...HEAD` range contain unrelated
historical whitespace; this closure does not silently narrow that scope,
update old reference digests, or mislabel older source snapshots as current.
Any such convergence requires a separate source-sealed and licensed
preservation review.

Canonical Synexia `M3SignalHistoricalMavenClosureRecipe` pins exact
pre/post snapshots with a real Maven/OpenRewrite SDK first apply, no-op
fixed point, missing source and drift refusal tests. The next exact product
CI on this branch must execute and complete before this receiver is qualified.
Full integrated JDK/jmods/jtreg/JCK, JNI/HotSpot/JIT/GC/CDS,
Windows/Linux and CPU/heap/native benchmarks, and the complete family
producer and M3 String/Regex/M3FS/DOOP consumer PRD remain HOLD.

Keep both existing PRs DRAFT. Do not force-merge or delete other owners.
