<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
# TQ source-image recovery

This receiver continues the existing `MIndexRegexTrigramQuery -> jdk.internal.mindex.M3TQ` mapping, record `synexia.counterpart.MIndexRegexTrigramQuery` in `m3/docs/name-mapping.json`. That file remains the mapping authority. This directory contains task lineage and verification receipts, not another mapping registry or recipe owner.

## Historical gap and selective recovery

M3JDK21 PR726 (`ae174c918e8b114cf49e5672971ff30fb5d5b7a8`) added bounded image/restore, but merged into `codex/m3jdk-langdex-contract-20261009`, not the inspected master baseline `d5abed865bd47c4dd5ad78e95704348fe2095ed2`. Recover only its source-image atom into the current owner. Preserve current factories, callback order/repeated visits/short circuits/exception identity, `termCount`, range/backing facts, seam concatenation and adaptive sparse/dense `containsAll`.

The historical evaluator's additional work/depth refusals are not imported. They could reject an existing legal query before its first short-circuiting callback. Image bounds describe storage and restoration, not bounded or stack-safe evaluation of the old callback API.

## Added internal contract

`ImageLimits`, immutable five-lane `Image`, `image()` and `fromImage()` provide bounded ordered-DAG snapshot/restore. Preserve shared-node identity and repeated edges; validate offsets, operation codes, 48-bit UTF-16 keys, graph reachability and acyclicity. Restore without regex parsing, sorting or text payload scanning. Existing public JDK `String`, `Pattern`, `Matcher`, backing interfaces and production native symbols remain unchanged. No Synexia dependency enters `java.base`.

Reusable recipe, templates, Maven/JUnit fixtures, category decisions and complete host logs stay in `hsoliwal/com.synexia`, under `synexia-openrewrite-recipes/crates/m3tq-image-recovery-20261014/`, with class-backed `com.synexia.rewrite.M3TqImageRecoveryRecipe` extending the unchanged canonical source-sealed owner. The existing `m3/migration/recipe.py` supplies exact custody replay; it is not a substitute for Maven/OpenRewrite execution.

## Verification boundary

Focused host evidence: actual Java21 baseline/candidate compilation with warnings as errors; 66,893 old-contract checks with equal observation roots and byte-identical compiled probe; 2,534 image checks including 4,096 nodes; 8,202 independent JNI checks in normal/interpreter modes with `-Xcheck:jni` and nonrecovering UBSan; exact source conservation and retained public descriptors; five prewrite refusal cases; six source writes followed by zero-write replay; repeated fresh-JVM demonstration.

The native source is a **test-only** Kahn/reachability/pure-membership oracle, not a production accelerator. It does not memoize effectful Java callbacks or retain heap pointers. Native signature agreement is checked against `javac -h` output.

This remains **CANDIDATE / HOLD**. Actual Maven/OpenRewrite/JUnit, global mapping/provenance/intake/full-borrow reconciliation, full JDK/HotSpot/jtreg/GC/CDS/JIT, other platforms and production CPU/allocation/retained/native-memory evidence remain required. The standard jtreg/native tests are supplied, but a full jtreg run is not claimed. Preserve per-file licenses; the existing Apache-in-src intake review is not silently resolved. No full regex/v7 migration, speedup, default-branch mutation or automatic merge is claimed.
