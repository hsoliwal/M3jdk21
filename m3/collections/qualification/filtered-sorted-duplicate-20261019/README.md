<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Java 21 filtered/sorted duplicate override repair (M3JDK21 receiver)

The product remains **one M3JDK21 SDK**, not an overlay. Canonical Synexia reusable Maven/OpenRewrite recipe descriptor: `com.synexia.m3.M3FilteredSortedDuplicateMethodRepair20261019`, handoff `hsoliwal/com.synexia@b07c0ce62c2a7dcc185ce43488b148dd91827465`. It reuses the existing `M3HashPinnedJavaSnapshotRecipe` class owner and its canonical parser, not a new rewrite engine.

## Source-sealed file pass

| Field | Recorded value |
| --- | --- |
| Owner | `m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java` |
| Before Git blob | `34fca25ba45467567213ed7a170bf4b27d6427e3` |
| Before SHA-256 | `eb3feb81c6534e617727cc2993ae01dea5675a40a18414b872dc7212edbf7f88` |
| Generated after Git blob | `4a44d2202a9ccbdaa5bdab39086074b3f4dbf49f` |
| Generated after SHA-256 | `2988e94a30948bc9188c18fbae5c790a3e260f7bb69588bae714f1e49f25158b` |
| Changed production file count | 1 |
| Source-code donor | Existing Synexia first-party Apache-2.0; no external donor source incorporated |

Three exact and order-preserving source removals eliminate:
1. The earlier, incompatible duplicate `SortedSetView.reversed()` override.
2. The earlier identical `SortedSetView.removeFirst()/removeLast()` declarations.
3. The earlier identical `NavigableSetView.reversed()` declaration.

Preserved: final live owner-bound `SortedSetView.reversed()`; `NavigableSetView.descendingSet()/reversed()`; strict `UnsupportedOperationException` for `addFirst/addLast/removeFirst/removeLast`; existing predicates and comparisons, including nested range views, all original class/interface names and public method signatures. No per-element data copy, new array or node owner, new native symbol, or runtime service.

## Verification

- LOCAL_PASS: exact Git source/postimage identity; a Java 21 minimized duplicate-method compilation witness fails before and compiles with `-Xlint:all -Werror` after removing the earlier definitions.
- ADDED_NOT_EXECUTED: canonical class-backed OpenRewrite JUnit replay, exact SHA-256, drift refusal, fixed point; SDK JUnit backed/reversed/range and mutation-refusal tests.
- NOT_EXECUTED: full target Maven collection reactor; all affected Oracle/contract fixtures; native/JNI/JTReg; source-built JDK21/HotSpot runtime and installed SDK; long-run concurrency, JMH CPU/heap/native measurement; full five-provider family contract closure.

The SDK tested output is a **source-candidate** until required gates run. The canonical String -> arrays -> collections promotion order is unchanged. Reuse original family-compatible JDK/Guava/Eclipse Collections/Apache Commons Collections/Spring names and contracts without introducing another storage hierarchy.

## Catalogue and next gates

`m3/collections/algorithm-catalog/filtered-sorted-duplicate-20261019.tsv` records selected/refused problem shapes, intended owners, bounds, and explicit pending qualification. Proceed serially file-by-file after earlier source preimages are sealed; do not interpret this correction as completion of all wrappers.
