<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File move candidate index: algorithm catalogue and admission

Status: Java 21 candidate implementation, not Eclipse IResourceDelta move proof or a modified JDK bootstrap.

## M3JDK21 integration

`M3FileDeltaLedger.scan` remains the only file content observation and slot authority. Pass its returned `Snapshot` (including from `M3FileWatcher.awaitChange`) to `M3FileMoveCandidates.index(snapshot)`. The index reads existing kinds, sizes, SHA-256 and change-state masks. The physical-slot pairs are advisory only. **Keep CREATED and REMOVED change masks intact**, including when exactly one source and one destination share content; an unrelated delete+copy can appear identical.

Storage: one primitive hash-bucket head array, primitive next-offset chains, 64-bit fingerprint hints, compact byte classification mask and physical-index result arrays. No retained entry objects, parallel filesystem, duplicate payload store, scheduler or native callback. `digestAt` returns short transient digest copies to protect existing snapshot ownership. JNI is deliberately excluded for small per-file control-plane joins; only a measured batched native kernel warrants crossing the boundary.

## Candidate comparison by problem category

| Category | Candidate | Complexity | Contract reason | Decision |
| --- | --- | --- | --- | --- |
| Matching/join | Cartesian removed x created | O(R*C) comparisons, O(1) index | Straight oracle but scales badly | differential oracle only |
| Matching/join | Sort SHA-256+size+kind, merge | O((R+C) log(R+C)) | Deterministic with sorted primitive index columns; worthwhile for huge mixed duplicates | retain as next candidate |
| Matching/join | Hash bucket / primitive chain / full digest equality | O(R+C+probes) expected; worst-case quadratic collision group | Keeps M3 physical slot identity, exact digest and ambiguous multiplicity | **selected** |
| Similarity matching | Git similarity-scored rename heuristic | Additional candidate scans and potentially quadratic comparisons | Appropriate for modified text renames but not exact-content move-only rule | not substituted |
| Resource delta projection | Eclipse MOVED_TO/MOVED_FROM | Event flags with provider/workspace lifecycle authority | Digest agreement is insufficient to set authoritative Eclipse move flags | not emitted |

Problems/catalogue references (algorithmic pattern, **not copied source**):
- LeetCode Two Sum (hash join, index pairing): https://leetcode.com/problems/two-sum/
- HackerRank Pairs (indexed pair lookup): https://www.hackerrank.com/challenges/pairs/problem
- GeeksforGeeks Two Sum (naive vs sort vs hash): https://www.geeksforgeeks.org/dsa/check-if-pair-with-given-sum-exists-in-array/
- Git diff rename/copy semantics: https://git-scm.com/docs/git-diff
- Eclipse platform IResourceDelta move flag contract: https://help.eclipse.org/latest/nftopic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/core/resources/IResourceDelta.html

No third-party algorithm implementation was copied. This first-party Java expression is Apache-2.0 under the M3JDK21 contribution boundary; the source references are contract/algorithm comparison evidence, not relicensing authority.

## Qualification

- Production: `m3/file/src/main/java/com/m3/file/M3FileMoveCandidates.java`
- Host + linked-image adversarial test: `M3FileMoveCandidatesContract`, added to the existing `verify.py` contract inventory
- Existing OpenRewrite sealed Java recipe: `M3HashPinnedJavaSnapshotRecipe`, crate `m3-file-move-candidates-20261021`, ABSENT-before two Java targets with exact SHA-256 postimages
- Recipe Maven check: `mvn -B -ntp -f m3/file/recipe/move-candidates-20261021/pom.xml test`
- Product module check: `python3 m3/file/verify.py --java-home /path/to/jdk21 --output /tmp/m3-file-proof`
- CI: `.github/workflows/m3-file-move-candidates-20261021.yml`

Acceptance still requires CI against the **exact PR head**, full JDK21 build if a core Java API is changed, and integration tests with real Eclipse workspace resource deltas. This new helper does not perform AST semantic equivalence checks or permit Maven/Gradle tasks to be skipped.
