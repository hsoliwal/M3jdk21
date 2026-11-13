<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# progress-monitor-unify-20261106: one owner for the progress-monitor contract across `m3/**`

Design before code (D2C). Base: M3JDK master `4072669632159f9cd58e3a5c8c185f09316edfe1` (merge of #2516; the
leaf was designed on `1db62e9d54a3`, merge of #2509, and re-pinned when master moved and #2516 brought a fourth
monitor shape into the owner's module). Scope: the Maven modules under `m3/**`; `java.base` is out of scope
(constraint 3 below).

## Decision

| Type | Module | Decision | Reason |
|---|---|---|---|
| `jdk.m3.collections.M3ProgressMonitor` | `m3/collections` (module `jdk.m3.collections`, `com.m3:m3-collections`) | **OWNER**, unchanged | long work units, `M3ProgressState state()`, `split(long)`, shadow streams; `m3/search` already depends on it (11 main files, 3 tests); builds on stock JDK 21 (installed from this tree, exit 0) |
| `jdk.m3.algorithm.M3ProgressMonitor` | `m3/algorithms` (`org.m3jdk.algorithms`, `com.m3:m3-algorithms`) | **ADAPTER** (interface `extends jdk.m3.collections.M3ProgressMonitor`) | the donor contract (int units, every method default, `none()`) is what the 11 algorithm families call and what the 4 oracles implement (anonymous monitors overriding only `beginTask(String,int)` / `worked(int)`); 17 `M3ProgressMonitor.UNKNOWN` int sites, 10 `worked(1)`, 71 `done()`, 25 `checkCanceled()` in main; bodies must not change (differential gate), so the names stay and the type becomes the owner's sub-interface |
| `jdk.m3.file.M3ProgressMonitor` | `m3/ports/file` (`org.m3jdk.file`, `com.m3:m3-ports-file`) | **ADAPTER** (interface `extends jdk.m3.collections.M3ProgressMonitor`) | 4 production users remain (`M3FileStreamTasks`, `M3FileUniverseTraversal`, `M3PageableTree`, `M3JsonStructuralIndex`: `split(long)`, `noop()`, `UNKNOWN`, `beginTask(String,long)`), plus `M3ChildProgressMonitor` (its weighted child with `snapshot()`); the module has no tests, its users are public API |
| `jdk.m3.file.M3Progress` | `m3/ports/file` | **RETIRE** | byte-shape duplicate of the owner interface (same 9 abstract methods, `checkCanceled`, `split`); its only users are the two files below and `M3ProgressMonitor extends M3Progress`; no consumer outside `m3/ports/file` (module-info / pom grep over the whole `m3/` tree at master: `org.m3jdk.file` is required only by `m3/search` (pom) and `m3/tooling/module-packs/nlp-search/frontend`, neither names a progress type) |
| `jdk.m3.file.M3ProgressChild` | `m3/ports/file` | **RETIRE** | package-private implementation of `M3Progress.split`, line-for-line the owner's `M3ChildProgressMonitor`; unreachable once `M3Progress` is gone |
| `jdk.m3.file.M3ProgressState` | `m3/ports/file` | **RETIRE** | identical record to `jdk.m3.collections.M3ProgressState` (8 components, same invariants); the adapter's `state()` must return the owner's record anyway (covariance), so a second record would be unused |
| `jdk.m3.file.M3ChildProgressMonitor`, `M3ProgressSnapshot`, `M3ProgressCalculations` | `m3/ports/file` | unchanged | the snapshot model (percentage, rate, ETA, timestamp) is what the file lane adds over the owner; it stays with the file adapter |
| `jdk.m3.collections.M3PoolProgressMonitor` | `m3/collections` (same module as the owner; from #2516, the m3scale immutable-pool contract) | **ADAPTER** (interface `extends M3ProgressMonitor`) | long units already; donor surface `begin(String,long)`, `worked(long)`, `cancelled()`, `done()`, `checkCancelled()`, all default, `none()`; one production user (`M3Uuid128HashIndex.build`: `begin`, `checkCancelled`, `worked(1)`) and the oracles (`M3Uuid128SelfTest` passes `none()`; `M3ImmutablePoolsDump` proxies the type by name, reads `UNKNOWN` reflectively and counts `begin`/`worked`/`done`/`cancelled`); retiring it would edit the relocated body and break the immutable-pools `body_match`, so the name stays and the type becomes the owner's sub-interface |
| `java.util.concurrent.ProgressMonitor` | `java.base` (M3) | unchanged, JDK-level owner | text-tools uses it; the `jdk.m3` owner adapts to it in a later leaf, not the other way round; no file under `src/java.base` is touched (constraint 3) |
| `jdk.m3.file.M3StreamProgress`, `M3FileStreams.SubProgressMonitor` | `m3/file` (module `jdk.m3.file`) | out of scope, recorded | a `@FunctionalInterface` stream progress (long units, `subProgress(budget)`) and an int tick sink; different contract, no `M3ProgressMonitor` in `m3/file`; a later leaf may adapt them to the owner |

Constraints from the lane owner, all kept: (1) owner = `jdk.m3.collections.M3ProgressMonitor`; (2) bodies edited only by
recipe, differential and oracles re-run; (3) `java.base` untouched; (4) int vs long units widen at the adapter, donor-visible
signatures the tests depend on stay.

## Owner contract (unchanged, `m3/collections/src/main/java/jdk/m3/collections/M3ProgressMonitor.java`, blob `117b4eadd484`)

```java
long UNKNOWN = -1L;
void beginTask(String taskName, long totalWork);   void done();
void worked(long work);                            void setProgress(long worked, long totalWork);
void setTaskName(String taskName);                 void subTask(String subTaskName);
boolean isCanceled();                              void setCanceled(boolean canceled);
M3ProgressState state();
default void checkCanceled();                      // CancellationException("Task canceled: " + state().taskName())
default M3ProgressMonitor split(long parentWork);  // new M3ChildProgressMonitor(this, parentWork, state().generation())
default InputStream shadowInput(...); default OutputStream shadowOutput(...);
```

## Adapter 1: `jdk.m3.algorithm.M3ProgressMonitor` (int units over the owner)

Mapping, donor-visible surface on the left (kept, the 4 oracles compile unchanged), owner on the right:

| Donor method (kept as `default`) | Delegation | Widening |
|---|---|---|
| `int UNKNOWN = -1` | hides the owner's `long UNKNOWN = -1L` (same value; `M3AlgorithmSupport.begin` needs the int, line 17 is a lossy conversion otherwise) | int -1 == long -1L |
| `beginTask(String name, int totalWork)` | `beginTask(name, (long) totalWork)` | exact (`UNKNOWN` maps to `UNKNOWN`) |
| `worked(int work)` | `worked((long) work)` | exact |
| `internalWorked(double work)` | no-op (donor-only, no owner counterpart) | none |
| `subTask`, `setTaskName`, `isCanceled` (false), `setCanceled`, `done` | donor defaults kept, now `@Override` of the owner's abstract methods | none |
| `checkCanceled()` | donor message `"M3 operation canceled"` kept (overrides the owner default) | none |
| `static none()` / `enum None` | kept; `None` inherits every default | none |

Owner methods the donor never had get all-default no-op bodies so a donor-style monitor (overrides int methods only, as all
4 oracles do) still compiles: `beginTask(String,long)`, `worked(long)`, `setProgress(long,long)`, and `state()` returning
`new M3ProgressState("", "", 0L, UNKNOWN, isCanceled(), false, 0L, 0L)`.

Widening direction is int -> long only. A monitor that overrides the long methods (owner-native) receives every algorithm
call through the int defaults; a monitor that overrides only the int methods (the oracles) receives algorithm calls directly
and owner-side long calls land in the no-op defaults. There is no long -> int narrowing default: a mutual default would
recurse for a monitor that overrides neither. An owner-to-donor wrapper (`of(owner)`) is not added: no caller passes an
owner monitor to an algorithm today, and new production classes are a Chrome-lane generation (unavailable this session).

Call resolution is unchanged: `monitor.worked(1)` and `monitor.beginTask(task, total)` with int arguments pick the int
overloads (most specific), so the 12 family class files and `M3AlgorithmSupport.class` are byte-identical before and after
(gate `class_identity`). The differential driver (`M3AlgorithmFamiliesDump`) is reflective: `none()` by name, cancelling
monitors as `java.lang.reflect.Proxy` over the parameter type, `checkCanceled` through `invokeDefault`; the proxy answers
every other method with `null`/`false`/`0`, so the inherited owner methods change nothing on the relocated side.

## Adapter 2: `jdk.m3.file.M3ProgressMonitor` (long units, snapshot model, over the owner)

| File-lane member | Before | After |
|---|---|---|
| `extends` | `jdk.m3.file.M3Progress` (retired duplicate) | `jdk.m3.collections.M3ProgressMonitor` |
| `long UNKNOWN` | redeclared as `M3Progress.UNKNOWN` | inherited from the owner (`M3ProgressMonitor.UNKNOWN` still resolves at every user) |
| 9 abstract methods | redeclared with Javadoc | inherited (one declaration, the owner's) |
| `M3ProgressSnapshot snapshot()` | abstract, file lane only | kept |
| `M3ProgressState state()` | default, derived from `snapshot()`, returned `jdk.m3.file.M3ProgressState` | default, same derivation, returns `jdk.m3.collections.M3ProgressState` |
| `checkCanceled()` | `"Task canceled: " + snapshot().taskName()` | inherited owner default: `"Task canceled: " + state().taskName()`, same text (state derives from the snapshot) |
| `split(long)` | covariant, `new M3ChildProgressMonitor(this, parentWork)` | kept (covariant over the owner's `split`) |
| `static noop()` / `NoOpHolder` | kept | kept; the anonymous instance implements the owner's 8 abstract methods and `snapshot()` |

No int/long widening: both sides use long. `M3ChildProgressMonitor` (file) keeps implementing the file adapter unchanged.

## Adapter 3: `jdk.m3.collections.M3PoolProgressMonitor` (immutable-pool contract, long units, same module)

| Donor method (kept as `default`) | Delegation | Widening |
|---|---|---|
| `long UNKNOWN = -1L` | redeclared under the donor-visible name (the dump driver reads it reflectively); same value as the owner's | none |
| `begin(String task, long totalWork)` | `beginTask(task, totalWork)` | none (long on both sides) |
| `cancelled()` | `isCanceled()` | none |
| `checkCancelled()` | donor message `"immutable-pool operation cancelled"` kept; consults `cancelled()` | none |
| `worked(long)`, `done()` | same signatures as the owner's abstract methods: donor defaults become `@Override` no-op defaults | none |
| `static none()` / `enum None` | kept | none |

Owner methods the donor never had get no-op defaults (`beginTask`, `setProgress`, `setTaskName`, `subTask`,
`isCanceled` false, `setCanceled`, `state()` as in adapter 1). Delegation runs donor -> owner (`begin` -> `beginTask`,
`cancelled` -> `isCanceled`), so an owner-native monitor drives `M3Uuid128HashIndex.build` unchanged, and a donor-style
monitor (the oracle's `Proxy`, which answers `begin`/`worked`/`done`/`cancelled` and `invokeDefault`s the rest) still
receives every pool call: `checkCancelled` -> `cancelled` -> proxy. The owner-side view of a donor-style monitor's
`cancelled()` stays `isCanceled() == false`, the same one-way limitation as adapter 1. No pool body changes; the
immutable-pools differential (oracle outputs and the 103,201-line dump) must keep its #2516 digests.

## Module and build wiring

| File | Change |
|---|---|
| `m3/algorithms/pom.xml` | dependency `com.m3:m3-collections:0.1.0-SNAPSHOT` (brings `m3-arrays`); description names the owner |
| `m3/algorithms/src/main/java/module-info.java` | `requires transitive jdk.m3.collections;` (the exported `M3ProgressMonitor` extends an owner type; `-Xlint:all -Werror` includes `exports`) |
| `m3/ports/file/pom.xml` | `<dependencies>` with `com.m3:m3-collections:0.1.0-SNAPSHOT`; description names the owner |
| `m3/ports/file/src/main/java/module-info.java` | `requires transitive jdk.m3.collections;` |
| `m3/ports/file/BUILD.md` | the dependency sentence |
| `m3/pom.xml` | `arrays` and `collections` move from the `m3-extended` profile to the default `<modules>` (before `ports/file`): two default modules now depend on `m3-collections`, and a default reactor must resolve it from the reactor, not from a local repository; `search` and `viewport-core` stay in the profile |
| `m3/algorithms/recipe/families-20261106/` | `RELOCATION_MAP.tsv`: the monitor's `FILE` row becomes `SUPERSEDED` (new row kind: a later recipe owns the target; `relocate.py` no longer writes it, `differential.sh` still compiles its donor source); `relocate.py` accepts the kind; `PROVENANCE.tsv` drops the row (what `relocate.py` now regenerates); `differential.sh` reads `DEP_CP` (dependency jars for the relocated side, which now loads `jdk.m3.collections.M3ProgressMonitor`); `verify.sh` derives `DEP_CP` from the Maven repository; `README.md`, `RECIPE_MEMORY.tsv` record it |
| `m3/collections/recipe/immutable-pools-20261106/` | the pool monitor's `FILE` row becomes `SUPERSEDED`; `relocate.py` v2 accepts the kind (`ROW_KINDS`, `read_map`) and `--donor-subset` still writes the donor source so the donor side of `differential.sh` compiles; `PROVENANCE.tsv` drops the row; `README.md`, `RECIPE_MEMORY.tsv` record it; `differential.sh` and `verify.sh` unchanged (the owner is in the same module, no extra classpath); the collections pom and module-info are unchanged |
| `m3/search` | untouched: imports `jdk.m3.collections.M3ProgressMonitor` already; rebuilt as a consumer of the new `m3-ports-file` jar |
| `m3/qualification/converge-collections-20261030/CONVERGE_COLLECTIONS.tsv` | untouched: row 539 (donor monitor -> `jdk.m3.algorithm.M3ProgressMonitor`, `PRESENT_IN_M3JDK`) stays true, the type exists at that path |

Dependency graph after the leaf (no cycle): `arrays <- collections <- {algorithms, ports/file} <- search`.

## Recipe: `unify.py` (PROGRESS_MONITOR_UNIFY v1)

Hash-pinned, idempotent, refuses drift. Every rule names its target path and the git blob sha (and sha256) of the preimage
at the base commit. Three rule kinds: `TEMPLATE` (the two adapters: the postimage is `templates/<fqn>.java`, re-joined with
the preimage's line ending), `EDIT` (anchored substring or line replacements, each anchor required exactly once), `RETIRE`
(delete, preimage pinned). Run: for each rule the on-disk bytes must equal the preimage (apply) or the postimage
(already applied, no-op); anything else is a drifted preimage and the recipe exits 2 without writing. The preimage is read
from disk, or, once applied, from `git cat-file blob <pinned sha>` so `--check` recomputes every postimage from the pinned
bytes. `PLAN.tsv` records `file, before_sha256, after_sha256, rule, state`. No donor package name appears in the recipe
(the naming recipe rewrites `.py`; anchors use the target paths).

## Gates (GATES.tsv)

algorithms: `mvn package` BUILD SUCCESS with `-Xlint:all -Werror`; 15 oracle tests (6+3+3+3); `differential.sh` with
`DEP_CP` = `f85280ac94b2fc25bf7f10ac17193bf7b788dc1aea731a6a2b4c3da04fc392f9` on both sides; family class files identical to
the master build; `relocate.py --check` fixed point (17 files). ports/file: `mvn package` BUILD SUCCESS with
`-Xlint:all,-module -Werror`; unchanged class files identical to the master build; `jar --describe-module`. search: `mvn
package` against the new `m3-ports-file` jar, same figures as the master baseline on this machine (64 run, 1 pre-existing
`PairSumJniTest` native-library failure). Recipe: second run `changed=0`; `jdk_naming.py apply --only` over the touched
modules, 0 refusals; `git diff --stat` touches only the adapter files, retired files, poms, module-infos, the families
recipe files named above, `m3/pom.xml` and this directory; no `java.base` file.
