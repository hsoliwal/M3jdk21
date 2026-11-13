# m3/compression moved into java.base (jdk.internal.m3.compression)

This is the first step of the layout you approved on 2026-11-08. M3 product code leaves the `m3/` side tree for real
JDK modules, and the runtime internals go into `java.base` as `jdk.internal.m3.<area>`. `jdk.m3.compression` had no
consumer outside its own module.

`m3/tooling/jdk-naming/move_area.py` (MOVE_TO_JDK_MODULE v1) does the whole move; `MOVES.tsv` lists every old path,
its new path, and the rules applied:

| From `m3/compression/` | To |
|---|---|
| `src/main/java/jdk/m3/compression/*` (10 classes) | `src/java.base/share/classes/jdk/internal/m3/compression/` (internal, not exported) |
| `src/test/java/jdk/m3/compression/*` (33) | `test/jdk/jdk/internal/m3/compression/` (same package: the tests use package-private types) |
| `src/main/native/m3_lz_jni.c` | `src/java.base/share/native/libm3compression/` (JNI symbols `Java_jdk_internal_m3_compression_*`; still the opt-in library loaded from `-Dm3.lz.library`) |
| `src/main/native/tests/m3_lz_test.c` | `test/jdk/jdk/internal/m3/compression/native/` |
| `src/main/native/CMakeLists.txt` | `make/m3/compression/CMakeLists.txt` (sources re-pointed) |
| `recipe/compression-20261106/*` | `m3/evidence/compression/recipe/` (one store; no dated directory) |
| `NOTICE` | `src/java.base/share/legal/m3compression.md` (placement sentence updated) |
| `pom.xml`, `module-info.java` (`org.m3jdk.compression`) | removed; `m3/pom.xml` drops the module |

The only test change is a declared rule: `MethodHandles.publicLookup()` becomes `MethodHandles.lookup()` in 3 test
helpers, because an internal `java.base` package is not unconditionally exported.

The library's own `Lib.gmk` entry in the JDK build is deferred until a full JDK build can verify it. Until then the
C source stays opt-in exactly as before.

## Gates

| Gate | Master layout (m3/compression) | Moved layout (java.base) |
|---|---|---|
| javac main | `-Xlint:all,-serial -Werror`: rc=0 | as a `java.base` patch: rc=0 |
| JUnit, 14 classes, no library / with library (Windows JDK 21 + MinGW) | 14/14 / 14/14 | 14/14 / 14/14 |
| `*SelfTest` mains, no library / with library | 9 of 11 / 9 of 11 (the other two need an argument) | 9 of 11 / 9 of 11, the same two |
| `M3LzNativeArenaSelfTest` with the library path | parity checks=386144 | parity checks=386144 |
| `make/m3/compression/test-java-base.sh` (WSL, OpenJDK 21, GCC) | - | rc=0: JUnit 14/14 in both modes; 10 mains PASS; `M3LzSelfTest` JNI=executed with the library |

`layout-comparison.sh` is the Windows comparison runner. CI: `.github/workflows/m3-java-base-internals.yml` runs
`test-java-base.sh`.
