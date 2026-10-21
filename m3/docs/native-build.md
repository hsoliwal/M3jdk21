# Native build evidence and reproduction

## Common JNI platform contract

All phone, desktop, server and embedded native work follows the
[common JNI and native platform contract](common-jni-platform-contract.md).
It requires shared JNI support and typed native service contracts with platform backends,
target-runtime headers and explicit buffer/reference lifetimes. Common source ownership does
not imply a single binary or remove existing JDK module/library boundaries.

Record source compilation, linking, JNI registration/loading, device/simulator execution and
complete M3JDK21 runtime qualification separately for each OS/CPU/runtime/linkage profile.
The contract also records current target-header and critical-array transfer follow-ups; this
documentation update does not implement those repairs or add mobile runtime support.

## Current checkout verification routes

Run commands from the repository root. The historical result below belongs only to its
named base. The commands in this section are reproduction instructions, **not new passing
results**. Review [OpenJDK prerequisites](../../doc/building.md) and
[test harness setup](../../doc/testing.md) for the selected host before building.

### Choose the artifact

```sh
# Separate foundation; does not rebuild java.base or HotSpot.
export M3_JDK=/absolute/path/to/private/jdk-21
bash m3/build.sh

# Independent Maven recipe/admission reactor; not the OpenJDK image build.
mvn -B -ntp -f m3/pom.xml clean verify

# Current collection receiver; see its README for Java/JUnit/native prerequisites.
mvn -f m3/collections/recipe/pom.xml verify
JUNIT_CONSOLE_JAR=/absolute/path/to/junit-platform-console-standalone-1.12.2.jar \
  python3 m3/collections/verify_donation.py --cost-probe
```

Use the current [collection instructions](../collections/README.md), not the frozen
historical lane installer against an expanded factory. Each command has independent
scope and may fail on missing dependencies or source/receipt drift.

### Verify String source and recipe gates before runtime claims

The [String workflow](../../.github/workflows/mindex-string-backing.yml) is the executable
selection reference. Representative commands it runs are:

```sh
python3 m3/synexia-import/current-full-borrow/check_string_phase.py
python3 m3/runtime-integration/test_string_completeness.py
python3 m3/runtime-integration/test_string_layout.py
python3 m3/runtime-integration/check-m3string-invariants.py
python3 m3/runtime-integration/check-m3string-history-recovery.py
python3 m3/runtime-integration/check-m3string-concat-reference-dag.py
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml \
  -Dtest=M3StringCanonicalDagMasterRepairRecipeTest test
```

This subset is a starting point, not the complete workflow. Receipt validation establishes
custody/admission for its checked scope; it cannot prove JNI execution or a platform port.
A refusal is actionable evidence: inspect the exact preimage, owner and successor receipt,
then fix the canonical recipe or record a blocker. Do not bypass the failing check.

### Build and test a complete runtime

```sh
export M3_BOOT_JDK=/absolute/path/to/private/jdk-21
# Requires host compiler, make, Autoconf and platform development dependencies.
bash m3/build-openjdk.sh
# Select the image actually produced by your configuration; do not assume another host's path.
export M3_TEST_JDK=/absolute/path/to/this-checkout/build/CONFIG/images/jdk
"$M3_TEST_JDK/bin/java" -version
# Focused Linux JNI API harness: also requires gcc and timeout on PATH.
bash m3/runtime-integration/run-api.sh
# Configure jtreg first, then run an explicit focused selection against the build.
make test TEST="test/jdk/java/lang/String/M3StringCanonicalDagTest.java test/jdk/java/lang/String/M3StringStreamsTest.java"
```

The script builds a fastdebug server/headless image. `M3_NATIVE_DEPS`, when supplied,
uses Linux x86-64 dependency-library paths; it is not a portable cross-build abstraction.
Do not patch new `java.base` classes into an unrelated installed VM. The JNI harness
compiles against `M3_BOOT_JDK` headers and executes with `M3_TEST_JDK`; both must be
intentional, compatible selections. Its `c2` case has M3 storage disabled; it is not
M3-enabled C2 qualification.

For the broader scripted jtreg lane, inspect `run-jtreg.sh` and supply `JTREG_JAR`,
`M3_TEST_JDK`, `M3_BOOT_JDK` and the built native test library directory via
`M3_NATIVE_DIR` when needed. The default native directory is Linux x86-64. Its default
and enabled test selections differ. Preserve separate reports and failing tests.

### Configuration and platform evidence matrix

| Surface | Checked-in implementation or recipe | Qualification boundary |
| --- | --- | --- |
| Separate Java 21 foundation | `m3/build.sh`; JIT, interpreter, noncompact and C2-focused cases | Module tests only; no claim for modified-JDK C2 |
| M3-enabled matched VM | `arguments.cpp` forces interpreter mode | Optimizing compilers are not supported with the flag enabled |
| CDS/JVMCI/JFR/string dedup with M3 enabled | VM disables shared-space use and rejects specified incompatible options | Do not remove guards to claim support |
| Linux String/JNI | `run-api.sh` uses GCC, `.so` and Linux headers; hosted String job uses Ubuntu | Exact-head execution still required; instructions are not a pass |
| Linux collections JNI | Current product verifier requires GCC/UBSan and Java/JUnit prerequisites | Independent module proof, not JDK backend promotion |
| macOS/Windows/other architectures | General OpenJDK build workflows exist | M3-specific JNI, layout, lifetime and resource qualification not established by this guide |
| Full JDK, GC/serviceability, desktop and GPU/SDK consumers | Separate source, workflow and design surfaces | Not covered by these focused commands; retain their own pending gates |

Restrictions are grounded in
[`src/hotspot/share/runtime/arguments.cpp`](../../src/hotspot/share/runtime/arguments.cpp).
Record OS/CPU, toolchain, exact commit, flags, logs and measured resource scope for every
new result. No build, SDK/native execution, jtreg suite or benchmark was run for this
documentation revision.

## Historical foundation build receipt

Base checkout: `hsoliwal/M3jdk21@890adb6410dab4606a4f26a942aed02fb2f55387`.
The foundation does not alter OpenJDK sources; this build validates the pinned VM
and provides a real fastdebug image on which to test the separate module.
It is not a VM-integrated joined String build.

This run used private Temurin 21+35 as boot JDK, GCC/G++ 14.2.0, GNU make and
Autoconf 2.72 on Debian 13 x86-64. Missing native tools/headers were downloaded
from Debian's signed repository and extracted under `/workspace/m3-build-deps`;
no system JDK was installed or replaced. CUPS image headers required a second
package after an incremental build failed on `cups/raster.h`. That dependency
was added and `make images JOBS=5` resumed successfully without cleaning objects.
The first failure and subsequent successful build logs are preserved in Library.

Equivalent configure/build entry point:

```sh
# Autoconf/m4 and compiler tools must already be on PATH; no installer runs here.
export M3_BOOT_JDK=/path/to/private/jdk-21+35
# Optional prefix with include/ and lib/x86_64-linux-gnu/ native dependencies:
export M3_NATIVE_DEPS=/path/to/extracted-deps/usr
./m3/build-openjdk.sh
export M3_JDK="$PWD/build/linux-x86_64-server-fastdebug/images/jdk"
./m3/build.sh
python3 m3/compatibility/check-synexia.py
python3 m3/verify-shared-mapping.py
```

The script configures fastdebug server, headless-only, bundled FreeType and
disabled warnings-as-errors. OpenJDK's Linux configure still required X11
headers/libraries in this configuration. The build emitted GCC 14 template
warnings. It created the JDK image and both CDS archive variants and exited 0.
No cleanup/rebuild was used to disguise the recovered dependency failure.

The built image runs the foundation's four configurations, the pinned Synexia
content/range comparison, and the separate String/regex/Unicode/JNI smoke harness.
Nine unmodified upstream String tests run with CompactStrings on/off. These are
direct-main tests, not a jtreg or tier-suite pass. Keep full jtreg, alternative
architectures and transparent joined String integration as separate gates.
