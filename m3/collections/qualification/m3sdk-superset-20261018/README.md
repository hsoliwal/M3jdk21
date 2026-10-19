<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3SDK collection superset: source-qualified receiving

M3JDK21 is the product implementation. This changes existing collection owners; there is no M3-over-JDK runtime adapter, competing collection store or Synexia runtime dependency.

Canonical recipe and proof: hsoliwal/com.synexia PR #12390, immutable commit a21c36331235f874c3e11aaa3f466c67d4c10d56.
Canonical task: synexia-openrewrite-recipes/recipes/keyed-api-20261015/m3sdk-superset-20261018/.
Named recipe: com.synexia.m3sdk.CollectionSuperset20261018.
Receiver base: ff4b3e3b4142fdc8ceb18c577b8b1fff3559515a, the #1377 utility stack preserving #1365 reentrant traversal and #1354 sorting. Default branches and parent PRs are not modified or merged.

## Implemented composition

M3IndexedListSnapshot combines #1366 packed predicate decisions, #1362 stable indirect filter/sort and #1364 full-width physical mask selection. The naive union is unsafe: filtering may share an immutable all-selected int[] while fused sorting mutates it. The private sortSelection atom detaches shared indexes before sorting, but reuses exclusively owned partial-selection indexes. The stable merge atom returns its final private lane, avoiding redundant copy-back. Payload references remain in the existing Object[]; nulls, duplicate occurrences, slices, callback order and comparator exceptions remain covered.

The native compactor is the exact #1366 empty-result postimage. Existing M3IndexMaskCompactor.java, M3IndexPathMasks.java and m3_index_path_masks.c are exact KEEP dependencies. No native symbol added. Four snapshot methods are additive; existing public declarations are retained. Live filtered/sorted wrappers are not replaced by detached snapshots.

## Executed source-bound qualification

Real offline Maven/OpenRewrite: five recipe tests plus one receiver test, eight SDK Result postimages and zero-change replay. Required-input missing/drift/duplicate, occupied additions, four runtime PRE/POST combinations in both input orders and named YAML resolution are exercised. No generated target output was hand repaired.

Strict Java21/C17 compilation, generated JNI headers, fatal UBSan and checked JNI: 54 commands. Original 351161 Java and 11879132 JNI checks pass per baseline/refinement/final state with identical digests. New composition checks: 106937 Java, 106939 required-native and 106941 missing-native per candidate state. The compiled naive union fails the frozen immutable-source assertion. Two actual emitted-owner JUnit entrypoints pass without skips. Nine exports and both JNI headers are unchanged; 45 observed existing public/protected declarations retained, four snapshot declarations added.

Finite n32768 half-selection allocation: 200848 ->135296 bytes/call. All-selected allocation is unchanged and sampled fused-all time was worse. No universal speed claim. Full module/provider suites, full JDK image/jtreg/JCK, other native platforms, whole atomizer/patternizer/IOP execution and remaining family implementations remain NOT_RUN or PENDING. Keep DRAFT.

## Saved recipe, fixtures and complete programme

The canonical task contains executable Maven recipe/tests, source seals, original/donor/postimage sources, ownership counterexample, raw logs and full-family/PR ledgers. Its three-part fixture capsule is SHA256 045d47978a717661846ca90da344a8a24aa5dc484978a6a95d0f2dcc744805db. restore-fixtures.py validates all 30 files before creating absent fixture paths and never writes product owners. See the canonical README for ordered compile/test/SDK application/JNI commands and product-pom.xml for actual emitted-owner JUnit.

The catalogue retains sequences, associative/multivalue/persistent families, live/sorted/readonly/mutable wrappers, bits/ranges, heaps, tries/ropes, concurrency and keyed operators, including Swift/PCollections. The pinned 11031-path census includes source/test/build/docs, not 11031 implemented collections. Remaining #1367/#1357/#1313/#1319/#1347/#1353/#1363 and Swift obligations are not silently marked consolidated. Source tree coverage is explicitly partial where unresolved. Existing String-first promotion order remains unchanged.
