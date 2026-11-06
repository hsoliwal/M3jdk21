<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 PMEM Gram campaign: verify the original source generation after search

This private Synexia recipe extends the existing `PmemGramCampaignAdmission` owner, stacked on #16571, without creating a new native search engine, source index, Java API, C17/JNI symbol, durable claim store or model dispatcher.

## Independently reproduced defect

A GramIndex is built from `D.java = "zzz"`. The reviewer (or a search callback) then makes a same-length external write to the source image, changing D to `"abc"`. The original GramIndex prunes D using stale grams and reports a **complete** two-hit first-occurrence result. A source-root manifest hash remains constant and cannot by itself detect the mutated backing bytes. This can incorrectly authorize an LLM worklist missing the third match.

## Exact private Java correction

After a completed gram/JNI exact first-hit search and before constructing any returned `FileTask`, iterate **all reviewed selected file indices**, including gram-pruned misses, and call the original `PmemSourceImage.readSealed(fileIndex)`. The PMEMSDK owner checks the original file SHA-256 and byte size. On drift, truncated backing, closed image or cancellation, fail closed and emit no worklist. Discard each temporary returned array immediately; no source strings, candidate nodes, extra JNI pointers, copies of challenge code or new global index are retained.

The original sorted source selection, quality tool receipts, Maven recipe and donor seals, source/behavior/interface/atom-map roots, original first-hit byte offsets, JNI/POSIX or FILE KMP and durable model-call ledger semantics remain unchanged. The original method descriptors are byte-identical according to `javap -p -s`.

The added work is O(sum of selected source bytes) per admitted search, not retained Java heap. For 20,000 x 50,000-byte files, approximately 1,000,000,000 bytes are rehashed after search. This is a correctness gate, **not** a speedup claim. Source capture still requires an immutable backing lease through review, indexing, search and dispatch; a final hash is not an OS write lock and cannot prevent transient restore attacks or later uncoordinated writes.

## Recipe and evidence

Reuse canonical `M3HashPinnedJavaSnapshotRecipe` with **two exact existing Java preimages**, two exact reviewed postimages, SHA-256 manifests, original schema/syntax, typed `Recipe.run`, fixed-point and stale/absent refusal. This leaf does not copy Maven recipe machinery to a public target. The new Java/JUnit source templates are materialized only after the recipe source, tests and evidence.

Local JDK21 proof: original source demonstrably accepts the stale pruned file; new source throws `SecurityException`. Restricted 20,000-source review remains 274 tasks, original 846 assertions and identical method signatures. Six JUnit-shaped methods passed against explicit FILE-image test doubles. Actual original Maven, Jupiter, JNI, JMOD, 1GiB hardware and 20,000 live Chrome calls remain **HOLD/NOT_RUN**.

No product-fork or protected develop promotion until full receipts are checked.
