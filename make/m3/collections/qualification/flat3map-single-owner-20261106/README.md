<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# flat3map-single-owner-20261106: one Flat3 map implementation (census leaf L075, recipe FLAT3MAP_SINGLE_OWNER v1)

**Owner decision** (made by the user, relayed by session com-synexia-63, 2026-11-06): the M3-native
`jdk.m3.collections.M3Flat3Map` of the core module `m3/collections` is the **single implementation**;
`jdk.m3.collections.apache.M3Flat3Map` (`implementations/apache-flat3-map`) stays only as a thin Commons-compatible
facade that delegates to it, proven by a Commons differential.

| Step | Content |
|---|---|
| commit 1 | the core is at the crate postimage: hash-pinned `VERIFY` of the core and its crate tests, the census hunks of the historical prepacked candidate with their verdicts (`HUNKS.tsv`) and `NEC.tsv` |
| commit 2 | the apache class becomes a facade over the core (declared `TEMPLATE` rewrite in the same recipe) |

## Crate and census

- Crate `m3jdk21-flat3map-20261029` on `hsoliwal/com.synexia` develop `b65df53330e3d82f6b2d5509ded971f19883f4ca`:
  `synexia-openrewrite-recipes/crates/m3jdk21-flat3map-20261029` (README, `PACKED_STATE_SUCCESSOR.md`, proofs) and
  `synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-flat3map-20261029/`
  (`manifest.tsv` sha256 `47dec31e5824...`).
- Manifest postimages (blob / sha256), all present on master under the module package `jdk.m3.collections` (the
  crate spells it `com.m3.collections`; the JDK naming rename `fe35ea98841` is the only difference):
  `M3Flat3Map.java.txt` `2117a78ef0b5` / `28fb1d0ac39c3e18...`, `M3Flat3MapContract.java.txt` `3cb5e4ecfda7` /
  `7378bc357e20e8e3...`, `M3Flat3MapTest.java.txt` `04db62d40e96` / `04f7b82e6a18c987...`,
  `M3Flat3MapProviderTest.java.txt` `802c3479c69e` / `508f91d8852c460c...`; support `M3SlotArrays.java.txt`
  `7869710b355d` / `59da94e994bbbf3d...` (unchanged). They were received by `abc8b3aea44` (2026-10-30, "Receive
  generated M3Flat3Map activation and bounded contract tests"); no later M3-native change touched the core before
  this leaf (`git log` of both paths: `abc8b3aea44`, `fe35ea98841`).
- Census row L075 was `AMBIGUOUS` (two owners named `M3Flat3Map`; the closer one, the core, had 132/155 postimage
  lines, 11 hunks, 66 changed lines). Re-run by hand with the census normalisation at master `e65f0f664fa3`: the
  ambiguous row is `prepacked/M3Flat3Map.java.txt` (blob `6da485d557f3`, sha256 `80df7fd25984`), the crate's
  **historical first candidate** (one `long` state word per slot, a `long[3]` lane in the inline regime). The
  crate's own `PACKED_STATE_SUCCESSOR.md` replaces it by the packed two-bit successor, which is the manifest
  postimage above and is on master byte for byte. The 11 hunks (66 lines) reproduce exactly; each would revert the
  successor: verdict `REJECT_SUPERSEDED`, keep master, one `NEC` row each with both versions (`HUNKS.tsv`,
  `NEC.tsv`). The second owner (`apache-flat3-map`) is the five-provider scaffold, not a candidate for the
  postimage (every body threw `UnsupportedOperationException`).

## Recipe

```
python -I m3/collections/qualification/flat3map-single-owner-20261106/apply.py [--root <repo>] [--check]
```

Derived from `m3/qualification/progress-monitor-unify-20261106/unify.py`. Every rule pins the git blob of its
preimage at master `e65f0f664fa3`; a target must equal its preimage or its postimage, otherwise the recipe refuses
(exit 2) and writes nothing. A rule may also pin the crate postimage sha256: the pinned preimage, spelled with the
crate's package again, must reproduce it byte for byte (the package is assembled from parts in the recipe, because
the naming recipe rewrites `.py` files). `EDITS.tsv`: file, kind, before blob, before sha256, after sha256, crate
sha256, rule; a second run is a fixed point (changed=0).

Commit 1: 5 `VERIFY` rules, changed=0; no product source changes.
Commit 2: 14 rules (4 `VERIFY`, 5 `EDIT`, 2 `TEMPLATE`, 3 `ADD`), changed=10 on the first run, 0 on the second;
`TEMPLATE` and `ADD` rules pin their postimage sha256 as well.

## Facade design (commit 2)

**Delegate, not extend.** The core is `public final class M3Flat3Map<K, V>` (it cannot be extended without
changing the owner) and has no `java.util.Map` supertype; the facade's `javap -public` reads
`public final class ... implements Cloneable, Serializable, Map, Get, IterableGet, IterableMap, Put` over
`java.lang.Object`, and a superclass would change that line and the inherited public surface. Delegation keeps
`javap -public` (and `javap -public -s`) byte-identical (34 lines). The facade holds one
`private transient jdk.m3.collections.M3Flat3Map<K, V> core` and nothing else. It is not `final`: `readObject` and
`clone` must assign it, and the serialized form below has no field for it.

| Facade member | Delegation |
|---|---|
| constructors, `get`, `put`, `remove(Object)`, `containsKey`, `containsValue`, `size`, `isEmpty`, `clear`, `putAll` | core map operations (`putAll` puts each entry, as Commons) |
| `entrySet`, `keySet`, `values`, `mapIterator` | new view per call (as Commons flat mode) over the core's slot cursor; iterators write through; `remove()` uses `removeSlot` and keeps the cursor; a removed entry flyweight refuses access (`IllegalStateException`) |
| `equals`, `hashCode`, `toString` | `java.util.Map` contracts over the cursor (`equals` returns false on `ClassCastException`/`NullPointerException` like `AbstractMap`; `toString` is `{k=v, k=v}` with `(this Map)`) |
| `clone` | `super.clone()` plus `core.copy()` |
| `getOrDefault`, `putIfAbsent`, `replace` x2, `remove(Object, Object)`, `compute`, `computeIfAbsent`, `computeIfPresent`, `merge`, `forEach`, `replaceAll` | `IterableMap.super.*`, the JDK `Map` defaults, which Commons `Flat3Map` 4.5.0 does not override either |

**Core additions** (the single owner gains what the facade needs; existing public members unchanged, `javap
-public` of the core gains exactly these 6 lines): `nextSlot(int)`, `slotKey(int)`, `slotValue(int)`,
`setSlotValue(int, V)`, `removeSlot(int)` (no key lookup; returns the resume slot: same slot inline, next slot
hashed) and `copy()` (same lanes, regime and order). `remove(Object)` is split into `removeHashed`/`removeInline`
(MRL method split) so `removeSlot` shares the bodies. Field names and lane layout are unchanged (the crate oracle
inspects them reflectively).

**Serialization.** Master form: class `jdk.m3.collections.apache.M3Flat3Map`, `serialVersionUID` 0, no serializable
fields (all fields transient), no data, and no instance could exist because every constructor threw. Kept: name,
`serialVersionUID` 0, zero serializable fields. Added: `writeObject`/`readObject` with the Commons `Flat3Map` data
layout (`defaultWriteObject`, `int` count, then key and value per entry), because a working map must carry its
entries; the only descriptor change is the `SC_WRITE_METHOD` flag. Probed (`serialcompat.log` in GATES.tsv): the
master class reads a new-form stream (skipping the data); a state-free master-form stream (built without a
constructor through `ReflectionFactory`) is refused by the facade with `EOFException` instead of yielding an object
whose every method throws; a truncated new-form stream is refused (`EOFException`). Commons quirks below were
confirmed against 4.5.0 (`commons-quirks.log` in GATES.tsv). Not `writeReplace`/`readResolve`:
a proxy would put a different class into the stream, a larger change than the flag.

**Iteration order.** The core's slot order: insertion order inline (removal shifts), table order hashed. Commons
4.5.0 differs and is itself mode-dependent (flat mode moves the last entry into a removed slot and prints
`toString` as `{k3=v3,k2=v2,k1=v1}`; delegate mode follows `HashedMap` buckets and stays in delegate mode until
`clear`). Neither `Map` nor `Flat3Map` specifies an order, so the differential compares sorted entry sets and
counts raw order differences (`orderDiffs`) without failing on them.

**Not compared** (Commons quirks outside the `Map` contract, recorded): `entrySet().remove(entry)` in Commons flat
mode removes by key even when the value differs (the facade follows `java.util`: key and value must match);
views held across a Commons `clear()` in delegate mode go stale (the facade's views always follow the core);
callback counts (`hashCode`/`equals` calls) differ (`removeSlot` does no lookup); fail-fast
`ConcurrentModificationException` exists only in Commons delegate mode (the facade, like Commons flat mode, is not
fail-fast; a cursor is defined across its own `remove` and `setValue`).

## Commons differential

`M3Flat3MapDifferential` (test source of `apache-flat3-map`, JUnit wrapper `M3Flat3MapCommonsDifferentialTest`):
seed `SEED = 0x4C303735_20261106L`, `OPS = 1,000,000`, lockstep with one draw per op applied to both sides, each side
hashed separately (SHA-256 of its records). Keys: 12 ids plus `null`; ids 6..11 share three hash codes with id 0.
Values 0..9 plus `null`. 23 op kinds: put/get/remove/containsKey/containsValue/size/clear/putAll, entry/key/value
iteration, iterator `remove` and entry `setValue` by key/value predicate, `mapIterator` walk with
`getKey`/`getValue`/`setValue`/`remove`, clone (independence checked), equals (copy, perturbed copy, the other
side, `null`, a string, an empty map), hashCode, serialization round trip (the copy replaces the map), toString
(parts sorted), the 11 `Map` defaults, iterator misuse (exception class), view queries. Every record ends with a
state stamp (size and `Map.hashCode`). Put/remove bias alternates every 512 ops, so the size crosses the
3-entry flat/hash boundary both ways. Commons Collections **4.5.0** (`commons-collections4-4.5.0.jar`, the
module's existing compile dependency, Maven Central) is the oracle; nothing of it is vendored.
