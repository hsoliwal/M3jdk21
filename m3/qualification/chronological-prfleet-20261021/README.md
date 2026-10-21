<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 — frozen PR-source superset receipt

This folder belongs to **M3JDK21**, one integrated Java 21/JDK/HotSpot/JNI product. It is NOT an M3 wrapper over an ordinary JDK. `com.synexia` remains the repository-independent **recipe/catalogue** owner; no Synexia runtime dependency or new merge engine is introduced.

Product snapshot: original `master` at `7f657d6ca05760af7bee7d4d2d9e14ae91ddd942`; GitHub source snapshot contains **101 open product PRs**, each with a frozen original branch, base HEAD, source HEAD, creation date and draft bit. Synexia PR-stack census records 204 additional convergence PRs from the same double-read; the full source is held at `hsoliwal/com.synexia` source census Git blob `daebe45de3e8cc18292c271006dbb772dff68338`.

`M3JDK21_PR_HEADS.tsv` and `M3JDK21_MERGE_GATES.tsv` represent the exact product subset of that broader ledger. `M3JDK21_SOURCE_PINNED_OWNERS.tsv` captures the product's **original six Java/JNI source owners** by Git blob, category and current product-branch SHA. This is additive immutable metadata, not an instruction to overwrite them or to mark historical lineage-only PR mergers as delivered.

## Contract and implementation order

- The existing M3 parent-owned Object[] and primitive array lanes, offsets, state bit masks, filtered/sorted live wrappers, immutable snapshots, Java 21 `java.base` and native JNI library are authoritative. Replace only validated private behavioral atoms within their existing owners.
- First apply the existing Synexia `PrStackRecipeDag`, `GitHubPatchMboxGate`, `GitHubPatchHunkChronology`, `ExactFileHunkReplay`, `M3PhysicalHunkMergeRecipe`, `M3HunkResolver`, `MergeCodeLossGuard` and `MergeReadback`. Parent source chronology precedes independent PR creation order; every file/hunk has APPLY, ALREADY_PRESENT, BOTH_VERIFIED or HOLD.
- Every new LLM-assisted edit is authored **recipe first** under Maven/OpenRewrite with source-preimage/postimage SHA-256, explicit source-to-package name, JDK/JNI and public-API contract proof, unit tests and correction ledger. Keep catalogue-driven comparison of licensed GitHub donor source versus LeetCode, HackerRank and GeeksforGeeks problem-category patterns (which by themselves convey no reusable implementation license).
- Do not squash, rebase, force, close original source PRs or silently choose ours/theirs. Only qualified, source-sealed, compatibility-tested candidate content is eligible for ordinary guarded GitHub merges, followed by a full native-source tree readback.

## Reuse real product tests, not external wrappers

Run exact original source probes from a clean JDK21 checkout:

```sh
bash m3/collections/qualification/required-excluded-windows-20261021/verify.sh
bash m3/collections/qualification/long-sorted-masked-window-20261021/verify.sh
```

These exercise bounded dynamic-index masks and sorted snapshot contracts over original Java21 and C11/JNI with `-Xcheck:jni`. The receiving workflow executes both separately. Additional live filtered/sorted wrapper tests, full Maven/OpenRewrite SDK, Java HotSpot image, jtreg/JCK, Windows/Android ABI, source mode/byte loss and setup-inclusive benchmarks remain independent required gates.

**Status:** historical source identities and corresponding Git blobs READ_BACK; JUnit/native execution on this receiving commit **NOT_RUN**, merge eligibility **HOLD** for all 101 product PRs. The snapshot's original 101 PRs remain preserved; new live PRs appearing after capture require append-only successor receipts, not edits to this one. No product Java, HotSpot or JNI runtime source is modified by this metadata-only wave.
