<!-- SPDX-License-Identifier: Apache-2.0 -->
# Bounded M3 path-rank receiving report

## Files changed

Additive candidate only: one Java21 `M3IndexPathRank` immutable rank/select model,
three target test sources, a sealed OpenRewrite crate with four exact Java postimages,
a JUnit recipe replay/refusal test, one named YAML recipe, one category catalogue,
a verification script, this receipt set, and an exact-head CI workflow.

## Exact delta

Existing `M3IndexPathMasks`, `M3BitLane28`, JNI C sources, JDK `java.util`,
public/protected JDK signatures, object/GC reference ownership, and prior
historical licence/provenance files are unchanged. No existing target file is
deleted/replaced. Existing live filtered/sorted view APIs are not rewritten.

## Verification executed locally

PASS: local Java21 `javac --release 21 -Xlint:all -Werror` compiling core and
standalone regression, then deterministic Java oracle: 900 randomized paths,
36,476 rank checkpoints, overlapping-mask rejection, negative and duplicate IDs,
caller mutation after capture, 64/128/256 boundaries, and 10 randomized offset
windows per path. PASS: strict 4-target SHA-256 template and output preflight.

## Exact blockers

Maven and pinned OpenRewrite dependencies unavailable in this isolated local
runtime; real OpenRewrite transform execution, JUnit and native C/JNI parity
were NOT_RUN. The new workflow requests them on exact PR head. Full reactor,
OpenJDK/jtreg, CPU+native allocation/performance and runtime-phase promotion
remain NOT_RUN/BLOCKED. The source-seal script is not an OpenRewrite substitute.

## Artifact paths

`m3/collections/qualification/m3-index-path-rank-20261017/`,
`m3/collections/algorithm-catalog/index-path-rank-20261017.tsv`,
`m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-index-path-rank-20261017/`.
