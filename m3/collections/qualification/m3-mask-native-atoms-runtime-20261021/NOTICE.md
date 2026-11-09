<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Target packet attribution

Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
Target-specific verification, receiving metadata and original documents use
Apache-2.0. Product source keeps its existing per-file notices and license.
`SYNEXIA_NOTICE.md` is an exact retained copy of the canonical native-only
packet's notice. The separate `m3/collections/LICENSE` remains authoritative
for this component; no OpenJDK source is relicensed.

The canonical Synexia recipe, source postimages and source proofs are pinned in
`RECEIVING.json`. The target verification driver adapts the already qualified
Synexia emitted-output driver, with its immutable source path/hash and license
recorded there. It adds selected-source application and target path validation;
it does not transform source or replace the canonical recipe. Existing oracle
and regression implementations are reused unchanged.

Logs and execution receipts retain original observation paths and byte hashes.
The target packet is bounded review evidence, with full receiving acceptance
and resource/platform matrices pending. Challenge and editorial pages supply
problem references only; no externally licensed solution body is copied here.
