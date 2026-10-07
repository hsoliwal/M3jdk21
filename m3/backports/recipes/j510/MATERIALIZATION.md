# JEP 510 current-tree materialization

Status: **candidate materialized, promotion not authorized**.

The product tree on this branch is the exact postimage of
`com.m3.jdk21.Jep510Kdf`:

- 5 additive KDF/HKDF Java classes;
- 3 Java-21 receiver integrations.

The security proof tree is the exact postimage of
`com.m3.jdk21.Jep510KdfCandidate`'s test atom:

- RFC 5869 known-answer tests;
- basic extract / expand / extract-then-expand behavior;
- non-extractable PRK delayed-provider behavior;
- delayed-provider synchronization;
- delayed-provider threading behavior.

The tests are pinned to OpenJDK commit
`79456110fb6dd11ef19e9637c6f40ee7ce329481`, the cumulative JEP 510 lineage
already recorded by this packet.

## Remaining gates

1. Maven/OpenRewrite recipe JUnit and fixed point.
2. A3 atomize/patternize preparation for affected Java source.
3. Java-21 boot JDK configure/build.
4. Focused KDF jtreg tests.
5. Provider registration/order smoke.
6. Relevant broader java.base security tests.
7. Candidate fixed-point replay against the materialized tree.
8. Canonical master readback after any promotion decision.

Until those pass, `promotion=NOT_AUTHORIZED`.
