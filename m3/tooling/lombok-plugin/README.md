<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# lombok-plugin: Lombok annotations compiled by M3JDK's javac, no Lombok jar

User direction (2026-10-31): inline Lombok into M3JDK's compile-time machinery. This lane holds the specification
of the `M3JdkLombok` javac plugin and the CPU tools that derived it. The implementation is HOLD: new production
classes are generated through Chrome local AI, one call per file, and Chrome is unavailable.

## Read in this order

| File | Content |
|---|---|
| `SPEC.md` | What each Lombok annotation, attribute and config key in use means, with evidence markers |
| `LLD.md` | Plugin placement (builtin in `jdk.compiler`), the three task-event stages, handler pipeline, javac hooks, M3AST and M3JdkStandard interaction, file plan |
| `INTERFACE.md` | `-Xplugin:M3JdkLombok` options, accepted annotations and config keys, diagnostics, guarantees |
| `TEST_PLAN.md` | T0 gates (run) and T1-T8 differential tests for the implementation |
| `USAGE_CENSUS.tsv` | Census of Lombok use in M3JDK and com.synexia |
| `PATTERNS.tsv` | Generated members per annotation group and per rule/combination case, from delombok |
| `CLASS_COMBINATIONS.tsv` | Class-level annotation combinations in use (constructors, with-ers, `@Builder.Default`) |
| `LOMBOK_API.tsv`, `LOMBOK_CONFIG_KEYS.tsv` | Closed vocabularies from the lombok 1.18.48 jar |
| `GATES.tsv`, `RECIPE_MEMORY.tsv`, `FOSS_REUSE_DECISION.tsv` | Gate results, tool corrections, donor decisions |

## Findings

- 3,558 Lombok annotation uses in 573 files: M3JDK 3,409 (the OpenRewrite modules), com.synexia 149. 22 annotation
  kinds; `@With`, `@Value` and `@EqualsAndHashCode` are 53% of all uses. One `lombok.config` content (OpenRewrite's)
  in 14 copies; com.synexia has none.
- The 6 vendored `third_party/rewrite-*` modules already compile from committed delombok 1.18.30 expansions.
  delombok 1.18.48 reproduces 267 of their 269 files; the 2 differences are a line wrap and a `@Delegate` that needs
  the classpath (D0).
- The 8 `third_party-images/rewrite-*` modules (219 files) and one runtime-integration copy still carry the
  annotations: the plugin's first consumers.
- delombok's output is not Lombok-free: `@lombok.Generated` on every member and `lombok.Lombok.sneakyThrow` for
  `@SneakyThrows`. The plugin emits neither (SPEC rule 3, LLD hook H2).
- `@Accessors(prefix)` and `lombok.accessors.prefix` are not used anywhere; `chain` is used through `fluent=true`.
  Both are specified and have pattern cases (C41, C42, C49, C50, C77).
- Review 2026-10-31, each point checked with delombok 1.18.48 and the lombok class files: `equals`/`hashCode` order
  fields by rank (primitive 1000, primitive wrapper 800, other 0); `@Builder.Default` initializers are removed
  before constructors and with-ers; a with-er calls whatever constructor exists; implied constructors give way to
  explicit constructor annotations and `@Builder`; accessor prefixes match first-listed, not longest (SPEC rules 4,
  9, 10, `@With`, `@Accessors`). Cases C68-C86 include one per class-level annotation combination in use
  (`CLASS_COMBINATIONS.tsv`: 23 rows); the pattern gate fails when a combination has no case.

## Tools (CPU, deterministic, re-runnable to a fixed point)

| Tool | Produces |
|---|---|
| `tools/lombok_api.py` | `LOMBOK_API.tsv`, `LOMBOK_CONFIG_KEYS.tsv` (javap over the jar) |
| `tools/lombok_census.py` + `tools/LombokCensus.java` | `USAGE_CENSUS.tsv` (git blobs read with `GIT_NO_LAZY_FETCH=1`; checkout files by ripgrep; javac parser) |
| `tools/lombok_patterns.py` + `tools/PatternDiff.java` | `PATTERNS.tsv` (69 representative cases, 4 config profiles, delombok, javac parser diff) |
| `tools/lombok_combinations.py` | `CLASS_COMBINATIONS.tsv` (class-level annotation combinations, from the census stage `OCCURRENCES.tsv`) |
| `tools/delombok_differential.py` | D0: delombok 1.18.48 vs the committed 1.18.30 expansions |
| `tools/fixture/` | census discriminator source and its expected rows |
| `tools/run_gates.sh` | all T0 gates, one PASS/FAIL line each |

```
LOMBOK_JAR=<lombok-1.18.48.jar> COM_SYNEXIA=<com.synexia checkout> M3_RG=<rg.exe> OUT=<scratch> \
    bash m3/tooling/lombok-plugin/tools/run_gates.sh
```

On a shared blobless clone add `SKIP_CENSUS=1`: the census gates (`git grep` over `m3/` at HEAD) print SKIP. Use it
only when no `m3/**/*.java` blob changed since the last census run.

Requirements: JDK 21+ (`JDK21_HOME`), Python 3, the `m3/**/*.java` blobs local (blobless clone: `PREFETCH=1` fetches
them in one batch; do not do this on a shared clone with little free space), the sparse paths
`m3/vendor/third_party/rewrite-*/{upstream-lombok,src/main/java,UPSTREAM_LOMBOK.*}` checked out. The lombok jar
is needed by the tools only, never by M3JDK's build or tests.
