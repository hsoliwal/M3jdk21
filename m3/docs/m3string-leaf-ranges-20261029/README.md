# M3String leaf ranges for native kernels (unix4j native, leaf 1b)

Status: **implemented, compile-proven, component oracle PASS on stock JDK 21 (Windows and
Linux)**. `make images` and jtreg on an M3JDK21 image: **NOT_RUN** (no local build toolchain or
M3 image). Base: `master` 7f7accdd54e2. Branch: `m3/m3string-leaf-ranges-20261029`.

Leaf 1 (the `wc` C kernel and Java facade in `m3/tooling/module-packs/text-tools`, branch
`m3/unix4j-native-wc-20261029`) consumes this contract. This leaf changes java.base only; the
module pack, `m3textio.c` and leaf 1's branch are untouched.

## Contract

`jdk.internal.access.JavaLangAccess`:

```java
int stringM3LeafRanges(String text, long[] lanes);
```

- Leaves are the scalar owner ranges of the String's canonical M3 DAG in logical order. Tuples
  are flattened left to right by the existing `M3StringLeafCursor`, with adjacent ranges of one
  owner coalesced. No per-leaf object is created; one cursor is allocated per call.
- Returns `M3StringLeafLanes.NOT_M3` (`-1`) when the String has no M3 storage (stock layout, or
  storage not admitted). The call never admits storage.
- Otherwise returns the total leaf count `n` (`0` for empty text) and writes only the first
  `min(n, lanes.length / STRIDE)` leaves; no other element is touched. A caller whose lanes were
  too small retries with `STRIDE * n` longs (the String is immutable, so `n` is stable).
- `NullPointerException` for a null `text` or `lanes`.
- **Lifetime:** an address is valid only while `text` is strongly reachable. The caller keeps
  `text` reachable for as long as it uses any returned address, e.g.
  `Reference.reachabilityFence(text)` after the native call returns. Evidence: `String.m3` is
  set at most once and never replaced (`String.ensureM3`); a reachable M3String keeps its owner
  DAG; VM-local atom blocks are freed only after the atom is weakly cleared
  (`M3StringPool.LocalRef.releaseNative` via `expungeLocals`); mapped atoms keep `payloadOwner`.
- **Read-only:** addresses name shared canonical storage (mapped lexicon pages or interned
  VM-local blocks). A write is undefined behavior that may fault or corrupt every String sharing
  that owner.

Lane layout (`JavaLangAccess.M3StringLeafLanes`, `STRIDE = 2` longs per leaf):

| word | ATOM leaf | INLINE leaf (IMMEDIATE owner) |
|---|---|---|
| `lanes[2i + ADDRESS]` | address of the first visible unit = `atom.address + (start << (width - 1))` | units 0..7, unit k in bits `8k..8k+7`, zero padded |
| `lanes[2i + DESCRIPTOR]` bits 0..31 | unit count (UTF-16 code units, at least 1) | unit count (1..10) |
| bits 32..39 | width in bytes: 1 (Latin-1) or 2 (UTF-16) | 1 |
| bit 40 `BIG_ENDIAN` | set only for big-endian width-2 atoms | 0 |
| bit 41 `INLINE` | 0 | 1 |
| bits 42..47 | 0 | 0 |
| bits 48..63 | 0 | units 8..9, unit `8 + j` in bits `48 + 8j..55 + 8j`, zero padded |

IMMEDIATE owners hold packed words in a Java `long[]` page that the GC may move, so they have no
stable address. The chosen answer decodes them into the lanes (at most 10 Latin-1 units,
`M3StringImmediate.LATIN1_MAX = 7`, `IDENT_MAX = 10`), so the native kernel needs no Java
callback and no second buffer. Width-1 leaves never set `BIG_ENDIAN` (canonical descriptor even
for a mapped Latin-1 atom created with `bigEndian = true`). ATOM addresses carry no alignment
guarantee (mapped lexicons, slices), so readers load bytes.

Files:

| file | change | sha256 before | sha256 after |
|---|---|---|---|
| `src/java.base/share/classes/jdk/internal/access/JavaLangAccess.java` | `M3StringLeafLanes` constants + `stringM3LeafRanges` | `ae16c88bd1009076911f7188cce6c20ce16072df9a9caca85ae453d205ea9a39` | `f8d5f581594d267158fca11c50719b0a789972fabdd0832f0327ff49d01b25cd` |
| `src/java.base/share/classes/java/lang/System.java` | JavaLangAccess implementation (delegates) | `7b23f8cde934a51e4b133c8850c641f89ffefc5a70135884f41a19dcc0d85734` | `cdc978754049a603128915c764ab343c71c544abd27ae77f7d0dcc6d26063778` |
| `src/java.base/share/classes/java/lang/M3StringLeafRanges.java` | new M3String-side helper | absent | `8b5e74f4abb9564607a07d580a47c77c7f757f05ec0e81f8f0db0a83d578df5c` |
| `test/jdk/java/lang/String/M3StringLeafRanges/java.base/java/lang/M3StringLeafRangesOracle.java` | new oracle (patched into java.base) | absent | `a5ab75e77adddd7950c966345aa434972cc84be47a4262772f6d36d5504347fd` |
| `test/jdk/java/lang/String/M3StringLeafRanges/M3StringLeafRangesDriver.java` | new jtreg driver | absent | `ec142683cf454c8a7188ea7708bf0bbe9cb302e16754ee51637197909d00e335` |

No field layout changed: `M3String`, `M3StringOwner`, `M3StringAtom`, `M3StringTuple`,
`M3StringImmediate` and `String` are untouched (HotSpot `javaClasses.cpp` offsets unaffected).
Reused owner: `M3StringLeafCursor` (blob `5ac0eee6e2a1`). No Apache-licensed code was copied.

## How the C kernel consumes the lanes

Java facade (leaf 1, in `jdk.m3.text`):

```java
long[] lanes = new long[M3StringLeafLanes.STRIDE * 16];
int n = JLA.stringM3LeafRanges(text, lanes);
if (n == M3StringLeafLanes.NOT_M3) return javaPath(text);     // stock-layout String
if (n > lanes.length / M3StringLeafLanes.STRIDE) {
    lanes = new long[M3StringLeafLanes.STRIDE * n];
    n = JLA.stringM3LeafRanges(text, lanes);
}
try {
    return wc0(lanes, n);                                      // native
} finally {
    Reference.reachabilityFence(text);                         // keeps every address valid
}
```

C side (`GetLongArrayRegion` or a critical region over `lanes`; no JNI call while critical):

```c
for (jint i = 0; i < n; i++) {
    uint64_t addr = (uint64_t) lanes[2 * i];
    uint64_t desc = (uint64_t) lanes[2 * i + 1];
    uint32_t units = (uint32_t) desc;
    unsigned width = (unsigned) ((desc >> 32) & 0xff);
    int big = (int) ((desc >> 40) & 1), inl = (int) ((desc >> 41) & 1);
    for (uint32_t k = 0; k < units; k++) {
        uint16_t u;
        if (inl) {
            u = (uint16_t) (k < 8 ? (addr >> (8 * k)) & 0xff : (desc >> (48 + 8 * (k - 8))) & 0xff);
        } else if (width == 1) {
            u = ((const uint8_t *) (uintptr_t) addr)[k];
        } else {
            const uint8_t *p = (const uint8_t *) (uintptr_t) addr + 2 * (size_t) k;
            u = (uint16_t) (big ? (p[0] << 8 | p[1]) : (p[1] << 8 | p[0]));
        }
        /* consume UTF-16 unit u (surrogate pairs may straddle leaves) */
    }
}
```

This decode is the one the oracle executes (`M3StringLeafRangesOracle.unit`). Width-1 leaves are
directly byte-scannable (`memchr` for `'\n'`); a surrogate pair can straddle two leaves, so a
code-point consumer carries one pending high surrogate across leaf boundaries.

## Export mechanism

Inventory at 7f7accdd54e2: no existing mechanism grants java.base internals to `jdk.m3.*`
modules.

- `src/java.base/share/classes/module-info.java` exports `jdk.internal.access` to
  `java.desktop .. jdk.crypto.cryptoki` (lines 154-166); no `jdk.m3.*` target.
- All 94 qualified-export (module, target) pairs in the 69 `src/*/share/classes/module-info.java`
  name modules that exist under `src/`; none targets a module outside the make build.
- Module packs are compiled outside the make build against an existing M3 image
  (`text-tools/build.py:48`: `-source 21 -target 21 -Xlint:all,-module -Werror
  --module-source-path`) and linked by `jmod-recipes/tools/jmod_pack.py:286` (`jlink`).
- Existing internal grants to pack code are command-line only:
  `--add-exports=jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED`
  (`build-tools/openrewrite_pack.py:41`) and `--add-opens java.base/java.lang=ALL-UNNAMED`
  (`cpu-tools/build.py:466`).

Options, with evidence:

1. **Qualified export in java.base `module-info.java`** (`exports jdk.internal.access to ...,
   jdk.m3.text`). make compiles java.base with `-Xlint:all ... -Werror`
   (`make/common/JavaCompilation.gmk:271,276`) and disables only `this-escape`
   (`make/modules/java.base/Java.gmk`). `jdk.m3.text` is not on the module source path, so the
   build fails: gate G4 reproduces `module-info.java:167: warning: [module] module not found:
   jdk.m3.text` + `error: warnings found and -Werror specified`, and G4c shows module lint is the
   only blocker. `module-info.java.extra` does not help: `GenModuleInfoSource` keeps only targets
   in the build's module list. Making it build needs `DISABLED_WARNINGS_java += module` for all of
   java.base (hides typos in every qualified export) or an empty upgradeable placeholder
   `src/jdk.m3.text` (OpenJDK's `jdk.internal.vm.compiler` precedent, listed in
   `make/conf/module-loader-map.conf` `UPGRADEABLE_PLATFORM_MODULES`), which then collides with
   the pack's own `jdk.m3.text` at jlink time. A qualified export to an absent module is inert
   at run time: gates E10-E13 link a probe module exporting a package to the absent
   `jdk.m3.text`; jlink and `java --validate-modules` both return 0 and `--describe-module` keeps
   `qualified exports probe.base.internal to jdk.m3.text`.
2. **Command-line export plus `jlink --add-options`** (chosen). The module pack compiles
   `jdk.m3.text` with `--add-exports java.base/jdk.internal.access=jdk.m3.text`, and its image
   is linked with `--add-options " --add-exports=java.base/jdk.internal.access=jdk.m3.text"`.
   java.base's descriptor stays unchanged; the grant exists only in images that contain the pack;
   it reuses the repo's command-line grant mechanism. `verify_export.sh` proves it on stock JDK 21
   with the real `jdk.internal.access` (gates E0-E9). E0: the module name itself fails full
   module lint (`module name component m3 should avoid terminal digits`), which is why the pack
   compiles with `-Xlint:all,-module`. Without the export javac reports
   `package jdk.internal.access is not visible` and the VM `does not export jdk.internal.access to
   module jdk.m3.text`; with it, and in a linked image with the option baked in,
   `PROBE_OK countPositives=2`; the unnamed module in that image is still denied.

Both options are name-based: any module named `jdk.m3.text` on that image's module path gets the
package. Applying option 2 is module-pack work (leaf 1 or a follow-up), not part of this leaf.

**Open question for the owner:** keep the export on the module-pack side (option 2), or move it
into java.base's descriptor (option 1) and accept one of its make changes?

## Gates

Commands are in `evidence/COMMANDS.tsv`; verdicts in `evidence/verify-GATES.tsv` and
`evidence/export-GATES.tsv`; key output lines in `evidence/KEY_LINES.txt`. JDK: Oracle
`21.0.9+7-LTS-338` (Windows). Linux: Ubuntu OpenJDK `21.0.12.1` (WSL).

| gate | command (short) | expected | verdict | key output |
|---|---|---|---|---|
| G1_base / G1_cand | `javac --release 21 -Xlint:all -Werror --patch-module java.base=<sparse src> <changed files>` (the exact requested command) | nonzero | PASS (rc 1, as expected) | `module-info.java:172: warning: [module] module not found: jdk.internal.le` ... `jdk.random`; 8 warnings, at HEAD as well: ct.sym of `--release 21` lacks those modules |
| G1_delta | `diff` of baseline and candidate diagnostics | identical | PASS | no new diagnostic |
| G2_base / G2_cand | `--release 21 -Xlint:all,-this-escape,-module -Werror` | 0 | PASS | - |
| G3_base / G3_cand | `-Xlint:all,-this-escape -Werror` (stock 21 system modules; module lint on) | 0 | PASS | - |
| G3d | make's `-Xdoclint:all/protected '-Xdoclint/package:java.*,javax.*'`, baseline vs candidate | no new diagnostic | PASS | 41 = 41 (all pre-existing) |
| G4 | G3 + overlay module-info exporting `jdk.internal.access` to `jdk.m3.text` | nonzero, `module not found: jdk.m3.text` | PASS | `module-info.java:167: warning: [module] module not found: jdk.m3.text` |
| G4c | G4 with `-module` | 0 | PASS | - |
| O1 javac + run | oracle on stock JDK 21, `--patch-module java.base=<new classes only>` | 0 | PASS | `M3_LEAF_RANGES_PASS mode=component stringLayout=stock checks=353136 leaves=27105 atomLeaves=18317 inlineLeaves=8788 startMutantsKilled=17739/17739 byteOrderMutantsKilled=13484/13484` |
| O1 Linux | same patch dir, WSL OpenJDK 21.0.12.1 `-Xcheck:jni` | 0 | PASS | same `M3_LEAF_RANGES_PASS` line |
| M1 | mutant: address uses `start + 1` | AssertionError | PASS | `AssertionError: latin1-local-atom unit mismatch at 0` |
| M2 | mutant: `BIG_ENDIAN` from `!atom.bigEndian` | AssertionError | PASS | `AssertionError: utf16-local-atom-big-endian unit mismatch at 0` (Windows and Linux) |
| M3 | mutant: inline head unit k at bit `8(k+1)` | AssertionError | PASS | `AssertionError: immediate-latin1 inline head padding` |
| E0-E13 | export probe (`verify_export.sh`) | per gate | PASS (14/14) | see Export mechanism |
| jtreg `M3StringLeafRangesDriver` (`m3`, `m3 -XX:-CompactStrings`, `stock`) | M3JDK21 image | - | NOT_RUN | no M3 image locally |
| `make images` | full OpenJDK build | - | NOT_RUN | no local toolchain or disk |

The oracle covers: empty; Latin-1 and UTF-16 local atoms (big-endian); mapped UTF-16
little-endian, mapped big-endian at an odd address, mapped Latin-1 with either flag; LATIN1 and
IDENT immediates (10 units, inline tail) and their slices; tuples, coalesced adjacent ranges of
one owner, non-adjacent ranges of one owner; every slice `[b, e)` of a six-leaf composite with an
independently computed leaf count; 300 pool-built DAGs (`M3StringPool.internChars` +
`M3StringPool.concat`) and slices; capacity 0..n+1 (prefix equal, rest untouched); NOT_M3 and
null handling. Each leaf is decoded independently of the producer and compared with `charAt` at
every index. The JavaLangAccess path (`System.java`) is compile-proven only: on a stock JDK the
String has no `m3` field (`stringLayout=stock`), so the stock `NOT_M3` case runs as
`fill(null, ...)`; the driver's `m3` and `stock` modes exercise the JavaLangAccess path on an M3
image (NOT_RUN).

Reproduce: `bash verify.sh <jdk-21-home> <checkout root> <fresh dir>` and
`bash verify_export.sh <jdk-21-home> <fresh dir>`. Corrections made after failing gates are in
`RECIPE_MEMORY.tsv`.
