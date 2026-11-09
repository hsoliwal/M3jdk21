<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Canonical integrated M3JDK21 utility recipe export

The product APIs belong directly to the existing JDK packages. This directory
retains the canonical authoring and qualification record; it adds no runtime
dependency on Maven or Synexia.

Canonical source: [Synexia token recipe PR #12449](https://github.com/hsoliwal/com.synexia/pull/12449),
commit `0f7659d5f55166beaa8e57a440b5db758ea6ee73`, module
`synexia-openrewrite-recipes/recipes/m3jdk21-integrated-utilities-20261018`. Every one of the 214 files under `recipe/` is an
exact source-commit mirror with a SHA-256 and Git blob identity in
`EXPORT_PROVENANCE.json`.

## Serial source transitions

The core collection, mask JNI, IO, stream and reflection pass is independently
reviewable in [M3JDK21 PR #1435](https://github.com/hsoliwal/M3jdk21/pull/1435)
and [Synexia PR #12441](https://github.com/hsoliwal/com.synexia/pull/12441).
Its exact export is retained in target commit
`2717d156ad2aba73dfdfcfc6a58b59a82371798e`.

The current token pass receives that exact target commit, seals 490 paths, emits
13 actual OpenRewrite Results and preserves 477 KEEP inputs. The source-bound
receiver verifies all 490 postimages; final actual SDK `--check` and its second
run return zero changes. Historical core receipts remain under `qualification/`.

Use the Java 21 Maven commands in [the canonical module README](recipe/README.md).
Make corrections in the canonical recipe candidates and templates, rerun the
actual SDK, and receive the admitted Result bytes. Do not hand-edit generated
product sources or turn the proof mirror into another authoring owner.

## Qualification boundaries

The received token classes pass strict Java 21 compilation, doclint, and 522,729
contract checks in each of mixed and interpreter modes. Both production files were
recovered with their exact earlier hashes after transient staging was lost; the
included token tests and receipts are fresh executions. Unavailable historical
token receipt files are not recreated.

Full M3JDK21 image, complete tier/platform gates, ordered receiving promotion and
stable performance thresholds remain pending. The current STRING-first receiving
phase is unchanged. Core snapshot measurements do not establish a general
speedup; Java mask selection remains the default.
