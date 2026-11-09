<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Qualified compiletime program precomputation

Canonical convergence commit: 12dfe09beb197223973788b9393ad559c342ef6e. Each recipe below is an exact source mirror; sdk-owner contains the four canonical OpenRewrite owners bound by that recipe qualification.

- [escape-recipe](escape-recipe/README.md): [m3-program-escape-precompute-20261017](https://github.com/hsoliwal/com.synexia/tree/12dfe09beb197223973788b9393ad559c342ef6e/synexia-openrewrite-recipes/recipe-crates/m3-program-escape-precompute-20261017).

Run Maven in a recipe mirror with Java21 and -Drecipe.owner.root=/absolute/path/to/that/mirror/sdk-owner. Its README gives the contract profile and serial receiving commands. Actual five-test SDK Results produce target/candidate; no build binaries are included.

The source comparison used frozen d47a4b4fdb8e3794ec56768b06aa9e23730c6b8e preimages. The exact [current-tree proof](current-tree/RESULT.json) records the receiving commit, all compiled source digests and commands; its [evidence inventory](current-tree/EVIDENCE_FILES.tsv) retains the literal logs. The [receiving successor proof](current-tree/successor-e913/RESULT.json) records the complete 146-source cohort on e913ab89056657eca4009678cfe9a657c020e79b; the earlier 143-source packet remains intact. The copied catalogue verifier supports repeated --recipe options for these mirror paths. Existing broader custody holds and resource tradeoffs remain visible. This handoff does not advance the global receiving phase or claim a new JVM image, CDS archive or GPU run.

GitHub commit/tree provenance was supplied by the external readback receipt recorded in CANONICAL_HANDOFF.json. The emitter checks every copied source blob against that receipt; it does not authenticate arbitrary JSON as a remote response.
