<!-- SPDX-License-Identifier: Apache-2.0 -->
# Direct M3JDK21 mask-window receiving receipt

Canonical recipe: hsoliwal/com.synexia PR #12549, commit
`f188e2af9117f0a0126b70aec3f87bb4489cbf42`.
Source subtree: `synexia-openrewrite-recipes/recipe-crates/m3-mask-window-core-20261018/current-20261020`.
Exact source/receiving 14-file packet tree: `34febf76a5471c984bb255daafbe9f5bb468f2c5`.
Product packet: `m3/collections/recipes/mask-window-core-20261018/current-20261020`.

Product parent is existing filtered/sorted owner PR #1515 at
`a7b83b8dff260cf415016fd7ed5617cc01f028cb`.
The parent is preserved, not rebased or merged by this task. This continuation
changes two original product owners and emits one synthetic proof. It does not
introduce an M3-over-JDK layer or runtime Synexia dependency.

Emitted blob identities, equal to the canonical source templates:

- M3IndexMaskCompactor.java: `daea60beeb202d9e996f9a018fe5974738a43523`
- m3_index_mask_compactor.c: `a94368898d0c5d9b0dbedb9dcd56d4d2e39a1414`
- M3MaskWindowCurrentProof.java: `636ceb1c9324c6cfd4fa9aa0077f8ffc2be8228d`

KEEP M3LongIndexProjection `4191858c5aa6e0c1961376e2724fccef0a1d6bcb`
and M3LongAddressKernel `9ac6c5a1e121affd28e6503d922927f6c91c3a66`.
Required/excluded APIs, all nine JNI exports, stable equal-value ordering,
shared payload/index windows and the parent's live wrappers remain intact.

Fresh local proof: 236639 Java and 320006 checked-JNI assertions per run,
all four original/candidate Java/C pairings, interpreted native run, 21 missing
library refusals, constructed legacy-ABI refusal fixture, four public descriptors,
one unchanged generated JNI header, nine exact exports, eight partial replay
states, seven pre-write refusals and two manifest-kernel cancellation checks.
The actual native source is built with strict C17 and nonrecovering UBSan.
The work-count probe reduces four payload reads to one for one selected chunk
and to zero for none, while retaining eight state reads. No throughput claim.

Maven returned127 (not installed); attributed OpenRewrite SDK, full owning module,
JUnit discovery, installed M3JDK21/jtreg/JCK, cross-platform native and measured
application performance remain HOLD. Parent historical proof is not fresh
acceptance of this commit. Existing source-sealed Java recipe replay, not SDK,
materialized the production changes. Review before promotion; never auto-merge.
