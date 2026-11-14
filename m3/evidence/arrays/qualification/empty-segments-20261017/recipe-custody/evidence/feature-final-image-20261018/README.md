<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Clean feature image qualification

The Runtime facade and ProgressJob were built into a clean Linux x86_64 fastdebug
JDK image. The images target passed with warnings as errors and the existing
doclint gates, including HotSpot, JNI, all JDK modules, jmods, jlink and both CDS
archive variants. Configure/build commands, timestamps and complete logs remain
in `build/`; actual image hashes are in `QUALIFICATION.json` and the execution
receipt. No image binaries or replacement production modules are stored here.

## Exact source basis

The actual build started from commit 86528a90079b89a50b51ef13da08e7e70d468cb4 plus
the qualified Runtime and ProgressJob recipes. The receiving branch starts from
07ba6e383343d32c83cfdaf641387bbe72bf325d, receives the same feature recipes, and
restores 31 exact current-baseline targets through `m3jdk21-runtime-restore-07ba`.
The restoration preserves the newer catalogue row and does not reverse newer
collection changes. The two source trees match by path, kind, executable mode,
SHA-256 and direct file-byte comparison across all 25,341 files under `src`,
`make`, `configure` and `Makefile` (398,067,787 bytes). The before-restoration
14-file difference remains a distinct historical observation. This is a scoped
OpenJDK build-source identity proof, not an assertion that both commits or their
separately built m3 modules are identical.

## Executed results

Nine jtreg files passed on the new image: six Object tests (including the new
HOLD witness), the Runtime legacy client, the Runtime facade and ProgressJob.
The actual run uses `-Xcheck:jni` and an explicitly pinned stock Java 21 compiler
for legacy clients. The independent feature verifier uses no `--patch-module`:
84 Runtime checks, 81 ProgressJob checks and 37 real Eclipse status checks each
passed under default, interpreter, C1 and C2 modes. Frozen stock-compiled Callable
client classes were hash-verified and executed unchanged. Eclipse jar/source
and license provenance belongs to the accompanying ProgressJob donor record;
these receipts do not claim an Eclipse platform or Synexia runtime launch.

The original primitive compiler, identity test and fixed-arena probe were reused.
JNI passed 131,403 checks. Two independent sequential JVMs mapped the same image
at 0x600000000000; each probe also asserted same-process collision refusal. This
does not claim simultaneous-process stress or support for other native platforms.

The Object compatibility witness intentionally reports HOLD: a new Object
method can preempt an old interface default and reject previously valid source.
A passing witness confirms that these counterexamples remain visible. Runtime
and ProgressJob's finite legacy-client results do not remove that global hold.
The STRING phase remains unchanged. These local receipts do not assert hosted
CI success, JCK certification, automatic console/UI wiring or whole-SDK completion.

## Recipe replay

Use `m3/runtime-integration/runtime-progress-facade-20261018/recipe-custody/README.md`
for the qualified Runtime parser host and source-built OpenRewrite dependency.
That successor entrypoint supersedes the older predecessor-host reference in
the feature introduction. ProgressJob and the current 31-target restoration use
the original public host at
`m3/arrays/qualification/empty-segments-20261017/recipe-custody` with its checked-in
`sdk-owner`. Exact preimages, SDK bindings, compiler checks and fixed-point
receipts remain in their respective datasets. This evidence is received by a
separate Maven custody recipe; the evidence receiving recipe is not recursively
embedded in its own output. No target file is hand-patched after receiving.

The authoring helpers validate and inventory existing receipts outside receiving
trees. Reproduction requires the pinned source/test/donor inputs; replace the
transient workspace and toolchain paths in commands with local equivalents.
Source headers and donor notices retain their original rights. Generated
execution evidence does not relicense the source or third-party artifacts.
