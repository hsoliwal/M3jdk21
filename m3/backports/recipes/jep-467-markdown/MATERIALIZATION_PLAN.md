# JEP 467 current-master materialization plan

Status: candidate execution plan; no product promotion authority.

## Pinned inputs

- Java 21 baseline: `jdk-21+35`
- JDK 23 donor state: `jdk-23+37`
- implementation commit: `0a58cffe88ba823e71fcdcca64b784ed04ca5398`
- selected denominator: **251 exact paths**

## Mechanical pipeline

```text
exact baseline/donor inventory
 -> crate-size=1 Java/text generation
 -> 251-path accounting
 -> FILE preimage/payload proof
 -> packet + atom-evidence validation
 -> ephemeral generated-crate materialization
 -> exact changed-path fence
 -> second apply fixed point
 -> OpenJDK configure
 -> full image build
 -> focused javac/javadoc/markdown jtreg
 -> existing doc-comment/javadoc regressions
 -> built-JDK markdown smoke
 -> serial LIBRARY_API review/promotion
```

The reusable `materialize_generated_crates.py` operator performs byte custody only. It does not
classify compatibility, infer semantic equivalence, widen FILE scope, decide the public API review,
or grant promotion.

## Composition authority

Physical files remain FILE replay atoms. The feature joins across:

- build wiring — MULTI_MODULE;
- java.base support — MODULE;
- compiler public/doc-tree API — LIBRARY_API;
- javac engine — MODULE;
- jdk.internal.md — MODULE;
- javadoc — MODULE;
- tests — MODULE/MULTI_MODULE;
- final feature join — LIBRARY_API.

The 251st path is
`test/langtools/tools/javac/processing/model/util/elements/TestGetDocComments.java`;
it belongs to the regression-test group and does not lower or widen any other atom.

## Required proof

1. generated + typed exclusions = 251;
2. every generated FILE crate has one exact target;
3. materializer refuses drift, duplicate ownership, unsafe paths/payloads and missing required preimages;
4. exact postimages after apply;
5. second apply makes zero byte changes;
6. configured Java 21 build succeeds with the private Markdown module;
7. focused Markdown/javac/javadoc tests pass;
8. existing documentation-comment regressions pass;
9. public API/legal provenance review remains explicit;
10. promotion remains serial and evidence-gated.
