# JEP 485 Stream Gatherers — M3JDK21 current-tree recovery

Status: candidate product recovery; no completion claim.

## Authority

- final feature: JEP 485, Stream Gatherers
- donor: OpenJDK JDK 24 GA tag `jdk-24+36`
- implementation lineage: `33b26f79a986d015abdcd84b89842adc0a4bde64`, graduation `ef0dc2518e7636cc8a9ca580613ff5edeb4c19fd`, mapConcurrent fix `450636ae28b84ded083b6861c6cba85fbf87e16e`
- target package: `java.util.stream` in `java.base`

## Current-tree precondition proof

Pinned M3JDK21 base: `fc6dedcb84e8eb9f95689d4e9ff9d1614775b529`.

The four modified Java 21 stream owners are byte-identical to the retained reviewed preimages:

- AbstractPipeline.java — Git blob `9517bc4f7b8fd71d37009cb4c9ae3a035bd96382`
- ReferencePipeline.java — Git blob `bdc9e7eab3d72cbf08db28fa02227e7cc3e24356`
- Stream.java — Git blob `9badd4133f15aacdd7b1b0a1d04dfcf00e267c2e`
- package-info.java — Git blob `635f44e74c561ad9ded58bac43dcab87e3ca1312`

Gatherer.java, GathererOp.java, Gatherers.java and all eight retained Gatherer jtreg files are absent on the pinned base.

The retained 15-target hash-pinned source crate is present on current master. Therefore recovery must replay that crate rather than merge the old PR tree.

## Contract / scope

This is an additive public Java SE library API backport. Authority is `LIBRARY_API / EXPLICIT_CONTRACT_CHANGE`. No language grammar or class-file format change is admitted by this packet.

## Admission gates

1. exact recipe replay and second-pass fixed point;
2. registry/recipe catalogue authority;
3. OpenJDK configure with a JDK21 boot JDK;
4. full JDK image build;
5. retained Gatherer jtreg family;
6. existing Stream recursive/short-circuit regressions;
7. built-JDK API smoke;
8. current-tree source retention after merge.

No performance or full-JDK completion claim is valid before those gates execute.
