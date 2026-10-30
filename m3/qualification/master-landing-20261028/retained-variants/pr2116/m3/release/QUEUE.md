<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 visible release queue

**Release: HOLD.** This index records the observed source `669a949d192345acefcfc5e9bb0b3724174d270c`
and tree `e7a32c2c567c8cb88908fde74532550f99f2006b`. It does not certify later commits.
The machine-readable [queue](queue.json) references the existing naming authority;
[release admission](README.md) explains evidence and producer verification.

New per-attempt output is written under `m3/release/queue/runs/` and uploaded
even when a job fails. Historical receipts and `target/m3-release-proof` output
stay preserved in place. Every generated `queue.tsv` row records source SHA/tree,
evidence, status and the next action.

## Release work

| Work item | Status | Next action |
| --- | --- | --- |
| `ATOMIZER_PATTERNIZER_CONVERGENCE` | PENDING | Extend canonical Synexia atomizer/patternizer fixtures with bounded permutations, code-looking strings/regex, compiler/JUnit oracles and permanent counterexamples. |
| `COLLECTION_DONOR_PERFORMANCE` | PENDING | Compare incumbent and licensed GitHub donor atoms by challenge category, then prove public contracts and measure cold/warm CPU, allocation, retained heap and native/process memory. |
| `CURRENT_NAME_CENSUS` | SCOPED_PASS | Retain the exact canonical map; perform typed, closed-scope recipe and ABI/JNI validation for any proposed rename. |
| `CUSTODY_CONTROL_REACTOR` | BLOCKED | Reconcile pure-int custody, migration-recipes, synexia-jdk-a3-recipes and recipe-agent-api module wiring into qualified Synexia execution, preserving product postimages and callers. |
| `CUSTODY_FOUNDATION` | BLOCKED | Seal the exact recipe files and callers in Synexia and replace the release job with target-only acceptance after qualified source execution. |
| `CUSTODY_PRECOMPUTE` | BLOCKED | Move canonical execution to Synexia, retain target Java/JNI parity gates and receive exact result evidence. |
| `HISTORY_BRANCH_CONTENT_RECONCILIATION` | PENDING | Run the existing canonical Synexia history/no-loss recipe against pinned relevant branch heads, reconcile current atoms and test each resolution. |
| `INSTALLED_JDK_JNI_PLATFORM_MATRIX` | PENDING | Build exact matched Java/HotSpot/JNI images and run required jtreg, ABI, Unicode, GC/JMM and supported-platform gates with per-platform receipts. |
| `METHOD_AND_JNI_RENAME_CLOSURE` | PENDING | For each proposed internal method rename, bind the stable capability ID and inventory descriptors, overrides, callers, reflection, serialization, module and JNI consumers before typed Synexia replay. |
| `RELEASE_PRODUCER_QUALIFICATION` | BLOCKED | Independently qualify each mandatory gate workflow, exact workflow source blob and actual job/step producers; retain fresh official API readback. |

## Existing naming queue

The exact-name receiver and canonical-map census passed at the observed source:
67 records, 55 concrete target rows, 51 distinct paths and zero attention findings.
Seven absent target paths belong to pending/blocked mappings. Four naming ideas
remain queued; no name, public method descriptor or JNI symbol was changed.

| Capability ID | Operation | Existing type → proposed type | Status |
| --- | --- | --- | --- |
| `synexia.counterpart.MIndexClass` | JDK_BACKEND | `jdk.internal.mindex.M3Class` → `java.lang.Class` | QUEUED |
| `synexia.collections.SegmentedAddress28` | FORMAT_ONLY | `jdk.internal.mindex.M3Address28` → `jdk.internal.mindex.M3Address28` | QUEUED |
| `m3.prefix-z` | INTERNAL_MOVE | `com.m3.algorithm.M3PrefixZ` → `com.m3.algorithms.M3PrefixZ` | QUEUED |
| `m3.collections.evicting-queue.owner-naming` | INTERNAL_MOVE | `com.m3.collections.guava.M3EvictingQueue` → `com.m3.collections.queue.M3EvictingQueue` | QUEUED |

The full refusal reasons stay in [name-plan.json](../tooling/name-contract/name-plan.json).
Public JDK names remain `java.lang.String`, `java.lang.Class`, `java.util.*`,
`java.util.concurrent.*` and `java.util.regex.*`. Internal moves require typed
recipe and caller/reflection/serialization/JNI closure; a census pass is insufficient.

## Mandatory release evidence

Each gate below lacks a qualified receipt for this candidate in this packet.
`NOT_RUN` here describes this packet, not a claim that the project has never run it.

| Gate | Status |
| --- | --- |
| `RIGHTS_AND_LICENSE_BOUNDARY` | NOT_RUN |
| `SOURCE_TREE_AND_UPSTREAM_BASELINE` | NOT_RUN |
| `OPENJDK_SANITY_MATRIX` | NOT_RUN |
| `M3_FOUNDATION_TESTS` | NOT_RUN |
| `PRECOMPUTE_JAVA_DIFFERENTIAL` | NOT_RUN |
| `PRECOMPUTE_JNI_NATIVE_PARITY` | NOT_RUN |
| `M3_MAVEN_CONTROL_REACTOR` | NOT_RUN |
| `STRING_UTF16_CONTRACT` | NOT_RUN |
| `RELEASE_CLAIMS_VALIDATION` | NOT_RUN |
| `PROOF_PACK_SHA256` | NOT_RUN |

## History observation

[#2071](https://github.com/hsoliwal/M3jdk21/pull/2071),
[#2072](https://github.com/hsoliwal/M3jdk21/pull/2072) and
[#2073](https://github.com/hsoliwal/M3jdk21/pull/2073) are merged.
[#1927](https://github.com/hsoliwal/M3jdk21/pull/1927) merge
`a9d25c6f69e36aae210a9df0d0b71821e9ca8617` is an ancestor of the observed master.
Its current `M3TreeWindowPlan` source validates before clearing committed indexes
and includes later defenses. A stale missing-master report is not a reason to
overwrite it with an old whole-file image. Relevant branch/atom reconciliation
and current runtime tests remain queued.

## Candidate versus qualification

This repair corrects release admission, documentation and report routing. It
does not claim completed JNI/HotSpot integration, exhaustive regex/compiler
convergence, collection performance superiority, full jtreg or all-platform
release support. Each such claim requires its own exact-source target evidence.
