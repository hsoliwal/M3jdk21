# pageable-tree donor verify (m3scale plan rank 6, ALREADY-verify)

Verification-only leaf of the com.synexia -> M3jdk21 convergence programme (ledger
`docs/M3-SCALE/runs/20261106-m3jdk-convergence/plans/m3scale.tsv` rank 6): the synexia-m3scale-apache2 module
`m3-pageable-tree` (generic `K,V` object trees, string snapshot ids, offset cursors, expand/collapse sessions, visible
windows, walkers, root paths; 12 main files, 744 lines) is driven as a differential oracle against the primitive owner
`jdk.m3.file.M3PageableTree` already on master. **No production source changes; nothing ported.** The donor stays what
the plan says: a RETIRE-able generic predecessor of the DOOP-shaped owner.

| item | value |
|---|---|
| donor | `hsoliwal/synexia-m3scale-apache2` `3385d79a2968c770aedf1d3398b2a62f05ebc092` (master, 2026-11-06), `m3-pageable-tree/src/main/java/com/synexia/m3scale/pageabletree/*.java` |
| fixtures | `m3/file/src/test/java/jdk/m3/file/pageabletree/donor/` (test tree only): 12 files, byte-identical to the donor except the package line and a two-line provenance note; blobs in `PROVENANCE.tsv` |
| differential | `m3/file/src/test/java/jdk/m3/file/M3PageableTreeDonorDifferentialTest.java` (6 tests) |
| mapping | donor node id = owner coordinate; donor sibling ordinal = coordinate (both order siblings ascending); donor `mayHaveChildren` = owner `childCount(node) != 0`; donor expansion set = owner `long[] expandedWords` bitset |
| shapes | empty forest, single root, the donor's PageableTreeCoreTest sample (a=0 b=1 a1=2 a2=3 a3=4 b1=5 a1x=6), random forests of 2/5/13/40/257 nodes x 3 seeds (parent < child, 1/4 roots) |
| compared | root/child pages for every parent and limits 1,2,3,7,4096 (items, totals, hasNext, next positions; owner offset and cursor forms); stale cursors; visible windows for 3 random expansion sets (leaves included) x branch page sizes 1,3,4096 x 7 offsets x limits 1,2,5,4096 (rows node/depth/expanded, hasMore, scanned count); depth-first and breadth-first walks at page sizes 1,2,7 vs preorder ranks and level pages; root paths vs `path(treeRoot(node), node)` |
| API map | `NEC.tsv`: 22 donor members -> owner member, status COVERED / COVERED_BY_DOMAIN / COVERED_BY_BITSET / COVERED_SEQUENTIAL / COVERED_DERIVED / EVOLVED / PARTIAL / NO_OWNER_BY_DESIGN / OUT, evidence test |

## Result

All 6 differential tests pass on the first run: every compared sequence is identical (`diff-first` row in `GATES.tsv`:
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0). The owner covers the donor's observable behaviour on every shape;
the differences are API-shape only (NEC findings below).

## NEC findings (no port proposed; recorded for the owner)

1. Snapshot binding: the owner binds cursors to the content fingerprint (`fingerprint64()`), the donor to a caller-chosen
   snapshot id string. An equal-content tree accepts the owner's cursor; the donor refuses a different id for the same
   content. Verified both ways in `staleCursorsFailClosedInBothAndContentFingerprintIsTheOwnerBinding`. Not a defect:
   the owner tree is immutable, so content identity is the stronger binding.
2. Stale-cursor exception type: donor `StaleTreeCursorException extends IllegalStateException`, owner
   `IllegalArgumentException`. Both fail closed before any read.
3. Page budget: the owner bounds `limit` to `DEFAULT_MAX_PAGE_SIZE` (4096); the donor accepts any positive limit.
4. Random-access visible offset: `VisibleTreeWindow.read(session, visibleOffset, limit)` starts at any visible row; the
   owner pages visible rows sequentially from `firstVisibleCursor` (rows identical when paged from rank 0).
   `VisibleCursor` is a public record, so a caller holding a known preorder rank can construct one. Candidate follow-up
   on the owner, only if a consumer needs it: `visibleRankCursor(expandedWords, visibleOffset)`.
5. Remote sources: the donor's `OffsetPageableTreeAdapter` / `CursorPageableTreeAdapter` page remote branches with
   unknown totals and keyset tokens; the owner's `Source` needs exact counts. PARTIAL, no M3jdk21 consumer; file as an
   ABSORB leaf only when a remote tree source appears.
6. Session page cache (LRU over branch pages) has no owner by design: a frozen in-memory tree loads no pages.
7. Progress monitor: the donor passes a monitor to every page; the owner takes `M3ProgressMonitor` where work is
   unbounded (`freeze`, `Source.readRoots/readChildren`) and none on O(limit) page copies.

## Gate

`gate-file-pageable` (GATES.tsv): `mvn -B -ntp -T1 -f m3/pom.xml -pl file -am -DskipTests=false package`, JDK 21,
module `-Xlint:all,-module -Werror` for main and test sources (the donor fixtures compile warning-free). m3-file:
49 tests, 1 failure, 0 errors (reactor baseline 43/1/0 + the 6 new tests, 0 new failures); the one failure is the
baseline's `jdk.m3.file.M3FileDeltaTest.fullSourceClassAndCliContract` (`FULL_VENDORHOLD2_FAILING.tsv`, #2785).
m3-collections 2278/0/0/135 and m3-arrays 115/0/0/17 unchanged.

The `m3-file` module's `file-source-class-content-contract` exec (`verify.py`: "module contract inventory changed;
review each executable contract") is red on master before this leaf (reactor-wiring baseline
`FULL_VENDORHOLD2_FAILING.tsv`); it stays the same single failing row here. Surefire ran before it and is the gate.
