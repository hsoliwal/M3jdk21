<!-- SPDX-License-Identifier: Apache-2.0 -->
# Canonical Synexia source-sealed receiver: indexed object selection

Canonical [Synexia PR #12244](https://github.com/hsoliwal/com.synexia/pull/12244) source commit `788e1756e751ef13596ca33da52201b8255f8376`.
Pinned target base `07ba6e383343d32c83cfdaf641387bbe72bf325d`; exact 3 output SHAs and one current preimage in `recipe/manifest.tsv`.

Product commit 1 contains canonical template and algorithm-catalogue mirror, plus strict-Java21/JNI workflow. Product commit 2 *receives* exactly the postimage of the existing `M3IndexedListSnapshot.java` and two new contract tests by byte-identical template. No downstream hand edits or alternative source transformer.

The M3 implementation maps existing JNI `M3IndexMaskCompactor.selectPositions` logical ordinals to the current immutable `positions[from + i]` physical payload IDs in-place, avoiding one O(n) input index-window copy. No JNI symbol or ABI changes; existing older-native compatibility remains under M3IndexMaskCompactor ownership. A physical BitSet is computed lazily by the established membership() owner. Both Java and `NATIVE_REQUIRED` versions keep original failure/callback behavior.

Only Java21 hosted differential, current real C17 JNI parity, Maven JUnit, canonical Maven/OpenRewrite, source-fixed point, and installed-JDK/performance gates can qualify promotion. No numerical speedup or five-library SDK completion implied.
