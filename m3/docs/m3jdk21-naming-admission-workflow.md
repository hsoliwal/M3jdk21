<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 class naming, source placement and implementation admission

**Scope:** target naming and receiving instructions. This is a product-policy receiver for the
canonical Synexia Maven/OpenRewrite naming-admission recipe, not a second recipe owner or a
completed `java.base` rewrite.

**Read first:** [porting invariant](m3jdk21-porting-invariant.md),
[machine-readable authority](name-mapping.json), [counterpart naming](counterpart-naming.md),
[mapping lifecycle](migration-mapping-lifecycle.md), and
[contribution gates](../../CONTRIBUTING.md). A change or discovery to the source-to-target
relation updates an existing `migration.records[]` entry in `name-mapping.json`, or adds a
reviewed stable capability ID there. No parallel list is authoritative.

## The public class stays the JDK class

When a qualified M3 implementation is proven more efficient under all required contracts, it
**becomes the internal body/atoms of the existing standard JDK class**, not a competing user-visible
subclass, adapter or new `M3*` public class. For example:

| Java SE contract owner (unchanged) | M3 implementation policy |
| --- | --- |
| `java.lang.String` | use existing canonical `java.lang.M3String*` and `jdk.internal.mindex` helpers; preserve every String, JVM, Unicode and JNI contract |
| `java.util.ArrayList` | existing `ArrayList` class delegates to admitted array/offset/mask/length atoms; no parallel `M3ArrayList` Java SE surface |
| `java.util.HashMap` / `TreeMap` | keep map class, Entry API projection semantics and all view/iteration/serialization contracts; no invented retained per-entry store |
| `java.util.concurrent.*` | keep interfaces/type names and happens-before/linearizability; use private M3 kernels only after concurrency proof |
| `java.util.regex.Pattern` / `Matcher` | preserve regex API, flags, captures and matcher state; prepared M3 internals are implementation details |
| `java.lang.Class` / compiler APIs | keep established reflective/compiler signatures; specialized M3 class/AST facts belong in correct internal module |

It is an *implementation switch*, not an API switch. Preserve old public/protected constructors
and signatures, method descriptors, checked exceptions, reflection/serialization, class-file,
module export and JNI ABI behaviors. Keep an original path/fallback where the candidate's
admission boundary requires it. Never infer full behavior equivalence merely from a donor
algorithm or a microbenchmark.

## Source, type, method and constructor names

For each newly placed type, record *what owns the feature*, which target module owns it, the
unmodified external contract (if any), and why the selected package is appropriate. Use the
existing target owner first.

- **JDK API:** use upstream `java.lang`, `java.util`, `java.util.concurrent`, `java.util.regex`,
  `javax.*`, or existing compiler/API package names **without prefixing the public type**.
  The matching `src/<module>/share/classes/<package>/<Type>.java` path and `module-info`
  edges must agree. Source-set/platform variants require reviewed exceptions.
- **Private M3 implementation:** prefer package-private classes or members within the existing
  owner, otherwise an established internal `jdk.internal.mindex.M3*`,
  `java.lang.M3*`, `java.util.M3*` or module-specific `com.sun.tools.javac.m3.M3*`
  owner. No new export. Remove duplicate public helper facades only after dependency closure.
- **Genuinely new capability:** use a separately admitted `com.m3.*` module/package, with
  deliberate exports and full installed-image evidence; do not add it to `java.*`.
- **Spelling:** class UpperCamelCase; constructor exactly the class's simple name; method and
  field lowerCamelCase; constants UPPER_SNAKE_CASE; stable existing Java naming and acronym
  spellings stay unchanged. Document short M3 abbreviations in Javadoc and the mapping contract.
  Renaming methods must update every attributed call site, override, reflection and native
  consumer, and preserve source/binary/serialized API. Do not mechanically rewrite
  `MIndexFoo` to `M3Foo`—map the behavior, not its old donor prefix.
- **Formatting:** keep JDK-local layout/import/Javadoc conventions on changed code only, and
  make the recipe replay byte-for-byte stable. Never repo-wide autoformat a 21-derived JDK
  to conceal unrelated changes. Formatting, moves, docs and source signatures are all
  SHA-256-sealed edits.

## One mapping record per capability, many-to-many atom lineage

Before authoring a recipe, inventory existing source, snapshots/branch history and mapped
`migration.records[].id`. Update its source and target arrays and their current
module, path, type, method/constructor descriptors, hashes, owners, aliases and lifecycle
status. One donor may supply several JDK owners; several compatible donors may become one
JDK internal atom. Splits, consolidations and package moves preserve the capability ID,
source/target atom DAG relationships, older lineage and an explicit disposition for every
left-behind helper.

The source of truth for received names is `name-mapping.json`. Synexia's
`CANONICAL_RECIPE_HOME.tsv` maps **recipe ownership**, not JDK public symbols.

## Mandatory executable preflight (Synexia-owned)

The canonical **Maven/OpenRewrite** preflight is
`com.synexia.rewrite.M3Jdk21NamingAdmission`, implemented in
`hsoliwal/com.synexia/synexia-openrewrite-recipes`, with
`com.synexia.rewrite.M3Jdk21NamingContractDemonstration` as an isolated, synthetic
hash-pinned transformation example. This receiver guide does **not** copy that Java recipe
into the runtime or take over its maintenance. Follow the source checkout and exact
canonical Git-blob pins for each future production crate.

```text
SOURCE: stable mapping ID -> donor/source/license/contract inventory
  -> candidate vs incumbent + LeetCode/HackerRank/GfG categories & licensed GitHub donors
  -> original JDK public ABI/constructor/method + package/JNI/module closure
  -> existing Source/Target Atomizer + Patternizer
  -> M3Jdk21NamingAdmission (structural preflight)
  -> existing M3HashPinnedJavaSnapshotRecipe (single serial file writer)
  -> source drift/refusal + fixed-point + JUnit / compiler oracle
  -> matched M3JDK21 JDK image + jtreg/GC/JNI/VM/effect tests
  -> cold/warm benchmark and heap/native-process costs
  -> exact artifact/recipe/provenance receipt and review
  -> **only then** promote implementation under original JDK class
```

The Java preflight takes stable IDs selected from the **current pinned** target
`name-mapping.json`. It checks candidate naming, path, phase and reported gate metadata.
It **does not read source blobs, verify the attached external proof artifacts, compile the
entire image or itself promote classes.** Its `READY_FOR_PINNED_REPLAY` decision means
only that the sealed recipe candidate may be attempted; the target acceptance decision is
separately reviewed and must follow every actual proof command and readback.

The synthetic demonstration retains `com.m3.example.StableList`'s public constructor and
method while extracting a private-in-package `M3Slots` helper. It has no JDK runtime effect.
Do not copy its synthetic ID, test receipts or package to `name-mapping.json`.

## Gates and promotion decision

**Do not call a class replaced because a competing implementation compiles or benchmarks fast.**
All affected public/protected/API source/binary, exceptions, Unicode, null, identity, ordering,
backed views, iteration, concurrency, serialization, reflection, JMM, instrumentation, and
JNI/GC/CDS/JIT/bootstrap requirements must be proved in the **built M3JDK21 image**.
Generated JNI signatures, header file names, exports, signatures consumed by HotSpot and
module graphs require exact target validation. For a fast path, compare cold/warm CPU,
allocations, retained heap, native/process bytes, p50/p99 and stability against the current
JDK counterpart; record conditions and regressions. Match the donor licensing with target
file license/NOTICE and any re-contribution grant.

Receiving phases remain **String -> arrays -> collections -> AST/compiler -> other**.
Do not advance a phase because an unrelated collection prototype or test is green.
An M3 helper stays internal unless an independently reviewed non-Java-SE extension API
is the approved product.

## How to run and what is not yet proved

From Synexia root, the new isolated proof is:

```sh
mvn -B -ntp -f synexia-openrewrite-recipes/pom-m3jdk21-naming-admission.xml test
```

From this target root, inventory authoritative mapping IDs:

```sh
python3 -c 'import json; print("\n".join(x["id"] for x in json.load(open("m3/docs/name-mapping.json", encoding="utf-8"))["migration"]["records"]))'
```

Each production candidate still needs its own pinned crate, source/fixed-point tests,
`mvn -B -ntp -f m3/pom.xml clean verify`, and relevant OpenJDK `configure`/`make`/
jtreg/native/VM and comparative performance proofs. Use the same exact candidate commit,
actual installed image, JDK/OS/arch/flags and recorded outputs.

**This documentation/receiver increment changes no `java.*` class, ABI or JNI implementation.**
Policy source compiles and isolated fixtures compile under Java 21; absence of an executed
whole-Maven/JDK test must remain `NOT_RUN` in PR evidence, not PASS.
