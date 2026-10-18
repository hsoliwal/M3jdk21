# Receipt-only closure — 2026-10-17 run (evidence only)

Leaf `RCPT-receipt-only-closure` of run `20261017-synexia-to-public`.
Input: leaf S `GAP_STRING_PRECOMPUTE.tsv` — 17 string/regex units whose owner class already
exists on pinned M3jdk21 master (`refs/m3run/m3jdk21-master-20261017` = `970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`)
but which had no receipt under `m3/compatibility/` and no crate pin.

## What these receipts ARE

- Schema `M3JDK21_SYNEXIA_RECEIPT_ONLY_CLOSURE_V1`, one per unit:
  `synexia-<unit>-receipt.json`, indexed in `RECEIPT_CLOSURE_LEDGER.tsv`.
- Evidence rows only: owner file path + git blob + sha256 as found on the pinned master
  (`before: PREEXISTING`), and the Synexia canonical unit (path, tree sha, key donor files + blobs)
  at the Synexia pin (`refs/m3run/synexia-develop-20261017` = `11c94feb18155a53cc213708c353791132edc4a7`).
- Every field that could not be verified mechanically is marked `UNVERIFIED`
  (pull-request numbers were not searched; owner association is the leaf-S token match where noted).

## What is NOT claimed

- No phase advance, no receiving, no runtime claim, no gate result: `gates.evidenceOnly = NOT_RUN`.
- No owner source file was changed; the branch touches `m3/compatibility/` and `m3/docs/` only.
- A receipt does not assert the Synexia unit PRODUCED the master content. Where the unit carries
  no hash-pinned manifest covering the master content, the receipt says `RE_SEAL_REQUIRED`
  (15 of 17); no manifest was invented.
- Drift is marked, not fixed: `m3jdk21-string-tuple-cursor-20261014` (M3String.java master
  sha256 differs from the unit manifest after-sha256) and `m3jdk21-dotstar-receiver-20261014`
  (String.java and workflow blobs differ; its two manifest ADD files are ABSENT on master).
- Only `m3jdk21-adaptive-projection-20261016` is sealed: its manifest after-sha256 values equal
  the pinned-master sha256 of `M3StringProjection.java` and `M3StringProjectionTest.java`.

## The 17 rows

| unit | owner(s) on pinned master | residue |
| --- | --- | --- |
| m3-compiletime-string-facts-20261013 | M3StringFacts | RE_SEAL_REQUIRED |
| m3-regex-v2-fused-facts | M3StringFacts | RE_SEAL_REQUIRED |
| compiletime-text-facts-v1 | M3StringFacts | RE_SEAL_REQUIRED |
| m3-regex-matcher-precompute-20261016 | M3StringRegexRowPrecompute | RE_SEAL_REQUIRED |
| m3-regex-subset-dp-20261016 | M3StringRegexDp, M3RegexDpPlan | RE_SEAL_REQUIRED |
| m3-tq-image-receive-20261014 | M3TQ | RE_SEAL_REQUIRED |
| m3jdk21-adaptive-projection-20261016 | M3StringRepresentationPlan, M3StringProjection | NONE (manifest matches master) |
| m3jdk21-composite-search-cursor-20261014 | M3StringSearchPrecompute | RE_SEAL_REQUIRED |
| m3jdk21-dotstar-receiver-20261014 | M3DotStarRelations | DRIFT + ABSENT manifest files |
| m3jdk21-string-tuple-cursor-20261014 | M3StringTuple, M3String | DRIFT (M3String.java) |
| regex-native-start-facts | M3StringFacts | RE_SEAL_REQUIRED |
| m3-javac-regex-expression-facts-20261014 | M3StringFacts | RE_SEAL_REQUIRED |
| m3jdk21-rx7-dfa-20261014 | M3RX7, M3RX7Codec, M3RX7Image | RE_SEAL_REQUIRED |
| m3-horspool-range-safe | M3StringPool (WEAK token match) | RE_SEAL_REQUIRED |
| m3-dotstar-rolling-dp-20261014 | M3DotStarRelations | RE_SEAL_REQUIRED |
| mindex-adaptive-string-view-20261016 | M3StringRepresentationPlan | RE_SEAL_REQUIRED |
| compiler-facts-20261014 | M3StringFacts | RE_SEAL_REQUIRED |

## Residue outside these receipts

- Apex leaves A10..A43 are DELIVERED per the apex run STATUS but have no crate, hash-pinned dir
  or receipt by NAME in either pinned tree (leaf S `APEX_LEAF_MAP.tsv`). No receipts were written
  for them here: without a named crate or manifest there is nothing to pin. They need the
  HIST-JDK PR file lists (re-seal lane), not evidence-only receipts.
- `RE_SEAL_REQUIRED` rows need a successor hash-pinned recipe (house
  `M3HashPinnedJavaSnapshotRecipe` lane) sealing the pinned-master owner content; that is a
  delivery leaf per unit, out of scope here.
