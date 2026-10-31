# Original A3 behavioral test execution — 2026-10-06

The four original A3 recipe test methods executed offline with the two unchanged canonical recipe owners, their exact current resource/fixture closure, and the two complete test classes produced by the already verified CI repair. Three methods pass; one method errors on a malformed existing Java template. No original assertions or cases were changed or skipped.

## Exact scope

- Current M3 revision: `d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2`.
- Root tree: `0812573f02908d8078d13f5f49346865cdf1c0e0`.
- M3 tree: `581f0c626bb8466afc71ac5171694e54be87327b`.
- Java owner: `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`, Git blob `d58ca50dfaeb60f581b64f43545e688aca5c6530`.
- Text owner: `com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe`, Git blob `a2e8c916208b563517e51b738744a05357e633aa`.
- Dependency/toolchain versions remain OpenRewrite 8.17.1, JUnit Jupiter 5.10.2, Maven 3.9.9, and JDK 21+35.
- The cache used for execution was a private verified copy. All 3,033 files in the immutable source cache were checked again after execution.

## Actual outcomes

| Original method | Result |
| --- | --- |
| `M3A3RegexMemoryWorkflowRecipeTest.exactCurrentTextPreimagesAdvanceRegexMemoryGateAndReplayAtFixedPoint` | Pass: three expected text outputs and no changes on replay |
| `M3A3RegexMemoryWorkflowRecipeTest.declarativeRecipeBindsExactTextCrate` | Pass |
| `M3A3RegexMemoryLabDeliveryRecipeTest.declarativeRecipeBindsExactHashPinnedJavaCrate` | Pass |
| `M3A3RegexMemoryLabDeliveryRecipeTest.exactCurrentA3PreimagesAdvanceRegexMemoryLabAndReachFixedPoint` | Error: the existing Java owner refuses `A3RegexMatrix.java` with `Java template parse/format drift` |

The failing template contains `"Pattern.compile("a+b?")"` at line 61, with its nested quotes unescaped. A separate invocation of the actual pinned javac against the complete, unchanged template reports three syntax errors at that line. Thus this is an invalid current template, rather than evidence that the pinned parser cannot represent valid Java.

The recipe template `m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/a3-regex-memory-lab/A3RegexMatrix.java.after` and current production file `m3/tooling/a3/src/main/java/com/m3/a3/A3RegexMatrix.java` share Git blob `bf91afcd627d8e2faa9790ea6592b6bc9c1b0e33`, SHA-256 `f000b1eea411056baaca6051b220149f25334bc70bf69b4f0536839c6c78e404`, and 7,514 bytes. The manifest correctly hashes these invalid bytes; a repair must update the production source, template, and its declared hash consistently through the existing recipe owner.

## Evidence and replay

`EXECUTION.json` records the actual process, all four method outcomes, and exact Surefire report identities. `MAVEN_PROCESS.json`, `maven.log`, and `junit/` retain the failure. `diagnostic/JAVAC_PROCESS.json` and `diagnostic/javac.log` retain the independent compiler check. `CURRENT_ADMISSION.json` binds the 18 unchanged owner/resource inputs to the current revision after recomputing the relevant Git trees; it separately binds the two executed test files to actual verified CI outputs over the exact current preimages. Those repaired test bytes are not asserted to be in current master. `RUNTIME_CUSTODY.json` records the actual test classpath artifacts and their hashes. The raw retrieval captures for the 18 owner/resource blobs are under `calls/`.

Run the focused repository verification with:

```sh
mvn -o -B -ntp -f m3/tooling/migration-recipes/tasks/jcc-ci-repair-20261006/a3-behavior-verification/pom.xml test
```

This command needs the same pinned JDK and exact dependencies available offline. The original test bodies, production owners, templates, and manifests have not been changed in this evidence supplement. The earlier eight-method CI-repair proof and javac compilation remain valid. This supplement closes the prior lack of execution evidence, but does not close the failing A3 Java behavioral gate. It does not claim a whole-module build, foundation workflow execution, A3 runtime behavior qualification, native/JNI qualification, or JDK build.
