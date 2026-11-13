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

## Step 1: fold fast path, measured and not shipped

The first phase of the brief's FAST mode was built and measured: allocation-free fold probes on the existing two
directories. An ASCII key probes the alias directory by its fold hash (`31*h + lower(c)`) and a fold compare instead
of building `convertKey(key)`. It applied only without `convertKey`/`getLocale` overrides and outside tr, az and lt.
- **Correctness:** `run_contracts.py` passed with it (20 commands, every frozen row equal). The locale oracle passed
  (4,160,000 checks), and a variant without the tr/az/lt gate failed the oracle.
- **Measurement:** JMH (`M3LinkedCaseInsensitiveMapFoldBench`, en) reached 0 B/op instead of 29 B/op, but
  `containsKey` went from 34.8 to 74.1 ns, `get` from 68.5 to 74.5 ns, an absent `containsKey` from 50.2 to 75.2 ns,
  and `put` from 69.3 to 90.1 ns. JDK 21's intrinsic `toLowerCase`, `hashCode` and `equals` beat a char-by-char loop.
- **Decision:** the body stays `8fbaa772`, and the catalogue row is `REJECTED_MEASURED`. The single-table merge
  (brief 4.2) is not measured; it would need a fold that keeps the intrinsics.

Kept as qualification tools:
- `SpringCaseFoldLocaleOracle.java`: owned against Spring 6.2.0 in 10 locales and 34 keys. `run_contracts.py` runs
  it as `locale-oracle`, so it also qualifies the current body.
- `M3LinkedCaseInsensitiveMapFoldBench.java`.
- `PROOF_20261024.json`: the runs and both benchmark tables.
