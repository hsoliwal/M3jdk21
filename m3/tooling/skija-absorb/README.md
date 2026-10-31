<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# Skija absorption into M3jdk, leaf 0: atoms, patterns, bindings, owner map (2026-10-31)

Goal: M3jdk carries the graphics stack itself. SWT (Render M3, GPU M3), Nebula, Swing and SwingX then draw through an
M3jdk owner, with no Skija, HumbleUI or Lombok jar downstream. This leaf is inventory and proposal only. Nothing under
`src/` changes yet.

Donor: HumbleUI Skija 0.143.18 (`shared/java`, 217 Java types, 3 of them in their Java 9 versions from `shared/java9`;
`platform/cc`, the JNI glue) and HumbleUI Types 0.2.0 (7 types). Both are Apache-2.0. Native: Skia chrome/m143
(BSD-3-Clause) with its third-party libraries; see
`FOSS_REUSE_DECISION.tsv`. The source snapshot is the one used by the SWT census (`i31-census/skija-capabilities/skija-0.143.18`).
The 0.143.18 binding is the one SWT now runs (SWT L12, PR #310: real-app smoke 31/31 in stock, m3 and gpu).

## Method (CPU only, re-runnable)

1. Atomize: `atomize_patternize.py` (tree-sitter L-Hash, com.synexia `donor-atomize`) over `shared/java`. Result: 217
   files, 2,918 atoms, 0 errors, 1,559 exact classes (334 with 2 or more members) and 135 structural classes.
   Files: `ledgers/PATTERNS_EXACT.tsv`, `ledgers/PATTERNS_STRUCTURAL.tsv`, `ledgers/SPECTRUM.tsv`, `ledgers/PROOF.json`.
   These atoms are those of the jar-root variants; the 3 classes taken from `shared/java9` were not re-atomized.
2. Patternize per type: `tools/skija_patterns.py <skija_root> <dll_exports> <out> jdk.m3.graphics jdk.m3.graphics.impl`
   writes `ledgers/CLASSES.tsv`, `ledgers/JNI_BINDINGS.tsv`, `ledgers/CC_CLASSREFS.tsv`, `ledgers/JAVA_CLASSREFS.tsv`,
   `ledgers/OWNER_MAP.tsv` and `ledgers/SUMMARY.tsv`, over the source of each class that `tools/mr_select.py` selects
   (the highest Multi-Release version <= 21). The pattern set is closed (SKIJA-PATTERN v1); a type outside it would be
   a RESIDUE row (typed @IAntiPattern).
3. Rename package: `tools/gen_rename.py <ledgers> <types_classes> jdk.m3.graphics` writes `ledgers/TYPE_RENAME.tsv`,
   `ledgers/JNI_RENAME.tsv` and `ledgers/CC_STRING_RENAME.tsv` (the string ledger: Java literals that name a donor
   class for a run-time lookup, then the C++ class-name strings), plus `recipe/M3SkijaToGraphics.yml`. The recipe is an
   OpenRewrite declarative recipe with one ChangeType per type, for the absorbed sources and every consumer.

## Results

| pattern (SKIJA-PATTERN v1) | types | M3 target structure |
|---|---:|---|
| ENUM | 64 | int-packed constants in the owner, `EnumSet`/bitset lanes for flag sets; the ordinal crosses JNI unchanged |
| VALUE_DATA (Lombok `@Data` / `@AllArgsConstructor` / `@Getter` / `@With`, `_`-prefixed fields) | 52 | records where the API is accessor-only; SoA lanes (DOOP) for bulk geometry (Point, Rect, RSXform arrays) |
| MANAGED_OWNED (`extends Managed`) | 37 | M3 residency: a parent-owned native handle lane (packed `long`s), lazy facades, explicit close; one cleaner per owner, not per object |
| MANAGED_REFCNT (`extends RefCnt`) | 20 | the same handle lane with a reference count |
| NATIVE_STATIC_FACADE | 19 | static owner classes over the native lane |
| INTERFACE | 8 | interfaces in the owner |
| SUBTYPE_PURE | 8 | kept as subtypes |
| IMPL_INFRA (`Managed`, `RefCnt`, `Native`, `Library`, `Stats`, ...) | 7 | replaced by the M3 residency and loader owners (`jdk.internal.m3.graphics`) |
| NATIVE_HANDLE | 1 | handle lane |
| STATIC_UTIL | 1 | static helper |
| **RESIDUE** | **0** | |

Structural method patterns (`PATTERNS_STRUCTURAL.tsv`). These make the absorption template work instead of 217 ports:

- NATIVE_DECL: `static native T _nX(...)` (999 members). This is the JNI surface; the bindings are listed in
  `JNI_BINDINGS.tsv`.
- NATIVE_CALL_FENCED: `try { Stats.onNativeCall(); return _nX(_ptr, ...); } finally { reachabilityFence(this); }`
  (216 members). One template in the owner.
- OVERLOAD_DEFAULTING: an overload that forwards default arguments (174 + 128 members). One template.

JNI bindings: 968 Java natives map to a C++ definition in `platform/cc` (overloads resolve by their JNI long names).
965 of them are exported by the shipped skija.dll; 3 are defined but not exported. 5 natives are dead: they are declared
in Java and absent from the 0.143 glue and dll, so calling them throws UnsatisfiedLinkError (`Bitmap._nIsVolatile`,
`Bitmap._nSetVolatile`, `Typeface._nMakeFromName`, `Typeface._nMakeFromFile`, `Typeface._nMakeFromData`). They are dropped,
not absorbed. The 65 dll exports without a Java native are 64 test-only `EnumTest` exports plus `svg.SVGSVG`.
The C++ glue names Java classes in 51 strings (FindClass and descriptors); all 51 are mapped, 0 unmapped.

Owner (parameter of `gen_rename.py`, changeable by rerunning it):
- `io.github.humbleui.skija[.x].T` -> `jdk.m3.graphics[.x].M3T`
- `io.github.humbleui.skija.impl.T` -> `jdk.internal.m3.graphics.M3T`
- `io.github.humbleui.types.T` -> `jdk.m3.graphics.M3T`

224 types in all. The JNI symbols follow mechanically, for example
`Java_io_github_humbleui_skija_Canvas__1nSave` -> `Java_jdk_m3_graphics_M3Canvas__1nSave`.

## Next leaves

1. Java absorption (compile-gated). Steps:
   - Apply `M3SkijaToGraphics.yml` to the donor sources.
   - DELOMBOK_VALUE recipe: generate the accessors, constructors, `with`-ers and equals/hashCode/toString that
     `@Data`/`@AllArgsConstructor`/`@Getter`/`@With` imply. The donor's `lombok.config` sets `accessors.prefix += _` and
     `accessors.chain = true`. M3jdk ships no Lombok.
   - Strip the JetBrains annotations (`@ApiStatus` 770, `@NotNull` 623, `@Nullable` 137, `@Contract` 20).
   - Emit the module `jdk.m3.graphics`.
   - Gate: javac `--release 21` with 0 errors, and the API surface (javap) equal to the donor's modulo the rename.
2. Native: rebuild the glue under the new JNI names, or as `extern "C"` entry points bound through FFM. Link it against
   Skia m143 static libraries into `libm3graphics`, inside the M3jdk native build.
   - Needs: a Windows SDK and MSVC toolset (only clang, cmake and ninja are installed here), the Skia m143 prebuilt
     static libraries, and several GB of free disk. H: and C: are at about 1 GB each today.
   - Until then, absorbed classes cannot load natives: the shipped dll exports only the `io_github_humbleui` names.
3. Consumers: run the same recipe over SWT Render M3 / GPU M3 (and Nebula, SwingX ports), then drop the Skija jars from
   the SWT fragment.
4. Swing / SwingX: a Java2D pipeline in `java.desktop` backed by the same owner, so Swing draws through M3 too.

## Leaf 1: Java absorption (done, compile- and API-gated)

`src/jdk.m3.graphics/share/classes` holds 225 sources and `module-info.java`. The module requires only `java.base` and
exports `jdk.m3.graphics` plus `.impl`, `.paragraph`, `.resources`, `.shaper`, `.skottie`, `.sksg` and `.svg`. It is in
`PLATFORM_MODULES` of `make/conf/module-loader-map.conf`, and `make/modules/jdk.m3.graphics/Java.gmk` disables the four
lint kinds it raises. Licence and changes: `src/jdk.m3.graphics/share/legal/skija.md`.

Pipeline (each stage gated, `leaf1/GATES.tsv`). `tools/absorb_leaf1.sh run|check <repo> <work_dir>` runs all of it,
the leaf-0 ledgers included, from the donor to the repository, then the gates; `check` exits 1 when the repository
differs from what the recipe produces, so a `check` after a `run` proves the fixed point:

1. Donor compile with Lombok: 275 classes. Its public surface equals the published 0.143.18 jars.
2. `tools/mr_select.py` (MR_SELECT v1). The published jars are Multi-Release: from Java 9 on, `impl.Cleanable`,
   `impl.ReferenceUtil` and `impl.ResourceFinder` load from `META-INF/versions/9` (sources in `shared/java9`), not from
   the jar root (`shared/java`, compiled for Java 8). For every class the stage takes the highest version <= 21 and
   checks the choice against the versioned entries of the jars: 3 classes come from `shared/java9`, 214 from the root,
   no other changes (`leaf1/MR_SELECT.tsv`). The root variant of `Cleanable` needs `sun.misc.Cleaner`, which JDK 21 does
   not have; the root `ReferenceUtil` fence is empty; the root `ResourceFinder` never looks inside a named module.
3. `lombok.jar delombok` 1.18.48 (Lombok on its classpath), then `tools/delombok_clean.py`. The tool removes the markers
   and the timestamps, which would make the output non-deterministic, and adds the SneakyThrow helper. The result
   compiles without Lombok, and its API is unchanged (plus the helper).
4. `tools/absorb_rename.py`. It strips the JetBrains annotations (3,026), renames packages (401) and type tokens
   (6,585). Edits happen only in code regions (SFA), and an unqualified token is renamed only when javac's scope makes
   it ours: 3 tokens are shadowed by `java.nio.file.*`. A string literal changes only through the string ledger: the
   one literal that names a donor class for a run-time lookup, `"Library.class"` in `M3Library.load`, becomes
   `"M3Library.class"` (`ledgers/JAVA_CLASSREFS.tsv` -> `ledgers/CC_STRING_RENAME.tsv`). Every other literal that
   names a donor type is listed in `leaf1/STRING_MENTIONS.tsv`: the native loader's module name (replaced in leaf 2)
   and 223 messages and toString texts, kept as the donor prints them. A class-file or binary-name literal left
   unrenamed stops the run.
5. `tools/normalize_ws.py` (NORMALIZE_WS v1): LF line ends, no trailing blanks, no blank lines at the end (jcheck).
6. Gates: 0 errors with no jar on the classpath. The javap surface equals the donor's renamed surface, 4,622 = 4,622,
   and the surface of the jars as Java 21 loads them. `tools/M3GraphicsLeaf1Probe.java` runs the three versioned
   classes and the loader's class-file lookup on JDK 21, on the module path and from a jlink image.

Owner correction (recorded): `io.github.humbleui.skija.impl` maps to `jdk.m3.graphics.impl`, not
`jdk.internal.m3.graphics`. SWT Render M3 uses `impl.Native` (6 places) and `impl.Library`, so the package is exported,
and a `jdk.internal` package is not exported unqualified. The ledgers and the recipe were regenerated with this owner.

Runtime state: the module compiles and its API is complete, but its natives are not built. `M3Library` still looks for
the donor's native module, and the JNI symbols in the shipped dll carry the old names, so calls into native code fail
until leaf 2 builds `libm3graphics` under the new names (`ledgers/JNI_RENAME.tsv`).

## Leaf 1 atom tests: JUnit and perf per atom (2026-10-31)

`tools/gen_atom_tests.py <repo> <jdk.m3.graphics classes> <skija-shared-0.143.18.jar><path sep><types-0.2.0.jar>`
generates the tests from javap facts, template first (CPU only, deterministic; `--check` proves the fixed point):

- `test/jdk/jdk/m3/graphics/M3GraphicsAtomsTest01..15.java`: one JUnit test per runnable atom (jtreg `@run junit`,
  `@modules jdk.m3.graphics`). Every public constructor, method and field of the atom is called on the M3 type and on
  the donor type with the same deterministic cases (boundary pools, then seeded random rounds) and the outcomes are
  compared structurally: result or thrown type, aliasing, receiver and arguments after the call, mutable statics,
  console output. Enums: `values()`, `valueOf`, every method over every constant.
- `M3GraphicsAtomsPerf.java` + `PerfGroup01..05`: one row per tested atom, ns/op M3 vs donor through static final
  MethodHandles; guard: no atom slower than its donor by more than 1.5x.
- `leaf1/ATOM_TESTS.tsv` (per atom: pattern, status, reason, counts) and `leaf1/ATOM_MEMBERS.tsv` (per member).
- The runner is hand-written support in the same directory (`M3GraphicsAtoms*.java`).

A member is BLOCKED_NATIVES when its bytecode can reach native code (a native method, `System.load*`, or a class
initializer that loads the library, transitively); a test fails if an emitted member reaches natives, so the
classification is checked by execution. The oracle jars come from `-Dm3.graphics.oracle=...` (jtreg:
`-vmoption:-Dm3.graphics.oracle=...`); without it the tests abort and the harness prints PERF_SKIPPED.

Result (`leaf1/ATOM_TEST_GATES.tsv`): 225 atoms, 142 tested (135 full, 7 static-pure), 79 BLOCKED_NATIVES, 4
NOT_APPLICABLE; 1,388 members, 20,392 cases; 141 of 142 tests pass, 4 of 4 mutants killed; perf 0 outliers. The
failing test is a real difference (`leaf1/ATOM_FINDINGS.tsv` F1): the donor jar is Multi-Release, and the absorbed
`Cleanable`, `ReferenceUtil` and `ResourceFinder` are its Java 8 base variants, not the `META-INF/versions/9` ones a
JDK 9+ runtime loads; `M3Cleanable` fails to initialize on JDK 21 (`sun.misc.Cleaner`).
