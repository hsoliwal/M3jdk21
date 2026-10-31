<!--
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
-->
# java.base: one fast hashing implementation

The non-cryptographic hash kernels of the vendored zero-allocation-hashing 0.16
(`m3/vendor/third_party/zero-allocation-hashing`) are part of java.base as `jdk.internal.m3.hash.M3*`
(`src/java.base/share/classes/jdk/internal/m3/hash`). A deterministic recipe makes them from the donor files. The
license and the list of changes are in `src/java.base/share/legal/zero-allocation-hashing.md`.

## What java.base now has

| Role | Types |
|---|---|
| ALGORITHM | M3XxHash, M3XXH3 (64 and 128 bit), M3WyHash, M3MurmurHash_3 (64 and 128 bit), M3CityAndFarmHash_1_1 (CityHash 1.1, FarmHash na and uo), M3MetroHash |
| FACADE | M3LongHashFunction, M3LongTupleHashFunction, M3DualHashFunction |
| ACCESS | M3Access (template), M3ByteArrayAccess (was UnsafeAccess), M3ByteBufferAccess, M3CharSequenceAccess |
| KERNEL | M3Maths (and M3MathsJDK9), M3Primitives, M3Util |

Inside java.base: `M3LongHashFunction.xx3().hashBytes(bytes)`, `M3LongHashFunction.city_1_1(seed0, seed1)`,
`M3LongTupleHashFunction.xx128(seed).hashBytes(bytes)` and so on. The names are the donor's.

Not kept: the per-JVM String-internals classes (StringHash, ModernCompactStringHash, ModernHotSpotStringHash,
HotSpotPrior7u6StringHash, UnknownJvmStringHash, CompactLatin1CharSequenceAccess) and package-info.

## The recipe

`tools/run_absorb.sh <repo> <work dir>` runs `tools/hash_jdk_form.py` (HASH_JDK_FORM v2) on a staged copy of the 16
kept donor files, then `tools/absorb_rename_generic.py` (the generic renamer of the number-parse and Skija
absorptions, unchanged) with `ABSORB_PKGS=net.openhft.hashing=jdk.internal.m3.hash` and `TYPE_RENAME.tsv`, then
`tools/literal_constants.py` (LITERAL_CONSTANTS v1.1) with `CONSTANT_NAMES.tsv` (see Constants).

| Rule family | What it does |
|---|---|
| UNSAFE_TO_BYTE_ARRAY v1 | UnsafeAccess becomes the byte[] access over `jdk.internal.util.ByteArrayLittleEndian` (little-endian, bounds-checked, index base 0); no sun.misc.Unsafe, no reflection, no pre-Nougat Android fallbacks |
| FACADE_DROP_RAW_MEMORY v2 | the facade methods that read raw memory are not kept; byte[] goes through the byte[] access, direct and read-only ByteBuffers through ByteBufferAccess; a direct buffer is read up to its capacity (through a view whose limit is the capacity when off + len passes the limit), as the donor's address read was (v1 read it through its limit) |
| ACCESS_DROP_UNSAFE v1 | `Access.unsafe()` is not kept |
| STRING_INTERNALS_TO_CHARSEQ v1 | `hashChars(String)` takes the donor's JVM-independent path (native-order chars through CharSequenceAccess); same hash values |
| UTIL_DROP_JVM_PROBES v1 | Util keeps `checkArrayOffs` only |
| XXH3_SECRET_ACCESS v1 | XXH3 reads its secrets through the byte[] access |
| STRIP_JSR305 v1, LINT_CLEAN v1 | compile-only annotations removed; the two java.base `-Xlint:all -Werror` findings fixed |
| CHANGE_NOTICE v1 | every changed file says so above its package line (Apache-2.0 section 4(b)) |
| STRIP_JB v1, RENAME v1 (renamer) | JetBrains annotations removed; package and type names |
| LITERAL_CONSTANTS v1.1 (literal_constants.py) | every magic literal becomes a `private static final` constant named by role (`CONSTANT_NAMES.tsv`) |

Every rule has an expected hit count (`RULE_COUNTS.tsv`); no forbidden token may be left in code; the rules applied
to their own output hit nothing; a second run leaves the 16 files byte-identical. Comments and documentation are the
donor's and still name the donor API (the renamer edits code only), except one comment line on each direct-buffer
branch (FACADE_DROP_RAW_MEMORY v2) and the constant blocks (LITERAL_CONSTANTS: an anchor line and one role Javadoc
per constant); the private donor names `unsafeHash` and `unsafeLE` stay and now read through the byte[] access.
`FILE_HASHES.tsv` binds donor and staged sha256 per file.

## Constants

User rule: no magic numbers and no floating strings. Recipe LITERAL_CONSTANTS v1.1, `tools/literal_constants.py`,
is step 3 of `tools/run_absorb.sh`:

- Inventory (`LITERALS.tsv`): 1,105 literals to name in 13 of the 16 types (1,103 numeric, 2 strings). These stay:
  0, 1 and -1 (190); the initializers of the donor's own named constants (64); the XXH3 default secret table
  (192 elements); and the 4 annotation arguments (`@SuppressWarnings`).
- Each literal became a `private static final` constant of its class, named by its role (`CONSTANT_NAMES.tsv`, 439
  rows; one row per occurrence in `CONSTANTS.tsv`): 345 constants. A value with two roles gets two names. The
  kernels define many literals by position (secret and input offsets, accumulator lanes, tail lengths, byte
  positions), so those names carry the position: `SECRET_24`, `LANE_3`, `TAIL_BYTE_9`, `BYTE_6_SHIFT`.
- Each constant keeps the literal's own type and spelling, so javac folds it back. All 42 classes have
  instruction-identical methods against the recipe output before this step (`tools/bytecode_identity.py`,
  `BYTECODE_IDENTITY.tsv`). A second pass makes 0 edits.
- Strings: no M3 interner is reachable from `jdk.internal.m3.hash` (the number-parse lane found the same:
  `java.lang.M3StringPool` is package-private, `jdk.internal.mindex.M3TokenIndex` indexes occurrences). The 2 message
  strings are therefore compile-time constants, which the JVM interns.
- The tool is the number-parse lane's `literal_constants.py` with three corrections, v1.1 (`RECIPE_MEMORY.tsv`).
  On the number-parse inputs it gives the same inventory and 0 edits. `bytecode_identity.py` is unchanged.

## API

Every kept member has the donor's signature (`API_SURFACE.tsv`: 95 kept, 37 dropped, 0 added) and, in the
differential test, the donor's results and exceptions for every kept hashing overload. Dropped are the members that
read raw memory through sun.misc.Unsafe and have no byte[]/ByteBuffer form: `hashMemory`, `hashBoolean`,
`hashBooleans`, `hashChars(char[])`, `hashShorts`, `hashInts`, `hashLongs` (long and tuple facades) and
`Access.unsafe()`.

## Gates

`tools/gate.sh <repo> <out dir> <junit classes> <M3GateJUnitRun classes>` on a stock JDK 21 (`M3_GATE_JDK`) with
java.base patched; results in `GATES.tsv`:

- recipe: `tools/run_absorb.sh` into a fresh directory reproduces the 16 committed sources byte for byte;
- patch compile with java.base's flags (`-Xlint:all -Werror`): 0 errors, 0 warnings;
- `tools/bytecode_identity.py`: 42/42 classes instruction-identical before and after LITERAL_CONSTANTS;
- `test/jdk/jdk/internal/m3/hash/M3HashAtomsTest.java`: 16/16, one test per atom, 3,325,487 comparisons against
  the vendored library (compiled unchanged by `M3HashOracle`) for every length 0..1100, fixed and random seeds and
  every kept hashing overload of both facades (M3LongHashFunction 15; M3LongTupleHashFunction 30, each input form
  in its long[] and its result-array form), direct buffers limited below off + len included;
- `tools/api_surface.py`: kept 95, dropped 37, added 0;
- `tools/mutation_gate.py`: 11/11 recipe-slip mutants killed (incl. the three tuple String rewrites and a direct
  buffer read through its limit, in each facade);
- `test/jdk/jdk/internal/m3/hash/M3HashAtomsPerf.java`: one row per atom, `PERF.tsv`.

## Performance: what the byte[] access costs (decision needed)

`PERF.tsv` (speedup = vendored / absorbed): byte[] hashing 0.66-0.99x of the vendored library (runs 4-5:
0.54-1.38x, host load varies), XXH3 128-bit 0.21-0.30x. C2 leaves 30 access calls in
`M3XXH3.XXH3_128bits_internal` out of line (`size > DesiredMethodLimit`): every `ByteArrayLittleEndian` read
inlines a VarHandle chain, and the 3515-byte method runs out of inlining budget.
The vendored copy, reading through Unsafe intrinsics, inlines them all.

The same rows with M3ByteArrayAccess reading through `jdk.internal.misc.Unsafe.getLong/getInt/getShort` (a
diagnostic build, not committed) run at 0.80-1.30x, XXH3 128-bit 0.94-0.98x. Only UNSAFE_TO_BYTE_ARRAY would change
(the import, the Unsafe field, BYTE_BASE and the reads). Bounds stay checked where they are checked today, at every entry
(`checkArrayOffs`, `input.length`); the byte[] access instance is package-private; and the bounds-checked run above
proves the kernels read only inside `[off, off + len)` for every length 0..1100. The leaf asked for
ByteArrayLittleEndian or VarHandles, so this change keeps them; the switch is the owner's decision.

Strings (0.25-0.29x Latin-1, 0.05-0.07x UTF-16) and direct ByteBuffers (0.14-0.17x) are slower because the String
internals and raw addresses are gone (as asked); see the next leaves.

## Open decision: recipe tools in M3JDK21

`AGENTS.md` on master (lines 8-27, 136-141) keeps recipe executors and recipe improvement memory in Synexia and admits
no new recipe assets to M3JDK21. This lane, like the number-parse lane (#2215), keeps them here as its workflow asked:
`tools/hash_jdk_form.py`, `tools/absorb_rename_generic.py`, `tools/literal_constants.py`, `tools/run_absorb.sh`,
`CONSTANT_NAMES.tsv` and `RECIPE_MEMORY.tsv`. Before
landing, the owner either amends `AGENTS.md` to admit lane recipe tools and memory (the 2026-10-31 standalone
decision) or moves them to com.synexia; M3JDK21 would then keep the gates (`tools/gate.sh`, `tools/api_surface.py`,
`tools/mutation_gate.py`, `tools/bytecode_identity.py`), the data-only receipts (`RULE_COUNTS.tsv`,
`FILE_HASHES.tsv`, `RENAME_COUNTS.tsv`, `LITERALS.tsv`, `CONSTANTS.tsv`, `BYTECODE_IDENTITY.tsv`, `API_SURFACE.tsv`,
`MUTANTS.tsv`, `GATES.tsv`, `PERF.tsv`) and a pointer to the Synexia revision.

## Next leaves

1. The byte[] access decision above.
2. Strings: read `String.value`/`coder` inside java.base without Unsafe (LATIN1 through a Latin-1-as-UTF-16 access,
   UTF16 as bytes, as the donor's ModernCompactStringHash does).
3. Direct ByteBuffers: an address-based access over ScopedMemoryAccess.
4. The public `jdk.m3.hash` API (capability catalogue family "hashing": JDK_SUPERSET_API).
5. Consumers (guava, commons-codec, lz4-java hashing) onto the JDK implementation; then the vendored copy goes.
