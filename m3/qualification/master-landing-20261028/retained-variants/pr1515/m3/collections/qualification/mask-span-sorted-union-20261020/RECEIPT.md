<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 owner-native sorted traversal and mask-span composition

Canonical source: hsoliwal/com.synexia PR #12571, commit
`a7a7eef155d5eb2fba1affac4c24f6ecdf0e140b`.
Source and receiving recipe packet tree:
`69d682fd29c1f5ae58c6b3612500cb764751f88c` (27 files).
Product packet: `m3/collections/recipes/mask-window-core-20261018/union-20261020`.

Receiving parent: PR #1549, `0122f7cf73b3b2a10429806c3e4beadc4caf0792`,
including the Windows proof checkpoint. No parent, donor branch or default
branch is rewritten. M3JDK21 owns these implementations; Synexia is the
canonical recipe/convergence workspace, not a runtime layer.

## Exact generated source

- Existing native compactor: `3aaa27ad34fded18f92e85f2e6377cf439b2bef9`.
- Actual java.base SortedSet: exact #1538 donor `ad3ee89d40de8c0ca344863bb6fc4469cec97a09`.
- Added common header: exact #1547 donor `a1bd5aab67eaed2db6fdc3851d85312f24bfc63d`.
- New Java split proof: `7a163ef09089b0c2b30e9e6ec1360794263f5e5c`.
- New native span proof: `dd352556d486a2094acc8d2eed255f4826b335e1`.

The unchanged SealReplay and M3GpuToolJoinManifest emitted these five files
from pinned preimages. Four existing Java/proof inputs remain KEEP. The
header prerequisite is first; serial intermediate native/java.base compile
checks and final composition fixed points executed. This is not a claim of
attributed OpenRewrite SDK execution.

## Preservation

Parent M3FilteredLists `a9b2f396ae9e952a5cf54cd99e5689b6a23b9517`,
M3FilteredViews `4a09a43ee8e963155144f88fa0a44255c373088f`,
M3LongIndexProjection `4191858c5aa6e0c1961376e2724fccef0a1d6bcb`,
M3LongAddressKernel `9ac6c5a1e121affd28e6503d922927f6c91c3a66`,
and Java mask owner `daea60beeb202d9e996f9a018fe5974738a43523` remain unchanged.
No new canonical payload owner, per-row Entry/Node, public method or native
export. Dynamic primitive offsets and masks drive native compaction. The
SortedSet split boundary retains lazy comparator lookup and adds one boundary
spliterator per split, not one object per stored row.

## Fresh evidence and resource caveat

The exact 27-file public packet reproduced successfully from frozen inputs.
236639 Java,320006 checked-JNI,361273 patched-java.base checks per applicable
run;9712 native span cases/67784 checks;16777730 common-header checks.
Five public descriptors, generated selector header and nine exports match.
32 partial source states converge without fixed-point writes;7 prewrite
refusals preserve bytes;2 manifest-kernel cancellation checks;21 missing
library checks. Baseline comparator counterexample reproduced.

Native span scratch increases by256 stack bytes. Singleton payload transfer
256->1; sparse1024-row fixture1024->683, state scans unchanged. Final prepared
262144-row two-fork CPU results improve singleton/sparse/all but regress half
selection (baseline/candidate ratio0.856). Representative performance remains
HOLD. Initial dense regression and fixture warning were corrected in templates
and pristine source replayed; failed candidates/raw samples remain in delivery.

Maven failed127 (not installed). Maven/OpenRewrite SDK, full owning module/JUnit,
whole live-wrapper suite, source-built installed M3JDK21/jtreg/JCK and
cross-platform union remain NOT_QUALIFIED. Actual SortedSet executed via stock
JDK21 --patch-module, not a complete product image. Prior Windows evidence
belongs to the preserved parent and is not relabelled as this union's proof.

Run `python3 <packet>/verify.py --out /absolute/new-exclusive-proof-directory`.
Keep draft/HOLD_FULL_GATES; no automatic promotion or merge. Other #1538/#1547
changes and broader String/AST/compiler/marketplace programmes remain separate.
