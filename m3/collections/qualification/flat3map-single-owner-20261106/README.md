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
