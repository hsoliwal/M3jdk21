<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 filtered List — recipe-sealed receiving candidate (2026-10-17)

Synexia donor: hsoliwal/com.synexia PR #11977 at
`dcc01cbf67317709e8dfad123acbfc5d187699ad`, original Git blob
`ab51de6af292a2286ef3326398994a89b50725fc`.
Reusable Maven/OpenRewrite recipe owner:
`com.synexia.rewrite.M3Jdk21FilteredListsReceiver20261017`.
Manifest exact SHA-256, ABSENT-before target list and six Java files are in the
canonical Synexia hash-pinned recipe crate. No hand-modified production postimage.

This receiver is additive to the existing `com.m3.collections` Maven module;
`java.util.List` remains the externally authoritative JDK API. The public
factory is `M3FilteredLists.filtered(List, Predicate)` or
`M3FilteredLists.readOnlyFiltered(List, Predicate)`. Do not introduce a
`M3CollectionViews.filtered(List,...)` overload: recompilation might silently
change existing caller overload resolution. No new retained payload owner.

**Observable contracts:** live selected order and multiplicity, nulls permitted
only where the predicate and backing admit them, physical subList boundary,
read-only UOE for all mutations including empty endpoints, iterator reversible
lookahead and last-delivered set/remove target, predicate preflight on addAll,
List.reversed() semantics, no false SIZED/SORTED/DISTINCT spliterator flags,
no retained per-element array, stable selected order of comparator-sorted source.

**JNI:** no new native symbols. Arbitrary Java Predicate/Comparator callbacks
are not callable from the native primitive-mask route without changing ordering,
exception and GC lifetime semantics. Existing M3IndexMaskCompactor JNI and
Synexia MIndex count/UTF-16 native algorithms remain separate donor/gate owners.

**Complexity:** List view indexed lookup O(n), sequential scan O(n); no
independent sorting index, no O(1) random access and no guaranteed parallel
splitting. Contrast: sorted snapshots are detached; navigable filtered Set/Map
owners are separately qualified in M3JDK21 #991/#994; primitive wrappers
are already present on M3JDK21 master. Do not duplicate their payload stores.

**Source and tests:** five Synexia-authored Java21 donor sources have only their
package and SPDX header adapted; one new M3TreeSet comparator interop test added.
Original Apache-2.0 attribution preserved. No copied challenge/editorial source.
LeetCode Remove Element #27, HackerRank Java Comparator and GeeksforGeeks
Remove Occurrences are problem-shape catalogues, not implementation donors.
Guava and Apache Commons are compared as contract references only.

**Gates:** Maven/OpenRewrite SDK source generation, fixed-point, drift refusal;
target Java21 compilation and selftest, target full JUnit/module packaging,
installed M3JDK21 image smoke, inherited JNI parity, and cold/warm CPU/retained
memory are distinct. No source snapshot, finite donor oracle or commit constitutes
full JDK backend promotion. Active M3JDK21 porting phase remains STRING.
