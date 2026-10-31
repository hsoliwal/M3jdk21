<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Synexia-owned gate receipts

Data-only receipts transferred from `hsoliwal/com.synexia` for the gates whose
owner in `release-contract.json` `execution_evidence.gate_owners` is Synexia
(currently `M3_MAVEN_CONTROL_REACTOR`). Layout: one directory per gate holding
`<GATE>.receipt.json` and its raw artifacts, as written by the Synexia emitter
`synexia-openrewrite-recipes/recipes/m3jdk21-release-evidence-20261030/emit_control_reactor_receipt.py`.

`collect_release_evidence.py assemble` imports this directory, re-verifies each
artifact SHA-256 and copies it into `queue/current/<GATE>/`. Nothing here is
executed by M3JDK21; the `canonical_recipe` and `target` pins inside a receipt
are the reviewer's provenance, not runtime acceptance.
