<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Fixed arena GNU source guard proof receiver

Canonical source: Synexia commit `d8426c06f67683763a8ddfc8bd99620500a0f536`, tree `94a5fe6699f514f83182563ca61dc7892a36b194`.
Canonical crate: `synexia-openrewrite-recipes/recipe-crates/m3jdk21-fixed-arena-gnu-source-20261018`.
Named recipe: `com.synexia.m3jdk.FixedArenaGnuSource20261018`.

The [canonical README](https://github.com/hsoliwal/com.synexia/blob/d8426c06f67683763a8ddfc8bd99620500a0f536/synexia-openrewrite-recipes/recipe-crates/m3jdk21-fixed-arena-gnu-source-20261018/README.md) is the reproduction entry point.
Run its recipe and verification commands in the canonical Synexia checkout,
where the existing engines and source dependencies remain available.

## Mirror custody

This directory contains selected documentation and actual proof receipts copied
byte-for-byte from source A. `SOURCE_FILES.tsv` maps each mirrored file to its
immutable canonical file URL, SHA-256, Git blob, and byte count. The selected
source bytes were checked against the source-A publication receipt, whose
selected paths and blobs were read back from immutable Git trees.

The `canonical-crate/` subtree is a selected mirror, not a runnable recipe
checkout. Recipe engines, POM/YAML definitions, verification scripts and helper
sources remain canonical. No new runtime dependency or production source is
introduced by this receipt directory. Canonical payloads retain their exact
original bytes and license/attribution notices; this source-home narrative does
not relicense them.

## Evidence scope

The canonical packet records six SDK groups and the strict whole-owner
macro-definition matrix for the existing
`src/java.base/linux/native/libjava/M3PrimitiveFixedArena.c`. The original owner
with an external `_GNU_SOURCE` definition is expected to fail on the duplicate
definition. The original without that definition and both guarded candidate
configurations compile without diagnostics and produce three identical object
files. The preserved result and admission receipts bind the exact original
and emitted source hashes.

This evidence covers the conditional preprocessing guard. It changes no JNI
signature, mapping policy, lifetime, error path or runtime instruction in the
qualified configurations. The frozen admission receipt's image-build field
remains `PENDING`; full-image results are supplied by the parent integration
evidence, not inferred or rewritten in this mirror. The canonical README and
catalogue preserve the existing GPL/ClassPath runtime-owner provenance and
Apache-2.0 recipe/tooling distinction.

The receiver baseline for the source transformations is
`2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. Product source changes and image
execution are governed by their separately retained SDK and integration
receipts. Copying this proof mirror does not rerun a qualification, promote a
runtime phase, or establish RAM latency, physical residency, iOS performance,
or execution on an untested platform.
