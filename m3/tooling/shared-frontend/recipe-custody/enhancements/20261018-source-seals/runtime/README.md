<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Descriptor-aware M3 compiler qualification image

This document precedes the qualification driver. The compiler candidates remain
frozen. The driver consumes qualified target sources; it does not edit the target
checkout, the stock JDK, or another build's exploded runtime.

## Classification and scope

The output is a **hybrid qualification image**: the exact stock Java 21 JMOD estate,
with four source-compiled M3 compiler owners and the actual target module descriptor
substituted into `jdk.compiler`. It is not a full M3JDK21 source build, a native/HotSpot
qualification, a benchmark, or proof of complete Maven/Gradle fork delivery.

The fixed compiler source list is:

- `src/jdk.compiler/share/classes/com/sun/source/util/M3AST.java`
- `src/jdk.compiler/share/classes/com/sun/tools/javac/m3/M3ASTPC.java`
- `src/jdk.compiler/share/classes/com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java`
- `src/jdk.compiler/share/classes/com/sun/tools/javac/main/JavaCompiler.java`
- `src/jdk.compiler/share/classes/module-info.java`

The existing shared frontend test and new source-seal test are compiled from the
qualified source root. An optional baseline root supplies only the same five compiler
sources; both images execute the same qualified test classes for a differential
source-capture failure witness.

## Ordered stages

`build` inventories and hashes inputs before creating a fresh output directory. It
copies the five source files into a private source path, performs source-hygiene lint,
and invokes the stock compiler with `-Xlint:all -Werror`, the existing compiler owner's
`-XDstringConcat=inline` setting, and `-implicit:none`. It compiles no implicit repository
source. Existing source-local warning pragmas remain intact; no warning is disabled.

The driver extracts stock `jdk.compiler` and `java.base` with the supported `jmod`
tool. Within the extracted compiler staging tree, it inventories and removes only the
four owned outer/nested class families, then overlays their newly compiled output and
the compiled target descriptor. All other class/resource entries, native commands,
libraries, configuration, headers, legal notices and man pages must preserve their
entry bytes. Reconstructed ZIP metadata is not represented as original archive bytes.

All other stock JMODs are copied exactly. `java.base.jmod` is deliberately absent from
the fresh module path while its replacement is created with `jmod create
--hash-modules`. The original `ModuleHashes` name set and algorithm are retained, with
the changed compiler hash recomputed. The completed module path is then passed to
the supported `jmod hash` operation and to `jlink`. No hash attribute is stripped and
no hash verification bypass is used. The native/base payload is not recompiled.

The reason for this ordering is present in the stock JDK source: `JmodTask.Hasher`
resolves the supplied module graph, so attempting to update a copied old base beside
an already changed compiler can reject the stale compiler hash before rehashing.
During creation, `JmodTask.computeHashes` supplies a synthetic descriptor for the base
being created; it is selected only when the module path does not already contain base.

`build` subsequently compiles the two tests with the stock compiler against the linked
candidate system image. It does not execute those tests or run the image's tools.

`validate` first verifies the complete build-output and input manifests. It executes
the linked image's module-description command, the existing shared frontend suite,
and the source-seal suite. The existing suite exercises the actual plugin provider
and consumer service loading. All runtime temporary files stay under the validation
directory. If a baseline image was requested, the old sharing suite must pass there;
the new seal suite must fail at its specific missing capture-abort assertion, and its
partial primitive image must exist. That is an expected negative witness, not a promoted
baseline success. Validation has its own receipt and cannot run twice in one output.

## Inputs, custody and failure behavior

The driver records SHA-256/size/mode or symlink-target records for the stock JDK tree,
the exact source/test files, the driver/docs, the Python interpreter, all outputs and
every command log. Build receipts include the actual compiler/base descriptor details,
entry-level preservation checks, recorded module hashes and their computed witnesses.
The stock JDK source archive is part of the stock tree inventory. Python's installed
standard library is the host runtime; its version and executable are recorded, not a
claim of a vendored Python environment.

Environment injection through `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS`, `_JAVA_OPTIONS`
and `CLASSPATH` is removed for child tool commands, with the removed variable names
recorded. Values are not printed. A stable JMOD timestamp controls archive timestamps;
event times remain execution evidence.

Commands run serially and stop on the first unexpected exit. A failed output is retained
with its logs and failure receipt. There is no automatic retry, source repair, output
deletion, warning suppression, or hidden resume. Rerun after a correction into a new
explicit output directory. Baseline and candidate native payloads remain stock.

## Invocation

```sh
python3 qualify_runtime.py build \
  --stock-jdk /tmp/m3-regex-toolchain-20261018/jdk-21.0.12.1+1 \
  --source-root /absolute/qualified/M3jdk21 \
  --output /absolute/fresh/qualification-output \
  --baseline-source-root /absolute/optional/baseline/M3jdk21

python3 qualify_runtime.py validate --output /absolute/qualification-output
```

The inspected stock image is Temurin `21.0.12.1+1-LTS`, target `linux-amd64`.
Its base descriptor records 65 SHA-256 module hashes. The stock compiler JMOD contains
classes, legal, man, bin and lib sections; base also contains conf and include sections.
The driver derives these facts from the selected input rather than assuming this one
inventory applies to every supplied Java 21 distribution.

Primary references: the selected JDK's `lib/src.zip`, especially
`jdk.jlink/jdk/tools/jmod/JmodTask.java`,
`java.base/jdk/internal/module/ModuleHashes.java`, and
`java.base/java/lang/module/Resolver.java`; and the
[Java 21 jmod specification](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jmod.html).
