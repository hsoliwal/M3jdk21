# A3 Synexia convergence borrow

Status: M3 tool-plane convergence profile. Candidate generation only; no automatic product mutation or promotion.

## Source authority

A3 reuses the Apache-2.0 Synexia convergence workspace instead of rebuilding a second convergence stack.

Pinned source:

- repository: `hsoliwal/com.synexia`
- revision: `ae955ea9b7e246d780269525b15fa38b95ffba67`
- license: Apache-2.0
- exact source/blob mapping: `m3/tooling/a3/synexia/SOURCE_MANIFEST.tsv`

The local read-only snapshot includes:

- Synexia public-polish workspace contract and target census;
- `RepositoryPublicPolishWorkspace`;
- tri-platform competitive-programming donor catalogue;
- `M3AtomizeRecipe`;
- `M3PatternizeRecipe`;
- `M3RepositoryAtomizePatternizeRecipe`;
- `M3RecipeMasteryLab`;
- `M3RegexStringMasteryCorpus`.

Every copied file retains its exact upstream Git blob identity. Java donor sources are stored as `.txt`
under the A3 donor snapshot so they cannot accidentally join the JDK or A3 compilation unit set.

## Why borrow instead of duplicate

Synexia already owns the mature convergence mechanics:

```text
inventory
 -> public/API evidence
 -> atomize
 -> patternize
 -> regex/string mastery
 -> bounded permutations and combinations
 -> compile + behavioral probe
 -> contract comparison
 -> fixed point
 -> donor/public-polish routing
```

M3JDK21 adapts those mechanics into its existing A3 owner instead of creating a parallel atomizer,
patternizer, donor catalogue or recipe laboratory.

A3 keeps its JDK-specific owners:

- `A3Inv` for the real `src/` + `test/` tree;
- `A3Alg` for LeetCode/HackerRank/GeeksforGeeks plus licensed GitHub lineage;
- `A3Lab` for compiler-driven in-memory Atomize/Patternize convergence;
- `A3Apply` for explicit FILE-local candidate copies;
- JEP/JBS/backport DAG owners for compatibility and absorption authority.

## Commands

Verify the exact Synexia donor snapshot:

```bash
mvn -B -ntp -f m3/pom.xml -pl tooling/a3 -am test
mvn -B -ntp -f m3/pom.xml -pl tooling/a3 -Dexec.mainClass=com.m3.a3.A3 \
  -Dexec.args="synexia --root . --out m3/build/a3/synexia" exec:java
```

Generate the JDK public-polish project target census:

```bash
mvn -B -ntp -f m3/pom.xml -pl tooling/a3 -Dexec.mainClass=com.m3.a3.A3 \
  -Dexec.args="polish --root . --out m3/build/a3/public-polish" exec:java
```

Outputs remain below `m3/build/`.

## JDK public-polish target model

The Synexia target model is adapted to OpenJDK coordinates rather than copied literally.

For each real `src/<module>` target:

- Java only -> `OWNED_CODE` / OpenRewrite dry-run candidate;
- Java + native -> `OWNED_MIXED_NATIVE` / dry-run candidate + native/JNI gate;
- native only -> `NATIVE_ONLY` / verification only;
- resource only -> `RESOURCE_ONLY` / verification only.

For each `test/<family>` target:

- Java-bearing test families receive dry-run candidates;
- native-only/resource-only test families remain verification-only;
- any native file activates the native/JNI gate.

The root `configure`, `Makefile`, and `make/` build system is a verification-only repository target.

Every `A3Inv` row must be covered exactly once by a source-module or test-family target.

## Public-polish recipe

`com.m3.a3.SynexiaPublicPolish` is a JDK-side declarative alias over the existing
`com.m3.rewrite.M3Java21Convergence` DAG.

That DAG preserves the already-admitted order:

```text
inventory -> atomize -> patternize/IOP -> documentation
```

The alias does not grant automatic application. A3 still performs explicit serial FILE candidate
work, compiler/JUnit/contract proof and fixed-point verification before a wider scope can be considered.

## Donor rules

LeetCode, HackerRank and GeeksforGeeks remain reference/catalogue evidence only. Their problem or
editorial bodies are not copied into the product.

Licensed GitHub donors may be adapted only under the existing `A3Alg` reuse policy:

- `JDK_OWNED`
- `ADAPT_PERMISSIVE`
- `REFERENCE_ONLY`

The borrowed Synexia donor catalogue is evidence for improving A3's category/shape coverage; it does
not replace A3's current JDK catalogue or bypass its license checks.

## Recipe mastery

Synexia's recipe-mastery laboratory is retained as exact source evidence because it proves the
stronger convergence model requested for A3:

- hostile and ambiguous in-memory Java fixtures;
- all bounded recipe subset/order schedules;
- fresh recipe instances;
- compile after every transformation step;
- public/protected contract comparison;
- behavioral probes;
- source/class/atom roots;
- regex/string payload signals;
- cycle detection;
- final no-change pass;
- same-subset permutation convergence.

A3 can incrementally absorb those mechanics into `A3Lab` without importing Synexia runtime
dependencies into the JDK build.

## Build authority

Maven/OpenRewrite is the M3 authoring, inventory and proof control plane only.

The OpenJDK product oracle remains:

```text
configure
 -> make
 -> jtreg
 -> runtime/serviceability/native tests
```

No Maven POM is added to OpenJDK product modules.

## Licensing and boundaries

The copied Synexia sources are Apache-2.0 and live only under the separately licensed `m3/` tooling
subtree. `m3/LICENSE`, `m3/NOTICE`, and the exact source manifest preserve the borrowing boundary.

This profile:

- does not relicense OpenJDK;
- does not copy challenge-platform source;
- does not mutate `src/` or `test/`;
- does not grant JEP/JBS compatibility authority;
- does not grant promotion authority;
- does not make a performance claim.

Its purpose is to make later absorption mechanical, reproducible and cheaper by reusing the mature
Synexia convergence/recipe substrate.
