# Compose the existing filtered and sorted wrapper fixes

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

This successor is stacked on M3JDK21 PR #1050 at
`b2df8a4d342286421c7d1eeb91a73e5d69d39bdb`. It adds the exact staged-addAll atom
and unchanged regression source from PR #1051 at
`c5e5e90e2dcc2298a372ab39640120dd60a2edb7`. The two predecessors share #1036,
so replacing either whole file with the other would lose an existing fix.

The source-sealed composition keeps both stored-representative membership/deletion
and validated batch admission. Only the existing `FilteredCollection.addAll`
production method changes relative to #1050; public APIs and production JNI
symbols are unchanged. The complete source SHA-256 is
`c1ae229f00911e0881de9cae102f2c65f0f7f9f8eb4da1abd364a816dd84ee0d`.

The canonical recipe, fixtures and reusable proof remain in Synexia. The
`recipe/` directory is an exact authoring/proof custody mirror, sealed by
`MIRROR_RECEIPT.tsv` and the canonical revision in `handoff.json`.
It creates no Synexia runtime dependency or independent recipe authority.
The existing workflow runs historical recipes on their original inputs, then
the successor against its new input/output seals. Historical manifests are
unchanged, and no new workflow is registered.

Executed evidence is in `recipe/evidence/RESULTS.json`: 7 actual successor
OpenRewrite/JUnit methods and 2 historical predecessor methods passed;
both composition orders, 16 partial-state/input-order combinations, serial and
two-worker replay, source refusal and fixed points passed. The unchanged Java
oracles passed 86,866 representative, 34,281 batch and 964,437 wrapper assertions
per tiered/interpreter mode. Deep-filter checks and both unchanged native
consumers passed, including 4,506 + 156 JNI assertions under UBSan and
`-Xcheck:jni`. All 89 production collection-module Java files compiled.

The existing module Maven test gate remains blocked by its unavailable
`junit-jupiter:5.10.2-m3-source-1` dependency. Source-built M3JDK21,
jtreg/JCK, full provider/reactor, other platforms, performance and process-memory
qualification and the successor hosted workflow remain separate gates.
Keep this PR draft; no receiving phase or `java.util` backend is promoted.

For reproduction, use the canonical mirrored README and `verify.sh`.
It consumes actual SDK emissions, the pinned inherited receiver proof sources,
and the unchanged native consumers. The README records the inherited staged
admission boundary, temporary O(batch size) references, and nontransactional
backing-owner behavior.

## Receiving branch convergence

Before publication, PRs #1050 and #1051 had both merged into `aix/m3jdk-wrapper-contract-refinement-20261017` at `6fe0f291145db7a99209159cddb77979ad2343c5`. Its collection implementation and batch-contract test already equal the qualified postimages byte for byte. The receiver adds the composition proof/recipe custody and updates the existing workflow from its exact admitted preimage. Historical predecessor replay remains recorded separately.
