<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Reading M3JDK21: a practical first session

This is a source-reading guide for the public fork, not a release or performance
claim. M3JDK21 combines an OpenJDK-derived runtime with separate M3 libraries,
recipes and evidence. Choose which artifact you are investigating before choosing
a build command: a passing library test does not qualify a modified JVM.

Start with the [project introduction](../../README.md), [contributor checklist](../../CONTRIBUTING.md)
and [current build routes](native-build.md). This guide adds a small worked trace and
a quick reference; it does not replace the existing naming or acceptance authorities.

## A map with three different boundaries

| Question | Source entry point | Boundary to keep in mind |
| --- | --- | --- |
| How are independent immutable text pieces represented? | [Foundation module](../core/) and [module README](../README.md) | Explicit `com.m3.text` APIs on a selected JDK; not a transparent String replacement |
| Where does the actual JDK live? | [Java platform sources](../../src/) and [runtime integration](../runtime-integration/) | Java classes, HotSpot layout and JNI must agree in one matched runtime image |
| How are changes admitted? | [Recipes](../recipes/), [compatibility](../compatibility/), [release evidence](../release/) | A recipe receipt establishes its checked custody scope, not all runtime behavior |

The [porting invariant](m3jdk21-porting-invariant.md) and
[naming map](name-mapping.json) remain authoritative. The receiving order is
String → arrays → collections → AST/compiler → remaining families.
A source-reading exercise or documentation update advances none of those gates.

## Quick-reference cheat sheet

Run commands from the checkout root. These are inspected entry points, **not commands
executed for this guide**. Build scripts write outputs; inspect available disk space,
the scripts and selected toolchain before running them.

| Intent | Command / entry point | Prerequisites and scope |
| --- | --- | --- |
| Identify the exact tree | `git rev-parse HEAD`; `git status --short` | Git checkout; preserve unrelated changes |
| Read the independent module | `git grep -n 'flatten\|subSequence' -- m3/core/src/com/m3/text` | Read-only symbol search |
| Build and test that module | `M3_JDK=/absolute/path/to/jdk-21 bash m3/build.sh` | Bash, JDK 21 and shell utilities including `sha256sum`; writes `m3/build/` |
| Check the foundation recipe manifest | `python3 m3/recipes/apply.py --check` | Python 3; a refusal needs investigation, not bypass |
| Test foundation recipe refusal/repeatability | `python3 m3/recipes/test_recipe.py` | Python 3; recipe scope only |
| Verify the independent Maven control plane | `mvn -B -ntp -f m3/pom.xml clean verify` | Maven, selected Java toolchain and resolvable pinned dependencies |
| Build a complete modified JDK | `M3_BOOT_JDK=/absolute/path/to/jdk-21 bash m3/build-openjdk.sh` | Host compiler, make, Autoconf and platform headers; follow [native-build](native-build.md) |
| Configure and select upstream tests | [OpenJDK testing guide](../../doc/testing.md) | jtreg and the actual built configuration; do not substitute foundation test results |

Read [OpenJDK build prerequisites](../../doc/building.md) for your OS. The native
wrapper's optional dependency paths are Linux-specific; the existence of an upstream
platform build route does not establish M3-specific qualification on that platform.

The foundation [build script](../build.sh) compiles the module and listed tests,
then runs JIT, interpreter, noncompact-String and C2-focused configurations. It later
creates a small fixture lexicon. Failure stops the script: an early passing log does
not imply that later tests or image publication ran. These modes exercise the
separate module; they do not demonstrate M3-enabled optimizing-compiler support.

## Worked trace: what does “share a substring” mean here?

Open [FoundationTest.java](../core/test/FoundationTest.java), method `pieces()`.
Follow this path instead of starting with the whole JVM:

1. A mutable `char[]` is passed to `LocalM3Arena.copyUtf16`.
   The test overwrites the caller's array and checks that the piece still flattens to
   the original text. This is the ownership boundary to inspect.
2. The piece is split with `subSequence(0, split)` and
   `subSequence(split, length)`. The test compares storage identities and checks
   the right-hand code-unit offset. Offsets are UTF-16 code units, not Unicode
   code-point positions.
3. `M3StringPiece.join(left, right)` creates a composition. The test mutates the
   caller's directory afterward and checks that the join still yields the original
   text. Inspect [JoinedM3StringPiece](../core/src/com/m3/text/JoinedM3StringPiece.java)
   to distinguish a copied directory from copied character payload.
4. Read [M3StringPiece.flatten](../core/src/com/m3/text/M3StringPiece.java):
   it allocates a character array, reads each code unit, and constructs a String.
   Sharing payload inside a representation is not the same as allocation-free
   conversion at an API boundary.
5. Read the malformed-surrogate and bounds cases in the same test. They explain why
   “text” here cannot silently mean only valid Unicode scalar values.
6. Follow [LocalM3StringPiece](../core/src/com/m3/text/LocalM3StringPiece.java)
   and [LocalM3Arena](../core/src/com/m3/text/LocalM3Arena.java) to inspect lifetime,
   range checks and storage identity. A small retained view may keep larger backing
   alive; evaluate retained memory separately from bytes copied.

A useful first review note answers: who owns the payload, which operations copy,
what a coordinate measures, and which test would detect a contract violation.
The test source is an executable specification candidate, not evidence that it passed
on your checkout.

For the separate runtime route, continue with the
[String backing contract](../../doc/mindex-string-backing.md) and
[JNI ingress/egress guide](mindex-jni-string-ingress.md). Never patch candidate
`java.base` classes into an unrelated installed VM to simulate a matched build.

## Troubleshooting by evidence scope

- **Wrong Java selected:** inspect `"$M3_JDK/bin/java" -version` for the foundation;
  native build and runtime execution use distinct boot/test JDK selections.
- **Recipe or receipt refusal:** record the checked path and exact base; compare the
  owning recipe and successor evidence. Do not refresh hashes merely to obtain PASS.
- **Module pass, runtime unknown:** retain separate NOT_RUN entries for native/JNI,
  jtreg, GC, serviceability and performance work.
- **Historical guide disagrees with current source:** retain the historical receipt;
  report the current commit and discrepancy rather than silently promoting its claim.

## Before proposing a change

Keep documentation changes out of hash-sealed Java/C++ mirrors unless the owning
recipe is being updated and qualified. For a documentation PR, check relative links,
match commands to current scripts, and distinguish inspected source from executed
results. For behavior changes, use the full [contributor evidence checklist](../../CONTRIBUTING.md).

Preserve upstream notices and per-file licenses. OpenJDK-derived material is not
relicensed by this guide; separable M3 material is Apache-2.0 only where explicitly
marked. See [the provenance notice](../../M3-SYNEXIA-NOTICE.md) and
[upstream license](../../LICENSE).

This guide was prepared by reading public repository files. No compilation, native
execution, jtreg run, SDK integration or benchmark was performed for this revision.
