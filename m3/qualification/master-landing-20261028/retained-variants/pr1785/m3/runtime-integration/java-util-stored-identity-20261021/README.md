<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered Set identity — original M3JDK21 java.base

M3JDK21 is the integrated Java/JNI/HotSpot product, not a separate
M3+JDK adapter. This leaf belongs in the original
`src/java.base/share/classes/java/util/CollectionViews.java` owner.
Existing JDK `Collection`/`Set`/`List`/sorted contracts stay authoritative.

## History and exact source
Already merged [M3JDK21 #1116](https://github.com/hsoliwal/M3jdk21/pull/1116)
implemented stored Set representative selection. Later active
`java.base` lost the owner membership gate. The currently stacked
[PR #1770](https://github.com/hsoliwal/M3jdk21/pull/1770)
fixed the reentrant Spliterator and restored List's first-visible
physical-index removal, but its generic Set scan still treats
an equals-equal *nonmember* of an identity-backed Set as a match.

Freeze the PR #1770 exact commit
`ef24da4c5224ca0f728db3c3c30fa412592c7765`:
- original Git blob: `1df992f3258e3b8573003172d9b7ea7083ac5e18`;
  SHA256 `e6c6779848e867e0898a5b577a5c6109094bb2b9ed5ba5e625b446735102e3c6`.
- candidate blob: `709b42eabc01bb766143ee824202601132d8690e`;
  SHA256 `779510ab4000f0154a387137244ccc8000a59b1eee31ca47782c03866891ebaa`.
- original historical test [#1116] `StoredRepresentativeOracle.java`
  Git blob `491b55c10f829b35f1cdcc5e2233d27564617d17`,
  SHA256 `f32eeb5079bcd6322e597303a937614931b7f33779a58d0ed03b53ae0a8302c6`.
- canonical Synexia source-first recipe:
  [#12872](https://github.com/hsoliwal/com.synexia/pull/12872),
  selected dataset `identity-set-ef24-20261021` from existing
  Maven/OpenRewrite `m3-ast-build-reverse-20261017` recipe crate.

## Bounded behavioral-atom change

Only private `FilteredCollection.contains(Object)` and
`remove(Object)` add a Set-specific branch:
`backing.contains(probe)` admits the original set domain, and stored
`==` gets first preference before equals-alias matching. This preserves
IdentityHashMap-backed Set identity and ordinary HashSet stored visibility
with no new payload or per-entry objects.

The newer List, generic Collection iterator, sorted/navigable
representative, mutable write/refusal rules and reentrant Spliterator
implementation are **unchanged**. Public Java SE APIs/descriptors, JNI
ABI and M3 primitive arrays/offsets/mask owners are unchanged. Arbitrary
Java callbacks stay CPU-side; no GPU or JNI change is useful here.

## Proof status

A **focused reconstructed-source Java21 model** passed 736,018 checks
(default, -Xint, C1, non-tiered); old identity-Set source first failed
at historical check 9. That is not a full `java.base` test.
The exact-head workflow now runs the real source patched into java.base,
the old-RED/new-GREEN Oracle, public javap API identity, and parent
FilteredRepresentative/FilteredSpliterator/Bootstrap tests.
Maven/OpenRewrite actual `Recipe.run`, SDK Result receiving/fixed
point, complete JDK21 build/JCK/jtreg, JNI/GC/HotSpot, five-provider
contracts and performance gates are NOT_RUN in local authoring.
Keep PR DRAFT until exact-head passing host evidence and real recipe
qualifies the product.

The first two historical Synexia receipt datasets have stale
preimages because #1770 advanced. Never replay them over this source.
No rebase/force push, no change to Synexia develop, no manual postimage
source patch to conceal recipe errors. Challenge sites supply category
taxonomies only; no challenge implementation source was copied.
