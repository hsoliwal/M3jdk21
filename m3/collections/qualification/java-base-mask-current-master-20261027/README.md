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
