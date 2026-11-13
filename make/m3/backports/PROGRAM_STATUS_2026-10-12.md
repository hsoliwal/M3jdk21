# M3JDK21 backport programme status — 2026-10-12

This is current-tree evidence, not a completion or promotion claim.

Baseline:

- repository: `hsoliwal/M3jdk21`
- branch: `master`
- baseline commit at branch creation: `dc2f175e4d1a811577fb5accc34e53df4b6f2d40`
- target contract: Java 21 source/class-file/runtime compatibility by default
- released donor interval: JDK 22 GA through JDK 27 GA

## Released JEP denominator

`JEP_CATALOGUE.tsv` still contains the complete released denominator of **85 rows**.

Disposition counts:

- candidate: **10**
- candidate-high-risk: **19**
- reject-language: **18**
- superseded: **17**
- hold-jit: **5**
- hold-compat: **5**
- superseded-high-risk: **2**
- reject-compat: **5**
- superseded-jit: **1**
- hold-preview: **3**

The active compatibility/proof residue remains **42 rows**:

- candidate: **10**
- candidate-high-risk: **19**
- hold-compat: **5**
- hold-preview: **3**
- hold-jit: **5**

A row in this queue is unfinished work, not an accepted backport.

## Residue evidence state

Current evidence states:

- `MATERIALIZED_PACKET`: **10**
- `PACKET_EVIDENCE`: **3**
- `NO_RECIPE_EVIDENCE`: **29**

Current receipt states:

- blank/no receipt: **31**
- `REVIEWED_POSTIMAGES_ALREADY_PRESENT`: **2**
- `EQUIVALENCE_PROOF_PENDING`: **1**
- `CANDIDATE_MATERIALIZED_UNVERIFIED`: **4**
- `PACKET_READY`: **2**
- `DEPENDENCY_INVENTORY_OPEN`: **1**
- `DEPENDENCY_CLOSURE_IN_PROGRESS`: **1**

Promotion state:

- explicit `NOT_AUTHORIZED`: **11**
- blank/no promotion receipt: **31**

No count above grants compatibility or promotion by itself.

## Recipe/DAG control plane

M3JDK21 already owns the richer product-side backport DAG:

- exact work packets and atom evidence;
- FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API scope validation;
- dependency waves and cycle rejection;
- exactly one serial terminal promotion node;
- 99% JaCoCo line/branch gate over the semantic DAG kernel;
- Camel/Airflow/Drools projections;
- A3 preparation before source-changing recipe crates.

The canonical reusable orchestration model is being moved into Synexia rather than duplicated further.

## Pending Synexia canonical DAG handoff

This branch adds `M3SynexiaCanonicalDagBinding` and
`m3-synexia-canonical-dag-binding.tsv`.

It binds the exact candidate:

- repository: `hsoliwal/com.synexia`
- PR: **#10630**
- branch: `m3/canonical-recipe-dag-orchestrators-20261012`
- content commit: `7c6dc007de99fdeac14dd86f4fdc594bf05953fd`

The bound portable owners are:

- `M3RecipeDagManifest`
- `M3RecipeOrchestratorPlans`
- `M3PureIntRecipeDag`
- `M3CanonicalMultiPassPlan`
- `M3Java21ConvergenceDag`

The required laws are:

- `SMALL_RECIPES_COMPOSE_CONTENT_ADDRESSED_DAG`
- `RECIPE_DAG_DEPENDENCY_WAVES_PARALLELIZE_WITH_DETERMINISTIC_FANIN`
- `CAMEL_AIRFLOW_DROOLS_PROJECT_DAG_WITHOUT_MUTATION_OR_PROMOTION_AUTHORITY`

The upstream PR is currently a draft pending unified proof. Therefore the M3JDK21 receiver is
**pre-admission evidence only**:

- upstream merged: false
- upstream proof green: false
- executable backport dependency: false
- source mutation authority: false
- semantic-equivalence authority: false
- promotion authority: false

The canonical backport DAG is deliberately unchanged by this binding.

## Backport execution remains active

Backport execution continues through the existing programme:

```text
released JEP / non-JEP / community donor inventory
 -> compatibility proof
 -> dependency closure
 -> exact file delta
 -> A3 atomize/patternize/document preparation for Java leaves
 -> source-sealed recipe crate
 -> recipe JUnit
 -> diff
 -> lint/static
 -> compile
 -> jtreg
 -> runtime/JNI parity where required
 -> fixed point
 -> serial promotion
```

Java-language features that require post-21 grammar/class-file semantics remain excluded from the
default Java-21-compatible lane. Compatible tooling/runtime/library leaves stay in scope.

## Completion boundary

The programme is not complete until:

1. all 14,948 released upstream commits are inventoried/classified;
2. all 85 released JEP rows and all non-JEP rows have a final compatibility decision;
3. every proven-compatible row is implemented or proven equivalent/current;
4. all accepted packets carry exact provenance, dependency closure and verification receipts;
5. full M3JDK21 build/jtreg/runtime gates are green; and
6. rerunning inventory/admission yields no compatible residue.

The current 42-row residue proves that backporting remains active work.
