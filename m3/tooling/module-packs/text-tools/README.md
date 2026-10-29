<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->

# M3 text module receiver

These are first-party Apache-2.0 source snapshots emitted by Synexia-owned recipes.
The `m3-text-jmods` recipe owns the original tools; `m3-cpu-tools-jmod-20261018`
adds the bounded `M3File` snapshot adapter. The target owns SDK naming and acceptance.
Public Java SE APIs are unchanged. The modules `jdk.m3.text`, `jdk.m3.io` and
`jdk.m3.structured` are opt-in extensions;
this packet does not replace java.base or advance the ordered runtime receiving phase.
`COVERAGE.json` defines every tool contract and pending broader family. The native
FIFO path is Linux-qualified; filesystem behavior stays explicit.

Run from the M3JDK21 repository root with an M3 JDK 21 development image and a C
compiler. `JAVA_HOME` must identify an M3 image exporting
`java.util.concurrent.ProgressMonitor` and `ProgressMonitors`: `M3File` uses those
APIs for bounded file admission and cooperative cancellation. The driver checks
the actual supplied image with `javap` before compiling any module sources.

```sh
python3 m3/tooling/module-packs/text-tools/build.py \
  --jdk-home "$JAVA_HOME" \
  --sources m3/tooling/module-packs/text-tools/src \
  --native-source m3/tooling/module-packs/text-tools/native/m3textio.c \
  --smoke m3/tooling/module-packs/text-tools/fixtures/src/test/java/M3TextSmoke.java \
  --legal-notices m3/tooling/module-packs/text-tools/legal \
  --packager m3/tooling/jmod-recipes/tools/jmod_pack.py \
  --out build/m3-text-tools
```

The thin build driver calls the existing JMOD packager. A fresh output directory is
required. It produces three JMODs, a developer image and compiler/native/runtime logs.
The image includes java, javac, jar, jmod, jlink, jdeps and jpackage; the probe uses
javac and java to execute all 24 existing tools, not every feature of every developer tool.
Use `-source 21 -target 21` with the image's javac for SDK extensions. Standard
`--release 21` does not expose these additional modules or the M3 progress APIs
in `java.base`; a stock Java 21 image cannot compile the `M3File` adapter.

`SOURCE_REF.json` pins the exact canonical Synexia commit and recipe manifests;
`PROVENANCE.json` binds each received blob to its source or executed recipe.
The target's canonical name map records the module mappings. Reusable recipe
changes go back to Synexia; target tooling mirrors are not a second source authority.
Local linked-image qualification is separate from full M3JDK21 product build/jtreg,
full source reactor admission, cross-platform behavior and performance qualification.

## JNI-backed Unix4j line kernels (2026-10-29)

The existing `jdk.m3.text.TextTools` remains the single receiving owner; this
change does **not** introduce an alternative Unix4j runtime or string interner.
On an M3JDK21 image the `java.lang.String` facade represents the canonical
M3 String payload. JNI accepts those references as `jstring` and accesses at
most 256 UTF-16 code units per `GetStringRegion` call. It does not request
`GetStringChars` or `GetStringCritical`, store a native string copy, or
cross the boundary with native-owned text objects. JVM implementation-specific
flattening during a region read has not been independently ruled out.

The two current native algorithms are:

- `sort`: stable bottom-up mergesort of primitive `int` line offsets,
  comparing unsigned UTF-16 code units exactly as `String.compareTo` does.
  Two packed 64-bit lexical prefix signals and the original length are
  manufactured once per line. Native comparison uses those primitive keys
  first, consulting bounded JNI regions only for long colliding prefixes.
  Ascending and descending order preserve equal-line stability. Java owns
  the original `TextLine` instances and the existing line-ending repair.
- `uniq`: adjacent (not global) duplicate detection into a packed `long[]`
  retention mask. Java retains the first original line and its ending.

`libm3textops` is installed in `jdk.m3.text`; `libm3textio` remains in
`jdk.m3.io`. Native failures after successful loading propagate, without
silent fallback. Plain Java 21 executions with no installed library use the
existing identical Java algorithms. Linked-image smoke tests require
`TextTools.nativeTextAvailable()` to return true and run 450 seeded
differential cases under `-Xcheck:jni`, covering lengths around 64/128/256,
BMP/supplementary Unicode, embedded NUL, long prefixes, stable ordering and
reverse ordering. The separate local C/JNI harness passed all 450 cases with
GCC 14.2 and OpenJDK 21.0.12.1; this is not a whole-product or JMOD-image pass.

Memory: primitive merge indices require O(n) native space and the returned
permutation requires O(n) Java integers; the unique mask uses O(n/64)
64-bit words. No performance speedup is claimed: repeated JNI string-region
comparisons can be slower than Java's intrinsic comparator. It must be
benchmarked under both the M3 image and a stock Java 21 baseline before an
optimization claim. The remainder of Unix4j's 14-command API and POSIX option
dialects are **not** represented as native by this first JNI implementation.
Upstream `tools4j/unix4j` (MIT) is a semantic reference only; no upstream
implementation code was copied.
