<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Fresh selected-source wrapper verification on native PR #1642

The receiving subset is stacked on `hsoliwal/M3JDK21` PR #1642 at
`6d9bad615851f346e3062e3034ccd74cbe2776af`. Its seven existing wrapper owners,
dependencies and regressions retain the original #1603 source identities. Its
five existing native-lane source files include the published native compactor
and position-mask oracle. `preimage-custody.json` pins all 12 input files and the
two initially absent wrapper test paths. This subset is not a full checkout or
an installed JDK image.

The parent publisher supplied the verified #1642 stacking head and confirmed
that the selected wrapper preimages were unchanged. This verification checked
the local source bytes, lengths and Git blob identities against the exact
source records. The copied historical range-failure probe comes from Synexia
qualification and is compiled as an external fixture; it is not introduced as
a receiving product file.

## Actual application and execution

Fourteen commands passed their specified outcomes: the unchanged qualified
copier's check/apply/replay, Java and javac version observations, strict Java21
compilation, direct focused probe, one actual JUnit adapter, stable-read tape,
three selected range modes, the original comparator-origin failure probe and
public/protected API extraction. Eight exact output comparisons passed.

Check and replay preserved their complete observed product snapshots. Apply
changed only `M3FilteredViews.java` and added `M3FirstVisibleAtomContract.java`
and `M3FirstVisibleAtomTest.java`. All five native-lane sources and the six other
existing wrapper dependencies/regressions retained their bytes, modes, mtimes
and inodes. Because the copier writes its own receipt under `target/`, the
unchanged copier and its exact qualified inputs were reconstructed under
`evidence/build/qualified-copier`; the canonical D packet was not edited.

The actual receiving Java files were compiled with `--release 21`, UTF-8,
`-Xlint:all`, `-Werror`, no annotation processing, no implicit compilation and a
fresh class directory. The only external Java inputs were the existing sealed
range-failure probe and unchanged JUnit Platform launcher. Seven JUnit
artifacts were matched to the prior emitted qualification's exact hashes.

The direct probe and JUnit each executed 64 normal cases and 1,104 caller-owned
failure probes, reproducing the original selection tape. These are two fresh
invocations of the same corpus, not 2,336 distinct cases. JUnit discovered and
successfully executed exactly one test, with zero failures, skips or aborts.
Public/protected API output and the stable-read tape matched the sealed
baseline exactly. The 120 exception-control cases passed 240 checks.

The 100 late-range failures, three real-comparator failures and original
comparator-origin failure also matched their exact baseline output and expected
exit code 1. These are preserved known defects, not successful repairs or
unexpected failures of this equivalence gate. The intentionally broader catch
repair remains excluded.

The sealed three-stage qualification is reused as historical evidence: 44
commands, 38 comparisons and 76 verified files. It was not rerun here. No native
execution occurred in this wrapper gate; the native sources were guarded as
excluded owners. Fresh command records remain separate from the historical
counts.

## Semantic and build boundaries

Internal stack-frame inspection, including `StackWalker`, is outside the
claimed equivalence: removing private adapters changes internal call stacks.
Exhaustive allocation-failure/OOME equivalence and allocation timing are also
outside scope. No zero-allocation or measured speedup claim is made. The tests
and source custody establish the recorded callback/result/failure observations;
they do not prove universal equivalence for every JVM resource condition.

Full JDK, whole-module, jtreg and performance gates remain unperformed here.
Metadata, receiving admission, publication and promotion belong to the parent
workflow and were not performed by this verification task.

## Portable reproduction

Prepare a fresh selected-source directory containing exactly the 12 files in
`preimage-custody.json` from the pinned #1642 source state, with both new D tests
absent. Use the already qualified `m3-filtered-first-visible-runtime-20261021`
crate and the approved Java21/JUnit toolchain that reproduces its exact proof.
Then run:

```sh
python3 verification/verify_target_wrappers.py \
  --repository /path/to/fresh-selected-source-M3JDK21 \
  --crate /path/to/synexia-openrewrite-recipes/recipe-crates/m3-filtered-first-visible-runtime-20261021 \
  --preimage-custody /path/to/preimage-custody.json \
  --java-home /path/to/jdk-21 \
  --maven-cache /path/to/prepared-maven-cache \
  --output /path/to/fresh-evidence-directory
```

The output directory must be new and outside both the receiving source subset
and active crate. The driver deliberately refuses an already materialized
input subset; its check/apply/replay sequence includes the fixed-point test.
Actual historical argv and source paths are preserved in `evidence/commands.json`.

`input-custody.json` binds sources, engine/template/proof inputs, fixture closure,
launcher and JUnit artifacts; `toolchain-custody.json` identifies Java tools.
`product-before.json` and `product-after-apply.json` retain snapshots.
`generated-build-artifacts.json` identifies compiled classes, which are excluded
from the durable handoff along with the isolated runner. The portable driver
adapts the existing bounded target verification and D emitted-verification
workflow; it introduces no production engine or new behavioral oracle.
Original file-level source notices remain intact. This authored driver and
document use Apache-2.0.
