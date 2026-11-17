<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 Enhancement Proposals

This directory is the **M3JDK21 product-facing design home** for 86 internal
enhancement proposal drafts: [70 cross-language capabilities](INDEX.md) and
16 shared primitive, compiler, `const`, JNI, storage and runtime prerequisites.

These are **M3JEP** identifiers, *not* numbered, submitted, targeted or approved
OpenJDK JEPs. They describe an additive M3JDK21 superset; no draft itself
installs a language extension or changes standard Java 21 behavior.

## How to read these proposals

- [Full index](INDEX.md) lists each stable proposal identity and first receiving stage.
- [Data-only proposal registry](PROPOSALS.tsv) maps capability IDs to reviewable documents.
- [Individual proposals](proposals/) document motivation, semantics, Java 21 safeguards,
  non-goals, dependency gates, alternatives and compatibility oracles.

The product receiving order remains **String → arrays → collections →
AST/compiler → remaining**. M3 syntax is opt-in; original Java classfile,
reflection, JNI, Unicode, exception, threading and API contracts remain valid.
An unimplemented candidate remains explicitly **Draft / HOLD**.

## Source custody and release boundary

The first-party designs are copyright 2026 Hitesh Soliwal and Contributors to
the Synexia Project, Apache-2.0. Foreign language specifications are cited as
*ideas*, not imported compiler/runtime implementation code.

Canonical OpenRewrite source templates, source SHA-256 manifests, deterministic
generation, fixtures, proof reports and correction history remain in
`hsoliwal/com.synexia` under its cross-language compiler recipe crate. They are
deliberately **not replicated here**. This directory contains only the executed
documentation postimages and the navigational registry.

Before a design changes `src/`, `make/`, the VM, or a JDK module, it needs a
separately qualified source-sealed recipe and M3JDK21's own compiler, jtreg,
JNI/GC/native, linkage, installed-image and CI acceptance. None of those
product feature gates is established by these drafts.
