# RL-fr exact release-date source-contract repair — 2026-10-20

Owner: M3JDK21 lexicon intake; source recipe owner: Synexia PR #12529.

## Failure
GitHub Actions run 39354880993 / job 118407283518, Java21 Maven offline recipe proof, failed at M3HfLexiconIntakeProof.java:1038 because the discovery row `rl-fr-2025-03` omitted the expected exact date.

## Source authority
ATILF lists RL-fr last updated on **7 March 2025**:
https://lexical-systems.atilf.fr/en/downloads/
The resource remains REFERENCE_ONLY. No corpus is ingested or relicensed.

## Exact source receipt
- Path: `m3/lexicon/hf-intake/src/test/resources/com/m3/lexicon/hf/language-resource-discovery.tsv`
- Input Git blob: `ed22aa162eb90e038b9be9b4e30624b704d31a91`
- Corrected Git blob: `4d20312d8e1c0f3e8eb0e514b1921ee46ef9ff90`
- Only RL-fr column 4 changes, by inserting `(last updated 7 March 2025)`.
- Original 226 data rows, nine columns per row; IDs, morphology, semantic relations, attribution, rights, other rows and URLs retained.

The 230-record PR #1519 fixture was independently source-sealed from `fd56ff97bf8c3dc80bf40ff27da102210dd63f76` to `c2d3599a835fa70e85873f196a4c6c466655d619`, with the additional four Japanese/Chinese records unchanged. Its former head was backed up under `preserve/pr-1519-before-rlfr-date-20261020`.

## Boundaries
Data proof and focused Maven CI are required. No claim of full JDK/JNI release qualification, corpus distribution, or standalone runtime improvement.
