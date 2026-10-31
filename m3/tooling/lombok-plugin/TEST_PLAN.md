<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# TEST_PLAN: M3JdkLombok differential tests

The oracle is Lombok's own expansion: `lombok delombok` 1.18.48 (jar sha1 `6858f13541bab505384f07053c5a7b539bbfd3e3`).
The comparison is on class files (`javap` surface), never on source text, so formatting differences between delombok
and the plugin do not matter. M3JDK stays standalone: the jar is used only by the tools that regenerate the golden
files; the tests read committed goldens and never need the jar.

## T0: specification gates (run now; `GATES.tsv`)

| Gate | Command | Result |
|---|---|---|
| census discriminator | `java tools/LombokCensus.java <fixture manifest> LOMBOK_API.tsv out.tsv`; compare with `tools/fixture/EXPECTED_OCCURRENCES.tsv` | 14/14 rows: comments, string literals and an import that shadows `lombok.*` produce no row; FQN, nested, static-field, record-component, constructor, parameter, local and `val` uses are classified |
| census fixed point | `tools/lombok_census.py` twice | byte-identical |
| pattern coverage | `tools/lombok_patterns.py` | 53/53 census annotation groups covered by 50 cases |
| pattern fixed point | `tools/lombok_patterns.py` twice | byte-identical `PATTERNS.tsv` |
| D0 | `tools/delombok_differential.py` on the 6 delomboked rewrite modules | 267/269 files equal after normalization; 1 FORMAT (rewrite-java `J.java`: delombok line-wraps an enum constant list differently), 1 CLASSPATH (rewrite-test `RewriteTest.java`: `@Delegate` needs the delegate type, absent without a classpath) |

`tools/run_gates.sh` runs all five.

## T1: template round trip (unit test, jdk.compiler)

For every `PATTERNS.tsv` row: bind `${Type}`, `${f}`, `${F}`, `${T}` to the case's representative class, build the
member with `M3JdkLombokMake`, print it with javac's `Pretty`, collapse whitespace, compare with the row's signature,
annotations (minus `@lombok.Generated` and `@SuppressWarnings`) and body. Pass: all 226 rows.

## T2: per-case differential (jtreg, `test/langtools/tools/javac/m3/lombok/`)

Inputs: the 50 representative sources of `tools/lombok_patterns.py` per profile (default, openrewrite,
accessors-chain-config, accessors-prefix-config), and their goldens: delombok output with
`-f generated:skip -f suppressWarnings:skip -f generateDelombokComment:skip`, committed under `golden/<profile>/`.

For each case:
1. `javac -Xplugin:M3JdkLombok` (with the profile's config) on the representative source.
2. `javac` on the golden.
3. Compare `javap -p -s` of both, members sorted: equal. Compare `javap -c -p` per generated method: equal,
   except C44 (`@SneakyThrows`: the golden calls `lombok.Lombok.sneakyThrow`; the plugin throws the caught exception
   directly). The golden of C44 compiles against a 5-line test-only `lombok.Lombok` stub in the test directory.
4. C44 behaviour: calling `read()` throws the original `java.io.IOException` instance, unwrapped.
5. Delombok WARNING rows of the case (C42, C50) appear as `M3LOMBOK_PREFIX_MISMATCH` at the same line.

Pass: all cases of all profiles.

## T3: real-source differential (D1)

1. The 8 `third_party-images/rewrite-*` modules and `runtime-integration/.../parser-source/rewrite-java-21`
   (the sources that still carry Lombok annotations): compile each module with the plugin and
   `config=<module>/UPSTREAM_LOMBOK.config`; compile its delombok golden (generated once by a variant of
   `tools/delombok_differential.py` with the module's vendored dependencies on the class path, so that `@Delegate`
   resolves); `javap -p -s` per class file equal.
2. The 6 delomboked `third_party/rewrite-*` modules: compile `upstream-lombok/` with the plugin and the committed
   `src/main/java/` expansion as is; `javap -p -s` per class file equal for all 269 sources (D0 is the text-level
   precursor: 267/269).

Pass: every class file equal. A difference is a FAIL unless it is listed in this file with its cause.

## T4: rule discriminators (jtreg, negative and edge cases)

New representative cases are added to `tools/lombok_patterns.py` (so they get delombok rows) before the rule they
cover is implemented; each `[L]` marker of `SPEC.md` has one:

| Case | Source | Expect |
|---|---|---|
| C52 | `@Getter` field with a hand-written `getCount()` | no getter generated; `M3LOMBOK_SKIPPED_EXISTING` |
| C53 | `@EqualsAndHashCode` on `class A extends B` without `callSuper` | no super call; `M3LOMBOK_EQUALS_WITHOUT_SUPER` |
| C54 | `@Value` with an explicit constructor | no all-args constructor |
| C55 | `@Getter @Setter boolean isActive` | `isActive()`, `setActive(boolean)` |
| C56 | `@Data` with an explicit constructor | no required-args constructor |
| C57 | `@EqualsAndHashCode` with a `transient` and a `static` field | both excluded |
| C58 | `@Accessors(chain=true)` on a field inside a `@Accessors(fluent=true)` class | field level wins |
| C59 | `@SneakyThrows` without exception classes | catches `Throwable` |
| C60 | `@NoArgsConstructor` with an uninitialized final field | `M3LOMBOK_FINAL_FIELD_UNINITIALIZED` |
| C61 | `@UtilityClass` declaring a constructor | `M3LOMBOK_UTILITY_CLASS_CONSTRUCTOR` |
| C62 | openrewrite profile, `@Value` with a `@Nullable` field | `@Nullable` on the getter and the constructor parameter (SPEC rule 7) |
| C63 | `@Accessors(prefix="f") @Value @With` | `withCount`, `getCount` from `fCount` |
| C64 | `@Getter(lazy=true)`, `@Cleanup`, `@Builder(toBuilder=true)` | `M3LOMBOK_UNSUPPORTED` each |
| C65 | `lombok.config` with `lombok.toString.includeFieldNames = false` | `M3LOMBOK_CONFIG_UNSUPPORTED_KEY` |
| C66 | `import lombok.*; import org.example.NonNull;` with `@NonNull` | not processed (shadowed), compiles against `org.example.NonNull` |
| C67 | real Lombok on the processor path | `M3LOMBOK_CONFLICT`; with `coexist=skip`, a note and no expansion |

## T5: order and idempotence

- An annotation processor that generates one source forces a second round: the report has exactly one
  `GENERATED` row per member (no double expansion); the class files equal those of a single-round compile.
- An `M3AST.Consumer` sees no generated member at PARSED and every generated member at ANALYZED; generated nodes
  have no end position and appear in the plugin's `generated` map.
- M3JdkStandard (#2188) over a source using `@Delegate` reports no finding on the generated methods.

## T6: determinism

Compile T2 and T3 twice with fresh output directories: class files and the report TSV byte-identical.

## T7: standalone

Compile T2 with no Lombok jar on any path (asserted by listing `-classpath`, `-processorpath` and the module path):
success, and `javap -v` of every class file contains no `lombok/` constant.

## T8: compile time (report only, not a gate)

`rewrite-maven-8.17.1` image (96 annotated files): wall time and javac heap with `-Xplugin:M3JdkLombok` versus real
Lombok as an annotation processor (`-processorpath lombok-1.18.48.jar`), 5 runs each, median reported.

## Exit criteria for the implementation leaf

T1, T2, T4, T5, T6, T7 PASS; T3 PASS for every class file; T0 still PASS after the new cases are added; every new
file follows the M3JDK standard (M3JdkStandard plugin, `-Xdoclint:all/protected`).
