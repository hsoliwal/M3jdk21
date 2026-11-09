<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 release CI failure triage — 2026-11-07

**Status: release HOLD.** This is a historical, exact-run diagnostic of failed
PR checks at review commit `e01d7556e5c11efcb8c1fcd6c77406cf69c6e5bf`.
It is **not** evidence that any subsequent commit passed. Never rewrite the
failure witness as a PASS without an actual exact-head, full-run receipt.

| Failed workflow/run | Observed failure | Disposition for release |
| --- | --- | --- |
| [Current core contract matrix](https://github.com/hsoliwal/M3jdk21/actions/runs/42356173984), original-core | `ValueError: source identity drift: m3/build.sh` | **Addressed in candidate** by restoring `m3/build.sh` byte-for-byte to the `master` Git blob and placing four additional tests in `m3/release/run_extra_core_contracts.sh`. Must rerun to verify |
| [Current core contract matrix](https://github.com/hsoliwal/M3jdk21/actions/runs/42356173984), wordrows-native | `ValueError: native source drift: m3/lexicon/run-wordrows-proof.sh` | **Outstanding**; repin only through the reviewed source/recipe and native-custody admission, not by deleting the check |
| [Public JDK naming contract](https://github.com/hsoliwal/M3jdk21/actions/runs/42356173915) | `test_new_donor_imports_in_jdk_owner_refused`: missed legacy `jdk.internal.m3.indexstring` donor import | **Addressed in candidate** by extending the lexical new-donor guard; real naming tests must rerun |
| [M3 JDK naming standard](https://github.com/hsoliwal/M3jdk21/actions/runs/42356173940) | Historical `test_recipe_memory_rows_name_a_recipe_and_a_fixture_that_exist` crash splitting prose-only guards | **Guard parser repaired in merged [#2765](https://github.com/hsoliwal/M3jdk21/pull/2765)** with original `RECIPE_MEMORY.tsv` bytes preserved and three exact historical binding corrections. The source-only 24/24 naming and 6/6 reseal tests passed at [run 42366163089](https://github.com/hsoliwal/M3jdk21/actions/runs/42366163089). **However, its whole-tree fixed-point gate still failed**: 35 moves, 56 rewrites. It remains a release HOLD until source-sealed ownership and exact-current-head replay are qualified. |
| [Current core runtime proof](https://github.com/hsoliwal/M3jdk21/actions/runs/42356173991) | Joined-range bootstrap expects `row['path'] in transition` and raises `AssertionError` | **Outstanding**; reconcile historical transitions and exact source seals |
| [Merged recipe atom preservation](https://github.com/hsoliwal/M3jdk21/actions/runs/42356174017) | `M3_PRODUCT_SOURCE_RECIPE_HANDOFF_REFUSED invalid immutable first-party recipe custody` | **Outstanding**; source provenance and recipe export need admission |
| [Historical Joined reference replay](https://github.com/hsoliwal/M3jdk21/actions/runs/42356173934) | Historical qualification gate failed | **Outstanding**; see uploaded run evidence, preserve original source pins |
| [Standalone references](https://github.com/hsoliwal/M3jdk21/actions/runs/42356173919) | Ledger and lineage-reference guard failed | **Outstanding**; inspect exact changed references without discarding history |

**New candidate scope:** the restored foundation build and added naming-guard
regression are **proposed remediations**, not green results. Separate tests
must run in the order `m3/build.sh` followed by
`m3/release/run_extra_core_contracts.sh`, then relevant native, Java21, JNI
and installed-image gates. CI can be queued/cancelled; such states are
**PENDING**, never PASS.

A newer `master` release-contract update now **reconciles the preview
branch name** across the machine contract, notes and checklist. Verify that
[`release-identity.json`](audit_release_identity.py) remains consistent
after merging this PR. Changed-atom JaCoCo line/branch coverage is an
additional mandatory gate and remains PENDING. The current
[`AGENTS.md`](../../AGENTS.md) and
[`release contract`](release-contract.json) agree that canonical
recipe execution belongs to Synexia, while this product must independently
qualify its JDK/JNI/runtime acceptance. The missing source-bound execution
receipt and historical recipe migration remain open HOLDs. A green isolated
test cannot override these hard policy gates. For the separately failed
whole-tree name fixed point, retain the
[original audited evidence](NAMING_FIXED_POINT_EVIDENCE.md).
