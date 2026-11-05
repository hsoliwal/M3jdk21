# M3JDK21 five-source additive blob custody — 2026-11-05

STATUS: SOURCE-SLICE MERGED, 25/25 EXACT ORIGINAL GIT BLOBS, 13 MODIFIED ATOMS HOLD, PRODUCT MASTER UNCHANGED.

## Exact pinned donor heads

| Original draft PR | Immutable donor head | Newly added original paths preserved | Modified atoms withheld |
|---|---|---:|---:|
| [#2479](https://github.com/hsoliwal/M3jdk21/pull/2479) | `8ef244bd14ad0c6f0dd2d3f7e438ecb99ab4b38b` | 14 | 2 |
| [#2483](https://github.com/hsoliwal/M3jdk21/pull/2483) | `8d4ce462ad0765609553c8656f9c9a6edb7261f9` | 2 | 7 |
| [#2482](https://github.com/hsoliwal/M3jdk21/pull/2482) | `20b810b2150b05a7978397f497f28de0e2d6b39a` | 3 | 2 |
| [#2406](https://github.com/hsoliwal/M3jdk21/pull/2406) | `6cfc59b98f7d9609feee847a720bcb851f36ef04` | 3 | 2 |
| [#2475](https://github.com/hsoliwal/M3jdk21/pull/2475) | `8774d4b48bdd8e05a175082db6517cebc17337bb` | 3 | 0 |

- Frozen source stage before this slice: `m3/frozen-source-recovery-simhash-20261104-gpt6` at `a11a3954d038cbe9483366548b76751b2735e34a`; originally compared with master base `172e39d5e69a8f7775c25ad1ce2bfaf2bf5d1246` as 44 ADDED, zero changed/deleted.
- All 25 donor new paths were absent on frozen receiver and mutually disjoint. Original Git unified diff and PR transport modes: all 100644. SHA object IDs fetched from immutable head SHAs and re-used without source edits.
- New source-slice Git tree SHA `002b6e86fc1910c105d34d624154908d5b64903e`, one-parent commit `bad2ce6dc7420efa97e8c4994c855d8d50e26fed` atop exact frozen parent. It carries 25 ADDED/0 modified/0 deleted, 25/25 blob SHA readback PASS.
- Official non-draft [transport PR #2484](https://github.com/hsoliwal/M3jdk21/pull/2484): PR diff exact 25 source added paths, 0 deletions, original modes preserved. Normal two-parent GitHub merge SHA `f1fb84f5dfd6dc3b62019cba90af51f7093b6ac3`. First-parent delta 25 ADDED, 0 changed/deleted. Post-merge blob SHA readback 25/25 PASS.
- Proof: `EXACT_25_BLOBS.tsv`; exact excluded non-additive contracts/tests: `HELD_13_MODIFIED_ATOMS.tsv`.

**Do not claim original donor ancestry merged:** source-slice commit includes only additive new blobs, original five donor PRs remain open, their 13 modifications need recipe-first semantic merging and test/ABI qualification. No master/default-branch write; Java21 baseline and standalone canonical JDK naming preserved by non-promotion.

## Follow-up gate
- Maven/OpenRewrite JUnit per atom, Java21, native JNI ABI/behavior, full M3JDK21 build and cross-platform CI: HOLD, not executed or passed here.
- Expected frozen stage vs original frozen master: 44 earlier additions + 25 new exact source paths + 3 source-qualification records = **72 ADDED / 0 MODIFIED / 0 DELETED**. Independently verify final compare.
