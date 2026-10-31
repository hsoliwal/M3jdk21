<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 collection file census and first non-overlapping algorithm tranche

- Pinned preimage M3LongArena28 Git blob: `b265551ff952606326e25c0361bc6f35c72adedf` (master).
- Census: 298 top-level production Java files, including package-info.
- No Java API, constructor, class, package, native symbol, naming contract or third-party dependency changed.
- Selected existing owner: `M3LongArena28.appendAll`.
- Reuse already existing `M3LongLane28.copyFrom(long[],int,int,int)` to copy at block granularity, preserve zero-page laziness and avoid a repeated per-element indexed write.
- `append` publishes logical size only after its primitive lane write. Bulk append publishes after the checked lane copy. Allocation failure is not claimed atomic for physical storage.
- Category: primitive array concatenation/bulk copy, indexed segmented storage (compare LeetCode 1929, concept only; no challenge solution code borrowed).
- Existing concurrent draft owners excluded: #2123, #2127, #2129, #2134, #2135, #2136.
- `OWNERS.tsv` is an inventory of Git blobs and *provisional* family classifications. Rows marked `PENDING_FILE_REVIEW` are explicitly unreviewed.
- New JUnit 5 tests cover exact 4096-element boundaries, sparse zero pages, source isolation, 5000 deterministic randomized operations against an independent list, and invalid bounds.
- Unexecuted at authoring: real Maven/JUnit module build, JNI, OpenRewrite SDK receiver, JDK image/jtreg, JMH comparative benchmarks, cross-platform gates. Do not merge until those pass.
- Renames remain deferred pending reflection, serialization, public descriptor, service-loading and binary-compatibility inventory.

Receiver target: `hsoliwal/M3jdk21`. Canonical recipe owner remains private Synexia; a source-sealed recipe must be qualified before merge.
