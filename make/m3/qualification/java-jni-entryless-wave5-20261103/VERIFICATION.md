<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 entryless Java/JNI wave 5 — actual source merge and proof ledger

2026-11-03. **Git source and ancestry merge executed on a dedicated integration branch.** `master` NOT modified. Java21/Maven/JUnit/OpenRewrite/native JNI ABI runtime **NOT EXECUTED ON THIS MERGED HEAD / HOLD**.

## Exact donor integration

- Receiving source [#2351](https://github.com/hsoliwal/M3jdk21/pull/2351) original head `9c70580bf46610ebe07f059be48f481daf6bd53b`, frozen `master@ce005d25b29fb6246e013bffb462afd5ed070362`. Source contract [README.md](README.md) committed FIRST at `cb4f704679729bacdf4e4f7f26f0f8b13bc7b8e1`.
- Donor [#2342](https://github.com/hsoliwal/M3jdk21/pull/2342) exact head `bc032a71a737ac1e9410cca059477cbb5bb39d8e`, against frozen master: **14 changed paths: 10 added, 4 modified, 0 removed**. Complete receiver changed-file list of 114 paths was inventoried. Only collision: existing `M3IndexMaskCompactor.java`; other three modified original Java/C sources have receiving Git blobs exactly equal to master originals and unchanged old public declarations.
- Examined both actual PR patches for `M3IndexMaskCompactor.java`: receiving #2351's six empty-result paths and documentation adjustment are at lines 25,169,238,312,335,387,429; donor #2342's optional position-page Java and JNI code is an additive hunk at line 110. This is a verified disjoint-hunk merge opportunity, **not automatic semantic compatibility**.
- Native GitHub transport [#2353](https://github.com/hsoliwal/M3jdk21/pull/2353) merged using GitHub's real `merge` method after exact donor SHA + receiving SHA precondition, yielding **two-parent commit `6aaf21f9af0ff66b7388f8159e8037964c34ad73`**, parents `[cb4f704679729bacdf4e4f7f26f0f8b13bc7b8e1, bc032a71a737ac1e9410cca059477cbb5bb39d8e]`, Git tree `5d2ccd425836f7fce3f7b4a7705217aa51aae068`. Branch ref read back exactly.
- Full `compare(previous receiver, merge)` filename/status set matched **14/14** original `compare(master,donor)` source paths, **10 added, 4 modified, 0 missing, 0 extra, 0 renamed/deleted**. All original donor parent Git histories retained, no squash/rebase or forced default branch write.

## Exact compactor union proof

- Receiver `M3IndexMaskCompactor.java` blob `ca33aaaff268d55a7f05e03ec18ea5b8b0b8d251` contained **six** exact `if (selected.length == 0) return selected;` empty-result fast path checks. Merged blob `09714d6626afbc303bb21635e5206680f9b05b58` still contains **six** identical checks and the original receiver's at-most-one-count-pass documentation.
- Donor blob `3a2c1faba46264fb302cefa4c81e8be5b35e90ef` adds exact `selectPositionsPage(byte[] states,...)`, `selectPositionsPageJava`, `NativePositionPage` guarded ABI probe, `positionsPageAbiNative` and `selectPositionsPageNative` declarations. **All five** source features are present in the final merged compactor.
- Original receiver and merged compactor have respectively **14 and 15 public/protected declaration lines**. All previous 14 are still present, in order; the only new public declaration is the opt-in `selectPositionsPage(...)`. This **is a public API expansion** and must be separately qualified before product acceptance. Existing JNI C exports were not removed; the new native page ABI is optional and must fail closed against older libraries for NATIVE_REQUIRED.

## Seven additional original-vs-merged blob witnesses (all byte-exact)

| Source | Git blob SHA on original parent and merged tree |
|---|---|
| Existing packed native C (`m3_packed_codec.c`) | `1813a024d4c2ce514cebe4928ab4379b7950ff94` |
| Existing borrowed token-cursor Java | `2e4df89ac667973e979e3568f497adbb6fe5a069` |
| Donor `M3FilteredViews.java` | `92f50be37cd05b0d4c908c4c5092f274a625ffc8` |
| Donor `M3TreeMap.java` | `c2f53c5a46c10ec67e596e51d1a4a84d3dd13cdd` |
| Donor `m3_index_mask_compactor.c` | `e2326af56fc3d56fe6279cafefd5e0a59062bcaa` |
| Donor `M3IndexMaskPositionPageTest.java` | `4a35031d3cb333b7dda394a26823feeb6e256ce6` |
| Donor qualification receipt `doops-entryless-receiver.tsv` | `8d91332829a9bb608772d3f25656b5e468bafdd7` |

All seven witness files were fetched from their exact original pinned Git commits and independently from the merged branch; Git blob and complete source content matched exactly. These are custody receipts, not measured runtime/JDK compatibility.

## Release blocks

Still required: canonical Maven/OpenRewrite SDK `Recipe.run`, original JDK21 compiler+Jupiter, sorted bulk/comparator and entrySet semantics, native C17 optional ABI probe against old and current libraries, `-Xcheck:jni`, precise first/length/state/output bounds, JNI release/caller arrays, concurrent-mutation tests, full module/reactor, serialization/source/binary public compatibility, platforms and performance. No test-pass claim is made. The original #2342 and #2351 PRs remain separately reviewable.

LeetCode, HackerRank and GeeksforGeeks provide search problem categories and independent test cases only, not copying rights. The source donor here is the existing first-party M3JDK21 Java/C/JNI implementation with preserved original license and source provenance. **Do not promote this draft to master until all blocking compiler/runtime gates pass.**
