<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# TornadoVM source and installed-image qualification

M3JDK21 owns the delivered compiler, runtime, backend modules and launcher. The
TornadoVM 7.2.0 source input is commit
`87a465f9185eb8e822f9532076fb4a5bc44fdf2a`. Reusable materialization recipes and
their correction memory remain Synexia-owned. This harness is a target acceptance
consumer; it does not introduce a Synexia runtime service or download a TornadoVM
SDK binary.

## Admission contract

The source lock inventories every admitted donor file with its relative path,
SHA-256 and executable mode. The harness checks the complete set before copying
it to a fresh bounded build directory, then rechecks the original tree after
qualification. Outputs never overwrite the vendored source. Runtime dependencies
have exact Maven coordinates and SHA-256 seals. Bootstrap dependency artifacts
are reported as dependency artifacts; they are not described as source-built
Tornado modules. The existing `jmod_pack.py` remains the JMOD assembly owner.
Source and staged-file modes follow Git's owner-execute convention: `100755`
requires the owner execute bit; a group or other execute bit cannot satisfy it.
Output executable seals use the same owner-execute convention. Staged source
copies receive canonical POSIX permissions of `0755` or `0644` from the lock.

Before source compilation, the selected Maven reactor's dependency trees must
agree with every locked runtime/bootstrap/test coordinate. This catches the
upstream JMH dependency on Commons Math **3.2**, rather than substituting a newer
artifact because its API appears compatible. Additional compile/test dependencies
are recorded with artifact hashes and are excluded from the product module
closure when no delivered module requires them.

The executable phases are ordered `lint`, `compile`, `test`, `image-runtime`,
`receipt`. Every phase requires its predecessor's successful receipt and the
same source, harness, JDK, Maven and dependency identity. Failed commands retain
their logs and failed disposition. A changed input starts a fresh qualification
directory. No command invokes `clean`, an upstream release workflow or `deploy`.
At each phase boundary the complete output inventory must equal the previous
seal, including paths, file bytes, executable modes and permitted image-local
symlink targets. Added files are refused as well as modified or removed files;
later JAR and module assembly cannot consume an unsealed extra class.

`lint` parses the selected source/build files, checks source seals, validates
backend identity and inspects the actual source dependencies. The donor's
Checkstyle configuration is run before product compilation. Dependency bootstrap
belongs to the compile phase. `compile` relocates the pinned Graal 23.1.0 compiler
using the donor's existing relocation source, compiles the selected Tornado
reactor and executable image probes. `test` executes the donor's no-device JVMCI
tests with tests explicitly enabled. No skipped GPU test is counted as a pass.

The pinned upstream OpenCL selection has 123 existing Checkstyle diagnostics
across 720 reported source files under plugin 3.3.0 / checker 9.3. The initial
failing audit is retained in development evidence. `checkstyle-baseline.json`
records the full normalized diagnostic multiset and reported file set, bound to
the upstream source inventory, checker version and unchanged configuration hash.
Lint requires exact equality: any added, removed or altered diagnostic, stale
source/configuration seal or changed checker version refuses admission. Its
disposition is `BASELINE_MATCH`, with the upstream command's exit 1 retained; it
is never represented as clean style. CUDA and Metal have no reviewed baseline
here and fail with `UNQUALIFIED_CHECKSTYLE_BASELINE` before source compilation.

The optional `--junit-lock` binds the existing source-built JUnit support
artifacts, all recorded support source files and build receipts. The unchanged
five donor tests and existing adapter test are compiled in `compile`, then run
in `test` and again in the installed image. The CPU lane expects 19 discovered
tests, 16 successes, two aborted hardware assumptions, one hardware condition
skip and no failures. Those three hardware outcomes remain explicit and are
separate from the required-device kernel and accelerated receiver parity probes.
JUnit artifacts are test support and never enter the product JMOD/image closure.
After a real kernel passes, the installed-image device lane reruns the unchanged
originals with the legacy `m3.gpu=true` test switch and the observed task-device
selection. It requires all 19 tests to succeed with no abort or skip. That legacy
switch enables acceleration tests; the observed device type determines whether
the evidence is CPU OpenCL or physical GPU execution.

`image-runtime` creates JMODs with the existing target packager, links a complete
JDK image from the supplied JDK's JMODs, validates the installed modules and runs
the image-local launcher. The launcher uses the image's own Java and modules;
`JAVA_HOME`, `TORNADOVM_HOME` and a separate Tornado installation are unnecessary.
Original export lists, native-access requirements and licenses remain attached
to the installed image. No JDK public API or HotSpot/JNI ABI is renamed.
The launcher enables JVMCI for Tornado's application compiler and explicitly
disables its use as the host JIT; a bootstrap GraalVM's default host-JIT choice
must not introduce a different compiler/support closure.

Custom JMODs precede bootstrap-JDK JMODs during linking. After linking, the
installed classes of every added module are extracted and compared byte for
byte with its exact input JMOD class set. A bootstrap JDK's bundled Graal version
cannot silently replace a locked module. The installed native VM is also
compared with the supplied JDK VM bytes. Module descriptor metadata may be
rewritten by JDK image tooling; executable class contents remain exact.

An OpenCL kernel probe independently computes an integer result, explicitly
selects a reported backend/device, executes a Tornado task graph, compares every
output element and emits device, transfer and result evidence. Device absence is
an explicit unqualified gate. It cannot become a successful CPU fallback or a
claim of GPU execution. An OpenCL CPU device is accepted as an OpenCL device and
reported as CPU; it is never described as a GPU. Performance is not inferred from
correctness or device enumeration.

`--require-gpu` additionally filters actual reported devices to type `GPU` and
implies `--require-device`. The accelerated receiver is bound to the same observed
backend/device through Tornado's existing task-device property. Runner names or
labels cannot satisfy this gate. The deterministic empty-device-catalog refusal
fixture exercises the selector's exit-3 path; its receipt does not claim that host
hardware was absent. Real host discovery and execution are separate commands.
Tornado inspects serialized method-reference lambdas. The receiver descriptor
therefore opens only its legacy provider package to `tornado.runtime`; the probe
package gets a test-only qualified open. No unrestricted package opening is added
to the installed application launcher.

## Backend and VM boundaries

The selected 7.2.0 source provides `opencl`, `cuda` and `metal` Maven profiles.
They are separate choices. CUDA generates PTX through NVRTC; this release has no
standalone `ptx` profile. SPIR-V ingestion is an OpenCL capability and has no
standalone `spirv` profile in this tree. Unsupported names fail before building.
OpenCL uses the Java 21 preview Foreign Function and Memory API and needs the
OpenCL loader plus a device implementation at execution time. Optional CUDA
library JNI modules have their own native-toolchain gates.

The source-built M3JDK is the final runtime authority. The harness accepts a
bootstrap JDK only with explicit provisional qualification and records that
distinction in every final receipt. M3 admission additionally binds the supplied
source-built JDK receipt and verifies the product's VM identity. The existing
`UseM3StringStorage` / JVMCI incompatibility remains enforced: no launcher adds
an option to bypass that guard, and a request to enable both is refused by the
product. A bootstrap JDK pass never advances M3 String admission or replaces
full JDK, jtreg, native, resource or platform qualification.

## Evidence and replay

Each command has an argv vector, working directory, exit code, elapsed time and
SHA-256-sealed output log. Receipts bind source lock, dependency lock, toolchain
release, packager, selected backend, built artifacts, installed image, actual
device outcome and unresolved gates. The final phase replays source and artifact
seals and checks the ordered phase chain; it cannot turn a failed or missing
runtime into a pass. A fresh output directory preserves earlier failure history.

All upstream Apache-2.0, GPLv2 with Classpath Exception, MIT and transitive
dependency notices remain scoped to their material. Copying source into M3JDK21
does not relicense those files.

## Invocation and toolchain inputs

Use the same options for each ordered phase:

```sh
python3 m3/runtime-integration/tornado/build_runtime.py \
  --repo-root /absolute/M3JDK21 \
  --jdk-home /absolute/source-built-m3jdk \
  --m3-jdk-receipt /absolute/m3-jdk-source-build.json \
  --maven /absolute/apache-maven-3.9.9/bin/mvn \
  --maven-repo /absolute/m2 \
  --junit-lock /absolute/source-junit-support-lock.json \
  --backend opencl --require-device \
  --out /absolute/fresh-qualification --phase lint
```

Then invoke `compile`, `test`, `image-runtime`, and `receipt` with the same
inputs. `--phase all` performs exactly those five phases in order. A bootstrap
development run replaces `--m3-jdk-receipt` with `--allow-bootstrap-jdk`; it
cannot be promoted to a source-built M3JDK result in the same output directory.
`--source-root` and `--receiver-source` explicitly select isolated development
candidates before canonical materialization. Their paths and contents remain
bound in the evidence identity.

The default Maven operation is offline. `--resolve-build-tools` allows explicit
Maven network resolution, including missing hash-locked dependency jars before
lint, and uses Maven's update flag to retry prior failed downloads. Downloads
are hash-checked before use. Resolving a dependency artifact does not compile
Tornado source. `--maven-settings` accepts an external settings path when a
network proxy is required; only the path is recorded. Credentials and settings
contents or hashes are not copied into receipts. `--maven-truststore` selects an
existing CA store without disabling TLS verification. Maven is bounded to one
reactor worker, four visible processors and a 2 GiB heap. Commands have a bounded
timeout and an expired process group is terminated.

## Source-built JDK receipt schema

The final image authority uses schema `m3-jdk-source-build/1`, `status: PASS`, an
absolute `jdk_home`, and `source` with `repository`, 40-hex `commit`, and a
64-hex `tree_sha256`. `build` includes its argv array, absolute working directory,
zero `exit_code`, and `log: {path, sha256}`. `artifacts` contains `{path, sha256}`
entries relative to that JDK for `release`, `bin/java`, `lib/server/libjvm.so`,
and every supplied `jmods/*.jmod`. `vm.UseM3StringStorage` must be `false`.
Additional provenance fields may be retained by the source build owner.

The harness verifies those bytes and the complete JMOD set, then independently
executes the installed VM to observe the actual M3 flag and the existing
String/JVMCI refusal. A caller-supplied `source_built: true` claim cannot substitute
for these checks. This implementation qualifies Linux image layout; other
platform layouts need their own source receipt and installed-image qualification.
