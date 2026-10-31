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

`tools/gate.sh <repo> <out dir> <junit classes> <M3GateJUnitRun classes>` on a stock JDK 21 (`M3_GATE_JDK`) with
java.base patched; results in `GATES.tsv`:

- recipe: `tools/run_absorb.sh` into a fresh directory reproduces the 16 committed sources byte for byte;
- patch compile with java.base's flags (`-Xlint:all -Werror`): 0 errors, 0 warnings;
- `tools/bytecode_identity.py`: 42/42 classes instruction-identical before and after LITERAL_CONSTANTS;
- `test/jdk/jdk/internal/m3/hash/M3HashAtomsTest.java`: 16/16, one test per atom, 3,325,487 comparisons against
  the vendored library (compiled unchanged by `M3HashOracle`) for every length 0..1100, fixed and random seeds and
  every kept hashing overload of both facades (M3LongHashFunction 15; M3LongTupleHashFunction 30, each input form
  in its long[] and its result-array form), direct buffers limited below off + len included;
- `test/jdk/jdk/internal/m3/hash/M3HashJitAtomsTest.java` (the JIT leaf): 19/19, one test per atom it adds or
  changes, and the bounds proof of every kernel (below);
- `tools/api_surface.py`: kept 95, dropped 37, added 0;
- `tools/mutation_gate.py`: 21/21 recipe-slip mutants killed (11 of the absorption and its review, incl. the three
  tuple String rewrites and a direct buffer read through its limit, in each facade; 10 of the JIT leaf);
- `test/jdk/jdk/internal/m3/hash/M3HashAtomsPerf.java`: one row per atom, `PERF.tsv`.

`tools/jit_gate.sh <repo> <out dir> <variant> <patch classes> <perf classes> <oracle classes>` is the JIT stage (one
JVM per path, PrintCompilation/PrintInlining and LogCompilation logs, `JIT.tsv`, `READ_COST.tsv`, `PERF.tsv` rows).

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

**1. Atomize.** `atomize_patternize.py --phase atomize` (tree-sitter L-Hash) over the 16 sources: 411 leaves (370
methods, 41 types) before, 452 (411 methods) after, 0 parse errors (`ATOMS_SUMMARY.tsv`). `tools/method_sizes.py`
sizes every method from its class file's Code attribute, cross-checked with `javap -c -p`, against HotSpot 21's
limits (MaxInlineSize 35, FreqInlineSize 325, DesiredMethodLimit 8000, HugeMethodLimit 8000; `METHOD_SIZES.tsv`): no
method passes HugeMethodLimit; `XXH3_128bits_internal` was 3515 bytes and `XXH3_64bits_internal` 2478, and the JIT
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

After the split the XXH3 kernels inlined every read, yet the mixed byte[] corpus stayed at 0.69-0.80x: diagnostic
builds (`PERF.tsv` rows `diag-*`, `diag3-*`, not committed) put the rest on the per-read bounds check, not on the
VarHandle code: d1 (each read through `jdk.internal.misc.Unsafe` behind ByteArrayLittleEndian's own
`Preconditions.checkIndex`) 0.72-0.75x, d2 (the same reads unchecked) 1.08-1.28x, d3 (a checked and an unchecked
instance chosen by a final field) 0.73-0.76x, because C2 keeps the field test in the loop. So the leaf's fallback:

- UNSAFE_TO_BYTE_ARRAY v2: the byte[] access's short names, which are what the kernels call (i64, u32, i32, u16,
  i16), read through `getLongUnaligned`/`getIntUnaligned`/`getShortUnaligned` with no check of their own. Its getters
  (`getLong`, ...) keep `ByteArrayLittleEndian`, so a direct caller still gets the same
  ArrayIndexOutOfBoundsException.
- FACADE_ENTRY_CHECK v1: every byte[] and heap ByteBuffer method of both facades reaches the kernels through the
  private `unsafeHash`, which now checks `[off, off + len)` with `Objects.checkFromIndexSize` (the public methods
  check first, so no exception changes). XXH3's secret reads use constant offsets inside the 192-byte secrets.
- ACCESS_SHORT_NAMES v1: once the byte[] access had its own short names, `M3Access.i64` was no longer monomorphic and
  a megamorphic kernel read dispatched twice (into `M3Access.i64`, then to the getter): with every input kind in one
  JVM the direct ByteBuffer rows fell from 0.41/0.38x to 0.23/0.26x (`v2-*` rows). The ByteBuffer, CharSequence and
  reverse accesses now carry the short names as their own methods (the defaults, verbatim): 0.43/0.73x.

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
0..1100 and to 4224, and matches the facade; one byte less throws. `tools/mutation_gate.py`: 21/21 killed.

**3. JIT.** `tools/jit_gate.sh`: one JVM per path with `-XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation
-XX:+PrintInlining` (text log) and `-XX:+LogCompilation` (attribution: one section per compiler thread), then a
clean run for the timings. Inlining failures at read sites in the last C2 compilation of each hashing method
(`JIT.tsv`, `JIT_SUMMARY`), absorbed side:

| path | before | after | note |
|---|---|---|---|
| xxh3-64 | 40 | 0 | every atom at tier 4 |
| xxh3-128 | 49 | 0 | was `size > DesiredMethodLimit` |
| bytes (all algorithms) | 70 | 6 | `low call site frequency` (cold) |
| string | 120 | 118 | root cause below |
| bytebuffer | 60 | 40 | `NodeCountInliningCutoff` in `XXH3_hashLong` |
| all groups in one JVM | 109 | 80 | the string and ByteBuffer sites |

Inlining budget per inlined 8-byte read (`READ_COST.tsv`, bytes charged to DesiredMethodLimit): byte[] 66 before, 48
after (plus a 10-byte bridge), vendored 17; String through the CharSequence access 511-795, vendored 17-119; direct
ByteBuffer 136-149, vendored 17-118.

`PERF.tsv` medians, speedup = vendored / absorbed, one JVM per path (`*-clean-<path>`) and all groups in one JVM
(`*-clean-all`):

| row | before | after | all in one JVM: before | after |
|---|---|---|---|---|
| xx3 byte[] 0..1100 | 0.83 | 1.07 | 0.72 | 1.21 |
| xx128low / xx128 byte[] | 0.29 / 0.32 | 1.08 / 1.06 | 0.37 / 0.38 | 1.08 / 1.16 |
| xx, wy_3, murmur_3, tuple | 0.79, 0.91, 0.81, 0.82 | 0.98, 0.99, 0.92, 0.93 | 0.86, 0.94, 0.77, 0.93 | 0.96, 0.99, 0.92, 0.96 |
| city_1_1, farmNa, farmUo, metro | 0.65, 0.79, 1.02, 0.84 | 0.87, 0.84, 1.07, 1.06 | 0.85, 0.82, 0.94, 1.05 | 0.93, 0.89, 1.11, 1.00 |
| XXH3_hashLong 64 / 128, 241..1100 | 0.81 / 0.51 | 2.30 / 2.17 | 0.68 / 0.31 | 1.10 / 1.08 |
| hashChars Latin-1 / UTF-16 | 0.31 / 0.08 | 0.34 / 0.09 | 0.35 / 0.09 | 0.38 / 0.08 |
| hashChars StringBuilder (control) | 0.39 | 0.28 | 0.42 | 0.51 |
| direct ByteBuffer BE / LE | 0.56 / 0.33 | 0.68 / 0.52 | 0.41 / 0.38 | 0.43 / 0.73 |
| read-only heap ByteBuffer (control) | 0.65 | 0.87 | 0.64 | 0.93 |

The per-length-class rows of one JVM per path (up to 5.9x) also measure the vendored monolith compiled for one class
and run on the next; the all-in-one rows are the fair comparison.

**4. Patternize.** `tools/hash_patterns.py` classifies every atom and read site into HASH-PATTERN v1 (the closed set
is in the tool: the four roles of this README and their members), with a typed residue (`PATTERNS.tsv`): 425 atoms
and 366 read sites (245 input, 90 key, 31 backing), 0 UNKNOWN_PATTERN. No version bump: the new XXH3 atoms are
ALG_LENGTH_CLASS and ALG_LENGTH_DISPATCH, patterns CityHash already had (`hashLen0To16` dispatching to
`hash1To3Bytes`, `hash4To7Bytes`, `hash8To16Bytes`). One owner where JUnit proved it: `XXH3_hashLong` serves
XXH3_hashLong_64b and the low half of XXH3_hashLong_128b, `XXH3_mergeAccs` the three merges.

**Root causes left (Strings, direct ByteBuffers).** Strings: the CharSequence access builds every 8-byte read from
up to four `CharSequence.charAt` calls (coder test and bounds check each), 511-795 budget bytes per read, so
`XXH3_hashLong` still passes DesiredMethodLimit on the String path (75 `size > DesiredMethodLimit` hits) and the
work per read is four loads; the vendored library reads `String.value` as bytes through sun.misc.Unsafe. Parity needs
a String-backed access (next leaf 2): a new class, on HOLD here. Direct ByteBuffers: each read is
`DirectByteBuffer.getLong(int)` (index check, ScopedMemoryAccess session check, byte order; a big-endian buffer adds
the reverse access), 136-149 budget bytes and many IR nodes, so `XXH3_hashLong` hits `NodeCountInliningCutoff` (40
sites, all in its last stripe and merges); the vendored library reads the buffer's address. Parity needs an
address-based access (next leaf 3), also a new class.

New tools (`method_sizes.py`, `jit_gate.sh`, `jit_inlining.py`, `hash_patterns.py`, `perf_table.py`,
`ledger_remap.py`) and the four new rule families fall under the open decision above.

## Next leaves

1. The byte[] access decision above: taken in the JIT leaf (above).
2. Strings: read `String.value`/`coder` inside java.base without Unsafe (LATIN1 through a Latin-1-as-UTF-16 access,
   UTF16 as bytes, as the donor's ModernCompactStringHash does). The JIT leaf measured why (above); it needs new code
   outside these files (a String-backed access class, or a JavaLangAccess accessor).
3. Direct ByteBuffers: an address-based access over ScopedMemoryAccess (a new access class), and then the split of
   XXH3_hashLong's last stripe and merges, which the ByteBuffer path still cuts off (NodeCountInliningCutoff).
4. The public `jdk.m3.hash` API (capability catalogue family "hashing": JDK_SUPERSET_API).
5. Consumers (guava, commons-codec, lz4-java hashing) onto the JDK implementation; then the vendored copy goes.
