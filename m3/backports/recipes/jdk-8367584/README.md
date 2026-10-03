# JDK-8367584 — Java 21 compatible FlightRecorderOptions help leaf

Upstream issue: `JDK-8367584`, implemented in donor commit
`openjdk/jdk@39de79eae23410e335d2d1ced8fe3b4d7937a541`.

The upstream commit implements JEP 536 JFR in-process data redaction. M3JDK21 intentionally imports
only the independent `-XX:FlightRecorderOptions:help` leaf that can be expressed without adopting
JEP 536 redaction events, filters, or runtime policy.

## Compatibility decision

The admitted leaf:

- adds startup dispatch for `-XX:FlightRecorderOptions:help`;
- prints syntax and the JFR options already present in JDK 21;
- exits after help rather than starting the VM;
- adds the focused startup-options jtreg;
- does not add redaction filters, redaction keys, new JFR redaction events, or new JEP 536 runtime
  semantics.

No Java language grammar, class-file version, public Java SE API, JNI/JVMTI ABI, persistent format,
GC policy, or JIT contract is changed.

## Recipe atoms

`M3Jdk8367584JfrOptionsHelpBackportRecipe` composes exactly two source-sealed atoms:

1. `M3Jdk21HashPinnedTextSnapshotRecipe(jdk27-jfr-options-help-8367584-text)`
   - `src/hotspot/share/jfr/dcmd/jfrDcmds.cpp`
   - `src/hotspot/share/jfr/dcmd/jfrDcmds.hpp`
   - `src/hotspot/share/jfr/recorder/service/jfrOptionSet.cpp`
2. `M3Jdk21HashPinnedSnapshotRecipe(jdk27-jfr-options-help-8367584-java)`
   - additive `test/jdk/jdk/jfr/startupargs/TestOptionsHelp.java`

All existing targets are bound to exact current-master SHA-256 preimages. The jtreg has an explicit
`ABSENT` preimage. Recipe JUnit proves exact replay, redaction exclusion, stale-preimage refusal and
a zero-change second pass.

## Scope

The edit is confined to HotSpot JFR implementation and its focused JDK test, so the packet is
classified as MODULE scope. It does not receive language, multi-module, or public-library API
authority.

## Promotion gates

Status remains `candidate-adapted` until:

1. packet hash verifier passes;
2. recipe JUnit passes;
3. OpenJDK configure/build image passes;
4. focused `TestOptionsHelp.java` jtreg passes;
5. help-output smoke confirms the new syntax text and nonzero redaction absence contract;
6. diff/lint checks pass;
7. recipe second-pass fixed point passes.

The packet makes no claim that JEP 536 itself has been backported.
