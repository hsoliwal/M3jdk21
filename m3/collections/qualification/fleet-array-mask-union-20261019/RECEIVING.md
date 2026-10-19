# M3JDK21 — original-owner fleet union: integer map and mask JNI (2026-10-19)

Product parent is **M3JDK21 #1391** (commit 06c56658975e90aca82b241fe1f4fe26f2b6b62c),
which already includes #1354 sorting, #1365 reentrant filtered views, #1377 utilities,
and #1391 immutable indexed-filter/sort + full-width physical masks.
**This is not an M3 layer on an otherwise unmodified JDK.**

Serial single-file atom delivery:
1. Existing `M3IntIntHashMap.java` source exactly receives first-party #1388
   `addTo` single-discovered-probe insertion atom. No new map owner or Entry.
2. Existing `m3_index_mask_compactor.c` exactly receives #1389 native int/long
   mask-window stable prefix compaction. Only two pre-existing window functions
   differ from parent: remove `accepted[256]` and compact in `values[256]`.
   Preexisting second-state-pass empty-output verification is retained.
3. Add independent Java 21 fixture, JUnit wrapper and source-sealed Java/C JNI CI.
   Do not alter M3IndexedListSnapshot, M3FilteredViews or M3IndexPathMasks.

The native comparison was source-proven mechanically: deleting two scratch
declarations, writing accepted values into the already-read prefix and changing
two SetArrayRegion pointers exactly yields the canonical #1389 source,
apart from two explanatory comments. All exported JNI symbols match.
`row_ids[write] = row_ids[i]` and `values[write] = values[i]` are safe
because accepted-prefix `write <= i`; source Java arrays are never mutated.

Synexia has the only build-time OpenRewrite recipe/candidate catalogue.
OpenRewrite Java LST, native-text and YAML recipe sources are sealed to the
parent's exact before SHA-256 and the reviewed after content.
No runtime Synexia dependency or new JNI symbol.
The category catalogue classifies LeetCode/HackerRank/GFG donor problems
and rejects XOR when mathematical frequency/neighbor contracts differ.

**Admission**: GitHub PR draft, not merged. Existing first-party donor
#1388 reported its own 260,388 Java/260,407 JNI differential checks; these
are historical evidence, NOT an execution of this combined successor.
Must execute this branch's source seals, new Java/JNI tests, inherited
M3PackedFilterTest, all owning module tests, JNI UBSan/ASan, ABI comparison,
Maven/OpenRewrite fixed point and broader modified JDK/HotSpot validation
before promotion. Never discard other fleet PR atoms; #1389's
M3FilteredViews / M3IndexPathMasks changes require separate per-atom analysis.
