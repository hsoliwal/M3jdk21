<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 filtered NavigableSet bounded navigation — Java 21 receiver, 2026-10-17

## Ownership and precise scope

This is a stacked receiver PR based on M3JDK21 #1061, head
3a6bf30f7dab192cfda138b348829ad919b7f3b2. It does not replace
java.util, rewrite HotSpot, change JNI exports, or promote the reference
collection phase into the JDK runtime. Canonical algorithm research and
general-purpose recipes stay in Synexia convergence workspaces; this PR keeps
the Maven-bound product receiver transformation, the generated source, the
differential proof, and searchable donor-category evidence together.

Existing public constructors/factory/signatures, mutability policy,
comparator, live backing semantics and range-view APIs are unchanged.

## Before and after

For a TreeSet bounded by subSet(3,true,8,false), filtered to even integers:

| Call | Java 21 backing contract | Before | After |
| --- | --- | --- | --- |
| lower(100) | 6 | IllegalArgumentException | 6 |
| floor(100) | 6 | IllegalArgumentException | 6 |
| ceiling(-100) | 4 | IllegalArgumentException | 4 |
| higher(-100) | 4 | IllegalArgumentException | 4 |
| headSet(100,false) | throws for out-of-range endpoint | throws | throws |

The original implementation creates a headSet/tailSet view directly using
the arbitrary query, which can be outside the backing view's legal range.
The replacement first resolves a stored representative with backing
lower/floor/ceiling/higher. It then constructs a range iterator anchored at
that valid representative and scans only until the predicate admits a value.

Complexity on balanced-tree owners: O(log n + k), where k is the number of
filtered-out stored entries scanned after the seek. Retained storage is O(1).
The rejected alternative of repeated navigation for each skipped element
takes O(k log n). No universal speedup claim is made.

## Mandatory recipe-first and verification

This is a bounded exact-source Maven Java recipe receiver, not an invented
OpenRewrite recipe ID. Existing Synexia OpenRewrite atom/pattern profiles
remain untouched. The recipe admits exactly the four reviewed preimage method
bodies and refuses mixed or drifted edits. It is idempotent on installed output;
no unrelated methods change.

From repository root with JDK 21 and Maven:

    mvn -B -ntp -f m3/collections/wrappers/navigable-bounds-20261017/pom.xml verify

To regenerate against the exact reviewed preimage:

    mvn -B -ntp -f m3/collections/wrappers/navigable-bounds-20261017/pom.xml -Papply verify

The normal verify path never writes product source. Maven compiles the recipe,
checks fixed-point/refusal, verifies installed output, compiles actual Java21
receiver plus standalone regression corpus and invokes the oracle. The apply
profile is the explicit bounded write operation.

The focused local Java 21 proof ran with --release 21 -Xlint:all -Werror:
- preimage: IllegalArgumentException on out-of-range navigation;
- generated source: M3_NAVIGABLE_BOUNDARY_PROOF_PASS checks=22410;
- M3_NAV_BOUND_RECIPE_FIXED_POINT_AND_REFUSAL_PASS;
- M3_NAV_BOUND_RECEIVER_FIXED_POINT_PASS.

A whole-M3JDK bootstrap, jtreg, native HotSpot/JNI matrix, and hosted Maven run
are separate gates and are not claimed as passing here. JNI gains nothing for
these comparator-authoritative reference operations; no JNI ABI changes.

## Searchable algorithm/donor catalogue

The existing m3/collections/algorithm-catalog/order-statistics.tsv is extended
by five additive entries: JDK NavigableSet contract; Guava filtered navigable
views; GeeksforGeeks tree predecessor/successor; LeetCode #34 sorted-array
bounds; and HackerRank Java Comparator. Contest references supply problem
categories only, not copied code. Guava is Apache-2.0; the selected atom is
independently authored under Apache-2.0.

Selecting BOTH conflicting method implementations blindly is not valid.
A behavioral atom replaces the old implementation only after contract tests,
while Git retains prior source versions and their provenance.
