# M3JDK21 sorted-owner and JNI mask convergence

This receiver continues PR #1417 at `46b35b58e142099020822d7189b1f18c129729eb`. M3JDK21 owns the runtime; this work modifies `java.util.SortedSet` and `java.util.ReverseOrderSortedSetView` in java.base, rather than introducing a Synexia dependency or an external collection framework.

Canonical recipe and proof: [Synexia PR #12446](https://github.com/hsoliwal/com.synexia/pull/12446). Qualified source revision: `b8ee85a73166407e0d82a6bdf51cfaad7698f4ed`. Publication supplement: `aa1d9e0e58de313ab624ff16b92a1ef61852a049`. The task is `synexia-openrewrite-recipes/recipes/m3jdk21-rx7-view-convergence-20261019`.

## Three selected existing source files

`SortedSet.spliterator()` now preserves the owner's comparator on recursively split iterator batches. Comparator lookup remains lazy; iterator/size binding, encounter order, sizes, consumer failures and fail-fast behavior remain. A private helper reuses the existing JDK batches and array payloads. There is one additional metadata object per split, retaining the owner for lazy comparator access; this is not allocation-free and does not introduce per-element entry storage.

`ReverseOrderSortedSetView.spliterator()` delegates to `SortedSet.super.spliterator()` instead of an ORDERED-only spliterator. The formerly incorrect comparator/characteristic metadata is intentionally corrected; public descriptors are unchanged. This is contract restoration, not equivalence to the defect.

The existing `m3_index_mask_window.c` is the exact native donor from PR #1303 at `e70058e73e4f42036815a24c2c22dac9dd948074`, blob `bd300c90d54ede9b7f238efe8a18c918d2fcef9e`. It records admitted relative ordinals in 128 fixed stack bytes and copies only their enclosing row span. State masks, signed and duplicate row IDs, array/output offsets, stable order, bounded resume cursors, validation and both JNI symbols remain. No native heap allocation, persistent pin, Java callback or per-entry object is added.

Existing RX7 Java/JNI power code and the newer filtered/navigable implementation are retained byte-for-byte. Inheriting filtered views benefit from the corrected JDK default without copying an older wrapper over the null-navigation fix. Independent overriding spliterators, all mutable families and every fork are not claimed as qualified here. Receiving phase remains unchanged.

## Qualification and catalogue

The canonical Java recipe generated three changed source files; second application changed zero. Its predecessor suite passed 214 checks and its new task suite passed 133 refusal/fixed-point/parallel-trial checks. The actual class-backed Maven/OpenRewrite/JUnit gate is supplied but NOT_RUN. The executed lane is OFFLINE_CANONICAL_JAVA_REPLAY_AND_HOST_COMPONENTS.

The retained combined host proof records: 1,031,319 sorted-split assertions; 337,790 top-level filtered-owner assertions; 251,558 RX7-find oracle assertions in normal and interpreted runs; 270,606 real JNI mask assertions; three missing-library checks; and 632,868 assertions from the preceding RX7 Java/JNI power corpus. Counts are assertions, not separate test methods; repeated runs are not summed as unique coverage. Java21 strict compilation, generated JNI headers, -Xcheck:jni and fatal UBSan for the mask kernel were used. Host-patched components are not a complete source-built JDK or production libjava linkage proof.

`m3/collections/qualification/jdk-rx7-view-convergence-20261019/CATALOGUE.tsv` is an exact read-only canonical snapshot. It records GitHub donor identities, LeetCode/HackerRank/GeeksforGeeks problem categories and preconditions, not copied submissions/editorials. In particular, pair-multiplicity XOR cancellation cannot replace general NFA OR/union.

The deterministic JNI region fixture transfers 1 rather than 128 row values for its sparse/dense limit-one cases and 130 rather than 300 for its sparse multi-chunk case. State-byte reads are unchanged. Those counts are separate from the real JNI correctness run and are not a universal timing claim.

An RX7 early-finalization candidate passed the semantic oracle but remains HOLD_NOT_APPLIED after mixed/regressing no-match and longest-match microprobes. Its patch, exact hashes and raw data stay in the canonical task. Splitting's metadata allocation and owner-retention costs are recorded there too; no blanket speedup is asserted.

## Reproduction and evidence boundary

From a checkout of the exact receiver commit printed on this PR:

```sh
M3_JAVA_HOME=/absolute/path/to/java21 \
M3_EXPECTED_COMMIT=<full-receiver-commit-from-the-PR> \
bash m3/runtime-integration/check-m3rx7-view-convergence.sh
```

The publication runner retains the qualified compile/execution sequence and adds a commit-pinned byte-comparison guard over every admitted input; it also checks production SHA-256 seals and logs SHA-256 for all consumed files. Bash syntax and invalid-pin refusal passed. The newly guarded wrapper has NOT_RUN end-to-end in the publication environment. The earlier completed host log is retained unchanged, not relabeled as a fresh guarded or hosted run.

Full M3JDK21/SDK/reactor builds, actual jtreg and production linkage, every collection/fork, long-index full-language regex, GC/JIT/CDS and cross-platform pressure/retention qualification remain NOT_RUN/HOLD. See the exact canonical `host-receipt.json`, `packaged-host-proof.log` and target `PUBLICATION.json`; no historical gate is relaxed or parent branch rewritten.

Original JDK notices and per-file donor/proof licenses remain intact. Synexia is the source of reusable recipes, templates, catalogue decisions and proof tooling; this receiver mirror is not another canonical owner.
