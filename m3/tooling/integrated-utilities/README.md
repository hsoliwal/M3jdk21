<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Integrated M3JDK21 utility recipe receipt

The implementation is owned by the existing JDK packages in `java.base`. The Apache-licensed Maven/OpenRewrite project in `recipe/` is an exact authoring and proof mirror of canonical Synexia commit `4de06d536308121b64bbe714096f7cad6bc0b908`; it is not a runtime dependency.

`EXPORT_PROVENANCE.json` seals every mirrored source file and the actual SDK Result manifest. Runtime files were copied from verified SDK Results after the engine checked every preimage and its real second run reached a fixed point. Original public APIs and implementation atoms are recorded in the integration contract packets. Fix a canonical template or recipe and regenerate; never hand-edit received product sources.

## Review and verification

Start with `m3/runtime-integration/integrated-collections/README.md`, `m3/runtime-integration/integrated-utilities/README.md`, and `m3/docs/INTEGRATED_UTILITY_COVERAGE_20261018.md`. The module README explains Maven test, serial `--emit`, and received-tree `--check` commands. Product verifiers execute Java 21 patch-module and JNI checks separately from recipe tests.

The catalogue inventories all 70 pinned source modules and 227 unqualified exported packages with representative API owners. It does not claim that every type or operation has been transformed. Full M3JDK21 image, platform and promotion gates remain pending; the ordered receiving phase is unchanged. Performance receipts retain slower and rejected candidates and justify Java-default mask routing, not a general speedup.

The target baseline used for this completed pass is `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. Later source changes require a new admitted per-file recipe pass and cannot be accepted by silently weakening its fingerprints.
