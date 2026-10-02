# JDK 22–27 → M3Jdk21 backport programme

Status: active downstream backport inventory. Target baseline is OpenJDK 21 semantics.

## Locked contract

The Java 21 language grammar, source acceptance, javac language semantics, class-file compatibility and existing public behavior are locked unless a future change is explicitly unlocked and independently accepted.

A later-JDK change is eligible for investigation when it can be adapted without requiring a new Java source-language construct. Tooling improvements are explicitly in scope, including `jcmd`, `jlink`, `jpackage`, `jfr`, `jconsole`, `jdeps`, `javadoc`, `keytool`, launchers, build/test tooling and serviceability.

HotSpot, GC, JFR, native, security, core-library and performance changes are also in scope when they preserve the Java 21 external contract. HotSpot compiler/JIT changes are a separate high-risk lane: they are not Java-language changes, but require compiler/VM-specific acceptance.

The following are not silently imported:

- Java grammar, parser, attribution or type-system features that make JDK 21 accept new source syntax.
- API or platform removals that narrow the JDK 21 contract.
- Preview/incubator contracts as if they were final. Prefer the latest final revision; otherwise keep the item gated.
- Compatibility warnings/restrictions whose purpose is to change existing behavior, unless separately approved.
- A commit merely because it exists in a newer JDK. Every accepted backport needs provenance, dependency closure, tests and an exact target diff.

## Upstream denominator

The released OpenJDK GA tag intervals used for mechanical inventory are:

| Release | GA tag |
| --- | --- |
| 21 | `jdk-21+35` |
| 22 | `jdk-22+36` |
| 23 | `jdk-23+37` |
| 24 | `jdk-24+36` |
| 25 | `jdk-25+36` |
| 26 | `jdk-26+35` |
| 27 | `jdk-27+35` |

`JEP_CATALOGUE.tsv` records all 82 JEPs delivered by JDK 22 through JDK 27 and gives the initial downstream disposition. A disposition is an admission decision, not implementation evidence.

`UPSTREAM_CHANGE_SEEDS.tsv` records individually inspected non-JEP enhancements. It is intentionally a seed, not the denominator. `inventory.py` is the denominator builder: against a complete local `openjdk/jdk` checkout it walks every commit in each GA interval, records JBS IDs and touched paths, and classifies likely language/compiler/tool/runtime/library/security/build lanes. This prevents release-note curation from being mistaken for complete RFE coverage.

## Backport packet

Each actual backport must record:

1. target baseline commit and preimage path hashes;
2. upstream repository, exact commit and JBS/JEP identity;
3. touched-path inventory and dependency closure;
4. admission reason and explicit exclusions;
5. exact patch or source-bound recipe;
6. lint/build/test/runtime receipts in that order where applicable;
7. postimage hashes and target diff;
8. idempotent replay/rollback or a documented reason why the upstream patch itself is the canonical replay unit.

Existing `m3/docs/name-mapping.json` remains the M3 migration mapping authority. This directory is an upstream-JDK backport catalogue and must not replace or fork that mapping authority.

## First admitted tool change

JDK-8357439, **Add bash autocompletion for jcmd**, is the first concrete tool backport. Upstream commit:

`8549d1896054dd230ba3038c83bce23b10dcda22`

Its two paths are additive in the inspected JDK 21 target:

- `make/modules/jdk.jcmd/Copy.gmk`
- `src/jdk.jcmd/share/conf/bash-completion/jcmd`

The target preimage for both paths is absent. The change does not alter Java grammar, javac, class files or JVM execution semantics. Build/runtime acceptance remains required before promotion.

## Verification boundary

This programme is being prepared through the authenticated GitHub source trees. A local M3Jdk21 executor was unavailable during this increment, so no JDK build, jtreg suite, shellcheck, Maven lifecycle or runtime smoke test is claimed here. Remote source readback and Git comparison are valid only for the exact branch commit they name.
