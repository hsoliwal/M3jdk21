<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/compiler/lifetime-slots: compile-time lifetime slot planner

Module `org.m3jdk.compiler.lifetime` (package `jdk.m3.compiler.lifetime`), Maven coordinates
`com.m3:m3-compiler-lifetime-slots:0.1.0-SNAPSHOT`, Java 21, no dependency outside `java.base`.

## What it is

`M3DoopLifetimeSlotPlanner` is a read-only candidate planner for half-open local lifetime intervals `[start, end)`
with unique source-atom IDs: the shape the lifetime census (`m3/qualification/lifetime-census-20261030`) produces
for `STACK_LOCAL`, `CALLER_SLOT` and `OWNER_SCOPED` allocation sites. `planCandidates` returns the exact minimum
number of logical slots (interval partitioning: starts sorted, min-heap of active ends, smallest free slot first)
and the slot of every input; `planCompatibleCandidates` does the same per compatibility ID without sharing a slot
across incompatible owners. Primitive scratch arrays only, defensive copies of inputs and outputs, refusal of empty
or negative intervals, duplicate atom IDs, length mismatches and more than `MAX_WINDOWS` (262,144) intervals.
Geometry only: it does not verify liveness or escape facts and authorizes no rewrite; the runtime veto stays with
the DOOPS residency owner (`jdk.m3.collections.M3ObjectResidency`).

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `64652588f0d4c23b1f6e0411090b2a796325ae0e` (planner introduced in `4a2e6452a7a`, 2026-10-31) |
| donor files | `synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3DoopLifetimeSlotPlanner.java` (blob `257b89b5022b`) and the four tests `M3DoopLifetimeSlotPlanner{SelfTest,Test}`, `M3DoopCompatibleLifetimeSlots{SelfTest,Test}` |
| recipe | `recipe/relocate.py` v1 (`RELOCATE_LIFETIME_SLOT_PLANNER`): `python -I recipe/relocate.py --donor-git <clone> --donor-rev <rev> [--check]` |
| map | `RELOCATION_MAP.tsv`: 5 `FILE` rows, `PACKAGE com.synexia.rewrite -> jdk.m3.compiler.lifetime`, 5 identity `TYPE` rows, `EXTERNAL org.junit.jupiter.api` |
| provenance | `PROVENANCE.tsv`: donor commit, blob sha, donor sha256, target sha256 and `body_match` per file |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/compiler/lifetime-slots/`: 0 moves, 0 rewrites |

The recipe rewrites only the license header (M3JDK style), the `package` line, mapped `import` lines and mapped
type names, and inserts one `//FLLDCIM: RELOCATE` anchor before the top-level type naming the donor blob URL at
the donor commit. Everything after the package line is kept byte for byte (`body_match=TRUE` for all five files),
so the planner's algorithm is the donor's. The recipe is idempotent (second run `changed=0`) and refuses an
unmapped package or import instead of guessing the closure.

## Build

| Step | Command |
|---|---|
| compile, test, jar | `JAVA_HOME=<JDK 21> mvn -q -B -ntp -f m3/compiler/lifetime-slots/pom.xml package` (`-Xlint:all -Werror`, `proc none`; surefire runs the two JUnit 5 wrappers, which run the two self-test mains) |
| from the control reactor | `mvn -f m3/pom.xml -pl compiler/lifetime-slots package` (`compiler/lifetime-slots` is a default module of `m3/pom.xml`) |
| self-tests directly | `java -cp target/classes;target/test-classes jdk.m3.compiler.lifetime.M3DoopLifetimeSlotPlannerSelfTest` and `...M3DoopCompatibleLifetimeSlotsSelfTest` (`:` instead of `;` outside Windows); they print `M3_DOOP_LIFETIME_SLOT_ORACLE_PASS trials=6000 pairs=1096649` and `M3_DOOP_COMPATIBLE_LIFETIME_PASS trials=5000 pairs=1654134` |
| differential | `bash recipe/differential.sh <donor clone> <donor rev> <scratch dir>` after `package`: compiles the donor planner from the donor tree, replays the oracle inputs through both classes with `M3LifetimeSlotPlanDump` and compares the dumps byte for byte |
| all gates | `bash verify.sh <donor clone> <donor rev> <scratch dir>` |

Consumers depend on the module with

```xml
<dependency>
  <groupId>com.m3</groupId>
  <artifactId>m3-compiler-lifetime-slots</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

and `requires org.m3jdk.compiler.lifetime;` in their module descriptor.

## Gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| 1 `mvn package`, JDK 21, `-Xlint:all -Werror` | BUILD SUCCESS |
| 2 four self-tests in the test phase | 2 JUnit tests, 0 failures; oracle figures 6,000 cases / 1,096,649 pairs and 5,000 workloads / 1,654,134 pairs reproduced |
| 3 differential donor vs relocated | identical dumps over 22,020 cases (22,022 lines, 1,940,854 bytes), sha256 `b0708356fef41fdb51abe9ba95982cfbf4c7b6401742569312c2f67b77982030` on both sides |
| 4 `jar --describe-module` | `org.m3jdk.compiler.lifetime` exports `jdk.m3.compiler.lifetime` |
| 5 `m3/pom.xml` default module | `compiler/lifetime-slots` after `ports/file` |

Lessons of the run are in `RECIPE_MEMORY.tsv`.

## Files

| File | Purpose |
|---|---|
| `src/main/java/module-info.java`, `src/main/java/jdk/m3/compiler/lifetime/M3DoopLifetimeSlotPlanner.java` | the module and the relocated planner |
| `src/test/java/jdk/m3/compiler/lifetime/M3Doop*{SelfTest,Test}.java` | the four relocated tests (two seeded oracles, two JUnit 5 wrappers) |
| `src/test/java/jdk/m3/compiler/lifetime/M3LifetimeSlotPlanDump.java` | reflective dump driver of the differential gate (not a test) |
| `recipe/relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table and its pins |
| `recipe/differential.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates and their record |
