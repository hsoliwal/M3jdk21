<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# progress-monitor-unify-20261106: one owner for the progress-monitor contract (recipe PROGRESS_MONITOR_UNIFY v1)

Owner: `jdk.m3.collections.M3ProgressMonitor` (`m3/collections`, `com.m3:m3-collections`), unchanged. The two other
`M3ProgressMonitor` types under `m3/**` become adapters that extend it; three duplicates of its types are retired.
Decision per type, the mapping tables (int -> long widening) and the wiring are in [DESIGN.md](DESIGN.md) (D2C);
`java.base` (`java.util.concurrent.ProgressMonitor`) is untouched.

| Type | Decision |
|---|---|
| `jdk.m3.algorithm.M3ProgressMonitor` (`m3/algorithms`) | adapter: `extends jdk.m3.collections.M3ProgressMonitor`; `int UNKNOWN`, `beginTask(String,int)`, `worked(int)` widen into the owner's long methods, `internalWorked`, `none()`, `None` and the donor `checkCanceled` message kept; the owner's abstract methods get no-op defaults so the four donor oracles (anonymous monitors overriding the int methods only) compile unchanged |
| `jdk.m3.file.M3ProgressMonitor` (`m3/ports/file`) | adapter: `extends jdk.m3.collections.M3ProgressMonitor`; keeps `snapshot()`, `state()` derived from it (returns the owner's `M3ProgressState`), the covariant weighted `split(long)` over its own `M3ChildProgressMonitor`, `noop()`; the nine redeclared abstract methods, `UNKNOWN` and `checkCanceled` are now the owner's (same message) |
| `jdk.m3.collections.M3PoolProgressMonitor` (`m3/collections`, from #2516) | adapter: `extends M3ProgressMonitor` in the owner's own module; `begin(String,long)` -> `beginTask`, `cancelled()` -> `isCanceled()`, `checkCancelled` message, `UNKNOWN`, `none()`, `None` kept; the owner's abstract methods get no-op defaults so the pool oracle's `Proxy` (answers `begin`/`worked`/`done`/`cancelled`, `invokeDefault`s the rest) still works; `M3Uuid128HashIndex.build` untouched |
| `jdk.m3.file.M3Progress`, `M3ProgressChild`, `M3ProgressState` | retired: duplicates of the owner interface, child and record; no user outside the retired family (grep over the sparse tree; module-info/pom grep over the whole `m3/` tree at master: `org.m3jdk.file` is consumed by `m3/search` and `make/m3/tooling/module-packs/nlp-search/frontend`, neither names a progress type) |
| `m3/search` | untouched; already on the owner; rebuilt as a consumer of the new `m3-ports-file` jar |
| `jdk.m3.file.M3StreamProgress` (`m3/file`) | out of scope: a different contract (functional stream progress), no `M3ProgressMonitor` in `m3/file`; recorded for a later leaf |

## Recipe

```
python -I make/m3/qualification/progress-monitor-unify-20261106/unify.py [--root <repo>] [--check]
```

24 hash-pinned rules (git blob sha of every preimage at master `407266963215`): 3 `TEMPLATE` (the adapters, postimage
under `templates/`, re-joined with the preimage's line ending), 18 `EDIT`/`APPEND` (anchored, every anchor exactly once),
3 `RETIRE`. A target must equal its preimage or its postimage, otherwise the recipe refuses (exit 2) and writes
nothing; `--check` recomputes every postimage from the pinned blob (`git cat-file`) and exits 1 on a difference.
`PLAN.tsv`: file, kind, before blob, before sha256, after sha256, rule. No donor package name appears in the recipe
(the naming recipe rewrites `.py` files; the map row is matched by its target path).

Wiring by the same recipe: `com.m3:m3-collections` dependency and `requires transitive jdk.m3.collections` in
`m3/algorithms` and `m3/ports/file` (the exported adapters extend an owner type; `-Xlint:exports` under `-Werror`);
`arrays` and `collections` move from the `m3-extended` profile of `make/m3/pom.xml` to the default modules (two default
modules now depend on them); `m3/ports/file/BUILD.md`; and the families relocation recipe
(`m3/algorithms/recipe/families-20261106`): the monitor's `FILE` row becomes `SUPERSEDED` (new kind: a later recipe
owns the target, `relocate.py` lists it and never writes it, `differential.sh` still compiles the donor source on the
donor side), `relocate.py` accepts the kind, `PROVENANCE.tsv` drops the row, `differential.sh` takes `DEP_CP` for the
relocated side (which now loads the owner), `verify.sh` derives `DEP_CP` from the Maven repository, `README.md` and
`RECIPE_MEMORY.tsv` record it. The immutable-pools recipe (`make/m3/collections/recipe/immutable-pools-20261106`) gets the
same treatment for the pool monitor's row (`relocate.py` v2: `ROW_KINDS`, `read_map`, and `--donor-subset` still writes
the donor source so the donor side of its `differential.sh` compiles); its `differential.sh` and `verify.sh` are
unchanged because the owner is in the same module.

## Gates

[GATES.tsv](GATES.tsv) (JDK 21.0.9, Maven 3.9.14, Windows, worktree `S:/m3-scratch/eb-conv/wt-monitor`). In short:
`m3/algorithms` `mvn package` BUILD SUCCESS (`-Xlint:all -Werror`), 15 oracle tests (6+3+3+3), `differential.sh`
with the owner jars on the relocated classpath `DIFFERENTIAL PASS` with the #2509 digest
`f85280ac94b2fc25bf7f10ac17193bf7b788dc1aea731a6a2b4c3da04fc392f9` on both sides (1,100,019 cases), the 12 family
class files, `M3AlgorithmSupport` and the catalog byte-identical to the master build (only `M3ProgressMonitor`,
`M3ProgressMonitor$None`, `module-info` differ); `relocate.py --check` fixed point at 17 files; `m3/ports/file`
`mvn package` BUILD SUCCESS (`-Xlint:all,-module -Werror`), 156 of 159 class files byte-identical to the master build
(the adapter, `M3ChildProgressMonitor` and `M3FileUniverseTraversal` differ only by the declaring interface of
owner methods in their constant pools, `javap -v`); `m3/search` rebuilt against the new jar with the master baseline
figures on this machine (64 run, 1 pre-existing `PairSumJniTest` native-library failure); `m3/collections` (after the
rebase onto #2516) `mvn package` BUILD SUCCESS with 0 warnings, 633 of 635 class files byte-identical to the master
baseline (`M3Uuid128HashIndex` included; only the pool adapter and its `None` differ), the immutable-pools
`verify.sh` with `FULL_TESTS=1`: oracles 71/26/7 checks, oracle digest `f5c5a176...`, dump digest `e5124ff1...`
(103,202 lines) and driver total `b8be5bf0...` all equal to #2516's gate 4, `relocate.py --check` fixed point at 12
files, module tests 1627 run / 5 failures / 49 errors / 2 skipped with the recorded failing set of 54 (Windows JNI
baseline); `unify.py` second run `changed=0 plan=UNCHANGED`, also after `jdk_naming.py apply --only` over the touched
modules (`moves=0 rewrites=0 refusals=0`); `git diff --stat` touches 34 paths: the three adapters, three retired
files, two poms, two module descriptors, `make/m3/pom.xml`, `BUILD.md`, seven families-recipe files, five
immutable-pools-recipe files and this directory; no `java.base` file.

`bash make/m3/qualification/progress-monitor-unify-20261106/verify.sh <donor m3scale clone> <donor rev> <scratch>`
runs the recipe check, both module builds, the families `verify.sh` (with `DEP_CP`) and the search build in order
(`JAVA_HOME` = JDK 21, `MAVEN_REPO_LOCAL` optional).
Since the module move of #2580 (`m3/ports/file` -> `m3/file`, `com.m3:m3-file`) the file module is one variable
(`FILE_MODULE`, jar name read from its `pom.xml`), its tests run as a separate baseline-compared step, and the recipe
check runs at the commit that applied it (`APPLIED`, its targets materialized under the scratch directory): later
landings edit those targets on purpose (GATES.tsv gates 16-18, RECIPE_MEMORY.tsv).

## Recorded, not done

An owner-to-donor wrapper (`jdk.m3.algorithm.M3ProgressMonitor.of(owner)`) is not added: no caller passes an owner
monitor to an algorithm today, and a new production class is a Chrome-lane generation (unavailable this session). The
adapters are Claude-authored postimages of existing types applied by the recipe. The `jdk.m3` owner's adaptation to
`java.util.concurrent.ProgressMonitor` (`java.base`) is a later leaf; so is `jdk.m3.file.M3StreamProgress`.
Moving `arrays`/`collections` into the default reactor makes `mvn -f make/m3/pom.xml package` run the collections tests,
whose 53 JNI-library tests fail on Windows (pre-existing baseline); the alternative (keep them in the profile, resolve
`m3-collections` from a local repository) was rejected because a default module would then depend on a profile module.
