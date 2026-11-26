<!-- SPDX-License-Identifier: Apache-2.0 -->
# Actual hosted source-ledger triage proof — November 25, 2026

This document is the **executed** CI receipt for the new read-only source
triage on [PR #3450](https://github.com/hsoliwal/M3jdk21/pull/3450),
not a claim that the installed-source manifest has been accepted.

## Hosted run and exact identities

- [GitHub Actions run #45586811660](https://github.com/hsoliwal/M3jdk21/actions/runs/45586811660),
  job `Original 13-stage receipt composition and exact current source differences`.
- Original 13-stage audit `Require current original installed source ledger
  to reconcile without waivers`: **FAILED** (expected HOLD).
- `installed_source_triage.py --self-test`: **PASSED**, one positive five-unit
  fixture and eleven negative/refusal assertions.
- `Build read-only, unverified source review queue on ledger failure`:
  **PASSED**, generated deterministic review material from the original
  unmodified installed-source inventory.
- Artifact upload: **PASSED**, real
  [artifact 15008558877](https://github.com/hsoliwal/M3jdk21/actions/runs/45586811660/artifacts/15008558877),
  93,025 bytes (ZIP download SHA-256
  `6479eb28d5a83bf730fa665f479b6a39ed0bee24856949c1709b3dbc58150f91`).
- Executed source checkout:
  `c0b7224e33aa0fd00e8d3c28344036c34d941a58`.
  Input inventory JSON SHA-256
  `6bbc5b0d2f16f2e09fca45f4d4fb4fa33591e66af233b0af483e3b963d6e7363`.

## Hosted result parity

`source-review-queue.tsv` contains **482 review units**, and
`candidate-moves-unverified.tsv` contains **110 proposed but unapproved
name moves**. Both files match the independent local run **byte for byte**:

| Real hosted output | SHA-256 |
| --- | --- |
| `source-review-queue.tsv` | `358d6e122e4fe4d0879bba5182ea4bd0b7e3493a485fc1662af5495a4bd09772` |
| `candidate-moves-unverified.tsv` | `855f1dafd2c87e12b94b7b3dec61af5eb13dbf0de3fc8952ab3e3807d3643820` |

The hosted summary metadata uses the **newer exact checkout commit**,
whereas the older local artifact records the earlier receiving commit.
All source-change rows, SHA-256 identities and pairings are identical.

### Current reviewed buckets: NONE ratified

- 120 in-place source digest drifts, old/new hashes preserved.
- 103 historical `Mat*` rename-name suggestions.
- 7 additional source-name suggestions.
- 1 historical missing/possibly removed owner with no matched current name.
- 251 unmatched current Java sources.
- **482 review units / 592 original source change records**.

Every rename candidate is labeled `UNVERIFIED_SOURCE_SEMANTICS`.
No source files, old receipts, JDK production Java, JNI declarations or
OpenRewrite rules have been modified by the diagnostic.

## Separate native, installed image and JMH evidence

The installed `IntLaneAlgorithms.java` and receiving sparse jtreg are
already merged through M3JDK21 PR #3447 and byte-identical to the reviewed
Synexia postimages. Independent receiving CI has reported:

- [Real JNI/UBSan run #45580086983](https://github.com/hsoliwal/M3jdk21/actions/runs/45580086983):
  **SUCCESS**.
- [Source-built JDK release image and installed sparse jtreg
  #45580086606](https://github.com/hsoliwal/M3jdk21/actions/runs/45580086606):
  **SUCCESS**, actual jdk.m3.collections JMOD, jtreg `IntLaneSparseRangeJtreg.java`
  passed, four image-backed array self-tests.
- [Source-built JMH #45580086807](https://github.com/hsoliwal/M3jdk21/actions/runs/45580086807):
  **SUCCESS** as an executed 64-case original/reference versus installed
  candidate JMH suite; median candidate/reference ratio 0.0103 and
  worst ratio **1.3466**. The worst two regressions occurred for random,
  high-density 9,000-update workloads at universes 65,535 and 65,536.
  Those workload-specific regressions **warrant further controlled
  retesting**, not a claim of universal performance superiority.
- The **installed-source ledger remains FAILED**, separately and correctly.

The cross-repository canonical Synexia Maven/OpenRewrite `Recipe.run` and
fresh non-skipped Surefire receipt have *not* been verified by this audit PR.
Other repository-level check failures must also be tracked independently.

**Disposition: HOLD for complete installed-source ledger acceptance.
PASS only for producing and byte-validating the new triage evidence.**
