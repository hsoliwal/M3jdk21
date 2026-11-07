<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe lint-storage-20261107: byte-aligned integer widths and 48-bit lint arithmetic

Census pass-2 leaf P2-105 of the M3JDK convergence programme (crate `lint-storage-20261029`, rows NO_OWNER for
`IntWidth`, `Lint`, `AdaptiveIntArray`, `PackedIntNative` and their tests; `PackedIntArray` / `VInt` matched the
module's `jdk.m3.arrays.packed` owners by name only). Relocates, by recipe, the part of the family whose closure is
free of undecided owners:

- `jdk.m3.arrays.M3IntWidth`: enum of the five byte-aligned widths `INT32`, `INT40`, `INT48`, `INT56`, `INT64` with
  `bits()`, `bytes()`, `mask()`, `minValue/maxValue(signed)` (unsigned INT64 is the raw bits of a `long`),
  `fits` / `require(value, signed)` (`ArithmeticException`) and `smallest(value, signed)`.
- `jdk.m3.arrays.M3Lint`: signed 48-bit ("lint") arithmetic on primitive `long` values: range constants,
  `check` / `checkUnsigned`, `add/subtract/multiply/divide/negate/absExact` (operands and result checked), `wrap`
  (low 48 bits, sign-extended) and `saturate`; no value objects.

| Target | Scope | Donor (hsoliwal/com.synexia develop `165ceb5a0202`) |
|---|---|---|
| `M3IntWidth` | main | `synexia-common/.../collections/IntWidth.java`, blob `150e2580b487` (= crate postimage) |
| `M3Lint` | main | `synexia-common/.../collections/Lint.java`, blob `a95f6ae3e10f` (= crate postimage) |
| `M3LintTest` | test | `synexia-common/src/test/.../collections/LintTest.java`, blob `fecd553b3d88` (= crate postimage) |
| `M3LintStorageReference` | test | authored: BigInteger reference model + the donor `randomValue` helper (TYPE row) |
| `M3LintStorageDump` | test | authored: reflective donor-vs-relocated dump with the reference check |
| `M3LintStorageReferenceTest` | test | authored: JUnit wrapper of the dump against the reference model |

## Decisions

| Donor class | Decision | Why |
|---|---|---|
| `IntWidth`, `Lint`, `LintTest` | ABSORB (relocated) | no owner on master, no open PR with the names |
| `PackedIntArray` (+ test) | NEC, held | module owner `jdk.m3.arrays.packed.PackedIntArray` is another lineage (sealed Int40/48/56, signed only, no monitor) |
| `VInt` (+ test) | NEC, held | module owner `jdk.m3.arrays.packed.VInt` is another lineage (fresh-array codec, no `MAX_BYTES`) |
| `AdaptiveIntArray` (+ test) | HELD | closure = NEC `PackedIntArray` + `ProgressMonitor` (no dependency of m3/arrays) |
| `PackedIntNative` (+ test, `packed_int_width.c`) | DEFERRED | closure = NEC `VInt` + `ProgressMonitor`; the WSL native gate is not run |
| `LintStorageBenchmark`, `M3LintRecipe` | OMIT | JMH harness over held classes; crate OpenRewrite recipe |

`M3PrimitiveBits` (jdk.m3.collections) and `Int48Array` (jdk.m3.arrays.packed) are listed as NEC module owners: their
overlap with `M3IntWidth` / `M3Lint` is not decided and nothing of them changes. The `M3Lint` class javadoc keeps the
donor's `{@link PackedIntArray#lint(int)}` sentence (byte-for-byte body); it names the held donor storage and does not
resolve under `-Xdoclint` (GATES.tsv `doclint_note`).

## Run

```
python -I m3/arrays/recipe/lint-storage-20261107/relocate.py --donor-git <com.synexia clone> --donor-rev 165ceb5a020291e99ebeb89783b88e8a9686ae5c [--check]
bash m3/arrays/recipe/lint-storage-20261107/verify.sh <com.synexia clone> 165ceb5a020291e99ebeb89783b88e8a9686ae5c <scratch> <baseline m3/arrays build> <junit-platform-console-standalone.jar>
```

`relocate.py` and the gate scripts `javap_parity.sh` / `class_identity.sh` are copies of
`int-ring-buffer-20261106` (only the relocate.py docstring names this leaf); `differential.sh` adds the crate-test run on
both sides (JUnit console launcher, donor closure fetched from the donor tree) next to the JIT and `-Xint` dumps.
Results: GATES.tsv; provenance: PROVENANCE.tsv; lessons: RECIPE_MEMORY.tsv.
