# CPU tools JMOD qualification

This report mirrors the [canonical qualification report](https://github.com/hsoliwal/com.synexia/blob/fd90cb89476b3da556a2ee89a827c36a9a56a18c/synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018/QUALIFICATION.md) at `fd90cb89476b3da556a2ee89a827c36a9a56a18c`, with links relocated to receiving evidence and source where available. [Canonical draft PR](https://github.com/hsoliwal/com.synexia/pull/12349).

## Review conclusion

The optional CPU pack passes its canonical recipe, actual receiver and final Linux x86_64 M3 Java 21 build qualification. The result contains **20 optional JMODs**: three existing text/I/O/structured modules, sixteen selected donor modules and `com.m3.tools.cpu`. Its linked image contains **47 modules total**, including JDK dependencies. The registry exposes **22 operations** plus direct Java APIs. See the [final qualification aggregate](FINAL_QUALIFICATION.json), [build receipt](pack-final/BUILD_RECEIPT.json), [API contracts](../../tooling/module-packs/cpu-tools/README.md) and [coverage map](../../tooling/module-packs/cpu-tools/COVERAGE.tsv).

This qualifies the optional add-on for draft review. The 110-entry catalogue (91 families plus 19 Unix rows) remains incomplete; M3File implements a bounded UTF-8 snapshot adapter, with the broader MIndexFile token/postings plane still partial. Public `String` uses actual M3String backing in the enabled-runtime tests, and M3AstTools exercises actual M3AST callbacks. No performance, throughput or resource-efficiency measurements are claimed.

## Canonical recipe and actual application

Canonical donor/engine baseline: `50f48cbbe11f17fb8dc209bac3812f8940a1e267`. Target baseline: `f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`. The [provenance ledger](../../tooling/module-packs/cpu-tools/PROVENANCE.tsv) identifies source owners. Apply the sealed datasets in order:

| Dataset | Targets | SDK / prepare / receiver cases | Actual application |
|---|---:|---|---|
| `current` | 61: 40 Java, 21 text | [5 / 10 / 10 passed](sdk-final/SDK_QUALIFICATION.json) | [61 changes; replay 0](receiver-actual/RECEIVER_ACTUAL.json) |
| `native-link` | 5 text replacements | [5 / 10 / 10 passed](sdk-native-link-final/SDK_QUALIFICATION.json) | [5 changes; replay 0](receiver-native-link-actual/RECEIVER_ACTUAL.json) |
| `linker-temp` | 4 text replacements | [5 / 10 / 10 passed](sdk-linker-temp-final/SDK_QUALIFICATION.json) | [4 changes; replay 0](receiver-linker-temp-actual/RECEIVER_ACTUAL.json) |

The final union remains 61 target files. Each actual receiver checked exact SDK postimages and a zero-change fixed point preserving bytes, modes and modification times. The SDK exercises drift, missing/duplicate/wrong representations, post-scan mutations, mixed preimages/postimages, replay and input ordering. Java uses the existing OpenRewrite Java source-tree engine. Successors bind their preceding results; shared engines and the bound harness remain unchanged across the two text successors.

The [donor API audit](https://github.com/hsoliwal/com.synexia/blob/fd90cb89476b3da556a2ee89a827c36a9a56a18c/synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018/qualification/donor-public-api.json) compares **21 public type surfaces across sixteen modules**. Topology uses the documented declaration-repaired API witness because the raw donor declaration was refused; this audit does not imply identical donor implementations.

## Final pack evidence

The [final Maven invocation](maven-pack-v4-command.json) completed successfully. Its [log](maven-pack-v4.log) records **18 Python build contracts** passing; [COMMANDS.json](pack-final/COMMANDS.json) records **83 passing bounded subprocesses**.

| Linked-image check | Result |
|---|---:|
| Sixteen donor tools | 105,059 assertions passed |
| M3File/M3AST/source integration, default / enabled M3String storage | 3,847 / 3,851 checks passed |
| Aggregate CPU integration, default / enabled M3String storage | 2,991 / 2,994 checks passed |
| Existing text/JNI smoke | 24 tools passed |
| CLI catalogue and self-test replay | Exact repeated output; 20 modules, 22 bindings |

The build receipt binds compiled sources, toolchain inputs, modular artifacts and logs. Source/toolchain identities match before and after execution. Native provenance verifies original compiled JNI bytes against the unique JMOD member, then independently applies the same PATH-resolved `objcopy -g` transformation and matches the linked library exactly. Compiler/objcopy executable identities and PATH remain stable. Both existing text-tools and donor CPU legal notice trees have exact byte-hash readback in every JMOD and linked module. The original JNI C owner and sole existing JMOD/image packager remain unchanged. JAR timestamps are fixed; byte-reproducible JMODs are explicitly unclaimed.

## Runtime provenance boundary

The executing source image reports **`git:4b24824c3aa0+`**, a dirty runtime lineage. Whole-JDK qualification at clean target `f29b0af` remains outstanding. The [owner comparison](runtime-source-owners.json) verifies identical target-baseline/runtime-checkout bytes for `String.java`, `M3String.java` and `M3AST.java`; it proves only those three source matches. Actual binaries are independently bound in [TOOLCHAIN_INPUTS.tsv](pack-final/TOOLCHAIN_INPUTS.tsv) and the build receipt:

| Runtime artifact | SHA-256 |
|---|---|
| `release` | `4c2ea1b8681670ccb0bd91d2d67d7e3dab97745087dc52f3bee9329e5a9c6e9a` |
| `lib/modules` | `6411554d3cd593f02a6ae73a80c16edbfee92c11a9908037450f24810bde9dc2` |
| `lib/server/libjvm.so` | `807a66b5923c7ea4d33de84b783fae8eb30505c674c7d90e3258b0afe17d8012` |

## Retained failures and release limits

[SDK v1](https://github.com/hsoliwal/com.synexia/blob/fd90cb89476b3da556a2ee89a827c36a9a56a18c/synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018/qualification/sdk-v1-refusal/FAILURE.json) refused the incumbent JPMS Java source layout. A narrow canonical layout successor preserved Java source-tree processing. [SDK v2](https://github.com/hsoliwal/com.synexia/blob/fd90cb89476b3da556a2ee89a827c36a9a56a18c/synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018/qualification/sdk-v2-timeout/TIMEOUT.json) exceeded its outer timeout; subsequent [harness reuse clarification](https://github.com/hsoliwal/com.synexia/blob/fd90cb89476b3da556a2ee89a827c36a9a56a18c/synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018/qualification/source-harness-mixed-reuse.md) and the full final SDK run resolved qualification. Both earlier runs retain their unsuccessful status.

[Pack v1](https://github.com/hsoliwal/com.synexia/blob/fd90cb89476b3da556a2ee89a827c36a9a56a18c/synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018/qualification/maven-pack-v1-command.json) exhausted overlay storage. Pack v2 used separate staging but [refused an incorrect raw-versus-linked native-byte assumption](https://github.com/hsoliwal/com.synexia/blob/fd90cb89476b3da556a2ee89a827c36a9a56a18c/synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018/qualification/pack-v2-native-refusal/NATIVE_DIAGNOSIS.json); `native-link` introduced exact transformation-aware verification. [Pack v3](https://github.com/hsoliwal/com.synexia/blob/fd90cb89476b3da556a2ee89a827c36a9a56a18c/synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018/qualification/pack-v3-linker-temp-refusal/BUILD_RECEIPT.json) exhausted Java's default temporary storage during linking. `linker-temp` confines nested Java scratch to the output directory using a quoted, isolated command environment. Pack v4 passes with both corrections; previous failures remain retained evidence.

The [existing scoped gates](review/scoped-gates.json) include **19 passing ToolPackLock tests** and **6/7 passing legacy SDK receiver tests**. The remaining receiver failure is an [inherited packager successor-ledger hash mismatch](review/existing-sdk-receiver-baseline.json): actual packager bytes equal the target baseline. That gate remains failed.

Full-JDK jtreg and platform CI were not run. Publication requires a **draft PR** and maintainer review of the remaining platform/full-JDK gates. The scoped results establish tested functionality and provenance within the stated contracts; they do not establish complete family promotion or hard operating-system CPU/memory isolation.
