<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 sizing and filtered/sorted views: gated receiving packet

Status: REVIEW_CANDIDATE; runtime materialization HOLD until actual canonical recipe execution.
This packet changes no m3/collections production source, java.base, HotSpot, JNI ABI, default
flags, installed-source admission ledger or receiving phase. It is a reviewable target handoff,
not a claim that the feature has been installed in the JDK.

Canonical authority: hsoliwal/com.synexia PR #12018, commit
f4a368c51858ddf18103e763c198d848cb7d7238.
Canonical documentation-first commit: 34b969e008de3b878965173c9e1c005534f5843b.
Receiver baseline: 970a34350ebdf6a3f1c5ff5acc8445d0f3515f55.
Recipe: com.synexia.m3.Jdk21SizeViews20261017.
Canonical task: synexia-openrewrite-recipes/recipes/m3jdk21-size-views-20261017.

## Exact proposed runtime scope

- M3LongFilteredView.size/isEmpty/forEachLong use the backing cursor directly, preserving callback
  order/count, null handling, exceptions and live backing. Existing M3LongSortedViews delegates to
  this owner and retains sorted navigation, range, mutability and unmodifiable-view contracts.
- M3IndexMaskCompactor selects every row for validated mask zero using count=length and detached
  clones. Its existing native implementation uses bounded int-region copies with no state scan.
  Native-required loading/failure behavior is unchanged, including empty arrays.
- Optional M3ObjectSize + JNI/JVMTI GetObjectSize diagnostic reports shallow bytes and an
  identity-deduplicated sum of explicitly supplied allocations. It does not walk the heap, discover
  backing arrays, calculate retained/native size, count Java references or evict anything.

Canonical Apache-2.0 templates retain attribution. No third-party editorial/submission code,
GPL implementation body, independent lifetime manager or replacement wrapper hierarchy is added.
The candidate.patch is mechanically rendered from those exact templates for review only; it is
NOT an alternative implementation/apply engine. Do not use git apply to bypass recipe gates.

## Receiving gates

The actual canonical Maven recipe fixture must execute before any production postimage is
materialized here. It reuses M3HashPinnedJavaSnapshotRecipe/M3HashPinnedTextSnapshotRecipe and
checks exact application, source drift/missing/wrong-kind/duplicate refusal, reverse-input replay
and fixed point. Its emitted target/receiver files must then pass the recorded strict Java21,
JNI-header, C17/UBSan, behavioral and native runtime checks, followed by source-hash readback.

With an authorized checkout of the pinned Synexia source, the existing task commands are:

    mvn -B -ntp -f synexia-openrewrite-recipes/recipes/m3jdk21-size-views-20261017/pom.xml verify

From that task directory:

    bash verify.sh target/receiver target/bounded

The output directory must be absent. These are instructions, not receipts of local Maven
execution. The task's upstream-SDK compatibility profile does not replace or qualify the
repository's patched source-only OpenRewrite SDK, full reactor or matched M3JDK image.

After successful recipe proof, recheck all live target preimages and admit only generated output
plus the successor installed-source evidence. Stop on drift; fix the canonical recipe, never its
output. Full module, matched JDK, jtreg/JCK, JNI platform/collector/JIT and broad resource gates
remain separate. String -> arrays -> collections -> AST/compiler receiving order is unchanged.

## Evidence boundary

The Java/C candidate templates were compiled and exercised locally, including identical baseline
oracles; LOCAL_VERIFICATION.md records actual scope and resource samples. Local Maven is absent.
At initial handoff, Synexia workflow run 39017869347/job 117353571119 was queued with zero executed
steps. A queued workflow is not a pass. The PR remains draft and runtime sources remain unchanged.
