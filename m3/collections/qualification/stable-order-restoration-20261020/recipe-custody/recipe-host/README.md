<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Replay of the existing stable-order atom

This host reuses the exact published Synexia POM, M3TaskRecipeTest and four
one-file pass packets from commit 528aaf79cfae79c5dbfe7c307340329d320e8c44.
It runs the existing canonical hash-pinned owners. ENGINE_SOURCES.tsv binds the
current proposal's actual owner bytes; HARNESS_LINEAGE.json records every copied
file and the narrow host rebinding. The original crate is absent at current
develop f04fbac91a7891e64b77c7b24cef1ab75ec88144; its historical publication and
exact target custody remain the source of these packets.

Select stable-order-groups, index-contract, index-resource-probe or index-junit
with -Dm3.pass. Each pass admits exactly one target and uses the existing named
recipe com.synexia.m3.WrapperConvergence20261018. There is no new rewrite engine.
Run test-compile with -DskipTests only after manifest/source lint, then execute
surefire:test only after that compilation succeeds. The POM accepts an explicit
real parser dependency classpath through -Dm3.parser.classpath.

The five ordered SDK tests cover exact replay, unchanged decoys, fixed point,
input order, mixed state, missing/drift/duplicate/wrong-representation refusals,
post-scan mutation and named descriptor composition. The last test retains actual
SDK Result output under target/candidate/<pass>, guarded by the four earlier
proof groups. Different existing output and symlink paths refuse publication.

The parent CONTRACT.md governs this task. Current product API/JNI behavior,
resource measurements and source-built M3JDK21 acceptance remain distinct gates.
Evidence records what actually ran; the historical PASS.md files are retained
unchanged as donor provenance, not as claims about this fresh execution.
