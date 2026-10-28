<!-- SPDX-License-Identifier: Apache-2.0 -->
# Current-master M3JDK21 java.base deletion-mask receiver — 2026-10-27

This is an **in-JDK** source receive from historical [M3JDK21 #1873](https://github.com/hsoliwal/M3jdk21/pull/1873), not a Synexia library layer. The receiver was created directly from current `master` commit `d7851cb359fe9d0efd1ad1a214192d822423a16b`. No develop/master rebase, force-push, squash or destructive history update was used.

The product implementation, two contract tests, original narrow verifier, its documentation and source-gated workflow were each copied from #1873 with **matching Git blob identity**. The before source in current `master` is the same exact predecessor blob `51ea3a72bb191afc250a58be964363bd5680467c` that the already-qualified Synexia recipe expects. The received compactor blob is `e6ca59d8ae7fba700765e670dd5ec7d8dd84bc8d`. No other `java.base` file is rewritten.

The current `java.util.ArrayList` and `Vector` owners already call `jdk.internal.mindex.M3CollectionMaskCompactor.compact`. Their Git blobs are `885d891b0f817080824622d592691a27cf8ffa26` and `a79a77ce8700dc494d02d906b2a7765670629eee`. The successor verifier refuses any drift of these callers, confirms the import and actual call expression, then runs the existing source-sealed Java 21 mask oracle.

Run from repository root:

```sh
bash m3/collections/qualification/java-base-mask-current-master-20261027/verify.sh
```

Historical [CI run #39960645843](https://github.com/hsoliwal/M3jdk21/actions/runs/39960645843) passed 2,000,865 compaction checks under stock-JDK `--patch-module java.base` plus 143,122 reference-API checks under the stock runtime. This is **inherited provenance, not a passing result for this new branch**. The new branch must have its **own** successful GitHub Actions gate before promotion.

Synexia source recipe [#13207](https://github.com/hsoliwal/com.synexia/pull/13207) is merged. Its exact SHA-256 source seals, fixed-point execution and refusal semantics remain unchanged; no Synexia runtime dependency is added to `java.base`.

## Open qualification gates

A complete matched M3JDK21 source-built fastdebug/release image; jtreg on that image covering the actual `ArrayList` and `Vector` integration; GC/barrier correctness; JNI/VM compatibility when affected; concurrency; and reproducible allocation/CPU performance benchmarking. Do not claim a speedup or complete JDK compatibility on the basis of `--patch-module` and stock-reference tests.

## Full matched-image acceptance workflow (not yet executed)

The opt-in GitHub Actions workflow
`.github/workflows/m3-jdk21-java-base-mask-matched-image-20261027.yml`
has a `workflow_dispatch` trigger only, to avoid consuming a full OpenJDK
build on every documentation/test edit while many unrelated checks are queued.
It installs the Linux build and jtreg prerequisites, reruns the exact
source/caller seals, executes the repository's
`m3/build-openjdk.sh` to build an M3JDK21 fastdebug image, locates the actual
image and runs the two named jtreg tests through `make test`.

A workflow declaration is **not proof that this full image has built or passed**.
Do not promote the candidate until this workflow completes, its build and
jtreg logs are reviewed, and any platform-specific failures are resolved.
Its default two build jobs limit runner pressure; the full test suite and
throughput/GC benchmarks still require separate admission.


## Current-master receiver execution receipt

[GitHub Actions run #40512534449](https://github.com/hsoliwal/M3jdk21/actions/runs/40512534449)
**passed for head `59c52e835c3270e462262419e6860ba95b3e3fd3`**.
The source-gated Java 21 verifier checked the existing `ArrayList` and
`Vector` M3 compactor callsites, then reported:

```text
M3CollectionMaskCompactorContract checks=2000865
M3ArrayListVectorRemoveIfContract checks=143122
PASS strict Java21 kernel oracle and stock-runtime ArrayList/Vector reference contract
PASS current-master java.util callsite custody and selected java.base mask contracts
```

These are real passing results on a patched stock `java.base` for the
internal compactor and on the unmodified host's public collection behavior
as a reference oracle. **They do not mean a matched M3JDK21 product-image
jtreg run was performed**. The manually triggered full-image workflow is
a declared acceptance gate, not completed evidence.

## Matched-image CI diagnosis (2026-10-27)

Workflow run 40517667807 completed its focused stock-JDK mask checks, then
stopped at OpenJDK configure before building the product image. Missing
Xrandr development headers caused an X11 prerequisites error. The runner
also supplied jtreg 5.1, below this fork's declared 7.2 minimum.

Commit 35880b6c3ac10d34592bee3f880202652c2a5156 adds libxrandr-dev
and an independently versioned jtreg 7.3.1 installation and published
checksum check. Replacement run 40521048052 was queued at last inspection.

The existing host-JDK checks have passed; the full M3JDK21 source image,
actual product-image jtreg, GC and performance work **have not**.


## Bounded G1/SerialGC reference stress (2026-10-28)

Added `test/jdk/java/util/ArrayList/M3ArrayListVectorGcMutationContract.java`
with Git blob `46eef59604fc52dae7dea11de615dffc5e90d943`.
It exercises the same Java `ArrayList` and `Vector` mask-removal contracts
across the 511/512/513 dispatch boundary, 1024/2049/4096 element windows,
and no-op/full/alternating/range/sparse deletion patterns. Each case preserves
and checks object **reference identity**, stable survivor position, payload
integrity, invocation count, and iterator fail-fast behavior before and after
explicit GC. The test executes twice, under G1 and Serial GC.

The exact committed test was independently compiled with
`javac --release 21 -Xlint:all -Werror` and executed with
`-XX:+UseG1GC -Xmx128m` and `-XX:+UseSerialGC -Xmx128m` on
stock OpenJDK 21.0.12.1. Both runs passed **399,890 checks** each.
These are stock-JDK **reference** results, not yet M3JDK21 product-image
results.

The full M3JDK21 fastdebug GitHub Actions workflow has been expanded to run
this new jtreg test under the product image alongside the previously passing
two cases. New source-built [run #40620188480](https://github.com/hsoliwal/M3jdk21/actions/runs/40620188480)
was queued at the time of this receipt. No current-head product GC/jtreg
result is claimed until the run finishes. These checks do not constitute
exhaustive remembered-set/card-barrier testing, collector stress suites,
or allocation/performance evidence.


## Additional G1/Serial reference-liveness oracle (2026-10-28)

New `test/jdk/java/util/ArrayList/M3CollectionMaskGcBarrierContract.java`,
Git blob `5bb6fb3ee9e7a3e3ce73bdbf2439eb209e126302`, complements the existing
`M3ArrayListVectorGcMutationContract`. Both use public Java collection
semantics, not a replacement M3 runtime layer.

The new oracle exercises lengths 511/512/513/1024/3072, six deletion-mask
patterns, duplicate object references, null elements, stable identity/order,
predicate callback indices, both ArrayList and Vector, and heap churn/full GC
after compaction. It is wired to jtreg under **both G1 and Serial** with 96 MiB
heap constraints. Unlike the prior GC mutation test, it specifically tests
null/shared survivors and allocation pressure after compaction.

Locally on OpenJDK 21.0.12.1 this exact **Git-blob-matched** source compiled
with `javac --release 21 -Xlint:all -Werror` and reported
`112174` checks under G1 and `112174` under Serial. These are **stock-JDK
reference-oracle runs only**; neither is a matched M3JDK21 image receipt.
The new `make test` selection must pass on the current product source head
before this GC coverage can be promoted. Forced `System.gc()` and retained
survivor identity are not a complete collector write-barrier/card-table proof.
