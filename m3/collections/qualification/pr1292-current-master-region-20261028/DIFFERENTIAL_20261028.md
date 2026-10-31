<!-- SPDX-License-Identifier: Apache-2.0 -->
# Chronological PR #1292: current-native 25,000-case deterministic C/JNI differential

This is the current-master test successor to the existing
`pr1292-current-master-region-20261028/RECEIPT.md` (#2073). The
native Java/JNI production source `m3_index_mask_window.c` is unchanged:
Git blob `bd300c90d54ede9b7f238efe8a18c918d2fcef9e`.
The already-admitted original instrumented C test
`m3_mask_window_current_master_contract.c` has preimage
`6711b9edeeba1ed1a4de3cfdc118ac939b92bd5d` and new postimage
`30faabd9efe6ba846fbb8d5b559371edff1e7eb1`.

**Recipe source:** Synexia [#13803](https://github.com/hsoliwal/com.synexia/pull/13803)
retains the pre/post SHA-256 manifest for the existing original OpenRewrite
`M3HashPinnedTextSnapshotRecipe`. It emits this exact test postimage.
No recipe source is copied into public M3JDK21.

## Local executed acceptance

- Local native source and its original test file Git blobs checked against
  actual GitHub master.
- Exact baseline test preserved as a **byte-exact ordered subsequence**
  (222 original lines; 295 generated; no removed/reordered original lines).
- Strict GCC C17, `-Wall -Wextra -Werror -O2` with stock JDK21 `jni.h`:
  25,000 randomized full/partial mask-window cases, fixed seed
  `0x4D335F4D41534B21`, output `M3 JNI RANDOM PASS cases=25000` and
  `M3 JNI CURRENT REGION PASS checks=1712636`.
- Repeated with `-fsanitize=undefined -fno-sanitize-recover=all`:
  **same 1,712,636 passing assertions**; zero sanitizer reports.
- Exact source script generated the postimage SHA-256
  `66b4725e6f03426440074e3a0d2b97c0150bdd23ffd1881a20b470a56c90a6d1`.
- The unchanged CI owner has its only source-identity pin advanced to this
  test successor and reruns both actual C17 configurations. A queued CI job
  is not a pass and full HotSpot `-Xcheck:jni` remains outside this proof.

The generated C target retains output guards, limits, cursor progress,
bounded examination, alias refusal, byte-mask semantics and max region
transfers; only the regression fixture and its CI pin are modified. The
historical [#1292](https://github.com/hsoliwal/M3jdk21/pull/1292)
source remains open because its older native kernel, long projection and
region fixture differ from the evolved current owner. The current JNI
production source is authoritative and **must not** be overwritten by
an old suffix copy kernel. No public API, native ABI, runtime dependency,
production source or protected branch force update is made.
