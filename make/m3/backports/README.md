# JDK 22–27 → M3JDK21 complete compatible-backport programme

Status: active downstream backport programme. Target baseline is OpenJDK 21 semantics.

## Current convergence and delivery boundary

**Canonical transformation work runs in `hsoliwal/com.synexia`; M3JDK21 receives
the executed product changes and independently qualifies them.** This is the
current [repository policy](../../../AGENTS.md) and
[porting contract](../../../doc/m3/m3jdk21-porting-invariant.md). Historical recipe
copies, generators and recipe profiles below remain custody debt, even where
older sections describe them as execution entry points. Do not execute them in
the public product or remove them before their exact source and callers have
been reconciled with the canonical Synexia owner.

Use the existing authorities together; these categories are related but do not
count the same thing:

| Input | Existing authority | Required receiving result |
| --- | --- | --- |
| Java specifications / JSRs | [Java SE 21 / JSR 396](https://jcp.org/en/jsr/detail?id=396), the compatibility decision and exact candidate packet | Explicit source, class-file, API and runtime obligations; a specification reference is not an implementation |
| JEPs | `RELEASE_JEP_AUTHORITY.tsv`, `JEP_CATALOGUE.tsv`, `JEP_RESIDUE_QUEUE.tsv`, `JEP_SOURCE_WITNESS.md` | Exact implementation/dependency paths and target tests; a catalogue disposition or matching blob is not installed-image acceptance |
| JBS fixes and non-JEP changes | `inventory.py`, `UPSTREAM_CHANGE_SEEDS.tsv`, `compatibility_queue.py` | Original issue and commit, follow-up/backout dependencies, adapted patch, regression and target evidence |
| JDK vendors and other implementations | `COMMUNITY_FORKS.tsv`, `COMMUNITY_FORK_CAPABILITIES.tsv`, `VENDOR_BEST_OF.tsv`; Synexia donor catalogue and vendor-delta recipe | Pinned donor/licence and one reviewed adaptation into the existing target owner |
| Synexia String, arrays, collections, AST, files and precompute | `doc/m3/name-mapping.json`, full-family pins and the Synexia `CANONICAL_RECIPE_HOME.tsv` | Stable capability ID, correct package/module, exact generated source and independent target acceptance |
| Maven/build work | Existing Synexia build planners and Maven extension owners; target `make/m3/pom.xml` and release contract | Executed product build/test configuration and source-bound receipts; reusable recipes remain in Synexia |

The same feature may have a JSR obligation, a JEP, several JBS issues and
multiple vendor commits. Preserve those relationships in its existing packet;
do not add their counts together or create a second implementation ledger.
The counts and named historical candidates later in this document describe
their recorded snapshots. Recompute and pin the current source before claiming
coverage, implementation or release readiness.

For each admitted batch, resolve the existing owner and compatibility contract,
then improve and execute the source-sealed Maven/OpenRewrite recipe in Synexia.
Its `M3Jdk21GitBlobRecipeCratePlanner`, hash-pinned snapshot recipes and existing
backport DAG are the reusable owners. Receive only emitted source/test/build
postimages, required notices and passive recipe-ID/revision/hash evidence.
Run the corresponding M3JDK21 compiler, jtreg, native/JNI and installed-image
checks, and carry failures back to the same recipe with a retained regression.

Every receiving change documents its purpose, target API/module, dependency
closure, donor rights, actual commands/results and remaining gates. Standard
JDK public names remain unchanged; separately admitted `jdk.m3.*` companions
use the existing naming map. String -> arrays -> collections -> AST/compiler
remains the runtime receiving order. Tooling repair does not advance that order.

The [release preparation guide](../release/README.md) distinguishes product
gate ownership from canonical recipe provenance. The current Maven reactor
contains legacy recipe dependencies, so complete custody/caller reconciliation
is required before its release execution can be accepted. Running a smaller
module subset or passing the recipe SDK tests does not satisfy the complete
`M3_MAVEN_CONTROL_REACTOR` gate. OpenJDK configure/make and installed-JDK tests
remain product authorities; Maven coordinates its own defined lifecycle.
See the [Maven lifecycle documentation](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html).

The governing execution law is [COMPATIBILITY_ADMISSION.md](COMPATIBILITY_ADMISSION.md).

## Locked contract

The Java 21 language grammar, source acceptance, class-file compatibility and existing externally
observable behavior are locked unless a future change is explicitly unlocked and independently
accepted. This section governs the Java-21-compatible backport lane. The
repository's directional-superset policy separately permits specified and
independently qualified M3 extensions; a rejection from this lane is not a
blanket rejection of an M3 extension merely because stock JDK 21 cannot express it.

The donor universe is intentionally complete, not curated. Every OpenJDK change after JDK 21 GA
through the latest released JDK enters inventory: JEP implementation, non-JEP JBS fix, javac fix,
HotSpot/GC/runtime change, library/security change, tooling improvement, build/test change and
follow-up repair.

The default state for an upstream change is `PENDING_COMPATIBILITY_PROOF`, not ignored.

A change is required for M3JDK21 when it can be adapted while preserving the Java 21 contract.
Tooling improvements are explicitly in scope, including `javac`, `jcmd`, `jlink`, `jpackage`,
`jfr`, `jconsole`, `jdeps`, `javadoc`, `keytool`, launchers, build/test tooling and
serviceability.

HotSpot, GC, JFR, native, security, core-library and performance changes are likewise in scope when
they preserve the Java 21 external contract. HotSpot compiler/JIT changes remain a high-risk lane,
but they are not excluded merely because they touch compiler/runtime internals.

A javac touch is also not an automatic hold. A compatible compiler bug fix, diagnostic improvement,
annotation-processing repair, documentation parser fix or performance improvement remains eligible.
Only evidence that a patch fundamentally requires post-21 language/class-file/spec semantics excludes
it from the Java-21-compatible lane.

The following are not silently imported:

- Java grammar/type-system features whose purpose is to make Java 21 accept post-21 source syntax.
- Class-file requirements that would make the runtime cease to satisfy the locked Java 21 contract.
- API/platform removals that narrow the JDK 21 contract.
- Preview/incubator contracts as if they were final.
- Compatibility restrictions whose purpose is to remove behavior Java 21 must continue to provide.
- A commit merely because it exists in a newer JDK. Every accepted backport needs provenance,
  dependency closure, tests and an exact target diff.

If an upstream patch mixes compatible and incompatible material, split it and keep the compatible
leaf in the backport queue.

## Upstream denominator

The released OpenJDK GA tag intervals used for mechanical inventory are:

| Release | GA tag |
| --- | --- |
| 21 | `jdk-21+35` |
| 22 | `jdk-22+36` |
| 23 | `jdk-23+37` |
| 24 | `jdk-24+36` |
| 25 | `jdk-25+36` |
| 26 | `jdk-26+35` |
| 27 | `jdk-27+35` |

The pinned GA intervals contain 14,948 upstream commits:

- JDK 22 interval: 2,384
- JDK 23 interval: 2,355
- JDK 24 interval: 2,562
- JDK 25 interval: 2,678
- JDK 26 interval: 2,611
- JDK 27 interval: 2,358

`RELEASE_JEP_AUTHORITY.tsv` is the fail-closed released-feature denominator for JDK 22 through JDK 27. `JEP_CATALOGUE.tsv` records all 85 authority JEPs and gives their downstream disposition. A disposition is an admission decision, not implementation evidence.

`UPSTREAM_CHANGE_SEEDS.tsv` records individually inspected non-JEP enhancements. It is intentionally
only a seed. `inventory.py` is the denominator builder: against a complete local `openjdk/jdk`
checkout it walks every commit in each GA interval, records JBS IDs and touched paths, and assigns a
review lane. Path classification never proves incompatibility.

The complete inventory must be used to generate the mechanical backport queue. Release-note curation
or a JEP-only list is never considered the complete denominator.

## Mechanical compatibility queue

The complete upstream denominator is converted into an ordered proof queue by
`compatibility_queue.py`.

The queue never admits or rejects a change from path heuristics. Every row remains:

`PENDING_COMPATIBILITY_PROOF`

but receives deterministic planning fields:

- risk class;
- physical scope floor;
- proof lane;
- recipe strategy;
- numeric priority;
- next mechanical action.

The early passes prioritize low-risk build/tool/test changes, followed by libraries, security,
runtime/HotSpot, javac, and finally language/compatibility-sensitive review. This is scheduling,
not semantic authority.

The upstream-inventory workflow exports:

- `UPSTREAM_CHANGES.tsv`;
- `UPSTREAM_CHANGES.summary.json`;
- `COMPATIBILITY_QUEUE.tsv`;
- `COMPATIBILITY_QUEUE.summary.json`.

## Actual-source presence witness

[JEP_SOURCE_WITNESS.md](JEP_SOURCE_WITNESS.md) documents the read-only, per-JEP
source-hash audit. The Maven verify phase emits
`target/jep-source-witness.tsv` for all 85 released JEP rows, differentiating
missing packets, unverified receipts, matching named source blobs, drift and
invalid receipt evidence. **A hash match is not Java-21 compatibility, complete
implementation, upstream provenance, runtime proof, or promotion authority.**

## Algorithm evidence catalogue

A3 algorithm evidence lives in ALGORITHM_CATALOGUE.tsv. One row is one JDK-owned/adapted algorithm
shape, not copied challenge solution source. The catalogue records tri-platform problem evidence,
target-owner/scope, GitHub lineage/license/reuse policy and the next proof. A3Alg validates it and
A3Plan includes validated rows as ALG work.

## A3 absorption preparation

A3 (Atomize -> Patternize -> Absorb) is the front-door preparation plane for current-tree
backport work. It does not replace this catalogue, compatibility queue, recipe crates, scope
DAG or OpenJDK build.

A3Inv inventories the actual src/ and test/ trees by module/area/path/hash. A3Plan joins every
current JEP row, inspected JBS seed and community capability row while preserving the original
disposition. The retained A3Apply source describes the historical FILE-local Java convergence
DAG. Its transformation execution belongs in canonical Synexia custody; do not
run this historical target copy. Candidate output is not target acceptance.

The intended join is:

    A3Inv/A3Plan
      -> compatible leaf discovery
      -> FILE atomize/patternize candidate
      -> file_delta_inventory / source-sealed recipe crate
      -> canonical backport DAG
      -> configure/make/jtreg/runtime proof

A3 makes source absorption mechanically smaller. It does not grant wider-scope compatibility
or promotion authority.

## Exact JDK21 ↔ donor file deltas and recipe crates

`file_delta_inventory.py` compares the entire JDK 21 GA tree verbatim against each released donor
GA tree and records SAME / MODIFIED / ADDED / REMOVED for the full path union.

For changed Java source, `generate_recipe_crates.py` creates bounded hash-pinned
OpenRewrite candidate crates. The generator:

- reads exact baseline/donor bytes from Git;
- requires strict UTF-8 round-trip for Java source;
- records SHA-256 preimages and postimages;
- emits typed exclusions rather than silently deleting/removing;
- accepts `--crate-size 1..256`;
- keeps generated crates `CANDIDATE_UNVERIFIED` until Java-21 compatibility proof succeeds.

Use `--crate-size 1` when each selected file is independently behavior/contract preserving and
therefore qualifies as a true FILE-scope recipe atom. Those one-file crates can occupy the same
parallel DAG layer.

Do **not** force one-file crates for a semantically coupled feature merely to increase apparent
parallelism. If compatibility or contract correctness depends on coordinated files, keep the
smallest honest composite crate and declare the resulting PACKAGE/MODULE/MULTI_MODULE promotion in
the packet DAG.

Historical file-atomic generator interface (custody reference, **not a command
to run in this target**). Execute the corresponding qualified canonical Synexia
recipe against pinned target inputs:

```bash
python3 make/m3/backports/generate_recipe_crates.py \
  --repo . \
  --release 27 \
  --paths-file target/selected-java-paths.txt \
  --crate-size 1 \
  --out target/file-atomic-crates
```

This is the bridge from whole-release inventory to genuinely per-file mechanical recipe work while
preserving honest scope for coupled changes.

The historical strict-UTF-8 interface for JDK tooling/build/resource candidates
is recorded below for custody reconciliation. Use the corresponding Synexia
text recipe for new work; do not run the retained target generator:

```bash
python3 make/m3/backports/generate_recipe_crates.py \
  --repo . \
  --release 24 \
  --paths-file target/selected-jdk24-paths.txt \
  --crate-size 1 \
  --include-text \
  --out target/file-atomic-crates
```

With `--include-text`, the generator keeps Java targets under
`M3Jdk21HashPinnedSnapshotRecipe` and emits non-Java UTF-8 targets under the existing
`M3Jdk21HashPinnedTextSnapshotRecipe`. Mixed runs use separate deterministic Java/text crate
names and compose them under one generated candidate recipe.

The text lane fails closed into `EXCLUSIONS.tsv` for non-UTF-8 payloads, removals, executable or
other file-mode changes. OpenRewrite byte replay cannot truthfully preserve those filesystem
semantics, so they require a separately reviewed native/Git/build-file mechanism rather than a
fake PlainText success.

The historical interface also records an explicit native lane for
native/HotSpot/JNI source with unchanged regular-file mode. Execute its qualified
canonical counterpart in Synexia, not this retained target generator:

```bash
python3 make/m3/backports/generate_recipe_crates.py \
  --repo . \
  --release 27 \
  --paths-file target/selected-native-paths.txt \
  --crate-size 1 \
  --include-native \
  --out target/a3-native-crates
```

The inventory marks C/C++/header/assembly rows with `native_source=true` and native-specific
`SOURCE_SEALED_*_NATIVE` recipe lanes. The generator emits deterministic
`jdk<release>-native-<ordinal>` one-file crates through
`M3Jdk21HashPinnedTextSnapshotRecipe`. This is exact preimage/postimage custody only; it does not
pretend OpenRewrite PlainText is a C/C++ AST. Native build, JNI/HotSpot semantics and scope-join proof
remain mandatory. See `doc/m3/a3-native-jni.md`.


## Recipe DAG control plane

Backport execution is composed from small immutable recipe/work atoms through the framework-neutral
DAG in `make/m3/tooling/backport-dag`.

The canonical authority is `m3-backport-dag.tsv` plus the Java validator `RecipeDag`.
Camel, Airflow and Drools files are orchestration/admission projections only; they do not gain
authority to change dependencies, compatibility decisions, edit scope or promotion order.

The locked scope ladder remains:

`FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API`

Independent FILE recipe atoms may share one parallel DAG layer. Any broader mutating node must carry
explicit scope-promotion approval. Canonical promotion is one serial terminal node after recipe
JUnit, diff, lint, compile, jtreg and runtime gates.

The product Maven control reactor is `make/m3/pom.xml`. Its eventual full execution
must preserve the complete reviewed product scope and generate the exact-source
release receipt described in [release preparation](../release/README.md).
Historical recipe roots and callers must first be reconciled with Synexia.
Do not use the legacy recipe profiles or a selected subset to claim the full
product reactor passed. Synexia SDK execution and product Maven acceptance are
separate results; neither replaces OpenJDK configure/make or installed-image tests.

The following retained projection interface documents the historical
orchestration binding. Reusable recipe scheduling/execution stays in Synexia;
this example is a custody reference, not a target recipe-execution entry point:

```bash
mvn -B -ntp -f make/m3/tooling/backport-dag/pom.xml \
  -Dexec.mainClass=jdk.m3.tooling.dag.OrchestrationProjectionMain \
  -Dexec.args="make/m3/backports/recipes/<packet>/packet.tsv target/m3-orchestration/<packet>" \
  exec:java
```

The output directory contains:

- `M3BackportRoutes.java` — Apache Camel Java DSL using parallel topological layers. Layer
  barriers may conservatively add ordering but never remove a dependency.
- `m3_backport_dag.py` — Apache Airflow DAG with every canonical direct dependency represented
  by one `>>` edge.
- `m3_backport_rules.drl` — Drools admission rules; a node becomes `READY` only after all of its
  canonical direct dependencies are `DONE`.
- `projection.tsv` — packet id, semantic DAG root and node count.

These generated artifacts do not execute OpenRewrite by themselves and do not gain mutation or
promotion authority. A framework-specific runner must bind the emitted node metadata back to the
existing Maven/OpenRewrite work reference, and the canonical M3 verification tail remains
authoritative.

## Second-pass review DAG pre-admission

The canonical Synexia backport DAG's recorded pre-admission design composes four
read-only FILE-scoped recipe atoms from Synexia PR #8973. Execution stays in
Synexia; their historical receipt does not prove current target qualification:

```text
M3CodeSignalTriggerRecipe
  -> M3AtomPatternSignalChain
  -> M3ProblemRecipePlanner
  -> M3JniContractInventoryRecipe
  -> existing JDK inventory
  -> compatibility proof
  -> recipe crate
  -> diff/lint/build/jtreg/runtime
  -> serial promotion
```

The exact content commit is
`5369fdc8c076b998b0dd39c7c67c85d11a4b2d8f`. The upstream PR is merged into
`hsoliwal/com.synexia:develop`.

This review chain grants no source mutation, semantic-equivalence, challenge-source copy,
JNI/native execution or promotion authority. It is evidence that narrows recipe/backport work before
the normal compatibility gate.

The canonical DAG still has exactly one serial terminal promotion node. Camel and Airflow remain
scheduler projections of the same DAG; Drools/KIE remains admission/rule evidence only.

The upstream GitHub Actions DAG run was created but produced no jobs. The custody receipt therefore
records `UPSTREAM_MERGED_ACTIONS_STARTUP_BLOCKED_NO_JOBS`; no hosted green-build claim is inferred.

## Synexia canonical small-recipe DAG custody

M3JDK21 keeps its richer product-side backport DAG as local build/promotion authority, but portable
small-recipe DAG/orchestrator semantics are canonically owned in Synexia.

`make/m3/tooling/backport-dag/M3SynexiaCanonicalDagBinding` binds the exact Synexia candidate PR/commit
and required laws. Until that upstream candidate is merged and its unified proof is green, the
binding is pre-admission evidence only and returns:

- `executableBackportDependency=false`;
- `sourceMutationAuthority=false`;
- `semanticEquivalenceAuthority=false`;
- `promotionAuthority=false`.

This prevents an unproven scheduler/control-plane handoff from silently entering JDK source
backport execution.

## Backport packet

Each actual backport must record:

1. target baseline commit and preimage path hashes;
2. upstream repository, exact commit and JBS/JEP identity;
3. touched-path inventory and dependency closure;
4. Java 21 compatibility decision and explicit exclusions;
5. exact patch or source-bound recipe;
6. lint/build/test/runtime receipts in that order where applicable;
7. postimage hashes and target diff;
8. idempotent replay/rollback or a documented reason why the upstream patch itself is the canonical
   replay unit.

For recurring or structurally repeated Java-source adaptation, a tested OpenRewrite recipe is
mandatory. JUnit must prove the recipe or recipe DAG on Java 21 fixtures before broad application.
A one-off exact donor patch may remain the replay unit only when hash-pinned and independently
verified.

Existing `doc/m3/name-mapping.json` remains the M3 migration mapping authority. This directory is an
upstream-JDK backport catalogue and must not replace or fork that mapping authority.

## First admitted tool change

JDK-8357439, **Add bash autocompletion for jcmd**, is the first concrete tool backport. Upstream
commit:

`8549d1896054dd230ba3038c83bce23b10dcda22`

Its two paths are additive in the inspected JDK 21 target:

- `make/modules/jdk.jcmd/Copy.gmk`
- `src/jdk.jcmd/share/conf/bash-completion/jcmd`

The target preimage for both paths is absent. The change does not alter Java grammar, javac, class
files or JVM execution semantics. Build/runtime acceptance remains required before promotion.

## Next adapted tooling candidate

JDK-8347112, **Copy nested directories in doc-files by default**, is materialized as a
Java-21-compatible split candidate from upstream commit
`b221cb6ba138672802644f37eebf368521a0a6f4`.

The M3 adaptation imports recursive copying by default and wildcard exclusion, but deliberately
retains Java 21's accepted `-docfilessubdirs` option processing. The packet is owned by
`M3Jdk8347112BackportRecipe` and remains `candidate-adapted` until focused javadoc build/jtreg and
recipe fixed-point CI pass. See `recipes/jdk-8347112/`.

## Java-21 update-line serviceability candidate — JDK-8359706

JDK-8359706, **VM.info and hs_err_pid Logs Now Report Current Open File Descriptors**, is
materialized from the actual OpenJDK 21 update-line backport rather than re-derived from JDK 27:

```text
openjdk/jdk21u-dev@3da873b1aa23c64e47a5da3352b7b48e8c2c74cd
  Backport-of: openjdk/jdk@b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e
```

The packet dependency-closure includes the mandatory macOS repair:

```text
openjdk/jdk21u-dev@f696cb16462f0201a10540ca36049472a79eba94
  JDK-8380236
  Backport-of: openjdk/jdk@3a109f49feb19f313632be6a2aa24ba7d9b7269b
```

`M3Jdk8359706BackportRecipe` owns seven hash-pinned HotSpot/native text targets and one
structured Java jtreg target. Current-tree postimages are materialized but promotion remains
`NOT_AUTHORIZED` until recipe JUnit, Linux image/jtreg, macOS image and fixed-point proof pass.
See `recipes/jdk-8359706/`.

## Next adapted serviceability candidate

JDK-8364182, **Add jcmd VM.security_properties command**, is materialized from upstream commit

`f2f8828188f45d16344c82adfbf951f7409b8825`.

The packet is additive: the existing Java 21 `VM.system_properties` command remains present and
visible. M3JDK21 adds the new security-property command, the internal SharedSecrets/VMSupport bridge,
the HotSpot diagnostic command registration and the upstream focused jtreg test. The only mechanical
source adaptation is the older JDK21 `DCmdFactoryImpl(export, enabled, hidden)` constructor.

The packet is owned by `M3Jdk8364182BackportRecipe` as separate Java-LST and HotSpot-text atoms.
It remains `candidate-adapted` until focused image build, both
`SecurityPropertiesTest.java` and existing `SystemPropertiesTest.java`, recipe fixed point and
backport verification pass. See `recipes/jdk-8364182/`.

## Next adapted security-library candidate

JDK-8374808, **Add KeyStore creation-date Instant methods**, is split from upstream commit

`264fdc5b4ed5f4e35168048533196e670c3dda6c`.

M3JDK21 imports only the Java-21-compatible additive API leaf:
`KeyStore.getCreationInstant(String)` and the default
`KeyStoreSpi.engineGetCreationInstant(String)` adapter. Existing provider implementations keep
their Java 21 `Date` storage and persistent-format behavior; the broader upstream provider
Date-to-Instant storage rewrite is explicitly outside this packet.

The packet is owned by `M3Jdk8374808BackportRecipe` and remains `candidate-adapted` until
focused `java.base` build, `CreationInstant.java` jtreg, existing KeyStore compatibility tests,
recipe JUnit and fixed-point verification pass. See `recipes/jdk-8374808/`.

## Completion boundary

M3JDK21 backport convergence is complete only when:

- the full 14,948-commit released denominator is inventoried;
- all 85 released JEPs and all non-JEP changes have a compatibility decision or explicit pending proof;
- every proven-compatible change is implemented or proven already present by equivalence;
- every accepted backport has provenance and verification evidence;
- the full M3JDK21 build/test gates are green; and
- rerunning inventory/admission yields no unclassified compatible residue.

Do not report completion merely because the catalogue or queue exists.
