<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered indexed cursor successor receipt — 2026-10-19

This successor refines the original M3JDK21 filtered-view owners at combined
parent `b0dcf90ae18be891220aad27059e31dfdf423354`. It preserves #1428's
physical set-slot paths and #1417's interior-null navigation correction.

Four map cursor implementations now prepare an accepted physical slot once,
matching the existing generic filtered cursor's selection behavior. Repeated
hasNext and the following next avoid redundant selection predicate calls.
Map/set slot cursors check the structural revision after predicates and
bounded navigation, and capture it before computing the first slot. A callback
exception retains its identity; a returning callback which structurally changes
the owner is detected before a saved slot is accepted or followed.

The added state is one boolean per map cursor. Keys, values, bounds and AVL
links remain in the original owner. There is no per-element cache, boxed
offset, retained Entry or new JNI symbol. Mutable iterator removal preserves
its prefetched successor across physical erase; null keys/values, stored key
representatives, range/descending order and live/immutable entry behavior are
covered by the receiving oracle. Comparator-equivalent entrySet.contains and
persistent predicate membership masks retain their predecessor HOLD because
their callback changes require a separate contract.

## Canonical recipe and source authority

Canonical repository: [hsoliwal/com.synexia](https://github.com/hsoliwal/com.synexia).
Canonical crate:
`synexia-openrewrite-recipes/crates/m3jdk21-20261019/collections`.
Class-backed composition:
`com.synexia.rewrite.M3FilteredCursorReadinessRecipe`.

The crate reuses the unchanged Java-LST M3HashPinnedJavaSnapshotRecipe and scope
helpers from Synexia commit `9f6374324c975e3111f705019397e3147b6c7193`.
Its ENGINE_PROVENANCE.tsv pins their exact Git blobs. Four single-file atoms
own frozen preimages/templates; target Java was written only from actual
Recipe.run compilation-unit output, serially with compiler/behavior gates.
The combined publication receipt identifies the published successor crate.

PRODUCT_SOURCE_MANIFEST.tsv seals the four current physical source files.
SCOPE_DISPOSITION.json records identity mappings and the unchanged receiving
phase. Earlier receipts describe their original revisions and are not edited
to pretend that their historical proofs covered these successor bytes.

## Executed bounded qualification

- Actual Maven/OpenRewrite SDK: six tests, zero failures/errors; all sixteen
  partially applied states, all-applied fixed point, exact path isolation,
  Java/string/comment/prose decoys, source identity and drift/missing/duplicate/
  non-Java/template/changed-after-scan refusals.
- Four negative witness modes reproduce duplicated callback traces and missing
  immediate mutation detection on the original frozen owners.
- Strict Java21 receiving compilation after each production file; the new
  combined standalone oracle passes 975 checks. The JUnit adapter compiles
  against pinned JUnit with a test-only named-module read edge.
- Existing readonly-slot 266,236, filtered-view 123,500, mutable-slot 478,161,
  mutable-view 288,223, set-slot 150,508, bounded-navigation 90,431,
  navigable-set-range 110,325, indexed-fusion 9,962 and deep-filter 3,119
  checks/assertions pass. The deep-filter depth is 100,000. Indexed-fusion was
  run with JNI=false in this packet.
- All four physical target files match the exact qualified postimage hashes.

Canonical QUALIFICATION.md and evidence logs retain the actual commands,
successful output and corrected setup/oracle failures. No full source-built
Synexia reactor, collection Maven suite, patched JDK image, full jtreg/GC or
wall-clock/allocation benchmark is claimed by this bounded receipt. Existing
JNI-required tests remain an integration gate; no native code is changed here.
The performance claim is reduced repeated predicate work, not RAM-speed or
an unmeasured end-to-end speedup.
