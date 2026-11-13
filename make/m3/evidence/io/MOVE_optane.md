# m3/optane into the JDK module jdk.m3.io (2026-11-09)

M3JDK is one JDK, not an m3/ side tree next to src/ (the user's instruction). The approved layout puts the I/O lane
into one JDK module (jdk.m3.io = fs + file + optane). Optane goes first: it depends only on JUnit, has no dependents,
and its M3 prefixes are gone (#2845).

`make/m3/tooling/jdk-naming/move_module_area.py . optane jdk.m3.io --keep-pom` (MOVE_TO_JDK_MODULE v3; rows in
`MOVES_optane.tsv`) moved the area, with git mv for history:
- src/main/java/jdk/{m3,internal/m3}/optane goes to src/jdk.m3.io/share/classes/...;
- the JUnit tests go to test/jdk/jdk/{m3,internal/m3}/optane;
- the JNI sources go to src/jdk.m3.io/share/native/libm3optane, and the C test oracles to test/jdk/jdk/m3/optane/native;
- CMakeLists.txt goes to make/m3/optane, re-pointed at the module sources.
It also wrote:
- src/jdk.m3.io/share/classes/module-info.java (`module jdk.m3.io { exports jdk.m3.optane; }`; jdk.internal.m3.optane
  stays internal);
- make/modules/jdk.m3.io/Java.gmk;
- the PLATFORM_MODULES line of make/conf/module-loader-map.conf;
- a bridge on m3/optane/pom.xml (build-helper over the moved sources and tests), so the artifact and its tests still
  build with Maven until the I/O lane has moved.
The generated module-info Javadoc named the collections lane; it now names the I/O lane (declared edit).

The live references follow by `move_refs.py` (MOVE_REFS v1.1, rules `MOVE_REFS_optane.tsv`), with residue 0 and a
fixed point:
- make/m3/optane/verify.sh (sources and natives);
- the CMake usage line;
- the README commands;
- PORT-MAPPING.pending.json.
The bridge descriptor m3/optane/src/main/java/module-info.java (module org.m3jdk.optane) stays as a KEEP. The
module's recipe/ and docs/ records stay under m3/optane for the evidence-store step.

## Gate (WSL OpenJDK 21), Maven layout (master) vs JDK-module layout

| | Before (m3/optane/src) | After (src/jdk.m3.io + test/jdk) |
|---|---|---|
| javac as the named module jdk.m3.io (-Xlint:all,-module -Werror, as Java.gmk) | - | rc=0, 50 classes |
| javac main (-h) / test | rc=0 49 classes 3 headers / rc=0 43 | same |
| JUnit (from m3/optane, as Maven) | 97: 57 pass, 35 fail, 5 skipped | same, per-test status identical (117 rows) |
| C sources -Werror against the headers (3 libraries, 2 test oracles) | 5/5, every Java_ symbol declared | same |
| make/m3/optane/verify.sh (contract, natives, jar, jmod, jlink runtime) | OPTANE_CONTRACT_PASS 1151 / 1149 no-native, OPTANE_JMOD_BUILD_PASS | same |

The 35 JUnit failures are the CPU host's missing PMEM library, on both sides.
