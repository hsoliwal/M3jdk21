<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 array-only LFU API facade receiver, 2026-10-18

Canonical Maven/OpenRewrite recipe: https://github.com/hsoliwal/com.synexia/pull/12339 at \`2a9caefe0d4c68bc991d024bd1a68a0ed6238429\`. This receiver is stacked directly on [JDK21 package-source PR #1325](https://github.com/hsoliwal/M3jdk21/pull/1325), which preserves 59 original JDK classes at head \`81172a891fa55bfd6d8c1fe04c89eefe92f5136b\`. PR #1325's imported HashMap Node/TreeNode, linked and concurrent entry-node owners remain **SOURCE_ONLY**; their original GPL+Classpath header/licensing is unaltered, and the 78 compilation errors are not hidden or declared resolved.

## Authoritative M3 storage invariant
- **All resident mapping structure is arrays:** Object[] for Java-GC-traced keys/values (or primitive arrays for primitive domains); int[] for hash buckets, order links, tree parent/left/right, free-slot indexes and offsets; byte[]/BitSet/word lanes for occupancy, state, filters and cached facts. No stored per-mapping Entry, Node or Cell objects in promoted runtime representations.
- Identity and slot lifetime use stable generation-bearing handles. Removal zeroes released Object[] references and metadata; visible ordering is an int[]/offset operation. JNI only receives checked primitive arrays in bulk and retains no Java heap pointer.
- **A public Map.Entry may be created only on API request.** Immutable detached snapshots may be reused through bounded, dynamically grown \`M3LFUCache\` metadata with verified handle + key/value identity. A previously escaped Entry is never repointed. Mutable Entry.setValue, backed iterator views, serializers and concurrency remain under their original contract; do not reuse immutable snapshots in their place.
- LFU cache is derived/evictable. LFU/LRU, capacity shrink/trim, free-slot reuse and callback refusal cannot change canonical keys or values. \`M3LFUCache\` is an API facade cache, never the actual key/value map backing.

## Actual files added through the source-sealed recipe
\`M3LFUCache.java\`, \`M3ApiEntryFacades.java\`, and \`M3ArrayOnlyFacadeSelfTest.java\` are three separate ABSENT generated outputs. This PR has no changed archived JDK source, no changed existing collection owner and no new native entry point.

The accompanying 59-row \`JDK21_STORAGE_PASS.tsv\` maps every imported original type to a suggested existing M3 owner and a specific unclosed contract gate, never claiming an uncompiled donor was optimized.

## Verified and pending
Actual original local Java21 \`javac --release 21 -proc:none -Xlint:all -Werror\` on the two exact runtime sources: **PASS**. Larger real-source differential fixture: **2,050,202 checks PASS** (tested original source/test; the independent shipped reduced hosted oracle has not yet been run). No test doubles used. Canonical Maven/OpenRewrite scheduler, target exact-head owning-module Maven, checked JNI regression, 59 JDK semantic/GC/concurrency ports, installed image and speed/heap qualification: **PENDING**. Keep draft and preserve PR #1325 source-only disposition.

Correct any generated output in the canonical Synexia templates, replay and verify exact hashes; never patch M3JDK21 generated production files by hand.
