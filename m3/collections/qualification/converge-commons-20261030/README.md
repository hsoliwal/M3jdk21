<!-- SPDX-License-Identifier: Apache-2.0 -->
# commons-collections fork: atom-level absorption plan (2026-10-30)

`absorb_plan.py` (pattern ATOM_LEVEL_ABSORPTION_PLAN) converges the `hsoliwal/commons-collections` fork into M3JDK by
atoms, not files. It reads three MOP `atomize` outputs:

- the fork head (6,558 atoms);
- M3JDK's relocated copy under `implementations/commons-collections-4.5.0` (6,354 atoms);
- the first-party M3 owners (8,844 atoms).

It gives each fork atom a convergence state (IN_COPY, COPY_STALE, NEW_IN_FORK) and an absorption verdict
(OWNER_CONTRACT, OWNER_SHAPE, GAP).

- `SUMMARY.tsv` holds the counts.
- `GAP_PATTERNS.tsv` groups the GAP atoms by shapeKey: 2,662 atoms in 1,763 distinct shapes, which are the patterns still to mint.
- The full `ABSORPTION_PLAN.tsv` (2.2 MB) is not committed. Regenerate it with
  `python absorb_plan.py <fork ATOMS.tsv> <copy ATOMS.tsv> <owners ATOMS.tsv> <out dir>`, using the atoms from
  `java -cp <mop classes> com.m3.mop.M3Mop atomize`.

The first absorption leaf is in `../decorator-fill-20261030`.
