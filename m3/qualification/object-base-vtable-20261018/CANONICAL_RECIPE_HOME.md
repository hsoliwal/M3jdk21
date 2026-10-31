<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Canonical Object bootstrap recipe

Canonical source: Synexia commit `d8426c06f67683763a8ddfc8bd99620500a0f536`, `synexia-openrewrite-recipes/recipe-crates/m3-object-base-vtable-20261018`.

Named recipe: `com.synexia.m3jdk.ObjectBaseVtable20261018`; Maven coordinate: `com.synexia.verification:m3-object-base-vtable-recipe-proof:1`. The existing `M3HashPinnedTextSnapshotRecipe` engine remains at its canonical owner path.

This receiver mirrors the exact crate and its fresh proof receipts so reviewers can examine the generated public-source change. Run Maven from the canonical Synexia checkout, where its existing engine dependencies are available. The recipe changes only `src/hotspot/share/classfile/classLoader.cpp` against target baseline `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

Fresh gates: six SDK tests, ten source-context seals, actual strict whole-owner compilation and one-byte5to6 difference, normal and interpreted actual-image fixture. Full image packaging/CDS is separately recorded by the integration PR. See `canonical-crate/README.md`, `CONTRACT.md`, `PROVENANCE.md` and `proof/recovery-20261019`.
