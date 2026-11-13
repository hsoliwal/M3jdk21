<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Optional build tool packs

The existing module-pack builder accepts explicit, independent tool selections.
It reads only selected pack payloads, checks their byte identities, resolves their
actual JMOD dependency closure with `M3ModuleGraph`, and copies only the selected
JMODs into the link inputs. Required dependencies expand the closure; `requires
static` does not. Service providers needed at runtime must be explicit roots.

## Build a reviewed selection

```
python3 m3/tooling/module-packs/lock_tool_distribution.py \
  --tool maven --distribution /inputs/apache-maven-3.9.11 \
  --version 3.9.11 --source '<reviewed distribution URL or source revision>' \
  --license Apache-2.0 --out /inputs/maven-lock.json

python3 m3/tooling/module-packs/build_pack.py \
  --jdk /inputs/jdk21 --out /fresh/maven-image \
  --tool-pack-lock /inputs/maven-lock.json --tool-pack maven

/fresh/maven-image/image/bin/m3-mvn --version
```

The lock generator records the complete supplied distribution tree, each file's
SHA-256 and executable mode, and a distribution-tree digest. It requires the real
upstream bootstrap/runtime assets and retains original launchers. It performs no
download. Its input inventory does not establish that a directory is a complete,
qualified source build of a repository; source revision and build receipts remain
separate acceptance inputs. Maven 3 distribution execution does not qualify the
Maven 4 fork, and a binary Gradle distribution does not qualify the Gradle fork.

The small `m3-mvn`, `m3-gradle` and `m3-ant` dispatchers set the image's Java home
and run the preserved upstream launcher and its complete retained libraries.
Maven Classworlds/plugin realms and Gradle distribution/worker loaders retain
their normal layout. Each tool may still honor a separately selected project
compiler/toolchain; the dispatcher does not replace that selection.

Compose packs by placing their records in one lock and repeating `--tool-pack`.
A selected record has `name`, `version`, `source`, `license`, explicit `roots`,
`modules` and `assets`. Each module record has `module`, `file` and `sha256` and
names a real JMOD. Include its complete candidate dependency set in the selected
pack. Each asset has `file`, an image-relative `target`, `sha256`, and a boolean
`executable`. Targets are restricted to `bin`, `conf`, `lib`, and `legal`.
Conflicting module/file identities, traversal, image collisions, symlink parents,
missing dependencies and changed input bytes are refused. Identical shared assets
and modules across selected packs are deduplicated.

The `modules` form also supports an OpenRewrite pack built with the existing
descriptor recipes. Retain version-matched Rewrite runtime/language/recipe JMODs,
their exact dependency closure and original licensing; existing split-package
groups remain single module owners. No generic OpenRewrite command launcher or
compiler-tree converter is fabricated by this pack builder.

The existing `jmod-recipes/tools/jmod_pack.py` also accepts repeatable
`--root-module`. It materializes and validates all supplied candidate manifests,
then uses the same graph to copy/link only their selected runtime closure.
Its default still links all supplied modules. Supply filtered input manifests to
avoid the cost or errors of materializing unrelated candidates. `SELECTION.json`
records this distinction explicitly.

## Capture the actual compiler task

These configurations enable the `M3AST` javac plugin already supplied by the
selected M3 compiler. They append the compiler option through each build system's
existing configuration and do not alter annotation-processor paths:

- Maven: place `config/maven-m3ast.xml` inside the project's `<profiles>` and use
  `-Pm3-ast`. It preserves existing compiler arguments using Maven's append merge.
- Gradle: use `--init-script <image>/conf/m3-tools/gradle-m3ast.init.gradle`.
  The configuration avoids adding the option twice.
- Ant: add the `config/ant-m3ast.xml` element inside the existing `<javac>` task.

The option is one compiler-argument string: `-Xplugin:M3AST`. An optional trace is
also one string, for example `-Xplugin:M3AST trace=/absolute/task-1.tsv`. Trace paths
must be unique for every compiler invocation; the plugin creates rather than
overwrites them. A single fixed trace path is unsuitable for concurrent source
sets, Maven main/test compilation, or repeated Gradle worker invocations.

Selecting a pack on a stock JDK does not manufacture an M3 compiler plugin.
Opting into the compiler option on a compiler without the provider must fail
explicitly. Successful image assembly certifies packaging and diagnostics only;
`RECEIPT.json` separately states that tool execution and shared-AST consumption
were not established by the assembler. Tool/build integration receipts provide
that evidence, including forked/worker compilation and processor behavior.

## Qualification

```
M3_TEST_JDK=/inputs/jdk21 python3 -m unittest -v test_tool_packs
```

Run that command from `m3/tooling/module-packs`. The suite compiles real modules,
links and executes selected images, proves runtime dependency/static-requires and
explicit provider selection, preserves JMOD launcher/config/resource/library/legal
assets, checks retained executable bytes, and covers refusal paths. Synthetic lock
fixtures are used only for lock contracts. Windows dispatcher execution and full
custom Maven/Gradle source-build acceptance require their own platform/build gates.
