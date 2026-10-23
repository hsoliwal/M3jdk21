<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# MOP-RCM/1.0 bounded profile

Profile: `m3-integrated-refinement-20261021/1.0`. Lifecycle is closed to RECORDED /
PENDING. The helper is adapted solely by profile identity from canonical PR12605
at `b6c35b9a1f528dd9e5a3d9f6f4c51ca360db43d7`; its retained independent conformance
fixture and unchanged SealHash owner are under `src/test/java`. No runtime engine
or dependency is added to M3JDK21.

The closed schema, recursive sorted ASCII keys, strict UTF-8 scalars, signed int64
numbers, lowercase control escapes, ordered arrays and sorted unique parent/digest
sets are enforced by that helper. Unknown fields, duplicate keys, floats,
surrogates, noncanonical storage and invalid or missing artifact/history pins
refuse. Canonical bytes have no BOM, optional whitespace, or trailing newline.
Their SHA-256 names each immutable event. The existing create-new hard-link
publication protocol refuses unsupported filesystems and differing existing bytes.

An event hash proves byte identity and the validator proves the declared structural
profile. Neither proves an arbitrary command executed. Every claimed gate needs
its actual retained command output and source pins. Fresh strict Java 21 compile
and execution passed 129 conformance checks in
`proof/memory-conformance-001/RESULT.json`. The separate combined Maven gate is
recorded in its own subsequent receipt.
