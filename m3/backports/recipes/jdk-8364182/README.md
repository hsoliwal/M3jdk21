# JDK-8364182 — Java 21 compatible VM.security_properties backport

Upstream issue: `JDK-8364182`, **Add jcmd VM.security_properties command**.

Pinned donor commit:

```text
openjdk/jdk@f2f8828188f45d16344c82adfbf951f7409b8825
```

## Compatibility decision

This packet is additive serviceability behavior:

- adds `VM.security_properties`;
- preserves the existing Java 21 `VM.system_properties` command;
- adds one internal SharedSecrets accessor for the live `java.security.Security` properties;
- adds one internal VMSupport serialization method;
- adds the HotSpot diagnostic command and VM symbol;
- adds the upstream focused jtreg test.

No Java grammar, source acceptance, class-file version, public Java SE API, JNI/JVMTI ABI, GC,
JIT, serialization or persistent format contract changes.

The only source adaptation is mechanical: JDK21's `DCmdFactoryImpl` registration still uses the
explicit `(export, enabled, hidden)` constructor, so the new command is registered with
`(full_export, true, false)`. Command semantics are otherwise donor-equivalent.

## Recipe DAG

`M3Jdk8364182BackportRecipe` composes two one-cycle atoms:

```text
M3Jdk21HashPinnedSnapshotRecipe(jdk27-security-properties-8364182-java)
  -> Security.java
  -> JavaSecurityPropertiesAccess.java
  -> VMSupport.java
  -> SecurityPropertiesTest.java

M3Jdk21HashPinnedTextSnapshotRecipe(jdk27-security-properties-8364182-text)
  -> vmSymbols.hpp
  -> diagnosticCommand.cpp
  -> diagnosticCommand.hpp
```

All existing targets are bound to exact JDK21 SHA-256 preimages. The jtreg test has an explicit
`ABSENT` preimage. JUnit requires exact replay and a zero-change second pass.

## Promotion gates

Candidate remains `candidate-adapted` until:

- recipe JUnit passes;
- backport catalogue verifier passes;
- focused `java.base` / HotSpot server build passes;
- `SecurityPropertiesTest.java` jtreg passes;
- the existing `VM.system_properties` smoke/compatibility path remains working;
- diff/lint checks pass;
- second-pass recipe fixed point passes.

Do not infer whole-JDK compatibility from this focused packet.
