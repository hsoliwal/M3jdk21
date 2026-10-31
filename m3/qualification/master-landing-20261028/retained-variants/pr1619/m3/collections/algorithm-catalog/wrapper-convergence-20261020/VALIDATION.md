<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0. -->
# Executed candidate evidence and receiving gates

This record describes executed, bounded checks of the selected postimages.
The canonical dataset and its SDK/receiving receipts establish transformation
custody separately. `JAVA_QUALIFICATION.json` records the final compilation's
270 production source hashes, generated class hashes, exact commands and
complete standalone output. `OWNING_SUITE.json` records the original owning
POM, source dependency builds, test reports and initial failures.

## Owning Java build

Both complete collection runs use the unchanged `m3/collections/pom.xml` and
its source-owned JUnit 5.10.2 family. The 10 dependency modules were built from
the pinned product sources. Compiler and Maven identities are retained in
the evidence; network trust was verified without disabling TLS verification.
Local proxy settings and trust-store files are environment configuration and
are not part of the publication.

| Owner | Suites | Tests | Failures | Errors | Skips |
| --- | ---: | ---: | ---: | ---: | ---: |
| Unchanged product baseline `2779aba3f98f31c324be43f731be7914e1a05935` | 140 | 475 | 3 | 0 | 2 |
| Reviewed isolated candidate with five selected source replacements | 144 | 483 | 0 | 0 | 2 |

The baseline failures are the existing merged sorted split, strict navigation
endpoint and strict hidden sorted endpoint tests. Their assertions are retained.
The two skips are retained existing native-mask split tests; they are not counted
as passes. The original `M3FilteredLists` source is byte-identical to the baseline
in the green candidate suite. Its rejected donor guard and ordinary mutation
JUnit bridge are excluded.

The source-owned launcher verifier initially fails because it resolves
repository-relative manifest paths against `m3/vendor` and expects a manifest
field that does not exist. The exact correction from existing PR 1566 passes
against the same **88 source/legal files, including 80 production files**, and
the same built JAR. No upstream JUnit implementation or legal source is edited.
The original source provenance checks pass for the other nine dependency owners.
Final target acceptance repeats all ten after receiving the verifier correction.

## Final isolated source and behavioural checks

The final isolated compilation uses Java 21 with `-Xlint:all,-module -Werror`.
It reconstructs all 270 actual production files, substitutes only the five
selected owners, and compiles the standalone contracts separately. This avoids
stale rejected classes and preserves the original source module descriptor.
`PUBLIC_SURFACE.json` records matching 130 callable/visible erased surface entries
for the five collection owners, plus the unchanged seven factories of the existing
core proposal. Generic semantics and callbacks remain separately reviewed and tested.

| Contract | Final result | Relevant baseline comparison |
| --- | --- | --- |
| Strict endpoints, reversal and sorted metadata | 1,843 checks pass | Baseline endpoint oracle fails with the wrong exception |
| Owner-dispatched COW bulk removal | 120 checks pass | Baseline iterator mutation throws `UnsupportedOperationException` |
| Stable index order and physical windows | 2,990,400 checks pass | Identical before/after observation fingerprint |
| Primitive filtered traversal | 40,803 total checks pass | Same 40,764 shallow checks and callback trace; baseline deep traversal overflows |
| Existing projection refinement probe | 282,479 checks pass | Actual collection owners; Java scope |

The stable-window observation fingerprint is
`e15c399b7be3d8c0fbd38bc6e2cd4c63b93bb0b95d1272bce9acdcc7bec5445b`.
The shallow predicate trace is `a28a3b365bae8591`. These finite comparisons
cover ordering, ties, masks, offset windows and hostile callbacks in the
retained fixtures; they do not prove every possible implementation behaviour.

## Core collection and JNI owners

The proposed `java.util.CollectionViews` body is mechanically reconciled with
the selected `M3CollectionViews` body. After package/class normalization, the
implementation bodies are byte-identical. All seven factories of the existing
PR 1551 proposal are preserved. The target baseline lacks this class; this is
the receiving of that proposed owner, not a claim that it was already installed.
There is no `com.m3` or `com.synexia` runtime dependency in the core owner.

The four core fixtures run in both mixed and interpreted modes in an installed
derived image: bootstrap origin (77 checks), core behavioural contract (14,694),
COW bulk removal (120), and stored representative/identity semantics (46).
They confirm bootstrap loading from `jrt:/java.base`. The reconciled identity
and COW fixtures fail against the older proposed core owner. The retained core
contract also observes zero allocation during its specified warm predicate
workload; this is not a general allocation-free collection claim.

`native-proof/README.md` and the existing
`m3/docs/m3jdk21-native-mask-window-receiver-20261020.md` describe the exact
native build and retained receipts. The driver compiles all 76 selected Linux
C translation units, uses generated JNI headers and links against the exact
recorded VM. It retains the existing two JNI exports and sparse 128-position
row-span kernel. The Java correction verifies both actual symbols before
claiming native availability, including zero-work `NATIVE_REQUIRED` calls.

Actual negative controls include the older false-positive availability result,
an absent native backend, a library missing one of the required exports, a
product libjava combined with the wrong String class, and a mismatched process
helper. The successful installed closure contains the pinned product String/M3
classes, source-built libjava and matching source-built `jspawnhelper`. The
recorded 14 native/core workloads pass in mixed and interpreted modes with
`-Xcheck:jni` and zero JNI warnings. The target process-launch probe also passes.

This is an installed **derived image using the recorded Temurin HotSpot and
remaining module binaries**. It is not a complete source-built HotSpot or
`make images` result. Final native evidence distinguishes a completed portable
run, later metadata and source-layout verification, and any infrastructure-interrupted run.

## Allocation and algorithm choice

`PROJECTION_MEASUREMENTS.json` preserves three alternating baseline/candidate
pairs. Each measured 40 known ascending-to-descending conversions of 262,144
long values with seven equal values per group, after 30 warm-up conversions, on the
recorded Java 21 runtime with a 256 MiB initial and 512 MiB maximum heap.

Allocated bytes were **83,888,952 before and 41,945,112 after** in every measured
pair. The chosen atom reverses equal-value run order while preserving encounter
order inside ties, producing one final primitive index array. It changes the
known opposite-order case from a stable merge sort to a linear pass. It does
not copy payload values or change the encounter-order stable merge fallback.

CPU and wall observations are retained with the workload. They are local
measurements, not a general sorting, JNI or Eclipse performance claim. A heap
sample after requesting GC is not proof of retained-heap usage or a leak bound.

## Rejected and unclosed behaviour

`diagnostics/wrapper-owner-boundaries` preserves exact historical sources,
independent assertions, source seals and a runnable replay. Its 30 recorded
operations include **13 explicit nonzero required-behaviour exits**. These are
retained evidence, not 30 acceptance passes. The list guard changes a valid
write into an unrequested read failure and still misses subList invalidation.
The original comparator-origin and read-only iterator-generation findings
remain pending an actual participating owner boundary. Their oracles are not
weakened to make a donor appear valid.

The first canonical SDK run rejected six standalone Java proof paths outside
its admitted source roots. The proof sources were relocated byte-for-byte into
standard `src/test/java` subtrees, their drivers updated and relevant executions
repeated. The four engines and the Java/text refusal rules remain unchanged.
The initial failed run and subsequent layout qualification remain distinct
canonical evidence; Java proof sources remain in the Java transformation lane.

The canonical Synexia dataset owns exact before/after templates, typed owner
and method expectations, real dependency/runtime seals, SDK Results, fixed
point and receiver replay. Parsing/type evidence and runtime semantic evidence
are deliberately identified separately. Target acceptance must match those
sealed output hashes and rerun the owning gates after serial receipt.

The actual Apache notices of the proposed core/native donors are preserved.
`EXISTING_DIRECT_COPY_REVIEW` remains explicit in `m3/docs/name-mapping.json`;
neither a green test nor a recipe changes donor rights. Full source-built VM,
other operating systems, broader GC/CDS/concurrency qualification and core
licence admission remain pending. The global receiving stage remains STRING.
The pinned Eclipse inventories remain retrieval inputs: no marked Eclipse UI
source file is represented as transformed or reviewed by this bounded pass.
