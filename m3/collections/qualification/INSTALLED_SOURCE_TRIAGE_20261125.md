<!-- SPDX-License-Identifier: Apache-2.0 -->
# Installed-source triage: exact source inventory review (November 25, 2026)

**Read-only / HOLD.** The existing 13-stage source-receipt audit remains
unchanged, fail-closed and red until source-by-source ratification. This change
**does not** replace historical receipts, alter Java code, update digests, or
authorize moves from matching names alone.

The actual [failed M3JDK21 run 45580086904](https://github.com/hsoliwal/M3jdk21/actions/runs/45580086904),
[artifact 15005857965](https://github.com/hsoliwal/M3jdk21/actions/runs/45580086904/artifacts/15005857965),
contained an exact source-inventory JSON for checkout
`96ad83d1e5eeedb9fa9f9139ff2f05c4e3bd6b7d`.
Its JSON SHA-256 is
`6bbc5b0d2f16f2e09fca45f4d4fb4fa33591e66af233b0af483e3b963d6e7363`;
the original installed source manifest SHA-256 is
`88ee9ceaa346a8fc2a24a84569ba852f5ad450dea26fe414d0095c31395272f7`.

## Executed baseline classification

| Read-only bucket | Review units |
|---|---:|
| Source SHA-256 changed in place (retains old/new bytes digests) | 120 |
| Historical Mat-prefix name-only move candidates | 103 |
| Additional name-only move candidates | 7 |
| Unresolved missing/possibly removed source | 1 |
| Unlisted, unpaired present Java sources | 251 |
| **Distinct review units** | **482** |
| **Underlying source-change records** | **592** |

The extra seven candidate name pairs are:
`MatAdaptiveIntIntMap → AdaptiveIntToIntMap`,
`MatAdaptiveLongLongMap → AdaptiveLongToLongMap`,
`MatIdIdMap → IdToIdMap`,
`M3ArrayStack → ArrayStack`,
`M3NodeCachingLinkedList → NodeCachingLinkedList`,
`M3TreeList → TreeList`, and
`M3CountedCatalogue → CountedCatalogue`.
All are **UNVERIFIED_SOURCE_SEMANTICS**, not accepted moves.
The unresolved historical owner is
`jdk/internal/m3/collections/StrategyHashMap.java`.

## New deterministic review-only workflow

`installed_source_triage.py` parses the **current actual audit JSON**
and checkout SHA, verifies all path and SHA-256 identities, source receipt
category counts, the original 12-transition custody chain, and the
reversible accounting of each old/current path.
It creates `source-review-queue.tsv`,
`candidate-moves-unverified.tsv`, `source-review-summary.json`,
and a short review README. All records remain unapproved.

`.github/workflows/m3-collections-installed-source-audit.yml` now runs the
triage self-test, and **only when the original installed-source audit fails**
builds the case queue and uploads it alongside the existing unmodified
failure inventory. The original red audit step is not masked, suppressed,
waived or replaced; successful triage creation does **not** turn it green.
On missing or invalid audit inputs, triage fails closed as well.

Rerun the original audit against the current source checkout, review
individual Java API/ABI/source-license/history changes and atomized
OpenRewrite recipes, then require an actual successful audit run.
The historical snapshot alone does not prove the present master is clean.

No source-built JDK/JTReg, OpenRewrite/Maven, JNI or JMH acceptance is
claimed by this read-only diagnostic enhancement.
