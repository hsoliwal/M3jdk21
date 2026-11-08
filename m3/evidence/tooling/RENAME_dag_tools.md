# jdk.m3.tooling: product prefix dropped in six packages (2026-11-08)

`rename_types.py` v1.7 renamed 63 types (`RENAME_dag_tools.tsv`), run by `run_rename_dag_tools.sh` with
`--string-words --comment-words` and declared docs:

| Package | Module | Types |
|---|---|---|
| jdk.m3.tooling.dag | m3/tooling/backport-dag | 41 |
| jdk.m3.tooling.atomjit | m3/tooling/atom-jit-gate | 11 |
| jdk.m3.tooling.recipe.agent | m3/tooling/recipe-agent-api | 4 |
| jdk.m3.tooling.cds | m3/tooling/cds-training | 3 |
| jdk.m3.tooling.smartbuild | m3/tooling/smart-build | 2 |
| jdk.m3.tooling.priority | m3/tooling/lazy-priority-frontier | 2 |

`pom_dependents.py` found no Maven dependent of any of the four artifacts. A scan of every text file under m3/,
.github/ and make/ (qualified, path and bare forms) found these consumers, and they follow:
- 14 workflows that run the DAG mains, the smart-build receiver and the atom-jit gate;
- backport-dag/pom.xml: 15 jar include filters and 2 main classes;
- the cds-training and lazy-priority-frontier launchers;
- module READMEs, m3/backports/*.md, m3/docs/recipe-atom-evidence-and-dag.md.

The following are records and keep their bytes:
- migration-recipes and hash-pinned-target-paths (pinned texts, before/after workflow fixtures, task ledgers);
- m3/compatibility maps;
- the lazy-priority-frontier application and provenance records;
- the recipe-agent-api custody receipt;
- the module recipe/ and fixtures/ directories;
- the 11 `//FLLDCIM: RELOCATE` anchors (donor URLs).

`DECLARED_EDITS_dag_tools.tsv` lists the two edits outside the recipe: README launch lines that still named the
pre-relocation package `com.m3.tooling.dag`.

## Recipe and gate corrections (RECIPE_MEMORY 2026-11-08)

| Pattern | Found by this leaf | Correction |
|---|---|---|
| BINARY_PATH_FORM | backport-dag's jar is cut by `<include>jdk/m3/tooling/dag/M3RecipeDag*</include>`; v1.6 left the path form, so the jar would have lost 15 class groups with a green compile | v1.7 rewrites `<p>/M3Rest` outside evidence |
| EVIDENCE_SEGMENT_SCOPE | the package `jdk.m3.tooling.recipe.agent` contains a `recipe/` directory: v1.6 skipped it as evidence, and `package_gate` never reported its 4 types (nor the 4 of `jdk.m3.graphics.resources`) | evidence segments count only above the Java source root, in both tools; the 4 graphics types join the baseline |
| JAVADOC_LINK_FORM | comments were kept byte for byte, so `{@link M3RecipeDag}` named a missing type | `--comment-words`, which never renames after `/` (the first draft rewrote 11 donor URLs; caught by the anchor diff) |
| GUARD_REGISTRATION | `m3jdk-naming.yml` has failed at step 1 since #2829 (an unregistered guard file), so `package_gate check --tree HEAD` has not run on master | `test_jdk_naming.py` registers the new guard files and reads `file#method (note)` |

`test_rename_types.py` (new, 14 tests) covers every reference form, evidence, records, anchors and the fixed point. The
v1.6 recipe fails it. `test_package_gate.py` gains 2 checks (15). The old gate fails the new package-segment check.

## Gate (`gate_dag_tools.sh`, WSL OpenJDK 21; before vs after)

| Module | Before (master) | After |
|---|---|---|
| backport-dag javac main | rc=1, 3 errors (stale `A3Apply.run` signature, record accessor shadowing `path()`, composer method ref) | the same 3 errors, names normalised |
| backport-dag without those 2 files and their 12 dependents; JUnit | compile rc=0; 29 found, 24 pass, 5 fail (checked-in resources drifted from the tests) | same 29 results per test, names normalised |
| atom-jit-gate (atom-census by source); JUnit | rc=0; 40/40 | same per test |
| recipe-agent-api; JUnit | rc=0; 3/3 | same |
| smart-build; JUnit | rc=0; 3/3 | same |
| cds-training verify.sh | M3_CDS_SELF_TEST PASS checks=38 | same |
| lazy-priority-frontier verify.sh | PASS cases=4 | same |
| package_gate check (fixed gate, new baseline, post-change path list) | - | violations=0 |
| rename fixed point | - | second run: no change |
| anchors in the six modules | 11 | 11, byte-identical |

## Debt found, not changed here

These fail on master and are left for a backport-dag repair leaf:
- the 3 compile errors;
- the 5 test failures;
- the checked-in drools/airflow projections and the `a3-preparation` row of `m3-backport-dag.tsv`, which still name
  `com.m3.tooling.dag.M3*` and are generator output to be regenerated, not hand-edited.

Renames done with v1.6 or earlier (precompute, regexcursor, search, compression, java.base internals) did not touch
comments. A pass over their `RENAME*.tsv` maps for stale `{@link M3…}` is queued.
