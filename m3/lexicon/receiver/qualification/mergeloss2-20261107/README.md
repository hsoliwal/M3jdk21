<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss audit phase 2, leaf 102: CAMEL/PASCAL boundaries through the precomputed table (2026-11-07)

Leaf 102 of the phase-2 merge-loss audit (`CLEAN_0E.tsv`, lane com-synexia-0e). Origin `bf42c35a3e3`, "perf(m3): route
original CAMEL/PASCAL window scalar to precomputed type table", routed `M3WindowBoundarySummary.boundary` to
`M3CaseBoundaryPrecompute.boundary`. The fold merge `fc917d80ea3c` (#2390 through API_FOLD v1) dropped that line.
Later, loss-guard #2517 (`e629e2501ec`) brought `M3CaseBoundaryPrecompute` back to master, but nothing called it.
`M3_CASE_LIANG_PRECOMPUTE.md` describes the 8 KiB process-frozen truth table that replaces the scalar CAMEL/PASCAL
decisions. Out-of-range synthetic categories fall back to the exact scalar.

Decision: RESTORE. The 3-way merge is clean, and its result equals the retained-variants/pr2390 copy.

| file | purpose |
|---|---|
| `apply.py` | recipe MERGELOSS2_RESTORE v1: pinned preimage blob, refuses drift, idempotent, `--check` |
| `EDITS.tsv` | before/after sha256, leaf, origin commit, dropping merge, 3-way record, rule |
| `DECISION.tsv` | the decision with evidence |
| `GATES.tsv` | executed gates with numbers |

```text
python -I m3/lexicon/receiver/qualification/mergeloss2-20261107/apply.py [--check]
```
