<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# bitmatrix-20261106: relocate the dense bit matrix and its SWAR transpose kernel into `jdk.m3.arrays.M3BitMatrix` (recipe RELOCATE_BIT_MATRIX v3)

Third leaf of the collections/arrays convergence plan (`C:/m3-superset/conv/plans/collections-arrays.tsv`, row 4
`M3BitMatrix: dense 64x64 SWAR word transpose (Java + C JNI) + CSR transpose kernel`, decision ABSORB): M3JDK master
had no bit matrix or transpose owner (`git grep -i 'bitmatrix|transpose'` over `m3/collections/src` and `m3/arrays/src`
= 0 files). The CSR transpose kernel of the same plan row is **held** (NEC rows, see below): it is bound to a square
graph owner, not to the module's CSR owner `M3CsrIndex`.

## What it is

`M3BitMatrix` is an immutable dense Boolean matrix held as row-major `long` words (`wordsPerRow = ceil(columns / 64)`,
row tails masked). `builder(rows, columns).set(row, column[, value]).build()` copies the words once (later builder
mutation never reaches a published matrix); queries are `rows()`, `columns()`, `primitivePayloadBytes()`,
`get(row, column)`, `rowCardinality(row)` and `setColumns(row)` (the set columns in order, an `M3IntPostingList` view
over one owned `int[]`). `transpose()` is deterministic Java: when both dimensions reach 64 and a bounded probe of the
first tile (the first word of the first 64 rows) holds at least 256 bits, every full 64 x 64 tile with at least 256
bits goes through the six-stage broadword butterfly (`transposeTile64`, in place on a 64-word tile), every other tile
and every partial row or column through exact bit visits; the result is a fresh owned word array, so the matrix is
published once. `transpose(Backend)` takes `JAVA` (the default path, never loads a library) or `NATIVE_REQUIRED`: one
whole-matrix JNI batch over a detached snapshot of the words, the complete native result compared with the Java
result before publication (`IllegalStateException("MAT_BIT_MATRIX_NATIVE_PARITY_FAILURE")` otherwise), and
`UnsatisfiedLinkError("MAT_BIT_MATRIX_NATIVE_REQUIRED_UNAVAILABLE")` when no library loaded; no native pointer is
retained and nothing partial is published. The library is loaded once, at class initialization, from the system
property `m3.matmatrix.native.path` (explicit absolute path; no library search, no fallback); `nativeAvailable()`
reports it. Zero-area universes (`0 x Integer.MAX_VALUE`) allocate nothing; `rows * wordsPerRow > Integer.MAX_VALUE`
is refused (`IllegalArgumentException("matrix too large")`).

`m3_bit_matrix.c` is the C JNI kernel of the two natives: `nativeAbi0` (returns 1) and `transposeNative0(source,
rows, columns, wordsPerRow, targetWordsPerRow)`, which validates both widths and both lengths (NULL on refusal),
borrows the input and the fresh output with `GetLongArrayElements` (input released with `JNI_ABORT`), runs the same
first-tile probe, 64 x 64 broadword tile transpose (`transpose_tile64`) and scalar fallback as the Java
(`__builtin_ctzll` / `__builtin_popcountll` with portable fallbacks), and is compiled into the module's `libm3_arrays`
by the `native-linux` profile (one `target_sources` block appended to `src/main/native/CMakeLists.txt` by the recipe).

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` develop `d4985c2a46d713724b3872c85633655e53d76662` (2026-11-06); owner commits `5105ecc2139` (2026-11-02, dense 64x64 bitmatrix transpose with sparse scalar fallback, Java) and `467a2ad8846` (2026-11-02, exact 64x64 SWAR word transpose without ABI changes, C); tile oracle commit `474be0583af` (2026-11-02) |
| donor files | `synexia-mat-collections/.../MatBitMatrix.java` (blob `b242a869e88f`, 257 lines), `synexia-mat-collections/native/mat_bit_matrix.c` (blob `e6b5cb6ea27f`, 144 lines), `MatBitMatrixConvergenceContract.java` (blob `f65323e7c40c`), `MatBitMatrixTileContract.java` (blob `02572bb4ed9b`) |
| lineage check | the develop main-path blobs are exactly the blobs written by `5105ecc2139` / `467a2ad8846` and no `retained-variants/pr<N>/` copy of either file exists in the develop tree: the develop head is the newest complete lineage (the regressed-kernel situation of the posting leaf did not recur) |
| recipe | `relocate.py` v3: `python -I m3/arrays/recipe/bitmatrix-20261106/relocate.py --donor-git <clone> --donor-rev <rev> [--module m3/arrays] [--check]` |
| map | `RELOCATION_MAP.tsv`: 4 `FILE` rows, 1 `PACKAGE` row (`com.synexia.matcollections` -> `jdk.m3.arrays`), 4 `TYPE` rows (one maps `MatIntList` to the module's existing `M3IntPostingList`, relocated by PR #2512 from the same donor blob: no second copy), 1 `JNI` row (symbol prefix), 1 `LITERAL` row (the property name), 3 `NEC` rows, 3 `OMIT` rows, no `INCLUDE` / `DROP` / `EXTERNAL` row |
| provenance | `PROVENANCE.tsv`: donor commit, blob sha, donor sha256, target sha256, `body_match`, `body_match_renamed`, `declared_drops` per `FILE` row |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/arrays/`: 0 moves, 0 rewrites, 0 refusals before and after (fixed point) |

The recipe rewrites only the license header (M3JDK style, inside the C block comment too), the `package` line, mapped
`import` lines, the mapped type names (whole-word tokens), the JNI symbol prefix (`Java_jdk_m3_arrays_M3BitMatrix_*`),
the one declared `LITERAL` token, and inserts one `//FLLDCIM: RELOCATE` anchor per file naming the donor blob URL at
the donor commit. Everything else is kept byte for byte: `body_match_renamed=TRUE` for all 4 files; `javap -public`
of `M3BitMatrix`, `M3BitMatrix$Backend` and `M3BitMatrix$Builder` is identical to the donor's modulo names. The recipe
is idempotent (second run `changed=0`), appends the CMake registration once, and refuses an unmapped package, import,
header or JNI symbol and a `LITERAL` row that matches nothing.

**LITERAL row.** The owner reads `synexia.matmatrix.native.path`; `NAMING_RULES.tsv` rule `SEGMENT_DOT` renames that
dotted segment to `m3.`, so `jdk_naming.py apply` would rewrite the relocated body and break the recipe's fixed point.
The recipe applies the same rename itself (`m3.matmatrix.native.path`, declared in the map, 1 occurrence), after which
both tools agree. It is the only non-structural byte of the port; the error strings and the oracle properties
(`m3.bitmatrix.requireNative`, `m3.bitmatrix.expectMutant`) are the donor's.

**Test wrappers (TFG, module template).** The donor JUnit wrappers are not relocated (OMIT rows): the owner loads its
library in a static initializer, so the module's `m3.arrays.native.path` has to reach the owner's property before any
test touches the class. `M3BitMatrixNativePath.bridge()` (test scope) copies it once and sets
`m3.bitmatrix.requireNative`; `M3BitMatrixContractTest` calls the bridge and the oracle `main` (figures in the surefire
output); `M3BitMatrixTileContractTest` skips through `Assumptions` only when no library is configured, as the module's
other native-required tests do (a configured library that does not load fails the oracle). `M3BitMatrixDump` is the
reflective differential driver (gate 3), not a test.

### Held: the CSR transpose kernel (NEC rows, nothing copied)

`synexia-mat-collections/src/main/c/m3csrtranspose.c` (blob `bc2135fedc57`) exports
`Java_com_synexia_matcollections_MatCsrGraph_transposeTargetsNative(offsets, targets, transposedOffsets)`: a **square**
adjacency kernel (it refuses `offsets.length != transposedOffsets.length` with `MAT_CSR_NATIVE_SHAPE` and any
`target >= nodes` with `MAT_CSR_NATIVE_TARGET_BOUNDS`), hosted by `MatCsrGraph.java` (blob `bad047aab7d8`: `of(nodes,
from, to)` sorts and deduplicates packed edges into `offsets[nodes + 1]` + sorted `targets`, `transpose()` by count,
prefix sum and scatter, `transpose(NATIVE_REQUIRED)` checked against the Java rows) with the oracle
`MatCsrGraphConvergenceContract.java` (blob `59397418cff1`). The module's CSR owner is `jdk.m3.arrays.M3CsrIndex`
(PR #2499): a **rectangular** row -> reference relation (`starts`, `references`), references any non-negative ints not
bounded by `rowCount`, input order kept, duplicates kept, no transpose. The two layouts are not decided equivalent here
(NEC): landing the kernel means either a square transpose on `M3CsrIndex` (new owner code, Chrome single-file
generation when available, this kernel as the C donor and the BitSet oracle as its oracle) or a second CSR owner
`M3CsrGraph`, which the plan refuses (never two CSR owners; plan row 3 lists `MatCsrGraph` as a follow-on **on** the
`M3CsrIndex` owner). The kernel is still read (`differential.sh` copies it to the scratch as `held-csr.c`) but neither
compiled into the module nor registered.

## Build and gates (2026-11-06, see GATES.tsv)

| Step | Command | Result |
|---|---|---|
| 1 compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/arrays/pom.xml package` (module flags `-Xlint:all,-module -Werror`) | BUILD SUCCESS; 94 tests, 0 failures, 17 skipped (baseline 92 / 0 / 16; +2 wrappers, +1 native-required skip: the tile oracle without `libm3_arrays`) |
| 2 oracle in the test phase | `M3BitMatrixContractTest` runs the relocated oracle `main`; direct: `java -cp target/classes;target/test-classes jdk.m3.arrays.M3BitMatrixContract` | `MAT_BIT_MATRIX_SCENARIOS=2348`, `MAT_BIT_MATRIX_ASSERTIONS=37291284`, `MAT_BIT_MATRIX_BACKEND=JAVA`, `MAT_BIT_MATRIX_RESULT=PASS` |
| 3 differential (Java) | `bash m3/arrays/recipe/bitmatrix-20261106/differential.sh <donor clone> <rev> <scratch>` after `package` | oracle output identical (sha256 `960c31750f58eec1...`); `M3BitMatrixDump` identical over 101,652 inputs (2 zero-area universes, 300 convergence fixtures, 1,350 tile fixtures, 100,000 seeded matrices with shapes up to 4,096 x 4,096 bits), 1,169,373,724 set calls, 203,304 transposes; 34,482,295 bytes, sha256 `f64972414be2f85b...` on both sides; 60 s |
| 4 native (WSL: gcc 13.3, cmake 3.28, Temurin 21.0.13) | `bash m3/arrays/recipe/bitmatrix-20261106/native-gate.sh <scratch> <out> 100000` | javac -h headers of donor and relocated class identical modulo the name (2 prototypes); donor C and relocated C compiled with `-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared` (relocated also with `-include` of its javac -h header and with the module's `-std=c11 -Wconversion -Wshadow` flags), 2 exported symbols each; under `-Xcheck:jni` against each library: convergence oracle `MAT_BIT_MATRIX_SCENARIOS=2348` / `ASSERTIONS=46612277` / `BACKEND=JNI_PARITY`, tile oracle `MAT_TILE_SCENARIOS=1754` / `ASSERTIONS=109369518`, both outputs identical donor vs relocated; one-line mutant of each kernel refused (`MAT_BIT_MATRIX_NATIVE_MUTANT_REFUSED=PASS`); `M3BitMatrixDump` through `transpose(Backend.NATIVE_REQUIRED)` over the same 101,652 inputs identical donor vs relocated and identical to the Java lane dump of gate 3 (sha256 `6d1180c80cee0111...`); 0 JNI warnings; the module's CMake builds `libm3_arrays.so` with the registration (0 warnings), ctest 8/8, 2 bit-matrix symbols in the library, both oracles pass against it; 133 s |
| 5 scope | `git status --short \| grep -v m3/arrays` | empty; inside the module only `src/main/native/CMakeLists.txt` changes (+7 lines, the registration); `pom.xml`, `module-info.java` unchanged |
| recipe | `relocate.py --check` after apply and after `jdk_naming.py` | `files=4 held=6 changed=0 provenance=UNCHANGED` |
| all | `JAVA_HOME=<JDK 21> bash m3/arrays/recipe/bitmatrix-20261106/verify.sh <donor clone> <rev> <scratch> [m2]` (`M3_BITMATRIX_NATIVE_GATE=1` on a Linux host adds gate 4) | one `GATE` line per gate, `VERIFY PASS` |

Lessons of the run are in `RECIPE_MEMORY.tsv`.

## Next leaves (not in this PR)

- the CSR transpose: decide the owner (square transpose on `M3CsrIndex`, with `m3csrtranspose.c` as the C donor and
  `MatCsrGraphConvergenceContract` as the oracle) and land it by its own recipe; the NEC rows pin the donor blobs.
- `com.synexia` keeps `MatBitMatrixTileBench` (timing harness) and its JUnit wrappers; nothing to re-merge there.
- the module's `pom.xml` / `module-info.java` are unchanged; the kernel is built only by the `native-linux` profile.

## Files

| File | Purpose |
|---|---|
| `m3/arrays/src/main/java/jdk/m3/arrays/M3BitMatrix.java` | the relocated owner (donor body, names mapped, one literal renamed) |
| `m3/arrays/src/main/native/m3_bit_matrix.c` | the relocated C JNI kernel (two natives) |
| `m3/arrays/src/main/native/CMakeLists.txt` | `+ target_sources(m3_arrays PRIVATE m3_bit_matrix.c)` block, appended by the recipe |
| `m3/arrays/src/test/java/jdk/m3/arrays/M3BitMatrixContract.java`, `M3BitMatrixTileContract.java` | the relocated oracle mains |
| `m3/arrays/src/test/java/jdk/m3/arrays/M3BitMatrixContractTest.java`, `M3BitMatrixTileContractTest.java`, `M3BitMatrixNativePath.java` | JUnit 5 wrappers in the module's template and the native-path bridge |
| `m3/arrays/src/test/java/jdk/m3/arrays/M3BitMatrixDump.java` | reflective dump driver of the differential gate (not a test) |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table (incl. NEC / OMIT rows) and its pins |
| `differential.sh`, `native-gate.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | gates (Java differential, Linux native gate, orchestration) and their record |
