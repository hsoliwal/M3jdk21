<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native mask-window receiver proof

This directory reproduces the product-specific Linux x64 acceptance run for
`jdk.internal.m3.M3IndexMaskWindow` in `java.base`, its two JNI methods in
`libjava`, and the combined `java.util.CollectionViews` receiver. The driver
reads the nominated product files and builds in a separate work directory.
It does not edit the repository or require Synexia at runtime.

The two default-package qualification helpers live in the admitted Java source
root `src/test/java`: `HeaderFromInstalledClasses.java` and
`ProcessBuilderProbe.java`. Their source bytes are unchanged from the original
proof layout. The portable driver records their source-root-relative input
paths, so the same Java files can pass the unchanged canonical Java engine.

## Reproduce

This recorded qualification requires **Temurin 21.0.12.1+1, Linux x64 HotSpot**,
with the ten supplied-JDK file hashes recorded under
`prior_metadata_driver_seal_control.supplied_jdk.files` in `receipt.json`. The verified
[distribution archive](https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz)
has SHA-256
`ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94`.
The complete JDK must contain `jmods`, JNI/JVMTI headers, `javac`, `jmod`, and
`jlink`. Other required tools are Python 3, Git, GCC, `nm`, and `tar`; the recorded
GCC version is 13.3.0.

The negative controls depend on this specific supplied JDK: it lacks the
receiver ABI, and its newer stock process helper disagrees with the pinned
product process launcher. Another JDK 21 build may already have a matching
helper, in which case `stock-process-helper-red` would correctly fail to produce
the expected failure. That input requires independent qualification of its
controls before this driver can be used to claim the recorded outcome. The
generic JDK-version check in the script does not expand the qualified input set.

The immutable input revisions must be available in the repository. In a shallow
checkout, fetch these exact commits if they are missing:

```sh
git fetch origin 2779aba3f98f31c324be43f731be7914e1a05935
git fetch origin 900a723619fb7021e41b6e2c48a773fd41407224
```

From the generated product checkout:

```sh
python3 m3/collections/algorithm-catalog/wrapper-convergence-20261020/native-proof/qualify.py \
  --repo . \
  --jdk /opt/temurin-21.0.12.1+1 \
  --work /tmp/m3-mask-window-proof \
  --compiler-image
```

The work directory must be new or empty. Preserve failed runs. For review before
canonical recipe application, `--target-root /path/to/candidates` selects the
same nominated Java/C/test paths from the candidate tree. An optional
`--donor-java /path/to/M3IndexMaskWindow.java` supplies the original PR #1557 Java
file; its exact recorded SHA-256 must match. Neither option substitutes a test
implementation for the product owner.

The proof includes real compiler capability when `--compiler-image` is supplied.
Its `image-compiler` directory is an installed JDK image with all available
modules. Set a build process's `JAVA_HOME` to that directory when its compiler
must discover `java.util.CollectionViews` from the actual `java.base` image.
Ordinary classpath entries and VM-only `--patch-module` flags did not provide that
platform-type attribution to the inspected OpenRewrite compiler.

## Exact source closure

The default base is M3JDK21
[`2779aba3f98f31c324be43f731be7914e1a05935`](https://github.com/hsoliwal/M3jdk21/commit/2779aba3f98f31c324be43f731be7914e1a05935).
The original receiver is PR [#1557](https://github.com/hsoliwal/M3jdk21/pull/1557),
head [`900a723619fb7021e41b6e2c48a773fd41407224`](https://github.com/hsoliwal/M3jdk21/commit/900a723619fb7021e41b6e2c48a773fd41407224).

The driver:

1. Compiles the actual candidate mask-window and collection-view Java owners,
   their actual receiving tests, and the unchanged #1557 Java donor.
2. Exports the pinned share/Unix/Linux `libjava` C sources, HotSpot/JNI includes,
   and the repository's `CoreLibraries.gmk` source-selection recipe. It compiles
   all 75 pinned Linux `libjava` translation units plus the receiver; none is
   replaced by a stub or excluded to make the test pass.
3. Uses the installed compiler's own `JNIWriter` to generate headers from its
   actual Java classes. It separately compiles the pinned M3 native declarations
   with their real `String`/M3 dependencies and generates those headers with
   `javac -h`. The receiver's original and candidate generated headers must be
   byte-identical, and the linked receiver must export exactly its original two
   methods.
4. Builds the pinned `jspawnhelper.c` using the same `childproc.o` as
   `libjava`, following the pairing in `make/modules/java.base/Launcher.gmk`.
   The helper and process launcher share their source revision and protocol.
5. Creates installed images from the supplied JDK's module payload, the actual
   compiled product class closure, the source-built `libjava`, and the matching
   source-built `jspawnhelper`. Image manifests seal module, native library,
   helper, VM, and overlay class hashes.

The build follows the nominated native source sets and records its actual flags
and link command. It is a focused manual source build of `libjava` and its
required process helper. It is **not** a configured, complete `make images`
build or a source-built HotSpot. The supplied JDK's HotSpot and unrelated module
binaries remain separately identified inputs.

## Required observations

| Gate | Expected observation |
| --- | --- |
| Original #1557 owner on a JDK without the new ABI | RED: `nativeAvailable()` incorrectly accepts the library name; four zero-work native-required calls incorrectly succeed. |
| Corrected owner without either method | GREEN: availability is false and every native-required case refuses, including zero output, zero source budget, terminal source and contradictory masks. |
| Actual library with only the bounded export hidden | GREEN: the same refusal cases pass even though the full-remainder method exists. This fixture links the real source objects and adds no native export. |
| Product `libjava` with stock `String` | RED: VM initialization reports the missing `java.lang.String.m3` field. |
| Pinned process launcher with stock newer `jspawnhelper` | RED: a real `ProcessBuilder` launch reports a failed spawn helper. |
| Matching String and process-helper closure | GREEN: both a shell and a child JVM execute through explicit `POSIX_SPAWN`. No switch to `FORK` is used. |
| Installed owner identity | `java.base`, bootstrap loader, `jrt:/java.base/jdk/internal/m3/M3IndexMaskWindow.class`; Linux process mappings identify the image's `libjava.so`. |
| Java/native behavior | Independent scalar oracles verify both APIs, every required/excluded byte-mask pair, sparse chunk edges, duplicate/signed row IDs, offsets, budgets, cursor continuation and guarded output. |
| Direct JNI validation | Reflection bypasses the Java validator to verify null arguments, mask width, source/output aliases, aligned lengths and invalid range/limit/output geometry. No rejected call changes the source or output. |
| Combined runtime | Native integration, the original native/Java oracle, and all four collection-view contracts run in fresh interpreter and mixed-mode JVMs with `-Xcheck:jni`. |

The raw JNI masks and aliases were already checked in the #1557 C donor. Their
qualification is preserved behavior, not a newly discovered defect. The code
correction is the Java availability decision. The C change binds compilation to
the generated JNI header and corrects the inherited library-ownership comment.

## Evidence files

`summary.json` records the bounded outcome and pending gates.
`inputs.json` records source pins and SHA-256 values.
`native-source-manifest.json` records every actual C source/object pair.
`generated-headers.json` and `abi.json` seal headers and exported methods.
`images.json` seals each positive and negative image independently.
`commands.json` records commands, exits, log hashes, CPU time, elapsed time,
maximum child RSS, and result lines. Complete stdout/stderr logs remain in
`logs/`; GNU `time -v` output is included when that executable is installed.
Linux `wait4` records the same process-level CPU and peak RSS dimensions.

The committed `receipt.json` distinguishes three proof revisions. The complete
runtime run used the original helper layout. After that run, a metadata-only
revision added hashes of ten supplied-JDK files. Its full rerun stopped at module
extraction when the workspace filled; a separate seal control checked that exact
metadata code and source/image identities, then reran the 14 workloads plus four
refusal/process controls.

The final layout revision moves the two byte-identical helper files into
`src/test/java` and updates only their lookup and input-path mapping. Its separate
source-layout control demonstrates that the old lookup fails against the new
layout, executes the final lookup, compiles both real helpers, compares their
class bytes and generated header bytes with the complete run, and runs fresh
POSIX_SPAWN shell/child-JVM probes on the retained matched images. The earlier
successful runtime and metadata-control runs retain their original driver hashes
and paths. Neither is presented as a full execution of this final layout
revision; no full image reconstruction was repeated for the relocation.

Resource numbers describe this qualification workload. They are not throughput
comparisons or a speedup claim. The kernel declares four fixed 128-element
scratch arrays: 1,280 bytes of primitive array storage before scalar locals,
compiler padding, and JNI/VM call frames. It allocates no native heap, retains no
array pointer, and invokes no Java predicate or comparator. The caller retains
the owner arrays and continuation/generation contract.

Full source-built HotSpot/`make images`, cross-platform ABI runs, sanitizer and
broader GC/concurrency stress, and comparative CPU/native-memory budgets remain
pending. The finite acceptance runs do not promote the global qualification
phase or qualify unrelated String, VM, or Eclipse behavior.

## Source rights

The original #1557 Java/C/test headers are Apache-2.0 and are retained.
The review record uses `EXISTING_DIRECT_COPY_REVIEW` for the existing receiver and
a target-owned correction; it does not relabel that source as GPL. Exported
OpenJDK source retains its original notices and rights. The qualification helper
uses the installed compiler implementation without copying it into this tool.
Receiving/distribution decisions continue to follow the repository's porting
invariant and name map.
