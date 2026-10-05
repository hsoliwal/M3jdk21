# Developing the M3JDK21 fork

This guide is an entry point for the public `hsoliwal/M3jdk21` checkout. It keeps the OpenJDK product build, the M3 foundation, and the Maven proof tools separate. Read the checked-out revision's scripts before running a lane; an older receipt or a green recipe test does not certify a different JDK image.

## Choose the right build lane

| Goal | Authoritative entry point | What it establishes |
| --- | --- | --- |
| Build a JDK image | [OpenJDK build guide](building.md), root `configure` and `Makefile` | Native/JVM/JDK compilation for one configuration |
| Exercise the standalone immutable-text foundation | [m3/build.sh](../m3/build.sh) | Foundation compilation, four execution modes and fixture-image generation |
| Verify recipes, inventory and admission tools | [m3/pom.xml](../m3/pom.xml) | The selected Maven reactor's own checks |
| Run JDK regression tests | [testing.md](testing.md) | Only the suites and platform actually executed |

The Maven reactor is not an alternative implementation of the OpenJDK build. Its enforcer requires Java 21 and Maven 3.9 or newer. The standalone foundation script also compiles with `--release 21`. These facts do not imply that a later-language feature or a proposed runtime change is accepted.

## First checkout and baseline

```sh
git clone https://github.com/hsoliwal/M3jdk21.git
cd M3jdk21
git rev-parse HEAD
git status --short
java -version
mvn -version
```

Use a local filesystem with enough space for a full native build. Install the compiler, platform libraries, GNU make and boot JDK described in [building.md](building.md). For ordinary OpenJDK development:

```sh
bash configure
make images
./build/*/images/jdk/bin/java -version
make run-test-tier1
```

If several configurations exist, select the intended configuration instead of treating the wildcard output as one image. Configure jtreg and other test prerequisites using [testing.md](testing.md). Preserve the configure log, compiler version, image identity and exact test command.

The fork also provides [m3/build-openjdk.sh](../m3/build-openjdk.sh), which requires `M3_BOOT_JDK` pointing to a private JDK 21 and selects fastdebug, server and headless options. It writes configure/build logs under `m3/build/logs`. Inspect its native-dependency options before using it on a different platform; it is a specific build recipe rather than a universal installer.

## Independent M3 checks

With `M3_JDK` set to an isolated JDK 21 directory:

```sh
./m3/build.sh
mvn -B -ntp -f m3/pom.xml clean verify
```

The foundation script runs normal JIT, interpreter, noncompact-String and C2-focused modes, then generates a small English fixture image. Its artifact hashes and test logs are under `m3/build/logs`. The fixture is not a production dictionary.

The default Maven modules are `indexdb`, `tooling/migration-recipes`, `tooling/a3`, `tooling/backport-dag`, `backports` and `tooling/module-packs`; the `m3-extended` profile additionally selects `collections`. A module failure or coverage failure remains a failure even if its unit tests pass. Do not lower thresholds to turn documentation verification into a green build.

## Source map and ownership boundaries

- `src/`: OpenJDK module and HotSpot product owners. Pair changes with the corresponding `test/` suites and native-platform evidence.
- `make/`, `configure`, root `Makefile`: native build machinery.
- `m3/core/` and `m3/lexicon/`: standalone foundation and fixture inputs.
- `m3/tooling/`, `m3/backports/`, `m3/recipes/`: recipe, inventory and admission machinery.
- `m3/runtime-integration/`, `m3/runtime-stage1/`, `m3/runtime-segments/`: bounded runtime work with its own source and evidence contracts.
- [m3/README.md](../m3/README.md): implemented foundation contracts and links to wider architecture/work packets.
- [.m3/README.md](../.m3/README.md): separate recipe-first sidecar instructions.

Treat these as source responsibilities, not a new human-maintainer registry. Read the nearest README, manifest and recipe before changing a subtree. Source-changing work must follow its existing recipe-first rules; this guide does not authorize direct product-source edits.

## Implemented, candidate and planned work

The foundation README distinguishes implemented immutable-piece and fixture-image behavior from future storage/publication and wider JDK work. Architecture documents describe the programme, not a completed implementation. Read-only inventory, source hashes, fixed-point checks, Java compilation, native execution and compatibility acceptance establish different things. Keep branch-scoped candidates and evidence attached to their exact commits.

## Review and handover

For fork work, target a draft PR at `hsoliwal/M3jdk21:master`. Do not accidentally target upstream or merge unrelated candidate branches. Upstream OpenJDK contributions follow [CONTRIBUTING.md](../CONTRIBUTING.md). Preserve root OpenJDK notices and the separate `m3/LICENSE`/`m3/NOTICE` boundaries.

A useful handover includes the base/head SHA, changed source owners, toolchain, exact commands, exit codes, test/coverage reports, native platform, remaining gates and artifact locations. Label each check passed, failed, skipped or not run. Documentation-only changes should verify relative links and the diff; they do not by themselves demonstrate a native build or full jtreg pass.
