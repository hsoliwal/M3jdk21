# Migration recipe 99% coverage closure

Status: candidate proof; stacked on parser-custody PR #124 head `fab519204af556cfb75148c4c36334537506d013`.

## Scope and locks

Target only the existing `com.m3.rewrite.scope` and `com.m3.rewrite.atom` coverage domain plus new JUnit tests. Public/protected JDK APIs, native ABI, Maven dependency versions, existing workflows, previous tests, JaCoCo includes and the 0.99 line/branch thresholds remain locked.

One production hardening is allowed: canonical repository paths ending in `/` must refuse with the same deliberate `IllegalArgumentException` used for other non-file paths, rather than falling through to an accidental substring exception. Redundant internal branches made unreachable by canonical path validation may be contracted only when valid-path behavior is unchanged.

## Local exact-base evidence

The source owner identities on PR #124 match the recovered and tested PR #117 lineage for scope/atom code. Real Maven 3.9.16/OpenRewrite 8.17.1/JUnit 5.10.2/JaCoCo 0.8.15 dependencies were recovered from the prior exact proof-kit artifact.

A clean focused run with the unchanged gate executed 30 tests, zero failures/errors/skips, and reported `All coverage checks have been met`.

Measured covered/missed counters:
- scope: 158/0 lines; 95/0 branches
- atom: 187/1 lines; 154/2 branches
- combined: 345/346 lines = 99.71%; 249/251 branches = 99.20%

No exclusion, threshold, source-set or dependency change produced that result.

## New tests

Boundary tests cover all supported repository path layouts and canonical-path refusals; malformed/synthetic AST guard branches; every admitted pure-int operator; unsupported effect/throwing/control shapes; depth/node budgets; atomized-shape guards; recipe metadata; TextComment and Javadoc documentation idempotence.

The existing slow 100-file performance-style test was not needed for the focused coverage measurement. It is not deleted or disabled and remains part of ordinary full-suite verification.

## Promotion

This packet closes only the configured migration scope/atom coverage gate when exact-head CI reproduces it. It does not close ordinary String/JNI product proof, native images, jtreg/platform gates, JEP/vendor intake, or the wider whole-JDK task ledger.
