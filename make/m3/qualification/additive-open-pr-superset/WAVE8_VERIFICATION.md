<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 wave 8: additive source / JNI / Arrays custody (2026-11-03)

**REVIEW-ONLY DRAFT / EXACT SOURCE POSTIMAGES PASS / COMBINED JDK-JNI QUALIFICATION HOLD.**

## Immutable ancestry

- Existing source-superset [M3JDK21 PR #2359](https://github.com/hsoliwal/M3jdk21/pull/2359): exact source HEAD `04c655c2ea9ee05307264f13ed4481e266cbcbd4`. It already carries prior #2361, #2381 and #2385 result source as ancestors.
- Original [PR #2225](https://github.com/hsoliwal/M3jdk21/pull/2225), head `5e018617806d6194c9dfef3985b21053e73a20b9`, original source base `52fb81c483c74448a12eecb63aa9465e42cdb82c`.
- Review-only transport [PR #2386](https://github.com/hsoliwal/M3jdk21/pull/2386), exact two-parent non-squash merge `2f2d6f388f30d7846091e4035c095e3dbf4773b6`. Parents ordered: `04c655c2ea9ee05307264f13ed4481e266cbcbd4`, `5e018617806d6194c9dfef3985b21053e73a20b9`.
- Review branch `m3/additive-current-source-wave8-20261103` was created at `04c655c2ea9ee05307264f13ed4481e266cbcbd4` with preservation ref `preserve/m3jdk21-superset-2359-before-wave8-20261103`. No source head rebase, squash, force-push or default-branch update.

## Full 16-file source custody

PR #2225 modifies 12 source/workflow/metadata paths and adds four receipt paths, with zero deleted paths. The original before-image Git blob for **every one of the 12 modified files** was checked against the exact #2359 review tree; **12/12 identical**. The four receipt paths were absent before transport.

After the merge, all **16 result source Git blob SHA-1s** were fetched from both the original donor head and the review branch, and **16/16 matched exactly**. The donor's original (filename,status) diff equals the review transport's complete changed-file list, no missing or extra path, no removed file. The adjacent `WAVE8_SOURCE_CENSUS.tsv` records all 16 original Git blobs, donor source HEAD and original change statuses.

Source domains include `SharedLexiconImage`, `SharedRelatedLexemeCatalog`, `m3_wordrows_jni.c`, Java WordRows regression, exact `java.util.Arrays.copyOfRange` exception ordering, primitive array CopyMethods tests, and schema/naming plan source proofs. Original PR #2225 documents six canonical Synexia recipe suites / 30 JUnit SDK groups plus bounded Java21 / JNI tests. Those are prior donor-specific evidence and **not tests run on this new merge commit**.

## Historical alternatives held rather than silently overwritten

Original older Arrays donor [#2181](https://github.com/hsoliwal/M3jdk21/pull/2181) and this later #2225 diverged from an older common parent. Among seven reviewed overlapping output blobs, **six differ** and one naming-schema blob matches. No ancestry-only claim is made for #2181; preserve both source branches, qualify and combine method/configuration atoms only after true source and contract comparison through canonical OpenRewrite recipes in Synexia.

Other complex collection donors #2169, #2170, #2174, Java parser / Jackson #2233, and JSpecify module #2217 may overlap current owners or introduce additional source-generation ownership. They remain separately reviewable and cannot be byte-overwritten into the JDK product without per-file atom qualification.

## Mandatory HOLD gates

Complete source-built Java21/JDK image compilation and JUnit/jtreg, true JNI C compilation / exception and local reference tests, HotSpot/GC/class loading/binary ABI, current-HEAD source-sealed OpenRewrite fixed point and merge-refusal oracles, performance regression and all-hosted CI. This project still has hundreds of open owner PRs and no complete stable all-`hsoliwal/*` merge-frontier DAG; do not claim completion.

No merge to public `master`, no rebase of Synexia `develop`, no force push, no copied coding-challenge solution and no change to user-facing Java APIs outside the source-qualified donor result.
