<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# census pass-1 remainder 20261107a: route and decision record

Ledger row 110 of the convergence programme (`C:/m3-superset/conv/record/LEAVES.tsv`, lane com-synexia-0e): the 42 leaves
of the census pass-1 proposal (`C:/m3-superset/conv/record/census/LEAVES_PROPOSED.tsv`, 88 leaves against com.synexia
develop `ca1aafc4d94f` and master `80743e25b9e6`) that no other ledger row cites. Every leaf was routed first
(`ROUTE.tsv`), then the lane-0e rows were decided (`DECISION.tsv`). Pins of this record: donor com.synexia develop
`56cbdc631259`, owner master `f6210762e04f` (2026-11-07).

## Route (ROUTE.tsv)

| lane | leaves | rule |
|---|---|---|
| com-synexia-4d | 12 | java.base, jdk.compiler, m3/search, m3/tooling, indexstring/mindex viewer family: L002, L013, L015, L024, L025, L043, L044, L049, L051, L053, L054, L055 |
| com-synexia-22 | 4 | primitives / doop / Mat*: L017, L036, L037, L045 |
| OMIT_RECIPE | 6 | OpenRewrite recipes and default-package probes: L042, L050, L052, L056, L057, L058 |
| com-synexia-0e | 20 | everything else: L005, L008, L009, L011, L014, L040, L041, L046, L047, L048, L059-L065, L072, L073, L077 |

| status | count | leaves |
|---|---|---|
| ALREADY | 10 | L002 (#2569), L005 (#2583), L014 (#2551, same donor blobs), L017 (#2711), L037 (#2632), L043 (#2692 HANDOFF_4D.tsv), L045 (#2658; residual M3NativeLeaseCounter noted), L072, L073, L077 (#2565) |
| PORT | 1 | L048 -> `m3/ast/recipe/null-flow-20261107` (PR m3/ast-census1-remainder-20261107a) |
| PORT_PARTIAL | 2 | L008, L047 -> `m3/optane/recipe/pmem-accelerator-20261107` (PR m3/optane-census1-remainder-20261107a); HOLD: the AVX-512 kernel chain and the accelerator jmod family's NUMA/affinity code (the gate machine has no AVX-512 and one NUMA node) |
| HANDOFF | 13 | the 4d and 22 rows not already on master, plus L009 and L011 (0e-routed by the literal rule, tooling/catalogue-shaped: offered to 4d) |
| DEFER | 2 | L040, L041: tests-only leaves whose owners (`M3EditDistancePlan`, `M3Utf16Glob`) sit in m3/ports/precompute, held by the census3 precompute worktree while this leaf ran |
| OUT | 7 | L059-L065: foreign namespaces (SWT, OpenRewrite vendor, Spring-style fixtures), not M3JDK owners |
| OMIT | 7 | the OMIT_RECIPE lane plus L046 (a recipe-mastery fixture oracle) |

## Method

- Citations: `grep -oE '\bL0[0-9]{2}\b'` over the ledger without row 110; the range citations of rows 69 and 71
  (`L001-L003`, `L071-L074`, `L076-L078`) expanded by hand and verified on master (`verify.py`).
- ALREADY checks: a name search of every leaf's new types (and their `M3`/`Mat`->`M3` spellings) over `git ls-tree -r`
  of master (149,810 paths), then `git log` of the owner path and `gh api .../commits/<sha>/pulls` for the PR.
- L014: the donor pin holds the same two blobs (`afa706282c5f`, `1f0dcde31649`) that #2551's `PROVENANCE.tsv` pins, so
  the census postimages are the relocated `M3MinCostFlow`/`M3MinCostFlowOracle`; same lineage, no NEC run needed.
- Donors were read from the blobless clone `C:/m3-superset/wt-syn-apitools` (`git fetch origin develop`, pin, `git show`
  per blob; nothing checked out, no `--follow`, no full-tree grep).

`verify.py` re-checks the ALREADY and owner probes: `python -I m3/qualification/census1-remainder-20261107a/verify.py [--rev REV]`;
on `f6210762e04f` probes=18, failures=0.

## Files

| File | Purpose |
|---|---|
| `ROUTE.tsv` | every uncited leaf: lane, status, evidence |
| `DECISION.tsv` | the lane-0e decisions and the ALREADY marks: donor blobs, master owner, action, record |
| `verify.py` | the mechanical probes of the ALREADY/owner rows |
| `GATES.tsv` | what was run for this record |
