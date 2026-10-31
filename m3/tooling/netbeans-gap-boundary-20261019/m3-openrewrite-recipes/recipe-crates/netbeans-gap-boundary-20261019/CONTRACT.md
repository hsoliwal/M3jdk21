<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# NetBeans offset-boundary convergence contract

Inventory completed before source authoring. Canonical source base is Synexia
7d9464e880d4aa18f9d68a4f90f720e1af62a067; target base is M3JDK21
677437be295b23f7ccf7409be287008944d0e8b0; NetBeans donor is
f26ca8d8025ff820b8ffe751b6db6c1cc67baa97.

## Sealed atom and owner

Change only javax.swing.text.GapContent.findMarkAdjustIndex(int). Keep its
search.index = max(searchIndex, 1) side effect, and return the first mark whose
raw index is at least that key, or marks.size() if absent. Marks are sorted by
existing GapContent mutators. Preserve findSortIndex: createPosition's chosen
StickyPosition identity among equal offsets must not change. Preserve mark
ownership, weak-reference cleanup, zero-position bias, gap state, text backing,
serialization, protected hooks, exceptions, undo/redo and public signatures.

Existing behavior first finds an arbitrary equal mark then walks backward over
duplicates. Replace that private search atom by a half-open lower-bound search,
retaining findSortIndex and the immediate return when the preceding mark is not equal. Worst-case probes change from O(log n+k)
to O(log n); result enumeration and mark relocation remain O(k). No new fields,
structural objects, payload copy, dependency, JNI symbol or public API. JNI is
not selected for scalar heap-reference lookup: marshalling or heap access cannot
be justified by this bounded operation. This is java.desktop product code.

## Donor evaluation and rights

NetBeans GapList already overlaps JDK GapVector. OffsetGapList's first-equal
lookup has the same duplicate scan, so it is an unchanged executable comparison
oracle, not a faster runtime replacement. Use exact Apache-2.0 donor sources only
in offline authoring/proof. Keep their LICENSE/NOTICE and source seals. Do not
copy Apache source into the GPL target. Refine the existing OpenJDK search under
its original GPLv2+Classpath terms. CompactMap is rejected (retained linked entries,
null and iteration differences); flyweight offsets and incremental lexing remain
catalogued, pending separate consumers and contracts. No new framework is admitted.
This is routine refinement of already-public algorithms, not disclosure of a new
private invention or a claim of novelty.

## Ordered proof and fixed point

Patch recipe/template -> diff and Checkstyle -> Java21 compiler -> Maven/JUnit
recipe tests -> actual SDK materialization -> product lint/compile -> regression
execution -> resource measurements. Baseline and candidate use exact same immutable
probes. Run baseline/candidate public/protected and serialization descriptors,
headless Swing edit/position/undo traces, duplicate and extreme-key lower-bound
oracles, HTMLDocument regression, interpreter and compiled modes. Compare actual
NetBeans offset lookup on its admitted nonnegative <=2^30 domain. Keep boundary
and storage snapshots independent from the proposed implementation. Reject drift,
occupied additions and duplicates; test all partial replay states and fixed point.

Measure complete call CPU and allocation separately, and instrument mark reads in
an isolated test subclass only for operation counts. Compare object identities,
raw index arrays and retained field/layout declarations. Record process RSS;
claim no retained/native reduction if unchanged. Keep every failed candidate/log.
No manual output patching: correct the recipe and replay fresh inputs.

Full source-built image, full jtreg/tier/platform/reactor/JCK qualification and
broader NetBeans/Eclipse absorption remain pending. This draft is a scoped candidate;
it does not advance the global String-first receiving phase.

## Second candidate

The first whole-range lower bound passed all finite checks. Retain its source and measurements.
Refine it to reuse findSortIndex and check the immediately preceding mark first; run a lower bound
only when a preceding duplicate exists. This preserves the incumbent quick exact-match path
(including its midpoint case), empty and above-last behavior. The duplicate search interval is
[0, index-1], where index-1 is already known equal. No generic search helper is added.

## Current canonical engine reconciliation

Develop advanced during qualification. The publication base is
4565392487bf83b55a3031a26ea1a61b1b526b3c. Its two existing recipe-engine additions
admit the legacy m3/core text roots and do not change this target path. Consume
them verbatim, retain prior engine hashes, and require a final SDK replay to emit
the identical already-qualified five outputs. Global metadata preimages are unchanged.
