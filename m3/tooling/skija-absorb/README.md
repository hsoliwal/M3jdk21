<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# Skija absorption into M3jdk, leaf 0: atoms, patterns, bindings, owner map (2026-10-31)

Goal: M3jdk carries the graphics stack itself. SWT (Render M3, GPU M3), Nebula, Swing and SwingX then draw through an
M3jdk owner, with no Skija, HumbleUI or Lombok jar downstream. This leaf is inventory and proposal only. Nothing under
`src/` changes yet.

Donor: HumbleUI Skija 0.143.18 (`shared/java`, 217 Java types; `platform/cc`, the JNI glue) and HumbleUI Types 0.2.0
(7 types). Both are Apache-2.0. Native: Skia chrome/m143 (BSD-3-Clause) with its third-party libraries; see
`FOSS_REUSE_DECISION.tsv`. The source snapshot is the one used by the SWT census (`i31-census/skija-capabilities/skija-0.143.18`).
The 0.143.18 binding is the one SWT now runs (SWT L12, PR #310: real-app smoke 31/31 in stock, m3 and gpu).

## Method (CPU only, re-runnable)

1. Atomize: `atomize_patternize.py` (tree-sitter L-Hash, com.synexia `donor-atomize`) over `shared/java`. Result: 217
   files, 2,918 atoms, 0 errors, 1,559 exact classes (334 with 2 or more members) and 135 structural classes.
   Files: `ledgers/PATTERNS_EXACT.tsv`, `ledgers/PATTERNS_STRUCTURAL.tsv`, `ledgers/SPECTRUM.tsv`, `ledgers/PROOF.json`.
2. Patternize per type: `tools/skija_patterns.py <skija_root> <dll_exports> <out>` writes `ledgers/CLASSES.tsv`,
   `ledgers/JNI_BINDINGS.tsv`, `ledgers/CC_CLASSREFS.tsv`, `ledgers/OWNER_MAP.tsv` and `ledgers/SUMMARY.tsv`. The pattern
   set is closed (SKIJA-PATTERN v1); a type outside it would be a RESIDUE row (typed @IAntiPattern).
3. Rename package: `tools/gen_rename.py <ledgers> <types_classes> jdk.m3.graphics` writes `ledgers/TYPE_RENAME.tsv`,
   `ledgers/JNI_RENAME.tsv` and `ledgers/CC_STRING_RENAME.tsv`, plus `recipe/M3SkijaToGraphics.yml`. The recipe is an
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

Pipeline (each stage gated, `leaf1/GATES.tsv`):

1. Donor compile with Lombok: 275 classes. Its public surface equals the published 0.143.18 jars.
2. `lombok.jar delombok` 1.18.48, then `tools/delombok_clean.py`. The tool removes the markers and the timestamps, which
   would make the output non-deterministic, and adds the SneakyThrow helper. The result compiles without Lombok, and its
   API is unchanged (plus the helper).
3. `tools/absorb_rename.py`. It strips the JetBrains annotations (3,026), renames packages (401) and type tokens (6,584).
   Edits happen only in code regions (SFA), and an unqualified token is renamed only when javac's scope makes it ours:
   1 token is shadowed by `java.nio.file.*`. The one string naming the old package is reported in
   `leaf1/STRING_MENTIONS.tsv`; it is the native loader's module name, replaced in leaf 2.
4. Gates: 0 errors with no jar on the classpath. The javap surface equals the donor's renamed surface: 4,622 = 4,622.

Owner correction (recorded): `io.github.humbleui.skija.impl` maps to `jdk.m3.graphics.impl`, not
`jdk.internal.m3.graphics`. SWT Render M3 uses `impl.Native` (6 places) and `impl.Library`, so the package is exported,
and a `jdk.internal` package is not exported unqualified. The ledgers and the recipe were regenerated with this owner.

Runtime state: the module compiles and its API is complete, but its natives are not built. `M3Library` still looks for
the donor's native module, and the JNI symbols in the shipped dll carry the old names, so calls into native code fail
until leaf 2 builds `libm3graphics` under the new names (`ledgers/JNI_RENAME.tsv`).

## Native descriptor intake v2 (2026-10-31)

The native preflight found 17 additional method/array descriptor literals in the pinned
Skija C++ sources. The earlier count of 51 covered direct class-name literals only.
The two existing Python recipes now retain complete literals and rename each known
object-type reference inside them. The regenerated ledgers contain 68 rows with no
unmapped real inputs; all original 51 rows remain. Unknown referenced types still
produce `UNMAPPED`. No type, JNI-symbol or ChangeType mapping changed.

Executed CPU checks: 73 inventory assertions and 220 complete-generator assertions,
including preserved primitive/array/JDK descriptor tokens, nested types, prefix-overlapping
type names, unknown-type refusal and deterministic replay. See
`leaf2/DESCRIPTOR_PROOF.json`, `leaf2/GATES.tsv` and `leaf2/RECIPE_MEMORY.tsv`.
The existing local Java artifact separately accounts for 966 unique native symbols;
the five donor-undefined declarations remain present and unimplemented in leaf 1.
The earlier statement that they were dropped is not an implementation claim.

Readiness correction: Windows SDK 10.0.26100.0 and VS Build Tools 2022 toolset
14.44.35207 are installed; the latter is under `H:/BuildTools/VS2022`.
The donor build pins SkiaBuild release `m143-da51f0d60e-5`.
Its Windows Release x64 archive is advertised as 51,009,973 bytes; it was not
downloaded and its expanded/build-output budget is not yet measured.
H:/K:/S: remain below the mandatory 8 GiB reserve, so no native build was started.

Native leaf 2 is still pending: integrate the renamed glue, JDK native build and
`m3graphics` loader, then prove compiled exports, JNI ownership/lifetime and rendering.
No native, pixel, performance, consumer migration, full-JDK or Maven/OpenRewrite
runtime qualification is conferred by these CPU checks.
