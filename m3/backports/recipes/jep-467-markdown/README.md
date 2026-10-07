# JEP 467 — Markdown Documentation Comments

Status: inventory / dependency-closure. No product-backport completion claim.

## Upstream authority

- JEP 467 — Markdown Documentation Comments
- JBS: JDK-8298405
- implementation commit: `0a58cffe88ba823e71fcdcca64b784ed04ca5398`
- implementation touched-path denominator: **251 files**
- JDK 23 GA tag: `jdk-23+37`
- JDK 23 GA commit: `9ad2e63f176364b96a827af80055e7db4b61fc9a`

The implementation commit defines the path denominator. GA is a follow-up review source, not automatic
replacement authority: each path must distinguish feature follow-ups from unrelated later changes.

## Compatibility boundary

This is not a Java source-language grammar backport, but it is also not a small javadoc-only patch.
The implementation spans:

- `javac` tokenizer/doc-comment/parser/tree/model internals;
- public/compiler-model and doc-tree API surfaces;
- `jdk.javadoc` rendering and tooling;
- a new private `jdk.internal.md` module containing transformed/vendored CommonMark sources;
- build/module wiring and legal provenance;
- extensive langtools tests.

Therefore the feature join requires **LIBRARY_API** authority and explicit contract review even though
many implementation leaves are independently replayable FILE atoms.

## M3 execution law

Every physical source/resource/build target is reviewed as an independent FILE atom first. Public API,
module, compiler and doclet joins happen only after the FILE fixed points are known. The canonical
packet DAG is framework-neutral; Camel, Airflow and Drools are projections only.

## Donor/licensing rule

The private Markdown module is derived from commonmark-java. No Apache-2.0 relabeling of OpenJDK or
third-party donor code is permitted. Preserve the OpenJDK legal file and audit the transformations and
upstream license before any materialization.

## Required next pass

1. compare each of the 251 implementation paths against JDK21 and JDK23 GA;
2. classify GA changes after the implementation commit as feature-related or unrelated;
3. generate one exact Java/text FILE recipe atom per admitted target;
4. explicitly review additive public compiler/doc-tree API;
5. verify the vendored Markdown module legal/provenance boundary;
6. compose the smallest honest LIBRARY_API packet;
7. build `jdk.compiler`, `jdk.javadoc`, `jdk.internal.md`;
8. run JEP467 Markdown/javac/javadoc langtools plus existing doc-comment regressions;
9. prove second-pass fixed point;
10. promote only after exact-head receipts.

