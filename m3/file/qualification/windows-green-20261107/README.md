# m3/file windows-green (reactor baseline red removed)

`m3-file` was the one m3 reactor module red on master for module-own reasons on the Windows build host (reactor-wiring
baseline `FULL_VENDORHOLD2_FAILING.tsv`, #2785): two independent causes, both fixed here with hash-pinned edits
(`EDITS.tsv`, `apply.py` copied from reactor-wiring-20261107; `//FLLDCIM: MRL` anchor on the Java change).

| cause | evidence | fix |
|---|---|---|
| surefire: `M3FileDeltaTest.fullSourceClassAndCliContract` -> `M3FileDeltaSelfTest.cliManifestDiffWithRealM3File` "CLI receipt version" | `M3FileDeltaCli` printed the receipt with `PrintStream.println`, which emits CRLF on Windows; the self-test (the contract) asserts the LF header `M3FILEDELTA_RECEIPT_V1\n`; every other jdk.m3.file writer already emits explicit `\n` (grep: the CLI was the only `println` user in `src/main`) | the six receipt lines print with explicit `\n` (2 edits: insert new block, delete old block). Byte-identical output on Linux/macOS (where `println` was already LF); Windows receipts become LF like every other m3/file artifact |
| exec `file-source-class-content-contract`: `verify.py` "module contract inventory changed; review each executable contract" | the pinned inventory (25 entrypoints) predates seven test files added by later leaves (3 JUnit classes, 1 census test, 3 `main()` contracts that JUnit wrappers call) and the 12 donor fixtures + 1 differential test of #2787; each reviewed in `INVENTORY.tsv` (category, `@Test`/`main` counts, calling wrapper and surefire marker) | `verify.py` registers the surefire-run classes in `maven_junit_sources` and a `fixture_sources` category (test-tree sources without an entrypoint, never module contracts); the receipt gains `fixtures_no_entrypoint` (6 edits) |

Gate (`GATES.tsv`): `mvn -B -ntp -T1 -f m3/pom.xml -pl file -am -DskipTests=false package`, JDK 21, module
`-Xlint:all,-module -Werror`: BUILD SUCCESS; m3-file 49 tests, 0 failures (was 49/1); `verify.py` ran its whole
pipeline on the host and on the linked image (17 module contracts, delta self-tests, bounded 48m probes) and ended
with `M3_FILE_DELTA_MODULE_PASS`. Full-reactor comparison against #2785's baseline: see the PR body and
`m3/qualification/reactor-wiring-20261107/FULL_WINDOWS_GREEN_*.tsv`.
