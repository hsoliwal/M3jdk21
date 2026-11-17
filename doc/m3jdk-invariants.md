# M3JDK invariants

The rules every change to M3JDK keeps, with the gate or test that checks each. A pull request that has to break
one changes this file in the same pull request and says why. Per-structure invariants of the collection lanes are
in [collection-invariants.md](collection-invariants.md).

## Repository and build

| Invariant | Checked by |
|---|---|
| M3JDK builds, tests and runs from what this repository contains (no sibling checkout or downloaded tree at build time) | the PR workflows (`modules`, `sdk-and-donors`) |
| The source tree keeps the OpenJDK layout: new code goes into the module it belongs to, not into a separate product folder | review; `package_gate.py` |
| Java 21 is the API level: code and forks compile against the Java 21 platform API | the module builds with `--release`-level checks in the PR workflows |
| `jdk.m3.collections` is the one module of the collection supersets; its `share/classes` compile without `providers/classes` (provider-dependent classes and tests live under `providers`) | the share-only bridge build (`m3/collections/pom.xml`) and the workflows that compile `share/classes` alone |

## Public surface and names

| Invariant | Checked by |
|---|---|
| No new public class or member without admission; every pull request lists its new public members | `python m3/compatibility/check_public_names.py --base <sha> --head <sha>` (violations 0) |
| No new packages beyond the fixed `jdk.m3.*` set; the layout baseline only shrinks | `python m3/tooling/jdk-naming/package_gate.py check --tree HEAD` (47 at 2026-11-17) |
| Changed names are plain: no repeated adjacent word, at most 6 semantic words, no new prefixed type names; changed Java lines at most 100 columns | `jdk.m3.compiler.standard.ChangedNameGate --base --head --report` |
| Existing interfaces gain default methods only, never abstract ones | `interface_additions.py` (0 abstract added) |
| Library forks keep their upstream packages; M3 facades live in `jdk.m3.*` | review; `package_gate.py` |

## Implementations

| Invariant | Checked by |
|---|---|
| One fast implementation per behaviour, owned by the JDK (`java.base` or the owning `jdk.m3` module); libraries call it instead of keeping a copy | review; convergence census |
| Strategies are externalized as a sorted collection takes a Comparator: equality by `HashingStrategy`, growth by `ArraySizingStrategy`; a container given a strategy uses it at every point and refuses serialization rather than lose it | strategy superset and adoption tests |
| Collection data lives in array lanes (structure of arrays, offset arrays, bit masks); objects are thin facades | [collection-invariants.md](collection-invariants.md) |
| New or rewritten kernels are atoms of at most 15 lines grouped under named patterns (`//FLLDCIM` anchors) | `method_lengths.py` in the leaf's recipe crate |
| A behaviour-preserving rewrite is proven against the previous code (reference copy or twin built by the same operations) and by mutants it kills | the leaf's differential test and mutant record in its commit message |

## Third-party code

| Invariant | Checked by |
|---|---|
| Absorbed sources keep their copyright and license; each modification is recorded in the source header and in `share/legal/<library>.md` | review of the legal file in the same pull request |
| New Synexia files carry the Synexia header (Apache-2.0 OR EPL-2.0) | review |

## Change process

| Invariant | Checked by |
|---|---|
| Every change lands through a branch and a pull request merged with a merge commit; history is never rewritten on shared branches | repository settings; review |
| Commit messages state what was checked and what was not run | review |
