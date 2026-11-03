# M3 Java/C17 JNI verification-order qualification — candidate

## Governing invariant

For one isolated verification run: source pin/readback -> source patch qualification ->
Bash syntax/lint -> Java21 strict compile -> C17 JNI strict compile -> Java
contract regression tests -> native checked/interpreted runtime. Any test started
before both applicable compilers complete must fail the admission audit.

## Scoped correction

Original, exact pinned owner: `m3/ports/regex-cursor/proofs/verify-token-cursor-seek-locality-20261102.sh`
in hsoliwal/M3jdk21 PR #2341, with source SHA-256
`c5b927b9530ab6da8c4c720ceddbeb4c8641d0d5024e719ce5c5266fd893d47e`.
The original Java regression tests ran *before* compilation of the unchanged C17 JNI
library. Move only the existing C17 compilation block immediately after the
strict Java 21 compiler, before all runtime/test invocations. This changes neither
Java runtime sources nor JNI exports, compiler flags, test commands, log names,
or pass/fail behavior. It makes the ordering fail-closed and auditable.

## Evidence hierarchy

Source and exact diff -> `bash -n` -> strict `javac`/`gcc` -> Java proof ->
JNI checked/interpreted runtime -> logs and SHA-256. The portable Python
source-stage checker is an independent gate and refuses an old-order fixture.
Real Maven/OpenRewrite/JUnit and whole-JDK jtreg remain separate HOLDs.
No LeetCode/HackerRank/GFG code used: these are category-only donors for
algorithmic optimizations, not verification-order semantics.

## GitHub plan

Create additive Synexia canonical text snapshot recipe, then receive its exact
postimage on an isolated M3JDK21 PR stacked on #2341. No default branch write,
merge, rebase, deletion, or public Java/JNI contract change.
