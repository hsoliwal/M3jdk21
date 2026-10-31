# JEP 523 — Make G1 the Default Garbage Collector in All Environments

Status: materialized Java-21-compatible candidate. Promotion is not authorized.

## Upstream authority

- JEP: 523
- issue: JDK-8383856
- implementation commit: `86637704fd01493ddb57e455dbd1e35e4798237d`
- implementation parent: `50deb1712c62b64937a996e8d025b32d400a2ba8`
- target release: JDK 27
- M3JDK21 target: current Java 21 compatible `master` lineage

The upstream implementation commit changes exactly one product path:

`src/hotspot/share/gc/shared/gcConfig.cpp`

It moves the G1 ergonomic selection outside the server-class-machine conditional. When G1 is
compiled into the VM and no collector is explicitly selected, G1 becomes the default for both
server-class and constrained/non-server ergonomics.

## Compatibility decision

This is a runtime default-policy change, not a Java grammar, class-file or public Java API change.

M3JDK21 keeps all explicit collector options. The adapted Java-21 regression therefore proves:

- no explicit collector + G1 compiled -> G1 for server-class mode;
- no explicit collector + G1 compiled -> G1 for non-server/constrained mode;
- explicit `-XX:+UseSerialGC` still selects Serial and disables G1.

When G1 is not compiled, the product atom retains the existing Java-21 fallback branch:

- server class -> Parallel when available, otherwise Serial;
- non-server -> Serial when available.

That no-G1 build configuration requires build/runtime proof before promotion.

## Recipe atoms

`M3Jep523BackportRecipe` composes two one-cycle atoms:

1. `M3Jdk21HashPinnedTextSnapshotRecipe("jdk27-jep523-g1-default-text")`
   - exact C++ product preimage/postimage;
2. `M3Jdk21HashPinnedSnapshotRecipe("jdk27-jep523-g1-default-java")`
   - exact structured Java jtreg preimage/postimage.

JUnit proves exact replay, stale-preimage rejection and second-pass fixed point.

## Required acceptance

Before promotion:

1. recipe JUnit green;
2. HotSpot product/release build with G1 included;
3. `test/hotspot/jtreg/gc/arguments/TestSelectDefaultGC.java`;
4. existing `TestDisableDefaultGC.java`;
5. explicit Serial/Parallel/G1 startup regressions;
6. at least one no-G1 build configuration proving the fallback branch;
7. ordinary Java-21 runtime smoke;
8. diff/readback and second-pass fixed point.

No throughput claim is made. No JNI/JVMTI ABI changes are introduced.

## Scope

The two source atoms are FILE-local. Their semantic join is HotSpot MODULE policy. Final canonical
promotion remains serial and evidence-gated.
