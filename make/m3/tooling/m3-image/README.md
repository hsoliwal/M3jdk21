<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# M3 Image Pipeline

`m3-image` is the M3JDK21-owned, dependency-free Java 21 control plane for moving repeatable
application work out of deployment startup without changing Java SE, JVM, class-file, JNI, or
application contracts.

It independently implements a compatibility-first workflow inspired by publicly documented AOT
systems and by standard JDK facilities. No Excelsior JET implementation source or binary material is
used. The executable JDK 21 backend composes `java`, JFR, `jdeps`, `jlink`, and AppCDS. An optional
external native-image adapter is isolated behind a backend interface and is never required for the
standard JDK 21 path.

## Pipeline

```text
TRAIN -> PROFILE -> ANALYZE -> PLAN -> BUILD -> PACKAGE -> VERIFY
```

1. **TRAIN** runs the application with class-load capture and JFR profiling.
2. **PROFILE** normalizes loaded classes, startup order, and sampled method hotness.
3. **ANALYZE** scans class files without loading application classes, computes application-class
   reachability through a deterministic CSR/bitset breadth-first kernel, imports `META-INF/services`
   providers as explicit roots, and detects reflection,
   JNI, proxy, service-loading, resources, method handles, and unresolved dynamic-bootstrap
   boundaries. Standard Java 21 bootstrap owners for lambdas, string concatenation, records,
   switches, and constant bootstraps are recognized rather than misclassified as unresolved work.
4. **PLAN** classifies each application class as `AOT_CANDIDATE`, `BYTECODE_FALLBACK`, or
   `UNREACHABLE`, records unresolved dynamic constraints, resolves required JDK modules with
   `jdeps`, and emits a content-addressed deterministic plan.
5. **BUILD** creates a minimized JDK runtime with `jlink`, preserves exact classpath precedence,
   packages directories deterministically, prunes unreachable classes in closed-world images, and
   attempts an application CDS archive from the training class list.
6. **PACKAGE** writes launchers, immutable manifests, provenance, and a build receipt.
7. **VERIFY** re-hashes the complete image and optionally executes its launcher.

## Compatibility modes

| Mode | Contract |
| --- | --- |
| `COMPATIBLE` | Keep every application class available as bytecode. Dynamic boundaries never reject the build. |
| `HYBRID` | Mark statically/profile-reachable classes as AOT candidates and retain all remaining classes as bytecode fallback. This is the default. |
| `CLOSED_WORLD` | Reject unresolved dynamic boundaries; retain only reachable/rooted application classes in the shifted image. Signed archives are refused rather than rewritten. |

The JDK 21 shifted backend always retains executable Java bytecode. Its `AOT_CANDIDATE` label means
"eligible for shifted/profile-guided treatment"; it is not a claim that HotSpot machine code was
persisted. Native machine-code claims belong only to a successfully executed external native-image
backend receipt.

## Commands

```bash
# Complete workflow
./run.sh all \
  --classpath=/path/app.jar:/path/lib.jar \
  --main=com.example.Main \
  --out=build/m3-image \
  --mode=hybrid \
  -- --application-argument

# Individual stages
./run.sh train  --classpath=... --main=... --out=build/train -- --args
./run.sh plan   --classpath=... --main=... --training=build/train --out=build/plan
./run.sh build  --plan=build/plan --out=build/image --backend=jdk21-shifted
./run.sh verify --image=build/image
```

Repeated options use repeated arguments, for example `--vm-arg=-Xmx256m` and
`--dynamic-root=com.example.Plugin`.

## Deterministic plan

The sealed plan consists of:

- `plan.properties`
- `inputs.tsv`
- `classes.tsv`
- `features.tsv`
- `roots.txt`
- `modules.txt`
- `loaded-classes.txt`
- `profile.tsv`
- `semantic-manifest.tsv`
- `PLAN_SHA256`

`bindings.properties` contains local absolute paths required to execute the plan and is deliberately
excluded from `PLAN_SHA256`. Relocating identical inputs therefore does not change semantic identity.

Training outputs are independently sealed by `training-manifest.tsv` and `TRAINING_SHA256`; a plan
refuses a modified or incomplete training directory. Classpath launchers enumerate each packaged
input in original plan order and never rely on wildcard directory enumeration.

## Java/JNI reachability kernel

The portable reference path canonicalizes class names to integer vertex IDs, stores dependencies as
compressed-sparse-row primitive arrays, and computes closure with an integer queue plus one-bit-per-vertex
visited state. An optional JNI implementation executes the identical atom in C. It is selected only by an
explicit absolute `m3.image.reachability.native.library` property; an invalid explicit request fails closed.
The native method releases every `GetIntArrayElements` view before return and retains no Java-heap pointer or
native allocation across calls. `ALGORITHM_CATALOGUE.tsv` records the LeetCode, HackerRank, and
GeeksforGeeks problem categories used for comparison; no donor solution source is copied.

## Build cache

`--cache=<directory>` enables a content-addressed cache keyed by plan identity, backend, selected JDK,
and backend options. A cache entry is admitted only when its complete marker and image manifest
verify. Reuse copies the sealed image; it never trusts timestamps.

## Explicit limits

- The standard backend is a shifted JDK image, not an Excelsior JET clone and not a native executable.
- Static analysis is class-granular. It intentionally over-approximates constant-pool and descriptor references.
- Unknown `invokedynamic` or dynamic-constant bootstrap owners remain fail-closed features; known
  Java 21 standard bootstrap owners are resolved structurally without executing application code.
- Java binary names are validated with Unicode-aware Java identifier rules; the tool does not impose
  an ASCII-only package or class-name policy.
- Training is workload-dependent. Unseen dynamic behavior remains visible in `features.tsv`.
- AppCDS is opportunistic in `COMPATIBLE` and `HYBRID`; failure is recorded and the bytecode image
  remains runnable. `CLOSED_WORLD` treats archive-generation failure as fatal.
- The JNI reachability accelerator is optional; the Java kernel is authoritative and Java/JNI parity is
  verified on deterministic small and 8192-vertex graphs.
- Native-image execution requires an independently installed compatible tool and separate licence,
  runtime, native ABI, and product qualification. The included backend is a process adapter, not a
  bundled native compiler.
- Closed-world pruning retains resources but cannot infer arbitrary framework configuration; such
  reflective targets must be declared with `--dynamic-root` and their feature kind admitted.

## Verification order

`verify.sh` enforces: shell syntax -> `javac -Xlint:all -Werror` plus JNI header generation ->
strict C11 native compile -> Java/JNI parity and malformed-input refusal -> dependency-free tests ->
Javadoc -> end-to-end train/plan/jlink/AppCDS/package/manifest/runtime -> fixed-point plan comparison.
Maven/JUnit is an additional binding, not the only executable proof.

Javadoc verification uses `-Werror -Xdoclint:all,-missing`: malformed documentation and broken references fail, while missing tag prose is not treated as a product defect.
