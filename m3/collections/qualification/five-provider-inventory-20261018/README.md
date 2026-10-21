<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Five-provider collection inventory and scaffold, 2026-10-18

`INVENTORY.tsv` lists every collection type in the pinned #1278 universe (740 public concrete
root types across JDK 21, Guava 33.4.8-jre, Commons Collections 4.5.0, Eclipse Collections 11.1.0
and Spring 6.2.0) plus sixteen first-party rows. Each row has one `owned_status`:

| status | meaning | rows |
|---|---|---|
| `DONE #PR` / `DONE master` | owned array/mask implementation on master or in an open PR | 16 provider (8 master, 8 PR) + 2 first-party |
| `EXISTING` | core JDK-shaped M3 owner already on master; no scaffold (NO-DUP) | 10 |
| `CLAIMED #PR` / `CLAIMED viewport-everywhere` | an open PR or the viewport agent owns it; do not build | 10 provider + 14 first-party |
| `SCAFFOLD` | generated skeleton module exists; bodies throw `UnsupportedOperationException` | 126 (133 generated; 7 since owned) |
| `PATTERN_ONLY` | Eclipse Collections (EPL-1.0/EDL-1.0): never copied, never a dependency | 578 |

There are no `OPEN` rows left: the generator turns every OPEN JDK/Guava/Apache/Spring row into a
SCAFFOLD module. A later owning PR flips one row to `DONE #PR` and replaces that module's bodies.

## Template-first generation

`M3FiveProviderScaffold.java` is a single-file Java 21 program; templates live in `scaffold-template/`
and storage plans in `STORAGE_PLANS.tsv`.

```
java -cp guava-33.4.8-jre.jar;failureaccess-1.0.3.jar;commons-collections4-4.5.0.jar;spring-core-6.2.0.jar;spring-jcl-6.2.0.jar \
     M3FiveProviderScaffold.java inventory m3/collections/qualification/five-provider-inventory-20261018
java -cp ... M3FiveProviderScaffold.java scaffold m3/collections/qualification/five-provider-inventory-20261018 m3/collections/implementations
```

Inputs: `CANDIDATES_PR1278.tsv` (exact copy, Git blob `590a4a3e9d7d971eb2a19c586068695a2623df1d` from
PR #1278 head `87b37faa`), `PROVIDERS.tsv`, `STATUS_OVERLAY.tsv` (DONE/EXISTING/CLAIMED evidence and reuse
hints), `FIRST_PARTY_ROWS.tsv` (viewport-everywhere claims, #1233, #1226) and `PEER_SCAFFOLD_CLAIMS.tsv`
(provider types covered by the peer native skeletons on `m3/five-library-scaffold-20261018@bec1ebba`,
recorded as reuse hints). Outputs: `INVENTORY.tsv`, `SCAFFOLD_SURFACE.tsv`, 133 modules under
`m3/collections/implementations/<lib>-<name>/` (pom.xml, README.md, LICENSE.txt, NOTICE.txt, one class) and
the aggregator `m3/collections/implementations/pom-five-provider-scaffold.xml`.

Each class is `public final`, implements the provider's public interfaces directly (no provider
implementation superclass, delegate or body), declares the provider's public constructors, static
factories and instance methods with generics resolved by reflection, and holds the planned lanes
(arrays, offsets, `long[]` state masks) as unused `private transient` fields. No Entry or Node object is
part of any storage plan; Entry-shaped values that provider APIs demand are planned as boundary flyweights.

Generation details recorded in `SCAFFOLD_SURFACE.tsv`:
- javac visibility bridges (public methods inherited from package-private provider superclasses) are
  resolved to their generic origin; erasure bridges are dropped.
- Commons `CursorableLinkedList` and `NodeCachingLinkedList` declare `boolean addFirst/addLast`, which
  cannot compile against JDK 21 `List.addFirst/addLast` (void); the scaffold declares the JDK 21 signature
  and records the conflict.
- Provider deprecations are mirrored with `@Deprecated`; references to deprecated provider types or raw
  provider bounds are suppressed per member/class and listed per module.

## Gates (this packet)

- Strict compile of all 133 scaffold sources: `javac --release 21 -proc:none -Xlint:all -Werror`, exit 0.
- Maven aggregator: `mvn -f m3/collections/implementations/pom-five-provider-scaffold.xml compile`
  (see the PR body for the run receipt).
- Second generation pass leaves every generated file byte-identical (fixed point).
- `git diff --check` clean.

Not claimed: no behavior, protocol, serialization, performance or allocation result; no row is owned by
this packet. Provider API types are compile dependencies of the guava-/apache-/spring- scaffold modules
only; the core `m3/collections` module is unchanged and still depends on `java.base` only.

## Fill order (one owned type per PR, each flips its row to DONE)

1. Guava `ArrayListMultimap` (DONE #1400). Guava `HashMultimap` is BLOCKED until open #1261 lands: it must
   reuse that PR's `M3IndexedSetMultiMap` engine, not duplicate it (coordinator, 2026-10-19).
2. Commons `HashBag` (DONE #1421) and Guava `HashMultiset` (DONE #1418).
3. Guava `EnumHashBiMap` (DONE #1460) and `EnumBiMap` (DONE #1462); Commons `DualLinkedHashBidiMap` (DONE #1468).
4. Filtered/sorted wrappers are already claimed (#1362, #1365, #1366, #1367, viewport selection lists): no scaffold.
5. Spring `MultiValueMapAdapter` (DONE #1469; `LinkedMultiValueMap` is DONE on master).
6. Next wave (approved 2026-10-19), one stacked PR each: Guava `EnumMultiset` (DONE #1499), `LinkedHashMultiset` (DONE #1523),
   `LinkedHashMultimap`, Commons `TreeBag`, Guava `TreeMultimap`. Each row's `in_place_landing` names the
   java.util store it lands in under the M3jdk21-is-the-JDK rule.

## Viewport-derived views (2026-10-19)

All viewport logic is folded into `m3/collections` as general collection views by the viewport-everywhere
agent (PR #1403, branch `m3/viewport-everywhere-20261018`). `FIRST_PARTY_ROWS.tsv` records them as
`CLAIMED viewport-everywhere` with `source=first-party-viewport`: present in the PR head are
`M3OrderStatisticForest`, `M3SparseExtents`, `M3SparseItemStore`/`M3BlockStore`, `M3BlockCache`,
`M3BitLanes`/`M3ColumnarLanes`, `M3LaneGrowth`/`M3HeapGrowth`, `M3SelectionItem`, `M3RowTreeItem`; announced are
`M3WindowView`, `M3FlatteningView`, `M3ListDiff`/`M3Patch`, `M3BoundedCache` and `M3RangeSet`. The rows name the
provider surfaces they cover (JDK `subList`, Guava `Lists.partition`/`RangeSet`, Apache `ListUtils`/
`CompositeCollection`/`TreeList`/`LRUMap`), and the matching scaffold rows carry them as reuse hints.
Filtered/sorted wrappers compose over these views (a view of a view) instead of duplicating them.

## In-place landing (2026-10-19)

M3jdk21 is the JDK: `m3/collections` is the proving ground and owned stores land in place in `java.base`.
`INVENTORY.tsv` therefore carries `in_place_landing` and `landing_consumer`, derived by the generator from
`LANDING_MAP.tsv` (category family to its `java.util` analogue):
- JDK rows land in their own `java.util` type. The public API stays unchanged and the internals move onto M3
  array+mask lanes through an M3JdkHandoff packet (TARGET_OWNED_PORT, GPLv2+CPE).
- Guava/Commons/Spring rows reuse the `java.base` store of their family's `java.util` analogue, while the
  provider-shaped API owner stays outside `java.base`.
- Eclipse rows never land.
