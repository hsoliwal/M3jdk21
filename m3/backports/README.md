# JDK 22–27 → M3JDK21 complete compatible-backport programme

Status: active downstream backport programme. Target baseline is OpenJDK 21 semantics.

The governing execution law is [COMPATIBILITY_ADMISSION.md](COMPATIBILITY_ADMISSION.md).

## Locked contract

The Java 21 language grammar, source acceptance, class-file compatibility and existing externally
observable behavior are locked unless a future change is explicitly unlocked and independently
accepted.

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

`JEP_CATALOGUE.tsv` records all 82 JEPs delivered by JDK 22 through JDK 27 and gives their
initial downstream disposition. A disposition is an admission decision, not implementation evidence.

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
disposition. A3Apply can run the retained FILE-local Java convergence DAG on explicit source
files and writes candidate copies only under m3/build.

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

Example file-atomic generation:

```bash
python3 m3/backports/generate_recipe_crates.py \
  --repo . \
  --release 27 \
  --paths-file target/selected-java-paths.txt \
  --crate-size 1 \
  --out target/file-atomic-crates
```

This is the bridge from whole-release inventory to genuinely per-file mechanical recipe work while
preserving honest scope for coupled changes.

For JDK tooling/build/resource changes that are not Java compilation units, opt into strict UTF-8
text candidates:

```bash
python3 m3/backports/generate_recipe_crates.py \
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

For native/HotSpot/JNI source with unchanged regular-file mode, use the explicit native lane:

```bash
python3 m3/backports/generate_recipe_crates.py \
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
remain mandatory. See `m3/docs/a3-native-jni.md`.


## Recipe DAG control plane

Backport execution is composed from small immutable recipe/work atoms through the framework-neutral
DAG in `m3/tooling/backport-dag`.

The canonical authority is `m3-backport-dag.tsv` plus the Java validator `M3RecipeDag`.
Camel, Airflow and Drools files are orchestration/admission projections only; they do not gain
authority to change dependencies, compatibility decisions, edit scope or promotion order.

The locked scope ladder remains:

`FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API`

Independent FILE recipe atoms may share one parallel DAG layer. Any broader mutating node must carry
explicit scope-promotion approval. Canonical promotion is one serial terminal node after recipe
JUnit, diff, lint, compile, jtreg and runtime gates.

The Maven control entry point is:

`mvn -B -ntp -f m3/pom.xml clean verify`

This verifies the OpenRewrite recipe substrate, the 99%-gated DAG semantic kernel and the backport
inventory/admission module without replacing OpenJDK's native configure/make build.

For one concrete packet TSV, all three framework projections are generated from the validated
composed DAG with one shared semantic SHA-256 root:

```bash
mvn -B -ntp -f m3/tooling/backport-dag/pom.xml \
  -Dexec.mainClass=com.m3.tooling.dag.M3OrchestrationProjectionMain \
  -Dexec.args="m3/backports/recipes/<packet>/packet.tsv target/m3-orchestration/<packet>" \
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

Existing `m3/docs/name-mapping.json` remains the M3 migration mapping authority. This directory is an
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
- all 82 JEPs and all non-JEP changes have a compatibility decision or explicit pending proof;
- every proven-compatible change is implemented or proven already present by equivalence;
- every accepted backport has provenance and verification evidence;
- the full M3JDK21 build/test gates are green; and
- rerunning inventory/admission yields no unclassified compatible residue.

Do not report completion merely because the catalogue or queue exists.
