<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Existing wrapper family: current-master reconciliation

This child qualifies the existing purpose-wrapper receiving family against M3JDK21 master `970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`. Parent Synexia PR #11949 supplies the source family; M3JDK21 PR #992 is a source donor with a divergent target tree. The recipe receives eight missing generic owners, restores the exact existing three primitive snapshot factories, and improves only the demonstrated count/cursor/raw-query atoms. It also reconciles the current collection inventory without removing historical evidence.

## Reuse and authority

`M3CollectionWrapperReconciliationRecipe` is a fixed resource binding over the unchanged existing `M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`, not a new transformation engine. Java postimages use real Java 21 LST parsing; build/native/custody text uses the existing text owner. The bounded Maven POM compiles those providers directly from this canonical module. It uses the already established OpenRewrite 8.17.1 and JUnit 5.10.2 proof dependencies; it does not change the source-derived product reactor.

Synexia owns recipe source, templates, tests, catalogue choices and custody. M3JDK21 owns installed product source and native/runtime qualification. A target may retain an exact source-pinned qualification mirror, with no Synexia runtime dependency. String remains the active ordered M3 phase; collection proving-ground work does not advance it.

## Qualify and apply

With JDK 21 and Maven, from this directory:

```sh
mvn -B -ntp verify
mvn -B -ntp test-compile exec:java \
  -Dexec.mainClass=com.synexia.rewrite.M3CollectionWrapperReconciliationApply \
  '-Dexec.args=/absolute/M3jdk21 /absolute/outside-target/wrapper-sdk.tsv'
mvn -B -ntp test-compile exec:java \
  -Dexec.mainClass=com.synexia.rewrite.M3CollectionWrapperReconciliationApply \
  '-Dexec.args=--catalogue flatmap'
```

The application adapter accepts an exact preimage or an already-installed exact postimage for each path. It rejects occupied additions, drift, missing required preimages, duplicate targets, wrong Java representations and unsafe path aliases. It verifies the complete combined output set and an unchanged second pass before serially writing SDK `Result` bytes. Its receipt must be outside the target and explicitly marks `SDK_EXECUTION`; it is not a transaction guarantee against arbitrary concurrent filesystem writes or I/O failures.

The main packet resources are under `src/main/resources/com/synexia/rewrite/hash-pinned-{java,text}/m3-collection-wrapper-reconciliation-20261017` in the canonical module. Four-column manifests bind every target path, preimage, postimage and template. Tests include exact generation, typed Java, unrelated source preservation, order/worker independence, refusal before disk mutation, partial replay, path-alias refusal and literal catalogue queries. A separately sealed `m3-collection-wrapper-custody-20261017` packet may install the reviewed source mirror after the source revision exists; it uses the same two engines.

After applying, run the actual target's wrapper Java/JNI proof and existing collection acceptance gate. Generated JNI headers, strict C17, fatal UBSan and checked JNI qualify both received verification peers and the retained native mask selector. Whole-JDK installed-image/jtreg/source-derived reactor qualification remains a distinct gate. See `QUALIFICATION.md` and its receipts for actual executed results and limits.

## Catalogue-driven refinement

`catalogue.tsv.txt` preserves the existing category schema and dated historical decisions, then appends each selected/rejected successor candidate with correctness, compatibility, complexity and proof rationale. `source-review.json.txt` records source commits/paths/blobs, original notices, negative controls and allocation observations. `FILE_PASSES.tsv.txt` separates the nine reviewed runtime dispositions from the remaining byte-preserved module sources. Further passes must reuse and extend these owners rather than infer coverage from merged ancestry or create another catalogue subsystem.

GitHub/Synexia source is selected under its actual license and contract. LeetCode, HackerRank and GeeksforGeeks are problem-category and adversarial-case references; no solution bodies are copied. No algorithm is selected solely for asymptotic cost: live aliases, comparator and stored-key identity, stable duplicates, callback/exception order, mutation guards and synchronization roots remain decisive. Decompose only semantically useful atoms; retain public contracts and fix the recipe before replaying a correction.
