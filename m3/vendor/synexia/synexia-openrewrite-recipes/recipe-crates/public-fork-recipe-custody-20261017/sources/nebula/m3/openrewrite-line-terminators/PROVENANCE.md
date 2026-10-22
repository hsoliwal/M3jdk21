# Qualified OpenRewrite source provenance

OpenRewrite 8.90.4, commit 398a6349648ca802aa050bdb6fe45a99395dce47, Apache-2.0. Three source classes are adapted for CR comment termination and Javadoc blank-line cursor alignment. `original/` preserves exact donor sources; `candidate.patch`, `source.sha256`, `dependency.sha256.tsv`, and POM checksum conditions bind the reviewed originals, adaptations, tests, and upstream artifacts. Original public APIs are unchanged.

Qualified coordinate: com.synexia.recipe:openrewrite-line-terminators:8.90.4-m3-f3e4bfe19654. This owner module derives from the independently qualified Synexia parser slice (15 passing tests; full consumer 21 passing tests; owning InventoryCli 1756 compilation units, zero failures). The only packaging change is the default source root `${project.basedir}/../..`; no source/test byte changes.

`third_party/LICENSE` retains OpenRewrite Apache-2.0; `third_party/LICENSE-SLF4J.txt` retains the SLF4J MIT notice. Pinned binary and source artifacts are repo-owned, verified by `third_party/SHA256SUMS`. Upstream dependencies remain official Maven coordinates; no upstream jar is overwritten. Build using `mvn -f m3/reactor.xml verify`.

The owning RoundScale test admits exactly two unchanged source identities: canonical Git LF (9350 bytes, SHA-256 09fa50c810f66c351000b350588d36716fd02d93657f626ce0fa1498f0a109a8) and Windows checkout CRLF (9687 bytes, SHA-256 2b8574dbbde69addfe473d96a6767cdf635ac09b2ac8b8cfcba817b237ee4c32). Neither input is normalized; actual parser print and disk bytes remain identical. This packaging test adjustment is sealed in the module POM; all three patched production-source hashes remain unchanged.

## Javadoc tag boundary refinement, 2026-10-05

Current coordinate: `8.90.4-m3-javadoc-d5e5b33b8292`. The existing throws visitor now selects the exact
`ThrowsTree.getTagName()` and preserves leading source fragments. The original `source.sha256`
and `candidate.patch` remain immutable predecessor receipts; `javadoc-source-sha256.tsv`,
updated overlay metadata and POM preflight seal this additive refinement. Original donor source,
licenses and earlier regression fixtures remain unchanged. The new immutable Collection donor
and 18 tag/spacing/line-ending combinations are qualified by JavadocThrowsBoundaryTest.

Refinement v2: `8.90.4-m3-javadoc-e898332e8f88` preserves CR-only Javadoc line endings and
`##fragment` reference suffixes while retaining the typed qualifier. Candidate v1 failed
lossless printing on those domains and is retained in the canonical task crate.
