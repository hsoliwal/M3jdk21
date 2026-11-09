<!-- SPDX-License-Identifier: Apache-2.0 -->
# EMF and databinding: atom-level convergence inventory (2026-10-30)

EMF and JFace/core databinding join the collections convergence. Both are collection-heavy. EMF is first on the
culprits list.

**Sources**

| Library | Fork and revision | Paths |
|---|---|---|
| EMF | `hsoliwal/org.eclipse.emf` at `c7af8f2` | `plugins/org.eclipse.emf.common/src`, `plugins/org.eclipse.emf.ecore/src` |
| databinding | `hsoliwal/eclipse.platform.ui` at `m3/databinding-m3-20261020` | `bundles/org.eclipse.core.databinding.observable/src` |

**Method:** `M3Mop atomize`, then `absorb_plan.py` (pattern ATOM_LEVEL_ABSORPTION_PLAN, in
`../../../collections/qualification/converge-commons-20261030`) against the M3 owners in `m3/collections/src/main/java`.

| Library | Files | Atoms | GAP | OWNER_CONTRACT | OWNER_SHAPE | Classes (GAP) |
|---|---|---|---|---|---|---|
| EMF | 229 | 12,117 | 6,871 | 717 | 4,529 | 249 (214) |
| databinding | 110 | 1,754 | 868 | 395 | 491 | 120 (102) |

**Largest collection families**

- EMF: ECollections 143, BasicEMap 138, DelegatingFeatureMap 136, BasicFeatureMap 135, DelegatingEcoreEList 111,
  EcoreEList 109, DelegatingEList 82, AbstractEList 80.
- databinding: MultiList 66, DecoratingObservableMap 52, AbstractObservableList 49, ObservableList 41.

**Next pattern:** EMF's `Delegating*` lists and maps and databinding's `Decorating*` observables are decorator families.
The DECORATOR_FILL / M3IFY primitives (M3jdk21 #2169) apply to them once their M3 owner scaffolds exist.

Atoms: `M3Mop` also reports METHOD_15 (EMF 1,246, databinding 102) and LOOP (EMF 758, databinding 108).

`CONVERGE_LEDGER_<lib>.tsv` uses the agreed columns (fork, fork_rev, fork_class, m3_owner, absorb_state), followed by atom
counts as evidence. `SUMMARY_<lib>.tsv` holds the convergence × absorption counts.
