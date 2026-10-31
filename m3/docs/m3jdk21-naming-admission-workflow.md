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

## Standard implementation and additive companion admission

When a qualified M3 implementation is proven more efficient under all required contracts, it
**becomes the internal body/atoms of the existing standard JDK class**. Separately, every standard
Java API may have an optional additive public `com.m3.*` companion with useful static operations
on original Java values or typed views sharing an admitted owner. The standard type and its
compatibility contract remain authoritative. No one-class-per-API rule or subclass requirement
follows; final and sealed Java types keep their existing restrictions. For implementation work:

| Java SE contract owner (unchanged) | M3 implementation policy |
| --- | --- |
| `java.lang.String` | use existing canonical `java.lang.M3String*` and `jdk.internal.mindex` helpers; preserve every String, JVM, Unicode and JNI contract |
| `java.util.ArrayList` | existing `ArrayList` class delegates to admitted array/offset/mask/length atoms; an additive companion does not substitute for this Java SE owner |
| `java.util.HashMap` / `TreeMap` | keep map class, Entry API projection semantics and all view/iteration/serialization contracts; no invented retained per-entry store |
| `java.util.concurrent.*` | keep interfaces/type names and happens-before/linearizability; use private M3 kernels only after concurrency proof |
| `java.util.regex.Pattern` / `Matcher` | preserve regex API, flags, captures and matcher state; prepared M3 internals are implementation details |
| `java.lang.Class` / compiler APIs | keep established reflective/compiler signatures; specialized M3 class/AST facts belong in correct internal module |

For a standard backend, promotion changes the implementation while preserving the API.
Companion admission adds its own separately contracted operations. Preserve old public/protected
constructors and signatures, method descriptors, checked exceptions, reflection/serialization, class-file,
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
  owner. Keep those implementation packages internal; reconcile duplicate helpers only after
  dependency closure without deleting an admitted companion surface.
- **Additive companion or other extension:** use an existing, separately admitted `com.m3.*`
  module/package, with deliberate exports and full installed-image evidence; do not add the
  companion to `java.*` or rename its standard API. Share admitted owners instead of creating
  another payload store. `java.lang.M3String` and storage/precompute internals stay internal.
  `java.base` cannot depend upward on a companion module; keep shared base kernels in their
  existing lower owner and preserve package access and the reviewed module graph.
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

## Optional companion relation and first inventoried source

Use the same map/schema and optional `migration.records[].companion_of` object:
`{java_api: {module, symbol, path}, target_symbol, form}`. The allowed forms are
`STATIC_OPERATIONS` and `TYPED_VIEW`; `target_symbol` refers to a target in that record.
Keep its stable ID, exact target artifact, source lineage, status and existing receipts.
A relation documents the intended additional surface and never grants method coverage,
substitutability or runtime qualification. There is no companion registry beside this map.

The first source-inventoried example is `com.m3.arrays.M3Arrays` in module `com.m3.arrays`,
at `m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java`, with module descriptor
`m3/arrays/src/main/java/module-info.java`. Its static factory/composition surface can be
related to `java.base` / `java.util.Arrays` /
`src/java.base/share/classes/java/util/Arrays.java` with `form: STATIC_OPERATIONS`.
Preserve the existing explicit snapshot boundary for mutable inputs and shared immutable
slice/join owners; do not imply that a snapshot aliases the caller's mutable payload.
No runtime source is added or qualified by recording this policy example.

Companion contract review includes nulls, exception order, equality/hash, encounter order,
identity, mutations, backed views and concurrency. Typed views retain the appropriate owner
and coordinates and disclose their own identity and lifetime; factory outputs and immutable
snapshots retain their declared ownership. Reuse checked bulk-JNI selectors and record any
materialization. Native platform availability is a distinct gate from Java API admission.

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
  -> **only then** promote the original JDK implementation or the separately mapped companion scope
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
String qualification remains the active runtime stage. Public M3 companions may accompany
standard Java APIs under the optional relation above; implementation helpers and canonical
storage remain internal. Source inventory or a companion relation cannot advance a phase.
Qualify the installed companion module and each native platform separately, preserving the
specified Java fallback or explicit-unavailable contract and every pending gate.

## How to run and what is not yet proved

From Synexia root, the new isolated proof is:

```sh
mvn -B -ntp -f synexia-openrewrite-recipes/pom-m3jdk21-naming-admission.xml test
```

From this target root, inventory authoritative mapping IDs:

```sh
python3 -c 'import json; print("\n".join(x["id"] for x in json.load(open("m3/docs/name-mapping.json", encoding="utf-8"))["migration"]["records"]))'
```

Each production candidate still needs its own pinned canonical Synexia task and
source/fixed-point tests, followed by Maven verification of that task in
`hsoliwal/com.synexia`. Compile and test each directly owned product module with
historical target-local recipe profiles disabled. Relevant OpenJDK
`configure`/`make`/jtreg/native/VM and comparative performance proofs must use the
same exact candidate commit, matched installed image, JDK/OS/arch/flags and
recorded outputs. The target recipe reactor is retained migration debt and is
not an execution entrypoint.

**This documentation/receiver increment changes no `java.*` class, ABI or JNI implementation.**
Policy source compiles and isolated fixtures compile under Java 21; absence of an executed
whole-Maven/JDK test must remain `NOT_RUN` in PR evidence, not PASS.

## Release review and visible intermediate output

Run the existing map checker before changing package, class or method names.
The stable mapping IDs and [name-plan.json](../tooling/name-contract/name-plan.json)
remain authoritative; the release queue projects their status without creating a
second rename registry. A `QUEUED` row is an excluded candidate, not an applied
rename or a reason to rewrite a public Java API.

| Change | Required naming work |
| --- | --- |
| Internal package/class move | Resolve the mapped destination and typed caller closure, then update source paths, imports, module/service resources and JNI names as applicable |
| Method rename | Match the exact owner and overload; retain public, override, reflection, serialization and native contracts |
| Documentation update | Describe the current mapped owner and exact scope; distinguish an existing implementation from installed-image qualification |
| Intermediate output | Put release reports under `m3/release/queue/current/`; retain failures and incomplete gates in the report and publish the report as a CI artifact |

See [the release queue](../release/queue/README.md) for the commands and the
current evidence requirements. Public JDK names stay `String`, `Class`,
`HashMap`, `Pattern` and their existing method signatures. This documentation
update does not promote the queued class or package proposals.
