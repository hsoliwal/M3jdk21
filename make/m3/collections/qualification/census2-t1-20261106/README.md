<!-- SPDX-License-Identifier: Apache-2.0 -->
# Census pass-2 group T1: lane contracts on existing owners (P2-144, P2-145, P2-146)

Census pass 2 (S:/m3-scratch/eb-conv/census2, `LEAVES_PROPOSED.tsv` rows P2-144..P2-146, com.synexia develop
`d5ad5090d87e`). The three crates' Java product postimages are already in the owners (census `APPLIED_SUPERSET`), so
the leaves are tests only: the standalone contracts and their JUnit wrappers are relocated by recipe and run on the
unedited owners. Census rows re-checked on the base: `CENSUS_RERUN.tsv`.

| leaf | crate | row | verdict | evidence |
|---|---|---|---|---|
| P2-144 | m3-lane-partition3-20261027 | `M3LanePartition3PreparedContract` + `M3LanePartition3PreparedTest` | PORTED (tests) | LC #75 three-way partition: `partition3Prepared` = `partition3` (exact permutation, packed bounds), refusals before mutation; checks=978782, donor = relocated stdout IDENTICAL |
| P2-144 | m3-lane-partition3-20261027 | `M3LaneAlgorithms` postimage | ALREADY | APPLIED_SUPERSET 31/31 |
| P2-145 | m3jdk21-masked-gather-20261026 | `M3MaskedGatherContract` + `M3MaskedGatherTest` | PORTED (tests) | fused masked gather per `M3IndexMaskCompactor.Backend`; checks=976328, IDENTICAL |
| P2-145 | m3jdk21-masked-gather-20261026 | `M3LongAddressKernel` postimage | ALREADY | APPLIED_SUPERSET 23/23 (the owner adds validate-before-allocate guards) |
| P2-146 | m3jdk21-lc239-sliding-max-20261027 | `M3LaneSlidingMaximumContract` + `M3LaneSlidingMaximumTest` | PORTED (tests) | LC #239 window maximum indexes vs an O(n*k) reference; checks=10215, IDENTICAL |
| P2-146 | m3jdk21-lc239-sliding-max-20261027 | `M3LaneAlgorithms` postimage | ALREADY | APPLIED_SUPERSET 57/57 |
| P2-144..146 | all three | JNI exports in `m3_long_address_kernel.c` | HOLD_NATIVE | master exports `gatherNative` only; the four crate exports live in the retained variant `m3/qualification/master-landing-20261028/retained-variants/pr1882/.../m3_long_address_kernel.c` (blob `055eee1f1194`); the contracts run the Java paths and assert the required-native refusal |

The relocated files equal the uncompiled retained variants of PR #1882 in `m3/qualification/master-landing-20261028`
except for the copyright line and the anchor. No main source, native source or pom change.

Files: `relocate.py` (RELOCATE_CENSUS v5, unchanged copy of `qualification/census-g7-20261106/relocate.py`; lineage
`recipe/entryless-lane-kernel-20261106/relocate.py` v3), `RELOCATION_MAP.tsv`, `PROVENANCE.tsv`, `differential.sh`
(donor contract on the donor owner postimage vs relocated contract on the module owner, byte-identical stdout),
`GATES.tsv`, `CENSUS_RERUN.tsv`, `RECIPE_MEMORY.tsv`.

Re-run: `python -I m3/collections/qualification/census2-t1-20261106/relocate.py --donor-git <com.synexia clone> --donor-rev d5ad5090d87ef8477a56317e60ac0d29c65029f7 [--check]`.
