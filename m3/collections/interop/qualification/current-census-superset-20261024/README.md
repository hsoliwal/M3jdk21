<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Interop census successor (E5), 2026-10-24

`verify_superset_v2.py` succeeds `../current-census-superset-20261022`, whose verifier stays byte-exact and is
pinned here by SHA-256. It runs that verifier unmodified over a view of the product. The view holds the tracked
interop sources, minus the successor rows, with the five pins in `receipts/REPIN.tsv` restored to their predecessor
bytes. Each re-pinned file must equal its after pin in the product.
- **What master changed:** after the v1 seal, master revised five pins in place:
  - the two `current-census-20261017` files, by commits 97015d17, c04370c1 and e8283401;
  - three tests, by e8283401.
- **Effect on v1:** v1 has failed on master since 2026-10-21 ("original 36-owner census authority mutated").
- **Predecessor bytes:** for the census files they come from the historical checkout the workflow already makes;
  for the three tests they are kept under `predecessors/`.
- **Successor shard:** `receipts/added-04.tsv` admits the eight provider stream bridges of leaf E5, four production
  sources and four tests, each pinned by SHA-256, Git blob and an Apache-2.0 SPDX header.

Local run (JDK 21 host, historical checkout a720780b): `M3_FIVE_LIBRARY_SOURCE_SUPERSET_V2_PASS original=36
revised=7 additive=40 successor=8 total=84`. Unit tests: v2 8/8; v1 14/14, unchanged.
