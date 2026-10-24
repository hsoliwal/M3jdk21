<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Owned Spring LinkedCaseInsensitiveMap packet, successor 20261024 (D2 step 0)

The 20261018 packet stays byte-exact as history. Its `verify.py` binds 221 inputs, and 46 bound core sources plus
both POMs have changed on master since #1269, so every hosted run of `m3-owned-spring-case-insensitive-map.yml`
failed at the first custody check. In addition, master's `module-info.java` now requires `com.m3.arrays`, which the
workflow's sparse checkout does not contain.

This successor is self-contained:
- Byte-identical copies: `verify.py`, `verify_module.py`, `compare.py`, `proof-summary.json`, the nine frozen
  fixtures and launchers, both hook dirs, `recorded/` and `archive/proof-plan/BASELINE_PARTITIONS_V2.json`.
- `run_contracts.py` is copied with two changes:
  - the bound core is the compile closure of `SpringCaseInsensitiveMapMaskProjectionProof`, so no
    `module-info.java`, no `M3LinkedHashMap` and no `m3/arrays`;
  - the volume reserve is 1 GiB beside the 512 MiB task budget. Hosted runners keep their own 8.5 GiB assertion.
- `source-bindings.json` binds the closure, the native compactor, this packet, the 20261018 recipe crate, the owner
  module and the workflow, all at current master bytes.
- The workflow's `M3_Q` points here.

Step 0 changes no body. The owner stays blob `8fbaa772` (SHA-256 `450e3f70...`). The proof below shows that the
packet is green again on the current base before the casefold change (D2 step 1) lands.

Local proof (JDK 21.0.9, GCC 15.2.0 MinGW for the native compactor, which builds with `-Werror`):
`run_contracts.py --native-library-dir`: PASS, 18 commands. These cover the strict core/owner/fixture compiles,
provider and owned protocol runs, surface, constructors, getter, equality, Java and native projections, and
`compare.py` against the sealed recorded outputs. Custody: 53 sources. The bound core is one file,
`M3IndexMaskCompactor.java`, the only core class the projection proof loads.
