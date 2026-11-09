<!-- SPDX-License-Identifier: Apache-2.0 -->
# Integrated M3JDK21 four-library interop convergence

This is the one integrated M3JDK21 Java21/JNI SDK. No M3+JDK wrapper, second model, or new native ABI is introduced. This receiving review is stacked on M3JDK21 #1795, which already unifies parent-owned dynamic arrays/bit masks, rank and filtered/sorted views, and the CPU/JNI viewport page+frame.

## Already qualified first-party donor

The existing upstream receiving PR #1766 has source-generated, source-pinned Apache/Spring/interop fixes and historical 36 -> 76 -> 76 -> 76 -> 76 census receipts. **Reuse its canonical Synexia Maven/OpenRewrite recipe**: https://github.com/hsoliwal/com.synexia/pull/12862 and exact recipe commit b0924dd680e62e86915aaa1fce93e48d0568c2c6. No hand rewrite of product source or duplicate recipe engine. The original donor's four-library interop admission workflow succeeded at commit 57cd7e6764cf3615684c3e9fb353570435057cb2: https://github.com/hsoliwal/M3jdk21/actions/runs/39625261146 . Six canonical recipe JUnit tests and 159 original interop Maven JUnit tests passed there with zero failures/errors/skips. This **does not automatically qualify a later merged head**.

## Serial/contract-preserving merge

Exactly 21 donor paths were reviewed individually against the original common ancestor b168a2b6891a87fd32aba07f48fbefed92ee0bde and current receiving head 452fd9fbd6845f6fa139eaeaf92980e157b78f60. Every existing receiving path still equals the common ancestor Git blob; six donor new paths are absent; hence no parallel edits collide. The receiving tree uses **the exact canonical product donor Git blobs** from #1766, and a two-parent Git merge preserves both histories. The accompanying SOURCE_MANIFEST.tsv records per-file Git blob preimages, donor blobs and SHA-256 successor digests.

- Apache Bag retainAll cardinality respects Apache Commons Collections 4.5.0 donor/owner behavior. Library owner remains the existing M3ApacheBag dynamic arrays, offsets and counts, not a copied foreign Bag implementation.
- Spring 6.2 MultiValueMap fixed-size list contract tests retain expected exception class and mutation behavior; do not replace existing M3SpringArrayMultiValueMap owner.
- Existing indexed/sorted parent-owned collection, native viewport page/frame, JNI mask symbols, model-to-render boundary, test/recipe source seals, and 76-path current receiver contract stay intact. Language/theme/GUI/GPU are separate consumers, not alternate model owners.

## Hold conditions

This merge is a **reviewable draft** until its exact-head four-library admission reproduces donor success, its current source census and recipe receipt pass and it is checked against existing #1795 Java/JNI and filtered/sorted view workflows. Full M3JDK21 HotSpot/JDK image, Android/Windows/macOS JNI, SWT/Nebula/GEF/RWT pixels, accessibility, localization and performance are not established here. Do not force push, squash away donor histories, bypass missing JUnit dependencies or assert overall speedups without benchmark proof. If a source contract drifts, improve and replay the existing canonical Synexia recipe.
