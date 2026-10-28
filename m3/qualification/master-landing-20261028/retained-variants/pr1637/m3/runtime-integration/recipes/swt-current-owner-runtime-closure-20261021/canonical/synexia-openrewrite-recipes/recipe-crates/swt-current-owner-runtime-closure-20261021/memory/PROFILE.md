<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Current-owner correction memory profile

Profile `swt-current-owner-runtime-closure-20261021/1.0` is closed to `RECORDED/PENDING`. The helper is derived from
published core #12648 with only its profile identity and dated comment changed.
The independent golden-byte conformance test and SHA-256 helper remain exact.
No previous task event history is inherited.

The canonical encoding uses recursively sorted ASCII keys, strict UTF-8 scalar
strings, signed int64 integers and lowercase control escapes. Ordinary arrays
retain order; parent/supersedes digest sets must be sorted and unique. Unknown or
duplicate keys, floats, surrogates, noncanonical storage, invalid history and
missing/mismatched artifact references refuse. Event bytes have no BOM, optional
whitespace or trailing newline. SHA-256 names each create-new immutable event.

The separate actual Java21 helper gate passed 129 conformance checks, including
32 control cases. This tooling is outside the product and recipe runtime. Event
integrity is distinct from runtime behavior, publication and product admission.
