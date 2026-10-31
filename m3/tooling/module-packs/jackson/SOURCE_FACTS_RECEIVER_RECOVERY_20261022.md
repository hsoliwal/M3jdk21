# M3 Jackson source-facts receiver — additive PR #1289 recovery

This is a bounded Java 21 source recovery from [M3JDK21 PR #1289](https://github.com/hsoliwal/M3jdk21/pull/1289), head \`5ec89ae530defe1c7df70e9b3ffd985b9e21f293\`.

The original PR contains 51 changed files, all additions relative to its historical base. A current \`master\` tree census found 45 missing paths, while 6 existing Jackson receiver paths have different blobs. Importing the entire historical branch would be unsafe because those six product owners have advanced independently.

## Admitted implementation

| Java 21 receiver | Exact original Git blob |
| --- | --- |
| \`src/main/java/jdk/m3/jackson/M3JsonLimits.java\` | \`c08f91664245759463c654f244cdf2bfaf19fbb3\` |
| \`src/main/java/jdk/m3/jackson/M3SourceHash.java\` | \`0465703e4c14d920d85764abd1b38a299378d50a\` |

The source files are original, byte-identical Git blobs from PR #1289. They are M3JDK21-owned Java implementation, not live Synexia recipe dependencies.

## Executed verification

Java 21 \`javac --release 21 -Xlint:all -Werror\` compiled both source files with the added standalone \`M3SourceFactsContract\` harness. All 16 assertions passed: positive and invalid memory budgets, bounded 100 MiB qualification budget, source-domain separation, UTF-16/code-unit sensitivity including supplementary and unpaired surrogates, and raw SHA-256 parity. The reproducible GitHub workflow is \`.github/workflows/m3-jackson-source-facts-java21.yml\`.

This focused gate **does not claim full JDK/JMOD/JNI or Jackson reactor success**, and does not modify \`M3File\`, \`M3ObjectMapper\`, \`module-info.java\`, build scripts, or JNI kernels. The remaining receiver files and their dependencies stay in PR #1289 pending reconciliation and full verification. No squash/rebase/force-push, code deletion, or ancestry-only merge is used.
