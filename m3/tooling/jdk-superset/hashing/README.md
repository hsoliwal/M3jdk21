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
| UNSAFE_TO_BYTE_ARRAY v2 | UnsafeAccess becomes the byte[] access (little-endian, index base 0); no sun.misc.Unsafe, no reflection, no pre-Nougat Android fallbacks. Its getters read through `jdk.internal.util.ByteArrayLittleEndian`, bounds-checked (v1); v2 (the JIT leaf): the short names the kernels call (i64, u32, i32, u16, i16) read through `jdk.internal.misc.Unsafe` getLongUnaligned/getIntUnaligned/getShortUnaligned with no check of their own |
| XXH3_SPLIT v1 | XXH3's input-length classes become private static atoms of M3XXH3 (the xxHash reference functions: XXH3_len_0to16, 1to3, 4to8, 9to16, 17to128, 129to240, 64 and 128 bit); one XXH3_hashLong (with XXH3_mergeAccs) for both widths; statements moved, never rewritten |
| ACCESS_SHORT_NAMES v1 | the ByteBuffer, CharSequence and reverse accesses carry the M3Access short-name defaults verbatim, so a kernel's read site dispatches once |
| FACADE_ENTRY_CHECK v1 | the private byte[] entry of both facades (`unsafeHash`) checks `[off, off + len)` with `Objects.checkFromIndexSize` before the unchecked reads |
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

`tools/gate.sh <repo> <out dir> <junit-platform-console-standalone jar>` on a stock JDK 21 (`M3_GATE_JDK`) with
java.base patched; results in `GATES.tsv`. The tests run on the stock JUnit Platform ConsoleLauncher of that jar
(1.10.1 or later), so the gates need nothing outside this repository and the JDK (they used to name a runner class,
M3GateJUnitRun, that was never committed):

- recipe: `tools/run_absorb.sh` into a fresh directory reproduces the 16 committed sources byte for byte;
- patch compile with java.base's flags (`-Xlint:all -Werror`): 0 errors, 0 warnings;
- `tools/bytecode_identity.py`: 42/42 classes instruction-identical before and after LITERAL_CONSTANTS;
- constants: `tools/literal_constants.py inventory` over the JIT leaf's tests (`M3HashJitAtomsTest`,
  `M3HashAtomsPerf`) finds no numeric literal left to name (0, 1 and -1, constant definitions and table data stay,
  the rule of the production code);
- `test/jdk/jdk/internal/m3/hash/M3HashAtomsTest.java`: 16/16, one test per atom, 3,325,487 comparisons against
  the vendored library (compiled unchanged by `M3HashOracle`) for every length 0..1100, fixed and random seeds and
  every kept hashing overload of both facades (M3LongHashFunction 15; M3LongTupleHashFunction 30, each input form
  in its long[] and its result-array form), direct buffers limited below off + len included;
- `test/jdk/jdk/internal/m3/hash/M3HashJitAtomsTest.java` (the JIT leaf): 19/19, one test per atom it adds or
  changes, and the bounds proof of every kernel (below);
- `tools/api_surface.py`: kept 95, dropped 37, added 0;
- `tools/mutation_gate.py`: 21/21 recipe-slip mutants killed (11 of the absorption and its review, incl. the three
  tuple String rewrites and a direct buffer read through its limit, in each facade; 10 of the JIT leaf); a mutant
  run counts only when it found tests (an empty run is ERROR_NO_TESTS, never a kill);
- `test/jdk/jdk/internal/m3/hash/M3HashAtomsPerf.java`: one row per atom, `PERF.tsv`.

`tools/jit_gate.sh <repo> <out dir> <variant> <patch classes> <perf classes> <oracle classes>` is the JIT stage (one
JVM per path, PrintCompilation/PrintInlining and LogCompilation logs, `JIT.tsv`, `READ_COST.tsv`, `PERF.tsv` rows;
`M3_JIT_CLEAN=0` skips the timing runs). Repeated runs `<variant>-r<n>` are joined by `tools/jit_inlining.py join`
into ranges; `tools/jit_inlining.py roots` writes `ROOTS.tsv` (the methods above FreqInlineSize);
`tools/perf_variants.py` rebuilds and runs the decision variants; `tools/perf_summary.py` gives the figures this
README quotes from `PERF.tsv`.

## Performance: what the byte[] access costs (decided in the JIT leaf below)

The record of the absorption (PR #2232), kept as it was measured; the JIT leaf below took the decision.

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

Since the JIT leaf's fix that alternative is reproducible: `tools/perf_variants.py` variant `unsafe` (the base with
the JIT leaf's byte[] access, XXH3 not split) against variant `base`, interleaved, rows `repro-unsafe-r1/r2` and
`repro-base-r1/r2` (medians of interleaved rounds, where the record above was the best of 7 rounds of a scratch
build): byte[] 0.79-1.14x against 0.30-1.03x, `xx128() hashBytes(byte[])` 0.80/0.84x against 0.37/0.36x.

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

## The JIT leaf: atomize, JUnit, JIT, patternize

Stacked on the absorption (base `m3/jdk-fast-hashing-20261031`). Four stages in this order, one `GATES.tsv` row each
(`stage1_atomize` .. `stage4_patternize`). Host: Windows 11, stock JDK 21.0.9 with java.base patched, shared with
other sessions (the rows carry the measured system load).

**1. Atomize.** `atomize_patternize.py --phase atomize` (tree-sitter L-Hash; com.synexia's converge tools,
`atomize_patternize.py` sha256 491d2652.., `cpu_lhash_treesitter.py` 175b15e8.., tree-sitter 0.26.0,
tree-sitter-language-pack 1.12.5) over the 16 sources: 411 leaves (370 methods, 41 types) before (the base,
827c4b65f73), 452 (411 methods, 41 types) after, 0 parse errors. `tools/atoms_summary.py` writes
`ATOMS_SUMMARY.tsv` from the atomizer's own PART.json, FILES.tsv and atom table: per file, M3XXH3 gains 14 methods
(12 length-class atoms, XXH3_hashLong, XXH3_mergeAccs) and the four access files 27 (M3Access 7,
M3ByteArrayAccess 6, M3ByteBufferAccess 7, M3CharSequenceAccess 7). The atomizer's
worker faults (native faults of the parser binding; it re-runs the batch in halves) vary from run to run: three runs
of the base gave 5, 0 and 1 faults and the same atom table (sha256 of ATOMS.tsv), the leaf 0. `tools/method_sizes.py`
sizes every method from its class file's Code attribute, cross-checked with `javap -c -p`, against HotSpot 21's
limits (MaxInlineSize 35, FreqInlineSize 325, DesiredMethodLimit 8000, HugeMethodLimit 8000; `METHOD_SIZES.tsv`,
both variants: 419 methods in the base's class files, 493 in the leaf's): no method passes HugeMethodLimit;
`XXH3_128bits_internal` was 3515 bytes and `XXH3_64bits_internal` 2478, and the JIT
stage found their read sites out of line, so XXH3_SPLIT v1 splits them in the recipe, one atom per input-length
class of the xxHash reference (the donor's own marker comments name them):

| atom (64 / 128 bit) | input lengths | bytecode bytes |
|---|---|---|
| `XXH3_64bits_internal` / `XXH3_128bits_internal` | dispatch | 76 / 83 |
| `XXH3_len_0to16_64b` / `_128b` | dispatch: 1to3, 4to8, 9to16, length 0 | 89 / 145 |
| `XXH3_len_1to3`, `4to8`, `9to16` | 1..3, 4..8, 9..16 | 104 / 179, 89 / 177, 116 / 202 |
| `XXH3_len_17to128` | 17..128 | 207 / 503 |
| `XXH3_len_129to240` | 129..240 | 144 / 492 |
| `XXH3_hashLong` (both widths) | above 240 | 1717 |
| `XXH3_mergeAccs` (the three merges) | | 61 |

After the split the XXH3 kernels inlined every read, yet the mixed byte[] corpus (row `xx3() hashBytes(byte[])`,
lengths 0..1100) stayed at 0.69-0.80x. Diagnostic builds put the rest on the per-read bounds check, not on the
VarHandle code. They were scratch builds at first (`PERF.tsv` rows `diag-*`, `diag3-*`); `tools/perf_variants.py`
now rebuilds every variant from the repository (the base commit by `git show`, the leaf's files, anchored edits of
M3ByteArrayAccess that must each match) and runs them interleaved, on the merged build, groups bytes, xxh3-64 and
xxh3-128 in one JVM (`PERF.tsv` rows `repro-<variant>-r1`, `-r2`):

| variant | change | xx3 byte[], scratch | xx3 byte[], reproduced r1 / r2 | bytes group median, r1 / r2 |
|---|---|---|---|---|
| split | XXH3_SPLIT v1 alone (reads through ByteArrayLittleEndian) | 0.72-0.75 | 0.69 / 0.74 | 0.83 / 0.80 |
| d1 | split + each read through `jdk.internal.misc.Unsafe` behind ByteArrayLittleEndian's own `Preconditions.checkIndex` | 0.72-0.75 | 0.71 / 0.74 | 0.80 / 0.85 |
| d2 | split + the same reads unchecked | 1.08-1.28 | 1.07 / 1.10 | 1.04 / 1.08 |
| d3 | d2 with a checked and an unchecked instance, a final field choosing per read | 0.73-0.76 | 1.09 / 1.03 | 1.04 / 1.03 |
| v2 | d2 + FACADE_ENTRY_CHECK v1 (the leaf without ACCESS_SHORT_NAMES v1) | - | 1.05 / 1.05 | 1.05 / 1.05 |
| after | the leaf | - | 1.08 / 1.08 | 1.06 / 1.04 |

d1 and d2 reproduce: the check costs, the Unsafe read does not. d3 does not reproduce. The scratch d3 was not kept,
and the rebuild from its description (a final boolean field per instance, the kernels holding the unchecked
instance) runs as fast as d2, so the reason given before ("C2 keeps the field test in the loop") does not hold for
this form. The decision rests on d1 against d2. So the leaf's fallback:

- UNSAFE_TO_BYTE_ARRAY v2: the byte[] access's short names, which are what the kernels call (i64, u32, i32, u16,
  i16), read through `getLongUnaligned`/`getIntUnaligned`/`getShortUnaligned` with no check of their own. Its getters
  (`getLong`, ...) keep `ByteArrayLittleEndian`, so a direct caller still gets the same
  ArrayIndexOutOfBoundsException.
- FACADE_ENTRY_CHECK v1: every byte[] and heap ByteBuffer method of both facades reaches the kernels through the
  private `unsafeHash`, which now checks `[off, off + len)` with `Objects.checkFromIndexSize` (the public methods
  check first, so no exception changes). XXH3's secret reads use constant offsets inside the 192-byte secrets.
- ACCESS_SHORT_NAMES v1: once the byte[] access had its own short names, `M3Access.i64` was no longer monomorphic and
  a megamorphic kernel read dispatched twice (into `M3Access.i64`, then to the getter; the leaf's reading of its
  first record, where with every input kind in one JVM the direct ByteBuffer rows fell from 0.41/0.38x to 0.23/0.26x,
  rows `v2-*`). The ByteBuffer, CharSequence and reverse accesses now carry the short names as their own methods
  (the defaults, verbatim). Rebuilt by `tools/perf_variants.py` with every group in one JVM (rows `reproall-*`, two
  rounds, direct ByteBuffer big-endian / little-endian): split 0.38-0.39 / 0.41-0.42, v2 0.34-0.47 / 0.36-0.55, the
  leaf 0.39 / 0.75-0.76; the read-only heap ByteBuffer control 0.77-0.88, 0.61-0.74, 0.93-0.94. So the short names
  help the little-endian and the heap buffer; the drop to 0.23/0.26 does not reproduce, and the big-endian buffer
  stays near 0.4 either way.

The review's LITERAL_CONSTANTS step names the literals per member occurrence, so the split would have renamed every
moved literal to its value's class-wide name (a secret offset 16 as MIX_BYTES); `tools/ledger_remap.py` rewrites the
split members' ledger rows by statement (327 rows, a fixed point), and `tools/bytecode_identity.py` still finds every
method instruction-identical to the recipe output before the constants.

**2. JUnit.** `M3HashAtomsTest` is unchanged and passes (both oracle paths). `M3HashJitAtomsTest`, 19/19, 954,133
comparisons: one test per new or changed atom (the twelve length-class atoms, `XXH3_hashLong`, `XXH3_mergeAccs`
against a fold over `Math.unsignedMultiplyHigh`, both dispatchers), each called directly through every access form,
lengths of its class (hashLong to 4224, every block edge), fixed and random seeds; `XXH3_hashLong`'s test proves the
one-owner claim on the vendored side (xx3 equals the low half of xx128 for every tested input). The read atom
against the checked getters and a little-endian ByteBuffer; the entry checks with eight out-of-range shapes per
facade; and the bounds proof, which no longer comes from the byte[] access: every kernel of every algorithm runs
through a test access whose every read is a bounds-checked array read, over exact-length arrays of every length
0..1100 and to 4224, and matches the facade; one byte less throws. `tools/mutation_gate.py`: 21/21 killed. The
test's limits are named constants (user rule: no magic numbers): the length-class bounds, the stripe, the merge's
secret offsets, the lane pairs, the secret kinds, the byte mask and the proof seeds, and `Character.BYTES`,
`Byte.SIZE`, `Long.BYTES` where the JDK names them; the eight accumulators reach `XXH3_mergeAccs` through a spreader
(`MethodHandle.asSpreader`). The gate's constants step finds no numeric literal left to name in it or in
`M3HashAtomsPerf`; the count is still 954,133.

**3. JIT.** `tools/jit_gate.sh`: one JVM per path with `-XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation
-XX:+PrintInlining` (text log) and `-XX:+LogCompilation` (attribution: one section per compiler thread), then a
clean run for the timings. C2's inlining follows the run's profile, so the counts move a little from run to run; the
record is three runs of each side on the merged build, interleaved (`before-r1..r3` = the base 827c4b65f73,
`after-r1..r3` = this leaf), joined by `tools/jit_inlining.py join` into `JIT.tsv`, where a cell is the value of
every run or its min..max. Inlining failures at read sites in the last C2 compilation of each hashing method, summed
per path (the total rows of `JIT.tsv`), absorbed side:

| path | before (3 runs) | after (3 runs) | note |
|---|---|---|---|
| xxh3-64 | 40 | 0 | every XXH3 atom at tier 4 in every run |
| xxh3-128 | 49 | 0 | before: `size > DesiredMethodLimit` 25..49 |
| bytes (all algorithms) | 68..70 | 6 | after: `low call site frequency` (cold) |
| string | 120..123 | 120..124 | no change; root cause below |
| bytebuffer | 59..61 | 46..51 | after: `NodeCountInliningCutoff` 35..38, in `XXH3_hashLong` |
| access+kernel | 18 | 18 | `low call site frequency` |
| all groups in one JVM | 103..130 | 74..86 | the string and ByteBuffer sites |

The leaf's first record (one run each, before the merge of the review commits) had xxh3-64 40 -> 0, xxh3-128
49 -> 0, bytes 70 -> 6, string 120 -> 118, bytebuffer 60 -> 40 and all 109 -> 80; its verification measured
bytebuffer 61 -> 46 and all 112 -> 70. The byte[] figures repeat exactly; the others are inside the ranges above or
next to them, which is why the record is now ranges.

Inlining budget per inlined 8-byte read (`READ_COST.tsv`, bytes charged to DesiredMethodLimit, every run): byte[] 66
before, 48 after (plus a 10-byte bridge), vendored 17; String through the CharSequence access 511-795 before,
511-521 after, vendored 17-119; direct ByteBuffer 126-149, vendored 17-118.

`PERF.tsv` medians on the merged build (runs `before-r1-clean-*`, `after-r1-clean-*`), speedup = vendored /
absorbed, one JVM per path and all groups in one JVM (`*-clean-all`):

| row | before | after | all in one JVM: before | after |
|---|---|---|---|---|
| xx3 byte[] 0..1100 | 0.74 | 1.10 | 0.75 | 1.07 |
| xx128low / xx128 byte[] | 0.34 / 0.35 | 1.08 / 1.14 | 0.33 / 0.37 | 1.11 / 1.08 |
| xx, wy_3, murmur_3, tuple | 0.86, 0.95, 0.71, 0.76 | 0.99, 1.04, 1.03, 1.02 | 0.86, 0.96, 0.65, 0.63 | 1.02, 0.98, 1.00, 1.08 |
| city_1_1, farmNa, farmUo, metro | 0.84, 0.89, 0.85, 1.01 | 0.95, 0.86, 1.08, 1.12 | 0.90, 0.79, 0.94, 1.01 | 0.98, 0.92, 1.05, 1.20 |
| XXH3_hashLong 64 / 128, 241..1100 | 0.72 / 0.39 | 2.03 / 1.83 | 0.69 / 0.34 | 1.15 / 1.08 |
| hashChars Latin-1 / UTF-16 | 0.29 / 0.07 | 0.35 / 0.09 | 0.31 / 0.09 | 0.35 / 0.08 |
| hashChars StringBuilder (control) | 0.42 | 0.52 | 0.49 | 0.55 |
| direct ByteBuffer BE / LE | 0.55 / 0.36 | 0.66 / 0.49 | 0.40 / 0.42 | 0.39 / 0.71 |
| read-only heap ByteBuffer (control) | 0.80 | 0.84 | 0.82 | 0.90 |

The per-length-class rows of one JVM per path (up to 7.7x) also measure the vendored monolith compiled for one class
and run on the next; the all-in-one rows are the fair comparison. The leaf's first record (rows `before-clean-*`,
`after-clean-*`, before the merge) is still in `PERF.tsv`.

**Methods above FreqInlineSize.** After the split 11 methods are larger than FreqInlineSize (325 bytes), the limit
for inlining a hot callee into its caller; 73 are larger than MaxInlineSize (35), the limit for a callee that is not
hot. The leaf split the methods whose own read sites C2 left out of line (both XXH3 monoliths), not every method
above 325 bytes. `ROOTS.tsv` (`tools/jit_inlining.py roots`, the three all-in-one runs of the leaf) shows what the
others cost:

- the seven donor kernels (`wyHash64` 1411 bytes, `uoHash64WithSeeds` 973, `naHash64` 966, `cityHash64` 875,
  `metroHash64` 653, `xxHash64` 637, `MurmurHash_3.hash` 555) have the vendored sizes (naHash64: 961 there), reach
  tier 4 as their own compilations, and their callers keep them out of line with the same reasons and counts on both
  sides (wyHash64: `hot method too big` 15 absorbed, 15 vendored). The cost is one call per hash, as in the
  library; their rows run at 0.86-1.20x of it;
- `XXH3_hashLong` (1717), `XXH3_len_17to128_128b` (503) and `XXH3_len_129to240_128b` (492) reach tier 4 and stay
  out of line in their dispatchers (`hot method too big`, `too big`, `already compiled into a medium method`); the
  vendored monoliths they came from (`XXH3_64bits_internal` 2705 bytes, `XXH3_128bits_internal` 3850) were out of
  line too. Their per-class rows run at 1.08-7.7x of the library;
- `M3XXH3.<clinit>` (1227) runs once, interpreted (never compiled).

Splitting those further (length-class atoms for the seven donor kernels, the 128-bit classes below 325 bytes) would
let a caller inline the short classes; it is next leaf 6, not a defect of this one.

**4. Patternize.** `tools/hash_patterns.py` classifies every atom and read site into HASH-PATTERN v1 (the closed set
is in the tool: the four roles of this README and their members), with a typed residue (`PATTERNS.tsv`): 425 atoms
and 366 read sites (245 input, 90 key, 31 backing), 0 UNKNOWN_PATTERN. No version bump: the new XXH3 atoms are
ALG_LENGTH_CLASS and ALG_LENGTH_DISPATCH, patterns CityHash already had (`hashLen0To16` dispatching to
`hash1To3Bytes`, `hash4To7Bytes`, `hash8To16Bytes`). One owner where JUnit proved it: `XXH3_hashLong` serves
XXH3_hashLong_64b and the low half of XXH3_hashLong_128b, `XXH3_mergeAccs` the three merges.

**After the leaf's verification.** The verification of PR #2248 raised eight points; this is what changed. The
tests' magic numbers are named, and the gate checks it (constants). The leaf's tables carry the SPDX lines. The gates
run on the stock JUnit ConsoleLauncher instead of a runner class the repository never had. The decision variants are
rebuilt from the repository by `tools/perf_variants.py`: d1 and d2 reproduce, d3 does not, and ACCESS_SHORT_NAMES'
gain reproduces for the little-endian and heap buffers but not its drop to 0.23/0.26. The JIT counts are
ranges over three runs of each side on the merged build. The methods above FreqInlineSize are measured
(`ROOTS.tsv`, next leaf 6). Stage 1 ran again with the atomizer's current version and gave the same atom counts.
`tools/gate.sh` ran as one command. Two tool faults surfaced while re-running and are fixed: `jit_inlining.py`
stopped on a LogCompilation element that the VM's exit had cut off (a `<fragment>`), and `method_sizes.py` and
`hash_patterns.py` now carry the variant column that was added to `METHOD_SIZES.tsv` by hand.

**After the second verification (2026-11-01).** Two points were raised on 6269b33f07. (g) The bounds and the XXH3
geometry the tests restate carry M3XXH3's names where M3XXH3 names the same quantity: `SHORT_INPUT_MAX` (the end
of XXH3_len_0to16 and of its 9..16 class), `LEN_4TO8_MIN`, `LEN_4TO8_MAX`, `LEN_17TO128_MAX`, `MIDSIZE_MAX`,
`STRIPE_LEN`, `SECRET_SIZE`, `SECRET_MERGEACCS_START` and `MIX_BYTES`; `MIDSIZE_MIN` names the first midsize
length, and bounds M3XXH3 does not name keep their `LEN_<class>_MIN/MAX` names. The values are restated in the
tests, never read from M3XXH3, so a slipped production bound still fails a test; javac folds the constants, and
the 167 methods of the test classes are instruction-identical before and after (per-method javap, constant-pool
indexes removed). (h) The gates already ran on the stock ConsoleLauncher at 6269b33f07; no file of the branch
names M3GateJUnitRun any more. `tools/gate.sh` ran as one command in a fresh blobless sparse clone of the branch
that holds only this lane, the hash sources and tests, the legal file and the three vendored donor trees
(zero-allocation-hashing, annotations, jsr305), with nothing outside the repository but JDK 21.0.9 and
junit-platform-console-standalone 1.11.4: gate.sh exit 0 (4 min 29 s): recipe reproduces the 16 committed sources; patch 42 classes; BYTECODE_IDENTITY 42/42 IDENTICAL; constants: 0 numeric literals left to name; M3HashAtomsTest 16/16 twice (prebuilt oracle, test.src); M3HashJitAtomsTest 19/19 twice, 954,133 comparisons; API surface kept 95, dropped 37, added 0; mutation_gate.py 21/21 killed; MUTANTS.tsv, API_SURFACE.tsv and BYTECODE_IDENTITY.tsv byte-identical to the committed tables; M3HashAtomsPerf 41 rows (evidence).

**Root causes left (Strings, direct ByteBuffers).** Strings: the CharSequence access builds every 8-byte read from
up to four `CharSequence.charAt` calls (coder test and bounds check each), 511-795 budget bytes per read, so
`XXH3_hashLong` still passes DesiredMethodLimit on the String path and the work per read is four loads. Counted two
ways, the same in all three runs: in the last C2 compilation of `XXH3_hashLong`, 42 read sites fail on
`size > DesiredMethodLimit` and 32 on `low call site frequency` (`JIT.tsv`); in the PrintInlining text log, 75 lines
that name a hashing method carry `size > DesiredMethodLimit` (the leaf's figure; its verification reported 95,
which this filter gives in none of the three runs); the vendored library reads `String.value` as bytes through
sun.misc.Unsafe. Parity needs
a String-backed access (next leaf 2): a new class, on HOLD here. Direct ByteBuffers: each read is
`DirectByteBuffer.getLong(int)` (index check, ScopedMemoryAccess session check, byte order; a big-endian buffer adds
the reverse access), 126-149 budget bytes and many IR nodes, so C2 stops inlining on `NodeCountInliningCutoff` at
35..38 read sites, 33..35 of them in `XXH3_hashLong` (12 secret reads, 10 or 11 buffer reads, 10 reverse-access
reads, and in one run three inner java.nio reads); the vendored library reads the buffer's address. Parity needs an
address-based access (next leaf 3), also a new class.

New tools (`method_sizes.py`, `jit_gate.sh`, `jit_inlining.py`, `hash_patterns.py`, `perf_table.py`,
`ledger_remap.py`; with the fix after the leaf's verification `atoms_summary.py`, `perf_variants.py`,
`perf_summary.py`) and the four new rule families fall under the open decision above. The tables the leaf added
(`ATOMS_SUMMARY.tsv`, `JIT.tsv`, `METHOD_SIZES.tsv`, `PATTERNS.tsv`, `READ_COST.tsv`, `ROOTS.tsv`) start with the
SPDX lines as `#` comments, which every reader skips (`PERF.tsv`'s own convention); their tools write them.

## Next leaves

1. The byte[] access decision above: taken in the JIT leaf (above).
2. Strings: read `String.value`/`coder` inside java.base without Unsafe (LATIN1 through a Latin-1-as-UTF-16 access,
   UTF16 as bytes, as the donor's ModernCompactStringHash does). The JIT leaf measured why (above); it needs new code
   outside these files (a String-backed access class, or a JavaLangAccess accessor).
3. Direct ByteBuffers: an address-based access over ScopedMemoryAccess (a new access class), and then the split of
   XXH3_hashLong's last stripe and merges, which the ByteBuffer path still cuts off (NodeCountInliningCutoff).
4. The public `jdk.m3.hash` API (capability catalogue family "hashing": JDK_SUPERSET_API).
5. Consumers (guava, commons-codec, lz4-java hashing) onto the JDK implementation; then the vendored copy goes.
6. Caller-side inlining of the remaining methods above FreqInlineSize (`ROOTS.tsv`): length-class atoms for the seven
   donor kernels (as XXH3_SPLIT did for XXH3; CityHash already has its short classes as methods) and the 128-bit
   XXH3 classes below 325 bytes, so that a hot caller can inline a short input's whole hash. Today these cost one
   call per hash, as in the vendored library.
