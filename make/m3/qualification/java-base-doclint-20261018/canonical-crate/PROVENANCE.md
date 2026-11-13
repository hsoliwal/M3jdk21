<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Provenance and licensing

Target source: public `hsoliwal/M3JDK21` commit `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. The before templates are exact public source bytes. The after templates retain their original GPL/OpenJDK notices and contain the bounded first-party changes described by the contract. Apache licensing of the recipe and proof harness does not relicense those Java source payloads.

The canonical transformation uses the existing Synexia `M3HashPinnedJavaSnapshotRecipe`, `M3ScopeInference` and `M3EditScope` owners from source baseline `289e32f130c9c8e4a1cf0edc1066327d699393bf`. These dependencies are referenced at their canonical paths; their contents are neither republished as new ownership nor replaced by a second engine. New recipe classes, tests, verifier scripts and documentation use Apache-2.0 with the full Synexia attribution.

This build prerequisite requires no third-party algorithm copying. The implementation choices follow the actual target method bodies and javac diagnostics. Fresh recovery qualification used Eclipse Adoptium Temurin 21.0.12.1+1 and Maven 3.9.9. Receipts retain executable and source hashes. A canonical publication commit must be resolved by the publisher before receiving PR links are finalized.
