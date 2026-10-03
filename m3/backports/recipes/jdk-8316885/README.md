# JDK-8316885 — Java 21 compatible CodeHeap analytics diagnostic backport

Upstream issue: `JDK-8316885`, **jcmd: Compiler.CodeHeap_Analytics cmd does not inform about
missing aggregate**.

Pinned donor commit:

```text
openjdk/jdk@1230aed61d286fe9c09f46e2bab626d0e8fe0273
```

## Compatibility decision

The donor changes only HotSpot serviceability output.

Before this packet, requesting a detailed `Compiler.CodeHeap_Analytics` view without first running
`aggregate` silently returned no useful output. The backport emits an explanatory line:

```text
No aggregated code heap data available. Run function aggregate first.
```

When the heap name is already known it reports that heap explicitly.

No Java grammar, source acceptance, class-file version, public Java SE API, JNI/JVMTI ABI, GC
policy, code-generation policy, JIT compilation semantics, persistent format or serialization
contract changes.

The two product postimages are byte-for-byte identical to the pinned OpenJDK donor commit.

## Recipe atoms

`M3Jdk8316885CodeHeapAnalyticsBackportRecipe` composes three FILE-local atoms:

```text
M3Jdk21HashPinnedTextSnapshotRecipe(jdk22-codeheap-analytics-8316885-cpp)
  -> src/hotspot/share/code/codeHeapState.cpp

M3Jdk21HashPinnedTextSnapshotRecipe(jdk22-codeheap-analytics-8316885-hpp)
  -> src/hotspot/share/code/codeHeapState.hpp

M3Jdk21HashPinnedSnapshotRecipe(jdk22-codeheap-analytics-8316885-java)
  -> test/hotspot/jtreg/serviceability/dcmd/compiler/CodeHeapAnalyticsMissingAggregate.java
```

The packet DAG executes those independent FILE atoms in parallel. A MODULE-scope join depends on
all three and carries the semantic feature boundary. No framework projection gains mutation or
promotion authority.

## Focused proof

The adapted Java 21 jtreg:

1. requests `UsedSpace`, `FreeSpace`, `MethodCount`, `MethodSpace`, `MethodAge`, and
   `MethodNames` before aggregation;
2. requires the new missing-aggregate explanation for every mode;
3. executes `Compiler.CodeHeap_Analytics aggregate`;
4. requires a subsequent `UsedSpace` request to stop emitting the prerequisite warning.

## Promotion gates

The packet remains `candidate-adapted` until:

1. packet/static target verifier passes;
2. OpenRewrite recipe JUnit passes;
3. configured HotSpot server image builds;
4. `CodeHeapAnalyticsMissingAggregate.java` passes jtreg;
5. existing `CodeHeapAnalyticsParams.java` and `CodeHeapAnalyticsMethodNames.java` remain green;
6. diff/lint checks pass;
7. second-pass recipe replay is unchanged.

This packet is one bounded backport; it does not imply whole-JDK backport completion.
