<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Five-provider collection inventory and scaffold, 2026-10-18

`INVENTORY.tsv` lists every collection type in the pinned #1278 universe (740 public concrete
root types across JDK 21, Guava 33.4.8-jre, Commons Collections 4.5.0, Eclipse Collections 11.1.0
and Spring 6.2.0) plus eight first-party rows. Each row has one `owned_status`:

| status | meaning | rows |
|---|---|---|
| `DONE #PR` / `DONE master` | owned array/mask implementation already on master | 9 provider + 2 first-party |
| `EXISTING` | core JDK-shaped M3 owner already on master; no scaffold (NO-DUP) | 10 |
| `CLAIMED #PR` / `CLAIMED viewport-everywhere` | an open PR or the viewport agent owns it; do not build | 10 provider + 6 first-party |
| `SCAFFOLD` | generated skeleton module exists; bodies throw `UnsupportedOperationException` | 133 |
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

1. Guava `ArrayListMultimap` (reuse interop `M3GuavaListMultimap`, core `M3IndexedMultiValueMap`); Guava
   `HashMultimap` reuses the #1261 engine.
2. Commons `HashBag` / Guava `HashMultiset` over `M3CountedBag` (coordinate with #1271).
3. Guava `EnumHashBiMap`/`EnumBiMap`, Commons `DualLinkedHashBidiMap` over the owned `M3HashBiMap`/`M3BidiMap`.
4. Filtered/sorted wrappers are already claimed (#1362, #1365, #1366, #1367, viewport selection lists): no scaffold.
5. Spring `MultiValueMapAdapter` over the owned `M3MultiValueMap` (`LinkedMultiValueMap` is DONE).
